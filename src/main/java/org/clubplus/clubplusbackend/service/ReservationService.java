package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final CategorieDao categorieRepository;
    private final SecurityService securityService; // Injecter

    // Constante pour la limite (peut être mise en configuration)
    private static final int RESERVATION_MAX_PER_EVENT_PER_MEMBER = 2; // Exemple

    /**
     * Crée une nouvelle réservation pour l'utilisateur courant.
     * Sécurité: Utilisateur authentifié.
     * Règles: Event futur, catégorie valide, limite/capacité non atteinte.
     * Lance 404, 403 (implicite via service), 409, 400.
     */
    public Reservation createMyReservation(Integer eventId, Integer categorieId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Récupère ID ou lance exception

        // 1. Récupérer les entités (lancent 404 si non trouvées)
        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre courant non trouvé (ID: " + currentUserId + ")")); // Devrait être rare
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId));
        // Récupérer la catégorie pour vérifier capacité et appartenance à l'event
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId));

        // --- Validation Règles Métier ---

        // 2. Vérifier appartenance catégorie à événement (déjà dans constructeur mais double check ici OK)
        if (!categorie.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("La catégorie (ID " + categorieId + ") n'appartient pas à l'événement (ID " + eventId + ")."); // -> 400
        }

        // 3. Événement futur ?
        if (event.getStart() == null || event.getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de réserver : l'événement est déjà commencé ou passé."); // -> 409
        }

        // 4. Limite de réservations par membre pour cet événement ?
        long existingReservationsCount = reservationRepository.countByMembreIdAndEventId(currentUserId, eventId);
        if (existingReservationsCount >= RESERVATION_MAX_PER_EVENT_PER_MEMBER) {
            throw new IllegalStateException("Limite de " + RESERVATION_MAX_PER_EVENT_PER_MEMBER + " réservations/membre atteinte pour cet événement."); // -> 409
        }

        // 5. Capacité de la catégorie ? (Vérification atomique moins critique ici, mais OK)
        long currentReservedInCategory = reservationRepository.countByCategorieId(categorieId);
        Integer capaciteCategorie = categorie.getCapacite(); // Utiliser getter
        if (capaciteCategorie == null || currentReservedInCategory >= capaciteCategorie) {
            throw new IllegalStateException("Capacité maximale atteinte pour la catégorie '" + categorie.getNom() + "'."); // -> 409
        }

        // --- Création et Sauvegarde ---
        Reservation newReservation = new Reservation(membre, event, categorie); // Constructeur gère UUID, date, et lien
        return reservationRepository.save(newReservation);
    }

    // --- Méthodes de Lecture (avec Sécurité) ---

    /**
     * Récupère une réservation par son ID.
     * Sécurité: L'utilisateur doit être le propriétaire OU manager du club organisateur.
     * Lance 404 (Non trouvé), 403 (Accès refusé).
     */
    @Transactional(readOnly = true)
    public Reservation getReservationByIdWithSecurityCheck(Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")")); // -> 404
        // Sécurité Contextuelle
        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation); // Méthode à créer dans SecurityService -> 403
        return reservation;
    }

    /**
     * Récupère les réservations de l'utilisateur courant.
     * Sécurité: Utilisateur authentifié.
     */
    @Transactional(readOnly = true)
    public List<Reservation> findMyReservations() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return reservationRepository.findByMembreId(currentUserId);
    }

    /**
     * Récupère les réservations pour un événement donné.
     * Sécurité: L'utilisateur doit être MANAGER du club organisateur.
     * Lance 404 (Event non trouvé), 403 (Non manager).
     */
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsByEventIdWithSecurityCheck(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")")); // -> 404
        // Sécurité Contextuelle
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId()); // -> 403
        return reservationRepository.findByEventId(eventId);
    }

    /**
     * Récupère les réservations pour une catégorie donnée.
     * Sécurité: L'utilisateur doit être MANAGER du club organisateur.
     * Lance 404 (Catégorie non trouvée), 403 (Non manager).
     */
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsByCategorieIdWithSecurityCheck(Integer categorieId) {
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId + ")")); // -> 404
        // Sécurité Contextuelle (via l'événement de la catégorie)
        securityService.checkManagerOfClubOrThrow(categorie.getEvent().getOrganisateur().getId()); // -> 403
        return reservationRepository.findByCategorieId(categorieId);
    }


    // --- Suppression ---

    /**
     * Supprime (annule) une réservation par son ID.
     * Sécurité: L'utilisateur doit être le propriétaire OU manager du club organisateur.
     * Règle: Annulation impossible si l'événement est commencé/passé.
     * Lance 404 (Non trouvé), 403 (Accès refusé), 409 (Event passé).
     */
    public void deleteReservationById(Integer reservationId) {
        // 1. Récupérer la réservation (lance 404)
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")"));

        // 2. Vérification Sécurité Contextuelle (lance 403)
        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation); // Méthode à créer

        // 3. Vérification métier (événement futur ?)
        if (reservation.getEvent() == null || reservation.getEvent().getStart() == null ||
                reservation.getEvent().getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Annulation impossible : l'événement est déjà commencé ou passé."); // -> 409
        }

        // 4. Suppression
        reservationRepository.delete(reservation);
    }
}

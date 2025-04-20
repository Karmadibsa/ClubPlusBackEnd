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
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
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
     * Vérifie l'état actif de l'événement et la capacité de la catégorie (places confirmées).
     * ... (autres règles inchangées)
     */
    public Reservation createMyReservation(Integer eventId, Integer categorieId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre courant non trouvé (ID: " + currentUserId + ")"));
        // Récupérer la catégorie suffit, elle contient l'événement
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId + ")"));
        Event event = categorie.getEvent(); // Récupérer l'événement via la catégorie
        securityService.checkMemberOfEventClubOrThrow(eventId);

        // --- VALIDATIONS ---
        // 1. Événement existe (implicite via catégorie) et est ACTIF
        if (event == null) { // Sécurité si la relation est rompue
            throw new EntityNotFoundException("Événement lié à la catégorie non trouvé.");
        }
        // La vérification event.getActif() est maintenant dans le constructeur de Reservation,
        // mais la garder ici est une bonne double vérification avant de charger/créer.
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de réserver : l'événement (ID: " + eventId + ") est annulé."); // -> 409
        }

        // 2. Cohérence catégorie/événement (déjà dans constructeur, mais ok ici)
        if (!categorie.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("La catégorie (ID " + categorieId + ") n'appartient pas à l'événement (ID " + eventId + ").");
        }


        // 3. Événement futur ?
        if (event.getStart() == null || event.getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de réserver : l'événement est déjà commencé ou passé.");
        }

        // 4. Limite par membre
        long existingReservationsCount = reservationRepository.countByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.CONFIRME); // Compte seulement CONFIRMED
        if (existingReservationsCount >= RESERVATION_MAX_PER_EVENT_PER_MEMBER) {
            throw new IllegalStateException("Limite de " + RESERVATION_MAX_PER_EVENT_PER_MEMBER + " réservations confirmées/membre atteinte pour cet événement.");
        }

        // 5. Capacité de la catégorie (basée sur les places CONFIRMEES)
        // Utiliser la méthode de Categorie qui filtre par statut
        if (categorie.getPlaceDisponible() <= 0) { // Vérifie si > 0 places dispo
            throw new IllegalStateException("Capacité maximale (confirmée) atteinte pour la catégorie '" + categorie.getNom() + "'."); // -> 409
        }

        // --- Création ---
        // Le constructeur met le statut à CONFIRMED et vérifie event actif
        Reservation newReservation = new Reservation(membre, event, categorie);
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
     * Récupère les réservations de l'utilisateur courant, potentiellement filtrées par statut.
     * Par défaut (statusFilter = null ou "all"), retourne tous les statuts.
     *
     * @param statusFilter "CONFIRMED", "USED", "CANCELLED", ou null/"all".
     * @return La liste des réservations filtrée.
     */
    @Transactional(readOnly = true)
    // Ajouter le paramètre statusFilter à la signature de la méthode
    public List<Reservation> findMyReservations(String statusFilter) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        try {
            ReservationStatus status = null; // Par défaut, on veut tout

            // Si un filtre est fourni et n'est pas "all", on essaie de le convertir en Enum
            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                // Convertit la chaîne (ex: "CONFIRMED") en valeur Enum. Le toUpperCase() gère la casse.
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            }

            // Appelle la méthode du repository appropriée
            if (status != null) {
                // Si un statut valide est trouvé, filtre par ce statut
                return reservationRepository.findByMembreIdAndStatus(currentUserId, status);
            } else {
                // Sinon (statusFilter est null, "all", ou invalide après le try-catch), retourne tout
                return reservationRepository.findByMembreId(currentUserId);
            }
        } catch (IllegalArgumentException e) {
            // Si statusFilter est une chaîne qui ne correspond à aucun Enum ReservationStatus
            System.err.println("Statut de réservation invalide fourni pour findMyReservations: " + statusFilter);
            // Retourne une liste vide ou lance une exception BadRequestException si tu préfères
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les réservations d'un événement, avec sécurité et filtrage par statut.
     * Par défaut (statusFilter = null ou "all"), retourne tous les statuts.
     *
     * @param eventId      L'ID de l'événement.
     * @param statusFilter "CONFIRMED", "USED", "CANCELLED", ou null/"all".
     * @return La liste des réservations filtrée.
     */
    @Transactional(readOnly = true)
    // Ajouter le paramètre statusFilter à la signature de la méthode
    public List<Reservation> findReservationsByEventIdWithSecurityCheck(Integer eventId, String statusFilter) {
        // 1. Vérifier existence event & sécurité (inchangé)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        // 2. Appliquer le filtre (logique similaire à findMyReservations)
        try {
            ReservationStatus status = null; // Par défaut, on veut tout

            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            }

            if (status != null) {
                return reservationRepository.findByEventIdAndStatus(eventId, status);
            } else {
                return reservationRepository.findByEventId(eventId); // Récupère tout
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Statut de réservation invalide fourni pour findReservationsByEventId: " + statusFilter);
            return Collections.emptyList(); // Ou lancer une exception BadRequestException
        }
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
     * Annule une réservation par son ID.
     * Sécurité: Propriétaire OU manager du club.
     * Règle: Annulation impossible si l'événement est commencé/passé OU si la résa est déjà annulée/utilisée.
     */
    public void cancelReservationById(Integer reservationId) {
        // 1. Récupérer la réservation (peu importe son statut initial)
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")"));

        // 2. Sécurité Contextuelle
        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation);

        // 3. Vérification métier: Événement futur ?
        if (reservation.getEvent() == null || reservation.getEvent().getStart() == null ||
                reservation.getEvent().getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Annulation impossible : l'événement est déjà commencé ou passé."); // -> 409
        }

        // 4. Vérification métier: Statut actuel ?
        if (reservation.getStatus() != ReservationStatus.CONFIRME) {
            throw new IllegalStateException("Annulation impossible : la réservation n'est pas au statut CONFIRMED (statut actuel: " + reservation.getStatus() + ")."); // -> 409
        }

        // 5. Mettre à jour le statut
        reservation.setStatus(ReservationStatus.ANNULE);
        reservationRepository.save(reservation); // Sauvegarder le changement de statut
    }

    /**
     * Marque une réservation comme UTILISÉE (par exemple, après scan QR code).
     * Utilise l'UUID pour l'identifier, idéal pour une API mobile.
     * Sécurité: Rôle spécifique (SCANNER, MANAGER, etc.) - à définir dans SecurityService.
     * Règles: Événement actif et en cours, Réservation doit être CONFIRMED.
     *
     * @param reservationUuid L'UUID unique de la réservation.
     * @throws EntityNotFoundException Si aucune réservation avec cet UUID n'est trouvée.
     * @throws SecurityException       Si l'utilisateur n'a pas le droit de scanner pour cet événement.
     * @throws IllegalStateException   Si l'événement n'est pas actif/en cours ou si la réservation n'est pas confirmée.
     */
    public Reservation markReservationAsUsed(String reservationUuid) {
        // 1. Récupérer la réservation par UUID
        Reservation reservation = reservationRepository.findByReservationUuid(reservationUuid)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée avec l'UUID : " + reservationUuid));

        Event event = reservation.getEvent(); // Récupérer l'événement lié

        // 2. Sécurité Contextuelle: L'utilisateur peut-il "scanner" pour cet événement/club ?
        // securityService.checkCanScanForEventOrThrow(event); // Méthode à créer dans SecurityService

        // 3. Vérifications Métier:
        //    a) Événement existe et est actif ?
        if (event == null || !event.getActif()) {
            throw new IllegalStateException("Impossible de marquer comme utilisée : l'événement lié est annulé ou introuvable."); // -> 409
        }
        //    b) Est-on dans la fenêtre de scan autorisée ? (1h avant début jusqu'à la fin)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = event.getStart();
        LocalDateTime eventEnd = event.getEnd();

        if (eventStart == null || eventEnd == null) {
            // Sécurité supplémentaire, bien que @NotNull devrait l'empêcher
            throw new IllegalStateException("Dates de début ou de fin manquantes pour l'événement (ID: " + event.getId() + ").");
        }

        // Calculer le début de la fenêtre de scan
        LocalDateTime scanWindowStart = eventStart.minusHours(1);
        // La fin de la fenêtre de scan est la fin de l'événement
        LocalDateTime scanWindowEnd = eventEnd;

        // Vérifier si 'now' est EN DEHORS de la fenêtre [scanWindowStart, scanWindowEnd]
        // now < scanWindowStart  OU  now > scanWindowEnd
        if (now.isBefore(scanWindowStart) || now.isAfter(scanWindowEnd)) {
            // Lancer une exception si on est hors de la fenêtre de temps autorisée
            // Importer DateTimeFormatter pour un meilleur formatage
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm 'le' dd/MM/yyyy");
            throw new IllegalStateException("Scan non autorisé actuellement. Fenêtre de scan : de "
                    + scanWindowStart.format(formatter) + " à "
                    + scanWindowEnd.format(formatter) + "."); // -> 409
        }
        //    c) Statut actuel de la réservation ?
        if (reservation.getStatus() != ReservationStatus.CONFIRME) {
            throw new IllegalStateException("Impossible de marquer comme utilisée : la réservation n'est pas au statut CONFIRMED (statut actuel: " + reservation.getStatus() + ")."); // -> 409
        }

        // 4. Mettre à jour le statut
        reservation.setStatus(ReservationStatus.UTILISE);
        return reservationRepository.save(reservation); // Sauvegarder et retourner la réservation mise à jour
    }
}

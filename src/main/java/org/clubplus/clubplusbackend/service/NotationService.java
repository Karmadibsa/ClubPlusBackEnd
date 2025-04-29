package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.NotationDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.dto.CreateNotationDto;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Notation;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
// Retrait Optional car les méthodes spécifiques ont été enlevées

@Service
@RequiredArgsConstructor
@Transactional
public class NotationService {

    private final NotationDao notationRepository;
    private final MembreDao membreRepository; // Gardé pour récupérer le membre courant
    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final SecurityService securityService;

    /**
     * Crée une nouvelle notation pour un événement par l'utilisateur courant.
     * Valide règles métier : événement terminé, participation (statut UTILISE), notation unique.
     */
    // Adaptez le DTO si vous utilisez EventRatingPayload
    public Notation createMyNotation(CreateNotationDto notationDto, Integer eventId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));

        // --- Validation Règles Métier ---
        // 1. Événement terminé (inchangé, suppose que event.getEnd() existe)
        if (event.getEnd() == null || event.getEnd().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de noter : l'événement n'est pas terminé.");
        }
        // 2. Vérifier participation via statut UTILISE (correction de la méthode)
        if (!reservationRepository.existsByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.UTILISE)) {
            throw new IllegalStateException("Notation impossible : seule une participation validée (réservation utilisée) permet de noter cet événement.");
        }
        // 3. Déjà noté (inchangé)
        if (notationRepository.existsByEventIdAndMembreId(eventId, currentUserId)) {
            throw new IllegalStateException("Vous avez déjà noté cet événement.");
        }

        // --- Préparation & Sauvegarde (inchangé) ---
        Notation newNotation = new Notation();
        newNotation.setMembre(membre);
        newNotation.setEvent(event);
        // Copier les notes du DTO
        newNotation.setAmbiance(notationDto.getAmbiance());
        newNotation.setProprete(notationDto.getProprete());
        newNotation.setOrganisation(notationDto.getOrganisation());
        newNotation.setFairPlay(notationDto.getFairPlay());
        newNotation.setNiveauJoueurs(notationDto.getNiveauJoueurs());
        // @PrePersist gère dateNotation

        return notationRepository.save(newNotation);
    }

    /**
     * Récupère les notations (anonymisées) d'un événement.
     * Sécurité: L'utilisateur doit être membre du club organisateur.
     * Lance 404 (Event non trouvé), 403 (Non membre).
     */
    @Transactional(readOnly = true)
    public List<Notation> findNotationsByEventIdWithSecurityCheck(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId)); // -> 404
        // Sécurité Contextuelle
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId()); // -> 403
        // Le DAO retourne la liste, le champ 'membre' ne sera pas sérialisé grâce aux @JsonView
        return notationRepository.findByEventId(eventId);
    }

    /**
     * Récupère la liste des événements auxquels l'utilisateur courant a participé
     * (Réservation avec statut UTILISE) mais qu'il n'a pas encore notés.
     */
    @Transactional(readOnly = true)
    public List<Event> getUnratedParticipatedEvents() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // 1. Trouver les réservations de l'utilisateur avec le statut UTILISE
        List<Reservation> usedReservations = reservationRepository.findByMembreIdAndStatus(currentUserId, ReservationStatus.UTILISE);

        if (usedReservations.isEmpty()) {
            return List.of(); // Aucune participation validée trouvée
        }

        // 2. Extraire les IDs des événements correspondants
        Set<Integer> participatedEventIds = usedReservations.stream()
                .map(reservation -> reservation.getEvent().getId()) // Assurez-vous que getEvent() n'est pas null
                .filter(java.util.Objects::nonNull) // Filtre sécurité si getEvent peut être null
                .collect(Collectors.toSet());

        if (participatedEventIds.isEmpty()) {
            return List.of(); // Peut arriver si getEvent était null
        }

        // 3. Trouver les IDs des événements déjà notés par cet utilisateur
        Set<Integer> ratedEventIds = notationRepository.findRatedEventIdsByMembreId(currentUserId);

        // 4. Trouver les IDs des événements participés mais non notés
        participatedEventIds.removeAll(ratedEventIds);

        if (participatedEventIds.isEmpty()) {
            return List.of(); // Tous les événements participés ont été notés
        }

        // 5. Récupérer les entités Event correspondantes
        // Attention: findAllById ne garantit pas l'ordre
        return eventRepository.findAllById(participatedEventIds);
    }

}

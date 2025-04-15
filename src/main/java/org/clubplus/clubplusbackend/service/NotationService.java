package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.NotationDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Notation;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
     * Valide règles métier : événement terminé, participation, notation unique.
     * Sécurité: L'utilisateur doit être authentifié.
     * Lance 404 (Event non trouvé), 409 (Règle métier violée), 401/500 (Non auth).
     */
    public Notation createMyNotation(Notation notationInput, Integer eventId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // On ne récupère le Membre que pour l'associer, pas besoin pour la validation si on a l'ID
        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")")); // Devrait être rare
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId)); // -> 404

        // --- Validation Règles Métier ---
        if (event.getEnd() == null || event.getEnd().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de noter : l'événement n'est pas terminé."); // -> 409
        }
        if (!reservationRepository.existsByMembreIdAndEventId(currentUserId, eventId)) { // Nécessite cette méthode dans ReservationDao
            throw new IllegalStateException("Notation impossible : participation non trouvée pour cet événement."); // -> 409
        }
        if (notationRepository.existsByEventIdAndMembreId(eventId, currentUserId)) {
            throw new IllegalStateException("Vous avez déjà noté cet événement."); // -> 409
        }

        // --- Préparation & Sauvegarde ---
        Notation newNotation = new Notation();
        newNotation.setId(null);
        newNotation.setMembre(membre); // Associer le membre (invisible dans JSON)
        newNotation.setEvent(event);   // Associer l'événement
        // @PrePersist gère dateNotation

        // Copie des notes
        newNotation.setAmbiance(notationInput.getAmbiance());
        newNotation.setPropreté(notationInput.getPropreté());
        newNotation.setOrganisation(notationInput.getOrganisation());
        newNotation.setFairPlay(notationInput.getFairPlay());
        newNotation.setNiveauJoueurs(notationInput.getNiveauJoueurs());
        // Validation @Min/@Max gérée par @Valid dans le contrôleur + JPA

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
        securityService.checkIsCurrentUserMemberOfClubOrThrow(event.getOrganisateur().getId()); // -> 403
        // Le DAO retourne la liste, le champ 'membre' ne sera pas sérialisé grâce aux @JsonView
        return notationRepository.findByEventId(eventId);
    }


}

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
import org.clubplus.clubplusbackend.security.ReservationStatus;
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
    public Notation createMyNotation(CreateNotationDto notationDto, Integer eventId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId));

        // --- Validation Règles Métier (inchangées) ---
        if (event.getEnd() == null || event.getEnd().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de noter : l'événement n'est pas terminé.");
        }
        // Vérifier participation via statut UTILISE
        if (!reservationRepository.existsByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.UTILISE)) {
            throw new IllegalStateException("Notation impossible : seule une participation validée (réservation utilisée) permet de noter cet événement.");
        }
        if (notationRepository.existsByEventIdAndMembreId(eventId, currentUserId)) {
            throw new IllegalStateException("Vous avez déjà noté cet événement.");
        }

        // --- Préparation & Sauvegarde ---
        // Créer une NOUVELLE entité Notation
        Notation newNotation = new Notation();
        newNotation.setId(null); // JPA générera l'ID
        newNotation.setMembre(membre); // Associer le membre récupéré
        newNotation.setEvent(event);   // Associer l'événement récupéré
        // @PrePersist devrait gérer dateNotation

        // Copier les notes depuis le DTO validé
        newNotation.setAmbiance(notationDto.getAmbiance());
        newNotation.setProprete(notationDto.getProprete());
        newNotation.setOrganisation(notationDto.getOrganisation());
        newNotation.setFairPlay(notationDto.getFairPlay());
        newNotation.setNiveauJoueurs(notationDto.getNiveauJoueurs());

        // Sauvegarder la nouvelle entité complète
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


}

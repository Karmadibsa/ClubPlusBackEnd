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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotationService {

    private final NotationDao notationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final ReservationDao reservationRepository; // Pour vérifier la participation

    /**
     * Crée une nouvelle notation pour un événement par un membre.
     *
     * @param notation Les détails de la notation (notes). L'ID, event, membre, dateNotation seront définis ici.
     * @param eventId  L'ID de l'événement noté.
     * @param membreId L'ID du membre qui note.
     * @return La notation créée et sauvegardée.
     * @throws EntityNotFoundException Si le membre ou l'événement n'est pas trouvé.
     * @throws IllegalStateException   Si le membre n'a pas participé, si l'événement n'est pas terminé, ou si le membre a déjà noté cet événement.
     */
    public Notation createNotation(Notation notation, Integer eventId, Integer membreId) {
        // 1. Récupérer le membre et l'événement
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé avec l'ID : " + membreId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        // --- Validation Logique ---

        // 2. Vérifier si l'événement est terminé (règle métier fréquente)
        if (event.getEnd() == null || event.getEnd().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de noter un événement qui n'est pas encore terminé.");
            // Ou InvalidOperationException
        }

        // 3. Vérifier si le membre a participé à l'événement (règle métier fréquente)
        //    Si vous ne voulez pas cette vérification, commentez/supprimez cette partie.
        boolean hasParticipated = reservationRepository.findByMembreIdAndEventId(membreId, eventId).isPresent();
        if (!hasParticipated) {
            throw new IllegalStateException("Le membre ID " + membreId + " ne semble pas avoir participé à l'événement ID " + eventId + " et ne peut donc pas le noter.");
            // Ou InvalidOperationException
        }

        // 4. Vérifier si le membre a déjà noté cet événement (contrainte unique)
        if (notationRepository.existsByEventIdAndMembreId(eventId, membreId)) {
            throw new IllegalStateException("Le membre ID " + membreId + " a déjà noté l'événement ID " + eventId);
            // Ou AlreadyExistsException
        }

        // --- Préparation et Sauvegarde ---
        // Assurer que l'ID est null pour la création
        notation.setId(null);
        // Lier le membre et l'événement
        notation.setMembre(membre);
        notation.setEvent(event);
        // La date est gérée par @PrePersist dans l'entité

        // Les notes (ambiance, propreté, etc.) viennent de l'objet 'notation' reçu.
        // La validation @Min/@Max sur l'entité sera déclenchée par la sauvegarde si @Valid est utilisé dans le contrôleur.

        return notationRepository.save(notation);
    }

    public List<Notation> getNotationsByEventId(Integer eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId);
        }
        return notationRepository.findByEventId(eventId);
    }

    public List<Notation> getNotationsByMembreId(Integer membreId) {
        if (!membreRepository.existsById(membreId)) {
            throw new EntityNotFoundException("Membre non trouvé avec l'ID : " + membreId);
        }
        return notationRepository.findByMembreId(membreId);
    }

    public Optional<Notation> getNotationByEventAndMembre(Integer eventId, Integer membreId) {
        // Pas besoin de vérifier l'existence de l'event/membre ici, car si la notation existe, ils existent.
        return notationRepository.findByEventIdAndMembreId(eventId, membreId);
    }

    public Notation getNotationByEventAndMembreOrThrow(Integer eventId, Integer membreId) {
        return notationRepository.findByEventIdAndMembreId(eventId, membreId)
                .orElseThrow(() -> new EntityNotFoundException("Aucune notation trouvée pour l'événement ID " + eventId + " par le membre ID " + membreId));
    }


    /**
     * Supprime une notation par son ID.
     * (Généralement réservé aux administrateurs).
     *
     * @param notationId L'ID de la notation à supprimer.
     * @throws EntityNotFoundException Si la notation n'est pas trouvée.
     */
    public void deleteNotation(Integer notationId) {
        if (!notationRepository.existsById(notationId)) {
            throw new EntityNotFoundException("Notation non trouvée avec l'ID : " + notationId);
        }
        notationRepository.deleteById(notationId);
    }

    // --- Méthodes pour les moyennes (Exemple) ---
    // public AverageRatingsDTO getAverageRatingsForEvent(Integer eventId) {
    //     if (!eventRepository.existsById(eventId)) {
    //         throw new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId);
    //     }
    //     // Appeler la méthode du repository qui retourne le DTO des moyennes
    //     return notationRepository.findAverageRatingsByEventId(eventId)
    //             .orElse(new AverageRatingsDTO()); // Retourner un DTO vide si pas de notations
    // }

}

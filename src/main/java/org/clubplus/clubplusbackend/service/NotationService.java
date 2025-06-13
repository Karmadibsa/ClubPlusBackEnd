package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.NotationDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.dto.CreateNotationDto;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier liée aux notations (évaluations) des événements.
 * <p>
 * Ce service assure les opérations de création et de consultation des notations,
 * en appliquant les règles métier (ex: l'événement doit être terminé) et de sécurité.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotationService {

    private final NotationDao notationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final SecurityService securityService;

    /**
     * Crée une nouvelle notation pour un événement, soumise par l'utilisateur courant.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'événement doit être terminé.</li>
     * <li>L'utilisateur doit avoir une réservation 'UTILISE' pour cet événement.</li>
     * <li>L'utilisateur ne doit pas avoir déjà noté cet événement.</li>
     * </ul>
     *
     * @param notationDto DTO contenant les notes.
     * @param eventId     L'ID de l'événement à noter.
     * @return La nouvelle notation créée.
     * @throws EntityNotFoundException si l'utilisateur ou l'événement n'est pas trouvé.
     * @throws IllegalStateException   si une des règles métier n'est pas respectée.
     */
    public Notation createMyNotation(CreateNotationDto notationDto, Integer eventId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));

        if (event.getEndTime().isAfter(Instant.now())) {
            throw new IllegalStateException("Impossible de noter : l'événement (ID: " + eventId + ") n'est pas encore terminé.");
        }

        boolean participated = reservationRepository.existsByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.UTILISE);
        if (!participated) {
            throw new IllegalStateException("Notation impossible : participation non validée pour l'événement (ID: " + eventId + ").");
        }

        boolean alreadyRated = notationRepository.existsByEventIdAndMembreId(eventId, currentUserId);
        if (alreadyRated) {
            throw new IllegalStateException("Vous avez déjà noté cet événement (ID: " + eventId + ").");
        }

        Notation newNotation = new Notation();
        newNotation.setMembre(membre);
        newNotation.setEvent(event);
        newNotation.setAmbiance(notationDto.getAmbiance());
        newNotation.setProprete(notationDto.getProprete());
        newNotation.setOrganisation(notationDto.getOrganisation());
        newNotation.setFairPlay(notationDto.getFairPlay());
        newNotation.setNiveauJoueurs(notationDto.getNiveauJoueurs());

        return notationRepository.save(newNotation);
    }

    /**
     * Récupère les notations pour un événement spécifique.
     * <p>
     * <b>Sécurité :</b> Seul un gestionnaire (ADMIN ou RESERVATION) du club organisateur peut consulter ces notations.
     *
     * @param eventId L'ID de l'événement.
     * @return Une liste de notations. Les informations de l'auteur sont généralement masquées par la vue JSON.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas un gestionnaire du club.
     */
    @Transactional(readOnly = true)
    public List<Notation> findNotationsByEventIdWithSecurityCheck(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));

        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        return notationRepository.findByEventId(eventId);
    }

    /**
     * Récupère les événements auxquels l'utilisateur a participé (réservation 'UTILISE') mais qu'il n'a pas encore notés.
     *
     * @return Une liste d'événements à noter.
     */
    @Transactional(readOnly = true)
    public List<Event> getUnratedParticipatedEvents() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        List<Reservation> usedReservations = reservationRepository.findByMembreIdAndStatus(currentUserId, ReservationStatus.UTILISE);
        if (usedReservations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> participatedEventIds = usedReservations.stream()
                .map(Reservation::getEvent)
                .filter(Objects::nonNull)
                .map(Event::getId)
                .collect(Collectors.toSet());
        if (participatedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> ratedEventIds = notationRepository.findRatedEventIdsByMembreId(currentUserId);

        participatedEventIds.removeAll(ratedEventIds);
        if (participatedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        return eventRepository.findAllById(participatedEventIds);
    }
}

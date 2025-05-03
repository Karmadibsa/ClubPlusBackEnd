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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier liée aux notations (évaluations) des événements.
 * Fournit des opérations pour créer et consulter des notations, et pour identifier
 * les événements que l'utilisateur peut noter, en intégrant des vérifications de sécurité
 * et des règles métier spécifiques.
 */
@Service
@RequiredArgsConstructor
@Transactional // Applique la transactionnalité par défaut à toutes les méthodes publiques
public class NotationService {

    // Dépendances injectées via Lombok @RequiredArgsConstructor
    private final NotationDao notationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final SecurityService securityService;

    /**
     * Crée une nouvelle notation pour un événement spécifique, soumise par l'utilisateur actuellement authentifié.
     * Applique plusieurs règles métier avant de permettre la création :
     * 1. L'événement doit être terminé (sa date/heure de fin est dans le passé).
     * 2. L'utilisateur doit y avoir participé (confirmé par une réservation au statut 'UTILISE').
     * 3. L'utilisateur ne doit pas avoir déjà soumis une notation pour cet événement.
     *
     * @param notationDto DTO contenant les différentes notes (ambiance, propreté, organisation, fair-play, niveau) attribuées par l'utilisateur.
     * @param eventId     L'identifiant unique de l'événement à noter.
     * @return L'entité {@link Notation} nouvellement créée et persistée.
     * @throws EntityNotFoundException si l'utilisateur courant ou l'événement spécifié n'est pas trouvé (-> HTTP 404).
     * @throws IllegalStateException   si l'événement n'est pas encore terminé, si l'utilisateur n'a pas de réservation confirmée comme 'UTILISE' pour cet événement,
     *                                 ou s'il a déjà soumis une notation pour cet événement (-> HTTP 409 Conflict).
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    public Notation createMyNotation(CreateNotationDto notationDto, Integer eventId) {
        // Récupère l'ID de l'utilisateur courant (lance une exception si non authentifié)
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // Récupère les entités Membre et Event (lancent EntityNotFoundException si non trouvées)
        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));

        // --- Validation des Règles Métier ---

        // 1. L'événement doit être terminé
        LocalDateTime now = LocalDateTime.now();
        if (event.getEnd() == null || event.getEnd().isAfter(now)) {
            throw new IllegalStateException("Impossible de noter : l'événement (ID: " + eventId + ") n'est pas encore terminé."); // -> 409
        }

        // 2. Vérifier la participation via une réservation au statut 'UTILISE'
        // Utilise une méthode DAO optimisée pour vérifier l'existence
        boolean participated = reservationRepository.existsByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.UTILISE);
        if (!participated) {
            throw new IllegalStateException("Notation impossible : participation non validée (réservation non marquée comme 'UTILISE') pour l'événement (ID: " + eventId + ")."); // -> 409
        }

        // 3. Vérifier si l'événement a déjà été noté par cet utilisateur
        // Utilise une méthode DAO optimisée pour vérifier l'existence
        boolean alreadyRated = notationRepository.existsByEventIdAndMembreId(eventId, currentUserId);
        if (alreadyRated) {
            throw new IllegalStateException("Vous avez déjà noté cet événement (ID: " + eventId + ")."); // -> 409
        }

        // --- Préparation et Sauvegarde de la Notation ---
        Notation newNotation = new Notation();
        newNotation.setMembre(membre); // Associe le membre courant
        newNotation.setEvent(event);   // Associe l'événement noté

        // Copie les notes depuis le DTO vers l'entité
        newNotation.setAmbiance(notationDto.getAmbiance());
        newNotation.setProprete(notationDto.getProprete());
        newNotation.setOrganisation(notationDto.getOrganisation());
        newNotation.setFairPlay(notationDto.getFairPlay());
        newNotation.setNiveauJoueurs(notationDto.getNiveauJoueurs());

        // La date de notation (dateNotation) est gérée automatiquement par @PrePersist sur l'entité Notation

        // Sauvegarde la nouvelle notation en base de données
        return notationRepository.save(newNotation);
    }

    /**
     * Récupère la liste de toutes les notations soumises pour un événement spécifique.
     * L'accès à cette information est sécurisé : seul un gestionnaire (ADMIN ou RESERVATION)
     * du club organisateur de l'événement peut consulter ces notations.
     * Les détails de l'utilisateur ayant soumis la note sont généralement masqués lors de la réponse
     * (anonymisation via des configurations comme @JsonView ou DTO spécifique dans le contrôleur).
     *
     * @param eventId L'identifiant unique de l'événement dont les notations sont demandées.
     * @return Une liste d'entités {@link Notation} associées à l'événement. Peut être vide.
     * Les informations sur le membre ayant soumis la notation sont typiquement exclues
     * de la sérialisation (anonymisation).
     * @throws EntityNotFoundException si l'événement avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou une exception de sécurité similaire lancée par SecurityService)
     *                                 si l'utilisateur courant n'est pas gestionnaire (ADMIN ou RESERVATION)
     *                                 du club organisateur de l'événement (-> HTTP 403 Forbidden).
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true) // Opération de lecture seule
    public List<Notation> findNotationsByEventIdWithSecurityCheck(Integer eventId) {
        // 1. Récupérer l'événement (lance 404 si non trouvé)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")")); // -> 404

        // 2. Vérification de Sécurité Contextuelle : Est-ce que l'utilisateur courant est manager du club organisateur ?
        //    Cette méthode lance une exception (ex: AccessDeniedException) si l'utilisateur n'a pas les droits.
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId()); // -> 403

        // 3. Si la sécurité est passée, récupérer les notations pour cet événement
        //    Le DAO retourne la liste. L'anonymisation (masquage du champ 'membre') est
        //    généralement gérée au niveau de la sérialisation JSON (ex: @JsonView sur l'entité ou DTO).
        return notationRepository.findByEventId(eventId);
    }

    /**
     * Récupère la liste des événements auxquels l'utilisateur actuellement authentifié a participé
     * (c'est-à-dire pour lesquels il existe une réservation avec le statut 'UTILISE')
     * mais qu'il n'a pas encore notés. Cette liste peut être utilisée pour inviter l'utilisateur
     * à fournir une évaluation pour ces événements passés.
     *
     * @return Une liste d'entités {@link Event} pour lesquels l'utilisateur a une réservation marquée
     * comme 'UTILISE' mais n'a pas encore soumis de notation. Peut être vide si aucune
     * participation n'a eu lieu, si toutes les participations ont déjà été notées,
     * ou si les événements correspondants ne sont pas trouvés.
     * @throws EntityNotFoundException si l'utilisateur courant n'est pas trouvé (peu probable si authentifié).
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true) // Opération de lecture seule
    public List<Event> getUnratedParticipatedEvents() {
        // Récupère l'ID de l'utilisateur courant (lance une exception si non authentifié)
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // 1. Trouver toutes les réservations de l'utilisateur avec le statut UTILISE
        List<Reservation> usedReservations = reservationRepository.findByMembreIdAndStatus(currentUserId, ReservationStatus.UTILISE);

        // Si aucune participation validée n'est trouvée, retourner une liste vide immédiatement
        if (usedReservations.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extraire les IDs uniques des événements correspondants à ces réservations
        Set<Integer> participatedEventIds = usedReservations.stream()
                .map(Reservation::getEvent) // Récupère l'objet Event lié
                .filter(Objects::nonNull)    // Sécurité : s'assure que l'événement lié existe
                .map(Event::getId)           // Extrait l'ID de l'événement
                .collect(Collectors.toSet()); // Collecte dans un Set pour unicité

        // Si, après filtrage, aucun ID d'événement valide n'est trouvé (cas improbable)
        if (participatedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Trouver les IDs des événements que cet utilisateur a déjà notés
        //    Utilise une méthode DAO optimisée retournant directement un Set d'IDs
        Set<Integer> ratedEventIds = notationRepository.findRatedEventIdsByMembreId(currentUserId);

        // 4. Calculer l'ensemble des IDs d'événements participés mais NON encore notés
        //    Modifie l'ensemble participatedEventIds en retirant les éléments présents dans ratedEventIds
        participatedEventIds.removeAll(ratedEventIds);

        // Si tous les événements participés ont déjà été notés
        if (participatedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. Récupérer les entités Event complètes pour les IDs restants
        //    findAllById retourne une liste d'événements correspondant aux IDs fournis.
        //    L'ordre n'est pas garanti par défaut. Si un ordre est nécessaire, il faudrait l'ajouter ici ou dans le DAO.
        return eventRepository.findAllById(participatedEventIds);
    }

}

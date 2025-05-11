package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service gérant la logique métier liée aux réservations d'événements.
 * Fournit des opérations CRUD (limitées ici : création, lecture, annulation, marquage comme utilisé),
 * applique les règles de gestion (limites de réservation, capacité, statuts, timing),
 * et intègre des vérifications de sécurité contextuelles via {@link SecurityService}.
 */
@Service
@RequiredArgsConstructor
@Transactional // Applique la transactionnalité par défaut à toutes les méthodes publiques
public class ReservationService {

    // Dépendances injectées via Lombok @RequiredArgsConstructor
    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final CategorieDao categorieRepository;
    private final SecurityService securityService;

    // Constante pour la limite de réservations par membre et par événement (peut être externalisée)
    // Documentée comme étant 2 dans le cahier des charges [1].
    private static final int RESERVATION_MAX_PER_EVENT_PER_MEMBER = 2;

    /**
     * Crée une nouvelle réservation pour l'utilisateur actuellement authentifié pour une catégorie spécifique d'un événement.
     * Vérifie de nombreuses règles métier avant de procéder :
     * <ul>
     *     <li>L'utilisateur et la catégorie doivent exister.</li>
     *     <li>L'événement lié à la catégorie doit exister, être actif et ne pas être déjà commencé ou passé.</li>
     *     <li>La catégorie doit appartenir à l'événement spécifié par {@code eventId}.</li>
     *     <li>L'utilisateur doit être membre du club organisateur de l'événement [1].</li>
     *     <li>L'utilisateur ne doit pas dépasser la limite de réservations confirmées pour cet événement ({@value #RESERVATION_MAX_PER_EVENT_PER_MEMBER}) [1].</li>
     *     <li>La catégorie doit avoir des places disponibles (capacité non atteinte par les réservations confirmées).</li>
     * </ul>
     *
     * @param eventId     L'identifiant de l'événement concerné.
     * @param categorieId L'identifiant de la catégorie spécifique pour laquelle réserver.
     * @return L'entité {@link Reservation} nouvellement créée et persistée, avec le statut {@link ReservationStatus#CONFIRME}.
     * @throws EntityNotFoundException  si le membre courant, la catégorie, ou l'événement associé n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException    (ou similaire via {@code SecurityService}) si l'utilisateur courant n'est pas membre du club organisateur (-> HTTP 403).
     * @throws IllegalStateException    si l'événement est annulé, déjà passé/commencé, si la limite de réservation par membre est atteinte,
     *                                  ou si la capacité de la catégorie est pleine (-> HTTP 409 Conflict).
     * @throws IllegalArgumentException si la catégorie spécifiée n'appartient pas à l'événement spécifié (-> HTTP 400 Bad Request).
     * @throws SecurityException        (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional
    public Reservation createMyReservation(Integer eventId, Integer categorieId) {
        // Récupère l'ID de l'utilisateur courant (lance une exception si non authentifié)
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // Récupère le membre courant (lance 404 si non trouvé)
        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre courant non trouvé (ID: " + currentUserId + ")"));
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId + ")"));

        // Récupère l'événement via la catégorie chargée
        Event event = categorie.getEvent();

        // --- VALIDATIONS ---

        // 0. Sécurité : Vérifier que l'utilisateur est membre du club organisateur de l'événement
        securityService.checkMemberOfEventClubOrThrow(eventId); // -> 403 si non membre

        // 1. Événement existe (implicite via catégorie) et est ACTIF
        if (event == null) {
            throw new EntityNotFoundException("Événement lié à la catégorie (ID: " + categorieId + ") non trouvé."); // -> 404 (ou 500 pour incohérence)
        }
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de réserver : l'événement (ID: " + eventId + ") est annulé."); // -> 409
        }

        // 2. Cohérence catégorie/événement
        if (!Objects.equals(event.getId(), eventId)) {
            throw new IllegalArgumentException("La catégorie (ID " + categorieId + ") n'appartient pas à l'événement (ID " + eventId + ")."); // -> 400
        }

        // 3. L'événement est-il futur ?
        LocalDateTime now = LocalDateTime.now();
        if (event.getStart() == null || event.getStart().isBefore(now)) {
            throw new IllegalStateException("Impossible de réserver : l'événement (ID: " + eventId + ") est déjà commencé ou passé."); // -> 409
        }

        // 4. Limite de réservations par membre pour cet événement [1]
        long existingReservationsCount = reservationRepository.countByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.CONFIRME);
        if (existingReservationsCount >= RESERVATION_MAX_PER_EVENT_PER_MEMBER) {
            throw new IllegalStateException("Limite de " + RESERVATION_MAX_PER_EVENT_PER_MEMBER + " réservations confirmées par membre atteinte pour cet événement (ID: " + eventId + ")."); // -> 409
        }

        // 5. Capacité de la catégorie (basée sur les places CONFIRMEES)
        if (categorie.getPlaceDisponible() <= 0) { // Vérifie s'il reste au moins une place disponible
            throw new IllegalStateException("Capacité maximale (confirmée) atteinte pour la catégorie '" + categorie.getNom() + "' (ID: " + categorieId + ")."); // -> 409
        }

        Reservation newReservation = new Reservation(membre, event, categorie);

        // Sauvegarde la nouvelle réservation en base de données
        return reservationRepository.save(newReservation);
    }

    // --- Méthodes de Lecture (avec Sécurité) ---

    /**
     * Récupère une réservation spécifique par son identifiant unique (ID entier).
     * L'accès est sécurisé : l'utilisateur doit être soit le propriétaire de la réservation,
     * soit un gestionnaire (rôle RESERVATION ou ADMIN [1]) du club qui organise l'événement associé.
     *
     * @param reservationId L'identifiant unique (entier) de la réservation à récupérer.
     * @return L'entité {@link Reservation} correspondante si elle est trouvée et si l'accès est autorisé.
     * @throws EntityNotFoundException si aucune réservation n'est trouvée avec cet ID (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur courant n'est ni le propriétaire
     *                                 ni un gestionnaire autorisé du club organisateur (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true) // Opération de lecture seule
    public Reservation getReservationByIdWithSecurityCheck(Integer reservationId) {
        // Récupère la réservation (lance 404 si non trouvée)
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")")); // -> 404

        // Vérification de Sécurité Contextuelle : propriétaire OU manager du club organisateur ?
        // Cette méthode dans SecurityService doit encapsuler la logique de vérification.
        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation); // -> 403 si accès refusé

        // Si la sécurité passe, retourne la réservation
        return reservation;
    }

    /**
     * Récupère la liste des réservations effectuées par l'utilisateur actuellement authentifié.
     * Permet de filtrer les réservations par leur statut (ex: "CONFIRME", "UTILISE", "ANNULE", insensible à la casse).
     * Si aucun filtre valide n'est fourni (`null`, "all", ou chaîne invalide), toutes les réservations
     * de l'utilisateur (quel que soit leur statut) sont retournées.
     *
     * @param statusFilter Une chaîne représentant le statut désiré ("CONFIRME", "UTILISE", "ANNULE", insensible à la casse),
     *                     ou `null` ou "all" pour ne pas filtrer par statut.
     * @return Une liste d'entités {@link Reservation} appartenant à l'utilisateur courant, filtrée selon le statut demandé.
     * Retourne une liste vide si l'utilisateur n'a aucune réservation ou si le filtre de statut est invalide.
     * @throws SecurityException (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true) // Opération de lecture seule
    public List<Reservation> findMyReservations(String statusFilter) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Récupère ID (lance exception si non auth)

        ReservationStatus status = null; // Par défaut, pas de filtre de statut
        try {
            // Si un filtre est fourni et n'est pas "all" (ignoré), essaie de le convertir en Enum
            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                // Convertit la chaîne (ex: "confirmed") en valeur Enum. Le toUpperCase() gère la casse.
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            // Si statusFilter est une chaîne qui ne correspond à aucun nom d'Enum ReservationStatus
            // Log l'erreur pour information (remplacer par un vrai logger en production)
            System.err.println("Statut de réservation invalide fourni pour findMyReservations: '" + statusFilter + "'. Retourne une liste vide.");
            return Collections.emptyList(); // Retourne une liste vide en cas de statut invalide
            // Alternative : Lancer une BadRequestException pour signaler une mauvaise requête au contrôleur
            // throw new BadRequestException("Statut de réservation invalide: " + statusFilter); // -> 400
        }

        // Appelle la méthode du repository appropriée en fonction du statut (filtré ou non)
        if (status != null) {
            // Si un statut valide a été trouvé, filtre par ce statut
            return reservationRepository.findByMembreIdAndStatus(currentUserId, status);
        } else {
            // Sinon (statusFilter était null, "all", ou invalide et géré par le catch), retourne toutes les réservations du membre
            return reservationRepository.findByMembreId(currentUserId);
        }
    }

    /**
     * Récupère la liste des réservations pour un événement spécifique.
     * L'accès est sécurisé : seul un gestionnaire (rôle RESERVATION ou ADMIN [1]) du club organisateur peut voir ces informations.
     * Permet de filtrer les réservations par leur statut (ex: "CONFIRME", "UTILISE", "ANNULE", insensible à la casse).
     * Si aucun filtre valide n'est fourni (`null`, "all", ou chaîne invalide), toutes les réservations
     * de l'événement (quel que soit leur statut) sont retournées.
     *
     * @param eventId      L'identifiant unique de l'événement concerné.
     * @param statusFilter Une chaîne représentant le statut désiré ("CONFIRME", "UTILISE", "ANNULE", insensible à la casse),
     *                     ou `null` ou "all" pour ne pas filtrer par statut.
     * @return Une liste d'entités {@link Reservation} pour l'événement spécifié, filtrée selon le statut demandé.
     * Retourne une liste vide si l'événement n'a aucune réservation ou si le filtre de statut est invalide.
     * @throws EntityNotFoundException si l'événement avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur courant n'est pas gestionnaire autorisé du club organisateur (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true) // Opération de lecture seule
    public List<Reservation> findReservationsByEventIdWithSecurityCheck(Integer eventId, String statusFilter) {
        // 1. Vérifier l'existence de l'événement
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")")); // -> 404

        // 2. Vérifier la Sécurité : l'utilisateur est-il manager du club organisateur ?
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId()); // -> 403 si non autorisé

        // 3. Appliquer le filtre de statut (logique identique à findMyReservations)
        ReservationStatus status = null; // Par défaut, pas de filtre
        try {
            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Statut de réservation invalide fourni pour findReservationsByEventId: '" + statusFilter + "'. Retourne une liste vide.");
            return Collections.emptyList(); // Ou lancer BadRequestException -> 400
        }

        // 4. Appeler le DAO approprié
        if (status != null) {
            return reservationRepository.findByEventIdAndStatus(eventId, status);
        } else {
            return reservationRepository.findByEventId(eventId); // Récupère tout pour cet événement
        }
    }

    /**
     * Récupère la liste des réservations pour une catégorie d'événement spécifique.
     * L'accès est sécurisé : seul un gestionnaire (rôle RESERVATION ou ADMIN [1]) du club organisateur
     * (via l'événement lié à la catégorie) peut voir ces informations.
     *
     * @param categorieId L'identifiant unique de la catégorie concernée.
     * @return Une liste d'entités {@link Reservation} pour la catégorie spécifiée. Peut être vide.
     * @throws EntityNotFoundException si la catégorie avec l'ID spécifié n'est pas trouvée (-> HTTP 404),
     *                                 ou si les relations catégorie-événement-organisateur sont incohérentes.
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur courant n'est pas gestionnaire autorisé du club organisateur (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true) // Opération de lecture seule
    public List<Reservation> findReservationsByCategorieIdWithSecurityCheck(Integer categorieId) {
        // 1. Récupérer la catégorie (et implicitement l'événement lié si bien mappé/chargé)
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId + ")")); // -> 404

        // 2. Vérifier la Sécurité Contextuelle : l'utilisateur est-il manager du club de l'événement ?
        //    Nécessite d'accéder à l'événement via la catégorie.
        if (categorie.getEvent() == null || categorie.getEvent().getOrganisateur() == null) {
            // Gérer le cas où les relations sont nulles (problème de données ou de chargement)
            throw new EntityNotFoundException("Impossible de vérifier les droits : événement ou organisateur non trouvé pour la catégorie (ID: " + categorieId + ")"); // -> 404 ou 500
        }
        securityService.checkManagerOfClubOrThrow(categorie.getEvent().getOrganisateur().getId()); // -> 403 si non autorisé

        // 3. Si la sécurité passe, récupérer les réservations pour cette catégorie
        return reservationRepository.findByCategorieId(categorieId);
    }


    // --- Méthodes de Modification de Statut ---

    /**
     * Annule une réservation spécifique en changeant son statut à {@link ReservationStatus#ANNULE}.
     * L'action est soumise à des conditions de sécurité et des règles métier :
     * <ul>
     *     <li>L'utilisateur doit être soit le propriétaire de la réservation, soit un gestionnaire (RESERVATION ou ADMIN [1]) du club organisateur.</li>
     *     <li>L'événement associé ne doit pas être déjà commencé ou passé.</li>
     *     <li>La réservation doit actuellement avoir le statut {@link ReservationStatus#CONFIRME}.</li>
     * </ul>
     *
     * @param reservationId L'identifiant unique (entier) de la réservation à annuler.
     * @throws EntityNotFoundException si aucune réservation n'est trouvée avec cet ID (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur courant n'est ni le propriétaire
     *                                 ni un gestionnaire autorisé du club organisateur (-> HTTP 403).
     * @throws IllegalStateException   si l'événement est déjà commencé/passé, ou si la réservation
     *                                 n'est pas au statut CONFIRME (-> HTTP 409 Conflict).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public void cancelReservationById(Integer reservationId) {
        // 1. Récupérer la réservation (quel que soit son statut initial)
        //    Il est bon de charger l'événement lié en même temps si la relation est LAZY.
        //    Supposons une méthode DAO avec JOIN FETCH ou que la relation est EAGER.
        Reservation reservation = reservationRepository.findById(reservationId) // Simplifié, adaptez si besoin de fetch
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")")); // -> 404

        // 2. Vérification de Sécurité Contextuelle : propriétaire OU manager ?
        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation); // -> 403 si non autorisé

        // 3. Vérification métier : L'événement est-il futur ?
        Event event = reservation.getEvent(); // Doit être chargé
        LocalDateTime now = LocalDateTime.now();
        if (event == null || event.getStart() == null || event.getStart().isBefore(now)) {
            throw new IllegalStateException("Annulation impossible : l'événement (ID: " + (event != null ? event.getId() : "inconnu") + ") est déjà commencé ou passé."); // -> 409
        }

        // 4. Vérification métier : Le statut actuel de la réservation est-il CONFIRME ?
        if (reservation.getStatus() != ReservationStatus.CONFIRME) {
            throw new IllegalStateException("Annulation impossible : la réservation (ID: " + reservationId + ") n'est pas au statut CONFIRME (statut actuel: " + reservation.getStatus() + ")."); // -> 409
        }

        // 5. Mettre à jour le statut de la réservation à ANNULE
        reservation.setStatus(ReservationStatus.ANNULE);
        reservationRepository.save(reservation); // Persiste le changement de statut
    }

    /**
     * Marque une réservation comme étant {@link ReservationStatus#UTILISE}, typiquement après validation
     * par un membre du staff (par exemple, scan d'un QR code associé à l'UUID de la réservation) [1].
     * L'action est soumise à des conditions de sécurité et des règles métier :
     * <ul>
     *     <li>L'utilisateur doit avoir un rôle l'autorisant à valider les présences pour cet événement/club (ex: RESERVATION ou ADMIN [1]).</li>
     *     <li>L'événement associé doit être actif.</li>
     *     <li>L'action doit avoir lieu pendant la fenêtre de temps autorisée pour la validation (implémentée ici : 1 heure avant le début jusqu'à la fin de l'événement).</li>
     *     <li>La réservation doit actuellement avoir le statut {@link ReservationStatus#CONFIRME}.</li>
     * </ul>
     *
     * @param reservationUuid L'identifiant UUID unique (chaîne de caractères) de la réservation à marquer comme utilisée.
     * @return L'entité {@link Reservation} mise à jour avec le statut {@link ReservationStatus#UTILISE}.
     * @throws EntityNotFoundException si aucune réservation n'est trouvée avec cet UUID (-> HTTP 404), ou si les relations sont incohérentes.
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur courant n'a pas les droits de validation
     *                                 (ex: n'est pas RESERVATION ou ADMIN) pour cet événement/club (-> HTTP 403).
     * @throws IllegalStateException   si l'événement est annulé, si l'action est tentée en dehors de la fenêtre de validation autorisée,
     *                                 ou si la réservation n'est pas au statut CONFIRME (-> HTTP 409 Conflict).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public Reservation markReservationAsUsed(String reservationUuid) {
        // 1. Récupérer la réservation par son UUID unique
        //    Il est essentiel de charger l'événement lié en même temps pour les validations suivantes.
        //    Supposons une méthode DAO spécifique ou que la relation est EAGER.
        Reservation reservation = reservationRepository.findByReservationUuid(reservationUuid) // Adaptez si besoin de fetch
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée avec l'UUID : " + reservationUuid)); // -> 404

        Event event = reservation.getEvent(); // Doit être chargé

        // 2. Vérification de Sécurité Contextuelle : L'utilisateur peut-il valider/scanner pour cet événement ?
        //    Ceci revient à vérifier si l'utilisateur est manager (RESERVATION ou ADMIN) du club organisateur.
        if (event == null || event.getOrganisateur() == null) {
            throw new EntityNotFoundException("Impossible de vérifier les droits : événement ou organisateur non trouvé pour la réservation (UUID: " + reservationUuid + ")"); // -> 404 ou 500
        }
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId()); // -> 403 si non autorisé

        // 3. Vérifications Métier :
        //    a) L'événement existe-t-il et est-il actif ?
        //       (La vérification de nullité de 'event' est déjà faite ci-dessus).
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de marquer comme utilisée : l'événement lié (ID: " + event.getId() + ") est annulé."); // -> 409
        }

        //    b) Est-on dans la fenêtre de scan/validation autorisée ? (Ex: 1h avant début jusqu'à la fin)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = event.getStart();
        LocalDateTime eventEnd = event.getEnd();

        if (eventStart == null || eventEnd == null) {
            // Sécurité des données, même si @NotNull devrait l'empêcher
            throw new IllegalStateException("Dates de début ou de fin manquantes pour l'événement (ID: " + event.getId() + "). Impossible de valider la fenêtre de scan."); // -> 500 ou 409
        }

        // Définir la fenêtre de validation
        LocalDateTime scanWindowStart = eventStart.minusHours(1); // 1 heure avant le début
        LocalDateTime scanWindowEnd = eventEnd;                 // Jusqu'à la fin

        // Vérifier si 'now' est EN DEHORS de la fenêtre [scanWindowStart, scanWindowEnd]
        if (now.isBefore(scanWindowStart) || now.isAfter(scanWindowEnd)) {
            // Formatter les dates pour un message d'erreur clair
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'le' dd/MM/yyyy");
            throw new IllegalStateException("Validation non autorisée actuellement. Fenêtre de validation : de "
                    + scanWindowStart.format(formatter) + " à "
                    + scanWindowEnd.format(formatter) + "."); // -> 409
        }

        //    c) Le statut actuel de la réservation est-il CONFIRME ?
        if (reservation.getStatus() != ReservationStatus.CONFIRME) {
            throw new IllegalStateException("Impossible de marquer comme utilisée : la réservation (UUID: " + reservationUuid + ") n'est pas au statut CONFIRME (statut actuel: " + reservation.getStatus() + ")."); // -> 409
        }

        // 4. Mettre à jour le statut de la réservation à UTILISE
        reservation.setStatus(ReservationStatus.UTILISE);

        // 5. Sauvegarder et retourner la réservation mise à jour
        return reservationRepository.save(reservation);
    }
}

package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateEventWithCategoriesDto;
import org.clubplus.clubplusbackend.dto.EventWithFriendsDto;
import org.clubplus.clubplusbackend.dto.UpdateEventWithCategoriesDto;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les opérations liées aux entités {@link Event}.
 * Fournit des endpoints pour la création, la lecture (détaillée, listes filtrées),
 * la mise à jour et la suppression (désactivation) d'événements.
 * Gère également la récupération d'événements basée sur le contexte de l'utilisateur (ses clubs, ses amis).
 * La sécurité est appliquée via des annotations personnalisées et des vérifications
 * contextuelles dans le service {@link EventService}.
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /events} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see Event
 * @see EventService
 */
@RestController
@RequestMapping("/events") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor
@CrossOrigin // Activation de CORS si nécessaire
public class EventController {

    /**
     * Service contenant la logique métier pour la gestion des événements.
     *
     * @see EventService
     */
    private final EventService eventService;

    /**
     * Récupère la liste des événements *futurs* des clubs auxquels l'utilisateur actuellement authentifié est membre.
     * Permet de filtrer les résultats par statut ('active', 'inactive', 'all').
     * <p>
     * <b>Requête:</b> GET /events?status={status}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * L'identifiant de l'utilisateur est utilisé par le service pour déterminer les clubs pertinents.
     * </p>
     *
     * @param status (Optionnel) Filtre (String) sur le statut des événements futurs ('active', 'inactive', 'all').
     *               Si non fourni ou invalide, le service peut avoir un comportement par défaut (ex: 'active' ou 'all').
     *               Voir {@link EventService#findAllEventsForMemberClubs}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Event>} des événements futurs correspondants,
     *         sérialisée selon {@link GlobalView.EventView}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou si son identité/rôle ne peut être déterminé
     *         (levé par le service via {@link SecurityException} ou similaire).</li>
     * </ul>
     * @see EventService#findAllEventsForMemberClubs(String)
     * @see GlobalView.EventView
     * @see IsReservation
     */
    @GetMapping
    @IsReservation // Requiert rôle RESERVATION ou ADMIN pour accéder à la liste des événements de ses clubs
    @JsonView(GlobalView.EventView.class) // Vue JSON pour la liste d'événements
    public ResponseEntity<List<Event>> getAllEventsForMyClubs(@RequestParam(required = false) String status) {
        // Le service utilise l'identité de l'utilisateur connecté pour filtrer les clubs
        List<Event> events = eventService.findAllEventsForMemberClubs(status);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupère la liste des événements *futurs* des clubs auxquels l'utilisateur est membre,
     * sous forme de DTO incluant la liste des amis participants (si le filtre `withFriends` est activé).
     * Permet de filtrer par statut et par présence d'amis.
     * <p>
     * <b>Requête:</b> GET /events/withfriend?status={status}&withFriends={withFriends}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert le rôle MEMBRE (ou supérieur) ({@link IsMembre @IsMembre}).
     * </p>
     *
     * @param status      (Optionnel) Filtre (String) sur le statut des événements futurs ('active', 'inactive', 'all').
     *                    Voir {@link EventService#findMemberEventsFiltered}.
     * @param withFriends (Optionnel) Booléen indiquant s'il faut filtrer pour ne garder que les événements
     *                    auxquels au moins un ami participe. Défaut à `false`.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<EventWithFriendsDto>} des événements correspondants. Peut être vide.
     *         La vue JSON {@link GlobalView.EventView} s'applique ici aussi (si les champs du DTO sont compatibles).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou si son identité ne peut être déterminée
     *         (levé par le service via {@link SecurityException} ou similaire).</li>
     * </ul>
     * @see EventService#findMemberEventsFiltered(String, boolean)
     * @see EventWithFriendsDto
     * @see GlobalView.EventView
     * @see IsMembre
     */
    @GetMapping("/withfriend")
    @IsMembre // Requiert rôle MEMBRE ou supérieur
    @JsonView(GlobalView.EventView.class) // Appliquer une vue appropriée pour le DTO
    public ResponseEntity<List<EventWithFriendsDto>> getAllEventsForMyClubsWithFriend(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean withFriends) {
        List<EventWithFriendsDto> eventDtos = eventService.findMemberEventsFiltered(status, withFriends);
        return ResponseEntity.ok(eventDtos);
    }

    /**
     * Récupère les détails d'un événement spécifique par son ID.
     * <p>
     * <b>Requête:</b> GET /events/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit connecté ({@link IsConnected @IsConnected}).
     * Le service ({@link EventService#getEventByIdWithSecurityCheck}) vérifie que l'utilisateur
     * est membre du club organisateur.
     * </p>
     *
     * @param id L'identifiant unique (Integer) de l'événement à récupérer. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'objet {@link Event} trouvé, sérialisé selon {@link GlobalView.EventView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement avec cet ID n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur connecté n'est pas membre du club organisateur (levé par le service via {@link AccessDeniedException} ou {@link SecurityException}).</li>
     * </ul>
     * @see EventService#getEventByIdWithSecurityCheck(Integer)
     * @see GlobalView.EventView
     * @see IsConnected
     */
    @GetMapping("/{id}")
    @IsConnected // L'utilisateur doit être connecté
    @JsonView(GlobalView.EventView.class) // Vue JSON pour les détails de l'événement
    public ResponseEntity<Event> getEventById(@PathVariable Integer id) {
        // Le service gère existence (-> 404) et sécurité d'accès (membre du club -> 403).
        Event event = eventService.getEventByIdWithSecurityCheck(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Récupère la liste des événements *futurs* des clubs auxquels l'utilisateur actuellement authentifié est membre.
     * Similaire à GET /events mais spécifiquement pour les événements à venir.
     * <p>
     * <b>Requête:</b> GET /events/my-clubs/upcoming?status={status}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit connecté ({@link IsConnected @IsConnected}).
     * L'identifiant de l'utilisateur est utilisé par le service pour déterminer les clubs pertinents.
     * </p>
     *
     * @param status (Optionnel) Filtre (String) sur le statut des événements futurs ('active', 'inactive', 'all').
     *               Voir {@link EventService#findUpcomingEventsForMemberClubs}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Event>} des événements futurs correspondants,
     *         sérialisée selon {@link GlobalView.EventView}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou si son identité ne peut être déterminée
     *         (levé par le service via {@link SecurityException} ou similaire).</li>
     * </ul>
     * @see EventService#findUpcomingEventsForMemberClubs(String)
     * @see GlobalView.EventView
     * @see IsConnected
     */
    @GetMapping("/my-clubs/upcoming")
    @IsConnected // Requiert que l'utilisateur soit connecté
    @JsonView(GlobalView.EventView.class) // Vue JSON pour la liste d'événements
    public ResponseEntity<List<Event>> getMyClubsUpcomingEvents(@RequestParam(required = false) String status) {
        // Le service utilise l'identité de l'utilisateur pour filtrer les clubs et les événements futurs.
        List<Event> events = eventService.findUpcomingEventsForMemberClubs(status);
        return ResponseEntity.ok(events);
    }

    /**
     * Désactive (supprime logiquement) un événement existant.
     * <p>
     * <b>Requête:</b> DELETE /events/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link EventService#deactivateEvent}) vérifie que l'utilisateur est gestionnaire
     * (RESERVATION ou ADMIN) du club organisateur.
     * </p>
     *
     * @param id L'identifiant unique (Integer) de l'événement à désactiver. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws EntityNotFoundException Si l'événement n'existe pas (levé par le service -> 404).
     * @throws AccessDeniedException   Si l'utilisateur n'est pas gestionnaire du club (levé par le service -> 403).
     *                                 L'opération est idempotente si l'événement est déjà inactif.
     * @see EventService#deactivateEvent(Integer)
     * @see IsReservation
     */
    @DeleteMapping("/{id}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    public ResponseEntity<Void> deleteEvent(@PathVariable Integer id) {
        // @ResponseStatus retiré, HttpStatus.NO_CONTENT est géré par ResponseEntity
        eventService.deactivateEvent(id);
        return ResponseEntity.noContent().build(); // Retourne 204 No Content
    }

    /**
     * Récupère les 5 prochains événements actifs du club géré par l'utilisateur connecté.
     * <p>
     * <b>Requête:</b> GET /events/managed-club/next
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link EventService#getNextFiveEventsForManagedClub}) identifie le club géré
     * par l'utilisateur et récupère les événements.
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Event>} (jusqu'à 5) des prochains événements actifs,
     *         sérialisée selon {@link GlobalView.EventView}. Peut être vide.</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire d'un club
     *         (levé par le service via {@link AccessDeniedException}).</li>
     * </ul>
     * @see EventService#getNextFiveEventsForManagedClub()
     * @see GlobalView.EventView
     * @see IsReservation
     */
    @GetMapping("/managed-club/next") // Chemin clarifié
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.EventView.class) // Vue JSON pour la liste d'événements
    public ResponseEntity<List<Event>> getNextFiveEventsForMyClub() {
        List<Event> nextEvents = eventService.getNextFiveEventsForManagedClub();
        return ResponseEntity.ok(nextEvents);
    }

    /**
     * Crée un nouvel événement avec ses catégories initiales associées.
     * <p>
     * <b>Requête:</b> POST /events?organisateurId={organisateurId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link EventService#createEventWithCategories}) vérifie que l'utilisateur
     * est gestionnaire du club spécifié par {@code organisateurId}.
     * </p>
     * <p>
     * <b>Validation:</b> Le corps de la requête ({@link CreateEventWithCategoriesDto}) est validé via {@link Valid @Valid}.
     * </p>
     *
     * @param organisateurId L'ID (Integer) du club qui organise l'événement. Fourni comme paramètre de requête.
     * @param eventDto       Un DTO contenant les détails de l'événement et de ses catégories initiales. Reçu dans le corps et validé.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> Le nouvel {@link Event} créé (avec ses catégories), sérialisé selon {@link GlobalView.EventView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code eventDto} sont invalides (ex: dates incohérentes, nom catégorie dupliqué)
     *         (levé par {@link MethodArgumentNotValidException} ou {@link IllegalArgumentException} du service).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club organisateur n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire du club organisateur (levé par le service via {@link AccessDeniedException}).</li>
     * </ul>
     * @see EventService#createEventWithCategories(Integer, CreateEventWithCategoriesDto)
     * @see CreateEventWithCategoriesDto
     * @see GlobalView.EventView
     * @see IsReservation
     * @see Valid
     */
    @PostMapping
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.EventView.class) // Vue JSON pour l'événement créé
    public ResponseEntity<Event> createEventWithCategories(@RequestParam Integer organisateurId,
                                                           @Valid @RequestBody CreateEventWithCategoriesDto eventDto) {
        // @ResponseStatus retiré, HttpStatus.CREATED géré par ResponseEntity
        // @Valid assure la validation du DTO -> 400
        // Le service gère la sécurité (-> 403), existence du club (-> 404), règles métier (-> 400)
        Event newEvent = eventService.createEventWithCategories(organisateurId, eventDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }

    /**
     * Met à jour un événement existant ainsi que ses catégories associées (réconciliation complète).
     * <p>
     * <b>Requête:</b> PUT /events/{id}/full
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link EventService#updateEventWithCategories}) vérifie que l'utilisateur
     * est gestionnaire du club organisateur.
     * </p>
     * <p>
     * <b>Validation:</b> Le corps de la requête ({@link UpdateEventWithCategoriesDto}) est validé via {@link Valid @Valid}.
     * </p>
     *
     * @param id       L'identifiant unique (Integer) de l'événement à mettre à jour. Provient du chemin de l'URL.
     * @param eventDto Un DTO contenant les nouvelles informations de l'événement et la liste complète des catégories désirées. Reçu dans le corps et validé.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'{@link Event} mis à jour (avec ses catégories), sérialisé selon {@link GlobalView.EventView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code eventDto} sont invalides (dates, nom catégorie dupliqué)
     *         (levé par {@link MethodArgumentNotValidException} ou {@link IllegalArgumentException} du service).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire du club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'événement est terminé/inactif, ou si une réduction de capacité est impossible à cause de réservations (levé par le service via {@link IllegalStateException}).</li>
     * </ul>
     * @see EventService#updateEventWithCategories(Integer, UpdateEventWithCategoriesDto)
     * @see UpdateEventWithCategoriesDto
     * @see GlobalView.EventView
     * @see IsReservation
     * @see Valid
     */
    @PutMapping("/{id}/full")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.EventView.class) // Vue JSON pour l'événement mis à jour
    public ResponseEntity<Event> updateEventWithCategories(@PathVariable Integer id,
                                                           @Valid @RequestBody UpdateEventWithCategoriesDto eventDto) {
        // @Valid assure la validation du DTO -> 400
        // Le service gère existence (-> 404), sécurité (-> 403), règles métier (-> 400, 409)
        Event updatedEvent = eventService.updateEventWithCategories(id, eventDto);
        return ResponseEntity.ok(updatedEvent);
    }

    // Rappel : La gestion centralisée des exceptions via @ControllerAdvice est essentielle
    // pour mapper les exceptions (EntityNotFoundException, AccessDeniedException, IllegalStateException,
    // IllegalArgumentException, MethodArgumentNotValidException) aux bonnes réponses HTTP (404, 403, 409, 400).
}

package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des événements.
 * <p>
 * Base URL: /events
 * </p>
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@CrossOrigin
public class EventController {

    private final EventService eventService;

    /**
     * Récupère les événements futurs des clubs de l'utilisateur.
     * <p>
     * Endpoint: GET /events
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN).
     *
     * @param status (Optionnel) Filtre sur le statut des événements ('active', 'inactive', 'all').
     * @return La liste des événements (200 OK).
     */
    @GetMapping
    @IsReservation
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<Event>> getAllEventsForMyClubs(@RequestParam(required = false) String status) {
        List<Event> events = eventService.findAllEventsForMemberClubs(status);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupère les événements futurs des clubs de l'utilisateur, en incluant les amis participants.
     * <p>
     * Endpoint: GET /events/withfriend
     * <p>
     * Accès réservé aux membres authentifiés.
     *
     * @param status      (Optionnel) Filtre sur le statut des événements.
     * @param withFriends (Optionnel) Si true, filtre les événements où au moins un ami participe.
     * @return La liste des événements avec les informations sur les amis (200 OK).
     */
    @GetMapping("/withfriend")
    @IsMembre
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<EventWithFriendsDto>> getAllEventsForMyClubsWithFriend(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean withFriends) {
        List<EventWithFriendsDto> eventDtos = eventService.findMemberEventsFiltered(status, withFriends);
        return ResponseEntity.ok(eventDtos);
    }

    /**
     * Récupère les détails d'un événement par son ID.
     * <p>
     * Endpoint: GET /events/{id}
     * <p>
     * Accès réservé aux utilisateurs connectés et membres du club organisateur.
     *
     * @param id L'ID de l'événement.
     * @return L'événement trouvé (200 OK).
     */
    @GetMapping("/{id}")
    @IsConnected
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> getEventById(@PathVariable Integer id) {
        Event event = eventService.getEventByIdWithSecurityCheck(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Récupère les événements futurs des clubs de l'utilisateur.
     * <p>
     * Endpoint: GET /events/my-clubs/upcoming
     * <p>
     * Accès réservé aux utilisateurs connectés.
     *
     * @param status (Optionnel) Filtre sur le statut des événements.
     * @return La liste des événements futurs (200 OK).
     */
    @GetMapping("/my-clubs/upcoming")
    @IsConnected
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<Event>> getMyClubsUpcomingEvents(@RequestParam(required = false) String status) {
        List<Event> events = eventService.findUpcomingEventsForMemberClubs(status);
        return ResponseEntity.ok(events);
    }

    /**
     * Désactive un événement (suppression logique).
     * <p>
     * Endpoint: DELETE /events/{id}
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param id L'ID de l'événement à désactiver.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/{id}")
    @IsReservation
    public ResponseEntity<Void> deleteEvent(@PathVariable Integer id) {
        eventService.deactivateEvent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les 5 prochains événements actifs du club géré par l'utilisateur.
     * <p>
     * Endpoint: GET /events/managed-club/next
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN).
     *
     * @return La liste des 5 prochains événements (200 OK).
     */
    @GetMapping("/managed-club/next")
    @IsReservation
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<Event>> getNextFiveEventsForMyClub() {
        List<Event> nextEvents = eventService.getNextFiveEventsForManagedClub();
        return ResponseEntity.ok(nextEvents);
    }

    /**
     * Crée un nouvel événement avec ses catégories.
     * <p>
     * Endpoint: POST /events?organisateurId={organisateurId}
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param organisateurId L'ID du club organisateur.
     * @param eventDto       DTO contenant les détails de l'événement et de ses catégories.
     * @return Le nouvel événement créé (201 Created).
     */
    @PostMapping
    @IsReservation
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> createEventWithCategories(@RequestParam Integer organisateurId,
                                                           @Valid @RequestBody CreateEventWithCategoriesDto eventDto) {
        Event newEvent = eventService.createEventWithCategories(organisateurId, eventDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }

    /**
     * Met à jour un événement et l'ensemble de ses catégories.
     * <p>
     * Endpoint: PUT /events/{id}/full
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param id       L'ID de l'événement à mettre à jour.
     * @param eventDto DTO contenant les nouvelles informations de l'événement et de ses catégories.
     * @return L'événement mis à jour (200 OK).
     */
    @PutMapping("/{id}/full")
    @IsReservation
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> updateEventWithCategories(@PathVariable Integer id,
                                                           @Valid @RequestBody UpdateEventWithCategoriesDto eventDto) {
        Event updatedEvent = eventService.updateEventWithCategories(id, eventDto);
        return ResponseEntity.ok(updatedEvent);
    }
}

package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin
public class EventController {

    private final EventService eventService;

    /**
     * GET /api/events
     * Récupère tous les événements (pas de filtre sécurité ici, peut être ajusté).
     * Sécurité: Authentifié.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.Base.class)
    public List<Event> getAllEvents() {
        return eventService.findAllEvents();
    }

    /**
     * GET /api/events/{id}
     * Récupère un événement par ID.
     * Sécurité: Authentifié + Membre du club organisateur (vérifié dans service).
     * Exceptions (globales): 404 (Non trouvé), 403 (Non membre).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // L'utilisateur doit être connecté
    @JsonView(GlobalView.EventView.class)
    public Event getEventById(@PathVariable Integer id) {
        // Le service lance 404 ou 403 si nécessaire
        return eventService.getEventByIdWithSecurityCheck(id);
    }

    /**
     * GET /api/events/upcoming
     * Récupère tous les événements futurs.
     * Sécurité: Authentifié.
     */
    @GetMapping("/upcoming")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.Base.class)
    public List<Event> getUpcomingEvents() {
        return eventService.findUpcomingEvents();
    }

    /**
     * GET /api/events/organisateur/{clubId}
     * Récupère les événements (tous) d'un club spécifique.
     * Sécurité: Authentifié + Membre de ce club (vérifié dans service).
     * Exceptions (globales): 404 (Club non trouvé), 403 (Non membre).
     */
    @GetMapping("/organisateur/{clubId}")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.Base.class)
    public List<Event> getEventsByOrganisateur(@PathVariable Integer clubId) {
        return eventService.findEventsByOrganisateurWithSecurityCheck(clubId);
    }

    /**
     * GET /api/events/organisateur/{clubId}/upcoming
     * Récupère les événements FUTURS d'un club spécifique.
     * Sécurité: Authentifié + Membre de ce club (vérifié dans service).
     * Exceptions (globales): 404 (Club non trouvé), 403 (Non membre).
     */
    @GetMapping("/organisateur/{clubId}/upcoming")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.Base.class)
    public List<Event> getUpcomingEventsByOrganisateur(@PathVariable Integer clubId) {
        return eventService.findUpcomingEventsByOrganisateurWithSecurityCheck(clubId);
    }

    /**
     * GET /api/events/my-clubs/upcoming
     * Récupère les événements futurs des clubs de l'utilisateur courant.
     * Sécurité: Authentifié.
     * Exceptions (globales): 401 (Si SecurityService ne trouve pas l'user).
     */
    @GetMapping("/my-clubs/upcoming")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.Base.class)
    public List<Event> getMyClubsUpcomingEvents() {
        // Le service utilise SecurityService pour trouver l'utilisateur et ses clubs
        return eventService.findUpcomingEventsForMemberClubs();
    }

    /**
     * POST /api/events?organisateurId={clubId}
     * Crée un nouvel événement pour un club.
     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
     * Validation: Données de l'événement validées (@Valid).
     * Exceptions (globales): 404 (Club non trouvé), 403 (Non manager), 400 (Validation ou dates invalides).
     */
    @PostMapping
    @IsReservation // Rôle minimum
    @ResponseStatus(HttpStatus.CREATED) // 201
    @JsonView(GlobalView.EventView.class)
    public Event createEvent(@RequestParam Integer organisateurId, @Valid @RequestBody Event event) {
        // Le service gère la création, la sécurité contextuelle, et les erreurs métier
        return eventService.createEvent(organisateurId, event);
    }

    /**
     * PUT /api/events/{id}
     * Met à jour un événement.
     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
     * Validation: Données de l'événement validées (@Valid).
     * Exceptions (globales): 404 (Event non trouvé), 403 (Non manager), 400 (Validation ou dates invalides).
     */
    @PutMapping("/{id}")
    @IsReservation // Rôle minimum
    @JsonView(GlobalView.EventView.class)
    public Event updateEvent(@PathVariable Integer id, @Valid @RequestBody Event eventDetails) {
        // Le service gère la mise à jour, la sécurité contextuelle, et les erreurs métier
        return eventService.updateEvent(id, eventDetails);
    }

    /**
     * DELETE /api/events/{id}
     * Supprime un événement.
     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
     * Exceptions (globales): 404 (Event non trouvé), 403 (Non manager), 409 (Réservations existent).
     */
    @DeleteMapping("/{id}")
    @IsReservation // Rôle minimum
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteEvent(@PathVariable Integer id) {
        // Le service gère la suppression, la sécurité contextuelle, et les erreurs métier
        eventService.deleteEvent(id);
    }

    // Le @ExceptionHandler pour MethodArgumentNotValidException doit être dans GlobalExceptionHandler.
}

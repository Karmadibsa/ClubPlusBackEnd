package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin
public class EventController {

    private final EventService eventService;

    // GET /api/events - Récupérer tous les événements (vue de base)
    @GetMapping
    @JsonView(GlobalView.Base.class)
    public List<Event> getAllEvents() {
        return eventService.findAllEvents();
    }

    // GET /api/events/{id} - Récupérer un événement par ID (vue détaillée)
    @GetMapping("/{id}")
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> getEventById(@PathVariable Integer id) {
        return eventService.findEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/events/upcoming - Récupérer les événements à venir (vue de base)
    @GetMapping("/upcoming")
    @JsonView(GlobalView.Base.class)
    public List<Event> getUpcomingEvents() {
        return eventService.findUpcomingEvents();
    }

    // GET /api/events/organisateur/{clubId} - Récupérer les événements par organisateur (vue de base)
    @GetMapping("/organisateur/{clubId}")
    @JsonView(GlobalView.Base.class)
    public List<Event> getEventsByOrganisateur(@PathVariable Integer clubId) {
        // Ajouter une validation pour vérifier si le clubId existe serait bien
        return eventService.findEventsByOrganisateur(clubId);
    }

    @GetMapping("/organisateur/{clubId}/upcoming")
    @JsonView(GlobalView.Base.class)
    public List<Event> getUpcomingEventsByOrganisateur(@PathVariable Integer clubId) {
        // Le service gère maintenant la logique de vérifier si le club existe (si implémenté)
        // et de combiner les filtres.
        return eventService.findUpcomingEventsByOrganisateur(clubId);
        // Si le service lance une EntityNotFoundException pour un club inexistant,
        // un @ControllerAdvice serait idéal pour la transformer en réponse 404.
    }


    @PostMapping
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event, @RequestParam Integer organisateurId) {
        try {
            Event createdEvent = eventService.createEvent(event, organisateurId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
        } catch (EntityNotFoundException e) { // Ex: Club organisateur non trouvé
            // Idéalement, retourner un message d'erreur plus précis dans le corps
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400 Bad Request
        } catch (IllegalArgumentException e) { // Ex: Dates invalides
            // Idéalement, retourner le message de l'exception dans le corps
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400 Bad Request
        }
    }

    // PUT /api/events/{id} - Mettre à jour un événement existant
    @PutMapping("/{id}")
    @JsonView(GlobalView.EventView.class)
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwner(principal, #id)") // Vérifier si l'utilisateur est l'organisateur
    public ResponseEntity<Event> updateEvent(@PathVariable Integer id, @Valid @RequestBody Event eventDetails) {
        try {
            Event updatedEvent = eventService.updateEvent(id, eventDetails);
            return ResponseEntity.ok(updatedEvent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            // Retourner un message d'erreur
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // DELETE /api/events/{id} - Supprimer un événement
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwner(principal, #id)")
    public ResponseEntity<Void> deleteEvent(@PathVariable Integer id) {
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (IllegalStateException e) { // Ex: Si on empêche la suppression si réservations existent
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        }
    }
    
}

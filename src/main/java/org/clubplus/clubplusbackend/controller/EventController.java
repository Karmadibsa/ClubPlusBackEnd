package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final CategorieService categorieService;

    public EventController(EventService eventService, CategorieService categorieService) {
        this.eventService = eventService;
        this.categorieService = categorieService;
    }

    // Récupérer un événement avec ses catégories
    @GetMapping("/{id}")
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> getEvent(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    // Récupérer tous les events
    @GetMapping
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }


    // Créer un nouvel événement avec catégories
    @PostMapping
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event nouveauEvent = eventService.createEvent(event);
        return new ResponseEntity<>(nouveauEvent, HttpStatus.CREATED);
    }


    // Supprimer un event
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (!eventService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        eventService.deleteEvent(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Mettre à jour un événement
    @PutMapping("/{id}")
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        return eventService.updateEvent(id, event)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

//    Essai

    // Récupérer les catégories d'un événement
    @GetMapping("/{id}/categories")
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<List<Categorie>> getEventCategories(@PathVariable Long id) {
        if (!eventService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Categorie> categories = categorieService.getCategoriesByEventId(id);
        return ResponseEntity.ok(categories);
    }

    // Ajouter une catégorie à un événement
    @PostMapping("/{id}/categories")
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<Categorie> addCategorie(@PathVariable Long id, @RequestBody Categorie categorie) {
        return categorieService.addCategorieToEvent(id, categorie)
                .map(savedCategorie -> new ResponseEntity<>(savedCategorie, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Supprimer une catégorie
    @DeleteMapping("/{eventId}/categories/{categorieId}")
    public ResponseEntity<Void> deleteCategorie(@PathVariable Long eventId, @PathVariable Long categorieId) {
        if (!eventService.existsById(eventId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        categorieService.deleteCategorie(categorieId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}

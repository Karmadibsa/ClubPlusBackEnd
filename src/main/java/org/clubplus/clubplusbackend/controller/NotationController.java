package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateNotationDto;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Notation;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.NotationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des notations des événements.
 * <p>
 * Base URL: /events
 * </p>
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@CrossOrigin
public class NotationController {

    private final NotationService notationService;

    /**
     * Crée une nouvelle notation pour un événement.
     * <p>
     * Endpoint: POST /events/{eventId}/notations
     * <p>
     * L'utilisateur doit être un membre authentifié ayant participé à l'événement
     * (réservation 'UTILISE') et ne doit pas l'avoir déjà noté.
     *
     * @param eventId     L'ID de l'événement à noter.
     * @param notationDto DTO contenant les notes.
     * @return La nouvelle notation créée (201 Created).
     */
    @PostMapping("/{eventId}/notations")
    @IsMembre
    @JsonView(GlobalView.NotationView.class)
    public ResponseEntity<Notation> createMyNotation(@PathVariable Integer eventId,
                                                     @Valid @RequestBody CreateNotationDto notationDto) {
        Notation newNotation = notationService.createMyNotation(notationDto, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newNotation);
    }

    /**
     * Récupère les notations pour un événement. Les données sont anonymisées.
     * <p>
     * Endpoint: GET /events/{eventId}/notations
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param eventId L'ID de l'événement.
     * @return La liste des notations pour l'événement (200 OK).
     */
    @GetMapping("/{eventId}/notations")
    @IsReservation
    @JsonView(GlobalView.NotationView.class)
    public ResponseEntity<List<Notation>> getNotationsByEvent(@PathVariable Integer eventId) {
        List<Notation> notations = notationService.findNotationsByEventIdWithSecurityCheck(eventId);
        return ResponseEntity.ok(notations);
    }

    /**
     * Récupère les événements auxquels l'utilisateur a participé mais qu'il n'a pas encore notés.
     * <p>
     * Endpoint: GET /events/notations/me/participated-events-unrated
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @return La liste des événements à noter (200 OK).
     */
    @GetMapping("/notations/me/participated-events-unrated")
    @IsMembre
    @JsonView(GlobalView.Base.class)
    public ResponseEntity<List<Event>> getMyUnratedEvents() {
        List<Event> events = notationService.getUnratedParticipatedEvents();
        return ResponseEntity.ok(events);
    }
}

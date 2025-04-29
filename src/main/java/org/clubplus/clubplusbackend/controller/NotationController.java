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

@RestController
// Les notations sont liées aux événements, donc le mapping de base via /events est logique
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin
public class NotationController {

    private final NotationService notationService;
    // Pas besoin de SecurityService ici

    /**
     * POST /api/events/{eventId}/notations
     * Crée une notation par l'utilisateur courant pour un événement.
     * Sécurité: Authentifié. Logique métier (participation, unicité, etc.) dans service.
     * Validation: Données de notation validées (@Valid).
     * Exceptions (globales): 404 (Event non trouvé), 401 (Non auth),
     * 409 (Non terminé, pas participé, déjà noté),
     * 400 (Validation @Valid échouée).
     */
    @PostMapping("/{eventId}/notations")
    @IsMembre
    @ResponseStatus(HttpStatus.CREATED) // 201
    @JsonView(GlobalView.NotationView.class)
    public Notation createMyNotation(@PathVariable Integer eventId,
                                     @Valid @RequestBody CreateNotationDto notationDto) {
        // Le service utilise SecurityService pour obtenir l'ID courant
        // et gère toute la logique métier et les erreurs.
        return notationService.createMyNotation(notationDto, eventId);
    }

    /**
     * GET /api/events/{eventId}/notations
     * Récupère les notations (anonymisées) d'un événement.
     * Sécurité: Authentifié + Membre du club organisateur (@IsMembre + vérif service).
     * Exceptions (globales): 404 (Event non trouvé), 403 (Non membre).
     */
    @GetMapping("/{eventId}/notations")
    @IsReservation
    @JsonView(GlobalView.NotationView.class)
    public List<Notation> getNotationsByEvent(@PathVariable Integer eventId) {
        // Le service vérifie l'existence de l'event et l'appartenance au club
        return notationService.findNotationsByEventIdWithSecurityCheck(eventId);
    }

    // --- Endpoint pour lister les événements non notés ---
    // Un chemin sous /api/me/ ou /api/notations/mine/ serait aussi logique
    @GetMapping("/notations/me/participated-events-unrated") // Nouveau chemin spécifique
    @IsMembre // Seul un membre peut voir ses propres événements à noter
    @JsonView(GlobalView.Base.class) // Ajustez si vous avez une vue EventListView
    public ResponseEntity<List<Event>> getMyUnratedEvents() {
        List<Event> events = notationService.getUnratedParticipatedEvents();
        return ResponseEntity.ok(events);
    }
}

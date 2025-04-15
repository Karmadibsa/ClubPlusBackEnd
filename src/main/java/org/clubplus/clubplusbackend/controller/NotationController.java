package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Notation;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.NotationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Les notations sont liées aux événements, donc le mapping de base via /events est logique
@RequestMapping("/api/events/{eventId}/notations")
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
    @PostMapping
    @PreAuthorize("isAuthenticated()") // Seul un utilisateur connecté peut noter
    @ResponseStatus(HttpStatus.CREATED) // 201
    // Retourne la notation créée (mais le membre sera anonyme grâce à l'entité)
    @JsonView(GlobalView.NotationView.class)
    public Notation createMyNotation(@PathVariable Integer eventId,
                                     @Valid @RequestBody Notation notationInput) {
        // Le service utilise SecurityService pour obtenir l'ID courant
        // et gère toute la logique métier et les erreurs.
        return notationService.createMyNotation(notationInput, eventId);
    }

    /**
     * GET /api/events/{eventId}/notations
     * Récupère les notations (anonymisées) d'un événement.
     * Sécurité: Authentifié + Membre du club organisateur (@IsMembre + vérif service).
     * Exceptions (globales): 404 (Event non trouvé), 403 (Non membre).
     */
    @GetMapping
    @IsReservation
    @JsonView(GlobalView.NotationView.class)
    public List<Notation> getNotationsByEvent(@PathVariable Integer eventId) {
        // Le service vérifie l'existence de l'event et l'appartenance au club
        return notationService.findNotationsByEventIdWithSecurityCheck(eventId);
    }

}

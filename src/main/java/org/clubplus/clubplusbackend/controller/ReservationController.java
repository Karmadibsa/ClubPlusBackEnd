package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ReservationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin
public class ReservationController {

    private final ReservationService reservationService;
    // Pas besoin de SecurityService ici

    /**
     * POST /api/reservations?eventId={eventId}&categorieId={categorieId}
     * Crée une réservation pour l'utilisateur courant pour un événement/catégorie.
     * Sécurité: Utilisateur authentifié (@IsMembre). Logique métier dans service.
     * Exceptions (globales): 404 (Event/Cat non trouvés), 401 (Non auth),
     * 409 (Event passé, limite/capacité atteinte),
     * 400 (Catégorie pas dans Event).
     */
    @PostMapping
    @IsMembre // Doit être au moins membre pour réserver
    @ResponseStatus(HttpStatus.CREATED) // 201
    @JsonView(GlobalView.ReservationView.class) // Retourne la réservation créée
    public Reservation createMyReservation(@RequestParam Integer eventId,
                                           @RequestParam Integer categorieId) {
        // Le service utilise SecurityService pour obtenir l'ID courant
        // et gère toute la logique métier (validation, capacité, limite) et les erreurs.
        return reservationService.createMyReservation(eventId, categorieId);
    }

    /**
     * GET /api/reservations/me
     * Récupère les réservations de l'utilisateur courant.
     * Sécurité: Utilisateur authentifié (@IsMembre).
     */
    @GetMapping("/me")
    @IsMembre // Ou @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.ReservationView.class) // Vue détaillée des réservations
    public List<Reservation> getMyReservations() {
        // Le service utilise SecurityService pour l'ID courant
        return reservationService.findMyReservations();
    }

    /**
     * GET /api/reservations/{id}
     * Récupère une réservation spécifique par ID.
     * Sécurité: Authentifié (@IsMembre). Propriétaire ou Manager vérifié dans le service.
     * Exceptions (globales): 404 (Non trouvé), 403 (Accès refusé).
     */
    @GetMapping("/{id}")
    @IsMembre // Rôle minimum pour tenter
    @JsonView(GlobalView.ReservationView.class)
    public Reservation getReservationById(@PathVariable Integer id) {
        // Le service gère l'existence et la sécurité (owner/manager)
        return reservationService.getReservationByIdWithSecurityCheck(id);
    }

    /**
     * GET /api/reservations/event/{eventId}
     * Récupère les réservations pour un événement (pour les managers).
     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
     * Exceptions (globales): 404 (Event non trouvé), 403 (Non manager).
     */
    @GetMapping("/event/{eventId}")
    @IsReservation // Rôle minimum requis
    @JsonView(GlobalView.ReservationView.class)
    public List<Reservation> getReservationsByEvent(@PathVariable Integer eventId) {
        // Le service gère l'existence et la sécurité (manager)
        return reservationService.findReservationsByEventIdWithSecurityCheck(eventId);
    }

    /**
     * GET /api/reservations/categorie/{categorieId}
     * Récupère les réservations pour une catégorie (pour les managers).
     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
     * Exceptions (globales): 404 (Catégorie non trouvée), 403 (Non manager).
     */
    @GetMapping("/categorie/{categorieId}")
    @IsReservation // Rôle minimum requis
    @JsonView(GlobalView.ReservationView.class)
    public List<Reservation> getReservationsByCategorie(@PathVariable Integer categorieId) {
        // Le service gère l'existence et la sécurité (manager)
        return reservationService.findReservationsByCategorieIdWithSecurityCheck(categorieId);
    }

    /**
     * DELETE /api/reservations/{id}
     * Annule (supprime) une réservation.
     * Sécurité: Authentifié (@IsMembre). Propriétaire ou Manager vérifié dans service.
     * Exceptions (globales): 404 (Non trouvé), 403 (Accès refusé), 409 (Event passé).
     */
    @DeleteMapping("/{id}")
    @IsMembre // Rôle minimum pour tenter
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteReservation(@PathVariable Integer id) {
        // Le service gère l'existence, la sécurité (owner/manager) et la logique métier (event passé)
        reservationService.deleteReservationById(id);
    }

    // Le @ExceptionHandler pour MethodArgumentNotValidException doit être dans GlobalExceptionHandler.
}

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
    @IsMembre
    @JsonView(GlobalView.ReservationView.class)
    public List<Reservation> getMyReservations(
            // Ajout du paramètre de filtrage par statut
            @RequestParam(required = false) String status
    ) {
        // Appel service mis à jour
        return reservationService.findMyReservations(status);
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
    @IsReservation
    @JsonView(GlobalView.ReservationView.class)
    public List<Reservation> getReservationsByEvent(
            @PathVariable Integer eventId,
            // Ajout du paramètre de filtrage par statut
            @RequestParam(required = false) String status
    ) {
        // Appel service mis à jour
        return reservationService.findReservationsByEventIdWithSecurityCheck(eventId, status);
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
     * PUT /api/reservations/{id}/cancel
     * Annule une réservation en changeant son statut à CANCELLED.
     * Sécurité: Authentifié (@IsMembre). Propriétaire ou Manager vérifié dans service.
     * Exceptions (globales): 404 (Non trouvé), 403 (Accès refusé), 409 (Event passé ou résa non confirmée).
     *
     * @param id L'ID de la réservation à annuler.
     * @return La réservation mise à jour avec le statut CANCELLED.
     */
    @PutMapping("/{id}/cancel") // Changement de méthode HTTP et de path
    @IsMembre // Rôle minimum pour tenter
    @ResponseStatus(HttpStatus.OK) // Retourne 200 OK avec le corps de la réponse
    @JsonView(GlobalView.ReservationView.class) // Retourner l'objet mis à jour
    public Reservation cancelReservation(@PathVariable Integer id) {
        // Appeler la méthode de service pour l'annulation logique
        reservationService.cancelReservationById(id);
        // Re-récupérer la réservation pour retourner l'état mis à jour (optionnel mais recommandé)
        return reservationService.getReservationByIdWithSecurityCheck(id);
    }

    /**
     * PUT /api/reservations/uuid/{uuid}/use
     * Marque une réservation comme UTILISÉE via son UUID (pour scan QR code).
     * Sécurité: Rôle spécifique (ex: SCANNER, MANAGER) vérifié dans le service ou via annotation dédiée.
     * Exceptions (globales): 404 (UUID non trouvé), 403 (Non autorisé),
     * 409 (Event non actif/en cours, Résa non confirmée).
     *
     * @param uuid L'UUID de la réservation à marquer comme utilisée.
     * @return La réservation mise à jour avec le statut USED.
     */
    @PutMapping("/uuid/{uuid}/use") // Nouveau chemin utilisant l'UUID
    @IsReservation
    @ResponseStatus(HttpStatus.OK) // 200 OK
    @JsonView(GlobalView.ReservationView.class) // Retourner l'objet mis à jour
    public Reservation markReservationUsed(@PathVariable String uuid) {
        // Le service gère toute la logique et les vérifications
        return reservationService.markReservationAsUsed(uuid);
    }
    // Le @ExceptionHandler pour MethodArgumentNotValidException doit être dans GlobalExceptionHandler.
}

package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/categories") // Chemin de base pour les catégories d'un événement
@RequiredArgsConstructor // Injecte CategorieService
public class CategorieController {

    private final CategorieService categorieService;

    /**
     * GET /api/events/{eventId}/categories
     * Récupère toutes les catégories pour un événement.
     * Sécurité : Rôle MEMBRE minimum requis (via @IsMembre).
     * Appartenance au club vérifiée dans le service.
     * Exceptions (gérées globalement) : 404 (Event non trouvé), 403 (Non membre).
     */
    @GetMapping
    @IsMembre // Vérification du rôle minimum au niveau du contrôleur
    @JsonView(GlobalView.Base.class) // Vue pour la liste
    public List<Categorie> getCategoriesByEvent(@PathVariable Integer eventId) {
        // Appelle directement le service.
        // Si une exception (EntityNotFound, AccessDenied) est levée, GlobalExceptionHandler la prendra en charge.
        return categorieService.findCategoriesByEventId(eventId);
    }

    /**
     * GET /api/events/{eventId}/categories/{categorieId}
     * Récupère une catégorie spécifique par ID et ID événement.
     * Sécurité : Rôle MEMBRE minimum requis (via @IsMembre).
     * Appartenance au club et à l'événement vérifiée dans le service.
     * Exceptions (gérées globalement) : 404 (Non trouvé/Non lié), 403 (Non membre).
     */
    @GetMapping("/{categorieId}")
    @IsMembre // Vérification du rôle minimum
    @JsonView(GlobalView.CategorieView.class) // Vue détaillée
    public Categorie getCategorieByIdAndEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        return categorieService.getCategorieByIdAndEventIdWithSecurityCheck(eventId, categorieId);
    }

    /**
     * POST /api/events/{eventId}/categories
     * Crée une nouvelle catégorie pour un événement.
     * Sécurité : Rôle RESERVATION ou ADMIN requis (via @IsReservation).
     * Droits de manager sur le club vérifiés dans le service.
     * Validation : Les données de la catégorie sont validées (@Valid).
     * Exceptions (gérées globalement) : 404 (Event non trouvé), 403 (Non manager),
     * 400 (Validation échouée, capacité négative),
     * 409 (Nom dupliqué).
     */
    @PostMapping
    @IsReservation // Vérification rôle minimum (RESERVATION ou ADMIN)
    @ResponseStatus(HttpStatus.CREATED) // Code 201 si succès
    @JsonView(GlobalView.CategorieView.class) // Retourne la catégorie créée
    public Categorie addCategorieToEvent(@PathVariable Integer eventId, @Valid @RequestBody Categorie categorie) {
        // @Valid déclenche la validation Bean. Si échec -> MethodArgumentNotValidException (gérée globalement -> 400).
        return categorieService.addCategorieToEvent(eventId, categorie);
    }

    /**
     * PUT /api/events/{eventId}/categories/{categorieId}
     * Met à jour une catégorie existante.
     * Sécurité : Rôle RESERVATION ou ADMIN requis (via @IsReservation).
     * Droits de manager sur le club vérifiés dans le service.
     * Validation : Les données de la catégorie sont validées (@Valid).
     * Exceptions (gérées globalement) : 404 (Non trouvé/Non lié), 403 (Non manager),
     * 400 (Validation échouée, capacité négative),
     * 409 (Nom dupliqué, capacité insuffisante).
     */
    @PutMapping("/{categorieId}")
    @IsReservation // Vérification rôle minimum
    @JsonView(GlobalView.CategorieView.class) // Retourne la catégorie mise à jour
    public Categorie updateCategorie(@PathVariable Integer eventId,
                                     @PathVariable Integer categorieId,
                                     @Valid @RequestBody Categorie categorieDetails) {
        return categorieService.updateCategorie(eventId, categorieId, categorieDetails);
    }

    /**
     * DELETE /api/events/{eventId}/categories/{categorieId}
     * Supprime une catégorie existante.
     * Sécurité : Rôle RESERVATION ou ADMIN requis (via @IsReservation).
     * Droits de manager sur le club vérifiés dans le service.
     * Exceptions (gérées globalement) : 404 (Non trouvé/Non lié), 403 (Non manager),
     * 409 (Réservations existantes empêchant la suppression).
     */
    @DeleteMapping("/{categorieId}")
    @IsReservation // Vérification rôle minimum
    @ResponseStatus(HttpStatus.NO_CONTENT) // Code 204 si succès, pas de corps de réponse
    public void deleteCategorie(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        categorieService.deleteCategorie(eventId, categorieId);
        // Rien à retourner en cas de succès (204).
    }

    // Le @ExceptionHandler(MethodArgumentNotValidException.class) a été retiré.
    // Il DOIT être placé dans votre classe @ControllerAdvice (GlobalExceptionHandler)
    // pour centraliser la gestion des erreurs de validation @Valid.
}

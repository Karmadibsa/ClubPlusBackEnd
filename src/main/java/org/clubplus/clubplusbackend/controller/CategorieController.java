package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateCategorieDto;
import org.clubplus.clubplusbackend.dto.UpdateCategorieDto;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour gérer les opérations CRUD sur les catégories d'un événement.
 * <p>
 * Base URL: /events/{eventId}/categories
 * </p>
 */
@RestController
@RequestMapping("/events/{eventId}/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    /**
     * Récupère la liste de toutes les catégories pour un événement spécifique.
     * <p>
     * Endpoint: GET /events/{eventId}/categories
     * <p>
     * Accès réservé aux utilisateurs connectés et membres du club organisateur.
     *
     * @param eventId L'ID de l'événement.
     * @return La liste des catégories (200 OK).
     */
    @GetMapping
    @IsConnected
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<List<Categorie>> getCategoriesByEvent(@PathVariable Integer eventId) {
        List<Categorie> categories = categorieService.findCategoriesByEventId(eventId);
        return ResponseEntity.ok(categories);
    }

    /**
     * Récupère une catégorie spécifique par son ID, pour un événement donné.
     * <p>
     * Endpoint: GET /events/{eventId}/categories/{categorieId}
     * <p>
     * Accès réservé aux utilisateurs connectés et membres du club organisateur.
     *
     * @param eventId     L'ID de l'événement parent.
     * @param categorieId L'ID de la catégorie à récupérer.
     * @return La catégorie trouvée (200 OK).
     */
    @GetMapping("/{categorieId}")
    @IsConnected
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<Categorie> getCategorieByIdAndEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        Categorie categorie = categorieService.getCategorieByIdAndEventIdWithSecurityCheck(eventId, categorieId);
        return ResponseEntity.ok(categorie);
    }

    /**
     * Crée une nouvelle catégorie et l'associe à un événement.
     * <p>
     * Endpoint: POST /events/{eventId}/categories
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN).
     *
     * @param eventId      L'ID de l'événement.
     * @param categorieDto DTO contenant les informations de la nouvelle catégorie.
     * @return La nouvelle catégorie créée (201 Created).
     */
    @PostMapping
    @IsReservation
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<Categorie> addCategorieToEvent(@PathVariable Integer eventId,
                                                         @Valid @RequestBody CreateCategorieDto categorieDto) {
        Categorie newCategorie = categorieService.addCategorieToEvent(eventId, categorieDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newCategorie);
    }

    /**
     * Met à jour les informations d'une catégorie existante.
     * <p>
     * Endpoint: PUT /events/{eventId}/categories/{categorieId}
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN).
     *
     * @param eventId      L'ID de l'événement parent.
     * @param categorieId  L'ID de la catégorie à mettre à jour.
     * @param categorieDto DTO contenant les nouvelles informations.
     * @return La catégorie mise à jour (200 OK).
     */
    @PutMapping("/{categorieId}")
    @IsReservation
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<Categorie> updateCategorie(@PathVariable Integer eventId,
                                                     @PathVariable Integer categorieId,
                                                     @Valid @RequestBody UpdateCategorieDto categorieDto) {
        Categorie updatedCategorie = categorieService.updateCategorie(eventId, categorieId, categorieDto);
        return ResponseEntity.ok(updatedCategorie);
    }

    /**
     * Supprime une catégorie d'un événement.
     * <p>
     * Endpoint: DELETE /events/{eventId}/categories/{categorieId}
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN).
     * La suppression est impossible si des réservations existent pour cette catégorie.
     *
     * @param eventId     L'ID de l'événement parent.
     * @param categorieId L'ID de la catégorie à supprimer.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/{categorieId}")
    @IsReservation
    public ResponseEntity<Void> deleteCategorie(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        categorieService.deleteCategorie(eventId, categorieId);
        return ResponseEntity.noContent().build();
    }
}

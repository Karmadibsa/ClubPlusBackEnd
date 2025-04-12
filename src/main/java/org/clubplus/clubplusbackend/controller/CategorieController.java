package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // Chemin de base, sera complété par les mappings de méthodes
@RequiredArgsConstructor
@CrossOrigin
public class CategorieController {

    private final CategorieService categorieService;
    // Injecter ReservationService si on ajoute un endpoint pour les réservations par catégorie
    // private final ReservationService reservationService;


    // GET /api/events/{eventId}/categories - Récupérer toutes les catégories d'un événement
    @GetMapping("/events/{eventId}/categories")
    @JsonView(GlobalView.Base.class) // Vue de base pour la liste
    public ResponseEntity<List<Categorie>> getCategoriesByEvent(@PathVariable Integer eventId) {
        try {
            List<Categorie> categories = categorieService.findCategoriesByEventId(eventId);
            return ResponseEntity.ok(categories);
        } catch (EntityNotFoundException e) { // Si l'événement lui-même n'est pas trouvé
            // Le service lance l'exception si l'événement n'existe pas
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/events/{eventId}/categories/{categorieId} - Récupérer une catégorie spécifique d'un événement
    @GetMapping("/events/{eventId}/categories/{categorieId}")
    @JsonView(GlobalView.CategorieView.class) // Vue détaillée de la catégorie
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Categorie> getCategorieByIdAndEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        // La méthode findCategorieByIdAndEventId gère déjà la non-trouvaille ou l'appartenance incorrecte
        return categorieService.findCategorieByIdAndEventId(eventId, categorieId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // 404 si non trouvé ou pas dans cet événement
    }


    // POST /api/events/{eventId}/categories - Ajouter une nouvelle catégorie à un événement
    @PostMapping("/events/{eventId}/categories")
    @JsonView(GlobalView.CategorieView.class) // Retourne la catégorie créée
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwner(principal, #eventId)") // Seul l'organisateur ou admin ?
    public ResponseEntity<?> addCategorieToEvent(@PathVariable Integer eventId, @Valid @RequestBody Categorie categorie) {
        try {
            Categorie createdCategorie = categorieService.addCategorieToEvent(eventId, categorie);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategorie);
        } catch (EntityNotFoundException e) { // Événement parent non trouvé
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Événement non trouvé: impossible d'ajouter la catégorie."));
        } catch (IllegalArgumentException e) { // Nom de catégorie déjà pris pour cet événement
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur ajout catégorie à événement {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de l'ajout de la catégorie."));
        }
    }

    // PUT /api/events/{eventId}/categories/{categorieId} - Mettre à jour une catégorie d'un événement
    @PutMapping("/events/{eventId}/categories/{categorieId}")
    @JsonView(GlobalView.CategorieView.class) // Retourne la catégorie mise à jour
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwner(principal, #eventId)")
    public ResponseEntity<?> updateCategorieInEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId, @Valid @RequestBody Categorie categorieDetails) {
        try {
            Categorie updatedCategorie = categorieService.updateCategorie(eventId, categorieId, categorieDetails);
            return ResponseEntity.ok(updatedCategorie);
        } catch (EntityNotFoundException e) { // Catégorie non trouvée ou pas dans cet événement
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage())); // Message de getCategorieByIdAndEventIdOrThrow
        } catch (IllegalArgumentException e) { // Conflit de nom ou capacité invalide
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur MàJ catégorie {} pour événement {}", categorieId, eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de la mise à jour de la catégorie."));
        }
    }

    // DELETE /api/events/{eventId}/categories/{categorieId} - Supprimer une catégorie d'un événement
    @DeleteMapping("/events/{eventId}/categories/{categorieId}")
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwner(principal, #eventId)")
    public ResponseEntity<?> deleteCategorieFromEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        try {
            categorieService.deleteCategorie(eventId, categorieId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) { // Catégorie non trouvée ou pas dans cet événement
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Suppression impossible (ex: réservations existent)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur suppression catégorie {} pour événement {}", categorieId, eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de la suppression de la catégorie."));
        }
    }

}

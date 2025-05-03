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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour gérer les opérations CRUD (Créer, Lire, Mettre à jour, Supprimer)
 * sur les entités {@link Categorie} associées à un événement spécifique ({@code Event}).
 * Fournit des endpoints pour lister, récupérer, ajouter, modifier et supprimer des catégories
 * pour un événement donné, en appliquant les contrôles de sécurité appropriés basés sur les rôles
 * et l'appartenance au club.
 * Le chemin de base pour ce contrôleur est "/api/events/{eventId}/categories".
 */
@RestController
@RequestMapping("/api/events/{eventId}/categories") // Chemin de base pour les catégories d'un événement spécifique
@RequiredArgsConstructor // Injection de dépendances via Lombok pour les champs final
public class CategorieController {

    /**
     * Service responsable de toute la logique métier concernant les catégories d'événements.
     */
    private final CategorieService categorieService;

    /**
     * Récupère la liste de toutes les catégories associées à un événement spécifique.
     * Endpoint: GET /api/events/{eventId}/categories
     * Sécurité: Requiert que l'utilisateur soit connecté ({@code @IsConnected}).
     * Le service sous-jacent vérifie l'appartenance de l'utilisateur au club organisant l'événement.
     *
     * @param eventId L'identifiant unique (Integer) de l'événement dont les catégories doivent être récupérées.
     *                Provient du chemin de l'URL.
     * @return Une {@code List<Categorie>} contenant toutes les catégories de l'événement spécifié,
     * potentiellement vide si aucune catégorie n'existe. La sérialisation utilise la vue {@link GlobalView.CategorieView}.
     * @throws org.clubplus.clubplusbackend.exception.EventNotFoundException Si l'événement avec l'ID spécifié n'existe pas (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException     Si l'utilisateur connecté n'est pas autorisé à accéder aux informations
     *                                                                       de cet événement (par exemple, non membre du club) (géré globalement -> 403 Forbidden).
     * @see CategorieService#findCategoriesByEventId(Integer)
     * @see GlobalView.CategorieView
     * @see IsConnected
     */
    @GetMapping
    @IsConnected // Assure que seul un utilisateur connecté peut accéder
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la réponse
    public List<Categorie> getCategoriesByEvent(@PathVariable Integer eventId) {
        // Délégation au service qui gère la logique métier et la sécurité fine.
        // Les exceptions (ex: EventNotFoundException, AccessDeniedException) sont gérées globalement.
        return categorieService.findCategoriesByEventId(eventId);
    }

    /**
     * Récupère une catégorie spécifique par son identifiant et l'identifiant de l'événement auquel elle est associée.
     * Endpoint: GET /api/events/{eventId}/categories/{categorieId}
     * Sécurité: Requiert que l'utilisateur soit connecté ({@code @IsConnected}).
     * Le service vérifie que la catégorie appartient bien à l'événement spécifié et que l'utilisateur
     * a le droit d'accéder à cet événement (appartenance au club).
     *
     * @param eventId     L'identifiant unique (Integer) de l'événement parent. Provient du chemin de l'URL.
     * @param categorieId L'identifiant unique (Integer) de la catégorie à récupérer. Provient du chemin de l'URL.
     * @return L'objet {@link Categorie} correspondant aux identifiants fournis.
     * La sérialisation utilise la vue {@link GlobalView.CategorieView}.
     * @throws org.clubplus.clubplusbackend.exception.CategorieNotFoundException Si la catégorie ou l'événement n'est pas trouvé,
     *                                                                           ou si la catégorie spécifiée n'appartient pas à l'événement indiqué (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException         Si l'utilisateur connecté n'est pas autorisé à accéder
     *                                                                           aux informations de cet événement (géré globalement -> 403 Forbidden).
     * @see CategorieService#getCategorieByIdAndEventIdWithSecurityCheck(Integer, Integer)
     * @see GlobalView.CategorieView
     * @see IsConnected
     */
    @GetMapping("/{categorieId}")
    @IsConnected // Assure que seul un utilisateur connecté peut accéder
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la réponse
    public Categorie getCategorieByIdAndEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        // Délégation au service qui gère la logique métier et la sécurité (vérification de l'appartenance event/catégorie).
        return categorieService.getCategorieByIdAndEventIdWithSecurityCheck(eventId, categorieId);
    }

    /**
     * Crée une nouvelle catégorie et l'associe à un événement existant.
     * Endpoint: POST /api/events/{eventId}/categories
     * Sécurité: Requiert un rôle de niveau RESERVATION ou supérieur ({@code @IsReservation}).
     * Le service vérifie que l'utilisateur a les droits de gestion sur le club de l'événement.
     * Validation: Les données de la catégorie reçues dans le corps de la requête sont validées via {@code @Valid}.
     *
     * @param eventId      L'identifiant unique (Integer) de l'événement auquel ajouter la catégorie. Provient du chemin de l'URL.
     * @param categorieDto Un DTO ({@link CreateCategorieDto}) contenant les informations nécessaires à la création de la catégorie
     *                     (nom, capacité, prix, etc.). Reçu dans le corps de la requête et validé.
     * @return La nouvelle {@link Categorie} créée et persistée, sérialisée selon {@link GlobalView.CategorieView}.
     * Retourne un statut HTTP 201 (Created) en cas de succès.
     * @throws org.springframework.web.bind.MethodArgumentNotValidException               Si les données dans {@code categorieDto} ne respectent pas
     *                                                                                    les contraintes de validation (géré globalement -> 400 Bad Request).
     * @throws org.clubplus.clubplusbackend.exception.EventNotFoundException              Si l'événement avec l'ID spécifié n'existe pas (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException                  Si l'utilisateur n'a pas les droits suffisants (rôle RESERVATION/ADMIN
     *                                                                                    et manager du club) pour créer une catégorie pour cet événement (géré globalement -> 403 Forbidden).
     * @throws org.clubplus.clubplusbackend.exception.CategorieNameAlreadyExistsException Si une catégorie avec le même nom existe déjà
     *                                                                                    pour cet événement (géré globalement -> 409 Conflict).
     * @throws IllegalArgumentException                                                   Si la capacité fournie est négative ou invalide (si non couverte par @Valid, géré globalement -> 400 Bad Request ou 409 Conflict).
     * @see CategorieService#addCategorieToEvent(Integer, CreateCategorieDto)
     * @see CreateCategorieDto
     * @see GlobalView.CategorieView
     * @see IsReservation
     * @see Valid
     */
    @PostMapping
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @ResponseStatus(HttpStatus.CREATED) // Réponse HTTP 201 en cas de succès
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la catégorie créée retournée
    public Categorie addCategorieToEvent(@PathVariable Integer eventId, @Valid @RequestBody CreateCategorieDto categorieDto) {
        // @Valid assure la validation des champs du DTO avant l'appel au service.
        // Les exceptions (Validation, NotFound, AccessDenied, Conflict) sont gérées globalement.
        return categorieService.addCategorieToEvent(eventId, categorieDto);
    }

    /**
     * Met à jour les informations d'une catégorie existante associée à un événement.
     * Endpoint: PUT /api/events/{eventId}/categories/{categorieId}
     * Sécurité: Requiert un rôle de niveau RESERVATION ou supérieur ({@code @IsReservation}).
     * Le service vérifie les droits de gestion de l'utilisateur sur le club et que la catégorie appartient bien à l'événement.
     * Validation: Les données fournies pour la mise à jour sont validées via {@code @Valid}.
     *
     * @param eventId      L'identifiant unique (Integer) de l'événement parent. Provient du chemin de l'URL.
     * @param categorieId  L'identifiant unique (Integer) de la catégorie à mettre à jour. Provient du chemin de l'URL.
     * @param categorieDto Un DTO ({@link UpdateCategorieDto}) contenant les nouvelles informations pour la catégorie.
     *                     Reçu dans le corps de la requête et validé.
     * @return La {@link Categorie} mise à jour et persistée, sérialisée selon {@link GlobalView.CategorieView}.
     * Retourne un statut HTTP 200 (OK) par défaut en cas de succès.
     * @throws org.springframework.web.bind.MethodArgumentNotValidException               Si les données dans {@code categorieDto} sont invalides (géré globalement -> 400 Bad Request).
     * @throws org.clubplus.clubplusbackend.exception.CategorieNotFoundException          Si la catégorie ou l'événement n'est pas trouvé,
     *                                                                                    ou si la catégorie n'appartient pas à l'événement spécifié (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException                  Si l'utilisateur n'a pas les droits suffisants pour modifier
     *                                                                                    cette catégorie (géré globalement -> 403 Forbidden).
     * @throws org.clubplus.clubplusbackend.exception.CategorieNameAlreadyExistsException Si le nouveau nom est déjà utilisé par une autre
     *                                                                                    catégorie du même événement (géré globalement -> 409 Conflict).
     * @throws org.clubplus.clubplusbackend.exception.InsufficientCapacityException       Si la nouvelle capacité est inférieure au nombre
     *                                                                                    de réservations déjà effectuées pour cette catégorie (géré globalement -> 409 Conflict).
     * @see CategorieService#updateCategorie(Integer, Integer, UpdateCategorieDto)
     * @see UpdateCategorieDto
     * @see GlobalView.CategorieView
     * @see IsReservation
     * @see Valid
     */
    @PutMapping("/{categorieId}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la catégorie mise à jour retournée
    public Categorie updateCategorie(@PathVariable Integer eventId,
                                     @PathVariable Integer categorieId,
                                     @Valid @RequestBody UpdateCategorieDto categorieDto) {
        // @Valid assure la validation des champs du DTO.
        // Délégation au service pour la logique de mise à jour et les vérifications de sécurité/métier.
        return categorieService.updateCategorie(eventId, categorieId, categorieDto);
    }

    /**
     * Supprime une catégorie existante associée à un événement.
     * Endpoint: DELETE /api/events/{eventId}/categories/{categorieId}
     * Sécurité: Requiert un rôle de niveau RESERVATION ou supérieur ({@code @IsReservation}).
     * Le service vérifie les droits de gestion et que la catégorie appartient bien à l'événement.
     * La suppression peut être empêchée s'il existe des réservations associées à cette catégorie.
     *
     * @param eventId     L'identifiant unique (Integer) de l'événement parent. Provient du chemin de l'URL.
     * @param categorieId L'identifiant unique (Integer) de la catégorie à supprimer. Provient du chemin de l'URL.
     * @return Rien (statut HTTP 204 No Content est retourné automatiquement en cas de succès sans corps de réponse).
     * @throws org.clubplus.clubplusbackend.exception.CategorieNotFoundException Si la catégorie ou l'événement n'est pas trouvé,
     *                                                                           ou si la catégorie n'appartient pas à l'événement spécifié (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException         Si l'utilisateur n'a pas les droits suffisants pour supprimer
     *                                                                           cette catégorie (géré globalement -> 403 Forbidden).
     * @throws org.clubplus.clubplusbackend.exception.CategorieNotEmptyException Si la catégorie ne peut pas être supprimée car elle
     *                                                                           contient des réservations actives (géré globalement -> 409 Conflict).
     * @see CategorieService#deleteCategorie(Integer, Integer)
     * @see IsReservation
     */
    @DeleteMapping("/{categorieId}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @ResponseStatus(HttpStatus.NO_CONTENT) // Réponse HTTP 204 en cas de succès de la suppression
    public void deleteCategorie(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        // Délégation au service qui gère la logique de suppression et les vérifications associées.
        categorieService.deleteCategorie(eventId, categorieId);
        // Pas de corps de réponse à retourner pour un statut 204.
    }

    // Rappel : La gestion des exceptions spécifiques comme MethodArgumentNotValidException
    // est idéalement centralisée dans une classe @ControllerAdvice (GlobalExceptionHandler)
    // pour éviter la répétition dans chaque contrôleur.
}

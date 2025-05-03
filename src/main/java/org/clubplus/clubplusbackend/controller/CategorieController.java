package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateCategorieDto;
import org.clubplus.clubplusbackend.dto.UpdateCategorieDto;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour gérer les opérations CRUD (Créer, Lire, Mettre à jour, Supprimer)
 * sur les entités {@link Categorie} associées à un événement spécifique ({@link Event}).
 * Fournit des endpoints pour lister, récupérer, ajouter, modifier et supprimer des catégories
 * pour un événement donné, en appliquant les contrôles de sécurité appropriés basés sur les rôles
 * et l'appartenance au club.
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /events/{eventId}/categories} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see Categorie
 * @see CategorieService
 * @see Event
 */
@RestController
@RequestMapping("/events/{eventId}/categories") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor // Injection des dépendances finales via le constructeur (Lombok)
public class CategorieController {

    /**
     * Service responsable de toute la logique métier concernant les catégories d'événements.
     *
     * @see CategorieService
     */
    private final CategorieService categorieService;

    /**
     * Récupère la liste de toutes les catégories associées à un événement spécifique.
     * <p>
     * <b>Requête:</b> GET /events/{eventId}/categories
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit connecté ({@link IsConnected @IsConnected}).
     * Le service sous-jacent ({@link CategorieService#findCategoriesByEventId}) vérifie
     * que l'utilisateur est membre du club organisant l'événement.
     * </p>
     *
     * @param eventId L'identifiant unique (Integer) de l'événement dont les catégories doivent être récupérées.
     *                Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Categorie>} dans le corps, sérialisée selon {@link GlobalView.CategorieView}.
     *         La liste peut être vide si aucune catégorie n'existe.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement avec l'ID spécifié n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur connecté n'est pas autorisé à accéder aux informations
     *         de cet événement (ex: non membre du club) (levé par le service via {@link AccessDeniedException}).</li>
     * </ul>
     * (Note: La gestion des exceptions est assurée par un {@code @ControllerAdvice} global).
     * @see CategorieService#findCategoriesByEventId(Integer)
     * @see GlobalView.CategorieView
     * @see IsConnected
     */
    @GetMapping
    @IsConnected // Assure que seul un utilisateur connecté peut accéder
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la réponse
    public ResponseEntity<List<Categorie>> getCategoriesByEvent(@PathVariable Integer eventId) {
        List<Categorie> categories = categorieService.findCategoriesByEventId(eventId);
        return ResponseEntity.ok(categories); // Retourne la liste avec statut 200 OK
    }

    /**
     * Récupère une catégorie spécifique par son identifiant, en vérifiant qu'elle appartient bien à l'événement spécifié.
     * <p>
     * <b>Requête:</b> GET /events/{eventId}/categories/{categorieId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit connecté ({@link IsConnected @IsConnected}).
     * Le service ({@link CategorieService#getCategorieByIdAndEventIdWithSecurityCheck}) vérifie
     * que la catégorie appartient bien à l'événement et que l'utilisateur est membre du club organisateur.
     * </p>
     *
     * @param eventId     L'identifiant unique (Integer) de l'événement parent. Provient du chemin de l'URL.
     * @param categorieId L'identifiant unique (Integer) de la catégorie à récupérer. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'objet {@link Categorie} trouvé dans le corps, sérialisé selon {@link GlobalView.CategorieView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la catégorie ou l'événement n'est pas trouvé, ou si la catégorie n'appartient pas à l'événement
     *         (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur connecté n'est pas membre du club organisateur
     *         (levé par le service via {@link AccessDeniedException}).</li>
     * </ul>
     * @see CategorieService#getCategorieByIdAndEventIdWithSecurityCheck(Integer, Integer)
     * @see GlobalView.CategorieView
     * @see IsConnected
     */
    @GetMapping("/{categorieId}")
    @IsConnected // Assure que seul un utilisateur connecté peut accéder
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la réponse
    public ResponseEntity<Categorie> getCategorieByIdAndEvent(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        Categorie categorie = categorieService.getCategorieByIdAndEventIdWithSecurityCheck(eventId, categorieId);
        return ResponseEntity.ok(categorie); // Retourne la catégorie avec statut 200 OK
    }

    /**
     * Crée une nouvelle catégorie et l'associe à un événement existant.
     * <p>
     * <b>Requête:</b> POST /events/{eventId}/categories
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link CategorieService#addCategorieToEvent}) vérifie que l'utilisateur est gestionnaire
     * (RESERVATION ou ADMIN) du club organisateur.
     * </p>
     * <p>
     * <b>Validation:</b> Le corps de la requête ({@link CreateCategorieDto}) est validé via {@link Valid @Valid}.
     * </p>
     *
     * @param eventId      L'identifiant unique (Integer) de l'événement auquel ajouter la catégorie. Provient du chemin de l'URL.
     * @param categorieDto Un DTO ({@link CreateCategorieDto}) contenant les informations pour la nouvelle catégorie (nom, capacité).
     *                     Reçu dans le corps de la requête et validé.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> La nouvelle {@link Categorie} créée dans le corps, sérialisée selon {@link GlobalView.CategorieView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code categorieDto} sont invalides (levé par {@link MethodArgumentNotValidException} ou {@link IllegalArgumentException} du service).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement parent n'est pas trouvé (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'a pas les droits de gestion requis (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'événement est terminé/inactif ou si une catégorie avec le même nom existe déjà pour cet événement (levé par le service via {@link IllegalStateException}).</li>
     * </ul>
     * @see CategorieService#addCategorieToEvent(Integer, CreateCategorieDto)
     * @see CreateCategorieDto
     * @see GlobalView.CategorieView
     * @see IsReservation
     * @see Valid
     */
    @PostMapping
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la catégorie créée retournée
    public ResponseEntity<Categorie> addCategorieToEvent(@PathVariable Integer eventId,
                                                         @Valid @RequestBody CreateCategorieDto categorieDto) {
        // @Valid assure la validation des champs du DTO avant l'appel au service.
        Categorie newCategorie = categorieService.addCategorieToEvent(eventId, categorieDto);
        // Retourne la catégorie créée avec le statut HTTP 201 Created.
        return ResponseEntity.status(HttpStatus.CREATED).body(newCategorie);
    }

    /**
     * Met à jour les informations d'une catégorie existante (nom et/ou capacité).
     * <p>
     * <b>Requête:</b> PUT /events/{eventId}/categories/{categorieId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link CategorieService#updateCategorie}) vérifie les droits de gestion,
     * l'appartenance de la catégorie à l'événement, et le statut de l'événement.
     * </p>
     * <p>
     * <b>Validation:</b> Le corps de la requête ({@link UpdateCategorieDto}) est validé via {@link Valid @Valid}.
     * </p>
     *
     * @param eventId      L'identifiant unique (Integer) de l'événement parent. Provient du chemin de l'URL.
     * @param categorieId  L'identifiant unique (Integer) de la catégorie à mettre à jour. Provient du chemin de l'URL.
     * @param categorieDto Un DTO ({@link UpdateCategorieDto}) contenant les nouvelles informations. Reçu dans le corps et validé.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> La {@link Categorie} mise à jour dans le corps, sérialisée selon {@link GlobalView.CategorieView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code categorieDto} sont invalides (levé par {@link MethodArgumentNotValidException} ou {@link IllegalArgumentException} du service).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la catégorie ou l'événement n'est pas trouvé, ou si la catégorie n'appartient pas à l'événement (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'a pas les droits de gestion requis (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'événement est terminé/inactif, si le nouveau nom est déjà pris, ou si la nouvelle capacité est insuffisante (levé par le service via {@link IllegalStateException}).</li>
     * </ul>
     * @see CategorieService#updateCategorie(Integer, Integer, UpdateCategorieDto)
     * @see UpdateCategorieDto
     * @see GlobalView.CategorieView
     * @see IsReservation
     * @see Valid
     */
    @PutMapping("/{categorieId}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.CategorieView.class) // Définit la vue JSON pour la catégorie mise à jour retournée
    public ResponseEntity<Categorie> updateCategorie(@PathVariable Integer eventId,
                                                     @PathVariable Integer categorieId,
                                                     @Valid @RequestBody UpdateCategorieDto categorieDto) {
        // @Valid assure la validation des champs du DTO.
        Categorie updatedCategorie = categorieService.updateCategorie(eventId, categorieId, categorieDto);
        return ResponseEntity.ok(updatedCategorie); // Retourne la catégorie mise à jour avec statut 200 OK
    }

    /**
     * Supprime une catégorie existante associée à un événement.
     * <p>
     * <b>Requête:</b> DELETE /events/{eventId}/categories/{categorieId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link CategorieService#deleteCategorie}) vérifie les droits de gestion,
     * l'appartenance de la catégorie à l'événement, et le statut de l'événement.
     * </p>
     * <p>
     * <b>Règle métier:</b> La suppression est empêchée s'il existe des réservations confirmées associées à cette catégorie.
     * </p>
     *
     * @param eventId     L'identifiant unique (Integer) de l'événement parent. Provient du chemin de l'URL.
     * @param categorieId L'identifiant unique (Integer) de la catégorie à supprimer. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws EntityNotFoundException si la catégorie ou l'événement n'est pas trouvé, ou si la catégorie n'appartient pas à l'événement (levé par le service via {@link EntityNotFoundException}, -> 404).
     * @throws AccessDeniedException   si l'utilisateur n'a pas les droits de gestion requis (levé par le service via {@link AccessDeniedException}, -> 403).
     * @throws IllegalStateException   si l'événement est terminé/inactif, ou si la catégorie contient des réservations confirmées (levé par le service via {@link IllegalStateException}, -> 409).
     */
    @DeleteMapping("/{categorieId}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    public ResponseEntity<Void> deleteCategorie(@PathVariable Integer eventId, @PathVariable Integer categorieId) {
        categorieService.deleteCategorie(eventId, categorieId);
        // En cas de succès, retourne un statut 204 No Content sans corps de réponse.
        return ResponseEntity.noContent().build();
    }

    // Rappel : La gestion des exceptions spécifiques (MethodArgumentNotValidException, EntityNotFoundException,
    // AccessDeniedException, IllegalStateException, etc.) et leur mapping vers les codes HTTP
    // sont idéalement centralisés dans une classe @ControllerAdvice (GlobalExceptionHandler).
}

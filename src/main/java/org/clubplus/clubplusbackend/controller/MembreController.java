package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST gérant les opérations relatives aux entités {@link Membre} (utilisateurs).
 * Fournit des endpoints pour la gestion du profil utilisateur (lecture, mise à jour, suppression),
 * la gestion des adhésions aux clubs (rejoindre, quitter), la consultation d'informations
 * (profil d'autres membres sous conditions, liste des membres récents), et la gestion des rôles
 * par les administrateurs de club.
 * La sécurité est appliquée via des annotations personnalisées et des vérifications
 * contextuelles dans le service {@link MembreService}.
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /membres} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see Membre
 * @see MembreService
 */
@RestController
@RequestMapping("/membres") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor
@CrossOrigin // Activation de CORS si nécessaire
public class MembreController {

    /**
     * Service contenant la logique métier pour la gestion des membres et de leurs interactions.
     *
     * @see MembreService
     */
    private final MembreService membreService;

    /**
     * Service de sécurité pour obtenir l'ID de l'utilisateur courant.
     * Utilisé uniquement pour récupérer le profil de l'utilisateur courant.
     *
     * @see SecurityService
     */
    private final SecurityService securityService; // Gardé spécifiquement pour getMyProfile

    /**
     * Récupère le profil complet de l'utilisateur actuellement authentifié.
     * <p>
     * <b>Requête:</b> GET /membres/profile
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * L'accès à son propre profil est implicitement autorisé par le service.
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'objet {@link Membre} de l'utilisateur courant, sérialisé selon {@link GlobalView.ProfilView}.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'utilisateur authentifié n'est pas trouvé en base (peu probable, levé par le service via {@link EntityNotFoundException}).</li>
     * </ul>
     * @see MembreService#getMembreByIdWithSecurityCheck(Integer)
     * @see SecurityService#getCurrentUserIdOrThrow()
     * @see GlobalView.ProfilView
     * @see IsConnected
     */
    @GetMapping("/profile")
    @IsConnected
    @JsonView(GlobalView.ProfilView.class) // Vue détaillée pour son propre profil
    public ResponseEntity<Membre> getMyProfile() {
        // Récupère l'ID de l'utilisateur courant via SecurityService
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        // Appelle le service pour récupérer le membre. La vérification "owner" est gérée dans le service.
        Membre profile = membreService.getMembreByIdWithSecurityCheck(currentUserId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Récupère les détails d'un membre spécifique par son identifiant.
     * L'accès est conditionné par le rôle de l'appelant et sa relation avec le membre cible.
     * <p>
     * <b>Requête:</b> GET /membres/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link MembreService#getMembreByIdWithSecurityCheck}) vérifie si l'appelant est
     * le membre cible lui-même OU s'ils partagent un club actif commun.
     * </p>
     *
     * @param id L'identifiant unique (Integer) du membre à récupérer. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'objet {@link Membre} trouvé, sérialisé selon {@link GlobalView.MembreView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le membre avec cet ID n'est pas trouvé ou est inactif (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'appelant n'est ni le membre cible, ni membre d'un club actif commun (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see MembreService#getMembreByIdWithSecurityCheck(Integer)
     * @see GlobalView.MembreView
     * @see IsReservation
     */
    @GetMapping("/{id}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.MembreView.class) // Vue standard pour un membre
    public ResponseEntity<Membre> getMembreById(@PathVariable Integer id) {
        Membre membre = membreService.getMembreByIdWithSecurityCheck(id);
        return ResponseEntity.ok(membre);
    }

    /**
     * Met à jour les informations de profil de l'utilisateur actuellement authentifié.
     * Seuls les champs autorisés (nom, prénom, contact, etc.) sont modifiables.
     * <p>
     * <b>Requête:</b> PUT /membres/profile
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * L'action ne peut être effectuée que sur son propre profil (géré par le service).
     * </p>
     * <p>
     * <b>Validation:</b> Les données reçues dans le DTO ({@link UpdateMembreDto}) sont validées via {@link Valid @Valid}.
     * </p>
     *
     * @param updateMembreDto DTO contenant les nouvelles valeurs potentielles pour le profil. Validé par {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'objet {@link Membre} mis à jour, sérialisé selon {@link GlobalView.ProfilView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code updateMembreDto} sont invalides (levé par {@link MethodArgumentNotValidException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si le nouvel email fourni est déjà utilisé par un autre membre (levé par le service via {@link IllegalArgumentException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'utilisateur authentifié n'est pas trouvé en base (peu probable, levé par le service via {@link EntityNotFoundException}).</li>
     * </ul>
     * @see MembreService#updateMyProfile(UpdateMembreDto)
     * @see UpdateMembreDto
     * @see GlobalView.ProfilView
     * @see IsConnected
     * @see Valid
     */
    @PutMapping("/profile")
    @IsConnected
    @JsonView(GlobalView.ProfilView.class) // Vue détaillée pour son propre profil mis à jour
    public ResponseEntity<Membre> updateMyProfile(@Valid @RequestBody UpdateMembreDto updateMembreDto) {
        // @Valid -> 400 si validation échoue
        // Le service utilise l'ID courant et gère les conflits (email -> 409) et l'existence (-> 404)
        Membre updatedProfile = membreService.updateMyProfile(updateMembreDto);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Désactive (supprime logiquement) et anonymise le compte de l'utilisateur actuellement authentifié.
     * Empêche l'opération si l'utilisateur est ADMIN d'un club.
     * <p>
     * <b>Requête:</b> DELETE /membres/profile
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * L'action ne peut être effectuée que sur son propre compte.
     * </p>
     *
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws IllegalStateException   Si l'utilisateur est ADMIN et gère encore un club (levé par le service -> 409).
     * @throws EntityNotFoundException Si l'utilisateur authentifié n'est pas trouvé en base (peu probable, levé par le service -> 404).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected} -> 401).
     * @see MembreService#deleteMyAccount()
     * @see IsConnected
     */
    @DeleteMapping("/profile")
    @IsConnected
    public ResponseEntity<Void> deleteMyAccount() {
        // @ResponseStatus retiré, géré par ResponseEntity
        // Le service utilise l'ID courant et gère les règles métier (Admin de club -> 409) et l'existence (-> 404)
        membreService.deleteMyAccount();
        return ResponseEntity.noContent().build(); // Retourne 204 No Content
    }

    /**
     * Récupère l'ensemble des clubs auxquels l'utilisateur actuellement authentifié adhère.
     * <p>
     * <b>Requête:</b> GET /membres/profile/clubs
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Un {@code Set<Club>} des clubs auxquels l'utilisateur adhère, sérialisé selon {@link GlobalView.Base}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'utilisateur authentifié n'est pas trouvé en base (peu probable, levé par le service via {@link EntityNotFoundException}).</li>
     * </ul>
     * @see MembreService#findClubsForCurrentUser()
     * @see GlobalView.Base
     * @see IsConnected
     */
    @GetMapping("/profile/clubs")
    @IsConnected
    @JsonView(GlobalView.Base.class) // Vue de base pour la liste des clubs
    public ResponseEntity<Set<Club>> getMyClubs() {
        Set<Club> clubs = membreService.findClubsForCurrentUser();
        return ResponseEntity.ok(clubs);
    }

    /**
     * Permet à l'utilisateur actuellement authentifié (avec rôle MEMBRE) de rejoindre un club via son code unique.
     * <p>
     * <b>Requête:</b> POST /membres/profile/join?codeClub={codeClub}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * Le service vérifie que l'utilisateur a le rôle MEMBRE.
     * </p>
     *
     * @param codeClub Le code unique (String) du club à rejoindre. Fourni comme paramètre de requête.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> L'objet {@link Adhesion} nouvellement créé, sérialisé selon {@link GlobalView.Base}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club avec ce code n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'utilisateur est déjà membre de ce club ou s'il a un rôle ADMIN/RESERVATION (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected}).</li>
     * </ul>
     * @see MembreService#joinClub(String)
     * @see GlobalView.Base
     * @see IsConnected
     */
    @PostMapping("/profile/join")
    @IsConnected
    @JsonView(GlobalView.Base.class) // Retourne l'adhésion créée
    public ResponseEntity<Adhesion> joinClub(@RequestParam String codeClub) {
        // @ResponseStatus retiré, géré par ResponseEntity
        Adhesion adhesion = membreService.joinClub(codeClub);
        // Retourne 201 Created avec l'adhésion dans le corps
        return ResponseEntity.status(HttpStatus.CREATED).body(adhesion);
    }

    /**
     * Permet à l'utilisateur actuellement authentifié (avec rôle MEMBRE) de quitter un club dont il est membre.
     * <p>
     * <b>Requête:</b> DELETE /membres/profile/leave/{clubId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * Le service vérifie que l'utilisateur a le rôle MEMBRE et qu'il est bien membre du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique (Integer) du club à quitter. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws EntityNotFoundException Si l'adhésion de l'utilisateur à ce club n'est pas trouvée (levé par le service -> 404).
     * @throws IllegalStateException   Si l'utilisateur a un rôle ADMIN ou RESERVATION (levé par le service -> 409).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected} -> 401).
     * @see MembreService#leaveClub(Integer)
     * @see IsConnected
     */
    @DeleteMapping("/profile/leave/{clubId}")
    @IsConnected
    public ResponseEntity<Void> leaveClub(@PathVariable Integer clubId) {
        // @ResponseStatus retiré, géré par ResponseEntity
        membreService.leaveClub(clubId);
        return ResponseEntity.noContent().build(); // Retourne 204 No Content
    }

    // --- Endpoints pour Admin de Club (Gestion Rôles) ---

    /**
     * Modifie le rôle (MEMBRE <-> RESERVATION) d'un membre spécifique au sein d'un club donné.
     * Nécessite que l'appelant soit administrateur (ADMIN) du club concerné.
     * <p>
     * <b>Requête:</b> PUT /membres/{membreId}/role?clubId={clubId}&newRole={newRole}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert le rôle ADMIN globalement ({@link IsAdmin @IsAdmin}).
     * Le service ({@link MembreService#changeMemberRoleInClub}) vérifie en plus que l'appelant
     * est bien l'ADMIN *spécifique* du club {@code clubId}.
     * </p>
     *
     * @param membreId L'ID (Integer) du membre dont le rôle doit être modifié. Provient du chemin de l'URL.
     * @param clubId   L'ID (Integer) du club dans lequel le changement s'applique. Fourni comme paramètre de requête.
     * @param newRole  Le nouveau rôle souhaité ({@link Role#MEMBRE} ou {@link Role#RESERVATION}). Fourni comme paramètre de requête.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Le {@link Membre} mis à jour avec son nouveau rôle, sérialisé selon {@link GlobalView.MembreView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si le {@code newRole} est invalide ou si l'admin tente de changer son propre rôle (levé par le service via {@link IllegalArgumentException}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le membre cible n'existe pas, ou s'il n'appartient pas au club spécifié (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'appelant n'est pas l'administrateur du club {@code clubId} (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si la transition de rôle est invalide (ex: changer rôle d'un autre ADMIN), si le membre a déjà le rôle cible,
     *         ou si un membre multi-clubs est promu RESERVATION (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle ADMIN (géré par Spring Security / {@code @IsAdmin}).</li>
     * </ul>
     * @see MembreService#changeMemberRoleInClub(Integer, Integer, Role)
     * @see GlobalView.MembreView
     * @see IsAdmin
     */
    @PutMapping("/{membreId}/role")
    @IsAdmin // Requiert le rôle ADMIN global
    @JsonView(GlobalView.MembreView.class) // Vue standard pour le membre mis à jour
    public ResponseEntity<Membre> changeMemberRoleInClub(@PathVariable Integer membreId,
                                                         @RequestParam Integer clubId,
                                                         @RequestParam Role newRole) {
        // Le service gère toutes les vérifications (droits admin club -> 403, existence -> 404, règles métier -> 400/409)
        Membre updatedMembre = membreService.changeMemberRoleInClub(membreId, clubId, newRole);
        return ResponseEntity.ok(updatedMembre);
    }

    /**
     * Récupère les 5 derniers membres actifs inscrits dans le club géré par l'utilisateur connecté.
     * <p>
     * <b>Requête:</b> GET /membres/managed-club/latest
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link MembreService#getLatestMembersForManagedClub}) identifie le club géré
     * par l'utilisateur et vérifie son rôle.
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Membre>} (jusqu'à 5) des derniers membres actifs,
     *         sérialisée selon {@link GlobalView.MembreView}. Peut être vide.</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire (RESERVATION ou ADMIN) d'un club
     *         (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis
     *         (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see MembreService#getLatestMembersForManagedClub()
     * @see GlobalView.MembreView
     * @see IsReservation
     */
    @GetMapping("/managed-club/latest") // Chemin plus explicite
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.MembreView.class) // Vue standard pour la liste des membres
    public ResponseEntity<List<Membre>> getLatestMembersForMyClub() {
        // Le service identifie le club géré, vérifie les droits, et récupère les données.
        List<Membre> latestMembers = membreService.getLatestMembersForManagedClub();
        return ResponseEntity.ok(latestMembers);
    }

    // Rappel : La gestion centralisée des exceptions via @ControllerAdvice est essentielle
    // pour mapper les exceptions (EntityNotFoundException, AccessDeniedException, IllegalStateException,
    // IllegalArgumentException, MethodArgumentNotValidException) aux bonnes réponses HTTP (404, 403, 409, 400).
}

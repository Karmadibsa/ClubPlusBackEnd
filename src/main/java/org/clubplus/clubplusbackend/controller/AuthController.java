package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.HomepageStatsDTO;
import org.clubplus.clubplusbackend.dto.LoginRequestDto;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.SecurityUtils;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.service.StatsService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Contrôleur REST gérant les points d'accès publics relatifs à l'authentification
 * et à l'inscription des utilisateurs (membres).
 * Fournit des endpoints pour l'inscription, la connexion (génération de token JWT)
 * et la récupération de statistiques publiques pour la page d'accueil.
 * <p>
 * Le chemin de base pour tous les endpoints de ce contrôleur est {@code /api/auth}.
 * </p>
 * <p>
 * Ce contrôleur ne nécessite aucune authentification préalable pour accéder à ses endpoints.
 * </p>
 */
@RestController
@RequestMapping("/auth") // Point d'entrée public pour l'authentification et actions associées
@RequiredArgsConstructor // Injection des dépendances finales via le constructeur (Lombok)
public class AuthController {

    private final String frontendEmailVerifiedUrl = "http://localhost:4200/connexion"; // À configurer
    private final String frontendEmailVerificationFailedUrl = "http://localhost:4200/connexion"; // À configurer

    /**
     * Service pour la logique métier liée aux clubs.
     *
     * @see ClubService
     */
    private final ClubService clubService;

    /**
     * Service pour gérer la logique métier liée aux membres, notamment l'inscription.
     *
     * @see MembreService
     */
    private final MembreService membreService;

    /**
     * Service pour récupérer les statistiques générales (ex: pour la page d'accueil).
     *
     * @see StatsService
     */
    private final StatsService statsService;

    /**
     * Fournisseur d'authentification de Spring Security pour valider les identifiants utilisateur lors de la connexion.
     * Il est configuré pour utiliser un {@code UserDetailsService} (probablement {@code AppUserDetailService})
     * et un {@code PasswordEncoder} (probablement {@code BCryptPasswordEncoder}).
     */
    private final AuthenticationProvider authenticationProvider;

    /**
     * Utilitaire pour la génération et la validation des tokens JWT (JSON Web Tokens).
     * Utilisé après une connexion réussie pour créer le token d'authentification.
     *
     * @see SecurityUtils
     */
    private final SecurityUtils jwtUtils; // Nommé jwtUtils pour clarifier son rôle avec les JWT

    /**
     * Endpoint public pour inscrire un nouveau membre et l'associer automatiquement à un club via son code.
     * <p>
     * <b>Requête:</b> POST /api/auth/inscription?codeClub={codeClub}
     * </p>
     * <p>
     * <b>Rôles requis:</b> Aucun (Public)
     * </p>
     * <p>
     * <b>Corps de la requête:</b> {@link Membre} (données initiales: nom, prénom, email, mot de passe brut, etc.) - Validé par {@code @Valid}.
     * </p>
     * <p>
     * <b>Paramètre de requête:</b> {@code codeClub} (String, obligatoire) - Le code unique du club à rejoindre.
     * </p>
     *
     * @param membre   L'objet {@link Membre} contenant les informations du nouvel utilisateur, extrait du corps JSON de la requête.
     *                 Les données sont validées selon les contraintes définies dans la classe {@code Membre}.
     *                 Certains champs (comme le mot de passe) ne seront pas inclus dans la réponse JSON grâce à la configuration de l'entité ou {@code @JsonView}.
     * @param codeClub Le code unique du club auquel le membre doit être ajouté. Fourni comme paramètre de requête obligatoire.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> Le {@link Membre} nouvellement créé et persisté, sérialisé selon la vue {@link GlobalView.MembreView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données fournies dans l'objet {@code membre} sont invalides (ex: email mal formé, champ manquant) - Lancé par {@link MethodArgumentNotValidException}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le {@code codeClub} fourni ne correspond à aucun club existant - Lancé par {@link EntityNotFoundException} depuis {@code MembreService}.</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'email fourni dans l'objet {@code membre} est déjà utilisé par un autre compte - Lancé par {@link IllegalArgumentException} depuis {@code MembreService}.</li>
     * </ul>
     * (Note: La gestion des exceptions et la transformation en codes HTTP est typiquement assurée par un {@code @ControllerAdvice} global).
     * @see MembreService#registerMembreAndJoinClub(Membre, String)
     * @see GlobalView.MembreView
     */
    @PostMapping("/membre/inscription")
    @JsonView(GlobalView.MembreView.class) // Applique une vue JSON pour filtrer les champs retournés
    public ResponseEntity<Membre> inscription(
            @Valid @RequestBody Membre membre, // Valide l'objet Membre reçu dans le corps de la requête
            @RequestParam String codeClub) {    // Récupère le code du club depuis les paramètres de l'URL
        // Délègue la logique d'inscription et d'ajout au club au service métier.
        // Les exceptions métier (ClubNotFound via EntityNotFoundException, EmailExists via IllegalArgumentException)
        // sont propagées et devraient être gérées globalement par un ControllerAdvice.
        Membre newMembre = membreService.registerMembreAndJoinClub(membre, codeClub);
        // Retourne le membre créé avec le statut HTTP 201 Created.
        return ResponseEntity.status(HttpStatus.CREATED).body(newMembre);
    }

    /**
     * Endpoint public pour authentifier un utilisateur existant via son email et mot de passe.
     * Si l'authentification réussit, un token JWT (JSON Web Token) est généré et retourné.
     * <p>
     * <b>Requête:</b> POST /api/auth/connexion
     * </p>
     * <p>
     * <b>Rôles requis:</b> Aucun (Public)
     * </p>
     * <p>
     * <b>Corps de la requête:</b> {@link LoginRequestDto} (email, password) - Validé par {@code @Valid}.
     * </p>
     *
     * @param loginRequest DTO {@link LoginRequestDto} contenant l'email et le mot de passe de l'utilisateur.
     *                     Les données sont validées selon les contraintes du DTO.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Le token JWT sous forme de {@code String} (Content-Type: text/plain) dans le corps de la réponse.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données fournies dans {@code loginRequest} sont invalides (ex: email mal formé, mot de passe manquant) - Lancé par {@link MethodArgumentNotValidException}.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si les identifiants (email/mot de passe) sont incorrects ou si le compte est inactif/bloqué - Lancé par {@link AuthenticationException} depuis {@code AuthenticationProvider}.</li>
     * </ul>
     * (Note: La gestion des exceptions est typiquement assurée par Spring Security et un {@code @ControllerAdvice} global).
     * @see AuthenticationProvider#authenticate(Authentication)
     * @see SecurityUtils#generateToken(AppUserDetails)
     * @see LoginRequestDto
     */
    @PostMapping("/connexion")
    public ResponseEntity<String> connexion(
            @Valid @RequestBody LoginRequestDto loginRequest // Valide le DTO de connexion
    ) {
        // Tente d'authentifier l'utilisateur avec les identifiants fournis.
        // L'AuthenticationProvider configuré (utilisant AppUserDetailService et PasswordEncoder)
        // lèvera une AuthenticationException (ou une sous-classe) si les identifiants sont invalides.
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Si l'authentification réussit, on récupère les détails de l'utilisateur authentifié (contenant ID, rôle, etc.).
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();

        // Génère le token JWT basé sur les détails de l'utilisateur authentifié.
        String jwtToken = jwtUtils.generateToken(userDetails);

        // Prépare les en-têtes de la réponse pour indiquer le type de contenu texte.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN); // Spécifie que le corps est du texte brut

        // Retourne le token JWT dans le corps de la réponse avec le statut OK.
        return new ResponseEntity<>(jwtToken, headers, HttpStatus.OK);
        // Alternative plus concise si pas besoin de header spécifique:
        // return ResponseEntity.ok(jwtToken); // Le Content-Type sera déduit (souvent text/plain par défaut pour String)
    }

    /**
     * Endpoint public pour récupérer des statistiques agrégées destinées à la page d'accueil publique.
     * Ces statistiques incluent généralement des compteurs globaux (nombre de clubs, membres, événements).
     * <p>
     * <b>Requête:</b> GET /api/auth/stats
     * </p>
     * <p>
     * <b>Rôles requis:</b> Aucun (Public)
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Un objet {@link HomepageStatsDTO} avec les statistiques agrégées dans le corps de la réponse.</li>
     *     <li><b>Erreur (500 Internal Server Error):</b> En cas d'erreur inattendue lors de l'accès aux données (très peu probable pour de simples comptages).</li>
     * </ul>
     * @see StatsService#getHomepageStats()
     * @see HomepageStatsDTO
     */
    @GetMapping("/stats")
    public ResponseEntity<HomepageStatsDTO> getStats() {
        // Délègue la récupération des statistiques au service dédié.
        HomepageStatsDTO stats = statsService.getHomepageStats();
        // Retourne les statistiques avec le statut OK (implicite si on retourne directement le DTO, mais ResponseEntity est plus explicite).
        return ResponseEntity.ok(stats);
    }

    /**
     * Crée un nouveau club ainsi que son membre administrateur initial.
     * <p>
     * <b>Requête:</b> POST /clubs/inscription
     * </p>
     * <p>
     * <b>Sécurité:</b> Aucune annotation de sécurité spécifique ici. L'accès est potentiellement
     * public ou restreint par la configuration globale de Spring Security.
     * </p>
     * <p>
     * <b>Validation:</b> Les données reçues dans le DTO ({@link CreateClubRequestDto}) sont validées via {@link Valid @Valid}.
     * </p>
     *
     * @param creationDto Un DTO contenant les infos du club et de l'admin initial. Validé par {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> Le {@link Club} nouvellement créé, sérialisé selon {@link GlobalView.ClubView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code creationDto} sont invalides (levé par {@link MethodArgumentNotValidException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'email fourni pour l'admin ou le club est déjà utilisé (levé par le service via {@link IllegalArgumentException}).</li>
     * </ul>
     * @see ClubService#createClubAndRegisterAdmin(CreateClubRequestDto)
     * @see CreateClubRequestDto
     * @see GlobalView.ClubView
     * @see Valid
     */
    @PostMapping("/club/inscription")
    // @ResponseStatus retiré, géré par ResponseEntity
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club créé
    public ResponseEntity<Club> createClubAndAdmin(
            @Valid @RequestBody CreateClubRequestDto creationDto // Valide le DTO reçu
    ) {
        // @Valid gère la validation -> 400 si échec.
        // Le service gère la logique de création et les conflits potentiels (ex: email) -> 409.
        Club newClub = clubService.createClubAndRegisterAdmin(creationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newClub);
    }


    @GetMapping("/verify-email")
    public RedirectView verifyEmail(@RequestParam("token") String token) {
        try {
            boolean isVerified = membreService.verifyUserToken(token); // Cette méthode doit être créée dans MembreService

            if (isVerified) {
                // Redirection vers une page de succès de votre frontend Angular
                return new RedirectView(frontendEmailVerifiedUrl);
            } else {
                // Redirection vers une page d'échec (token invalide/expiré ou utilisateur déjà vérifié)
                return new RedirectView(frontendEmailVerificationFailedUrl);
            }
        } catch (Exception e) {
            // Gérer les erreurs inattendues
            // Log l'erreur
            System.err.println("Erreur lors de la vérification de l'email : " + e.getMessage());
            return new RedirectView(frontendEmailVerificationFailedUrl);
        }
    }
}

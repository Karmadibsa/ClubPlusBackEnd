package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.clubplus.clubplusbackend.dto.*;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.SecurityUtils;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.service.StatsService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Contrôleur REST gérant les points d'accès publics relatifs à l'authentification
 * et à l'inscription des utilisateurs.
 * <p>
 * Base URL: /auth
 * </p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Value("${APP_FRONTEND_BASE_URL}")
    private String frontendBaseUrl;

    private String frontendEmailVerifiedSuccessPage;
    private String frontendEmailVerifiedFailurePage;

    private final ClubService clubService;
    private final MembreService membreService;
    private final StatsService statsService;
    private final AuthenticationProvider authenticationProvider;
    private final SecurityUtils jwtUtils;

    public AuthController(ClubService clubService, MembreService membreService, StatsService statsService, AuthenticationProvider authenticationProvider, SecurityUtils jwtUtils) {
        this.clubService = clubService;
        this.membreService = membreService;
        this.statsService = statsService;
        this.authenticationProvider = authenticationProvider;
        this.jwtUtils = jwtUtils;
        logger.info("AuthController a été initialisé.");
    }

    /**
     * Initialise les URLs de redirection vers le frontend après le chargement des propriétés.
     * Utilisé pour la redirection après la vérification de l'email.
     */
    @PostConstruct
    public void initUrls() {
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            logger.error("'APP_FRONTEND_BASE_URL' n'est pas configuré. Les redirections après vérification d'email risquent d'échouer.");
            // Initialisation à des valeurs par défaut pour éviter les NullPointerException
            this.frontendEmailVerifiedSuccessPage = "";
            this.frontendEmailVerifiedFailurePage = "";
        } else {
            this.frontendEmailVerifiedSuccessPage = frontendBaseUrl + "/connexion";
            this.frontendEmailVerifiedFailurePage = frontendBaseUrl + "/accueil";
        }
        logger.info("URLs de redirection du frontend initialisées : Succès='{}', Échec='{}'", frontendEmailVerifiedSuccessPage, frontendEmailVerifiedFailurePage);
    }

    /**
     * Inscrit un nouveau membre et l'associe à un club via son code.
     * <p>
     * Endpoint: POST /auth/membre/inscription
     *
     * @param membre   Les informations du nouveau membre.
     * @param codeClub Le code unique du club à rejoindre.
     * @return Le membre nouvellement créé (201 Created).
     */
    @PostMapping("/membre/inscription")
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> inscription(
            @Valid @RequestBody Membre membre,
            @RequestParam String codeClub) {
        // La logique métier est entièrement déléguée au service.
        // Les exceptions (ex: club non trouvé, email déjà utilisé) sont gérées par le GlobalExceptionHandler.
        Membre newMembre = membreService.registerMembreAndJoinClub(membre, codeClub);
        return ResponseEntity.status(HttpStatus.CREATED).body(newMembre);
    }

    /**
     * Crée un nouveau club ainsi que son membre administrateur initial.
     * <p>
     * Endpoint: POST /auth/club/inscription
     *
     * @param creationDto Un DTO contenant les informations du club et de l'admin.
     * @return Le club nouvellement créé (201 Created).
     */
    @PostMapping("/club/inscription")
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> createClubAndAdmin(
            @Valid @RequestBody CreateClubRequestDto creationDto) {
        Club newClub = clubService.createClubAndRegisterAdmin(creationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newClub);
    }

    /**
     * Authentifie un utilisateur et retourne un token JWT en cas de succès.
     * <p>
     * Endpoint: POST /auth/connexion
     *
     * @param loginRequest DTO avec l'email et le mot de passe.
     * @return Le token JWT (200 OK).
     */
    @PostMapping("/connexion")
    public ResponseEntity<String> connexion(
            @Valid @RequestBody LoginRequestDto loginRequest) {
        // Tente l'authentification via le provider de Spring Security
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        // Si l'authentification réussit, on génère le token
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(jwtToken);
    }

    /**
     * Récupère des statistiques agrégées pour la page d'accueil publique.
     * <p>
     * Endpoint: GET /auth/stats
     *
     * @return Un DTO avec les statistiques (200 OK).
     */
    @GetMapping("/stats")
    public ResponseEntity<HomepageStatsDTO> getStats() {
        HomepageStatsDTO stats = statsService.getHomepageStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Vérifie le token d'activation de compte envoyé par email.
     *
     * @param token Le token à vérifier.
     * @return Une redirection vers la page de succès ou d'échec du frontend.
     */
    @GetMapping("/verify-email")
    public RedirectView verifyEmail(@RequestParam("token") String token) {
        logger.info("Vérification de l'email avec le token : {}", token);
        try {
            boolean isVerified = membreService.verifyUserValidationToken(token);
            if (isVerified) {
                logger.info("Vérification d'email réussie pour le token : {}", token);
                return new RedirectView(frontendEmailVerifiedSuccessPage);
            } else {
                logger.warn("Échec de la vérification d'email (token invalide ou expiré) : {}", token);
                return new RedirectView(frontendEmailVerifiedFailurePage);
            }
        } catch (Exception e) {
            logger.error("Erreur durant la vérification du token d'email {}: {}", token, e.getMessage(), e);
            return new RedirectView(frontendEmailVerifiedFailurePage);
        }
    }

    /**
     * Déclenche une demande de réinitialisation de mot de passe par email.
     *
     * @param email L'email de l'utilisateur.
     * @return Un message de confirmation générique (200 OK).
     */
    @PostMapping("/mail-password-reset")
    public ResponseEntity<String> requestPasswordReset(@RequestParam("email") String email) {
        try {
            membreService.requestPasswordReset(email);
            return ResponseEntity.ok("Si un compte est associé à cet email, un lien de réinitialisation a été envoyé.");
        } catch (MessagingException e) {
            logger.error("Échec de l'envoi de l'email de réinitialisation pour {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'envoi de l'email.");
        }
    }

    /**
     * Réinitialise le mot de passe de l'utilisateur à partir d'un token.
     *
     * @param payload Contient le token et le nouveau mot de passe.
     * @return Un message de succès (200 OK) ou d'erreur (400 Bad Request).
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordPayload payload) {
        try {
            membreService.resetPassword(payload.getToken(), payload.getNewPassword());
            return ResponseEntity.ok("Votre mot de passe a été réinitialisé avec succès.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Valide un token de réinitialisation de mot de passe.
     *
     * @param token Le token à valider.
     * @return 200 OK si le token est valide, 401 Unauthorized sinon.
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<Void> validateResetToken(@RequestParam("token") String token) {
        boolean valid = membreService.verifyUserResetToken(token);
        if (valid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Permet à un utilisateur connecté de changer son propre mot de passe.
     *
     * @param authentication L'objet d'authentification de Spring.
     * @param payload        Contient le mot de passe actuel et le nouveau.
     * @return Un message de succès (200 OK) ou d'erreur.
     */
    @IsConnected
    @PostMapping("/change-password")
    public ResponseEntity<String> changePasswordConnected(Authentication authentication, @Valid @RequestBody ChangePasswordConnectedPayload payload) {
        String userEmail = authentication.getName();
        try {
            membreService.changePasswordForAuthenticatedUser(userEmail, payload.getCurrentPassword(), payload.getNewPassword());
            return ResponseEntity.ok("Mot de passe changé avec succès.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur lors du changement de mot de passe pour l'utilisateur {}", userEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur interne est survenue.");
        }
    }
}

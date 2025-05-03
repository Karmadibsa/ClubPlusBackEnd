package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.HomepageStatsDTO;
import org.clubplus.clubplusbackend.dto.LoginRequestDto;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST gérant les points d'accès publics relatifs à l'authentification
 * et à l'inscription des utilisateurs (membres).
 * Fournit des endpoints pour l'inscription, la connexion (génération de token JWT)
 * et la récupération de statistiques publiques pour la page d'accueil.
 * Le chemin de base pour tous les endpoints de ce contrôleur est "/api/auth".
 */
@RestController
@RequestMapping("/api/auth") // Point d'entrée public pour l'authentification et actions associées
@RequiredArgsConstructor // Injection des dépendances finales via le constructeur (Lombok)
public class AuthController {

    /**
     * Service pour gérer la logique métier liée aux membres (inscription, recherche, etc.).
     */
    private final MembreService membreService;

    /**
     * Service pour récupérer les statistiques générales (ex: pour la page d'accueil).
     */
    private final StatsService statsService;

    /**
     * Fournisseur d'authentification de Spring Security pour valider les identifiants utilisateur.
     */
    private final AuthenticationProvider authenticationProvider;

    /**
     * Utilitaire pour la génération et la validation des tokens JWT.
     */
    private final SecurityUtils jwtUtils; // Nommé jwtUtils pour clarifier son rôle avec les JWT

    /**
     * Gère la requête POST pour inscrire un nouveau membre et l'associer à un club spécifié par son code.
     * Cet endpoint est public. Les validations métier (existence du club, unicité de l'email)
     * sont effectuées au niveau du {@link MembreService}.
     * Renvoie une vue JSON spécifique du membre créé.
     *
     * @param membre   L'objet {@link Membre} contenant les informations du nouvel utilisateur.
     *                 Les données sont validées selon les contraintes définies dans la classe {@code Membre} (via {@code @Valid}).
     *                 Le mot de passe ne sera pas inclus dans la réponse JSON si annoté correctement (ex: {@code @JsonProperty(access = WRITE_ONLY)}).
     * @param codeClub Le code unique du club auquel le membre doit être ajouté. Fourni comme paramètre de requête obligatoire.
     * @return Le {@link Membre} nouvellement créé et persisté, sérialisé selon la vue {@link GlobalView.MembreView}.
     * Retourne un statut HTTP 201 (Created) en cas de succès.
     * @throws org.springframework.web.bind.MethodArgumentNotValidException       Si les données du {@code membre} ne sont pas valides (géré globalement -> 400 Bad Request).
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException       Si le {@code codeClub} ne correspond à aucun club existant (géré globalement -> 404 Not Found).
     * @throws org.clubplus.clubplusbackend.exception.EmailAlreadyExistsException Si l'email fourni est déjà utilisé par un autre membre (géré globalement -> 409 Conflict).
     * @see MembreService#registerMembreAndJoinClub(Membre, String)
     * @see GlobalView.MembreView
     */
    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED) // Réponse HTTP 201 en cas de succès de la création
    @JsonView(GlobalView.MembreView.class) // Applique une vue JSON pour filtrer les champs retournés
    public Membre inscription(
            @Valid @RequestBody Membre membre, // Valide l'objet Membre reçu dans le corps de la requête
            @RequestParam String codeClub) {    // Récupère le code du club depuis les paramètres de l'URL
        // Délègue la logique d'inscription et d'ajout au club au service métier.
        // Les exceptions métier (ClubNotFound, EmailExists) sont propagées et gérées globalement.
        return membreService.registerMembreAndJoinClub(membre, codeClub);
    }

    /**
     * Gère la requête POST pour authentifier un utilisateur existant via son email et mot de passe.
     * Si l'authentification réussit, un token JWT est généré et retourné dans le corps de la réponse
     * en tant que texte brut.
     * Cet endpoint est public.
     *
     * @param loginRequest DTO {@link LoginRequestDto} contenant l'email et le mot de passe de l'utilisateur.
     *                     Les données sont validées via {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant le token JWT sous forme de {@code String} (Content-Type: text/plain)
     * avec un statut HTTP 200 (OK) si l'authentification est réussie.
     * @throws org.springframework.security.core.AuthenticationException    Si les identifiants (email/mot de passe) sont incorrects
     *                                                                      (géré globalement via Spring Security -> 401 Unauthorized).
     * @throws org.springframework.web.bind.MethodArgumentNotValidException Si les données de connexion dans {@code loginRequest}
     *                                                                      ne respectent pas les contraintes de validation (géré globalement -> 400 Bad Request).
     * @see AuthenticationProvider#authenticate(Authentication)
     * @see SecurityUtils#generateToken(AppUserDetails)
     * @see LoginRequestDto
     */
    @PostMapping("/connexion")
    // Pas besoin de @ResponseStatus(OK), c'est le défaut pour une réponse avec ResponseEntity OK.
    public ResponseEntity<String> connexion(
            @Valid @RequestBody LoginRequestDto loginRequest // Valide le DTO de connexion
    ) {
        // Tente d'authentifier l'utilisateur avec les identifiants fournis.
        // L'AuthenticationProvider configuré (utilisant AppUserDetailService et PasswordEncoder)
        // lèvera une AuthenticationException si les identifiants sont invalides.
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Si l'authentification réussit, on récupère les détails de l'utilisateur authentifié.
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();

        // Génère le token JWT basé sur les détails de l'utilisateur authentifié.
        String jwtToken = jwtUtils.generateToken(userDetails);

        // Prépare les en-têtes de la réponse pour indiquer le type de contenu.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN); // Spécifie que le corps est du texte brut

        // Retourne le token JWT dans le corps de la réponse avec le statut OK.
        return new ResponseEntity<>(jwtToken, headers, HttpStatus.OK);
    }

    /**
     * Gère la requête GET pour récupérer des statistiques agrégées destinées à la page d'accueil publique.
     * Ces statistiques peuvent inclure des compteurs globaux (nombre de clubs, membres, etc.).
     * Cet endpoint est public.
     *
     * @return Un objet {@link HomepageStatsDTO} contenant les statistiques agrégées.
     * Retourne un statut HTTP 200 (OK) par défaut en cas de succès.
     * @see StatsService#getHomepageStats()
     * @see HomepageStatsDTO
     */
    @GetMapping("/stats")
    public HomepageStatsDTO getStats() {
        // Délègue la récupération des statistiques au service dédié.
        return statsService.getHomepageStats();
    }
}

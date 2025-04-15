package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.LoginRequestDto;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.SecurityUtils;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Point d'entrée public pour l'authentification
@RequiredArgsConstructor
// @CrossOrigin // Décommentez si nécessaire
public class AuthController {

    private final MembreService membreService;
    private final AuthenticationProvider authenticationProvider; // Pour gérer l'authentification
    private final SecurityUtils jwtUtils; // Pour générer le token

    /**
     * POST /api/auth/inscription?codeClub={codeClub}
     * Inscrit un nouveau membre et l'ajoute à un club.
     * Endpoint public.
     * Sécurité: Aucune (vérifications métier dans le service).
     * Validation: Données du membre validées via @Valid.
     * Exceptions (globales): 400 (Validation), 404 (Club non trouvé), 409 (Email existe).
     */
    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED) // Code 201 si succès
    @JsonView(GlobalView.MembreView.class) // Vue pour le membre retourné
    public Membre inscription(
            @Valid @RequestBody Membre membre, // Valide les contraintes sur l'entité Membre
            @RequestParam String codeClub) {
        // Appelle directement le service. Pas de try-catch.
        // MembreService.registerMembreAndJoinClub lance les exceptions appropriées.
        // Assurez-vous que Membre.password a @JsonProperty(access = WRITE_ONLY).
        return membreService.registerMembreAndJoinClub(membre, codeClub);
    }

    /**
     * POST /api/auth/connexion
     * Authentifie un utilisateur et retourne un token JWT.
     * Endpoint public.
     * Sécurité: Gérée par AuthenticationProvider.
     * Validation: Données de connexion validées via @Valid sur LoginRequestDto.
     * Exceptions (globales): 400 (Validation), 401 (Mauvais identifiants).
     */
    @PostMapping("/connexion")
    // Pas besoin de @ResponseStatus(OK) car c'est le défaut pour POST/PUT/GET sans erreur
    public Map<String, String> connexion(
            @Valid @RequestBody LoginRequestDto loginRequest // Utilise le DTO dédié et @Valid
    ) {
        // Tente l'authentification via Spring Security.
        // authenticationProvider utilise AppUserDetailService et PasswordEncoder.
        // Si l'authentification échoue (mauvais email/mdp), il lance AuthenticationException.
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Si l'authentification réussit, le principal est notre AppUserDetails.
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();

        // Générer le token JWT.
        String jwtToken = jwtUtils.generateToken(userDetails);

        // Retourner le token dans une structure JSON simple.
        return Map.of("token", jwtToken);
    }

}

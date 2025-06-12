package org.clubplus.clubplusbackend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre Spring Security qui intercepte chaque requête pour valider un token JWT.
 * <p>
 * Ce filtre recherche un token dans l'en-tête "Authorization", le valide, et si le token est correct,
 * il authentifie l'utilisateur pour la durée de la requête en configurant le contexte de sécurité de Spring.
 * Il s'assure d'être exécuté une seule fois par requête.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final SecurityUtils jwtUtils;
    private final AppUserDetailService appUserDetailService;

    /**
     * Construit le filtre JWT avec les dépendances nécessaires.
     *
     * @param jwtUtils             L'utilitaire pour la manipulation des tokens JWT.
     * @param appUserDetailService Le service pour charger les détails de l'utilisateur.
     */
    @Autowired
    public JwtFilter(SecurityUtils jwtUtils, AppUserDetailService appUserDetailService) {
        this.jwtUtils = jwtUtils;
        this.appUserDetailService = appUserDetailService;
    }

    /**
     * Méthode principale du filtre, appliquée à chaque requête HTTP.
     * <p>
     * Elle extrait le token JWT de l'en-tête "Authorization". Si le token est présent et valide,
     * elle charge les informations de l'utilisateur et met à jour le contexte de sécurité de Spring.
     * La requête poursuit ensuite son chemin dans la chaîne de filtres, que l'authentification
     * ait réussi ou non.
     *
     * @param request     La requête HTTP.
     * @param response    La réponse HTTP.
     * @param filterChain La chaîne de filtres.
     * @throws ServletException en cas d'erreur de servlet.
     * @throws IOException      en cas d'erreur d'I/O.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Si l'en-tête est absent ou ne commence pas par "Bearer ", on passe au filtre suivant.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrait le token en retirant le préfixe "Bearer ".
        jwt = authHeader.substring(7);

        try {
            // Valide le token et en extrait l'email (le "sujet").
            userEmail = jwtUtils.getSubjectFromJwt(jwt);

            // Si l'email est extrait et que l'utilisateur n'est pas déjà authentifié dans le contexte actuel...
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // On charge les détails de l'utilisateur depuis la base de données.
                UserDetails userDetails = this.appUserDetailService.loadUserByUsername(userEmail);

                // On crée un objet d'authentification pour Spring Security.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Pas de mot de passe ici, l'authentification est basée sur le token.
                        userDetails.getAuthorities()
                );

                // On enrichit l'objet d'authentification avec les détails de la requête (IP, etc.).
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // On place l'objet d'authentification dans le contexte de sécurité.
                // L'utilisateur est maintenant considéré comme authentifié pour cette requête.
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Utilisateur '{}' authentifié avec succès via JWT.", userEmail);
            }
        } catch (ExpiredJwtException e) {
            log.warn("Tentative d'accès avec un token JWT expiré: {}", e.getMessage());
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            log.warn("Échec de la validation du token JWT : {}", e.getMessage());
        }

        // On passe la main au filtre suivant dans tous les cas.
        filterChain.doFilter(request, response);
    }
}

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
 * Filtre Spring Security exécuté une fois par requête pour traiter les tokens JWT (JSON Web Tokens).
 * Ce filtre intercepte les requêtes entrantes, recherche un token JWT dans l'en-tête "Authorization",
 * le valide, et si le token est valide, charge les détails de l'utilisateur correspondant
 * et configure le contexte de sécurité de Spring ({@link SecurityContextHolder}).
 * Cela permet aux mécanismes d'autorisation ultérieurs (ex: {@code @PreAuthorize}) de fonctionner
 * correctement pour les requêtes authentifiées via JWT.
 *
 * <p>Hérite de {@link OncePerRequestFilter} pour garantir une exécution unique par requête.</p>
 *
 * @see OncePerRequestFilter
 * @see SecurityUtils
 * @see AppUserDetailService
 * @see SecurityContextHolder
 * @see UsernamePasswordAuthenticationToken
 */
@Component // Marque cette classe comme un bean Spring, la rendant détectable et gérable par le conteneur.
public class JwtFilter extends OncePerRequestFilter {

    // Logger SLF4j pour enregistrer les informations et erreurs liées au traitement JWT.
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    /**
     * Utilitaire pour la validation et l'extraction d'informations à partir des tokens JWT.
     *
     * @see SecurityUtils
     */
    private final SecurityUtils jwtUtils; // Déclaré final pour l'immutabilité après construction

    /**
     * Service pour charger les {@link UserDetails} à partir de l'identifiant (email) extrait du token JWT.
     *
     * @see AppUserDetailService
     */
    private final AppUserDetailService appUserDetailService;

    /**
     * Construit le filtre JWT avec les dépendances nécessaires injectées par Spring.
     *
     * @param jwtUtils             L'instance de {@link SecurityUtils} pour la manipulation des JWT.
     * @param appUserDetailService L'instance de {@link AppUserDetailService} pour charger les utilisateurs.
     */
    @Autowired
    public JwtFilter(SecurityUtils jwtUtils, AppUserDetailService appUserDetailService) {
        this.jwtUtils = jwtUtils;
        this.appUserDetailService = appUserDetailService;
    }

    /**
     * Méthode principale du filtre, exécutée pour chaque requête entrante.
     * Tente d'extraire et de valider un token JWT de l'en-tête "Authorization".
     * Si un token valide est trouvé, charge les détails de l'utilisateur et configure
     * le {@link SecurityContextHolder}. Si aucun token n'est présent, ou si le token est invalide,
     * le contexte de sécurité n'est pas modifié (l'utilisateur reste non authentifié pour Spring Security
     * à ce stade), et la requête continue dans la chaîne de filtres.
     *
     * @param request     La requête HTTP entrante. Ne doit pas être null.
     * @param response    La réponse HTTP sortante. Ne doit pas être null.
     * @param filterChain La chaîne de filtres permettant de passer la requête au filtre suivant. Ne doit pas être null.
     * @throws ServletException Si une erreur liée au servlet survient.
     * @throws IOException      Si une erreur d'entrée/sortie survient.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail; // Utiliser un nom plus descriptif que 'subject'

        // Vérifie si l'en-tête Authorization est présent et commence par "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token JWT ou format incorrect, on passe au filtre suivant sans rien faire.
            filterChain.doFilter(request, response);
            return; // Important de retourner ici pour ne pas continuer le traitement JWT.
        }

        // Extrait le token JWT (la partie après "Bearer ")
        jwt = authHeader.substring(7);

        try {
            // Extrait l'email (sujet) du token. Cette étape implique aussi une validation de base (signature, expiration).
            userEmail = jwtUtils.getSubjectFromJwt(jwt);

            // Vérifie si l'utilisateur est déjà authentifié dans le contexte actuel
            // (évite de recharger l'utilisateur inutilement si déjà fait par un filtre précédent, peu probable ici mais bonne pratique).
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Charge les UserDetails correspondants à l'email extrait du token.
                UserDetails userDetails = this.appUserDetailService.loadUserByUsername(userEmail);

                // (Optionnel mais recommandé) Vérifier si le token est toujours valide selon la logique métier
                // (ex: le token lui-même n'est pas expiré ET peut-être une vérification supplémentaire si nécessaire).
                // Ici, getSubjectFromJwt a déjà validé la signature et l'expiration standard.

                // Crée un objet Authentication pour Spring Security.
                // Note: Le mot de passe est null car l'authentification est basée sur le token validé, pas sur un mdp fourni.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // Le principal (qui est l'objet UserDetails)
                        null,        // Pas de credentials (mot de passe) ici
                        userDetails.getAuthorities() // Les autorités (rôles) de l'utilisateur
                );

                // Ajoute des détails supplémentaires à l'objet Authentication (ex: IP, session ID).
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Met à jour le SecurityContextHolder avec l'objet Authentication,
                // authentifiant ainsi l'utilisateur pour la durée de cette requête.
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Utilisateur '{}' authentifié avec succès via JWT.", userEmail);
            }
        } catch (ExpiredJwtException e) {
            log.warn("Tentative d'accès avec un token JWT expiré: {}", e.getMessage());
            // Le contexte de sécurité reste null (non authentifié). Laisser la requête continuer
            // pour que d'autres mécanismes (ex: autorisation d'accès public ou refus) s'appliquent.
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            // Capture les autres erreurs liées au JWT (malformé, signature invalide, etc.),
            // ou si l'utilisateur du token n'existe plus, ou autre argument invalide.
            log.warn("Échec de la validation du token JWT ou chargement utilisateur: {}", e.getMessage());
            // Le contexte de sécurité reste null (non authentifié).
        }
        // Quoi qu'il arrive (token valide, invalide, ou absent), passe la requête au filtre suivant dans la chaîne.
        filterChain.doFilter(request, response);
    }
}

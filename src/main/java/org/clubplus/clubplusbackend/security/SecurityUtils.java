package org.clubplus.clubplusbackend.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service utilitaire pour la génération et la validation de tokens JWT (JSON Web Tokens).
 * <p>
 * Ce service utilise la bibliothèque {@code io.jsonwebtoken} pour créer des tokens signés
 * et pour valider les tokens entrants. La clé secrète et la durée d'expiration sont
 * configurées via les propriétés de l'application.
 */
@Service
public class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private final String secretKey;
    private final long jwtExpirationMs;

    /**
     * Construit une instance de SecurityUtils en injectant la configuration JWT.
     *
     * @param secretKey       La clé secrète pour signer les tokens, injectée via {@code @Value("${jwt.secret}")}.
     * @param jwtExpirationMs La durée d'expiration en millisecondes, injectée via {@code @Value("${jwt.expiration.ms}")}.
     * @throws IllegalArgumentException si la clé secrète est nulle ou vide.
     */
    public SecurityUtils(@Value("${jwt.secret}") String secretKey,
                         @Value("${jwt.expiration.ms}") long jwtExpirationMs) {
        if (secretKey == null || secretKey.isBlank()) {
            log.error("ERREUR CRITIQUE : La propriété 'jwt.secret' ne peut pas être vide. L'application ne peut pas démarrer de manière sécurisée.");
            throw new IllegalArgumentException("La clé secrète JWT ne peut pas être vide.");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("AVERTISSEMENT DE SÉCURITÉ : La clé secrète JWT ('jwt.secret') est potentiellement trop courte (< 256 bits).");
        }

        this.secretKey = secretKey;
        this.jwtExpirationMs = jwtExpirationMs;
        log.info("SecurityUtils initialisé avec une expiration JWT de {} ms.", jwtExpirationMs);
    }

    /**
     * Génère un token JWT pour un utilisateur donné.
     * <p>
     * Le token contient l'email de l'utilisateur comme sujet ('sub'), ainsi que des informations
     * supplémentaires (claims) comme le rôle et, pour les gestionnaires, l'ID du club géré.
     * Le token est signé avec la clé secrète et a une durée de validité définie.
     *
     * @param userDetails L'objet {@link AppUserDetails} représentant l'utilisateur authentifié.
     * @return Une chaîne de caractères représentant le token JWT signé.
     */
    public String generateToken(AppUserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            log.error("Tentative de génération de token JWT avec userDetails ou username null.");
            throw new IllegalArgumentException("UserDetails et Username ne peuvent pas être null pour générer un token.");
        }

        // Préparation des claims (informations à inclure dans le token)
        Map<String, Object> claims = new HashMap<>();
        String role = getRole(userDetails);
        if (role != null) {
            claims.put("role", role);
        } else {
            log.warn("Aucun rôle trouvé pour l'utilisateur '{}' lors de la génération du JWT.", userDetails.getUsername());
        }

        // Ajout conditionnel de l'ID du club géré
        if ("ROLE_ADMIN".equals(role) || "ROLE_RESERVATION".equals(role)) {
            Integer managedClubId = userDetails.getManagedClubId();
            if (managedClubId != null) {
                claims.put("managedClubId", managedClubId);
            } else {
                log.warn("Utilisateur gestionnaire '{}' (rôle {}) sans managedClubId défini. Le claim 'managedClubId' ne sera pas ajouté au JWT.", userDetails.getUsername(), role);
            }
        }

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date validity = new Date(nowMillis + jwtExpirationMs);

        // Construction du token
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }


    /**
     * Extrait le sujet (l'email de l'utilisateur) d'un token JWT.
     * <p>
     * Cette méthode valide la signature et la date d'expiration du token.
     * Une exception est levée si le token est invalide.
     *
     * @param jwt La chaîne du token JWT.
     * @return Le sujet (email) du token.
     * @throws ExpiredJwtException si le token a expiré.
     * @throws JwtException        pour toute autre erreur de validation (signature, format...).
     */
    public String getSubjectFromJwt(String jwt) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Méthode utilitaire interne pour extraire le rôle principal de l'utilisateur.
     *
     * @param userDetails Les détails de l'utilisateur.
     * @return La chaîne de caractères du rôle (ex: "ROLE_ADMIN") ou {@code null}.
     */
    private String getRole(AppUserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities() == null) {
            return null;
        }
        return userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .findFirst()
                .orElse(null);
    }
}

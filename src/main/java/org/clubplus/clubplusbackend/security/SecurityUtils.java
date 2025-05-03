package org.clubplus.clubplusbackend.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service utilitaire pour la génération et la validation de base des tokens JWT (JSON Web Tokens).
 * Fournit des méthodes pour créer un token JWT à partir des détails d'un utilisateur authentifié
 * et pour extraire le sujet (généralement l'identifiant utilisateur) d'un token JWT existant.
 *
 * <p>Cette classe utilise la bibliothèque {@code io.jsonwebtoken} (jjwt) pour la manipulation des JWT.</p>
 * <p>
 * La clé secrète doit impérativement être externalisée (ex: via les propriétés d'application,
 * variables d'environnement) et être suffisamment longue et complexe.</p>
 *
 * @see Jwts
 * @see AppUserDetails
 */
@Service
public class SecurityUtils {

    // Logger SLF4j pour tracer les informations, notamment les avertissements de sécurité.
    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    // --- Injection de la clé secrète et de la durée d'expiration depuis application.properties ---
    // IMPORTANT: Assurez-vous d'avoir ces propriétés dans votre fichier application.properties ou application.yml
    // jwt.secret=VOTRE_CLE_SECRETE_TRES_LONGUE_ET_COMPLEXE_ICI // Exemple: utiliser un générateur de clé forte
    // jwt.expiration.ms=86400000 // Durée en millisecondes (ici 24 heures)

    /**
     * La clé secrète utilisée pour signer et vérifier les tokens JWT.
     * **Injectée depuis la propriété 'jwt.secret'. NE PAS CODER EN DUR.**
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * La durée de validité d'un token JWT généré, en millisecondes.
     * **Injectée depuis la propriété 'jwt.expiration.ms'.**
     */
    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;


    /**
     * Méthode utilitaire interne pour extraire la première autorité (rôle)
     * de l'objet {@link AppUserDetails}.
     * Suppose que le rôle principal est la première autorité dans la liste.
     *
     * @param userDetails Les détails de l'utilisateur authentifié.
     * @return La chaîne de caractères représentant le rôle (ex: "ROLE_ADMIN") ou {@code null} si aucune autorité n'est trouvée.
     */
    private String getRole(AppUserDetails userDetails) {
        // Retourne la première autorité trouvée ou null si la collection est vide.
        return userDetails.getAuthorities().stream()
                .map(r -> r.getAuthority()) // Extrait la chaîne de l'autorité (ex: "ROLE_ADMIN")
                .findFirst()               // Prend la première
                .orElse(null);             // Retourne null si aucune n'existe
    }

    /**
     * Génère un token JWT pour un utilisateur authentifié.
     * Le token inclut l'email de l'utilisateur comme sujet ('sub'), son rôle ('role'),
     * et conditionnellement l'ID du club géré ('managedClubId') si l'utilisateur a un rôle
     * de gestion (ADMIN ou RESERVATION). Le token inclut également une date d'expiration.
     *
     * <p><b>ATTENTION :</b> La sécurité de ce token repose entièrement sur la robustesse
     * et la confidentialité de la {@code secretKey} injectée.</p>
     *
     * @param userDetails L'objet {@link AppUserDetails} contenant les informations de l'utilisateur (email, rôle, ID club géré...).
     * @return Une chaîne de caractères représentant le token JWT compact et signé.
     * @throws IllegalArgumentException si {@code userDetails} est null ou invalide.
     * @see #getRole(AppUserDetails)
     * @see AppUserDetails#getManagedClubId()
     * @see SignatureAlgorithm#HS256
     */
    public String generateToken(AppUserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("UserDetails ou Username ne peuvent pas être null pour générer un token.");
        }

        // 1. Récupérer le rôle
        String role = getRole(userDetails); // Format attendu: "ROLE_ADMIN", "ROLE_MEMBRE", etc.

        // 2. Créer les claims (informations à inclure dans le payload du JWT)
        Map<String, Object> claims = new HashMap<>();
        if (role != null) {
            claims.put("role", role); // Ajouter le rôle
        } else {
            log.warn("Aucun rôle trouvé pour l'utilisateur '{}' lors de la génération du JWT.", userDetails.getUsername());
        }

        // 3. Ajouter 'managedClubId' conditionnellement si l'utilisateur est un gestionnaire
        if ("ROLE_ADMIN".equals(role) || "ROLE_RESERVATION".equals(role)) {
            Integer managedClubId = userDetails.getManagedClubId(); // Récupère l'ID via AppUserDetails
            if (managedClubId != null) {
                claims.put("managedClubId", managedClubId);
                log.debug("Ajout de managedClubId ({}) au JWT pour l'utilisateur '{}'.", managedClubId, userDetails.getUsername());
            } else {
                // Loguer une incohérence si un manager n'a pas d'ID de club géré
                log.warn("Utilisateur gestionnaire '{}' (rôle {}) n'a pas de managedClubId défini dans AppUserDetails.", userDetails.getUsername(), role);
            }
        }

        // 4. Définir les dates d'émission et d'expiration
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date validity = new Date(nowMillis + jwtExpirationMs); // Utilise la durée injectée

        // 5. Construire le JWT
        return Jwts.builder()
                // Définit le sujet (subject 'sub') du token : l'email de l'utilisateur.
                .setSubject(userDetails.getUsername())
                // Ajoute les claims personnalisées (rôle, managedClubId).
                .addClaims(claims)
                // Définit la date d'émission ('iat').
                .setIssuedAt(now)
                // Définit la date d'expiration ('exp').
                .setExpiration(validity)
                // Signe le token avec l'algorithme HS256 et la clé secrète injectée.
                // C'est l'étape qui garantit l'intégrité et l'authenticité du token.
                .signWith(SignatureAlgorithm.HS256, secretKey)
                // Construit le token et le sérialise en une chaîne compacte URL-safe.
                .compact();
    }


    /**
     * Extrait le sujet ('sub' claim) d'un token JWT signé.
     * Cette méthode valide également la signature et l'expiration du token en utilisant la même
     * clé secrète ({@code secretKey}) que celle utilisée pour la génération.
     *
     * <p>Dans cette application, le sujet correspond à l'email de l'utilisateur.</p>
     *
     * @param jwt La chaîne de caractères représentant le token JWT compact.
     * @return Le sujet (String) extrait du token (l'email de l'utilisateur).
     * @throws ExpiredJwtException      si le token a expiré.
     * @throws MalformedJwtException    si le token est structurellement invalide.
     * @throws SignatureException       si la signature du token est invalide (token modifié ou mauvaise clé).
     * @throws UnsupportedJwtException  si le type de token n'est pas supporté.
     * @throws IllegalArgumentException si la chaîne {@code jwt} est null, vide ou invalide.
     * @see Jwts#parser()
     */
    public String getSubjectFromJwt(String jwt) {
        // Crée un parser JWT configuré avec la clé secrète utilisée pour la signature.
        // Le parser vérifiera automatiquement la signature et l'expiration lors de l'appel à parseClaimsJws.
        return Jwts.parser()
                .setSigningKey(secretKey) // Définit la clé pour vérifier la signature
                .parseClaimsJws(jwt)      // Parse et valide le token signé
                .getBody()                // Récupère le payload (les claims)
                .getSubject();            // Extrait la claim 'sub' (le sujet/email)
    }

}

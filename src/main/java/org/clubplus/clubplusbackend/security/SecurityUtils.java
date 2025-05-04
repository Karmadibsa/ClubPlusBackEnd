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
 * Service utilitaire pour la génération et la validation de base des tokens JWT (JSON Web Tokens).
 * Fournit des méthodes pour créer un token JWT à partir des détails d'un utilisateur authentifié
 * et pour extraire le sujet (généralement l'identifiant utilisateur) d'un token JWT existant.
 *
 * <p>Cette classe utilise la bibliothèque {@code io.jsonwebtoken} (jjwt) pour la manipulation des JWT.</p>
 * <p>
 * La clé secrète et la durée d'expiration sont injectées à partir des propriétés de l'application.
 * Il est crucial que la clé secrète soit suffisamment longue, complexe et gardée confidentielle.
 * </p>
 *
 * @see Jwts
 * @see AppUserDetails
 * @see JwtFilter
 */
@Service
public class SecurityUtils {

    // Logger SLF4j pour tracer les informations, notamment les avertissements de sécurité.
    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * La clé secrète utilisée pour signer et vérifier les tokens JWT.
     * Injectée depuis la propriété 'jwt.secret'. Ne doit JAMAIS être vide ou faible.
     */
    private final String secretKey;

    /**
     * La durée de validité d'un token JWT en millisecondes.
     * Injectée depuis la propriété 'jwt.expiration.ms'.
     */
    private final long jwtExpirationMs;

    /**
     * Construit une instance de SecurityUtils en injectant la clé secrète et la durée d'expiration
     * depuis les propriétés de l'application.
     * Valide que la clé secrète fournie n'est pas nulle ou vide.
     *
     * @param secretKey       La clé secrète JWT, injectée via {@code @Value("${jwt.secret}")}.
     * @param jwtExpirationMs La durée d'expiration en millisecondes, injectée via {@code @Value("${jwt.expiration.ms}")}.
     * @throws IllegalArgumentException si la {@code secretKey} est nulle, vide ou ne contient que des espaces blancs.
     */
    public SecurityUtils(@Value("${jwt.secret}") String secretKey,
                         @Value("${jwt.expiration.ms}") long jwtExpirationMs) {
        if (secretKey == null || secretKey.isBlank()) {
            // Il est crucial de vérifier que la clé n'est pas vide !
            log.error("ERREUR CRITIQUE : La propriété 'jwt.secret' ne doit pas être vide dans application.properties/yml ! L'application ne peut pas démarrer de manière sécurisée.");
            throw new IllegalArgumentException("La clé secrète JWT ne peut pas être vide.");
        }
        // Optionnel : ajouter une vérification de longueur minimale pour la clé.
        // Exemple pour HS256 (256 bits = 32 octets)
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("AVERTISSEMENT DE SÉCURITÉ : La clé secrète JWT fournie ('jwt.secret') est potentiellement trop courte (< 256 bits). Envisagez une clé plus longue et complexe.");
        }

        this.secretKey = secretKey;
        this.jwtExpirationMs = jwtExpirationMs;
        log.info("SecurityUtils initialisé. Durée d'expiration JWT configurée : {} ms.", jwtExpirationMs);
    }


    /**
     * Méthode utilitaire interne pour extraire la première autorité (rôle)
     * de l'objet {@link AppUserDetails}.
     * Suppose que le rôle principal est la première autorité dans la liste retournée par {@code getAuthorities()}.
     *
     * @param userDetails Les détails de l'utilisateur authentifié (implémentation de UserDetails).
     * @return La chaîne de caractères représentant le rôle (ex: "ROLE_ADMIN", "ROLE_MEMBRE")
     * ou {@code null} si aucune autorité n'est trouvée pour cet utilisateur.
     */
    private String getRole(AppUserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities() == null) {
            return null;
        }
        // Retourne la première autorité trouvée ou null si la collection est vide.
        return userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority()) // Extrait la chaîne de l'autorité (ex: "ROLE_ADMIN")
                .findFirst()               // Prend la première trouvée dans le flux
                .orElse(null);             // Retourne null si le flux est vide (aucune autorité)
    }

    /**
     * Génère un token JWT pour un utilisateur authentifié représenté par {@link AppUserDetails}.
     * Le token inclut l'identifiant de l'utilisateur (son email/username) comme sujet ('sub') du token.
     * Il inclut également des informations supplémentaires (claims) :
     * <ul>
     *     <li>{@code role}: Le rôle principal de l'utilisateur (ex: "ROLE_ADMIN").</li>
     *     <li>{@code managedClubId}: L'ID du club géré par l'utilisateur, uniquement si celui-ci a un rôle
     *     de gestion (ADMIN ou RESERVATION) et si cet ID est défini dans {@link AppUserDetails}.</li>
     * </ul>
     * Le token est signé avec l'algorithme HS256 et la clé secrète configurée, et inclut des dates
     * d'émission ('iat') et d'expiration ('exp') basées sur la durée {@code jwtExpirationMs}.
     *
     * <p><b>ATTENTION :</b> La sécurité de ce token repose entièrement sur la robustesse
     * et la confidentialité de la {@code secretKey} injectée. Une clé faible ou compromise
     * permettrait à un attaquant de forger des tokens valides.</p>
     *
     * @param userDetails L'objet {@link AppUserDetails} contenant les informations de l'utilisateur
     *                    (username, autorités/rôle, ID du club géré si applicable). Ne doit pas être null.
     * @return Une chaîne de caractères représentant le token JWT compact et signé (format JWS).
     * @throws IllegalArgumentException si {@code userDetails} ou son {@code username} est null.
     * @see #getRole(AppUserDetails)
     * @see AppUserDetails#getManagedClubId()
     * @see SignatureAlgorithm#HS256
     */
    public String generateToken(AppUserDetails userDetails) {
        // Validation d'entrée
        if (userDetails == null || userDetails.getUsername() == null) {
            log.error("Tentative de génération de token JWT avec userDetails ou username null.");
            throw new IllegalArgumentException("UserDetails et Username ne peuvent pas être null pour générer un token.");
        }

        // 1. Récupérer le rôle principal de l'utilisateur
        String role = getRole(userDetails); // Format attendu: "ROLE_ADMIN", "ROLE_MEMBRE", etc.

        // 2. Préparer les claims (charge utile) du JWT
        Map<String, Object> claims = new HashMap<>();
        if (role != null) {
            claims.put("role", role); // Ajouter le rôle s'il existe
        } else {
            // Loguer si aucun rôle n'est trouvé, cela peut indiquer un problème de configuration utilisateur
            log.warn("Aucun rôle trouvé pour l'utilisateur '{}' lors de la génération du JWT. Le token sera généré sans claim 'role'.", userDetails.getUsername());
        }

        // 3. Ajouter conditionnellement l'ID du club géré pour les rôles spécifiques
        if ("ROLE_ADMIN".equals(role) || "ROLE_RESERVATION".equals(role)) {
            Integer managedClubId = userDetails.getManagedClubId(); // Récupère l'ID via AppUserDetails
            if (managedClubId != null) {
                claims.put("managedClubId", managedClubId);
                log.debug("Ajout de managedClubId ({}) au JWT pour l'utilisateur gestionnaire '{}'.", managedClubId, userDetails.getUsername());
            } else {
                // Loguer une incohérence potentielle : un admin/gestionnaire devrait avoir un club géré.
                log.warn("Utilisateur gestionnaire '{}' (rôle {}) n'a pas de managedClubId défini dans AppUserDetails. La claim 'managedClubId' ne sera pas ajoutée au JWT.", userDetails.getUsername(), role);
            }
        }

        // 4. Définir les timestamps : date d'émission et date d'expiration
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date validity = new Date(nowMillis + jwtExpirationMs); // Utilise la durée d'expiration configurée

        // 5. Construire le token JWT
        log.debug("Génération du token JWT pour l'utilisateur '{}' avec expiration à {}.", userDetails.getUsername(), validity);
        return Jwts.builder()
                // Définit le sujet (subject 'sub') du token : identifiant unique de l'utilisateur (ici, son email/username).
                .setSubject(userDetails.getUsername())
                // Ajoute toutes les claims personnalisées préparées (rôle, managedClubId).
                .addClaims(claims)
                // Définit la date d'émission ('iat' - issued at).
                .setIssuedAt(now)
                // Définit la date d'expiration ('exp'). Après cette date, le token sera invalide.
                .setExpiration(validity)
                // Signe le token avec l'algorithme HS256 (HMAC using SHA-256) et la clé secrète injectée.
                // C'est l'étape cruciale qui garantit l'intégrité (non-modification) et l'authenticité du token.
                .signWith(SignatureAlgorithm.HS256, secretKey)
                // Construit le token et le sérialise en une chaîne compacte URL-safe (Header.Payload.Signature).
                .compact();
    }


    /**
     * Extrait le sujet ('sub' claim) d'un token JWT fourni sous forme de chaîne.
     * Cette méthode effectue implicitement plusieurs validations cruciales en utilisant la
     * clé secrète ({@code secretKey}) configurée dans ce service :
     * <ul>
     *     <li>Vérifie que la chaîne JWT est bien formée (syntaxe correcte).</li>
     *     <li>Vérifie que la signature du token est valide (prouve qu'il n'a pas été modifié et a été signé avec notre clé).</li>
     *     <li>Vérifie que le token n'a pas expiré.</li>
     * </ul>
     * Si l'une de ces vérifications échoue, une exception spécifique est levée.
     *
     * <p>Dans le contexte de cette application, le sujet ('sub') du token correspond à
     * l'identifiant unique de l'utilisateur (son email/username).</p>
     *
     * @param jwt La chaîne de caractères représentant le token JWT compact (format JWS : Header.Payload.Signature).
     * @return Le sujet (String) extrait du token (qui est l'email/username de l'utilisateur).
     * @throws ExpiredJwtException      si le token a dépassé sa date d'expiration.
     * @throws MalformedJwtException    si la chaîne {@code jwt} n'est pas un JWT valide structurellement.
     * @throws SignatureException       si la signature du JWT est invalide (le token a été altéré ou la clé de vérification est incorrecte).
     * @throws UnsupportedJwtException  si le token utilise un algorithme ou un format non supporté par la bibliothèque.
     * @throws IllegalArgumentException si la chaîne {@code jwt} est null, vide, ou si le parser rencontre un problème inattendu.
     * @see Jwts#parser()
     * @see Claims#getSubject()
     */
    public String getSubjectFromJwt(String jwt) {
        // Crée un parser JWT configuré avec la clé secrète utilisée pour la signature.
        // C'est cette clé qui permet de vérifier l'authenticité de la signature.
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey) // Définit la clé pour VÉRIFIER la signature
                .parseClaimsJws(jwt)      // Parse et valide la structure, la signature ET l'expiration du token signé
                .getBody();               // Récupère le payload (l'ensemble des claims) si tout est valide

        // Extrait la claim standard 'sub' (subject) du payload.
        return claims.getSubject();
    }

}

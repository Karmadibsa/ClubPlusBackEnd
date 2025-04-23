package org.clubplus.clubplusbackend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service utilitaire pour la génération de tokens JWT (JSON Web Tokens)
 */
@Service
public class SecurityUtils {

    public String getRole(AppUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(r -> r.getAuthority())
                .findFirst()
                .orElse(null);
    }

    /**
     * Génère un token JWT à partir des détails de l'utilisateur authentifié
     * <p>
     * param userDetails L'objet contenant les informations de l'utilisateur
     * return Une chaîne de caractères représentant le token JWT
     */
    public String generateToken(AppUserDetails userDetails) {
        // 1. Déterminer le rôle (assurez-vous que getRole retourne le bon format, ex: "ROLE_ADMIN")
        String role = getRole(userDetails); // Vous avez déjà cette méthode

        // 2. Créer une Map mutable pour les claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // Ajouter le rôle systématiquement

        // 3. Ajouter 'managedClubId' CONDITIONNELLEMENT
        // Vérifier si le rôle est ADMIN ou RESERVATION (adaptez si les noms de rôles sont différents)
        // **IMPORTANT**: Assurez-vous que les chaînes correspondent exactement aux rôles utilisés.
        if ("ROLE_ADMIN".equals(role) || "ROLE_RESERVATION".equals(role)) {

            // **HYPOTHÈSE IMPORTANTE**:
            // On suppose que votre classe 'AppUserDetails' (ou un service accessible ici)
            // peut fournir l'ID de l'unique club géré par cet utilisateur ADMIN/RESA.
            // Remplacez 'userDetails.getManagedClubId()' par la méthode réelle pour obtenir cet ID.
            // La méthode doit retourner un Integer (ou Long).
            Integer managedClubId = userDetails.getManagedClubId(); // <--- À ADAPTER !

            // Sécurité: Ne pas ajouter la claim si l'ID n'est pas trouvé pour un admin/resa
            if (managedClubId != null) {
                claims.put("managedClubId", managedClubId);
                System.out.println("Ajout de managedClubId au JWT: " + managedClubId); // Log pour déboguer
            } else {
                // Loguer une erreur ou un avertissement serait bien ici, car un ADMIN/RESA
                // devrait normalement avoir un club associé.
                System.err.println("AVERTISSEMENT: L'utilisateur " + userDetails.getUsername() + " avec le rôle " + role + " n'a pas de managedClubId fourni par AppUserDetails !");
            }
        }
        return Jwts.builder()
                // Définit le sujet du token (généralement l'identifiant unique de l'utilisateur)
                // Dans ce cas, getUsername() renvoie l'email de l'utilisateur
                .setSubject(userDetails.getUsername())
                .addClaims(claims)

                // Signe le token avec l'algorithme HMAC SHA-256 et une clé secrète
                // ATTENTION: Utiliser une clé codée en dur ("azerty") n'est pas sécurisé
                // en production, elle devrait être stockée dans une variable d'environnement
                // ou un fichier de configuration sécurisé
                .signWith(SignatureAlgorithm.HS256, "azerty")

                // Convertit le token en format compact (chaîne de caractères)
                .compact();
    }


    public String getSubjectFromJwt(String jwt) {
        return Jwts.parser()
                .setSigningKey("azerty")
                .parseClaimsJws(jwt)
                .getBody()
                .getSubject();
    }
}


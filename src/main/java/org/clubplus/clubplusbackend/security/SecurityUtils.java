package org.clubplus.clubplusbackend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service utilitaire pour la génération de tokens JWT (JSON Web Tokens)
 */
@Service
public class SecurityUtils implements ISecurityUtils {

    @Override
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
    @Override
    public String generateToken(AppUserDetails userDetails) {
        return Jwts.builder()
                // Définit le sujet du token (généralement l'identifiant unique de l'utilisateur)
                // Dans ce cas, getUsername() renvoie l'email de l'utilisateur
                .setSubject(userDetails.getUsername())
                .addClaims(Map.of("role", getRole(userDetails)))

                // Signe le token avec l'algorithme HMAC SHA-256 et une clé secrète
                // ATTENTION: Utiliser une clé codée en dur ("azerty") n'est pas sécurisé
                // en production, elle devrait être stockée dans une variable d'environnement
                // ou un fichier de configuration sécurisé
                .signWith(SignatureAlgorithm.HS256, "azerty")

                // Convertit le token en format compact (chaîne de caractères)
                .compact();
    }


    @Override
    public String getSubjectFromJwt(String jwt) {
        return Jwts.parser()
                .setSigningKey("azerty")
                .parseClaimsJws(jwt)
                .getBody()
                .getSubject();
    }
}


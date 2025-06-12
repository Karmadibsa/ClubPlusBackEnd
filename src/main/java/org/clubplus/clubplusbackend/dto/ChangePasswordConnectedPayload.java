package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO (Data Transfer Object) pour la requête de changement de mot de passe
 * d'un utilisateur déjà authentifié.
 * Ce DTO transporte le mot de passe actuel pour vérification et le nouveau mot de passe.
 */
@Getter
@Setter
public class ChangePasswordConnectedPayload {

    /**
     * Le mot de passe actuel de l'utilisateur.
     * Doit correspondre à celui stocké en base de données pour que l'opération réussisse.
     * Ne peut pas être vide.
     */
    @NotBlank(message = "Le mot de passe actuel ne peut pas être vide.")
    private String currentPassword;

    /**
     * Le nouveau mot de passe souhaité par l'utilisateur.
     * Doit respecter les contraintes de sécurité (longueur, etc.).
     * Ne peut pas être vide.
     */
    @NotBlank(message = "Le nouveau mot de passe ne peut pas être vide.")
    @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères.")
    private String newPassword;
}

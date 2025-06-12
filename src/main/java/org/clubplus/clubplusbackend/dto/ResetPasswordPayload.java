package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO (Data Transfer Object) pour la requête de réinitialisation de mot de passe.
 * Ce DTO transporte le token de réinitialisation (reçu par email) et le nouveau mot de passe
 * choisi par l'utilisateur.
 */
@Data
public class ResetPasswordPayload {

    /**
     * Le token de réinitialisation unique envoyé à l'utilisateur par email.
     * Ne peut pas être vide.
     */
    @NotBlank(message = "Le token est obligatoire.")
    private String token;

    /**
     * Le nouveau mot de passe que l'utilisateur souhaite définir.
     * Doit respecter les contraintes de sécurité de l'application.
     */
    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[{}\\]:;',?/*~$^+=<>]).{8,100}$",
            message = "Le mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial.")
    private String newPassword;
}

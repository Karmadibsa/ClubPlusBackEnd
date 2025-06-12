package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO (Data Transfer Object) pour une requête de connexion.
 * Ce DTO valide et transporte l'email et le mot de passe fournis par l'utilisateur
 * lors d'une tentative d'authentification.
 */
@Data
public class LoginRequestDto {

    /**
     * L'adresse email de l'utilisateur.
     * Doit être une adresse email bien formée et ne peut pas être vide.
     */
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    private String email;

    /**
     * Le mot de passe de l'utilisateur.
     * Ne peut pas être vide.
     */
    @NotBlank(message = "Le mot de passe est obligatoire.")
    private String password;
}

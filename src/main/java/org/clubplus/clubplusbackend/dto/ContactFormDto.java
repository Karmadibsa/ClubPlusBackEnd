package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO (Data Transfer Object) pour le formulaire de contact public.
 * Ce DTO valide et transporte les informations soumises par un visiteur
 * depuis le site web.
 */
@Getter
@Setter
public class ContactFormDto {

    /**
     * Le nom de la personne qui envoie le message.
     * Doit être non nul et contenir au moins un caractère non blanc.
     */
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String name;

    /**
     * L'adresse email de la personne à recontacter.
     * Doit être une adresse email bien formée.
     */
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'adresse email doit être valide.")
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères.")
    private String email;

    /**
     * Le sujet du message de contact.
     */
    @NotBlank(message = "Le sujet est obligatoire.")
    @Size(max = 150, message = "Le sujet ne doit pas dépasser 150 caractères.")
    private String subject;

    /**
     * Le contenu du message envoyé par l'utilisateur.
     * Doit contenir entre 10 et 2000 caractères.
     */
    @NotBlank(message = "Le message est obligatoire.")
    @Size(min = 10, message = "Le message doit contenir au moins 10 caractères.")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères.")
    private String message;

}

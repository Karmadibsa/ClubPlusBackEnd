package org.clubplus.clubplusbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) pour la création simultanée d'un club et de son administrateur.
 * Agrège les informations du club et de l'admin pour une création en une seule requête API.
 */
@Data
public class CreateClubRequestDto {

    // --- Champs pour le Club ---

    /**
     * Nom du nouveau club.
     */
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit avoir entre 2 et 100 caractères.")
    private String nom;

    /**
     * Date de création du club, fournie par l'utilisateur.
     * Doit être une date passée ou la date du jour.
     */
    @NotNull(message = "La date de création 'réelle' du club est obligatoire.")
    @PastOrPresent(message = "La date de création doit être dans le passé ou aujourd'hui.")
    private LocalDate date_creation;

    /**
     * Numéro de voie de l'adresse du club.
     */
    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    private String numero_voie;

    /**
     * Rue de l'adresse du club.
     */
    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    private String rue;

    /**
     * Code postal du club.
     */
    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    private String codepostal;

    /**
     * Ville du club.
     */
    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    private String ville;

    /**
     * Numéro de téléphone principal du club.
     */
    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    private String telephone;

    /**
     * Adresse email principale et unique du club.
     */
    @NotBlank(message = "L'email du club est obligatoire.")
    @Email(message = "Le format de l'email du club est invalide.")
    @Size(max = 254, message = "L'email du club ne doit pas dépasser 254 caractères.")
    private String email;

    // --- Champs pour l'Administrateur ---

    /**
     * Informations de l'administrateur initial.
     * La validation est déléguée à la classe interne grâce à @Valid.
     */
    @NotNull(message = "Les informations de l'administrateur sont requises.")
    @Valid
    private AdminInfo admin;

    /**
     * Classe interne encapsulant les informations de l'administrateur.
     */
    @Data
    public static class AdminInfo {

        /**
         * Nom de famille de l'administrateur.
         */
        @NotBlank(message = "Le nom de l'admin est obligatoire.")
        @Size(min = 2, max = 50, message = "Le nom de l'admin doit avoir entre 2 et 50 caractères.")
        private String nom;

        /**
         * Prénom de l'administrateur.
         */
        @NotBlank(message = "Le prénom de l'admin est obligatoire.")
        @Size(min = 2, max = 50, message = "Le prénom de l'admin doit avoir entre 2 et 50 caractères.")
        private String prenom;

        /**
         * Date de naissance de l'administrateur. Doit être dans le passé.
         */
        @NotNull(message = "La date de naissance de l'admin est obligatoire.")
        @Past(message = "La date de naissance de l'admin doit être dans le passé.")
        private LocalDate date_naissance;

        /**
         * Numéro de téléphone de l'administrateur.
         */
        @NotBlank(message = "Le numéro de téléphone de l'admin est obligatoire.")
        @Size(max = 20, message = "Le numéro de téléphone de l'admin ne doit pas dépasser 20 caractères.")
        private String telephone;

        /**
         * Adresse email unique de l'administrateur.
         */
        @NotBlank(message = "L'email de l'admin est obligatoire.")
        @Email(message = "Le format de l'email de l'admin est invalide.")
        @Size(max = 254, message = "L'email de l'admin ne doit pas dépasser 254 caractères.")
        private String email;

        /**
         * Mot de passe du compte administrateur.
         * Doit contenir une majuscule, une minuscule, un chiffre et un caractère spécial.
         */
        @NotBlank(message = "Le mot de passe admin est obligatoire.")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[{}\\]:;',?/*~$^+=<>]).{8,100}$",
                message = "Le mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial.")
        private String password;
    }
}

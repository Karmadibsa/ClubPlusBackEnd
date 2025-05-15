package org.clubplus.clubplusbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les informations nécessaires
 * à la création simultanée d'un nouveau {@link Club} et de son {@link Membre} administrateur initial.
 * Cette structure agrège les champs du club et les champs de l'administrateur (via une classe interne {@link AdminInfo}),
 * permettant de tout créer en une seule requête API.
 * Inclut des annotations de validation pour tous les champs requis.
 *
 * @see Club
 * @see Membre
 * @see org.clubplus.clubplusbackend.controller.ClubController#createClubAndAdmin(CreateClubRequestDto)
 */
@Data // Lombok: Raccourci pour @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor.
public class CreateClubRequestDto {

    // --- Champs pour l'entité Club ---

    /**
     * Nom du nouveau club. Obligatoire, taille entre 2 et 100 caractères.
     */
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit avoir entre 2 et 100 caractères.")
    private String nom;

    /**
     * Date de création "réelle" du club (fournie par l'utilisateur). Obligatoire et doit être dans le passé ou aujourd'hui.
     */
    @NotNull(message = "La date de création 'réelle' du club est obligatoire.")
    @PastOrPresent(message = "La date de création doit être dans le passé ou aujourd'hui.")
    private LocalDate date_creation; // La date d'inscription système sera gérée par l'entité/service.

    /**
     * Numéro de voie de l'adresse du club. Obligatoire, max 10 caractères.
     */
    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    private String numero_voie;

    /**
     * Rue de l'adresse du club. Obligatoire, max 100 caractères.
     */
    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    private String rue;

    /**
     * Code postal du club. Obligatoire, entre 3 et 10 caractères.
     */
    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    private String codepostal;

    /**
     * Ville du club. Obligatoire, max 100 caractères.
     */
    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    private String ville;

    /**
     * Numéro de téléphone principal du club. Obligatoire, max 20 caractères.
     */
    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    private String telephone;

    /**
     * Adresse email principale du club. Obligatoire, format email valide, max 254 caractères. Doit être unique.
     */
    @NotBlank(message = "L'email du club est obligatoire.")
    @Email(message = "Le format de l'email du club est invalide.")
    @Size(max = 254, message = "L'email du club ne doit pas dépasser 254 caractères.")
    private String email;

    // --- Champs pour l'entité Membre (Admin) via Classe Interne ---

    /**
     * Informations détaillées de l'administrateur initial à créer pour ce club.
     * Cet objet imbriqué est obligatoire et ses propres champs sont également validés
     * grâce à l'annotation {@link Valid @Valid}.
     */
    @NotNull(message = "Les informations de l'administrateur sont requises.")
    @Valid // Annotation cruciale pour déclencher la validation des champs dans AdminInfo.
    private AdminInfo admin;

    /**
     * Classe interne statique encapsulant les informations requises pour créer le
     * membre administrateur initial du club.
     * Utilise {@link Data @Data} de Lombok pour la concision.
     */
    @Data // Lombok pour getters/setters etc. sur l'admin
    public static class AdminInfo {

        /**
         * Nom de famille de l'admin. Obligatoire, taille entre 2 et 50.
         */
        @NotBlank(message = "Le nom de l'admin est obligatoire.")
        @Size(min = 2, max = 50, message = "Le nom de l'admin doit avoir entre 2 et 50 caractères.")
        private String nom;

        /**
         * Prénom de l'admin. Obligatoire, taille entre 2 et 50.
         */
        @NotBlank(message = "Le prénom de l'admin est obligatoire.")
        @Size(min = 2, max = 50, message = "Le prénom de l'admin doit avoir entre 2 et 50 caractères.")
        private String prenom;

        /**
         * Date de naissance de l'admin. Obligatoire et doit être dans le passé.
         */
        @NotNull(message = "La date de naissance de l'admin est obligatoire.")
        @Past(message = "La date de naissance de l'admin doit être dans le passé.")
        private LocalDate date_naissance;
        
        /**
         * Numéro de téléphone de l'admin. Obligatoire, max 20.
         */
        @NotBlank(message = "Le numéro de téléphone de l'admin est obligatoire.")
        @Size(max = 20, message = "Le numéro de téléphone de l'admin ne doit pas dépasser 20 caractères.")
        private String telephone;

        /**
         * Adresse email de l'admin. Obligatoire, format email, max 254. Doit être unique parmi les membres.
         */
        @NotBlank(message = "L'email de l'admin est obligatoire.")
        @Email(message = "Le format de l'email de l'admin est invalide.")
        @Size(max = 254, message = "L'email de l'admin ne doit pas dépasser 254 caractères.")
        private String email;

        /**
         * Mot de passe choisi pour l'admin. Obligatoire, taille entre 8 et 100. Doit être haché avant persistance.
         */
        @NotBlank(message = "Le mot de passe admin est obligatoire.")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[{}\\]:;',?/*~$^+=<>]).{8,100}$",
                message = "Le mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial (!@#&()...)")
        @Size(min = 8, max = 100, message = "Le mot de passe doit avoir entre 8 et 100 caractères.")
        private String password;
    }
}

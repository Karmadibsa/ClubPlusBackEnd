package org.clubplus.clubplusbackend.dto; // Créez un package dto si ce n'est pas fait

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data // Raccourci Lombok pour @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
public class CreateClubRequestDto {

    // --- Champs Club ---
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100)
    private String nom;

    @NotNull(message = "La date de création 'réelle' du club est obligatoire.")
    @PastOrPresent(message = "La date de création doit être dans le passé ou aujourd'hui.")
    private LocalDate date_creation; // Date "réelle" fournie par l'utilisateur

    @NotBlank
    @Size(max = 10)
    private String numero_voie;
    @NotBlank
    @Size(max = 100)
    private String rue;
    @NotBlank
    @Size(min = 3, max = 10)
    private String codepostal;
    @NotBlank
    @Size(max = 100)
    private String ville;
    @NotBlank
    @Size(max = 20)
    private String telephone;

    @NotBlank(message = "L'email du club est obligatoire.")
    @Email
    @Size(max = 254)
    private String email;

    // --- Champs Admin Imbriqué ---
    @NotNull(message = "Les informations de l'administrateur sont requises.")
    @Valid // !!! TRÈS IMPORTANT: Valide les contraintes DANS AdminInfo !!!
    private AdminInfo admin;

    // Classe interne statique pour les détails de l'admin
    @Data // Lombok pour getters/setters etc. sur l'admin
    public static class AdminInfo {
        @NotBlank
        @Size(min = 2, max = 50)
        private String nom;
        @NotBlank
        @Size(min = 2, max = 50)
        private String prenom;

        @NotNull
        @Past // Validation date naissance
        private LocalDate date_naissance;

        @NotBlank
        @Size(max = 10)
        private String numero_voie;
        @NotBlank
        @Size(max = 100)
        private String rue;
        @NotBlank
        @Size(min = 3, max = 10)
        private String codepostal;
        @NotBlank
        @Size(max = 100)
        private String ville;
        @NotBlank
        @Size(max = 20)
        private String telephone;

        @NotBlank
        @Email
        @Size(max = 254)
        private String email;

        @NotBlank(message = "Le mot de passe admin est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit avoir entre 8 et 100 caractères.")
        private String password;
    }
}

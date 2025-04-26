package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateMembreDto {
    @NotBlank(message = "Le prénom est requis")
    private String prenom;

    @NotBlank(message = "Le nom est requis")
    private String nom;

    @NotNull(message = "La date de naissance est requise")
    private LocalDate date_naissance; // Ou String si format YYYY-MM-DD attendu

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le téléphone est requis")
    private String telephone;

    // Champs Adresse
    private String numero_voie; // Peut-être pas @NotBlank si optionnel ? Adaptez selon vos règles.
    private String rue;
    private String codepostal;
    private String ville;
}

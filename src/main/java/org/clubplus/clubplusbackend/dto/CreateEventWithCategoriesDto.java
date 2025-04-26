// --- DTO pour la Création ---
package org.clubplus.clubplusbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateEventWithCategoriesDto {

    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private LocalDateTime start;

    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    private LocalDateTime end;

    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    private String location;

    @NotNull(message = "La liste des catégories ne peut pas être nulle (peut être vide).")
    @Valid // Valider chaque élément de la liste
    private List<CreateCategorieDto> categories; // Utilise le DTO simple pour la création de catégorie
}

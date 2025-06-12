package org.clubplus.clubplusbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * DTO (Data Transfer Object) pour la création d'un événement avec ses catégories initiales.
 * Combine les informations de l'événement et une liste de DTOs pour les catégories
 * afin de permettre une création en une seule requête API.
 */
@Getter
@Setter
public class CreateEventWithCategoriesDto {

    /**
     * Le nom de l'événement.
     */
    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    /**
     * La date et heure de début de l'événement.
     * Doit être dans le présent ou le futur.
     */
    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private Instant startTime;

    /**
     * La date et heure de fin de l'événement.
     * Doit être dans le présent ou le futur. La cohérence avec startTime est vérifiée dans le service.
     */
    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    private Instant endTime;

    /**
     * La description détaillée de l'événement.
     */
    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    /**
     * Le lieu où se déroule l'événement (optionnel).
     */
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    private String location;

    /**
     * La liste des catégories à créer pour cet événement.
     * La validation de chaque élément est déléguée via l'annotation @Valid.
     */
    @NotNull(message = "La liste des catégories ne peut pas être nulle.")
    @Valid
    private List<CreateCategorieDto> categories;
}

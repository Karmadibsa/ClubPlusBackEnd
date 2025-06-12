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
 * DTO (Data Transfer Object) pour la mise à jour complète d'un événement et de ses catégories.
 * <p>
 * Ce DTO permet de modifier les informations d'un événement et de redéfinir entièrement sa liste de catégories
 * en une seule opération. La logique du service se chargera de comparer cette liste avec l'état existant
 * pour ajouter, mettre à jour ou supprimer des catégories.
 */
@Getter
@Setter
public class UpdateEventWithCategoriesDto {

    /**
     * Le nouveau nom de l'événement.
     */
    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    /**
     * La nouvelle date et heure de début de l'événement.
     * Doit être dans le présent ou le futur.
     */
    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private Instant startTime;

    /**
     * La nouvelle date et heure de fin de l'événement.
     * Doit être dans le présent ou le futur. La cohérence avec startTime est vérifiée dans le service.
     */
    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    private Instant endTime;

    /**
     * La nouvelle description détaillée de l'événement.
     */
    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    /**
     * Le nouveau lieu de l'événement (optionnel).
     */
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    private String location;

    /**
     * La liste complète et finale des catégories souhaitées pour l'événement.
     * <p>
     * - Une catégorie avec un ID sera mise à jour.
     * <p>
     * - Une catégorie sans ID sera créée.
     * <p>
     * - Toute catégorie existante non présente dans cette liste sera supprimée.
     * <p>
     * La validation de chaque élément est déléguée via l'annotation @Valid.
     */
    @NotNull(message = "La liste des catégories ne peut pas être nulle.")
    @Valid
    private List<UpdateCategorieDto> categories;
}

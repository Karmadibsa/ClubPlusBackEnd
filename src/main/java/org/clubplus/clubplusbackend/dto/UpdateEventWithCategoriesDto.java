package org.clubplus.clubplusbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données nécessaires à la mise à jour
 * d'un {@link Event} existant **et** de ses {@link Categorie}s associées en une seule opération.
 * Combine les champs modifiables de l'événement avec une liste de {@link UpdateCategorieDto}.
 *
 * <p>La logique de mise à jour dans le service devra traiter cette liste : mettre à jour les catégories
 * existantes (basé sur l'ID fourni dans {@code UpdateCategorieDto}), créer les nouvelles (celles
 * sans ID dans le DTO ?), et potentiellement supprimer celles présentes dans l'entité mais absentes
 * de la liste DTO (selon la stratégie de mise à jour choisie).</p>
 *
 * <p>Inclut des validations pour les champs de l'événement et utilise {@link Valid @Valid} pour
 * valider les DTOs de catégorie imbriqués.</p>
 *
 * @see Event
 * @see Categorie
 * @see UpdateCategorieDto
 * @see org.clubplus.clubplusbackend.service.EventService (où ce DTO serait traité)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
public class UpdateEventWithCategoriesDto {

    // L'ID de l'événement à mettre à jour est fourni via @PathVariable.

    /**
     * Le nouveau nom de l'événement. Obligatoire, taille 3-150.
     */
    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    /**
     * La nouvelle date/heure de début. Obligatoire, présent ou futur.
     */
    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private LocalDateTime start;

    /**
     * La nouvelle date/heure de fin. Obligatoire, présent ou futur, et après 'start' (validé en service).
     */
    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    private LocalDateTime end;

    /**
     * La nouvelle description. Obligatoire, max 2000.
     */
    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    /**
     * Le nouveau lieu. Optionnel, max 255.
     */
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    private String location; // Optionnel

    /**
     * La liste des catégories souhaitées pour l'événement après mise à jour.
     * La liste elle-même est obligatoire (ne peut être nulle).
     * Chaque {@link UpdateCategorieDto} dans la liste contient les informations
     * pour mettre à jour une catégorie existante (si l'ID est fourni et correspond)
     * ou potentiellement pour en créer une nouvelle (si l'ID est null ou absent - dépend de la logique service).
     * La validation {@link Valid @Valid} s'applique à chaque élément de la liste.
     */
    @NotNull(message = "La liste des catégories ne peut pas être nulle (peut être vide pour supprimer toutes les catégories, selon la logique service).")
    @Valid // Valide chaque UpdateCategorieDto dans la liste.
    private List<UpdateCategorieDto> categories;
}

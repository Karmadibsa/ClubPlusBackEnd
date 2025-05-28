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

import java.time.Instant;
import java.util.List;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données nécessaires à la création
 * d'un nouvel {@link Event} **incluant** une liste de ses {@link Categorie}s initiales.
 * Ce DTO combine les informations de base de l'événement avec une liste de {@link CreateCategorieDto}
 * pour permettre la création de l'événement et de ses catégories en une seule requête API.
 * Inclut des annotations de validation pour les champs de l'événement et utilise {@link Valid @Valid}
 * pour déclencher la validation des DTOs de catégorie imbriqués.
 *
 * @see Event
 * @see Categorie
 * @see CreateCategorieDto
 * @see org.clubplus.clubplusbackend.service.EventService (où ce DTO serait probablement traité)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
public class CreateEventWithCategoriesDto {

    /**
     * Nom du nouvel événement. Obligatoire, taille entre 3 et 150.
     */
    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    /**
     * Date et heure de début de l'événement. Obligatoire, présent ou futur.
     */
    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private Instant startTime;

    /**
     * Date et heure de fin de l'événement. Obligatoire, présent ou futur, et après 'start' (validé en service).
     */
    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    private Instant endTime;

    /**
     * Description de l'événement. Obligatoire, max 2000 caractères.
     */
    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    /**
     * Lieu de l'événement. Optionnel, max 255 caractères.
     */
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    private String location; // Optionnel

    /**
     * Liste des catégories à créer pour cet événement.
     * La liste elle-même ne peut pas être nulle (mais peut être vide si aucun catégorie n'est créée initialement).
     * Chaque élément {@link CreateCategorieDto} dans la liste sera validé grâce à l'annotation {@link Valid @Valid}.
     */
    @NotNull(message = "La liste des catégories ne peut pas être nulle (peut être vide).")
    @Valid // Annotation importante pour valider les objets CreateCategorieDto dans la liste.
    private List<CreateCategorieDto> categories;

    // L'organisateur est déterminé via l'utilisateur authentifié dans le service.
}

package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Categorie;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données nécessaires
 * à la création d'une nouvelle {@link Categorie} via l'API.
 * Contient les champs essentiels pour définir une catégorie, avec les
 * annotations de validation Bean Validation (jakarta.validation) correspondantes.
 *
 * @see Categorie
 * @see org.clubplus.clubplusbackend.controller.CategorieController#addCategorieToEvent(Integer, CreateCategorieDto)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
public class CreateCategorieDto {

    /**
     * Nom de la catégorie à créer.
     * Doit être non vide et avoir une longueur comprise entre 2 et 100 caractères.
     */
    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.")
    private String nom;

    /**
     * Capacité maximale de la catégorie à créer.
     * Doit être fournie (non nulle) et être supérieure ou égale à 1 (une capacité de 0
     * pourrait être autorisée selon la logique métier, mais ici 1 est le minimum).
     */
    @NotNull(message = "La capacité est obligatoire.")
    // La validation Min(0) était dans l'entité, mais pour la création,
    // une capacité de 1 minimum est souvent plus logique. À ajuster si 0 est permis.
    @Min(value = 1, message = "La capacité doit être au minimum de 1.")
    private Integer capacite;

    // Note: L'ID de l'événement auquel ajouter la catégorie est généralement fourni
    // via une variable de chemin (@PathVariable) dans le contrôleur, pas dans ce DTO.
}

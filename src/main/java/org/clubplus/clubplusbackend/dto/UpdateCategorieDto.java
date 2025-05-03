package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Categorie;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données lors de la mise à jour
 * partielle d'une {@link Categorie} existante.
 * Les champs (nom, capacite) sont optionnels dans ce DTO : si un champ est fourni (non null),
 * il sera mis à jour ; s'il est null, la valeur existante dans l'entité sera conservée.
 * Des annotations de validation sont présentes pour s'assurer que *si* une valeur est fournie,
 * elle respecte les contraintes définies (taille pour le nom, minimum pour la capacité).
 *
 * @see Categorie
 * @see org.clubplus.clubplusbackend.controller.CategorieController#updateCategorie(Integer, Integer, UpdateCategorieDto)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
public class UpdateCategorieDto {

    /**
     * Le nouvel ID de la catégorie. Généralement non utilisé pour la mise à jour
     * car l'ID est fourni via le chemin de l'URL, mais peut être utile dans certains contextes.
     */
    private Integer id; // Optionnel, dépend de l'usage exact.

    /**
     * Le nouveau nom souhaité pour la catégorie.
     * Ce champ est optionnel. S'il est fourni (non null), il doit avoir
     * une longueur comprise entre 2 et 100 caractères. S'il est null, le nom
     * actuel de la catégorie ne sera pas modifié.
     */
    // Pas de @NotBlank car le champ est optionnel pour la mise à jour.
    @Size(min = 2, max = 100, message = "Si fourni, le nom doit contenir entre 2 et 100 caractères.")
    private String nom;

    /**
     * La nouvelle capacité souhaitée pour la catégorie.
     * Ce champ est optionnel. S'il est fourni (non null), il doit être
     * supérieur ou égal à 0. S'il est null, la capacité actuelle ne sera pas modifiée.
     * La logique métier (service) devra vérifier si la nouvelle capacité est suffisante
     * par rapport aux réservations existantes.
     */
    // Pas de @NotNull car le champ est optionnel.
    @Min(value = 0, message = "Si fournie, la capacité ne peut pas être négative.")
    private Integer capacite;

    // L'ID de l'événement et l'ID de la catégorie à mettre à jour sont fournis
    // via @PathVariable dans le contrôleur.
}

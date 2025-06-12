package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Categorie;

/**
 * DTO (Data Transfer Object) pour la création d'une nouvelle {@link Categorie}.
 * Ce DTO valide et transporte les données nécessaires depuis une requête API.
 */
@Getter
@Setter
public class CreateCategorieDto {

    /**
     * Le nom de la catégorie.
     * Doit contenir entre 2 et 100 caractères.
     */
    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.")
    private String nom;

    /**
     * La capacité maximale de la catégorie.
     * Doit être un nombre supérieur ou égal à 1.
     */
    @NotNull(message = "La capacité est obligatoire.")
    @Min(value = 1, message = "La capacité doit être au minimum de 1.")
    private Integer capacite;

}

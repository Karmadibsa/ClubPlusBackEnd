package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Categorie;

/**
 * DTO (Data Transfer Object) pour la mise à jour partielle d'une {@link Categorie}.
 * Les champs de ce DTO sont optionnels. Seuls les champs non nuls fournis dans la requête
 * seront pris en compte pour la mise à jour.
 */
@Getter
@Setter
public class UpdateCategorieDto {

    /**
     * Le nouvel ID de la catégorie. Ce champ est généralement ignoré lors d'une mise à jour
     * via un endpoint RESTful standard, car l'ID est passé dans l'URL.
     */
    private Integer id;

    /**
     * Le nouveau nom de la catégorie.
     * S'il est fourni (non null), sa longueur doit être comprise entre 2 et 100 caractères.
     * S'il est null, le nom actuel ne sera pas modifié.
     */
    @Size(min = 2, max = 100, message = "Si fourni, le nom doit contenir entre 2 et 100 caractères.")
    private String nom;

    /**
     * La nouvelle capacité de la catégorie.
     * Si elle est fournie (non null), elle doit être supérieure ou égale à 0.
     * S'il est null, la capacité actuelle ne sera pas modifiée.
     * La logique métier doit vérifier si cette nouvelle capacité est compatible avec les réservations existantes.
     */
    @Min(value = 0, message = "Si fournie, la capacité ne peut pas être négative.")
    private Integer capacite;

}

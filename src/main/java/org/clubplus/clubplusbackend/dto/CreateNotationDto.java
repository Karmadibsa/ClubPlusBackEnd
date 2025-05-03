package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Notation;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données d'une nouvelle
 * {@link Notation} soumise par un utilisateur pour un événement.
 * Contient les notes pour chaque critère défini (ambiance, propreté, etc.).
 * Inclut des annotations de validation pour s'assurer que chaque note est fournie
 * et se situe dans la plage autorisée (typiquement 1 à 5).
 *
 * @see Notation
 * @see org.clubplus.clubplusbackend.controller.NotationController (où ce DTO serait probablement utilisé)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
public class CreateNotationDto {

    // Les IDs de l'événement et du membre qui note sont généralement fournis
    // via @PathVariable ou déterminés par l'utilisateur authentifié, pas dans ce DTO.

    /**
     * Note pour l'ambiance. Obligatoire, entre 1 et 5.
     */
    @NotNull(message = "La note d'ambiance est obligatoire.")
    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5")
    private Integer ambiance;

    /**
     * Note pour la propreté. Obligatoire, entre 1 et 5.
     */
    @NotNull(message = "La note de propreté est obligatoire.")
    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    private Integer proprete;

    /**
     * Note pour l'organisation. Obligatoire, entre 1 et 5.
     */
    @NotNull(message = "La note d'organisation est obligatoire.")
    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    private Integer organisation;

    /**
     * Note pour le fair-play. Obligatoire, entre 1 et 5.
     */
    @NotNull(message = "La note de fair-play est obligatoire.")
    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    private Integer fairPlay;

    /**
     * Note pour le niveau des joueurs. Obligatoire, entre 1 et 5.
     */
    @NotNull(message = "La note du niveau des joueurs est obligatoire.")
    @Min(value = 1, message = "La note du niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note du niveau des joueurs doit être au maximum 5")
    private Integer niveauJoueurs;
}

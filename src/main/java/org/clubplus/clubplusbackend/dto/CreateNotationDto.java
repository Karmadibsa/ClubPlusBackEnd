package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Notation;

/**
 * DTO (Data Transfer Object) pour la création d'une nouvelle {@link Notation} d'événement.
 * Ce DTO valide et transporte les notes attribuées par un utilisateur pour différents critères.
 */
@Getter
@Setter
public class CreateNotationDto {

    /**
     * Note pour l'ambiance générale de l'événement.
     * Doit être comprise entre 1 et 5.
     */
    @NotNull(message = "La note d'ambiance est obligatoire.")
    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5")
    private Integer ambiance;

    /**
     * Note pour la propreté des lieux.
     * Doit être comprise entre 1 et 5.
     */
    @NotNull(message = "La note de propreté est obligatoire.")
    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    private Integer proprete;

    /**
     * Note pour l'organisation de l'événement.
     * Doit être comprise entre 1 et 5.
     */
    @NotNull(message = "La note d'organisation est obligatoire.")
    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    private Integer organisation;

    /**
     * Note pour le fair-play des participants.
     * Doit être comprise entre 1 et 5.
     */
    @NotNull(message = "La note de fair-play est obligatoire.")
    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    private Integer fairPlay;

    /**
     * Note pour le niveau général des joueurs.
     * Doit être comprise entre 1 et 5.
     */
    @NotNull(message = "La note du niveau des joueurs est obligatoire.")
    @Min(value = 1, message = "La note du niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note du niveau des joueurs doit être au maximum 5")
    private Integer niveauJoueurs;
}

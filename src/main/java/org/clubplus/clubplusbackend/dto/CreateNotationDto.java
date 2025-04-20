package org.clubplus.clubplusbackend.dto; // Ou votre package DTO

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotationDto {

    // Utiliser @NotNull pour s'assurer que la note est fournie
    // Utiliser @Min et @Max pour valider la plage

    @NotNull(message = "La note d'ambiance est obligatoire.")
    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5")
    private Integer ambiance;

    @NotNull(message = "La note de propreté est obligatoire.")
    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    private Integer proprete; // Attention à l'accent si c'est le nom exact du champ

    @NotNull(message = "La note d'organisation est obligatoire.")
    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    private Integer organisation;

    @NotNull(message = "La note de fair-play est obligatoire.")
    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    private Integer fairPlay;

    @NotNull(message = "La note du niveau des joueurs est obligatoire.")
    @Min(value = 1, message = "La note du niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note du niveau des joueurs doit être au maximum 5")
    private Integer niveauJoueurs;
}

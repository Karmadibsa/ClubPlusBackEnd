package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.clubplus.clubplusbackend.model.Event;

import java.time.Instant;

/**
 * DTO (Data Transfer Object) pour la mise à jour des informations d'un {@link Event}.
 * Ce DTO transporte les champs modifiables d'un événement et valide leur contenu.
 * Tous les champs sont obligatoires, impliquant une mise à jour complète de ces informations.
 */
@Data
public class UpdateEventDto {

    /**
     * Le nouveau nom de l'événement.
     */
    @NotBlank(message = "Le nom de l'événement est obligatoire.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    /**
     * La nouvelle date et heure de début de l'événement.
     * Doit être dans le présent ou le futur.
     */
    @NotNull(message = "La date et heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private Instant startTime;

    /**
     * La nouvelle date et heure de fin de l'événement.
     * Doit être dans le futur. La cohérence avec startTime est vérifiée dans le service.
     */
    @NotNull(message = "La date et heure de fin sont obligatoires.")
    @Future(message = "La date de fin doit être dans le futur.")
    private Instant endTime;

    /**
     * La nouvelle description de l'événement.
     */
    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères.")
    private String description;

    /**
     * Le nouveau lieu de l'événement.
     */
    @NotBlank(message = "La localisation est obligatoire.")
    @Size(max = 100, message = "La localisation ne doit pas dépasser 100 caractères.")
    private String location;
}

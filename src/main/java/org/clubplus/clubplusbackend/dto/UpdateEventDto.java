package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.clubplus.clubplusbackend.model.Event;

import java.time.Instant;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données nécessaires
 * à la mise à jour des informations d'un {@link Event} existant.
 * Permet de modifier le nom, les dates, la description et le lieu.
 * Tous les champs sont marqués comme obligatoires, suggérant une mise à jour
 * complète des informations modifiables. Pour une mise à jour partielle,
 * les annotations de validation comme {@code @NotBlank}/{@code @NotNull} pourraient être retirées.
 *
 * @see Event
 */
@Data // Lombok: Raccourci pour @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor.
public class UpdateEventDto {

    // L'ID de l'événement à mettre à jour est fourni via @PathVariable dans le contrôleur.

    /**
     * Le nouveau nom de l'événement. Obligatoire, taille 3-150.
     */
    @NotBlank(message = "Le nom de l'événement est obligatoire.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    private String nom;

    /**
     * La nouvelle date/heure de début. Obligatoire, présent ou futur.
     */
    @NotNull(message = "La date et heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    private Instant startTime;

    /**
     * La nouvelle date/heure de fin. Obligatoire, futur et après 'start' (validé en service).
     */
    @NotNull(message = "La date et heure de fin sont obligatoires.")
    @Future(message = "La date de fin doit être dans le futur.") // @Future est plus strict que @FutureOrPresent ici
    private Instant endTime;
    // La validation end > start est gérée dans le service.

    /**
     * La nouvelle description. Obligatoire, max 500 (ajuster si besoin).
     */
    @NotBlank(message = "La description est obligatoire.")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères.") // Taille max
    private String description;

    /**
     * Le nouveau lieu. Obligatoire, max 100 (ajuster si besoin).
     */
    @NotBlank(message = "La localisation est obligatoire.") // Si la localisation devient obligatoire à l'update
    @Size(max = 100, message = "La localisation ne doit pas dépasser 100 caractères.") // Taille max
    private String location;

    // Les champs comme l'organisateur ou le statut 'actif' ne sont généralement pas modifiés via ce DTO.
    // La gestion des catégories est faite via UpdateEventWithCategoriesDto ou des endpoints dédiés.
}

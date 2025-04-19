package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateEventDto {

    @NotBlank
    @Size(min = 3, max = 150)
    private String nom;

    @NotNull
    @FutureOrPresent // Ou juste @Future si on ne peut pas le ramener à maintenant
    private LocalDateTime start;

    @NotNull
    @Future
    private LocalDateTime end;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String location;

    // Ajoutez d'autres champs si vous voulez permettre leur modification
    // Mais PAS id, organisateur, actif (sauf si c'est une action spécifique)
}

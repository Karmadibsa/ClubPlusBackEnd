package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventDto {

    @NotBlank
    @Size(min = 3, max = 150)
    private String nom;

    @NotNull
    @Future
    private LocalDateTime start;

    @NotNull
    @Future // Doit être dans le futur et après 'start' (validation supplémentaire dans le service)
    private LocalDateTime end;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String location;

    // PAS DE CHAMP organisateur ici !
}

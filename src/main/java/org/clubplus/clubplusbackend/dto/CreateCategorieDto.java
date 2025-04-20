package org.clubplus.clubplusbackend.dto; // Ou un autre package approprié pour les DTOs

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategorieDto {

    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.")
    private String nom;

    @NotNull(message = "La capacité est obligatoire.")
    @Min(value = 1, message = "La capacité ne peut pas être négative.")
    private Integer capacite;
}

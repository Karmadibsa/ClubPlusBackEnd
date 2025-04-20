package org.clubplus.clubplusbackend.dto; // Ou votre package DTO

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategorieDto {

    // Le nom est optionnel, mais s'il est fourni, il doit être valide
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.")
    private String nom; // Pas de @NotBlank, car on peut ne pas vouloir le changer

    // La capacité est optionnelle, mais si elle est fournie, elle doit être valide
    @Min(value = 0, message = "La capacité ne peut pas être négative.")
    private Integer capacite; // Pas de @NotNull, car on peut ne pas vouloir la changer

}

package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactFormDto {

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String name;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'adresse email doit être valide.")
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères.")
    private String email;

    @NotBlank(message = "Le sujet est obligatoire.")
    @Size(max = 150, message = "Le sujet ne doit pas dépasser 150 caractères.")
    private String subject;

    @NotBlank(message = "Le message est obligatoire.")
    @Size(min = 10, message = "Le message doit contenir au moins 10 caractères.")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères.")
    private String message;

}

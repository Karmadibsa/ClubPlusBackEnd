package org.clubplus.clubplusbackend.dto; // Dans votre package DTO

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data // Lombok pour @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    private String password;
}

package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClubDto {

    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100)
    private String nom;

    @NotBlank
    @Size(max = 10)
    private String numero_voie;

    @NotBlank
    @Size(max = 100)
    private String rue;

    @NotBlank
    @Size(min = 3, max = 10)
    private String codepostal;

    @NotBlank
    @Size(max = 100)
    private String ville;

    @NotBlank
    @Size(max = 20)
    private String telephone;

    @NotBlank(message = "L'email du club est obligatoire.")
    @Email
    @Size(max = 254)
    private String email;

}

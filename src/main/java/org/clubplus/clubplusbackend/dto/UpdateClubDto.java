package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.clubplus.clubplusbackend.model.Club;

/**
 * DTO (Data Transfer Object) pour la mise à jour des informations d'un {@link Club}.
 * Ce DTO transporte les champs modifiables d'un club et valide leur contenu.
 * Tous les champs sont obligatoires, impliquant une mise à jour complète de ces informations.
 */
@Data
public class UpdateClubDto {

    /**
     * Le nouveau nom du club.
     */
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit avoir entre 2 et 100 caractères.")
    private String nom;

    /**
     * Le nouveau numéro de voie de l'adresse.
     */
    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    private String numero_voie;

    /**
     * La nouvelle rue de l'adresse.
     */
    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    private String rue;

    /**
     * Le nouveau code postal.
     */
    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    private String codepostal;

    /**
     * La nouvelle ville.
     */
    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    private String ville;

    /**
     * Le nouveau numéro de téléphone.
     */
    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    private String telephone;

    /**
     * Le nouvel email du club, qui doit rester unique.
     */
    @NotBlank(message = "L'email du club est obligatoire.")
    @Email(message = "Le format de l'email du club est invalide.")
    @Size(max = 254, message = "L'email du club ne doit pas dépasser 254 caractères.")
    private String email;
}

package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Membre;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) pour la mise à jour du profil d'un {@link Membre}.
 * <p>
 * Ce DTO transporte les champs modifiables du profil utilisateur et valide leur contenu.
 * Tous les champs sont obligatoires, impliquant une mise à jour complète de ces informations.
 */
@Getter
@Setter
public class UpdateMembreDto {

    /**
     * Le nouveau prénom du membre.
     */
    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères.")
    private String prenom;

    /**
     * Le nouveau nom de famille du membre.
     */
    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères.")
    private String nom;

    /**
     * La nouvelle date de naissance du membre. Doit être dans le passé.
     */
    @NotNull(message = "La date de naissance est requise")
    @Past(message = "La date de naissance doit être dans le passé.")
    private LocalDate date_naissance;

    /**
     * Le nouvel email du membre, qui doit rester unique.
     */
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.")
    private String email;

    /**
     * Le nouveau numéro de téléphone du membre.
     */
    @NotBlank(message = "Le téléphone est requis")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    private String telephone;
}

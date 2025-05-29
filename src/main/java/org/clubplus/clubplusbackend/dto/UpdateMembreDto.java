package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.model.Membre;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données nécessaires à la mise à jour
 * du profil d'un {@link Membre} existant.
 * Permet de modifier les informations personnelles (nom, prénom, date naissance),
 * les informations de contact (email, téléphone), et l'adresse.
 *
 * <p>La plupart des champs sont marqués comme obligatoires ({@code @NotBlank}/{@code @NotNull}),
 * indiquant une mise à jour complète du profil modifiable. Si une mise à jour partielle était
 * souhaitée, ces annotations pourraient être ajustées.</p>
 *
 * <p>Ne contient pas de champ pour le mot de passe, le rôle, ou le statut actif, car
 * la modification de ces éléments est généralement gérée via des endpoints ou logiques spécifiques.</p>
 *
 * @see Membre
 * @see org.clubplus.clubplusbackend.controller.MembreController (où ce DTO serait utilisé)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
public class UpdateMembreDto {

    // L'ID du membre à mettre à jour est fourni via @PathVariable ou déterminé par l'utilisateur authentifié.

    /**
     * Le nouveau prénom du membre. Obligatoire.
     */
    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères.") // Ajout taille
    private String prenom;

    /**
     * Le nouveau nom de famille du membre. Obligatoire.
     */
    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères.") // Ajout taille
    private String nom;

    /**
     * La nouvelle date de naissance. Obligatoire et doit être dans le passé.
     */
    @NotNull(message = "La date de naissance est requise")
    @Past(message = "La date de naissance doit être dans le passé.") // Validation ajoutée
    private LocalDate date_naissance;

    /**
     * Le nouvel email du membre. Obligatoire, format valide. Doit être unique.
     */
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.") // Ajout taille
    private String email;

    /**
     * Le nouveau numéro de téléphone. Obligatoire.
     */
    @NotBlank(message = "Le téléphone est requis")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.") // Ajout taille
    private String telephone;
}

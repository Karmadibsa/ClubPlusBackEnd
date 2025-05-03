package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.clubplus.clubplusbackend.model.Club;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les données nécessaires
 * à la mise à jour des informations d'un {@link Club} existant.
 * Ce DTO inclut les champs modifiables d'un club (nom, adresse, contact).
 * Tous les champs sont marqués comme obligatoires ({@code @NotBlank}) car ce DTO
 * est probablement destiné à une mise à jour complète des informations modifiables.
 * Si une mise à jour partielle était souhaitée, les annotations {@code @NotBlank}
 * pourraient être retirées pour certains champs.
 *
 * @see Club
 * @see org.clubplus.clubplusbackend.controller.ClubController#updateClub(Integer, UpdateClubDto)
 */
@Data // Lombok: Raccourci pour @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor.
public class UpdateClubDto {

    // L'ID du club à mettre à jour est fourni via @PathVariable dans le contrôleur.

    /**
     * Le nouveau nom du club. Obligatoire, taille 2-100.
     */
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit avoir entre 2 et 100 caractères.")
    private String nom;

    /**
     * Le nouveau numéro de voie. Obligatoire, max 10.
     */
    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    private String numero_voie;

    /**
     * La nouvelle rue. Obligatoire, max 100.
     */
    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    private String rue;

    /**
     * Le nouveau code postal. Obligatoire, taille 3-10.
     */
    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    private String codepostal;

    /**
     * La nouvelle ville. Obligatoire, max 100.
     */
    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    private String ville;

    /**
     * Le nouveau numéro de téléphone. Obligatoire, max 20.
     */
    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    private String telephone;

    /**
     * Le nouvel email du club. Obligatoire, format valide, max 254. Doit être unique.
     */
    @NotBlank(message = "L'email du club est obligatoire.")
    @Email(message = "Le format de l'email du club est invalide.")
    @Size(max = 254, message = "L'email du club ne doit pas dépasser 254 caractères.")
    private String email;

    // Les champs non modifiables (date_creation, date_inscription, codeClub, actif, etc.) ne sont pas inclus.
}

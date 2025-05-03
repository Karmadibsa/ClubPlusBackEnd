package org.clubplus.clubplusbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.clubplus.clubplusbackend.controller.AuthController;

/**
 * DTO (Data Transfer Object) utilisé pour recevoir les informations d'identification
 * (email et mot de passe) d'un utilisateur lors d'une tentative de connexion via l'API.
 * Contient les annotations de validation pour s'assurer que les champs sont présents et valides.
 *
 * @see AuthController#connexion(LoginRequestDto)
 */
@Data // Lombok: Raccourci pour @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor.
public class LoginRequestDto {

    /**
     * Adresse email de l'utilisateur tentant de se connecter.
     * Obligatoire et doit être un format d'email valide.
     */
    @NotBlank(message = "L'email est obligatoire.") // Ne peut pas être null ou vide.
    @Email(message = "Le format de l'email est invalide.") // Doit respecter le format email standard.
    private String email;

    /**
     * Mot de passe fourni par l'utilisateur pour la connexion.
     * Obligatoire. La validation de la longueur ou de la complexité n'est généralement
     * pas faite ici, mais lors de l'inscription. Le service d'authentification
     * comparera ce mot de passe (après hachage si nécessaire côté client, bien que non recommandé)
     * avec le hash stocké en base de données.
     */
    @NotBlank(message = "Le mot de passe est obligatoire.") // Ne peut pas être null ou vide.
    private String password;
}

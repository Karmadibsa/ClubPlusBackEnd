package org.clubplus.clubplusbackend.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classe de test pour les contraintes de validation de l'entité {@link Membre}.
 * Vérifie que les annotations de Bean Validation (ex: @NotBlank, @Size, @Email, @Past)
 * sur les champs de l'entité Membre fonctionnent comme attendu.
 */
class MembreTest {

    /**
     * Validateur utilisé pour tester les contraintes sur l'objet Membre.
     */
    private Validator validator;

    /**
     * Initialise le validateur avant chaque test.
     * Cette méthode est exécutée avant chaque méthode annotée avec @Test.
     */
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Crée une instance de Membre avec des données valides.
     * Utilisé comme base pour les tests, où des champs spécifiques sont ensuite modifiés
     * pour tester les violations de contraintes.
     *
     * @return Une instance de Membre avec des valeurs conformes aux contraintes.
     */
    private Membre createValidMembre() {
        Membre membre = new Membre();
        membre.setNom("ValideNom");
        membre.setPrenom("ValidePrenom");
        membre.setDate_naissance(LocalDate.now().minusYears(20)); // Date dans le passé
        membre.setDate_inscription(LocalDate.now()); // Date actuelle
        membre.setTelephone("0123456789"); // Format téléphone valide (simple exemple)
        membre.setEmail("valide@example.com"); // Format email valide
        membre.setPassword("ValidPass1!"); // Mot de passe respectant les contraintes de complexité
        membre.setRole(Role.MEMBRE); // Rôle valide
        membre.setActif(true);
        membre.setVerified(false); // La vérification n'est pas testée ici pour la validité de base
        // Le champ codeAmi est typiquement généré après la persistance (@PostPersist),
        // donc il n'est pas inclus dans la validation à la création de l'objet ici.
        return membre;
    }

    /**
     * Teste qu'un objet Membre créé avec des données valides ne produit aucune violation de contrainte.
     */
    @Test
    @DisplayName("Un membre valide ne doit générer aucune violation de contrainte")
    void whenMembreIsValid_thenNoViolations() {
        Membre membre = createValidMembre();
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertTrue(violations.isEmpty(), "Un membre valide ne devrait pas avoir de violations de contraintes.");
    }

    // --- Tests pour les champs obligatoires (@NotBlank, @NotNull) ---

    /**
     * Teste qu'une violation est générée si le champ 'nom' est vide ou nul.
     */
    @Test
    @DisplayName("Le nom vide ou nul doit générer une violation")
    void whenNomIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setNom(""); // Scénario: nom vide
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un nom vide devrait générer une violation.");
        // Vérifie que la violation concerne bien le champ 'nom' et contient le message attendu.
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom ne peut pas être vide")));
    }

    /**
     * Teste qu'une violation est générée si le champ 'prenom' est vide ou nul.
     */
    @Test
    @DisplayName("Le prénom vide ou nul doit générer une violation")
    void whenPrenomIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setPrenom(" "); // Scénario: prénom contenant seulement un espace (considéré comme vide par @NotBlank)
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un prénom vide devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("prenom") && v.getMessage().contains("Le prénom ne peut pas être vide")));
    }

    /**
     * Teste qu'une violation est générée si le champ 'date_naissance' est nul.
     */
    @Test
    @DisplayName("La date de naissance nulle doit générer une violation")
    void whenDateNaissanceIsNull_thenViolation() {
        Membre membre = createValidMembre();
        membre.setDate_naissance(null); // Scénario: date de naissance nulle
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Une date de naissance nulle devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_naissance") && v.getMessage().contains("La date de naissance est obligatoire")));
    }

    /**
     * Teste qu'une violation est générée si le champ 'telephone' est vide ou nul.
     */
    @Test
    @DisplayName("Le téléphone vide ou nul doit générer une violation")
    void whenTelephoneIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setTelephone(null); // Scénario: téléphone nul
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un téléphone nul devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("telephone") && v.getMessage().contains("Le numéro de téléphone est obligatoire")));
    }

    /**
     * Teste qu'une violation est générée si le champ 'email' est vide ou nul.
     */
    @Test
    @DisplayName("L'email vide ou nul doit générer une violation")
    void whenEmailIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setEmail(""); // Scénario: email vide
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un email vide devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("L'email est obligatoire")));
    }

    /**
     * Teste qu'une violation est générée si le champ 'password' est vide ou nul.
     */
    @Test
    @DisplayName("Le mot de passe vide ou nul doit générer une violation")
    void whenPasswordIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setPassword(null); // Scénario: mot de passe nul
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un mot de passe nul devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password") && v.getMessage().contains("Le mot de passe est obligatoire")));
    }

    // --- Tests pour @Size (contraintes de taille) ---

    /**
     * Teste qu'une violation est générée si le champ 'nom' est trop court.
     */
    @Test
    @DisplayName("Le nom trop court doit générer une violation")
    void whenNomIsTooShort_thenViolation() {
        Membre membre = createValidMembre();
        membre.setNom("N"); // Scénario: nom d'un seul caractère
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un nom trop court devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom doit contenir entre 2 et 50 caractères")));
    }

    /**
     * Teste qu'une violation est générée si le champ 'email' est trop long.
     */
    @Test
    @DisplayName("L'email trop long doit générer une violation")
    void whenEmailIsTooLong_thenViolation() {
        Membre membre = createValidMembre();
        membre.setEmail("a".repeat(250) + "@example.com"); // Construit un email de plus de 254 caractères
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un email trop long devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("L'email ne doit pas dépasser 254 caractères")));
    }

    // --- Tests pour les formats et logiques spécifiques (ex: @Past, @Email, @Pattern) ---

    /**
     * Teste qu'une violation est générée si la 'date_naissance' est dans le futur.
     */
    @Test
    @DisplayName("La date de naissance dans le futur doit générer une violation")
    void whenDateNaissanceIsInFuture_thenViolation() {
        Membre membre = createValidMembre();
        membre.setDate_naissance(LocalDate.now().plusDays(1)); // Scénario: date de naissance demain
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Une date de naissance dans le futur devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_naissance") && v.getMessage().contains("La date de naissance doit être dans le passé")));
    }

    /**
     * Teste qu'une violation est générée si l'email' n'a pas un format valide.
     */
    @Test
    @DisplayName("L'email avec un format invalide doit générer une violation")
    void whenEmailIsInvalidFormat_thenViolation() {
        Membre membre = createValidMembre();
        membre.setEmail("invalid-email-format"); // Scénario: format d'email incorrect
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un email au format invalide devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("Le format de l'email est invalide")));
    }

    /**
     * Teste qu'une violation est générée si le 'password' ne respecte pas le pattern de complexité défini.
     */
    @Test
    @DisplayName("Le mot de passe ne respectant pas le pattern doit générer une violation")
    void whenPasswordDoesNotMatchPattern_thenViolation() {
        Membre membre = createValidMembre();
        membre.setPassword("short"); // Scénario: mot de passe trop simple et/ou trop court
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty(), "Un mot de passe ne respectant pas le pattern devrait générer une violation.");
        // Le message exact dépendra de celui défini dans votre annotation @Pattern ou de la composition des messages de plusieurs contraintes.
        // Ici, on vérifie une partie commune du message attendu pour la complexité.
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password") && v.getMessage().contains("Le mot de passe doit faire entre 8 et 100 caractères")));
    }

    // Note: Vous pouvez ajouter d'autres tests pour couvrir toutes les contraintes de chaque champ,
    // y compris les limites maximales de taille pour nom, prénom, téléphone, etc.
    // Il est également bon de tester les valeurs limites (par exemple, un nom de 50 caractères vs 51).
}

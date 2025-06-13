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
 * Classe de test pour les contraintes de validation de l'entité {@link Club}.
 * Vérifie que les annotations de Bean Validation (ex: @NotBlank, @Size, @Email, @PastOrPresent)
 * sur les champs de l'entité Club fonctionnent comme attendu.
 * Ces tests n'interagissent pas avec la base de données et se concentrent uniquement
 * sur la validité de l'objet Club lui-même.
 */
class ClubTest {

    /**
     * Instance du validateur JSR 380 (Bean Validation) utilisée pour tester les contraintes.
     */
    private Validator validator;

    /**
     * Méthode de configuration exécutée avant chaque test (@Test).
     * Initialise le validateur.
     */
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Crée une instance de Club avec des données valides, conformes à toutes les contraintes de validation.
     * Cette instance sert de base pour les tests, où des champs spécifiques sont ensuite
     * modifiés pour provoquer et vérifier les violations de contraintes.
     *
     * @return Une instance de {@link Club} avec des valeurs valides.
     */
    private Club createValidClub() {
        Club club = new Club();
        club.setNom("Club Sportif Valide");
        club.setDate_creation(LocalDate.of(2024, 1, 15));
        club.setDate_inscription(LocalDate.now().minusMonths(11));
        club.setNumero_voie("12B");
        club.setRue("Rue Principale");
        club.setCodepostal("75001");
        club.setVille("Paris");
        club.setTelephone("0123456789");
        club.setEmail("contact.valide@clubexample.com");
        club.setCodeClub("CLUB001");
        club.setActif(true);
        return club;
    }

    /**
     * Teste qu'un objet Club créé avec des données valides ne produit aucune violation de contrainte.
     */
    @Test
    @DisplayName("Un club valide ne doit générer aucune violation de contrainte")
    void quandClubEstValide_alorsAucuneViolation() {
        Club club = createValidClub();
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertTrue(violations.isEmpty(), "Un club valide ne devrait pas avoir de violations de contraintes.");
    }

    // --- Tests pour les champs obligatoires (@NotBlank, @NotNull) ---

    @Test
    @DisplayName("Nom du club vide doit générer une violation")
    void quandNomClubEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setNom(" "); // Teste avec une chaîne contenant seulement des espaces (non blank)
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty(), "Un nom de club vide devrait générer une violation.");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom du club ne peut pas être vide.")));
    }

    @Test
    @DisplayName("Date de création nulle doit générer une violation")
    void quandDateCreationEstNulle_alorsViolation() {
        Club club = createValidClub();
        club.setDate_creation(null);
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_creation") && v.getMessage().contains("La date de création est obligatoire.")));
    }

    @Test
    @DisplayName("Date d'inscription nulle doit générer une violation")
    void quandDateInscriptionEstNulle_alorsViolation() {
        Club club = createValidClub();
        club.setDate_inscription(null);
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_inscription") && v.getMessage().contains("La date d'inscription est obligatoire.")));
    }

    @Test
    @DisplayName("Numéro de voie vide doit générer une violation")
    void quandNumeroVoieEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setNumero_voie("");
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("numero_voie") && v.getMessage().contains("Le numéro de voie est obligatoire.")));
    }

    @Test
    @DisplayName("Rue vide doit générer une violation")
    void quandRueEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setRue(" "); // Espace est considéré comme vide par @NotBlank
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rue") && v.getMessage().contains("La rue est obligatoire.")));
    }

    @Test
    @DisplayName("Code postal vide doit générer une violation")
    void quandCodePostalEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setCodepostal(null);
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("codepostal") && v.getMessage().contains("Le code postal est obligatoire.")));
    }

    @Test
    @DisplayName("Ville vide doit générer une violation")
    void quandVilleEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setVille("");
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("ville") && v.getMessage().contains("La ville est obligatoire.")));
    }

    @Test
    @DisplayName("Téléphone vide doit générer une violation")
    void quandTelephoneEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setTelephone(" ");
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("telephone") && v.getMessage().contains("Le numéro de téléphone est obligatoire.")));
    }

    @Test
    @DisplayName("Email vide doit générer une violation")
    void quandEmailEstVide_alorsViolation() {
        Club club = createValidClub();
        club.setEmail(null);
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("L'email du club est obligatoire.")));
    }

    @Test
    @DisplayName("Champ 'actif' nul doit générer une violation")
    void quandActifEstNul_alorsViolation() {
        // Arrange
        Club club = createValidClub();
        club.setActif(null); // On met le champ à null pour déclencher la violation

        // Act
        Set<ConstraintViolation<Club>> violations = validator.validate(club);

        // Assert
        // On vérifie d'abord qu'il y a bien au moins une violation
        assertFalse(violations.isEmpty(), "Le champ 'actif' nul devrait générer une violation.");

        // Ensuite, on vérifie la violation spécifique
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("actif") &&
                        // CORRECTION : On vérifie le message par défaut, plus robuste.
                        v.getMessage().equals("ne doit pas être nul")
        ), "La violation pour 'actif' nul n'a pas été trouvée ou le message ne correspond pas.");
    }


    // --- Tests pour @Size ---

    @Test
    @DisplayName("Nom du club trop court doit générer une violation")
    void quandNomClubEstTropCourt_alorsViolation() {
        Club club = createValidClub();
        club.setNom("C"); // Un seul caractère, alors que min=2
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom du club doit contenir entre 2 et 100 caractères.")));
    }

    @Test
    @DisplayName("Nom du club trop long doit générer une violation")
    void quandNomClubEstTropLong_alorsViolation() {
        Club club = createValidClub();
        club.setNom("a".repeat(101)); // 101 caractères, alors que max=100
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom du club doit contenir entre 2 et 100 caractères.")));
    }

    @Test
    @DisplayName("Numéro de voie trop long doit générer une violation")
    void quandNumeroVoieEstTropLong_alorsViolation() {
        Club club = createValidClub();
        club.setNumero_voie("12345678901"); // 11 caractères, alors que max=10
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("numero_voie") && v.getMessage().contains("Le numéro de voie ne doit pas dépasser 10 caractères.")));
    }

    // Ajouter des tests similaires pour @Size sur rue, codepostal, ville, telephone, email.

    // --- Tests pour les formats et logiques spécifiques ---

    @Test
    @DisplayName("Date de création dans le futur doit générer une violation")
    void quandDateCreationEstDansLeFutur_alorsViolation() {
        Club club = createValidClub();
        club.setDate_creation(LocalDate.of(2035, 1, 15));
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_creation") && v.getMessage().contains("La date de création doit être dans le passé ou aujourd'hui.")));
    }

    @Test
    @DisplayName("Date d'inscription dans le futur doit générer une violation")
    void quandDateInscriptionEstDansLeFutur_alorsViolation() {
        Club club = createValidClub();
        club.setDate_inscription(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_inscription") && v.getMessage().contains("La date d'inscription doit être dans le passé ou aujourd'hui.")));
    }

    @Test
    @DisplayName("Email du club avec format invalide doit générer une violation")
    void quandEmailClubEstInvalideFormat_alorsViolation() {
        Club club = createValidClub();
        club.setEmail("format-email-invalide");
        Set<ConstraintViolation<Club>> violations = validator.validate(club);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("Le format de l'email est invalide.")));
    }

    // Note: Les contraintes d'unicité (comme @Column(unique=true) pour email et codeClub)
    // ne sont pas testées ici car elles sont vérifiées au niveau de la base de données.
    // Ces tests d'unicité sont effectués dans ClubDaoTest.

    // Note: La logique de génération de codeClub (@PostPersist) n'est pas testée ici
    // car cela concerne le cycle de vie de la persistance, non la validation de l'objet.
}

package org.clubplus.clubplusbackend.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembreTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private Membre createValidMembre() {
        Membre membre = new Membre();
        membre.setNom("ValideNom");
        membre.setPrenom("ValidePrenom");
        membre.setDate_naissance(LocalDate.now().minusYears(20));
        membre.setDate_inscription(LocalDate.now());
        membre.setTelephone("0123456789");
        membre.setEmail("valide@example.com");
        membre.setPassword("ValidPass1!");
        membre.setRole(Role.MEMBRE);
        membre.setActif(true);
        membre.setVerified(false);
        // codeAmi est généré @PostPersist, donc non testé ici directement pour la validation à la création
        return membre;
    }

    @Test
    void whenMembreIsValid_thenNoViolations() {
        Membre membre = createValidMembre();
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertTrue(violations.isEmpty(), "Un membre valide ne devrait pas avoir de violations");
    }

    // --- Tests pour les champs obligatoires (@NotBlank, @NotNull) ---

    @Test
    void whenNomIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setNom(""); // ou null
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom ne peut pas être vide")));
    }

    @Test
    void whenPrenomIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setPrenom(" "); // ou null
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("prenom") && v.getMessage().contains("Le prénom ne peut pas être vide")));
    }

    @Test
    void whenDateNaissanceIsNull_thenViolation() {
        Membre membre = createValidMembre();
        membre.setDate_naissance(null);
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_naissance") && v.getMessage().contains("La date de naissance est obligatoire")));
    }

    @Test
    void whenTelephoneIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setTelephone(null);
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("telephone") && v.getMessage().contains("Le numéro de téléphone est obligatoire")));
    }

    @Test
    void whenEmailIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setEmail("");
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("L'email est obligatoire")));
    }

    @Test
    void whenPasswordIsBlank_thenViolation() {
        Membre membre = createValidMembre();
        membre.setPassword(null);
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password") && v.getMessage().contains("Le mot de passe est obligatoire")));
    }

    // --- Tests pour @Size ---
    @Test
    void whenNomIsTooShort_thenViolation() {
        Membre membre = createValidMembre();
        membre.setNom("N");
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom") && v.getMessage().contains("Le nom doit contenir entre 2 et 50 caractères")));
    }

    @Test
    void whenEmailIsTooLong_thenViolation() {
        Membre membre = createValidMembre();
        membre.setEmail("a".repeat(250) + "@example.com"); // > 254
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("L'email ne doit pas dépasser 254 caractères")));
    }


    // --- Tests pour les formats et logiques spécifiques ---

    @Test
    void whenDateNaissanceIsInFuture_thenViolation() {
        Membre membre = createValidMembre();
        membre.setDate_naissance(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date_naissance") && v.getMessage().contains("La date de naissance doit être dans le passé")));
    }

    @Test
    void whenEmailIsInvalidFormat_thenViolation() {
        Membre membre = createValidMembre();
        membre.setEmail("invalid-email");
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email") && v.getMessage().contains("Le format de l'email est invalide")));
    }

    @Test
    void whenPasswordDoesNotMatchPattern_thenViolation() {
        Membre membre = createValidMembre();
        membre.setPassword("short"); // Ne respecte pas le pattern
        Set<ConstraintViolation<Membre>> violations = validator.validate(membre);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password") && v.getMessage().contains("Le mot de passe doit faire entre 8 et 100 caractères")));
    }

    // Vous pouvez ajouter plus de tests pour chaque contrainte et chaque champ.
    // Par exemple, tester les limites de taille pour nom, prenom, telephone.
}

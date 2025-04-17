package org.clubplus.clubplusbackend.TestUnitaire.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

public class MembreTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void createValidMembre_shouldNotThrowException() {
        // Arrange: Crée une instance et initialise tous les champs requis/contraints
        Membre membreTest = new Membre();

        // Act: Attribue des valeurs valides respectant les contraintes
        membreTest.setNom("Dupont");               // @NotBlank, @Size(min=2, max=50)
        membreTest.setPrenom("Jean");                // @NotBlank, @Size(min=2, max=50)
        membreTest.setDate_naissance(LocalDate.of(1990, 1, 15)); // @NotNull, @Past
        membreTest.setDate_inscription(LocalDate.now());     // @NotNull
        membreTest.setNumero_voie("10 Bis");           // @NotBlank, @Size(max=10)
        membreTest.setRue("Avenue des Tests");         // @NotBlank, @Size(max=100)
        membreTest.setCodepostal("75010");             // @NotBlank, @Size(min=3, max=10)
        membreTest.setVille("Paris");                  // @NotBlank, @Size(max=100)
        membreTest.setTelephone("0123456789");         // @NotBlank, @Size(max=20)
        membreTest.setEmail("jean.dupont@test.com");   // @NotBlank, @Email, @Size(max=254)
        membreTest.setPassword("motdepasseSecurise123"); // @NotBlank, @Size(min=8, max=100)
        membreTest.setRole(Role.MEMBRE);               // @NotNull

        Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);

        Assertions.assertTrue(violations.isEmpty());

    }
}

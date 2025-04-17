package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Reservation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de tests unitaires pour l'entité Categorie.
 * Teste les contraintes de validation, les méthodes calculées (@Transient),
 * et la logique equals/hashCode.
 */
public class CategorieTest {

    // Le validateur Jakarta Bean Validation
    private static Validator validator;

    // Initialisation du validateur une seule fois
    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========================================================================
    // Helper Mocks/Stubs (Simulacres pour Event et Reservation)
    // ========================================================================

    // Crée une instance simple d'Event pour les tests.
    private Event createMockEvent(Integer id) {
        Event event = new Event();
        // Supposons qu'Event a un setId et un equals/hashCode basé sur l'ID
        event.setId(id);
        // Pas besoin des autres champs pour tester Categorie
        return event;
    }

    // Crée une instance simple de Reservation pour les tests.
    private Reservation createMockReservation(Integer id) {
        Reservation reservation = new Reservation();
        // Supposons que Reservation a un setId
        reservation.setId(id);
        // Pas besoin des autres champs pour les tests de comptage
        return reservation;
    }

    // ========================================================================
    // Helper Assertion Methods (Copier/Adapter depuis les tests précédents)
    // ========================================================================

    private <T> void assertSingleViolation(Set<ConstraintViolation<T>> violations, String expectedProperty, String expectedMessagePart) {
        assertEquals(1, violations.size(), "Devrait avoir exactement une violation pour '" + expectedProperty + "'");
        ConstraintViolation<T> violation = violations.iterator().next();
        assertEquals(expectedProperty, violation.getPropertyPath().toString(), "Violation sur la mauvaise propriété");
        if (expectedMessagePart != null) {
            assertTrue(violation.getMessage().contains(expectedMessagePart), "Message ne contient pas '" + expectedMessagePart + "': " + violation.getMessage());
        }
    }

    private <T> void assertSingleViolationWithExactMessage(Set<ConstraintViolation<T>> violations, String expectedProperty, String expectedExactMessage) {
        assertEquals(1, violations.size(), "Devrait avoir exactement une violation pour '" + expectedProperty + "'");
        ConstraintViolation<T> violation = violations.iterator().next();
        assertEquals(expectedProperty, violation.getPropertyPath().toString(), "Violation sur la mauvaise propriété");
        assertEquals(expectedExactMessage, violation.getMessage(), "Message exact incorrect");
    }


    // ========================================================================
    // Helper Method pour créer une Categorie valide de base
    // ========================================================================
    private Categorie createValidBaseCategorie() {
        Categorie categorie = new Categorie();
        categorie.setNom("Catégorie Standard");
        categorie.setCapacite(100);
        categorie.setEvent(createMockEvent(1)); // Lie à un événement mock
        // La liste de réservations est initialisée vide par défaut, ce qui est valide
        return categorie;
    }


    // ========================================================================
    // Test Cases (Cas de Test)
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation des Contraintes")
    class ValidationTests {

        @Test
        @DisplayName("Categorie valide ne doit avoir aucune violation")
        void validCategorie_shouldHaveNoViolations() {
            // Arrange
            Categorie categorie = createValidBaseCategorie();
            // Act
            Set<ConstraintViolation<Categorie>> violations = validator.validate(categorie);
            // Assert
            assertTrue(violations.isEmpty(), "Une catégorie valide ne devrait avoir aucune violation");
        }

        // --- Tests pour 'nom' (@NotBlank, @Size(min=2, max=100)) ---
        @Test
        @DisplayName("nom: null doit être invalide")
        void nomIsNull_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setNom(null);
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolationWithExactMessage(v, "nom", "Le nom de la catégorie ne peut pas être vide.");
        }

        @Test
        @DisplayName("nom: vide doit être invalide")
        void nomIsEmpty_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setNom(""); // Viole @NotBlank et @Size(min=2)
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertEquals(2, v.size(), "Nom vide viole @NotBlank et @Size(min=2)");
            Set<String> messages = v.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
            assertTrue(messages.contains("Le nom de la catégorie ne peut pas être vide."));
            assertTrue(messages.contains("Le nom doit contenir entre 2 et 100 caractères."));
        }

        @Test
        @DisplayName("nom: espaces seuls doit être invalide")
        void nomIsWhitespace_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setNom("   ");
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolationWithExactMessage(v, "nom", "Le nom de la catégorie ne peut pas être vide.");
        }

        @Test
        @DisplayName("nom: trop court doit être invalide")
        void nomIsTooShort_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setNom("A");
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolation(v, "nom", "entre 2 et 100");
        }

        @Test
        @DisplayName("nom: trop long doit être invalide")
        void nomIsTooLong_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setNom("A".repeat(101));
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolation(v, "nom", "entre 2 et 100");
        }

        // --- Tests pour 'capacite' (@NotNull, @Min(0)) ---
        @Test
        @DisplayName("capacite: null doit être invalide")
        void capaciteIsNull_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setCapacite(null);
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolationWithExactMessage(v, "capacite", "La capacité est obligatoire.");
        }

        @Test
        @DisplayName("capacite: négative doit être invalide")
        void capaciteIsNegative_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setCapacite(-1);
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolationWithExactMessage(v, "capacite", "La capacité ne peut pas être négative.");
        }

        @Test
        @DisplayName("capacite: zéro doit être valide")
        void capaciteIsZero_shouldBeValid() {
            Categorie c = createValidBaseCategorie();
            c.setCapacite(0);
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertTrue(v.isEmpty(), "Capacité de 0 devrait être valide");
        }

        // --- Test pour 'event' (@NotNull) ---
        @Test
        @DisplayName("event: null doit être invalide")
        void eventIsNull_shouldBeInvalid() {
            Categorie c = createValidBaseCategorie();
            c.setEvent(null);
            Set<ConstraintViolation<Categorie>> v = validator.validate(c);
            assertSingleViolationWithExactMessage(v, "event", "La catégorie doit être liée à un événement.");
        }

    } // Fin @Nested ValidationTests


    @Nested
    @DisplayName("Tests des Méthodes Calculées (@Transient)")
    class CalculatedMethodsTests {

        // --- Tests pour getPlaceReserve() ---
        @Test
        @DisplayName("getPlaceReserve: liste reservations null => 0")
        void getPlaceReserve_whenReservationsIsNull_shouldReturnZero() {
            Categorie categorie = createValidBaseCategorie();
            categorie.setReservations(null); // Force la liste à null
            assertEquals(0, categorie.getPlaceReserve(), "Devrait retourner 0 si la liste est null");
        }

        @Test
        @DisplayName("getPlaceReserve: liste reservations vide => 0")
        void getPlaceReserve_whenReservationsIsEmpty_shouldReturnZero() {
            Categorie categorie = createValidBaseCategorie();
            // La liste est déjà initialisée vide par createValidBaseCategorie
            assertTrue(categorie.getReservations().isEmpty());
            assertEquals(0, categorie.getPlaceReserve(), "Devrait retourner 0 si la liste est vide");
        }

        @Test
        @DisplayName("getPlaceReserve: liste avec N reservations => N")
        void getPlaceReserve_whenReservationsHasItems_shouldReturnCorrectCount() {
            Categorie categorie = createValidBaseCategorie();
            List<Reservation> resas = new ArrayList<>();
            resas.add(createMockReservation(1));
            resas.add(createMockReservation(2));
            resas.add(createMockReservation(3));
            categorie.setReservations(resas); // Définit la liste avec 3 réservations

            assertEquals(3, categorie.getPlaceReserve(), "Devrait retourner le nombre d'éléments dans la liste");
        }

        // --- Tests pour getPlaceDisponible() ---
        @Test
        @DisplayName("getPlaceDisponible: capacite null => 0")
        void getPlaceDisponible_whenCapaciteIsNull_shouldReturnZero() {
            Categorie categorie = createValidBaseCategorie();
            categorie.setCapacite(null);
            assertEquals(0, categorie.getPlaceDisponible(), "Devrait retourner 0 si capacité est null");
        }

        @Test
        @DisplayName("getPlaceDisponible: capacite négative => 0")
        void getPlaceDisponible_whenCapaciteIsNegative_shouldReturnZero() {
            Categorie categorie = createValidBaseCategorie();
            categorie.setCapacite(-10);
            assertEquals(0, categorie.getPlaceDisponible(), "Devrait retourner 0 si capacité est négative");
        }

        @Test
        @DisplayName("getPlaceDisponible: capacite=50, reserve=0 => 50")
        void getPlaceDisponible_whenCapacitePositiveNoReservations_shouldReturnCapacite() {
            Categorie categorie = createValidBaseCategorie();
            categorie.setCapacite(50);
            // Aucune réservation ajoutée
            assertEquals(50, categorie.getPlaceDisponible());
        }

        @Test
        @DisplayName("getPlaceDisponible: capacite=50, reserve=10 => 40")
        void getPlaceDisponible_whenCapacitePositiveSomeReservations_shouldReturnDifference() {
            Categorie categorie = createValidBaseCategorie();
            categorie.setCapacite(50);
            List<Reservation> resas = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                resas.add(createMockReservation(i));
            }
            categorie.setReservations(resas);
            assertEquals(40, categorie.getPlaceDisponible());
        }

        @Test
        @DisplayName("getPlaceDisponible: capacite=50, reserve=50 => 0")
        void getPlaceDisponible_whenFull_shouldReturnZero() {
            Categorie categorie = createValidBaseCategorie();
            categorie.setCapacite(50);
            List<Reservation> resas = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                resas.add(createMockReservation(i));
            }
            categorie.setReservations(resas);
            assertEquals(0, categorie.getPlaceDisponible());
        }

        @Test
        @DisplayName("getPlaceDisponible: capacite=50, reserve=60 => -10 (surbooking)")
        void getPlaceDisponible_whenOverbooked_shouldReturnNegative() {
            // NOTE: Ce test vérifie le comportement actuel du code qui retourne
            // capacite - placeReserve, ce qui peut être négatif.
            Categorie categorie = createValidBaseCategorie();
            categorie.setCapacite(50);
            List<Reservation> resas = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                resas.add(createMockReservation(i));
            }
            categorie.setReservations(resas);
            assertEquals(-10, categorie.getPlaceDisponible(), "Retourne la différence négative en cas de surbooking");
            // Si le comportement souhaité était de retourner 0 au lieu de négatif,
            // le code de getPlaceDisponible devrait être :
            // return Math.max(0, currentCapacite - getPlaceReserve());
            // et ce test devrait vérifier assertEquals(0, ...);
        }
    } // Fin @Nested CalculatedMethodsTests


    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur ID)")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("equals: même ID => true")
        void equals_sameId_shouldBeTrue() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            Categorie c2 = createValidBaseCategorie();
            c2.setId(1);
            c2.setNom("Autre Nom"); // ID identique, autre champ différent
            assertEquals(c1, c2);
            assertTrue(c1.equals(c2) && c2.equals(c1));
        }

        @Test
        @DisplayName("equals: ID différents => false")
        void equals_differentId_shouldBeFalse() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            Categorie c2 = createValidBaseCategorie();
            c2.setId(2);
            assertNotEquals(c1, c2);
        }

        @Test
        @DisplayName("equals: un ID null => false")
        void equals_oneIdNull_shouldBeFalse() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            Categorie c2 = createValidBaseCategorie(); // ID null
            assertNotEquals(c1, c2);
            assertNotEquals(c2, c1);
        }

        @Test
        @DisplayName("equals: deux ID null => false (si objets différents)")
        void equals_bothIdNull_shouldBeFalse() {
            Categorie c1 = createValidBaseCategorie();
            c1.setNom("Nom 1");
            Categorie c2 = createValidBaseCategorie();
            c2.setNom("Nom 2");
            assertNotEquals(c1, c2);
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            assertEquals(c1, c1);
        }

        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            assertNotEquals(null, c1);
            assertFalse(c1.equals(null));
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            Object autre = new Object();
            assertNotEquals(c1, autre);
            assertFalse(c1.equals(autre));
        }

        @Test
        @DisplayName("hashCode: même ID => même hashCode")
        void hashCode_sameId_shouldBeEqual() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            Categorie c2 = createValidBaseCategorie();
            c2.setId(1);
            assertEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("hashCode: ID différents => hashCodes différents (probable)")
        void hashCode_differentId_shouldBeDifferent() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            Categorie c2 = createValidBaseCategorie();
            c2.setId(2);
            assertNotEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("hashCode: consistance")
        void hashCode_consistent() {
            Categorie c1 = createValidBaseCategorie();
            c1.setId(1);
            int initialHashCode = c1.hashCode();
            // Modifie des champs non utilisés dans hashCode
            c1.setNom("Nouveau Nom");
            c1.setCapacite(200);
            assertEquals(initialHashCode, c1.hashCode());
        }
    } // Fin @Nested EqualsHashCodeTests

}

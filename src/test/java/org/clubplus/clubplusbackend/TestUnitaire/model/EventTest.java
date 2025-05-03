package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Classe de tests unitaires pour l'entité Event.
 * Teste les contraintes de validation, les méthodes calculées (@Transient),
 * et la logique equals/hashCode.
 */
public class EventTest {

    // Le validateur Jakarta Bean Validation
    private static Validator validator;

    // Initialisation du validateur une seule fois
    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========================================================================
    // Helper Mocks/Stubs
    // ========================================================================
    private Club createMockClub(Integer id) {
        Club club = new Club();
        club.setId(id); // Supposant equals/hashCode basé sur ID
        return club;
    }

    // Utilisation de Mockito pour créer des mocks de Categorie
    // Cela permet de contrôler la valeur retournée par getCapacite() et getPlaceReserve()
    private Categorie createMockCategorie(Integer id, Integer capacite, int placeReserve) {
        Categorie mockCategorie = Mockito.mock(Categorie.class);
        when(mockCategorie.getId()).thenReturn(id); // Si jamais nécessaire
        when(mockCategorie.getCapacite()).thenReturn(capacite);
        // Simule la méthode calculée de Categorie
        when(mockCategorie.getPlaceReserve()).thenReturn(placeReserve);
        return mockCategorie;
    }

    // ========================================================================
    // Helper Assertion Methods (Copier/Adapter)
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
    // Helper Method pour créer un Event valide de base
    // ========================================================================
    private Event createValidBaseEvent() {
        Event event = new Event();
        event.setNom("Événement Test Valide");
        event.setStart(LocalDateTime.now().plusDays(1)); // Futur
        event.setEnd(LocalDateTime.now().plusDays(1).plusHours(2)); // Futur et après start
        event.setDescription("Description valide de l'événement.");
        event.setLocation("Lieu de test");
        event.setOrganisateur(createMockClub(1)); // Lie à un club mock
        // Les listes categories et notations sont initialisées vides par défaut
        return event;
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation des Contraintes")
    class ValidationTests {

        @Test
        @DisplayName("Event valide ne doit avoir aucune violation")
        void validEvent_shouldHaveNoViolations() {
            Event event = createValidBaseEvent();
            Set<ConstraintViolation<Event>> violations = validator.validate(event);
            assertTrue(violations.isEmpty(), "Un événement valide ne devrait avoir aucune violation");
        }

        // --- Tests pour 'nom' (@NotBlank, @Size(min=3, max=150)) ---
        @Nested
        @DisplayName("Validation 'nom'")
        class NomValidation {
            @Test
            @DisplayName("nom: null")
            void nullNom() {
                Event e = createValidBaseEvent();
                e.setNom(null);
                assertSingleViolationWithExactMessage(validator.validate(e), "nom", "Le nom de l'événement ne peut pas être vide.");
            }

            @Test
            @DisplayName("nom: vide")
            void emptyNom() {
                Event e = createValidBaseEvent();
                e.setNom(""); // Viole @NotBlank et @Size(min=3)
                Set<ConstraintViolation<Event>> v = validator.validate(e);
                assertEquals(2, v.size());
                assertTrue(v.stream().anyMatch(viol -> viol.getMessage().equals("Le nom de l'événement ne peut pas être vide.")));
                assertTrue(v.stream().anyMatch(viol -> viol.getMessage().equals("Le nom doit contenir entre 3 et 150 caractères.")));
            }

            @Test
            @DisplayName("nom: espaces")
            void whitespaceNom() {
                Event e = createValidBaseEvent();
                e.setNom("   ");
                assertSingleViolationWithExactMessage(validator.validate(e), "nom", "Le nom de l'événement ne peut pas être vide.");
            }

            @Test
            @DisplayName("nom: trop court")
            void tooShortNom() {
                Event e = createValidBaseEvent();
                e.setNom("Ab");
                assertSingleViolation(validator.validate(e), "nom", "entre 3 et 150");
            }

            @Test
            @DisplayName("nom: trop long")
            void tooLongNom() {
                Event e = createValidBaseEvent();
                e.setNom("A".repeat(151));
                assertSingleViolation(validator.validate(e), "nom", "entre 3 et 150");
            }
        }

        // --- Tests pour 'start' (@NotNull, @FutureOrPresent) ---
        @Nested
        @DisplayName("Validation 'start'")
        class StartValidation {
            @Test
            @DisplayName("start: null")
            void nullStart() {
                Event e = createValidBaseEvent();
                e.setStart(null);
                assertSingleViolationWithExactMessage(validator.validate(e), "start", "La date et l'heure de début sont obligatoires.");
            }

            @Test
            @DisplayName("start: passé")
            void pastStart() {
                Event e = createValidBaseEvent();
                e.setStart(LocalDateTime.now().minusSeconds(1)); // Strictement passé
                assertSingleViolationWithExactMessage(validator.validate(e), "start", "La date de début doit être dans le présent ou le futur.");
            }

            // Note: tester 'présent' peut être floconneux à cause des millisecondes. On le saute souvent.
            @Test
            @DisplayName("start: futur (valide)")
            void futureStart() {
                Event e = createValidBaseEvent();
                e.setStart(LocalDateTime.now().plusMinutes(1));
                // Supprime la violation sur 'start' si elle existe et vérifie si d'autres restent
                Set<ConstraintViolation<Event>> violations = validator.validate(e);
                violations.removeIf(v -> v.getPropertyPath().toString().equals("start"));
                assertTrue(violations.isEmpty(), "Date de début future devrait être valide");
            }
        }

        // --- Tests pour 'end' (@NotNull, @FutureOrPresent) ---
        // Note: Similaire à 'start'
        @Nested
        @DisplayName("Validation 'end'")
        class EndValidation {
            @Test
            @DisplayName("end: null")
            void nullEnd() {
                Event e = createValidBaseEvent();
                e.setEnd(null);
                assertSingleViolationWithExactMessage(validator.validate(e), "end", "La date et l'heure de fin sont obligatoires.");
            }

            @Test
            @DisplayName("end: passé")
            void pastEnd() {
                Event e = createValidBaseEvent();
                e.setEnd(LocalDateTime.now().minusSeconds(1));
                assertSingleViolationWithExactMessage(validator.validate(e), "end", "La date de fin doit être dans le présent ou le futur.");
            }
        }

        // --- Tests pour 'description' (@NotBlank, @Size(max=2000)) ---
        @Nested
        @DisplayName("Validation 'description'")
        class DescriptionValidation {
            @Test
            @DisplayName("description: null")
            void nullDesc() {
                Event e = createValidBaseEvent();
                e.setDescription(null);
                assertSingleViolationWithExactMessage(validator.validate(e), "description", "La description ne peut pas être vide.");
            }

            @Test
            @DisplayName("description: vide")
            void emptyDesc() {
                Event e = createValidBaseEvent();
                e.setDescription("");
                assertSingleViolationWithExactMessage(validator.validate(e), "description", "La description ne peut pas être vide.");
            }

            @Test
            @DisplayName("description: espaces")
            void whitespaceDesc() {
                Event e = createValidBaseEvent();
                e.setDescription("   ");
                assertSingleViolationWithExactMessage(validator.validate(e), "description", "La description ne peut pas être vide.");
            }

            @Test
            @DisplayName("description: trop long")
            void tooLongDesc() {
                Event e = createValidBaseEvent();
                e.setDescription("A".repeat(2001));
                assertSingleViolation(validator.validate(e), "description", "ne doit pas dépasser 2000");
            }
        }

        // --- Test pour 'location' (@Size(max=255)) ---
        // @NotBlank n'est pas là, donc null et vide sont valides pour cette contrainte.
        @Nested
        @DisplayName("Validation 'location'")
        class LocationValidation {
            @Test
            @DisplayName("location: null (valide)")
            void nullLocation() {
                Event e = createValidBaseEvent();
                e.setLocation(null);
                assertTrue(validator.validate(e).isEmpty(), "Location null devrait être valide");
            }

            @Test
            @DisplayName("location: vide (valide)")
            void emptyLocation() {
                Event e = createValidBaseEvent();
                e.setLocation("");
                assertTrue(validator.validate(e).isEmpty(), "Location vide devrait être valide");
            }

            @Test
            @DisplayName("location: trop long")
            void tooLongLocation() {
                Event e = createValidBaseEvent();
                e.setLocation("A".repeat(256));
                assertSingleViolation(validator.validate(e), "location", "ne doit pas dépasser 255");
            }
        }

        // --- Test pour 'organisateur' (@NotNull) ---
        @Nested
        @DisplayName("Validation 'organisateur'")
        class OrganisateurValidation {
            @Test
            @DisplayName("organisateur: null")
            void nullOrganisateur() {
                Event e = createValidBaseEvent();
                e.setOrganisateur(null);
                assertSingleViolationWithExactMessage(validator.validate(e), "organisateur", "L'événement doit avoir un organisateur.");
            }
        }

    } // Fin @Nested ValidationTests


    @Nested
    @DisplayName("Tests des Méthodes Calculées (@Transient)")
    class CalculatedMethodsTests {

        // --- Tests pour getPlaceTotal() ---
        @Test
        @DisplayName("getPlaceTotal: liste categories null => 0")
        void getPlaceTotal_whenCategoriesIsNull_shouldReturnZero() {
            Event event = createValidBaseEvent();
            event.setCategories(null);
            assertEquals(0, event.getPlaceTotal());
        }

        @Test
        @DisplayName("getPlaceTotal: liste categories vide => 0")
        void getPlaceTotal_whenCategoriesIsEmpty_shouldReturnZero() {
            Event event = createValidBaseEvent();
            // Liste vide par défaut
            assertEquals(0, event.getPlaceTotal());
        }

        @Test
        @DisplayName("getPlaceTotal: somme capacités (avec une null)")
        void getPlaceTotal_shouldSumCapacitiesIgnoringNull() {
            Event event = createValidBaseEvent();
            List<Categorie> categories = Arrays.asList(
                    createMockCategorie(1, 100, 0), // Capacité 100
                    createMockCategorie(2, 50, 0),  // Capacité 50
                    createMockCategorie(3, null, 0) // Capacité null (doit être comptée comme 0)
            );
            event.setCategories(categories);
            assertEquals(150, event.getPlaceTotal(), "Devrait sommer 100 + 50 + 0");
        }

        @Test
        @DisplayName("getPlaceTotal: somme capacités (avec une négative)")
        void getPlaceTotal_shouldSumCapacitiesIncludingNegative() {
            // Le code actuel somme les capacités telles quelles, même négatives.
            Event event = createValidBaseEvent();
            List<Categorie> categories = Arrays.asList(
                    createMockCategorie(1, 100, 0),
                    createMockCategorie(2, -10, 0) // Capacité négative
            );
            event.setCategories(categories);
            assertEquals(90, event.getPlaceTotal(), "Devrait sommer 100 + (-10)");
            // Si une capacité négative n'est pas souhaitée, la validation @Min sur Categorie devrait l'empêcher
        }


        // --- Tests pour getPlaceReserve() ---
        @Test
        @DisplayName("getPlaceReserve: liste categories null => 0")
        void getPlaceReserve_whenCategoriesIsNull_shouldReturnZero() {
            Event event = createValidBaseEvent();
            event.setCategories(null);
            assertEquals(0, event.getPlaceReserve());
        }

        @Test
        @DisplayName("getPlaceReserve: liste categories vide => 0")
        void getPlaceReserve_whenCategoriesIsEmpty_shouldReturnZero() {
            Event event = createValidBaseEvent();
            assertEquals(0, event.getPlaceReserve());
        }

        @Test
        @DisplayName("getPlaceReserve: somme places réservées")
        void getPlaceReserve_shouldSumReservedPlacesFromCategories() {
            Event event = createValidBaseEvent();
            List<Categorie> categories = Arrays.asList(
                    createMockCategorie(1, 100, 20), // 20 réservées
                    createMockCategorie(2, 50, 5),   // 5 réservées
                    createMockCategorie(3, 200, 0)   // 0 réservées
            );
            event.setCategories(categories);
            assertEquals(25, event.getPlaceReserve(), "Devrait sommer 20 + 5 + 0");
        }

        // --- Tests pour getPlaceDisponible() ---
        @Test
        @DisplayName("getPlaceDisponible: cas nominal")
        void getPlaceDisponible_nominalCase() {
            // Utilise les mêmes mocks que les tests précédents pour cohérence
            Event event = createValidBaseEvent();
            List<Categorie> categories = Arrays.asList(
                    createMockCategorie(1, 100, 20), // 80 dispo
                    createMockCategorie(2, 50, 5),   // 45 dispo
                    createMockCategorie(3, 200, 0)   // 200 dispo
                    // Total Capacité = 350
                    // Total Réservé = 25
            );
            event.setCategories(categories);

            // getPlaceTotal() doit retourner 350
            // getPlaceReserve() doit retourner 25
            assertEquals(350, event.getPlaceTotal());
            assertEquals(25, event.getPlaceReserve());

            // Donc, getPlaceDisponible() doit retourner 350 - 25 = 325
            assertEquals(325, event.getPlaceDisponible());
        }

        @Test
        @DisplayName("getPlaceDisponible: cas avec surbooking dans une catégorie")
        void getPlaceDisponible_whenOverbookedInCategory() {
            Event event = createValidBaseEvent();
            List<Categorie> categories = Arrays.asList(
                    createMockCategorie(1, 100, 20),  // Capa 100, Res 20
                    createMockCategorie(2, 50, 60)   // Capa 50, Res 60 (surbookée)
                    // Total Capacité = 150
                    // Total Réservé = 80
            );
            event.setCategories(categories);

            assertEquals(150, event.getPlaceTotal());
            assertEquals(80, event.getPlaceReserve());
            // Place dispo = 150 - 80 = 70 (même si une catégorie est négative individuellement)
            assertEquals(70, event.getPlaceDisponible());
        }

        @Test
        @DisplayName("getPlaceDisponible: cas où le total réservé dépasse le total capacité")
        void getPlaceDisponible_whenTotalOverbooked() {
            Event event = createValidBaseEvent();
            List<Categorie> categories = Arrays.asList(
                    createMockCategorie(1, 100, 110), // Surbooké
                    createMockCategorie(2, 50, 60)   // Surbooké
                    // Total Capacité = 150
                    // Total Réservé = 170
            );
            event.setCategories(categories);

            assertEquals(150, event.getPlaceTotal());
            assertEquals(170, event.getPlaceReserve());
            // Place dispo = 150 - 170 = -20
            assertEquals(-20, event.getPlaceDisponible());
        }

    } // Fin @Nested CalculatedMethodsTests


    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur ID)")
    class EqualsHashCodeTests {
        // Logique identique à ClubTest et CategorieTest
        @Test
        @DisplayName("equals: même ID => true")
        void equals_sameId_shouldBeTrue() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            Event e2 = createValidBaseEvent();
            e2.setId(1);
            e2.setNom("Autre");
            assertEquals(e1, e2);
            assertTrue(e1.equals(e2) && e2.equals(e1));
        }

        @Test
        @DisplayName("equals: ID différents => false")
        void equals_differentId_shouldBeFalse() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            Event e2 = createValidBaseEvent();
            e2.setId(2);
            assertNotEquals(e1, e2);
        }

        @Test
        @DisplayName("equals: un ID null => false")
        void equals_oneIdNull_shouldBeFalse() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            Event e2 = createValidBaseEvent(); // ID null
            assertNotEquals(e1, e2);
            assertNotEquals(e2, e1);
        }

        @Test
        @DisplayName("equals: deux ID null => false")
        void equals_bothIdNull_shouldBeFalse() {
            Event e1 = createValidBaseEvent();
            e1.setNom("N1");
            Event e2 = createValidBaseEvent();
            e2.setNom("N2");
            assertNotEquals(e1, e2);
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            assertEquals(e1, e1);
        }

        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            assertNotEquals(null, e1);
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            Object autre = new Object();
            assertNotEquals(e1, autre);
        }

        @Test
        @DisplayName("hashCode: même ID => même hashCode")
        void hashCode_sameId_shouldBeEqual() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            Event e2 = createValidBaseEvent();
            e2.setId(1);
            assertEquals(e1.hashCode(), e2.hashCode());
        }

        @Test
        @DisplayName("hashCode: ID différents => hashCodes différents (probable)")
        void hashCode_differentId_shouldBeDifferent() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            Event e2 = createValidBaseEvent();
            e2.setId(2);
            assertNotEquals(e1.hashCode(), e2.hashCode());
        }

        @Test
        @DisplayName("hashCode: consistance")
        void hashCode_consistent() {
            Event e1 = createValidBaseEvent();
            e1.setId(1);
            int initialHashCode = e1.hashCode();
            e1.setNom("Nouveau Nom"); // Champ non utilisé dans hashCode
            assertEquals(initialHashCode, e1.hashCode());
        }
    } // Fin @Nested EqualsHashCodeTests
}

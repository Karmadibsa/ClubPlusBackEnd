package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Notation;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de tests unitaires pour l'entité Notation.
 * Teste les contraintes de validation, la méthode de callback @PrePersist,
 * la méthode calculée getNoteMoyenne(), et la logique equals/hashCode.
 * Note: La contrainte @UniqueConstraint(event_id, membre_id) n'est pas testée ici.
 */
public class NotationTest {

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
    private Event createMockEvent(Integer id) {
        Event event = new Event();
        event.setId(id); // Supposant equals/hashCode basé sur ID
        return event;
    }

    private Membre createMockMembre(Integer id) {
        Membre membre = new Membre();
        membre.setId(id); // Supposant equals/hashCode basé sur ID
        return membre;
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
        // Pour @NotNull, on n'a pas de message standard, donc on ne vérifie que la propriété
        // Pour @Min/@Max, on a des messages définis, donc on peut les vérifier.
        if (expectedExactMessage != null) {
            assertEquals(expectedExactMessage, violation.getMessage(), "Message exact incorrect");
        }
    }

    // ========================================================================
    // Helper Method pour créer une Notation valide de base
    // ========================================================================
    private Notation createValidBaseNotation() {
        Notation notation = new Notation();
        notation.setEvent(createMockEvent(1));
        notation.setMembre(createMockMembre(1));
        notation.setAmbiance(4);
        notation.setProprete(5);
        notation.setOrganisation(3);
        notation.setFairPlay(5);
        notation.setNiveauJoueurs(4);
        notation.setDateNotation(LocalDateTime.now().minusHours(1)); // Passé récent
        return notation;
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation des Contraintes")
    class ValidationTests {

        @Test
        @DisplayName("Notation valide ne doit avoir aucune violation")
        void validNotation_shouldHaveNoViolations() {
            Notation notation = createValidBaseNotation();
            Set<ConstraintViolation<Notation>> violations = validator.validate(notation);
            assertTrue(violations.isEmpty(), "Une notation valide ne devrait avoir aucune violation");
        }

        // --- Tests pour les relations @NotNull ---
        @Test
        @DisplayName("event: null")
        void eventIsNull() {
            Notation n = createValidBaseNotation();
            n.setEvent(null);
            assertSingleViolationWithExactMessage(validator.validate(n), "event", "La notation doit être liée à un événement.");
        }

        @Test
        @DisplayName("membre: null")
        void membreIsNull() {
            Notation n = createValidBaseNotation();
            n.setMembre(null);
            assertSingleViolationWithExactMessage(validator.validate(n), "membre", "La notation doit être faite par un membre.");
        }

        // --- Tests pour les notes (@Min(1), @Max(5)) ---
        // On teste une note (ex: ambiance), les autres sont identiques
        @Nested
        @DisplayName("Validation des Notes (exemple: ambiance)")
        class ScoreValidation {
            @Test
            @DisplayName("ambiance: 0 (invalide < min)")
            void scoreIsZero() {
                Notation n = createValidBaseNotation();
                n.setAmbiance(0);
                assertSingleViolationWithExactMessage(validator.validate(n), "ambiance", "La note d'ambiance doit être au minimum 1");
            }

            @Test
            @DisplayName("ambiance: 1 (valide = min)")
            void scoreIsOne() {
                Notation n = createValidBaseNotation();
                n.setAmbiance(1);
                assertTrue(validator.validate(n).isEmpty(), "Note de 1 doit être valide");
            }

            @Test
            @DisplayName("ambiance: 3 (valide)")
            void scoreIsThree() {
                Notation n = createValidBaseNotation();
                n.setAmbiance(3);
                assertTrue(validator.validate(n).isEmpty(), "Note de 3 doit être valide");
            }

            @Test
            @DisplayName("ambiance: 5 (valide = max)")
            void scoreIsFive() {
                Notation n = createValidBaseNotation();
                n.setAmbiance(5);
                assertTrue(validator.validate(n).isEmpty(), "Note de 5 doit être valide");
            }

            @Test
            @DisplayName("ambiance: 6 (invalide > max)")
            void scoreIsSix() {
                Notation n = createValidBaseNotation();
                n.setAmbiance(6);
                assertSingleViolationWithExactMessage(validator.validate(n), "ambiance", "La note d'ambiance doit être au maximum 5");
            }
            // Répéter des tests similaires pour propreté, organisation, fairPlay, niveauJoueurs si nécessaire,
            // mais tester une seule note pour chaque contrainte (@Min, @Max) est souvent suffisant.
        }

        // --- Tests pour 'dateNotation' (@NotNull, @PastOrPresent) ---
        @Nested
        @DisplayName("Validation 'dateNotation'")
        class DateNotationValidation {
            @Test
            @DisplayName("dateNotation: null")
            void nullDate() {
                Notation n = createValidBaseNotation();
                n.setDateNotation(null);
                // On n'a pas défini de message custom pour @NotNull
                assertSingleViolation(validator.validate(n), "dateNotation", null);
            }

            @Test
            @DisplayName("dateNotation: futur")
            void futureDate() {
                Notation n = createValidBaseNotation();
                n.setDateNotation(LocalDateTime.now().plusDays(1)); // Date future
                Set<ConstraintViolation<Notation>> violations = validator.validate(n);

                // Accepte le message par défaut (version française)
                assertSingleViolationWithExactMessage(violations, "dateNotation", "doit être une date dans le passé ou le présent");
            }


            @Test
            @DisplayName("dateNotation: aujourd'hui (valide)")
            void todayDate() {
                Notation n = createValidBaseNotation();
                n.setDateNotation(LocalDateTime.now());
                assertTrue(validator.validate(n).isEmpty(), "Date de notation aujourd'hui doit être valide");
            }
        }

    } // Fin @Nested ValidationTests

    @Nested
    @DisplayName("Tests du Callback @PrePersist (via effet)")
    class CallbackTests {

        @Test
        @DisplayName("onPrePersist: dateNotation est settée si null initialement")
        void onPrePersist_setsDateNotation_ifNull() {
            // Arrange: Crée une notation sans setter la date explicitement
            Notation notation = new Notation();
            notation.setEvent(createMockEvent(1));
            notation.setMembre(createMockMembre(1));
            notation.setAmbiance(3); // Mettre les autres notes aussi
            notation.setProprete(3);
            notation.setOrganisation(3);
            notation.setFairPlay(3);
            notation.setNiveauJoueurs(3);

            // Simule l'état juste avant la persistance où dateNotation est null
            assertNull(notation.getDateNotation());

            // Act: Appelle la méthode du callback directement
            // Note: La méthode est protected, cet appel fonctionne si le test est dans le même package
            // ou si la visibilité est ajustée (package-private ou public pour le test).
            // Si ce n'est pas possible, on teste juste l'état après création via constructeur/setter
            // en supposant que le callback fonctionne.
            // Ici, on suppose qu'on peut l'appeler :
            LocalDateTime beforeCallback = LocalDateTime.now();
            notation.onPrePersist(); // Appel direct de la logique
            LocalDateTime afterCallback = LocalDateTime.now();

            // Assert
            assertNotNull(notation.getDateNotation(), "dateNotation devrait être settée par onPrePersist");
            assertTrue(!notation.getDateNotation().isBefore(beforeCallback) && !notation.getDateNotation().isAfter(afterCallback),
                    "La date settée devrait être proche du moment de l'appel");
        }

        @Test
        @DisplayName("onPrePersist: dateNotation n'est pas écrasée si déjà settée")
        void onPrePersist_doesNotOverwrite_ifDateNotNull() {
            // Arrange: Crée une notation en settant la date explicitement
            Notation notation = new Notation();
            notation.setEvent(createMockEvent(1));
            notation.setMembre(createMockMembre(1));
            // ... set scores ...
            notation.setAmbiance(3);
            notation.setProprete(3);
            notation.setOrganisation(3);
            notation.setFairPlay(3);
            notation.setNiveauJoueurs(3);

            LocalDateTime specificDate = LocalDateTime.now().minusDays(1);
            notation.setDateNotation(specificDate); // Date spécifique

            // Act
            notation.onPrePersist();

            // Assert
            assertEquals(specificDate, notation.getDateNotation(), "dateNotation ne devrait pas avoir été modifiée");
        }
    } // Fin @Nested CallbackTests


    @Nested
    @DisplayName("Tests de la Méthode Calculée getNoteMoyenne()")
    class CalculatedMethodTests {

        @Test
        @DisplayName("getNoteMoyenne: calcul nominal")
        void getNoteMoyenne_nominalCalculation() {
            Notation notation = createValidBaseNotation(); // 4, 5, 3, 5, 4
            // Somme = 4 + 5 + 3 + 5 + 4 = 21
            // Moyenne = 21 / 5.0 = 4.2
            assertEquals(4.2, notation.getNoteMoyenne(), 0.001, "Calcul de moyenne incorrect"); // Utilise delta pour comparaison double
        }

        @Test
        @DisplayName("getNoteMoyenne: toutes notes à 1")
        void getNoteMoyenne_allOnes() {
            Notation notation = createValidBaseNotation();
            notation.setAmbiance(1);
            notation.setProprete(1);
            notation.setOrganisation(1);
            notation.setFairPlay(1);
            notation.setNiveauJoueurs(1);
            // Somme = 5, Moyenne = 5 / 5.0 = 1.0
            assertEquals(1.0, notation.getNoteMoyenne(), 0.001);
        }

        @Test
        @DisplayName("getNoteMoyenne: toutes notes à 5")
        void getNoteMoyenne_allFives() {
            Notation notation = createValidBaseNotation();
            notation.setAmbiance(5);
            notation.setProprete(5);
            notation.setOrganisation(5);
            notation.setFairPlay(5);
            notation.setNiveauJoueurs(5);
            // Somme = 25, Moyenne = 25 / 5.0 = 5.0
            assertEquals(5.0, notation.getNoteMoyenne(), 0.001);
        }

        @Test
        @DisplayName("getNoteMoyenne: notes mixtes")
        void getNoteMoyenne_mixedNotes() {
            Notation notation = createValidBaseNotation();
            notation.setAmbiance(1);
            notation.setProprete(2);
            notation.setOrganisation(3);
            notation.setFairPlay(4);
            notation.setNiveauJoueurs(5);
            // Somme = 1+2+3+4+5 = 15
            // Moyenne = 15 / 5.0 = 3.0
            assertEquals(3.0, notation.getNoteMoyenne(), 0.001);
        }
    } // Fin @Nested CalculatedMethodTests

    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur event/membre)")
    class EqualsHashCodeTests {

        private Event event1, event2; // Différent ID
        private Membre membre1, membre2; // Différent ID

        @BeforeEach
        void setUpEntities() {
            event1 = createMockEvent(10);
            event2 = createMockEvent(20);
            membre1 = createMockMembre(100);
            membre2 = createMockMembre(200);
        }

        // Helper pour créer une Notation avec variations
        private Notation createNotation(Event e, Membre m, Integer id, int score, LocalDateTime date) {
            Notation n = new Notation();
            n.setEvent(e);
            n.setMembre(m);
            n.setId(id); // ID de la notation (ignoré par equals/hashCode)
            n.setAmbiance(score); // Score (ignoré)
            n.setProprete(score);
            n.setOrganisation(score);
            n.setFairPlay(score);
            n.setNiveauJoueurs(score);
            n.setDateNotation(date); // Date (ignorée)
            return n;
        }

        @Test
        @DisplayName("equals: même event, même membre => true")
        void equals_sameEventSameMembre_shouldBeTrue() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event1, membre1, 2, 5, LocalDateTime.now().minusDays(1)); // Champs non-clés différents
            assertEquals(n1, n2);
            assertTrue(n1.equals(n2) && n2.equals(n1)); // Symétrie
        }

        @Test
        @DisplayName("equals: event différent => false")
        void equals_differentEvent_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event2, membre1, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2);
        }

        @Test
        @DisplayName("equals: membre différent => false")
        void equals_differentMembre_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event1, membre2, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2);
        }

        @Test
        @DisplayName("equals: les deux différents => false")
        void equals_bothDifferent_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event2, membre2, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2);
        }

        @Test
        @DisplayName("equals: un event null => false")
        void equals_oneEventNull_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(null, membre1, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2);
            assertNotEquals(n2, n1);
        }

        @Test
        @DisplayName("equals: un membre null => false")
        void equals_oneMembreNull_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event1, null, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2);
            assertNotEquals(n2, n1);
        }

        // Test basé sur les objets Event/Membre, donc l'ID null est géré par leurs propres equals.
        // Si Event/Membre.equals retournent false si un ID est null, alors ce test est valide.
        @Test
        @DisplayName("equals: un event ID null => false")
        void equals_oneEventIdNull_shouldBeFalse() {
            Event eventSansId = new Event(); // ID null
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(eventSansId, membre1, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2); // Car event1 != eventSansId
            assertNotEquals(n2, n1);
        }

        @Test
        @DisplayName("equals: un membre ID null => false")
        void equals_oneMembreIdNull_shouldBeFalse() {
            Membre membreSansId = new Membre(); // ID null
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event1, membreSansId, 2, 3, LocalDateTime.now());
            assertNotEquals(n1, n2); // Car membre1 != membreSansId
            assertNotEquals(n2, n1);
        }

        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            assertFalse(n1.equals(null));
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Object autre = new Object();
            assertFalse(n1.equals(autre));
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            assertEquals(n1, n1);
        }

        // --- HashCode Tests ---
        @Test
        @DisplayName("hashCode: même event/membre => même hashCode")
        void hashCode_sameEventMembre_shouldBeEqual() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event1, membre1, 2, 5, LocalDateTime.now().minusDays(1));
            assertEquals(n1.hashCode(), n2.hashCode());
        }

        @Test
        @DisplayName("hashCode: event différent => hashCode différent (probable)")
        void hashCode_differentEvent_shouldBeDifferent() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event2, membre1, 2, 3, LocalDateTime.now());
            assertNotEquals(n1.hashCode(), n2.hashCode());
        }

        @Test
        @DisplayName("hashCode: membre différent => hashCode différent (probable)")
        void hashCode_differentMembre_shouldBeDifferent() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            Notation n2 = createNotation(event1, membre2, 2, 3, LocalDateTime.now());
            assertNotEquals(n1.hashCode(), n2.hashCode());
        }

        @Test
        @DisplayName("hashCode: consistance")
        void hashCode_consistent() {
            Notation n1 = createNotation(event1, membre1, 1, 3, LocalDateTime.now());
            int initialHashCode = n1.hashCode();
            // Modifie champs non utilisés dans hashCode
            n1.setId(99);
            n1.setAmbiance(1);
            n1.setDateNotation(LocalDateTime.now().plusYears(1));
            assertEquals(initialHashCode, n1.hashCode());
        }

        @Test
        @DisplayName("hashCode: gestion des nulls")
        void hashCode_handlesNulls() {
            // Teste que le hashCode ne plante pas si event/membre sont null
            Notation n_null_event = createNotation(null, membre1, 1, 3, LocalDateTime.now());
            Notation n_null_membre = createNotation(event1, null, 1, 3, LocalDateTime.now());
            Notation n_null_both = createNotation(null, null, 1, 3, LocalDateTime.now());

            assertDoesNotThrow(() -> n_null_event.hashCode());
            assertDoesNotThrow(() -> n_null_membre.hashCode());
            assertDoesNotThrow(() -> n_null_both.hashCode());

            // Si Event/Membre peuvent avoir des ID nulls, leur hashCode doit aussi gérer cela
            Event eventSansId = new Event();
            Membre membreSansId = new Membre();
            Notation n_null_eventId = createNotation(eventSansId, membre1, 1, 3, LocalDateTime.now());
            Notation n_null_membreId = createNotation(event1, membreSansId, 1, 3, LocalDateTime.now());
            Notation n_null_bothIds = createNotation(eventSansId, membreSansId, 1, 3, LocalDateTime.now());

            assertDoesNotThrow(() -> n_null_eventId.hashCode());
            assertDoesNotThrow(() -> n_null_membreId.hashCode());
            assertDoesNotThrow(() -> n_null_bothIds.hashCode());
        }

    } // Fin @Nested EqualsHashCodeTests
}

package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Statut;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de tests unitaires pour l'entité DemandeAmi.
 * Teste les contraintes de validation, le constructeur, les valeurs par défaut,
 * et la logique equals/hashCode spécifique (basée sur envoyeur/recepteur).
 * Note: La contrainte @UniqueConstraint(envoyeur_id, recepteur_id) est une contrainte BDD
 * et n'est pas testée ici.
 */
public class DemandeAmiTest {

    // Le validateur Jakarta Bean Validation
    private static Validator validator;

    // Initialisation du validateur une seule fois
    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========================================================================
    // Helper Mocks/Stubs (Simulacres pour Membre)
    // ========================================================================
    private Membre createMockMembre(Integer id) {
        Membre membre = new Membre();
        membre.setId(id); // Supposant Membre.equals/hashCode basé sur ID
        return membre;
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
        // Note: @NotNull n'a pas de message standardisé, donc on ne l'utilise pas ici pour @NotNull
        assertEquals(1, violations.size(), "Devrait avoir exactement une violation pour '" + expectedProperty + "'");
        ConstraintViolation<T> violation = violations.iterator().next();
        assertEquals(expectedProperty, violation.getPropertyPath().toString(), "Violation sur la mauvaise propriété");
        // On pourrait vérifier le message s'il était défini dans l'annotation @NotNull(message="...")
        // assertEquals(expectedExactMessage, violation.getMessage(), "Message exact incorrect");
    }

    // ========================================================================
    // Test Cases (Cas de Test)
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation des Contraintes (@NotNull)")
    class ValidationTests {

        private Membre validEnvoyeur;
        private Membre validRecepteur;

        @BeforeEach
        void setUpValidEntities() {
            validEnvoyeur = createMockMembre(1);
            validRecepteur = createMockMembre(2);
        }

        @Test
        @DisplayName("DemandeAmi valide (via constructeur) ne doit avoir aucune violation")
        void validDemande_viaConstructor_shouldHaveNoViolations() {
            // Arrange: Le constructeur initialise envoyeur/recepteur, les autres ont des défauts valides (@NotNull)
            DemandeAmi demande = new DemandeAmi(validEnvoyeur, validRecepteur);
            // Act
            Set<ConstraintViolation<DemandeAmi>> violations = validator.validate(demande);
            // Assert
            assertTrue(violations.isEmpty(), "Demande valide via constructeur OK");
        }

        @Test
        @DisplayName("DemandeAmi valide (via setters) ne doit avoir aucune violation")
        void validDemande_viaSetters_shouldHaveNoViolations() {
            // Arrange: Utilise le constructeur par défaut et les setters
            DemandeAmi demande = new DemandeAmi();
            demande.setEnvoyeur(validEnvoyeur);
            demande.setRecepteur(validRecepteur);
            demande.setStatut(Statut.ACCEPTE); // Ou laisse le défaut ATTENTE
            demande.setDateDemande(LocalDateTime.now().minusHours(1)); // Ou laisse le défaut now()
            // Act
            Set<ConstraintViolation<DemandeAmi>> violations = validator.validate(demande);
            // Assert
            assertTrue(violations.isEmpty(), "Demande valide via setters OK");
        }

        @Test
        @DisplayName("envoyeur: null doit être invalide")
        void envoyeurIsNull_shouldBeInvalid() {
            // Arrange
            DemandeAmi demande = new DemandeAmi();
            // demande.setEnvoyeur(null);
            demande.setRecepteur(validRecepteur);
            demande.setStatut(Statut.ATTENTE);
            demande.setDateDemande(LocalDateTime.now());
            // Act
            Set<ConstraintViolation<DemandeAmi>> violations = validator.validate(demande);
            // Assert
            assertSingleViolation(violations, "envoyeur", null); // @NotNull
        }

        @Test
        @DisplayName("recepteur: null doit être invalide")
        void recepteurIsNull_shouldBeInvalid() {
            // Arrange
            DemandeAmi demande = new DemandeAmi();
            demande.setEnvoyeur(validEnvoyeur);
            // demande.setRecepteur(null);
            demande.setStatut(Statut.ATTENTE);
            demande.setDateDemande(LocalDateTime.now());
            // Act
            Set<ConstraintViolation<DemandeAmi>> violations = validator.validate(demande);
            // Assert
            assertSingleViolation(violations, "recepteur", null); // @NotNull
        }

        @Test
        @DisplayName("statut: null doit être invalide")
        void statutIsNull_shouldBeInvalid() {
            // Arrange
            DemandeAmi demande = new DemandeAmi();
            demande.setEnvoyeur(validEnvoyeur);
            demande.setRecepteur(validRecepteur);
            demande.setStatut(null); // Force à null (ignore le défaut)
            demande.setDateDemande(LocalDateTime.now());
            // Act
            Set<ConstraintViolation<DemandeAmi>> violations = validator.validate(demande);
            // Assert
            assertSingleViolation(violations, "statut", null); // @NotNull
        }

        @Test
        @DisplayName("dateDemande: null doit être invalide")
        void dateDemandeIsNull_shouldBeInvalid() {
            // Arrange
            DemandeAmi demande = new DemandeAmi();
            demande.setEnvoyeur(validEnvoyeur);
            demande.setRecepteur(validRecepteur);
            demande.setStatut(Statut.ATTENTE);
            demande.setDateDemande(null); // Force à null (ignore le défaut)
            // Act
            Set<ConstraintViolation<DemandeAmi>> violations = validator.validate(demande);
            // Assert
            assertSingleViolation(violations, "dateDemande", null); // @NotNull
        }

    } // Fin @Nested ValidationTests

    @Nested
    @DisplayName("Tests du Constructeur et des Valeurs par Défaut")
    class ConstructorAndDefaultsTests {

        private Membre envoyeur;
        private Membre recepteur;
        private LocalDateTime beforeConstruction;

        @BeforeEach
        void setUpMembers() {
            envoyeur = createMockMembre(5);
            recepteur = createMockMembre(10);
            beforeConstruction = LocalDateTime.now();
        }

        @Test
        @DisplayName("Constructeur(envoyeur, recepteur) initialise correctement")
        void constructorWithArgs_shouldInitializeFields() {
            // Act
            DemandeAmi demande = new DemandeAmi(envoyeur, recepteur);
            LocalDateTime afterConstruction = LocalDateTime.now();

            // Assert
            assertSame(envoyeur, demande.getEnvoyeur(), "Envoyeur mal initialisé");
            assertSame(recepteur, demande.getRecepteur(), "Recepteur mal initialisé");
            // Vérifie les valeurs par défaut pour les autres champs @NotNull
            assertEquals(Statut.ATTENTE, demande.getStatut(), "Statut devrait être ATTENTE par défaut");
            assertNotNull(demande.getDateDemande(), "DateDemande ne devrait pas être nulle");
            assertTrue(!demande.getDateDemande().isBefore(beforeConstruction) && !demande.getDateDemande().isAfter(afterConstruction),
                    "DateDemande devrait être proche du moment de la construction");
        }

        @Test
        @DisplayName("Constructeur sans argument initialise les valeurs par défaut")
        void noArgsConstructor_shouldInitializeDefaults() {
            // Act
            DemandeAmi demande = new DemandeAmi();
            LocalDateTime afterConstruction = LocalDateTime.now();

            // Assert
            // Les membres seront null (ce qui est invalide, mais on teste juste les défauts ici)
            assertNull(demande.getEnvoyeur());
            assertNull(demande.getRecepteur());
            // Vérifie les valeurs par défaut
            assertEquals(Statut.ATTENTE, demande.getStatut(), "Statut devrait être ATTENTE par défaut");
            assertNotNull(demande.getDateDemande(), "DateDemande ne devrait pas être nulle par défaut");
            assertTrue(!demande.getDateDemande().isBefore(beforeConstruction) && !demande.getDateDemande().isAfter(afterConstruction),
                    "DateDemande par défaut devrait être proche du moment de la construction");
        }
    } // Fin @Nested ConstructorAndDefaultsTests

    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur envoyeurId/recepteurId)")
    class EqualsHashCodeTests {

        private Membre envoyeur1;
        private Membre envoyeur2; // Différent ID
        private Membre recepteur1;
        private Membre recepteur2; // Différent ID

        @BeforeEach
        void setUpMembers() {
            envoyeur1 = createMockMembre(1);
            envoyeur2 = createMockMembre(2);
            recepteur1 = createMockMembre(10);
            recepteur2 = createMockMembre(20);
        }

        // Méthode helper pour créer une demande avec possibilité de nulls
        private DemandeAmi createDemande(Membre e, Membre r, Integer id, Statut statut) {
            DemandeAmi d = new DemandeAmi();
            d.setEnvoyeur(e);
            d.setRecepteur(r);
            d.setId(id); // ID de la demande elle-même (ignoré par equals/hashCode)
            d.setStatut(statut); // Statut (ignoré par equals/hashCode)
            d.setDateDemande(LocalDateTime.now()); // Date (ignorée par equals/hashCode)
            return d;
        }

        @Test
        @DisplayName("equals: même envoyeurId, même recepteurId => true")
        void equals_sameEnvoyeurIdSameRecepteurId_shouldBeTrue() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur1, recepteur1, 200, Statut.ACCEPTE); // Champs non-clés différents
            assertEquals(d1, d2);
            assertTrue(d1.equals(d2) && d2.equals(d1)); // Symétrie
        }

        @Test
        @DisplayName("equals: envoyeurId différent => false")
        void equals_differentEnvoyeurId_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur2, recepteur1, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
        }

        @Test
        @DisplayName("equals: recepteurId différent => false")
        void equals_differentRecepteurId_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur1, recepteur2, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
        }

        @Test
        @DisplayName("equals: les deux IDs différents => false")
        void equals_bothIdsDifferent_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur2, recepteur2, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
        }

        @Test
        @DisplayName("equals: un envoyeur est null => false")
        void equals_oneEnvoyeurIsNull_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(null, recepteur1, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
            assertNotEquals(d2, d1); // Symétrie
        }

        @Test
        @DisplayName("equals: un recepteur est null => false")
        void equals_oneRecepteurIsNull_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur1, null, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
            assertNotEquals(d2, d1);
        }

        @Test
        @DisplayName("equals: un envoyeur ID est null => false")
        void equals_oneEnvoyeurIdIsNull_shouldBeFalse() {
            Membre envoyeurSansId = new Membre(); // ID est null
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeurSansId, recepteur1, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
            assertNotEquals(d2, d1);
        }

        @Test
        @DisplayName("equals: un recepteur ID est null => false")
        void equals_oneRecepteurIdIsNull_shouldBeFalse() {
            Membre recepteurSansId = new Membre(); // ID est null
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur1, recepteurSansId, 200, Statut.ATTENTE);
            assertNotEquals(d1, d2);
            assertNotEquals(d2, d1);
        }

        @Test
        @DisplayName("equals: les deux envoyeurs null, recepteurs identiques => true")
        void equals_bothEnvoyeursNullSameRecepteurs_shouldBeTrue() {
            // Ce cas est un peu étrange fonctionnellement mais teste la logique equals/hashCode
            DemandeAmi d1 = createDemande(null, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(null, recepteur1, 200, Statut.ACCEPTE);
            assertEquals(d1, d2);
        }

        @Test
        @DisplayName("equals: les deux envoyeurs sans ID, recepteurs identiques => true")
        void equals_bothEnvoyeursIdNullSameRecepteurs_shouldBeTrue() {
            Membre envoyeurSansId1 = new Membre();
            Membre envoyeurSansId2 = new Membre(); // Objets différents mais ID null
            DemandeAmi d1 = createDemande(envoyeurSansId1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeurSansId2, recepteur1, 200, Statut.ACCEPTE);
            assertEquals(d1, d2);
        }


        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            assertFalse(d1.equals(null));
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            Object autre = new Object();
            assertFalse(d1.equals(autre));
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            assertEquals(d1, d1);
        }

        // --- HashCode Tests ---
        @Test
        @DisplayName("hashCode: même envoyeurId/recepteurId => même hashCode")
        void hashCode_sameIds_shouldBeEqual() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur1, recepteur1, 200, Statut.ACCEPTE);
            assertEquals(d1.hashCode(), d2.hashCode());
        }

        @Test
        @DisplayName("hashCode: envoyeurId différent => hashCode différent (probable)")
        void hashCode_differentEnvoyeurId_shouldBeDifferent() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur2, recepteur1, 200, Statut.ATTENTE);
            assertNotEquals(d1.hashCode(), d2.hashCode());
        }

        @Test
        @DisplayName("hashCode: recepteurId différent => hashCode différent (probable)")
        void hashCode_differentRecepteurId_shouldBeDifferent() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d2 = createDemande(envoyeur1, recepteur2, 200, Statut.ATTENTE);
            assertNotEquals(d1.hashCode(), d2.hashCode());
        }

        @Test
        @DisplayName("hashCode: consistance (champs non-clés modifiés)")
        void hashCode_consistent() {
            DemandeAmi d1 = createDemande(envoyeur1, recepteur1, 100, Statut.ATTENTE);
            int initialHashCode = d1.hashCode();
            // Modifie des champs non utilisés dans hashCode
            d1.setId(999);
            d1.setStatut(Statut.REFUSE);
            d1.setDateDemande(LocalDateTime.now().minusDays(5));
            assertEquals(initialHashCode, d1.hashCode());
        }

        @Test
        @DisplayName("hashCode: gestion des nulls")
        void hashCode_handlesNulls() {
            // Teste que le hashCode ne plante pas si envoyeur/recepteur ou leurs IDs sont null
            DemandeAmi d_null_envoyeur = createDemande(null, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d_null_recepteur = createDemande(envoyeur1, null, 100, Statut.ATTENTE);
            DemandeAmi d_null_both = createDemande(null, null, 100, Statut.ATTENTE);

            Membre envoyeurSansId = new Membre();
            Membre recepteurSansId = new Membre();
            DemandeAmi d_null_envoyeurId = createDemande(envoyeurSansId, recepteur1, 100, Statut.ATTENTE);
            DemandeAmi d_null_recepteurId = createDemande(envoyeur1, recepteurSansId, 100, Statut.ATTENTE);
            DemandeAmi d_null_bothIds = createDemande(envoyeurSansId, recepteurSansId, 100, Statut.ATTENTE);

            // L'important ici est juste que ça ne lève pas d'exception
            assertDoesNotThrow(() -> d_null_envoyeur.hashCode());
            assertDoesNotThrow(() -> d_null_recepteur.hashCode());
            assertDoesNotThrow(() -> d_null_both.hashCode());
            assertDoesNotThrow(() -> d_null_envoyeurId.hashCode());
            assertDoesNotThrow(() -> d_null_recepteurId.hashCode());
            assertDoesNotThrow(() -> d_null_bothIds.hashCode());
        }

    } // Fin @Nested EqualsHashCodeTests
}

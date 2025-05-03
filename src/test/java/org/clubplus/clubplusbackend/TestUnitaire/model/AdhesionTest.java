package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de tests unitaires pour l'entité Adhesion.
 * Teste les contraintes de validation (@NotNull), le constructeur,
 * et la logique equals/hashCode.
 * Note: La contrainte @UniqueConstraint(membre_id, club_id) est une contrainte de base de données
 * et NE PEUT PAS être testée unitairement avec le Validator. Elle sera vérifiée
 * lors des tests d'intégration (persistance).
 */
public class AdhesionTest {

    // Le validateur Jakarta Bean Validation
    private static Validator validator;

    // Initialisation du validateur une seule fois
    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========================================================================
    // Helper Mocks/Stubs (Simulacres pour Membre et Club)
    // ========================================================================

    // Crée des instances simples de Membre et Club pour les tests.
    // Juste besoin qu'ils soient non-null et aient éventuellement un ID pour equals/hashCode.
    private Membre createMockMembre(Integer id) {
        Membre membre = new Membre();
        membre.setId(id); // Supposant que Membre a une méthode setId et equals/hashCode basé sur l'ID
        // Pas besoin de remplir les autres champs pour ces tests unitaires d'Adhesion
        return membre;
    }

    private Club createMockClub(Integer id) {
        Club club = new Club();
        club.setId(id); // Supposant que Club a une méthode setId et equals/hashCode basé sur l'ID
        // Pas besoin de remplir les autres champs
        return club;
    }

    // ========================================================================
    // Helper Assertion Methods (Déjà définis dans MembreTest, supposés disponibles ou copiés ici)
    // ========================================================================

    /**
     * Méthode d'assertion pour vérifier exactement UNE violation pour une propriété.
     * (Copie/adapte depuis MembreTest si nécessaire)
     */
    private <T> void assertSingleViolation(Set<ConstraintViolation<T>> violations, String expectedProperty, String expectedMessagePart) {
        assertEquals(1, violations.size(), "Devrait avoir exactement une violation pour '" + expectedProperty + "'");
        ConstraintViolation<T> violation = violations.iterator().next();
        assertEquals(expectedProperty, violation.getPropertyPath().toString(), "Violation sur la mauvaise propriété");
        if (expectedMessagePart != null) {
            assertTrue(violation.getMessage().contains(expectedMessagePart), "Message ne contient pas '" + expectedMessagePart + "': " + violation.getMessage());
        }
    }

    /**
     * Méthode d'assertion pour vérifier exactement UNE violation avec message exact.
     * (Copie/adapte depuis MembreTest si nécessaire)
     */
    private <T> void assertSingleViolationWithExactMessage(Set<ConstraintViolation<T>> violations, String expectedProperty, String expectedExactMessage) {
        assertEquals(1, violations.size(), "Devrait avoir exactement une violation pour '" + expectedProperty + "'");
        ConstraintViolation<T> violation = violations.iterator().next();
        assertEquals(expectedProperty, violation.getPropertyPath().toString(), "Violation sur la mauvaise propriété");
        assertEquals(expectedExactMessage, violation.getMessage(), "Message exact incorrect");
    }


    // ========================================================================
    // Test Cases (Cas de Test)
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation (@NotNull)")
    class ValidationTests {

        private Membre validMembre;
        private Club validClub;

        @BeforeEach
        void setUpValidEntities() {
            validMembre = createMockMembre(1);
            validClub = createMockClub(10);
        }

        @Test
        @DisplayName("Adhesion valide (via constructeur) ne doit avoir aucune violation")
        void validAdhesion_shouldHaveNoViolations_whenUsingConstructor() {
            // Arrange: Utilise le constructeur qui initialise tout correctement
            Adhesion adhesion = new Adhesion(validMembre, validClub);
            // Act
            Set<ConstraintViolation<Adhesion>> violations = validator.validate(adhesion);
            // Assert
            assertTrue(violations.isEmpty(), "Une adhésion valide via constructeur ne devrait pas avoir de violations");
        }

        @Test
        @DisplayName("Adhesion valide (via setters) ne doit avoir aucune violation")
        void validAdhesion_shouldHaveNoViolations_whenUsingSetters() {
            // Arrange: Utilise le constructeur par défaut et les setters
            Adhesion adhesion = new Adhesion();
            adhesion.setMembre(validMembre);
            adhesion.setClub(validClub);
            adhesion.setDateAdhesion(LocalDateTime.now()); // Important de le setter aussi !
            // Act
            Set<ConstraintViolation<Adhesion>> violations = validator.validate(adhesion);
            // Assert
            assertTrue(violations.isEmpty(), "Une adhésion valide via setters ne devrait pas avoir de violations");
        }


        @Test
        @DisplayName("membre: null doit être invalide (@NotNull)")
        void membreIsNull_shouldBeInvalid() {
            // Arrange: Utilise le constructeur par défaut, ne set pas le membre
            Adhesion adhesion = new Adhesion();
            adhesion.setClub(validClub);
            adhesion.setDateAdhesion(LocalDateTime.now());
            // adhesion.setMembre(null); // Ou juste ne pas appeler le setter

            // Act
            Set<ConstraintViolation<Adhesion>> violations = validator.validate(adhesion);

            // Assert: Vérifie la violation @NotNull sur 'membre'
            // Le message par défaut de @NotNull n'est pas standardisé, on vérifie juste la propriété
            assertSingleViolation(violations, "membre", null); // On ne vérifie pas le message exact
            // Si tu avais @NotNull(message="..."), tu utiliserais assertSingleViolationWithExactMessage
        }

        @Test
        @DisplayName("club: null doit être invalide (@NotNull)")
        void clubIsNull_shouldBeInvalid() {
            // Arrange: Utilise le constructeur par défaut, ne set pas le club
            Adhesion adhesion = new Adhesion();
            adhesion.setMembre(validMembre);
            adhesion.setDateAdhesion(LocalDateTime.now());
            // adhesion.setClub(null);

            // Act
            Set<ConstraintViolation<Adhesion>> violations = validator.validate(adhesion);

            // Assert: Vérifie la violation @NotNull sur 'club'
            assertSingleViolation(violations, "club", null);
        }

        @Test
        @DisplayName("dateAdhesion: null doit être invalide (@NotNull)")
        void dateAdhesionIsNull_shouldBeInvalid() {
            // Arrange: Utilise le constructeur par défaut, ne set pas la date
            Adhesion adhesion = new Adhesion();
            adhesion.setMembre(validMembre);
            adhesion.setClub(validClub);
            // adhesion.setDateAdhesion(null);

            // Act
            Set<ConstraintViolation<Adhesion>> violations = validator.validate(adhesion);

            // Assert: Vérifie la violation @NotNull sur 'dateAdhesion'
            assertSingleViolation(violations, "dateAdhesion", null);
        }
    } // Fin @Nested ValidationTests


    @Nested
    @DisplayName("Tests du Constructeur Adhesion(Membre, Club)")
    class ConstructorTests {

        private Membre validMembre;
        private Club validClub;
        private LocalDateTime beforeConstruction;

        @BeforeEach
        void setUpValidEntities() {
            validMembre = createMockMembre(1);
            validClub = createMockClub(10);
            beforeConstruction = LocalDateTime.now(); // Capture le temps avant l'appel au constructeur
        }

        @Test
        @DisplayName("Constructeur valide doit initialiser membre, club et dateAdhesion")
        void validConstructor_shouldInitializeFields() {
            // Act
            Adhesion adhesion = new Adhesion(validMembre, validClub);
            LocalDateTime afterConstruction = LocalDateTime.now(); // Capture le temps après

            // Assert
            // 1. Vérifie que les objets membre et club sont correctement assignés
            assertSame(validMembre, adhesion.getMembre(), "Le membre assigné doit être le même objet");
            assertSame(validClub, adhesion.getClub(), "Le club assigné doit être le même objet");

            // 2. Vérifie que la date d'adhésion a été initialisée
            assertNotNull(adhesion.getDateAdhesion(), "La date d'adhésion ne doit pas être nulle");

            // 3. Vérifie que la date est raisonnable (entre juste avant et juste après la création)
            assertTrue(!adhesion.getDateAdhesion().isBefore(beforeConstruction) && !adhesion.getDateAdhesion().isAfter(afterConstruction),
                    "La date d'adhésion devrait être proche du moment de la construction");
            // Alternative plus simple si une petite marge est acceptable :
            // assertTrue(Duration.between(beforeConstruction, adhesion.getDateAdhesion()).getSeconds() < 1);
        }

        @Test
        @DisplayName("Constructeur avec membre null doit lancer IllegalArgumentException")
        void constructor_withNullMembre_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new Adhesion(null, validClub);
            });
            // Optionnel: Vérifier le message de l'exception
            assertEquals("Membre et Club ne peuvent pas être nuls pour créer une Adhesion", exception.getMessage());
        }

        @Test
        @DisplayName("Constructeur avec club null doit lancer IllegalArgumentException")
        void constructor_withNullClub_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new Adhesion(validMembre, null);
            });
            assertEquals("Membre et Club ne peuvent pas être nuls pour créer une Adhesion", exception.getMessage());
        }

        @Test
        @DisplayName("Constructeur avec membre et club null doit lancer IllegalArgumentException")
        void constructor_withBothNull_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new Adhesion(null, null);
            });
            assertEquals("Membre et Club ne peuvent pas être nuls pour créer une Adhesion", exception.getMessage());
        }

    } // Fin @Nested ConstructorTests


    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur membre et club)")
    class EqualsHashCodeTests {

        private Membre membre1;
        private Membre membre2; // Différent de membre1
        private Club club1;
        private Club club2;   // Différent de club1

        @BeforeEach
        void setUpEntities() {
            membre1 = createMockMembre(1);
            membre2 = createMockMembre(2);
            club1 = createMockClub(10);
            club2 = createMockClub(20);
        }

        @Test
        @DisplayName("equals: même membre et même club => true")
        void equals_sameMembreSameClub_shouldBeTrue() {
            // Arrange: Deux adhésions créées séparément mais avec les mêmes membre/club
            // La date de création sera légèrement différente mais ignorée par equals/hashCode
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            // Petite pause pour s'assurer que dateAdhesion puisse être différente
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Adhesion adhesion2 = new Adhesion(membre1, club1);
            adhesion1.setId(100); // L'ID est aussi ignoré par equals/hashCode
            adhesion2.setId(200);

            // Assert
            assertEquals(adhesion1, adhesion2, "Adhésions avec même membre et club devraient être égales");
            assertTrue(adhesion1.equals(adhesion2) && adhesion2.equals(adhesion1)); // Symétrie
        }

        @Test
        @DisplayName("equals: membre différent, même club => false")
        void equals_differentMembreSameClub_shouldBeFalse() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            Adhesion adhesion2 = new Adhesion(membre2, club1); // Membre différent
            assertNotEquals(adhesion1, adhesion2);
        }

        @Test
        @DisplayName("equals: même membre, club différent => false")
        void equals_sameMembreDifferentClub_shouldBeFalse() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            Adhesion adhesion2 = new Adhesion(membre1, club2); // Club différent
            assertNotEquals(adhesion1, adhesion2);
        }

        @Test
        @DisplayName("equals: membre différent, club différent => false")
        void equals_differentMembreDifferentClub_shouldBeFalse() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            Adhesion adhesion2 = new Adhesion(membre2, club2); // Les deux différents
            assertNotEquals(adhesion1, adhesion2);
        }

        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            assertNotEquals(null, adhesion1);
            assertNotEquals(null, adhesion1); // Appel direct
        }

        @Test
        @DisplayName("equals: comparaison avec classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            Object autre = new Object();
            assertNotEquals(adhesion1, autre);
            assertNotEquals(adhesion1, autre); // Appel direct
        }

        @Test
        @DisplayName("equals: même objet => true (reflexivité)")
        void equals_sameObject_shouldBeTrue() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            assertEquals(adhesion1, adhesion1);
        }

        @Test
        @DisplayName("hashCode: même membre et même club => même hashCode")
        void hashCode_sameMembreSameClub_shouldBeEqual() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            // Petite pause pour s'assurer que dateAdhesion puisse être différente
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Adhesion adhesion2 = new Adhesion(membre1, club1);
            adhesion1.setId(100); // L'ID est aussi ignoré
            adhesion2.setId(200);

            assertEquals(adhesion1.hashCode(), adhesion2.hashCode(), "hashCode doit être le même pour des adhésions égales");
        }

        @Test
        @DisplayName("hashCode: membre différent => hashCode différent (probable)")
        void hashCode_differentMembre_shouldBeDifferent() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            Adhesion adhesion2 = new Adhesion(membre2, club1); // Membre différent
            // Pas garanti à 100% mais très probable
            assertNotEquals(adhesion1.hashCode(), adhesion2.hashCode(), "hashCode devrait différer si le membre diffère");
        }

        @Test
        @DisplayName("hashCode: club différent => hashCode différent (probable)")
        void hashCode_differentClub_shouldBeDifferent() {
            Adhesion adhesion1 = new Adhesion(membre1, club1);
            Adhesion adhesion2 = new Adhesion(membre1, club2); // Club différent
            assertNotEquals(adhesion1.hashCode(), adhesion2.hashCode(), "hashCode devrait différer si le club diffère");
        }

        @Test
        @DisplayName("hashCode: consistance (ne change pas si champ non inclus modifié)")
        void hashCode_consistent() {
            Adhesion adhesion = new Adhesion(membre1, club1);
            adhesion.setId(50); // ID non inclus dans hashCode
            int initialHashCode = adhesion.hashCode();

            // Modifie des champs non inclus (ID et dateAdhesion, bien que dateAdhesion soit updatable=false)
            adhesion.setId(150);
            // adhesion.setDateAdhesion(LocalDateTime.now().plusDays(1)); // Ne devrait pas être possible à cause de updatable=false

            assertEquals(initialHashCode, adhesion.hashCode(), "hashCode ne doit pas changer si seuls ID ou dateAdhesion sont modifiés");
        }

    } // Fin @Nested EqualsHashCodeTests

}

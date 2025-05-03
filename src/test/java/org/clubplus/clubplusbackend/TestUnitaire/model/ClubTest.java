package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Club;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de tests unitaires pour l'entité Club.
 * Teste les contraintes de validation, la logique de la méthode generateCode(),
 * et la logique equals/hashCode.
 */
public class ClubTest {

    // Le validateur Jakarta Bean Validation
    private static Validator validator;

    // Initialisation du validateur une seule fois
    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
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
    // Helper Method pour créer un Club valide de base
    // ========================================================================
    private Club createValidBaseClub() {
        Club club = new Club();
        club.setNom("Club de Test Valide");
        club.setDate_creation(LocalDate.now().minusDays(1)); // Passé récent
        club.setDate_inscription(LocalDate.now().minusDays(1)); // Passé récent (supposé identique ici)
        club.setNumero_voie("123");
        club.setRue("Rue des Tests");
        club.setCodepostal("75001");
        club.setVille("Testville");
        club.setTelephone("0123456789");
        club.setEmail("contact@clubtestvalide.com");
        // codeClub n'est pas setté ici car généré post-persist
        // Les collections adhesions et evenements sont initialisées vides par défaut
        return club;
    }

    // ========================================================================
    // Test Cases (Cas de Test)
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation des Contraintes")
    class ValidationTests {

        @Test
        @DisplayName("Club valide ne doit avoir aucune violation")
        void validClub_shouldHaveNoViolations() {
            Club club = createValidBaseClub();
            Set<ConstraintViolation<Club>> violations = validator.validate(club);
            assertTrue(violations.isEmpty(), "Un club valide ne devrait avoir aucune violation");
        }

        // --- Tests pour 'nom' (@NotBlank, @Size(min=2, max=100)) ---
        @Nested
        @DisplayName("Validation 'nom'")
        class NomValidation {
            @Test
            @DisplayName("nom: null")
            void nullNom() {
                Club c = createValidBaseClub();
                c.setNom(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "nom", "Le nom du club ne peut pas être vide.");
            }

            @Test
            @DisplayName("nom: vide")
            void emptyNom() {
                Club c = createValidBaseClub();
                c.setNom(""); // Viole @NotBlank et @Size(min=2)
                Set<ConstraintViolation<Club>> v = validator.validate(c);
                assertEquals(2, v.size());
                assertTrue(v.stream().anyMatch(viol -> viol.getMessage().equals("Le nom du club ne peut pas être vide.")));
                assertTrue(v.stream().anyMatch(viol -> viol.getMessage().equals("Le nom du club doit contenir entre 2 et 100 caractères.")));
            }

            @Test
            @DisplayName("nom: espaces")
            void whitespaceNom() {
                Club c = createValidBaseClub();
                c.setNom("   ");
                assertSingleViolationWithExactMessage(validator.validate(c), "nom", "Le nom du club ne peut pas être vide.");
            }

            @Test
            @DisplayName("nom: trop court")
            void tooShortNom() {
                Club c = createValidBaseClub();
                c.setNom("A");
                assertSingleViolation(validator.validate(c), "nom", "entre 2 et 100");
            }

            @Test
            @DisplayName("nom: trop long")
            void tooLongNom() {
                Club c = createValidBaseClub();
                c.setNom("A".repeat(101));
                assertSingleViolation(validator.validate(c), "nom", "entre 2 et 100");
            }
        }

        // --- Tests pour 'date_creation' (@NotNull, @PastOrPresent) ---
        @Nested
        @DisplayName("Validation 'date_creation'")
        class DateCreationValidation {
            @Test
            @DisplayName("date_creation: null")
            void nullDate() {
                Club c = createValidBaseClub();
                c.setDate_creation(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "date_creation", "La date de création est obligatoire.");
            }

            @Test
            @DisplayName("date_creation: futur")
            void futureDate() {
                Club c = createValidBaseClub();
                c.setDate_creation(LocalDate.now().plusDays(1));
                assertSingleViolationWithExactMessage(validator.validate(c), "date_creation", "La date de création doit être dans le passé ou aujourd'hui.");
            }

            @Test
            @DisplayName("date_creation: aujourd'hui (valide)")
            void todayDate() {
                Club c = createValidBaseClub();
                c.setDate_creation(LocalDate.now());
                assertTrue(validator.validate(c).isEmpty(), "Date de création aujourd'hui doit être valide");
            }
        }

        // --- Tests pour 'date_inscription' (@NotNull, @PastOrPresent) ---
        // Note: Similaire à date_creation, car les annotations sont identiques
        @Nested
        @DisplayName("Validation 'date_inscription'")
        class DateInscriptionValidation {
            @Test
            @DisplayName("date_inscription: null")
            void nullDate() {
                Club c = createValidBaseClub();
                c.setDate_inscription(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "date_inscription", "La date d'inscription est obligatoire.");
            }

            @Test
            @DisplayName("date_inscription: futur")
            void futureDate() {
                Club c = createValidBaseClub();
                c.setDate_inscription(LocalDate.now().plusDays(1));
                assertSingleViolationWithExactMessage(validator.validate(c), "date_inscription", "La date d'inscription doit être dans le passé ou aujourd'hui.");
            }

            @Test
            @DisplayName("date_inscription: aujourd'hui (valide)")
            void todayDate() {
                Club c = createValidBaseClub();
                c.setDate_inscription(LocalDate.now());
                assertTrue(validator.validate(c).isEmpty(), "Date d'inscription aujourd'hui doit être valide");
            }
        }

        // --- Tests pour les champs Adresse ---
        @Nested
        @DisplayName("Validation Adresse")
        class AdresseValidation {
            // numero_voie (@NotBlank, @Size(max=10))
            @Test
            @DisplayName("numero_voie: null")
            void nullNumero() {
                Club c = createValidBaseClub();
                c.setNumero_voie(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "numero_voie", "Le numéro de voie est obligatoire.");
            }

            @Test
            @DisplayName("numero_voie: trop long")
            void tooLongNumero() {
                Club c = createValidBaseClub();
                c.setNumero_voie("12345678901");
                assertSingleViolation(validator.validate(c), "numero_voie", "ne doit pas dépasser 10");
            }

            // rue (@NotBlank, @Size(max=100))
            @Test
            @DisplayName("rue: null")
            void nullRue() {
                Club c = createValidBaseClub();
                c.setRue(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "rue", "La rue est obligatoire.");
            }

            @Test
            @DisplayName("rue: trop long")
            void tooLongRue() {
                Club c = createValidBaseClub();
                c.setRue("A".repeat(101));
                assertSingleViolation(validator.validate(c), "rue", "ne doit pas dépasser 100");
            }

            // codepostal (@NotBlank, @Size(min=3, max=10))
            @Test
            @DisplayName("codepostal: null")
            void nullCP() {
                Club c = createValidBaseClub();
                c.setCodepostal(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "codepostal", "Le code postal est obligatoire.");
            }

            @Test
            @DisplayName("codepostal: trop court")
            void tooShortCP() {
                Club c = createValidBaseClub();
                c.setCodepostal("12");
                assertSingleViolation(validator.validate(c), "codepostal", "entre 3 et 10");
            }

            @Test
            @DisplayName("codepostal: trop long")
            void tooLongCP() {
                Club c = createValidBaseClub();
                c.setCodepostal("12345678901");
                assertSingleViolation(validator.validate(c), "codepostal", "entre 3 et 10");
            }

            // ville (@NotBlank, @Size(max=100))
            @Test
            @DisplayName("ville: null")
            void nullVille() {
                Club c = createValidBaseClub();
                c.setVille(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "ville", "La ville est obligatoire.");
            }

            @Test
            @DisplayName("ville: trop long")
            void tooLongVille() {
                Club c = createValidBaseClub();
                c.setVille("A".repeat(101));
                assertSingleViolation(validator.validate(c), "ville", "ne doit pas dépasser 100");
            }
        }

        // --- Tests pour les champs Contact ---
        @Nested
        @DisplayName("Validation Contact")
        class ContactValidation {
            // telephone (@NotBlank, @Size(max=20))
            @Test
            @DisplayName("telephone: null")
            void nullTel() {
                Club c = createValidBaseClub();
                c.setTelephone(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "telephone", "Le numéro de téléphone est obligatoire.");
            }

            @Test
            @DisplayName("telephone: trop long")
            void tooLongTel() {
                Club c = createValidBaseClub();
                c.setTelephone("0".repeat(21));
                assertSingleViolation(validator.validate(c), "telephone", "ne doit pas dépasser 20");
            }

            // email (@NotBlank, @Email, @Size(max=254))
            @Test
            @DisplayName("email: null")
            void nullEmail() {
                Club c = createValidBaseClub();
                c.setEmail(null);
                assertSingleViolationWithExactMessage(validator.validate(c), "email", "L'email du club est obligatoire.");
            }

            @Test
            @DisplayName("email: vide")
            void emptyEmail() {
                Club c = createValidBaseClub();
                c.setEmail("");
                // Viole @NotBlank et potentiellement @Email
                Set<ConstraintViolation<Club>> v = validator.validate(c);
                assertFalse(v.isEmpty());
                assertTrue(v.stream().anyMatch(viol -> viol.getMessage().equals("L'email du club est obligatoire.")));
            }

            @Test
            @DisplayName("email: format invalide")
            void invalidFormatEmail() {
                // Exclut "test@domain" qui est souvent valide
                String[] invalidEmails = {"test", "test@", "@domain.com", "test@.com", "test@domain."};
                for (String invalidEmail : invalidEmails) {
                    Club c = createValidBaseClub();
                    c.setEmail(invalidEmail);
                    Set<ConstraintViolation<Club>> v = validator.validate(c);
                    assertTrue(v.stream().anyMatch(viol -> viol.getPropertyPath().toString().equals("email") &&
                                    viol.getMessage().equals("Le format de l'email est invalide.")),
                            "Format invalide pour: " + invalidEmail);
                }
            }

            @Test
            @DisplayName("email: trop long")
            void tooLongEmail() {
                Club c = createValidBaseClub();
                String longEmail = "a".repeat(245) + "@example.com"; // 257 chars
                c.setEmail(longEmail);
                Set<ConstraintViolation<Club>> v = validator.validate(c);
                // Attendu: violation @Size et potentiellement @Email
                assertFalse(v.isEmpty());
                assertTrue(v.stream().allMatch(viol -> viol.getPropertyPath().toString().equals("email")));
                assertTrue(v.stream().anyMatch(viol -> viol.getMessage().contains("ne doit pas dépasser 254")),
                        "Violation @Size attendue");
            }
        }

    } // Fin @Nested ValidationTests


    @Nested
    @DisplayName("Tests de la méthode generateCode()")
    class GenerateCodeTest {

        @Test
        @DisplayName("generateCode: ID set, codeClub null => codeClub généré")
        void generateCode_whenIdSetAndCodeNull_shouldGenerateCode() {
            // Arrange
            Club club = createValidBaseClub();
            club.setId(123); // Simule un ID généré par la BDD
            club.setCodeClub(null); // Assure que le code n'est pas déjà là

            // Act: Appelle directement la méthode (sans passer par JPA @PostPersist)
            club.generateCode();

            // Assert
            assertNotNull(club.getCodeClub(), "codeClub ne devrait plus être null");
            assertEquals("CLUB-0123", club.getCodeClub(), "Le code généré est incorrect");
        }

        @Test
        @DisplayName("generateCode: ID set (petit), codeClub null => codeClub généré avec padding")
        void generateCode_whenSmallIdSetAndCodeNull_shouldGenerateCodeWithPadding() {
            // Arrange
            Club club = createValidBaseClub();
            club.setId(5); // Petit ID
            club.setCodeClub(null);
            // Act
            club.generateCode();
            // Assert
            assertEquals("CLUB-0005", club.getCodeClub(), "Padding incorrect pour petit ID");
        }

        @Test
        @DisplayName("generateCode: ID null => codeClub reste null")
        void generateCode_whenIdNull_shouldNotGenerateCode() {
            // Arrange
            Club club = createValidBaseClub();
            club.setId(null); // Simule un objet avant persistance
            club.setCodeClub(null);

            // Act
            club.generateCode();

            // Assert
            assertNull(club.getCodeClub(), "codeClub doit rester null si l'ID est null");
        }

        @Test
        @DisplayName("generateCode: ID set, codeClub déjà set => codeClub non modifié")
        void generateCode_whenCodeAlreadySet_shouldNotOverwrite() {
            // Arrange
            Club club = createValidBaseClub();
            club.setId(456);
            String existingCode = "EXISTING-CODE";
            club.setCodeClub(existingCode); // Code déjà défini

            // Act
            club.generateCode();

            // Assert
            assertEquals(existingCode, club.getCodeClub(), "Le codeClub existant ne doit pas être écrasé");
        }
    } // Fin @Nested GenerateCodeTest


    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur ID)")
    class EqualsHashCodeTests {
        // La logique est identique à celle de CategorieTest, juste remplacer Categorie par Club
        @Test
        @DisplayName("equals: même ID => true")
        void equals_sameId_shouldBeTrue() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            Club c2 = createValidBaseClub();
            c2.setId(1);
            c2.setNom("Autre");
            assertEquals(c1, c2);
            assertTrue(c1.equals(c2) && c2.equals(c1));
        }

        @Test
        @DisplayName("equals: ID différents => false")
        void equals_differentId_shouldBeFalse() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            Club c2 = createValidBaseClub();
            c2.setId(2);
            assertNotEquals(c1, c2);
        }

        @Test
        @DisplayName("equals: un ID null => false")
        void equals_oneIdNull_shouldBeFalse() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            Club c2 = createValidBaseClub(); // ID null
            assertNotEquals(c1, c2);
            assertNotEquals(c2, c1);
        }

        @Test
        @DisplayName("equals: deux ID null => false (si objets différents)")
        void equals_bothIdNull_shouldBeFalse() {
            Club c1 = createValidBaseClub();
            c1.setNom("Nom 1");
            Club c2 = createValidBaseClub();
            c2.setNom("Nom 2");
            assertNotEquals(c1, c2);
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            assertEquals(c1, c1);
        }

        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            assertNotEquals(null, c1);
            assertNotEquals(null, c1);
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            Object autre = new Object();
            assertNotEquals(c1, autre);
            assertNotEquals(c1, autre);
        }

        @Test
        @DisplayName("hashCode: même ID => même hashCode")
        void hashCode_sameId_shouldBeEqual() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            Club c2 = createValidBaseClub();
            c2.setId(1);
            assertEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("hashCode: ID différents => hashCodes différents (probable)")
        void hashCode_differentId_shouldBeDifferent() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            Club c2 = createValidBaseClub();
            c2.setId(2);
            assertNotEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("hashCode: consistance")
        void hashCode_consistent() {
            Club c1 = createValidBaseClub();
            c1.setId(1);
            int initialHashCode = c1.hashCode();
            c1.setNom("Nouveau Nom"); // Modifie un champ non utilisé dans hashCode
            assertEquals(initialHashCode, c1.hashCode());
        }
    } // Fin @Nested EqualsHashCodeTests

}

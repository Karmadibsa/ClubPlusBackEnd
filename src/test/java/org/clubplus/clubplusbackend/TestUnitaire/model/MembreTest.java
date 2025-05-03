package org.clubplus.clubplusbackend.TestUnitaire.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classe de tests unitaires pour l'entité Membre.
 * Utilise Jakarta Bean Validation (JSR 380) pour tester les contraintes d'annotations.
 */
public class MembreTest {

    // Le validateur Jakarta Bean Validation, partagé par tous les tests.
    private static Validator validator;

    // Initialisation du validateur une seule fois pour toute la classe de test (@BeforeAll)
    // C'est légèrement plus efficace que @BeforeEach si la création de la factory est coûteuse.
    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Méthode utilitaire pour créer une instance de Membre avec des données valides.
     * Réduit la duplication de code dans les tests.
     *
     * @return Un objet Membre valide.
     */
    private Membre createValidBaseMembre() {
        Membre membre = new Membre();
        membre.setNom("ValideNom");
        membre.setPrenom("ValidePrenom");
        membre.setDate_naissance(LocalDate.of(1995, 5, 20)); // Date passée valide
        membre.setDate_inscription(LocalDate.now()); // Date valide
        membre.setNumero_voie("123");
        membre.setRue("Rue Valide");
        membre.setCodepostal("12345");
        membre.setVille("Ville Valide");
        membre.setTelephone("0102030405");
        membre.setEmail("valide.email@example.com");
        membre.setPassword("MotDePasseValide123"); // > 8 caractères
        membre.setRole(Role.MEMBRE);
        return membre;
    }

    // ========================================================================
    // Helper Assertion Methods (Méthodes d'Assertion Utilitaires)
    // ========================================================================

    /**
     * Méthode d'assertion réutilisable pour vérifier qu'il y a exactement UNE violation
     * pour une propriété donnée et contenant (optionnellement) un message spécifique.
     *
     * @param violations          L'ensemble des violations retourné par le validateur.
     * @param expectedProperty    Le nom du champ attendu ("nom", "email", etc.).
     * @param expectedMessagePart Une partie du message d'erreur attendu (peut être null si non vérifié).
     */
    private void assertSingleViolation(Set<ConstraintViolation<Membre>> violations, String expectedProperty, String expectedMessagePart) {
        // Vérifie qu'il y a exactement une violation
        Assertions.assertEquals(1, violations.size(),
                "Devrait avoir exactement une violation pour la propriété '" + expectedProperty + "'");

        // Récupère la seule violation
        ConstraintViolation<Membre> violation = violations.iterator().next();

        // Vérifie que la violation concerne la bonne propriété
        Assertions.assertEquals(expectedProperty, violation.getPropertyPath().toString(),
                "La violation devrait concerner la propriété '" + expectedProperty + "'");

        // Si une partie du message est attendue, vérifie qu'elle est présente
        if (expectedMessagePart != null && !expectedMessagePart.isEmpty()) {
            Assertions.assertTrue(violation.getMessage().contains(expectedMessagePart),
                    "Le message de violation pour '" + expectedProperty + "' devrait contenir '" + expectedMessagePart + "', mais était : '" + violation.getMessage() + "'");
        }
    }

    /**
     * Méthode d'assertion réutilisable pour vérifier qu'il y a exactement UNE violation
     * pour une propriété donnée avec un message exact.
     *
     * @param violations           L'ensemble des violations retourné par le validateur.
     * @param expectedProperty     Le nom du champ attendu ("nom", "email", etc.).
     * @param expectedExactMessage Le message d'erreur exact attendu.
     */
    private void assertSingleViolationWithExactMessage(Set<ConstraintViolation<Membre>> violations, String expectedProperty, String expectedExactMessage) {
        Assertions.assertEquals(1, violations.size(),
                "Devrait avoir exactement une violation pour la propriété '" + expectedProperty + "'");
        ConstraintViolation<Membre> violation = violations.iterator().next();
        Assertions.assertEquals(expectedProperty, violation.getPropertyPath().toString(),
                "La violation devrait concerner la propriété '" + expectedProperty + "'");
        Assertions.assertEquals(expectedExactMessage, violation.getMessage(),
                "Le message de violation pour '" + expectedProperty + "' devrait être exactement '" + expectedExactMessage + "'");
    }


    // ========================================================================
    // Test Cases (Cas de Test)
    // ========================================================================

    // --- Test du cas valide ---
    @Test
    @DisplayName("Un Membre valide ne doit avoir aucune violation")
    void createValidMembre_shouldHaveNoViolations() {
        // Arrange
        Membre membreTest = createValidBaseMembre();
        // Act
        Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
        // Assert
        Assertions.assertTrue(violations.isEmpty(), "Un membre valide ne devrait avoir aucune violation");
    }

    // --- Tests pour le champ 'nom' (@NotBlank, @Size(min=2, max=50)) ---
    @Nested // Groupe les tests relatifs au champ 'nom' pour une meilleure organisation
    @DisplayName("Tests de validation pour 'nom'")
    class NomValidationTests {

        @Test
        @DisplayName("nom: null doit être invalide (@NotBlank)")
        void nomIsNull_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setNom(null);
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            // Vérifie la violation spécifique pour @NotBlank (on peut tester le message exact si défini)
            assertSingleViolationWithExactMessage(violations, "nom", "Le nom ne peut pas être vide.");
        }

        @Test
        @DisplayName("nom: vide doit être invalide (@NotBlank et @Size min)")
        void nomIsEmpty_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setNom(""); // Viole @NotBlank ET @Size(min=2)
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);

            // Assert: S'attendre à DEUX violations
            Assertions.assertEquals(2, violations.size(), "Un nom vide doit violer @NotBlank et @Size(min=2)");
            // Vérifie que les deux violations concernent 'nom' et contiennent les bons messages
            Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
            Assertions.assertTrue(messages.contains("Le nom ne peut pas être vide."));
            Assertions.assertTrue(messages.contains("Le nom doit contenir entre 2 et 50 caractères."));
            Assertions.assertTrue(violations.stream().allMatch(v -> v.getPropertyPath().toString().equals("nom")));
        }

        @Test
        @DisplayName("nom: espaces seuls doit être invalide (@NotBlank)")
        void nomIsWhitespace_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setNom("   "); // @NotBlank échoue
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolationWithExactMessage(violations, "nom", "Le nom ne peut pas être vide.");
        }

        @Test
        @DisplayName("nom: trop court doit être invalide (@Size min)")
        void nomIsTooShort_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setNom("A"); // @Size(min=2) échoue
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            // Utilise la méthode helper pour vérifier la violation de taille
            assertSingleViolation(violations, "nom", "entre 2 et 50");
        }

        @Test
        @DisplayName("nom: trop long doit être invalide (@Size max)")
        void nomIsTooLong_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setNom("A".repeat(51)); // @Size(max=50) échoue
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolation(violations, "nom", "entre 2 et 50");
        }
    } // Fin @Nested NomValidationTests

    // --- Tests pour le champ 'prenom' (@NotBlank, @Size(min=2, max=50)) ---
    // (Structure similaire à 'nom', on utilise @Nested et les helpers)
    @Nested
    @DisplayName("Tests de validation pour 'prenom'")
    class PrenomValidationTests {
        @Test
        @DisplayName("prenom: null doit être invalide (@NotBlank)")
        void prenomIsNull_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setPrenom(null);
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolationWithExactMessage(violations, "prenom", "Le prénom ne peut pas être vide.");
        }

        // Note: Le test pour prenom vide serait identique à celui pour nom vide (2 violations)
        @Test
        @DisplayName("prenom: vide doit être invalide (@NotBlank et @Size min)")
        void prenomIsEmpty_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setPrenom(""); // Viole @NotBlank ET @Size(min=2)
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            Assertions.assertEquals(2, violations.size());
            Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
            Assertions.assertTrue(messages.contains("Le prénom ne peut pas être vide."));
            Assertions.assertTrue(messages.contains("Le prénom doit contenir entre 2 et 50 caractères."));
            Assertions.assertTrue(violations.stream().allMatch(v -> v.getPropertyPath().toString().equals("prenom")));
        }


        @Test
        @DisplayName("prenom: trop court doit être invalide (@Size min)")
        void prenomIsTooShort_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setPrenom("B"); // @Size(min=2) échoue
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolation(violations, "prenom", "entre 2 et 50");
        }

        @Test
        @DisplayName("prenom: trop long doit être invalide (@Size max)")
        void prenomIsTooLong_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setPrenom("B".repeat(51)); // @Size(max=50) échoue
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolation(violations, "prenom", "entre 2 et 50");
        }
    } // Fin @Nested PrenomValidationTests

    // --- Tests pour le champ 'date_naissance' (@NotNull, @Past) ---
    @Nested
    @DisplayName("Tests de validation pour 'date_naissance'")
    class DateNaissanceValidationTests {
        @Test
        @DisplayName("date_naissance: null doit être invalide (@NotNull)")
        void dateNaissanceIsNull_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setDate_naissance(null);
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            // Utilisation de l'helper avec message exact
            assertSingleViolationWithExactMessage(violations, "date_naissance", "La date de naissance est obligatoire.");
        }

        @Test
        @DisplayName("date_naissance: future doit être invalide (@Past)")
        void dateNaissanceIsInFuture_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setDate_naissance(LocalDate.now().plusDays(1));
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolationWithExactMessage(violations, "date_naissance", "La date de naissance doit être dans le passé.");
        }

        @Test
        @DisplayName("date_naissance: aujourd'hui doit être invalide (@Past)")
        void dateNaissanceIsToday_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setDate_naissance(LocalDate.now());
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            assertSingleViolationWithExactMessage(violations, "date_naissance", "La date de naissance doit être dans le passé.");
        }
    } // Fin @Nested DateNaissanceValidationTests

    // --- Tests pour le champ 'date_inscription' (@NotNull) ---
    @Nested
    @DisplayName("Tests de validation pour 'date_inscription'")
    class DateInscriptionValidationTests {
        @Test
        @DisplayName("date_inscription: null doit être invalide (@NotNull)")
        void dateInscriptionIsNull_shouldBeInvalid() {
            Membre membreTest = createValidBaseMembre();
            membreTest.setDate_inscription(null);
            Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);
            // Ici, on n'a pas défini de message custom pour @NotNull,
            // donc on utilise l'helper SANS vérifier le message (message par défaut dépend de l'implémentation)
            assertSingleViolation(violations, "date_inscription", null); // Vérifie juste 1 violation sur le bon champ
            // Si tu ajoutais message="..." à @NotNull, tu pourrais utiliser assertSingleViolationWithExactMessage ici.
        }
    } // Fin @Nested DateInscriptionValidationTests


    // --- Tests pour l'adresse (numero_voie, rue, codepostal, ville) ---
    @Nested
    @DisplayName("Tests de validation pour l'adresse")
    class AdresseValidationTests {

        // numero_voie (@NotBlank, @Size(max=10))
        @Test
        @DisplayName("numero_voie: null doit être invalide")
        void numeroVoieIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setNumero_voie(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "numero_voie", "Le numéro de voie est obligatoire.");
        }

        @Test
        @DisplayName("numero_voie: trop long doit être invalide")
        void numeroVoieIsTooLong_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setNumero_voie("12345678901");
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "numero_voie", "ne doit pas dépasser 10");
        }

        // rue (@NotBlank, @Size(max=100))
        @Test
        @DisplayName("rue: null doit être invalide")
        void rueIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setRue(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "rue", "La rue est obligatoire.");
        }

        @Test
        @DisplayName("rue: trop longue doit être invalide")
        void rueIsTooLong_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setRue("A".repeat(101));
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "rue", "ne doit pas dépasser 100");
        }

        // codepostal (@NotBlank, @Size(min=3, max=10))
        @Test
        @DisplayName("codepostal: null doit être invalide")
        void codePostalIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setCodepostal(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "codepostal", "Le code postal est obligatoire.");
        }

        @Test
        @DisplayName("codepostal: trop court doit être invalide")
        void codePostalIsTooShort_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setCodepostal("12");
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "codepostal", "entre 3 et 10");
        }

        @Test
        @DisplayName("codepostal: trop long doit être invalide")
        void codePostalIsTooLong_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setCodepostal("12345678901");
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "codepostal", "entre 3 et 10");
        }

        // ville (@NotBlank, @Size(max=100))
        @Test
        @DisplayName("ville: null doit être invalide")
        void villeIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setVille(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "ville", "La ville est obligatoire.");
        }

        @Test
        @DisplayName("ville: trop longue doit être invalide")
        void villeIsTooLong_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setVille("A".repeat(101));
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "ville", "ne doit pas dépasser 100");
        }
    } // Fin @Nested AdresseValidationTests


    // --- Tests pour le champ 'telephone' (@NotBlank, @Size(max=20)) ---
    @Nested
    @DisplayName("Tests de validation pour 'telephone'")
    class TelephoneValidationTests {
        @Test
        @DisplayName("telephone: null doit être invalide")
        void telephoneIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setTelephone(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "telephone", "Le numéro de téléphone est obligatoire.");
        }

        @Test
        @DisplayName("telephone: trop long doit être invalide")
        void telephoneIsTooLong_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setTelephone("0".repeat(21));
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "telephone", "ne doit pas dépasser 20");
        }
    } // Fin @Nested TelephoneValidationTests


    // --- Tests pour le champ 'email' (@NotBlank, @Email, @Size(max=254)) ---
    @Nested
    @DisplayName("Tests de validation pour 'email'")
    class EmailValidationTests {
        @Test
        @DisplayName("email: null doit être invalide (@NotBlank)")
        void emailIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setEmail(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            // @NotBlank a priorité sur @Email et @Size si null
            assertSingleViolationWithExactMessage(v, "email", "L'email est obligatoire.");
        }

        @Test
        @DisplayName("email: vide doit être invalide (@NotBlank)")
        void emailIsEmpty_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setEmail("");
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            // "" viole @NotBlank mais est syntaxiquement valide pour @Email (parfois)
            // On s'attend principalement à la violation @NotBlank
            Assertions.assertFalse(v.isEmpty()); // Au moins une violation
            boolean notBlankViolationFound = v.stream()
                    .anyMatch(viol -> viol.getPropertyPath().toString().equals("email") &&
                            viol.getMessage().equals("L'email est obligatoire."));
            Assertions.assertTrue(notBlankViolationFound, "Violation @NotBlank attendue pour email vide");

            // Optionnel: vérifier s'il y a AUSSI une violation @Email (peut dépendre de l'implémentation du validateur)
            // boolean emailFormatViolationFound = v.stream()
            //        .anyMatch(viol -> viol.getPropertyPath().toString().equals("email") &&
            //                           viol.getMessage().equals("Le format de l'email est invalide."));
            // Selon le validateur, cette seconde violation peut exister ou non.
        }

        @Test
        @DisplayName("email: format invalide doit échouer (@Email)")
        void emailFormatIsInvalid_shouldBeInvalid() {
            // "test@domain" est souvent valide par défaut, on l'exclut.
            String[] invalidEmails = {"test", "test@", "@domain.com", "test@.com", "test@domain."};

            for (String invalidEmail : invalidEmails) {
                Membre membreTest = createValidBaseMembre();
                membreTest.setEmail(invalidEmail);
                Set<ConstraintViolation<Membre>> violations = validator.validate(membreTest);

                // Assert: Au moins une violation pour 'email' avec le message @Email
                Assertions.assertFalse(violations.isEmpty(), "Invalide: '" + invalidEmail + "'");
                boolean emailFormatViolationFound = violations.stream()
                        .filter(v -> v.getPropertyPath().toString().equals("email"))
                        .anyMatch(v -> v.getMessage().equals("Le format de l'email est invalide."));
                Assertions.assertTrue(emailFormatViolationFound,
                        "Message '@Email' attendu pour: " + invalidEmail);
            }
        }

        @Test
        @DisplayName("email: trop long doit être invalide (@Size max et potentiellement @Email)")
        void emailIsTooLong_shouldBeInvalid() {
            // Arrange
            Membre m = createValidBaseMembre();
            String longEmail = "a".repeat(245) + "@example.com"; // 257 caractères > 254
            m.setEmail(longEmail);

            // Act
            Set<ConstraintViolation<Membre>> violations = validator.validate(m);

            violations.forEach(v -> System.out.println("  - Property: " + v.getPropertyPath() + ", Message: '" + v.getMessage() + "'"));
            // --- FIN DEBUGGING ---

            // Assert:
            // 1. S'assurer qu'il y a AU MOINS une violation.
            Assertions.assertFalse(violations.isEmpty(),
                    "Un email trop long (" + longEmail.length() + " chars) devrait causer au moins une violation.");

            // 2. Vérifier que TOUTES les violations concernent bien le champ 'email'.
            //    (Utile pour s'assurer qu'aucune autre contrainte inattendue n'est violée ailleurs).
            Assertions.assertTrue(violations.stream().allMatch(v -> v.getPropertyPath().toString().equals("email")),
                    "Toutes les violations devraient concerner le champ 'email'.");

            // 3. S'assurer que la violation spécifique à @Size (max=254) est présente parmi les violations.
            boolean sizeViolationFound = violations.stream()
                    .anyMatch(v -> v.getMessage().contains("ne doit pas dépasser 254"));
            Assertions.assertTrue(sizeViolationFound,
                    "La violation attendue pour @Size(max=254) sur le champ 'email' n'a pas été trouvée.");

        }

    } // Fin @Nested EmailValidationTests


    // --- Tests pour le champ 'password' (@NotBlank, @Size(min=8, max=100)) ---
    @Nested
    @DisplayName("Tests de validation pour 'password'")
    class PasswordValidationTests {
        @Test
        @DisplayName("password: null doit être invalide")
        void passwordIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setPassword(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "password", "Le mot de passe est obligatoire.");
        }

        @Test
        @DisplayName("password: trop court doit être invalide")
        void passwordIsTooShort_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setPassword("1234567");
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "password", "au moins 8");
        }

        @Test
        @DisplayName("password: trop long doit être invalide")
        void passwordIsTooLong_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setPassword("p".repeat(101));
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolation(v, "password", "ne doit pas dépasser 100");
        }
    } // Fin @Nested PasswordValidationTests


    // --- Tests pour le champ 'role' (@NotNull) ---
    @Nested
    @DisplayName("Tests de validation pour 'role'")
    class RoleValidationTests {
        @Test
        @DisplayName("role: null doit être invalide")
        void roleIsNull_shouldBeInvalid() {
            Membre m = createValidBaseMembre();
            m.setRole(null);
            Set<ConstraintViolation<Membre>> v = validator.validate(m);
            assertSingleViolationWithExactMessage(v, "role", "Le rôle est obligatoire.");
        }
    } // Fin @Nested RoleValidationTests


    // --- Tests pour equals() et hashCode() ---
    // (Ces tests sont spécifiques et n'utilisent pas le validateur)
    @Nested
    @DisplayName("Tests pour equals() et hashCode()")
    class EqualsHashCodeTests {
        @Test
        @DisplayName("equals: même ID => true")
        void equals_sameId_shouldBeTrue() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Membre m2 = createValidBaseMembre();
            m2.setId(1);
            m2.setNom("Autre");
            Assertions.assertEquals(m1, m2);
            Assertions.assertTrue(m1.equals(m2) && m2.equals(m1)); // Vérification symétrique
        }

        @Test
        @DisplayName("equals: ID différents => false")
        void equals_differentId_shouldBeFalse() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Membre m2 = createValidBaseMembre();
            m2.setId(2);
            Assertions.assertNotEquals(m1, m2);
        }

        @Test
        @DisplayName("equals: un ID null => false")
        void equals_oneIdNull_shouldBeFalse() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Membre m2 = createValidBaseMembre(); // ID null
            Assertions.assertNotEquals(m1, m2);
            Assertions.assertNotEquals(m2, m1); // Vérification symétrique
        }

        @Test
        @DisplayName("equals: deux ID null => false (si objets différents)")
        void equals_bothIdNull_shouldBeFalse() {
            Membre m1 = createValidBaseMembre();
            m1.setEmail("e1@a.com");
            Membre m2 = createValidBaseMembre();
            m2.setEmail("e2@a.com");
            // Basé sur l'implémentation hashCode/equals qui utilise getClass si ID null
            Assertions.assertNotEquals(m1, m2);
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Assertions.assertEquals(m1, m1);
        }

        @Test
        @DisplayName("equals: objet null => false")
        void equals_nullObject_shouldBeFalse() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Assertions.assertNotEquals(null, m1);
            Assertions.assertNotEquals(null, m1); // Appel direct pour être sûr
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_differentClass_shouldBeFalse() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Object autre = new Object();
            Assertions.assertNotEquals(m1, autre);
            Assertions.assertNotEquals(m1, autre); // Appel direct
        }

        @Test
        @DisplayName("hashCode: même ID => même hashCode")
        void hashCode_sameId_shouldBeEqual() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Membre m2 = createValidBaseMembre();
            m2.setId(1);
            Assertions.assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        @DisplayName("hashCode: ID différents => hashCodes différents (probable)")
        void hashCode_differentId_shouldBeDifferent() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            Membre m2 = createValidBaseMembre();
            m2.setId(2);
            // Pas garanti à 100% mais extrêmement probable
            Assertions.assertNotEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        @DisplayName("hashCode: consistence (ne change pas si champ non-ID modifié)")
        void hashCode_consistent() {
            Membre m1 = createValidBaseMembre();
            m1.setId(1);
            int initialHashCode = m1.hashCode();
            m1.setNom("NouveauNom"); // Modifie un champ non utilisé dans hashCode
            Assertions.assertEquals(initialHashCode, m1.hashCode());
        }
    } // Fin @Nested EqualsHashCodeTests


    // --- Tests pour les méthodes addAmi/removeAmi ---
    @Nested
    @DisplayName("Tests pour addAmi() et removeAmi()")
    class AmiRelationTests {
        // Membres utilisés dans ces tests
        private Membre membre1;
        private Membre membre2;

        @BeforeEach
            // Initialise les membres avant chaque test de ce groupe @Nested
        void setUpMembres() {
            membre1 = createValidBaseMembre();
            membre1.setId(1);
            membre2 = createValidBaseMembre();
            membre2.setId(2);
            membre2.setEmail("ami@test.com");
        }

        @Test
        @DisplayName("addAmi: établit la relation dans les deux sens")
        void addAmi_shouldAddBothWays() {
            // Act
            membre1.addAmi(membre2);
            // Assert
            Assertions.assertTrue(membre1.getAmis().contains(membre2));
            Assertions.assertTrue(membre2.getAmis().contains(membre1));
            Assertions.assertEquals(1, membre1.getAmis().size());
            Assertions.assertEquals(1, membre2.getAmis().size());
        }

        @Test
        @DisplayName("removeAmi: rompt la relation dans les deux sens")
        void removeAmi_shouldRemoveBothWays() {
            // Arrange: Crée l'amitié d'abord
            membre1.addAmi(membre2);
            // Act
            membre1.removeAmi(membre2);
            // Assert
            Assertions.assertFalse(membre1.getAmis().contains(membre2));
            Assertions.assertFalse(membre2.getAmis().contains(membre1));
            Assertions.assertTrue(membre1.getAmis().isEmpty());
            Assertions.assertTrue(membre2.getAmis().isEmpty());
        }

        @Test
        @DisplayName("addAmi: sécurité (collections null) ne doit pas planter")
        void addAmi_nullSafety_shouldNotThrowException() {
            // Arrange: Force les collections à null
            membre1.setAmis(null);
            membre2.setAmis(null);
            // Act & Assert
            Assertions.assertDoesNotThrow(() -> membre1.addAmi(membre2));
            // Vérifie que les collections ont été créées et contiennent les liens
            Assertions.assertNotNull(membre1.getAmis());
            Assertions.assertNotNull(membre2.getAmis());
            Assertions.assertTrue(membre1.getAmis().contains(membre2));
            Assertions.assertTrue(membre2.getAmis().contains(membre1));
        }

        @Test
        @DisplayName("removeAmi: sécurité (collections null) ne doit pas planter")
        void removeAmi_nullSafety_shouldNotThrowException() {
            // Arrange 1: La collection de celui qui appelle est null
            membre1.setAmis(null);
            // Act & Assert 1
            Assertions.assertDoesNotThrow(() -> membre1.removeAmi(membre2));
            Assertions.assertNull(membre1.getAmis()); // Doit rester null

            // Arrange 2: La collection de l'ami à retirer est null (après ajout)
            Membre m3 = createValidBaseMembre();
            m3.setId(3);
            Membre m4 = createValidBaseMembre();
            m4.setId(4);
            m3.addAmi(m4); // Ajout normal
            m4.setAmis(null); // Force la collection de l'ami à null
            // Act & Assert 2
            Assertions.assertDoesNotThrow(() -> m3.removeAmi(m4));
            Assertions.assertFalse(m3.getAmis().contains(m4)); // L'ami doit être retiré de m3
        }
    } // Fin @Nested AmiRelationTests

}

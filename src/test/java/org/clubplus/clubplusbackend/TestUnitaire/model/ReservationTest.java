package org.clubplus.clubplusbackend.TestUnitaire.model; // Adapte le package si nécessaire

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Reservation;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Classe de tests unitaires pour l'entité Reservation.
 * Teste les contraintes de validation, le constructeur (y compris la logique de cohérence),
 * la génération de l'UUID, la génération des données QR code, et la logique equals/hashCode.
 */
public class ReservationTest {

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
    private Membre createMockMembre(Integer id, String prenom, String nom) {
        Membre membre = Mockito.mock(Membre.class); // Crée le mock
        // Stub uniquement les méthodes nécessaires pour les tests de Reservation
        when(membre.getId()).thenReturn(id);
        when(membre.getPrenom()).thenReturn(prenom);
        when(membre.getNom()).thenReturn(nom);

        // *** SUPPRIMER LES LIGNES SUIVANTES ***
        // when(membre.hashCode()).thenReturn(id != null ? id.hashCode() : 0);
        // when(membre.equals(Mockito.any())).thenAnswer(invocation -> { ... });

        // Optionnel: Ajouter un toString() pour faciliter le debug si un mock pose problème
        when(membre.toString()).thenReturn("MockMembre(id=" + id + ")");
        return membre;
    }

    private Event createMockEvent(Integer id) {
        Event event = Mockito.mock(Event.class);
        when(event.getId()).thenReturn(id); // Stub getId()

        // *** SUPPRIMER LES LIGNES SUIVANTES ***
        // when(event.hashCode()).thenReturn(id != null ? id.hashCode() : 0);
        // when(event.equals(Mockito.any())).thenAnswer(invocation -> { ... });

        when(event.toString()).thenReturn("MockEvent(id=" + id + ")");
        return event;
    }

    private Categorie createMockCategorie(Integer id, Event associatedEvent) {
        Categorie categorie = Mockito.mock(Categorie.class);
        when(categorie.getId()).thenReturn(id);
        when(categorie.getEvent()).thenReturn(associatedEvent); // Stub getEvent()

        // *** SUPPRIMER LES LIGNES SUIVANTES ***
        // when(categorie.hashCode()).thenReturn(id != null ? id.hashCode() : 0);
        // when(categorie.equals(Mockito.any())).thenAnswer(invocation -> { ... });

        when(categorie.toString()).thenReturn("MockCategorie(id=" + id + ")");
        return categorie;
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
        if (expectedExactMessage != null) { // Pour @NotNull où le message n'est pas standard
            assertEquals(expectedExactMessage, violation.getMessage(), "Message exact incorrect");
        }
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    @Nested
    @DisplayName("Tests de Validation des Contraintes")
    class ValidationTests {

        private Membre validMembre;
        private Event validEvent;
        private Categorie validCategorie;

        @BeforeEach
        void setUpValidEntities() {
            validMembre = createMockMembre(1, "Jean", "Dupont");
            validEvent = createMockEvent(10);
            // Important: La catégorie doit être liée au MÊME mock Event
            validCategorie = createMockCategorie(100, validEvent);
        }

        // Méthode pour créer une réservation de base valide (via setters, pour tester les contraintes individuellement)
        private Reservation createValidBaseReservationForValidation() {
            Reservation res = new Reservation();
            res.setMembre(validMembre);
            res.setEvent(validEvent);
            res.setCategorie(validCategorie);
            res.setDateReservation(LocalDateTime.now().minusMinutes(5));
            res.setReservationUuid(UUID.randomUUID().toString()); // UUID valide
            return res;
        }

        @Test
        @DisplayName("Reservation valide ne doit avoir aucune violation")
        void validReservation_shouldHaveNoViolations() {
            Reservation res = createValidBaseReservationForValidation();
            Set<ConstraintViolation<Reservation>> violations = validator.validate(res);
            assertTrue(violations.isEmpty(), "Une réservation valide ne devrait avoir aucune violation");
        }

        // --- Tests pour reservationUuid (@NotNull, @Size(36)) ---
        @Test
        @DisplayName("reservationUuid: null")
        void uuidIsNull() {
            Reservation res = createValidBaseReservationForValidation();
            res.setReservationUuid(null);
            assertSingleViolationWithExactMessage(validator.validate(res), "reservationUuid", "L'UUID de réservation est obligatoire.");
        }

        @Test
        @DisplayName("reservationUuid: trop court")
        void uuidTooShort() {
            Reservation res = createValidBaseReservationForValidation();
            res.setReservationUuid("invalid-uuid");
            assertSingleViolation(validator.validate(res), "reservationUuid", "doit faire 36 caractères");
        }

        @Test
        @DisplayName("reservationUuid: trop long")
        void uuidTooLong() {
            Reservation res = createValidBaseReservationForValidation();
            res.setReservationUuid(UUID.randomUUID().toString() + "extra");
            assertSingleViolation(validator.validate(res), "reservationUuid", "doit faire 36 caractères");
        }

        @Test
        @DisplayName("reservationUuid: bonne longueur")
        void uuidCorrectLength() {
            Reservation res = createValidBaseReservationForValidation();
            res.setReservationUuid(UUID.randomUUID().toString()); // 36 chars
            assertTrue(validator.validate(res).isEmpty(), "UUID valide ne doit pas causer de violation");
        }


        // --- Tests pour les relations @NotNull ---
        @Test
        @DisplayName("membre: null")
        void membreIsNull() {
            Reservation res = createValidBaseReservationForValidation();
            res.setMembre(null);
            assertSingleViolationWithExactMessage(validator.validate(res), "membre", "La réservation doit appartenir à un membre.");
        }

        @Test
        @DisplayName("event: null")
        void eventIsNull() {
            Reservation res = createValidBaseReservationForValidation();
            res.setEvent(null);
            assertSingleViolationWithExactMessage(validator.validate(res), "event", "La réservation doit être liée à un événement.");
        }

        @Test
        @DisplayName("categorie: null")
        void categorieIsNull() {
            Reservation res = createValidBaseReservationForValidation();
            res.setCategorie(null);
            assertSingleViolationWithExactMessage(validator.validate(res), "categorie", "La réservation doit concerner une catégorie.");
        }

        // --- Tests pour dateReservation (@NotNull, @PastOrPresent) ---
        @Test
        @DisplayName("dateReservation: null")
        void dateIsNull() {
            Reservation res = createValidBaseReservationForValidation();
            res.setDateReservation(null);
            // Message exact non standardisé pour @NotNull
            assertSingleViolation(validator.validate(res), "dateReservation", null);
        }

        @Test
        @DisplayName("dateReservation: futur")
        void dateIsFuture() {
            Reservation res = createValidBaseReservationForValidation();
            res.setDateReservation(LocalDateTime.now().plusDays(1));
            assertSingleViolationWithExactMessage(validator.validate(res), "dateReservation", "La date de réservation doit être dans le passé ou aujourd'hui.");
        }

        @Test
        @DisplayName("dateReservation: présent (valide)")
        void dateIsPresent() {
            Reservation res = createValidBaseReservationForValidation();
            res.setDateReservation(LocalDateTime.now());
            assertTrue(validator.validate(res).isEmpty(), "Date réservation présente doit être valide");
        }

    } // Fin @Nested ValidationTests

    @Nested
    @DisplayName("Tests du Constructeur et Initialisation")
    class ConstructorAndInitTests {
        private Membre validMembre;
        private Event validEvent;
        private Event otherEvent;
        private Categorie validCategorie;
        private Categorie categorieForOtherEvent;
        private LocalDateTime beforeConstruction;

        @BeforeEach
        void setUpEntities() {
            validMembre = createMockMembre(1, "Alice", "Test");
            validEvent = createMockEvent(10);
            otherEvent = createMockEvent(20); // Un autre événement
            validCategorie = createMockCategorie(100, validEvent); // Catégorie liée à validEvent
            categorieForOtherEvent = createMockCategorie(200, otherEvent); // Catégorie liée à otherEvent
            beforeConstruction = LocalDateTime.now();
        }

        @Test
        @DisplayName("Constructeur valide initialise tous les champs")
        void validConstructor_shouldInitializeFields() {
            // Act
            Reservation res = new Reservation(validMembre, validEvent, validCategorie);
            LocalDateTime afterConstruction = LocalDateTime.now();

            // Assert
            assertSame(validMembre, res.getMembre());
            assertSame(validEvent, res.getEvent());
            assertSame(validCategorie, res.getCategorie());

            // UUID généré
            assertNotNull(res.getReservationUuid());
            assertEquals(36, res.getReservationUuid().length()); // Vérifie la taille
            try {
                UUID.fromString(res.getReservationUuid()); // Vérifie que c'est un UUID valide
            } catch (IllegalArgumentException e) {
                fail("reservationUuid n'est pas un UUID valide: " + res.getReservationUuid());
            }

            // Date de réservation
            assertNotNull(res.getDateReservation());
            assertTrue(!res.getDateReservation().isBefore(beforeConstruction) && !res.getDateReservation().isAfter(afterConstruction),
                    "DateReservation devrait être proche du moment de la construction");

            // QR code data non généré initialement
            // L'accès direct au champ n'est pas possible, on ne teste pas l'état interne 'null' ici.
        }

        // --- Tests des validations dans le constructeur ---
        @Test
        @DisplayName("Constructeur: membre null lance Exception")
        void constructor_withNullMembre_shouldThrowException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Reservation(null, validEvent, validCategorie));
            assertEquals("Membre, Event et Categorie sont requis pour une réservation.", ex.getMessage());
        }

        @Test
        @DisplayName("Constructeur: event null lance Exception")
        void constructor_withNullEvent_shouldThrowException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Reservation(validMembre, null, validCategorie));
            assertEquals("Membre, Event et Categorie sont requis pour une réservation.", ex.getMessage());
        }

        @Test
        @DisplayName("Constructeur: categorie null lance Exception")
        void constructor_withNullCategorie_shouldThrowException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Reservation(validMembre, validEvent, null));
            assertEquals("Membre, Event et Categorie sont requis pour une réservation.", ex.getMessage());
        }

        // --- Test de la validation de cohérence Categorie/Event ---
        @Test
        @DisplayName("Constructeur: catégorie n'appartenant pas à l'event lance Exception")
        void constructor_withMismatchedCategorieEvent_shouldThrowException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Reservation(validMembre, validEvent, categorieForOtherEvent)); // Utilise la catégorie de l'autre event
            assertTrue(ex.getMessage().contains("n'appartient pas à l'événement"));
            assertTrue(ex.getMessage().contains("catégorie ID " + categorieForOtherEvent.getId()));
            assertTrue(ex.getMessage().contains("événement ID " + validEvent.getId()));
        }

    } // Fin @Nested ConstructorAndInitTests

    @Nested
    @DisplayName("Tests de la Génération QR Code")
    class QrCodeGenerationTests {

        private Membre membre;
        private Event event;
        private Categorie categorie;
        private Reservation reservation;
        private LocalDateTime testDate;

        @BeforeEach
        void setUpReservation() {
            membre = createMockMembre(123, "Prénom Test", "Nom Test");
            event = createMockEvent(456);
            categorie = createMockCategorie(789, event);
            reservation = new Reservation(membre, event, categorie);

            // Fixer la date pour la rendre prédictible dans le test
            testDate = LocalDateTime.of(2025, 4, 17, 14, 30, 0);
            reservation.setDateReservation(testDate);
        }

        @Test
        @DisplayName("getQrcodeData: génère la chaîne correcte la première fois")
        void getQrcodeData_generatesCorrectString_onFirstCall() throws Exception { // Ajout throws pour URLEncoder
            // Arrange
            String expectedUuid = reservation.getReservationUuid();
            String nomComplet = "Prénom Test Nom Test";
            String dateFormatted = testDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String encodedNom = URLEncoder.encode(nomComplet, StandardCharsets.UTF_8.toString());
            String encodedDate = URLEncoder.encode(dateFormatted, StandardCharsets.UTF_8.toString());

            String expectedData = String.format("uuid:%s|evt:%d|cat:%d|user:%d|date:%s|name:%s",
                    expectedUuid, 456, 789, 123, encodedDate, encodedNom);

            // Act
            String actualData = reservation.getQrcodeData();

            // Assert
            assertEquals(expectedData, actualData);
        }

        @Test
        @DisplayName("getQrcodeData: retourne la même chaîne lors d'appels suivants")
        void getQrcodeData_returnsSameString_onSubsequentCalls() {
            // Act
            String firstCallData = reservation.getQrcodeData();
            String secondCallData = reservation.getQrcodeData();
            String thirdCallData = reservation.getQrcodeData();

            // Assert
            assertSame(firstCallData, secondCallData, "Doit retourner la même instance de String (génération paresseuse)");
            assertSame(secondCallData, thirdCallData, "Doit retourner la même instance de String");
            assertEquals(firstCallData, thirdCallData); // Vérifie l'égalité aussi par sécurité
        }

        // --- Tests des cas d'erreur de génération ---
        @Test
        @DisplayName("getQrcodeData: données manquantes (ex: membre null)")
        void getQrcodeData_missingData_returnsErrorString() {
            // Arrange
            Reservation res = new Reservation(membre, event, categorie); // Constructeur OK
            res.setMembre(null); // Rend la réservation invalide pour la génération
            // Act
            String data = res.getQrcodeData();
            // Assert
            assertEquals("error:missing-data", data);
            // Pourrait aussi vérifier les logs System.err si nécessaire
        }

        @Test
        @DisplayName("getQrcodeData: ID manquant (ex: event ID null)")
        void getQrcodeData_missingId_returnsErrorString() {
            // Arrange
            // 1. Crée les objets avec des IDs VALIDES pour que le constructeur passe
            Membre membreValide = createMockMembre(123, "Prenom", "Nom"); // Assure-toi d'avoir membre défini ou utilise create...
            Event eventAvecId = createMockEvent(456); // ID valide initialement
            Categorie catPourEventAvecId = createMockCategorie(789, eventAvecId);

            // 2. Le constructeur doit maintenant réussir
            Reservation res = new Reservation(membreValide, eventAvecId, catPourEventAvecId);

            // 3. APRÈS la construction, modifie le mock pour qu'il retourne null pour getId()
            when(eventAvecId.getId()).thenReturn(null); // Re-stubbing du mock

            // Act: Appelle la méthode à tester
            String data = res.getQrcodeData();

            // Assert
            assertEquals("error:missing-ids", data);
        }


        // Note: Le cas 'error:generation-failed' est difficile à tester unitairement
        // car il faudrait simuler une exception lors de l'encodage URL, ce qui est rare.

    } // Fin @Nested QrCodeGenerationTests

    @Nested
    @DisplayName("Tests pour equals() et hashCode() (basés sur reservationUuid)")
    class EqualsHashCodeTests {

        private String uuid1;
        private String uuid2;

        @BeforeEach
        void setUpUuids() {
            uuid1 = UUID.randomUUID().toString();
            uuid2 = UUID.randomUUID().toString();
        }

        // Helper pour créer une réservation avec un UUID spécifique
        private Reservation createReservationWithUuid(String uuid, Membre m, Event e, Categorie c) {
            // Utilise le constructeur normal (qui génère un UUID), puis l'écrase.
            Reservation res = new Reservation(m, e, c);
            res.setReservationUuid(uuid);
            return res;
        }

        @Test
        @DisplayName("equals: même reservationUuid => true")
        void equals_sameUuid_shouldBeTrue() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            // Créer une deuxième instance avec le même UUID mais autres champs potentiellement différents
            Membre m2 = createMockMembre(2, "C", "D");
            Event ev2 = createMockEvent(2);
            Categorie cat2 = createMockCategorie(2, ev2);
            Reservation r2 = createReservationWithUuid(uuid1, m2, ev2, cat2); // Utilise uuid1

            assertEquals(r1, r2);
            assertTrue(r1.equals(r2) && r2.equals(r1));
        }

        @Test
        @DisplayName("equals: reservationUuid différent => false")
        void equals_differentUuid_shouldBeFalse() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            Reservation r2 = createReservationWithUuid(uuid2, m, ev, cat); // Utilise uuid2
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("equals: un reservationUuid null => false")
        void equals_oneUuidNull_shouldBeFalse() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            Reservation r2 = createReservationWithUuid(null, m, ev, cat); // UUID null
            assertNotEquals(r1, r2);
            assertNotEquals(r2, r1);
        }

        @Test
        @DisplayName("equals: deux reservationUuid null => true (cas limite)")
        void equals_bothUuidNull_shouldBeTrue() {
            // C'est le comportement de Objects.equals(null, null)
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(null, m, ev, cat);
            Reservation r2 = createReservationWithUuid(null, m, ev, cat);
            assertEquals(r1, r2);
        }

        @Test
        @DisplayName("equals: comparaison avec null => false")
        void equals_compareWithNull_shouldBeFalse() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            assertFalse(r1.equals(null));
        }

        @Test
        @DisplayName("equals: classe différente => false")
        void equals_compareWithDifferentClass_shouldBeFalse() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            Object autre = new Object();
            assertFalse(r1.equals(autre));
        }

        @Test
        @DisplayName("equals: même objet => true")
        void equals_sameObject_shouldBeTrue() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            assertEquals(r1, r1);
        }

        // --- HashCode Tests ---
        @Test
        @DisplayName("hashCode: même reservationUuid => même hashCode")
        void hashCode_sameUuid_shouldBeEqual() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            Reservation r2 = createReservationWithUuid(uuid1, m, ev, cat); // Même UUID
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("hashCode: reservationUuid différent => hashCode différent (probable)")
        void hashCode_differentUuid_shouldBeDifferent() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            Reservation r2 = createReservationWithUuid(uuid2, m, ev, cat); // UUID différent
            assertNotEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("hashCode: reservationUuid null")
        void hashCode_nullUuid() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r_null = createReservationWithUuid(null, m, ev, cat); // reservationUuid est null

            // Le hashCode calculé par Objects.hash(null) est 31.
            assertEquals(31, r_null.hashCode()); // <- Changer 0 par 31

            // Test aussi que ça ne lève pas d'exception (cette partie est toujours bonne)
            assertDoesNotThrow(() -> r_null.hashCode());
        }


        @Test
        @DisplayName("hashCode: consistance")
        void hashCode_consistent() {
            Membre m = createMockMembre(1, "A", "B");
            Event ev = createMockEvent(1);
            Categorie cat = createMockCategorie(1, ev);
            Reservation r1 = createReservationWithUuid(uuid1, m, ev, cat);
            int initialHashCode = r1.hashCode();
            // Modifie champs non utilisés dans hashCode
            r1.setMembre(createMockMembre(99, "X", "Y"));
            r1.setDateReservation(LocalDateTime.now().plusYears(1));
            r1.setId(12345);
            assertEquals(initialHashCode, r1.hashCode());
        }

    } // Fin @Nested EqualsHashCodeTests
}

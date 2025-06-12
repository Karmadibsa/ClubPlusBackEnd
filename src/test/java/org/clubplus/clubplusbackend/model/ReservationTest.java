package org.clubplus.clubplusbackend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pas besoin d'annotations Spring, c'est un test unitaire pur.
class ReservationTest {

    // --- Données de test ---
    // On crée des objets simples pour nos tests. Pas besoin qu'ils soient persistés.
    private Membre membreValide;
    private Event eventActif;
    private Event eventInactif;
    private Categorie categorieValide;
    private Categorie autreCategorie;

    @BeforeEach
    void setUp() {
        // Arrange : Préparation des objets nécessaires pour chaque test.

        membreValide = new Membre();
        membreValide.setId(1);
        membreValide.setNom("Testeur");

        eventActif = new Event();
        eventActif.setId(10);
        eventActif.setNom("Événement Actif");
        eventActif.setActif(true);

        eventInactif = new Event();
        eventInactif.setId(11);
        eventInactif.setNom("Événement Inactif");
        eventInactif.setActif(false);

        categorieValide = new Categorie();
        categorieValide.setId(100);
        categorieValide.setNom("Catégorie Standard");
        categorieValide.setEvent(eventActif); // La catégorie appartient à l'événement actif.

        autreCategorie = new Categorie();
        autreCategorie.setId(101);
        autreCategorie.setNom("Catégorie d'un autre événement");
        autreCategorie.setEvent(eventInactif); // Cette catégorie appartient à un autre événement.
    }

    // --- Tests du Constructeur Métier ---
    // C'est la partie la plus importante à tester pour cette classe. [1]

    @Test
    @DisplayName("Le constructeur doit initialiser correctement les champs quand les données sont valides")
    void constructor_ShouldInitializeFieldsCorrectly_WhenDataIsValid() {
        // Act : On appelle le constructeur métier.
        Reservation reservation = new Reservation(membreValide, eventActif, categorieValide);

        // Assert : On vérifie que tout est correctement initialisé.
        assertThat(reservation.getMembre()).isEqualTo(membreValide);
        assertThat(reservation.getEvent()).isEqualTo(eventActif);
        assertThat(reservation.getCategorie()).isEqualTo(categorieValide);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRME);
        assertThat(reservation.getReservationUuid()).isNotNull().hasSize(36); // Vérifie que l'UUID est généré.
        assertThat(reservation.getDateReservation()).isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Le constructeur doit lever une IllegalStateException si l'événement n'est pas actif")
    void constructor_ShouldThrowIllegalStateException_WhenEventIsNotActive() {
        // Act & Assert : On vérifie que l'exception correcte est levée avec le bon message.
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new Reservation(membreValide, eventInactif, autreCategorie);
        });

        assertThat(exception.getMessage()).contains("Impossible de créer une réservation pour un événement non actif");
    }

    @Test
    @DisplayName("Le constructeur doit lever une IllegalArgumentException si la catégorie n'appartient pas à l'événement")
    void constructor_ShouldThrowIllegalArgumentException_WhenCategorieDoesNotBelongToEvent() {
        // Act & Assert : On tente de créer une réservation pour eventActif avec une catégorie qui appartient à eventInactif.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new Reservation(membreValide, eventActif, autreCategorie);
        });

        assertThat(exception.getMessage()).contains("Incohérence : La catégorie ID", "n'appartient pas à l'événement ID");
    }

    @Test
    @DisplayName("Le constructeur doit lever une NullPointerException si le membre est null")
    void constructor_ShouldThrowNullPointerException_WhenMembreIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new Reservation(null, eventActif, categorieValide);
        });
    }

    // --- Tests de equals() et hashCode() ---

    @Test
    @DisplayName("equals() et hashCode() doivent être basés uniquement sur reservationUuid")
    void equalsAndHashCode_ShouldBeBasedOnUuidOnly() {
        // Arrange
        String sharedUuid = UUID.randomUUID().toString();

        Reservation r1 = new Reservation();
        r1.setId(1);
        r1.setReservationUuid(sharedUuid);
        r1.setStatus(ReservationStatus.CONFIRME);

        Reservation r2 = new Reservation();
        r2.setId(2); // ID différent
        r2.setReservationUuid(sharedUuid); // Mais même UUID
        r2.setStatus(ReservationStatus.UTILISE); // Et statut différent

        // Assert
        assertThat(r1).isEqualTo(r2); // Doivent être considérées comme égales.
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode()); // Leurs hashcodes doivent être identiques.
    }

    @Test
    @DisplayName("equals() doit retourner false pour des UUIDs différents")
    void equals_ShouldReturnFalse_ForDifferentUuids() {
        // Arrange
        Reservation r1 = new Reservation();
        r1.setReservationUuid(UUID.randomUUID().toString());

        Reservation r2 = new Reservation();
        r2.setReservationUuid(UUID.randomUUID().toString());

        // Assert
        assertThat(r1).isNotEqualTo(r2);
    }

    // --- Tests de la logique du QR Code ---

    @Test
    @DisplayName("getQrcodeData doit retourner la donnée formatée correctement")
    void getQrcodeData_ShouldReturnCorrectlyFormattedData() {
        // Arrange
        Reservation reservation = new Reservation(membreValide, eventActif, categorieValide);
        String expectedData = "uuid:" + reservation.getReservationUuid();

        // Act
        String qrCodeData = reservation.getQrcodeData();

        // Assert
        assertThat(qrCodeData).isEqualTo(expectedData);
    }

    @Test
    @DisplayName("getQrcodeData doit mettre en cache la valeur générée")
    void getQrcodeData_ShouldBeLazyAndCached() {
        // Arrange
        Reservation reservation = new Reservation(membreValide, eventActif, categorieValide);

        // Act
        String firstCall = reservation.getQrcodeData(); // Génère et stocke la valeur.

        // On modifie manuellement le champ pour simuler un état "corrompu".
        reservation.setQrcodeData("valeur-modifiee");

        String secondCall = reservation.getQrcodeData(); // Doit retourner la valeur en cache.

        // Assert
        assertThat(secondCall).isEqualTo("valeur-modifiee")
                .as("La deuxième appel doit retourner la valeur en cache et non la régénérer.");
        assertThat(secondCall).isNotEqualTo(firstCall);
    }

    @Test
    @DisplayName("generateQrCodeDataString doit gérer le cas où l'UUID est null")
    void generateQrCodeDataString_ShouldHandleNullUuid() {
        // Arrange
        // On utilise le constructeur sans argument pour créer un objet "invalide" pour ce test.
        Reservation reservationSansUuid = new Reservation();
        reservationSansUuid.setReservationUuid(null);

        // Act
        String qrCodeData = reservationSansUuid.getQrcodeData();

        // Assert
        assertThat(qrCodeData).isEqualTo("error:uuid-missing");
    }
}


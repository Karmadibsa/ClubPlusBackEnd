package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor // JPA requis
@AllArgsConstructor // Pratique
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class) // OK
    private Integer id;

    // UUID unique pour identifier la réservation (pour QR code, API externe, etc.)
    @NotNull(message = "L'UUID de réservation est obligatoire.")
    @Size(min = 36, max = 36, message = "L'UUID doit faire 36 caractères.") // Validation taille UUID
    @Column(unique = true, nullable = false, updatable = false, length = 36) // Parfait: unique, non null/modifiable
    @JsonView(GlobalView.ReservationView.class) // Visible dans la vue détaillée
    private String reservationUuid;

    // Relation vers le Membre qui réserve
    @NotNull(message = "La réservation doit appartenir à un membre.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // LAZY, non optionnel
    @JoinColumn(name = "membre_id", nullable = false) // FK non nulle
    // JsonView: Base est un bon choix ici pour ne pas surcharger.
    @JsonView({GlobalView.Base.class, GlobalView.ReservationView.class})
    private Membre membre;

    // Relation vers l'Événement concerné
    // Bien que redondant car accessible via Categorie, cela peut simplifier certaines requêtes
    // ou affichages spécifiques à la réservation. C'est un choix de modélisation acceptable.
    @NotNull(message = "La réservation doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // LAZY, non optionnel
    @JoinColumn(name = "event_id", nullable = false) // FK non nulle
    // JsonView: Base est un bon choix.
    @JsonView({GlobalView.Base.class, GlobalView.ReservationView.class})
    private Event event;

    // Relation vers la Catégorie spécifique réservée
    @NotNull(message = "La réservation doit concerner une catégorie.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // LAZY, non optionnel
    @JoinColumn(name = "categorie_id", nullable = false) // FK non nulle
    // JsonView: Base est un bon choix.
    @JsonView({GlobalView.Base.class, GlobalView.ReservationView.class})
    private Categorie categorie;

    // Timestamp de la réservation
    @NotNull(message = "La date de réservation est obligatoire.")
    @PastOrPresent(message = "La date de réservation doit être dans le passé ou aujourd'hui.") // Validation ajoutée
    @Column(nullable = false, updatable = false) // Parfait: non null, non modifiable
    @JsonView(GlobalView.ReservationView.class) // Date pertinente dans vue détaillée
    private LocalDateTime dateReservation;

    // --- QR Code ---
    // Donnée calculée, non persistée
    @Transient
    @JsonView(GlobalView.ReservationView.class) // Inclure dans la vue détaillée
    private String qrcodeData;

    // Constructeur principal: EXCELLENT
    public Reservation(Membre membre, Event event, Categorie categorie) {
        // Validation des arguments
        if (membre == null || event == null || categorie == null) {
            throw new IllegalArgumentException("Membre, Event et Categorie sont requis pour une réservation.");
        }
        // Validation de cohérence: la catégorie appartient bien à l'événement ! Très important.
        if (!categorie.getEvent().getId().equals(event.getId())) {
            throw new IllegalArgumentException("La catégorie ID " + categorie.getId() + " n'appartient pas à l'événement ID " + event.getId());
        }

        // Initialisation des champs
        this.membre = membre;
        this.event = event;
        this.categorie = categorie;
        this.dateReservation = LocalDateTime.now();
        this.reservationUuid = UUID.randomUUID().toString(); // Génération UUID au moment de la création objet
    }

    // Getter pour QR Code (avec génération paresseuse): TRÈS BIEN
    public String getQrcodeData() {
        // Génère la donnée seulement si elle n'a pas déjà été calculée
        if (this.qrcodeData == null) {
            this.qrcodeData = generateQrCodeDataString();
        }
        return this.qrcodeData;
    }

    // Génération de la chaîne pour QR Code: Logique semble correcte et robuste
    private String generateQrCodeDataString() {
        try {
            // Vérifications robustes des données nécessaires
            if (reservationUuid == null || event == null || membre == null || categorie == null || dateReservation == null) {
                System.err.println("WARN: Données manquantes pour génération QR code (Réservation UUID: " + reservationUuid + ")");
                return "error:missing-data"; // Format simple pour l'erreur
            }
            Integer eventId = event.getId();
            Integer membreId = membre.getId();
            Integer categorieId = categorie.getId();
            if (eventId == null || membreId == null || categorieId == null) {
                System.err.println("WARN: ID(s) manquant(s) pour génération QR code (Réservation UUID: " + reservationUuid + ")");
                return "error:missing-ids";
            }
            String nomComplet = membre.getPrenom() + " " + membre.getNom(); // Espace plus lisible que _ ?
            String dateFormatted = dateReservation.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Encodage URL pour nom et date (bonne précaution)
            String encodedNom = URLEncoder.encode(nomComplet, StandardCharsets.UTF_8.toString());
            String encodedDate = URLEncoder.encode(dateFormatted, StandardCharsets.UTF_8.toString());

            // Format de la chaîne de données (personnalisable)
            return String.format("uuid:%s|evt:%d|cat:%d|user:%d|date:%s|name:%s",
                    this.reservationUuid, eventId, categorieId, membreId, encodedDate, encodedNom);

        } catch (Exception e) {
            System.err.println("ERREUR génération QR code pour Réservation UUID " + reservationUuid + ": " + e.getMessage());
            // Logger.error(...) serait mieux ici qu'un System.err
            return "error:generation-failed";
        }
    }

    // equals() et hashCode() basés sur reservationUuid: EXCELLENT CHOIX
    // Stable, unique et disponible avant la persistance.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        // Utiliser reservationUuid qui est défini à la création de l'objet
        return Objects.equals(reservationUuid, that.reservationUuid);
    }

    @Override
    public int hashCode() {
        // Cohérent avec equals
        return Objects.hash(reservationUuid);
    }
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entité JPA représentant une réservation effectuée par un {@link Membre} pour une {@link Categorie} d'un {@link Event}.
 * <p>
 * Chaque réservation est identifiée de manière unique par un UUID, idéal pour un usage externe comme les QR codes.
 * L'égalité des objets est basée sur cet UUID, assurant une identification stable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations")
public class Reservation {

    private static final Logger log = LoggerFactory.getLogger(Reservation.class);

    /**
     * Identifiant unique interne de la réservation, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Identifiant Universellement Unique (UUID) de la réservation.
     * Utilisé pour les références externes (ex: QR Code) de manière stable et sécurisée.
     */
    @NotNull(message = "L'UUID de réservation est obligatoire.")
    @Size(min = 36, max = 36, message = "L'UUID doit faire 36 caractères.")
    @Column(unique = true, nullable = false, updatable = false, length = 36)
    @JsonView(GlobalView.ReservationView.class)
    private String reservationUuid;

    /**
     * Le membre qui a effectué la réservation.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     */
    @NotNull(message = "La réservation doit appartenir à un membre.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_membre"))
    @JsonView(GlobalView.ReservationView.class)
    private Membre membre;

    /**
     * L'événement concerné par la réservation.
     * Relation directe pour simplifier les requêtes.
     */
    @NotNull(message = "La réservation doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_event"))
    @JsonView({GlobalView.MembreView.class, GlobalView.ReservationView.class})
    private Event event;

    /**
     * La catégorie spécifique réservée pour cet événement.
     */
    @NotNull(message = "La réservation doit concerner une catégorie.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_categorie"))
    @JsonView(GlobalView.ReservationView.class)
    private Categorie categorie;

    /**
     * Date et heure auxquelles la réservation a été effectuée.
     * Non modifiable après création.
     */
    @NotNull(message = "La date de réservation est obligatoire.")
    @PastOrPresent(message = "La date de réservation doit être dans le passé ou aujourd'hui.")
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.ReservationView.class)
    private Instant dateReservation;

    /**
     * Statut actuel de la réservation (ex: CONFIRME, UTILISE, ANNULE).
     */
    @NotNull(message = "Le statut de la réservation est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @JsonView(GlobalView.Base.class)
    private ReservationStatus status;

    /**
     * Données formatées pour le QR Code, générées à la demande.
     * Ce champ n'est pas persisté en base de données.
     */
    @Transient
    @JsonView(GlobalView.ReservationView.class)
    private String qrcodeData;

    /**
     * Constructeur principal pour créer une nouvelle réservation.
     * Valide les entrées, vérifie la cohérence entre la catégorie et l'événement,
     * et initialise les valeurs par défaut (date, UUID, statut).
     *
     * @param membre    Le membre qui réserve.
     * @param event     L'événement concerné.
     * @param categorie La catégorie réservée.
     * @throws IllegalArgumentException si la catégorie n'appartient pas à l'événement.
     * @throws IllegalStateException    si l'événement n'est pas actif.
     */
    public Reservation(Membre membre, Event event, Categorie categorie) {
        this.membre = Objects.requireNonNull(membre, "Le Membre ne peut pas être null.");
        this.event = Objects.requireNonNull(event, "L'Event ne peut pas être null.");
        this.categorie = Objects.requireNonNull(categorie, "La Categorie ne peut pas être null.");

        if (categorie.getEvent() == null || !Objects.equals(categorie.getEvent().getId(), event.getId())) {
            throw new IllegalArgumentException(
                    String.format("Incohérence : La catégorie ID %d n'appartient pas à l'événement ID %d.",
                            categorie.getId(), event.getId())
            );
        }

        if (!event.getActif()) {
            throw new IllegalStateException(
                    String.format("Impossible de réserver pour un événement non actif (ID: %d).", event.getId())
            );
        }

        this.dateReservation = Instant.now();
        this.reservationUuid = UUID.randomUUID().toString();
        this.status = ReservationStatus.CONFIRME;
    }

    /**
     * Retourne la chaîne de données pour le QR Code (génération paresseuse).
     *
     * @return Les données formatées pour le QR Code.
     */
    public String getQrcodeData() {
        if (this.qrcodeData == null) {
            this.qrcodeData = generateQrCodeDataString();
        }
        return this.qrcodeData;
    }

    /**
     * Génère la chaîne de caractères brute pour le QR Code.
     *
     * @return L'UUID de la réservation, préfixé.
     */
    private String generateQrCodeDataString() {
        if (this.reservationUuid == null) {
            log.warn("Tentative de génération de données QR code alors que reservationUuid est null.");
            return "error:uuid-missing";
        }
        return "uuid:" + this.reservationUuid;
    }

    /**
     * L'égalité est basée sur l'UUID de la réservation, garantissant une unicité stable.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation that)) return false;
        return Objects.equals(reservationUuid, that.reservationUuid);
    }

    /**
     * Le hashcode est basé sur l'UUID, en cohérence avec la méthode equals.
     */
    @Override
    public int hashCode() {
        return Objects.hash(reservationUuid);
    }

    /**
     * Représentation textuelle de la réservation pour le débogage.
     */
    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", uuid='" + reservationUuid + '\'' +
                ", membreId=" + (membre != null ? membre.getId() : "null") +
                ", eventId=" + (event != null ? event.getId() : "null") +
                ", status=" + status +
                '}';
    }
}

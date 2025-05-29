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
 * Entité JPA représentant une réservation effectuée par un {@link Membre} pour une
 * {@link Categorie} spécifique d'un {@link Event}.
 *
 * <p>Chaque réservation possède un identifiant unique interne ({@code id}) et un UUID
 * universellement unique ({@code reservationUuid}) généré lors de la création, utile
 * pour les références externes (ex: QR codes, API). Elle enregistre également la date
 * de réservation et son statut actuel (ex: CONFIRME, UTILISE, ANNULE).</p>
 *
 * <p>Des relations obligatoires et chargées paresseusement la lient au membre, à l'événement
 * et à la catégorie concernés.</p>
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont basés sur le champ
 * {@code reservationUuid}, ce qui garantit une identification unique et stable même
 * avant la persistance de l'entité.</p>
 *
 * @see Membre
 * @see Event
 * @see Categorie
 * @see ReservationStatus
 * @see GlobalView
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
@NoArgsConstructor // Lombok: Constructeur sans argument (requis par JPA).
@AllArgsConstructor // Lombok: Constructeur avec tous les arguments (pratique pour tests/fixtures).
@Entity // Marque comme entité JPA.
@Table(name = "reservations") // Nom explicite de la table.
public class Reservation {

    // Logger pour tracer les avertissements/erreurs, notamment dans la génération QR code.
    private static final Logger log = LoggerFactory.getLogger(Reservation.class);

    /**
     * Identifiant unique interne de la réservation, généré automatiquement par la BDD.
     * Visible dans la vue JSON de base.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Identifiant Universellement Unique (UUID) de la réservation.
     * Généré lors de la création de l'objet (via le constructeur).
     * Il est unique, non modifiable et obligatoire. Utilisé pour identifier
     * la réservation de manière externe et stable (ex: dans un QR Code).
     * Visible dans la vue détaillée de la réservation.
     */
    @NotNull(message = "L'UUID de réservation est obligatoire.")
    @Size(min = 36, max = 36, message = "L'UUID doit faire 36 caractères.")
    // UUID format standard xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    @Column(unique = true, nullable = false, updatable = false, length = 36) // Contraintes BDD fortes.
    @JsonView(GlobalView.ReservationView.class)
    private String reservationUuid;

    /**
     * Le membre qui a effectué la réservation.
     * Relation Many-to-One obligatoire, chargée paresseusement.
     * Visible dans la vue détaillée de la réservation.
     */
    @NotNull(message = "La réservation doit appartenir à un membre.")
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "membre_id",  // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_reservation_membre")) // Nom explicite de la contrainte FK.
    @JsonView(GlobalView.ReservationView.class) // Vue détaillée inclut le membre.
    private Membre membre;

    /**
     * L'événement concerné par la réservation.
     * Bien que l'événement soit accessible via la catégorie, cette relation directe
     * peut simplifier les requêtes et l'affichage.
     * Relation Many-to-One obligatoire, chargée paresseusement.
     * Visible dans la vue Membre (liste des réservations) et la vue Réservation détaillée.
     */
    @NotNull(message = "La réservation doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "event_id",   // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_reservation_event")) // Nom explicite de la contrainte FK.
    @JsonView({GlobalView.MembreView.class, GlobalView.ReservationView.class})
    // Vue Base de l'Event est probablement suffisante.
    private Event event;

    /**
     * La catégorie spécifique de l'événement pour laquelle cette réservation est faite.
     * Relation Many-to-One obligatoire, chargée paresseusement.
     * Visible dans la vue détaillée de la réservation.
     */
    @NotNull(message = "La réservation doit concerner une catégorie.")
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "categorie_id", // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_reservation_categorie")) // Nom explicite de la contrainte FK.
    @JsonView(GlobalView.ReservationView.class) // Vue Base de la Catégorie est probablement suffisante.
    private Categorie categorie;

    /**
     * Date et heure exactes auxquelles la réservation a été effectuée.
     * Doit être non nulle et dans le passé ou le présent. Non modifiable après création.
     * Initialisée lors de la création de l'objet (via le constructeur).
     * Visible dans la vue détaillée de la réservation.
     */
    @NotNull(message = "La date de réservation est obligatoire.")
    @PastOrPresent(message = "La date de réservation doit être dans le passé ou aujourd'hui.") // Validation logique.
    @Column(nullable = false, updatable = false) // Contrainte BDD: non null, non modifiable.
    @JsonView(GlobalView.ReservationView.class)
    private Instant dateReservation;

    /**
     * Statut actuel de la réservation (ex: CONFIRME, UTILISE, ANNULE).
     * Représenté par l'énumération {@link ReservationStatus}.
     * Le statut est obligatoire et stocké comme une chaîne en BDD.
     * Initialisé à {@code ReservationStatus.CONFIRME} par défaut (via le constructeur).
     * Visible dans la vue JSON de base.
     *
     * @see ReservationStatus
     */
    @NotNull(message = "Le statut de la réservation est obligatoire.")
    @Enumerated(EnumType.STRING) // Stocke le nom de l'enum (ex: "CONFIRME") en BDD.
    @Column(nullable = false, length = 20) // Taille pour les noms de statut.
    @JsonView(GlobalView.Base.class) // Visible dans la vue de base.
    private ReservationStatus status;

    // --- Donnée pour QR Code (Calculée, Non Persistée) ---

    /**
     * Chaîne de caractères formatée contenant les informations essentielles de la réservation,
     * destinée à être encodée dans un QR Code pour une identification/validation rapide.
     * Cette donnée est générée à la demande via le getter {@link #getQrcodeData()} et n'est pas
     * stockée en base de données.
     * Visible dans la vue détaillée de la réservation.
     *
     * @see #getQrcodeData()
     * @see #generateQrCodeDataString()
     */
    @Transient // Non persisté en BDD.
    @JsonView(GlobalView.ReservationView.class)
    private String qrcodeData;

    /**
     * Constructeur métier principal pour créer une nouvelle réservation.
     * Valide les entrées (membre, événement, catégorie non nulls), vérifie que la catégorie
     * appartient bien à l'événement et que l'événement est actif.
     * Initialise la date de réservation à l'heure actuelle, génère un UUID unique,
     * et définit le statut initial à {@code CONFIRME}.
     *
     * @param membre    Le {@link Membre} effectuant la réservation. Ne doit pas être null.
     * @param event     L'{@link Event} concerné. Ne doit pas être null.
     * @param categorie La {@link Categorie} spécifique réservée. Ne doit pas être null.
     * @throws IllegalArgumentException si l'un des paramètres est null, ou si la catégorie
     *                                  n'appartient pas à l'événement spécifié.
     * @throws IllegalStateException    si l'événement spécifié n'est pas actif ({@code actif = false}).
     */
    public Reservation(Membre membre, Event event, Categorie categorie) {
        // Validation des arguments essentiels.
        this.membre = Objects.requireNonNull(membre, "Le Membre ne peut pas être null pour créer une réservation.");
        this.event = Objects.requireNonNull(event, "L'Event ne peut pas être null pour créer une réservation.");
        this.categorie = Objects.requireNonNull(categorie, "La Categorie ne peut pas être null pour créer une réservation.");

        // Vérification de cohérence : la catégorie doit appartenir à l'événement.
        // Suppose que Event et Categorie ont des IDs et que event est chargé ou a un ID accessible.
        if (categorie.getEvent() == null || !Objects.equals(categorie.getEvent().getId(), event.getId())) {
            throw new IllegalArgumentException(
                    String.format("Incohérence : La catégorie ID %d n'appartient pas à l'événement ID %d.",
                            categorie.getId(), event.getId())
            );
        }

        // Vérification métier : on ne peut réserver que pour un événement actif.
        if (!event.getActif()) { // Utilise le getter Lombok.
            throw new IllegalStateException(
                    String.format("Impossible de créer une réservation pour un événement non actif/annulé (ID: %d).",
                            event.getId())
            );
        }

        // Initialisation des champs par défaut.
        this.dateReservation = Instant.now();
        this.reservationUuid = UUID.randomUUID().toString(); // Génération de l'UUID unique.
        this.status = ReservationStatus.CONFIRME; // Statut par défaut.
    }

    /**
     * Retourne la chaîne de données formatée pour le QR Code, en la générant
     * uniquement lors du premier appel (génération paresseuse/lazy).
     * Les appels suivants retournent la valeur déjà calculée et stockée dans le champ {@code qrcodeData}.
     *
     * @return La chaîne de caractères contenant les données de réservation formatées pour un QR Code,
     * ou une chaîne d'erreur si la génération échoue.
     * @see #generateQrCodeDataString()
     */
    public String getQrcodeData() {
        // Génération paresseuse : calcule si pas déjà fait.
        if (this.qrcodeData == null) {
            this.qrcodeData = generateQrCodeDataString();
        }
        return this.qrcodeData;
    }

    /**
     * Génère la chaîne de caractères pour le QR Code, contenant uniquement l'UUID de la réservation.
     *
     * @return L'UUID de la réservation sous forme de chaîne, ou une chaîne d'erreur si l'UUID est manquant.
     */
    private String generateQrCodeDataString() {
        if (this.reservationUuid == null) {
            log.warn("Impossible de générer les données du QR code : reservationUuid est null.");
            return "error:uuid-missing";
        }
        // Préfixer l'UUID avec "uuid:"
        return "uuid:" + this.reservationUuid.toString();
    }

    // --- equals() et hashCode() ---

    /**
     * Détermine si cet objet Reservation est égal à un autre objet.
     * L'égalité est basée **uniquement** sur l'identifiant {@code reservationUuid}.
     * C'est une stratégie robuste car l'UUID est unique, non nul et défini dès la création
     * de l'objet, même avant sa persistance en base de données.
     *
     * @param o L'objet à comparer.
     * @return {@code true} si les objets sont des Réservations avec le même {@code reservationUuid}, {@code false} sinon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifie si 'o' est une instance de Reservation et non null.
        if (!(o instanceof Reservation that)) return false;
        // Compare les reservationUuid (qui devraient toujours être non nuls après construction).
        return Objects.equals(reservationUuid, that.reservationUuid);
    }

    /**
     * Retourne un code de hachage pour cet objet Reservation.
     * Le code de hachage est calculé **uniquement** à partir de l'identifiant {@code reservationUuid},
     * en cohérence avec la méthode {@code equals}.
     *
     * @return Le code de hachage basé sur {@code reservationUuid}.
     */
    @Override
    public int hashCode() {
        // Cohérent avec equals : utilise reservationUuid.
        return Objects.hash(reservationUuid);
    }

    // Optionnel: toString() pour le débogage
    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", reservationUuid='" + reservationUuid + '\'' +
                ", membreId=" + (membre != null ? membre.getId() : "null") +
                ", eventId=" + (event != null ? event.getId() : "null") +
                ", categorieId=" + (categorie != null ? categorie.getId() : "null") +
                ", dateReservation=" + dateReservation +
                ", status=" + status +
                '}';
    }
}

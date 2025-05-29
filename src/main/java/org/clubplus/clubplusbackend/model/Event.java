package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité JPA représentant un événement organisé par un {@link Club}.
 * Contient les informations descriptives de l'événement (nom, dates, description, lieu),
 * le statut (actif/désactivé), et les relations vers les {@link Categorie}s proposées,
 * le {@link Club} organisateur, et les {@link Notation}s associées.
 *
 * <p>La suppression logique est gérée via le champ {@code actif} et la date de désactivation.</p>
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont basés sur l'identifiant unique {@code id},
 * ce qui est standard pour les entités JPA.</p>
 *
 * @see Club
 * @see Categorie
 * @see Notation
 * @see GlobalView
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
@NoArgsConstructor // Lombok: Constructeur sans argument (requis par JPA).
@AllArgsConstructor // Lombok: Constructeur avec tous les arguments.
@Entity // Marque comme entité JPA.
@Table(name = "events") // Nom explicite de la table.
public class Event {

    /**
     * Identifiant unique de l'événement, généré automatiquement.
     * Visible dans la vue JSON de base.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom de l'événement.
     * Doit être non vide et respecter les contraintes de taille.
     * Visible dans la vue JSON de base.
     */
    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    @Column(nullable = false, length = 150)
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Date et heure de début de l'événement.
     * Doit être définie et être dans le présent ou le futur au moment de la validation.
     * Visible dans la vue JSON de base.
     */
    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    // Validation: date future ou présente.
    @Column(nullable = false) // Contrainte BDD: non null.
    @JsonView(GlobalView.Base.class)
    private Instant startTime;

    /**
     * Date et heure de fin de l'événement.
     * Doit être définie et être dans le présent ou le futur au moment de la validation.
     * La validation logique que {@code end} soit après {@code start} doit être effectuée au niveau service.
     * Visible dans la vue détaillée de l'événement.
     */
    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Instant endTime;

    /**
     * Description détaillée de l'événement.
     * Doit être non vide et respecter la limite de taille.
     * Visible dans la vue détaillée de l'événement.
     */
    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    @Column(nullable = false, length = 2000) // Type TEXT ou LONGTEXT en BDD est souvent utilisé ici.
    @JsonView(GlobalView.EventView.class)
    private String description;

    /**
     * Lieu ou adresse où se déroule l'événement.
     * Ce champ est optionnel mais limité en taille s'il est fourni.
     * Visible dans la vue JSON de base.
     */
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    @Column(length = 255)
    @JsonView(GlobalView.Base.class)
    private String location;

    // --- Champs pour Soft Delete ---

    /**
     * Indicateur du statut actif de l'événement.
     * {@code true} si l'événement est actif/prévu, {@code false} s'il est annulé/désactivé.
     * Ce champ est essentiel pour la stratégie de suppression logique.
     * Visible dans la vue JSON de base.
     */
    @NotNull // Important pour la cohérence (et potentiellement pour @Where si utilisé).
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Boolean actif = true; // Initialisé à 'true' par défaut pour les nouveaux événements.

    /**
     * Date et heure auxquelles l'événement a été marqué comme désactivé/annulé.
     * Reste {@code null} tant que l'événement est actif.
     */
    @Column(name = "desactivation_date") // Nom de colonne clair en BDD.
    private Instant desactivationDate;

    // --- Relations ---

    /**
     * Liste des {@link Categorie}s associées à cet événement.
     * Relation One-to-Many : Un événement peut avoir plusieurs catégories.
     * Le chargement est paresseux (LAZY).
     * La cascade {@code ALL} et {@code orphanRemoval = true} assurent que le cycle de vie
     * des catégories est lié à celui de l'événement.
     * Visible dans la vue détaillée de l'événement.
     *
     * <p>Note sur {@link JsonView}: L'inclusion de ce champ dans la vue {@code EventView}
     * déclenchera le chargement paresseux de la collection de {@link Categorie}s.</p>
     */
    @OneToMany(mappedBy = "event", // 'event' est le champ dans Categorie qui référence Event.
            cascade = CascadeType.ALL, // Propagation des opérations de persistance aux catégories.
            orphanRemoval = true,    // Suppression des catégories orphelines.
            fetch = FetchType.LAZY)      // Chargement paresseux (essentiel).
    @JsonView(GlobalView.EventView.class) // Visible dans la vue Event détaillée.
    private List<Categorie> categories = new ArrayList<>(); // Initialisation.

    /**
     * Le {@link Club} organisateur de cet événement.
     * Relation Many-to-One : Plusieurs événements peuvent être organisés par le même club.
     * Cette relation est obligatoire.
     * Le chargement est paresseux (LAZY).
     * Visible dans les vues détaillées de l'événement et de la réservation.
     *
     * <p>Note sur {@link JsonView}: L'inclusion de ce champ déclenchera le chargement LAZY
     * de l'entité {@link Club}. Utiliser une vue appropriée (ex: {@code GlobalView.Base})
     * pour l'organisateur est souvent suffisant pour éviter de charger trop d'informations.</p>
     */
    @NotNull(message = "L'événement doit avoir un organisateur.") // Validation applicative.
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "organisateur_id", // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_event_organisateur")) // Nom explicite de la contrainte FK.
    @JsonView({GlobalView.EventView.class, GlobalView.ReservationView.class})
    private Club organisateur;

    /**
     * Liste des {@link Notation}s laissées par les participants pour cet événement.
     * Relation One-to-Many : Un événement peut recevoir plusieurs notations.
     * Le chargement est paresseux (LAZY).
     * La cascade {@code ALL} et {@code orphanRemoval = true} lient le cycle de vie des notations
     * à celui de l'événement.
     * Généralement ignoré dans les vues JSON par défaut pour éviter de charger des données non
     * essentielles et pour prévenir les boucles. Les notations peuvent être récupérées via un endpoint dédié.
     */
    @OneToMany(mappedBy = "event", // 'event' est le champ dans Notation qui référence Event.
            cascade = CascadeType.ALL, // Propagation des opérations.
            orphanRemoval = true,    // Suppression des notations orphelines.
            fetch = FetchType.LAZY)      // Chargement paresseux.
    @JsonIgnore // Ignoré par défaut lors de la sérialisation JSON.
    private List<Notation> notations = new ArrayList<>(); // Initialisation.

    // --- Méthode de Préparation à la Désactivation ---

    /**
     * Prépare l'entité Event pour une désactivation logique (soft delete), souvent appelée "annulation".
     * Modifie le nom de l'événement pour indiquer visuellement son état annulé
     * (en ajoutant un préfixe "[Annulé]") et enregistre la date/heure actuelle comme date de désactivation.
     * Ne modifie pas l'état {@code actif} directement (cela doit être fait séparément avant la sauvegarde).
     * <p>
     * <b>Important :</b> Cette méthode modifie l'état de l'objet mais ne le persiste pas.
     * Elle doit être appelée dans une transaction avant de sauvegarder l'entité avec {@code actif = false}.
     * </p>
     *
     * @throws IllegalStateException si l'événement n'a pas encore été persisté ({@code id} est null).
     */
    public void prepareForDeactivation() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible de préparer la désactivation d'un événement non persisté.");
        }

        // Ajoute un préfixe au nom pour marquer visuellement l'annulation, sauf s'il est déjà présent.
        if (!this.nom.startsWith("[Annulé]")) {
            this.nom = "[Annulé] " + this.nom;
        }

        // Enregistre le moment de la désactivation.
        this.desactivationDate = Instant.now();

        // Note: Le champ 'actif' doit être mis à 'false' par l'appelant (service) avant la sauvegarde.
    }

    // --- Méthodes Calculées (@Transient) ---

    /**
     * Calcule la capacité totale de l'événement en additionnant les capacités
     * de toutes ses {@link Categorie}s associées.
     * Déclenche le chargement paresseux de la collection {@code categories} si nécessaire.
     *
     * @return La capacité totale (Integer) de l'événement. Retourne 0 si aucune catégorie n'est associée
     * ou si les capacités des catégories sont nulles ou invalides.
     * @see Transient
     * @see JsonView
     * @see Categorie
     */
    @Transient // Non persisté en BDD.
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class}) // Visible dans ces vues JSON.
    public int getPlaceTotal() {
        // Utilise le getter pour le chargement LAZY potentiel.
        List<Categorie> currentCategories = getCategories();
        if (currentCategories == null) {
            return 0;
        }
        // Somme des capacités des catégories, en gérant le cas où la capacité d'une catégorie serait null.
        return currentCategories.stream()
                .mapToInt(cat -> (cat != null && cat.getCapacite() != null) ? cat.getCapacite() : 0)
                .sum();
    }

    /**
     * Calcule le nombre total de places réservées (confirmées) pour cet événement
     * en additionnant les places réservées confirmées de toutes ses {@link Categorie}s.
     * Déclenche le chargement paresseux de la collection {@code categories} et potentiellement
     * des collections {@code reservations} de chaque catégorie.
     *
     * @return Le nombre total (Integer) de places réservées confirmées pour l'événement.
     * @see Transient
     * @see JsonView
     * @see Categorie#getPlaceReserve()
     */
    @Transient // Non persisté en BDD.
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class}) // Visible dans ces vues JSON.
    public int getPlaceReserve() {
        // Utilise le getter pour le chargement LAZY.
        List<Categorie> currentCategories = getCategories();
        if (currentCategories == null) {
            return 0;
        }
        // Somme des places réservées (confirmées) de chaque catégorie.
        return currentCategories.stream()
                .filter(Objects::nonNull) // Sécurité: ignore les catégories nulles dans la liste
                .mapToInt(Categorie::getPlaceReserve) // Appelle la méthode calculée de Categorie
                .sum();
    }

    /**
     * Calcule le nombre total de places encore disponibles pour l'événement.
     * Se base sur la capacité totale ({@link #getPlaceTotal()}) et le nombre total
     * de places réservées confirmées ({@link #getPlaceReserve()}).
     *
     * @return Le nombre total (Integer) de places disponibles pour l'événement. Retourne 0 si le
     * nombre de places réservées dépasse la capacité totale (cas d'incohérence).
     * @see Transient
     * @see JsonView
     */
    @Transient // Non persisté en BDD.
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class}) // Visible dans ces vues JSON.
    public int getPlaceDisponible() {
        // Utilise les autres méthodes @Transient pour le calcul.
        int total = getPlaceTotal();
        int reservees = getPlaceReserve();
        return Math.max(0, total - reservees); // Assure un résultat non négatif.
    }

    // --- equals et hashCode (Basés sur l'ID) ---

    /**
     * Détermine si cet objet Event est égal à un autre objet.
     * L'égalité est basée sur l'identifiant ({@code id}) pour les entités persistées.
     * Deux événements sont égaux s'ils ont le même ID non nul.
     *
     * @param o L'objet à comparer.
     * @return {@code true} si les objets sont égaux (même instance ou même ID persisté), {@code false} sinon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifie si 'o' est une instance de Event (ou sous-classe) et non null.
        if (!(o instanceof Event event)) return false;
        // Compare les IDs s'ils sont non nuls.
        return id != null && id.equals(event.id);
    }

    /**
     * Retourne un code de hachage pour cet objet Event.
     * Basé sur l'identifiant ({@code id}) si l'entité est persistée.
     * Utilise une valeur dérivée de la classe pour les instances non persistées ({@code id} null).
     *
     * @return Le code de hachage basé sur l'ID ou une valeur par défaut pour les instances non persistées.
     */
    @Override
    public int hashCode() {
        // Cohérent avec equals: utilise l'ID si disponible, sinon se base sur la classe.
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }

    // Optionnel: toString() pour le débogage
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", start=" + startTime +
                ", end=" + endTime +
                ", actif=" + actif +
                ", organisateurId=" + (organisateur != null ? organisateur.getId() : "null") + // Évite chargement LAZY
                '}';
    }
}

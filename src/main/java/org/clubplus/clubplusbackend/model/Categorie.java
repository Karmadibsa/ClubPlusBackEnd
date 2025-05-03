package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité JPA représentant une catégorie spécifique au sein d'un {@link Event}.
 * Chaque événement peut être divisé en plusieurs catégories (ex: tarifs différents,
 * sections différentes) ayant chacune leur propre nom et capacité.
 * Elle est liée à l'événement parent et aux réservations effectuées pour cette catégorie spécifique.
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont basés sur l'identifiant unique {@code id}.
 * Cela est approprié pour les entités gérées par JPA une fois qu'elles ont été persistées.</p>
 *
 * @see Event
 * @see Reservation
 * @see GlobalView
 */
@Getter // Lombok: Génère les getters.
@Setter
// Lombok: Génère les setters (utiles pour JPA/formulaires, même si certaines propriétés ne devraient pas changer après création).
@Entity // Marque cette classe comme une entité JPA.
@NoArgsConstructor // Lombok: Constructeur sans argument (requis par JPA).
@AllArgsConstructor // Lombok: Constructeur avec tous les arguments (peut être utile, mais attention à l'ordre).
@Table(name = "categories") // Nom explicite de la table en base de données.
public class Categorie {

    /**
     * Identifiant unique de la catégorie, généré automatiquement.
     * Visible dans la vue JSON de base.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Stratégie d'auto-incrémentation.
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom distinctif de la catégorie au sein de l'événement (ex: "Standard", "VIP", "Enfant").
     * Ne doit pas être vide et doit respecter les contraintes de taille.
     * Visible dans la vue JSON de base.
     */
    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide.") // Validation: non null et non vide.
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.") // Validation: taille.
    @Column(nullable = false, length = 100) // Contrainte BDD: non null, longueur max.
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Capacité maximale de places disponibles spécifiquement pour cette catégorie.
     * Doit être définie et ne peut pas être négative.
     * Visible dans la vue JSON de base.
     */
    @NotNull(message = "La capacité est obligatoire.") // Validation: non null.
    @Min(value = 0, message = "La capacité ne peut pas être négative.") // Validation: valeur minimale.
    @Column(nullable = false) // Contrainte BDD: non null.
    @JsonView(GlobalView.Base.class)
    private Integer capacite;

    /**
     * L'événement parent auquel cette catégorie appartient.
     * Relation Many-to-One : Plusieurs catégories peuvent appartenir au même événement.
     * Cette relation est obligatoire (une catégorie existe toujours dans le contexte d'un événement).
     * Le chargement est paresseux (LAZY) pour optimiser les performances.
     *
     * <p>Note sur {@link JsonView}: L'inclusion de ce champ dans la vue {@code CategorieView}
     * déclenchera le chargement paresseux de l'entité {@link Event} lors de la sérialisation.</p>
     */
    @NotNull(message = "La catégorie doit être liée à un événement.") // Validation applicative.
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux (TRÈS important).
            optional = false)       // Indique à JPA que la relation est obligatoire.
    @JoinColumn(name = "event_id",   // Nom de la colonne clé étrangère.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_categorie_event")) // Nom explicite de la contrainte FK.
    @JsonView(GlobalView.CategorieView.class) // Visible dans la vue spécifique Catégorie.
    private Event event;

    /**
     * La liste des réservations effectuées spécifiquement pour cette catégorie.
     * Relation One-to-Many : Une catégorie peut avoir plusieurs réservations.
     * Le chargement est paresseux (LAZY).
     * La cascade {@code ALL} et {@code orphanRemoval = true} signifient que les réservations sont
     * totalement dépendantes du cycle de vie de la catégorie : elles sont créées, mises à jour, et
     * supprimées avec la catégorie. Si une réservation est retirée de cette liste (et non associée ailleurs),
     * elle sera supprimée de la base de données.
     * La liste est initialisée pour éviter les NullPointerExceptions.
     *
     * <p>Note sur {@link JsonView}: L'inclusion de ce champ dans la vue {@code CategorieView}
     * déclenchera le chargement paresseux de la collection de {@link Reservation} lors de la sérialisation.</p>
     */
    @OneToMany(mappedBy = "categorie", // 'categorie' est le nom du champ dans l'entité Reservation qui référence Categorie.
            cascade = CascadeType.ALL, // Opérations de persistance (save, update, delete) sont propagées aux réservations.
            orphanRemoval = true,    // Supprime une réservation de la BDD si elle est retirée de cette collection.
            fetch = FetchType.LAZY)      // Chargement paresseux (TRÈS important pour les collections).
    @JsonView(GlobalView.CategorieView.class) // Visible dans la vue spécifique Catégorie.
    private List<Reservation> reservations = new ArrayList<>(); // Initialisation pour éviter les NPE.


    // --- Méthodes Calculées (Non persistées) ---

    /**
     * Calcule et retourne le nombre de places actuellement réservées pour cette catégorie
     * dont le statut est {@link ReservationStatus#CONFIRME}.
     * Cette méthode déclenche le chargement paresseux de la collection {@code reservations} si elle n'a pas déjà été chargée.
     * Les réservations avec d'autres statuts (ex: ANNULE, UTILISE) ne sont pas comptées.
     *
     * @return Le nombre entier de réservations confirmées pour cette catégorie. Retourne 0 si la liste est null ou vide.
     * @see Transient
     * @see JsonView
     * @see ReservationStatus#CONFIRME
     */
    @Transient // Indique que ce n'est pas un champ à persister en base de données.
    @JsonView({GlobalView.EventView.class, GlobalView.CategorieView.class}) // Visible dans ces vues JSON.
    public int getPlaceReserve() {
        // Utilise le getter pour potentiellement bénéficier du proxy Hibernate et du chargement LAZY.
        List<Reservation> currentReservations = getReservations();
        if (currentReservations == null) {
            return 0;
        }
        // Filtre le flux des réservations pour ne compter que celles qui sont confirmées.
        return (int) currentReservations.stream()
                .filter(r -> r != null && r.getStatus() == ReservationStatus.CONFIRME) // Ajout vérification nullité 'r' par sécurité
                .count();
    }

    /**
     * Calcule et retourne le nombre de places encore disponibles dans cette catégorie.
     * Se base sur la {@code capacite} totale et le nombre de places déjà réservées (confirmées).
     * Retourne 0 si la capacité n'est pas définie ou est négative.
     *
     * @return Le nombre entier de places disponibles pour cette catégorie.
     * @see Transient
     * @see JsonView
     * @see #getCapacite()
     * @see #getPlaceReserve()
     */
    @Transient // Non persisté en BDD.
    @JsonView({GlobalView.EventView.class, GlobalView.CategorieView.class}) // Visible dans ces vues JSON.
    public int getPlaceDisponible() {
        // Utilise les getters pour la robustesse et le respect du chargement LAZY potentiel.
        Integer currentCapacite = getCapacite();
        // Gestion des cas où la capacité serait invalide.
        if (currentCapacite == null || currentCapacite < 0) {
            return 0;
        }
        // Calcule la différence. Appelle getPlaceReserve() qui gère le compte des places prises.
        int placesReservees = getPlaceReserve();
        return Math.max(0, currentCapacite - placesReservees); // Assure de ne pas retourner un nombre négatif.
    }

    // --- equals et hashCode ---

    /**
     * Détermine si cet objet Catégorie est égal à un autre objet.
     * Deux catégories sont considérées égales si elles représentent la même ligne dans la base de données,
     * c'est-à-dire si leurs identifiants ({@code id}) sont non nuls et égaux.
     * Pour les instances non encore persistées ({@code id} est null), elles ne sont égales qu'à elles-mêmes.
     *
     * @param o L'objet à comparer avec cette catégorie.
     * @return {@code true} si les objets sont égaux (même instance ou même ID persisté), {@code false} sinon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifie si 'o' est une instance de Categorie (ou sous-classe) et non null.
        if (!(o instanceof Categorie categorie)) return false;
        // Si l'ID de cette instance est null, ils ne peuvent être égaux que s'ils sont la même instance (déjà vérifié par 'this == o').
        // Si l'ID est non null, compare les IDs.
        return id != null && id.equals(categorie.getId());
    }

    /**
     * Retourne un code de hachage pour cet objet Catégorie.
     * Basé sur l'identifiant ({@code id}) si l'entité est persistée ({@code id} non null).
     * Si l'entité n'est pas persistée, utilise une valeur constante dérivée de la classe
     * pour assurer la cohérence avec la méthode {@code equals} (les objets non persistés
     * ne sont égaux qu'à eux-mêmes et ne devraient idéalement pas être utilisés comme clés
     * dans des HashMaps/HashSets avant d'avoir un ID).
     *
     * @return Le code de hachage basé sur l'ID ou une valeur par défaut pour les instances non persistées.
     */
    @Override
    public int hashCode() {
        // Utilise Objects.hash(id) si id est non null, sinon utilise une constante (souvent 31 ou hash de la classe)
        // pour les objets transitoires. Utiliser getClass().hashCode() est une approche simple.
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }

    // Optionnel: toString() pour le débogage
    @Override
    public String toString() {
        return "Categorie{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", capacite=" + capacite +
                ", eventId=" + (event != null ? event.getId() : "null") + // Évite chargement LAZY
                '}';
    }
}

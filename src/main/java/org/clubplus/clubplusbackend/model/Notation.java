package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.Instant;
import java.util.Objects;

/**
 * Entité JPA représentant la notation d'un {@link Event} par un {@link Membre}.
 * Elle capture les évaluations sur plusieurs critères spécifiques (ambiance, propreté, etc.)
 * ainsi que la date à laquelle la notation a été effectuée.
 *
 * <p>Une contrainte d'unicité garantit qu'un membre ne peut noter qu'une seule fois
 * un événement spécifique (basée sur la paire {@code event_id}, {@code membre_id}).</p>
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont définis sur la base
 * des entités {@code event} et {@code membre} associées, reflétant ainsi la clé fonctionnelle
 * et la contrainte unique.</p>
 *
 * @see Event
 * @see Membre
 * @see GlobalView
 */
@Entity
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
@NoArgsConstructor // Lombok: Constructeur sans argument (requis par JPA).
@AllArgsConstructor // Lombok: Constructeur avec tous les arguments (pratique pour création/tests).
@Table(name = "notations", // Nom explicite de la table.
        // Contrainte unique: un membre ne note qu'une fois un événement donné.
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membre_id"}, name = "uk_notation_event_membre")
)
public class Notation {

    /**
     * Identifiant unique de la notation, généré automatiquement.
     * Visible dans plusieurs vues JSON (Base, EventView, NotationView).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class, GlobalView.NotationView.class})
    private Integer id;

    /**
     * L'événement qui est noté.
     * Relation Many-to-One : Plusieurs notations peuvent concerner le même événement.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     * Visible dans la vue JSON de base et la vue Notation détaillée.
     */
    @NotNull(message = "La notation doit être liée à un événement.") // Validation applicative.
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "event_id",   // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_notation_event")) // Nom explicite de la contrainte FK.
    @JsonView({GlobalView.Base.class, GlobalView.NotationView.class})
    private Event event;

    /**
     * Le membre qui a soumis la notation.
     * Relation Many-to-One : Un membre peut soumettre plusieurs notations (pour des événements différents).
     * La relation est obligatoire et chargée paresseusement (LAZY).
     * N'est généralement pas inclus dans les vues JSON de la notation elle-même pour éviter d'exposer
     * l'auteur, sauf si explicitement nécessaire (ex: dans une vue admin ou profil).
     * La relation est néanmoins requise pour la contrainte unique et la logique métier.
     */
    @NotNull(message = "La notation doit être faite par un membre.") // Validation applicative.
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "membre_id",  // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_notation_membre")) // Nom explicite de la contrainte FK.
    // Pas de @JsonView par défaut pour masquer l'auteur dans les contextes généraux.
    private Membre membre;

    // --- Critères de Notation (échelle de 1 à 5) ---
    // Les annotations @Min, @Max valident les entrées.
    // @Column(nullable = false) sur un type primitif 'int' est techniquement redondant
    // mais clarifie l'intention de non-nullité conceptuelle.

    /**
     * Note attribuée pour l'ambiance générale de l'événement (1 à 5).
     */
    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5")
    @Column(nullable = false) // Intention de non-nullité.
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class}) // Visible dans les vues Event et Notation.
    private int ambiance;

    /**
     * Note attribuée pour la propreté des lieux lors de l'événement (1 à 5).
     */
    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int proprete;

    /**
     * Note attribuée pour l'organisation de l'événement (1 à 5).
     */
    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int organisation;

    /**
     * Note attribuée pour le fair-play observé durant l'événement (1 à 5).
     */
    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int fairPlay;

    /**
     * Note attribuée pour le niveau général des joueurs/participants (1 à 5).
     */
    @Min(value = 1, message = "La note de niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note de niveau des joueurs doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int niveauJoueurs;

    // --- Timestamp ---

    /**
     * Date et heure exactes auxquelles la notation a été enregistrée.
     * Doit être non nulle et dans le passé ou le présent.
     * Non modifiable après la création initiale.
     * Initialisée automatiquement via {@code @PrePersist} si non fournie explicitement.
     * Visible dans la vue détaillée de la notation.
     */
    @NotNull // Validation applicative.
    @PastOrPresent // Une notation est faite après ou pendant l'événement.
    @Column(nullable = false, updatable = false) // Contrainte BDD: non null, non modifiable.
    @JsonView(GlobalView.NotationView.class)
    private Instant dateNotation;

    // --- Callbacks JPA ---

    /**
     * Callback JPA exécuté juste avant la première persistance (insertion) de l'entité.
     * Assure que le champ {@code dateNotation} est initialisé à l'heure actuelle
     * si ce n'était pas déjà fait explicitement lors de la création de l'objet.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.dateNotation == null) {
            this.dateNotation = Instant.now();
        }
    }

    // --- Méthode Calculée (Non persistée) ---

    /**
     * Calcule la note moyenne globale pour cette notation, basée sur les cinq critères.
     * La division est effectuée en virgule flottante pour obtenir un résultat précis.
     *
     * @return La note moyenne (double) calculée à partir des notes des critères.
     * @see Transient
     * @see JsonView
     */
    @Transient // Non persisté en BDD.
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class}) // Visible dans ces vues JSON.
    public double getNoteMoyenne() {
        // La somme est effectuée sur des 'int', puis division par 5.0 pour forcer le calcul en double.
        return (ambiance + proprete + organisation + fairPlay + niveauJoueurs) / 5.0;
    }

    // --- equals et hashCode ---

    /**
     * Détermine si cet objet Notation est égal à un autre objet.
     * L'égalité est basée sur l'identité de l'événement noté et du membre auteur de la notation,
     * ce qui correspond à la clé fonctionnelle et à la contrainte unique de la table.
     * Deux notations sont égales si elles concernent le même événement et le même membre.
     *
     * <p>Attention : Cette comparaison repose sur les méthodes {@code equals} des entités
     * {@code Event} et {@code Membre} associées (qui sont basées sur leur ID). Si l'une de ces
     * entités liées n'est pas encore persistée (ID null) ou n'est pas chargée (proxy non initialisé),
     * le résultat peut être inattendu.</p>
     *
     * @param o L'objet à comparer.
     * @return {@code true} si l'objet est une Notation pour le même événement par le même membre, {@code false} sinon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifie si 'o' est une instance de Notation et non null.
        if (!(o instanceof Notation notation)) return false;

        // Compare les entités Event et Membre liées.
        // Suppose que Event et Membre ont une implémentation correcte de equals/hashCode basée sur l'ID.
        return Objects.equals(event, notation.event) &&
                Objects.equals(membre, notation.membre);
    }

    /**
     * Retourne un code de hachage pour cet objet Notation.
     * Le code de hachage est calculé à partir des entités {@code event} et {@code membre} liées,
     * en cohérence avec la méthode {@code equals}.
     *
     * @return Le code de hachage basé sur les entités liées.
     */
    @Override
    public int hashCode() {
        // Calcule le hash basé sur les entités liées.
        // Suppose que Event et Membre ont une implémentation correcte de equals/hashCode.
        return Objects.hash(event, membre);
    }

    // Optionnel: toString() pour le débogage
    @Override
    public String toString() {
        return "Notation{" +
                "id=" + id +
                ", eventId=" + (event != null ? event.getId() : "null") +
                ", membreId=" + (membre != null ? membre.getId() : "null") +
                ", ambiance=" + ambiance +
                ", proprete=" + proprete +
                ", organisation=" + organisation +
                ", fairPlay=" + fairPlay +
                ", niveauJoueurs=" + niveauJoueurs +
                ", dateNotation=" + dateNotation +
                ", noteMoyenne=" + getNoteMoyenne() + // Inclut la moyenne calculée
                '}';
    }
}

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
 * <p>
 * Une contrainte d'unicité garantit qu'un membre ne peut noter un événement qu'une seule fois.
 * L'égalité des objets est basée sur la paire (événement, membre).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membre_id"}, name = "uk_notation_event_membre")
)
public class Notation {

    /**
     * Identifiant unique de la notation, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class, GlobalView.NotationView.class})
    private Integer id;

    /**
     * L'événement qui est noté.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     */
    @NotNull(message = "La notation doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notation_event"))
    @JsonView({GlobalView.Base.class, GlobalView.NotationView.class})
    private Event event;

    /**
     * Le membre qui a soumis la notation.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     * Par défaut, ce champ n'est pas exposé en JSON pour préserver l'anonymat.
     */
    @NotNull(message = "La notation doit être faite par un membre.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notation_membre"))
    private Membre membre;

    // --- Critères de Notation (1 à 5) ---

    /**
     * Note pour l'ambiance générale de l'événement.
     */
    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int ambiance;

    /**
     * Note pour la propreté des lieux.
     */
    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int proprete;

    /**
     * Note pour l'organisation de l'événement.
     */
    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int organisation;

    /**
     * Note pour le fair-play des participants.
     */
    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int fairPlay;

    /**
     * Note pour le niveau général des joueurs.
     */
    @Min(value = 1, message = "La note de niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note de niveau des joueurs doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int niveauJoueurs;

    // --- Timestamp ---

    /**
     * Date et heure de l'enregistrement de la notation.
     * Définie automatiquement avant la persistance.
     */
    @NotNull
    @PastOrPresent
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.NotationView.class)
    private Instant dateNotation;

    /**
     * Callback JPA pour définir la date de notation avant la première sauvegarde.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.dateNotation == null) {
            this.dateNotation = Instant.now();
        }
    }

    // --- Champ Calculé ---

    /**
     * Calcule la note moyenne globale de cette notation.
     * <p>
     * Ce champ n'est pas persisté en base de données.
     *
     * @return La note moyenne (double).
     */
    @Transient
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    public double getNoteMoyenne() {
        return (ambiance + proprete + organisation + fairPlay + niveauJoueurs) / 5.0;
    }

    /**
     * L'égalité est basée sur la paire (événement, membre).
     * Deux notations sont égales si elles concernent le même événement et le même membre.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notation notation)) return false;
        return Objects.equals(event, notation.event) &&
                Objects.equals(membre, notation.membre);
    }

    /**
     * Le hashcode est basé sur les entités événement et membre.
     */
    @Override
    public int hashCode() {
        return Objects.hash(event, membre);
    }

    /**
     * Représentation textuelle de la notation pour le débogage.
     */
    @Override
    public String toString() {
        return "Notation{" +
                "id=" + id +
                ", eventId=" + (event != null ? event.getId() : "null") +
                ", membreId=" + (membre != null ? membre.getId() : "null") +
                ", noteMoyenne=" + getNoteMoyenne() +
                '}';
    }
}

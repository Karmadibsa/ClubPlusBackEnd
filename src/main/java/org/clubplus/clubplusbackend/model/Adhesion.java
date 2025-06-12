package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.Instant;
import java.util.Objects;

/**
 * Entité JPA représentant l'adhésion d'un {@link Membre} à un {@link Club}.
 * <p>
 * Cette classe agit comme une table de jointure enrichie, ajoutant une date d'adhésion
 * à la relation Many-to-Many entre Membre et Club.
 * Une contrainte d'unicité sur la paire (membre, club) garantit qu'un membre
 * ne peut adhérer qu'une seule fois au même club.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "adhesion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"membre_id", "club_id"}, name = "uk_adhesion_membre_club")
)
@EqualsAndHashCode(of = {"membre", "club"})
public class Adhesion {

    /**
     * Identifiant unique de l'adhésion, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Le membre concerné par cette adhésion.
     * La relation est obligatoire et chargée paresseusement (LAZY) pour optimiser les performances.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adhesion_membre"))
    private Membre membre;

    /**
     * Le club concerné par cette adhésion.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adhesion_club"))
    @JsonView(GlobalView.MembreView.class)
    private Club club;

    /**
     * La date et l'heure de création de l'adhésion.
     * Ce champ est initialisé à la création et ne peut pas être mis à jour.
     */
    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant dateAdhesion;

    /**
     * Constructeur pour créer une nouvelle adhésion.
     * Associe un membre et un club, et définit la date d'adhésion à l'instant présent.
     *
     * @param membre Le membre qui adhère (ne doit pas être null).
     * @param club   Le club rejoint (ne doit pas être null).
     */
    public Adhesion(Membre membre, Club club) {
        this.membre = Objects.requireNonNull(membre, "Le membre ne peut pas être null pour une adhésion.");
        this.club = Objects.requireNonNull(club, "Le club ne peut pas être null pour une adhésion.");
        this.dateAdhesion = Instant.now();
    }

    /**
     * Représentation textuelle de l'adhésion pour le débogage.
     * Affiche les IDs pour éviter le chargement paresseux des entités liées.
     */
    @Override
    public String toString() {
        return "Adhesion{" +
                "id=" + id +
                ", membreId=" + (membre != null ? membre.getId() : "null") +
                ", clubId=" + (club != null ? club.getId() : "null") +
                ", dateAdhesion=" + dateAdhesion +
                '}';
    }
}

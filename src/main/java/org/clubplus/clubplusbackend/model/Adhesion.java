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
 * Cette classe sert de table de jointure pour la relation Many-to-Many entre Membre et Club,
 * enrichie avec la date à laquelle l'adhésion a eu lieu.
 *
 * <p>Une contrainte d'unicité garantit qu'un membre ne peut adhérer qu'une seule fois
 * au même club (basée sur la paire `membre_id`, `club_id`).</p>
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont définis sur la base
 * des entités {@code membre} et {@code club} associées. Cela signifie que deux objets {@code Adhesion}
 * sont considérés égaux s'ils représentent l'adhésion du même membre au même club,
 * indépendamment de leur ID de persistance ou de leur date d'adhésion. C'est pertinent
 * pour vérifier l'existence d'une adhésion avant de la créer, par exemple.</p>
 *
 * @see Membre
 * @see Club
 * @see GlobalView
 */
@Entity
@Getter // Lombok: Génère les getters pour tous les champs.
@Setter
// Lombok: Génère les setters. Utile pour JPA, mais membre/club/dateAdhesion ne devraient pas être modifiés après création via le constructeur.
@NoArgsConstructor // Lombok: Génère un constructeur sans argument, requis par JPA.
@Table(
        name = "adhesion", // Nom explicite de la table (bonne pratique)
        // Définit une contrainte d'unicité au niveau base de données pour la paire membre/club.
        uniqueConstraints = @UniqueConstraint(columnNames = {"membre_id", "club_id"}, name = "uk_adhesion_membre_club")
)
// Lombok: Définit equals/hashCode basés sur les objets 'membre' et 'club'.
// Important pour les opérations sur les Set avant persistance ou si l'ID n'est pas encore assigné.
@EqualsAndHashCode(of = {"membre", "club"})
public class Adhesion {

    /**
     * Identifiant unique de l'adhésion, généré automatiquement par la base de données.
     * Peut être inclus dans certaines vues JSON (ex: {@link GlobalView.Base}) si nécessaire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Stratégie d'auto-incrémentation standard.
    @JsonView(GlobalView.Base.class) // Visible dans la vue de base.
    private Integer id;

    /**
     * Le membre concerné par cette adhésion.
     * Relation Many-to-One : Plusieurs adhésions peuvent pointer vers un même membre.
     * Ne peut pas être nul (une adhésion doit avoir un membre).
     * Chargé paresseusement (LAZY) pour optimiser les performances (le membre n'est chargé que si nécessaire).
     *
     * <p>Note sur {@link JsonView}: Si cette relation est incluse dans une vue JSON,
     * cela déclenchera le chargement LAZY de l'entité {@link Membre}.</p>
     */
    @NotNull // Validation au niveau applicatif.
    @ManyToOne(fetch = FetchType.LAZY, // Stratégie de chargement paresseux (bonne pratique).
            optional = false)       // Indique à JPA que la relation est obligatoire.
    @JoinColumn(name = "membre_id", // Nom de la colonne clé étrangère en BDD.
            nullable = false,      // Contrainte NOT NULL au niveau BDD.
            foreignKey = @ForeignKey(name = "fk_adhesion_membre"))
    // Nom explicite de la contrainte FK (optionnel mais recommandé).
    private Membre membre;

    /**
     * Le club concerné par cette adhésion.
     * Relation Many-to-One : Plusieurs adhésions peuvent pointer vers un même club.
     * Ne peut pas être nul (une adhésion doit appartenir à un club).
     * Chargé paresseusement (LAZY).
     *
     * <p>Note sur {@link JsonView}: Inclure cette relation dans une vue déclenchera son chargement LAZY.</p>
     */
    @NotNull // Validation applicative.
    @ManyToOne(fetch = FetchType.LAZY, // Chargement paresseux.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "club_id",   // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_adhesion_club"))  // Nom de la contrainte FK.
    @JsonView(GlobalView.MembreView.class)
    private Club club;

    /**
     * La date et l'heure exactes auxquelles l'adhésion a été créée/enregistrée.
     * Cette date est définie lors de la création de l'objet via le constructeur dédié
     * et ne devrait pas être modifiée par la suite (colonne `updatable = false`).
     * Ne peut pas être nulle.
     *
     * <p>Alternative : Utiliser {@code @CreationTimestamp} de Hibernate pour laisser la BDD gérer
     * automatiquement la date de création, mais l'initialisation dans le constructeur est aussi une approche valide.</p>
     */
    @NotNull // Validation applicative.
    @Column(nullable = false,       // Contrainte NOT NULL BDD.
            updatable = false)    // La date d'adhésion ne doit pas être modifiée après création.
    private Instant dateAdhesion;

    /**
     * Constructeur métier pour créer une nouvelle instance d'Adhesion.
     * Associe un membre à un club et initialise la date d'adhésion à l'heure actuelle.
     * Lève une exception si le membre ou le club fourni est null, garantissant un état initial valide.
     *
     * @param membre Le {@link Membre} qui adhère. Ne doit pas être null.
     * @param club   Le {@link Club} auquel le membre adhère. Ne doit pas être null.
     * @throws IllegalArgumentException si {@code membre} ou {@code club} est null.
     */
    public Adhesion(Membre membre, Club club) {
        // Validation défensive pour garantir l'intégrité de l'objet avant même la persistance.
        this.membre = Objects.requireNonNull(membre, "Membre ne peut pas être null pour créer une Adhesion");
        this.club = Objects.requireNonNull(club, "Club ne peut pas être null pour créer une Adhesion");
        // Initialisation de la date au moment de la création de l'instance.
        this.dateAdhesion = Instant.now();
    }

    // Les Getters et Setters sont générés par Lombok (@Getter, @Setter).
    // Le constructeur sans argument est généré par Lombok (@NoArgsConstructor).
    // equals() et hashCode() sont générés par Lombok (@EqualsAndHashCode).

    // Optionnel: toString() pour le débogage (Lombok peut aussi le générer avec @ToString)
    @Override
    public String toString() {
        return "Adhesion{" +
                "id=" + id +
                ", membreId=" + (membre != null ? membre.getId() : "null") + // Affiche juste l'ID pour éviter chargement LAZY
                ", clubId=" + (club != null ? club.getId() : "null") +       // Affiche juste l'ID
                ", dateAdhesion=" + dateAdhesion +
                '}';
    }
}

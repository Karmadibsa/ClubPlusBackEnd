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
 * <p>
 * Gère les informations de l'événement, son statut (actif/inactif), et ses relations
 * avec les catégories, le club organisateur et les notations.
 * L'égalité des objets est basée sur l'ID de persistance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    /**
     * Identifiant unique et auto-généré de l'événement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom de l'événement.
     */
    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    @Column(nullable = false, length = 150)
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Date et heure de début de l'événement.
     * Doit être dans le présent ou le futur.
     */
    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.")
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Instant startTime;

    /**
     * Date et heure de fin de l'événement.
     * Doit être dans le présent ou le futur.
     */
    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Instant endTime;

    /**
     * Description détaillée de l'événement.
     */
    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.")
    @Column(nullable = false, length = 2000)
    @JsonView(GlobalView.EventView.class)
    private String description;

    /**
     * Lieu où se déroule l'événement (optionnel).
     */
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    @Column(length = 255)
    @JsonView(GlobalView.Base.class)
    private String location;

    // --- Soft Delete ---

    /**
     * Indique si l'événement est actif (true) ou annulé (false).
     */
    @NotNull
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Boolean actif = true;

    /**
     * Date de désactivation de l'événement. Reste null tant que l'événement est actif.
     */
    @Column(name = "desactivation_date")
    private Instant desactivationDate;

    // --- Relations ---

    /**
     * Liste des catégories de l'événement.
     * Le cycle de vie des catégories est lié à celui de l'événement (cascade).
     */
    @OneToMany(mappedBy = "event",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonView(GlobalView.EventView.class)
    private List<Categorie> categories = new ArrayList<>();

    /**
     * Le club organisateur de l'événement.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     */
    @NotNull(message = "L'événement doit avoir un organisateur.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_organisateur"))
    @JsonView({GlobalView.EventView.class, GlobalView.ReservationView.class})
    private Club organisateur;

    /**
     * Liste des notations reçues pour cet événement.
     * Ignorée en JSON pour éviter les chargements inutiles et les boucles.
     */
    @OneToMany(mappedBy = "event",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Notation> notations = new ArrayList<>();

    /**
     * Prépare l'événement à la désactivation en modifiant son nom et en enregistrant la date d'annulation.
     */
    public void prepareForDeactivation() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible de désactiver un événement non persisté.");
        }
        if (!this.nom.startsWith("[Annulé]")) {
            this.nom = "[Annulé] " + this.nom;
        }
        this.desactivationDate = Instant.now();
    }

    // --- Champs Calculés ---

    /**
     * Calcule la capacité totale de l'événement en sommant la capacité de ses catégories.
     *
     * @return La capacité totale.
     */
    @Transient
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    public int getPlaceTotal() {
        if (this.categories == null) {
            return 0;
        }
        return this.categories.stream()
                .mapToInt(cat -> (cat != null && cat.getCapacite() != null) ? cat.getCapacite() : 0)
                .sum();
    }

    /**
     * Calcule le nombre total de places réservées (statut CONFIRME) pour l'événement.
     *
     * @return Le nombre total de réservations confirmées.
     */
    @Transient
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    public int getPlaceReserve() {
        if (this.categories == null) {
            return 0;
        }
        return this.categories.stream()
                .filter(Objects::nonNull)
                .mapToInt(Categorie::getPlaceReserve)
                .sum();
    }

    /**
     * Calcule le nombre de places encore disponibles pour l'événement.
     *
     * @return Le nombre de places disponibles.
     */
    @Transient
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    public int getPlaceDisponible() {
        return Math.max(0, getPlaceTotal() - getPlaceReserve());
    }

    /**
     * L'égalité est basée sur l'ID. Deux événements sont égaux si leurs IDs sont les mêmes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return id != null && id.equals(event.id);
    }

    /**
     * Le hashcode est basé sur l'ID pour les entités persistées.
     */
    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }

    /**
     * Représentation textuelle de l'événement pour le débogage.
     */
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", actif=" + actif +
                ", organisateurId=" + (organisateur != null ? organisateur.getId() : "null") +
                '}';
    }
}

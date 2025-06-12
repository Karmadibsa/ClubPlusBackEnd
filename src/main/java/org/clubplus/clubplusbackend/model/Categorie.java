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
 * Entité JPA représentant une catégorie au sein d'un {@link Event}.
 * <p>
 * Chaque catégorie définit une subdivision d'un événement, avec son propre nom et sa propre capacité.
 * L'égalité des objets est basée sur l'ID de persistance.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Categorie {

    /**
     * Identifiant unique de la catégorie, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom de la catégorie (ex: "Standard", "VIP").
     * Doit être unique au sein d'un même événement.
     */
    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Capacité maximale de places disponibles pour cette catégorie.
     */
    @NotNull(message = "La capacité est obligatoire.")
    @Min(value = 0, message = "La capacité ne peut pas être négative.")
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Integer capacite;

    /**
     * L'événement parent auquel cette catégorie est rattachée.
     * La relation est obligatoire et chargée paresseusement (LAZY) pour des raisons de performance.
     */
    @NotNull(message = "La catégorie doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_categorie_event"))
    @JsonView(GlobalView.CategorieView.class)
    private Event event;

    /**
     * Liste des réservations effectuées pour cette catégorie.
     * Le cycle de vie des réservations est entièrement géré par la catégorie (cascade et suppression des orphelins).
     */
    @OneToMany(mappedBy = "categorie",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonView(GlobalView.CategorieView.class)
    private List<Reservation> reservations = new ArrayList<>();


    /**
     * Calcule le nombre de places réservées (statut CONFIRME) pour cette catégorie.
     * <p>
     * Ce champ n'est pas persisté en base de données.
     *
     * @return Le nombre de réservations confirmées.
     */
    @Transient
    @JsonView({GlobalView.EventView.class, GlobalView.CategorieView.class})
    public int getPlaceReserve() {
        if (this.reservations == null) {
            return 0;
        }
        return (int) this.reservations.stream()
                .filter(r -> r != null && r.getStatus() == ReservationStatus.CONFIRME)
                .count();
    }

    /**
     * Calcule le nombre de places encore disponibles dans cette catégorie.
     * <p>
     * Ce champ n'est pas persisté en base de données.
     *
     * @return Le nombre de places disponibles.
     */
    @Transient
    @JsonView({GlobalView.EventView.class, GlobalView.CategorieView.class})
    public int getPlaceDisponible() {
        if (this.capacite == null || this.capacite < 0) {
            return 0;
        }
        int placesReservees = getPlaceReserve();
        return Math.max(0, this.capacite - placesReservees);
    }

    /**
     * L'égalité est basée sur l'ID. Deux catégories sont égales si leurs IDs sont les mêmes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categorie that)) return false;
        return id != null && id.equals(that.id);
    }

    /**
     * Le hashcode est basé sur l'ID pour les entités persistées.
     */
    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }

    /**
     * Représentation textuelle de la catégorie pour le débogage.
     */
    @Override
    public String toString() {
        return "Categorie{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", capacite=" + capacite +
                ", eventId=" + (event != null ? event.getId() : "null") +
                '}';
    }
}

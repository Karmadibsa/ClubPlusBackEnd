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
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories") // Bon nom de table
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.") // Ajout de taille
    @Column(nullable = false, length = 100) // Contrainte DB + taille
    @JsonView(GlobalView.Base.class)
    private String nom;

    @NotNull(message = "La capacité est obligatoire.")
    @Min(value = 0, message = "La capacité ne peut pas être négative.") // Très bien
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Integer capacite; // Capacité spécifique à cette catégorie

    // --- Relation vers l'Événement ---
    @NotNull(message = "La catégorie doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, // ESSENTIEL pour éviter de charger l'événement inutilement
            optional = false)
    @JoinColumn(name = "event_id", nullable = false) // FK non nulle, parfait
    @JsonView(GlobalView.CategorieView.class)
    private Event event;

    // --- Relation vers les Réservations ---
    @OneToMany(mappedBy = "categorie", // Lien correct vers Reservation.categorie
            cascade = CascadeType.ALL, // Supprimer les résas si la catégorie est supprimée
            orphanRemoval = true,    // Supprimer une résa si retirée de la liste
            fetch = FetchType.LAZY)      // ESSENTIEL pour éviter de charger toutes les résas
    @JsonView(GlobalView.CategorieView.class)
    private List<Reservation> reservations = new ArrayList<>(); // Bonne initialisation


    // --- Méthodes Calculées ---

    /**
     * Calcule le nombre de places actuellement réservées ET CONFIRMEES dans cette catégorie.
     * Les réservations annulées ou utilisées ne sont pas comptées.
     *
     * @return Le nombre de réservations confirmées.
     */
    @Transient
    @JsonView({GlobalView.EventView.class, GlobalView.CategorieView.class})
    public int getPlaceReserve() {
        List<Reservation> currentReservations = getReservations(); // Utilise le getter (LAZY)
        if (currentReservations == null) {
            return 0;
        }
        // Filtrer par statut avant de compter
        return (int) currentReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRME)
                .count();
    }

    /**
     * Calcule le nombre de places encore disponibles DANS CETTE CATEGORIE.
     *
     * @return Le nombre de places disponibles, ou 0 si la capacité n'est pas définie.
     */
    @Transient // Ne pas mapper en DB.
    @JsonView({GlobalView.EventView.class, GlobalView.CategorieView.class})
    public int getPlaceDisponible() {
        // Utilisation des getters pour la robustesse.
        Integer currentCapacite = getCapacite();
        if (currentCapacite == null || currentCapacite < 0) { // Gérer aussi le cas < 0 pour la cohérence
            return 0;
        }
        // Appelle l'autre méthode transiente, c'est bien.
        return currentCapacite - getPlaceReserve();
    }

    // --- equals et hashCode (Basés sur l'ID pour les entités persistées) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categorie categorie = (Categorie) o;
        // Ne sont égaux que si les IDs sont non nuls et identiques
        return id != null && Objects.equals(id, categorie.id);
    }

    @Override
    public int hashCode() {
        // Utilise l'ID s'il existe, sinon le hash de la classe (pour les objets non persistés)
        return id != null ? Objects.hashCode(id) : Objects.hashCode(getClass());
    }
}

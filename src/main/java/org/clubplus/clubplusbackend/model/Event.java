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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events") // Bon nom de table
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @NotBlank(message = "Le nom de l'événement ne peut pas être vide.")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères.")
    @Column(nullable = false, length = 150)
    @JsonView(GlobalView.Base.class)
    private String nom;

    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    @FutureOrPresent(message = "La date de début doit être dans le présent ou le futur.") // Validation logique
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class) // Date de début souvent utile dans les listes
    private LocalDateTime start;

    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    @FutureOrPresent(message = "La date de fin doit être dans le présent ou le futur.")
    @Column(nullable = false)
    @JsonView(GlobalView.EventView.class)
    private LocalDateTime end;
    // Note: La validation end > start devra être faite dans le Service.

    @NotBlank(message = "La description ne peut pas être vide.")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères.") // Augmenté la taille
    @Column(nullable = false, length = 2000) // Longueur suffisante pour une description
    @JsonView(GlobalView.EventView.class)
    private String description;

    // Location peut être optionnelle
    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères.")
    @Column(length = 255) // Taille standard si fournie
    @JsonView(GlobalView.Base.class) // Location utile dans les listes
    private String location;

    // --- Champs pour Soft Delete ---
    @NotNull // Important pour @Where
    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Boolean actif = true; // Statut de l'événement

    @Column(name = "desactivation_date") // Nom de colonne explicite
    private LocalDateTime desactivationDate; // Date de désactivation/annulation
    // --- Relations ---

    // Relation vers Categorie : CORRECT
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(GlobalView.EventView.class) // OK, mais attention à la vue de Categorie utilisée ici
    private List<Categorie> categories = new ArrayList<>();

    // Relation vers Club (Organisateur) : CORRECT
    @NotNull(message = "L'événement doit avoir un organisateur.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id", nullable = false)
    @JsonView(GlobalView.EventView.class) // OK, mais attention à la vue de Club utilisée ici (Base est préférable)
    private Club organisateur;

    // Relation vers Notation : CORRECT
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore // Moins prioritaire à afficher par défaut, peut être récupéré via endpoint dédié
    private List<Notation> notations = new ArrayList<>();


    // --- Méthode de Préparation à la Désactivation ---

    /**
     * Prépare l'entité Event pour la désactivation (annulation).
     * Modifie le nom pour indiquer l'état et met à jour la date de désactivation.
     * À appeler AVANT de sauvegarder l'événement désactivé.
     *
     * @throws IllegalStateException si l'ID de l'événement est null.
     */
    public void prepareForDeactivation() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible de désactiver un événement sans ID persistant.");
        }

        // Modifier le nom pour indiquer clairement l'état (ex: Annulé)
        if (!this.nom.startsWith("[Annulé]")) { // Évite double préfixe
            this.nom = "[Annulé] " + this.nom;
        }

        // Les dates (start, end), description, location restent inchangées pour l'historique.

        // Mettre à jour la date de désactivation
        this.desactivationDate = LocalDateTime.now();

    }
    // --- Méthodes Calculées (@Transient) ---

    /**
     * Calcule la capacité totale de l'événement en additionnant les capacités de toutes ses catégories.
     *
     * @return La capacité totale.
     */
    @Transient // Ne pas mapper en DB
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class}) // Utile dans les deux vues
    public int getPlaceTotal() {
        List<Categorie> currentCategories = getCategories(); // Utilise le getter (LAZY)
        if (currentCategories == null) return 0;
        return currentCategories.stream()
                // Assure que getCapacite() sur Categorie ne retourne pas null
                .mapToInt(cat -> cat.getCapacite() != null ? cat.getCapacite() : 0)
                .sum();
    }

    /**
     * Calcule le nombre total de places réservées pour l'événement
     * en additionnant les places réservées de toutes ses catégories.
     *
     * @return Le nombre total de places réservées.
     */
    @Transient // Ne pas mapper en DB
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class}) // Utile dans les deux vues
    public int getPlaceReserve() {
        List<Categorie> currentCategories = getCategories(); // Utilise le getter (LAZY)
        if (currentCategories == null) return 0;
        // Appelle la méthode calculée de Categorie
        return currentCategories.stream()
                .mapToInt(Categorie::getPlaceReserve)
                .sum();
    }

    /**
     * Calcule le nombre total de places disponibles pour l'événement.
     *
     * @return Le nombre total de places disponibles.
     */
    @Transient // Ne pas mapper en DB
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class}) // Utile dans les deux vues
    public int getPlaceDisponible() {
        // Utilise les autres méthodes @Transient
        return getPlaceTotal() - getPlaceReserve();
    }

    // --- equals et hashCode (Basés sur l'ID) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id != null && Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : Objects.hashCode(getClass());
    }
}

package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Entité JPA représentant un club.
 * <p>
 * Gère les informations principales, les adhésions, les événements et la suppression logique (soft delete)
 * via un champ 'actif'. L'annotation @Where filtre automatiquement les clubs inactifs des requêtes.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "actif = true")
@Table(name = "club")
public class Club {

    /**
     * Identifiant unique du club, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom officiel du club.
     */
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit contenir entre 2 et 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Date de création officielle du club (fournie par l'utilisateur).
     */
    @NotNull(message = "La date de création est obligatoire.")
    @PastOrPresent(message = "La date de création doit être dans le passé ou aujourd'hui.")
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.ClubView.class)
    private LocalDate date_creation;

    /**
     * Date d'inscription du club dans le système.
     */
    @NotNull(message = "La date d'inscription est obligatoire.")
    @PastOrPresent(message = "La date d'inscription doit être dans le passé ou aujourd'hui.")
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.ClubView.class)
    private LocalDate date_inscription;

    // --- Adresse ---

    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView({GlobalView.ClubView.class, GlobalView.ReservationView.class, GlobalView.ClubMembreView.class})
    private String numero_voie;

    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView({GlobalView.ClubView.class, GlobalView.ReservationView.class, GlobalView.ClubMembreView.class})
    private String rue;

    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView({GlobalView.ClubView.class, GlobalView.ReservationView.class, GlobalView.ClubMembreView.class})
    private String codepostal;

    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.Base.class)
    private String ville;

    // --- Contact ---

    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    @Column(nullable = false, length = 20)
    @JsonView({GlobalView.ClubView.class, GlobalView.ReservationView.class, GlobalView.ClubMembreView.class})
    private String telephone;

    @NotBlank(message = "L'email du club est obligatoire.")
    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Le format de l'email est invalide.")
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.")
    @Column(nullable = false, unique = true, length = 254)
    @JsonView({GlobalView.ClubView.class, GlobalView.ReservationView.class, GlobalView.ClubMembreView.class})
    private String email;

    /**
     * Code unique du club, généré automatiquement après la création.
     */
    @Column(unique = true, length = 9)
    @JsonView({GlobalView.Base.class, GlobalView.ClubView.class})
    private String codeClub;

    // --- Soft Delete ---

    /**
     * Indique si le club est actif (true) ou désactivé (false).
     */
    @NotNull
    @Column(nullable = false)
    private Boolean actif = true;

    /**
     * Date de désactivation du club. Reste null tant que le club est actif.
     */
    @Column(name = "desactivation_date")
    private Instant desactivationDate;

    // --- Relations ---

    /**
     * Ensemble des adhésions à ce club. Ignoré en JSON pour éviter les boucles.
     */
    @OneToMany(mappedBy = "club",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Adhesion> adhesions = new HashSet<>();

    /**
     * Liste des événements organisés par ce club.
     */
    @OneToMany(mappedBy = "organisateur",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonView(GlobalView.ClubView.class)
    private List<Event> evenements = new ArrayList<>();

    /**
     * Prépare le club à la désactivation en modifiant ses informations pour refléter son statut inactif.
     */
    public void prepareForDeactivation() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible de désactiver un club non persisté.");
        }
        if (!this.nom.startsWith("[Désactivé]")) {
            this.nom = "[Désactivé] " + this.nom;
        }
        this.email = "inactive+" + this.id + "@clubplus.invalid";
        this.desactivationDate = Instant.now();
    }

    /**
     * L'égalité est basée sur l'ID. Deux clubs sont égaux si leurs IDs sont les mêmes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Club club)) return false;
        return id != null && id.equals(club.id);
    }

    /**
     * Le hashcode est basé sur l'ID pour les entités persistées.
     */
    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }
}

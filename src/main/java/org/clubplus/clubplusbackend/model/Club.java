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
 * Entité JPA représentant un club sportif dans l'application ClubPlus.
 * Cette entité contient les informations principales du club, ses coordonnées,
 * ses adhésions, et les événements qu'il organise.
 *
 * <p>La suppression logique est gérée via le champ {@code actif} et la date de désactivation.
 * Une contrainte {@code @Where} filtre automatiquement les clubs désactivés dans les requêtes.</p>
 *
 * <p>Les relations vers {@link Adhesion} et {@link Event} sont chargées paresseusement
 * et gérées avec cascade et orphanRemoval pour assurer la cohérence des données.</p>
 *
 * @see Adhesion
 * @see Event
 * @see GlobalView
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "actif = true") // Filtre automatique pour exclure les clubs désactivés
@Table(name = "club") // Nom explicite de la table (ajuster si nécessaire)
public class Club {

    /**
     * Identifiant unique du club, généré automatiquement par la base de données.
     * Visible dans la vue JSON de base.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom officiel du club.
     * Ne peut pas être vide, doit contenir entre 2 et 100 caractères.
     * Visible dans la vue JSON de base.
     */
    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit contenir entre 2 et 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Date de création officielle du club.
     * Doit être une date passée ou présente.
     * Non modifiable après création.
     * Visible dans la vue détaillée du club.
     */
    @NotNull(message = "La date de création est obligatoire.")
    @PastOrPresent(message = "La date de création doit être dans le passé ou aujourd'hui.")
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.ClubView.class)
    private LocalDate date_creation;

    /**
     * Date d'inscription du club dans le système.
     * Doit être une date passée ou présente.
     * Non modifiable après création.
     * Visible dans la vue détaillée du club.
     *
     * <p>À clarifier si différente de {@code date_creation} ou à fusionner.</p>
     */
    @NotNull(message = "La date d'inscription est obligatoire.")
    @PastOrPresent(message = "La date d'inscription doit être dans le passé ou aujourd'hui.")
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.ClubView.class)
    private Instant date_inscription;

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
    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Le format de l'email est invalide. Il doit contenir un domaine valide (ex: .com, .fr).")
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.")
    @Column(nullable = false, unique = true, length = 254)
    @JsonView({GlobalView.ClubView.class, GlobalView.ReservationView.class, GlobalView.ClubMembreView.class})
    private String email;


    // --- Code Club ---

    /**
     * Code unique généré automatiquement après persistance.
     * Exemple : "CLUB-0001".
     * Non modifiable.
     * Visible dans les vues de base et détaillée du club.
     */
    @Column(unique = true, length = 9)
    @JsonView({GlobalView.Base.class, GlobalView.ClubView.class})
    private String codeClub;

    // --- Soft Delete ---

    /**
     * Indique si le club est actif.
     * Utilisé pour la suppression logique.
     * Ne peut pas être null.
     */
    @NotNull(message = "Le statut 'actif' du club est obligatoire et ne peut pas être nul.")
    @Column(nullable = false)
    private Boolean actif = true;

    /**
     * Date et heure de désactivation du club.
     * Null si le club est actif.
     */
    @Column(name = "desactivation_date")
    private Instant desactivationDate;

    // --- Relations ---

    /**
     * Ensemble des adhésions associées à ce club.
     * Chargé paresseusement.
     * Cascade et orphanRemoval assurent la cohérence.
     * Ignoré en sérialisation JSON pour éviter les boucles infinies et fuites de données.
     */
    @OneToMany(mappedBy = "club",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Adhesion> adhesions = new HashSet<>();

    /**
     * Liste des événements organisés par ce club.
     * Chargé paresseusement.
     * Cascade et orphanRemoval assurent la cohérence.
     * Visible dans la vue détaillée du club.
     */
    @OneToMany(mappedBy = "organisateur",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonView(GlobalView.ClubView.class)
    private List<Event> evenements = new ArrayList<>();

    // --- Méthodes métier ---

    /**
     * Prépare le club pour une désactivation (soft delete).
     * Modifie le nom et l'email pour refléter l'état inactif,
     * met à jour la date de désactivation.
     * Ne modifie pas les relations (adhésions, événements),
     * la gestion des événements futurs doit être faite au niveau service.
     *
     * @throws IllegalStateException si le club n'a pas d'ID persistant.
     */
    public void prepareForDeactivation() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible de désactiver un club sans ID persistant.");
        }
        if (!this.nom.startsWith("[Désactivé]")) {
            this.nom = "[Désactivé] " + this.nom;
        }
        this.email = "inactive+" + this.id + "@clubplus.invalid";
        this.desactivationDate = Instant.now();
    }

    // --- equals et hashCode basés sur l'ID ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Club club)) return false;
        return id != null && id.equals(club.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : Objects.hashCode(getClass());
    }
}

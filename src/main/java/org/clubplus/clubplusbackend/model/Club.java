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

import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class) // OK pour la vue de base
    private Integer id;

    @NotBlank(message = "Le nom du club ne peut pas être vide.")
    @Size(min = 2, max = 100, message = "Le nom du club doit contenir entre 2 et 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.Base.class) // OK pour la vue de base
    private String nom;

    // Date de création/inscription du club dans le système
    @NotNull(message = "La date de création est obligatoire.")
    @PastOrPresent(message = "La date de création doit être dans le passé ou aujourd'hui.") // Validation logique
    @Column(nullable = false, updatable = false) // Non modifiable après création
    @JsonView(GlobalView.ClubView.class) // Visible dans la vue détaillée
    private LocalDate date_creation;

    // Redondant avec date_creation? Si c'est la même chose, on peut en supprimer une.
    // Si elle a un sens différent (ex: date d'activation), renommer et commenter.
    // Supposons qu'elle soit identique pour l'instant.
    @NotNull(message = "La date d'inscription est obligatoire.")
    @PastOrPresent(message = "La date d'inscription doit être dans le passé ou aujourd'hui.")
    @Column(nullable = false, updatable = false)
    @JsonView(GlobalView.ClubView.class)
    private LocalDate date_inscription; // À clarifier si différente de date_creation

    // --- Champs Adresse ---
    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView(GlobalView.ClubView.class)
    private String numero_voie;

    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.ClubView.class)
    private String rue;

    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView(GlobalView.ClubView.class)
    private String codepostal;

    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    // Mettre la ville dans Base est souvent utile pour les listes.
    @JsonView(GlobalView.Base.class)
    private String ville;

    // --- Champs Contact ---
    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    @Column(nullable = false, length = 20)
    @JsonView(GlobalView.ClubView.class)
    private String telephone;

    @NotBlank(message = "L'email du club est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.")
    @Column(nullable = false, unique = true, length = 254) // Unicité + taille
    @JsonView(GlobalView.ClubView.class)
    private String email;

    // --- Code Club ---
    // Généré après persistance. unique=true est essentiel.
    @Column(unique = true, length = 9, updatable = false) // Ex: "CLUB-0001", non modifiable
    // La visibilité dans toutes ces vues peut être discutable. Est-ce nécessaire partout ?
    // Le mettre dans Base.class ou ClubView.class est généralement suffisant.
    @JsonView({GlobalView.Base.class, GlobalView.ClubView.class}) // Simplifié la visibilité
    private String codeClub;

    // --- Relations ---

    // Relation vers Adhesion: C'est LE moyen de lier Membre et Club. Parfait.
    @OneToMany(mappedBy = "club",
            cascade = CascadeType.ALL, // Si club supprimé, adhésions supprimées. Logique.
            orphanRemoval = true,    // Si on retire une Adhesion de ce Set, elle est supprimée. OK.
            fetch = FetchType.LAZY)      // ESSENTIEL: Ne pas charger toutes les adhésions par défaut.
    @JsonIgnore // TRÈS IMPORTANT: Ne jamais sérialiser cette liste directement pour éviter boucles/fuites.
    private Set<Adhesion> adhesions = new HashSet<>(); // Bonne initialisation.

    // Relation vers Event: Le club organise des événements.
    @OneToMany(mappedBy = "organisateur",
            cascade = CascadeType.ALL, // Si club supprimé, ses événements sont supprimés. Logique.
            orphanRemoval = true,
            fetch = FetchType.LAZY)      // AJOUT ESSENTIEL: Charger les événements paresseusement.
    // JsonView: Attention, charger une liste d'événements peut être lourd.
    // S'assurer que la vue pour Event dans ce contexte est restreinte (ex: GlobalView.Base).
    @JsonView(GlobalView.ClubView.class)
    private List<Event> evenements = new ArrayList<>(); // Bonne initialisation.


    // Méthode pour générer le code après la première persistance.
    @PostPersist
    public void generateCode() {
        // Vérifie que l'ID a été généré et que codeClub n'est pas déjà défini (sécurité)
        if (this.id != null && this.codeClub == null) {
            this.codeClub = String.format("CLUB-%04d", this.id);

        }
    }

    // --- equals et hashCode (Basés sur l'ID) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Club club = (Club) o;
        return id != null && Objects.equals(id, club.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : Objects.hashCode(getClass());
    }
}

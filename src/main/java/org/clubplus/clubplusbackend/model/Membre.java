package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Membre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class) // Visible dans la vue de base
    private Integer id;

    @NotBlank(message = "Le nom ne peut pas être vide.") // Doit contenir au moins un caractère non blanc
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères.") // Longueur raisonnable
    @Column(nullable = false, length = 50) // Contrainte DB + taille
    @JsonView(GlobalView.Base.class)
    private String nom;

    @NotBlank(message = "Le prénom ne peut pas être vide.")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères.")
    @Column(nullable = false, length = 50)
    @JsonView(GlobalView.Base.class)
    private String prenom;

    @NotNull(message = "La date de naissance est obligatoire.") // Utiliser @NotNull pour les dates/objets
    @Past(message = "La date de naissance doit être dans le passé.") // Validation logique
    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class) // Visible dans la vue détaillée
    private LocalDate date_naissance;

    // Date d'inscription gérée par le système, pas d'annotation de validation d'entrée nécessaire
    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private LocalDate date_inscription;

    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView(GlobalView.MembreView.class)
    private String numero_voie;

    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.MembreView.class)
    private String rue;

    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView(GlobalView.MembreView.class)
    private String codepostal;

    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView(GlobalView.MembreView.class)
    private String ville;

    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    @Column(nullable = false, length = 20)
    @JsonView(GlobalView.MembreView.class)
    private String telephone;

    @NotBlank(message = "L'email est obligatoire.") // Assure non-vide
    @Email(message = "Le format de l'email est invalide.") // Valide le format standard
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.") // Limite standard
    @Column(nullable = false, unique = true, length = 254) // Contrainte DB (unicité + taille)
    @JsonView(GlobalView.MembreView.class) // Visible dans la vue détaillée
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    @Size(max = 100, message = "Le mot de passe ne doit pas dépasser 100 caractères.")
    @Column(nullable = false) // Le hash sera stocké, taille dépend du hash (BCrypt ~60 chars)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // N'autoriser que l'écriture (désérialisation), pas la lecture (sérialisation)
    private String password; // Mot de passe en clair pour l'entrée/validation, hashé avant sauvegarde

    @Enumerated(EnumType.STRING) // Stocke "ADMIN", "MEMBRE", etc.
    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class) // Visible dans la vue détaillée
    private Role role;

    // --- Relations ---
    // Pas besoin de validation directe sur les collections ici,
    // la validation se fait sur les objets DANS les collections (ex: une Réservation doit être valide)

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.MembreView.class)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(GlobalView.MembreView.class)
    private Set<Adhesion> adhesions = new HashSet<>();

    @OneToMany(mappedBy = "envoyeur",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore // Ne pas inclure par défaut dans le JSON du Membre
    private Set<DemandeAmi> demandesEnvoyees = new HashSet<>();

    @OneToMany(mappedBy = "recepteur",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore // Ne pas inclure par défaut dans le JSON du Membre
    private Set<DemandeAmi> demandesRecues = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "amitie", // Nom de la table de jointure en BDD
            joinColumns = @JoinColumn(name = "membre_id"), // Clé étrangère vers l'ID de CE membre
            inverseJoinColumns = @JoinColumn(name = "ami_id") // Clé étrangère vers l'ID de l'AUTRE membre (l'ami)
    )
    @JsonIgnore // Ne pas inclure par défaut dans le JSON (CRUCIAL pour éviter boucles et fuites)
    private Set<Membre> amis = new HashSet<>();

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Notation> notations = new ArrayList<>();

    // --- equals & hashCode basés sur l'ID (bonne pratique) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membre membre = (Membre) o;
        // Si l'ID est null (nouvelle entité non persistée), ne pas considérer égaux basé sur ID null.
        // Seules les entités persistées avec le même ID sont égales.
        return id != null && Objects.equals(id, membre.id);
    }

    @Override
    public int hashCode() {
        // Utiliser une constante si l'ID est null pour éviter hashCode=0 pour toutes les nouvelles entités
        return id != null ? Objects.hashCode(id) : Objects.hashCode(getClass());
    }

    public void addAmi(Membre ami) {
        if (this.amis == null) this.amis = new HashSet<>(); // Défensif
        if (ami.getAmis() == null) ami.setAmis(new HashSet<>()); // Défensif
        this.amis.add(ami);
        ami.getAmis().add(this);
    }

    public void removeAmi(Membre ami) {
        if (this.amis != null) {
            this.amis.remove(ami);
        }
        if (ami.getAmis() != null) {
            ami.getAmis().remove(this);
        }

    }
}

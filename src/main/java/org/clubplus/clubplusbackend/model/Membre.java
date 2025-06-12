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
import org.clubplus.clubplusbackend.view.GlobalView;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Entité JPA représentant un membre (utilisateur) de l'application.
 * <p>
 * Gère les informations personnelles, le rôle, le statut (actif/inactif),
 * ainsi que les relations avec les clubs, les amis et les événements.
 * L'annotation @Where filtre automatiquement les membres inactifs des requêtes.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "actif = true")
@Table(name = "membre")
public class Membre {

    /**
     * Identifiant unique du membre, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.Base.class, GlobalView.ProfilView.class})
    private Integer id;

    /**
     * Nom de famille du membre.
     */
    @NotBlank(message = "Le nom ne peut pas être vide.")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères.")
    @Column(nullable = false, length = 50)
    @JsonView({GlobalView.Base.class, GlobalView.ProfilView.class})
    private String nom;

    /**
     * Prénom du membre.
     */
    @NotBlank(message = "Le prénom ne peut pas être vide.")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères.")
    @Column(nullable = false, length = 50)
    @JsonView({GlobalView.Base.class, GlobalView.ProfilView.class})
    private String prenom;

    /**
     * Date de naissance du membre. Doit être dans le passé.
     */
    @NotNull(message = "La date de naissance est obligatoire.")
    @Past(message = "La date de naissance doit être dans le passé.")
    @Column(nullable = false)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private LocalDate date_naissance;

    /**
     * Date d'inscription du membre dans le système.
     */
    @NotNull
    @Column(nullable = false, updatable = false)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private LocalDate date_inscription = LocalDate.now();

    // --- Contact ---

    /**
     * Numéro de téléphone du membre.
     */
    @NotBlank(message = "Le numéro de téléphone est obligatoire.")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    @Column(nullable = false, length = 20)
    @JsonView({GlobalView.MembreView.class, GlobalView.ReservationView.class, GlobalView.ProfilView.class})
    private String telephone;


    /**
     * Adresse email du membre, utilisée comme identifiant de connexion.
     * Doit être unique.
     */
    @NotBlank(message = "L'email est obligatoire.")
    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Le format de l'email est invalide.")
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.")
    @Column(nullable = false, unique = true, length = 254)
    @JsonView({GlobalView.MembreView.class, GlobalView.ReservationView.class, GlobalView.ProfilView.class})
    private String email;

    // --- Identifiants Système ---

    /**
     * Code unique pour les demandes d'ami. Généré automatiquement après création.
     */
    @Column(unique = true, length = 11)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private String codeAmi;

    /**
     * Mot de passe du membre.
     * Ce champ est uniquement utilisé pour l'écriture (inscription/modification).
     * Il n'est jamais retourné dans les réponses API grâce à @JsonProperty(access = WRITE_ONLY).
     */
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[{}\\]:;',?/*~$^+=<>]).{8,100}$",
            message = "Le mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial.")
    @Column(nullable = false, length = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // --- Rôle et Statut ---

    /**
     * Rôle de l'utilisateur (MEMBRE, RESERVATION, ADMIN), déterminant ses permissions.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private Role role;

    /**
     * Indique si le compte est actif (true) ou désactivé/anonymisé (false).
     */
    @NotNull
    @Column(nullable = false)
    private Boolean actif = true;

    /**
     * Date d'anonymisation du compte. Reste null tant que le membre est actif.
     */
    private Instant anonymizeDate;

    /**
     * Indique si l'email du membre a été vérifié.
     */
    @Column(nullable = false)
    private boolean verified = false;

    /**
     * Token unique pour la vérification de l'email.
     */
    @Column(unique = true)
    private String verificationToken;

    /**
     * Token unique pour la réinitialisation du mot de passe.
     */
    @Column(unique = true)
    private String resetPasswordToken;

    /**
     * Date d'expiration du token de réinitialisation de mot de passe.
     */
    private Instant resetPasswordTokenExpiryDate;


    // --- Relations ---

    /**
     * Liste des réservations effectuées par ce membre.
     */
    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(GlobalView.MembreView.class)
    private List<Reservation> reservations = new ArrayList<>();

    /**
     * Ensemble des adhésions de ce membre à des clubs.
     */
    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(GlobalView.MembreView.class)
    private Set<Adhesion> adhesions = new HashSet<>();

    /**
     * Ensemble des demandes d'ami envoyées par ce membre.
     * Ignoré en JSON pour éviter les boucles.
     */
    @OneToMany(mappedBy = "envoyeur",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<DemandeAmi> demandesEnvoyees = new HashSet<>();

    /**
     * Ensemble des demandes d'ami reçues par ce membre.
     * Ignoré en JSON pour éviter les boucles.
     */
    @OneToMany(mappedBy = "recepteur",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<DemandeAmi> demandesRecues = new HashSet<>();

    /**
     * Ensemble des amis de ce membre. Relation bidirectionnelle gérée par une table de jointure.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "amitie",
            joinColumns = @JoinColumn(name = "membre_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_amitie_membre")),
            inverseJoinColumns = @JoinColumn(name = "ami_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_amitie_ami"))
    )
    @JsonView(GlobalView.MembreView.class)
    private Set<Membre> amis = new HashSet<>();

    /**
     * Liste des notations laissées par ce membre.
     * Ignorée en JSON.
     */
    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Notation> notations = new ArrayList<>();

    /**
     * Callback JPA pour générer le code ami après la première sauvegarde.
     */
    @PostPersist
    public void generateCodeAmi() {
        if (this.id != null && this.codeAmi == null) {
            this.codeAmi = String.format("AMIS-%06d", this.id);
        }
    }

    /**
     * Ajoute un ami et maintient la relation bidirectionnelle.
     *
     * @param ami Le membre à ajouter comme ami.
     */
    public void addAmi(Membre ami) {
        Objects.requireNonNull(ami, "L'ami à ajouter ne peut pas être null.");
        this.amis.add(ami);
        ami.getAmis().add(this);
    }

    /**
     * Retire un ami et maintient la relation bidirectionnelle.
     *
     * @param ami Le membre à retirer des amis.
     */
    public void removeAmi(Membre ami) {
        Objects.requireNonNull(ami, "L'ami à retirer ne peut pas être null.");
        this.amis.remove(ami);
        ami.getAmis().remove(this);
    }

    /**
     * Anonymise les données personnelles identifiables de ce membre.
     * Cette méthode est une étape clé du processus de suppression de compte (soft delete).
     */
    public void anonymizeData() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible d'anonymiser un membre non persisté.");
        }

        String anonymizedSuffix = "_id" + this.id;
        this.nom = "Utilisateur";
        this.prenom = "Anonyme" + anonymizedSuffix;
        this.date_naissance = LocalDate.of(1990, 1, 1);
        this.telephone = "0000000000";
        this.email = "anonymized_" + this.id + "@example.com";
        this.password = "$2a$10$invalidHashPlaceholder." + UUID.randomUUID();
        this.codeAmi = "ANON-" + this.id;
        this.role = Role.ANONYME;
        this.anonymizeDate = Instant.now();

        // Nettoyer les relations sociales
        if (this.demandesEnvoyees != null) this.demandesEnvoyees.clear();
        if (this.demandesRecues != null) this.demandesRecues.clear();
        if (this.amis != null && !this.amis.isEmpty()) {
            new HashSet<>(this.amis).forEach(this::removeAmi);
        }
    }

    /**
     * L'égalité est basée sur l'ID. Deux membres sont égaux si leurs IDs sont les mêmes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Membre membre)) return false;
        return id != null && id.equals(membre.id);
    }

    /**
     * Le hashcode est basé sur l'ID pour les entités persistées.
     */
    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }

    /**
     * Représentation textuelle du membre pour le débogage.
     */
    @Override
    public String toString() {
        return "Membre{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", actif=" + actif +
                '}';
    }
}

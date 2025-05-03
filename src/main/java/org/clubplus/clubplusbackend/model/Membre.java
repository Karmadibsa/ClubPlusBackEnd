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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entité JPA représentant un membre (utilisateur) de l'application ClubPlus.
 * Contient les informations personnelles, les coordonnées, le rôle, le statut (actif/anonymisé),
 * ainsi que les relations avec les clubs (via {@link Adhesion}), les événements (via {@link Reservation}),
 * les autres membres (amitiés via {@link DemandeAmi} et la table `amitie`), et les notations.
 *
 * <p>Une stratégie de suppression logique (soft delete) est implémentée via le champ {@code actif}
 * et la clause {@code @Where}, qui filtre automatiquement les membres inactifs des requêtes SELECT.
 * L'anonymisation des données est gérée par la méthode {@link #anonymizeData()}.</p>
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont basés sur l'identifiant unique {@code id}.</p>
 *
 * @see Role
 * @see Adhesion
 * @see Reservation
 * @see DemandeAmi
 * @see Notation
 * @see GlobalView
 */
@Entity // Marque comme entité JPA.
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
@NoArgsConstructor // Lombok: Constructeur sans argument (requis par JPA).
@AllArgsConstructor // Lombok: Constructeur avec tous les arguments (utile pour les tests/fixtures).
// Applique un filtre global pour exclure les membres inactifs (supprimés logiquement)
// de TOUTES les requêtes SELECT par défaut effectuées via JPA/Hibernate sur cette entité.
@Where(clause = "actif = true")
@Table(name = "membre") // Nom explicite de la table.
public class Membre {

    /**
     * Identifiant unique du membre, généré automatiquement.
     * Visible dans la vue JSON de base.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom de famille du membre.
     * Obligatoire, non vide, et de longueur contrainte.
     * Visible dans la vue JSON de base.
     */
    @NotBlank(message = "Le nom ne peut pas être vide.")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères.")
    @Column(nullable = false, length = 50)
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Prénom du membre.
     * Obligatoire, non vide, et de longueur contrainte.
     * Visible dans la vue JSON de base.
     */
    @NotBlank(message = "Le prénom ne peut pas être vide.")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères.")
    @Column(nullable = false, length = 50)
    @JsonView(GlobalView.Base.class)
    private String prenom;

    /**
     * Date de naissance du membre.
     * Obligatoire et doit être une date dans le passé.
     * Visible dans les vues détaillées (MembreView, ProfilView).
     */
    @NotNull(message = "La date de naissance est obligatoire.")
    @Past(message = "La date de naissance doit être dans le passé.") // Validation logique.
    @Column(nullable = false)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private LocalDate date_naissance;

    /**
     * Date d'inscription du membre dans le système.
     * Généralement définie par le système lors de la création.
     * Doit être non nulle (contrainte BDD).
     * Visible dans les vues détaillées (MembreView, ProfilView).
     */
    @NotNull // Assure qu'une date est présente lors de la persistance.
    @Column(nullable = false, updatable = false) // Non modifiable après création.
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private LocalDate date_inscription = LocalDate.now(); // Initialisation par défaut, peut être surchargée.

    // --- Adresse ---

    /**
     * Numéro dans la voie de l'adresse du membre.
     */
    @NotBlank(message = "Le numéro de voie est obligatoire.")
    @Size(max = 10, message = "Le numéro de voie ne doit pas dépasser 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private String numero_voie;

    /**
     * Nom de la rue de l'adresse du membre.
     */
    @NotBlank(message = "La rue est obligatoire.")
    @Size(max = 100, message = "La rue ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private String rue;

    /**
     * Code postal de l'adresse du membre.
     */
    @NotBlank(message = "Le code postal est obligatoire.")
    @Size(min = 3, max = 10, message = "Le code postal doit contenir entre 3 et 10 caractères.")
    @Column(nullable = false, length = 10)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private String codepostal;

    /**
     * Ville de l'adresse du membre.
     */
    @NotBlank(message = "La ville est obligatoire.")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private String ville;

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
     * Adresse email du membre.
     * Utilisée comme identifiant unique pour la connexion.
     * Doit être unique dans la base de données et au format email valide.
     */
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.") // Validation de format.
    @Size(max = 254, message = "L'email ne doit pas dépasser 254 caractères.") // Limite standard RFC.
    @Column(nullable = false, unique = true, length = 254) // Contraintes BDD: non null, unique, taille max.
    @JsonView({GlobalView.MembreView.class, GlobalView.ReservationView.class, GlobalView.ProfilView.class})
    private String email;

    // --- Identifiants Système ---

    /**
     * Code unique permettant aux autres membres d'envoyer une demande d'ami à cet utilisateur.
     * Généré automatiquement après la première persistance de l'entité (via {@link #generateCodeAmi()}).
     * Format: "AMIS-XXXXXX". Non modifiable.
     * Visible dans la vue détaillée du profil de l'utilisateur.
     */
    @Column(unique = true, length = 11, updatable = false) // Contraintes BDD: unique, taille fixe, non modifiable.
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private String codeAmi;

    /**
     * Mot de passe du membre.
     * Ce champ est destiné à recevoir le mot de passe en clair lors de l'inscription ou de la modification.
     * Il est obligatoire et doit respecter une politique de complexité définie par le {@code @Pattern}.
     * **IMPORTANT:** Il est marqué comme {@code WRITE_ONLY} par {@code @JsonProperty} pour empêcher
     * sa sérialisation (son inclusion dans les réponses JSON). Il doit être haché (ex: avec BCrypt)
     * par la couche service avant d'être persisté en base de données.
     */
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[{}\\]:;',?/*~$^+=<>]).{8,100}$",
            message = "Le mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial (!@#&()...)")
    @Column(nullable = false, length = 100) // La longueur BDD doit accommoder le hash (BCrypt ~60 chars).
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Sécurité: Empêche la fuite du hash dans les API.
    private String password;

    // --- Rôle et Statut ---

    /**
     * Rôle principal de l'utilisateur dans l'application (ex: MEMBRE, RESERVATION, ADMIN).
     * Détermine les permissions d'accès. Stocké comme une chaîne de caractères en BDD.
     * Visible dans les vues détaillées.
     *
     * @see Role
     */
    @Enumerated(EnumType.STRING) // Stocke le nom de l'enum (plus lisible).
    @Column(nullable = false, length = 20) // Contrainte BDD: non null, taille pour les noms de rôle.
    @JsonView({GlobalView.MembreView.class, GlobalView.ProfilView.class})
    private Role role;

    /**
     * Indicateur du statut actif du membre.
     * {@code true} si le compte est actif, {@code false} s'il a été désactivé/anonymisé (soft delete).
     * Utilisé par la clause {@code @Where} pour filtrer les membres actifs par défaut.
     */
    @NotNull // Le statut doit toujours être défini.
    @Column(nullable = false)
    private Boolean actif = true; // Initialisé à 'true' par défaut.

    /**
     * Date et heure auxquelles les données du membre ont été anonymisées.
     * Reste {@code null} tant que le membre est actif.
     */
    private LocalDateTime anonymizeDate;

    // --- Relations ---

    /**
     * Liste des réservations effectuées par ce membre.
     * Relation One-to-Many : Un membre peut avoir plusieurs réservations.
     * Chargement par défaut (EAGER possible ici si souvent nécessaire, mais LAZY reste plus sûr).
     * Cascade ALL et orphanRemoval lient le cycle de vie des réservations à celui du membre.
     * Visible dans la vue détaillée du membre.
     *
     * <p>Note: Charger cette liste peut être coûteux si un membre a beaucoup de réservations.</p>
     */
    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // Préférer LAZY
    @JsonView(GlobalView.MembreView.class)
    private List<Reservation> reservations = new ArrayList<>();

    /**
     * Ensemble des adhésions de ce membre à différents clubs.
     * Relation One-to-Many vers l'entité de jointure {@link Adhesion}.
     * Chargement paresseux (LAZY). Cascade et orphanRemoval lient les adhésions au membre.
     * Visible dans la vue détaillée du membre.
     */
    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(GlobalView.MembreView.class)
    private Set<Adhesion> adhesions = new HashSet<>();

    /**
     * Ensemble des demandes d'ami envoyées par ce membre.
     * Relation One-to-Many vers {@link DemandeAmi}.
     * Chargement paresseux (LAZY). Cascade et orphanRemoval assurent la suppression des demandes si le membre est supprimé.
     * Ignoré par défaut en JSON pour éviter les boucles et la surcharge d'informations.
     */
    @OneToMany(mappedBy = "envoyeur",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<DemandeAmi> demandesEnvoyees = new HashSet<>();

    /**
     * Ensemble des demandes d'ami reçues par ce membre.
     * Relation One-to-Many vers {@link DemandeAmi}.
     * Chargement paresseux (LAZY). Cascade et orphanRemoval assurent la suppression des demandes si le membre est supprimé.
     * Ignoré par défaut en JSON.
     */
    @OneToMany(mappedBy = "recepteur",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<DemandeAmi> demandesRecues = new HashSet<>();

    /**
     * Ensemble des membres qui sont amis avec ce membre.
     * Relation Many-to-Many réflexive, gérée via la table de jointure "amitie".
     * Chargement paresseux (LAZY).
     * Visible dans la vue détaillée du membre.
     *
     * <p>Les méthodes {@link #addAmi(Membre)} et {@link #removeAmi(Membre)} doivent être utilisées
     * pour gérer correctement la bidirectionnalité de cette relation.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "amitie", // Nom de la table de jointure.
            joinColumns = @JoinColumn(name = "membre_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_amitie_membre")), // Clé vers cette entité.
            inverseJoinColumns = @JoinColumn(name = "ami_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_amitie_ami")) // Clé vers l'entité "ami".
    )
    @JsonView(GlobalView.MembreView.class)
    private Set<Membre> amis = new HashSet<>();

    /**
     * Liste des notations laissées par ce membre sur des événements.
     * Relation One-to-Many vers {@link Notation}.
     * Chargement paresseux (LAZY). Cascade et orphanRemoval lient les notations au membre.
     * Ignoré par défaut en JSON.
     */
    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Notation> notations = new ArrayList<>();

    // --- Méthodes de Cycle de Vie et Utilitaires ---

    /**
     * Callback JPA exécuté après la première persistance de l'entité.
     * Génère le {@code codeAmi} unique basé sur l'ID qui vient d'être assigné.
     * Ne fait rien si l'ID est null ou si le codeAmi existe déjà.
     */
    @PostPersist
    public void generateCodeAmi() {
        if (this.id != null && this.codeAmi == null) {
            this.codeAmi = String.format("AMIS-%06d", this.id);
        }
    }

    /**
     * Ajoute un membre à la liste d'amis de ce membre ET met à jour la liste d'amis de l'autre membre
     * pour maintenir la bidirectionnalité de la relation {@code @ManyToMany}.
     * Inclut des vérifications défensives pour les collections nulles.
     *
     * @param ami Le {@link Membre} à ajouter comme ami. Ne doit pas être null.
     */
    public void addAmi(Membre ami) {
        Objects.requireNonNull(ami, "L'ami à ajouter ne peut pas être null.");
        if (this.amis == null) this.amis = new HashSet<>();
        if (ami.getAmis() == null) ami.setAmis(new HashSet<>()); // Important pour la bidirectionnalité

        this.amis.add(ami);
        ami.getAmis().add(this); // Assure la relation dans l'autre sens.
    }

    /**
     * Retire un membre de la liste d'amis de ce membre ET met à jour la liste d'amis de l'autre membre
     * pour maintenir la bidirectionnalité de la relation {@code @ManyToMany}.
     *
     * @param ami Le {@link Membre} à retirer de la liste d'amis. Ne doit pas être null.
     */
    public void removeAmi(Membre ami) {
        Objects.requireNonNull(ami, "L'ami à retirer ne peut pas être null.");
        if (this.amis != null) {
            this.amis.remove(ami);
        }
        if (ami.getAmis() != null) {
            ami.getAmis().remove(this); // Assure le retrait dans l'autre sens.
        }
    }

    // --- Méthode d'Anonymisation ---

    /**
     * Anonymise les données personnelles identifiables de ce membre, définit son rôle à ANONYME,
     * et nettoie les relations d'amitié et les demandes d'ami en attente.
     * Cette méthode est une étape clé du processus de suppression logique (soft delete) conforme au RGPD.
     * Elle modifie l'état de l'objet mais ne le persiste pas. Elle doit être appelée
     * dans une transaction avant de sauvegarder l'entité avec {@code actif = false}.
     *
     * <p>Les relations comme les réservations, adhésions et notations sont conservées
     * pour l'historique, mais seront difficiles à lier à un utilisateur spécifique
     * en raison de l'anonymisation et du filtre {@code @Where}.</p>
     *
     * @throws IllegalStateException si le membre n'a pas encore été persisté ({@code id} est null),
     *                               car l'ID est utilisé pour garantir l'unicité de l'email anonymisé.
     */
    public void anonymizeData() {
        if (this.id == null) {
            throw new IllegalStateException("Impossible d'anonymiser un membre non persisté (ID est null).");
        }

        String anonymizedSuffix = "_id" + this.id; // Suffixe basé sur l'ID.

        // 1. Anonymiser les informations personnelles identifiables
        this.nom = "Utilisateur";
        this.prenom = "Anonyme" + anonymizedSuffix; // Rend unique pour éviter collisions potentielles
        this.date_naissance = LocalDate.of(1900, 1, 1); // Date générique (ou null si BDD permet)
        this.numero_voie = "N/A";
        this.rue = "N/A";
        this.codepostal = "00000";
        this.ville = "N/A";
        this.telephone = "0000000000"; // Numéro invalide (ou null si BDD permet)
        this.email = "anonymized_" + this.id + "@example.com"; // Email unique et manifestement invalide
        this.password = "$2a$10$invalidHashPlaceholder." + UUID.randomUUID(); // Hash invalide/inutilisable
        this.codeAmi = "ANON-" + this.id; // Code ami invalidé

        // 2. Changer le rôle
        this.role = Role.ANONYME; // Assigner un rôle spécifique pour les anonymisés.

        // 3. Nettoyer les relations sociales
        // Supprimer les demandes en attente (envoyées ou reçues)
        if (this.demandesEnvoyees != null) this.demandesEnvoyees.clear();
        if (this.demandesRecues != null) this.demandesRecues.clear();

        // Supprimer les liens d'amitié (en respectant la bidirectionnalité)
        if (this.amis != null && !this.amis.isEmpty()) {
            // Copie pour itération sûre tout en modifiant la collection originale via removeAmi
            Set<Membre> currentAmis = new HashSet<>(this.amis);
            for (Membre ami : currentAmis) {
                this.removeAmi(ami); // Gère les deux côtés de la relation
            }
        }
        // Note: this.amis devrait être vide après la boucle.

        // 4. Enregistrer la date d'anonymisation
        this.anonymizeDate = LocalDateTime.now();

        // Note: Le champ 'actif' doit être mis à 'false' par l'appelant (service) avant la sauvegarde.
    }

    // --- equals & hashCode (Basés sur l'ID) ---

    /**
     * Détermine si cet objet Membre est égal à un autre objet.
     * L'égalité est basée sur l'identifiant ({@code id}) pour les entités persistées.
     *
     * @param o L'objet à comparer.
     * @return {@code true} si les objets sont égaux (même instance ou même ID persisté), {@code false} sinon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Membre membre)) return false; // Utilise pattern matching
        // Deux membres sont égaux s'ils ont le même ID non null.
        return id != null && id.equals(membre.id);
    }

    /**
     * Retourne un code de hachage pour cet objet Membre.
     * Basé sur l'identifiant ({@code id}) si disponible, sinon utilise une valeur constante
     * dérivée de la classe pour les instances non persistées.
     *
     * @return Le code de hachage.
     */
    @Override
    public int hashCode() {
        // Cohérent avec equals.
        return id != null ? Objects.hashCode(id) : getClass().hashCode();
    }

    // Optionnel: toString() pour le débogage, évitant de charger les relations LAZY
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

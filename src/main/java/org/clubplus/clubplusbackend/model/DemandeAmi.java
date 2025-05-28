package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.Instant;
import java.util.Objects;

/**
 * Entité JPA représentant une demande d'amitié entre deux {@link Membre}s.
 * Elle enregistre qui a envoyé la demande (envoyeur), qui l'a reçue (recepteur),
 * le statut actuel de la demande (ATTENTE, ACCEPTEE, REFUSEE), et la date de création.
 *
 * <p>Une contrainte d'unicité au niveau de la base de données ({@code uk_demandeami_envoyeur_recepteur})
 * empêche la création de plusieurs demandes pour la même paire envoyeur/recepteur,
 * garantissant qu'une seule demande (dans un sens) peut exister à la fois.</p>
 *
 * <p>L'égalité ({@code equals}) et le hachage ({@code hashCode}) sont définis sur la base
 * des identifiants de l'{@code envoyeur} et du {@code recepteur}. Cela reflète la contrainte
 * unique et permet d'identifier une demande par la paire d'utilisateurs impliqués,
 * indépendamment de l'ID de la demande elle-même ou de son statut.</p>
 *
 * @see Membre
 * @see Statut (Enumération supposée exister pour les statuts de demande)
 * @see GlobalView.DemandeView
 */
@Entity
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
@NoArgsConstructor // Lombok: Constructeur sans argument (requis par JPA).
// Contrainte unique essentielle pour éviter les demandes multiples entre les mêmes personnes.
@Table(name = "demande_ami", // Nom explicite de la table
        uniqueConstraints = @UniqueConstraint(columnNames = {"envoyeur_id", "recepteur_id"}, name = "uk_demandeami_envoyeur_recepteur")
)
public class DemandeAmi {

    /**
     * Identifiant unique de la demande d'amitié, généré automatiquement.
     * Visible dans la vue JSON spécifique aux demandes.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.DemandeView.class})
    private Integer id;

    /**
     * Le membre qui a initié (envoyé) la demande d'amitié.
     * Relation Many-to-One : Un membre peut envoyer plusieurs demandes.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     * Les informations de base de l'envoyeur sont incluses dans la vue JSON des demandes.
     */
    @NotNull // Validation applicative : une demande doit avoir un envoyeur.
    @ManyToOne(fetch = FetchType.LAZY, // LAZY: Ne charge pas l'objet Membre envoyeur sauf si nécessaire.
            optional = false)       // Indique à JPA que la relation est obligatoire.
    @JoinColumn(name = "envoyeur_id", // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_demandeami_envoyeur")) // Nom de la contrainte FK.
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour l'envoyeur dans le contexte d'une demande.
    private Membre envoyeur;

    /**
     * Le membre qui a reçu la demande d'amitié.
     * Relation Many-to-One : Un membre peut recevoir plusieurs demandes.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     * Les informations de base du récepteur sont incluses dans la vue JSON des demandes.
     */
    @NotNull // Validation applicative : une demande doit avoir un récepteur.
    @ManyToOne(fetch = FetchType.LAZY, // LAZY: Ne charge pas l'objet Membre récepteur sauf si nécessaire.
            optional = false)       // Relation obligatoire pour JPA.
    @JoinColumn(name = "recepteur_id", // Nom de la colonne FK.
            nullable = false,      // Contrainte NOT NULL BDD.
            foreignKey = @ForeignKey(name = "fk_demandeami_recepteur")) // Nom de la contrainte FK.
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour le récepteur dans le contexte d'une demande.
    private Membre recepteur;

    /**
     * Le statut actuel de la demande d'amitié (ex: ATTENTE, ACCEPTEE, REFUSEE).
     * Représenté par l'énumération {@link Statut} (nom supposé).
     * Le statut est obligatoire et stocké sous forme de chaîne de caractères en BDD.
     * La valeur par défaut est {@code Statut.ATTENTE}.
     * Le statut est inclus dans la vue JSON des demandes.
     */
    @NotNull // Validation applicative : le statut doit être défini.
    @Enumerated(EnumType.STRING) // Stocke le nom de l'enum (ex: "ATTENTE") en BDD.
    @Column(nullable = false, length = 20) // Contrainte BDD: non null, taille suffisante pour les noms de statut.
    @JsonView({GlobalView.DemandeView.class})
    private Statut statut = Statut.ATTENTE; // Initialisation à la valeur par défaut 'ATTENTE'.

    /**
     * La date et l'heure exactes auxquelles la demande d'amitié a été créée.
     * Cette date est définie lors de la création de l'objet et ne peut pas être modifiée.
     * Elle est obligatoire et incluse dans la vue JSON des demandes.
     */
    @NotNull // Validation applicative : la date est requise.
    @Column(nullable = false, updatable = false) // Contrainte BDD: non null, non modifiable après création.
    @JsonView({GlobalView.DemandeView.class})
    private Instant dateDemande = Instant.now(); // Initialisation à la date/heure actuelle.

    /**
     * Constructeur métier pour créer une nouvelle instance de DemandeAmi.
     * Initialise l'envoyeur, le récepteur, et implicitement le statut à ATTENTE et la date à maintenant.
     *
     * @param envoyeur  Le {@link Membre} qui envoie la demande.
     * @param recepteur Le {@link Membre} qui reçoit la demande.
     * @throws NullPointerException si {@code envoyeur} ou {@code recepteur} est null.
     */
    public DemandeAmi(Membre envoyeur, Membre recepteur) {
        // Utilisation de Objects.requireNonNull pour la validation et l'affectation concise.
        this.envoyeur = Objects.requireNonNull(envoyeur, "L'envoyeur ne peut pas être null");
        this.recepteur = Objects.requireNonNull(recepteur, "Le récepteur ne peut pas être null");
        // Le statut et la date sont initialisés par défaut lors de la déclaration des champs.
    }

    // --- equals et hashCode ---

    /**
     * Détermine si cette DemandeAmi est égale à un autre objet.
     * L'égalité est basée sur l'identité de l'envoyeur et du récepteur, reflétant la
     * contrainte unique {@code uk_demandeami_envoyeur_recepteur}. Deux demandes sont considérées
     * égales si elles concernent la même paire envoyeur-récepteur, indépendamment de leur ID
     * de persistance ou de leur statut.
     *
     * <p>Attention : Cette comparaison repose sur les IDs des objets {@code Membre} associés.
     * Si ces IDs sont null (cas des membres non encore persistés), la comparaison pourrait
     * ne pas donner le résultat attendu si les objets Membre eux-mêmes ne sont pas la même instance.</p>
     *
     * @param o L'objet à comparer.
     * @return {@code true} si l'objet est une DemandeAmi concernant le même envoyeur et le même récepteur, {@code false} sinon.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifie si 'o' est une instance de DemandeAmi (ou sous-classe) et non null.
        if (!(o instanceof DemandeAmi that)) return false;

        // Compare les IDs de l'envoyeur et du récepteur.
        // Gère le cas où les membres ou leurs IDs pourraient être null.
        Integer thisEnvoyeurId = (this.envoyeur != null) ? this.envoyeur.getId() : null;
        Integer thatEnvoyeurId = (that.envoyeur != null) ? that.envoyeur.getId() : null;
        Integer thisRecepteurId = (this.recepteur != null) ? this.recepteur.getId() : null;
        Integer thatRecepteurId = (that.recepteur != null) ? that.recepteur.getId() : null;

        // Les demandes sont égales si les paires (envoyeurId, recepteurId) sont identiques.
        return Objects.equals(thisEnvoyeurId, thatEnvoyeurId) &&
                Objects.equals(thisRecepteurId, thatRecepteurId);
    }

    /**
     * Retourne un code de hachage pour cet objet DemandeAmi.
     * Le code de hachage est calculé à partir des identifiants de l'envoyeur et du récepteur,
     * en cohérence avec la méthode {@code equals}.
     * Gère correctement les cas où l'envoyeur, le récepteur ou leurs IDs seraient null.
     *
     * @return Le code de hachage basé sur les IDs de l'envoyeur et du récepteur.
     */
    @Override
    public int hashCode() {
        // Calcule le hash basé sur les IDs, gérant les cas null.
        Integer envoyeurId = (envoyeur != null) ? envoyeur.getId() : null;
        Integer recepteurId = (recepteur != null) ? recepteur.getId() : null;
        return Objects.hash(envoyeurId, recepteurId);
    }

    // Optionnel: toString() pour le débogage
    @Override
    public String toString() {
        return "DemandeAmi{" +
                "id=" + id +
                ", envoyeurId=" + (envoyeur != null ? envoyeur.getId() : "null") +
                ", recepteurId=" + (recepteur != null ? recepteur.getId() : "null") +
                ", statut=" + statut +
                ", dateDemande=" + dateDemande +
                '}';
    }
}

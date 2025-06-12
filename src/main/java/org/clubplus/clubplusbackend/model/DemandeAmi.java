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
 * <p>
 * Une contrainte d'unicité sur la paire (envoyeur, recepteur) empêche les demandes multiples.
 * L'égalité des objets est basée sur l'identité de l'envoyeur et du récepteur.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "demande_ami",
        uniqueConstraints = @UniqueConstraint(columnNames = {"envoyeur_id", "recepteur_id"}, name = "uk_demandeami_envoyeur_recepteur")
)
public class DemandeAmi {

    /**
     * Identifiant unique de la demande d'amitié, généré automatiquement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.DemandeView.class})
    private Integer id;

    /**
     * Le membre qui a envoyé la demande d'amitié.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envoyeur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_demandeami_envoyeur"))
    @JsonView(GlobalView.DemandeView.class)
    private Membre envoyeur;

    /**
     * Le membre qui a reçu la demande d'amitié.
     * La relation est obligatoire et chargée paresseusement (LAZY).
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recepteur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_demandeami_recepteur"))
    @JsonView(GlobalView.DemandeView.class)
    private Membre recepteur;

    /**
     * Le statut actuel de la demande (ex: ATTENTE, ACCEPTEE, REFUSEE).
     * La valeur par défaut est {@code Statut.ATTENTE}.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @JsonView({GlobalView.DemandeView.class})
    private Statut statut = Statut.ATTENTE;

    /**
     * La date et l'heure de création de la demande.
     * Ce champ est initialisé à la création et ne peut pas être mis à jour.
     */
    @NotNull
    @Column(nullable = false, updatable = false)
    @JsonView({GlobalView.DemandeView.class})
    private Instant dateDemande = Instant.now();

    /**
     * Constructeur pour créer une nouvelle demande d'amitié.
     *
     * @param envoyeur  Le membre qui envoie la demande (ne doit pas être null).
     * @param recepteur Le membre qui reçoit la demande (ne doit pas être null).
     */
    public DemandeAmi(Membre envoyeur, Membre recepteur) {
        this.envoyeur = Objects.requireNonNull(envoyeur, "L'envoyeur ne peut pas être null");
        this.recepteur = Objects.requireNonNull(recepteur, "Le récepteur ne peut pas être null");
    }

    /**
     * L'égalité est basée sur la paire envoyeur/recepteur.
     * Deux demandes sont égales si elles concernent les mêmes deux membres.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DemandeAmi that)) return false;

        Integer thisEnvoyeurId = (this.envoyeur != null) ? this.envoyeur.getId() : null;
        Integer thatEnvoyeurId = (that.envoyeur != null) ? that.envoyeur.getId() : null;
        Integer thisRecepteurId = (this.recepteur != null) ? this.recepteur.getId() : null;
        Integer thatRecepteurId = (that.recepteur != null) ? that.recepteur.getId() : null;

        return Objects.equals(thisEnvoyeurId, thatEnvoyeurId) &&
                Objects.equals(thisRecepteurId, thatRecepteurId);
    }

    /**
     * Le hashcode est basé sur les IDs de l'envoyeur et du récepteur.
     */
    @Override
    public int hashCode() {
        Integer envoyeurId = (envoyeur != null) ? envoyeur.getId() : null;
        Integer recepteurId = (recepteur != null) ? recepteur.getId() : null;
        return Objects.hash(envoyeurId, recepteurId);
    }

    /**
     * Représentation textuelle de la demande pour le débogage.
     */
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

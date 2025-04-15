package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.security.Statut;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
// La contrainte unique est très importante pour éviter les demandes multiples entre les mêmes personnes
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"envoyeur_id", "recepteur_id"}, name = "uk_demandeami_envoyeur_recepteur"))
public class DemandeAmi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.DemandeView.class}) // Vue spécifique pour les détails de la demande
    private Integer id;

    @NotNull // Une demande doit avoir un envoyeur
    @ManyToOne(fetch = FetchType.LAZY) // LAZY est préférable ici
    @JoinColumn(name = "envoyeur_id", nullable = false)
    @JsonView(GlobalView.DemandeView.class) // Inclure les infos de base de l'envoyeur dans la vue demande
    private Membre envoyeur;

    @NotNull // Une demande doit avoir un recepteur
    @ManyToOne(fetch = FetchType.LAZY) // LAZY est préférable ici
    @JoinColumn(name = "recepteur_id", nullable = false)
    @JsonView(GlobalView.DemandeView.class) // Inclure les infos de base du récepteur dans la vue demande
    private Membre recepteur;

    @NotNull // Le statut est obligatoire
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) // Longueur pour stocker les noms de statut
    @JsonView({GlobalView.DemandeView.class}) // Le statut est essentiel dans la vue demande
    private Statut statut = Statut.ATTENTE; // Valeur par défaut logique

    @NotNull // La date est importante
    @Column(nullable = false, updatable = false) // Date de création non modifiable
    @JsonView({GlobalView.DemandeView.class})
    private LocalDateTime dateDemande = LocalDateTime.now(); // Initialisation par défaut

    // Constructeur utile pour la création dans le service
    public DemandeAmi(Membre envoyeur, Membre recepteur) {
        this.envoyeur = envoyeur;
        this.recepteur = recepteur;
    }

    // Equals et hashCode basés sur les IDs des membres pour identifier une demande unique
    // Attention: Si les IDs sont null (avant persistance), cela peut ne pas fonctionner comme attendu.
    // Une approche plus robuste pourrait inclure les objets Membre eux-mêmes ou utiliser l'ID de la demande si non null.
    // Cependant, pour une demande persistée, ceci est généralement suffisant.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DemandeAmi that = (DemandeAmi) o;
        // Vérifie si les envoyeurs et recepteurs sont les mêmes (indépendamment de l'ID de la demande elle-même)
        // Important car la contrainte unique est sur envoyeur/recepteur
        return Objects.equals(envoyeur != null ? envoyeur.getId() : null, that.envoyeur != null ? that.envoyeur.getId() : null) &&
                Objects.equals(recepteur != null ? recepteur.getId() : null, that.recepteur != null ? that.recepteur.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                envoyeur != null ? envoyeur.getId() : null,
                recepteur != null ? recepteur.getId() : null
        );
    }
}

package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor // Nécessaire pour JPA
@AllArgsConstructor // Pratique
@Table(name = "notations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membre_id"}, name = "uk_notation_event_membre"))
// Nommer la contrainte
public class Notation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Visibilité ID : OK
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class, GlobalView.NotationView.class})
    private Integer id;

    // Relation vers l'événement noté : PARFAIT
    @NotNull(message = "La notation doit être liée à un événement.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // LAZY, non optionnel
    @JoinColumn(name = "event_id", nullable = false) // FK non nulle
    @JsonView({GlobalView.Base.class, GlobalView.NotationView.class})
    private Event event;

    // Relation vers le membre qui a noté : PARFAIT
    @NotNull(message = "La notation doit être faite par un membre.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // LAZY, non optionnel
    @JoinColumn(name = "membre_id", nullable = false) // FK non nulle
    private Membre membre;

    // --- Critères de notation : EXCELLENT ---
    // @Min/@Max sont parfaits.
    // @Column(nullable = false) est techniquement redondant pour 'int' mais clarifie l'intention.

    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5")
    @Column(nullable = false) // Clarifie l'intention
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int ambiance;

    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int proprete;

    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int organisation;

    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int fairPlay;

    @Min(value = 1, message = "La note de niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note de niveau des joueurs doit être au maximum 5")
    @Column(nullable = false)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int niveauJoueurs;

    // --- Timestamp : PARFAIT ---
    @NotNull // La date doit être présente
    @PastOrPresent // Une notation se fait après ou pendant l'événement
    @Column(nullable = false, updatable = false) // Non modifiable après création
    @JsonView(GlobalView.NotationView.class) // Vue détaillée de la notation
    private LocalDateTime dateNotation;

    // --- Callbacks JPA ---
    @PrePersist // Méthode appelée avant l'insertion en BDD
    public void onPrePersist() {
        // Assure que la date est définie si elle n'est pas fournie explicitement lors de la création
        if (this.dateNotation == null) {
            this.dateNotation = LocalDateTime.now();
        }
    }

    // --- Méthode Calculée : TRÈS BIEN ---
    @Transient // Ne pas mapper en DB
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class}) // Inclure dans les vues pertinentes
    public double getNoteMoyenne() {
        // La division par 5.0 assure un résultat double
        return (ambiance + proprete + organisation + fairPlay + niveauJoueurs) / 5.0;
    }

    // --- equals et hashCode : ESSENTIEL (basé sur la clé fonctionnelle event+membre) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifier si l'objet peut même avoir les champs event et membre
        if (o == null || getClass() != o.getClass()) return false;
        Notation notation = (Notation) o;
        // Comparaison basée sur les objets liés (qui doivent avoir leur propre equals/hashCode correct basé sur l'ID)
        // Cette égalité fonctionnelle correspond à la contrainte unique.
        return Objects.equals(event, notation.event) &&
                Objects.equals(membre, notation.membre);
    }

    @Override
    public int hashCode() {
        // Cohérent avec equals, basé sur les objets liés.
        return Objects.hash(event, membre);
    }
}

package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
// Ajout d'une contrainte unique pour qu'un membre ne note qu'une fois un événement
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membre_id"}))
public class Notation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false) // Rendre non nullable
    @JsonView(GlobalView.NotationView.class)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "membre_id", nullable = false) // Rendre non nullable
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private Membre membre;

    @Column(nullable = false)
    @Min(value = 1, message = "La note d'ambiance doit être au minimum 1")
    @Max(value = 5, message = "La note d'ambiance doit être au maximum 5") // Supposons une échelle de 1 à 5
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int ambiance;

    @Column(nullable = false)
    @Min(value = 1, message = "La note de propreté doit être au minimum 1")
    @Max(value = 5, message = "La note de propreté doit être au maximum 5")
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int propreté;

    @Column(nullable = false)
    @Min(value = 1, message = "La note d'organisation doit être au minimum 1")
    @Max(value = 5, message = "La note d'organisation doit être au maximum 5")
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int organisation;

    @Column(nullable = false)
    @Min(value = 1, message = "La note de fair-play doit être au minimum 1")
    @Max(value = 5, message = "La note de fair-play doit être au maximum 5")
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int fairPlay;

    @Column(nullable = false)
    @Min(value = 1, message = "La note de niveau des joueurs doit être au minimum 1")
    @Max(value = 5, message = "La note de niveau des joueurs doit être au maximum 5")
    @JsonView({GlobalView.EventView.class, GlobalView.NotationView.class})
    private int niveauJoueurs;

    @Column(nullable = false, updatable = false) // Non modifiable après création
    @JsonView(GlobalView.NotationView.class)
    private LocalDateTime dateNotation;

    // Constructeur par défaut, getters, setters (gérés par Lombok)

    // Méthode exécutée avant la persistance pour définir la date
    @PrePersist
    protected void onCreate() {
        dateNotation = LocalDateTime.now();
    }
}


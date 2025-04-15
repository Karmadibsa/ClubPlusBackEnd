package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"membre_id", "club_id"}, name = "uk_adhesion_membre_club"))
// Donner un nom à la contrainte est une bonne pratique
// --- Equals & HashCode ---
// Basé sur les relations clés. C'est un choix intelligent pour une entité de liaison,
// surtout si on la manipule dans des collections AVANT la persistance.
// Il définit l'égalité fonctionnelle (la même personne dans le même club).
@EqualsAndHashCode(of = {"membre", "club"})
public class Adhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class) // Si besoin de sérialiser l'ID
    private Integer id; // Cohérent si toutes vos IDs sont Integer.

    @NotNull // Une adhésion DOIT concerner un membre.
    @ManyToOne(fetch = FetchType.LAZY, // TRÈS BIEN: On ne charge pas le membre sauf si besoin.
            optional = false) // Renforce la contrainte NotNull au niveau JPA.
    @JoinColumn(name = "membre_id", nullable = false) // Définit la colonne FK et la contrainte BDD. Parfait.
    // @JsonView(GlobalView.Base.class) // Si besoin de sérialiser le membre (sa vue de base)
    private Membre membre;

    @NotNull // Une adhésion DOIT concerner un club.
    @ManyToOne(fetch = FetchType.LAZY, // TRÈS BIEN: On ne charge pas le club sauf si besoin.
            optional = false) // Renforce la contrainte NotNull au niveau JPA.
    @JoinColumn(name = "club_id", nullable = false) // Définit la colonne FK et la contrainte BDD. Parfait.
    // @JsonView(GlobalView.Base.class) // Si besoin de sérialiser le club (sa vue de base)
    private Club club;

    @NotNull // La date d'adhésion est une information importante.
    @Column(nullable = false, updatable = false) // PARFAIT: La date est obligatoire et ne doit jamais changer.
    // @JsonView(GlobalView.Base.class) // Si besoin de sérialiser la date
    private LocalDateTime dateAdhesion;

    // Constructeur: Très utile pour créer l'adhésion dans les services.
    public Adhesion(Membre membre, Club club) {
        // Bonne validation défensive pour éviter les états incohérents.
        if (membre == null || club == null) {
            throw new IllegalArgumentException("Membre et Club ne peuvent pas être nuls pour créer une Adhesion");
        }
        this.membre = membre;
        this.club = club;
        // Initialise la date au moment de la création de l'objet Adhesion.
        this.dateAdhesion = LocalDateTime.now();
    }


}

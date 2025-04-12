package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.Base.class)
    private String nom;


    @Column(nullable = false)
    @JsonView(GlobalView.ClubView.class)
    private LocalDate date_creation;

    @Column(nullable = false)
    @JsonView(GlobalView.ClubView.class)
    private LocalDate date_inscription;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.ClubView.class)
    private String numero_voie;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.ClubView.class)
    private String rue;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.ClubView.class)
    private String codepostal;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.Base.class)
    private String ville;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.ClubView.class)
    private String telephone;

    @Column(nullable = false, unique = true)
    @JsonView(GlobalView.ClubView.class)
    @Email
    private String email;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JsonView(GlobalView.ClubView.class)
    @JoinColumn(name = "admin_id")
    private Membre admin;

    //     Relation avec les membres (1Club → N Membres)
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.ClubView.class)
    private List<Membre> membres = new ArrayList<>();

    // Relation avec les événements organisés (1 Club → N Événements)
    @OneToMany(mappedBy = "organisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.ClubView.class)
    private List<Event> evenements = new ArrayList<>();

    @Column(unique = true, length = 9)
    @JsonView({GlobalView.ReservationView.class, GlobalView.EventView.class, GlobalView.MembreView.class, GlobalView.CategorieView.class, GlobalView.ClubView.class})
    private String codeClub; // Format "CL01", "CL02", etc.

    // Méthode pour générer le code après la persistance de l'entité
    @PostPersist
    public void generateCode() {
        if (this.id != null) {
            this.codeClub = String.format("CLUB-%04d", this.id); // Exemple : CL0001, CL0002, etc.
        }
    }
}

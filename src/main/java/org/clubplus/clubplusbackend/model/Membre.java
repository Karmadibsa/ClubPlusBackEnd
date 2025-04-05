package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Membre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.ReservationView.class, GlobalView.EventView.class, GlobalView.MembreView.class, GlobalView.CategorieView.class})
    private Long id;

    @Column(nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.EventView.class, GlobalView.MembreView.class, GlobalView.CategorieView.class})
    private String nom;

    @Column(nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.EventView.class, GlobalView.MembreView.class, GlobalView.CategorieView.class})
    private String prenom;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String date_naissance;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String date_inscription;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String numero_voie;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String rue;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String codepostal;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String ville;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String telephone;

    @Column(nullable = false, unique = true)
    @JsonView(GlobalView.MembreView.class)
    private String email;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private String role;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.MembreView.class)
    private List<Reservation> reservations = new ArrayList<>();

}

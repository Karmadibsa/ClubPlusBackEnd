package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Membre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.Base.class)
    private String nom;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.Base.class)
    private String prenom;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.MembreView.class)
    private LocalDate date_naissance;

    @Column(nullable = false)
    @JsonView(GlobalView.MembreView.class)
    private LocalDate date_inscription;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.MembreView.class)
    private String numero_voie;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.MembreView.class)
    private String rue;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.MembreView.class)
    private String codepostal;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.MembreView.class)
    private String ville;

    @Column(nullable = false)
    @NotBlank
    @JsonView(GlobalView.MembreView.class)
    private String telephone;

    @Column(nullable = false, unique = true)
    @JsonView(GlobalView.MembreView.class)
    @Email
    private String email;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('MEMBRE','RESERVATION', 'ADMIN')")
    @JsonView(GlobalView.MembreView.class)
    private Role role;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.MembreView.class)
    private List<Reservation> reservations = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "club_id")
    @JsonView(GlobalView.MembreView.class)
    private Club club;

    @OneToMany(mappedBy = "envoyeur", cascade = CascadeType.ALL)
    @JsonView(GlobalView.MembreView.class)
    private List<DemandeAmi> demandeEnvoye = new ArrayList<>();

    @OneToMany(mappedBy = "recepteur", cascade = CascadeType.ALL)
    private List<DemandeAmi> demandeRecu = new ArrayList<>();


    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notation> notations = new ArrayList<>();
}

package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.security.Statut;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"envoyeur_id", "recepteur_id"}))
public class DemandeAmi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.MembreView.class, GlobalView.DemandeView.class})
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "envoyeur_id", nullable = false)
    @JsonView(GlobalView.DemandeView.class)
    private Membre envoyeur;

    @ManyToOne
    @JoinColumn(name = "recepteur_id", nullable = false)
    @JsonView({GlobalView.MembreView.class, GlobalView.DemandeView.class})
    private Membre recepteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView({GlobalView.MembreView.class, GlobalView.DemandeView.class})
    private Statut statut = Statut.ATTENTE;

    @Column(nullable = false, updatable = false)
    @JsonView({GlobalView.MembreView.class, GlobalView.DemandeView.class})
    private LocalDateTime dateDemande = LocalDateTime.now();

}

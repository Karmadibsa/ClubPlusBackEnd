package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Categorie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private String nom;

    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private Integer capacite;

    @Transient
    @JsonView({GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class})
    public int getPlaceReserve() {
        if (reservations == null) {
            return 0;
        }
        return reservations.size(); // Nombre de réservations associées
    }

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    @JsonView(GlobalView.CategorieView.class)
    private Event event;

    @OneToMany(mappedBy = "categorie", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.CategorieView.class)
    private List<Reservation> reservations = new ArrayList<>();


}

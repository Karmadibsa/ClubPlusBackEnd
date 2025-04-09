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
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class, GlobalView.ClubView.class})
    private Long id;

    @Column(nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class, GlobalView.ClubView.class})
    private String title;

    @Column(nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class, GlobalView.ClubView.class})
    private String start;

    @Column(nullable = false)
    @JsonView(GlobalView.EventView.class)
    private String end;

    @Column(nullable = false)
    @JsonView(GlobalView.EventView.class)
    private String description;

    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class, GlobalView.ClubView.class})
    private String location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.EventView.class)
    private List<Categorie> categories = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.EventView.class)
    private List<Reservation> reservations = new ArrayList<>();

    // Relation avec le club organisateur (N Événements → 1 Club)
    @ManyToOne
    @JoinColumn(name = "organisateur_id")
    @JsonView(GlobalView.EventView.class)
    private Club organisateur;

    @JsonView(GlobalView.EventView.class)
    public int getPlaceTotal() {
        if (categories == null) return 0;
        return categories.stream()
                .mapToInt(Categorie::getCapacite)
                .sum();
    }

    @JsonView(GlobalView.EventView.class)
    public int getPlaceReserve() {
        if (reservations == null) return 0;
        return reservations.size();
    }
}

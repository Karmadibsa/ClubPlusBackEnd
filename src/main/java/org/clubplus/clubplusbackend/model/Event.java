package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;
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
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private String nom;

    @Column(nullable = false)
    @JsonView(GlobalView.Base.class)
    private LocalDateTime start;

    @Column(nullable = false)
    @JsonView(GlobalView.EventView.class)
    private LocalDateTime end;

    @Column(nullable = false)
    @JsonView(GlobalView.EventView.class)
    private String description;

    @JsonView(GlobalView.Base.class)
    private String location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.EventView.class)
    private List<Categorie> categories = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonView(GlobalView.EventView.class)
    private List<Reservation> reservations = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "organisateur_id")
    @JsonView(GlobalView.EventView.class)
    private Club organisateur;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @JsonView(GlobalView.EventView.class)
    private List<Notation> notations = new ArrayList<>();

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

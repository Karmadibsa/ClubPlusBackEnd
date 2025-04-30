package org.clubplus.clubplusbackend.dto; // Ou un package approprié

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.LocalDateTime;
import java.util.List;

@Data // Ajoute Getters, Setters, toString, equals, hashCode
public class EventWithFriendsDto {

    // --- Champs de l'Event que vous voulez exposer ---
    @JsonView(GlobalView.Base.class)
    private Integer id;

    @JsonView(GlobalView.Base.class)
    private String nom;

    @JsonView(GlobalView.Base.class)
    private LocalDateTime start;

    @JsonView(GlobalView.EventView.class) // Exemple
    private LocalDateTime end;

    @JsonView(GlobalView.EventView.class) // Exemple
    private String description;

    @JsonView(GlobalView.Base.class)
    private String location;

    @JsonView(GlobalView.Base.class)
    private Boolean actif;

    // --- Champs calculés de l'Event ---
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeTotal;

    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeReserve;

    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeDisponible;

    // --- NOUVEAU CHAMP : Liste des noms des amis participants ---
    @JsonView(GlobalView.Base.class) // Inclure dans la vue de base ? A adapter.
    private List<String> amiParticipants; // Ex: ["Jean Dupont", "Marie Durand"]

    @JsonView(GlobalView.Base.class) // Afficher les catégories dans la vue détaillée
    private List<Categorie> categories; // Utilise CategoryDto

    @JsonView(GlobalView.Base.class) // Afficher l'organisateur dans la vue de base
    private Club organisateur; // Utilise OrganizerDto
}


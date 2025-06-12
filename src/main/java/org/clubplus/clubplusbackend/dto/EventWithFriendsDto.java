package org.clubplus.clubplusbackend.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.Instant;
import java.util.List;

/**
 * DTO (Data Transfer Object) représentant un événement enrichi avec la liste
 * des amis de l'utilisateur qui y participent.
 * <p>
 * Ce DTO est utilisé pour fournir une vue personnalisée et sociale des événements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWithFriendsDto {

    // --- Champs de base de l'événement ---

    /**
     * ID unique de l'événement.
     */
    @JsonView(GlobalView.Base.class)
    private Integer id;

    /**
     * Nom de l'événement.
     */
    @JsonView(GlobalView.Base.class)
    private String nom;

    /**
     * Date et heure de début de l'événement.
     */
    @JsonView(GlobalView.Base.class)
    private Instant startTime;

    /**
     * Date et heure de fin de l'événement.
     */
    @JsonView(GlobalView.EventView.class)
    private Instant endTime;

    /**
     * Description détaillée de l'événement.
     */
    @JsonView(GlobalView.EventView.class)
    private String description;

    /**
     * Lieu de l'événement.
     */
    @JsonView(GlobalView.Base.class)
    private String location;

    /**
     * Indique si l'événement est actif (true) ou annulé (false).
     */
    @JsonView(GlobalView.Base.class)
    private Boolean actif;

    // --- Champs calculés sur les places ---

    /**
     * Capacité totale de l'événement (somme des capacités de ses catégories).
     */
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeTotal;

    /**
     * Nombre total de places actuellement réservées.
     */
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeReserve;

    /**
     * Nombre de places encore disponibles.
     */
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeDisponible;

    // --- Entités liées ---

    /**
     * Liste des catégories de l'événement.
     * Note: Pour une meilleure isolation, l'utilisation d'un DTO simple (ex: CategorieBasicDto) serait préférable.
     */
    @JsonView(GlobalView.Base.class)
    private List<Categorie> categories;

    /**
     * Le club qui organise l'événement.
     * Note: Pour une meilleure isolation, l'utilisation d'un DTO simple (ex: ClubBasicDto) serait préférable.
     */
    @JsonView(GlobalView.Base.class)
    private Club organisateur;

    // --- Champ spécifique à ce DTO ---

    /**
     * Liste des noms des amis de l'utilisateur courant qui participent à cet événement.
     * Ce champ est calculé spécifiquement pour l'utilisateur effectuant la requête.
     */
    @JsonView(GlobalView.Base.class)
    private List<String> amiParticipants;

}

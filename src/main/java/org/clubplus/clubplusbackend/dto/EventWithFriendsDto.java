package org.clubplus.clubplusbackend.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.time.Instant;
import java.util.List;

/**
 * DTO (Data Transfer Object) représentant un {@link Event} enrichi avec des informations
 * contextuelles spécifiques à l'utilisateur consultant, notamment la liste de ses amis
 * qui participent également à cet événement.
 *
 * <p>Ce DTO est typiquement construit dans la couche Service lorsqu'il faut présenter
 * une vue personnalisée des événements à un utilisateur, en mettant en évidence l'aspect social.</p>
 *
 * <p>Il réutilise les annotations {@link JsonView @JsonView} pour contrôler la sérialisation
 * des champs hérités de l'entité Event, et ajoute un champ spécifique {@code amiParticipants}.</p>
 *
 * @see Event
 * @see Membre (pour la notion d'amis)
 * @see org.clubplus.clubplusbackend.service.EventService (où ce DTO serait construit)
 */
@Data // Lombok: Ajoute Getters, Setters, toString, equals, hashCode.
@NoArgsConstructor // Pour flexibilité (ex: mapping)
@AllArgsConstructor // Pour création directe
@Builder // Pour construction fluide
public class EventWithFriendsDto {

    // --- Champs hérités/mappés de l'entité Event ---

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
     * Date et heure de début.
     */
    @JsonView(GlobalView.Base.class)
    private Instant startTime;

    /**
     * Date et heure de fin.
     */
    @JsonView(GlobalView.EventView.class) // Visible dans la vue détaillée de l'Event
    private Instant endTime;

    /**
     * Description détaillée.
     */
    @JsonView(GlobalView.EventView.class) // Visible dans la vue détaillée de l'Event
    private String description;

    /**
     * Lieu de l'événement.
     */
    @JsonView(GlobalView.Base.class)
    private String location;

    /**
     * Statut actif de l'événement (true = actif/prévu, false = annulé).
     */
    @JsonView(GlobalView.Base.class)
    private Boolean actif;

    // --- Champs calculés hérités/mappés de l'entité Event (@Transient) ---

    /**
     * Capacité totale de l'événement (somme des capacités des catégories).
     */
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeTotal;

    /**
     * Nombre total de places réservées (confirmées) pour l'événement.
     */
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeReserve;

    /**
     * Nombre total de places encore disponibles pour l'événement.
     */
    @JsonView({GlobalView.Base.class, GlobalView.EventView.class})
    private int placeDisponible;

    // --- Informations liées à l'Event ---

    /**
     * Liste des catégories associées à l'événement.
     * Utilise directement l'entité {@link Categorie}. Envisager un DTO simple (CategorieBasicDto)
     * pour ne pas exposer toute l'entité et éviter les problèmes de chargement LAZY non désirés.
     * Visible dans la vue de base dans ce DTO.
     */
    @JsonView(GlobalView.Base.class) // Ou une vue plus spécifique si nécessaire
    private List<Categorie> categories; // TODO: Utiliser un CategorieDto simple?

    /**
     * Le club organisateur de l'événement.
     * Utilise directement l'entité {@link Club}. Envisager un DTO simple (ClubBasicDto)
     * pour ne pas exposer toute l'entité et éviter les problèmes de chargement LAZY non désirés.
     * Visible dans la vue de base dans ce DTO.
     */
    @JsonView(GlobalView.Base.class) // Ou une vue plus spécifique si nécessaire
    private Club organisateur; // TODO: Utiliser un ClubDto simple?

    // --- NOUVEAU CHAMP SPÉCIFIQUE À CE DTO ---

    /**
     * Liste des noms complets (Prénom Nom) des amis de l'utilisateur courant
     * qui ont une réservation confirmée pour cet événement.
     * Cette liste est calculée spécifiquement pour l'utilisateur qui consulte.
     * Visible dans la vue de base dans ce DTO.
     */
    @JsonView(GlobalView.Base.class)
    private List<String> amiParticipants; // Ex: ["Jean Dupont", "Marie Durand"]

}

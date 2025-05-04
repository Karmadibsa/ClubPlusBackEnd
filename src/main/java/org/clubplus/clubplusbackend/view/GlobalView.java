package org.clubplus.clubplusbackend.view;

import com.fasterxml.jackson.annotation.JsonView;
import org.clubplus.clubplusbackend.controller.MembreController;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.*;

/**
 * Classe conteneur pour les différentes interfaces de vue utilisées avec l'annotation
 * {@link JsonView @JsonView} de Jackson. Ces vues permettent de contrôler finement
 * quels champs des objets (Entités, DTOs) sont inclus dans la sérialisation JSON
 * lors de la génération des réponses API REST.
 *
 * <p>Chaque interface de vue définit un sous-ensemble spécifique de propriétés à exposer.
 * En appliquant {@code @JsonView(NomDeLaVue.class)} sur les champs des entités/DTOs
 * et sur les méthodes des contrôleurs, on peut adapter la représentation JSON
 * au contexte de l'appel API (ex: liste vs détail, public vs privé).</p>
 *
 * @see JsonView
 */
public class GlobalView {

    /**
     * Interface de vue de base. Sert de marqueur commun ou représente les champs
     * minimaux à inclure dans presque toutes les sérialisations JSON.
     * Toutes les autres vues spécifiques héritent (directement ou indirectement) de celle-ci,
     * ce qui signifie que les champs marqués avec {@code @JsonView(GlobalView.Base.class)}
     * seront inclus dans *toutes* les vues.
     */
    public interface Base {
    }

    /**
     * Vue spécifique pour la sérialisation des entités {@link Event}.
     * Généralement utilisée pour inclure les champs principaux d'un événement,
     * potentiellement en excluant certaines relations pour éviter les chargements excessifs
     * ou les références circulaires dans les réponses de liste.
     * Hérite de {@link Base}.
     *
     * @see Event
     */
    public interface EventView extends Base {
    }

    /**
     * Vue spécifique pour la sérialisation des entités {@link Categorie}.
     * Utilisée pour contrôler les détails d'une catégorie à exposer, par exemple,
     * en incluant la capacité mais potentiellement pas la référence complète à l'événement parent
     * dans certains contextes pour éviter les cycles.
     * Hérite de {@link Base}.
     *
     * @see Categorie
     */
    public interface CategorieView extends Base {
    }

    /**
     * Vue spécifique pour la sérialisation des entités {@link Reservation}.
     * Permet de définir quels détails d'une réservation (ex: statut, date, références à l'événement/membre/catégorie)
     * sont inclus dans la réponse JSON.
     * Hérite de {@link Base}.
     *
     * @see Reservation
     */
    public interface ReservationView extends Base {
    }

    /**
     * Vue standard pour la sérialisation des entités {@link Membre}.
     * Typiquement utilisée pour des informations publiques ou semi-publiques (ID, nom, prénom),
     * en excluant les données sensibles comme le mot de passe ou l'email dans certains contextes.
     * Hérite de {@link Base}.
     *
     * @see Membre
     */
    public interface MembreView extends Base {
    }

    /**
     * Vue spécifique pour la sérialisation des entités {@link Club}.
     * Contrôle les informations du club (nom, adresse, code, etc.) à exposer.
     * Hérite de {@link Base}.
     *
     * @see Club
     */
    public interface ClubView extends Base {
    }

    /**
     * Vue spécifique pour la sérialisation des entités {@link DemandeAmi}.
     * Définit quels détails d'une demande d'ami (statut, date, références aux membres)
     * sont inclus dans la réponse JSON.
     * Hérite de {@link Base}.
     *
     * @see DemandeAmi
     */
    public interface DemandeView extends Base {
    }

    /**
     * Vue spécifique pour la sérialisation des entités {@link Notation}.
     * Permet de contrôler quels aspects d'une notation (les différentes notes, la date)
     * sont inclus. L'association avec le membre ayant noté est souvent exclue
     * de cette vue pour l'anonymat.
     * Hérite de {@link Base}.
     *
     * @see Notation
     */
    public interface NotationView extends Base {
    }

    /**
     * Vue détaillée spécifiquement pour le profil de l'utilisateur connecté ({@link Membre}).
     * Inclut généralement plus d'informations que {@link MembreView}, comme l'email,
     * l'adresse complète, le numéro de téléphone, etc., qui ne sont visibles que
     * par l'utilisateur lui-même.
     * Hérite de {@link Base}.
     *
     * @see Membre
     * @see MembreController#getMyProfile()
     * @see MembreController#updateMyProfile(UpdateMembreDto)
     */
    public interface ProfilView extends Base {
    }

    // Constructeur privé pour empêcher l'instanciation de la classe conteneur
    private GlobalView() {
        throw new AssertionError("Classe conteneur de vues non instantiable");
    }
}

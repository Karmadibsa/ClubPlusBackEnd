package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface Repository pour l'entité {@link Categorie}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} et des méthodes
 * personnalisées pour rechercher des catégories dans le contexte d'un événement,
 * vérifier l'unicité, et obtenir des informations liées aux réservations.
 *
 * @see Categorie
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface CategorieDao extends JpaRepository<Categorie, Integer> {

    /**
     * Recherche toutes les catégories associées à un événement spécifique, identifié par son ID.
     *
     * @param eventId L'ID de l'{@link Event} parent.
     * @return Une liste de {@link Categorie} appartenant à l'événement spécifié.
     * La liste est vide si l'événement n'a pas de catégories.
     */
    List<Categorie> findByEventId(Integer eventId);

    /**
     * Recherche une catégorie spécifique au sein d'un événement donné, basée sur son nom.
     * La comparaison du nom est effectuée sans tenir compte de la casse (IgnoreCase).
     * Principalement utilisé pour valider l'unicité du nom d'une catégorie lors de sa création ou mise à jour
     * au sein d'un même événement.
     *
     * @param eventId L'ID de l'{@link Event} parent.
     * @param nom     Le nom de la catégorie à rechercher (comparaison insensible à la casse).
     * @return Un {@link Optional} contenant la {@link Categorie} si trouvée, sinon un Optional vide.
     */
    Optional<Categorie> findByEventIdAndNomIgnoreCase(Integer eventId, String nom);

    /**
     * Recherche une catégorie par son identifiant unique (ID) et vérifie simultanément
     * qu'elle appartient bien à l'événement spécifié par {@code eventId}.
     * Utile pour récupérer une catégorie de manière sécurisée dans le contexte d'un événement spécifique.
     *
     * @param id      L'ID de la {@link Categorie} à rechercher.
     * @param eventId L'ID de l'{@link Event} auquel la catégorie doit appartenir.
     * @return Un {@link Optional} contenant la {@link Categorie} si elle existe et appartient à l'événement,
     * sinon un Optional vide.
     */
    Optional<Categorie> findByIdAndEventId(Integer id, Integer eventId);

    /**
     * Recherche une catégorie par son ID et l'ID de son événement parent, en chargeant
     * **impérativement** (via {@code LEFT JOIN FETCH}) la collection de {@link Reservation}s associées.
     * Cette méthode est spécifiquement conçue pour être utilisée *avant* la suppression
     * d'une catégorie, afin de pouvoir vérifier facilement si des réservations existent
     * ({@code categorie.getReservations().isEmpty()}) sans déclencher de requêtes N+1 ou
     * d'erreurs de chargement paresseux après une potentielle suppression de session.
     *
     * @param categorieId L'ID de la {@link Categorie} à rechercher.
     * @param eventId     L'ID de l'{@link Event} parent.
     * @return Un {@link Optional} contenant la {@link Categorie} avec sa collection {@code reservations}
     * initialisée (potentiellement vide), ou un Optional vide si la catégorie
     * n'est pas trouvée ou n'appartient pas à l'événement.
     */
    @Query("SELECT c FROM Categorie c LEFT JOIN FETCH c.reservations res WHERE c.id = :categorieId AND c.event.id = :eventId")
    Optional<Categorie> findByIdAndEventIdFetchingReservations(@Param("categorieId") Integer categorieId, @Param("eventId") Integer eventId);

    /**
     * Compte le nombre de réservations ayant le statut {@code CONFIRME} pour une catégorie spécifique.
     * Utilise une requête optimisée pour ne récupérer que le compte.
     *
     * @param categorieId L'ID de la {@link Categorie}.
     * @return Le nombre ({@code int}) de réservations confirmées pour cette catégorie.
     * @see org.clubplus.clubplusbackend.model.ReservationStatus#CONFIRME
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.categorie.id = :categorieId AND r.status = 'CONFIRME'")
    int countConfirmedReservations(@Param("categorieId") Integer categorieId);
}

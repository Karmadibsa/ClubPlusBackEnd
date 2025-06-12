package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité {@link Categorie}.
 * Fournit les opérations CRUD et des requêtes personnalisées pour la gestion des catégories d'événements.
 */
@Repository
public interface CategorieDao extends JpaRepository<Categorie, Integer> {

    /**
     * Recherche toutes les catégories associées à un événement spécifique.
     *
     * @param eventId L'ID de l'{@link Event} parent.
     * @return Une liste de catégories pour l'événement.
     */
    List<Categorie> findByEventId(Integer eventId);

    /**
     * Recherche une catégorie par son nom au sein d'un événement, sans tenir compte de la casse.
     * Utile pour vérifier l'unicité d'un nom de catégorie lors de la création ou de la mise à jour.
     *
     * @param eventId L'ID de l'{@link Event} parent.
     * @param nom     Le nom de la catégorie (insensible à la casse).
     * @return Un {@link Optional} contenant la catégorie si elle est trouvée.
     */
    Optional<Categorie> findByEventIdAndNomIgnoreCase(Integer eventId, String nom);

    /**
     * Recherche une catégorie par son ID en s'assurant qu'elle appartient à l'événement spécifié.
     *
     * @param id      L'ID de la catégorie.
     * @param eventId L'ID de l'{@link Event} parent.
     * @return Un {@link Optional} contenant la catégorie si elle correspond aux critères.
     */
    Optional<Categorie> findByIdAndEventId(Integer id, Integer eventId);

    /**
     * Recherche une catégorie et charge simultanément ses réservations associées.
     * <p>
     * Cette méthode est optimisée pour la suppression d'une catégorie, car elle permet de
     * vérifier la présence de réservations sans risque d'erreur de type LazyInitializationException.
     *
     * @param categorieId L'ID de la catégorie.
     * @param eventId     L'ID de l'événement parent.
     * @return Un {@link Optional} contenant la catégorie avec ses réservations initialisées.
     */
    @Query("SELECT c FROM Categorie c LEFT JOIN FETCH c.reservations res WHERE c.id = :categorieId AND c.event.id = :eventId")
    Optional<Categorie> findByIdAndEventIdFetchingReservations(@Param("categorieId") Integer categorieId, @Param("eventId") Integer eventId);

    /**
     * Compte le nombre de réservations confirmées pour une catégorie.
     *
     * @param categorieId L'ID de la catégorie.
     * @return Le nombre de réservations confirmées.
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.categorie.id = :categorieId AND r.status = 'CONFIRME'")
    int countConfirmedReservations(@Param("categorieId") Integer categorieId);
}

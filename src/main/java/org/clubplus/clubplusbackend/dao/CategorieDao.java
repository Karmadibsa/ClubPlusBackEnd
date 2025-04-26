package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieDao extends JpaRepository<Categorie, Integer> {

    /**
     * Trouve toutes les catégories pour un événement donné.
     */
    List<Categorie> findByEventId(Integer eventId);

    /**
     * Trouve une catégorie par son nom (ignorant la casse) au sein d'un événement spécifique.
     * Utilisé pour vérifier l'unicité du nom lors de la création/mise à jour.
     */
    Optional<Categorie> findByEventIdAndNomIgnoreCase(Integer eventId, String nom);

    /**
     * Trouve une catégorie par son ID et s'assure qu'elle appartient à l'événement spécifié.
     * Utilisé pour récupérer une catégorie dans le contexte d'un événement.
     */
    Optional<Categorie> findByIdAndEventId(Integer id, Integer eventId);

    /**
     * Trouve une catégorie par son ID et ID événement, en chargeant EAGERLY (via JOIN FETCH)
     * la collection de réservations associées.
     * Nécessaire avant la suppression pour vérifier si des réservations existent.
     *
     * @param categorieId L'ID de la catégorie.
     * @param eventId     L'ID de l'événement.
     * @return Un Optional contenant la Categorie avec ses réservations chargées, ou vide si non trouvée.
     */
    @Query("SELECT c FROM Categorie c LEFT JOIN FETCH c.reservations res WHERE c.id = :categorieId AND c.event.id = :eventId")
    Optional<Categorie> findByIdAndEventIdFetchingReservations(@Param("categorieId") Integer categorieId, @Param("eventId") Integer eventId);

    /**
     * Compte le nombre de réservations confirmées pour une catégorie spécifique.
     *
     * @param categorieId L'ID de la catégorie.
     * @return Le nombre de réservations avec le statut CONFIRME.
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.categorie.id = :categorieId AND r.status = 'CONFIRME'")
    int countConfirmedReservations(@Param("categorieId") Integer categorieId);
}

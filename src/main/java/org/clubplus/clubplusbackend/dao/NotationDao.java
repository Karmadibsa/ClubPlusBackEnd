package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Notation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotationDao extends JpaRepository<Notation, Integer> {
    // --- Méthodes de recherche de base ---

    /**
     * Trouve une notation par son ID.
     *
     * @param id L'ID de la notation.
     * @return Un Optional contenant la notation si trouvée.
     */
    Optional<Notation> findById(Integer id);

    /**
     * Trouve toutes les notations associées à un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return Une liste des notations pour cet événement.
     */
    List<Notation> findByEventId(Integer eventId);

    /**
     * Trouve toutes les notations laissées par un membre spécifique.
     *
     * @param membreId L'ID du membre.
     * @return Une liste des notations laissées par ce membre.
     */
    List<Notation> findByMembreId(Integer membreId);

    /**
     * Trouve la notation unique laissée par un membre pour un événement spécifique.
     *
     * @param eventId  L'ID de l'événement.
     * @param membreId L'ID du membre.
     * @return Un Optional contenant la notation si elle existe.
     */
    Optional<Notation> findByEventIdAndMembreId(Integer eventId, Integer membreId);

    /**
     * Vérifie si une notation existe pour un couple événement/membre donné.
     *
     * @param eventId  L'ID de l'événement.
     * @param membreId L'ID du membre.
     * @return true si une notation existe, false sinon.
     */
    boolean existsByEventIdAndMembreId(Integer eventId, Integer membreId);


    // --- Méthodes de suppression ---

    /**
     * Supprime une notation par son ID.
     *
     * @param id L'ID de la notation à supprimer.
     */
    void deleteById(Integer id); // Hérité de JpaRepository mais explicitement listé pour clarté


    // --- Requêtes personnalisées pour les moyennes ---

    /**
     * Calcule la moyenne de la note 'ambiance' pour un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return La moyenne (Double) ou null s'il n'y a pas de notations.
     */
    @Query("SELECT AVG(n.ambiance) FROM Notation n WHERE n.event.id = :eventId")
    Double findAverageAmbianceByEventId(@Param("eventId") Integer eventId);

    /**
     * Calcule la moyenne de la note 'propreté' pour un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return La moyenne (Double) ou null s'il n'y a pas de notations.
     */
    @Query("SELECT AVG(n.propreté) FROM Notation n WHERE n.event.id = :eventId")
    Double findAveragePropreteByEventId(@Param("eventId") Integer eventId);

    /**
     * Calcule la moyenne de la note 'organisation' pour un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return La moyenne (Double) ou null s'il n'y a pas de notations.
     */
    @Query("SELECT AVG(n.organisation) FROM Notation n WHERE n.event.id = :eventId")
    Double findAverageOrganisationByEventId(@Param("eventId") Integer eventId);

    /**
     * Calcule la moyenne de la note 'fairPlay' pour un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return La moyenne (Double) ou null s'il n'y a pas de notations.
     */
    @Query("SELECT AVG(n.fairPlay) FROM Notation n WHERE n.event.id = :eventId")
    Double findAverageFairPlayByEventId(@Param("eventId") Integer eventId);

    /**
     * Calcule la moyenne de la note 'niveauJoueurs' pour un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return La moyenne (Double) ou null s'il n'y a pas de notations.
     */
    @Query("SELECT AVG(n.niveauJoueurs) FROM Notation n WHERE n.event.id = :eventId")
    Double findAverageNiveauJoueursByEventId(@Param("eventId") Integer eventId);

}

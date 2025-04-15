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

    /**
     * Trouve toutes les notations pour un événement (pour affichage anonyme).
     */
    List<Notation> findByEventId(Integer eventId);

    /**
     * Vérifie si un membre a déjà noté un événement (pour la logique de création).
     */
    boolean existsByEventIdAndMembreId(Integer eventId, Integer membreId);

    // --- Requêtes pour les moyennes par critère (utiles pour l'affichage de l'event) ---
    @Query("SELECT AVG(n.ambiance) FROM Notation n WHERE n.event.id = :eventId")
    Optional<Double> findAverageAmbianceByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT AVG(n.propreté) FROM Notation n WHERE n.event.id = :eventId")
    Optional<Double> findAveragePropreteByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT AVG(n.organisation) FROM Notation n WHERE n.event.id = :eventId")
    Optional<Double> findAverageOrganisationByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT AVG(n.fairPlay) FROM Notation n WHERE n.event.id = :eventId")
    Optional<Double> findAverageFairPlayByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT AVG(n.niveauJoueurs) FROM Notation n WHERE n.event.id = :eventId")
    Optional<Double> findAverageNiveauJoueursByEventId(@Param("eventId") Integer eventId);

    // --- Requête pour les moyennes globales (StatsService - inchangée) ---
    @Query("SELECT AVG(n.ambiance), AVG(n.propreté), AVG(n.organisation), AVG(n.fairPlay), AVG(n.niveauJoueurs) " +
            "FROM Notation n " +
            "WHERE n.event.id IN :eventIds")
    Optional<Object[]> findAverageRatingsForEventIds(@Param("eventIds") List<Integer> eventIds);
}

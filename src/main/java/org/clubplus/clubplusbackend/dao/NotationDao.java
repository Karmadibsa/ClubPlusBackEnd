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

    // Méthodes pour StatsService (moyennes sur une liste d'events)
    @Query("SELECT AVG(n.ambiance) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageAmbianceForEventIds(@Param("eventIds") List<Integer> eventIds);

    @Query("SELECT AVG(n.proprete) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAveragePropreteForEventIds(@Param("eventIds") List<Integer> eventIds);

    @Query("SELECT AVG(n.organisation) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageOrganisationForEventIds(@Param("eventIds") List<Integer> eventIds);

    @Query("SELECT AVG(n.fairPlay) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageFairPlayForEventIds(@Param("eventIds") List<Integer> eventIds);

    @Query("SELECT AVG(n.niveauJoueurs) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageNiveauJoueursForEventIds(@Param("eventIds") List<Integer> eventIds);
}

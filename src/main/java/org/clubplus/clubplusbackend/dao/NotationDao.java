package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Notation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository pour l'entité {@link Notation}.
 * Fournit les opérations CRUD et des requêtes pour gérer les notations d'événements.
 */
@Repository
public interface NotationDao extends JpaRepository<Notation, Integer> {

    /**
     * Recherche toutes les notations pour un événement spécifique.
     *
     * @param eventId L'ID de l'{@link Event}.
     * @return Une liste de notations pour cet événement.
     */
    List<Notation> findByEventId(Integer eventId);

    /**
     * Récupère les IDs des événements qu'un membre a déjà notés.
     * <p>
     * Requête optimisée pour vérifier rapidement si des événements ont été notés.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Un ensemble d'IDs des événements notés.
     */
    @Query("SELECT n.event.id FROM Notation n WHERE n.membre.id = :membreId")
    Set<Integer> findRatedEventIdsByMembreId(@Param("membreId") Integer membreId);

    /**
     * Vérifie si un membre a déjà noté un événement spécifique.
     * <p>
     * Utilisé pour empêcher les notations multiples.
     *
     * @param eventId  L'ID de l'{@link Event}.
     * @param membreId L'ID du {@link Membre}.
     * @return {@code true} si une notation existe, {@code false} sinon.
     */
    boolean existsByEventIdAndMembreId(Integer eventId, Integer membreId);

    // --- Méthodes pour Statistiques ---

    /**
     * Calcule la note moyenne pour le critère 'ambiance' sur une liste d'événements.
     *
     * @param eventIds La liste des IDs d'événements.
     * @return Un {@link Optional<Double>} contenant la moyenne.
     */
    @Query("SELECT AVG(n.ambiance) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageAmbianceForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'proprete' sur une liste d'événements.
     *
     * @param eventIds La liste des IDs d'événements.
     * @return Un {@link Optional<Double>} contenant la moyenne.
     */
    @Query("SELECT AVG(n.proprete) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAveragePropreteForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'organisation' sur une liste d'événements.
     *
     * @param eventIds La liste des IDs d'événements.
     * @return Un {@link Optional<Double>} contenant la moyenne.
     */
    @Query("SELECT AVG(n.organisation) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageOrganisationForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'fairPlay' sur une liste d'événements.
     *
     * @param eventIds La liste des IDs d'événements.
     * @return Un {@link Optional<Double>} contenant la moyenne.
     */
    @Query("SELECT AVG(n.fairPlay) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageFairPlayForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'niveauJoueurs' sur une liste d'événements.
     *
     * @param eventIds La liste des IDs d'événements.
     * @return Un {@link Optional<Double>} contenant la moyenne.
     */
    @Query("SELECT AVG(n.niveauJoueurs) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageNiveauJoueursForEventIds(@Param("eventIds") List<Integer> eventIds);
}

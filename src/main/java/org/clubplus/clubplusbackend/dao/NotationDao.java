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
 * Interface Repository pour l'entité {@link Notation}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} et des méthodes
 * personnalisées pour lister les notations d'un événement, vérifier si un événement
 * a été noté par un membre, et calculer des statistiques (moyennes) sur les notations.
 *
 * @see Notation
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface NotationDao extends JpaRepository<Notation, Integer> {

    /**
     * Recherche toutes les notations soumises pour un événement spécifique.
     * Utile pour afficher les notations (souvent anonymisées ou agrégées) sur la page d'un événement.
     *
     * @param eventId L'ID de l'{@link Event} concerné.
     * @return Une liste de {@link Notation} pour cet événement. Vide si aucune notation n'a été laissée.
     */
    List<Notation> findByEventId(Integer eventId);

    /**
     * Récupère uniquement les identifiants (IDs) des événements qu'un membre spécifique a déjà notés.
     * C'est une requête optimisée pour vérifier rapidement quels événements ont été notés
     * par un utilisateur, par exemple pour afficher un indicateur "déjà noté" dans une liste d'événements.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Un ensemble ({@link Set}) d'{@link Integer} représentant les IDs des événements notés.
     * Utiliser un Set évite les doublons si jamais (par erreur de logique ailleurs)
     * une double notation existait, bien que la contrainte unique doive l'empêcher.
     */
    @Query("SELECT n.event.id FROM Notation n WHERE n.membre.id = :membreId")
    Set<Integer> findRatedEventIdsByMembreId(@Param("membreId") Integer membreId);

    /**
     * Vérifie de manière optimisée si une notation existe déjà pour une paire
     * événement/membre spécifique.
     * Principalement utilisé avant de permettre à un membre de soumettre une nouvelle notation,
     * pour s'assurer qu'il n'a pas déjà noté cet événement (respect de la contrainte unique).
     *
     * @param eventId  L'ID de l'{@link Event}.
     * @param membreId L'ID du {@link Membre}.
     * @return {@code true} si le membre a déjà soumis une notation pour cet événement, {@code false} sinon.
     */
    boolean existsByEventIdAndMembreId(Integer eventId, Integer membreId);

    // --- Méthodes pour Statistiques (Calcul de Moyennes) ---
    // Ces méthodes calculent la moyenne d'un critère spécifique sur un ensemble d'événements.
    // Elles retournent Optional<Double> pour gérer le cas où aucun événement de la liste n'a de notation.

    /**
     * Calcule la note moyenne pour le critère 'ambiance' sur une liste d'événements spécifiée.
     *
     * @param eventIds La liste des IDs des {@link Event}s à inclure dans le calcul.
     * @return Un {@link Optional<Double>} contenant la moyenne si au moins une notation existe
     * pour les événements fournis, sinon un Optional vide.
     */
    @Query("SELECT AVG(n.ambiance) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageAmbianceForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'proprete' sur une liste d'événements spécifiée.
     *
     * @param eventIds La liste des IDs des {@link Event}s à inclure dans le calcul.
     * @return Un {@link Optional<Double>} contenant la moyenne si au moins une notation existe, sinon vide.
     */
    @Query("SELECT AVG(n.proprete) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAveragePropreteForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'organisation' sur une liste d'événements spécifiée.
     *
     * @param eventIds La liste des IDs des {@link Event}s à inclure dans le calcul.
     * @return Un {@link Optional<Double>} contenant la moyenne si au moins une notation existe, sinon vide.
     */
    @Query("SELECT AVG(n.organisation) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageOrganisationForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'fairPlay' sur une liste d'événements spécifiée.
     *
     * @param eventIds La liste des IDs des {@link Event}s à inclure dans le calcul.
     * @return Un {@link Optional<Double>} contenant la moyenne si au moins une notation existe, sinon vide.
     */
    @Query("SELECT AVG(n.fairPlay) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageFairPlayForEventIds(@Param("eventIds") List<Integer> eventIds);

    /**
     * Calcule la note moyenne pour le critère 'niveauJoueurs' sur une liste d'événements spécifiée.
     *
     * @param eventIds La liste des IDs des {@link Event}s à inclure dans le calcul.
     * @return Un {@link Optional<Double>} contenant la moyenne si au moins une notation existe, sinon vide.
     */
    @Query("SELECT AVG(n.niveauJoueurs) FROM Notation n WHERE n.event.id IN :eventIds")
    Optional<Double> findAverageNiveauJoueursForEventIds(@Param("eventIds") List<Integer> eventIds);
}

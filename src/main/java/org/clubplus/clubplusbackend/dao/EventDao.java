package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventDao extends JpaRepository<Event, Integer> {

    // --- Recherche par Attributs ---
    List<Event> findByNomContainingIgnoreCase(String nom);

    List<Event> findByOrganisateurId(Integer clubId);

    List<Event> findByStartAfter(LocalDateTime dateTime); // Événements futurs

    List<Event> findByEndBefore(LocalDateTime dateTime); // Événements passés

    List<Event> findByStartBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Trouve les événements actifs à un instant T.
     */
    @Query("SELECT e FROM Event e WHERE e.start <= :now AND e.end >= :now")
    List<Event> findActiveEvents(@Param("now") LocalDateTime now);

    // --- Recherche Combinée ---

    /**
     * Événements futurs organisés par une liste de clubs.
     */
    List<Event> findByOrganisateurIdInAndStartAfter(Collection<Integer> clubIds, LocalDateTime now);

    /**
     * Événements futurs organisés par un club.
     */
    List<Event> findByOrganisateurIdAndStartAfter(Integer clubId, LocalDateTime now);

    // --- Comptage ---
    long countByOrganisateurId(Integer clubId);

    long countByStartBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByOrganisateurIdAndStartBetween(Integer clubId, LocalDateTime start, LocalDateTime end);

    // --- Requêtes Spécifiques ---

    /**
     * Récupère les IDs des événements passés d'un organisateur.
     */
    @Query("SELECT e.id FROM Event e WHERE e.organisateur.id = :clubId AND e.end < :now AND e.actif = true")
    List<Integer> findPastEventIdsByOrganisateurId(@Param("clubId") Integer clubId, @Param("now") LocalDateTime now);

    long countByOrganisateurIdAndActif(Integer clubId, boolean actif);

    /**
     * Récupère le nombre de réservations et la capacité totale pour chaque événement d'un club
     * ayant une capacité > 0.
     * Correction: Utilise LEFT JOIN pour inclure les événements sans réservation.
     */
    @Query("SELECT e.id, SUM(CASE WHEN r.status = 'CONFIRMED' THEN 1 ELSE 0 END), SUM(COALESCE(c.capacite, 0)) " + // Utilise COALESCE pour SUM sur potentiellement 0 catégories
            "FROM Event e " +
            "LEFT JOIN e.categories c " + // LEFT JOIN pour inclure event sans catégorie
            "LEFT JOIN c.reservations r " + // LEFT JOIN pour inclure catégorie sans réservation
            "WHERE e.organisateur.id = :clubId AND e.actif = true " +
            "GROUP BY e.id " +
            "HAVING SUM(COALESCE(c.capacite, 0)) > 0")
    // Assure capacité totale > 0
    List<Object[]> findEventStatsForOccupancy(@Param("clubId") Integer clubId);

    // --- Ajout pour suppression sécurisée ---

    /**
     * Trouve un événement par son ID, en chargeant EAGERLY ses catégories et les réservations de ces catégories.
     * Nécessaire pour vérifier les réservations avant suppression.
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.categories c LEFT JOIN FETCH c.reservations r WHERE e.id = :eventId")
    Optional<Event> findByIdFetchingCategoriesAndReservations(@Param("eventId") Integer eventId);


    // --- Ajouts pour filtrage par statut ---

    // Trouver tous les événements par statut actif/inactif
    List<Event> findByActif(boolean actif);

    // Trouver les événements d'un organisateur par statut actif/inactif
    List<Event> findByOrganisateurIdAndActif(Integer organisateurId, boolean actif);

    // Trouver les événements futurs ACTIFS d'un organisateur
    List<Event> findByOrganisateurIdAndActifAndStartAfter(Integer organisateurId, boolean actif, LocalDateTime startTime);

    // Trouver les événements futurs ACTIFS pour une liste de clubs
    List<Event> findByOrganisateurIdInAndActifAndStartAfter(Collection<Integer> clubIds, boolean actif, LocalDateTime now);

    // Trouver tous les événements futurs par statut actif/inactif
    List<Event> findByActifAndStartAfter(boolean actif, LocalDateTime startTime);


    long countByOrganisateurIdAndActifAndStartBetween(Integer clubId, boolean actif, LocalDateTime start, LocalDateTime end);

    List<Event> findByOrganisateurIdIn(Set<Integer> memberClubIds);

    List<Event> findByOrganisateurIdInAndActifIsFalse(Set<Integer> memberClubIds);

    List<Event> findByOrganisateurIdInAndActifIsTrue(Set<Integer> memberClubIds);
}

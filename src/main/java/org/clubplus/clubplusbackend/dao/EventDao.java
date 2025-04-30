package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Trouve les 5 prochains événements actifs d’un club, triés par date de début croissante.
     *
     * @param clubId L’ID du club organisateur.
     * @param now    Date actuelle pour filtrer les événements futurs.
     * @return Liste des 5 prochains événements.
     */
    List<Event> findTop5ByOrganisateurIdAndActifTrueAndStartAfterOrderByStartAsc(Integer clubId, LocalDateTime now);
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
    /**
     * Récupère le nombre de réservations (avec statuts spécifiés) et la capacité totale
     * pour chaque événement actif d'un club ayant une capacité > 0.
     */
    @Query("SELECT e.id, " +
            // Compte les réservations dont le statut est dans la liste fournie
            "SUM(CASE WHEN r.status IN :consideredStatuses THEN 1 ELSE 0 END), " +
            "SUM(COALESCE(c.capacite, 0)) " +
            "FROM Event e " +
            "LEFT JOIN e.categories c " +
            "LEFT JOIN c.reservations r " + // LEFT JOIN pour inclure events/cats sans résa
            "WHERE e.organisateur.id = :clubId AND e.actif = true " + // Événements actifs du club
            "GROUP BY e.id " +
            "HAVING SUM(COALESCE(c.capacite, 0)) > 0")
    // Capacité totale > 0
    List<Object[]> findEventStatsForOccupancy(@Param("clubId") Integer clubId,
                                              @Param("consideredStatuses") List<ReservationStatus> consideredStatuses); // Nouveau paramètre


    // --- Ajout pour suppression sécurisée ---

    /**
     * Trouve un événement par son ID, en chargeant EAGERLY ses catégories et les réservations de ces catégories.
     * Nécessaire pour vérifier les réservations avant suppression.
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.categories c LEFT JOIN FETCH c.reservations r WHERE e.id = :eventId")
    Optional<Event> findByIdFetchingCategoriesAndReservations(@Param("eventId") Integer eventId);


    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.categories LEFT JOIN FETCH e.organisateur WHERE e.id = :id")
    Optional<Event> findByIdFetchingCategoriesWithJoinFetch(@Param("id") Integer id);

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

    Page<Event> findByOrganisateurId(Integer clubId, Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.organisateur.id = :clubId
            AND (e.start BETWEEN :dateStart AND :dateEnd)
            """)
    Page<Event> findByOrganisateurIdAndDate(@Param("clubId") Integer clubId,
                                            @Param("dateStart") LocalDateTime dateStart,
                                            @Param("dateSEnd") LocalDateTime dateEnd,
                                            Pageable pageable);

    List<Event> findByOrganisateurIdInAndActifIsFalseAndStartAfter(Set<Integer> memberClubIds, LocalDateTime now);

    List<Event> findByOrganisateurIdInAndActifIsTrueAndStartAfter(Set<Integer> memberClubIds, LocalDateTime now);

    /**
     * AVEC filtre ami - AJOUTER JOIN FETCH
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.categories cat " +     // <-- Charger les catégories
            "LEFT JOIN FETCH e.organisateur org " +   // <-- Charger l'organisateur
            "LEFT JOIN cat.reservations r LEFT JOIN r.membre m " + // Jointures pour le filtre ami
            "WHERE e.organisateur.id IN :clubIds " +
            "AND e.start > :after " +
            "AND (:actifStatus IS NULL OR e.actif = :actifStatus) " +
            "AND m.id IN :friendIds")
    List<Event> findUpcomingEventsInClubsWithStatusAndFriends(
            @Param("clubIds") Collection<Integer> clubIds,
            @Param("after") LocalDateTime after,
            @Param("actifStatus") Boolean actifStatus,
            @Param("friendIds") Collection<Integer> friendIds);

    /**
     * SANS filtre ami - AJOUTER JOIN FETCH
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.categories cat " +     // <-- Charger les catégories
            "LEFT JOIN FETCH e.organisateur org " +   // <-- Charger l'organisateur
            "WHERE e.organisateur.id IN :clubIds " +
            "AND e.start > :after " +
            "AND (:actifStatus IS NULL OR e.actif = :actifStatus)")
    List<Event> findUpcomingEventsInClubsWithStatus(
            @Param("clubIds") Collection<Integer> clubIds,
            @Param("after") LocalDateTime after,
            @Param("actifStatus") Boolean actifStatus);
}

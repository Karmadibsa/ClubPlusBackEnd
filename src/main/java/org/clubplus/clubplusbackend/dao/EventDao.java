package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository pour l'entité {@link Event}.
 * Fournit les opérations CRUD et de nombreuses requêtes pour rechercher et analyser des événements.
 */
@Repository
public interface EventDao extends JpaRepository<Event, Integer> {

    /**
     * Recherche les événements organisés par un club.
     */
    List<Event> findByOrganisateurId(Integer clubId);

    /**
     * Recherche les événements qui commencent après une date/heure donnée.
     */
    List<Event> findByStartTimeAfter(Instant dateTime);

    /**
     * Recherche les événements futurs pour une collection de clubs.
     */
    List<Event> findByOrganisateurIdInAndStartTimeAfter(Collection<Integer> clubIds, Instant now);

    /**
     * Recherche les événements futurs pour un club spécifique.
     */
    List<Event> findByOrganisateurIdAndStartTimeAfter(Integer clubId, Instant now);

    /**
     * Recherche les 5 prochains événements actifs d'un club, triés par date de début.
     */
    List<Event> findTop5ByOrganisateurIdAndActifTrueAndStartTimeAfterOrderByStartTimeAsc(Integer clubId, Instant now);

    /**
     * Récupère les IDs des événements passés et actifs d'un club.
     * <p>
     * Requête optimisée qui ne charge que les IDs.
     */
    @Query("SELECT e.id FROM Event e WHERE e.organisateur.id = :clubId AND e.endTime < :now AND e.actif = true")
    List<Integer> findPastEventIdsByOrganisateurId(@Param("clubId") Integer clubId, @Param("now") Instant now);

    /**
     * Compte les événements d'un club, filtrés par statut actif.
     */
    long countByOrganisateurIdAndActif(Integer clubId, boolean actif);

    /**
     * Récupère des statistiques d'occupation (réservations et capacité) pour les événements actifs d'un club.
     * <p>
     * Ne sont inclus que les événements avec une capacité > 0.
     *
     * @param clubId             L'ID du club.
     * @param consideredStatuses Les statuts de réservation à compter (ex: CONFIRME, UTILISE).
     * @return Une liste de tableaux d'objets : [eventId, reservationCount, totalCapacity].
     */
    @Query("SELECT e.id, " +
            "SUM(CASE WHEN r.status IN :consideredStatuses THEN 1 ELSE 0 END), " +
            "SUM(COALESCE(c.capacite, 0)) " +
            "FROM Event e " +
            "LEFT JOIN e.categories c " +
            "LEFT JOIN c.reservations r " +
            "WHERE e.organisateur.id = :clubId AND e.actif = true " +
            "GROUP BY e.id " +
            "HAVING SUM(COALESCE(c.capacite, 0)) > 0")
    List<Object[]> findEventStatsForOccupancy(@Param("clubId") Integer clubId,
                                              @Param("consideredStatuses") List<ReservationStatus> consideredStatuses);

    /**
     * Recherche un événement par ID, en chargeant ses catégories et son organisateur.
     * <p>
     * Utilise JOIN FETCH pour éviter les problèmes de chargement paresseux.
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.categories LEFT JOIN FETCH e.organisateur WHERE e.id = :id")
    Optional<Event> findByIdFetchingCategoriesWithJoinFetch(@Param("id") Integer id);

    /**
     * Recherche les événements par leur statut actif/inactif.
     */
    List<Event> findByActif(boolean actif);

    /**
     * Recherche les événements d'un organisateur, filtrés par statut actif.
     */
    List<Event> findByOrganisateurIdAndActif(Integer organisateurId, boolean actif);

    /**
     * Recherche les événements futurs et actifs d'un organisateur.
     */
    List<Event> findByOrganisateurIdAndActifAndStartTimeAfter(Integer organisateurId, boolean actif, Instant startTime);

    /**
     * Recherche les événements futurs et actifs pour une liste de clubs.
     */
    List<Event> findByOrganisateurIdInAndActifAndStartTimeAfter(Collection<Integer> clubIds, boolean actif, Instant now);

    /**
     * Recherche les événements futurs, filtrés par statut actif.
     */
    List<Event> findByActifAndStartTimeAfter(boolean actif, Instant startTime);

    /**
     * Compte les événements actifs d'un club dans un intervalle de temps.
     */
    long countByOrganisateurIdAndActifAndStartTimeBetween(Integer clubId, boolean actif, Instant start, Instant end);

    /**
     * Recherche une page d'événements pour un club dans un intervalle de dates.
     */
    @Query("SELECT e FROM Event e WHERE e.organisateur.id = :clubId AND (e.startTime BETWEEN :dateStart AND :dateEnd)")
    Page<Event> findByOrganisateurIdAndDate(@Param("clubId") Integer clubId,
                                            @Param("dateStart") Instant dateStart,
                                            @Param("dateEnd") Instant dateEnd,
                                            Pageable pageable);

    /**
     * Recherche les événements futurs et inactifs pour un ensemble de clubs.
     */
    List<Event> findByOrganisateurIdInAndActifIsFalseAndStartTimeAfter(Set<Integer> memberClubIds, Instant now);

    /**
     * Recherche les événements futurs et actifs pour un ensemble de clubs.
     */
    List<Event> findByOrganisateurIdInAndActifIsTrueAndStartTimeAfter(Set<Integer> memberClubIds, Instant now);

    /**
     * Recherche les événements futurs pour des clubs, avec un statut optionnel et la participation d'amis.
     * <p>
     * Utilise JOIN FETCH pour optimiser le chargement des données.
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.categories cat " +
            "LEFT JOIN FETCH e.organisateur org " +
            "LEFT JOIN cat.reservations r LEFT JOIN r.membre m " +
            "WHERE e.organisateur.id IN :clubIds " +
            "AND e.startTime > :after " +
            "AND (:actifStatus IS NULL OR e.actif = :actifStatus) " +
            "AND m.id IN :friendIds")
    List<Event> findUpcomingEventsInClubsWithStatusAndFriends(
            @Param("clubIds") Collection<Integer> clubIds,
            @Param("after") Instant after,
            @Param("actifStatus") Boolean actifStatus,
            @Param("friendIds") Collection<Integer> friendIds);

    /**
     * Recherche les événements futurs pour des clubs, avec un statut optionnel, sans filtre sur les amis.
     * <p>
     * Utilise JOIN FETCH pour optimiser le chargement des données.
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.categories cat " +
            "LEFT JOIN FETCH e.organisateur org " +
            "WHERE e.organisateur.id IN :clubIds " +
            "AND e.startTime > :after " +
            "AND (:actifStatus IS NULL OR e.actif = :actifStatus)")
    List<Event> findUpcomingEventsInClubsWithStatus(
            @Param("clubIds") Collection<Integer> clubIds,
            @Param("after") Instant after,
            @Param("actifStatus") Boolean actifStatus);
}

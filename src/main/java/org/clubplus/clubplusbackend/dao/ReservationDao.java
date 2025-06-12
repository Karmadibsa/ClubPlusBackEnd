package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité {@link Reservation}.
 * Fournit les opérations CRUD et des requêtes pour gérer les réservations.
 */
@Repository
public interface ReservationDao extends JpaRepository<Reservation, Integer> {

    /**
     * Recherche toutes les réservations d'un membre.
     */
    List<Reservation> findByMembreId(Integer membreId);

    /**
     * Recherche toutes les réservations d'un événement.
     */
    List<Reservation> findByEventId(Integer eventId);

    /**
     * Recherche toutes les réservations d'une catégorie.
     */
    List<Reservation> findByCategorieId(Integer categorieId);

    /**
     * Recherche une réservation par son UUID unique.
     * <p>
     * Utile pour identifier une réservation via une référence externe (ex: QR code).
     */
    Optional<Reservation> findByReservationUuid(String uuid);

    /**
     * Compte les réservations d'un membre pour un événement, filtrées par statut.
     */
    long countByMembreIdAndEventIdAndStatus(Integer membreId, Integer eventId, ReservationStatus status);

    /**
     * Recherche les réservations d'un membre, filtrées par statut.
     */
    List<Reservation> findByMembreIdAndStatus(Integer currentUserId, ReservationStatus status);

    /**
     * Recherche les réservations d'un événement, filtrées par statut.
     */
    List<Reservation> findByEventIdAndStatus(Integer eventId, ReservationStatus status);

    /**
     * Vérifie si un membre a une réservation pour un événement, avec un statut donné.
     */
    boolean existsByMembreIdAndEventIdAndStatus(Integer currentUserId, Integer eventId, ReservationStatus reservationStatus);

    /**
     * Compte les réservations pour un club, filtrées par statut.
     */
    long countByStatusAndEventOrganisateurId(ReservationStatus status, Integer clubId);

    /**
     * Recherche les réservations confirmées pour une liste d'événements et de membres.
     * <p>
     * Utilise JOIN FETCH pour charger les membres et événements associés, optimisant les accès futurs.
     *
     * @param eventIds  IDs des événements à inclure.
     * @param memberIds IDs des membres à inclure.
     * @param status    Le statut des réservations à rechercher.
     * @return Une liste de réservations avec leurs membres et événements chargés.
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.membre m JOIN FETCH r.event e " +
            "WHERE e.id IN :eventIds " +
            "AND m.id IN :memberIds " +
            "AND r.status = :status")
    List<Reservation> findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember(
            @Param("eventIds") Collection<Integer> eventIds,
            @Param("memberIds") Collection<Integer> memberIds,
            @Param("status") ReservationStatus status
    );

    /**
     * Trouve les réservations futures d'un membre pour un statut donné.
     */
    List<Reservation> findByMembreIdAndStatusAndEvent_EndTimeAfter(Integer membreId, ReservationStatus status, Instant currentTime);

    /**
     * Trouve toutes les réservations futures d'un membre.
     */
    List<Reservation> findByMembreIdAndEvent_EndTimeAfter(Integer membreId, Instant currentTime);
}

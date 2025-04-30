package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationDao extends JpaRepository<Reservation, Integer> {

    /**
     * Trouve toutes les réservations d'un membre.
     */
    List<Reservation> findByMembreId(Integer membreId);

    /**
     * Trouve toutes les réservations pour un événement.
     */
    List<Reservation> findByEventId(Integer eventId);

    /**
     * Trouve toutes les réservations pour une catégorie.
     */
    List<Reservation> findByCategorieId(Integer categorieId);

    /**
     * Trouve TOUTES les réservations d'un membre pour un événement donné (utile si > 1 possible).
     */
    List<Reservation> findAllByMembreIdAndEventId(Integer membreId, Integer eventId);

    /**
     * Compte les réservations pour une catégorie (vérification capacité).
     */
    long countByCategorieId(Integer categorieId);


    /**
     * Compte les réservations d'un membre pour un événement (vérification limite).
     */
    long countByMembreIdAndEventId(Integer membreId, Integer eventId);

    /**
     * Vérifie si un membre a participé à un événement (pour NotationService).
     */
    boolean existsByMembreIdAndEventId(Integer membreId, Integer eventId);

    // Optionnel: Trouver par UUID si nécessaire pour une API externe/validation QR code
    Optional<Reservation> findByReservationUuid(String uuid);

    long countByMembreIdAndEventIdAndStatus(Integer membreId, Integer membreId1, ReservationStatus status);

    List<Reservation> findByMembreIdAndStatus(Integer currentUserId, ReservationStatus status);

    List<Reservation> findByEventIdAndStatus(Integer eventId, ReservationStatus status);


    boolean existsByMembreIdAndEventIdAndStatus(Integer currentUserId, Integer eventId, ReservationStatus reservationStatus);

    /**
     * Compte le nombre de réservations pour un club donné ayant un statut spécifique.
     *
     * @param status Le statut de réservation recherché (ex: "utilisé").
     * @param clubId L'identifiant du club.
     * @return Le nombre de réservations correspondantes.
     */
    long countByStatusAndEventOrganisateurId(ReservationStatus status, Integer clubId);

    /**
     * Trouve les réservations confirmées pour une liste d'IDs d'événements
     * et une liste d'IDs de membres (amis), en chargeant (fetch)
     * les informations nécessaires du Membre (pour nom/prénom).
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.membre m JOIN FETCH r.event e " + // JOIN FETCH pour charger Membre et Event
            "WHERE e.id IN :eventIds " +
            "AND m.id IN :memberIds " +
            "AND r.status = :status")
    // Filtrer par statut CONFIRME
    List<Reservation> findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember(
            @Param("eventIds") Collection<Integer> eventIds,
            @Param("memberIds") Collection<Integer> memberIds,
            @Param("status") ReservationStatus status // Passer ReservationStatus.CONFIRME
    );
}

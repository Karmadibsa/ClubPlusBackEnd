package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

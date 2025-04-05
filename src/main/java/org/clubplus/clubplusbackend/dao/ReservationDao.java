package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationDao extends JpaRepository<Reservation, Long> {
    List<Reservation> findByMembreId(Long membreId);

    List<Reservation> findByEventId(Long eventId);

    List<Reservation> findByCategorieId(Long categorieId);

    // Compter les réservations d'un membre pour un événement
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.membre.id = ?1 AND r.event.id = ?2")
    int countByMembreIdAndEventId(Long membreId, Long eventId);

    List<Reservation> findByMembreIdAndEventId(Long membreId, Long eventId);
}

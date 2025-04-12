package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventDao extends JpaRepository<Event, Integer> {
    // Trouver les événements par nom (recherche simple)
    List<Event> findByNomContainingIgnoreCase(String nom);

    // Trouver les événements organisés par un club spécifique
    List<Event> findByOrganisateurId(Integer clubId);

    // Trouver les événements qui se déroulent après une certaine date/heure
    List<Event> findByStartAfter(LocalDateTime dateTime);

    // Trouver les événements qui se terminent avant une certaine date/heure
    List<Event> findByEndBefore(LocalDateTime dateTime);

    // Trouver les événements actifs à un moment donné (entre start et end)
    @Query("SELECT e FROM Event e WHERE e.start <= :now AND e.end >= :now")
    List<Event> findActiveEvents(@Param("now") LocalDateTime now);

    // Trouver les événements dans une plage de dates
    List<Event> findByStartBetween(LocalDateTime startTime, LocalDateTime endTime);

    // Compte les événements commençant dans une période donnée
    long countByStartBetween(LocalDateTime startDate, LocalDateTime endDate);


    List<Event> findByOrganisateurIdAndStartAfter(Integer clubId, LocalDateTime now);
    

}

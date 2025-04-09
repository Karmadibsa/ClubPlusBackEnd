package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventDao extends JpaRepository<Event, Long> {
    List<Event> findByOrganisateurId(Long clubId);

}

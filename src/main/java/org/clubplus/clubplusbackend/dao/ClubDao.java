package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubDao extends JpaRepository<Club, Long> {
    Optional<Club> findByCodeClub(String codeClub);

}

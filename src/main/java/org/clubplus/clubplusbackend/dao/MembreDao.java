package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembreDao extends JpaRepository<Membre, Long> {

    Optional<Membre> findByEmail(String email);

//    long countByDateInscriptionBetween(LocalDate start, LocalDate end);

}

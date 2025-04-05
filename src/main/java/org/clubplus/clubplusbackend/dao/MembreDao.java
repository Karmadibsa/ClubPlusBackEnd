package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembreDao extends JpaRepository<Membre, Long> {
}

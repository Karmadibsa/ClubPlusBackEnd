package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieDao extends JpaRepository<Categorie, Long> {
    List<Categorie> findByEventId(Long eventId);

}

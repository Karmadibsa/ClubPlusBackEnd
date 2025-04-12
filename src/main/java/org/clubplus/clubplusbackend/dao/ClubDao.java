package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubDao extends JpaRepository<Club, Integer> {
    // Trouver un club par son code unique (généré automatiquement)
    Optional<Club> findByCodeClub(String codeClub);

    // Trouver des clubs par ville (recherche fréquente)
    List<Club> findByVilleContainingIgnoreCase(String ville);

    // Trouver un club par l'ID de son administrateur (si nécessaire)
    Optional<Club> findByAdminId(Integer adminId);

    // Vérifier si un club existe avec un certain email (pour validation)
    boolean existsByEmail(String email);


}

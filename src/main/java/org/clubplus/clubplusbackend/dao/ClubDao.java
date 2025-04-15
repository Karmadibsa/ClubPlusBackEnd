package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubDao extends JpaRepository<Club, Integer> {

    /**
     * Trouve un club par son code unique (généré).
     */
    Optional<Club> findByCodeClub(String codeClub);

    /**
     * Trouve des clubs dont la ville contient la chaîne fournie (ignorant la casse).
     */
    List<Club> findByVilleContainingIgnoreCase(String ville);

    /**
     * Trouve des clubs dont le nom contient la chaîne fournie (ignorant la casse).
     */
    List<Club> findByNomContainingIgnoreCase(String nom);

    /**
     * Vérifie si un club existe avec un email donné (pour validation unicité).
     */
    boolean existsByEmail(String email);

    /**
     * Trouve un club par son email exact.
     */
    Optional<Club> findByEmail(String email); // Type de retour Optional<Club> est correct

    /**
     * Vérifie s'il existe un AUTRE club (ID différent) avec le même email.
     * Utile pour la validation lors de la mise à jour.
     */
    boolean existsByEmailAndIdNot(String email, Integer id);
}

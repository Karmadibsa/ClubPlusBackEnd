package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MembreDao extends JpaRepository<Membre, Integer> {

    /**
     * Trouve un membre par email (pour login, validation).
     */
    Optional<Membre> findByEmail(String email);

    /**
     * Vérifie si un email est déjà utilisé (pour validation création/update).
     */
    boolean existsByEmail(String email); // AJOUTÉ

    /**
     * Vérifie si un email est utilisé par un AUTRE membre (pour validation update).
     */
    boolean existsByEmailAndIdNot(String email, Integer id); // AJOUTÉ

    /**
     * Trouve l'unique ADMIN d'un club.
     */
    @Query("SELECT m FROM Membre m JOIN m.adhesions a WHERE a.club.id = :clubId AND m.role = org.clubplus.clubplusbackend.model.Role.ADMIN")
    Optional<Membre> findAdminByClubId(@Param("clubId") Integer clubId);

    /**
     * Récupère tous les membres d'un club (avec leurs adhésions).
     */
    @Query("SELECT DISTINCT m FROM Membre m JOIN FETCH m.adhesions a WHERE a.club.id = :clubId")
    Set<Membre> findMembresByClubIdWithAdhesions(@Param("clubId") Integer clubId); // Renommé pour clarté

    // Optionnel: Requête pour les statistiques globales d'inscription (si nécessaire)
    @Query("SELECT FUNCTION('YEAR', m.date_inscription), FUNCTION('MONTH', m.date_inscription), COUNT(m.id) " +
            "FROM Membre m " +
            "WHERE m.date_inscription >= :startDate " +
            "GROUP BY FUNCTION('YEAR', m.date_inscription), FUNCTION('MONTH', m.date_inscription) " +
            "ORDER BY FUNCTION('YEAR', m.date_inscription) ASC, FUNCTION('MONTH', m.date_inscription) ASC")
    List<Object[]> findMonthlyRegistrationsSince(@Param("startDate") LocalDate startDate);

    List<Membre> findByAdhesionsClubId(Integer clubId);

    Optional<Membre> findByCodeAmi(String codeAmi);

}

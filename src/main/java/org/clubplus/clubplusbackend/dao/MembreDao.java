package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembreDao extends JpaRepository<Membre, Integer> {

    Optional<Membre> findByEmail(String email);

    List<Membre> findByClubId(Integer id);

    long countByClubId(Integer clubId);

    // Retourne une liste de tableaux [Année, Mois, Compte] pour les inscriptions mensuelles
    @Query("SELECT FUNCTION('YEAR', m.date_inscription), FUNCTION('MONTH', m.date_inscription), COUNT(m.id) " +
            "FROM Membre m " +
            "WHERE m.date_inscription >= :startDate " + // startDate = 12 mois avant aujourd'hui
            "GROUP BY FUNCTION('YEAR', m.date_inscription), FUNCTION('MONTH', m.date_inscription) " +
            "ORDER BY FUNCTION('YEAR', m.date_inscription) ASC, FUNCTION('MONTH', m.date_inscription) ASC")
    List<Object[]> findMonthlyRegistrationsSince(@Param("startDate") LocalDate startDate);

}

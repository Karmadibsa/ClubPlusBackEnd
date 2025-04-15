package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Adhesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdhesionDao extends JpaRepository<Adhesion, Integer> {

    // --- Méthodes pour trouver des adhésions spécifiques ---

    /**
     * Trouve toutes les adhésions d'un membre donné.
     */
    List<Adhesion> findByMembreId(Integer membreId);

    /**
     * Trouve toutes les adhésions pour un club donné.
     */
    List<Adhesion> findByClubId(Integer clubId);

    /**
     * Trouve l'adhésion unique (si elle existe) entre un membre et un club.
     */
    Optional<Adhesion> findByMembreIdAndClubId(Integer membreId, Integer clubId);

    // --- Méthodes pour vérifier l'existence ---

    /**
     * Vérifie si un membre a une adhésion à un club donné. Très utile pour les contrôles d'accès.
     */
    boolean existsByMembreIdAndClubId(Integer membreId, Integer clubId);

    /**
     * Vérifie si un club a au moins une adhésion (donc au moins un membre).
     */
    boolean existsByClubId(Integer clubId);

    // --- Méthodes pour compter ---

    /**
     * Compte le nombre total d'adhésions (membres) dans un club.
     */
    long countByClubId(Integer clubId);

    /**
     * Compte le nombre de clubs auxquels un membre est affilié.
     */
    long countByMembreId(Integer membreId);


    // --- Méthode pour Statistiques ---

    /**
     * Récupère le compte mensuel des nouvelles adhésions pour un club spécifique
     * depuis une date donnée.
     * Utilise les fonctions YEAR et MONTH de la base de données (via FUNCTION).
     *
     * @param clubId    ID du club.
     * @param startDate Date de début (inclusive).
     * @return Liste de Object[]: [Année(Integer), Mois(Integer), Compte(Number)].
     * Le type de Compte peut varier (Long, BigInteger...).
     */
    @Query("SELECT FUNCTION('YEAR', a.dateAdhesion), FUNCTION('MONTH', a.dateAdhesion), COUNT(a.id) " +
            "FROM Adhesion a " +
            "WHERE a.club.id = :clubId AND a.dateAdhesion >= :startDate " +
            "GROUP BY FUNCTION('YEAR', a.dateAdhesion), FUNCTION('MONTH', a.dateAdhesion) " +
            "ORDER BY FUNCTION('YEAR', a.dateAdhesion) ASC, FUNCTION('MONTH', a.dateAdhesion) ASC")
    List<Object[]> findMonthlyAdhesionsToClubSince(@Param("clubId") Integer clubId, @Param("startDate") LocalDateTime startDate);

    // --- Ajout potentiel pour SecurityService ---

    /**
     * Trouve l'ID et le rôle d'un membre via son adhésion à un club.
     * Utile pour vérifier rapidement si un utilisateur est ADMIN ou RESERVATION d'un club.
     * NOTE: Renvoie Optional<Object[]> car un membre peut ne pas avoir d'adhésion.
     */
    @Query("SELECT m.id, m.role FROM Adhesion a JOIN a.membre m WHERE a.membre.id = :membreId AND a.club.id = :clubId")
    Optional<Object[]> findMembreIdAndRoleByMembreIdAndClubId(@Param("membreId") Integer membreId, @Param("clubId") Integer clubId);
}

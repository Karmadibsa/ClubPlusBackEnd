package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité {@link Adhesion}.
 * Fournit les opérations CRUD et des requêtes personnalisées pour la gestion des adhésions.
 */
@Repository
public interface AdhesionDao extends JpaRepository<Adhesion, Integer> {

    /**
     * Recherche une adhésion spécifique entre un membre et un club.
     *
     * @param membreId L'ID du {@link Membre}.
     * @param clubId   L'ID du {@link Club}.
     * @return Un {@link Optional} contenant l'adhésion si elle existe.
     */
    Optional<Adhesion> findByMembreIdAndClubId(Integer membreId, Integer clubId);

    /**
     * Vérifie si une adhésion existe entre un membre et un club.
     * <p>
     * Plus performant que {@code findByMembreIdAndClubId().isPresent()} pour un simple test d'existence.
     *
     * @param membreId L'ID du {@link Membre}.
     * @param clubId   L'ID du {@link Club}.
     * @return {@code true} si une adhésion existe, {@code false} sinon.
     */
    boolean existsByMembreIdAndClubId(Integer membreId, Integer clubId);

    /**
     * Compte le nombre de clubs auxquels un membre adhère.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Le nombre total d'adhésions.
     */
    long countByMembreId(Integer membreId);

    /**
     * Calcule le décompte mensuel des nouvelles adhésions pour un club depuis une date donnée.
     *
     * @param clubId    L'ID du club.
     * @param startDate La date de début pour le calcul.
     * @return Une liste de tableaux d'objets [année (Integer), mois (Integer), nombre (Number)].
     */
    @Query("SELECT FUNCTION('YEAR', a.dateAdhesion), FUNCTION('MONTH', a.dateAdhesion), COUNT(a.id) " +
            "FROM Adhesion a " +
            "WHERE a.club.id = :clubId AND a.dateAdhesion >= :startDate " +
            "GROUP BY FUNCTION('YEAR', a.dateAdhesion), FUNCTION('MONTH', a.dateAdhesion) " +
            "ORDER BY FUNCTION('YEAR', a.dateAdhesion) ASC, FUNCTION('MONTH', a.dateAdhesion) ASC")
    List<Object[]> findMonthlyAdhesionsToClubSince(@Param("clubId") Integer clubId, @Param("startDate") Instant startDate);

    /**
     * Récupère les IDs de tous les clubs auxquels un membre adhère.
     * <p>
     * Requête optimisée qui ne charge pas les entités Club complètes.
     *
     * @param membreId L'ID du membre.
     * @return Une liste d'IDs de clubs.
     */
    @Query("SELECT a.club.id FROM Adhesion a WHERE a.membre.id = :membreId")
    List<Integer> findClubIdsByMembreId(@Param("membreId") Integer membreId);

    /**
     * Compte le nombre de membres actifs dans un club.
     *
     * @param clubId L'ID du club.
     * @return Le nombre de membres actifs.
     */
    @Query("SELECT COUNT(a) FROM Adhesion a JOIN a.membre m WHERE a.club.id = :clubId AND m.actif = true")
    long countActiveMembersByClubId(@Param("clubId") Integer clubId);

    /**
     * Récupère les IDs des clubs actifs auxquels un membre adhère.
     *
     * @param membreId L'ID du membre.
     * @return Une liste d'IDs de clubs actifs.
     */
    @Query("SELECT a.club.id FROM Adhesion a WHERE a.membre.id = :membreId AND a.club.actif = true")
    List<Integer> findActiveClubIdsByMembreId(@Param("membreId") Integer membreId);

    /**
     * Récupère les adhésions les plus récentes de membres actifs pour un club, avec une limite.
     * <p>
     * Utilise {@code JOIN FETCH} pour charger les membres associés et éviter le N+1 select.
     *
     * @param clubId L'ID du club.
     * @param limit  Le nombre maximum de résultats à retourner.
     * @return Une liste limitée des adhésions les plus récentes.
     */
    @Query("SELECT a FROM Adhesion a JOIN FETCH a.membre m WHERE a.club.id = :clubId AND m.actif = true ORDER BY a.dateAdhesion DESC")
    List<Adhesion> findLatestActiveMembersAdhesionsWithLimit(@Param("clubId") Integer clubId, Limit limit);

}

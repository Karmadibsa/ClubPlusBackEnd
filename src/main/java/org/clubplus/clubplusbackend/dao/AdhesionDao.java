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
 * Interface Repository pour l'entité {@link Adhesion}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} ainsi que des méthodes
 * personnalisées pour interroger les données d'adhésion, vérifier l'existence,
 * et récupérer des statistiques.
 *
 * @see Adhesion
 * @see JpaRepository
 */
@Repository // Indique que c'est un bean Repository géré par Spring.
public interface AdhesionDao extends JpaRepository<Adhesion, Integer> {

    /**
     * Recherche l'adhésion unique (si elle existe) entre un membre spécifique et un club spécifique.
     * Utilise les identifiants du membre et du club.
     *
     * @param membreId L'ID du {@link Membre}.
     * @param clubId   L'ID du {@link Club}.
     * @return Un {@link Optional} contenant l'{@link Adhesion} si trouvée, sinon un Optional vide.
     * Utile pour vérifier si une adhésion existe déjà avant d'en créer une nouvelle.
     */
    Optional<Adhesion> findByMembreIdAndClubId(Integer membreId, Integer clubId);

    /**
     * Vérifie de manière optimisée si une adhésion existe entre un membre spécifique et un club spécifique.
     * Préférable à {@code findByMembreIdAndClubId().isPresent()} si seul le test d'existence est requis.
     *
     * @param membreId L'ID du {@link Membre}.
     * @param clubId   L'ID du {@link Club}.
     * @return {@code true} si une adhésion existe pour cette paire membre/club, {@code false} sinon.
     * Très utilisé dans les logiques de contrôle d'accès (ex: {@code SecurityService}).
     */
    boolean existsByMembreIdAndClubId(Integer membreId, Integer clubId);

    /**
     * Compte le nombre total d'adhésions (donc de clubs) pour un membre spécifique.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Le nombre total ({@code long}) de clubs auxquels le membre adhère.
     */
    long countByMembreId(Integer membreId);

    /**
     * Récupère le décompte mensuel des nouvelles adhésions pour un club spécifique,
     * à partir d'une date donnée.
     * Utilise les fonctions SQL {@code YEAR} et {@code MONTH} (via {@code FUNCTION}) pour agréger les résultats.
     * Les résultats sont groupés par année et par mois, puis triés chronologiquement.
     *
     * @param clubId    L'ID du {@link Club} pour lequel compter les adhésions.
     * @param startDate La date et heure à partir desquelles inclure les adhésions (inclusive).
     * @return Une liste de tableaux d'objets ({@code List<Object[]>}). Chaque tableau contient :
     * <ul>
     *     <li>Index 0: L'année ({@link Integer})</li>
     *     <li>Index 1: Le mois ({@link Integer}, 1-12)</li>
     *     <li>Index 2: Le nombre d'adhésions pour ce mois/année ({@link Number}, typiquement Long ou BigInteger)</li>
     * </ul>
     * La liste est vide si aucune adhésion n'est trouvée pour la période/club.
     */
    @Query("SELECT FUNCTION('YEAR', a.dateAdhesion), FUNCTION('MONTH', a.dateAdhesion), COUNT(a.id) " +
            "FROM Adhesion a " +
            "WHERE a.club.id = :clubId AND a.dateAdhesion >= :startDate " +
            "GROUP BY FUNCTION('YEAR', a.dateAdhesion), FUNCTION('MONTH', a.dateAdhesion) " +
            "ORDER BY FUNCTION('YEAR', a.dateAdhesion) ASC, FUNCTION('MONTH', a.dateAdhesion) ASC")
    List<Object[]> findMonthlyAdhesionsToClubSince(@Param("clubId") Integer clubId, @Param("startDate") Instant startDate);

    /**
     * Récupère uniquement les identifiants (IDs) des clubs auxquels un membre spécifique adhère.
     * C'est une requête optimisée qui évite de charger les entités {@link Club} complètes
     * lorsque seuls les IDs sont nécessaires.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Une liste d'{@link Integer} représentant les IDs des clubs du membre.
     * La liste est vide si le membre n'a aucune adhésion.
     */
    @Query("SELECT a.club.id FROM Adhesion a WHERE a.membre.id = :membreId")
    List<Integer> findClubIdsByMembreId(@Param("membreId") Integer membreId);

    /**
     * Compte le nombre de membres **actifs** ({@code membre.actif = true}) dans un club spécifique.
     *
     * @param clubId L'ID du {@link Club}.
     * @return Le nombre total ({@code long}) de membres actifs dans le club.
     */
    @Query("SELECT COUNT(a) FROM Adhesion a JOIN a.membre m WHERE a.club.id = :clubId AND m.actif = true")
    long countActiveMembersByClubId(@Param("clubId") Integer clubId);

    /**
     * Récupère les identifiants (IDs) des clubs **actifs** ({@code club.actif = true})
     * auxquels un membre spécifique adhère.
     * Filtre les clubs qui auraient pu être désactivés après l'adhésion du membre.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Une liste d'{@link Integer} représentant les IDs des clubs actifs du membre.
     */
    @Query("SELECT a.club.id FROM Adhesion a WHERE a.membre.id = :membreId AND a.club.actif = true")
    List<Integer> findActiveClubIdsByMembreId(@Param("membreId") Integer membreId);

    /**
     * Récupère les adhésions les plus récentes pour un club donné, en ne considérant
     * que les membres actuellement actifs ({@code membre.actif = true}).
     * Utilise {@code JOIN FETCH} pour charger également les données des membres associés
     * dans la même requête (optimisation EAGER pour ce cas d'usage).
     * Les résultats sont triés par date d'adhésion décroissante (les plus récents d'abord).
     * Le nombre de résultats retournés est limité par le paramètre {@code limit}.
     *
     * @param clubId L'ID du {@link Club}.
     * @param limit  Un objet {@link Limit} spécifiant le nombre maximum d'adhésions à retourner.
     * @return Une liste d'{@link Adhesion} (avec les {@link Membre}s associés chargés), limitée en taille.
     * @see Limit
     */
    @Query("SELECT a FROM Adhesion a JOIN FETCH a.membre m WHERE a.club.id = :clubId AND m.actif = true ORDER BY a.dateAdhesion DESC")
    List<Adhesion> findLatestActiveMembersAdhesionsWithLimit(@Param("clubId") Integer clubId, Limit limit);

}

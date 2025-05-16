package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface Repository pour l'entité {@link Event}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} ainsi qu'un large éventail
 * de méthodes personnalisées pour rechercher, compter et récupérer des statistiques sur les événements
 * selon divers critères (organisateur, dates, statut, relations avec d'autres entités).
 *
 * @see Event
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface EventDao extends JpaRepository<Event, Integer> {

    /**
     * Recherche tous les événements organisés par un club spécifique.
     *
     * @param clubId L'ID du {@link Club} organisateur.
     * @return Une liste des {@link Event}s organisés par ce club.
     */
    List<Event> findByOrganisateurId(Integer clubId);

    /**
     * Recherche tous les événements dont la date de début est postérieure à la date/heure spécifiée.
     * Utile pour trouver les événements futurs.
     *
     * @param dateTime La date et heure de référence.
     * @return Une liste des {@link Event}s commençant après la date/heure fournie.
     */
    List<Event> findByStartTimeAfter(LocalDateTime dateTime);

    // --- Recherche Combinée ---

    /**
     * Recherche les événements futurs ({@code start} après {@code now}) organisés par
     * n'importe lequel des clubs dont les IDs sont fournis dans la collection.
     *
     * @param clubIds Une collection des IDs des {@link Club}s organisateurs.
     * @param now     La date et heure actuelle pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs correspondants.
     */
    List<Event> findByOrganisateurIdInAndStartTimeAfter(Collection<Integer> clubIds, LocalDateTime now);

    /**
     * Recherche les événements futurs ({@code start} après {@code now}) organisés par
     * un club spécifique.
     *
     * @param clubId L'ID du {@link Club} organisateur.
     * @param now    La date et heure actuelle pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs correspondants.
     */
    List<Event> findByOrganisateurIdAndStartTimeAfter(Integer clubId, LocalDateTime now);

    /**
     * Recherche les 5 prochains événements actifs ({@code actif = true}) organisés par un club spécifique,
     * triés par date de début croissante (le plus proche en premier).
     *
     * @param clubId L'ID du {@link Club} organisateur.
     * @param now    La date et heure actuelle pour filtrer les événements futurs.
     * @return Une liste contenant jusqu'à 5 des prochains {@link Event}s actifs du club.
     */
    List<Event> findTop5ByOrganisateurIdAndActifTrueAndStartTimeAfterOrderByStartTimeAsc(Integer clubId, LocalDateTime now);

    // --- Requêtes Spécifiques ---

    /**
     * Récupère uniquement les identifiants (IDs) des événements actifs ({@code actif = true})
     * organisés par un club spécifique et dont la date de fin est antérieure à maintenant (événements passés).
     * Requête optimisée ne chargeant que les IDs.
     *
     * @param clubId L'ID du {@link Club} organisateur.
     * @param now    La date et heure actuelle pour déterminer si un événement est passé.
     * @return Une liste d'{@link Integer} représentant les IDs des événements passés et actifs du club.
     */
    @Query("SELECT e.id FROM Event e WHERE e.organisateur.id = :clubId AND e.endTime < :now AND e.actif = true")
    List<Integer> findPastEventIdsByOrganisateurId(@Param("clubId") Integer clubId, @Param("now") LocalDateTime now);

    /**
     * Compte le nombre d'événements pour un club spécifique, en fonction de leur statut actif ou inactif.
     *
     * @param clubId L'ID du {@link Club} organisateur.
     * @param actif  {@code true} pour compter les événements actifs, {@code false} pour les inactifs.
     * @return Le nombre ({@code long}) d'événements correspondants.
     */
    long countByOrganisateurIdAndActif(Integer clubId, boolean actif);

    /**
     * Récupère des statistiques agrégées pour les événements actifs d'un club donné,
     * spécifiquement le nombre de réservations (pour certains statuts) et la capacité totale,
     * pour les événements ayant une capacité totale supérieure à zéro.
     * <p>
     * Utilise des {@code LEFT JOIN} pour inclure les événements/catégories même sans réservations.
     * Le compte des réservations est conditionnel basé sur la liste de {@code consideredStatuses} fournie.
     * La capacité totale est la somme des capacités de toutes les catégories de l'événement.
     * </p>
     *
     * @param clubId             L'ID du {@link Club} organisateur.
     * @param consideredStatuses Une liste des {@link ReservationStatus} à inclure dans le décompte des réservations
     *                           (ex: CONFIRME, peut-être UTILISE selon la logique métier).
     * @return Une liste de tableaux d'objets ({@code List<Object[]>}). Chaque tableau représente un événement et contient :
     * <ul>
     *     <li>Index 0: L'ID de l'événement ({@link Integer})</li>
     *     <li>Index 1: Le nombre total de réservations pour cet événement ayant un statut dans {@code consideredStatuses} ({@link Number}, typiquement Long ou BigInteger). Vaut 0 si aucune réservation correspondante.</li>
     *     <li>Index 2: La capacité totale de l'événement (somme des capacités de ses catégories) ({@link Number}, typiquement Long ou BigInteger).</li>
     * </ul>
     * Seuls les événements actifs avec une capacité totale > 0 sont inclus.
     */
    @Query("SELECT e.id, " +
            // Compte conditionnellement les réservations basées sur les statuts fournis.
            "SUM(CASE WHEN r.status IN :consideredStatuses THEN 1 ELSE 0 END), " +
            // Somme des capacités des catégories, gérant le cas où une catégorie n'aurait pas de capacité définie (COALESCE).
            "SUM(COALESCE(c.capacite, 0)) " +
            "FROM Event e " +
            "LEFT JOIN e.categories c " + // Jointure pour accéder aux catégories
            "LEFT JOIN c.reservations r " + // Jointure pour accéder aux réservations (LEFT pour inclure events/cats sans résa)
            "WHERE e.organisateur.id = :clubId AND e.actif = true " + // Filtre sur le club et le statut actif de l'event
            "GROUP BY e.id " + // Aggrège par événement
            "HAVING SUM(COALESCE(c.capacite, 0)) > 0")
    // Ne garde que les événements avec une capacité > 0
    List<Object[]> findEventStatsForOccupancy(@Param("clubId") Integer clubId,
                                              @Param("consideredStatuses") List<ReservationStatus> consideredStatuses);

    /**
     * Recherche un événement par son ID en chargeant impérativement (via {@code LEFT JOIN FETCH})
     * ses {@link org.clubplus.clubplusbackend.model.Categorie}s et son {@link Club} organisateur associés.
     * Utile pour éviter les problèmes de chargement paresseux lors de l'affichage des détails d'un événement.
     *
     * @param id L'ID de l'{@link Event} à rechercher.
     * @return Un {@link Optional} contenant l'{@link Event} avec ses catégories et organisateur chargés,
     * ou un Optional vide si non trouvé.
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.categories LEFT JOIN FETCH e.organisateur WHERE e.id = :id")
    Optional<Event> findByIdFetchingCategoriesWithJoinFetch(@Param("id") Integer id);

    // --- Ajouts pour filtrage par statut ---

    /**
     * Recherche tous les événements en fonction de leur statut actif/inactif.
     *
     * @param actif {@code true} pour les actifs, {@code false} pour les inactifs.
     * @return Une liste des {@link Event}s correspondants.
     */
    List<Event> findByActif(boolean actif);

    /**
     * Recherche les événements d'un organisateur spécifique en fonction de leur statut actif/inactif.
     *
     * @param organisateurId L'ID du {@link Club} organisateur.
     * @param actif          {@code true} pour les actifs, {@code false} pour les inactifs.
     * @return Une liste des {@link Event}s correspondants.
     */
    List<Event> findByOrganisateurIdAndActif(Integer organisateurId, boolean actif);

    /**
     * Recherche les événements futurs ({@code start} après {@code startTime}) et actifs
     * ({@code actif = true}) d'un organisateur spécifique.
     *
     * @param organisateurId L'ID du {@link Club} organisateur.
     * @param actif          Doit être {@code true}.
     * @param startTime      La date et heure de référence pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs et actifs correspondants.
     */
    List<Event> findByOrganisateurIdAndActifAndStartTimeAfter(Integer organisateurId, boolean actif, LocalDateTime startTime);

    /**
     * Recherche les événements futurs ({@code start} après {@code now}) et actifs
     * ({@code actif = true}) organisés par une liste de clubs.
     *
     * @param clubIds Une collection des IDs des {@link Club}s organisateurs.
     * @param actif   Doit être {@code true}.
     * @param now     La date et heure actuelle pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs et actifs correspondants.
     */
    List<Event> findByOrganisateurIdInAndActifAndStartTimeAfter(Collection<Integer> clubIds, boolean actif, LocalDateTime now);

    /**
     * Recherche tous les événements futurs ({@code start} après {@code startTime})
     * en fonction de leur statut actif/inactif.
     *
     * @param actif     {@code true} pour les actifs, {@code false} pour les inactifs.
     * @param startTime La date et heure de référence pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs correspondants.
     */
    List<Event> findByActifAndStartTimeAfter(boolean actif, LocalDateTime startTime);

    /**
     * Compte le nombre d'événements actifs ({@code actif = true}) pour un club donné
     * qui commencent dans un intervalle de temps spécifié.
     *
     * @param clubId L'ID du {@link Club} organisateur.
     * @param actif  Doit être {@code true}.
     * @param start  La date/heure de début de l'intervalle (inclusive).
     * @param end    La date/heure de fin de l'intervalle (inclusive).
     * @return Le nombre ({@code long}) d'événements correspondants.
     */
    long countByOrganisateurIdAndActifAndStartTimeBetween(Integer clubId, boolean actif, LocalDateTime start, LocalDateTime end);

    /**
     * Recherche une page d'événements pour un club organisateur spécifique, dont la date de début
     * se situe dans un intervalle de temps donné.
     *
     * @param clubId    L'ID du {@link Club} organisateur.
     * @param dateStart La date/heure de début de l'intervalle (inclusive).
     * @param dateEnd   La date/heure de fin de l'intervalle (inclusive).
     * @param pageable  Les informations de pagination (numéro de page, taille, tri).
     * @return Une {@link Page} d'{@link Event}s correspondants.
     */
    @Query("""
            SELECT e FROM Event e
            WHERE e.organisateur.id = :clubId
            AND (e.startTime BETWEEN :dateStart AND :dateEnd)
            """)
    Page<Event> findByOrganisateurIdAndDate(@Param("clubId") Integer clubId,
                                            @Param("dateStart") LocalDateTime dateStart,
                                            @Param("dateEnd") LocalDateTime dateEnd, // Corrigé le nom du paramètre ici
                                            Pageable pageable);

    /**
     * Recherche les événements futurs ({@code start} après {@code now}) et **inactifs**
     * ({@code actif = false}) organisés par une liste de clubs.
     *
     * @param memberClubIds Un ensemble des IDs des {@link Club}s organisateurs.
     * @param now           La date et heure actuelle pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs et inactifs correspondants.
     */
    List<Event> findByOrganisateurIdInAndActifIsFalseAndStartTimeAfter(Set<Integer> memberClubIds, LocalDateTime now);

    /**
     * Recherche les événements futurs ({@code start} après {@code now}) et **actifs**
     * ({@code actif = true}) organisés par une liste de clubs.
     *
     * @param memberClubIds Un ensemble des IDs des {@link Club}s organisateurs.
     * @param now           La date et heure actuelle pour filtrer les événements futurs.
     * @return Une liste des {@link Event}s futurs et actifs correspondants.
     */
    List<Event> findByOrganisateurIdInAndActifIsTrueAndStartTimeAfter(Set<Integer> memberClubIds, LocalDateTime now);

    /**
     * Recherche les événements futurs ({@code start} après {@code after}) pour une collection de clubs,
     * en filtrant éventuellement par statut actif, et en ne retournant que les événements
     * auxquels au moins un des amis spécifiés ({@code friendIds}) participe (via une réservation).
     * <p>
     * Utilise {@code LEFT JOIN FETCH} pour charger agressivement les catégories et l'organisateur
     * afin d'optimiser l'affichage ultérieur et éviter les problèmes N+1.
     * Utilise {@code DISTINCT} pour éviter les duplicatas d'événements si un événement a plusieurs catégories
     * ou si plusieurs amis participent au même événement.
     * </p>
     *
     * @param clubIds     Collection des IDs des clubs organisateurs à inclure.
     * @param after       Date/heure de référence pour les événements futurs.
     * @param actifStatus Statut actif de l'événement à filtrer ({@code true}, {@code false}, ou {@code null} pour ignorer le filtre).
     * @param friendIds   Collection des IDs des amis dont la participation est recherchée.
     * @return Une liste distincte d'{@link Event}s correspondants, avec catégories et organisateur chargés.
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.categories cat " +     // Charge les catégories
            "LEFT JOIN FETCH e.organisateur org " +   // Charge l'organisateur
            "LEFT JOIN cat.reservations r LEFT JOIN r.membre m " + // Jointures nécessaires pour le filtre ami
            "WHERE e.organisateur.id IN :clubIds " + // Filtre sur les clubs
            "AND e.startTime > :after " +               // Filtre sur la date future
            "AND (:actifStatus IS NULL OR e.actif = :actifStatus) " + // Filtre optionnel sur le statut actif
            "AND m.id IN :friendIds")
    // Filtre sur la participation des amis
    List<Event> findUpcomingEventsInClubsWithStatusAndFriends(
            @Param("clubIds") Collection<Integer> clubIds,
            @Param("after") LocalDateTime after,
            @Param("actifStatus") Boolean actifStatus, // null pour ignorer ce filtre
            @Param("friendIds") Collection<Integer> friendIds);

    /**
     * Recherche les événements futurs ({@code start} après {@code after}) pour une collection de clubs,
     * en filtrant éventuellement par statut actif. Cette version n'applique **pas** de filtre sur
     * la participation des amis.
     * <p>
     * Utilise {@code LEFT JOIN FETCH} pour charger agressivement les catégories et l'organisateur
     * afin d'optimiser l'affichage ultérieur.
     * Utilise {@code DISTINCT} pour éviter les duplicatas d'événements dus aux jointures fetch.
     * </p>
     *
     * @param clubIds     Collection des IDs des clubs organisateurs à inclure.
     * @param after       Date/heure de référence pour les événements futurs.
     * @param actifStatus Statut actif de l'événement à filtrer ({@code true}, {@code false}, ou {@code null} pour ignorer le filtre).
     * @return Une liste distincte d'{@link Event}s correspondants, avec catégories et organisateur chargés.
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.categories cat " +     // Charge les catégories
            "LEFT JOIN FETCH e.organisateur org " +   // Charge l'organisateur
            "WHERE e.organisateur.id IN :clubIds " + // Filtre sur les clubs
            "AND e.startTime > :after " +               // Filtre sur la date future
            "AND (:actifStatus IS NULL OR e.actif = :actifStatus)")
    // Filtre optionnel sur le statut actif
    List<Event> findUpcomingEventsInClubsWithStatus(
            @Param("clubIds") Collection<Integer> clubIds,
            @Param("after") LocalDateTime after,
            @Param("actifStatus") Boolean actifStatus); // null pour ignorer ce filtre
}

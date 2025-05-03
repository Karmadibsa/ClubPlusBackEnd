package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Interface Repository pour l'entité {@link Reservation}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} et des méthodes
 * personnalisées pour rechercher, compter et vérifier l'existence de réservations
 * selon divers critères (membre, événement, catégorie, statut, UUID).
 *
 * @see Reservation
 * @see ReservationStatus
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface ReservationDao extends JpaRepository<Reservation, Integer> {

    /**
     * Recherche toutes les réservations effectuées par un membre spécifique.
     *
     * @param membreId L'ID du {@link Membre}.
     * @return Une liste des {@link Reservation}s de ce membre.
     */
    List<Reservation> findByMembreId(Integer membreId);

    /**
     * Recherche toutes les réservations pour un événement spécifique.
     *
     * @param eventId L'ID de l'{@link Event}.
     * @return Une liste des {@link Reservation}s pour cet événement.
     */
    List<Reservation> findByEventId(Integer eventId);

    /**
     * Recherche toutes les réservations pour une catégorie spécifique d'un événement.
     *
     * @param categorieId L'ID de la {@link Categorie}.
     * @return Une liste des {@link Reservation}s pour cette catégorie.
     */
    List<Reservation> findByCategorieId(Integer categorieId);

    /**
     * Recherche une réservation par son identifiant UUID unique.
     * Utile pour retrouver une réservation à partir d'une référence externe
     * comme un QR code ou une URL spécifique, sans exposer l'ID interne.
     *
     * @param uuid L'UUID (String de 36 caractères) de la réservation.
     * @return Un {@link Optional} contenant la {@link Reservation} si trouvée, sinon un Optional vide.
     */
    Optional<Reservation> findByReservationUuid(String uuid);

    /**
     * Compte le nombre de réservations effectuées par un membre spécifique pour un événement
     * spécifique, ayant un statut donné.
     *
     * @param membreId L'ID du {@link Membre}.
     * @param eventId  L'ID de l'{@link Event}. (Note: le nom du 2ème paramètre était membreId1, corrigé en eventId pour la clarté).
     * @param status   Le {@link ReservationStatus} recherché.
     * @return Le nombre ({@code long}) de réservations correspondantes.
     */
    long countByMembreIdAndEventIdAndStatus(Integer membreId, Integer eventId, ReservationStatus status);

    /**
     * Recherche toutes les réservations d'un membre spécifique ayant un statut donné.
     *
     * @param currentUserId L'ID du {@link Membre}.
     * @param status        Le {@link ReservationStatus} recherché.
     * @return Une liste des {@link Reservation}s correspondantes.
     */
    List<Reservation> findByMembreIdAndStatus(Integer currentUserId, ReservationStatus status);

    /**
     * Recherche toutes les réservations pour un événement spécifique ayant un statut donné.
     *
     * @param eventId L'ID de l'{@link Event}.
     * @param status  Le {@link ReservationStatus} recherché.
     * @return Une liste des {@link Reservation}s correspondantes.
     */
    List<Reservation> findByEventIdAndStatus(Integer eventId, ReservationStatus status);

    /**
     * Vérifie de manière optimisée si un membre spécifique a au moins une réservation
     * pour un événement spécifique avec un statut donné.
     *
     * @param currentUserId     L'ID du {@link Membre}.
     * @param eventId           L'ID de l'{@link Event}.
     * @param reservationStatus Le {@link ReservationStatus} à vérifier.
     * @return {@code true} si au moins une telle réservation existe, {@code false} sinon.
     */
    boolean existsByMembreIdAndEventIdAndStatus(Integer currentUserId, Integer eventId, ReservationStatus reservationStatus);

    /**
     * Compte le nombre total de réservations ayant un statut spécifique (ex: {@code UTILISE})
     * pour tous les événements organisés par un club donné.
     *
     * @param status Le {@link ReservationStatus} des réservations à compter.
     * @param clubId L'ID du {@link Club} organisateur des événements.
     * @return Le nombre total ({@code long}) de réservations correspondantes.
     */
    long countByStatusAndEventOrganisateurId(ReservationStatus status, Integer clubId);

    /**
     * Recherche les réservations ayant un statut spécifique (ex: {@code CONFIRME})
     * pour une collection d'événements donnée et appartenant à une collection de membres donnée (ex: amis).
     * <p>
     * Utilise {@code JOIN FETCH} pour charger agressivement les entités {@link Membre} et {@link Event} associées,
     * ce qui peut être utile si les informations de ces entités (nom, dates, etc.) sont nécessaires
     * immédiatement après la récupération des réservations, évitant ainsi des requêtes N+1.
     * </p>
     *
     * @param eventIds  Une collection des IDs des {@link Event}s à inclure.
     * @param memberIds Une collection des IDs des {@link Membre}s à inclure.
     * @param status    Le {@link ReservationStatus} recherché (ex: {@code ReservationStatus.CONFIRME}).
     * @return Une liste des {@link Reservation}s correspondantes, avec les Membres et Events associés chargés.
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.membre m JOIN FETCH r.event e " + // JOIN FETCH pour charger Membre et Event
            "WHERE e.id IN :eventIds " +  // Filtre sur les IDs d'événement
            "AND m.id IN :memberIds " + // Filtre sur les IDs de membre
            "AND r.status = :status")
    // Filtre sur le statut de la réservation
    List<Reservation> findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember(
            @Param("eventIds") Collection<Integer> eventIds,
            @Param("memberIds") Collection<Integer> memberIds,
            @Param("status") ReservationStatus status // Passer ReservationStatus.CONFIRME par ex.
    );
}

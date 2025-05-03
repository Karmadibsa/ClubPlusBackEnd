package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface Repository pour l'entité {@link DemandeAmi}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} et des méthodes
 * personnalisées pour gérer les demandes d'amitié, vérifier les relations existantes,
 * et lister les demandes ou les amis selon différents critères.
 *
 * @see DemandeAmi
 * @see Membre
 * @see Statut
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface DemandeAmiDao extends JpaRepository<DemandeAmi, Integer> {

    /**
     * Vérifie s'il existe une demande d'amitié, soit en attente ({@code statusAttente})
     * soit déjà acceptée ({@code statusAccepte}), entre deux utilisateurs spécifiés,
     * indépendamment de qui a envoyé la demande initiale (vérification bidirectionnelle).
     * <p>
     * Utile pour empêcher l'envoi d'une nouvelle demande si une est déjà en cours ou si
     * les utilisateurs sont déjà amis.
     * </p>
     *
     * @param user1Id       L'ID du premier utilisateur.
     * @param user2Id       L'ID du second utilisateur.
     * @param statusAttente Le statut représentant une demande en attente (ex: {@code Statut.ATTENTE}).
     * @param statusAccepte Le statut représentant une demande acceptée (ex: {@code Statut.ACCEPTEE}).
     * @return {@code true} si une telle demande existe, {@code false} sinon.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DemandeAmi d " +
            "WHERE (d.statut = :statusAttente OR d.statut = :statusAccepte) AND " +
            "((d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) OR " +
            "(d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id))")
    boolean existsPendingOrAcceptedRequestBetween(
            @Param("user1Id") Integer user1Id,
            @Param("user2Id") Integer user2Id,
            @Param("statusAttente") Statut statusAttente, // Ex: Statut.ATTENTE
            @Param("statusAccepte") Statut statusAccepte  // Ex: Statut.ACCEPTEE
    );

    /**
     * Recherche l'entité {@link DemandeAmi} spécifique qui représente une relation d'amitié
     * **acceptée** entre deux utilisateurs donnés, indépendamment de qui a envoyé la demande
     * initiale (vérification bidirectionnelle).
     * <p>
     * Utilisé principalement pour retrouver l'enregistrement représentant une amitié existante,
     * par exemple avant de la supprimer.
     * </p>
     *
     * @param user1Id L'ID du premier utilisateur.
     * @param user2Id L'ID du second utilisateur.
     * @param statut  Le statut recherché (doit être {@code Statut.ACCEPTEE} lors de l'appel).
     * @return Un {@link Optional} contenant l'entité {@link DemandeAmi} de l'amitié acceptée si trouvée,
     * sinon un Optional vide.
     */
    @Query("SELECT d FROM DemandeAmi d WHERE d.statut = :statut AND " +
            "((d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) OR " +
            "(d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id))")
    Optional<DemandeAmi> findAcceptedFriendshipBetween(
            @Param("user1Id") Integer user1Id,
            @Param("user2Id") Integer user2Id,
            @Param("statut") Statut statut // Passer Statut.ACCEPTEE ici
    );

    /**
     * Recherche toutes les demandes d'amitié reçues par un membre spécifique
     * et ayant un statut donné (ex: ATTENTE, ACCEPTEE).
     *
     * @param recepteurId L'ID du {@link Membre} destinataire des demandes.
     * @param statut      Le {@link Statut} des demandes à rechercher.
     * @return Une liste de {@link DemandeAmi} correspondantes.
     */
    List<DemandeAmi> findByRecepteurIdAndStatut(Integer recepteurId, Statut statut);

    /**
     * Recherche toutes les demandes d'amitié envoyées par un membre spécifique
     * et ayant un statut donné (ex: ATTENTE).
     *
     * @param envoyeurId L'ID du {@link Membre} expéditeur des demandes.
     * @param statut     Le {@link Statut} des demandes à rechercher.
     * @return Une liste de {@link DemandeAmi} correspondantes.
     */
    List<DemandeAmi> findByEnvoyeurIdAndStatut(Integer envoyeurId, Statut statut);

    // --- Requêtes optimisées pour lister les amis ---

    /**
     * Récupère uniquement les identifiants (IDs) des membres qui sont amis avec l'utilisateur spécifié.
     * L'amitié est définie par une {@link DemandeAmi} ayant le statut fourni (normalement {@code Statut.ACCEPTEE}).
     * La requête sélectionne l'ID de l'"autre" membre dans la relation (celui qui n'est pas {@code userId}).
     * <p>
     * C'est une requête optimisée car elle évite de charger les entités {@link Membre} complètes.
     * </p>
     *
     * @param userId L'ID de l'utilisateur dont on veut les amis.
     * @param statut Le statut définissant une amitié (doit être {@code Statut.ACCEPTEE} lors de l'appel).
     * @return Une liste d'{@link Integer} représentant les IDs des amis de l'utilisateur.
     */
    @Query("SELECT CASE WHEN d.envoyeur.id = :userId THEN d.recepteur.id ELSE d.envoyeur.id END " +
            "FROM DemandeAmi d WHERE d.statut = :statut AND (d.envoyeur.id = :userId OR d.recepteur.id = :userId)")
    List<Integer> findFriendIdsOfUser(@Param("userId") Integer userId, @Param("statut") Statut statut); // Passer Statut.ACCEPTEE ici

    // --- Méthodes par défaut (Commodité) ---

    /**
     * Méthode de commodité (via {@code default} dans l'interface) pour trouver les demandes d'ami
     * reçues par un utilisateur qui sont actuellement en attente (statut = {@code Statut.ATTENTE}).
     * Appelle {@link #findByRecepteurIdAndStatut(Integer, Statut)}.
     *
     * @param recepteurId L'ID du membre destinataire.
     * @return Une liste des {@link DemandeAmi} en attente reçues.
     */
    default List<DemandeAmi> findPendingReceivedRequests(Integer recepteurId) {
        return findByRecepteurIdAndStatut(recepteurId, Statut.ATTENTE);
    }

    /**
     * Méthode de commodité (via {@code default} dans l'interface) pour trouver les demandes d'ami
     * envoyées par un utilisateur qui sont actuellement en attente (statut = {@code Statut.ATTENTE}).
     * Appelle {@link #findByEnvoyeurIdAndStatut(Integer, Statut)}.
     *
     * @param envoyeurId L'ID du membre expéditeur.
     * @return Une liste des {@link DemandeAmi} en attente envoyées.
     */
    default List<DemandeAmi> findPendingSentRequests(Integer envoyeurId) {
        return findByEnvoyeurIdAndStatut(envoyeurId, Statut.ATTENTE);
    }
}

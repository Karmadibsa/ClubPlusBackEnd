package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité {@link DemandeAmi}.
 * Fournit les opérations CRUD et des requêtes pour gérer les relations d'amitié.
 */
@Repository
public interface DemandeAmiDao extends JpaRepository<DemandeAmi, Integer> {

    /**
     * Vérifie s'il existe une demande d'amitié en attente ou acceptée entre deux utilisateurs.
     * <p>
     * La vérification est bidirectionnelle pour empêcher les doublons.
     *
     * @param user1Id       L'ID du premier utilisateur.
     * @param user2Id       L'ID du second utilisateur.
     * @param statusAttente Le statut {@code ATTENTE}.
     * @param statusAccepte Le statut {@code ACCEPTEE}.
     * @return {@code true} si une demande correspondante existe, {@code false} sinon.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DemandeAmi d " +
            "WHERE (d.statut = :statusAttente OR d.statut = :statusAccepte) AND " +
            "((d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) OR " +
            "(d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id))")
    boolean existsPendingOrAcceptedRequestBetween(
            @Param("user1Id") Integer user1Id,
            @Param("user2Id") Integer user2Id,
            @Param("statusAttente") Statut statusAttente,
            @Param("statusAccepte") Statut statusAccepte
    );

    /**
     * Recherche la demande d'amitié acceptée entre deux utilisateurs.
     * <p>
     * La recherche est bidirectionnelle. Utile pour retrouver l'entité représentant une amitié.
     *
     * @param user1Id L'ID du premier utilisateur.
     * @param user2Id L'ID du second utilisateur.
     * @param statut  Le statut de la demande (doit être {@code Statut.ACCEPTEE}).
     * @return Un {@link Optional} contenant la demande si elle est trouvée.
     */
    @Query("SELECT d FROM DemandeAmi d WHERE d.statut = :statut AND " +
            "((d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) OR " +
            "(d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id))")
    Optional<DemandeAmi> findAcceptedFriendshipBetween(
            @Param("user1Id") Integer user1Id,
            @Param("user2Id") Integer user2Id,
            @Param("statut") Statut statut
    );

    /**
     * Recherche les demandes d'amitié par destinataire et par statut.
     *
     * @param recepteurId L'ID du membre destinataire.
     * @param statut      Le statut des demandes à rechercher (ex: ATTENTE).
     * @return Une liste de demandes correspondantes.
     */
    List<DemandeAmi> findByRecepteurIdAndStatut(Integer recepteurId, Statut statut);

    /**
     * Recherche les demandes d'amitié par envoyeur et par statut.
     *
     * @param envoyeurId L'ID du membre expéditeur.
     * @param statut     Le statut des demandes à rechercher (ex: ATTENTE).
     * @return Une liste de demandes correspondantes.
     */
    List<DemandeAmi> findByEnvoyeurIdAndStatut(Integer envoyeurId, Statut statut);

    /**
     * Récupère les IDs des amis d'un utilisateur.
     * <p>
     * Requête optimisée qui ne charge pas les entités Membre complètes.
     *
     * @param userId L'ID de l'utilisateur.
     * @param statut Le statut définissant une amitié (doit être {@code Statut.ACCEPTEE}).
     * @return Une liste d'IDs des amis.
     */
    @Query("SELECT CASE WHEN d.envoyeur.id = :userId THEN d.recepteur.id ELSE d.envoyeur.id END " +
            "FROM DemandeAmi d WHERE d.statut = :statut AND (d.envoyeur.id = :userId OR d.recepteur.id = :userId)")
    List<Integer> findFriendIdsOfUser(@Param("userId") Integer userId, @Param("statut") Statut statut);

    /**
     * Méthode de commodité pour trouver les demandes reçues en attente.
     *
     * @param recepteurId L'ID du membre destinataire.
     * @return Une liste des demandes en attente reçues.
     */
    default List<DemandeAmi> findPendingReceivedRequests(Integer recepteurId) {
        return findByRecepteurIdAndStatut(recepteurId, Statut.ATTENTE);
    }

    /**
     * Méthode de commodité pour trouver les demandes envoyées en attente.
     *
     * @param envoyeurId L'ID du membre expéditeur.
     * @return Une liste des demandes en attente envoyées.
     */
    default List<DemandeAmi> findPendingSentRequests(Integer envoyeurId) {
        return findByEnvoyeurIdAndStatut(envoyeurId, Statut.ATTENTE);
    }
}

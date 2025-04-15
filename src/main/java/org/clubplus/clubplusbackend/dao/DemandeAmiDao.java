package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeAmiDao extends JpaRepository<DemandeAmi, Integer> {

    /**
     * Trouve une demande spécifique (unidirectionnelle).
     */
    Optional<DemandeAmi> findByEnvoyeurIdAndRecepteurId(Integer envoyeurId, Integer recepteurId);

    /**
     * Vérifie si une demande (quel que soit le statut) existe entre deux utilisateurs (bidirectionnel).
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
            "FROM DemandeAmi d " +
            "WHERE (d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) " +
            "   OR (d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id)")
    boolean existsBetweenUsers(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id);

    /**
     * Vérifie s'il existe une demande EN ATTENTE ou ACCEPTEE entre deux utilisateurs (bidirectionnel).
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
     * Trouve la relation d'amitié ACCEPTÉE entre deux utilisateurs (bidirectionnel).
     */
    @Query("SELECT d FROM DemandeAmi d WHERE d.statut = :statut AND " +
            "((d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) OR " +
            "(d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id))")
    Optional<DemandeAmi> findAcceptedFriendshipBetween(
            @Param("user1Id") Integer user1Id,
            @Param("user2Id") Integer user2Id,
            @Param("statut") Statut statut // Passer Statut.ACCEPTE lors de l'appel
    );

    /**
     * Trouve les demandes reçues par un membre avec un statut spécifique.
     */
    List<DemandeAmi> findByRecepteurIdAndStatut(Integer recepteurId, Statut statut);

    /**
     * Trouve les demandes envoyées par un membre avec un statut spécifique.
     */
    List<DemandeAmi> findByEnvoyeurIdAndStatut(Integer envoyeurId, Statut statut);

    /**
     * Compte les demandes par statut.
     */
    long countByStatut(Statut statut);

    // --- Requêtes optimisées pour lister les amis ---

    /**
     * Récupère directement les objets Membre qui sont amis (statut ACCEPTE) avec un utilisateur donné.
     *
     * @param userId L'ID de l'utilisateur dont on cherche les amis.
     * @param statut Doit être Statut.ACCEPTE.
     * @return Liste des objets Membre amis.
     */
    @Query("SELECT CASE WHEN d.envoyeur.id = :userId THEN d.recepteur ELSE d.envoyeur END " +
            "FROM DemandeAmi d WHERE d.statut = :statut AND (d.envoyeur.id = :userId OR d.recepteur.id = :userId)")
    List<Membre> findFriendsOfUser(@Param("userId") Integer userId, @Param("statut") Statut statut); // Passer Statut.ACCEPTE

    // --- Méthodes par défaut (facultatives, juste pour la commodité) ---
    default List<DemandeAmi> findPendingReceivedRequests(Integer recepteurId) {
        return findByRecepteurIdAndStatut(recepteurId, Statut.ATTENTE);
    }

    default List<DemandeAmi> findPendingSentRequests(Integer envoyeurId) {
        return findByEnvoyeurIdAndStatut(envoyeurId, Statut.ATTENTE);
    }
}

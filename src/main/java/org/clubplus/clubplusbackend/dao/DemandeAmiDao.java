package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.security.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeAmiDao extends JpaRepository<DemandeAmi, Integer> {

    // Trouver une demande spécifique entre deux membres (indépendamment du statut)
    Optional<DemandeAmi> findByEnvoyeurIdAndRecepteurId(Integer envoyeurId, Integer recepteurId);

    // Vérifier si une demande existe entre deux membres (dans un sens ou l'autre)
    // Utile pour empêcher les doublons ou les demandes à soi-même indirectement
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
            "FROM DemandeAmi d " +
            "WHERE (d.envoyeur.id = :user1Id AND d.recepteur.id = :user2Id) " +
            "   OR (d.envoyeur.id = :user2Id AND d.recepteur.id = :user1Id)")
    boolean existsBetweenUsers(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id);

    // Trouver les demandes reçues par un membre avec un statut spécifique
    List<DemandeAmi> findByRecepteurIdAndStatut(Integer recepteurId, Statut statut);

    // Trouver les demandes envoyées par un membre avec un statut spécifique
    List<DemandeAmi> findByEnvoyeurIdAndStatut(Integer envoyeurId, Statut statut);

    // Trouver toutes les demandes (envoyées ou reçues) impliquant un membre et ayant le statut ACCEPTE
    // Utile pour construire la liste d'amis
    @Query("SELECT d FROM DemandeAmi d WHERE d.statut = :statut AND (d.envoyeur.id = :membreId OR d.recepteur.id = :membreId)")
    List<DemandeAmi> findAllAcceptedRequestsInvolvingUser(@Param("membreId") Integer membreId, @Param("statut") Statut statut);

    // Trouver les demandes reçues en attente par un membre
    default List<DemandeAmi> findPendingReceivedRequests(Integer recepteurId) {
        return findByRecepteurIdAndStatut(recepteurId, Statut.ATTENTE);
    }

    // Trouver les demandes envoyées en attente par un membre
    default List<DemandeAmi> findPendingSentRequests(Integer envoyeurId) {
        return findByEnvoyeurIdAndStatut(envoyeurId, Statut.ATTENTE);
    }

    long countByStatut(Statut statut);

}

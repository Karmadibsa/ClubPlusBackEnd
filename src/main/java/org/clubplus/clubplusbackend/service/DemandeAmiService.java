package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.DemandeAmiDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Statut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeAmiService {

    private final DemandeAmiDao demandeAmiRepository;
    private final MembreDao membreRepository; // Pour valider l'existence des membres

    /**
     * Envoie une demande d'ami de l'envoyeur vers le récepteur.
     *
     * @param envoyeurId  L'ID du membre qui envoie la demande.
     * @param recepteurId L'ID du membre qui reçoit la demande.
     * @return La DemandeAmi créée avec le statut ATTENTE.
     * @throws EntityNotFoundException  Si l'envoyeur ou le récepteur n'existe pas.
     * @throws IllegalArgumentException Si l'envoyeur et le récepteur sont identiques.
     * @throws IllegalStateException    Si une demande (ou une amitié acceptée) existe déjà entre ces deux membres.
     */
    public DemandeAmi sendFriendRequest(Integer envoyeurId, Integer recepteurId) {
        // 1. Vérifier si les ID sont différents
        if (envoyeurId.equals(recepteurId)) {
            throw new IllegalArgumentException("Impossible d'envoyer une demande d'ami à soi-même.");
        }

        // 2. Vérifier l'existence des membres
        Membre envoyeur = membreRepository.findById(envoyeurId)
                .orElseThrow(() -> new EntityNotFoundException("Membre envoyeur non trouvé avec l'ID : " + envoyeurId));
        Membre recepteur = membreRepository.findById(recepteurId)
                .orElseThrow(() -> new EntityNotFoundException("Membre récepteur non trouvé avec l'ID : " + recepteurId));

        // 3. Vérifier s'il existe déjà une relation (demande ou amitié) entre les deux
        if (demandeAmiRepository.existsBetweenUsers(envoyeurId, recepteurId)) {
            // Vous pouvez affiner ici : vérifier le statut existant si vous voulez permettre
            // de renvoyer une demande après un refus, par exemple.
            // Pour l'instant, on bloque si une relation existe, quel que soit son statut.
            throw new IllegalStateException("Une demande d'ami ou une relation existe déjà entre ces deux membres.");
        }

        // 4. Créer et sauvegarder la nouvelle demande
        DemandeAmi nouvelleDemande = new DemandeAmi();
        nouvelleDemande.setEnvoyeur(envoyeur);
        nouvelleDemande.setRecepteur(recepteur);
        nouvelleDemande.setStatut(Statut.ATTENTE);
        nouvelleDemande.setDateDemande(LocalDateTime.now()); // La date peut aussi être gérée par @PrePersist

        return demandeAmiRepository.save(nouvelleDemande);
    }

    /**
     * Accepte une demande d'ami en attente.
     *
     * @param demandeId   L'ID de la demande d'ami à accepter.
     * @param accepteurId L'ID du membre qui accepte (doit être le récepteur initial).
     * @return La DemandeAmi mise à jour avec le statut ACCEPTE.
     * @throws EntityNotFoundException Si la demande n'est pas trouvée.
     * @throws SecurityException       Si le membre qui accepte n'est pas le récepteur de la demande.
     * @throws IllegalStateException   Si la demande n'est pas au statut ATTENTE.
     */
    public DemandeAmi acceptFriendRequest(Integer demandeId, Integer accepteurId) {
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'ami non trouvée avec l'ID : " + demandeId));

        // Vérifier si l'utilisateur est bien le récepteur
        if (!demande.getRecepteur().getId().equals(accepteurId)) {
            throw new SecurityException("Seul le récepteur peut accepter cette demande d'ami.");
        }

        // Vérifier si la demande est bien en attente
        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Cette demande d'ami n'est pas en attente (Statut actuel: " + demande.getStatut() + ").");
        }

        demande.setStatut(Statut.ACCEPTE);
        return demandeAmiRepository.save(demande);
    }

    /**
     * Refuse une demande d'ami en attente.
     *
     * @param demandeId  L'ID de la demande d'ami à refuser.
     * @param refuseurId L'ID du membre qui refuse (doit être le récepteur initial).
     * @return La DemandeAmi mise à jour avec le statut REFUSE.
     * @throws EntityNotFoundException Si la demande n'est pas trouvée.
     * @throws SecurityException       Si le membre qui refuse n'est pas le récepteur de la demande.
     * @throws IllegalStateException   Si la demande n'est pas au statut ATTENTE.
     */
    public DemandeAmi refuseFriendRequest(Integer demandeId, Integer refuseurId) {
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'ami non trouvée avec l'ID : " + demandeId));

        // Vérifier si l'utilisateur est bien le récepteur
        if (!demande.getRecepteur().getId().equals(refuseurId)) {
            throw new SecurityException("Seul le récepteur peut refuser cette demande d'ami.");
        }

        // Vérifier si la demande est bien en attente
        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Cette demande d'ami n'est pas en attente (Statut actuel: " + demande.getStatut() + ").");
        }

        demande.setStatut(Statut.REFUSE);
        return demandeAmiRepository.save(demande);

        // Alternative : Supprimer la demande au lieu de la marquer comme refusée
        // demandeAmiRepository.delete(demande);
    }

    /**
     * Annule une demande d'ami envoyée qui est toujours en attente.
     *
     * @param demandeId  L'ID de la demande à annuler.
     * @param annuleurId L'ID du membre qui annule (doit être l'envoyeur initial).
     * @throws EntityNotFoundException Si la demande n'est pas trouvée.
     * @throws SecurityException       Si le membre qui annule n'est pas l'envoyeur de la demande.
     * @throws IllegalStateException   Si la demande n'est plus au statut ATTENTE.
     */
    public void cancelFriendRequest(Integer demandeId, Integer annuleurId) {
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'ami non trouvée avec l'ID : " + demandeId));

        // Vérifier si l'utilisateur est bien l'envoyeur
        if (!demande.getEnvoyeur().getId().equals(annuleurId)) {
            throw new SecurityException("Seul l'envoyeur peut annuler cette demande d'ami.");
        }

        // Vérifier si la demande est bien en attente
        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible d'annuler une demande qui n'est plus en attente (Statut actuel: " + demande.getStatut() + ").");
        }

        demandeAmiRepository.delete(demande);
    }

    /**
     * Supprime une relation d'amitié (marquée comme ACCEPTE).
     * N'importe lequel des deux amis peut initier la suppression.
     *
     * @param demandeId L'ID de la relation d'amitié (DemandeAmi avec statut ACCEPTE).
     * @param membreId  L'ID d'un des deux membres impliqués dans l'amitié.
     * @throws EntityNotFoundException Si la demande n'est pas trouvée.
     * @throws SecurityException       Si le membreId n'est ni l'envoyeur ni le récepteur.
     * @throws IllegalStateException   Si la relation n'est pas au statut ACCEPTE.
     */
    public void removeFriendship(Integer demandeId, Integer membreId) {
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Relation d'amitié non trouvée avec l'ID : " + demandeId));

        // Vérifier que le membreId est bien l'un des deux participants
        if (!demande.getEnvoyeur().getId().equals(membreId) && !demande.getRecepteur().getId().equals(membreId)) {
            throw new SecurityException("Vous n'êtes pas partie prenante de cette relation d'amitié.");
        }

        // Vérifier que le statut est bien ACCEPTE
        if (demande.getStatut() != Statut.ACCEPTE) {
            throw new IllegalStateException("Cette relation n'est pas actuellement une amitié acceptée (Statut actuel: " + demande.getStatut() + ").");
        }

        // Supprimer la relation
        demandeAmiRepository.delete(demande);
        // Alternative: Marquer comme REFUSE ou créer un statut "TERMINE" ? La suppression est plus simple.
    }


    // --- Méthodes de consultation ---

    public List<DemandeAmi> getPendingReceivedRequests(Integer membreId) {
        return demandeAmiRepository.findPendingReceivedRequests(membreId);
    }

    public List<DemandeAmi> getPendingSentRequests(Integer membreId) {
        return demandeAmiRepository.findPendingSentRequests(membreId);
    }

    /**
     * Récupère la liste des amis (Membres) d'un membre donné.
     * Un ami est défini par une DemandeAmi avec le statut ACCEPTE où le membre est soit l'envoyeur soit le récepteur.
     *
     * @param membreId L'ID du membre dont on veut la liste d'amis.
     * @return Une liste d'objets Membre représentant les amis.
     */
    public List<Membre> getFriends(Integer membreId) {
        List<DemandeAmi> acceptedRequests = demandeAmiRepository.findAllAcceptedRequestsInvolvingUser(membreId, Statut.ACCEPTE);

        // Extraire l'ID de l'ami (l'autre personne dans la relation) pour chaque demande
        List<Integer> friendIds = acceptedRequests.stream()
                .map(demande -> {
                    if (demande.getEnvoyeur().getId().equals(membreId)) {
                        return demande.getRecepteur().getId();
                    } else {
                        return demande.getEnvoyeur().getId();
                    }
                })
                .distinct() // Éviter les doublons si jamais c'est possible
                .collect(Collectors.toList());

        // Récupérer les objets Membre correspondants aux IDs des amis
        if (friendIds.isEmpty()) {
            return List.of(); // Retourne une liste vide si pas d'amis
        }
        return membreRepository.findAllById(friendIds); // Récupère tous les amis en une seule requête
    }

}


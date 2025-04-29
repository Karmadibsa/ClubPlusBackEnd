package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.DemandeAmiDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.clubplus.clubplusbackend.security.Statut;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeAmiService {

    private final DemandeAmiDao demandeAmiRepository;
    private final MembreDao membreRepository;
    private final SecurityService securityService; // Injecter pour accès utilisateur courant

    /**
     * Envoie une demande d'ami en utilisant le codeAmi du récepteur.
     * Vérifications : existence membres, auto-demande, relation déjà existante.
     * La contrainte de club commun est supprimée.
     */
    public DemandeAmi sendFriendRequestByCode(String recepteurCodeAmi) { // Prend le code en paramètre
        Integer envoyeurId = securityService.getCurrentUserIdOrThrow(); // Récupère l'envoyeur courant

        // Trouve le récepteur par son codeAmi
        Membre recepteur = membreRepository.findByCodeAmi(recepteurCodeAmi)
                .orElseThrow(() -> new EntityNotFoundException("Aucun membre trouvé avec le code ami : " + recepteurCodeAmi)); // -> 404

        Integer recepteurId = recepteur.getId();

        // --- Mêmes vérifications qu'avant (sauf club commun) ---
        if (envoyeurId.equals(recepteurId)) {
            throw new IllegalArgumentException("Impossible d'envoyer une demande à soi-même."); // -> 400
        }

        Membre envoyeur = membreRepository.findById(envoyeurId)
                .orElseThrow(() -> new EntityNotFoundException("Membre envoyeur non trouvé : " + envoyeurId)); // Devrait pas arriver si authentifié

        // Vérifier si le récepteur est un MEMBRE (si cette règle métier existe toujours)
        if (recepteur.getRole() != Role.MEMBRE) {
            throw new AccessDeniedException("Les demandes d'ami ne peuvent être envoyées qu'à des membres."); // -> 403
        }

        // Vérifier s'il existe déjà une demande en attente ou une amitié acceptée
        if (demandeAmiRepository.existsPendingOrAcceptedRequestBetween(envoyeurId, recepteurId, Statut.ATTENTE, Statut.ACCEPTE)) {
            throw new IllegalStateException("Une demande en attente ou une relation d'amitié existe déjà."); // -> 409
        }

        DemandeAmi nouvelleDemande = new DemandeAmi(envoyeur, recepteur);
        return demandeAmiRepository.save(nouvelleDemande);
    }

    /**
     * Accepte une demande d'ami en attente.
     * Sécurité : Vérifie que l'utilisateur courant est bien le récepteur de la demande.
     * Met à jour le statut de la demande ET la relation @ManyToMany 'amis'.
     */
    public DemandeAmi acceptFriendRequest(Integer demandeId) { // Retire accepteurId, on utilise SecurityService
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Récupère ID courant ou lance exception si non auth

        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'ami non trouvée : " + demandeId)); // -> 404

        // Vérification Sécurité Contextuelle : Est-ce le récepteur ?
        if (!demande.getRecepteur().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Seul le récepteur peut accepter cette demande."); // -> 403
        }

        // Vérification Métier : La demande est-elle en attente ?
        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Cette demande n'est pas en attente."); // -> 409
        }

        // Mise à jour Statut Demande
        demande.setStatut(Statut.ACCEPTE);
        DemandeAmi savedDemande = demandeAmiRepository.save(demande);

        // *** MISE À JOUR RELATION @ManyToMany 'amis' ***
        Membre envoyeur = demande.getEnvoyeur();
        Membre recepteur = demande.getRecepteur();
        // Ajouter l'amitié dans les deux sens (méthodes helper dans Membre)
        recepteur.addAmi(envoyeur);
        // Sauvegarder les entités Membre modifiées pour persister le lien dans la table 'amitie'
        membreRepository.save(recepteur);
        membreRepository.save(envoyeur); // IMPORTANT: Sauver les deux côtés

        return savedDemande;
    }

    /**
     * Refuse une demande d'ami en attente.
     * Sécurité : Vérifie que l'utilisateur courant est bien le récepteur.
     */
    public DemandeAmi refuseFriendRequest(Integer demandeId) { // Retire refuseurId
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'ami non trouvée : " + demandeId)); // -> 404

        if (!demande.getRecepteur().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Seul le récepteur peut refuser cette demande."); // -> 403
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Cette demande n'est pas en attente."); // -> 409
        }

        demande.setStatut(Statut.REFUSE); // Met à jour le statut
        return demandeAmiRepository.save(demande);
        // Alternative : supprimer la demande refusée ? demandeAmiRepository.delete(demande);
    }

    /**
     * Annule une demande d'ami envoyée qui est toujours en attente.
     * Sécurité : Vérifie que l'utilisateur courant est bien l'envoyeur.
     */
    public void cancelFriendRequest(Integer demandeId) { // Retire annuleurId
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'ami non trouvée : " + demandeId)); // -> 404

        if (!demande.getEnvoyeur().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Seul l'envoyeur peut annuler cette demande."); // -> 403
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible d'annuler : la demande n'est pas en attente."); // -> 409
        }

        demandeAmiRepository.delete(demande); // Suppression de la demande annulée
    }

    /**
     * Supprime une relation d'amitié (DemandeAmi avec statut ACCEPTE) entre l'utilisateur courant et un autre membre.
     * Met à jour la relation @ManyToMany 'amis'.
     */
    public void removeFriendshipByFriendId(Integer friendIdToRemove) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        if (currentUserId.equals(friendIdToRemove)) {
            throw new IllegalArgumentException("Impossible de se retirer soi-même comme ami."); // -> 400
        }

        // 1. Trouver l'entité DemandeAmi représentant l'amitié ACCEPTÉE
        DemandeAmi friendship = demandeAmiRepository.findAcceptedFriendshipBetween(currentUserId, friendIdToRemove, Statut.ACCEPTE)
                .orElseThrow(() -> new EntityNotFoundException("Aucune relation d'amitié active trouvée avec l'utilisateur ID: " + friendIdToRemove)); // -> 404

        // 2. Récupérer les objets Membre impliqués (nécessaires pour la mise à jour ManyToMany)
        // On pourrait les récupérer via friendship.getEnvoyeur()/getRecepteur(), mais findById est plus sûr.
        Membre currentUser = membreRepository.findById(currentUserId).orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé ?!")); // Devrait pas arriver
        Membre friendToRemove = membreRepository.findById(friendIdToRemove).orElseThrow(() -> new EntityNotFoundException("Ami à supprimer non trouvé : " + friendIdToRemove));

        // *** MISE À JOUR RELATION @ManyToMany 'amis' ***
        currentUser.removeAmi(friendToRemove);
        // Sauvegarder les entités Membre modifiées pour persister la suppression du lien dans la table 'amitie'
        membreRepository.save(currentUser);
        membreRepository.save(friendToRemove); // IMPORTANT: Sauver les deux

        // 3. Supprimer l'entité DemandeAmi (ou la mettre à un statut "TERMINEE")
        demandeAmiRepository.delete(friendship);
    }

    // --- Méthodes de Consultation ---

    /**
     * Récupère les demandes reçues en attente pour l'utilisateur courant.
     */
    @Transactional(readOnly = true)
    public List<DemandeAmi> getPendingReceivedRequests() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return demandeAmiRepository.findByRecepteurIdAndStatut(currentUserId, Statut.ATTENTE);
    }

    /**
     * Récupère les demandes envoyées en attente par l'utilisateur courant.
     */
    @Transactional(readOnly = true)
    public List<DemandeAmi> getPendingSentRequests() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return demandeAmiRepository.findByEnvoyeurIdAndStatut(currentUserId, Statut.ATTENTE);
    }

    /**
     * Récupère la liste des amis (objets Membre) de l'utilisateur courant.
     * Modifié pour utiliser une approche en deux étapes pour éviter le bug Hibernate CASE.
     */
    @Transactional(readOnly = true)
    public List<Membre> getFriends() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // Étape 1: Récupérer les IDs des amis
        List<Integer> friendIds = demandeAmiRepository.findFriendIdsOfUser(currentUserId, Statut.ACCEPTE);


        if (friendIds == null || friendIds.isEmpty()) {

            return Collections.emptyList(); // Important de retourner une liste vide
        }

        // Étape 2: Récupérer les entités Membre correspondantes

        // Utilise la vue Base pour éviter de charger trop d'infos non nécessaires ici
        // Note: findAllById ne garantit pas l'ordre et peut nécessiter une JsonView sur le controller
        return membreRepository.findAllById(friendIds);
    }


}

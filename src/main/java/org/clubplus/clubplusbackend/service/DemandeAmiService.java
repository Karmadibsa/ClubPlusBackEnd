package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.DemandeAmiDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.clubplus.clubplusbackend.model.Statut;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service gérant la logique métier pour les demandes d'ami et les relations d'amitié.
 * <p>
 * Ce service orchestre les opérations telles que l'envoi, l'acceptation, le refus
 * ou l'annulation de demandes d'ami, tout en appliquant les règles métier et de sécurité.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DemandeAmiService {

    private static final Logger log = LoggerFactory.getLogger(DemandeAmiService.class);

    private final DemandeAmiDao demandeAmiRepository;
    private final MembreDao membreRepository;
    private final SecurityService securityService;

    /**
     * Envoie une demande d'amitié de l'utilisateur courant vers un autre membre via son code ami.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>Un utilisateur ne peut pas s'envoyer de demande à lui-même.</li>
     * <li>Le destinataire doit avoir le rôle {@link Role#MEMBRE}.</li>
     * <li>Une demande ne peut être envoyée s'il en existe déjà une en attente ou si une amitié est déjà établie.</li>
     * </ul>
     *
     * @param recepteurCodeAmi Le code ami unique du membre destinataire.
     * @return La nouvelle demande d'ami créée.
     * @throws EntityNotFoundException  si le destinataire n'est pas trouvé.
     * @throws IllegalArgumentException si l'utilisateur s'envoie une demande à lui-même.
     * @throws AccessDeniedException    si le destinataire n'a pas le rôle requis.
     * @throws IllegalStateException    si une relation existe déjà.
     */
    public DemandeAmi sendFriendRequestByCode(String recepteurCodeAmi) {
        Integer envoyeurId = securityService.getCurrentUserIdOrThrow();

        Membre recepteur = membreRepository.findByCodeAmi(recepteurCodeAmi)
                .orElseThrow(() -> new EntityNotFoundException("Aucun membre trouvé avec le code ami : " + recepteurCodeAmi));
        Integer recepteurId = recepteur.getId();

        if (envoyeurId.equals(recepteurId)) {
            throw new IllegalArgumentException("Impossible d'envoyer une demande d'ami à soi-même.");
        }

        Membre envoyeur = membreRepository.findById(envoyeurId)
                .orElseThrow(() -> new EntityNotFoundException("Membre envoyeur (ID: " + envoyeurId + ") non trouvé alors qu'il est authentifié. Incohérence BDD."));

        if (recepteur.getRole() != Role.MEMBRE) {
            throw new AccessDeniedException("Les demandes d'ami ne peuvent être envoyées qu'à des membres (rôle MEMBRE).");
        }

        if (demandeAmiRepository.existsPendingOrAcceptedRequestBetween(envoyeurId, recepteurId, Statut.ATTENTE, Statut.ACCEPTEE)) {
            throw new IllegalStateException("Une demande d'amitié en attente ou une relation acceptée existe déjà entre ces deux membres.");
        }

        log.info("Envoi d'une demande d'ami de l'utilisateur ID {} vers l'utilisateur ID {}", envoyeurId, recepteurId);
        DemandeAmi nouvelleDemande = new DemandeAmi(envoyeur, recepteur);
        return demandeAmiRepository.save(nouvelleDemande);
    }

    /**
     * Accepte une demande d'amitié reçue.
     * <p>
     * <b>Sécurité :</b> L'utilisateur courant doit être le destinataire de la demande.
     * <p>
     * <b>Règle métier :</b> La demande doit être en statut {@code ATTENTE}.
     *
     * @param demandeId L'ID de la demande à accepter.
     * @return La demande mise à jour.
     * @throws EntityNotFoundException si la demande n'est pas trouvée.
     * @throws AccessDeniedException   si l'utilisateur n'est pas le destinataire.
     * @throws IllegalStateException   si la demande n'est plus en attente.
     */
    public DemandeAmi acceptFriendRequest(Integer demandeId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'accepter : Demande d'ami non trouvée (ID: " + demandeId + ")"));

        if (!Objects.equals(demande.getRecepteur().getId(), currentUserId)) {
            throw new AccessDeniedException("Action non autorisée : Seul le destinataire peut accepter cette demande d'ami.");
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible d'accepter : La demande (ID: " + demandeId + ") n'est plus en attente.");
        }

        demande.setStatut(Statut.ACCEPTEE);
        DemandeAmi savedDemande = demandeAmiRepository.save(demande);
        log.info("Demande d'ami ID {} acceptée par l'utilisateur ID {}.", demandeId, currentUserId);

        Membre envoyeur = demande.getEnvoyeur();
        Membre recepteur = demande.getRecepteur();
        recepteur.addAmi(envoyeur);
        membreRepository.save(recepteur);
        membreRepository.save(envoyeur);
        log.debug("Relation d'amitié @ManyToMany mise à jour entre {} et {}", envoyeur.getId(), recepteur.getId());

        return savedDemande;
    }

    /**
     * Refuse une demande d'amitié reçue.
     * <p>
     * <b>Sécurité :</b> L'utilisateur courant doit être le destinataire de la demande.
     * <p>
     * <b>Règle métier :</b> La demande doit être en statut {@code ATTENTE}.
     *
     * @param demandeId L'ID de la demande à refuser.
     * @return La demande mise à jour.
     */
    public DemandeAmi refuseFriendRequest(Integer demandeId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible de refuser : Demande d'ami non trouvée (ID: " + demandeId + ")"));

        if (!Objects.equals(demande.getRecepteur().getId(), currentUserId)) {
            throw new AccessDeniedException("Action non autorisée : Seul le destinataire peut refuser cette demande d'ami.");
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible de refuser : La demande (ID: " + demandeId + ") n'est plus en attente.");
        }

        demande.setStatut(Statut.REFUSEE);
        log.info("Demande d'ami ID {} refusée par l'utilisateur ID {}.", demandeId, currentUserId);
        return demandeAmiRepository.save(demande);
    }

    /**
     * Annule une demande d'amitié envoyée qui est toujours en attente.
     * <p>
     * <b>Sécurité :</b> L'utilisateur courant doit être l'expéditeur de la demande.
     * <p>
     * <b>Règle métier :</b> La demande doit être en statut {@code ATTENTE}.
     *
     * @param demandeId L'ID de la demande à annuler.
     */
    public void cancelFriendRequest(Integer demandeId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'annuler : Demande d'ami non trouvée (ID: " + demandeId + ")"));

        if (!Objects.equals(demande.getEnvoyeur().getId(), currentUserId)) {
            throw new AccessDeniedException("Action non autorisée : Seul l'expéditeur peut annuler cette demande d'ami.");
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible d'annuler : La demande (ID: " + demandeId + ") n'est plus en attente.");
        }

        log.info("Annulation de la demande d'ami ID {} par l'expéditeur ID {}.", demandeId, currentUserId);
        demandeAmiRepository.delete(demande);
    }

    /**
     * Supprime une amitié existante entre l'utilisateur courant et un autre membre.
     *
     * @param friendIdToRemove L'ID de l'ami à supprimer.
     * @throws IllegalArgumentException si l'utilisateur tente de se supprimer lui-même.
     * @throws EntityNotFoundException  si aucune amitié n'est trouvée.
     */
    public void removeFriendshipByFriendId(Integer friendIdToRemove) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        if (currentUserId.equals(friendIdToRemove)) {
            throw new IllegalArgumentException("Action invalide : Impossible de se supprimer soi-même de sa liste d'amis.");
        }

        DemandeAmi friendship = demandeAmiRepository.findAcceptedFriendshipBetween(currentUserId, friendIdToRemove, Statut.ACCEPTEE)
                .orElseThrow(() -> new EntityNotFoundException("Aucune relation d'amitié active trouvée avec l'utilisateur ID: " + friendIdToRemove));

        Membre currentUser = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Incohérence : Utilisateur courant (ID: " + currentUserId + ") non trouvé."));
        Membre friendToRemove = membreRepository.findById(friendIdToRemove)
                .orElseThrow(() -> new EntityNotFoundException("Membre ami à supprimer (ID: " + friendIdToRemove + ") non trouvé."));

        currentUser.removeAmi(friendToRemove);
        membreRepository.save(currentUser);
        membreRepository.save(friendToRemove);

        demandeAmiRepository.delete(friendship);
        log.info("Amitié (Demande ID: {}) entre les utilisateurs {} et {} supprimée.", friendship.getId(), currentUserId, friendIdToRemove);
    }

    /**
     * Récupère les demandes d'amitié en attente reçues par l'utilisateur courant.
     *
     * @return Une liste des demandes reçues.
     */
    @Transactional(readOnly = true)
    public List<DemandeAmi> getPendingReceivedRequests() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return demandeAmiRepository.findByRecepteurIdAndStatut(currentUserId, Statut.ATTENTE);
    }

    /**
     * Récupère les demandes d'amitié en attente envoyées par l'utilisateur courant.
     *
     * @return Une liste des demandes envoyées.
     */
    @Transactional(readOnly = true)
    public List<DemandeAmi> getPendingSentRequests() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return demandeAmiRepository.findByEnvoyeurIdAndStatut(currentUserId, Statut.ATTENTE);
    }

    /**
     * Récupère la liste d'amis de l'utilisateur courant.
     *
     * @return Une liste de {@link Membre}s amis.
     */
    @Transactional(readOnly = true)
    public List<Membre> getFriends() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        List<Integer> friendIds = demandeAmiRepository.findFriendIdsOfUser(currentUserId, Statut.ACCEPTEE);

        if (friendIds == null || friendIds.isEmpty()) {
            return Collections.emptyList();
        }

        return membreRepository.findAllById(friendIds);
    }
}

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
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service gérant la logique métier pour les demandes d'ami ({@link DemandeAmi})
 * et les relations d'amitié entre {@link Membre}s.
 * <p>
 * Fournit des fonctionnalités pour :
 * <ul>
 *     <li>Envoyer une demande d'ami.</li>
 *     <li>Accepter ou refuser une demande reçue.</li>
 *     <li>Annuler une demande envoyée.</li>
 *     <li>Supprimer une amitié existante.</li>
 *     <li>Consulter les demandes en attente (envoyées ou reçues).</li>
 *     <li>Consulter la liste d'amis de l'utilisateur courant.</li>
 * </ul>
 * Ce service intègre des vérifications de sécurité via {@link SecurityService} et gère
 * les transactions de base de données. Les exceptions métier spécifiques sont levées
 * pour être traitées par le gestionnaire global d'exceptions.
 * </p>
 *
 * @see DemandeAmi
 * @see Membre
 * @see DemandeAmiDao
 * @see MembreDao
 * @see SecurityService
 * @see Statut
 */
@Service
@RequiredArgsConstructor // Lombok: Injecte les dépendances final via le constructeur.
@Transactional // Transaction par défaut (read-write) pour toutes les méthodes publiques.
public class DemandeAmiService {

    // Logger pour tracer les informations ou erreurs.
    private static final Logger log = LoggerFactory.getLogger(DemandeAmiService.class);

    /**
     * DAO pour l'accès aux données des demandes d'ami.
     */
    private final DemandeAmiDao demandeAmiRepository;
    /**
     * DAO pour l'accès aux données des membres.
     */
    private final MembreDao membreRepository;
    /**
     * Service pour les opérations de sécurité (accès utilisateur courant, vérifications).
     */
    private final SecurityService securityService;

    /**
     * Envoie une demande d'amitié de l'utilisateur actuellement authentifié vers un autre
     * membre identifié par son code ami unique.
     * <p>
     * Vérifications effectuées :
     * <ul>
     *     <li>L'utilisateur courant est authentifié.</li>
     *     <li>Le membre destinataire existe (via son code ami).</li>
     *     <li>La demande n'est pas envoyée à soi-même.</li>
     *     <li>Le destinataire a le rôle {@link Role#MEMBRE}.</li>
     *     <li>Aucune demande en attente ou amitié acceptée n'existe déjà entre les deux membres.</li>
     * </ul>
     * La vérification d'appartenance à un club commun a été retirée.
     * </p>
     *
     * @param recepteurCodeAmi Le code ami unique (String) du membre destinataire.
     * @return L'entité {@link DemandeAmi} nouvellement créée avec le statut {@link Statut#ATTENTE}.
     * @throws AuthenticationException  si l'utilisateur courant n'est pas authentifié (via {@code securityService}).
     * @throws EntityNotFoundException  si le membre destinataire (via {@code recepteurCodeAmi}) n'est pas trouvé (Statut HTTP 404).
     * @throws IllegalArgumentException si l'utilisateur tente d'envoyer une demande à lui-même (Statut HTTP 400/409).
     * @throws AccessDeniedException    si le destinataire n'a pas le rôle requis (MEMBRE) (Statut HTTP 403).
     * @throws IllegalStateException    si une demande/amitié existe déjà entre les deux membres (Statut HTTP 409).
     */
    public DemandeAmi sendFriendRequestByCode(String recepteurCodeAmi) {
        // 1. Obtenir l'ID de l'utilisateur courant (authentifié).
        Integer envoyeurId = securityService.getCurrentUserIdOrThrow();

        // 2. Trouver le membre destinataire par son code ami.
        Membre recepteur = membreRepository.findByCodeAmi(recepteurCodeAmi)
                .orElseThrow(() -> new EntityNotFoundException("Aucun membre trouvé avec le code ami : " + recepteurCodeAmi));
        Integer recepteurId = recepteur.getId();

        // 3. Vérifications métier et de cohérence.
        if (envoyeurId.equals(recepteurId)) {
            throw new IllegalArgumentException("Impossible d'envoyer une demande d'ami à soi-même.");
        }

        // Récupérer l'entité Membre de l'envoyeur (nécessaire pour créer l'objet DemandeAmi).
        // Bien que getCurrentUserIdOrThrow implique son existence, on le récupère.
        Membre envoyeur = membreRepository.findById(envoyeurId)
                .orElseThrow(() -> new EntityNotFoundException("Membre envoyeur (ID: " + envoyeurId + ") non trouvé alors qu'il est authentifié. Incohérence BDD."));

        // Vérifier le rôle du destinataire.
        if (recepteur.getRole() != Role.MEMBRE) {
            throw new AccessDeniedException("Les demandes d'ami ne peuvent être envoyées qu'à des membres (rôle MEMBRE).");
        }

        // Vérifier l'absence de relation existante (en attente ou acceptée).
        if (demandeAmiRepository.existsPendingOrAcceptedRequestBetween(envoyeurId, recepteurId, Statut.ATTENTE, Statut.ACCEPTE)) {
            throw new IllegalStateException("Une demande d'amitié en attente ou une relation acceptée existe déjà entre ces deux membres.");
        }

        // 4. Créer et sauvegarder la nouvelle demande.
        log.info("Envoi d'une demande d'ami de l'utilisateur ID {} vers l'utilisateur ID {}", envoyeurId, recepteurId);
        DemandeAmi nouvelleDemande = new DemandeAmi(envoyeur, recepteur); // Statut mis à ATTENTE par défaut.
        return demandeAmiRepository.save(nouvelleDemande);
    }

    /**
     * Accepte une demande d'amitié reçue par l'utilisateur courant qui est en attente.
     * Met à jour le statut de la {@link DemandeAmi} à {@code ACCEPTEE} et crée
     * la relation d'amitié bidirectionnelle dans la table de jointure 'amitie'
     * en mettant à jour les collections {@code amis} des deux entités {@link Membre}.
     * <p>
     * Sécurité : Vérifie que l'utilisateur courant est bien le destinataire de la demande.
     * </p>
     *
     * @param demandeId L'identifiant de la {@link DemandeAmi} à accepter.
     * @return L'entité {@link DemandeAmi} mise à jour avec le statut {@code ACCEPTEE}.
     * @throws AuthenticationException si l'utilisateur courant n'est pas authentifié.
     * @throws EntityNotFoundException si la demande n'est pas trouvée (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas le destinataire de la demande (Statut HTTP 403).
     * @throws IllegalStateException   si la demande n'a pas le statut {@code ATTENTE} (Statut HTTP 409).
     */
    public DemandeAmi acceptFriendRequest(Integer demandeId) {
        // 1. Obtenir l'ID de l'utilisateur courant.
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // 2. Trouver la demande d'ami.
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'accepter : Demande d'ami non trouvée (ID: " + demandeId + ")"));

        // 3. Vérification Sécurité : L'utilisateur courant est-il le destinataire ?
        if (!Objects.equals(demande.getRecepteur().getId(), currentUserId)) {
            throw new AccessDeniedException("Action non autorisée : Seul le destinataire peut accepter cette demande d'ami.");
        }

        // 4. Vérification Métier : La demande est-elle en attente ?
        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible d'accepter : La demande (ID: " + demandeId + ") n'est plus en attente (Statut: " + demande.getStatut() + ").");
        }

        // 5. Mettre à jour le statut de la demande.
        demande.setStatut(Statut.ACCEPTE);
        DemandeAmi savedDemande = demandeAmiRepository.save(demande);
        log.info("Demande d'ami ID {} acceptée par l'utilisateur ID {}.", demandeId, currentUserId);

        // 6. Mettre à jour la relation @ManyToMany 'amis' (CRUCIAL).
        Membre envoyeur = demande.getEnvoyeur();
        Membre recepteur = demande.getRecepteur();
        // Utilise les méthodes helper de l'entité Membre pour gérer la bidirectionnalité.
        recepteur.addAmi(envoyeur);
        // Sauvegarde les deux membres pour persister la relation dans la table 'amitie'.
        membreRepository.save(recepteur);
        membreRepository.save(envoyeur); // Important de sauvegarder les deux côtés.
        log.debug("Relation d'amitié @ManyToMany mise à jour entre {} et {}", envoyeur.getId(), recepteur.getId());

        return savedDemande;
    }

    /**
     * Refuse une demande d'amitié reçue par l'utilisateur courant qui est en attente.
     * Met à jour le statut de la {@link DemandeAmi} à {@code REFUSEE}.
     * (Alternativement, cette méthode pourrait supprimer la demande).
     * <p>
     * Sécurité : Vérifie que l'utilisateur courant est bien le destinataire de la demande.
     * </p>
     *
     * @param demandeId L'identifiant de la {@link DemandeAmi} à refuser.
     * @return L'entité {@link DemandeAmi} mise à jour avec le statut {@code REFUSEE}.
     * @throws AuthenticationException si l'utilisateur courant n'est pas authentifié.
     * @throws EntityNotFoundException si la demande n'est pas trouvée (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas le destinataire de la demande (Statut HTTP 403).
     * @throws IllegalStateException   si la demande n'a pas le statut {@code ATTENTE} (Statut HTTP 409).
     */
    public DemandeAmi refuseFriendRequest(Integer demandeId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible de refuser : Demande d'ami non trouvée (ID: " + demandeId + ")"));

        if (!Objects.equals(demande.getRecepteur().getId(), currentUserId)) {
            throw new AccessDeniedException("Action non autorisée : Seul le destinataire peut refuser cette demande d'ami.");
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible de refuser : La demande (ID: " + demandeId + ") n'est plus en attente (Statut: " + demande.getStatut() + ").");
        }

        demande.setStatut(Statut.REFUSE); // Met à jour le statut.
        log.info("Demande d'ami ID {} refusée par l'utilisateur ID {}.", demandeId, currentUserId);
        return demandeAmiRepository.save(demande);
    }

    /**
     * Annule une demande d'amitié envoyée par l'utilisateur courant qui est toujours en attente.
     * Supprime l'enregistrement {@link DemandeAmi} correspondant de la base de données.
     * <p>
     * Sécurité : Vérifie que l'utilisateur courant est bien l'expéditeur de la demande.
     * </p>
     *
     * @param demandeId L'identifiant de la {@link DemandeAmi} à annuler.
     * @throws AuthenticationException si l'utilisateur courant n'est pas authentifié.
     * @throws EntityNotFoundException si la demande n'est pas trouvée (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas l'expéditeur de la demande (Statut HTTP 403).
     * @throws IllegalStateException   si la demande n'a pas le statut {@code ATTENTE} (Statut HTTP 409).
     */
    public void cancelFriendRequest(Integer demandeId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        DemandeAmi demande = demandeAmiRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'annuler : Demande d'ami non trouvée (ID: " + demandeId + ")"));

        if (!Objects.equals(demande.getEnvoyeur().getId(), currentUserId)) {
            throw new AccessDeniedException("Action non autorisée : Seul l'expéditeur peut annuler cette demande d'ami.");
        }

        if (demande.getStatut() != Statut.ATTENTE) {
            throw new IllegalStateException("Impossible d'annuler : La demande (ID: " + demandeId + ") n'est plus en attente (Statut: " + demande.getStatut() + ").");
        }

        log.info("Annulation de la demande d'ami ID {} par l'expéditeur ID {}.", demandeId, currentUserId);
        demandeAmiRepository.delete(demande); // Supprime la demande.
    }

    /**
     * Supprime une relation d'amitié existante (représentée par une {@link DemandeAmi} acceptée)
     * entre l'utilisateur courant et un autre membre spécifié par son ID.
     * Met à jour la relation bidirectionnelle {@code @ManyToMany} 'amis' dans les entités {@link Membre}
     * et supprime l'enregistrement {@link DemandeAmi} correspondant.
     *
     * @param friendIdToRemove L'ID du membre avec lequel rompre l'amitié.
     * @throws AuthenticationException  si l'utilisateur courant n'est pas authentifié.
     * @throws IllegalArgumentException si l'utilisateur tente de se supprimer lui-même (Statut HTTP 400/409).
     * @throws EntityNotFoundException  si aucune amitié acceptée n'est trouvée entre les deux utilisateurs,
     *                                  ou si l'un des membres n'est pas trouvé en BDD (cas d'incohérence) (Statut HTTP 404).
     */
    public void removeFriendshipByFriendId(Integer friendIdToRemove) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        if (currentUserId.equals(friendIdToRemove)) {
            throw new IllegalArgumentException("Action invalide : Impossible de se supprimer soi-même de sa liste d'amis.");
        }

        // 1. Trouver l'enregistrement DemandeAmi qui représente l'amitié acceptée.
        DemandeAmi friendship = demandeAmiRepository.findAcceptedFriendshipBetween(currentUserId, friendIdToRemove, Statut.ACCEPTE)
                .orElseThrow(() -> new EntityNotFoundException("Aucune relation d'amitié active trouvée avec l'utilisateur ID: " + friendIdToRemove));
        Integer demandeId = friendship.getId(); // Pour les logs

        // 2. Récupérer les entités Membre pour la mise à jour ManyToMany.
        Membre currentUser = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Incohérence : Utilisateur courant (ID: " + currentUserId + ") non trouvé."));
        Membre friendToRemove = membreRepository.findById(friendIdToRemove)
                .orElseThrow(() -> new EntityNotFoundException("Membre ami à supprimer (ID: " + friendIdToRemove + ") non trouvé."));

        // 3. Mettre à jour la relation @ManyToMany 'amis' (bidirectionnelle).
        currentUser.removeAmi(friendToRemove); // Utilise la méthode helper de Membre.
        // Sauvegarder les deux entités pour persister la suppression dans la table 'amitie'.
        membreRepository.save(currentUser);
        membreRepository.save(friendToRemove);
        log.debug("Relation d'amitié @ManyToMany rompue entre {} et {}", currentUserId, friendIdToRemove);

        // 4. Supprimer l'enregistrement DemandeAmi original.
        demandeAmiRepository.delete(friendship);
        log.info("Amitié (Demande ID: {}) entre l'utilisateur ID {} et l'utilisateur ID {} supprimée.", demandeId, currentUserId, friendIdToRemove);
    }

    // --- Méthodes de Consultation ---

    /**
     * Récupère la liste des demandes d'amitié reçues par l'utilisateur courant
     * qui sont actuellement en attente ({@code Statut.ATTENTE}).
     *
     * @return Une {@code List<DemandeAmi>} contenant les demandes reçues en attente. Peut être vide.
     * @throws AuthenticationException si l'utilisateur courant n'est pas authentifié.
     */
    @Transactional(readOnly = true) // Optimisation lecture seule.
    public List<DemandeAmi> getPendingReceivedRequests() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return demandeAmiRepository.findByRecepteurIdAndStatut(currentUserId, Statut.ATTENTE);
    }

    /**
     * Récupère la liste des demandes d'amitié envoyées par l'utilisateur courant
     * qui sont actuellement en attente ({@code Statut.ATTENTE}).
     *
     * @return Une {@code List<DemandeAmi>} contenant les demandes envoyées en attente. Peut être vide.
     * @throws AuthenticationException si l'utilisateur courant n'est pas authentifié.
     */
    @Transactional(readOnly = true) // Optimisation lecture seule.
    public List<DemandeAmi> getPendingSentRequests() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        return demandeAmiRepository.findByEnvoyeurIdAndStatut(currentUserId, Statut.ATTENTE);
    }

    /**
     * Récupère la liste des entités {@link Membre} qui sont amis avec l'utilisateur courant.
     * L'amitié est déterminée par les enregistrements {@link DemandeAmi} ayant le statut {@code ACCEPTEE}.
     * <p>
     * Utilise une approche en deux étapes pour des raisons de compatibilité/performance :
     * 1. Récupère les IDs de tous les amis via {@link DemandeAmiDao#findFriendIdsOfUser}.
     * 2. Récupère les entités {@link Membre} correspondantes via {@code membreRepository.findAllById()}.
     * </p>
     *
     * @return Une {@code List<Membre>} contenant les amis de l'utilisateur courant. Peut être vide.
     * @throws AuthenticationException si l'utilisateur courant n'est pas authentifié.
     */
    @Transactional(readOnly = true) // Optimisation lecture seule.
    public List<Membre> getFriends() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // Étape 1: Récupérer les IDs des amis (via la requête optimisée dans le DAO).
        List<Integer> friendIds = demandeAmiRepository.findFriendIdsOfUser(currentUserId, Statut.ACCEPTE);

        // Si l'utilisateur n'a pas d'amis, retourner une liste vide immédiatement.
        if (friendIds == null || friendIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Étape 2: Récupérer les entités Membre complètes pour les IDs trouvés.
        // findAllById est efficace pour récupérer plusieurs entités par leurs IDs.
        // Note: L'ordre n'est pas garanti par findAllById. Si l'ordre est important, un tri supplémentaire serait nécessaire.
        List<Membre> friends = membreRepository.findAllById(friendIds);
        log.debug("Récupération de {} ami(s) pour l'utilisateur ID {}.", friends.size(), currentUserId);
        return friends;
    }
}

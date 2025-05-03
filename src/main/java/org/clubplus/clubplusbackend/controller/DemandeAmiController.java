package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.service.DemandeAmiService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les opérations liées aux demandes d'ami ({@link DemandeAmi})
 * et aux relations d'amitié entre membres ({@link Membre}).
 * Fournit des endpoints pour envoyer, accepter, refuser, annuler des demandes,
 * ainsi que pour lister les demandes en attente, lister les amis et supprimer une amitié.
 * La sécurité de base est assurée par l'annotation {@link IsMembre @IsMembre} (nécessite une authentification),
 * tandis que les vérifications contextuelles spécifiques (ex: vérifier si l'utilisateur est le destinataire/envoyeur)
 * sont déléguées au {@link DemandeAmiService}.
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /amis} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see DemandeAmi
 * @see Membre
 * @see DemandeAmiService
 * @see SecurityService
 */
@RestController
@RequestMapping("/amis") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor // Injection via constructeur pour les champs final (Lombok)
@CrossOrigin // Activation de CORS si nécessaire (peut être géré globalement)
public class DemandeAmiController {

    /**
     * Service contenant la logique métier pour la gestion des demandes d'ami et des amitiés.
     *
     * @see DemandeAmiService
     */
    private final DemandeAmiService demandeAmiService;
    // SecurityService est injecté dans DemandeAmiService et utilisé là-bas,
    // donc pas besoin de l'injecter ou de le référencer directement ici
    // si le contrôleur ne l'utilise pas lui-même.

    /**
     * Envoie une demande d'ami à un autre membre en utilisant le code ami unique de ce dernier.
     * <p>
     * <b>Requête:</b> POST /amis/demandes/envoyer-par-code?code={recepteurCodeAmi}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * L'identifiant de l'envoyeur est récupéré automatiquement via le contexte de sécurité dans le service.
     * </p>
     *
     * @param recepteurCodeAmi Le code ami unique (String) du membre destinataire de la demande.
     *                         Fourni comme paramètre de requête obligatoire ('code').
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> La {@link DemandeAmi} créée avec statut ATTENTE, sérialisée selon {@link GlobalView.DemandeView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si aucun membre ne correspond au {@code recepteurCodeAmi} (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si l'utilisateur tente de s'envoyer une demande à lui-même (levé par le service via {@link IllegalArgumentException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si le destinataire n'a pas le rôle MEMBRE (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si une demande en attente ou une amitié acceptée existe déjà (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security).</li>
     * </ul>
     * @see DemandeAmiService#sendFriendRequestByCode(String)
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @PostMapping("/demandes/envoyer-par-code") // Utilisation du code ami pour l'envoi
    @IsMembre // Requiert une authentification rôle MEMBRE
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour la demande créée
    public ResponseEntity<DemandeAmi> sendFriendRequestByCode(@RequestParam("code") String recepteurCodeAmi) {
        // @ResponseStatus retiré, HttpStatus.CREATED est géré par ResponseEntity
        DemandeAmi nouvelleDemande = demandeAmiService.sendFriendRequestByCode(recepteurCodeAmi);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleDemande);
    }

    /**
     * Accepte une demande d'ami reçue par l'utilisateur actuellement authentifié.
     * <p>
     * <b>Requête:</b> PUT /amis/demandes/accepter/{demandeId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * Le service vérifie que l'utilisateur courant est bien le destinataire de la demande.
     * </p>
     *
     * @param demandeId L'identifiant unique (Integer) de la demande d'ami à accepter. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> La {@link DemandeAmi} mise à jour avec le statut ACCEPTEE, sérialisée selon {@link GlobalView.DemandeView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la demande avec l'ID spécifié n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur authentifié n'est pas le destinataire de cette demande (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si la demande n'est pas dans l'état ATTENTE (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security).</li>
     * </ul>
     * @see DemandeAmiService#acceptFriendRequest(Integer)
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @PutMapping("/demandes/accepter/{demandeId}")
    @IsMembre // Requiert une authentification rôle MEMBRE
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour la demande mise à jour
    public ResponseEntity<DemandeAmi> acceptFriendRequest(@PathVariable Integer demandeId) {
        DemandeAmi demandeAcceptee = demandeAmiService.acceptFriendRequest(demandeId);
        return ResponseEntity.ok(demandeAcceptee);
    }

    /**
     * Refuse une demande d'ami reçue par l'utilisateur actuellement authentifié.
     * <p>
     * <b>Requête:</b> PUT /amis/demandes/refuser/{demandeId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * Le service vérifie que l'utilisateur courant est bien le destinataire de la demande.
     * </p>
     *
     * @param demandeId L'identifiant unique (Integer) de la demande d'ami à refuser. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> La {@link DemandeAmi} mise à jour avec le statut REFUSEE, sérialisée selon {@link GlobalView.DemandeView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la demande n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas le destinataire (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si la demande n'est pas ATTENTE (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security).</li>
     * </ul>
     * @see DemandeAmiService#refuseFriendRequest(Integer)
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @PutMapping("/demandes/refuser/{demandeId}")
    @IsMembre // Requiert une authentification rôle MEMBRE
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour la demande mise à jour
    public ResponseEntity<DemandeAmi> refuseFriendRequest(@PathVariable Integer demandeId) {
        DemandeAmi demandeRefusee = demandeAmiService.refuseFriendRequest(demandeId);
        return ResponseEntity.ok(demandeRefusee);
    }

    /**
     * Annule une demande d'ami précédemment envoyée par l'utilisateur actuellement authentifié.
     * Cette opération supprime la demande si elle est encore en attente.
     * <p>
     * <b>Requête:</b> DELETE /amis/demandes/annuler/{demandeId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * Le service vérifie que l'utilisateur courant est bien l'envoyeur de la demande.
     * </p>
     *
     * @param demandeId L'identifiant unique (Integer) de la demande d'ami à annuler. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws EntityNotFoundException Si la demande n'existe pas (levé par le service -> 404).
     * @throws AccessDeniedException   Si l'utilisateur n'est pas l'envoyeur (levé par le service -> 403).
     * @throws IllegalStateException   Si la demande n'est pas ATTENTE (levé par le service -> 409).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré par Spring Security -> 401).
     * @see DemandeAmiService#cancelFriendRequest(Integer)
     * @see IsMembre
     */
    @DeleteMapping("/demandes/annuler/{demandeId}")
    @IsMembre // Requiert une authentification rôle MEMBRE
    public ResponseEntity<Void> cancelFriendRequest(@PathVariable Integer demandeId) {
        // @ResponseStatus retiré, HttpStatus.NO_CONTENT est géré par ResponseEntity
        demandeAmiService.cancelFriendRequest(demandeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprime la relation d'amitié entre l'utilisateur actuellement authentifié et un autre membre.
     * Cela supprime l'enregistrement {@code DemandeAmi} acceptée et met à jour la relation ManyToMany.
     * <p>
     * <b>Requête:</b> DELETE /amis/{amiId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * Le service vérifie qu'une amitié existe bien entre l'utilisateur courant et l'utilisateur spécifié par {@code amiId}.
     * </p>
     *
     * @param amiId L'identifiant unique (Integer) du membre avec lequel rompre l'amitié. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws EntityNotFoundException  Si aucune relation d'amitié active n'existe entre les deux utilisateurs,
     *                                  ou si l'un des utilisateurs n'est pas trouvé (levé par le service -> 404).
     * @throws IllegalArgumentException Si l'utilisateur tente de se supprimer lui-même comme ami (levé par le service -> 400).
     * @throws AuthenticationException  Si l'utilisateur n'est pas authentifié (géré par Spring Security -> 401).
     * @see DemandeAmiService#removeFriendshipByFriendId(Integer)
     * @see IsMembre
     */
    @DeleteMapping("/{amiId}")
    @IsMembre // Requiert une authentification rôle MEMBRE
    public ResponseEntity<Void> removeFriend(@PathVariable Integer amiId) {
        // @ResponseStatus retiré, HttpStatus.NO_CONTENT est géré par ResponseEntity
        demandeAmiService.removeFriendshipByFriendId(amiId);
        return ResponseEntity.noContent().build();
    }

    // --- Consultation des Demandes et Amis ---

    /**
     * Récupère la liste des demandes d'ami reçues par l'utilisateur courant qui sont encore en attente de réponse.
     * <p>
     * <b>Requête:</b> GET /amis/demandes/recues
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<DemandeAmi>} des demandes reçues en attente, sérialisée selon {@link GlobalView.DemandeView}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security).</li>
     * </ul>
     * @see DemandeAmiService#getPendingReceivedRequests()
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @GetMapping("/demandes/recues")
    @IsMembre // Requiert une authentification rôle MEMBRE
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour les demandes listées
    public ResponseEntity<List<DemandeAmi>> getPendingReceivedRequests() {
        List<DemandeAmi> demandes = demandeAmiService.getPendingReceivedRequests();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère la liste des demandes d'ami envoyées par l'utilisateur courant qui sont encore en attente de réponse.
     * <p>
     * <b>Requête:</b> GET /amis/demandes/envoyees
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<DemandeAmi>} des demandes envoyées en attente, sérialisée selon {@link GlobalView.DemandeView}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security).</li>
     * </ul>
     * @see DemandeAmiService#getPendingSentRequests()
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @GetMapping("/demandes/envoyees")
    @IsMembre // Requiert une authentification rôle MEMBRE
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour les demandes listées
    public ResponseEntity<List<DemandeAmi>> getPendingSentRequests() {
        List<DemandeAmi> demandes = demandeAmiService.getPendingSentRequests();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère la liste des membres qui sont amis avec l'utilisateur actuellement authentifié.
     * <p>
     * <b>Requête:</b> GET /amis
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Membre>} des amis, sérialisée selon {@link GlobalView.Base}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security).</li>
     * </ul>
     * @see DemandeAmiService#getFriends()
     * @see GlobalView.Base
     * @see IsMembre
     */
    @GetMapping("") // Chemin GET /amis
    @IsMembre // Requiert une authentification rôle MEMBRE
    @JsonView(GlobalView.Base.class) // Vue JSON de base pour la liste d'amis (infos limitées)
    public ResponseEntity<List<Membre>> getFriends() {
        List<Membre> amis = demandeAmiService.getFriends();
        return ResponseEntity.ok(amis);
    }

    // Rappel : La gestion des exceptions spécifiques levées par le service
    // (EntityNotFoundException, AccessDeniedException, IllegalStateException, IllegalArgumentException)
    // et leur mapping vers les réponses HTTP appropriées (404, 403, 409, 400)
    // est idéalement gérée de manière centralisée par un @ControllerAdvice (GlobalExceptionHandler).
}

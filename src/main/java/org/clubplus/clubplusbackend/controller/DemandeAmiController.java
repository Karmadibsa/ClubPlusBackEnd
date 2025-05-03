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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les opérations liées aux demandes d'ami ({@link DemandeAmi})
 * et aux relations d'amitié entre membres ({@link Membre}).
 * Fournit des endpoints pour envoyer, accepter, refuser, annuler des demandes,
 * ainsi que pour lister les demandes en attente, lister les amis et supprimer une amitié.
 * La sécurité de base est assurée par l'annotation {@code @IsMembre} (nécessite une authentification),
 * tandis que les vérifications contextuelles spécifiques (ex: vérifier si l'utilisateur est le destinataire/envoyeur)
 * sont déléguées au {@link DemandeAmiService} qui utilise le {@link SecurityService}.
 * Les exceptions levées par le service sont interceptées par le {@code GlobalExceptionHandler}.
 * Le chemin de base pour ce contrôleur est "/api/amis".
 */
@RestController
@RequestMapping("/api/amis") // Chemin de base pour toutes les opérations liées aux amis et demandes
@RequiredArgsConstructor // Injection via constructeur pour les champs final (Lombok)
@CrossOrigin // Activation de CORS si nécessaire (peut être géré globalement)
public class DemandeAmiController {

    /**
     * Service contenant la logique métier pour la gestion des demandes d'ami et des amitiés.
     */
    private final DemandeAmiService demandeAmiService;
    /**
     * Service utilitaire pour récupérer les informations de l'utilisateur actuellement authentifié (utilisé par demandeAmiService).
     */
    private final SecurityService securityService; // Injecté mais utilisé indirectement via demandeAmiService

    /**
     * Envoie une demande d'ami à un autre membre en utilisant le code ami unique de ce dernier.
     * Endpoint: POST /api/amis/demandes/envoyer-par-code?code={recepteurCodeAmi}
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     * L'identifiant de l'envoyeur est récupéré automatiquement via le contexte de sécurité dans le service.
     *
     * @param recepteurCodeAmi Le code ami unique (String) du membre destinataire de la demande. Fourni comme paramètre de requête obligatoire.
     * @return La {@link DemandeAmi} nouvellement créée avec le statut EN_ATTENTE, sérialisée selon {@link GlobalView.DemandeView}.
     * Retourne un statut HTTP 201 (Created) en cas de succès.
     * @throws EntityNotFoundException  Si aucun membre ne correspond au {@code recepteurCodeAmi} fourni (géré globalement -> 404 Not Found).
     * @throws IllegalArgumentException Si l'utilisateur tente de s'envoyer une demande à lui-même (géré globalement -> 409 Conflict ou 400 Bad Request selon le handler).
     * @throws AccessDeniedException    Si le destinataire n'est pas un membre avec le rôle MEMBRE (géré globalement -> 403 Forbidden).
     * @throws IllegalStateException    Si une demande en attente ou une amitié acceptée existe déjà entre ces deux membres (géré globalement -> 409 Conflict).
     * @throws AuthenticationException  Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#sendFriendRequestByCode(String)
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @PostMapping("/demandes/envoyer-par-code") // Utilisation du code ami pour l'envoi
    @IsMembre // Requiert une authentification
    @ResponseStatus(HttpStatus.CREATED) // Réponse HTTP 201 en cas de succès
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour la demande créée
    public DemandeAmi sendFriendRequestByCode(@RequestParam("code") String recepteurCodeAmi) {
        return demandeAmiService.sendFriendRequestByCode(recepteurCodeAmi);
    }

    /**
     * Accepte une demande d'ami reçue par l'utilisateur actuellement authentifié.
     * Endpoint: PUT /api/amis/demandes/accepter/{demandeId}
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     * Le service vérifie que l'utilisateur courant est bien le destinataire de la demande.
     *
     * @param demandeId L'identifiant unique (Integer) de la demande d'ami à accepter. Provient du chemin de l'URL.
     * @return La {@link DemandeAmi} mise à jour avec le statut ACCEPTEE, sérialisée selon {@link GlobalView.DemandeView}.
     * Retourne un statut HTTP 200 (OK) par défaut en cas de succès.
     * @throws EntityNotFoundException Si la demande avec l'ID spécifié n'existe pas (géré globalement -> 404 Not Found).
     * @throws AccessDeniedException   Si l'utilisateur authentifié n'est pas le destinataire de cette demande (géré globalement -> 403 Forbidden).
     * @throws IllegalStateException   Si la demande n'est pas dans l'état EN_ATTENTE (géré globalement -> 409 Conflict).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#acceptFriendRequest(Integer)
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @PutMapping("/demandes/accepter/{demandeId}")
    @IsMembre // Requiert une authentification
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour la demande mise à jour
    public DemandeAmi acceptFriendRequest(@PathVariable Integer demandeId) {
        return demandeAmiService.acceptFriendRequest(demandeId);
    }

    /**
     * Refuse une demande d'ami reçue par l'utilisateur actuellement authentifié.
     * Endpoint: PUT /api/amis/demandes/refuser/{demandeId}
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     * Le service vérifie que l'utilisateur courant est bien le destinataire de la demande.
     *
     * @param demandeId L'identifiant unique (Integer) de la demande d'ami à refuser. Provient du chemin de l'URL.
     * @return La {@link DemandeAmi} mise à jour avec le statut REFUSEE, sérialisée selon {@link GlobalView.DemandeView}.
     * Retourne un statut HTTP 200 (OK) par défaut en cas de succès.
     * @throws EntityNotFoundException Si la demande n'existe pas (géré globalement -> 404 Not Found).
     * @throws AccessDeniedException   Si l'utilisateur n'est pas le destinataire (géré globalement -> 403 Forbidden).
     * @throws IllegalStateException   Si la demande n'est pas EN_ATTENTE (géré globalement -> 409 Conflict).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#refuseFriendRequest(Integer)
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @PutMapping("/demandes/refuser/{demandeId}")
    @IsMembre // Requiert une authentification
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour la demande mise à jour
    public DemandeAmi refuseFriendRequest(@PathVariable Integer demandeId) {
        return demandeAmiService.refuseFriendRequest(demandeId);
    }

    /**
     * Annule une demande d'ami précédemment envoyée par l'utilisateur actuellement authentifié.
     * Endpoint: DELETE /api/amis/demandes/annuler/{demandeId}
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     * Le service vérifie que l'utilisateur courant est bien l'envoyeur de la demande.
     *
     * @param demandeId L'identifiant unique (Integer) de la demande d'ami à annuler. Provient du chemin de l'URL.
     * @return Rien (statut HTTP 204 No Content est retourné automatiquement en cas de succès).
     * @throws EntityNotFoundException Si la demande n'existe pas (géré globalement -> 404 Not Found).
     * @throws AccessDeniedException   Si l'utilisateur n'est pas l'envoyeur (géré globalement -> 403 Forbidden).
     * @throws IllegalStateException   Si la demande n'est pas EN_ATTENTE (géré globalement -> 409 Conflict).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#cancelFriendRequest(Integer)
     * @see IsMembre
     */
    @DeleteMapping("/demandes/annuler/{demandeId}")
    @IsMembre // Requiert une authentification
    @ResponseStatus(HttpStatus.NO_CONTENT) // Réponse HTTP 204 en cas de succès
    public void cancelFriendRequest(@PathVariable Integer demandeId) {
        demandeAmiService.cancelFriendRequest(demandeId);
    }

    /**
     * Supprime la relation d'amitié entre l'utilisateur actuellement authentifié et un autre membre.
     * Cela supprime l'entrée correspondante dans la table d'association des amitiés (`amitie`)
     * et l'enregistrement {@code DemandeAmi} qui représentait cette amitié acceptée.
     * Endpoint: DELETE /api/amis/{amiId}
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     * Le service vérifie qu'une amitié existe bien entre l'utilisateur courant et l'utilisateur spécifié par {@code amiId}.
     *
     * @param amiId L'identifiant unique (Integer) du membre avec lequel rompre l'amitié. Provient du chemin de l'URL.
     * @return Rien (statut HTTP 204 No Content est retourné automatiquement en cas de succès).
     * @throws EntityNotFoundException  Si aucune relation d'amitié active (DemandeAmi acceptée) n'existe entre les deux utilisateurs,
     *                                  ou si l'un des utilisateurs n'est pas trouvé (géré globalement -> 404 Not Found).
     * @throws IllegalArgumentException Si l'utilisateur tente de se supprimer lui-même comme ami (géré globalement -> 409 Conflict ou 400 Bad Request).
     * @throws AuthenticationException  Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#removeFriendshipByFriendId(Integer)
     * @see IsMembre
     */
    @DeleteMapping("/{amiId}")
    @IsMembre // Requiert une authentification
    @ResponseStatus(HttpStatus.NO_CONTENT) // Réponse HTTP 204 en cas de succès
    public void removeFriend(@PathVariable Integer amiId) {
        demandeAmiService.removeFriendshipByFriendId(amiId);
    }

    // --- Consultation des Demandes et Amis ---

    /**
     * Récupère la liste des demandes d'ami reçues par l'utilisateur courant qui sont encore en attente de réponse.
     * Endpoint: GET /api/amis/demandes/recues
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     *
     * @return Une {@code List<DemandeAmi>} contenant les demandes reçues en attente, sérialisée selon {@link GlobalView.DemandeView}.
     * Peut être une liste vide si aucune demande n'est en attente.
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#getPendingReceivedRequests()
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @GetMapping("/demandes/recues")
    @IsMembre // Requiert une authentification
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour les demandes listées
    public List<DemandeAmi> getPendingReceivedRequests() {
        return demandeAmiService.getPendingReceivedRequests();
    }

    /**
     * Récupère la liste des demandes d'ami envoyées par l'utilisateur courant qui sont encore en attente de réponse.
     * Endpoint: GET /api/amis/demandes/envoyees
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     *
     * @return Une {@code List<DemandeAmi>} contenant les demandes envoyées en attente, sérialisée selon {@link GlobalView.DemandeView}.
     * Peut être une liste vide.
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#getPendingSentRequests()
     * @see GlobalView.DemandeView
     * @see IsMembre
     */
    @GetMapping("/demandes/envoyees")
    @IsMembre // Requiert une authentification
    @JsonView(GlobalView.DemandeView.class) // Vue JSON pour les demandes listées
    public List<DemandeAmi> getPendingSentRequests() {
        return demandeAmiService.getPendingSentRequests();
    }

    /**
     * Récupère la liste des membres qui sont amis avec l'utilisateur actuellement authentifié.
     * Endpoint: GET /api/amis
     * Sécurité: Requiert que l'utilisateur soit authentifié ({@code @IsMembre}).
     *
     * @return Une {@code List<Membre>} contenant les informations de base des amis de l'utilisateur courant,
     * sérialisée selon {@link GlobalView.Base}. Peut être une liste vide.
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré globalement -> 401 Unauthorized).
     * @see DemandeAmiService#getFriends()
     * @see GlobalView.Base
     * @see IsMembre
     */
    @GetMapping("") // Chemin GET /api/amis
    @IsMembre // Requiert une authentification
    @JsonView(GlobalView.Base.class) // Vue JSON de base pour la liste d'amis (infos limitées)
    public List<Membre> getFriends() {
        return demandeAmiService.getFriends();
    }

    // La gestion centralisée des exceptions via @ControllerAdvice est la pratique recommandée.
}

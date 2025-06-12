package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.service.DemandeAmiService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les demandes d'ami et les relations d'amitié.
 * <p>
 * Base URL: /amis
 * </p>
 */
@RestController
@RequestMapping("/amis")
@RequiredArgsConstructor
@CrossOrigin
public class DemandeAmiController {

    private final DemandeAmiService demandeAmiService;

    /**
     * Envoie une demande d'ami via le code unique d'un autre membre.
     * <p>
     * Endpoint: POST /amis/demandes/envoyer-par-code?code={recepteurCodeAmi}
     * <p>
     * Accès réservé aux membres authentifiés.
     *
     * @param recepteurCodeAmi Le code ami unique du destinataire.
     * @return La demande d'ami créée (201 Created).
     */
    @PostMapping("/demandes/envoyer-par-code")
    @IsMembre
    @JsonView(GlobalView.DemandeView.class)
    public ResponseEntity<DemandeAmi> sendFriendRequestByCode(@RequestParam("code") String recepteurCodeAmi) {
        DemandeAmi nouvelleDemande = demandeAmiService.sendFriendRequestByCode(recepteurCodeAmi);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleDemande);
    }

    /**
     * Accepte une demande d'ami reçue.
     * <p>
     * Endpoint: PUT /amis/demandes/accepter/{demandeId}
     * <p>
     * L'utilisateur authentifié doit être le destinataire de la demande.
     *
     * @param demandeId L'ID de la demande à accepter.
     * @return La demande mise à jour avec le statut ACCEPTEE (200 OK).
     */
    @PutMapping("/demandes/accepter/{demandeId}")
    @IsMembre
    @JsonView(GlobalView.DemandeView.class)
    public ResponseEntity<DemandeAmi> acceptFriendRequest(@PathVariable Integer demandeId) {
        DemandeAmi demandeAcceptee = demandeAmiService.acceptFriendRequest(demandeId);
        return ResponseEntity.ok(demandeAcceptee);
    }

    /**
     * Refuse une demande d'ami reçue.
     * <p>
     * Endpoint: PUT /amis/demandes/refuser/{demandeId}
     * <p>
     * L'utilisateur authentifié doit être le destinataire de la demande.
     *
     * @param demandeId L'ID de la demande à refuser.
     * @return La demande mise à jour avec le statut REFUSEE (200 OK).
     */
    @PutMapping("/demandes/refuser/{demandeId}")
    @IsMembre
    @JsonView(GlobalView.DemandeView.class)
    public ResponseEntity<DemandeAmi> refuseFriendRequest(@PathVariable Integer demandeId) {
        DemandeAmi demandeRefusee = demandeAmiService.refuseFriendRequest(demandeId);
        return ResponseEntity.ok(demandeRefusee);
    }

    /**
     * Annule une demande d'ami envoyée.
     * <p>
     * Endpoint: DELETE /amis/demandes/annuler/{demandeId}
     * <p>
     * L'utilisateur authentifié doit être l'envoyeur de la demande.
     *
     * @param demandeId L'ID de la demande à annuler.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/demandes/annuler/{demandeId}")
    @IsMembre
    public ResponseEntity<Void> cancelFriendRequest(@PathVariable Integer demandeId) {
        demandeAmiService.cancelFriendRequest(demandeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprime une relation d'amitié.
     * <p>
     * Endpoint: DELETE /amis/{amiId}
     * <p>
     * L'utilisateur authentifié doit être ami avec l'utilisateur spécifié.
     *
     * @param amiId L'ID de l'ami à supprimer.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/{amiId}")
    @IsMembre
    public ResponseEntity<Void> removeFriend(@PathVariable Integer amiId) {
        demandeAmiService.removeFriendshipByFriendId(amiId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les demandes d'ami en attente reçues par l'utilisateur.
     * <p>
     * Endpoint: GET /amis/demandes/recues
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @return La liste des demandes reçues en attente (200 OK).
     */
    @GetMapping("/demandes/recues")
    @IsMembre
    @JsonView(GlobalView.DemandeView.class)
    public ResponseEntity<List<DemandeAmi>> getPendingReceivedRequests() {
        List<DemandeAmi> demandes = demandeAmiService.getPendingReceivedRequests();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère les demandes d'ami en attente envoyées par l'utilisateur.
     * <p>
     * Endpoint: GET /amis/demandes/envoyees
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @return La liste des demandes envoyées en attente (200 OK).
     */
    @GetMapping("/demandes/envoyees")
    @IsMembre
    @JsonView(GlobalView.DemandeView.class)
    public ResponseEntity<List<DemandeAmi>> getPendingSentRequests() {
        List<DemandeAmi> demandes = demandeAmiService.getPendingSentRequests();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère la liste d'amis de l'utilisateur.
     * <p>
     * Endpoint: GET /amis
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @return La liste des amis (200 OK).
     */
    @GetMapping("")
    @IsMembre
    @JsonView(GlobalView.Base.class)
    public ResponseEntity<List<Membre>> getFriends() {
        List<Membre> amis = demandeAmiService.getFriends();
        return ResponseEntity.ok(amis);
    }
}

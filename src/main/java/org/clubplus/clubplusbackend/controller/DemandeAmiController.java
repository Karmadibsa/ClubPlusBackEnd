package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.clubplus.clubplusbackend.service.DemandeAmiService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// Retrait Map, Optional, Authentication, MembreService si plus nécessaires

/**
 * Controller gérant les demandes d'ami et les relations d'amitié.
 * Sécurité de base (@PreAuthorize), les vérifications contextuelles sont dans DemandeAmiService.
 */
@RestController
@RequestMapping("/api/amis") // Ou "/api/friendships" ? "/api/amis" est bien.
@RequiredArgsConstructor
@CrossOrigin
public class DemandeAmiController {

    private final DemandeAmiService demandeAmiService;
    private final SecurityService securityService;


    /**
     * POST /api/amis/demandes/envoyer/{recepteurId}
     * Envoie une demande d'ami à un autre utilisateur.
     * Sécurité: Utilisateur authentifié.
     * Exceptions (gérées globalement): 404 (Récepteur non trouvé), 400 (Auto-demande),
     * 409 (Demande/Amitié existante), 401 (Non authentifié).
     */
    @PostMapping("/demandes/envoyer/{recepteurId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED) // Statut 201
    @JsonView(GlobalView.DemandeView.class) // Retourne la demande créée
    public DemandeAmi sendFriendRequest(@PathVariable Integer recepteurId) {
        // Le service utilise SecurityService pour obtenir l'envoyeurId
        return demandeAmiService.sendFriendRequest(securityService.getCurrentUserIdOrThrow(), recepteurId); // Passer l'ID courant explicitement
        // Ou si le service est modifié pour récupérer l'ID lui-même:
        // return demandeAmiService.sendFriendRequest(recepteurId);
    }

    /**
     * PUT /api/amis/demandes/accepter/{demandeId}
     * Accepte une demande d'ami reçue.
     * Sécurité: Utilisateur authentifié. Vérification du récepteur dans le service.
     * Exceptions (gérées globalement): 404 (Demande non trouvée), 403 (Pas le récepteur),
     * 409 (Demande pas en attente), 401 (Non authentifié).
     */
    @PutMapping("/demandes/accepter/{demandeId}")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.DemandeView.class) // Retourne la demande mise à jour
    public DemandeAmi acceptFriendRequest(@PathVariable Integer demandeId) {
        // Le service utilise SecurityService pour vérifier que l'utilisateur courant est le récepteur
        return demandeAmiService.acceptFriendRequest(demandeId);
    }

    /**
     * PUT /api/amis/demandes/refuser/{demandeId}
     * Refuse une demande d'ami reçue.
     * Sécurité: Utilisateur authentifié. Vérification du récepteur dans le service.
     * Exceptions (gérées globalement): 404, 403, 409, 401.
     */
    @PutMapping("/demandes/refuser/{demandeId}")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.DemandeView.class) // Retourne la demande mise à jour (avec statut REFUSEE)
    public DemandeAmi refuseFriendRequest(@PathVariable Integer demandeId) {
        // Le service utilise SecurityService pour vérifier que l'utilisateur courant est le récepteur
        return demandeAmiService.refuseFriendRequest(demandeId);
        // Si le service supprime la demande, changer le retour en void + @ResponseStatus(NO_CONTENT)
    }

    /**
     * DELETE /api/amis/demandes/annuler/{demandeId}
     * Annule une demande d'ami envoyée par l'utilisateur courant.
     * Sécurité: Utilisateur authentifié. Vérification de l'envoyeur dans le service.
     * Exceptions (gérées globalement): 404, 403, 409, 401.
     */
    @DeleteMapping("/demandes/annuler/{demandeId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Statut 204
    public void cancelFriendRequest(@PathVariable Integer demandeId) {
        // Le service utilise SecurityService pour vérifier que l'utilisateur courant est l'envoyeur
        demandeAmiService.cancelFriendRequest(demandeId);
    }

    /**
     * DELETE /api/amis/{amiId}
     * Supprime une relation d'amitié avec un autre utilisateur.
     * Sécurité: Utilisateur authentifié. Vérification de l'implication dans l'amitié dans le service.
     * Exceptions (gérées globalement): 404 (Amitié non trouvée), 400 (Auto-suppression), 401.
     */
    @DeleteMapping("/{amiId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Statut 204
    public void removeFriend(@PathVariable Integer amiId) {
        // Le service utilise SecurityService pour obtenir l'ID courant et vérifier l'amitié
        demandeAmiService.removeFriendshipByFriendId(amiId);
    }

    // --- Consultation ---

    /**
     * GET /api/amis/demandes/recues
     * Récupère les demandes d'ami reçues en attente par l'utilisateur courant.
     * Sécurité: Utilisateur authentifié.
     * Exceptions (gérées globalement): 401.
     */
    @GetMapping("/demandes/recues")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.DemandeView.class)
    public List<DemandeAmi> getPendingReceivedRequests() {
        // Le service utilise SecurityService pour obtenir l'ID courant
        return demandeAmiService.getPendingReceivedRequests();
    }

    /**
     * GET /api/amis/demandes/envoyees
     * Récupère les demandes d'ami envoyées en attente par l'utilisateur courant.
     * Sécurité: Utilisateur authentifié.
     * Exceptions (gérées globalement): 401.
     */
    @GetMapping("/demandes/envoyees")
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.DemandeView.class)
    public List<DemandeAmi> getPendingSentRequests() {
        // Le service utilise SecurityService pour obtenir l'ID courant
        return demandeAmiService.getPendingSentRequests();
    }

    /**
     * GET /api/amis
     * Récupère la liste des amis (objets Membre) de l'utilisateur courant.
     * Sécurité: Utilisateur authentifié.
     * Exceptions (gérées globalement): 401.
     */
    @GetMapping("") // GET /api/amis
    @PreAuthorize("isAuthenticated()")
    @JsonView(GlobalView.Base.class) // Vue de base pour la liste d'amis
    public List<Membre> getFriends() {
        // Le service utilise SecurityService pour obtenir l'ID courant
        return demandeAmiService.getFriends();
    }

    // Pas besoin de @ExceptionHandler ici, doit être dans GlobalExceptionHandler.
}

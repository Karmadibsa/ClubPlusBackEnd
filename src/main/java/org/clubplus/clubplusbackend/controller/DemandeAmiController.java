package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.DemandeAmiDao;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Statut;
import org.clubplus.clubplusbackend.service.DemandeAmiService;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/amis") // Préfixe pour les opérations liées aux amis/demandes
@RequiredArgsConstructor
@CrossOrigin
public class DemandeAmiController {

    private final DemandeAmiService demandeAmiService;
    private final DemandeAmiDao demandeAmiRepository;
    private final MembreService membreService; // Nécessaire pour l'ID utilisateur

    // --- Méthode utilitaire (privée) pour obtenir l'ID de l'utilisateur connecté ---
    // À remplacer par votre vraie logique de sécurité
    private Integer getConnectedUserId(/* Authentication authentication */) {
        /*
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
             return null; // Ou lancer une exception d'authentification
        }
        String userEmail = auth.getName();
        Membre user = membreService.findMembreByEmail(userEmail).orElse(null);
        return (user != null) ? user.getId() : null;
        */
        // --- Placeholder non sécurisé ---
        return 1; // Simule l'utilisateur avec ID 1 connecté
        // --- Fin Placeholder ---
    }


    /**
     * Envoie une demande d'ami à un autre membre.
     * L'envoyeur est l'utilisateur connecté.
     */
    @PostMapping("/demandes/envoyer/{recepteurId}")
    @JsonView(GlobalView.DemandeView.class) // Retourne la demande créée
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendFriendRequest(@PathVariable Integer recepteurId) {
        Integer envoyeurId = getConnectedUserId();
        if (envoyeurId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentification requise."));
        }

        try {
            DemandeAmi demande = demandeAmiService.sendFriendRequest(envoyeurId, recepteurId);
            return ResponseEntity.status(HttpStatus.CREATED).body(demande);
        } catch (EntityNotFoundException e) { // Receveur non trouvé
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) { // Auto-demande, déjà amis/demandé
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne lors de l'envoi de la demande."));
        }
    }

    /**
     * Accepte une demande d'ami reçue.
     * L'accepteur est l'utilisateur connecté.
     */
    @PutMapping("/demandes/accepter/{demandeId}")
    @JsonView(GlobalView.DemandeView.class) // Retourne la demande mise à jour
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable Integer demandeId) {
        Integer accepteurId = getConnectedUserId();
        if (accepteurId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentification requise."));
        }

        try {
            DemandeAmi demande = demandeAmiService.acceptFriendRequest(demandeId, accepteurId);
            return ResponseEntity.ok(demande);
        } catch (EntityNotFoundException e) { // Demande non trouvée
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) { // Pas le récepteur
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Pas en attente
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne lors de l'acceptation."));
        }
    }

    /**
     * Refuse une demande d'ami reçue.
     * Le refuseur est l'utilisateur connecté.
     */
    @PutMapping("/demandes/refuser/{demandeId}") // Ou @DeleteMapping si on supprime
    @JsonView(GlobalView.DemandeView.class) // Retourne la demande mise à jour (si statut REFUSE)
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> refuseFriendRequest(@PathVariable Integer demandeId) {
        Integer refuseurId = getConnectedUserId();
        if (refuseurId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentification requise."));
        }

        try {
            DemandeAmi demande = demandeAmiService.refuseFriendRequest(demandeId, refuseurId);
            return ResponseEntity.ok(demande); // 200 OK avec la demande marquée REFUSE
            // Si le service supprime au lieu de marquer REFUSE :
            // demandeAmiService.refuseFriendRequest(demandeId, refuseurId);
            // return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) { // Demande non trouvée
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) { // Pas le récepteur
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Pas en attente
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne lors du refus."));
        }
    }

    /**
     * Annule une demande d'ami envoyée (par l'envoyeur).
     * L'annuleur est l'utilisateur connecté.
     */
    @DeleteMapping("/demandes/annuler/{demandeId}")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelFriendRequest(@PathVariable Integer demandeId) {
        Integer annuleurId = getConnectedUserId();
        if (annuleurId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentification requise."));
        }

        try {
            demandeAmiService.cancelFriendRequest(demandeId, annuleurId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) { // Demande non trouvée
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) { // Pas l'envoyeur
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Pas en attente
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne lors de l'annulation."));
        }
    }

    /**
     * Supprime une relation d'amitié existante (demande avec statut ACCEPTE).
     * Le suppresseur est l'utilisateur connecté (qui doit être l'un des deux amis).
     *
     * @param amiId L'ID de l'ami à retirer.
     */
    @DeleteMapping("/{amiId}")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeFriend(@PathVariable Integer amiId) {
        Integer membreId = getConnectedUserId();
        if (membreId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentification requise."));
        }

        // Trouver la demande ACCEPTEE entre membreId et amiId
        List<DemandeAmi> relations = demandeAmiRepository.findAllAcceptedRequestsInvolvingUser(membreId, Statut.ACCEPTE);
        Optional<DemandeAmi> relationToRemove = relations.stream()
                .filter(d -> d.getEnvoyeur().getId().equals(amiId) || d.getRecepteur().getId().equals(amiId))
                .findFirst();

        if (relationToRemove.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Aucune relation d'amitié active trouvée avec cet utilisateur."));
        }

        try {
            // On utilise l'ID de la DemandeAmi trouvée
            demandeAmiService.removeFriendship(relationToRemove.get().getId(), membreId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException | SecurityException | IllegalStateException e) {
            // Ces erreurs ne devraient pas arriver si la logique ci-dessus est correcte, mais par sécurité :
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne lors de la suppression de l'ami."));
        }
    }


    // --- Consultation des demandes ---

    /**
     * Récupère les demandes d'ami reçues en attente pour l'utilisateur connecté.
     */
    @GetMapping("/demandes/recues")
    @JsonView(GlobalView.DemandeView.class) // Montre la demande et l'envoyeur
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPendingReceivedRequests() {
        Integer membreId = getConnectedUserId();
        if (membreId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<DemandeAmi> demandes = demandeAmiService.getPendingReceivedRequests(membreId);
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère les demandes d'ami envoyées en attente par l'utilisateur connecté.
     */
    @GetMapping("/demandes/envoyees")
    @JsonView(GlobalView.DemandeView.class) // Montre la demande et le récepteur
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPendingSentRequests() {
        Integer membreId = getConnectedUserId();
        if (membreId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<DemandeAmi> demandes = demandeAmiService.getPendingSentRequests(membreId);
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère la liste des amis (Membres) de l'utilisateur connecté.
     */
    @GetMapping("") // GET /api/amis
    @JsonView(GlobalView.Base.class) // On retourne juste les infos de base des membres amis
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFriends() {
        Integer membreId = getConnectedUserId();
        if (membreId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Membre> amis = demandeAmiService.getFriends(membreId);
        return ResponseEntity.ok(amis);
    }

}

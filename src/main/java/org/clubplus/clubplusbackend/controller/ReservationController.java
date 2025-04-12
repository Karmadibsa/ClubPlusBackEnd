package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.service.ReservationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin // À configurer précisément pour la production
public class ReservationController {

    private final ReservationService reservationService;
    // Injectez MembreService si vous devez récupérer le Membre à partir de Principal/Authentication
    // private final MembreService membreService;

    /**
     * Crée une nouvelle réservation.
     * Vérifie si le membre connecté réserve pour lui-même (sauf si admin).
     * Gère les erreurs métier du service (capacité, limite de résa, etc.).
     */
    @PostMapping
    @JsonView(GlobalView.ReservationView.class) // Vue détaillée de la réservation créée
    // @PreAuthorize("isAuthenticated()") // Au minimum, l'utilisateur doit être connecté
    public ResponseEntity<?> createReservation(
            @RequestParam Integer membreId,
            @RequestParam Integer eventId,
            @RequestParam Integer categorieId
            // --- Pour la sécurité (à décommenter et implémenter) ---
            // , Principal principal // Ou Authentication authentication
    ) {

        // --- !!! Section Sécurité Essentielle !!! ---
        // Vous DEVEZ vérifier que l'utilisateur connecté (principal/authentication)
        // correspond au membreId fourni, ou qu'il a un rôle ADMIN.
        // Exemple (nécessite MembreService et configuration Spring Security) :
        /*
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utilisateur non authentifié."));
        }
        String userEmail = authentication.getName(); // Ou récupérer l'objet UserDetails
        Membre connectedUser = membreService.findMembreByEmail(userEmail)
                                     .orElse(null); // Gérer le cas où l'utilisateur n'est pas trouvé en BDD

        if (connectedUser == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utilisateur non trouvé."));
        }

        // Vérifier si l'ID correspond OU si l'utilisateur est ADMIN
        if (!connectedUser.getId().equals(membreId) && connectedUser.getRole() != Role.ADMIN) {
              return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                   .body(Map.of("error", "Vous n'êtes pas autorisé à réserver pour ce membre."));
        }
        */
        // --- Fin Section Sécurité ---


        try {
            // Le service gère maintenant la limite de 2 réservations/membre/event
            Reservation createdReservation = reservationService.createReservation(membreId, eventId, categorieId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReservation);
        } catch (EntityNotFoundException e) { // Membre, Event ou Catégorie non trouvé
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) { // Catégorie n'appartient pas à l'événement
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Capacité atteinte, déjà réservé (limite 2), événement passé
            return ResponseEntity.status(HttpStatus.CONFLICT) // 409 Conflict est souvent approprié ici
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur création réservation...", e); // Pensez à logger les erreurs inattendues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de la création de la réservation."));
        }
    }

    /**
     * Récupère une réservation spécifique par son ID.
     * (Nécessite une vérification de droits : est-ce ma réservation ou suis-je admin ?)
     */
    @GetMapping("/{id}")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Reservation> getReservationById(@PathVariable Integer id /*, Principal principal */) {
        return reservationService.findReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère toutes les réservations d'un membre spécifique.
     * (Nécessite une vérification de droits : est-ce moi ou suis-je admin ?)
     */
    @GetMapping("/membre/{membreId}")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<?> getReservationsByMembre(@PathVariable Integer membreId /*, Principal principal */) {

        try {
            List<Reservation> reservations = reservationService.findReservationsByMembreId(membreId);
            return ResponseEntity.ok(reservations);
        } catch (EntityNotFoundException e) { // Si le membreId lui-même n'existe pas
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur get reservations membre...", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne."));
        }
    }

    @GetMapping("/event/{eventId}")
    @JsonView(GlobalView.ReservationView.class)
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwner(principal, #eventId)")
    public ResponseEntity<?> getReservationsByEvent(@PathVariable Integer eventId /*, Principal principal */) {
        // --- Sécurité : Vérifier si l'utilisateur connecté est l'organisateur ou un ADMIN ---

        try {
            // Le service lance EntityNotFoundException si l'événement n'existe pas
            List<Reservation> reservations = reservationService.findReservationsByEventId(eventId);
            return ResponseEntity.ok(reservations);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur get reservations event...", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne."));
        }
    }
    
    @GetMapping("/categorie/{categorieId}")
    @JsonView(GlobalView.ReservationView.class)
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isEventOwnerForCategorie(principal, #categorieId)")
    public ResponseEntity<?> getReservationsByCategorie(@PathVariable Integer categorieId /*, Principal principal */) {
        // --- Sécurité : Vérifier si l'utilisateur connecté est l'organisateur ou un ADMIN ---

        try {
            // Le service lance EntityNotFoundException si la catégorie n'existe pas
            List<Reservation> reservations = reservationService.findReservationsByCategorieId(categorieId);
            return ResponseEntity.ok(reservations);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur get reservations categorie...", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur interne."));
        }
    }

    /**
     * Annule (supprime) une réservation.
     * Vérifie si l'utilisateur connecté est le propriétaire de la réservation ou un admin.
     */
    @DeleteMapping("/{id}")
    // @PreAuthorize("isAuthenticated()") // Doit être connecté pour annuler
    public ResponseEntity<?> deleteReservation(@PathVariable Integer id /*, Principal principal */) { // Ou Authentication authentication

        // --- !!! Section Sécurité Essentielle !!! ---
        // Récupérer l'ID et le rôle de l'utilisateur connecté
        Integer demandeurId = null;
        boolean isAdmin = false;

        /*
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utilisateur non authentifié."));
        }
        String userEmail = authentication.getName();
        Membre connectedUser = membreService.findMembreByEmail(userEmail).orElse(null);

        if (connectedUser == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utilisateur non trouvé."));
        }

        demandeurId = connectedUser.getId();
        isAdmin = (connectedUser.getRole() == Role.ADMIN);
        */

        // --- À remplacer par la vraie logique ci-dessus ---
        if (demandeurId == null) demandeurId = 1; // Placeholder non sécurisé pour test
        // isAdmin = false; // Placeholder
        // --- Fin section à remplacer ---


        try {
            // Le service vérifie les droits avec demandeurId et isAdmin
            reservationService.deleteReservation(id, demandeurId, isAdmin);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) { // Réservation non trouvée
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) { // Pas le droit d'annuler
            return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403 Forbidden est plus approprié que 401 ici
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Annulation impossible (ex: événement passé, si activé dans le service)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error("Erreur suppression réservation {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de l'annulation de la réservation."));
        }
    }
}

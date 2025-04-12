package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@CrossOrigin
public class ClubController {

    private final ClubService clubService;
    // Injecter d'autres services si nécessaire pour les endpoints liés
    private final EventService eventService;
    private final MembreService membreService; // Supposons qu'il existe une méthode findMembresByClubId

    // GET /api/clubs - Récupérer tous les clubs (vue de base)
    @GetMapping
    @JsonView(GlobalView.Base.class)
    public List<Club> getAllClubs() {
        return clubService.findAllClubs();
    }

    // GET /api/clubs/{id} - Récupérer un club par ID (vue détaillée)
    @GetMapping("/{id}")
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> getClubById(@PathVariable Integer id) {
        return clubService.findClubById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/clubs/code/{codeClub} - Récupérer un club par son code (vue détaillée)
    @GetMapping("/code/{codeClub}")
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> getClubByCode(@PathVariable String codeClub) {
        return clubService.findClubByCode(codeClub)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/clubs - Créer un nouveau club ET son admin initial via un objet Club imbriqué
    @PostMapping
    @JsonView(GlobalView.ClubView.class) // Vue pour le retour
    public ResponseEntity<?> createClubWithNestedAdmin(@Valid @RequestBody Club clubInput) {
        // @Valid validera les contraintes sur Club (ex: @NotBlank)
        // La validation du Membre imbriqué dépendra si @Valid est sur Club.admin dans l'entité
        if (clubInput.getAdmin() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Les informations de l'administrateur sont requises dans le champ 'admin'."));
        }
        // Vérification basique que le mot de passe est fourni (la vraie validation est dans le service)
        if (clubInput.getAdmin().getPassword() == null || clubInput.getAdmin().getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Le mot de passe de l'administrateur est requis."));
        }


        try {
            Club createdClub = clubService.createClubAndAdminFromInput(clubInput);
            // Re-fetch pour être sûr d'avoir le codeClub généré dans la réponse
            Club fetchedClub = clubService.getClubByIdOrThrow(createdClub.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(fetchedClub);
        } catch (IllegalArgumentException e) { // Gère les emails dupliqués ou autres erreurs logiques
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // Ex: PasswordEncoder manquant
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Loggez l'erreur côté serveur
            // logger.error("Erreur lors de la création du club et admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Une erreur interne est survenue lors de la création du club."));
        }
    }

    // Gestionnaire d'erreurs de validation (@Valid sur Club)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            // Le fieldName peut être complexe si l'erreur vient de l'admin imbriqué (ex: "admin.email")
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    // PUT /api/clubs/{id} - Mettre à jour un club existant
    @PutMapping("/{id}")
    @JsonView(GlobalView.ClubView.class)
    // @PreAuthorize("hasRole('ADMIN') or @securityService.isClubAdmin(principal, #id)") // Seul admin du club ou super admin
    public ResponseEntity<?> updateClub(@PathVariable Integer id, @Valid @RequestBody Club clubDetails) {
        try {
            Club updatedClub = clubService.updateClub(id, clubDetails);
            return ResponseEntity.ok(updatedClub);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) { // Ex: Email déjà pris
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // DELETE /api/clubs/{id} - Supprimer un club
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('SUPER_ADMIN')") // Peut-être seul un super admin peut supprimer un club
    public ResponseEntity<?> deleteClub(@PathVariable Integer id) {
        try {
            clubService.deleteClub(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (IllegalStateException e) { // Ex: Ne peut pas supprimer car membres/événements existent
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict
        }
    }

    // --- Endpoints pour les relations ---

    // GET /api/clubs/{id}/membres - Récupérer les membres d'un club
    @GetMapping("/{id}/membres")
    @JsonView(GlobalView.Base.class) // Vue de base pour la liste des membres
    // @PreAuthorize("isAuthenticated()") // Ouvert à tous les utilisateurs connectés ? Ou seulement membres du club ?
    public ResponseEntity<List<Membre>> getClubMembres(@PathVariable Integer id) {
        try {
            Club club = clubService.getClubByIdOrThrow(id); // Valide que le club existe
            // Il faut une méthode dans MembreService pour récupérer les membres par clubId
            List<Membre> membres = membreService.findMembresByClubId(id);
            return ResponseEntity.ok(membres);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/clubs/{id}/events - Récupérer les événements organisés par un club (passés et futurs)
    @GetMapping("/{id}/events")
    @JsonView(GlobalView.Base.class) // Vue de base pour la liste des événements
    public ResponseEntity<List<Event>> getClubEvents(@PathVariable Integer id) {
        // Utilise la méthode existante dans EventService
        // Vérifie implicitement que le club existe car EventService peut lancer une exception si club non trouvé
        List<Event> events = eventService.findEventsByOrganisateur(id);
        // On pourrait aussi ajouter une vérification explicite clubService.existsById(id) avant
        return ResponseEntity.ok(events);
    }

    // GET /api/clubs/{id}/events/upcoming - Récupérer les événements futurs organisés par un club
    @GetMapping("/{id}/events/upcoming")
    @JsonView(GlobalView.Base.class)
    public ResponseEntity<List<Event>> getClubUpcomingEvents(@PathVariable Integer id) {
        // Utilise la méthode que nous avons ajoutée précédemment dans EventService
        List<Event> upcomingEvents = eventService.findUpcomingEventsByOrganisateur(id);
        // Le service gère le cas où le club n'existe pas (retourne liste vide ou lance exception)
        return ResponseEntity.ok(upcomingEvents);
    }

}

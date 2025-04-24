package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@CrossOrigin // Si nécessaire
public class ClubController {

    private final ClubService clubService;
    private final EventService eventService; // Supposé exister pour les events

    /**
     * GET /api/clubs
     * Récupère la liste de tous les clubs (infos de base).
     * Sécurité: Tout utilisateur authentifié.
     * Exceptions: Aucune attendue (sauf erreur serveur 500).
     */
    @GetMapping
    @IsMembre
    @JsonView(GlobalView.Base.class)
    public List<Club> getAllClubs() {
        return clubService.findAllClubs();
    }

    /**
     * GET /api/clubs/{id}
     * Récupère les détails d'un club par ID.
     * Sécurité: Authentifié + Membre du club (vérifié dans le service).
     * Exceptions (gérées globalement): 404 (Club non trouvé), 403 (Non membre).
     */
    @GetMapping("/{id}")
    @IsMembre
    @JsonView(GlobalView.ClubView.class)
    public Club getClubById(@PathVariable Integer id) {
        // Le service gère existence + sécurité contextuelle (membre)
        return clubService.getClubByIdWithSecurityCheck(id);
    }

    /**
     * GET /api/clubs/code/{codeClub}
     * Récupère les détails d'un club par son code unique.
     * Sécurité: Tout utilisateur authentifié.
     * Exceptions (gérées globalement): 404 (Code non trouvé).
     */
    @GetMapping("/code/{codeClub}")
    @IsMembre
    @JsonView(GlobalView.ClubView.class)
    public Club getClubByCode(@PathVariable String codeClub) {
        // Le service lance 404 si non trouvé
        return clubService.getClubByCodeOrThrow(codeClub);
    }

    /**
     * POST /api/clubs
     * Crée un nouveau club et son admin initial à partir d'un payload JSON unique.
     * Sécurité: Tout utilisateur authentifié.
     * Validation: Le DTO est validé via @Valid.
     * Exceptions (gérées globalement): 400 (Validation échouée), 409 (Emails déjà utilisés).
     */
    @PostMapping("/inscription") // Pas besoin de 'consumes' car @RequestBody suppose application/json par défaut
    @ResponseStatus(HttpStatus.CREATED)
    @JsonView(GlobalView.ClubView.class)
    public Club createClubAndAdmin(
            @Valid @RequestBody CreateClubRequestDto creationDto // Utilise @RequestBody avec le DTO
    ) {
        // @Valid gère la validation Bean du DTO (y compris le AdminInfo imbriqué) -> 400 si échec
        // Le service gère la logique et les conflits (-> 409)
        return clubService.createClubAndRegisterAdmin(creationDto); // Passe le DTO au service
    }

    /**
     * PUT /api/clubs/{id}
     * Met à jour les informations d'un club.
     * Sécurité: Authentifié + ADMIN spécifique de ce club (vérifié dans le service).
     * Exceptions (gérées globalement): 404 (Club non trouvé), 403 (Pas l'admin du club),
     * 400 (Validation @Valid échouée), 409 (Email dupliqué).
     */
    @PutMapping("/{id}")
    @IsAdmin
    @JsonView(GlobalView.ClubView.class)
    public Club updateClub(@PathVariable Integer id, @Valid @RequestBody UpdateClubDto updateDto) {
        // @Valid gère validation Bean -> 400
        // Le service gère existence (-> 404), sécurité admin (-> 403), conflits (-> 409)
        return clubService.updateClub(id, updateDto);
    }

    /**
     * DELETE /api/clubs/{id}
     * Supprime un club.
     * Sécurité: Authentifié + ADMIN spécifique de ce club (vérifié dans le service).
     * Exceptions (gérées globalement): 404 (Club non trouvé), 403 (Pas l'admin du club),
     * 409 (Événements futurs empêchent suppression).
     */
    @DeleteMapping("/{id}")
    @IsAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT) // Code 204 si succès
    public void deleteClub(@PathVariable Integer id) {
        // Le service gère existence (-> 404), sécurité admin (-> 403), conflits (-> 409)
        clubService.deactivateClub(id);
    }

    /**
     * GET /api/clubs/{id}/membres
     * Récupère la liste des membres d'un club.
     * Sécurité: Authentifié + Membre du club (vérifié dans le service).
     * Exceptions (gérées globalement): 404 (Club non trouvé), 403 (Non membre).
     */
    @GetMapping("/{id}/membres")
    @IsReservation
    @JsonView(GlobalView.Base.class) // Vue de base pour les membres listés
    public List<Membre> getClubMembres(@PathVariable Integer id) {
        // Le service gère existence (-> 404), sécurité membre (-> 403)
        Set<Membre> membresSet = clubService.findMembresForClub(id);
        return new ArrayList<>(membresSet); // Convertir Set en List pour JSON
    }

    /**
     * GET /api/clubs/{id}/admin
     * Récupère l'administrateur d'un club.
     * Sécurité: Authentifié + Membre du club (vérifié dans le service).
     * Exceptions (gérées globalement): 404 (Club ou Admin non trouvé), 403 (Non membre).
     */
    @GetMapping("/{id}/admin")
    @IsConnected
    @JsonView(GlobalView.MembreView.class) // Vue détaillée de l'admin
    public Membre getClubAdmin(@PathVariable Integer id) {
        // Le service gère existence (-> 404), sécurité membre (-> 403)
        return clubService.getAdminForClubOrThrow(id);
    }


    // --- Endpoints liés aux événements (simplifiés, dépendent d'EventService) ---
    // NOTE: La sécurité contextuelle (être membre du club) DOIT être dans EventService

    /**
     * GET /api/clubs/{id}/events
     * Récupère les événements d'un club.
     * Sécurité: Gérée par EventService (doit être membre du club).
     * Exceptions (gérées globalement): 403 (Non membre), 500 (Erreur EventService).
     */
    @GetMapping("/{id}/events")
    @IsConnected
    @JsonView(GlobalView.Base.class)
    public List<Event> getClubEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        // Supposons qu'EventService existe et a la sécurité intégrée
        return eventService.findEventsByOrganisateurWithSecurityCheck(id, status);
    }

    /**
     * GET /api/clubs/{id}/events/upcoming
     * Récupère les événements futurs d'un club.
     * Sécurité: Gérée par EventService (doit être membre du club).
     * Exceptions (gérées globalement): 403 (Non membre), 500 (Erreur EventService).
     */
    @GetMapping("/{id}/events/upcoming")
    @IsConnected
    @JsonView(GlobalView.Base.class)
    public List<Event> getClubUpcomingEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        // Supposons qu'EventService existe et a la sécurité intégrée
        return eventService.findUpcomingEventsByOrganisateurWithSecurityCheck(id, status);
    }

    // Le @ExceptionHandler(MethodArgumentNotValidException.class) a été retiré
    // et doit être dans GlobalExceptionHandler.
}

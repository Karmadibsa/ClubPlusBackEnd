package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
@CrossOrigin
public class MembreController {

    private final MembreService membreService;
    private final SecurityService securityService;
    // Pas besoin de SecurityService ici, géré dans MembreService

    /**
     * GET /api/membres/profile
     * Récupère le profil de l'utilisateur COURANT.
     * Sécurité: Utilisateur authentifié.
     * Exceptions (globales): 401/500 (User non trouvé dans SecurityContext).
     */
    @GetMapping("/profile")
    @IsConnected
    @JsonView(GlobalView.MembreView.class)
    public Membre getMyProfile() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Nécessite SecurityService injecté ou via MembreService
        return membreService.getMembreByIdWithSecurityCheck(currentUserId); // Vérif owner implicite
    }


    /**
     * GET /api/membres/{id} (Potentiellement pour Admins Globaux ?)
     * Récupère les détails d'un membre spécifique par ID.
     * Sécurité: Authentifié (Vérification Owner/Admin Global dans le service).
     * Exceptions (globales): 404 (Non trouvé), 403 (Accès refusé).
     */
    @GetMapping("/{id}")
    @IsConnected
    @JsonView(GlobalView.MembreView.class)
    public Membre getMembreById(@PathVariable Integer id) {
        return membreService.getMembreByIdWithSecurityCheck(id);
    }

    /**
     * PUT /api/membres/profile
     * Met à jour le profil de l'utilisateur COURANT.
     * Sécurité: Utilisateur authentifié (propriétaire vérifié dans service).
     * Exceptions (globales): 400 (Validation), 409 (Email dupliqué).
     */
    @PutMapping("/profile")
    @IsConnected
    @JsonView(GlobalView.MembreView.class)
    public Membre updateMyProfile(@Valid @RequestBody Membre membreDetails) {
        // Le service utilisera l'ID de l'utilisateur courant
        return membreService.updateMyProfile(membreDetails);
    }

    /**
     * DELETE /api/membres/profile
     * Supprime le compte de l'utilisateur COURANT.
     * Sécurité: Utilisateur authentifié (propriétaire vérifié dans service).
     * Exceptions (globales): 409 (Est ADMIN d'un club).
     */
    @DeleteMapping("/profile")
    @IsConnected
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteMyAccount() {
        // Le service utilise l'ID de l'utilisateur courant
        membreService.deleteMyAccount();
    }

    /**
     * GET /api/membres/profile/clubs
     * Récupère les clubs de l'utilisateur COURANT.
     * Sécurité: Utilisateur authentifié.
     */
    @GetMapping("/profile/clubs")
    @IsConnected
    @JsonView(GlobalView.Base.class)
    public Set<Club> getMyClubs() {
        return membreService.findClubsForCurrentUser();
    }

    /**
     * POST /api/membres/profile/join?codeClub={codeClub}
     * Permet à l'utilisateur COURANT de rejoindre un club.
     * Sécurité: Utilisateur authentifié. Rôle vérifié dans service.
     * Exceptions (globales): 404 (Club non trouvé), 409 (Déjà membre / Rôle interdit).
     */
    @PostMapping("/profile/join")
    @IsConnected
    @ResponseStatus(HttpStatus.CREATED) // 201
    @JsonView(GlobalView.Base.class) // Retourner juste l'adhésion ou le club ?
    public Adhesion joinClub(@RequestParam String codeClub) { // Retourne l'adhésion créée
        return membreService.joinClub(codeClub);
    }

    /**
     * DELETE /api/membres/profile/leave/{clubId}
     * Permet à l'utilisateur COURANT de quitter un club.
     * Sécurité: Utilisateur authentifié. Rôle vérifié dans service.
     * Exceptions (globales): 404 (Pas membre), 409 (Rôle interdit).
     */
    @DeleteMapping("/profile/leave/{clubId}")
    @IsConnected
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void leaveClub(@PathVariable Integer clubId) {
        membreService.leaveClub(clubId);
    }

    // --- Endpoint pour Admin de Club (Gestion Rôles) ---

    /**
     * PUT /api/membres/{membreId}/role?clubId={clubId}
     * Change le rôle (MEMBRE <-> RESERVATION) d'un membre dans un club.
     * Sécurité: Rôle ADMIN (@IsAdmin). Vérification ADMIN DU CLUB dans le service.
     * Exceptions (globales): 404, 403, 400, 409.
     */
    @PutMapping("/{membreId}/role")
    @IsAdmin // L'appelant doit avoir le rôle ADMIN global (à affiner si besoin)
    @JsonView(GlobalView.MembreView.class)
    public Membre changeMemberRoleInClub(@PathVariable Integer membreId,
                                         @RequestParam Integer clubId,
                                         @RequestParam Role newRole) { // Reçoit Role.MEMBRE ou Role.RESERVATION
        return membreService.changeMemberRoleInClub(membreId, clubId, newRole);
    }

    // Le @ExceptionHandler pour MethodArgumentNotValidException doit être dans GlobalExceptionHandler.

    // Endpoint GET /api/membres désactivé par PreAuthorize("false") - OK
    @GetMapping
    @PreAuthorize("false") // Désactive cet endpoint
    @JsonView(GlobalView.Base.class)
    public List<Membre> getAllMembres() {
        throw new UnsupportedOperationException("Lister tous les membres n'est pas autorisé.");
    }
}


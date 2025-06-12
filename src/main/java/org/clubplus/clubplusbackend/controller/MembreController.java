package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST pour la gestion des membres (profils, adhésions, rôles).
 * <p>
 * Base URL: /membres
 * </p>
 */
@RestController
@RequestMapping("/membres")
@RequiredArgsConstructor
@CrossOrigin
public class MembreController {

    private final MembreService membreService;
    private final SecurityService securityService;

    /**
     * Récupère le profil complet de l'utilisateur authentifié.
     * <p>
     * Endpoint: GET /membres/profile
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @return Le profil de l'utilisateur (200 OK).
     */
    @GetMapping("/profile")
    @IsConnected
    @JsonView(GlobalView.ProfilView.class)
    public ResponseEntity<Membre> getMyProfile() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre profile = membreService.getMembreByIdWithSecurityCheck(currentUserId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Récupère les détails d'un membre par son ID.
     * <p>
     * Endpoint: GET /membres/{id}
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN)
     * qui partagent un club avec le membre cible.
     *
     * @param id L'ID du membre à récupérer.
     * @return Le membre trouvé (200 OK).
     */
    @GetMapping("/{id}")
    @IsReservation
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> getMembreById(@PathVariable Integer id) {
        Membre membre = membreService.getMembreByIdWithSecurityCheck(id);
        return ResponseEntity.ok(membre);
    }

    /**
     * Met à jour le profil de l'utilisateur authentifié.
     * <p>
     * Endpoint: PUT /membres/profile
     * <p>
     * Accès réservé à l'utilisateur authentifié pour son propre profil.
     *
     * @param updateMembreDto DTO contenant les informations à mettre à jour.
     * @return Le profil mis à jour (200 OK).
     */
    @PutMapping("/profile")
    @IsConnected
    @JsonView(GlobalView.ProfilView.class)
    public ResponseEntity<Membre> updateMyProfile(@Valid @RequestBody UpdateMembreDto updateMembreDto) {
        Membre updatedProfile = membreService.updateMyProfile(updateMembreDto);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Désactive et anonymise le compte de l'utilisateur authentifié.
     * <p>
     * Endpoint: DELETE /membres/profile
     * <p>
     * Accès réservé à l'utilisateur authentifié. Impossible si l'utilisateur est ADMIN d'un club.
     *
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/profile")
    @IsConnected
    public ResponseEntity<Void> deleteMyAccount() {
        membreService.deleteMyAccount();
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les clubs de l'utilisateur authentifié.
     * <p>
     * Endpoint: GET /membres/profile/clubs
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @return La liste des clubs de l'utilisateur (200 OK).
     */
    @GetMapping("/profile/clubs")
    @IsMembre
    @JsonView(GlobalView.ClubMembreView.class)
    public ResponseEntity<Set<Club>> getMyClubs() {
        Set<Club> clubs = membreService.findClubsForCurrentUser();
        return ResponseEntity.ok(clubs);
    }

    /**
     * Permet à un membre de rejoindre un club via son code.
     * <p>
     * Endpoint: POST /membres/profile/join?codeClub={codeClub}
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @param codeClub Le code unique du club à rejoindre.
     * @return La nouvelle adhésion (201 Created).
     */
    @PostMapping("/profile/join")
    @IsConnected
    @JsonView(GlobalView.Base.class)
    public ResponseEntity<Adhesion> joinClub(@RequestParam String codeClub) {
        Adhesion adhesion = membreService.joinClub(codeClub);
        return ResponseEntity.status(HttpStatus.CREATED).body(adhesion);
    }

    /**
     * Permet à un membre de quitter un club.
     * <p>
     * Endpoint: DELETE /membres/profile/leave/{clubId}
     * <p>
     * Accès réservé à l'utilisateur authentifié. Impossible s'il a un rôle de gestion dans le club.
     *
     * @param clubId L'ID du club à quitter.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/profile/leave/{clubId}")
    @IsConnected
    public ResponseEntity<Void> leaveClub(@PathVariable Integer clubId) {
        membreService.leaveClub(clubId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Modifie le rôle d'un membre au sein d'un club.
     * <p>
     * Endpoint: PUT /membres/{membreId}/role?clubId={clubId}&amp;newRole={newRole}
     * <p>
     * Accès réservé à l'administrateur (ADMIN) du club concerné.
     *
     * @param membreId L'ID du membre à modifier.
     * @param clubId   L'ID du club.
     * @param newRole  Le nouveau rôle (MEMBRE ou RESERVATION).
     * @return Le membre avec son rôle mis à jour (200 OK).
     */
    @PutMapping("/{membreId}/role")
    @IsAdmin
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> changeMemberRoleInClub(@PathVariable Integer membreId,
                                                         @RequestParam Integer clubId,
                                                         @RequestParam Role newRole) {
        Membre updatedMembre = membreService.changeMemberRoleInClub(membreId, clubId, newRole);
        return ResponseEntity.ok(updatedMembre);
    }

    /**
     * Récupère les 5 derniers membres inscrits dans le club géré par l'utilisateur.
     * <p>
     * Endpoint: GET /membres/managed-club/latest
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN).
     *
     * @return La liste des 5 derniers membres (200 OK).
     */
    @GetMapping("/managed-club/latest")
    @IsReservation
    @JsonView(GlobalView.ProfilView.class)
    public ResponseEntity<List<Membre>> getLatestMembersForMyClub() {
        List<Membre> latestMembers = membreService.getLatestMembersForManagedClub();
        return ResponseEntity.ok(latestMembers);
    }
}

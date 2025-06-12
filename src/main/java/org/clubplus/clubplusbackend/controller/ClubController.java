package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST pour la gestion des clubs, de leurs membres et de leurs événements.
 * <p>
 * Base URL: /clubs
 * </p>
 */
@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
@CrossOrigin
public class ClubController {

    private final ClubService clubService;
    private final EventService eventService;

    /**
     * Récupère les détails d'un club par son ID.
     * <p>
     * Endpoint: GET /clubs/{id}
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN)
     * et qui sont membres du club concerné.
     *
     * @param id L'ID du club à récupérer.
     * @return Le club trouvé (200 OK).
     */
    @GetMapping("/{id}")
    @IsReservation
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> getClubById(@PathVariable Integer id) {
        Club club = clubService.getClubByIdWithSecurityCheck(id);
        return ResponseEntity.ok(club);
    }


    /**
     * Met à jour les informations d'un club.
     * <p>
     * Endpoint: PUT /clubs/{id}
     * <p>
     * Accès réservé à l'administrateur du club concerné.
     *
     * @param id        L'ID du club à mettre à jour.
     * @param updateDto Le DTO contenant les informations à mettre à jour.
     * @return Le club mis à jour (200 OK).
     */
    @PutMapping("/{id}")
    @IsAdmin
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> updateClub(@PathVariable Integer id, @Valid @RequestBody UpdateClubDto updateDto) {
        Club updatedClub = clubService.updateClub(id, updateDto);
        return ResponseEntity.ok(updatedClub);
    }

    /**
     * Désactive un club (suppression logique).
     * <p>
     * Endpoint: DELETE /clubs/{id}
     * <p>
     * Accès réservé à l'administrateur du club. La suppression peut être bloquée
     * s'il reste des événements futurs actifs.
     *
     * @param id L'ID du club à désactiver.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @DeleteMapping("/{id}")
    @IsAdmin
    public ResponseEntity<Void> deleteClub(@PathVariable Integer id) {
        clubService.deactivateClub(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère la liste des membres d'un club.
     * <p>
     * Endpoint: GET /clubs/{id}/membres
     * <p>
     * Accès réservé aux utilisateurs avec un rôle de gestion (RESERVATION ou ADMIN)
     * et qui sont membres du club.
     *
     * @param id L'ID du club.
     * @return La liste des membres (200 OK).
     */
    @GetMapping("/{id}/membres")
    @IsReservation
    @JsonView(GlobalView.ProfilView.class)
    public ResponseEntity<List<Membre>> getClubMembres(@PathVariable Integer id) {
        Set<Membre> membresSet = clubService.findMembresForClub(id);
        List<Membre> membresList = new ArrayList<>(membresSet);
        return ResponseEntity.ok(membresList);
    }

    /**
     * Récupère les événements d'un club, avec un filtre optionnel sur le statut.
     * <p>
     * Endpoint: GET /clubs/{id}/events
     * <p>
     * Accès réservé aux utilisateurs connectés et membres du club.
     *
     * @param id     L'ID du club organisateur.
     * @param status (Optionnel) Filtre sur le statut des événements ('active', 'inactive', 'all').
     * @return La liste des événements (200 OK).
     */
    @GetMapping("/{id}/events")
    @IsConnected
    @JsonView(GlobalView.Base.class)
    public ResponseEntity<List<Event>> getClubEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        List<Event> events = eventService.findEventsByOrganisateurWithSecurityCheck(id, status);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupère les événements futurs d'un club, avec un filtre optionnel sur le statut.
     * <p>
     * Endpoint: GET /clubs/{id}/events/upcoming
     * <p>
     * Accès réservé aux utilisateurs connectés et membres du club.
     *
     * @param id     L'ID du club organisateur.
     * @param status (Optionnel) Filtre sur le statut ('active', 'inactive', 'all').
     * @return La liste des événements futurs (200 OK).
     */
    @GetMapping("/{id}/events/upcoming")
    @IsConnected
    @JsonView(GlobalView.Base.class)
    public ResponseEntity<List<Event>> getClubUpcomingEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        List<Event> upcomingEvents = eventService.findUpcomingEventsByOrganisateurWithSecurityCheck(id, status);
        return ResponseEntity.ok(upcomingEvents);
    }
}

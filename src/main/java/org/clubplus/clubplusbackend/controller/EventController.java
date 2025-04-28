package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateEventWithCategoriesDto;
import org.clubplus.clubplusbackend.dto.UpdateEventWithCategoriesDto;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin
public class EventController {

    private final EventService eventService;

    //    /**
//     * GET /api/events
//     * Récupère tous les événements (pas de filtre sécurité ici, peut être ajusté).
//     * Sécurité: Authentifié.
//     */
    @GetMapping
    @IsConnected // Garder: on a besoin de savoir QUI est connecté
    @JsonView(GlobalView.EventView.class)
    public List<Event> getAllEventsForMyClubs( // Renommer la méthode pour la clarté est une bonne idée
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) boolean filtre
    ) {
        System.out.println("Filtre Axel  :" + filtre);
        if (filtre) {
            // Le service doit maintenant récupérer l'utilisateur courant
            return eventService.findAllEventsForMemberClubs(status); // Appeler une nouvelle méthode de service ou une version modifiée
        } else {
            return eventService.findAllEventsForMemberClubs(status); // Appeler une nouvelle méthode de service ou une version modifiée
        }
    }
//
//    /**
//     * GET /api/events
//     * Récupère tous les événements (pas de filtre sécurité ici, peut être ajusté).
//     * Sécurité: Authentifié.
//     */
//    @GetMapping
//    @IsConnected // Garder: on a besoin de savoir QUI est connecté
//    @JsonView(GlobalView.EventView.class)
//    public ResponseEntity<Page<Event>> getAllEventsForMyClubs( // Renommer la méthode pour la clarté est une bonne idée
//                                                               @RequestParam(defaultValue = "0") int page,
//                                                               @RequestParam(defaultValue = "10") int size,
//                                                               @RequestParam(defaultValue = "name") String sortBy,
//                                                               @RequestParam(defaultValue = "asc") String sortOrder) {
//        return ResponseEntity.ok(eventService.getAllEvents(page, size, sortBy, sortOrder));
//    }
//
//    @GetMapping("/filter")
//    @IsConnected // Garder: on a besoin de savoir QUI est connecté
//    @JsonView(GlobalView.EventView.class)
//    public ResponseEntity<Page<Event>> getAllEventsForMyClubsWithFiltre( // Renommer la méthode pour la clarté est une bonne idée
//                                                                         @RequestParam Integer organisateurId,
//                                                                         @RequestParam LocalDateTime dateStart,
//                                                                         @RequestParam LocalDateTime dateEnd,
//                                                                         @RequestParam(defaultValue = "0") int page,
//                                                                         @RequestParam(defaultValue = "10") int size,
//                                                                         @RequestParam(defaultValue = "name") String sortBy,
//                                                                         @RequestParam(defaultValue = "asc") String sortOrder) {
//        return ResponseEntity.ok(eventService.findByOrganisateurIdAndDate(organisateurId, dateStart, dateEnd, page, size, sortBy, sortOrder));
//    }

    /**
     * GET /api/events/{id}
     * Récupère un événement par ID.
     * Sécurité: Authentifié + Membre du club organisateur (vérifié dans service).
     * Exceptions (globales): 404 (Non trouvé), 403 (Non membre).
     */
    @GetMapping("/{id}")
    @IsConnected // L'utilisateur doit être connecté
    @JsonView(GlobalView.EventView.class)
    public Event getEventById(@PathVariable Integer id) {
        // Le service lance 404 ou 403 si nécessaire
        return eventService.getEventByIdWithSecurityCheck(id);
    }


    /**
     * GET /api/events/my-clubs/upcoming
     * Récupère les événements futurs des clubs de l'utilisateur courant.
     * Sécurité: Authentifié.
     * Exceptions (globales): 401 (Si SecurityService ne trouve pas l'user).
     */
    @GetMapping("/my-clubs/upcoming")
    @IsConnected
    @JsonView(GlobalView.EventView.class)
    public List<Event> getMyClubsUpcomingEvents(@RequestParam(required = false) String status) {
        // Le service utilise SecurityService pour trouver l'utilisateur et ses clubs
        return eventService.findUpcomingEventsForMemberClubs(status);
    }

//    /**
//     * POST /api/events?organisateurId={clubId}
//     * Crée un nouvel événement pour un club.
//     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
//     * Validation: Données de l'événement validées (@Valid).
//     * Exceptions (globales): 404 (Club non trouvé), 403 (Non manager), 400 (Validation ou dates invalides).
//     */
//    @PostMapping
//    @IsReservation // Rôle minimum
//    @ResponseStatus(HttpStatus.CREATED) // 201
//    @JsonView(GlobalView.EventView.class)
//    public Event createEvent(@RequestParam Integer organisateurId, @Valid @RequestBody CreateEventDto eventDto) {
//        // Le service gère la création, la sécurité contextuelle, et les erreurs métier
//        return eventService.createEvent(organisateurId, eventDto);
//    }

//    /**
//     * PUT /api/events/{id}
//     * Met à jour un événement.
//     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
//     * Validation: Données de l'événement validées (@Valid).
//     * Exceptions (globales): 404 (Event non trouvé), 403 (Non manager), 400 (Validation ou dates invalides).
//     */
//    @PutMapping("/{id}")
//    @IsReservation // Rôle minimum
//    @JsonView(GlobalView.EventView.class)
//    public Event updateEvent(@PathVariable Integer id, @Valid @RequestBody UpdateEventDto eventDto) {
//        // Le service gère la mise à jour, la sécurité contextuelle, et les erreurs métier
//        return eventService.updateEvent(id, eventDto);
//    }

    /**
     * DELETE /api/events/{id}
     * Supprime un événement.
     * Sécurité: Rôle RESERVATION/ADMIN requis (@IsReservation). Manager du club vérifié dans service.
     * Exceptions (globales): 404 (Event non trouvé), 403 (Non manager), 409 (Réservations existent).
     */
    @DeleteMapping("/{id}")
    @IsReservation // Rôle minimum
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteEvent(@PathVariable Integer id) {
        // Le service gère la suppression, la sécurité contextuelle, et les erreurs métier
        eventService.deactivateEvent(id);
    }

    /**
     * GET /api/events/next
     * Récupère les 5 prochains événements actifs du club géré par l’utilisateur connecté.
     *
     * @return Liste des 5 prochains événements.
     */
    @GetMapping("/clubs/next-event")
    @IsReservation
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<Event>> getNextFiveEventsForMyClub() {
        List<Event> nextEvents = eventService.getNextFiveEventsForManagedClub();
        return ResponseEntity.ok(nextEvents);
    }
    // Le @ExceptionHandler pour MethodArgumentNotValidException doit être dans GlobalExceptionHandler.

    /**
     * POST /api/events?organisateurId={clubId}
     * Crée un nouvel événement AVEC ses catégories initiales.
     * Sécurité: Rôle RESERVATION/ADMIN requis. Manager du club vérifié dans service.
     */
    @PostMapping // Utilise le même chemin POST standard
    @IsReservation // Rôle minimum
    @ResponseStatus(HttpStatus.CREATED)
    @JsonView(GlobalView.EventView.class)
    // Changer le type du @RequestBody pour utiliser le nouveau DTO
    public Event createEventWithCategories(@RequestParam Integer organisateurId,
                                           @Valid @RequestBody CreateEventWithCategoriesDto eventDto) {
        // Appeler la nouvelle méthode du service
        return eventService.createEventWithCategories(organisateurId, eventDto);
    }


    /**
     * PUT /api/events/{id}/full
     * Met à jour un événement ET réconcilie ses catégories (ajout/modif/suppression).
     * Sécurité: Rôle RESERVATION/ADMIN requis. Manager du club vérifié dans service.
     */
    @PutMapping("/{id}/full") // Nouveau chemin spécifique
    @IsReservation // Rôle minimum
    @JsonView(GlobalView.EventView.class)
    public Event updateEventWithCategories(@PathVariable Integer id,
                                           @Valid @RequestBody UpdateEventWithCategoriesDto eventDto) {
        // Appeler la nouvelle méthode du service
        return eventService.updateEventWithCategories(id, eventDto);
    }
}

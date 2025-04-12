package org.clubplus.clubplusbackend.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.service.StatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats") // Chemin de base pour les statistiques
@RequiredArgsConstructor
@CrossOrigin
public class StatsController {

    private final StatsService statisticsService;

    /**
     * Récupère le taux d'occupation des événements.
     * Retourne une liste de Maps: [{"eventId": 1, "eventName": "Tournoi A", "occupancyRate": 75.5}, ...]
     */
    @GetMapping("/events/occupancy")
    public ResponseEntity<List<Map<String, Object>>> getEventOccupancy() {
        try {
            List<Map<String, Object>> occupancyData = statisticsService.getEventOccupancyRates();
            return ResponseEntity.ok(occupancyData);
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", "Erreur interne lors du calcul des taux d'occupation.")));
        }
    }

    /**
     * Récupère le nombre d'événements prévus dans les 30 prochains jours.
     * Retourne un JSON simple: {"count": 12}
     */
    @GetMapping("/events/upcoming-count-30d")
    public ResponseEntity<Map<String, Long>> getUpcomingEventCount() {
        try {
            long count = statisticsService.countUpcomingEventsNext30Days();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", -1L)); // Retourne une map avec une valeur d'erreur
        }
    }

    /**
     * Récupère les moyennes des notes pour un événement spécifique.
     * Retourne une Map: {"ambiance": 4.2, "proprete": 3.8, ..., "moyenneGenerale": 4.05}
     */
    @GetMapping("/events/{eventId}/ratings")
    public ResponseEntity<?> getEventAverageRatings(@PathVariable Integer eventId) {
        try {
            Map<String, Double> averages = statisticsService.getAverageRatingsForEvent(eventId);
            return ResponseEntity.ok(averages);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors du calcul des moyennes."));
        }
    }

    /**
     * Récupère les données pour le graphique des inscriptions mensuelles sur 12 mois.
     * Retourne une liste de Maps: [{"monthYear": "2024-05", "count": 5}, ...]
     */
    @GetMapping("/members/registrations-monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRegistrations() {
        try {
            List<Map<String, Object>> registrationData = statisticsService.getMonthlyRegistrationsLast12Months();
            return ResponseEntity.ok(registrationData);
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", "Erreur interne lors de la récupération des inscriptions.")));
        }
    }

    /**
     * Récupère le nombre total de membres pour un club spécifique.
     * Retourne un JSON simple: {"totalMembers": 250}
     */
    @GetMapping("/clubs/{clubId}/total-members")
    public ResponseEntity<?> getTotalMembers(@PathVariable Integer clubId) {
        try {
            long total = statisticsService.getTotalMembersForClub(clubId);
            return ResponseEntity.ok(Map.of("totalMembers", total));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // logger.error(...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", -1L)); // Retourne une map avec une valeur d'erreur
        }
    }

    /**
     * Récupère le nombre moyen d'amis par membre.
     * Retourne un JSON simple: {"averageFriends": 3.75}
     */
    @GetMapping("/members/average-friends")
    public ResponseEntity<Map<String, Object>> getAverageFriends() { // Changé le type de retour pour l'erreur
        try {
            double average = statisticsService.getAverageFriendsPerMember();
            return ResponseEntity.ok(Map.of("averageFriends", average));
        } catch (Exception e) {
            // logger.error("Erreur calcul moyenne amis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne"));
        }
    }

    /**
     * Récupère les 5 événements passés les mieux notés.
     * Possibilité d'ajuster le nombre via le paramètre 'limit'.
     * Retourne une liste de Maps: [{"eventId": 10, "eventName": "Super Tournoi", "overallAverageRating": 4.8}, ...]
     */
    @GetMapping("/events/top-rated")
    public ResponseEntity<List<Map<String, Object>>> getTopRatedEvents(
            @RequestParam(defaultValue = "5") int limit // Paramètre optionnel avec valeur par défaut 5
    ) {
        if (limit <= 0) {
            return ResponseEntity.badRequest().body(Collections.singletonList(Map.of("error", "La limite doit être supérieure à zéro.")));
        }
        try {
            List<Map<String, Object>> topEvents = statisticsService.getTopRatedEvents(limit);
            return ResponseEntity.ok(topEvents);
        } catch (Exception e) {
            // logger.error("Erreur get top rated events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(Map.of("error", "Erreur interne lors de la récupération des événements les mieux notés.")));
        }
    }
}

package org.clubplus.clubplusbackend.controller;

// Retrait des imports non nécessaires (ResponseEntity, HttpStatus, EntityNotFoundException)

import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.service.StatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller pour récupérer des statistiques SPÉCIFIQUES À UN CLUB.
 * L'accès est restreint aux gestionnaires (RESERVATION/ADMIN) du club via @PreAuthorize.
 */
@RestController
@RequestMapping("/api/stats/clubs/{clubId}") // Base URL inclut clubId
@RequiredArgsConstructor
@CrossOrigin
public class StatsController {

    private final StatsService statsService;
    // Pas besoin de SecurityService ici, utilisé dans @PreAuthorize ou dans StatsService

    // Annotation de sécurité unique pour toute la classe si toutes les méthodes ont la même exigence
    // Utilise la méthode isManagerOfClub de SecurityService via SpEL (#clubId fait référence à la variable de chemin)
    @PreAuthorize("@securityService.isManagerOfClub(#clubId)") // Applique à toutes les méthodes
    // Note: Assurez-vous que SecurityService est un bean (@Component/@Service) et que la méthode
    // isManagerOfClub(Integer clubId) existe et fonctionne correctement (elle retourne boolean).
    // Si la méthode isManagerOfClub lance AccessDeniedException, il faut utiliser
    // @securityService.checkManagerOfClubOrThrow(#clubId) mais @PreAuthorize attend un booléen.
    // On garde donc isManagerOfClub() qui retourne boolean.

    /**
     * STAT 1: GET /api/stats/clubs/{clubId}/registrations-monthly
     * Inscriptions mensuelles (12 derniers mois).
     * Sécurité: Vérifiée par @PreAuthorize sur la classe.
     * Exceptions (globales): 404 (Club non trouvé), 403 (Pas manager).
     */
    @GetMapping("/registrations-monthly")
    public List<Map<String, Object>> getClubMonthlyRegistrations(@PathVariable Integer clubId) {
        // Le service lance 404 si club non trouvé
        return statsService.getClubMonthlyRegistrations(clubId);
    }

    /**
     * STAT 2: GET /api/stats/clubs/{clubId}/average-event-ratings
     * Moyennes des notes des événements passés.
     * Sécurité: Vérifiée par @PreAuthorize sur la classe.
     * Exceptions (globales): 404 (Club non trouvé), 403 (Pas manager).
     */
    @GetMapping("/average-event-ratings")
    public Map<String, Double> getClubAverageEventRatings(@PathVariable Integer clubId) {
        // Le service lance 404 si club non trouvé
        return statsService.getClubAverageEventRatings(clubId);
    }

    /**
     * STAT 3: GET /api/stats/clubs/{clubId}/total-events
     * Nombre total d'événements.
     * Sécurité: Vérifiée par @PreAuthorize sur la classe.
     * Exceptions (globales): 404 (Club non trouvé), 403 (Pas manager).
     */
    @GetMapping("/total-events")
    public Map<String, Long> getTotalEventsForClub(@PathVariable Integer clubId) {
        // Le service lance 404 si club non trouvé
        long total = statsService.getTotalEventsForClub(clubId);
        return Map.of("totalEvents", total); // Retourne directement une map
    }

    /**
     * STAT 4: GET /api/stats/clubs/{clubId}/average-event-occupancy
     * Taux d'occupation moyen des événements.
     * Sécurité: Vérifiée par @PreAuthorize sur la classe.
     * Exceptions (globales): 404 (Club non trouvé), 403 (Pas manager).
     */
    @GetMapping("/average-event-occupancy")
    public Map<String, Double> getClubAverageEventOccupancy(@PathVariable Integer clubId) {
        // Le service lance 404 si club non trouvé
        double averageRate = statsService.getClubAverageEventOccupancy(clubId);
        return Map.of("averageOccupancyRate", averageRate);
    }

    /**
     * STAT 5: GET /api/stats/clubs/{clubId}/upcoming-event-count-30d
     * Nombre d'événements à venir (30j).
     * Sécurité: Vérifiée par @PreAuthorize sur la classe.
     * Exceptions (globales): 404 (Club non trouvé), 403 (Pas manager).
     */
    @GetMapping("/upcoming-event-count-30d")
    public Map<String, Long> getClubUpcomingEventCount30d(@PathVariable Integer clubId) {
        // Le service lance 404 si club non trouvé
        long count = statsService.getClubUpcomingEventCount30d(clubId);
        return Map.of("count", count);
    }

    /**
     * STAT 6: GET /api/stats/clubs/{clubId}/total-members
     * Nombre total de membres du club.
     * Sécurité: Vérifiée par @PreAuthorize sur la classe.
     * Exceptions (globales): 404 (Club non trouvé), 403 (Pas manager).
     */
    @GetMapping("/total-members")
    public Map<String, Long> getTotalMembers(@PathVariable Integer clubId) {
        // Le service lance 404 si club non trouvé
        long total = statsService.getTotalMembersForClub(clubId);
        return Map.of("totalMembers", total);
    }

    // Pas de @ExceptionHandler ici, doit être dans GlobalExceptionHandler.
}

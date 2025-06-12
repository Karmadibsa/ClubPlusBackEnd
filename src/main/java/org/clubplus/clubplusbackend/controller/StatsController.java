package org.clubplus.clubplusbackend.controller;

import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.DashboardSummaryDto;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la récupération de statistiques spécifiques à un club.
 * <p>
 * Base URL: /stats/clubs/{clubId}
 * <p>
 * L'accès à tous les endpoints requiert un rôle de gestion (RESERVATION ou ADMIN)
 * pour le club concerné.
 */
@RestController
@RequestMapping("/stats/clubs/{clubId}")
@RequiredArgsConstructor
@CrossOrigin
@IsReservation
public class StatsController {

    private final StatsService statsService;

    /**
     * Récupère le nombre d'inscriptions mensuelles pour le club sur les 12 derniers mois.
     * <p>
     * Endpoint: GET /stats/clubs/{clubId}/registrations-monthly
     *
     * @param clubId L'ID du club.
     * @return Une liste de maps, chaque map représentant un mois et son nombre d'inscriptions (200 OK).
     */
    @GetMapping("/registrations-monthly")
    public ResponseEntity<List<Map<String, Object>>> getClubMonthlyRegistrations(@PathVariable Integer clubId) {
        List<Map<String, Object>> registrations = statsService.getClubMonthlyRegistrations(clubId);
        return ResponseEntity.ok(registrations);
    }

    /**
     * Récupère les notes moyennes des événements passés du club.
     * <p>
     * Endpoint: GET /stats/clubs/{clubId}/average-event-ratings
     *
     * @param clubId L'ID du club.
     * @return Une map contenant les notes moyennes par critère et une moyenne générale (200 OK).
     */
    @GetMapping("/average-event-ratings")
    public ResponseEntity<Map<String, Double>> getClubAverageEventRatings(@PathVariable Integer clubId) {
        Map<String, Double> ratings = statsService.getClubAverageEventRatings(clubId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Récupère le nombre total d'événements actifs pour le club.
     * <p>
     * Endpoint: GET /stats/clubs/{clubId}/total-events
     *
     * @param clubId L'ID du club.
     * @return Une map avec le nombre total d'événements (200 OK).
     */
    @GetMapping("/total-events")
    public ResponseEntity<Map<String, Long>> getTotalEventsForClub(@PathVariable Integer clubId) {
        long total = statsService.getTotalEventsForClub(clubId);
        Map<String, Long> result = Map.of("totalEvents", total);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère le taux d'occupation moyen des événements actifs du club.
     * <p>
     * Endpoint: GET /stats/clubs/{clubId}/average-event-occupancy
     *
     * @param clubId L'ID du club.
     * @return Une map avec le taux d'occupation moyen (200 OK).
     */
    @GetMapping("/average-event-occupancy")
    public ResponseEntity<Map<String, Double>> getClubAverageEventOccupancy(@PathVariable Integer clubId) {
        double averageRate = statsService.getClubAverageEventOccupancy(clubId);
        Map<String, Double> result = Map.of("averageOccupancyRate", averageRate);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère le nombre d'événements actifs prévus dans les 30 prochains jours pour le club.
     * <p>
     * Endpoint: GET /stats/clubs/{clubId}/upcoming-event-count-30d
     *
     * @param clubId L'ID du club.
     * @return Une map avec le nombre d'événements à venir (200 OK).
     */
    @GetMapping("/upcoming-event-count-30d")
    public ResponseEntity<Map<String, Long>> getClubUpcomingEventCount30d(@PathVariable Integer clubId) {
        long count = statsService.getClubUpcomingEventCount30d(clubId);
        Map<String, Long> result = Map.of("count", count);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère un résumé complet des statistiques pour le tableau de bord du club.
     * <p>
     * Endpoint: GET /stats/clubs/{clubId}/dashboard-summary
     *
     * @param clubId L'ID du club.
     * @return Un DTO contenant le résumé des statistiques (200 OK).
     */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@PathVariable Integer clubId) {
        DashboardSummaryDto summary = statsService.getDashboardSummary(clubId);
        return ResponseEntity.ok(summary);
    }
}

package org.clubplus.clubplusbackend.controller;

// Imports nécessaires

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.DashboardSummaryDto;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST dédié à la récupération de statistiques spécifiques à un club donné.
 * Tous les endpoints de ce contrôleur nécessitent que l'utilisateur soit authentifié
 * et ait au moins le rôle RESERVATION. De plus, le service sous-jacent vérifie
 * que l'utilisateur est bien un gestionnaire (RESERVATION ou ADMIN) du club
 * spécifié dans le chemin de l'URL ({clubId}).
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /stats/clubs/{clubId}} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see StatsService
 */
@RestController
@RequestMapping("/stats/clubs/{clubId}") // Base URL SANS /api, inclut clubId
@RequiredArgsConstructor
@CrossOrigin // Activation de CORS si nécessaire
@IsReservation // Annotation de sécurité au niveau classe: requiert rôle RESERVATION ou ADMIN
public class StatsController {

    /**
     * Service contenant la logique métier pour le calcul des statistiques.
     *
     * @see StatsService
     */
    private final StatsService statsService;

    /**
     * Récupère le nombre de nouvelles adhésions mensuelles pour le club spécifié,
     * couvrant les 12 derniers mois glissants (y compris les mois à zéro).
     * <p>
     * <b>Requête:</b> GET /stats/clubs/{clubId}/registrations-monthly
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert rôle RESERVATION/ADMIN (via {@code @IsReservation} sur la classe).
     * Le service vérifie que l'utilisateur est gestionnaire du club {@code clubId}.
     * </p>
     *
     * @param clubId L'ID (Integer) du club. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Map<String, Object>>} où chaque map représente un mois ("monthYear", "count").</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see StatsService#getClubMonthlyRegistrations(Integer)
     */
    @GetMapping("/registrations-monthly")
    // @IsReservation redondant ici car déjà sur la classe
    public ResponseEntity<List<Map<String, Object>>> getClubMonthlyRegistrations(@PathVariable Integer clubId) {
        // Service gère existence (-> 404) et sécurité (manager -> 403)
        List<Map<String, Object>> registrations = statsService.getClubMonthlyRegistrations(clubId);
        return ResponseEntity.ok(registrations);
    }

    /**
     * Récupère les moyennes des différentes notes attribuées aux événements *passés* du club spécifié.
     * Inclut une moyenne générale. Les valeurs sont arrondies à une décimale.
     * <p>
     * <b>Requête:</b> GET /stats/clubs/{clubId}/average-event-ratings
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert rôle RESERVATION/ADMIN (via classe). Service vérifie gestionnaire du club.
     * </p>
     *
     * @param clubId L'ID (Integer) du club. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code Map<String, Double>} avec les moyennes par critère et la moyenne générale.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis.</li>
     * </ul>
     * @see StatsService#getClubAverageEventRatings(Integer)
     */
    @GetMapping("/average-event-ratings")
    public ResponseEntity<Map<String, Double>> getClubAverageEventRatings(@PathVariable Integer clubId) {
        // Service gère existence (-> 404) et sécurité (manager -> 403)
        Map<String, Double> ratings = statsService.getClubAverageEventRatings(clubId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Récupère le nombre total d'événements *actifs* organisés par le club spécifié.
     * <p>
     * <b>Requête:</b> GET /stats/clubs/{clubId}/total-events
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert rôle RESERVATION/ADMIN (via classe). Service vérifie gestionnaire du club.
     * </p>
     *
     * @param clubId L'ID (Integer) du club. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code Map<String, Long>} contenant la clé "totalEvents" et le nombre d'événements.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis.</li>
     * </ul>
     * @see StatsService#getTotalEventsForClub(Integer)
     */
    @GetMapping("/total-events")
    public ResponseEntity<Map<String, Long>> getTotalEventsForClub(@PathVariable Integer clubId) {
        // Service gère existence (-> 404) et sécurité (manager -> 403)
        long total = statsService.getTotalEventsForClub(clubId);
        Map<String, Long> result = Map.of("totalEvents", total);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère le taux d'occupation moyen (en %) des événements *actifs* du club spécifié (capacité > 0).
     * Le résultat est arrondi à une décimale.
     * <p>
     * <b>Requête:</b> GET /stats/clubs/{clubId}/average-event-occupancy
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert rôle RESERVATION/ADMIN (via classe). Service vérifie gestionnaire du club.
     * </p>
     *
     * @param clubId L'ID (Integer) du club. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code Map<String, Double>} contenant la clé "averageOccupancyRate" et le taux moyen.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis.</li>
     * </ul>
     * @see StatsService#getClubAverageEventOccupancy(Integer)
     */
    @GetMapping("/average-event-occupancy")
    public ResponseEntity<Map<String, Double>> getClubAverageEventOccupancy(@PathVariable Integer clubId) {
        // Service gère existence (-> 404) et sécurité (manager -> 403)
        double averageRate = statsService.getClubAverageEventOccupancy(clubId);
        Map<String, Double> result = Map.of("averageOccupancyRate", averageRate);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère le nombre d'événements *actifs* du club spécifié prévus dans les 30 prochains jours.
     * <p>
     * <b>Requête:</b> GET /stats/clubs/{clubId}/upcoming-event-count-30d
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert rôle RESERVATION/ADMIN (via classe). Service vérifie gestionnaire du club.
     * </p>
     *
     * @param clubId L'ID (Integer) du club. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code Map<String, Long>} contenant la clé "count" et le nombre d'événements futurs.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis.</li>
     * </ul>
     * @see StatsService#getClubUpcomingEventCount30d(Integer)
     */
    @GetMapping("/upcoming-event-count-30d")
    public ResponseEntity<Map<String, Long>> getClubUpcomingEventCount30d(@PathVariable Integer clubId) {
        // Service gère existence (-> 404) et sécurité (manager -> 403)
        long count = statsService.getClubUpcomingEventCount30d(clubId);
        Map<String, Long> result = Map.of("count", count);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère un résumé complet des statistiques clés pour le tableau de bord du club spécifié.
     * Agrège plusieurs indicateurs (événements, membres, occupation, notes, inscriptions).
     * <p>
     * <b>Requête:</b> GET /stats/clubs/{clubId}/dashboard-summary
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert rôle RESERVATION/ADMIN (via classe). Service vérifie gestionnaire du club.
     * </p>
     *
     * @param clubId L'ID (Integer) du club. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Un objet {@link DashboardSummaryDto} contenant toutes les statistiques agrégées.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis.</li>
     * </ul>
     * @see StatsService#getDashboardSummary(Integer)
     * @see DashboardSummaryDto
     */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@PathVariable Integer clubId) {
        // Service gère existence (-> 404) et sécurité (manager -> 403)
        DashboardSummaryDto summary = statsService.getDashboardSummary(clubId);
        return ResponseEntity.ok(summary);
    }

    // Rappel : La gestion centralisée des exceptions (EntityNotFoundException, AccessDeniedException)
    // via une classe @ControllerAdvice est recommandée pour mapper ces exceptions aux réponses HTTP (404, 403).
}

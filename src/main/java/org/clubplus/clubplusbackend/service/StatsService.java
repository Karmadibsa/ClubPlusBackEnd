package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.*;
import org.clubplus.clubplusbackend.dto.DashboardSummaryDto;
import org.clubplus.clubplusbackend.dto.HomepageStatsDTO;
import org.clubplus.clubplusbackend.model.ReservationStatus;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.clubplus.clubplusbackend.model.ReservationStatus.UTILISE;

/**
 * Service dédié au calcul et à la récupération de statistiques diverses
 * pour les clubs et la plateforme.
 * <p>
 * Les méthodes sont sécurisées pour s'assurer que seul un gestionnaire du club
 * concerné peut accéder à ses statistiques.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final NotationDao notationRepository;
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final SecurityService securityService;

    /**
     * Calcule le nombre d'adhésions mensuelles pour un club sur les 12 derniers mois.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire (ADMIN ou RESERVATION) du club.
     *
     * @param clubId L'ID du club.
     * @return Une liste de maps, chaque map représentant un mois avec les clés "monthYear" et "count".
     * @throws EntityNotFoundException si le club n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas un gestionnaire du club.
     */
    public List<Map<String, Object>> getClubMonthlyRegistrations(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        log.debug("Calcul des adhésions mensuelles pour clubId: {}", clubId);

        LocalDate localDateTodayUTC = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate localDateElevenMonthsAgoUTC = localDateTodayUTC.minusMonths(11);
        LocalDate localDateFirstDayOfThatMonthUTC = localDateElevenMonthsAgoUTC.withDayOfMonth(1);
        Instant startDate = localDateFirstDayOfThatMonthUTC.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> results = adhesionRepository.findMonthlyAdhesionsToClubSince(clubId, startDate);
        log.debug("Résultats bruts des adhésions reçus ({} mois avec données) pour le club {}", results.size(), clubId);

        return formatMonthlyResults(results, startDate, Instant.now());
    }

    /**
     * Calcule les notes moyennes des événements passés d'un club.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Une Map avec les moyennes par critère et une moyenne générale, arrondies à une décimale.
     */
    public Map<String, Double> getClubAverageEventRatings(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        log.debug("Calcul des moyennes de notation pour clubId: {}", clubId);

        List<Integer> pastEventIds = eventRepository.findPastEventIdsByOrganisateurId(clubId, Instant.now());
        if (pastEventIds.isEmpty()) {
            log.debug("Aucun événement passé trouvé pour clubId {}, retour des moyennes par défaut.", clubId);
            return defaultRatingMap();
        }

        double avgAmbiance = notationRepository.findAverageAmbianceForEventIds(pastEventIds).orElse(0.0);
        double avgProprete = notationRepository.findAveragePropreteForEventIds(pastEventIds).orElse(0.0);
        double avgOrganisation = notationRepository.findAverageOrganisationForEventIds(pastEventIds).orElse(0.0);
        double avgFairPlay = notationRepository.findAverageFairPlayForEventIds(pastEventIds).orElse(0.0);
        double avgNiveauJoueurs = notationRepository.findAverageNiveauJoueursForEventIds(pastEventIds).orElse(0.0);

        Map<String, Double> averagesMap = new LinkedHashMap<>();
        averagesMap.put("ambiance", avgAmbiance);
        averagesMap.put("proprete", avgProprete);
        averagesMap.put("organisation", avgOrganisation);
        averagesMap.put("fairPlay", avgFairPlay);
        averagesMap.put("niveauJoueurs", avgNiveauJoueurs);

        double sum = averagesMap.values().stream().mapToDouble(Double::doubleValue).sum();
        long count = averagesMap.values().stream().filter(v -> v > 0.0).count();
        double moyenneGenerale = (count > 0) ? (sum / count) : 0.0;
        averagesMap.put("moyenneGenerale", moyenneGenerale);

        averagesMap.replaceAll((key, value) -> Math.round(value * 10.0) / 10.0);
        log.debug("Moyennes calculées pour clubId {}: {}", clubId, averagesMap);
        return averagesMap;
    }

    /**
     * Compte le nombre total d'événements actifs d'un club.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Le nombre d'événements actifs.
     */
    public long getTotalEventsForClub(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        return eventRepository.countByOrganisateurIdAndActif(clubId, true);
    }

    /**
     * Calcule le taux d'occupation moyen des événements actifs d'un club.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Le taux d'occupation moyen en pourcentage.
     */
    public double getClubAverageEventOccupancy(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        log.debug("Calcul du taux d'occupation moyen pour clubId: {}", clubId);

        List<ReservationStatus> statusesToCount = List.of(ReservationStatus.CONFIRME, UTILISE);
        List<Object[]> eventStatsList = eventRepository.findEventStatsForOccupancy(clubId, statusesToCount);

        if (eventStatsList.isEmpty()) {
            return 0.0;
        }

        double totalOccupancyPercentageSum = 0;
        for (Object[] stats : eventStatsList) {
            long reservedCount = ((Number) stats[1]).longValue();
            long totalCapacity = ((Number) stats[2]).longValue();
            if (totalCapacity > 0) {
                totalOccupancyPercentageSum += ((double) reservedCount / totalCapacity) * 100.0;
            }
        }

        double averageRate = totalOccupancyPercentageSum / eventStatsList.size();
        return Math.round(averageRate * 10.0) / 10.0;
    }

    /**
     * Compte les événements actifs d'un club prévus dans les 30 prochains jours.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Le nombre d'événements à venir.
     */
    public long getClubUpcomingEventCount30d(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        Instant now = Instant.now();
        Instant futureDate = now.plus(30, ChronoUnit.DAYS);
        return eventRepository.countByOrganisateurIdAndActifAndStartTimeBetween(clubId, true, now, futureDate);
    }

    /**
     * Récupère un résumé complet des statistiques pour le tableau de bord d'un club.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Un DTO contenant le résumé des statistiques.
     */
    public DashboardSummaryDto getDashboardSummary(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        log.info("Génération du résumé du tableau de bord pour clubId: {}", clubId);

        return DashboardSummaryDto.builder()
                .totalEvents(getTotalEventsForClub(clubId))
                .upcomingEventsCount30d(getClubUpcomingEventCount30d(clubId))
                .averageEventOccupancyRate(getClubAverageEventOccupancy(clubId))
                .totalActiveMembers(getTotalActiveMembersForClub(clubId))
                .totalParticipations(getTotalEventParticipationsForClub(clubId))
                .monthlyRegistrations(getClubMonthlyRegistrations(clubId))
                .averageEventRatings(getClubAverageEventRatings(clubId))
                .build();
    }

    /**
     * Récupère les statistiques globales de la plateforme (nombre de clubs, événements, membres).
     * <p>
     * <b>Sécurité :</b> Endpoint public, aucune restriction.
     *
     * @return Un DTO contenant les décomptes globaux.
     */
    public HomepageStatsDTO getHomepageStats() {
        log.info("Récupération des statistiques globales pour la page d'accueil.");
        long clubs = clubRepository.count();
        long events = eventRepository.count();
        long members = membreRepository.count();
        return new HomepageStatsDTO(clubs, events, members);
    }

    /**
     * Compte le nombre total de membres actifs pour un club.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Le nombre de membres actifs.
     */
    public long getTotalActiveMembersForClub(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        return adhesionRepository.countActiveMembersByClubId(clubId);
    }

    /**
     * Compte le nombre total de participations ('UTILISE') aux événements d'un club.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club.
     *
     * @param clubId L'ID du club.
     * @return Le nombre total de participations.
     */
    public long getTotalEventParticipationsForClub(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        return reservationRepository.countByStatusAndEventOrganisateurId(UTILISE, clubId);
    }


    // --- Méthodes privées ---

    private List<Map<String, Object>> formatMonthlyResults(List<Object[]> results, Instant startDate, Instant today) {
        Map<YearMonth, Long> monthlyCounts = new TreeMap<>();

        ZonedDateTime zdtStartDate = startDate.atZone(ZoneOffset.UTC);
        ZonedDateTime zdtToday = today.atZone(ZoneOffset.UTC);

        YearMonth startMonth = YearMonth.from(zdtStartDate);
        YearMonth endMonth = YearMonth.from(zdtToday);

        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            monthlyCounts.put(current, 0L);
            current = current.plusMonths(1);
        }

        for (Object[] result : results) {
            try {
                if (result != null && result.length >= 3 && result[0] != null && result[1] != null && result[2] != null) {
                    int year = ((Number) result[0]).intValue();
                    int month = ((Number) result[1]).intValue();
                    long count = ((Number) result[2]).longValue();
                    YearMonth ym = YearMonth.of(year, month);
                    if (monthlyCounts.containsKey(ym)) {
                        monthlyCounts.put(ym, count);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors du formatage d'un résultat d'adhésion mensuelle: {} - {}",
                        Arrays.toString(result), e.getMessage(), e);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return monthlyCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("monthYear", entry.getKey().format(formatter));
                    monthData.put("count", entry.getValue());
                    return monthData;
                })
                .toList();
    }

    private Map<String, Double> defaultRatingMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("ambiance", 0.0);
        map.put("proprete", 0.0);
        map.put("organisation", 0.0);
        map.put("fairPlay", 0.0);
        map.put("niveauJoueurs", 0.0);
        map.put("moyenneGenerale", 0.0);
        return map;
    }
}

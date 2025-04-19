package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.NotationDao;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // La plupart des méthodes sont en lecture seule
public class StatsService {

    private final EventDao eventRepository;
    private final NotationDao notationRepository;
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final SecurityService securityService; // Pour les vérifications de droits

    /**
     * STAT 1: Nouvelles adhésions mensuelles pour un club (12 derniers mois).
     * Sécurité: Vérifie que l'appelant est MANAGER du club.
     * Lance 404 (Club non trouvé), 403 (Non manager).
     */
    public List<Map<String, Object>> getClubMonthlyRegistrations(Integer clubId) {
        // 1. Sécurité d'abord
        securityService.checkManagerOfClubOrThrow(clubId); // Lance 403 si pas manager

        // 2. Vérifier existence club (pour 404 clair)
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId); // -> 404
        }

        // 3. Calculer la période et appeler le DAO
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startDate = today.minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay();
        List<Object[]> results = adhesionRepository.findMonthlyAdhesionsToClubSince(clubId, startDate);

        // 4. Formater les résultats
        return formatMonthlyResults(results, startDate, today);
    }

    /**
     * STAT 2: Moyennes des notes des événements passés du club.
     * Sécurité: Vérifie que l'appelant est MANAGER du club.
     * Lance 404 (Club non trouvé), 403 (Non manager).
     */
    public Map<String, Double> getClubAverageEventRatings(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }

        List<Integer> pastEventIds = eventRepository.findPastEventIdsByOrganisateurId(clubId, LocalDateTime.now());
        if (pastEventIds.isEmpty()) {
            return defaultRatingMap(); // Pas d'événements passés = pas de notes
        }

        // Appeler le DAO qui calcule toutes les moyennes en une fois
        Optional<Object[]> averagesResult = notationRepository.findAverageRatingsForEventIds(pastEventIds);
        if (averagesResult.isEmpty() || averagesResult.get()[0] == null) {
            // Si la première moyenne est null, aucune notation n'a été trouvée pour ces events
            return defaultRatingMap();
        }

        Object[] averagesArray = averagesResult.get();
        Map<String, Double> averagesMap = new LinkedHashMap<>();
        double sum = 0;
        int count = 0;

        // Extraction plus sûre et calcul moyenne générale
        double[] notes = new double[5];
        notes[0] = extractDouble(averagesArray, 0); // ambiance
        notes[1] = extractDouble(averagesArray, 1); // proprete
        notes[2] = extractDouble(averagesArray, 2); // organisation
        notes[3] = extractDouble(averagesArray, 3); // fairPlay
        notes[4] = extractDouble(averagesArray, 4); // niveauJoueurs

        for (double note : notes) {
            if (note > 0) { // AVG retourne null si aucune ligne, ce qui donne 0.0 ici. On ne compte que les vraies moyennes.
                sum += note;
                count++;
            }
        }

        averagesMap.put("ambiance", notes[0]);
        averagesMap.put("proprete", notes[1]);
        averagesMap.put("organisation", notes[2]);
        averagesMap.put("fairPlay", notes[3]);
        averagesMap.put("niveauJoueurs", notes[4]);
        averagesMap.put("moyenneGenerale", (count > 0) ? sum / count : 0.0);

        // Arrondir
        averagesMap.replaceAll((key, value) -> Math.round(value * 10.0) / 10.0); // 1 décimale suffit peut-être
        return averagesMap;
    }

    /**
     * STAT 3: Nombre total d'événements pour un club.
     * Sécurité: Vérifie que l'appelant est MANAGER du club.
     * Lance 404 (Club non trouvé), 403 (Non manager).
     */
    public long getTotalEventsForClub(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        return eventRepository.countByOrganisateurIdAndActif(clubId, true); // Méthode DAO simple
    }

    /**
     * STAT 4: Taux d'occupation moyen des événements du club (capacité > 0).
     * Sécurité: Vérifie que l'appelant est MANAGER du club.
     * Lance 404 (Club non trouvé), 403 (Non manager).
     */
    public double getClubAverageEventOccupancy(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }

        List<Object[]> eventStatsList = eventRepository.findEventStatsForOccupancy(clubId); // Utilise la requête DAO optimisée
        if (eventStatsList.isEmpty()) {
            return 0.0;
        }

        double totalOccupancyPercentageSum = 0;
        // On utilise la taille de la liste car chaque ligne représente un événement valide (capacité > 0)
        int numberOfEventsConsidered = eventStatsList.size();

        for (Object[] stats : eventStatsList) {
            // stats[0] = eventId (inutilisé ici)
            long reservedCount = stats[1] != null ? ((Number) stats[1]).longValue() : 0L; // COUNT peut être null si LEFT JOIN
            long totalCapacity = stats[2] != null ? ((Number) stats[2]).longValue() : 0L; // SUM peut être null si LEFT JOIN

            // La clause HAVING assure totalCapacity > 0, mais une vérification reste prudente
            if (totalCapacity > 0) {
                totalOccupancyPercentageSum += ((double) reservedCount / totalCapacity) * 100.0;
            } else {
                numberOfEventsConsidered--; // Ne pas compter cet événement s'il a une capacité nulle malgré HAVING
            }
        }

        if (numberOfEventsConsidered == 0) {
            return 0.0;
        }

        double averageRate = totalOccupancyPercentageSum / numberOfEventsConsidered;
        return Math.round(averageRate * 10.0) / 10.0; // Arrondir à 1 décimale
    }

    /**
     * STAT 5: Nombre d'événements à venir (30j) pour un club.
     * Sécurité: Vérifie que l'appelant est MANAGER du club.
     * Lance 404 (Club non trouvé), 403 (Non manager).
     */
    public long getClubUpcomingEventCount30d(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(30);
        return eventRepository.countByOrganisateurIdAndActifAndStartBetween(clubId, true, now, futureDate); // Méthode DAO
    }

    /**
     * STAT 6: Nombre total de membres pour un club.
     * Sécurité: Vérifie que l'appelant est MANAGER du club.
     * Lance 404 (Club non trouvé), 403 (Non manager).
     */
    public long getTotalMembersForClub(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        return adhesionRepository.countActiveMembersByClubId(clubId);
    }

    // --- Méthodes Helper Privées ---

    private List<Map<String, Object>> formatMonthlyResults(List<Object[]> results, LocalDateTime startDate, LocalDateTime today) {
        Map<YearMonth, Long> monthlyCounts = new TreeMap<>(); // TreeMap pour trier par clé (YearMonth)
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(today);

        // Initialiser les 12 derniers mois (ou moins si l'app est plus récente)
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            monthlyCounts.put(current, 0L);
            current = current.plusMonths(1);
        }

        // Remplir avec les résultats de la BDD
        for (Object[] result : results) {
            try {
                if (result[0] != null && result[1] != null && result[2] != null) {
                    int year = ((Number) result[0]).intValue();
                    int month = ((Number) result[1]).intValue();
                    long count = ((Number) result[2]).longValue();
                    YearMonth ym = YearMonth.of(year, month);
                    if (monthlyCounts.containsKey(ym)) { // Mettre à jour seulement les mois pré-initialisés
                        monthlyCounts.put(ym, count);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur formatage adhésions mensuelles: " + Arrays.toString(result) + " - " + e.getMessage()); // -> Logger
            }
        }

        // Formater pour la sortie API
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return monthlyCounts.entrySet().stream()
                .map(entry -> Map.<String, Object>of("monthYear", entry.getKey().format(formatter), "count", entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Double> defaultRatingMap() {
        // Utiliser LinkedHashMap si l'ordre d'insertion est important pour l'affichage
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("ambiance", 0.0);
        map.put("proprete", 0.0);
        map.put("organisation", 0.0);
        map.put("fairPlay", 0.0);
        map.put("niveauJoueurs", 0.0);
        map.put("moyenneGenerale", 0.0);
        return map;
    }

    private double extractDouble(Object[] array, int index) {
        if (array == null || index < 0 || index >= array.length || array[index] == null) {
            return 0.0; // AVG(col) retourne NULL si aucune ligne, ce qui devient 0.0
        }
        if (array[index] instanceof Number) {
            return ((Number) array[index]).doubleValue();
        }
        System.err.println("WARN: Type inattendu pour moyenne de notation à l'index " + index + ": " + array[index].getClass()); // -> Logger
        return 0.0;
    }
}

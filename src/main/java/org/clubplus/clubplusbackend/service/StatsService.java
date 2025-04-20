package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.NotationDao;
import org.clubplus.clubplusbackend.security.ReservationStatus;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(StatsService.class); // Ajouter logger


    public Map<String, Double> getClubAverageEventRatings(Integer clubId) {
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Calcul des moyennes de notation pour clubId: {}", clubId);

        // On ne cherche les moyennes que pour les événements passés ET notés.
        // Plutôt que de chercher les events puis les notes, on peut directement chercher les moyennes
        // sur les notations liées aux events du club qui sont passés.

        // Récupérer les IDs des événements passés du club (cette partie reste utile)
        List<Integer> pastEventIds = eventRepository.findPastEventIdsByOrganisateurId(clubId, LocalDateTime.now());
        log.debug("IDs des événements passés trouvés pour clubId {}: {}", clubId, pastEventIds);

        if (pastEventIds.isEmpty()) {
            log.debug("Aucun événement passé trouvé pour clubId {}, retour des moyennes par défaut.", clubId);
            return defaultRatingMap(); // Pas d'événements passés = pas de notes
        }

        // --- Utilisation des requêtes AVG individuelles sur les events passés ---
        // Note: Ces requêtes calculent la moyenne sur TOUTES les notations des events passés combinés.
        // C'est ce que faisait la requête findAverageRatingsForEventIds.
        log.debug("Calcul des moyennes individuelles pour les eventIds: {}", pastEventIds);

        double avgAmbiance = notationRepository.findAverageAmbianceForEventIds(pastEventIds).orElse(0.0);
        double avgProprete = notationRepository.findAveragePropreteForEventIds(pastEventIds).orElse(0.0);
        double avgOrganisation = notationRepository.findAverageOrganisationForEventIds(pastEventIds).orElse(0.0);
        double avgFairPlay = notationRepository.findAverageFairPlayForEventIds(pastEventIds).orElse(0.0);
        double avgNiveauJoueurs = notationRepository.findAverageNiveauJoueursForEventIds(pastEventIds).orElse(0.0);

        // --- FIN Utilisation des requêtes AVG individuelles ---

        Map<String, Double> averagesMap = new LinkedHashMap<>();
        double sum = 0;
        int count = 0;
        double[] notes = {avgAmbiance, avgProprete, avgOrganisation, avgFairPlay, avgNiveauJoueurs};

        for (double note : notes) {
            // On ne compte que les moyennes > 0 pour la moyenne générale
            // car Optional.orElse(0.0) retourne 0 si AVG était NULL.
            if (note > 0.0) {
                sum += note;
                count++;
            }
        }

        averagesMap.put("ambiance", notes[0]);
        averagesMap.put("proprete", notes[1]);
        averagesMap.put("organisation", notes[2]);
        averagesMap.put("fairPlay", notes[3]);
        averagesMap.put("niveauJoueurs", notes[4]);
        averagesMap.put("moyenneGenerale", (count > 0) ? (sum / count) : 0.0);

        // Arrondir à une décimale
        averagesMap.replaceAll((key, value) -> Math.round(value * 10.0) / 10.0);

        log.debug("Moyennes calculées et arrondies pour clubId {}: {}", clubId, averagesMap);
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
        securityService.checkManagerOfClubOrThrow(clubId);
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")");
        }
        log.debug("Calcul du taux d'occupation moyen pour clubId: {}", clubId);

        // Définir les statuts de réservation à compter pour l'occupation
        List<ReservationStatus> statusesToCount = List.of(ReservationStatus.CONFIRME, ReservationStatus.UTILISE);
        log.debug("Statuts considérés pour l'occupation: {}", statusesToCount);

        // Appeler la méthode DAO mise à jour
        List<Object[]> eventStatsList = eventRepository.findEventStatsForOccupancy(clubId, statusesToCount);
        log.debug("Statistiques brutes reçues pour l'occupation ({} événements): {}", eventStatsList.size(), eventStatsList);


        if (eventStatsList.isEmpty()) {
            log.debug("Aucun événement actif avec capacité > 0 trouvé pour clubId {}", clubId);
            return 0.0;
        }

        double totalOccupancyPercentageSum = 0;
        int numberOfEventsConsidered = eventStatsList.size(); // Initialisé avec le nombre d'events retournés

        for (Object[] stats : eventStatsList) {
            // stats[0] = eventId (inutilisé ici)
            // Utilisation de extractDouble pour la robustesse (même si SUM devrait retourner Long/BigDecimal)
            long reservedCount = (long) extractDouble(stats, 1); // Nombre de CONFIRME + UTILISE
            long totalCapacity = (long) extractDouble(stats, 2); // Capacité totale

            log.debug("Stats pour Event ID {}: reserved={}, capacity={}", stats[0], reservedCount, totalCapacity);

            // La clause HAVING assure totalCapacity > 0, mais une vérification reste prudente
            if (totalCapacity > 0) {
                double eventOccupancy = ((double) reservedCount / totalCapacity) * 100.0;
                log.debug(" Taux d'occupation pour cet événement: {}%", eventOccupancy);
                totalOccupancyPercentageSum += eventOccupancy;
            } else {
                // Ne devrait pas arriver à cause du HAVING, mais sécurité
                log.warn("Événement ID {} retourné avec capacité <= 0 malgré HAVING clause.", stats[0]);
                numberOfEventsConsidered--; // Décrémenter si l'événement ne doit pas être compté
            }
        }

        if (numberOfEventsConsidered == 0) {
            log.debug("Aucun événement avec capacité > 0 n'a pu être traité.");
            return 0.0;
        }

        double averageRate = totalOccupancyPercentageSum / numberOfEventsConsidered;
        log.debug("Somme des pourcentages: {}, Nombre d'événements: {}, Taux moyen brut: {}", totalOccupancyPercentageSum, numberOfEventsConsidered, averageRate);

        // Arrondir à 1 décimale
        double roundedAverageRate = Math.round(averageRate * 10.0) / 10.0;
        log.debug("Taux moyen arrondi: {}", roundedAverageRate);
        return roundedAverageRate;
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

    // Helper method to safely extract Double from Object array (handles null and different number types)
    private double extractDouble(Object[] array, int index) {
        if (array == null || index < 0 || index >= array.length || array[index] == null) {
            log.debug("extractDouble: Valeur nulle ou index invalide à l'index {}", index);
            return 0.0; // AVG retourne NULL si aucune ligne, on représente ça par 0.0
        }
        Object value = array[index];
        if (value instanceof Number) {
            // Convertit n'importe quel Number (Double, BigDecimal, Long etc.) en double
            return ((Number) value).doubleValue();
        } else {
            log.warn("extractDouble: Type inattendu {} à l'index {}", value.getClass().getName(), index);
            return 0.0; // Retourne 0.0 si le type est inattendu
        }
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

    // Méthode pour récupérer une moyenne optionnelle et retourner 0.0 si absente
    private double getAverageOrDefault(Optional<Double> avgOpt) {
        return avgOpt.orElse(0.0);
    }
}

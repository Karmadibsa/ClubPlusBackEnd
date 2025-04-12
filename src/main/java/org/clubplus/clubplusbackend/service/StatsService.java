package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.*;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.Statut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // La plupart des stats sont en lecture seule
public class StatsService {

    private final EventDao eventRepository;
    private final DemandeAmiDao demandeAmiRepository;
    private final NotationDao notationRepository;
    private final MembreDao membreRepository;
    private final ClubDao clubRepository; // Pour vérifier l'existence du club

    /**
     * Calcule le taux d'occupation pour les événements.
     *
     * @return Une liste de Maps, chaque Map contenant "eventId", "eventName", "occupancyRate".
     */
    public List<Map<String, Object>> getEventOccupancyRates() {
        List<Event> events = eventRepository.findAll(); // Ou filtrer si nécessaire

        return events.stream()
                .map(event -> {
                    double occupancyRate = 0.0;
                    int totalCapacity = event.getPlaceTotal();
                    if (totalCapacity > 0) {
                        occupancyRate = ((double) event.getPlaceReserve() / totalCapacity) * 100.0;
                    }
                    // Arrondir à 2 décimales pour la propreté
                    occupancyRate = Math.round(occupancyRate * 100.0) / 100.0;

                    Map<String, Object> eventStat = new HashMap<>();
                    eventStat.put("eventId", event.getId());
                    eventStat.put("eventName", event.getNom());
                    eventStat.put("occupancyRate", occupancyRate);
                    return eventStat;
                })
                .collect(Collectors.toList());
    }

    /**
     * Compte le nombre d'événements commençant dans les 30 prochains jours.
     *
     * @return Le nombre d'événements.
     */
    public long countUpcomingEventsNext30Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(30);
        return eventRepository.countByStartBetween(now, futureDate);
    }

    /**
     * Calcule les moyennes des notes pour un événement spécifique.
     *
     * @param eventId L'ID de l'événement.
     * @return Une Map contenant les moyennes par critère et la moyenne générale.
     * @throws EntityNotFoundException si l'événement n'existe pas.
     */
    public Map<String, Double> getAverageRatingsForEvent(Integer eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId);
        }

        Double ambianceAvg = notationRepository.findAverageAmbianceByEventId(eventId);
        Double propreteAvg = notationRepository.findAveragePropreteByEventId(eventId);
        Double organisationAvg = notationRepository.findAverageOrganisationByEventId(eventId);
        Double fairPlayAvg = notationRepository.findAverageFairPlayByEventId(eventId);
        Double niveauJoueursAvg = notationRepository.findAverageNiveauJoueursByEventId(eventId);

        List<Double> validAverages = new ArrayList<>();
        if (ambianceAvg != null) validAverages.add(ambianceAvg);
        if (propreteAvg != null) validAverages.add(propreteAvg);
        if (organisationAvg != null) validAverages.add(organisationAvg);
        if (fairPlayAvg != null) validAverages.add(fairPlayAvg);
        if (niveauJoueursAvg != null) validAverages.add(niveauJoueursAvg);

        Double generalAverage = validAverages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0); // Moyenne de 0 si aucune note

        Map<String, Double> averages = new LinkedHashMap<>(); // LinkedHashMap pour garder l'ordre
        averages.put("ambiance", Optional.ofNullable(ambianceAvg).orElse(0.0));
        averages.put("proprete", Optional.ofNullable(propreteAvg).orElse(0.0));
        averages.put("organisation", Optional.ofNullable(organisationAvg).orElse(0.0));
        averages.put("fairPlay", Optional.ofNullable(fairPlayAvg).orElse(0.0));
        averages.put("niveauJoueurs", Optional.ofNullable(niveauJoueursAvg).orElse(0.0));
        averages.put("moyenneGenerale", Math.round(generalAverage * 100.0) / 100.0); // Arrondi

        return averages;
    }

    /**
     * Récupère le nombre d'inscriptions mensuelles sur les 12 derniers mois.
     *
     * @return Une liste de Maps, chaque Map contenant "monthYear" (String YYYY-MM) et "count" (long).
     */
    public List<Map<String, Object>> getMonthlyRegistrationsLast12Months() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(11).withDayOfMonth(1); // Premier jour du mois il y a 11 mois

        List<Object[]> results = membreRepository.findMonthlyRegistrationsSince(startDate);

        // Préparer une map pour tous les 12 mois avec compte à 0
        Map<YearMonth, Long> monthlyCounts = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.from(startDate);
        for (int i = 0; i < 12; i++) {
            monthlyCounts.put(currentMonth, 0L);
            currentMonth = currentMonth.plusMonths(1);
        }

        // Remplir avec les données de la BDD
        for (Object[] result : results) {
            Integer year = (Integer) result[0];
            Integer month = (Integer) result[1];
            Long count = (Long) result[2];
            YearMonth ym = YearMonth.of(year, month);
            if (monthlyCounts.containsKey(ym)) { // Sécurité, ne devrait arriver que pour les 12 mois concernés
                monthlyCounts.put(ym, count);
            }
        }

        // Transformer en List<Map<String, Object>> pour l'API
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return monthlyCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("monthYear", entry.getKey().format(formatter));
                    monthData.put("count", entry.getValue());
                    return monthData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calcule le nombre total de membres pour un club spécifique.
     *
     * @param clubId L'ID du club.
     * @return Le nombre total de membres.
     * @throws EntityNotFoundException si le club n'existe pas.
     */
    public long getTotalMembersForClub(Integer clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        return membreRepository.countByClubId(clubId);
    }

    /**
     * Calcule le nombre moyen d'amis par membre.
     *
     * @return Le nombre moyen d'amis, ou 0.0 si aucun membre n'existe.
     */
    public double getAverageFriendsPerMember() {
        long totalMembers = membreRepository.count();
        if (totalMembers == 0) {
            return 0.0;
        }
        // Chaque ligne ACCEPTE représente une amitié liant 2 personnes
        long totalFriendshipRows = demandeAmiRepository.countByStatut(Statut.ACCEPTE);
        long totalFriendConnections = totalFriendshipRows * 2;

        double average = (double) totalFriendConnections / totalMembers;
        // Arrondir à 2 décimales
        return Math.round(average * 100.0) / 100.0;
    }

    /**
     * Récupère les N événements passés les mieux notés (basé sur la moyenne générale des notes).
     *
     * @param limit Le nombre maximum d'événements à retourner (ex: 5).
     * @return Une liste de Maps, chaque Map contenant "eventId", "eventName", "overallAverageRating".
     * La liste est triée par note décroissante.
     */
    public List<Map<String, Object>> getTopRatedEvents(int limit) {
        List<Event> pastEvents = eventRepository.findByEndBefore(LocalDateTime.now());
        if (pastEvents.isEmpty()) {
            return List.of(); // Retourne une liste vide si pas d'événements passés
        }

        List<Map<String, Object>> ratedEvents = new ArrayList<>();

        for (Event event : pastEvents) {
            // Utilise la logique de getAverageRatingsForEvent pour obtenir la moyenne générale
            // Note: Ceci peut être optimisé avec une requête unique si performance devient un problème
            try {
                Map<String, Double> averages = getAverageRatingsForEvent(event.getId());
                // Récupère la moyenne générale calculée (peut être null ou 0.0 si pas de notes)
                Double overallAverage = averages.getOrDefault("moyenneGenerale", 0.0);

                if (overallAverage > 0) { // Ne considérer que les événements ayant au moins une note
                    Map<String, Object> eventRating = new HashMap<>();
                    eventRating.put("eventId", event.getId());
                    eventRating.put("eventName", event.getNom());
                    eventRating.put("overallAverageRating", overallAverage);
                    ratedEvents.add(eventRating);
                }
            } catch (EntityNotFoundException e) {
                // Ignore si l'événement n'est étrangement pas trouvé pendant le calcul (ne devrait pas arriver)
            }
        }

        // Trier la liste par "overallAverageRating" en ordre décroissant
        ratedEvents.sort((map1, map2) -> {
            Double rating1 = (Double) map1.getOrDefault("overallAverageRating", 0.0);
            Double rating2 = (Double) map2.getOrDefault("overallAverageRating", 0.0);
            return rating2.compareTo(rating1); // Tri décroissant
        });

        // Retourner les 'limit' premiers éléments
        return ratedEvents.stream().limit(limit).collect(Collectors.toList());
    }
}

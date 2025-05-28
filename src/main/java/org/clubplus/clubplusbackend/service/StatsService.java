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
import java.util.stream.Collectors;

import static org.clubplus.clubplusbackend.model.ReservationStatus.UTILISE;

/**
 * Service dédié au calcul et à la récupération de statistiques diverses
 * concernant l'activité des clubs et de la plateforme globale.
 * Les méthodes nécessitant un contexte de club sont sécurisées pour s'assurer
 * que seul un gestionnaire (ADMIN ou RESERVATION) du club concerné peut y accéder.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // La plupart des méthodes sont en lecture seule par défaut
public class StatsService {

    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final NotationDao notationRepository;
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final SecurityService securityService; // Pour les vérifications de droits

    private static final Logger log = LoggerFactory.getLogger(StatsService.class); // Logger pour le service

    /**
     * Calcule le nombre de nouvelles adhésions mensuelles pour un club spécifique sur les 12 derniers mois glissants.
     * Inclut les mois avec zéro adhésion dans la période.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club pour lequel calculer les statistiques d'adhésion.
     * @return Une liste de Map, chaque Map représentant un mois avec les clés "monthYear" (String au format "yyyy-MM")
     * et "count" (Long représentant le nombre d'adhésions ce mois-là). La liste couvre les 12 derniers mois.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public List<Map<String, Object>> getClubMonthlyRegistrations(Integer clubId) {
        // 1. Sécurité d'abord : Vérifie si l'utilisateur est manager du club
        securityService.checkManagerOfClubOrThrow(clubId); // Lance une exception si non autorisé (-> 403)

        // 2. Vérifier l'existence du club pour retourner un 404 clair si nécessaire
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId); // -> 404
        }
        log.debug("Calcul des adhésions mensuelles pour clubId: {}", clubId);

        // 3. Calculer la période de 12 mois (les 11 mois précédents + le mois courant) et appeler le DAO

        Instant today = Instant.now();

// Convertir l'Instant actuel en LocalDate en UTC
        LocalDate localDateTodayUTC = today.atZone(ZoneOffset.UTC).toLocalDate();

// Soustraire 11 mois
        LocalDate localDateElevenMonthsAgoUTC = localDateTodayUTC.minusMonths(11);

// Aller au premier jour de ce mois
        LocalDate localDateFirstDayOfThatMonthUTC = localDateElevenMonthsAgoUTC.withDayOfMonth(1);

// Convertir cette LocalDate (qui est en UTC) en un Instant au début de ce jour
        Instant startDate = localDateFirstDayOfThatMonthUTC.atStartOfDay(ZoneOffset.UTC).toInstant();
        log.debug("Période de calcul des adhésions: de {} à {}", startDate, today);
        // Récupère les agrégats bruts [année, mois, compte] depuis le DAO
        List<Object[]> results = adhesionRepository.findMonthlyAdhesionsToClubSince(clubId, LocalDateTime.from(startDate));
        log.debug("Résultats bruts des adhésions reçus ({} mois avec données): {}", results.size(), results);

        // 4. Formater les résultats pour inclure les mois à zéro et structurer la sortie
        List<Map<String, Object>> formattedResults = formatMonthlyResults(results, startDate, today);
        log.debug("Résultats formatés des adhésions pour clubId {}: {}", clubId, formattedResults);
        return formattedResults;
    }

    /**
     * Calcule les moyennes des différentes notes attribuées aux événements *passés* d'un club spécifique.
     * Inclut une moyenne générale calculée à partir des moyennes des critères individuels ayant reçu des notes (> 0).
     * Les moyennes sont arrondies à une décimale.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club pour lequel calculer les moyennes de notation.
     * @return Une Map où les clés sont les noms des critères de notation ("ambiance", "proprete", "organisation", "fairPlay", "niveauJoueurs")
     * et "moyenneGenerale", et les valeurs sont les moyennes (Double) arrondies à une décimale. Retourne 0.0 si aucun événement passé
     * n'a été noté pour un critère donné ou globalement. Utilise un {@link LinkedHashMap} pour préserver l'ordre d'insertion.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public Map<String, Double> getClubAverageEventRatings(Integer clubId) {
        // Sécurité et vérification d'existence
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Calcul des moyennes de notation pour clubId: {}", clubId);

        // 1. Identifier les événements passés du club
        Instant now = Instant.now();
        List<Integer> pastEventIds = eventRepository.findPastEventIdsByOrganisateurId(clubId, now);
        log.debug("IDs des événements passés trouvés pour clubId {}: {}", clubId, pastEventIds);

        // Si aucun événement passé n'existe pour ce club, retourner des moyennes par défaut (0.0)
        if (pastEventIds.isEmpty()) {
            log.debug("Aucun événement passé trouvé pour clubId {}, retour des moyennes par défaut.", clubId);
            return defaultRatingMap();
        }

        // 2. Calculer la moyenne pour chaque critère sur l'ensemble des notations de ces événements passés
        log.debug("Calcul des moyennes individuelles pour les eventIds: {}", pastEventIds);
        // Les méthodes findAverage... retournent Optional<Double>, orElse(0.0) gère le cas où AVG est NULL (aucune note)
        double avgAmbiance = notationRepository.findAverageAmbianceForEventIds(pastEventIds).orElse(0.0);
        double avgProprete = notationRepository.findAveragePropreteForEventIds(pastEventIds).orElse(0.0);
        double avgOrganisation = notationRepository.findAverageOrganisationForEventIds(pastEventIds).orElse(0.0);
        double avgFairPlay = notationRepository.findAverageFairPlayForEventIds(pastEventIds).orElse(0.0);
        double avgNiveauJoueurs = notationRepository.findAverageNiveauJoueursForEventIds(pastEventIds).orElse(0.0);
        log.debug("Moyennes brutes calculées: Ambiance={}, Proprete={}, Organisation={}, FairPlay={}, NiveauJoueurs={}", avgAmbiance, avgProprete, avgOrganisation, avgFairPlay, avgNiveauJoueurs);

        // 3. Préparer la map de résultats et calculer la moyenne générale
        Map<String, Double> averagesMap = new LinkedHashMap<>(); // Préserve l'ordre
        double sum = 0;
        int count = 0;
        double[] notes = {avgAmbiance, avgProprete, avgOrganisation, avgFairPlay, avgNiveauJoueurs};

        // Calcule la moyenne générale en ne considérant que les critères ayant reçu des notes (moyenne > 0)
        for (double note : notes) {
            if (note > 0.0) {
                sum += note;
                count++;
            }
        }
        double moyenneGenerale = (count > 0) ? (sum / count) : 0.0;
        log.debug("Calcul moyenne générale: somme={}, nombre critères notés={}, moyenne brute={}", sum, count, moyenneGenerale);

        // Remplir la map avec les moyennes
        averagesMap.put("ambiance", notes[0]);
        averagesMap.put("proprete", notes[1]);
        averagesMap.put("organisation", notes[2]);
        averagesMap.put("fairPlay", notes[3]);
        averagesMap.put("niveauJoueurs", notes[4]);
        averagesMap.put("moyenneGenerale", moyenneGenerale);

        // 4. Arrondir toutes les valeurs à une décimale
        averagesMap.replaceAll((key, value) -> Math.round(value * 10.0) / 10.0);

        log.debug("Moyennes calculées et arrondies pour clubId {}: {}", clubId, averagesMap);
        return averagesMap;
    }


    /**
     * Compte le nombre total d'événements *actifs* organisés par un club spécifique.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club.
     * @return Le nombre total (long) d'événements actifs pour ce club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public long getTotalEventsForClub(Integer clubId) {
        // Sécurité et vérification d'existence
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Comptage du nombre total d'événements actifs pour clubId: {}", clubId);
        // Appelle une méthode DAO simple qui compte les événements actifs du club
        long count = eventRepository.countByOrganisateurIdAndActif(clubId, true);
        log.debug("Nombre total d'événements actifs trouvés pour clubId {}: {}", clubId, count);
        return count;
    }

    /**
     * Calcule le taux d'occupation moyen (en pourcentage) pour les événements *actifs* d'un club,
     * en ne considérant que les événements ayant une capacité totale supérieure à zéro.
     * Le taux d'occupation est calculé comme (nombre de réservations 'CONFIRME' + 'UTILISE') / (capacité totale de l'événement).
     * Le résultat final est la moyenne de ces taux individuels, arrondie à une décimale.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club.
     * @return Le taux d'occupation moyen (double) en pourcentage (0.0 à 100.0), arrondi à une décimale.
     * Retourne 0.0 si aucun événement actif avec capacité > 0 n'existe pour ce club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public double getClubAverageEventOccupancy(Integer clubId) {
        // Sécurité et vérification d'existence
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Calcul du taux d'occupation moyen pour clubId: {}", clubId);

        // Définir les statuts de réservation qui comptent pour l'occupation
        List<ReservationStatus> statusesToCount = List.of(ReservationStatus.CONFIRME, UTILISE);
        log.debug("Statuts considérés pour l'occupation: {}", statusesToCount);

        // 1. Récupérer les statistiques nécessaires (réservations confirmées/utilisées et capacité totale)
        //    pour chaque événement actif du club ayant une capacité > 0.
        List<Object[]> eventStatsList = eventRepository.findEventStatsForOccupancy(clubId, statusesToCount);
        log.debug("Statistiques brutes reçues pour l'occupation ({} événements pertinents): {}", eventStatsList.size(), eventStatsList);

        // Si aucun événement ne correspond aux critères (actif, capacité > 0)
        if (eventStatsList.isEmpty()) {
            log.debug("Aucun événement actif avec capacité > 0 trouvé pour clubId {}", clubId);
            return 0.0;
        }

        // 2. Calculer le taux d'occupation pour chaque événement et faire la moyenne
        double totalOccupancyPercentageSum = 0;
        int numberOfEventsConsidered = 0; // Compteur pour la moyenne

        for (Object[] stats : eventStatsList) {
            // stats[0] = eventId
            // stats[1] = count(reservations CONFIRME ou UTILISE) -> Long ou BigDecimal selon le SGBD
            // stats[2] = sum(categories.capacite) pour cet event -> Long ou BigDecimal
            long reservedCount = ((Number) stats[1]).longValue(); // Supposons Long retourné par SUM/COUNT
            long totalCapacity = ((Number) stats[2]).longValue();

            log.debug("Stats pour Event ID {}: reserved={}, capacity={}", stats[0], reservedCount, totalCapacity);

            // La requête DAO filtre déjà capacity > 0, mais une vérification reste saine
            if (totalCapacity > 0) {
                // Calcul du taux pour cet événement en pourcentage
                double eventOccupancy = ((double) reservedCount / totalCapacity) * 100.0;
                log.debug(" Taux d'occupation pour cet événement: {}%", eventOccupancy);
                totalOccupancyPercentageSum += eventOccupancy;
                numberOfEventsConsidered++; // Incrémenter le compteur pour la moyenne
            } else {
                // Log si un événement avec capacité 0 est retourné malgré le filtre (ne devrait pas arriver)
                log.warn("Événement ID {} retourné avec capacité <= 0 malgré le filtre DAO.", stats[0]);
            }
        }

        // Éviter division par zéro si aucun événement n'a pu être traité
        if (numberOfEventsConsidered == 0) {
            log.debug("Aucun événement avec capacité > 0 n'a pu être traité pour la moyenne.");
            return 0.0;
        }

        // Calcul de la moyenne des taux
        double averageRate = totalOccupancyPercentageSum / numberOfEventsConsidered;
        log.debug("Somme des pourcentages: {}, Nombre d'événements considérés: {}, Taux moyen brut: {}", totalOccupancyPercentageSum, numberOfEventsConsidered, averageRate);

        // Arrondir à 1 décimale
        double roundedAverageRate = Math.round(averageRate * 10.0) / 10.0;
        log.debug("Taux moyen arrondi: {}", roundedAverageRate);
        return roundedAverageRate;
    }


    /**
     * Compte le nombre d'événements *actifs* organisés par un club spécifique et
     * dont la date de début est prévue dans les 30 prochains jours (à partir de maintenant).
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club.
     * @return Le nombre (long) d'événements actifs prévus dans les 30 prochains jours.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public long getClubUpcomingEventCount30d(Integer clubId) {
        // Sécurité et vérification d'existence
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Comptage des événements à venir (30j) pour clubId: {}", clubId);

        // Définir la période : de maintenant à dans 30 jours
        Instant now = Instant.now();
        Instant futureDate = now.plus(30, ChronoUnit.DAYS); // Ajoute 30 jours
        log.debug("Période de recherche des événements à venir: de {} à {}", now, futureDate);

        // Appelle une méthode DAO qui compte les événements actifs dans la période donnée
        long count = eventRepository.countByOrganisateurIdAndActifAndStartTimeBetween(clubId, true, now, futureDate);
        log.debug("Nombre d'événements actifs à venir (30j) trouvés pour clubId {}: {}", clubId, count);
        return count;
    }


    // --- Méthodes Helper Privées ---

    /**
     * Formate les résultats bruts d'agrégation mensuelle (année, mois, compte) en une liste
     * de maps pour l'API, en s'assurant que tous les mois de la période demandée sont présents
     * (avec une valeur de 0 si aucune donnée n'a été retournée par la BDD pour ce mois).
     *
     * @param results   Liste de {@code Object[]} contenant [année (Number), mois (Number), compte (Number)] issue du DAO.
     * @param startDate La date de début de la période (utilisée pour déterminer le premier mois).
     * @param today     La date de fin de la période (utilisée pour déterminer le dernier mois).
     * @return Une liste de {@code Map<String, Object>} triée chronologiquement, chaque map contenant "monthYear" et "count".
     */
    private List<Map<String, Object>> formatMonthlyResults(List<Object[]> results, Instant startDate, Instant today) {
        // Utilise TreeMap pour garantir l'ordre chronologique des mois
        Map<YearMonth, Long> monthlyCounts = new TreeMap<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(today);

        // 1. Initialiser la map avec 0 pour tous les mois de la période
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            monthlyCounts.put(current, 0L);
            current = current.plusMonths(1);
        }
        log.trace("Map des mois initialisée ({} mois): {}", monthlyCounts.size(), monthlyCounts.keySet());

        // 2. Remplir la map avec les comptes réels issus de la base de données
        for (Object[] result : results) {
            try {
                // Vérification de nullité pour robustesse
                if (result != null && result.length >= 3 && result[0] != null && result[1] != null && result[2] != null) {
                    int year = ((Number) result[0]).intValue();
                    int month = ((Number) result[1]).intValue();
                    long count = ((Number) result[2]).longValue();
                    YearMonth ym = YearMonth.of(year, month);
                    // Mettre à jour la valeur si le mois est bien dans notre période initialisée
                    if (monthlyCounts.containsKey(ym)) {
                        monthlyCounts.put(ym, count);
                        log.trace("Mise à jour pour {}: {}", ym, count);
                    } else {
                        log.warn("Résultat reçu pour un mois hors période initialisée: {}", ym);
                    }
                } else {
                    log.warn("Résultat brut invalide ou incomplet ignoré: {}", Arrays.toString(result));
                }
            } catch (Exception e) {
                // Log l'erreur mais continue le traitement des autres résultats
                log.error("Erreur lors du formatage d'un résultat d'adhésion mensuelle: {} - {}", Arrays.toString(result), e.getMessage(), e);
            }
        }

        // 3. Formater la map en liste de maps pour la sortie JSON
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return monthlyCounts.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "monthYear", entry.getKey().format(formatter),
                        "count", entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Méthode utilitaire pour extraire en toute sécurité une valeur de type double
     * d'un tableau d'objets, en gérant les cas null et les différents types numériques.
     *
     * @param array Le tableau d'objets.
     * @param index L'index de l'élément à extraire.
     * @return La valeur double extraite, ou 0.0 si la valeur est null, l'index invalide,
     * ou si le type n'est pas un {@link Number}.
     */
    private double extractDouble(Object[] array, int index) {
        // Vérifications de base pour éviter les erreurs
        if (array == null || index < 0 || index >= array.length || array[index] == null) {
            log.debug("extractDouble: Valeur nulle ou index invalide ({}) dans le tableau.", index);
            return 0.0; // Convention : une absence de valeur (AVG=NULL) est représentée par 0.0
        }
        Object value = array[index];
        // Vérifie si la valeur est un type numérique standard (Long, Integer, Double, BigDecimal...)
        if (value instanceof Number) {
            // Convertit en double
            return ((Number) value).doubleValue();
        } else {
            // Log un avertissement si un type inattendu est rencontré
            log.warn("extractDouble: Type inattendu {} trouvé à l'index {}. Retourne 0.0.", value.getClass().getName(), index);
            return 0.0;
        }
    }

    /**
     * Retourne une map pré-remplie avec toutes les clés de notation et une valeur de 0.0.
     * Utilisé comme valeur par défaut lorsque aucune notation n'est disponible.
     *
     * @return Une {@link LinkedHashMap} avec les clés de notation initialisées à 0.0.
     */
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

    /**
     * Méthode utilitaire (non utilisée dans le code fourni mais potentiellement utile)
     * pour obtenir la valeur d'un {@code Optional<Double>} ou 0.0 si l'Optional est vide.
     *
     * @param avgOpt L'Optional contenant potentiellement une moyenne.
     * @return La valeur double de l'Optional, ou 0.0 si vide.
     */
    private double getAverageOrDefault(Optional<Double> avgOpt) {
        return avgOpt.orElse(0.0);
    }

    /**
     * Compte le nombre total de membres actifs associés à un club spécifique.
     * L'activité est déterminée par l'existence d'une adhésion valide dans la table `Adhesion`.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club.
     * @return Le nombre (long) de membres ayant une adhésion active pour ce club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public long getTotalActiveMembersForClub(Integer clubId) {
        // Sécurité et vérification d'existence ajoutées pour cohérence
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Comptage des membres actifs pour clubId: {}", clubId);
        // Appelle une méthode DAO qui compte les adhésions (supposées représenter les membres actifs du club)
        long count = adhesionRepository.countActiveMembersByClubId(clubId); // Nom de méthode supposé
        log.debug("Nombre de membres actifs trouvés pour Club {}: {}", clubId, count);
        return count;
    }

    /**
     * Compte le nombre total de participations confirmées (réservations avec statut {@link ReservationStatus#UTILISE})
     * à tous les événements organisés par un club spécifique.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club organisateur.
     * @return Le nombre total (long) de réservations marquées comme 'UTILISE' pour les événements de ce club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public long getTotalEventParticipationsForClub(Integer clubId) {
        // Sécurité et vérification d'existence ajoutées pour cohérence
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.debug("Comptage des participations totales (statut UTILISE) pour clubId: {}", clubId);
        // Appelle une méthode DAO qui compte les réservations au statut UTILISE pour les événements du club
        long totalParticipations = reservationRepository.countByStatusAndEventOrganisateurId(UTILISE, clubId);
        log.debug("Nombre total de participations (UTILISE) trouvées pour Club {}: {}", clubId, totalParticipations);
        return totalParticipations;
    }

    /**
     * Récupère un résumé complet des statistiques clés pour le tableau de bord d'un club spécifique.
     * Agrège les résultats de plusieurs autres méthodes de ce service.
     * <p>
     * Sécurité : Nécessite que l'utilisateur appelant soit gestionnaire (ADMIN ou RESERVATION) du club spécifié.
     * </p>
     *
     * @param clubId L'identifiant unique du club.
     * @return Un objet {@link DashboardSummaryDto} contenant diverses statistiques agrégées pour le club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'est pas trouvé (-> HTTP 404).
     * @throws AccessDeniedException   (ou similaire via {@code SecurityService}) si l'utilisateur n'est pas gestionnaire du club (-> HTTP 403).
     * @throws SecurityException       (ou similaire via {@code SecurityService}) si l'utilisateur courant ne peut être identifié.
     */
    public DashboardSummaryDto getDashboardSummary(Integer clubId) {
        // Sécurité et vérification d'existence (effectuées une seule fois ici)
        securityService.checkManagerOfClubOrThrow(clubId); // -> 403
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé (ID: " + clubId + ")"); // -> 404
        }
        log.info("Génération du résumé du tableau de bord pour clubId: {}", clubId); // Log info pour cette action globale

        // Appelle les différentes méthodes de calcul de statistiques (sécurité déjà vérifiée)
        long totalEvents = this.getTotalEventsForClub(clubId);
        long upcomingCount = this.getClubUpcomingEventCount30d(clubId);
        double avgOccupancy = this.getClubAverageEventOccupancy(clubId);
        long totalActiveMembers = this.getTotalActiveMembersForClub(clubId);
        long totalParticipations = this.getTotalEventParticipationsForClub(clubId);
        List<Map<String, Object>> registrations = this.getClubMonthlyRegistrations(clubId);
        Map<String, Double> ratings = this.getClubAverageEventRatings(clubId);

        // Construit et retourne le DTO avec les résultats agrégés
        DashboardSummaryDto summary = DashboardSummaryDto.builder()
                .totalEvents(totalEvents)
                .upcomingEventsCount30d(upcomingCount)
                .averageEventOccupancyRate(avgOccupancy)
                .totalActiveMembers(totalActiveMembers)
                .totalParticipations(totalParticipations)
                .monthlyRegistrations(registrations)
                .averageEventRatings(ratings)
                .build();

        log.info("Résumé du tableau de bord généré avec succès pour clubId: {}", clubId);
        return summary;
    }

    /**
     * Récupère des statistiques globales de la plateforme (nombre total de clubs, d'événements et de membres enregistrés).
     * Ces statistiques sont destinées à une page d'accueil publique et n'appliquent pas de filtres spécifiques
     * (comme "actif" ou "réussi") sauf si les méthodes DAO sous-jacentes (`.count()`) le font.
     * <p>
     * Sécurité : Aucune restriction d'accès spécifique, destiné à être public.
     * </p>
     *
     * @return Un objet {@link HomepageStatsDTO} contenant les décomptes globaux.
     */
    public HomepageStatsDTO getHomepageStats() {
        log.info("Récupération des statistiques globales pour la page d'accueil.");
        // Compte simple des entités totales dans les tables respectives.
        // Adaptez si des filtres globaux (ex: compter seulement clubs actifs) sont nécessaires.
        long clubs = clubRepository.count();
        long events = eventRepository.count();
        long members = membreRepository.count();
        log.info("Statistiques globales trouvées: Clubs={}, Events={}, Members={}", clubs, events, members);

        return new HomepageStatsDTO(clubs, events, members);
    }
}

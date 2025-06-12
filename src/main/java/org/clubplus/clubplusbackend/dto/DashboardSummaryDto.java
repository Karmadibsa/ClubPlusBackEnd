package org.clubplus.clubplusbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO (Data Transfer Object) pour le résumé des statistiques du tableau de bord d'un club.
 * Agrège plusieurs indicateurs clés pour un affichage synthétique destiné aux gestionnaires.
 */
@Data
@Builder
public class DashboardSummaryDto {

    /**
     * Le nombre total d'événements actifs du club.
     */
    private long totalEvents;

    /**
     * Le nombre d'événements prévus dans les 30 prochains jours.
     */
    private long upcomingEventsCount30d;

    /**
     * Le taux d'occupation moyen des événements, exprimé en pourcentage.
     */
    private double averageEventOccupancyRate;

    /**
     * Le nombre total de membres actifs dans le club.
     */
    private long totalActiveMembers;

    /**
     * Le nombre total de participations effectives (réservations marquées comme utilisées).
     */
    private long totalParticipations;

    /**
     * Les données sur les inscriptions mensuelles pour une période récente.
     * Chaque Map contient des clés comme "annee", "mois", et "compte".
     */
    private List<Map<String, Object>> monthlyRegistrations;

    /**
     * Les notes moyennes des événements, réparties par critère.
     * La clé de la Map est le nom du critère (ex: "ambiance") et la valeur est la note moyenne.
     */
    private Map<String, Double> averageEventRatings;

}

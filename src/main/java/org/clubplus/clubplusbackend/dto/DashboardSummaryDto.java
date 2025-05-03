package org.clubplus.clubplusbackend.dto;

import lombok.Builder;
import lombok.Data;
import org.clubplus.clubplusbackend.service.StatsService;

import java.util.List;
import java.util.Map;

/**
 * DTO (Data Transfer Object) conçu pour regrouper les statistiques et données sommaires
 * destinées à être affichées sur un tableau de bord (Dashboard), typiquement pour un gestionnaire
 * de club (rôle RESERVATION ou ADMIN).
 *
 * <p>Cette classe agrège diverses métriques clés calculées par le {@link StatsService}, telles que
 * le nombre d'événements, le taux d'occupation, le nombre de membres, les inscriptions mensuelles,
 * et les notes moyennes des événements.</p>
 *
 * <p>L'annotation {@link Builder @Builder} permet une construction facile et lisible de l'objet,
 * et {@link Data @Data} fournit les getters, setters, toString, etc.</p>
 *
 * @see StatsService (où ce DTO est probablement construit)
 * @see org.clubplus.clubplusbackend.controller.StatsController (où ce DTO serait retourné)
 */
@Data    // Lombok: Génère getters, setters, toString, equals, hashCode.
@Builder // Lombok: Fournit un pattern Builder pour une construction fluide de l'objet.
public class DashboardSummaryDto {

    /**
     * Nombre total d'événements (actifs ou tous, selon la logique service) associés au contexte (ex: club).
     */
    private long totalEvents;

    /**
     * Nombre d'événements à venir dans les 30 prochains jours.
     */
    private long upcomingEventsCount30d;

    /**
     * Taux d'occupation moyen des événements récents/passés, exprimé en pourcentage (ex: 75.5 pour 75.5%).
     * Le calcul exact (événements considérés, période) est défini dans le service.
     */
    private double averageEventOccupancyRate;

    /**
     * Nombre total de membres actifs associés au contexte (ex: club).
     */
    private long totalActiveMembers;

    /**
     * Nombre total de participations (réservations confirmées ou utilisées) aux événements.
     */
    private long totalParticipations;

    /**
     * Liste des inscriptions mensuelles récentes.
     * Chaque élément de la liste est une {@code Map} représentant un mois, contenant typiquement
     * des clés comme "annee", "mois", et "compte".
     * Exemple: {@code [{"annee": 2024, "mois": 5, "compte": 15}, {"annee": 2024, "mois": 6, "compte": 22}]}
     */
    private List<Map<String, Object>> monthlyRegistrations;

    /**
     * Carte contenant les notes moyennes pour différents critères d'évaluation des événements.
     * La clé représente le critère (ex: "ambiance", "organisation") et la valeur est la note moyenne ({@code Double}).
     * Exemple: {@code {"ambiance": 4.2, "proprete": 4.8, "organisation": 4.5, ...}}
     */
    private Map<String, Double> averageEventRatings;

}

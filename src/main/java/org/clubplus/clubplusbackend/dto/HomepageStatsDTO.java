package org.clubplus.clubplusbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO (Data Transfer Object) pour les statistiques globales de la page d'accueil.
 * Transporte des indicateurs clés sur l'ensemble de l'application.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomepageStatsDTO {

    /**
     * Le nombre total de clubs actifs dans le système.
     */
    private long clubCount;

    /**
     * Le nombre total d'événements actifs.
     */
    private long eventCount;

    /**
     * Le nombre total de membres actifs.
     */
    private long memberCount;

}

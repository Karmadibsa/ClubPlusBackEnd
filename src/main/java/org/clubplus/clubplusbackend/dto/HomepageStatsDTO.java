package org.clubplus.clubplusbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.service.StatsService;

/**
 * DTO (Data Transfer Object) simple utilisé pour transmettre des statistiques
 * agrégées de base sur l'ensemble de l'application, destinées à être affichées
 * sur la page d'accueil publique.
 *
 * @see StatsService (où ce DTO est probablement construit)
 * @see org.clubplus.clubplusbackend.controller.AuthController#getStats() (où ce DTO est retourné)
 */
@Getter // Lombok: Génère les getters.
@Setter // Lombok: Génère les setters.
@NoArgsConstructor // Constructeur vide
@AllArgsConstructor // Constructeur avec tous les champs
public class HomepageStatsDTO {

    /**
     * Nombre total de clubs actifs enregistrés dans le système.
     */
    private long clubCount;

    /**
     * Nombre total d'événements actifs (ou tous, selon la définition) enregistrés.
     */
    private long eventCount;

    /**
     * Nombre total de membres actifs enregistrés.
     */
    private long memberCount;

}

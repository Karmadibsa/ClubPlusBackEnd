package org.clubplus.clubplusbackend.model;

/**
 * Définit les différents rôles qu'un {@link Membre} peut avoir au sein de l'application.
 * Ces rôles déterminent les permissions et l'accès aux différentes fonctionnalités.
 */
public enum Role {
    /**
     * Rôle standard pour un membre. Permet de rejoindre des clubs, de s'inscrire
     * à des événements et de gérer son profil personnel.
     */
    MEMBRE,

    /**
     * Rôle de gestionnaire pour un club spécifique.
     * Permet de créer et gérer les événements, de valider les réservations
     * et de consulter les statistiques du club.
     */
    RESERVATION,

    /**
     * Rôle d'administrateur pour un club spécifique.
     * Inclut toutes les permissions de {@code RESERVATION}, et permet en plus de
     * modifier les informations du club et de gérer les rôles des autres membres.
     */
    ADMIN,

    /**
     * Rôle spécial attribué à un compte après sa suppression logique (anonymisation).
     * Ce rôle bloque l'accès et signale que les données personnelles ont été supprimées.
     */
    ANONYME
}

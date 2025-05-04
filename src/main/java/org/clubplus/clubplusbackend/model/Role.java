package org.clubplus.clubplusbackend.model;

/**
 * Définit les différents rôles qu'un utilisateur ({@link Membre}) peut avoir
 * au sein de l'application ClubPlus. Ces rôles déterminent les permissions
 * et l'accès aux différentes fonctionnalités et sections de l'application [1].
 */
public enum Role {
    /**
     * Rôle standard pour un membre adhérent d'un ou plusieurs clubs.
     * Peut consulter les événements de ses clubs, effectuer des réservations (jusqu'à 2 par événement),
     * gérer son profil et ses amitiés [1].
     */
    MEMBRE,

    /**
     * Rôle de gestionnaire pour un club spécifique (section Rservation).
     * Peut gérer les événements (créer, modifier, annuler) et les réservations
     * (consulter, valider/scanner) pour *son* club unique.
     * Peut consulter les statistiques du club et les notations des événements.
     * Ne peut *pas* modifier les informations du club ni les rôles des autres membres [1].
     */
    RESERVATION,

    /**
     * Rôle d'administrateur principal pour un club spécifique (section Administrative).
     * Possède toutes les permissions du rôle {@code RESERVATION} pour *son* club unique,
     * et peut en plus modifier les informations du club et gérer les rôles
     * (promouvoir/rétrograder entre {@code MEMBRE} et {@code RESERVATION})
     * des membres de ce club [1].
     */
    ADMIN,

    /**
     * Rôle attribué à un compte utilisateur qui a été supprimé logiquement (anonymisé).
     * Ce rôle empêche la connexion et signale que les données personnelles
     * identifiables ont été retirées ou masquées.
     */
    ANONYME
}

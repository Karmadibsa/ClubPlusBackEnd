package org.clubplus.clubplusbackend.model;

/**
 * Représente l'état d'une demande d'ami ({@link DemandeAmi}) entre deux membres.
 * Utilisé pour suivre le processus d'établissement d'une relation d'amitié.
 */
public enum Statut {
    /**
     * La demande d'ami a été envoyée mais n'a pas encore été traitée
     * (acceptée ou refusée) par le destinataire.
     */
    ATTENTE,

    /**
     * La demande d'ami a été acceptée par le destinataire.
     * Une relation d'amitié est maintenant établie.
     */
    ACCEPTEE,

    /**
     * La demande d'ami a été refusée par le destinataire.
     * Aucune relation d'amitié n'est établie.
     */
    REFUSE
}

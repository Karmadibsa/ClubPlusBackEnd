package org.clubplus.clubplusbackend.model;

/**
 * Représente l'état d'une demande d'ami ({@link DemandeAmi}) entre deux membres.
 * Utilisé pour suivre le processus d'établissement d'une relation d'amitié.
 */
public enum Statut {
    /**
     * La demande d'ami a été envoyée par l'initiateur mais n'a pas encore
     * été traitée (acceptée ou refusée) par le destinataire.
     */
    ATTENTE,

    /**
     * La demande d'ami a été explicitement acceptée par le destinataire.
     * Une relation d'amitié est établie entre les deux membres.
     */
    ACCEPTE,

    /**
     * La demande d'ami a été explicitement refusée par le destinataire.
     * Aucune relation d'amitié n'est établie suite à cette demande.
     */
    REFUSE
}

package org.clubplus.clubplusbackend.model;

/**
 * Représente les différents statuts possibles pour une {@link Reservation}.
 * Ces statuts décrivent le cycle de vie d'une réservation depuis sa création
 * jusqu'à son utilisation ou son annulation.
 */
public enum ReservationStatus {
    /**
     * La réservation est confirmée et la place est retenue pour l'utilisateur.
     * C'est le statut initial d'une réservation réussie.
     */
    CONFIRME,

    /**
     * La réservation a été utilisée, indiquant que le membre s'est présenté à l'événement
     * (par exemple, après le scan d'un QR code). Une réservation utilisée permet au membre
     * de noter l'événement par la suite.
     */
    UTILISE,

    /**
     * La réservation a été annulée par l'utilisateur ou un gestionnaire.
     * La place n'est plus considérée comme réservée.
     */
    ANNULE
}

package org.clubplus.clubplusbackend.model;

/**
 * Représente les différents statuts possibles pour une réservation ({@link Reservation}).
 * Ces statuts décrivent le cycle de vie d'une réservation, depuis sa confirmation
 * jusqu'à son utilisation effective ou son annulation.
 */
public enum ReservationStatus {
    /**
     * La réservation est confirmée et active. Une place est retenue pour l'utilisateur
     * pour la catégorie d'événement spécifiée. C'est l'état initial après une création réussie.
     */
    CONFIRME,

    /**
     * La réservation a été validée comme utilisée, indiquant typiquement que le membre
     * s'est présenté à l'événement (par exemple, après scan d'un QR code) [1].
     * Ce statut est important pour la notation ultérieure de l'événement.
     */
    UTILISE,

    /**
     * La réservation a été annulée, soit par l'utilisateur avant l'événement,
     * soit potentiellement par un gestionnaire. La place n'est plus retenue.
     */
    ANNULE
}

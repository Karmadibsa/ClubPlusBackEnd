package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ReservationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des réservations.
 * <p>
 * Base URL: /reservations
 * </p>
 */
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@CrossOrigin
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);
    private final ReservationService reservationService;

    /**
     * Crée une nouvelle réservation pour l'utilisateur authentifié.
     * <p>
     * Endpoint: POST /reservations?eventId={eventId}&amp;categorieId={categorieId}
     * <p>
     * Accès réservé aux membres.
     *
     * @param eventId     L'ID de l'événement.
     * @param categorieId L'ID de la catégorie à réserver.
     * @return La nouvelle réservation créée (201 Created).
     */
    @PostMapping
    @IsMembre
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Reservation> createMyReservation(@RequestParam Integer eventId,
                                                           @RequestParam Integer categorieId) {
        Reservation newReservation = reservationService.createMyReservation(eventId, categorieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newReservation);
    }

    /**
     * Récupère les réservations de l'utilisateur authentifié, avec un filtre optionnel par statut.
     * <p>
     * Endpoint: GET /reservations/me?status={status}
     * <p>
     * Accès réservé à l'utilisateur authentifié.
     *
     * @param status (Optionnel) Filtre sur le statut ('CONFIRME', 'UTILISE', 'ANNULE').
     * @return La liste des réservations de l'utilisateur (200 OK).
     */
    @GetMapping("/me")
    @IsMembre
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<List<Reservation>> getMyReservations(@RequestParam(required = false) String status) {
        List<Reservation> reservations = reservationService.findMyReservations(status);
        logger.debug("Nombre de réservations trouvées pour l'utilisateur actuel : {}", reservations.size());
        return ResponseEntity.ok(reservations);
    }

    /**
     * Récupère une réservation par son ID.
     * <p>
     * Endpoint: GET /reservations/{id}
     * <p>
     * L'accès est autorisé si l'utilisateur est le propriétaire de la réservation
     * ou un gestionnaire du club organisateur.
     *
     * @param id L'ID de la réservation.
     * @return La réservation trouvée (200 OK).
     */
    @GetMapping("/{id}")
    @IsConnected
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Reservation> getReservationById(@PathVariable Integer id) {
        Reservation reservation = reservationService.getReservationByIdWithSecurityCheck(id);
        return ResponseEntity.ok(reservation);
    }

    /**
     * Récupère les réservations pour un événement, avec un filtre optionnel par statut.
     * <p>
     * Endpoint: GET /reservations/event/{eventId}?status={status}
     * <p>
     * Accès réservé aux gestionnaires (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param eventId L'ID de l'événement.
     * @param status  (Optionnel) Filtre sur le statut des réservations.
     * @return La liste des réservations pour l'événement (200 OK).
     */
    @GetMapping("/event/{eventId}")
    @IsReservation
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<List<Reservation>> getReservationsByEvent(@PathVariable Integer eventId,
                                                                    @RequestParam(required = false) String status) {
        List<Reservation> reservations = reservationService.findReservationsByEventIdWithSecurityCheck(eventId, status);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Récupère les réservations pour une catégorie d'événement spécifique.
     * <p>
     * Endpoint: GET /reservations/categorie/{categorieId}
     * <p>
     * Accès réservé aux gestionnaires (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param categorieId L'ID de la catégorie.
     * @return La liste des réservations pour la catégorie (200 OK).
     */
    @GetMapping("/categorie/{categorieId}")
    @IsReservation
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<List<Reservation>> getReservationsByCategorie(@PathVariable Integer categorieId) {
        List<Reservation> reservations = reservationService.findReservationsByCategorieIdWithSecurityCheck(categorieId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Annule une réservation.
     * <p>
     * Endpoint: PUT /reservations/{id}/cancel
     * <p>
     * L'action peut être effectuée par le propriétaire de la réservation ou un gestionnaire du club.
     *
     * @param id L'ID de la réservation à annuler.
     * @return Une réponse vide (204 No Content) en cas de succès.
     */
    @PutMapping("/{id}/cancel")
    @IsConnected
    public ResponseEntity<Void> cancelReservation(@PathVariable Integer id) {
        reservationService.cancelReservationById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque une réservation comme 'UTILISE' via son UUID (typiquement via QR Code).
     * <p>
     * Endpoint: PATCH /reservations/uuid/{uuid}/use
     * <p>
     * Accès réservé aux gestionnaires (RESERVATION ou ADMIN) du club organisateur.
     *
     * @param uuid L'UUID de la réservation.
     * @return La réservation mise à jour (200 OK).
     */
    @PatchMapping("/uuid/{uuid}/use")
    @IsReservation
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Reservation> markReservationUsed(@PathVariable String uuid) {
        Reservation updatedReservation = reservationService.markReservationAsUsed(uuid);
        return ResponseEntity.ok(updatedReservation);
    }
}

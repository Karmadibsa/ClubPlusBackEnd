package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.service.ReservationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final CategorieService categorieService;

    public ReservationController(ReservationService reservationService, CategorieService categorieService) {
        this.reservationService = reservationService;
        this.categorieService = categorieService;
    }

    @GetMapping
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<List<Reservation>> getAllReservations() {
        List<Reservation> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Reservation> getReservation(@PathVariable Long id) {
        return reservationService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/{idEvent}/{idCategory}")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Reservation> createReservation(@PathVariable Long idEvent, @PathVariable Long idCategory) {
        long membreId = 1;
        if (idEvent == null || idCategory == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return reservationService.createReservation(membreId, idEvent, idCategory)
                .map(reservation -> new ResponseEntity<>(reservation, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        if (reservationService.getReservationById(id).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        reservationService.deleteReservation(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/membre/{membreId}")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<List<Reservation>> getReservationsByMembre(@PathVariable Long membreId) {
        List<Reservation> reservations = reservationService.getReservationsByMembre(membreId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/event/{eventId}")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<List<Reservation>> getReservationsByEvent(@PathVariable Long eventId) {
        List<Reservation> reservations = reservationService.getReservationsByEvent(eventId);
        return ResponseEntity.ok(reservations);
    }

    @DeleteMapping("/membre/{membreId}/cancel/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long membreId, @PathVariable Long reservationId) {
        boolean success = reservationService.cancelReservation(membreId, reservationId);

        if (success) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/{id}/places-disponibles")
    @JsonView(GlobalView.ReservationView.class)
    public ResponseEntity<Map<String, Object>> getPlacesDisponibles(@PathVariable Long id) {
        Optional<Categorie> categorieOpt = categorieService.getCategorieById(id);

        if (categorieOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Categorie categorie = categorieOpt.get();
        Integer placesDisponibles = categorieService.getPlacesDisponibles(id);

        Map<String, Object> response = new HashMap<>();
        response.put("id", categorie.getId());
        response.put("nom", categorie.getNom());
        response.put("capacite", Integer.parseInt(String.valueOf(categorie.getCapacite())));
        response.put("disponibles", placesDisponibles);

        return ResponseEntity.ok(response);
    }


}

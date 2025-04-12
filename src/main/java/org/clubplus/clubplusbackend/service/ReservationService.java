package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Reservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final CategorieDao categorieRepository;
    private final CategorieService categorieService;
    int reservationMax = 2;

    /**
     * Crée une nouvelle réservation pour un membre à un événement/catégorie.
     *
     * @param membreId    L'ID du membre qui réserve.
     * @param eventId     L'ID de l'événement.
     * @param categorieId L'ID de la catégorie choisie.
     * @return La réservation créée.
     * @throws EntityNotFoundException  Si le membre, l'événement ou la catégorie n'est pas trouvé.
     * @throws IllegalArgumentException Si la catégorie n'appartient pas à l'événement.
     * @throws IllegalStateException    Si l'événement est déjà passé, si le membre a déjà réservé pour cet événement, ou si la capacité de la catégorie est atteinte.
     */
    public Reservation createReservation(Integer membreId, Integer eventId, Integer categorieId) {
        // 1. Récupérer les entités liées (inchangé)
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé avec l'ID : " + membreId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée avec l'ID : " + categorieId));

        // --- Validation Logique ---

        // 2. Vérifier si l'événement est futur (inchangé)
        if (event.getStart() != null && event.getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de réserver pour un événement déjà commencé ou passé.");
        }

        // 3. Vérifier si la catégorie appartient bien à l'événement (inchangé)
        if (!categorie.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("La catégorie ID " + categorieId + " n'appartient pas à l'événement ID " + eventId);
        }

        // 4. --- MODIFIÉ : Vérifier si le membre a déjà atteint la limite de 2 réservations pour cet événement ---
        long existingReservationsCount = reservationRepository.countByMembreIdAndEventId(membreId, eventId);
        if (existingReservationsCount >= reservationMax) {
            throw new IllegalStateException("Le membre ID " + membreId + " a déjà atteint le nombre maximum de 2 réservations pour l'événement ID " + eventId);
            // Ou : throw new AlreadyReservedException("Limite de 2 réservations atteinte pour cet événement.")
        }

        // 5. Vérifier la capacité de la catégorie (inchangé)
        if (categorie.getPlaceReserve() >= categorie.getCapacite()) {
            throw new IllegalStateException("La capacité maximale (" + categorie.getCapacite() + ") pour la catégorie '" + categorie.getNom() + "' est atteinte.");
            // Ou : throw new CapacityExceededException(...)
        }

        // --- Création de la Réservation --- (inchangé)
        Reservation reservation = new Reservation(membre, event, categorie);
        reservation.setDateReservation(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> findReservationById(Integer id) {
        // Le qrcodeurl sera généré lors de l'appel au getter sur l'objet retourné
        return reservationRepository.findById(id);
    }

    public Reservation getReservationByIdOrThrow(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée avec l'ID : " + id));
        // .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
    }

    public List<Reservation> findReservationsByMembreId(Integer membreId) {
        // Vérifier si le membre existe peut être une bonne pratique
        if (!membreRepository.existsById(membreId)) {
            throw new EntityNotFoundException("Membre non trouvé avec l'ID : " + membreId);
        }
        return reservationRepository.findByMembreId(membreId);
    }

    public List<Reservation> findReservationsByEventId(Integer eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId);
        }
        return reservationRepository.findByEventId(eventId);
    }

    public List<Reservation> findReservationsByCategorieId(Integer categorieId) {
        if (!categorieRepository.existsById(categorieId)) {
            throw new EntityNotFoundException("Catégorie non trouvée avec l'ID : " + categorieId);
        }
        return reservationRepository.findByCategorieId(categorieId);
    }


    /**
     * Supprime (annule) une réservation.
     *
     * @param reservationId L'ID de la réservation à annuler.
     * @param demandeurId   L'ID du membre qui demande l'annulation (pour vérification de droits).
     * @param isAdmin       Indique si le demandeur est un administrateur (peut annuler n'importe quoi).
     * @throws EntityNotFoundException Si la réservation n'est pas trouvée.
     * @throws SecurityException       Si le demandeur n'a pas le droit d'annuler cette réservation.
     * @throws IllegalStateException   Si l'événement est déjà passé.
     */
    public void deleteReservation(Integer reservationId, Integer demandeurId, boolean isAdmin) {
        Reservation reservation = getReservationByIdOrThrow(reservationId);

        // Vérification des droits d'annulation
        if (!isAdmin && !reservation.getMembre().getId().equals(demandeurId)) {
            throw new SecurityException("Le membre ID " + demandeurId + " n'est pas autorisé à annuler la réservation ID " + reservationId);
        }

        // Vérification si l'événement est passé (optionnel, on peut vouloir annuler même après)
        // if (reservation.getEvent().getStart() != null && reservation.getEvent().getStart().isBefore(LocalDateTime.now())) {
        //     throw new IllegalStateException("Impossible d'annuler une réservation pour un événement passé.");
        // }

        reservationRepository.delete(reservation);
    }
}

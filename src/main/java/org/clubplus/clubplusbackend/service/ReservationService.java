package org.clubplus.clubplusbackend.service;

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

import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final CategorieDao categorieRepository;

    public ReservationService(ReservationDao reservationRepository, MembreDao membreRepository, EventDao eventRepository, CategorieDao categorieRepository) {
        this.reservationRepository = reservationRepository;
        this.membreRepository = membreRepository;
        this.eventRepository = eventRepository;
        this.categorieRepository = categorieRepository;
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    public List<Reservation> getReservationsByMembre(Long membreId) {
        return reservationRepository.findByMembreId(membreId);
    }

    public List<Reservation> getReservationsByEvent(Long eventId) {
        return reservationRepository.findByEventId(eventId);
    }

    @Transactional
    public Optional<Reservation> createReservation(Long membreId, Long eventId, Long categorieId) {
        // Vérifier si le membre existe
        Optional<Membre> membreOpt = membreRepository.findById(membreId);
        if (membreOpt.isEmpty()) {
            return Optional.empty();
        }

        // Vérifier si l'événement existe
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return Optional.empty();
        }

        // Vérifier si la catégorie existe et appartient à l'événement
        Optional<Categorie> categorieOpt = categorieRepository.findById(categorieId);
        if (categorieOpt.isEmpty() || !categorieOpt.get().getEvent().getId().equals(eventId)) {
            return Optional.empty();
        }

        Membre membre = membreOpt.get();
        Event event = eventOpt.get();
        Categorie categorie = categorieOpt.get();

        // Vérifier si le membre a déjà 2 réservations pour cet événement
        int nbReservations = reservationRepository.countByMembreIdAndEventId(membreId, eventId);
        if (nbReservations >= 2) {
            return Optional.empty(); // Limite atteinte
        }

        // Vérifier s'il reste des places dans cette catégorie
        int capacite;
        try {
            capacite = Integer.valueOf(categorie.getCapacite());
        } catch (NumberFormatException e) {
            // Gérer le cas où la capacité n'est pas un nombre valide
            return Optional.empty(); // Impossible de déterminer la capacité
        }

        List<Reservation> reservationsCategorie = reservationRepository.findByCategorieId(categorieId);
        if (reservationsCategorie.size() >= capacite) {
            return Optional.empty(); // Plus de places disponibles
        }

        // Créer la réservation
        Reservation reservation = new Reservation(membre, event, categorie);
        return Optional.of(reservationRepository.save(reservation));
    }


    @Transactional
    public void deleteReservation(Long id) {
        reservationRepository.deleteById(id);
    }

    @Transactional
    public boolean cancelReservation(Long membreId, Long reservationId) {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);

        if (reservationOpt.isPresent() && reservationOpt.get().getMembre().getId().equals(membreId)) {
            reservationRepository.deleteById(reservationId);
            return true;
        }

        return false;
    }
}

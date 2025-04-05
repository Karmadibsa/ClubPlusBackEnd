package org.clubplus.clubplusbackend.service;

import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategorieService {
    private final CategorieDao categorieRepository;
    private final ReservationDao reservationRepository;
    private final EventDao eventRepository;

    public CategorieService(CategorieDao categorieRepository, ReservationDao reservationRepository, EventDao eventRepository) {
        this.categorieRepository = categorieRepository;
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
    }

    // Récupère tous les categories
    public List<Categorie> getAllCategories() {
        return categorieRepository.findAll();
    }

    // Récupère un categorie par son ID
    public Optional<Categorie> getCategorieById(Long id) {
        return categorieRepository.findById(id);
    }

    // Sauvegarde un nouveau categorie ou met à jour un existant
    public Categorie save(Categorie categorie) {
        return categorieRepository.save(categorie);
    }

    // Suppression d'un categorie
    public void deleteCategorie(Long id) {
        categorieRepository.deleteById(id);
    }

    // Vérifie si un categorie existe par son ID
    public boolean existsById(Long id) {
        return categorieRepository.existsById(id);
    }


//    Essai

    public List<Categorie> getCategoriesByEventId(Long eventId) {
        return categorieRepository.findByEventId(eventId);
    }

    public Optional<Categorie> addCategorieToEvent(Long eventId, Categorie categorie) {
        return eventRepository.findById(eventId).map(event -> {
            categorie.setEvent(event);
            return categorieRepository.save(categorie);
        });
    }

    // Méthode pour calculer les places disponibles
    public Integer getPlacesDisponibles(Long categorieId) {
        Optional<Categorie> categorieOpt = categorieRepository.findById(categorieId);
        if (categorieOpt.isEmpty()) {
            return 0;
        }

        Categorie categorie = categorieOpt.get();
        Integer capaciteInt = Integer.parseInt(String.valueOf(categorie.getCapacite()));
        int reservationCount = reservationRepository.findByCategorieId(categorieId).size();

        return Math.max(0, capaciteInt - reservationCount);
    }

    // Version alternative qui prend directement l'objet Categorie
    public Integer getPlacesDisponibles(Categorie categorie) {
        if (categorie == null) {
            return 0;
        }

        Integer capaciteInt = Integer.parseInt(String.valueOf(categorie.getCapacite()));
        int reservationCount = reservationRepository.findByCategorieId(categorie.getId()).size();

        return Math.max(0, capaciteInt - reservationCount);
    }
}


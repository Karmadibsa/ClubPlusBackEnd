package org.clubplus.clubplusbackend.service;

import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    private final EventDao eventRepository;

    public EventService(EventDao eventRepository) {
        this.eventRepository = eventRepository;
    }

    // Récupère tous les events
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    // Récupère un event par son ID
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    // Sauvegarde un nouveau event ou met à jour un existant
    public Event save(Event event) {
        return eventRepository.save(event);
    }

    // Suppression d'un event
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    // Vérifie si un event existe par son ID
    public boolean existsById(Long id) {
        return eventRepository.existsById(id);
    }

//    Essai pour les categorie

    public Optional<Event> updateEvent(Long id, Event eventDetails) {
        return eventRepository.findById(id).map(existingEvent -> {
            // Mettre à jour les propriétés de base
            existingEvent.setTitle(eventDetails.getTitle());
            existingEvent.setStart(eventDetails.getStart());
            existingEvent.setEnd(eventDetails.getEnd());
            existingEvent.setDescription(eventDetails.getDescription());
            existingEvent.setLocation(eventDetails.getLocation());

            // Gérer les catégories
            if (eventDetails.getCategories() != null) {
                existingEvent.getCategories().clear();
                for (Categorie categorie : eventDetails.getCategories()) {
                    categorie.setEvent(existingEvent);
                    existingEvent.getCategories().add(categorie);
                }
            }

            return eventRepository.save(existingEvent);
        });
    }

    public Event createEvent(Event event) {
        // Configurer la relation bidirectionnelle
        if (event.getCategories() != null) {
            for (Categorie categorie : event.getCategories()) {
                categorie.setEvent(event);
            }
        }
        return eventRepository.save(event);
    }
}

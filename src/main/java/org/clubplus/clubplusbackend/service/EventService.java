package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventDao eventRepository;
    private final ClubDao clubRepository; // Injecter si besoin de valider/lier l'organisateur

    public List<Event> findAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> findEventById(Integer id) {
        return eventRepository.findById(id);
    }

    public Event getEventByIdOrThrow(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + id));
    }

    public List<Event> findEventsByOrganisateur(Integer clubId) {
        return eventRepository.findByOrganisateurId(clubId);
    }

    public List<Event> findUpcomingEvents() {
        return eventRepository.findByStartAfter(LocalDateTime.now());
    }

    public Event createEvent(Event event, Integer organisateurId) {
        // 1. Valider les dates (end doit être après start)
        if (event.getStart() != null && event.getEnd() != null && !event.getEnd().isAfter(event.getStart())) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début.");
        }

        // 2. Vérifier et lier l'organisateur
        Club organisateur = clubRepository.findById(organisateurId)
                .orElseThrow(() -> new EntityNotFoundException("Club organisateur non trouvé avec l'ID : " + organisateurId));
        event.setOrganisateur(organisateur);

        // 3. Gérer les catégories associées (important !)
        // Assurer la cohérence bidirectionnelle si les catégories sont envoyées dans la requête
        if (event.getCategories() != null) {
            for (Categorie categorie : event.getCategories()) {
                categorie.setEvent(event); // Lier chaque catégorie à cet événement
            }
        } else {
            // Initialiser la liste si elle est nulle pour éviter les NullPointerException plus tard
            event.setCategories(new java.util.ArrayList<>());
        }

        // Les listes de réservations et notations sont généralement gérées par d'autres opérations
        // Initialiser les listes si elles sont nulles
        if (event.getReservations() == null) {
            event.setReservations(new java.util.ArrayList<>());
        }
        if (event.getNotations() == null) {
            event.setNotations(new java.util.ArrayList<>());
        }


        return eventRepository.save(event);
    }

    public Event updateEvent(Integer id, Event eventDetails) {
        Event existingEvent = getEventByIdOrThrow(id);

        // Mettre à jour les champs simples
        existingEvent.setNom(eventDetails.getNom());
        existingEvent.setDescription(eventDetails.getDescription());
        existingEvent.setLocation(eventDetails.getLocation());

        // Mettre à jour les dates (avec validation)
        if (eventDetails.getStart() != null && eventDetails.getEnd() != null) {
            if (!eventDetails.getEnd().isAfter(eventDetails.getStart())) {
                throw new IllegalArgumentException("La date de fin doit être après la date de début.");
            }
            existingEvent.setStart(eventDetails.getStart());
            existingEvent.setEnd(eventDetails.getEnd());
        } else {
            // Gérer les cas où seulement start ou end est fourni, si nécessaire
            if (eventDetails.getStart() != null) existingEvent.setStart(eventDetails.getStart());
            if (eventDetails.getEnd() != null) existingEvent.setEnd(eventDetails.getEnd());
            // Re-valider si une seule date est changée
            if (existingEvent.getStart() != null && existingEvent.getEnd() != null && !existingEvent.getEnd().isAfter(existingEvent.getStart())) {
                throw new IllegalArgumentException("Incohérence de dates après mise à jour partielle.");
            }
        }

        return eventRepository.save(existingEvent);
    }

    public void deleteEvent(Integer id) {
        Event event = getEventByIdOrThrow(id); // Vérifie l'existence avant suppression
        // Logique de validation avant suppression (ex: vérifier s'il y a des réservations actives ?)
        if (!event.getReservations().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer un événement avec des réservations existantes.");
        }
        eventRepository.delete(event);
    }

    public List<Event> findUpcomingEventsByOrganisateur(Integer clubId) {
        // Optionnel : Vérifier d'abord si le club existe
        if (!clubRepository.existsById(clubId)) {
            // Vous pouvez soit retourner une liste vide, soit lancer une exception
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findByOrganisateurIdAndStartAfter(clubId, now);
    }
    // Méthodes supplémentaires possibles :
    // - Ajouter une catégorie à un événement
    // - Supprimer une catégorie d'un événement
    // - Récupérer les réservations d'un événement
    // - Récupérer les notations d'un événement
}

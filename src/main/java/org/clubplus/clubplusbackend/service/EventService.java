package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventDao eventRepository;
    private final ClubDao clubRepository;
    private final SecurityService securityService; // Injecter

    // --- Méthodes de Lecture Publiques (Certaines avec Sécurité) ---

    /**
     * Récupère tous les événements (sans filtre sécurité par défaut).
     */
    @Transactional(readOnly = true)
    public List<Event> findAllEvents() {
        // Potentiellement ajouter un filtre si on ne veut montrer que les events des clubs de l'user ?
        return eventRepository.findAll();
    }

    /**
     * Récupère un événement par ID ou lance 404.
     */
    @Transactional(readOnly = true)
    public Event getEventByIdOrThrow(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un événement par ID et vérifie si l'utilisateur est membre du club organisateur.
     * Lance 404 (Event non trouvé) ou 403 (Non membre).
     */
    @Transactional(readOnly = true)
    public Event getEventByIdWithSecurityCheck(Integer eventId) {
        Event event = getEventByIdOrThrow(eventId); // Lance 404 si non trouvé
        // Vérification Sécurité Contextuelle
        securityService.checkIsCurrentUserMemberOfClubOrThrow(event.getOrganisateur().getId()); // Lance 403 si non membre
        return event;
    }

    /**
     * Récupère les événements d'un organisateur et vérifie si l'utilisateur est membre.
     * Lance 404 (Club non trouvé via eventId logique) ou 403 (Non membre).
     */
    @Transactional(readOnly = true)
    public List<Event> findEventsByOrganisateurWithSecurityCheck(Integer clubId) {
        // 1. Vérifier l'existence du club pour un message d'erreur clair si clubId invalide.
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        // 2. Vérifier la sécurité contextuelle : l'utilisateur est-il membre de ce club ?
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance 403 si non membre

        // 3. Récupérer les événements
        return eventRepository.findByOrganisateurId(clubId);
    }

    /**
     * Récupère les événements FUTURS d'un organisateur et vérifie si l'utilisateur est membre.
     * Lance 404 (Club non trouvé) ou 403 (Non membre).
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsByOrganisateurWithSecurityCheck(Integer clubId) {
        // 1. Vérifier l'existence du club.
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        // 2. Vérifier la sécurité contextuelle.
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance 403 si non membre

        // 3. Récupérer les événements futurs.
        return eventRepository.findByOrganisateurIdAndStartAfter(clubId, LocalDateTime.now());
    }

    /**
     * Récupère tous les événements futurs (sans filtre sécurité).
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEvents() {
        return eventRepository.findByStartAfter(LocalDateTime.now());
    }

    /**
     * Récupère les événements futurs des clubs dont l'utilisateur est membre.
     * Utilise l'ID de l'utilisateur courant via SecurityService.
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsForMemberClubs() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Lance si non authentifié

        // Récupérer les IDs des clubs du membre (suppose une méthode dans AdhesionDao ou MembreService)
        List<Integer> memberClubIds = findClubIdsForMember(currentUserId); // Méthode helper à implémenter

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList();
        }
        return eventRepository.findByOrganisateurIdInAndStartAfter(memberClubIds, LocalDateTime.now());
    }

    // Méthode helper (à placer ici ou dans MembreService/AdhesionDao)
    private List<Integer> findClubIdsForMember(Integer membreId) {
        // Exemple d'implémentation (à adapter selon votre DAO)
        // return adhesionRepository.findByMembreId(membreId).stream()
        //         .map(adhesion -> adhesion.getClub().getId())
        //         .collect(Collectors.toList());
        return List.of(); // Placeholder
    }


    // --- Méthodes d'Écriture (CRUD avec Sécurité) ---

    /**
     * Crée un nouvel événement pour un club.
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     * Lance 404 (Club non trouvé), 403 (Non manager), 400 (Données invalides).
     */
    public Event createEvent(Integer clubId, Event eventData) {
        // 1. Vérifier et récupérer le club organisateur
        Club organisateur = clubRepository.findById(clubId)
                .orElseThrow(() -> new EntityNotFoundException("Club organisateur non trouvé avec l'ID " + clubId)); // -> 404

        // 2. Vérification Sécurité Contextuelle : Est-ce un manager DE CE club ?
        securityService.checkManagerOfClubOrThrow(clubId); // Lance 403 si non manager

        // 3. Validation Métier (Dates)
        if (eventData.getStart() == null || eventData.getEnd() == null || eventData.getEnd().isBefore(eventData.getStart())) {
            throw new IllegalArgumentException("Dates de début/fin invalides ou manquantes."); // -> 400
        }
        // Autres validations si nécessaire (nom non vide, etc.) - @Valid dans le contrôleur gère déjà beaucoup

        // 4. Création et sauvegarde
        Event newEvent = new Event();
        newEvent.setNom(eventData.getNom());
        newEvent.setStart(eventData.getStart());
        newEvent.setEnd(eventData.getEnd());
        newEvent.setDescription(eventData.getDescription());
        newEvent.setLocation(eventData.getLocation());
        newEvent.setOrganisateur(organisateur); // Lier au club
        newEvent.setCategories(new ArrayList<>()); // Initialiser collections
        newEvent.setNotations(new ArrayList<>());
        newEvent.setId(null);

        return eventRepository.save(newEvent);
    }

    /**
     * Met à jour un événement.
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     * Lance 404 (Event non trouvé), 403 (Non manager), 400 (Données invalides).
     */
    public Event updateEvent(Integer eventId, Event eventDetails) {
        // 1. Récupérer l'événement existant
        Event existingEvent = getEventByIdOrThrow(eventId); // Lance 404 si non trouvé

        // 2. Vérification Sécurité Contextuelle
        securityService.checkManagerOfClubOrThrow(existingEvent.getOrganisateur().getId()); // Lance 403 si non manager

        boolean updated = false;

        // 3. Mise à jour des champs (avec validation)
        if (eventDetails.getNom() != null && !eventDetails.getNom().isBlank()) {
            existingEvent.setNom(eventDetails.getNom());
            updated = true;
        }

        LocalDateTime newStart = eventDetails.getStart();
        LocalDateTime newEnd = eventDetails.getEnd();
        if (newStart != null || newEnd != null) { // Si au moins une date est fournie
            LocalDateTime checkStart = (newStart != null) ? newStart : existingEvent.getStart();
            LocalDateTime checkEnd = (newEnd != null) ? newEnd : existingEvent.getEnd();
            if (checkEnd.isBefore(checkStart)) {
                throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début."); // -> 400
            }
            if (newStart != null) {
                existingEvent.setStart(newStart);
                updated = true;
            }
            if (newEnd != null) {
                existingEvent.setEnd(newEnd);
                updated = true;
            }
        }

        if (eventDetails.getDescription() != null) { // Permet de mettre à jour avec une description vide/nulle si besoin
            existingEvent.setDescription(eventDetails.getDescription());
            updated = true;
        }
        if (eventDetails.getLocation() != null) { // Idem pour la localisation
            existingEvent.setLocation(eventDetails.getLocation());
            updated = true;
        }

        // 4. Sauvegarder si nécessaire
        if (updated) {
            return eventRepository.save(existingEvent);
        }
        return existingEvent;
    }

    /**
     * Supprime un événement.
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     * Règle métier: Empêche la suppression s'il existe des réservations.
     * Lance 404 (Event non trouvé), 403 (Non manager), 409 (Réservations existent).
     */
    public void deleteEvent(Integer eventId) {
        // 1. Récupérer l'événement et ses dépendances pour la vérification
        Event eventToDelete = eventRepository.findByIdFetchingCategoriesAndReservations(eventId) // Utilise la requête avec FETCH
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId)); // -> 404

        // 2. Vérification Sécurité Contextuelle
        securityService.checkManagerOfClubOrThrow(eventToDelete.getOrganisateur().getId()); // Lance 403 si non manager

        // 3. Validation Métier : Vérifier l'existence de réservations
        boolean hasReservations = eventToDelete.getCategories().stream()
                .anyMatch(categorie -> !categorie.getReservations().isEmpty());

        if (hasReservations) {
            throw new IllegalStateException("Impossible de supprimer l'événement car il contient des réservations."); // -> 409 Conflict
        }

        // 4. Si tout est OK, supprimer l'événement
        // Les cascades s'occuperont des Catégories et Notations (si configurées avec CascadeType.ALL)
        eventRepository.delete(eventToDelete);
    }
}

package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
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
    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final ClubDao clubRepository;
    private final SecurityService securityService; // Injecter

    // --- Méthodes de Lecture Publiques (Certaines avec Sécurité) ---

    /**
     * Récupère une liste d'événements, potentiellement filtrée par statut.
     *
     * @param statusFilter "active", "inactive", ou null/"all" pour tout récupérer. Par défaut (null), retourne les actifs.
     * @return La liste des événements filtrée.
     */
    @Transactional(readOnly = true)
    public List<Event> findAllEvents(String statusFilter) { // Signature modifiée
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByActif(false);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findAll(); // Récupère tout
        } else { // "active" ou null (comportement par défaut)
            return eventRepository.findByActif(true);
        }
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
     * Récupère les événements d'un organisateur, avec sécurité et filtrage par statut.
     * Par défaut (statusFilter = null), retourne uniquement les événements actifs.
     *
     * @param clubId       L'ID du club organisateur.
     * @param statusFilter "active", "inactive", ou null/"all" (null retourne actifs par défaut).
     * @return Liste filtrée des événements.
     */
    @Transactional(readOnly = true)
    // Signature modifiée pour inclure statusFilter
    public List<Event> findEventsByOrganisateurWithSecurityCheck(Integer clubId, String statusFilter) {
        if (!clubRepository.existsById(clubId)) { // Garde la vérification d'existence du club
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        // Appliquer le filtre
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdAndActif(clubId, false);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurId(clubId); // Récupère tout
        } else { // "active" ou null
            return eventRepository.findByOrganisateurIdAndActif(clubId, true);
        }
    }

    /**
     * Récupère les événements FUTURS d'un organisateur, avec sécurité et filtrage par statut.
     * Par défaut (statusFilter = null), retourne uniquement les événements futurs ACTIFS.
     *
     * @param clubId       L'ID du club organisateur.
     * @param statusFilter "active", "inactive", ou null/"all" (null retourne actifs par défaut).
     * @return Liste filtrée des événements futurs.
     */
    @Transactional(readOnly = true)
    // Signature modifiée pour inclure statusFilter
    public List<Event> findUpcomingEventsByOrganisateurWithSecurityCheck(Integer clubId, String statusFilter) {
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        LocalDateTime now = LocalDateTime.now();
        // Appliquer le filtre
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            // Trouve les futurs inactifs
            return eventRepository.findByOrganisateurIdAndActifAndStartAfter(clubId, false, now);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            // Trouve tous les futurs (actifs et inactifs)
            return eventRepository.findByOrganisateurIdAndStartAfter(clubId, now); // Méthode DAO existante
        } else { // "active" ou null
            // Trouve les futurs actifs
            return eventRepository.findByOrganisateurIdAndActifAndStartAfter(clubId, true, now);
        }
    }


    /**
     * Récupère tous les événements futurs, potentiellement filtrés par statut.
     * Par défaut (null), retourne les actifs.
     *
     * @param statusFilter "active", "inactive", ou null/"all".
     * @return Liste filtrée des événements futurs.
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEvents(String statusFilter) { // Signature modifiée
        LocalDateTime now = LocalDateTime.now();
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByActifAndStartAfter(false, now);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByStartAfter(now); // Méthode DAO existante
        } else { // "active" ou null
            return eventRepository.findByActifAndStartAfter(true, now);
        }
    }

    /**
     * Récupère les événements futurs des clubs dont l'utilisateur est membre,
     * potentiellement filtrés par statut.
     * Par défaut (statusFilter = null ou "all"), retourne TOUS les événements futurs (actifs et inactifs).
     *
     * @param statusFilter "active", "inactive", ou null/"all".
     * @return Liste filtrée des événements futurs.
     */
    @Transactional(readOnly = true)
    // Ajouter le paramètre statusFilter
    public List<Event> findUpcomingEventsForMemberClubs(String statusFilter) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        List<Integer> memberClubIds = findClubIdsForMember(currentUserId); // Utilise la méthode helper

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();

        // Appliquer le filtre basé sur le paramètre statusFilter
        if ("active".equalsIgnoreCase(statusFilter)) {
            // Récupère seulement les futurs actifs
            return eventRepository.findByOrganisateurIdInAndActifAndStartAfter(memberClubIds, true, now);
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            // Récupère seulement les futurs inactifs (annulés)
            return eventRepository.findByOrganisateurIdInAndActifAndStartAfter(memberClubIds, false, now);
        } else { // null, "all", ou une valeur invalide -> retourne TOUT (comportement par défaut)
            // Récupère tous les futurs pour ces clubs (actifs et inactifs)
            return eventRepository.findByOrganisateurIdInAndStartAfter(memberClubIds, now);
        }
    }

    // --- Méthode Helper ---
    // Assure-toi que cette méthode est correctement implémentée
    private List<Integer> findClubIdsForMember(Integer membreId) {

        Membre membre = membreRepository.findById(membreId)
                .orElse(null); // Gère le cas où le membre n'est pas trouvé

        if (membre == null || membre.getAdhesions() == null) {
            return Collections.emptyList();
        }

        return adhesionRepository.findClubIdsByMembreId(membreId); // Si cette méthode existe
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
        if (!organisateur.getActif()) { // Utilise le getter généré par Lombok
            throw new IllegalStateException("Impossible de créer un événement pour un club désactivé (ID: " + clubId + ")."); // -> 409
        }
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
        newEvent.setActif(true);
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
        if (!existingEvent.getActif()) {
            throw new IllegalStateException("Impossible de modifier un événement déjà annulé (ID: " + eventId + ")."); // -> 409
        }
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
     * Désactive (annule) un événement.
     * Remplace la suppression physique par une désactivation logique.
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     * N'est PLUS bloqué par l'existence de réservations.
     *
     * @param eventId L'ID de l'événement à désactiver.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws SecurityException       (ou similaire) si l'utilisateur n'est pas manager du club.
     */
    @Transactional
    public void deactivateEvent(Integer eventId) {
        // 1. Récupérer l'événement (actif ou inactif, peu importe ici, on veut le désactiver s'il est actif)
        Event eventToDeactivate = getEventByIdOrThrow(eventId);

        // Si l'événement est déjà inactif, on peut soit sortir, soit continuer (idempotence)
        if (!eventToDeactivate.getActif()) {
            // Optionnel: logguer ou simplement retourner
            System.out.println("L'événement " + eventId + " est déjà inactif.");
            return;
        }

        // 2. Vérification Sécurité Contextuelle
        securityService.checkManagerOfClubOrThrow(eventToDeactivate.getOrganisateur().getId());

        // 4. Procéder à la Désactivation Logique
        eventToDeactivate.prepareForDeactivation(); // Modifie nom, met date
        eventToDeactivate.setActif(false);          // Met le flag à false
        eventRepository.save(eventToDeactivate);    // Sauvegarde (UPDATE)
    }

}

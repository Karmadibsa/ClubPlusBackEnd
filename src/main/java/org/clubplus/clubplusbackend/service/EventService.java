package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.CreateEventDto;
import org.clubplus.clubplusbackend.dto.UpdateEventDto;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public Event createEvent(Integer organisateurId, CreateEventDto eventDto) {
        // 1. Sécurité: Vérifier si l'utilisateur courant est manager du club organisateur
        securityService.checkManagerOfClubOrThrow(organisateurId);

        // 2. Récupérer le club organisateur
        Club organisateur = clubRepository.findById(organisateurId)
                .orElseThrow(() -> new IllegalStateException("Probleme au niveau de la recuperation du club"));

        // 3. Créer la nouvelle entité Event
        Event newEvent = new Event();

        // 4. Mapper les champs du DTO vers l'entité
        newEvent.setNom(eventDto.getNom());
        newEvent.setStart(eventDto.getStart());
        newEvent.setEnd(eventDto.getEnd());
        newEvent.setDescription(eventDto.getDescription());
        newEvent.setLocation(eventDto.getLocation());

        // 5. Définir l'organisateur et l'état initial
        newEvent.setOrganisateur(organisateur); // <-- L'assignation clé !
        newEvent.setActif(true); // Par défaut, un nouvel événement est actif

        // 6. Validation métier supplémentaire (ex: start < end)
        if (newEvent.getStart().isAfter(newEvent.getEnd())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin.");
            // Ou une exception plus spécifique gérée par GlobalExceptionHandler -> 400
        }

        // 7. Sauvegarder
        return eventRepository.save(newEvent);
    }

    /**
     * Met à jour un événement.
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     * Lance 404 (Event non trouvé), 403 (Non manager), 400 (Données invalides).
     */
    public Event updateEvent(Integer eventId, UpdateEventDto eventDto) {
        // 1. Récupérer l'entité Event existante
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("L'event non recuperé"));

        // 2. Sécurité: Vérifier si l'utilisateur est manager du club organisateur
        //    (Suppose que Event a une relation `getOrganisateur()` vers Club)
        if (existingEvent.getOrganisateur() == null) {
            // Cas étrange, un événement devrait toujours avoir un organisateur
            throw new IllegalStateException("L'événement ID " + eventId + " n'a pas d'organisateur défini.");
        }
        securityService.checkManagerOfClubOrThrow(existingEvent.getOrganisateur().getId());
        // Ou si vous avez/préférez : securityService.checkManagerOfEventClubOrThrow(eventId);

        // 3. Vérifications métier (exemples, à adapter) :
        //    - Ne pas modifier un événement passé ?
        if (existingEvent.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de modifier un événement déjà terminé.");
        }
        //    - Vérifier start < end pour les nouvelles dates
        if (eventDto.getStart().isAfter(eventDto.getEnd())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin.");
            // Ou une exception plus spécifique gérée par GlobalExceptionHandler -> 400
        }
        //    - Vérifier si la mise à jour affecte des réservations existantes de manière problématique ? (logique complexe)

        // 4. Mapper les champs du DTO vers l'entité EXISTANTE
        //    IMPORTANT: Ne modifiez QUE les champs présents dans le DTO.
        existingEvent.setNom(eventDto.getNom());
        existingEvent.setStart(eventDto.getStart());
        existingEvent.setEnd(eventDto.getEnd());
        existingEvent.setDescription(eventDto.getDescription());
        existingEvent.setLocation(eventDto.getLocation());
        // Ne touchez PAS à existingEvent.setId(), existingEvent.setOrganisateur(),
        // existingEvent.setActif() (sauf si c'est une action dédiée), etc.

        // 5. Sauvegarder l'entité mise à jour
        return eventRepository.save(existingEvent);
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

    public List<Event> findAllEventsForMemberClubs(String status) {
        // 1. Récupérer l'utilisateur courant
        Membre currentUser = securityService.getCurrentMembreOrThrow();

        // 2. Récupérer les IDs des clubs dont l'utilisateur est membre actif
        // (Attention: assurez-vous que l'adhésion et le club sont actifs si nécessaire)
        Set<Integer> memberClubIds = currentUser.getAdhesions().stream()
                .filter(adhesion -> adhesion.getClub() != null && adhesion.getClub().getActif()) // Filtre optionnel sur club actif
                .map(adhesion -> adhesion.getClub().getId())
                .collect(Collectors.toSet());

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList(); // L'utilisateur n'est membre d'aucun club actif
        }

        // 3. Appeler le Repository pour trouver les événements de ces clubs
        //    en fonction du statut demandé (actif, inactif, all)
        if ("all".equalsIgnoreCase(status)) {
            return eventRepository.findByOrganisateurIdIn(memberClubIds); // Méthode à créer dans EventRepository
        } else if ("inactive".equalsIgnoreCase(status)) {
            return eventRepository.findByOrganisateurIdInAndActifIsFalse(memberClubIds); // Méthode à créer
        } else { // "active" ou par défaut
            return eventRepository.findByOrganisateurIdInAndActifIsTrue(memberClubIds); // Méthode à créer
        }
        // Ajoutez la logique de tri si nécessaire (par date par exemple)
    }
}

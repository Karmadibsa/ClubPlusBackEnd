package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.*;
import org.clubplus.clubplusbackend.dto.*;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventDao eventRepository;
    private final CategorieDao categorieRepository;
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
        Membre currentUser = securityService.getCurrentMembreOrThrow();

        Set<Integer> memberClubIds = currentUser.getAdhesions().stream()
                .filter(adhesion -> adhesion.getClub() != null && adhesion.getClub().getActif())
                .map(adhesion -> adhesion.getClub().getId())
                .collect(Collectors.toSet());

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();

        if ("all".equalsIgnoreCase(status)) {
            return eventRepository.findByOrganisateurIdInAndStartAfter(memberClubIds, now);
        } else if ("inactive".equalsIgnoreCase(status)) {
            return eventRepository.findByOrganisateurIdInAndActifIsFalseAndStartAfter(memberClubIds, now);
        } else { // "active" ou par défaut
            return eventRepository.findByOrganisateurIdInAndActifIsTrueAndStartAfter(memberClubIds, now);
        }
    }


    /**
     * Récupère les 5 prochains événements actifs du club géré par l’utilisateur connecté.
     *
     * @return Liste des 5 prochains événements.
     * @throws AccessDeniedException si l’utilisateur n’est pas autorisé ou pas associé à un club.
     */
    @Transactional(readOnly = true)
    public List<Event> getNextFiveEventsForManagedClub() {
        // Récupérer l’ID du club géré par l’utilisateur connecté
        Integer clubId = securityService.getCurrentUserManagedClubIdOrThrow();

        // Date actuelle pour filtrage
        LocalDateTime now = LocalDateTime.now();

        // Requête pour récupérer les 5 prochains événements actifs
        return eventRepository.findTop5ByOrganisateurIdAndActifTrueAndStartAfterOrderByStartAsc(clubId, now);
    }

    /**
     * Crée un nouvel événement AVEC ses catégories initiales.
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     */
    @Transactional // Transaction pour l'ensemble de l'opération
    public Event createEventWithCategories(Integer organisateurId, CreateEventWithCategoriesDto dto) {
        // 1. Sécurité
        securityService.checkManagerOfClubOrThrow(organisateurId);

        // 2. Récupérer le club
        Club organisateur = clubRepository.findById(organisateurId)
                .orElseThrow(() -> new EntityNotFoundException("Club organisateur non trouvé avec l'ID : " + organisateurId));

        // 3. Validation dates
        if (dto.getStart().isAfter(dto.getEnd())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin.");
        }

        // 4. Créer l'entité Event et mapper les champs
        Event newEvent = new Event();
        newEvent.setNom(dto.getNom());
        newEvent.setStart(dto.getStart());
        newEvent.setEnd(dto.getEnd());
        newEvent.setDescription(dto.getDescription());
        newEvent.setLocation(dto.getLocation());
        newEvent.setOrganisateur(organisateur);
        newEvent.setActif(true); // Nouvel événement est actif

        // 5. Créer et associer les catégories
        if (dto.getCategories() != null) {
            for (CreateCategorieDto catDto : dto.getCategories()) {
                // Validation nom unique pour ce nouvel événement (au sein de la transaction)
                if (newEvent.getCategories().stream().anyMatch(c -> c.getNom().equalsIgnoreCase(catDto.getNom()))) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' est dupliqué dans la requête de création."); // 400
                }

                Categorie newCategory = new Categorie();
                newCategory.setNom(catDto.getNom());
                newCategory.setCapacite(catDto.getCapacite());
                newCategory.setEvent(newEvent); // Lier à l'événement
                newEvent.getCategories().add(newCategory); // Ajouter à la collection de l'événement
            }
        }

        // 6. Sauvegarder (CascadeType.ALL s'occupe des catégories)
        return eventRepository.save(newEvent);
    }


    /**
     * Met à jour un événement ET ses catégories (ajout/modif/suppression).
     * Sécurité: Vérifie que l'utilisateur est MANAGER du club organisateur.
     */
    @Transactional // Transaction pour l'ensemble de l'opération
    public Event updateEventWithCategories(Integer eventId, UpdateEventWithCategoriesDto dto) {
        // 1. Récupérer l'événement existant AVEC ses catégories
        Event existingEvent = eventRepository.findByIdFetchingCategoriesWithJoinFetch(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        // 2. Sécurité
        securityService.checkManagerOfClubOrThrow(existingEvent.getOrganisateur().getId());

        // 3. Validation métier sur l'événement
        if (existingEvent.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de modifier un événement déjà terminé."); // 409
        }
        if (!existingEvent.getActif()) {
            throw new IllegalStateException("Impossible de modifier un événement annulé."); // 409
        }
        if (dto.getStart().isAfter(dto.getEnd())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin."); // 400
        }

        // 4. Mettre à jour les champs simples de l'événement
        existingEvent.setNom(dto.getNom());
        existingEvent.setStart(dto.getStart());
        existingEvent.setEnd(dto.getEnd());
        existingEvent.setDescription(dto.getDescription());
        existingEvent.setLocation(dto.getLocation());

        // 5. Réconcilier les catégories
        reconcileCategories(existingEvent, dto.getCategories());

        // 6. Sauvegarder (Cascade gère les updates, orphanRemoval gère les deletes)
        return eventRepository.save(existingEvent);
    }

    private void reconcileCategories(Event event, List<UpdateCategorieDto> categoryDtos) {
        // Map des catégories existantes pour accès rapide par ID
        Map<Integer, Categorie> existingCategoriesMap = event.getCategories().stream()
                .collect(Collectors.toMap(Categorie::getId, Function.identity()));

        // Ensemble des IDs des catégories dans le DTO (pour identifier les suppressions)
        Set<Integer> dtoCategoryIds = categoryDtos.stream()
                .map(UpdateCategorieDto::getId)
                .filter(Objects::nonNull) // Ignorer les nouvelles catégories ici
                .collect(Collectors.toSet());

        // --- Suppression (grâce à orphanRemoval=true) ---
        // Supprime de la collection les catégories qui ne sont PAS dans le DTO
        boolean removed = event.getCategories().removeIf(cat -> !dtoCategoryIds.contains(cat.getId()));
        if (removed) {
            // Si la suppression échoue à cause de réservations, une exception sera levée
            // par la contrainte de clé étrangère ou une logique métier dans un listener/callback.
            // Il est préférable de vérifier AVANT si possible (voir CategorieService.deleteCategorie)
            // Mais pour orphanRemoval, la vérification explicite est plus complexe ici.
            // S'assurer que la suppression de Categorie cascade bien vers Reservation
            // ou que deleteCategorie lève une exception si des réservations existent.
            System.out.println("Des catégories ont été marquées pour suppression via orphanRemoval.");
        }


        // --- Ajout & Mise à jour ---
        for (UpdateCategorieDto catDto : categoryDtos) {
            if (catDto.getId() == null) {
                // --- AJOUT ---
                // Vérifier nom unique au sein du DTO ET par rapport aux existantes non supprimées
                if (event.getCategories().stream().anyMatch(c -> c.getNom().equalsIgnoreCase(catDto.getNom())) ||
                        categoryDtos.stream().filter(d -> d.getId() == null && !d.equals(catDto)).anyMatch(d -> d.getNom().equalsIgnoreCase(catDto.getNom()))) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' est dupliqué.");
                }

                Categorie newCategory = new Categorie();
                newCategory.setNom(catDto.getNom());
                newCategory.setCapacite(catDto.getCapacite());
                newCategory.setEvent(event);
                event.getCategories().add(newCategory); // Ajouter à la collection

            } else {
                // --- MISE A JOUR ---
                Categorie categoryToUpdate = existingCategoriesMap.get(catDto.getId());
                if (categoryToUpdate == null) {
                    // L'ID fourni dans le DTO ne correspond à aucune catégorie existante de cet événement
                    // OU elle a été supprimée dans l'étape précédente. Dans ce dernier cas, c'est ok.
                    // Si elle n'a jamais existé ou n'appartenait pas à cet event, c'est une erreur.
                    // On peut choisir d'ignorer ou lever une exception. Ignorer est plus simple.
                    System.out.println("Tentative de mise à jour d'une catégorie (ID: " + catDto.getId() + ") non trouvée ou déjà supprimée.");
                    continue; // Passe à la catégorie suivante du DTO
                }

                // Vérifier nom unique (excluant elle-même)
                String newNom = catDto.getNom();
                if (!newNom.equalsIgnoreCase(categoryToUpdate.getNom())) {
                    // Vérifier si le nouveau nom existe déjà parmi les autres catégories
                    if (event.getCategories().stream().anyMatch(c -> !c.getId().equals(catDto.getId()) && c.getNom().equalsIgnoreCase(newNom)) ||
                            categoryDtos.stream().anyMatch(d -> !Objects.equals(d.getId(), catDto.getId()) && d.getNom().equalsIgnoreCase(newNom))) {
                        throw new IllegalArgumentException("Le nom de catégorie '" + newNom + "' existe déjà.");
                    }
                    categoryToUpdate.setNom(newNom);
                }

                // Mettre à jour la capacité (avec vérification)
                Integer newCapacite = catDto.getCapacite();
                if (!newCapacite.equals(categoryToUpdate.getCapacite())) {
                    // Récupérer les réservations confirmées pour cette catégorie
                    // Idéalement, charger les réservations ici ou utiliser une query
                    int placesConfirmees = categorieRepository.countConfirmedReservations(categoryToUpdate.getId()); // Méthode à ajouter au DAO
                    if (newCapacite < placesConfirmees) {
                        throw new IllegalStateException("Impossible de réduire la capacité à " + newCapacite +
                                " pour la catégorie '" + categoryToUpdate.getNom() + "' car " +
                                placesConfirmees + " places sont confirmées."); // 409
                    }
                    categoryToUpdate.setCapacite(newCapacite);
                }
            }
        }
    }

    public Page<Event> getAllEvents(int page,
                                    int size,
                                    String sortBy,
                                    String sortOrder) {
        var sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        var pageable = PageRequest.of(page, size, sort);
        return eventRepository.findAll(pageable);

    }

    public Page<Event> findByOrganisateurIdAndDate(
            Integer organisateurId,
            LocalDateTime dateStart,
            LocalDateTime dateEnd,
            int page,
            int size,
            String sortBy,
            String sortOrder
    ) {
        var sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        var pageable = PageRequest.of(page, size, sort);

        return eventRepository.findByOrganisateurIdAndDate(
                organisateurId,
                dateStart,
                dateEnd,
                (Pageable) pageable);
    }
}

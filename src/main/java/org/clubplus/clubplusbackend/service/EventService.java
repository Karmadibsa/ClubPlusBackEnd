package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.*;
import org.clubplus.clubplusbackend.dto.*;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier pour les {@link Event}s.
 * <p>
 * Ce service orchestre les opérations CRUD, les recherches filtrées et applique les règles
 * de sécurité relatives aux événements et à l'appartenance aux clubs.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final CategorieDao categorieRepository;
    private final DemandeAmiDao demandeAmiRepository;
    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final ClubDao clubRepository;
    private final SecurityService securityService;

    /**
     * Récupère les événements en fonction d'un filtre de statut.
     *
     * @param statusFilter Filtre "active", "inactive", ou "all". "active" par défaut.
     * @return Une liste d'événements.
     */
    @Transactional(readOnly = true)
    public List<Event> findAllEvents(String statusFilter) {
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByActif(false);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findAll();
        } else {
            return eventRepository.findByActif(true);
        }
    }

    /**
     * Récupère un événement par son ID.
     *
     * @param id L'ID de l'événement.
     * @return L'entité {@link Event} correspondante.
     * @throws EntityNotFoundException si aucun événement n'est trouvé.
     */
    @Transactional(readOnly = true)
    public Event getEventByIdOrThrow(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un événement par son ID, en vérifiant que l'utilisateur courant est membre du club organisateur.
     *
     * @param eventId L'ID de l'événement.
     * @return L'entité {@link Event}.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public Event getEventByIdWithSecurityCheck(Integer eventId) {
        Event event = getEventByIdOrThrow(eventId);
        securityService.checkIsCurrentUserMemberOfClubOrThrow(event.getOrganisateur().getId());
        return event;
    }

    /**
     * Récupère les événements d'un club, avec vérification des droits et filtre de statut.
     * <p>
     * <b>Sécurité :</b> L'utilisateur courant doit être membre du club.
     *
     * @param clubId       L'ID du club organisateur.
     * @param statusFilter Filtre "active", "inactive", ou "all". "active" par défaut.
     * @return Une liste filtrée d'événements.
     */
    @Transactional(readOnly = true)
    public List<Event> findEventsByOrganisateurWithSecurityCheck(Integer clubId, String statusFilter) {
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdAndActif(clubId, false);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurId(clubId);
        } else {
            return eventRepository.findByOrganisateurIdAndActif(clubId, true);
        }
    }

    /**
     * Récupère les événements futurs d'un club, avec vérification des droits et filtre de statut.
     * <p>
     * <b>Sécurité :</b> L'utilisateur courant doit être membre du club.
     *
     * @param clubId       L'ID du club organisateur.
     * @param statusFilter Filtre "active", "inactive", ou "all". "active" par défaut.
     * @return Une liste filtrée d'événements futurs.
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsByOrganisateurWithSecurityCheck(Integer clubId, String statusFilter) {
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        Instant now = Instant.now();
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdAndActifAndStartTimeAfter(clubId, false, now);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdAndStartTimeAfter(clubId, now);
        } else {
            return eventRepository.findByOrganisateurIdAndActifAndStartTimeAfter(clubId, true, now);
        }
    }

    /**
     * Récupère les événements futurs des clubs auxquels l'utilisateur courant adhère.
     *
     * @param statusFilter Filtre "active", "inactive", ou "all". Par défaut, retourne tout.
     * @return Une liste filtrée d'événements futurs.
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsForMemberClubs(String statusFilter) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        List<Integer> memberClubIds = findClubIdsForMember(currentUserId);

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList();
        }

        Instant now = Instant.now();

        if ("active".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdInAndActifAndStartTimeAfter(memberClubIds, true, now);
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdInAndActifAndStartTimeAfter(memberClubIds, false, now);
        } else {
            return eventRepository.findByOrganisateurIdInAndStartTimeAfter(memberClubIds, now);
        }
    }

    /**
     * Désactive un événement (annulation logique).
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club organisateur.
     *
     * @param eventId L'ID de l'événement à désactiver.
     */
    public void deactivateEvent(Integer eventId) {
        Event eventToDeactivate = getEventByIdOrThrow(eventId);

        if (!eventToDeactivate.getActif()) {
            log.info("L'événement {} est déjà inactif. Aucune action n'est requise.", eventId);
            return;
        }

        securityService.checkManagerOfClubOrThrow(eventToDeactivate.getOrganisateur().getId());

        eventToDeactivate.prepareForDeactivation();
        eventToDeactivate.setActif(false);
        eventRepository.save(eventToDeactivate);
    }

    /**
     * Récupère tous les événements futurs des clubs d'un membre, avec un filtre de statut.
     *
     * @param status Filtre "active", "inactive", ou "all". "active" par défaut.
     * @return Une liste d'événements.
     */
    @Transactional(readOnly = true)
    public List<Event> findAllEventsForMemberClubs(String status) {
        Membre currentUser = securityService.getCurrentMembreOrThrow();

        Set<Integer> memberClubIds = currentUser.getAdhesions().stream()
                .filter(adhesion -> adhesion.getClub() != null && adhesion.getClub().getActif())
                .map(adhesion -> adhesion.getClub().getId())
                .collect(Collectors.toSet());

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList();
        }

        Instant now = Instant.now();

        if ("all".equalsIgnoreCase(status)) {
            return eventRepository.findByOrganisateurIdInAndStartTimeAfter(memberClubIds, now);
        } else if ("inactive".equalsIgnoreCase(status)) {
            return eventRepository.findByOrganisateurIdInAndActifIsFalseAndStartTimeAfter(memberClubIds, now);
        } else {
            return eventRepository.findByOrganisateurIdInAndActifIsTrueAndStartTimeAfter(memberClubIds, now);
        }
    }

    /**
     * Récupère les événements futurs des clubs d'un membre, enrichis avec les amis participants.
     *
     * @param statusFilter      Filtre de statut de l'événement.
     * @param filterWithFriends Si true, ne retourne que les événements avec des amis participants.
     * @return Une liste de DTOs {@link EventWithFriendsDto}.
     */
    @Transactional(readOnly = true)
    public List<EventWithFriendsDto> findMemberEventsFiltered(String statusFilter, boolean filterWithFriends) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        List<Integer> memberClubIds = findClubIdsForMember(currentUserId);
        if (memberClubIds.isEmpty()) {
            log.debug("Aucun club trouvé pour le membre {}", currentUserId);
            return Collections.emptyList();
        }
        log.debug("Membre {} appartient aux clubs: {}", currentUserId, memberClubIds);

        Instant now = Instant.now();
        boolean actifStatus = !"inactive".equalsIgnoreCase(statusFilter);

        log.debug("Filtre statut appliqué (true=actif, false=inactif): {}", actifStatus);
        
        List<Integer> friendIds = demandeAmiRepository.findFriendIdsOfUser(currentUserId, Statut.ACCEPTEE);
        log.debug("Amis trouvés pour membre {}: {} amis", currentUserId, friendIds.size());

        List<Event> events;
        if (filterWithFriends) {
            log.debug("Application du filtre 'avec amis'.");
            if (friendIds.isEmpty()) {
                return Collections.emptyList();
            }
            events = eventRepository.findUpcomingEventsInClubsWithStatusAndFriends(memberClubIds, now, actifStatus, friendIds);
        } else {
            events = eventRepository.findUpcomingEventsInClubsWithStatus(memberClubIds, now, actifStatus);
        }
        log.debug("DAO a retourné {} événements.", events.size());

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> eventIds = events.stream().map(Event::getId).toList();
        Map<Integer, List<String>> friendsInEventsMap = findParticipatingFriendNamesForEvents(eventIds, friendIds);

        return events.stream().map(event -> {
            EventWithFriendsDto dto = new EventWithFriendsDto();
            dto.setId(event.getId());
            dto.setNom(event.getNom());
            dto.setStartTime(event.getStartTime());
            dto.setEndTime(event.getEndTime());
            dto.setDescription(event.getDescription());
            dto.setLocation(event.getLocation());
            dto.setActif(event.getActif());
            dto.setPlaceTotal(event.getPlaceTotal());
            dto.setPlaceReserve(event.getPlaceReserve());
            dto.setPlaceDisponible(event.getPlaceDisponible());
            dto.setAmiParticipants(friendsInEventsMap.getOrDefault(event.getId(), Collections.emptyList()));
            dto.setOrganisateur(event.getOrganisateur());
            dto.setCategories(event.getCategories());
            return dto;
        }).toList();
    }

    /**
     * Récupère les 5 prochains événements actifs du club géré par l'utilisateur.
     *
     * @return Une liste de 5 événements maximum.
     * @throws AccessDeniedException si l'utilisateur n'est pas un gestionnaire.
     */
    @Transactional(readOnly = true)
    public List<Event> getNextFiveEventsForManagedClub() {
        Integer clubId = securityService.getCurrentUserManagedClubIdOrThrow();
        Instant now = Instant.now();
        return eventRepository.findTop5ByOrganisateurIdAndActifTrueAndStartTimeAfterOrderByStartTimeAsc(clubId, now);
    }

    /**
     * Crée un nouvel événement avec ses catégories.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club organisateur.
     *
     * @param organisateurId L'ID du club organisateur.
     * @param dto            Le DTO de création.
     * @return Le nouvel événement créé.
     * @throws IllegalArgumentException si les dates sont incohérentes ou si des noms de catégorie sont dupliqués.
     */
    public Event createEventWithCategories(Integer organisateurId, CreateEventWithCategoriesDto dto) {
        securityService.checkManagerOfClubOrThrow(organisateurId);
        Club organisateur = clubRepository.findById(organisateurId)
                .orElseThrow(() -> new EntityNotFoundException("Club organisateur non trouvé avec l'ID : " + organisateurId));

        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin.");
        }

        Event newEvent = new Event();
        newEvent.setNom(dto.getNom());
        newEvent.setStartTime(dto.getStartTime());
        newEvent.setEndTime(dto.getEndTime());
        newEvent.setDescription(dto.getDescription());
        newEvent.setLocation(dto.getLocation());
        newEvent.setOrganisateur(organisateur);
        newEvent.setActif(true);

        if (dto.getCategories() != null) {
            Set<String> categoryNamesInRequest = new HashSet<>();
            for (CreateCategorieDto catDto : dto.getCategories()) {
                if (!categoryNamesInRequest.add(catDto.getNom().toLowerCase())) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' est dupliqué dans la requête.");
                }
                Categorie newCategory = new Categorie();
                newCategory.setNom(catDto.getNom());
                newCategory.setCapacite(catDto.getCapacite());
                newCategory.setEvent(newEvent);
                newEvent.getCategories().add(newCategory);
            }
        }
        return eventRepository.save(newEvent);
    }

    /**
     * Met à jour un événement et réconcilie sa liste de catégories.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club organisateur.
     *
     * @param eventId L'ID de l'événement à mettre à jour.
     * @param dto     Le DTO contenant les nouvelles données.
     * @return L'événement mis à jour.
     */
    public Event updateEventWithCategories(Integer eventId, UpdateEventWithCategoriesDto dto) {
        Event existingEvent = eventRepository.findByIdFetchingCategoriesWithJoinFetch(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        securityService.checkManagerOfClubOrThrow(existingEvent.getOrganisateur().getId());

        if (existingEvent.getEndTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Impossible de modifier un événement déjà terminé.");
        }
        if (!existingEvent.getActif()) {
            throw new IllegalStateException("Impossible de modifier un événement annulé.");
        }
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin.");
        }

        existingEvent.setNom(dto.getNom());
        existingEvent.setStartTime(dto.getStartTime());
        existingEvent.setEndTime(dto.getEndTime());
        existingEvent.setDescription(dto.getDescription());
        existingEvent.setLocation(dto.getLocation());

        reconcileCategories(existingEvent, dto.getCategories());

        return eventRepository.save(existingEvent);
    }

    // --- Méthodes privées ---

    private List<Integer> findClubIdsForMember(Integer membreId) {
        Membre membre = membreRepository.findById(membreId).orElse(null);
        if (membre == null) {
            return Collections.emptyList();
        }
        return adhesionRepository.findClubIdsByMembreId(membreId);
    }

    private Map<Integer, List<String>> findParticipatingFriendNamesForEvents(List<Integer> eventIds, List<Integer> friendIds) {
        if (eventIds == null || eventIds.isEmpty() || friendIds == null || friendIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Reservation> friendReservations = reservationRepository.findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember(
                eventIds, friendIds, ReservationStatus.CONFIRME);

        return friendReservations.stream()
                .filter(r -> r.getEvent() != null && r.getMembre() != null && r.getMembre().getPrenom() != null && r.getMembre().getNom() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getEvent().getId(),
                        Collectors.mapping(
                                r -> r.getMembre().getPrenom() + " " + r.getMembre().getNom(),
                                Collectors.toList()
                        )
                ));
    }

    private void reconcileCategories(Event event, List<UpdateCategorieDto> categoryDtos) {
        Map<Integer, Categorie> existingCategoriesMap = event.getCategories().stream()
                .collect(Collectors.toMap(Categorie::getId, Function.identity()));

        Set<Integer> dtoCategoryIds = categoryDtos.stream()
                .map(UpdateCategorieDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        event.getCategories().removeIf(cat -> !dtoCategoryIds.contains(cat.getId()));

        Set<String> finalCategoryNames = new HashSet<>();

        for (UpdateCategorieDto catDto : categoryDtos) {
            if (catDto.getId() == null) {
                // Ajout
                if (!finalCategoryNames.add(catDto.getNom().toLowerCase())) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' est dupliqué.");
                }
                if (event.getCategories().stream().anyMatch(c -> c.getNom().equalsIgnoreCase(catDto.getNom()))) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' existe déjà.");
                }
                Categorie newCategory = new Categorie();
                newCategory.setNom(catDto.getNom());
                newCategory.setCapacite(catDto.getCapacite());
                newCategory.setEvent(event);
                event.getCategories().add(newCategory);
            } else {
                // Mise à jour
                Categorie categoryToUpdate = existingCategoriesMap.get(catDto.getId());
                if (categoryToUpdate == null) {
                    log.warn("Tentative de mise à jour d'une catégorie (ID: {}) non trouvée ou marquée pour suppression. Ignoré.", catDto.getId());
                    continue;
                }
                String newNom = catDto.getNom();
                if (!finalCategoryNames.add(newNom.toLowerCase())) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + newNom + "' est dupliqué.");
                }
                if (event.getCategories().stream().anyMatch(c -> !c.getId().equals(catDto.getId()) && c.getNom().equalsIgnoreCase(newNom))) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + newNom + "' existe déjà.");
                }
                categoryToUpdate.setNom(newNom);

                Integer newCapacite = catDto.getCapacite();
                if (!newCapacite.equals(categoryToUpdate.getCapacite())) {
                    int placesConfirmees = categorieRepository.countConfirmedReservations(categoryToUpdate.getId());
                    if (newCapacite < placesConfirmees) {
                        throw new IllegalStateException("Impossible de réduire la capacité à " + newCapacite + " car " + placesConfirmees + " places sont confirmées.");
                    }
                    categoryToUpdate.setCapacite(newCapacite);
                }
            }
        }
    }
}

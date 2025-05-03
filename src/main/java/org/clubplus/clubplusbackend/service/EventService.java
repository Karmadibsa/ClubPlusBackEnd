package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.*;
import org.clubplus.clubplusbackend.dto.*;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier liée aux événements (Events).
 * Fournit des opérations CRUD, des recherches filtrées et gère les aspects de sécurité
 * relatifs aux événements et à l'appartenance aux clubs.
 */
@Service
@RequiredArgsConstructor
@Transactional // Applique la transactionnalité par défaut à toutes les méthodes publiques
public class EventService {

    // Dépendances injectées via Lombok @RequiredArgsConstructor
    private final EventDao eventRepository;
    private final ReservationDao reservationRepository;
    private final CategorieDao categorieRepository;
    private final DemandeAmiDao demandeAmiRepository;
    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final ClubDao clubRepository;
    private final SecurityService securityService;

    // --- Méthodes de Lecture Publiques (Certaines avec Sécurité) ---

    /**
     * Récupère une liste d'événements, potentiellement filtrée par statut (actif/inactif).
     * Par défaut, si aucun filtre n'est spécifié ou si le filtre est "active",
     * seuls les événements actifs sont retournés.
     *
     * @param statusFilter Filtre de statut: "active" (défaut), "inactive", ou "all" pour tous les événements.
     *                     La casse est ignorée. Null est traité comme "active".
     * @return Une liste d'objets {@link Event} correspondant au filtre appliqué.
     */
    @Transactional(readOnly = true)
    public List<Event> findAllEvents(String statusFilter) {
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByActif(false);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findAll(); // Récupère tous les événements
        } else { // "active" ou null (comportement par défaut)
            return eventRepository.findByActif(true);
        }
    }

    /**
     * Récupère un événement spécifique par son identifiant unique.
     * Lance une exception si aucun événement n'est trouvé avec cet ID.
     *
     * @param id L'identifiant unique de l'événement à récupérer.
     * @return L'entité {@link Event} correspondante.
     * @throws EntityNotFoundException si aucun événement n'est trouvé pour l'ID fourni.
     */
    @Transactional(readOnly = true)
    public Event getEventByIdOrThrow(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un événement par son ID et vérifie si l'utilisateur actuellement authentifié
     * est membre du club organisateur de cet événement.
     *
     * @param eventId L'ID de l'événement à récupérer.
     * @return L'entité {@link Event} si elle est trouvée et si l'utilisateur est membre du club organisateur.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws SecurityException       (ou une exception similaire lancée par {@code SecurityService})
     *                                 si l'utilisateur actuel n'est pas membre du club organisateur.
     */
    @Transactional(readOnly = true)
    public Event getEventByIdWithSecurityCheck(Integer eventId) {
        Event event = getEventByIdOrThrow(eventId); // Lance EntityNotFoundException si non trouvé
        // Vérification de sécurité : l'utilisateur doit être membre du club organisateur
        securityService.checkIsCurrentUserMemberOfClubOrThrow(event.getOrganisateur().getId()); // Lance une exception de sécurité si non membre
        return event;
    }

    /**
     * Récupère les événements organisés par un club spécifique, avec une vérification de sécurité
     * et un filtrage optionnel par statut.
     * L'utilisateur actuel doit être membre du club pour accéder à ces informations.
     * Par défaut (statusFilter = null ou "active"), retourne uniquement les événements actifs.
     *
     * @param clubId       L'ID du club organisateur dont on veut les événements.
     * @param statusFilter Filtre de statut: "active" (défaut), "inactive", ou "all". Null est traité comme "active".
     * @return Une liste filtrée des événements {@link Event} organisés par le club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'existe pas.
     * @throws SecurityException       (ou similaire) si l'utilisateur actuel n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public List<Event> findEventsByOrganisateurWithSecurityCheck(Integer clubId, String statusFilter) {
        // Vérifie d'abord l'existence du club
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        // Vérifie ensuite si l'utilisateur est membre du club
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        // Applique le filtre de statut pour la récupération des événements
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurIdAndActif(clubId, false);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            return eventRepository.findByOrganisateurId(clubId); // Récupère tous les événements du club
        } else { // "active" ou null (comportement par défaut)
            return eventRepository.findByOrganisateurIdAndActif(clubId, true);
        }
    }

    /**
     * Récupère les événements FUTURS (dont la date de début est postérieure à maintenant)
     * organisés par un club spécifique, avec vérification de sécurité et filtrage par statut.
     * L'utilisateur actuel doit être membre du club.
     * Par défaut (statusFilter = null ou "active"), retourne uniquement les événements futurs actifs.
     *
     * @param clubId       L'ID du club organisateur.
     * @param statusFilter Filtre de statut: "active" (défaut), "inactive", ou "all". Null est traité comme "active".
     * @return Une liste filtrée des événements {@link Event} futurs organisés par le club.
     * @throws EntityNotFoundException si le club avec l'ID spécifié n'existe pas.
     * @throws SecurityException       (ou similaire) si l'utilisateur actuel n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsByOrganisateurWithSecurityCheck(Integer clubId, String statusFilter) {
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        LocalDateTime now = LocalDateTime.now();
        // Applique le filtre de statut et de date
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            // Trouve les événements futurs et inactifs
            return eventRepository.findByOrganisateurIdAndActifAndStartAfter(clubId, false, now);
        } else if ("all".equalsIgnoreCase(statusFilter)) {
            // Trouve tous les événements futurs (actifs et inactifs)
            return eventRepository.findByOrganisateurIdAndStartAfter(clubId, now);
        } else { // "active" ou null (comportement par défaut)
            // Trouve les événements futurs et actifs
            return eventRepository.findByOrganisateurIdAndActifAndStartAfter(clubId, true, now);
        }
    }

    /**
     * Récupère les événements FUTURS des clubs dont l'utilisateur actuellement authentifié est membre.
     * Permet un filtrage optionnel par statut de l'événement (actif/inactif).
     * Par défaut (statusFilter = null, "all", ou invalide), retourne TOUS les événements futurs (actifs et inactifs)
     * des clubs auxquels l'utilisateur appartient.
     *
     * @param statusFilter Filtre de statut: "active", "inactive", ou null/"all" pour tous.
     *                     Null ou "all" retourne les actifs et inactifs.
     * @return Une liste filtrée des événements {@link Event} futurs pertinents pour l'utilisateur.
     * @throws SecurityException Si l'utilisateur actuel ne peut pas être identifié.
     */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsForMemberClubs(String statusFilter) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        List<Integer> memberClubIds = findClubIdsForMember(currentUserId); // Utilise la méthode helper

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList(); // Si l'utilisateur n'est membre d'aucun club
        }

        LocalDateTime now = LocalDateTime.now();

        // Applique le filtre basé sur le paramètre statusFilter
        if ("active".equalsIgnoreCase(statusFilter)) {
            // Récupère seulement les événements futurs et actifs
            return eventRepository.findByOrganisateurIdInAndActifAndStartAfter(memberClubIds, true, now);
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            // Récupère seulement les événements futurs et inactifs (annulés)
            return eventRepository.findByOrganisateurIdInAndActifAndStartAfter(memberClubIds, false, now);
        } else { // null, "all", ou une valeur invalide -> retourne TOUT (comportement par défaut)
            // Récupère tous les événements futurs pour ces clubs (actifs et inactifs)
            return eventRepository.findByOrganisateurIdInAndStartAfter(memberClubIds, now);
        }
    }

    /**
     * Méthode utilitaire privée pour récupérer les IDs des clubs dont un membre spécifique fait partie.
     *
     * @param membreId L'ID du membre.
     * @return Une liste des IDs des clubs auxquels le membre adhère, ou une liste vide si non trouvé ou sans adhésions.
     */
    private List<Integer> findClubIdsForMember(Integer membreId) {
        // Il est plus sûr de vérifier si le membre existe, bien que findClubIdsByMembreId puisse retourner vide.
        Membre membre = membreRepository.findById(membreId)
                .orElse(null); // Gère le cas où le membre n'est pas trouvé explicitement

        if (membre == null) {
            // Log ou gestion d'erreur si nécessaire, mais retourner liste vide est cohérent.
            return Collections.emptyList();
        }

        // Appelle directement la méthode DAO optimisée si elle existe et fait le travail.
        return adhesionRepository.findClubIdsByMembreId(membreId);

    }

    // --- Méthodes d'Écriture (CRUD avec Sécurité) ---

    /**
     * Désactive un événement existant (annulation logique).
     * Modifie le nom pour indiquer l'annulation et passe le flag `actif` à false.
     * Nécessite que l'utilisateur actuel soit MANAGER du club organisateur.
     * Cette opération est idempotente : désactiver un événement déjà inactif n'a pas d'effet.
     *
     * @param eventId L'ID de l'événement à désactiver.
     * @throws EntityNotFoundException si l'événement avec l'ID spécifié n'est pas trouvé.
     * @throws SecurityException       (ou similaire) si l'utilisateur actuel n'est pas manager du club organisateur.
     */
    @Transactional
    public void deactivateEvent(Integer eventId) {
        // 1. Récupérer l'événement (indépendamment de son statut actuel)
        Event eventToDeactivate = getEventByIdOrThrow(eventId);

        // 2. Vérifier si l'événement est déjà inactif (Idempotence)
        if (!eventToDeactivate.getActif()) {
            // Optionnel: log ou message indiquant que l'opération n'est pas nécessaire
            System.out.println("L'événement " + eventId + " est déjà inactif."); // Remplacer par un logger en production
            return; // Aucune action requise
        }

        // 3. Vérification de Sécurité : l'utilisateur doit être manager du club
        securityService.checkManagerOfClubOrThrow(eventToDeactivate.getOrganisateur().getId());

        // 4. Procéder à la Désactivation Logique
        eventToDeactivate.prepareForDeactivation(); // Méthode sur l'entité pour modifier nom, etc.
        eventToDeactivate.setActif(false);          // Marquer comme inactif
        eventRepository.save(eventToDeactivate);    // Persister les changements (UPDATE SQL)
    }

    /**
     * Récupère tous les événements FUTURS des clubs dont l'utilisateur actuel est membre,
     * filtrés par statut. Cette méthode semble similaire à findUpcomingEventsForMemberClubs,
     * mais utilise une approche légèrement différente pour obtenir les IDs de club
     * et les requêtes DAO spécifiques au statut booléen.
     *
     * @param status Filtre de statut : "active", "inactive", ou "all".
     *               "active" est utilisé par défaut si la valeur n'est ni "inactive" ni "all".
     * @return Une liste d'événements {@link Event} futurs correspondant aux critères.
     * @throws SecurityException Si l'utilisateur actuel ne peut pas être identifié ou chargé.
     */
    @Transactional(readOnly = true) // Lecture seule car on ne modifie rien
    public List<Event> findAllEventsForMemberClubs(String status) {
        Membre currentUser = securityService.getCurrentMembreOrThrow(); // Récupère l'entité Membre

        // Récupère les IDs des clubs ACTIFS auxquels le membre adhère
        Set<Integer> memberClubIds = currentUser.getAdhesions().stream()
                .filter(adhesion -> adhesion.getClub() != null && adhesion.getClub().getActif()) // Filtre aussi les clubs inactifs
                .map(adhesion -> adhesion.getClub().getId())
                .collect(Collectors.toSet());

        if (memberClubIds.isEmpty()) {
            return Collections.emptyList(); // Pas de clubs actifs ou pas d'adhésions
        }

        LocalDateTime now = LocalDateTime.now(); // Date de référence pour "futur"

        // Sélection de la bonne méthode DAO en fonction du statut demandé
        if ("all".equalsIgnoreCase(status)) {
            // Tous les événements futurs (actifs et inactifs)
            return eventRepository.findByOrganisateurIdInAndStartAfter(memberClubIds, now);
        } else if ("inactive".equalsIgnoreCase(status)) {
            // Événements futurs et inactifs
            return eventRepository.findByOrganisateurIdInAndActifIsFalseAndStartAfter(memberClubIds, now);
        } else { // "active" ou par défaut
            // Événements futurs et actifs
            return eventRepository.findByOrganisateurIdInAndActifIsTrueAndStartAfter(memberClubIds, now);
        }
    }

    /**
     * Récupère les événements FUTURS pour les clubs dont l'utilisateur est membre,
     * sous forme de DTOs incluant les noms des amis de l'utilisateur qui participent également.
     * Offre des options de filtrage par statut de l'événement et par présence d'amis.
     *
     * @param statusFilter      Filtre de statut : "active", "inactive", ou null/"all" (par défaut: tous statuts).
     * @param filterWithFriends Si true, ne retourne que les événements auxquels au moins un ami (confirmé) participe.
     *                          Si false, retourne tous les événements correspondants aux autres filtres.
     * @return Une liste de {@link EventWithFriendsDto}, chacun représentant un événement futur
     * et contenant la liste des noms ("Prénom Nom") des amis participants.
     * Retourne une liste vide si aucun événement ne correspond ou si l'utilisateur n'a pas de club/amis (selon filtre).
     * @throws SecurityException Si l'utilisateur actuel ne peut pas être identifié.
     */
    @Transactional(readOnly = true)
    public List<EventWithFriendsDto> findMemberEventsFiltered(String statusFilter, boolean filterWithFriends) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // 1. Récupérer les IDs des clubs du membre (via la méthode helper)
        List<Integer> memberClubIds = findClubIdsForMember(currentUserId);
        if (memberClubIds.isEmpty()) {
            System.out.println("Aucun club trouvé pour le membre " + currentUserId); // Logger préférable
            return Collections.emptyList();
        }
        System.out.println("Membre " + currentUserId + " appartient aux clubs: " + memberClubIds); // Logger préférable

        // 2. Déterminer le statut booléen pour la requête DAO basé sur statusFilter
        LocalDateTime now = LocalDateTime.now(); // Pour filtrer les événements futurs
        Boolean actifStatus = null; // Null signifie "tous les statuts" (actif et inactif)
        if ("active".equalsIgnoreCase(statusFilter)) {
            actifStatus = true;
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            actifStatus = false;
        }
        System.out.println("Filtre statut appliqué: " + (actifStatus == null ? "aucun" : actifStatus)); // Logger préférable

        // 3. Récupérer les IDs des amis confirmés de l'utilisateur
        List<Integer> friendIds = demandeAmiRepository.findFriendIdsOfUser(currentUserId, Statut.ACCEPTE);
        System.out.println("Amis trouvés pour membre " + currentUserId + ": " + friendIds.size()); // Logger préférable

        // 4. Récupérer les entités Event en fonction des filtres (statut, clubs, amis si demandé)
        List<Event> events;
        if (filterWithFriends) {
            System.out.println("Application du filtre 'avec amis'."); // Logger préférable
            if (friendIds.isEmpty()) {
                System.out.println("Filtre 'avec amis' actif mais aucun ami trouvé, retour liste vide."); // Logger préférable
                return Collections.emptyList(); // Si on filtre par amis mais qu'il n'y en a pas, aucun événement ne peut correspondre
            }
            // Utilise la méthode DAO optimisée qui filtre par amis participants
            events = eventRepository.findUpcomingEventsInClubsWithStatusAndFriends(
                    memberClubIds, now, actifStatus, friendIds);
            System.out.println("DAO (avec amis) a retourné " + events.size() + " événements."); // Logger préférable
        } else {
            System.out.println("Filtre 'avec amis' inactif."); // Logger préférable
            // Utilise la méthode DAO sans le filtre ami
            events = eventRepository.findUpcomingEventsInClubsWithStatus(
                    memberClubIds, now, actifStatus);
            System.out.println("DAO (sans amis) a retourné " + events.size() + " événements."); // Logger préférable
        }

        // 5. Si aucun événement n'est trouvé après filtrage, retourner une liste vide de DTOs
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. Préparer une Map associant chaque ID d'événement à la liste des noms des amis participants
        List<Integer> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Integer, List<String>> friendsInEventsMap = findParticipatingFriendNamesForEvents(eventIds, friendIds);
        System.out.println("Map des amis participants créée pour " + friendsInEventsMap.size() + " événements."); // Logger préférable

        // 7. Mapper les entités Event vers les DTOs EventWithFriendsDto, en ajoutant les amis participants
        List<EventWithFriendsDto> dtos = events.stream().map(event -> {
            EventWithFriendsDto dto = new EventWithFriendsDto();
            // Copie des propriétés de base de l'événement
            dto.setId(event.getId());
            dto.setNom(event.getNom());
            dto.setStart(event.getStart());
            dto.setEnd(event.getEnd());
            dto.setDescription(event.getDescription());
            dto.setLocation(event.getLocation());
            dto.setActif(event.getActif());
            // Copie/Calcul des informations sur les places (via méthodes @Transient de l'entité)
            dto.setPlaceTotal(event.getPlaceTotal());
            dto.setPlaceReserve(event.getPlaceReserve());
            dto.setPlaceDisponible(event.getPlaceDisponible());
            // Ajout des informations spécifiques au DTO
            dto.setAmiParticipants(friendsInEventsMap.getOrDefault(event.getId(), Collections.emptyList()));
            dto.setOrganisateur(event.getOrganisateur()); // Peut nécessiter un DTO Club simple ou juste l'ID/Nom
            dto.setCategories(event.getCategories());     // Peut nécessiter un Set de DTOs Categorie simples

            return dto;
        }).collect(Collectors.toList());

        System.out.println("Mappage vers DTO terminé. Retour de " + dtos.size() + " DTOs."); // Logger préférable
        return dtos;
    }

    /**
     * Méthode privée utilitaire pour trouver les noms ("Prénom Nom") des amis (parmi friendIds)
     * qui ont une réservation confirmée pour les événements spécifiés (par eventIds).
     * Utilise une requête optimisée pour récupérer les réservations et les membres associés.
     *
     * @param eventIds  Liste des IDs des événements concernés.
     * @param friendIds Liste des IDs des amis de l'utilisateur courant.
     * @return Une Map où les clés sont les IDs d'événements et les valeurs sont des listes
     * de noms ("Prénom Nom") des amis ayant une réservation confirmée pour cet événement.
     * Retourne une Map vide si les listes d'IDs sont vides ou null.
     */
    private Map<Integer, List<String>> findParticipatingFriendNamesForEvents(List<Integer> eventIds, List<Integer> friendIds) {
        // Vérification initiale pour éviter des requêtes inutiles
        if (eventIds == null || eventIds.isEmpty() || friendIds == null || friendIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Appelle la méthode DAO qui récupère les réservations CONFIRMEES des amis pour ces événements,
        // en s'assurant que les informations du membre (prénom, nom) sont chargées efficacement (JOIN FETCH).
        List<Reservation> friendReservations = reservationRepository.findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember(
                eventIds,
                friendIds,
                ReservationStatus.CONFIRME); // Assurez-vous que ce statut correspond bien aux réservations valides

        // Groupe les résultats par ID d'événement et extrait le nom complet du membre
        return friendReservations.stream()
                // Filtrage robuste pour éviter NullPointerException si des données sont manquantes
                .filter(r -> r.getEvent() != null && r.getMembre() != null && r.getMembre().getPrenom() != null && r.getMembre().getNom() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getEvent().getId(), // Clé de la map : ID de l'événement
                        Collectors.mapping(
                                // Valeur de la map : Nom complet de l'ami participant
                                r -> r.getMembre().getPrenom() + " " + r.getMembre().getNom(),
                                Collectors.toList() // Collecte les noms dans une liste pour chaque événement
                        )
                ));
    }

    /**
     * Récupère les 5 prochains événements actifs (date de début future) organisés par le club
     * dont l'utilisateur actuellement authentifié est le manager.
     * Les événements sont triés par date de début croissante.
     *
     * @return Une liste contenant jusqu'à 5 des prochains événements {@link Event} actifs du club géré.
     * La liste peut être vide si aucun événement futur actif n'est trouvé ou si l'utilisateur ne gère aucun club.
     * @throws AccessDeniedException (ou similaire) si l'utilisateur n'est pas manager d'un club
     *                               ou si son rôle ne peut être déterminé.
     */
    @Transactional(readOnly = true)
    public List<Event> getNextFiveEventsForManagedClub() {
        // Récupère l'ID du club géré par l'utilisateur (lance une exception si non autorisé/non trouvé)
        Integer clubId = securityService.getCurrentUserManagedClubIdOrThrow();

        LocalDateTime now = LocalDateTime.now(); // Point de référence temporel

        // Appelle la méthode du repository pour trouver les 5 prochains événements actifs, triés par date
        return eventRepository.findTop5ByOrganisateurIdAndActifTrueAndStartAfterOrderByStartAsc(clubId, now);
    }


    /**
     * Crée un nouvel événement avec ses catégories initiales associées.
     * Nécessite que l'utilisateur actuel soit MANAGER du club organisateur spécifié.
     * Valide que la date de début est antérieure à la date de fin et que les noms de catégorie
     * fournis sont uniques au sein de cette requête de création.
     *
     * @param organisateurId L'ID du club qui organise l'événement.
     * @param dto            Le DTO {@link CreateEventWithCategoriesDto} contenant les détails de l'événement
     *                       et la liste des catégories {@link CreateCategorieDto} à créer.
     * @return L'entité {@link Event} nouvellement créée et persistée, incluant ses catégories.
     * @throws SecurityException        (ou similaire) si l'utilisateur n'est pas manager du club.
     * @throws EntityNotFoundException  si le club organisateur n'est pas trouvé.
     * @throws IllegalArgumentException si la date de début est après la date de fin,
     *                                  ou si des noms de catégorie sont dupliqués dans le DTO.
     */
    @Transactional // Assure que la création de l'événement et de ses catégories est atomique
    public Event createEventWithCategories(Integer organisateurId, CreateEventWithCategoriesDto dto) {
        // 1. Vérification de Sécurité : L'utilisateur doit être manager du club
        securityService.checkManagerOfClubOrThrow(organisateurId);

        // 2. Récupérer l'entité Club organisateur
        Club organisateur = clubRepository.findById(organisateurId)
                .orElseThrow(() -> new EntityNotFoundException("Club organisateur non trouvé avec l'ID : " + organisateurId));

        // 3. Validation des dates de l'événement
        if (dto.getStart().isAfter(dto.getEnd())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin."); // HTTP 400 Bad Request
        }

        // 4. Créer l'instance de l'entité Event et mapper les champs depuis le DTO
        Event newEvent = new Event();
        newEvent.setNom(dto.getNom());
        newEvent.setStart(dto.getStart());
        newEvent.setEnd(dto.getEnd());
        newEvent.setDescription(dto.getDescription());
        newEvent.setLocation(dto.getLocation());
        newEvent.setOrganisateur(organisateur); // Associer l'événement au club
        newEvent.setActif(true); // Un nouvel événement est actif par défaut

        // 5. Créer et associer les catégories fournies dans le DTO
        if (dto.getCategories() != null) {
            // Utilisation d'un Set temporaire pour vérifier l'unicité des noms dans la requête
            Set<String> categoryNamesInRequest = new HashSet<>();
            for (CreateCategorieDto catDto : dto.getCategories()) {
                // Validation d'unicité du nom de catégorie DANS CETTE REQUÊTE
                if (!categoryNamesInRequest.add(catDto.getNom().toLowerCase())) { // Ignore la casse pour l'unicité
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' est dupliqué dans la requête de création."); // HTTP 400
                }
                // Créer l'entité Categorie
                Categorie newCategory = new Categorie();
                newCategory.setNom(catDto.getNom());
                newCategory.setCapacite(catDto.getCapacite());
                newCategory.setEvent(newEvent); // Établir la relation bidirectionnelle
                newEvent.getCategories().add(newCategory); // Ajouter la catégorie à la collection de l'événement
            }
        }

        // 6. Sauvegarder l'événement. Grâce à CascadeType.ALL (ou PERSIST/MERGE) sur Event.categories,
        // les nouvelles catégories seront également persistées.
        return eventRepository.save(newEvent);
    }

    /**
     * Met à jour un événement existant ainsi que ses catégories associées.
     * Permet de modifier les détails de l'événement et d'ajouter, modifier ou supprimer des catégories.
     * Nécessite que l'utilisateur actuel soit MANAGER du club organisateur.
     * Empêche la modification d'événements déjà terminés ou annulés.
     * Valide la cohérence des dates et l'unicité des noms de catégorie.
     * Gère la suppression des catégories via {@code orphanRemoval=true}.
     *
     * @param eventId L'ID de l'événement à mettre à jour.
     * @param dto     Le DTO {@link UpdateEventWithCategoriesDto} contenant les nouvelles données
     *                de l'événement et la liste complète des catégories souhaitées
     *                ({@link UpdateCategorieDto}), incluant les IDs pour celles à modifier/garder.
     * @return L'entité {@link Event} mise à jour et persistée.
     * @throws EntityNotFoundException  si l'événement n'est pas trouvé.
     * @throws SecurityException        (ou similaire) si l'utilisateur n'est pas manager du club.
     * @throws IllegalStateException    si l'événement est déjà terminé ou annulé (HTTP 409 Conflict).
     *                                  ou si la réduction de capacité d'une catégorie échoue à cause de réservations existantes.
     * @throws IllegalArgumentException si la date de début est après la date de fin,
     *                                  ou si des noms de catégorie sont dupliqués après mise à jour (HTTP 400 Bad Request).
     */
    @Transactional // Transaction pour l'ensemble de l'opération de mise à jour
    public Event updateEventWithCategories(Integer eventId, UpdateEventWithCategoriesDto dto) {
        // 1. Récupérer l'événement existant ET ses catégories (essentiel pour la réconciliation)
        // Utilisation d'une méthode DAO avec JOIN FETCH pour éviter le N+1 et charger les catégories.
        Event existingEvent = eventRepository.findByIdFetchingCategoriesWithJoinFetch(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        // 2. Vérification de Sécurité : Manager du club organisateur requis
        securityService.checkManagerOfClubOrThrow(existingEvent.getOrganisateur().getId());

        // 3. Validations métier sur l'état de l'événement avant modification
        if (existingEvent.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de modifier un événement déjà terminé."); // HTTP 409 Conflict
        }
        if (!existingEvent.getActif()) {
            throw new IllegalStateException("Impossible de modifier un événement annulé."); // HTTP 409 Conflict
        }
        // Validation des dates fournies dans le DTO
        if (dto.getStart().isAfter(dto.getEnd())) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin."); // HTTP 400 Bad Request
        }

        // 4. Mettre à jour les propriétés simples de l'événement depuis le DTO
        existingEvent.setNom(dto.getNom());
        existingEvent.setStart(dto.getStart());
        existingEvent.setEnd(dto.getEnd());
        existingEvent.setDescription(dto.getDescription());
        existingEvent.setLocation(dto.getLocation());
        // Ne pas modifier 'actif' ou 'organisateur' ici, sauf si requis spécifiquement.

        // 5. Réconcilier la collection de catégories de l'événement avec la liste fournie dans le DTO
        reconcileCategories(existingEvent, dto.getCategories());

        // 6. Sauvegarder l'événement. JPA/Hibernate gérera les mises à jour de l'événement,
        // la mise à jour/insertion des catégories modifiées/nouvelles (via cascade),
        // et la suppression des catégories retirées de la collection (via orphanRemoval).
        return eventRepository.save(existingEvent);
    }

    /**
     * Méthode privée utilitaire pour synchroniser la collection de catégories d'un événement
     * avec une liste de DTOs de catégories fournie (typique d'une requête de mise à jour).
     * Gère l'ajout de nouvelles catégories (ID null dans DTO), la mise à jour des catégories
     * existantes (ID non null correspondant), et la suppression des catégories existantes
     * qui ne sont plus présentes dans la liste DTO (via {@code orphanRemoval=true} sur la relation Event->Categories).
     * Assure l'unicité des noms de catégorie au sein de l'événement après modification.
     * Vérifie la validité de la réduction de capacité par rapport aux réservations confirmées.
     *
     * @param event        L'entité {@link Event} à mettre à jour (avec sa collection de catégories chargée).
     * @param categoryDtos La liste des {@link UpdateCategorieDto} représentant l'état désiré des catégories.
     *                     Les DTOs avec ID null sont considérés comme nouveaux.
     *                     Les DTOs avec ID non null doivent correspondre à des catégories existantes de l'événement.
     * @throws IllegalArgumentException Si un nom de catégorie est dupliqué après la mise à jour.
     * @throws IllegalStateException    Si la capacité d'une catégorie est réduite en dessous du nombre de réservations confirmées.
     */
    private void reconcileCategories(Event event, List<UpdateCategorieDto> categoryDtos) {
        // Map des catégories actuellement associées à l'événement, pour accès rapide par ID.
        Map<Integer, Categorie> existingCategoriesMap = event.getCategories().stream()
                .collect(Collectors.toMap(Categorie::getId, Function.identity()));

        // Ensemble des IDs des catégories présentes dans le DTO de mise à jour (pour identifier celles à garder/modifier).
        Set<Integer> dtoCategoryIds = categoryDtos.stream()
                .map(UpdateCategorieDto::getId)
                .filter(Objects::nonNull) // Ignore les nouvelles catégories (ID null) pour cette étape
                .collect(Collectors.toSet());

        // --- Étape 1: Suppression (implicite via orphanRemoval) ---
        // Retire de la collection 'event.categories' toutes les catégories dont l'ID
        // n'est PAS présent dans l'ensemble des IDs du DTO.
        // Si 'orphanRemoval=true' est configuré sur la relation @OneToMany Event.categories,
        // JPA supprimera automatiquement ces catégories de la base de données lors du flush/commit.
        boolean removed = event.getCategories().removeIf(cat -> !dtoCategoryIds.contains(cat.getId()));
        if (removed) {
            // Note: Si la suppression échoue à cause de contraintes (ex: réservations liées à une catégorie supprimée
            // et pas de ON DELETE CASCADE/SET NULL), une exception sera levée plus tard (flush/commit).
            // Une vérification préalable des réservations *avant* la suppression logique ici serait plus robuste
            // mais complique la logique car orphanRemoval agit à la fin.
            System.out.println("Des catégories existantes ont été retirées de la collection et seront supprimées par orphanRemoval."); // Logger préférable
        }

        // --- Étape 2: Ajout et Mise à jour ---
        // Set pour vérifier l'unicité des noms DANS CETTE OPERATION (en ignorant la casse)
        Set<String> finalCategoryNames = new HashSet<>();

        for (UpdateCategorieDto catDto : categoryDtos) {
            if (catDto.getId() == null) {
                // --- AJOUT d'une nouvelle catégorie ---
                // Vérifier l'unicité du nom par rapport aux autres nouvelles ET aux existantes gardées
                String newCatNameLower = catDto.getNom().toLowerCase();
                if (!finalCategoryNames.add(newCatNameLower)) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' est dupliqué dans l'opération de mise à jour."); // HTTP 400
                }
                // Vérifier aussi contre les catégories existantes qui sont conservées (non supprimées à l'étape 1)
                if (event.getCategories().stream().anyMatch(existingCat -> existingCat.getNom().equalsIgnoreCase(catDto.getNom()))) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + catDto.getNom() + "' existe déjà pour cet événement."); // HTTP 400
                }

                // Créer et configurer la nouvelle entité Categorie
                Categorie newCategory = new Categorie();
                newCategory.setNom(catDto.getNom());
                newCategory.setCapacite(catDto.getCapacite()); // Assumer validation capacité >= 0 ailleurs si nécessaire
                newCategory.setEvent(event); // Lier à l'événement parent
                event.getCategories().add(newCategory); // Ajouter à la collection gérée par JPA
                System.out.println("Nouvelle catégorie '" + catDto.getNom() + "' ajoutée."); // Logger préférable

            } else {
                // --- MISE A JOUR d'une catégorie existante ---
                Categorie categoryToUpdate = existingCategoriesMap.get(catDto.getId());

                // Vérifier si la catégorie à mettre à jour existe bien dans la map (donc elle n'a pas été supprimée à l'étape 1)
                if (categoryToUpdate == null) {
                    // Soit l'ID dans le DTO est invalide, soit il correspond à une catégorie qui vient d'être marquée pour suppression.
                    // On peut choisir de lever une erreur ou d'ignorer silencieusement. Ignorer est plus simple.
                    System.out.println("Tentative de mise à jour d'une catégorie (ID: " + catDto.getId() + ") non trouvée ou marquée pour suppression. Ignoré."); // Logger préférable
                    continue; // Passe au DTO suivant
                }

                // Vérifier l'unicité du nouveau nom (si le nom change)
                String newNom = catDto.getNom();
                String newNomLower = newNom.toLowerCase();
                if (!finalCategoryNames.add(newNomLower)) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + newNom + "' est dupliqué dans l'opération de mise à jour."); // HTTP 400
                }
                // Vérifier contre les autres catégories (nouvelles ou existantes modifiées/non modifiées)
                if (event.getCategories().stream().anyMatch(c -> !c.getId().equals(catDto.getId()) && c.getNom().equalsIgnoreCase(newNom))) {
                    throw new IllegalArgumentException("Le nom de catégorie '" + newNom + "' existe déjà pour cet événement."); // HTTP 400
                }
                categoryToUpdate.setNom(newNom); // Appliquer le nouveau nom

                // Mettre à jour la capacité, mais seulement si elle est valide
                Integer newCapacite = catDto.getCapacite();
                if (!newCapacite.equals(categoryToUpdate.getCapacite())) {
                    // Avant de réduire la capacité, vérifier les réservations confirmées
                    // Appel à une méthode DAO pour compter ces réservations est nécessaire.
                    int placesConfirmees = categorieRepository.countConfirmedReservations(categoryToUpdate.getId()); // Doit être implémentée dans CategorieDao
                    if (newCapacite < placesConfirmees) {
                        throw new IllegalStateException("Impossible de réduire la capacité à " + newCapacite +
                                " pour la catégorie '" + categoryToUpdate.getNom() + "' car " +
                                placesConfirmees + " places sont déjà confirmées."); // HTTP 409 Conflict
                    }
                    categoryToUpdate.setCapacite(newCapacite); // Appliquer la nouvelle capacité
                    System.out.println("Catégorie '" + newNom + "' (ID: " + catDto.getId() + ") mise à jour. Nouvelle capacité: " + newCapacite); // Logger préférable
                } else {
                    // Si la capacité n'a pas changé, s'assurer que le nom est ajouté au set de vérification d'unicité
                    // (peut déjà y être si le nom n'a pas changé non plus, .add retournera false mais c'est sans conséquence ici)
                    finalCategoryNames.add(categoryToUpdate.getNom().toLowerCase());
                    System.out.println("Catégorie '" + categoryToUpdate.getNom() + "' (ID: " + catDto.getId() + ") mise à jour (nom potentiellement changé, capacité identique)."); // Logger préférable
                }
            }
        }
    }
}

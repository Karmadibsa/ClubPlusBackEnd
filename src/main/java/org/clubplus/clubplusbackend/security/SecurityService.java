package org.clubplus.clubplusbackend.security; // Ou un autre package approprié

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Reservation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component("securityService") // Nom du bean pour SpEL dans @PreAuthorize si besoin
@RequiredArgsConstructor
public class SecurityService {

    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;           // Injecter DAO Event
    private final ReservationDao reservationRepository; // Injecter DAO Reservation
    // Injecter d'autres DAOs si nécessaire (CategorieDao, DemandeAmiDao...)

    // --- Helpers Internes pour l'Utilisateur Courant ---

    /**
     * Récupère l'ID de l'utilisateur actuellement authentifié.
     * Lance une exception si l'utilisateur n'est pas authentifié ou si le principal est incorrect.
     * À utiliser quand l'authentification est une condition préalable.
     *
     * @return L'ID de l'utilisateur.
     * @throws AccessDeniedException si non authentifié ou type de principal invalide.
     */
    public Integer getCurrentUserIdOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserDetails)) {
            // Utiliser AccessDeniedException ou une exception plus spécifique (ex: UnauthenticatedUserException)
            throw new AccessDeniedException("Utilisateur non authentifié ou informations d'authentification invalides."); // -> 401 ou 403 selon GlobalExceptionHandler
        }
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        Integer userId = userDetails.getId();
        if (userId == null) {
            // Cas très improbable mais sécuritaire
            throw new AccessDeniedException("ID utilisateur non trouvé dans les informations d'authentification.");
        }
        return userId;
    }

    /**
     * Récupère l'objet Membre complet de l'utilisateur actuellement authentifié depuis la BDD.
     * Utilise getCurrentUserIdOrThrow().
     *
     * @return L'entité Membre.
     * @throws AccessDeniedException   si non authentifié.
     * @throws EntityNotFoundException si l'utilisateur authentifié n'est pas trouvé en BDD (incohérence).
     */
    @Transactional(readOnly = true)
    public Membre getCurrentMembreOrThrow() {
        Integer userId = getCurrentUserIdOrThrow(); // Gère le cas non authentifié
        return membreRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Membre authentifié (ID: " + userId + ") non trouvé dans la base de données.")); // Incohérence grave
    }

    /**
     * Récupère l'ID de l'utilisateur courant (version Optional, pour méthodes 'is...').
     */
    private Optional<Integer> getCurrentUserIdOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserDetails)) {
            return Optional.empty();
        }
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        return Optional.ofNullable(userDetails.getId());
    }

    /**
     * Récupère le Membre courant (version Optional, pour méthodes 'is...').
     */
    private Optional<Membre> getCurrentMembreOptional() {
        return getCurrentUserIdOptional().flatMap(membreRepository::findById);
    }

    // --- Vérifications de Propriété (Ownership) ---

    /**
     * Vérifie si l'utilisateur courant est le propriétaire de la ressource (basé sur l'ID).
     *
     * @param ownerId L'ID du propriétaire de la ressource.
     * @return true si l'utilisateur courant est le propriétaire.
     */
    public boolean isOwner(Integer ownerId) {
        if (ownerId == null) return false; // Ne peut pas être propriétaire de "null"
        Optional<Integer> currentUserIdOpt = getCurrentUserIdOptional();
        return currentUserIdOpt.isPresent() && ownerId.equals(currentUserIdOpt.get());
    }

    /**
     * Vérifie si l'utilisateur courant est le propriétaire ou lance une exception.
     *
     * @param ownerId L'ID du propriétaire de la ressource.
     * @throws AccessDeniedException si non propriétaire ou non authentifié.
     */
    public void checkIsOwnerOrThrow(Integer ownerId) {
        if (!isOwner(ownerId)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le propriétaire de cette ressource.");
        }
    }

    // --- Vérifications liées aux Clubs (Existantes + Complétées) ---

    /**
     * Vérifie si l'utilisateur courant est membre du club (quel que soit le rôle).
     */
    @Transactional(readOnly = true)
    public boolean isCurrentUserMemberOfClub(Integer clubId) {
        return getCurrentUserIdOptional()
                .map(userId -> adhesionRepository.existsByMembreIdAndClubId(userId, clubId))
                .orElse(false); // Non authentifié -> pas membre
    }

    /**
     * Vérifie si membre du club ou lance exception.
     */
    public void checkIsCurrentUserMemberOfClubOrThrow(Integer clubId) {
        if (!isCurrentUserMemberOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas membre du club ID " + clubId);
        }
    }

    /**
     * Vérifie si l'utilisateur courant est MANAGER (ADMIN ou RESERVATION) du club.
     */
    @Transactional(readOnly = true)
    public boolean isManagerOfClub(Integer clubId) {
        // Utilise la requête DAO optimisée qui récupère ID et Role
        Optional<Object[]> resultOpt = getCurrentUserIdOptional()
                .flatMap(userId -> adhesionRepository.findMembreIdAndRoleByMembreIdAndClubId(userId, clubId));

        if (resultOpt.isEmpty()) {
            return false; // Pas d'adhésion trouvée ou non authentifié
        }
        Object[] result = resultOpt.get();
        // result[0] est userId (pas utile ici), result[1] est Role
        Role userRole = (Role) result[1];
        return userRole == Role.ADMIN || userRole == Role.RESERVATION;
    }

    /**
     * Vérifie si manager du club ou lance exception.
     */
    public void checkManagerOfClubOrThrow(Integer clubId) {
        if (!isManagerOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Droits de gestionnaire (ADMIN/RESERVATION) requis pour le club ID " + clubId);
        }
    }

    /**
     * Vérifie si l'utilisateur courant est ADMIN et membre du club spécifié.
     * (Basé sur le rôle global du Membre).
     */
    @Transactional(readOnly = true)
    public boolean isActualAdminOfClub(Integer clubId) {
        // 1. Récupérer l'ID de l'utilisateur courant
        Optional<Integer> userIdOpt = getCurrentUserIdOptional();
        if (userIdOpt.isEmpty()) {
            return false; // Utilisateur non connecté
        }
        Integer userId = userIdOpt.get();

        // 2. Vérifier si une adhésion existe entre l'utilisateur et le club
        //    (On suppose que AdhesionDao a cette méthode simple)
        boolean isMemberOfClub = adhesionRepository.existsByMembreIdAndClubId(userId, clubId);

        if (!isMemberOfClub) {
            return false; // L'utilisateur n'est pas membre du club
        }

        // 3. Si membre, récupérer le rôle global de l'utilisateur
        //    On récupère le Membre complet pour obtenir son rôle
        Optional<Membre> membreOpt = membreRepository.findById(userId);
        if (membreOpt.isEmpty()) {
            // Ne devrait pas arriver si une adhésion existe, mais sécurité
            return false;
        }

        // 4. Vérifier si le rôle global est ADMIN
        return membreOpt.get().getRole() == Role.ADMIN;
    }

    /**
     * Vérifie si admin spécifique du club ou lance exception.
     */
    public void checkIsActualAdminOfClubOrThrow(Integer clubId) {
        if (!isActualAdminOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Droits d'administrateur (ADMIN) requis pour le club ID " + clubId);
        }
    }


    // --- Vérifications liées aux Événements ---

    /**
     * Vérifie si l'utilisateur courant est MANAGER du club qui organise cet événement.
     *
     * @param eventId L'ID de l'événement.
     * @return true si manager du club organisateur.
     * @throws EntityNotFoundException si l'événement n'existe pas.
     */
    @Transactional(readOnly = true)
    public boolean isManagerOfEventClub(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé pour vérification sécurité (ID: " + eventId + ")"));
        return isManagerOfClub(event.getOrganisateur().getId());
    }

    /**
     * Vérifie si manager du club de l'événement ou lance exception.
     */
    public void checkManagerOfEventClubOrThrow(Integer eventId) {
        if (!isManagerOfEventClub(eventId)) {
            // Le message d'erreur de EntityNotFound (si event non trouvé) sera lancé avant
            throw new AccessDeniedException("Accès refusé : Droits de gestionnaire requis pour le club organisateur de cet événement.");
        }
    }

    /**
     * Vérifie si l'utilisateur courant est MEMBRE du club qui organise cet événement.
     * Utile pour la consultation des détails d'un événement ou de ses catégories/notations.
     *
     * @param eventId L'ID de l'événement.
     * @return true si membre du club organisateur.
     * @throws EntityNotFoundException si l'événement n'existe pas.
     */
    @Transactional(readOnly = true)
    public boolean isMemberOfEventClub(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé pour vérification sécurité (ID: " + eventId + ")"));
        return isCurrentUserMemberOfClub(event.getOrganisateur().getId());
    }

    /**
     * Vérifie si membre du club de l'événement ou lance exception.
     */
    public void checkMemberOfEventClubOrThrow(Integer eventId) {
        if (!isMemberOfEventClub(eventId)) {
            throw new AccessDeniedException("Accès refusé : Vous devez être membre du club organisateur pour accéder à cet événement.");
        }
    }

    // --- Vérifications liées aux Réservations ---

    /**
     * Vérifie si l'utilisateur courant est le propriétaire de la réservation OU manager du club organisateur.
     * Utile pour voir/annuler une réservation.
     *
     * @param reservation La réservation à vérifier (doit être chargée, pas juste l'ID).
     * @return true si autorisé.
     */
    @Transactional(readOnly = true) // readOnly car on lit les relations
    public boolean isOwnerOrManagerForReservation(Reservation reservation) {
        if (reservation == null || reservation.getMembre() == null || reservation.getEvent() == null || reservation.getEvent().getOrganisateur() == null) {
            // Gérer le cas où les données sont incomplètes
            System.err.println("WARN: Vérification sécurité sur réservation avec données incomplètes."); // -> Logger
            return false;
        }
        Integer ownerId = reservation.getMembre().getId();
        Integer clubId = reservation.getEvent().getOrganisateur().getId();

        // Vérifier si propriétaire OU manager du club associé
        return isOwner(ownerId) || isManagerOfClub(clubId);
    }

    /**
     * Vérifie si propriétaire ou manager pour une réservation ou lance exception.
     */
    public void checkIsOwnerOrManagerOfAssociatedClubOrThrow(Reservation reservation) {
        if (!isOwnerOrManagerForReservation(reservation)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le propriétaire de cette réservation ou gestionnaire du club associé.");
        }
    }

    // --- Vérifications liées aux Demandes d'Ami (Exemples) ---

    /**
     * Vérifie si l'utilisateur courant est le récepteur d'une demande.
     */
    public void checkIsRecepteurOfDemandeOrThrow(DemandeAmi demande) {
        Integer currentUserId = getCurrentUserIdOrThrow();
        if (demande == null || demande.getRecepteur() == null || !currentUserId.equals(demande.getRecepteur().getId())) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le destinataire de cette demande d'ami.");
        }
    }

    /**
     * Vérifie si l'utilisateur courant est l'envoyeur d'une demande.
     */
    public void checkIsEnvoyeurOfDemandeOrThrow(DemandeAmi demande) {
        Integer currentUserId = getCurrentUserIdOrThrow();
        if (demande == null || demande.getEnvoyeur() == null || !currentUserId.equals(demande.getEnvoyeur().getId())) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas l'expéditeur de cette demande d'ami.");
        }
    }

    // --- Vérifications plus complexes (Exemples pour MembreController) ---

    /**
     * Vérifie si l'utilisateur courant est le propriétaire de l'ID cible OU un admin global (logique admin global à définir).
     * Utile pour GET /api/membres/{id} ou DELETE /api/membres/{id} (si admin global peut supprimer).
     */
    public void checkIsOwnerOrGlobalAdminOrThrow(Integer targetUserId) {
        // boolean isGlobalAdmin = checkIsGlobalAdmin(); // Logique à définir si nécessaire
        if (!isOwner(targetUserId) /* && !isGlobalAdmin */) {
            throw new AccessDeniedException("Accès refusé : Vous ne pouvez accéder ou modifier que votre propre profil.");
        }
    }

    /**
     * Vérifie si l'utilisateur courant est l'auteur d'une notation OU l'admin du club organisateur.
     * Utile pour DELETE /api/notations/{id}.
     */
    public void checkIsOwnerOrAdminOfClubOrThrow(Integer notationOwnerId, Integer clubId) {
        // boolean isAdmin = isActualAdminOfClub(clubId); // Est-il admin du club ?
        // boolean owner = isOwner(notationOwnerId); // Est-ce l'auteur ?
        if (!isOwner(notationOwnerId) && !isActualAdminOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Seul l'auteur ou l'administrateur du club peut supprimer cette notation.");
        }
    }


    // Ajoutez d'autres méthodes check...OrThrow selon les besoins spécifiques de vos services...
    // Exemple : checkCanManageCategorieOrThrow(Integer categorieId), checkCanReserveCategorieOrThrow(Integer categorieId), etc.

}

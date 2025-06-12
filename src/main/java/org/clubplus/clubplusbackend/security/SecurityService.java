package org.clubplus.clubplusbackend.security;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Service centralisant les vérifications de sécurité réutilisables.
 * <p>
 * Fournit des méthodes pour vérifier les droits de l'utilisateur courant, son appartenance à des clubs,
 * et la propriété de ressources. Ce service peut être utilisé dans les expressions SpEL
 * des annotations de sécurité (ex: @PreAuthorize).
 */
@Component("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;

    // --- Méthodes d'accès à l'utilisateur courant ---

    /**
     * Récupère l'ID de l'utilisateur actuellement authentifié.
     * Lève une exception si l'utilisateur n'est pas authentifié.
     *
     * @return L'ID de l'utilisateur authentifié.
     * @throws AccessDeniedException si aucun utilisateur n'est authentifié.
     */
    public Integer getCurrentUserIdOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            throw new AccessDeniedException("Utilisateur non authentifié ou informations invalides.");
        }
        Integer userId = userDetails.getId();
        if (userId == null) {
            log.error("Incohérence critique : ID utilisateur null pour l'utilisateur authentifié '{}'.", userDetails.getUsername());
            throw new AccessDeniedException("ID utilisateur non trouvé dans le contexte de sécurité.");
        }
        return userId;
    }

    /**
     * Récupère l'entité {@link Membre} de l'utilisateur actuellement authentifié.
     *
     * @return L'entité {@link Membre} de l'utilisateur courant.
     * @throws AccessDeniedException   si l'utilisateur n'est pas authentifié.
     * @throws EntityNotFoundException si l'utilisateur authentifié n'est pas trouvé en base de données.
     */
    @Transactional(readOnly = true)
    public Membre getCurrentMembreOrThrow() {
        Integer userId = getCurrentUserIdOrThrow();
        return membreRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Incohérence critique : Membre authentifié (ID: {}) non trouvé en BDD.", userId);
                    return new EntityNotFoundException("Membre authentifié (ID: " + userId + ") introuvable.");
                });
    }

    /**
     * Récupère l'ID de l'utilisateur courant de manière optionnelle (ne lève pas d'exception).
     *
     * @return Un {@code Optional<Integer>} contenant l'ID, ou vide si non authentifié.
     */
    private Optional<Integer> getCurrentUserIdOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            return Optional.empty();
        }
        return Optional.ofNullable(userDetails.getId());
    }

    /**
     * Récupère l'entité {@link Membre} de l'utilisateur courant de manière optionnelle.
     *
     * @return Un {@code Optional<Membre>} contenant l'entité, ou vide si non authentifié ou non trouvé.
     */
    @Transactional(readOnly = true)
    protected Optional<Membre> getCurrentMembreOptional() {
        return getCurrentUserIdOptional().flatMap(membreRepository::findById);
    }

    // --- Vérifications de propriété ---

    /**
     * Vérifie si l'utilisateur courant est le propriétaire d'une ressource.
     *
     * @param ownerId L'ID du propriétaire présumé.
     * @return {@code true} si l'utilisateur courant est le propriétaire, {@code false} sinon.
     */
    public boolean isOwner(Integer ownerId) {
        if (ownerId == null) return false;
        return getCurrentUserIdOptional().map(ownerId::equals).orElse(false);
    }

    /**
     * Vérifie si l'utilisateur courant est le propriétaire, sinon lève une exception.
     *
     * @param ownerId L'ID du propriétaire présumé.
     * @throws AccessDeniedException si l'utilisateur n'est pas le propriétaire.
     */
    public void checkIsOwnerOrThrow(Integer ownerId) {
        if (!isOwner(ownerId)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le propriétaire de cette ressource.");
        }
    }

    // --- Vérifications liées aux Clubs ---

    /**
     * Vérifie si l'utilisateur courant est membre d'un club spécifique.
     *
     * @param clubId L'ID du club.
     * @return {@code true} si l'utilisateur est membre, {@code false} sinon.
     */
    @Transactional(readOnly = true)
    public boolean isCurrentUserMemberOfClub(Integer clubId) {
        if (clubId == null) return false;
        return getCurrentUserIdOptional()
                .map(userId -> adhesionRepository.existsByMembreIdAndClubId(userId, clubId))
                .orElse(false);
    }

    /**
     * Vérifie si l'utilisateur courant est membre du club, sinon lève une exception.
     *
     * @param clubId L'ID du club.
     * @throws AccessDeniedException si l'utilisateur n'est pas membre.
     */
    public void checkIsCurrentUserMemberOfClubOrThrow(Integer clubId) {
        if (!isCurrentUserMemberOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas membre du club ID " + clubId);
        }
    }

    /**
     * Vérifie si l'utilisateur courant a un rôle de gestion (ADMIN ou RESERVATION) dans un club spécifique.
     *
     * @param clubId L'ID du club.
     * @return {@code true} si l'utilisateur est un gestionnaire du club, {@code false} sinon.
     */
    @Transactional(readOnly = true)
    public boolean isManagerOfClub(Integer clubId) {
        if (clubId == null) return false;
        Optional<Membre> membreOpt = getCurrentMembreOptional();
        if (membreOpt.isEmpty()) return false;

        Membre membre = membreOpt.get();
        Role userRole = membre.getRole();

        boolean isManagerRole = userRole == Role.ADMIN || userRole == Role.RESERVATION;
        return isManagerRole && adhesionRepository.existsByMembreIdAndClubId(membre.getId(), clubId);
    }

    /**
     * Vérifie si l'utilisateur courant est gestionnaire du club, sinon lève une exception.
     *
     * @param clubId L'ID du club.
     * @throws AccessDeniedException si l'utilisateur n'est pas un gestionnaire du club.
     */
    public void checkManagerOfClubOrThrow(Integer clubId) {
        if (!isManagerOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Droits de gestionnaire (ADMIN/RESERVATION) requis pour le club ID " + clubId);
        }
    }

    /**
     * Vérifie si l'utilisateur courant a le rôle ADMIN dans un club spécifique.
     *
     * @param clubId L'ID du club.
     * @return {@code true} si l'utilisateur est admin du club, {@code false} sinon.
     */
    @Transactional(readOnly = true)
    public boolean isActualAdminOfClub(Integer clubId) {
        if (clubId == null) return false;
        Optional<Membre> membreOpt = getCurrentMembreOptional();
        if (membreOpt.isEmpty()) return false;

        Membre membre = membreOpt.get();
        return membre.getRole() == Role.ADMIN && adhesionRepository.existsByMembreIdAndClubId(membre.getId(), clubId);
    }

    /**
     * Vérifie si l'utilisateur courant est admin du club, sinon lève une exception.
     *
     * @param clubId L'ID du club.
     * @throws AccessDeniedException si l'utilisateur n'est pas admin du club.
     */
    public void checkIsActualAdminOfClubOrThrow(Integer clubId) {
        if (!isActualAdminOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Droits d'administrateur (ADMIN) requis pour le club ID " + clubId);
        }
    }


    // --- Vérifications liées aux Événements ---

    /**
     * Vérifie si l'utilisateur courant est gestionnaire du club qui organise un événement.
     *
     * @param eventId L'ID de l'événement.
     * @return {@code true} si l'utilisateur est gestionnaire du club organisateur, {@code false} sinon.
     */
    @Transactional(readOnly = true)
    public boolean isManagerOfEventClub(Integer eventId) {
        if (eventId == null) return false;
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));
        if (event.getOrganisateur() == null) {
            log.warn("L'événement ID {} n'a pas d'organisateur défini.", eventId);
            return false;
        }
        return isManagerOfClub(event.getOrganisateur().getId());
    }

    /**
     * Vérifie si l'utilisateur courant est membre du club qui organise un événement.
     *
     * @param eventId L'ID de l'événement.
     * @return {@code true} si l'utilisateur est membre du club organisateur, {@code false} sinon.
     */
    @Transactional(readOnly = true)
    public boolean isMemberOfEventClub(Integer eventId) {
        if (eventId == null) return false;
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));
        if (event.getOrganisateur() == null) {
            log.warn("L'événement ID {} n'a pas d'organisateur défini.", eventId);
            return false;
        }
        return isCurrentUserMemberOfClub(event.getOrganisateur().getId());
    }

    /**
     * Vérifie si l'utilisateur courant est membre du club organisateur, sinon lève une exception.
     *
     * @param eventId L'ID de l'événement.
     * @throws AccessDeniedException si l'utilisateur n'est pas membre.
     */
    public void checkMemberOfEventClubOrThrow(Integer eventId) {
        if (!isMemberOfEventClub(eventId)) {
            throw new AccessDeniedException("Accès refusé : Vous devez être membre du club organisateur.");
        }
    }

    // --- Vérifications liées aux Réservations ---

    /**
     * Vérifie si l'utilisateur courant est le propriétaire de la réservation ou un gestionnaire du club organisateur.
     *
     * @param reservation L'entité {@link Reservation} à vérifier.
     * @return {@code true} si l'utilisateur a les droits, {@code false} sinon.
     */
    @Transactional(readOnly = true)
    public boolean isOwnerOrManagerForReservation(Reservation reservation) {
        if (reservation == null || reservation.getMembre() == null || reservation.getEvent() == null || reservation.getEvent().getOrganisateur() == null) {
            log.warn("Tentative de vérification sur une réservation avec des données liées incomplètes.");
            return false;
        }
        return isOwner(reservation.getMembre().getId()) || isManagerOfClub(reservation.getEvent().getOrganisateur().getId());
    }

    /**
     * Vérifie les droits sur une réservation, sinon lève une exception.
     *
     * @param reservation L'entité {@link Reservation}.
     * @throws AccessDeniedException si l'utilisateur n'a pas les droits.
     */
    public void checkIsOwnerOrManagerOfAssociatedClubOrThrow(Reservation reservation) {
        if (!isOwnerOrManagerForReservation(reservation)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes ni le propriétaire de cette réservation, ni un gestionnaire du club associé.");
        }
    }

    // --- Vérifications liées aux Demandes d'Ami ---

    /**
     * Vérifie si l'utilisateur courant est le destinataire de la demande, sinon lève une exception.
     *
     * @param demande La {@link DemandeAmi}.
     * @throws AccessDeniedException si l'utilisateur n'est pas le destinataire.
     */
    public void checkIsRecepteurOfDemandeOrThrow(DemandeAmi demande) {
        Integer currentUserId = getCurrentUserIdOrThrow();
        if (demande == null || demande.getRecepteur() == null || !currentUserId.equals(demande.getRecepteur().getId())) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le destinataire de cette demande.");
        }
    }

    /**
     * Vérifie si l'utilisateur courant est l'envoyeur de la demande, sinon lève une exception.
     *
     * @param demande La {@link DemandeAmi}.
     * @throws AccessDeniedException si l'utilisateur n'est pas l'envoyeur.
     */
    public void checkIsEnvoyeurOfDemandeOrThrow(DemandeAmi demande) {
        Integer currentUserId = getCurrentUserIdOrThrow();
        if (demande == null || demande.getEnvoyeur() == null || !currentUserId.equals(demande.getEnvoyeur().getId())) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas l'expéditeur de cette demande.");
        }
    }

    // --- Vérifications complexes ---

    /**
     * Vérifie si l'utilisateur courant est le propriétaire d'un profil ou un administrateur global.
     *
     * @param targetUserId L'ID du profil cible.
     * @throws AccessDeniedException si l'utilisateur n'a pas les droits.
     */
    public void checkIsOwnerOrGlobalAdminOrThrow(Integer targetUserId) {
        // TODO: Définir la logique pour 'isGlobalAdmin' si ce concept existe.
        boolean isGlobalAdmin = false;
        if (!isOwner(targetUserId) && !isGlobalAdmin) {
            throw new AccessDeniedException("Accès refusé : Action non autorisée sur ce profil.");
        }
    }

    /**
     * Vérifie si l'utilisateur est l'auteur d'une notation ou l'admin du club de l'événement noté.
     *
     * @param notationOwnerId L'ID de l'auteur de la notation.
     * @param clubId          L'ID du club organisateur.
     * @throws AccessDeniedException si l'utilisateur n'a pas les droits.
     */
    public void checkIsOwnerOrAdminOfClubOrThrow(Integer notationOwnerId, Integer clubId) {
        if (!isOwner(notationOwnerId) && !isActualAdminOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Seul l'auteur ou l'administrateur du club peut gérer cette notation.");
        }
    }

    /**
     * Récupère l'ID du club géré par l'utilisateur courant.
     * <p>
     * <b>Hypothèse:</b> Un utilisateur avec un rôle de gestion (ADMIN/RESERVATION) ne gère qu'un seul club.
     *
     * @return L'ID du club géré.
     * @throws AccessDeniedException si l'utilisateur n'est pas un gestionnaire ou n'est pas associé à un club.
     * @throws IllegalStateException si un gestionnaire est associé à plus d'un club, indiquant une incohérence de données.
     */
    @Transactional(readOnly = true)
    public Integer getCurrentUserManagedClubIdOrThrow() {
        Membre currentUser = getCurrentMembreOrThrow();
        Role userRole = currentUser.getRole();

        if (userRole != Role.ADMIN && userRole != Role.RESERVATION) {
            throw new AccessDeniedException("Accès refusé : Rôle ADMIN ou RESERVATION requis.");
        }

        Set<Adhesion> adhesions = currentUser.getAdhesions();
        if (adhesions == null || adhesions.isEmpty()) {
            log.error("Incohérence : Utilisateur gestionnaire {} (ID: {}) n'a aucune adhésion.", currentUser.getEmail(), currentUser.getId());
            throw new AccessDeniedException("Accès refusé : L'utilisateur gestionnaire n'est associé à aucun club.");
        }
        if (adhesions.size() > 1) {
            log.error("Incohérence : Utilisateur gestionnaire {} (ID: {}) est associé à {} clubs. Le modèle suppose un seul club géré.",
                    currentUser.getEmail(), currentUser.getId(), adhesions.size());
            throw new IllegalStateException("Configuration invalide : Un gestionnaire ne peut être associé qu'à un seul club.");
        }

        Adhesion managedAdhesion = adhesions.iterator().next();
        if (managedAdhesion.getClub() == null || managedAdhesion.getClub().getId() == null) {
            log.error("Incohérence : L'adhésion (ID: {}) du gestionnaire {} ne référence pas un club valide.", managedAdhesion.getId(), currentUser.getEmail());
            throw new EntityNotFoundException("Incohérence des données : Le club associé à la gestion est introuvable.");
        }
        return managedAdhesion.getClub().getId();
    }
}

package org.clubplus.clubplusbackend.security; // Ou un autre package approprié

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
 * Service centralisant les vérifications de sécurité réutilisables pour l'application ClubPlus.
 * Fournit des méthodes pour vérifier les droits de l'utilisateur courant, son appartenance à des clubs,
 * ses droits de gestion, la propriété de certaines ressources (réservations, demandes d'ami, etc.).
 *
 * <p>Ce service interagit avec le {@link SecurityContextHolder} pour obtenir les informations
 * de l'utilisateur authentifié et utilise divers DAOs ({@link MembreDao}, {@link AdhesionDao}, etc.)
 * pour récupérer les données nécessaires aux vérifications depuis la base de données.</p>
 *
 * <p>Il est marqué comme un {@link Component @Component} Spring avec le nom "securityService",
 * ce qui permet de l'utiliser directement dans les expressions SpEL (Spring Expression Language)
 * des annotations de sécurité comme {@code @PreAuthorize("@securityService.isManagerOfClub(#clubId)")}.</p>
 *
 * <p>Beaucoup de méthodes suivent un pattern :</p>
 * <ul>
 *     <li>{@code isXxx(...)} : Retourne un booléen, souvent utilisé dans les conditions ou pour des vérifications non bloquantes.</li>
 *     <li>{@code checkXxxOrThrow(...)} : Ne retourne rien (void) mais lève une {@link AccessDeniedException}
 *         (ou parfois {@link EntityNotFoundException}) si la condition de sécurité n'est pas remplie. À utiliser
 *         pour appliquer des règles de sécurité impératives au début d'une méthode de service/contrôleur.</li>
 * </ul>
 *
 * <p>Les méthodes nécessitant un accès à la base de données sont annotées avec
 * {@code @Transactional(readOnly = true)} pour l'optimisation.</p>
 *
 * @see SecurityContextHolder
 * @see Authentication
 * @see AppUserDetails
 * @see AccessDeniedException
 * @see EntityNotFoundException
 * @see Transactional
 */
@Component("securityService") // Nom du bean pour SpEL dans @PreAuthorize
@RequiredArgsConstructor // Lombok: Injecte les dépendances final via le constructeur
public class SecurityService {

    // Logger pour tracer les avertissements ou informations pertinentes.
    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    // --- Dépendances DAO Injectées ---
    /**
     * DAO pour accéder aux données des adhésions (liens Membre-Club).
     */
    private final AdhesionDao adhesionRepository;
    /**
     * DAO pour accéder aux données des membres.
     */
    private final MembreDao membreRepository;
    /**
     * DAO pour accéder aux données des événements.
     */
    private final EventDao eventRepository;

    // --- Helpers Internes pour l'Utilisateur Courant ---

    /**
     * Récupère l'ID de l'utilisateur actuellement authentifié via {@link SecurityContextHolder}.
     * C'est une méthode critique utilisée par de nombreuses autres vérifications.
     * Elle lève une exception si l'utilisateur n'est pas correctement authentifié
     * (contexte de sécurité vide, non authentifié, ou principal d'un type inattendu).
     * À utiliser lorsque l'authentification est une condition préalable stricte.
     *
     * @return L'ID (Integer) unique de l'utilisateur authentifié.
     * @throws AccessDeniedException si l'utilisateur n'est pas authentifié, si le principal
     *                               n'est pas une instance de {@link AppUserDetails}, ou si l'ID utilisateur est null
     *                               dans les détails d'authentification (ce dernier cas indique une incohérence grave).
     *                               Cette exception est typiquement gérée globalement pour retourner un statut HTTP 401 ou 403.
     */
    public Integer getCurrentUserIdOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Vérifie si une authentification valide existe et si le principal est du type attendu
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            throw new AccessDeniedException("Utilisateur non authentifié ou informations d'authentification invalides.");
        }
        Integer userId = userDetails.getId();
        // Vérification supplémentaire de sécurité
        if (userId == null) {
            log.error("Incohérence critique : ID utilisateur null dans AppUserDetails pour l'utilisateur authentifié '{}'.", userDetails.getUsername());
            throw new AccessDeniedException("ID utilisateur non trouvé dans les informations d'authentification.");
        }
        return userId;
    }

    /**
     * Récupère l'entité {@link Membre} complète correspondant à l'utilisateur actuellement authentifié,
     * en effectuant une recherche en base de données via son ID.
     * Utilise {@link #getCurrentUserIdOrThrow()} pour obtenir l'ID.
     * Utile lorsque les informations complètes du membre (rôle, adhésions, etc.) sont nécessaires pour une vérification.
     *
     * @return L'entité {@link Membre} de l'utilisateur courant. Ne retourne jamais null.
     * @throws AccessDeniedException   si l'utilisateur n'est pas authentifié (via {@code getCurrentUserIdOrThrow}).
     * @throws EntityNotFoundException si l'utilisateur authentifié (par son ID) n'est pas trouvé en base de données,
     *                                 ce qui signale une incohérence critique entre le contexte de sécurité et l'état de la BDD.
     *                                 Cette exception est typiquement gérée globalement pour retourner un statut HTTP 404.
     */
    @Transactional(readOnly = true) // Opération de lecture
    public Membre getCurrentMembreOrThrow() {
        Integer userId = getCurrentUserIdOrThrow(); // Vérifie l'authentification et récupère l'ID
        // Recherche le membre en BDD et lève une exception claire si non trouvé
        return membreRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Incohérence critique : Membre authentifié (ID: {}) non trouvé dans la base de données.", userId);
                    return new EntityNotFoundException("Membre authentifié (ID: " + userId + ") non trouvé dans la base de données.");
                });
    }

    /**
     * Récupère l'ID de l'utilisateur courant sous forme d'{@link Optional}.
     * Version non bloquante de {@link #getCurrentUserIdOrThrow()}, retournant un Optional vide
     * si l'utilisateur n'est pas authentifié. Principalement destinée à être utilisée en interne
     * par les méthodes {@code isXxx()} pour éviter de lever des exceptions prématurément.
     *
     * @return Un {@code Optional<Integer>} contenant l'ID utilisateur s'il est authentifié,
     * ou un Optional vide sinon.
     */
    private Optional<Integer> getCurrentUserIdOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            return Optional.empty(); // Pas authentifié ou principal incorrect
        }
        // Retourne un Optional basé sur l'ID (qui pourrait théoriquement être null, bien que peu probable)
        return Optional.ofNullable(userDetails.getId());
    }

    /**
     * Récupère l'entité {@link Membre} de l'utilisateur courant sous forme d'{@link Optional}.
     * Utilise {@link #getCurrentUserIdOptional()} et recherche en BDD.
     * Version non bloquante de {@link #getCurrentMembreOrThrow()}.
     *
     * @return Un {@code Optional<Membre>} contenant l'entité si l'utilisateur est authentifié et trouvé en BDD,
     * ou un Optional vide sinon.
     */
    @Transactional(readOnly = true) // Nécessite une transaction pour findById
    protected Optional<Membre> getCurrentMembreOptional() {
        // Enchaîne l'Optional de l'ID avec la recherche en base de données
        return getCurrentUserIdOptional().flatMap(membreRepository::findById);
    }

    // --- Vérifications de Propriété (Ownership) ---

    /**
     * Vérifie si l'utilisateur actuellement authentifié est le propriétaire d'une ressource donnée,
     * en comparant son ID avec l'ID du propriétaire fourni.
     *
     * @param ownerId L'ID (Integer) du propriétaire présumé de la ressource. Peut être null.
     * @return {@code true} si l'utilisateur courant est authentifié et que son ID correspond à {@code ownerId},
     * {@code false} si {@code ownerId} est null, si l'utilisateur n'est pas authentifié, ou si les IDs ne correspondent pas.
     */
    public boolean isOwner(Integer ownerId) {
        if (ownerId == null) {
            return false; // On ne peut pas être propriétaire de 'null'
        }
        // Récupère l'ID courant de manière optionnelle
        Optional<Integer> currentUserIdOpt = getCurrentUserIdOptional();
        // Compare l'ID courant (s'il existe) avec l'ID propriétaire
        return currentUserIdOpt.isPresent() && ownerId.equals(currentUserIdOpt.get());
    }

    /**
     * Vérifie si l'utilisateur courant est le propriétaire d'une ressource (basé sur l'ID).
     * Si la vérification échoue (non propriétaire ou non authentifié), lève une {@link AccessDeniedException}.
     *
     * @param ownerId L'ID (Integer) du propriétaire présumé de la ressource.
     * @throws AccessDeniedException si l'utilisateur courant n'est pas le propriétaire
     *                               ou s'il n'est pas authentifié.
     */
    public void checkIsOwnerOrThrow(Integer ownerId) {
        if (!isOwner(ownerId)) {
            // Lève une exception si la vérification booléenne échoue
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le propriétaire de cette ressource.");
        }
    }

    // --- Vérifications liées aux Clubs ---

    /**
     * Vérifie si l'utilisateur actuellement authentifié est membre du club spécifié (via une adhésion),
     * indépendamment de son rôle dans ce club.
     *
     * @param clubId L'ID (Integer) du club à vérifier.
     * @return {@code true} si l'utilisateur est authentifié et a une adhésion active pour ce club,
     * {@code false} sinon (y compris s'il n'est pas authentifié).
     */
    @Transactional(readOnly = true) // Nécessite accès BDD via adhesionRepository
    public boolean isCurrentUserMemberOfClub(Integer clubId) {
        if (clubId == null) return false;
        // Utilise l'ID optionnel et la méthode existsBy... du DAO
        return getCurrentUserIdOptional()
                .map(userId -> adhesionRepository.existsByMembreIdAndClubId(userId, clubId))
                .orElse(false); // Si non authentifié, retourne false
    }

    /**
     * Vérifie si l'utilisateur courant est membre du club spécifié.
     * Si la vérification échoue (non membre ou non authentifié), lève une {@link AccessDeniedException}.
     *
     * @param clubId L'ID (Integer) du club.
     * @throws AccessDeniedException si l'utilisateur n'est pas membre du club ou n'est pas authentifié.
     */
    public void checkIsCurrentUserMemberOfClubOrThrow(Integer clubId) {
        if (!isCurrentUserMemberOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas membre du club ID " + clubId);
        }
    }

    /**
     * Vérifie si l'utilisateur actuellement authentifié a un rôle de gestion (ADMIN ou RESERVATION)
     * ET est membre du club spécifié.
     *
     * @param clubId L'ID (Integer) du club.
     * @return {@code true} si l'utilisateur est authentifié, membre du club, et a le rôle ADMIN ou RESERVATION,
     * {@code false} sinon.
     */
    @Transactional(readOnly = true) // Nécessite BDD pour adhésion et rôle membre
    public boolean isManagerOfClub(Integer clubId) {
        if (clubId == null) return false;
        // Récupère le membre optionnel (gère l'authentification et l'existence en BDD)
        Optional<Membre> membreOpt = getCurrentMembreOptional();
        if (membreOpt.isEmpty()) {
            return false; // Non authentifié ou non trouvé en BDD
        }
        Membre membre = membreOpt.get();

        // Vérifie si membre du club spécifié
        boolean isMemberOfClub = adhesionRepository.existsByMembreIdAndClubId(membre.getId(), clubId);
        if (!isMemberOfClub) {
            return false; // Pas membre de ce club
        }

        // Vérifie si le rôle global est ADMIN ou RESERVATION
        Role userRole = membre.getRole();
        return userRole == Role.ADMIN || userRole == Role.RESERVATION;
    }

    /**
     * Vérifie si l'utilisateur courant est manager (ADMIN ou RESERVATION) du club spécifié.
     * Si la vérification échoue, lève une {@link AccessDeniedException}.
     *
     * @param clubId L'ID (Integer) du club.
     * @throws AccessDeniedException si l'utilisateur n'est pas manager du club ou n'est pas authentifié/membre.
     */
    public void checkManagerOfClubOrThrow(Integer clubId) {
        if (!isManagerOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Droits de gestionnaire (ADMIN/RESERVATION) requis pour le club ID " + clubId);
        }
    }

    /**
     * Vérifie si l'utilisateur actuellement authentifié a spécifiquement le rôle ADMIN global
     * ET est membre du club spécifié.
     *
     * @param clubId L'ID (Integer) du club.
     * @return {@code true} si l'utilisateur est authentifié, membre du club, et a le rôle ADMIN,
     * {@code false} sinon.
     */
    @Transactional(readOnly = true) // Nécessite BDD pour adhésion et rôle membre
    public boolean isActualAdminOfClub(Integer clubId) {
        if (clubId == null) return false;
        // Récupère le membre optionnel
        Optional<Membre> membreOpt = getCurrentMembreOptional();
        if (membreOpt.isEmpty()) {
            return false; // Non authentifié ou non trouvé en BDD
        }
        Membre membre = membreOpt.get();

        // Vérifie si membre du club
        boolean isMemberOfClub = adhesionRepository.existsByMembreIdAndClubId(membre.getId(), clubId);
        if (!isMemberOfClub) {
            return false; // Pas membre
        }

        // Vérifie si le rôle global est spécifiquement ADMIN
        return membre.getRole() == Role.ADMIN;
    }

    /**
     * Vérifie si l'utilisateur courant est administrateur (rôle ADMIN) du club spécifié.
     * Si la vérification échoue, lève une {@link AccessDeniedException}.
     *
     * @param clubId L'ID (Integer) du club.
     * @throws AccessDeniedException si l'utilisateur n'a pas le rôle ADMIN pour ce club ou n'est pas authentifié/membre.
     */
    public void checkIsActualAdminOfClubOrThrow(Integer clubId) {
        if (!isActualAdminOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Droits d'administrateur (ADMIN) requis pour le club ID " + clubId);
        }
    }


    // --- Vérifications liées aux Événements ---

    /**
     * Vérifie si l'utilisateur actuellement authentifié est manager (ADMIN ou RESERVATION)
     * du club qui organise l'événement spécifié.
     *
     * @param eventId L'ID (Integer) de l'événement.
     * @return {@code true} si l'utilisateur est manager du club organisateur, {@code false} sinon.
     * @throws EntityNotFoundException si l'événement avec l'ID donné n'est pas trouvé en base de données.
     *                                 (Levée par la recherche initiale de l'événement).
     */
    @Transactional(readOnly = true) // Nécessite BDD pour l'événement et les vérifs club/membre
    public boolean isManagerOfEventClub(Integer eventId) {
        if (eventId == null) return false;
        // Récupère l'événement, lève EntityNotFoundException si non trouvé
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé pour vérification sécurité (ID: " + eventId + ")"));
        // Vérifie si l'utilisateur courant est manager du club organisateur de cet événement
        if (event.getOrganisateur() == null) {
            log.warn("L'événement ID {} n'a pas de club organisateur défini.", eventId);
            return false; // Ou lancer une exception si un event doit TOUJOURS avoir un organisateur
        }
        return isManagerOfClub(event.getOrganisateur().getId());
    }

    /**
     * Vérifie si l'utilisateur actuellement authentifié est membre (quel que soit son rôle)
     * du club qui organise l'événement spécifié.
     * Utile pour autoriser la consultation des détails d'un événement, de ses catégories, etc.
     *
     * @param eventId L'ID (Integer) de l'événement.
     * @return {@code true} si l'utilisateur est membre du club organisateur, {@code false} sinon.
     * @throws EntityNotFoundException si l'événement avec l'ID donné n'est pas trouvé.
     */
    @Transactional(readOnly = true) // Nécessite BDD pour l'événement et l'adhésion
    public boolean isMemberOfEventClub(Integer eventId) {
        if (eventId == null) return false;
        // Récupère l'événement
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé pour vérification sécurité (ID: " + eventId + ")"));
        // Vérifie si l'utilisateur courant est membre du club organisateur
        if (event.getOrganisateur() == null) {
            log.warn("L'événement ID {} n'a pas de club organisateur défini.", eventId);
            return false;
        }
        return isCurrentUserMemberOfClub(event.getOrganisateur().getId());
    }

    /**
     * Vérifie si l'utilisateur courant est membre du club organisateur de l'événement.
     * Si l'événement n'existe pas, lève {@link EntityNotFoundException}.
     * Si l'utilisateur n'est pas membre, lève {@link AccessDeniedException}.
     *
     * @param eventId L'ID (Integer) de l'événement.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas membre du club organisateur.
     */
    public void checkMemberOfEventClubOrThrow(Integer eventId) {
        // isMemberOfEventClub gère EntityNotFoundException.
        if (!isMemberOfEventClub(eventId)) {
            throw new AccessDeniedException("Accès refusé : Vous devez être membre du club organisateur pour accéder à cet événement.");
        }
    }

    // --- Vérifications liées aux Réservations ---

    /**
     * Vérifie si l'utilisateur actuellement authentifié est soit le propriétaire de la réservation
     * (le membre qui a réservé), soit un manager (ADMIN ou RESERVATION) du club qui organise l'événement
     * associé à la réservation.
     * Utile pour les opérations comme consulter ou annuler une réservation spécifique.
     *
     * @param reservation L'entité {@link Reservation} complète (avec membre et événement chargés) à vérifier.
     * @return {@code true} si l'utilisateur est le propriétaire OU un manager du club de l'événement,
     * {@code false} si {@code reservation} est null, si les données liées sont incomplètes,
     * ou si aucune des conditions n'est remplie.
     */
    @Transactional(readOnly = true) // Nécessite potentiellement de lire le rôle/adhésion pour isManagerOfClub
    public boolean isOwnerOrManagerForReservation(Reservation reservation) {
        // Vérification de robustesse des données d'entrée
        if (reservation == null || reservation.getMembre() == null || reservation.getEvent() == null || reservation.getEvent().getOrganisateur() == null) {
            log.warn("Tentative de vérification de sécurité sur une réservation avec des données liées incomplètes (Reservation ID: {}, Membre ID: {}, Event ID: {}).",
                    reservation != null ? reservation.getId() : "null",
                    reservation != null && reservation.getMembre() != null ? reservation.getMembre().getId() : "null",
                    reservation != null && reservation.getEvent() != null ? reservation.getEvent().getId() : "null");
            return false;
        }
        Integer ownerId = reservation.getMembre().getId();
        Integer clubId = reservation.getEvent().getOrganisateur().getId();

        // Vérifie si l'utilisateur courant est le propriétaire OU un manager du club associé
        return isOwner(ownerId) || isManagerOfClub(clubId);
    }

    /**
     * Vérifie si l'utilisateur courant est propriétaire de la réservation ou manager du club associé.
     * Lève une {@link AccessDeniedException} si la vérification échoue.
     *
     * @param reservation L'entité {@link Reservation} complète à vérifier.
     * @throws AccessDeniedException si l'utilisateur n'est ni propriétaire ni manager, ou si les données sont invalides.
     */
    public void checkIsOwnerOrManagerOfAssociatedClubOrThrow(Reservation reservation) {
        if (!isOwnerOrManagerForReservation(reservation)) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le propriétaire de cette réservation ou gestionnaire du club associé.");
        }
    }

    // --- Vérifications liées aux Demandes d'Ami ---

    /**
     * Vérifie si l'utilisateur actuellement authentifié est le destinataire ({@code recepteur})
     * de la demande d'ami spécifiée.
     * Utile pour les actions comme accepter ou refuser une demande.
     *
     * @param demande L'entité {@link DemandeAmi} à vérifier.
     * @throws AccessDeniedException si l'utilisateur courant n'est pas le destinataire,
     *                               si {@code demande} est null, si le récepteur est null, ou si l'utilisateur n'est pas authentifié.
     */
    public void checkIsRecepteurOfDemandeOrThrow(DemandeAmi demande) {
        Integer currentUserId = getCurrentUserIdOrThrow(); // Vérifie l'authentification
        if (demande == null || demande.getRecepteur() == null || !currentUserId.equals(demande.getRecepteur().getId())) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas le destinataire de cette demande d'ami.");
        }
    }

    /**
     * Vérifie si l'utilisateur actuellement authentifié est l'expéditeur ({@code envoyeur})
     * de la demande d'ami spécifiée.
     * Utile pour l'action d'annuler une demande envoyée.
     *
     * @param demande L'entité {@link DemandeAmi} à vérifier.
     * @throws AccessDeniedException si l'utilisateur courant n'est pas l'expéditeur,
     *                               si {@code demande} est null, si l'envoyeur est null, ou si l'utilisateur n'est pas authentifié.
     */
    public void checkIsEnvoyeurOfDemandeOrThrow(DemandeAmi demande) {
        Integer currentUserId = getCurrentUserIdOrThrow(); // Vérifie l'authentification
        if (demande == null || demande.getEnvoyeur() == null || !currentUserId.equals(demande.getEnvoyeur().getId())) {
            throw new AccessDeniedException("Accès refusé : Vous n'êtes pas l'expéditeur de cette demande d'ami.");
        }
    }

    // --- Vérifications plus complexes (Exemples pour d'autres contrôleurs) ---

    /**
     * Vérifie si l'utilisateur courant est soit le propriétaire du profil ciblé (basé sur l'ID),
     * soit un administrateur global (cette dernière logique est un placeholder et doit être définie si nécessaire).
     * Utile pour des endpoints comme GET /api/membres/{id} (si seul le propriétaire ou un admin peut voir les détails)
     * ou DELETE /api/membres/{id} (si seul le propriétaire ou un admin peut supprimer).
     *
     * @param targetUserId L'ID (Integer) du profil membre cible.
     * @throws AccessDeniedException si l'utilisateur n'est ni propriétaire ni admin global (ou non authentifié).
     */
    public void checkIsOwnerOrGlobalAdminOrThrow(Integer targetUserId) {
        // TODO: Définir la logique pour 'isGlobalAdmin' si ce concept existe dans l'application.
        boolean isGlobalAdmin = false; // Placeholder - Mettre la vraie logique ici si nécessaire.
        if (!isOwner(targetUserId) && !isGlobalAdmin) {
            throw new AccessDeniedException("Accès refusé : Vous ne pouvez accéder ou modifier que votre propre profil.");
        }
    }

    /**
     * Vérifie si l'utilisateur courant est soit l'auteur d'une notation spécifique,
     * soit l'administrateur (rôle ADMIN) du club qui a organisé l'événement lié à la notation.
     * Utile pour des endpoints comme DELETE /api/notations/{id}.
     *
     * @param notationOwnerId L'ID (Integer) du membre auteur de la notation.
     * @param clubId          L'ID (Integer) du club organisateur de l'événement noté.
     * @throws AccessDeniedException si l'utilisateur n'est ni l'auteur ni l'admin du club concerné, ou non authentifié.
     */
    public void checkIsOwnerOrAdminOfClubOrThrow(Integer notationOwnerId, Integer clubId) {
        // Vérifie si l'utilisateur courant est propriétaire OU admin du club
        if (!isOwner(notationOwnerId) && !isActualAdminOfClub(clubId)) {
            throw new AccessDeniedException("Accès refusé : Seul l'auteur ou l'administrateur du club peut gérer cette notation.");
        }
    }

    /**
     * Récupère l'ID de l'unique club géré par l'utilisateur actuellement authentifié,
     * en vérifiant qu'il a bien le rôle requis (ADMIN ou RESERVATION) et qu'il est associé
     * (via l'entité {@link Adhesion}) à un et un seul club gérable.
     *
     * <p><b>Hypothèse clé du modèle :</b> Un utilisateur avec le rôle ADMIN ou RESERVATION
     * est toujours associé à exactement un club via l'entité Adhesion pour représenter
     * le club qu'il gère.</p>
     *
     * @return L'ID (Integer) du club géré par l'utilisateur.
     * @throws AccessDeniedException   Si l'utilisateur n'est pas authentifié, n'a pas le rôle ADMIN ou RESERVATION,
     *                                 ou n'est associé à aucun club via une adhésion (contrairement à l'hypothèse du modèle).
     * @throws EntityNotFoundException Si l'utilisateur authentifié n'est pas trouvé en BDD (incohérence),
     *                                 ou si l'adhésion trouvée ne référence pas de club (incohérence).
     * @throws IllegalStateException   Si l'utilisateur (ADMIN/RESERVATION) est trouvé associé à plus d'un club
     *                                 via Adhesion, ce qui contredit l'hypothèse du modèle d'un seul club géré.
     */
    @Transactional(readOnly = true) // Nécessaire pour lire Membre et Adhesions
    public Integer getCurrentUserManagedClubIdOrThrow() {
        // 1. Récupérer le membre complet (gère authentification + existence BDD)
        Membre currentUser = getCurrentMembreOrThrow();

        // 2. Vérifier le rôle requis (ADMIN ou RESERVATION)
        Role userRole = currentUser.getRole();
        if (userRole != Role.ADMIN && userRole != Role.RESERVATION) {
            throw new AccessDeniedException("Accès refusé : Rôle ADMIN ou RESERVATION requis pour cette opération de gestion.");
        }

        // 3. Trouver l'adhésion (et donc le club) liée à ce gestionnaire
        // Suppose que la relation 'adhesions' est correctement mappée et accessible
        Set<Adhesion> adhesions = currentUser.getAdhesions();

        // 4. Valider l'hypothèse : exactement une adhésion pour un manager
        if (adhesions == null || adhesions.isEmpty()) {
            log.error("Incohérence : Utilisateur {} (ID: {}) avec rôle {} n'a aucune adhésion.", currentUser.getEmail(), currentUser.getId(), userRole);
            throw new AccessDeniedException("Accès refusé : L'utilisateur gestionnaire n'est associé à aucun club.");
        }
        if (adhesions.size() > 1) {
            // Si le modèle permettait plusieurs adhésions, il faudrait une logique plus complexe
            // pour déterminer quel est le club "géré" (peut-être un flag sur Adhesion?).
            log.error("Incohérence : Utilisateur {} (ID: {}) avec rôle {} est associé à {} clubs via Adhesion. Le modèle suppose un seul club géré.",
                    currentUser.getEmail(), currentUser.getId(), userRole, adhesions.size());
            throw new IllegalStateException("Configuration invalide : Un utilisateur gestionnaire ne peut être associé qu'à un seul club.");
        }

        // 5. Récupérer et retourner l'ID du club de l'unique adhésion
        Adhesion managedAdhesion = adhesions.iterator().next(); // Prend l'unique adhésion
        if (managedAdhesion.getClub() == null || managedAdhesion.getClub().getId() == null) {
            log.error("Incohérence : L'adhésion (ID: {}) du gestionnaire {} ne référence pas un club valide.", managedAdhesion.getId(), currentUser.getEmail());
            throw new EntityNotFoundException("Incohérence des données : Le club associé à la gestion n'a pu être trouvé.");
        }
        return managedAdhesion.getClub().getId();
    }
}

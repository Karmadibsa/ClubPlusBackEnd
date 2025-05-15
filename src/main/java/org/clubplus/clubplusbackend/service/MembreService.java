package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier liée aux membres (utilisateurs).
 * Fournit des opérations pour l'inscription, la gestion de profil, les adhésions aux clubs,
 * la gestion des rôles (limitée), et la récupération d'informations sur les membres,
 * tout en intégrant des vérifications de sécurité via {@link SecurityService}.
 */
@Service
@RequiredArgsConstructor
@Transactional // Applique la transactionnalité par défaut à toutes les méthodes publiques
public class MembreService {

    // Dépendances injectées via Lombok @RequiredArgsConstructor
    private final MembreDao membreRepository;
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityService securityService;

    // --- Méthodes de Lecture ---

    /**
     * Récupère une entité {@link Membre} par son identifiant unique.
     * Lance une exception si aucun membre n'est trouvé avec cet ID.
     *
     * @param id L'identifiant unique du membre à récupérer.
     * @return L'entité {@link Membre} correspondante.
     * @throws EntityNotFoundException si aucun membre n'est trouvé pour l'ID fourni.
     */
    @Transactional(readOnly = true)
    public Membre getMembreByIdOrThrow(Integer id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un membre par son ID avec une vérification de sécurité contextuelle.
     * L'accès est autorisé si l'utilisateur actuellement authentifié est le membre cible lui-même,
     * ou s'ils partagent au moins un club *actif* en commun.
     * Seuls les membres actifs peuvent être récupérés via cette méthode.
     *
     * @param targetUserId L'ID du membre dont le profil est demandé.
     * @return L'entité {@link Membre} du membre cible si l'accès est autorisé.
     * @throws EntityNotFoundException si le membre cible n'est pas trouvé ou est inactif,
     *                                 ou si l'utilisateur courant n'est pas trouvé (cas peu probable si authentifié).
     * @throws AccessDeniedException   si l'utilisateur courant n'est ni le membre cible, ni membre d'un club actif commun.
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true)
    public Membre getMembreByIdWithSecurityCheck(Integer targetUserId) {
        // 1. Récupérer l'ID de l'utilisateur courant (lance une exception si non authentifié)
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // 2. Récupérer le membre cible (doit être actif, grâce au filtre @Where sur l'entité Membre)
        Membre targetUser = membreRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé ou inactif (ID: " + targetUserId + ")")); // -> 404

        // 3. Vérifier si l'utilisateur courant est le propriétaire du profil demandé
        if (currentUserId.equals(targetUserId)) {
            return targetUser; // Accès autorisé car c'est soi-même
        }

        // 4. Récupérer l'utilisateur courant (doit être actif)
        //    Bien que l'ID soit connu, recharger l'entité est plus sûr pour obtenir les adhésions.
        Membre currentUser = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")")); // Sécurité supplémentaire

        // 5. Trouver les IDs des clubs ACTIFS communs aux deux membres
        List<Integer> currentUserActiveClubIds = findActiveClubIdsForMember(currentUserId);
        List<Integer> targetUserActiveClubIds = findActiveClubIdsForMember(targetUserId);

        // 6. Vérifier s'il existe au moins un club actif en commun
        boolean hasCommonActiveClub = currentUserActiveClubIds.stream()
                .anyMatch(targetUserActiveClubIds::contains);

        if (hasCommonActiveClub) {
            return targetUser; // Accès autorisé car partage d'un club actif
        }

        // 7. Si aucune condition d'accès n'est remplie, refuser l'accès
        throw new AccessDeniedException("Accès refusé : Vous n'êtes pas autorisé à voir ce profil."); // -> 403
    }

    /**
     * Méthode privée utilitaire pour récupérer les IDs des clubs *actifs* auxquels un membre adhère.
     * Utilise une requête spécifique dans {@link AdhesionDao}.
     *
     * @param membreId L'ID du membre concerné.
     * @return Une liste des IDs des clubs actifs auxquels le membre appartient. Peut être vide.
     */
    private List<Integer> findActiveClubIdsForMember(Integer membreId) {
        // Appelle la méthode dédiée dans AdhesionDao qui filtre par club actif
        return adhesionRepository.findActiveClubIdsByMembreId(membreId);
    }

    /**
     * Récupère l'ensemble des clubs auxquels l'utilisateur actuellement authentifié adhère.
     * Utilise le chargement LAZY des adhésions dans le contexte transactionnel.
     *
     * @return Un {@link Set} des entités {@link Club} auxquelles l'utilisateur courant est membre.
     * Peut être vide si l'utilisateur n'est membre d'aucun club ou si les clubs sont inactifs (selon la logique de chargement).
     * @throws EntityNotFoundException si l'utilisateur courant n'est pas trouvé en base.
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional(readOnly = true)
    public Set<Club> findClubsForCurrentUser() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = getMembreByIdOrThrow(currentUserId); // Charge le membre

        // Accède à la collection 'adhesions' qui sera chargée (LAZY) grâce à la transaction active.
        // Assurez-vous que la relation Membre -> Adhesion est bien configurée (FetchType.LAZY est ok).
        return membre.getAdhesions().stream()
                .map(Adhesion::getClub) // Récupère le Club depuis chaque Adhesion
                .filter(Objects::nonNull) // Sécurité : ignore les adhésions sans club lié (ne devrait pas arriver)
                // Optionnel: Filtrer ici les clubs inactifs si nécessaire
                // .filter(Club::getActif)
                .collect(Collectors.toSet());
    }

    /**
     * Récupère les 5 derniers membres inscrits dans le club *géré* par l'utilisateur actuellement connecté.
     * L'utilisateur doit avoir le rôle ADMIN ou RESERVATION pour un club spécifique.
     * La méthode ne retourne que les membres dont le compte est actif.
     *
     * @return Une liste contenant jusqu'à 5 des derniers membres {@link Membre} actifs ayant rejoint le club géré.
     * La liste est triée par date d'adhésion décroissante implicitement par la requête DAO.
     * @throws AccessDeniedException Si l'utilisateur n'est pas connecté, n'a pas le rôle requis (ADMIN/RESERVATION)
     *                               ou n'est pas associé à un club gérable via {@link SecurityService#getCurrentUserManagedClubIdOrThrow()}.
     */
    @Transactional(readOnly = true)
    public List<Membre> getLatestMembersForManagedClub() {
        // 1. Vérifie les droits et récupère l'ID du club géré (lance AccessDeniedException si non autorisé)
        Integer managedClubId = securityService.getCurrentUserManagedClubIdOrThrow();

        // 2. Créer un objet Limit pour spécifier le nombre maximum de résultats (5)
        Limit topFive = Limit.of(5);

        // 3. Appeler la méthode DAO optimisée qui récupère les adhésions des membres actifs, triées et limitées
        List<Adhesion> latestAdhesions = adhesionRepository.findLatestActiveMembersAdhesionsWithLimit(managedClubId, topFive);

        // 4. Extraire les entités Membre distinctes à partir des adhésions récupérées
        return latestAdhesions.stream()
                .map(Adhesion::getMembre)       // Extrait le Membre de chaque Adhesion
                .filter(Objects::nonNull)       // Sécurité : ignore si le membre est null (ne devrait pas arriver)
                .distinct()                     // Évite les doublons si un membre avait plusieurs adhésions (peu probable ici)
                .collect(Collectors.toList());
    }

    // --- Inscription et Gestion de Compte ---

    /**
     * Enregistre un nouveau membre, le fait adhérer à un club, et envoie un email de vérification.
     * [Voir la documentation complète de la méthode précédente pour les détails]
     *
     * @param membreData Les infos du membre.
     * @param codeClub   Le code du club.
     * @return Le membre enregistré.
     * @throws IllegalArgumentException Si l'email existe déjà.
     * @throws EntityNotFoundException  Si le club n'existe pas.
     */
    @Transactional
    public Membre registerMembreAndJoinClub(Membre membreData, String codeClub) {
        // 1. Validation, normalisation, recherche du club (comme avant)
        String email = membreData.getEmail().toLowerCase().trim();
        if (membreRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }

        Club clubToJoin = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub));

        // 2. Préparation de l'entité Membre (avant sauvegarde initiale)
        membreData.setEmail(email);
        membreData.setDate_inscription(LocalDate.now());
        membreData.setPassword(passwordEncoder.encode(membreData.getPassword()));
        membreData.setRole(Role.MEMBRE);
        membreData.setId(null);
        membreData.setActif(true);
        membreData.setAdhesions(new HashSet<>());
        membreData.setAmis(new HashSet<>());
        membreData.setVerified(false); // Ajout : Initialiser isVerified à false
        String verificationToken = UUID.randomUUID().toString(); // Ajout : Générer un token unique
        membreData.setVerificationToken(verificationToken); // Ajout : Stocker le token
        // membreData.setTokenExpiryDate(LocalDateTime.now().plusDays(1)); // Optionnel: Ajouter une date d'expiration

        // 3. Sauvegarde initiale du membre (avec isVerified = false et le token)
        Membre nouveauMembre = membreRepository.save(membreData);

        // 4. Création et sauvegarde de l'adhésion initiale (comme avant)
        Adhesion nouvelleAdhesion = new Adhesion(nouveauMembre, clubToJoin);
        adhesionRepository.save(nouvelleAdhesion);

        // 5. Envoi de l'email de vérification (après sauvegarde réussie)
        try {
            emailService.sendVerificationEmail(nouveauMembre); // Méthode dédiée pour l'envoi
        } catch (Exception e) {
            // Gérer l'erreur d'envoi d'email (log, rollback si nécessaire, etc.)
            System.err.println("Erreur lors de l'envoi de l'email de vérification à " + nouveauMembre.getEmail() + ": " + e.getMessage());
            // Option (selon votre besoin):
            // - Rollback de la transaction si l'envoi d'email est critique
            //   TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            // - Lancer une exception custom pour informer l'utilisateur
            //   throw new EmailSendingException("Erreur lors de l'envoi de l'email. Veuillez réessayer plus tard.", e);
            // - Continuer l'inscription mais informer l'utilisateur qu'il devra contacter le support
        }

        return nouveauMembre;
    }

    /**
     * Met à jour les informations de profil de l'utilisateur actuellement authentifié.
     * Seuls les champs fournis dans le DTO et modifiables par l'utilisateur (nom, prénom, contact, etc.) sont mis à jour.
     * Le mot de passe et le rôle ne sont PAS modifiables via cette méthode.
     * Gère la mise à jour de l'email avec vérification d'unicité (hors propre email).
     *
     * @param updateMembreDto DTO contenant les nouvelles valeurs potentielles pour le profil.
     *                        Les champs null ou vides dans le DTO sont ignorés.
     * @return L'entité {@link Membre} mise à jour et persistée.
     * @throws EntityNotFoundException  si l'utilisateur courant n'est pas trouvé en base (peu probable si authentifié).
     * @throws IllegalArgumentException si le nouvel email fourni est déjà utilisé par un autre membre (HTTP 409 Conflict).
     * @throws SecurityException        (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional
    public Membre updateMyProfile(UpdateMembreDto updateMembreDto) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Récupère l'ID courant
        Membre existingMembre = getMembreByIdOrThrow(currentUserId); // Récupère l'entité Membre associée

        boolean updated = false; // Flag pour savoir si une sauvegarde est nécessaire

        // Mise à jour conditionnelle des champs modifiables
        if (updateMembreDto.getNom() != null && !updateMembreDto.getNom().isBlank()) {
            existingMembre.setNom(updateMembreDto.getNom().trim()); // Trim pour la propreté
            updated = true;
        }
        if (updateMembreDto.getPrenom() != null && !updateMembreDto.getPrenom().isBlank()) {
            existingMembre.setPrenom(updateMembreDto.getPrenom().trim());
            updated = true;
        }
        if (updateMembreDto.getDate_naissance() != null) {
            // Ajouter validation si nécessaire (ex: date non future)
            existingMembre.setDate_naissance(updateMembreDto.getDate_naissance());
            updated = true;
        }
        // Mise à jour du téléphone
        if (updateMembreDto.getTelephone() != null) { // Peut être null ou vide
            // Ajouter validation de format si nécessaire
            existingMembre.setTelephone(updateMembreDto.getTelephone());
            updated = true;
        }

        // Mise à jour de l'email (avec validation d'unicité)
        String newEmail = updateMembreDto.getEmail();
        if (newEmail != null && !newEmail.isBlank()) {
            String normalizedNewEmail = newEmail.toLowerCase().trim();
            // Vérifier si l'email a changé ET s'il existe déjà pour un AUTRE utilisateur
            if (!normalizedNewEmail.equalsIgnoreCase(existingMembre.getEmail()) &&
                    membreRepository.existsByEmailAndIdNot(normalizedNewEmail, currentUserId)) {
                throw new IllegalArgumentException("Cet email est déjà utilisé par un autre membre."); // Conflit -> HTTP 409
            }
            // Si l'email est différent ou si la casse/espaces ont changé, mettre à jour
            if (!normalizedNewEmail.equals(existingMembre.getEmail())) {
                existingMembre.setEmail(normalizedNewEmail);
                updated = true;
            }
        }

        // Sauvegarde uniquement si des modifications ont été détectées
        if (updated) {
            return membreRepository.save(existingMembre);
        }

        // Si aucune mise à jour n'a été effectuée, retourne l'entité existante non modifiée
        return existingMembre;
    }

    /**
     * Désactive et anonymise le compte de l'utilisateur actuellement authentifié.
     * Il s'agit d'une suppression logique : le compte devient inactif et les données personnelles
     * identifiables sont effacées ou remplacées par des valeurs neutres via {@link Membre#anonymizeData()}.
     * Une règle métier spécifique empêche un utilisateur avec le rôle ADMIN de supprimer son compte
     * s'il est encore associé à (gère) au moins un club.
     *
     * @throws IllegalStateException   si l'utilisateur courant est un ADMIN qui gère encore un club (HTTP 409 Conflict/403 Forbidden).
     * @throws EntityNotFoundException si l'utilisateur courant n'est pas trouvé en base (peu probable si authentifié).
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional // Assure l'atomicité de la désactivation et de l'anonymisation
    public void deleteMyAccount() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membreToDelete = getMembreByIdOrThrow(currentUserId); // Récupère l'entité du membre actif

        // --- Vérification spécifique au rôle ADMIN ---
        if (membreToDelete.getRole() == Role.ADMIN) {
            // Vérifier si l'admin est associé à au moins un club via ses adhésions
            // La présence d'une adhésion signifie qu'il gère ce club (car seul l'ADMIN a une adhésion pour son club).
            boolean isAdminOfAnyClub = !membreToDelete.getAdhesions().isEmpty(); // ou adhesionRepository.existsByMembreId(currentUserId)
            if (isAdminOfAnyClub) {
                // Empêche la suppression si l'admin gère encore un club
                throw new IllegalStateException("Impossible de supprimer un compte ADMIN qui gère encore un club. Veuillez d'abord transférer la propriété du club."); // -> HTTP 409 ou 403
            }
            // Si c'est un Admin sans adhésion (cas limite, peut-être après suppression manuelle du club?), la désactivation est permise.
        }

        // --- Processus de Désactivation et Anonymisation ---
        // 1. Anonymiser les données personnelles (méthode sur l'entité)
        membreToDelete.anonymizeData(); // Modifie nom, prénom, email, adresse, tel, dateNaissance, etc. Met aussi dateAnonymisation.

        // 2. Marquer le compte comme inactif
        membreToDelete.setActif(false);

        // 3. Persister les modifications (UPDATE SQL)
        membreRepository.save(membreToDelete);

        // Le compte est maintenant logiquement supprimé et anonymisé.
        // Aucune suppression physique (DELETE SQL) n'est effectuée.
    }

    // --- Gestion des Adhésions aux Clubs ---

    /**
     * Permet à l'utilisateur actuellement authentifié de rejoindre un club spécifié par son code.
     * Cette action est réservée aux utilisateurs ayant le rôle {@link Role#MEMBRE}.
     * Les utilisateurs ADMIN ou RESERVATION sont liés à un club unique et ne peuvent pas en rejoindre d'autres.
     * Vérifie également que l'utilisateur n'est pas déjà membre de ce club.
     *
     * @param codeClub Le code unique du club à rejoindre.
     * @return L'entité {@link Adhesion} nouvellement créée et persistée.
     * @throws IllegalStateException   si l'utilisateur a un rôle ADMIN ou RESERVATION, ou s'il est déjà membre du club (HTTP 409 Conflict).
     * @throws EntityNotFoundException si aucun club n'est trouvé avec le {@code codeClub} fourni, ou si l'utilisateur courant n'est pas trouvé (HTTP 404 Not Found).
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional
    public Adhesion joinClub(String codeClub) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = getMembreByIdOrThrow(currentUserId); // Récupère le membre courant

        // Vérification du rôle : seuls les MEMBRES peuvent rejoindre d'autres clubs
        if (membre.getRole() == Role.ADMIN || membre.getRole() == Role.RESERVATION) {
            throw new IllegalStateException("Les utilisateurs avec le rôle ADMIN ou RESERVATION ne peuvent pas rejoindre d'autres clubs."); // -> HTTP 409
        }

        // Recherche du club par son code
        Club club = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub)); // -> HTTP 404

        // Vérification : l'utilisateur est-il déjà membre de ce club ?
        if (adhesionRepository.existsByMembreIdAndClubId(currentUserId, club.getId())) {
            throw new IllegalStateException("Vous êtes déjà membre de ce club."); // -> HTTP 409
        }

        // Création et sauvegarde de la nouvelle adhésion
        Adhesion adhesion = new Adhesion(membre, club);
        return adhesionRepository.save(adhesion);
    }

    /**
     * Permet à l'utilisateur actuellement authentifié de quitter un club dont il est membre.
     * Cette action est principalement destinée aux utilisateurs avec le rôle {@link Role#MEMBRE}.
     * Les utilisateurs ADMIN ou RESERVATION ne peuvent pas quitter leur club unique via cette méthode,
     * car cela nécessiterait des processus spécifiques (transfert de propriété pour ADMIN).
     *
     * @param clubId L'ID du club que l'utilisateur souhaite quitter.
     * @throws EntityNotFoundException si l'utilisateur n'est pas membre du club spécifié, ou si l'utilisateur/club n'existe pas (HTTP 404 Not Found).
     * @throws IllegalStateException   si l'utilisateur a un rôle ADMIN ou RESERVATION (HTTP 409 Conflict).
     * @throws SecurityException       (ou similaire) si l'utilisateur courant ne peut être identifié.
     */
    @Transactional
    public void leaveClub(Integer clubId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // Trouve l'adhésion spécifique à supprimer
        Adhesion adhesion = adhesionRepository.findByMembreIdAndClubId(currentUserId, clubId)
                .orElseThrow(() -> new EntityNotFoundException("Vous n'êtes pas membre de ce club (ID: " + clubId + ").")); // -> HTTP 404

        // Recharger le membre pour une vérification de rôle fiable au moment de l'action
        Membre membre = getMembreByIdOrThrow(currentUserId);

        // Vérification du rôle : ADMIN et RESERVATION ne peuvent pas quitter leur club via cette méthode
        if (membre.getRole() == Role.ADMIN || membre.getRole() == Role.RESERVATION) {
            // Message d'erreur plus précis
            String reason = (membre.getRole() == Role.ADMIN)
                    ? "Un ADMIN doit d'abord transférer la propriété du club."
                    : "Un utilisateur RESERVATION est lié à ce club et ne peut pas le quitter.";
            throw new IllegalStateException("Impossible de quitter ce club avec votre rôle actuel (" + membre.getRole() + "). " + reason); // -> HTTP 409
        }

        // Si c'est un MEMBRE, la suppression de l'adhésion est autorisée
        adhesionRepository.delete(adhesion);
    }

    // --- Gestion des Rôles (Par Admin du Club) ---

    /**
     * Modifie le rôle d'un membre au sein d'un club spécifique.
     * Seul un administrateur (ADMIN) du club concerné peut effectuer cette action.
     * Les changements autorisés sont : MEMBRE -> RESERVATION et RESERVATION -> MEMBRE.
     * Contraintes :
     * - L'appelant doit être ADMIN du {@code clubId}.
     * - La cible doit être membre du {@code clubId}.
     * - L'admin ne peut pas changer son propre rôle.
     * - Le rôle d'un autre ADMIN ne peut pas être changé.
     * - Le nouveau rôle doit être MEMBRE ou RESERVATION.
     * - Pour promouvoir en RESERVATION, le membre ne doit appartenir qu'à ce seul club.
     *
     * @param targetMemberId L'ID du membre dont le rôle doit être modifié.
     * @param clubId         L'ID du club dans lequel le changement de rôle s'applique.
     * @param newRole        Le nouveau rôle souhaité (doit être {@link Role#MEMBRE} ou {@link Role#RESERVATION}).
     * @return L'entité {@link Membre} mise à jour avec le nouveau rôle.
     * @throws SecurityException        (ou similaire) si l'appelant n'est pas ADMIN du club {@code clubId}.
     * @throws IllegalArgumentException si le {@code newRole} est invalide, ou si l'admin tente de changer son propre rôle (HTTP 400 Bad Request).
     * @throws EntityNotFoundException  si le membre cible n'existe pas, ou s'il n'appartient pas au club spécifié (HTTP 404 Not Found).
     * @throws IllegalStateException    si on tente de modifier le rôle d'un ADMIN, si la transition de rôle est invalide (ex: ADMIN -> MEMBRE),
     *                                  si le membre a déjà le rôle cible, ou si un membre multi-clubs est promu RESERVATION (HTTP 409 Conflict).
     */
    @Transactional
    public Membre changeMemberRoleInClub(Integer targetMemberId, Integer clubId, Role newRole) {
        // L'ID de l'admin appelant est récupéré dans checkIsActualAdminOfClubOrThrow
        // Integer currentAdminId = securityService.getCurrentUserIdOrThrow(); // Pas forcément nécessaire ici

        // 1. Sécurité : Vérifie que l'utilisateur courant est bien ADMIN de ce club spécifique.
        securityService.checkIsActualAdminOfClubOrThrow(clubId); // Lance une exception si non autorisé -> HTTP 403

        // 2. Validation du rôle cible demandé
        if (newRole != Role.MEMBRE && newRole != Role.RESERVATION) {
            throw new IllegalArgumentException("Le nouveau rôle doit être MEMBRE ou RESERVATION."); // -> HTTP 400
        }

        // 3. Récupérer le membre cible
        Membre targetMember = getMembreByIdOrThrow(targetMemberId); // Lance 404 si non trouvé
        Role currentRole = targetMember.getRole();

        // 4. Vérifier que la cible est bien membre de CE club
        if (!adhesionRepository.existsByMembreIdAndClubId(targetMemberId, clubId)) {
            throw new EntityNotFoundException("Le membre cible (ID: " + targetMemberId + ") n'appartient pas à ce club (ID: " + clubId + ")."); // -> HTTP 404
        }

        // 5. Vérifications des cas interdits
        // Récupérer l'ID de l'admin appelant pour la comparaison
        Integer currentAdminId = securityService.getCurrentUserIdOrThrow();
        if (currentAdminId.equals(targetMemberId)) {
            throw new IllegalArgumentException("L'administrateur ne peut pas changer son propre rôle via cette méthode."); // -> HTTP 400
        }
        if (currentRole == Role.ADMIN) {
            // On ne peut pas rétrograder un autre admin via cette méthode (nécessiterait un processus différent)
            throw new IllegalStateException("Impossible de changer le rôle d'un autre administrateur (ADMIN) via cette fonction."); // -> HTTP 409
        }
        if (currentRole == newRole) {
            // Aucune action requise, on pourrait retourner le membre ou lancer une exception (409). Retourner est plus idempotent.
            // throw new IllegalStateException("Le membre cible a déjà le rôle " + newRole + "."); // Alternative -> HTTP 409
            return targetMember;
        }

        // 6. Logique de changement de rôle (MEMBRE &lt;-> RESERVATION)
        if (newRole == Role.RESERVATION) { // Promotion MEMBRE -> RESERVATION
            if (currentRole != Role.MEMBRE) {
                // Seuls les MEMBRES peuvent être promus RESERVATION
                throw new IllegalStateException("Seul un membre avec le rôle MEMBRE peut être promu au rôle RESERVATION."); // -> HTTP 409
            }
            // Contrainte supplémentaire : un RESERVATION ne peut appartenir qu'à un seul club.
            if (adhesionRepository.countByMembreId(targetMemberId) > 1) {
                throw new IllegalStateException("Impossible de promouvoir au rôle RESERVATION un membre qui appartient à plusieurs clubs."); // -> HTTP 409
            }
            targetMember.setRole(Role.RESERVATION);

        } else { // newRole == Role.MEMBRE : Rétrogradation RESERVATION -> MEMBRE
            if (currentRole != Role.RESERVATION) {
                // Seuls les RESERVATION peuvent être rétrogradés MEMBRE par cette action
                throw new IllegalStateException("Seul un membre avec le rôle RESERVATION peut être rétrogradé au rôle MEMBRE."); // -> HTTP 409
            }
            targetMember.setRole(Role.MEMBRE);
        }

        // 7. Sauvegarder le changement de rôle
        return membreRepository.save(targetMember);
    }

    @Transactional // Important pour s'assurer que les modifications sont bien persistées
    public boolean verifyUserToken(String token) {
        Optional<Membre> membreOptional = membreRepository.findByVerificationToken(token); // Vous devez créer cette méthode dans MembreRepository

        if (membreOptional.isEmpty()) {
            return false; // Token non trouvé
        }

        Membre membre = membreOptional.get();

        if (membre.isVerified()) {
            // Compte déjà vérifié, vous pouvez considérer cela comme un succès ou un cas spécial
            // Si vous voulez éviter la réutilisation du lien, ne retournez pas true ici mais plutôt un code d'erreur.
            // Pour l'instant, considérons que si déjà vérifié, c'est "ok"
            return true;
        }


        membre.setVerified(true);
        membre.setVerificationToken(null); // Très important : Invalider le token après utilisation
        membreRepository.save(membre);

        return true;
    }
}

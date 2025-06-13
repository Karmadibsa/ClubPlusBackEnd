package org.clubplus.clubplusbackend.service;

import jakarta.mail.MessagingException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier liée aux membres (utilisateurs).
 * <p>
 * Ce service orchestre l'inscription, la gestion de profil, les adhésions aux clubs,
 * et la gestion des rôles, tout en appliquant les règles de sécurité.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MembreService {

    private static final Logger log = LoggerFactory.getLogger(MembreService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.reset-token.expiration-ms}")
    private long jwtResetTokenExpirationMs;

    private static final String PASSWORD_PATTERN_REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\\-\\[{}\\]:;',?/*~$^+=<>]).{8,100}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_PATTERN_REGEX);

    private final MembreDao membreRepository;
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityService securityService;

    /**
     * Récupère un membre par son ID.
     *
     * @param id L'ID du membre.
     * @return L'entité {@link Membre}.
     * @throws EntityNotFoundException si aucun membre n'est trouvé.
     */
    @Transactional(readOnly = true)
    public Membre getMembreByIdOrThrow(Integer id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un membre par son ID, avec des vérifications de sécurité.
     * <p>
     * <b>Sécurité :</b> L'accès est autorisé si l'utilisateur courant est un ADMIN,
     * s'il consulte son propre profil, ou s'il partage un club actif avec la cible.
     *
     * @param targetUserId L'ID du profil membre demandé.
     * @return L'entité {@link Membre} du membre cible.
     * @throws EntityNotFoundException si le membre cible n'est pas trouvé.
     * @throws AccessDeniedException   si l'accès est refusé.
     */
    @Transactional(readOnly = true)
    public Membre getMembreByIdWithSecurityCheck(Integer targetUserId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre currentUser = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")"));

        if (currentUser.getRole() == Role.ADMIN) {
            return membreRepository.findById(targetUserId)
                    .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé ou inactif (ID: " + targetUserId + ")"));
        }

        Membre targetUser = membreRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé ou inactif (ID: " + targetUserId + ")"));

        if (currentUserId.equals(targetUserId)) {
            return targetUser;
        }

        List<Integer> currentUserActiveClubIds = findActiveClubIdsForMember(currentUserId);
        List<Integer> targetUserActiveClubIds = findActiveClubIdsForMember(targetUserId);
        boolean hasCommonActiveClub = currentUserActiveClubIds.stream()
                .anyMatch(targetUserActiveClubIds::contains);

        if (hasCommonActiveClub) {
            return targetUser;
        }

        throw new AccessDeniedException("Accès refusé : Vous n'êtes pas autorisé à voir ce profil.");
    }

    /**
     * Récupère les clubs de l'utilisateur courant.
     *
     * @return Un ensemble de {@link Club}.
     */
    @Transactional(readOnly = true)
    public Set<Club> findClubsForCurrentUser() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = getMembreByIdOrThrow(currentUserId);

        return membre.getAdhesions().stream()
                .map(Adhesion::getClub)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Récupère les 5 derniers membres inscrits dans le club géré par l'utilisateur.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit avoir un rôle de gestionnaire (ADMIN/RESERVATION).
     *
     * @return Une liste de 5 membres maximum.
     */
    @Transactional(readOnly = true)
    public List<Membre> getLatestMembersForManagedClub() {
        Integer managedClubId = securityService.getCurrentUserManagedClubIdOrThrow();
        Limit topFive = Limit.of(5);
        List<Adhesion> latestAdhesions = adhesionRepository.findLatestActiveMembersAdhesionsWithLimit(managedClubId, topFive);

        return latestAdhesions.stream()
                .map(Adhesion::getMembre)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Inscrit un nouveau membre, l'ajoute à un club et envoie un email de vérification.
     *
     * @param membreData Les informations du membre.
     * @param codeClub   Le code du club à rejoindre.
     * @return Le membre créé.
     * @throws IllegalArgumentException si l'email existe déjà.
     * @throws EntityNotFoundException  si le club n'est pas trouvé.
     */
    public Membre registerMembreAndJoinClub(Membre membreData, String codeClub) {
        String email = membreData.getEmail().toLowerCase().trim();
        if (membreRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }

        Club clubToJoin = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub));

        membreData.setEmail(email);
        membreData.setDate_inscription(LocalDate.now());
        membreData.setPassword(passwordEncoder.encode(membreData.getPassword()));
        membreData.setRole(Role.MEMBRE);
        membreData.setId(null);
        membreData.setActif(true);
        membreData.setAdhesions(new HashSet<>());
        membreData.setAmis(new HashSet<>());
        membreData.setVerified(false);
        membreData.setVerificationToken(UUID.randomUUID().toString());

        Membre nouveauMembre = membreRepository.save(membreData);

        Adhesion nouvelleAdhesion = new Adhesion(nouveauMembre, clubToJoin);
        adhesionRepository.save(nouvelleAdhesion);

        try {
            emailService.sendVerificationEmail(nouveauMembre);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de vérification à {} : {}", nouveauMembre.getEmail(), e.getMessage());
            // Selon la politique, on pourrait vouloir annuler la transaction ici.
        }

        return nouveauMembre;
    }

    /**
     * Met à jour le profil de l'utilisateur courant.
     *
     * @param updateMembreDto DTO contenant les nouvelles informations.
     * @return Le membre mis à jour.
     * @throws IllegalArgumentException si l'email est déjà utilisé par un autre membre.
     */
    public Membre updateMyProfile(UpdateMembreDto updateMembreDto) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre existingMembre = getMembreByIdOrThrow(currentUserId);

        boolean updated = false;

        // Nom
        if (updateMembreDto.getNom() != null && !updateMembreDto.getNom().isBlank()) {
            existingMembre.setNom(updateMembreDto.getNom().trim());
            updated = true;
        }
        // Prenom
        if (updateMembreDto.getPrenom() != null && !updateMembreDto.getPrenom().isBlank()) {
            existingMembre.setPrenom(updateMembreDto.getPrenom().trim());
            updated = true;
        }
        // Date Naissance
        if (updateMembreDto.getDate_naissance() != null) {
            existingMembre.setDate_naissance(updateMembreDto.getDate_naissance());
            updated = true;
        }
        // Telephone
        if (updateMembreDto.getTelephone() != null) {
            existingMembre.setTelephone(updateMembreDto.getTelephone().trim());
            updated = true;
        }

        // Email
        String newEmailFromDto = updateMembreDto.getEmail();
        if (newEmailFromDto != null && !newEmailFromDto.isBlank()) {
            String normalizedNewEmail = newEmailFromDto.toLowerCase().trim();
            if (!normalizedNewEmail.equalsIgnoreCase(existingMembre.getEmail())) {
                if (membreRepository.existsByEmailAndIdNot(normalizedNewEmail, currentUserId)) {
                    throw new IllegalArgumentException("Cet email est déjà utilisé par un autre membre.");
                }
                existingMembre.setEmail(normalizedNewEmail);
                updated = true;
            }
        }

        if (updated) {
            return membreRepository.save(existingMembre);
        }
        return existingMembre;
    }

    /**
     * Désactive et anonymise le compte de l'utilisateur courant.
     * <p>
     * <b>Règle métier :</b> Un ADMIN ne peut pas supprimer son compte s'il gère encore un club.
     *
     * @throws IllegalStateException si l'utilisateur est un ADMIN qui gère un club.
     */
    public void deleteMyAccount() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membreToDelete = getMembreByIdOrThrow(currentUserId);

        if (membreToDelete.getRole() == Role.ADMIN) {
            if (!membreToDelete.getAdhesions().isEmpty()) {
                throw new IllegalStateException("Impossible de supprimer un compte ADMIN qui gère encore un club.");
            }
        }

        membreToDelete.anonymizeData();
        membreToDelete.setActif(false);
        membreRepository.save(membreToDelete);
    }

    /**
     * Permet à un membre de rejoindre un club.
     * <p>
     * <b>Règle métier :</b> Seuls les utilisateurs avec le rôle MEMBRE peuvent rejoindre des clubs.
     *
     * @param codeClub Le code du club.
     * @return La nouvelle adhésion.
     * @throws IllegalStateException si l'utilisateur est un gestionnaire ou déjà membre.
     */
    public Adhesion joinClub(String codeClub) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = getMembreByIdOrThrow(currentUserId);

        if (membre.getRole() == Role.ADMIN || membre.getRole() == Role.RESERVATION) {
            throw new IllegalStateException("Les utilisateurs avec le rôle ADMIN ou RESERVATION ne peuvent pas rejoindre d'autres clubs.");
        }

        Club club = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub));

        if (adhesionRepository.existsByMembreIdAndClubId(currentUserId, club.getId())) {
            throw new IllegalStateException("Vous êtes déjà membre de ce club.");
        }

        Adhesion adhesion = new Adhesion(membre, club);
        return adhesionRepository.save(adhesion);
    }

    /**
     * Permet à un membre de quitter un club.
     * <p>
     * <b>Règle métier :</b> Les gestionnaires (ADMIN/RESERVATION) ne peuvent pas quitter leur club via cette méthode.
     *
     * @param clubId L'ID du club à quitter.
     * @throws EntityNotFoundException si l'adhésion n'est pas trouvée.
     * @throws IllegalStateException   si l'utilisateur est un gestionnaire.
     */
    public void leaveClub(Integer clubId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Adhesion adhesion = adhesionRepository.findByMembreIdAndClubId(currentUserId, clubId)
                .orElseThrow(() -> new EntityNotFoundException("Vous n'êtes pas membre de ce club (ID: " + clubId + ")."));

        Membre membre = getMembreByIdOrThrow(currentUserId);

        if (membre.getRole() == Role.ADMIN || membre.getRole() == Role.RESERVATION) {
            throw new IllegalStateException("Impossible de quitter ce club avec votre rôle actuel (" + membre.getRole() + ").");
        }

        adhesionRepository.delete(adhesion);
    }

    /**
     * Modifie le rôle d'un membre dans un club.
     * <p>
     * <b>Sécurité :</b> L'appelant doit être ADMIN du club.
     *
     * @param targetMemberId L'ID du membre cible.
     * @param clubId         L'ID du club.
     * @param newRole        Le nouveau rôle (MEMBRE ou RESERVATION).
     * @return Le membre mis à jour.
     */
    public Membre changeMemberRoleInClub(Integer targetMemberId, Integer clubId, Role newRole) {
        securityService.checkIsActualAdminOfClubOrThrow(clubId);

        if (newRole != Role.MEMBRE && newRole != Role.RESERVATION) {
            throw new IllegalArgumentException("Le nouveau rôle doit être MEMBRE ou RESERVATION.");
        }

        Membre targetMember = getMembreByIdOrThrow(targetMemberId);
        Role currentRole = targetMember.getRole();

        if (!adhesionRepository.existsByMembreIdAndClubId(targetMemberId, clubId)) {
            throw new EntityNotFoundException("Le membre cible (ID: " + targetMemberId + ") n'appartient pas à ce club (ID: " + clubId + ").");
        }

        Integer currentAdminId = securityService.getCurrentUserIdOrThrow();
        if (currentAdminId.equals(targetMemberId)) {
            throw new IllegalArgumentException("L'administrateur ne peut pas changer son propre rôle.");
        }
        if (currentRole == Role.ADMIN) {
            throw new IllegalStateException("Impossible de changer le rôle d'un autre administrateur.");
        }
        if (currentRole == newRole) {
            return targetMember;
        }

        if (newRole == Role.RESERVATION) {
            if (currentRole != Role.MEMBRE) {
                throw new IllegalStateException("Seul un MEMBRE peut être promu RESERVATION.");
            }
            if (adhesionRepository.countByMembreId(targetMemberId) > 1) {
                throw new IllegalStateException("Impossible de promouvoir un membre appartenant à plusieurs clubs.");
            }
            targetMember.setRole(Role.RESERVATION);
        } else {
            if (currentRole != Role.RESERVATION) {
                throw new IllegalStateException("Seul un RESERVATION peut être rétrogradé MEMBRE.");
            }
            targetMember.setRole(Role.MEMBRE);
        }

        return membreRepository.save(targetMember);
    }

    /**
     * Valide un token de vérification d'email.
     *
     * @param token Le token à vérifier.
     * @return {@code true} si la vérification réussit.
     */
    public boolean verifyUserValidationToken(String token) {
        Optional<Membre> membreOptional = membreRepository.findByVerificationToken(token);
        if (membreOptional.isEmpty()) {
            return false;
        }

        Membre membre = membreOptional.get();
        if (membre.isVerified()) {
            return true;
        }

        membre.setVerified(true);
        membre.setVerificationToken(null);
        membreRepository.save(membre);
        return true;
    }

    /**
     * Envoie un email de réinitialisation de mot de passe si l'email existe.
     *
     * @param email L'adresse email.
     * @throws MessagingException en cas d'erreur d'envoi.
     */
    public void requestPasswordReset(String email) throws MessagingException {
        Optional<Membre> membreOptional = membreRepository.findByEmail(email.toLowerCase().trim());

        if (membreOptional.isPresent()) {
            Membre membre = membreOptional.get();
            String resetToken = UUID.randomUUID().toString();
            Instant expiryDate = Instant.now().plus(30, ChronoUnit.MINUTES);
            membre.setResetPasswordToken(resetToken);
            membre.setResetPasswordTokenExpiryDate(expiryDate);
            membreRepository.save(membre);
            emailService.sendPasswordResetEmail(membre, resetToken);
            log.info("Email de réinitialisation envoyé à : {}", membre.getEmail());
        } else {
            log.info("Tentative de réinitialisation pour un email non trouvé : {}", email);
        }
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur à partir d'un token valide.
     *
     * @param token       Le token de réinitialisation.
     * @param newPassword Le nouveau mot de passe.
     * @return {@code true} en cas de succès.
     * @throws IllegalArgumentException si le token est invalide ou si le mot de passe ne respecte pas les règles.
     * @throws IllegalStateException    si le token a expiré.
     */
    public boolean resetPassword(String token, String newPassword) {
        Membre membre = membreRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Le lien de réinitialisation est invalide ou a déjà été utilisé."));

        if (membre.getResetPasswordTokenExpiryDate() == null || Instant.now().isAfter(membre.getResetPasswordTokenExpiryDate())) {
            membre.setResetPasswordToken(null);
            membre.setResetPasswordTokenExpiryDate(null);
            membreRepository.save(membre);
            throw new IllegalStateException("Le lien de réinitialisation a expiré.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nouveau mot de passe ne peut pas être vide.");
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial.");
        }

        if (passwordEncoder.matches(newPassword, membre.getPassword())) {
            throw new IllegalArgumentException("Une erreur c'est produite.");
        }

        membre.setPassword(passwordEncoder.encode(newPassword));
        membre.setResetPasswordToken(null);
        membre.setResetPasswordTokenExpiryDate(null);
        membreRepository.save(membre);
        log.info("Mot de passe réinitialisé avec succès pour l'utilisateur (ID) : {}", membre.getId());
        return true;
    }

    /**
     * Vérifie la validité d'un token de réinitialisation de mot de passe.
     *
     * @deprecated La validation complète se fait dans {@code resetPassword}.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public boolean verifyUserResetToken(String token) {
        return membreRepository.findByResetPasswordToken(token)
                .map(membre -> !Instant.now().isAfter(membre.getResetPasswordTokenExpiryDate()))
                .orElse(false);
    }

    /**
     * Change le mot de passe pour un utilisateur authentifié.
     *
     * @param userEmail       L'email de l'utilisateur.
     * @param currentPassword Le mot de passe actuel fourni.
     * @param newPassword     Le nouveau mot de passe.
     * @throws IllegalArgumentException si le mot de passe actuel est incorrect ou si le nouveau ne respecte pas les règles.
     */
    public void changePasswordForAuthenticatedUser(String userEmail, String currentPassword, String newPassword) {
        Membre membre = membreRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

        if (!passwordEncoder.matches(currentPassword, membre.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nouveau mot de passe ne peut pas être vide.");
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit faire entre 8 et 100 caractères et contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial.");
        }

        if (passwordEncoder.matches(newPassword, membre.getPassword())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        membre.setPassword(passwordEncoder.encode(newPassword));
        membreRepository.save(membre);
        log.info("Mot de passe changé avec succès pour l'utilisateur : {}", userEmail);
    }

    List<Integer> findActiveClubIdsForMember(Integer membreId) {
        return adhesionRepository.findActiveClubIdsByMembreId(membreId);
    }
}

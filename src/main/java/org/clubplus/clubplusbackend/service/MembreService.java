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
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MembreService {

    private final MembreDao membreRepository;
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService; // Injection

    // --- Lecture ---

    /**
     * Trouve un membre par ID ou lance 404.
     */
    @Transactional(readOnly = true)
    public Membre getMembreByIdOrThrow(Integer id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un membre par ID avec vérification de sécurité.
     * Doit être le membre lui-même ou un admin global (règle à affiner dans SecurityService).
     * Lance 404 (Non trouvé) ou 403 (Accès refusé).
     */
    @Transactional(readOnly = true)
    public Membre getMembreByIdWithSecurityCheck(Integer targetUserId) {
        // 1. Récupérer l'ID de l'utilisateur courant
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        // 2. Récupérer le membre cible (doit être actif pour être visible via cet endpoint)
        //    Utilise la méthode standard findById qui respecte @Where sur Membre
        Membre targetUser = membreRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé ou inactif (ID: " + targetUserId + ")")); // -> 404

        // 3. Vérifier si l'utilisateur courant est le propriétaire
        if (currentUserId.equals(targetUserId)) {
            return targetUser; // C'est soi-même, accès autorisé
        }

        // --- NOUVELLE LOGIQUE : Vérifier l'appartenance à un club commun ---
        // 4. Récupérer l'utilisateur courant (actif)
        Membre currentUser = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur courant non trouvé (ID: " + currentUserId + ")")); // Devrait pas arriver si connecté, mais sécurité

        // 5. Trouver les IDs des clubs actifs communs aux deux membres
        //    (Nécessite une méthode helper ou une requête DAO spécifique)
        List<Integer> currentUserActiveClubIds = findActiveClubIdsForMember(currentUserId);
        List<Integer> targetUserActiveClubIds = findActiveClubIdsForMember(targetUserId);

        // Cherche une intersection (au moins un club actif en commun)
        boolean hasCommonActiveClub = currentUserActiveClubIds.stream()
                .anyMatch(targetUserActiveClubIds::contains);

        if (hasCommonActiveClub) {
            return targetUser; // Accès autorisé car dans le même club actif
        }
        // --- FIN NOUVELLE LOGIQUE ---

        // 6. Si aucune des conditions précédentes n'est remplie, refuser l'accès
        throw new AccessDeniedException("Accès refusé : Vous n'êtes pas autorisé à voir ce profil."); // -> 403
    }

    /**
     * Méthode privée pour récupérer les IDs des clubs actifs pour un membre donné.
     * Utilise AdhesionDao pour interroger la base.
     */
    private List<Integer> findActiveClubIdsForMember(Integer membreId) {
        // Appelle la méthode que nous avons définie dans AdhesionDao
        return adhesionRepository.findActiveClubIdsByMembreId(membreId);
    }

    // --- Inscription (Logique déjà revue, semble OK) ---
    public Membre registerMembreAndJoinClub(Membre membreData, String codeClub) {
        String email = membreData.getEmail().toLowerCase().trim();
        if (membreRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email."); // -> 409
        }
        Club clubToJoin = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub)); // -> 404

        membreData.setEmail(email);
        membreData.setDate_inscription(LocalDate.now());
        membreData.setPassword(passwordEncoder.encode(membreData.getPassword()));
        membreData.setRole(Role.MEMBRE);
        membreData.setId(null);
        membreData.setAdhesions(new HashSet<>()); // Init collections
        membreData.setAmis(new HashSet<>());
        // ... autres collections ...

        Membre nouveauMembre = membreRepository.save(membreData);
        Adhesion nouvelleAdhesion = new Adhesion(nouveauMembre, clubToJoin);
        adhesionRepository.save(nouvelleAdhesion);
        return nouveauMembre;
    }

    // --- Mise à Jour Profil (Utilisateur Courant) ---
    public Membre updateMyProfile(UpdateMembreDto updateMembreDto) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow(); // Récupère ID courant
        Membre existingMembre = getMembreByIdOrThrow(currentUserId); // Pas besoin de checkIsOwner ici

        boolean updated = false;
        // ... (logique de copie des champs nom, prenom, date_naissance, adresse, telephone...) ...
        if (updateMembreDto.getNom() != null && !updateMembreDto.getNom().isBlank()) {
            existingMembre.setNom(updateMembreDto.getNom());
            updated = true;
        }
        if (updateMembreDto.getPrenom() != null && !updateMembreDto.getPrenom().isBlank()) {
            existingMembre.setPrenom(updateMembreDto.getPrenom());
            updated = true;
        }
        if (updateMembreDto.getDate_naissance() != null) {
            existingMembre.setDate_naissance(updateMembreDto.getDate_naissance());
            updated = true;
        }
        if (updateMembreDto.getNumero_voie() != null) {
            existingMembre.setNumero_voie(updateMembreDto.getNumero_voie());
            updated = true;
        }
        if (updateMembreDto.getRue() != null) {
            existingMembre.setRue(updateMembreDto.getRue());
            updated = true;
        }
        if (updateMembreDto.getCodepostal() != null) {
            existingMembre.setCodepostal(updateMembreDto.getCodepostal());
            updated = true;
        }
        if (updateMembreDto.getVille() != null) {
            existingMembre.setVille(updateMembreDto.getVille());
            updated = true;
        }
        if (updateMembreDto.getTelephone() != null) {
            existingMembre.setTelephone(updateMembreDto.getTelephone());
            updated = true;
        }


        String newEmail = updateMembreDto.getEmail();
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(existingMembre.getEmail())) {
            String normalizedNewEmail = newEmail.toLowerCase().trim();
            if (membreRepository.existsByEmailAndIdNot(normalizedNewEmail, currentUserId)) { // Utilise DAO
                throw new IllegalArgumentException("Email déjà utilisé par un autre membre."); // -> 409
            }
            existingMembre.setEmail(normalizedNewEmail);
            updated = true;
        }

        // NE PAS METTRE A JOUR password ou role ici

        if (updated) {
            return membreRepository.save(existingMembre);
        }
        return existingMembre;
    }

    /**
     * Désactive et anonymise le compte de l'utilisateur actuellement connecté.
     * Remplace la suppression physique par une désactivation logique et une anonymisation.
     * Respecte la logique métier interdisant la suppression d'un admin ayant des adhésions.
     *
     * @throws IllegalStateException   si l'utilisateur est un ADMIN avec des adhésions.
     * @throws EntityNotFoundException si l'utilisateur actif n'est pas trouvé.
     */
    @Transactional // Indispensable pour garantir que tout réussit ou échoue ensemble
    public void deleteMyAccount() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membreToDelete = getMembreByIdOrThrow(currentUserId); // Récupère le membre actif

        // --- Vérification Admin (Logique métier conservée comme demandé) ---
        if (membreToDelete.getRole() == Role.ADMIN) {
            // La logique métier spécifiée : un Admin ne peut se supprimer s'il a des adhésions.
            boolean isAdminOfAnyClub = !membreToDelete.getAdhesions().isEmpty();
            if (isAdminOfAnyClub) {
                // Utilise une exception appropriée, peut-être une 409 Conflict ou 403 Forbidden au niveau du contrôleur
                throw new IllegalStateException("Impossible de supprimer un compte ADMIN qui gère encore un club.");
            }
            // Si c'est un Admin sans adhésion (cas possiblement rare), la suppression est autorisée.
        }

        // --- Logique de Désactivation et Anonymisation ---
        // 1. Appeler la méthode d'anonymisation DANS l'entité Membre
        membreToDelete.anonymizeData(); // L'entité se charge de modifier ses propres données

        // 2. Marquer le compte comme inactif
        membreToDelete.setActif(false);
        // Note: la date d'anonymisation est déjà mise à jour DANS anonymizeData()

        // 3. Sauvegarder l'état modifié (fait un UPDATE en BDD)
        membreRepository.save(membreToDelete);

        // FIN - Pas d'appel à membreRepository.delete() requis.
        // L'approche find-modify-save est utilisée ici.
    }

    // --- Gestion Adhésions Club (Utilisateur Courant) ---
    public Adhesion joinClub(String codeClub) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = getMembreByIdOrThrow(currentUserId);

        if (membre.getRole() == Role.ADMIN || membre.getRole() == Role.RESERVATION) {
            throw new IllegalStateException("Les membres ADMIN ou RESERVATION ne peuvent pas rejoindre d'autres clubs."); // -> 409
        }

        Club club = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub)); // -> 404

        if (adhesionRepository.existsByMembreIdAndClubId(currentUserId, club.getId())) {
            throw new IllegalStateException("Vous êtes déjà membre de ce club."); // -> 409
        }

        Adhesion adhesion = new Adhesion(membre, club);
        return adhesionRepository.save(adhesion);
    }

    public void leaveClub(Integer clubId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Adhesion adhesion = adhesionRepository.findByMembreIdAndClubId(currentUserId, clubId)
                .orElseThrow(() -> new EntityNotFoundException("Vous n'êtes pas membre de ce club (ID: " + clubId + ").")); // -> 404

        Membre membre = getMembreByIdOrThrow(currentUserId); // Recharger pour rôle sûr
        if (membre.getRole() == Role.ADMIN || membre.getRole() == Role.RESERVATION) {
            // Spécifier pourquoi: l'ADMIN ne peut pas via cette méthode, le RESERVATION non plus car lié à un seul club.
            throw new IllegalStateException("Impossible de quitter ce club avec votre rôle actuel (" + membre.getRole() + "). Un ADMIN doit transférer la propriété. Un RESERVATION est lié à ce club."); // -> 409
        }

        adhesionRepository.delete(adhesion); // OK pour rôle MEMBRE
    }

    @Transactional(readOnly = true)
    public Set<Club> findClubsForCurrentUser() {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = getMembreByIdOrThrow(currentUserId); // Charge le membre
        // Accès LAZY dans la transaction
        return membre.getAdhesions().stream()
                .map(Adhesion::getClub)
                .collect(Collectors.toSet());
    }

    // --- Gestion Rôles (par Admin de Club) ---
    public Membre changeMemberRoleInClub(Integer targetMemberId, Integer clubId, Role newRole) {
        Integer currentAdminId = securityService.getCurrentUserIdOrThrow();

        // 1. Sécurité: Appelant est ADMIN DE CE CLUB
        securityService.checkIsActualAdminOfClubOrThrow(clubId); // Vérifie l'appelant pour ce club -> 403

        // 2. Validation rôle cible
        if (newRole != Role.MEMBRE && newRole != Role.RESERVATION) {
            throw new IllegalArgumentException("Le nouveau rôle doit être MEMBRE ou RESERVATION."); // -> 400
        }

        Membre targetMember = getMembreByIdOrThrow(targetMemberId); // -> 404
        Role currentRole = targetMember.getRole();

        // 3. Vérifier appartenance cible au club
        if (!adhesionRepository.existsByMembreIdAndClubId(targetMemberId, clubId)) {
            throw new EntityNotFoundException("Le membre cible (ID: " + targetMemberId + ") n'appartient pas à ce club (ID: " + clubId + ")."); // -> 404
        }

        // 4. Vérifier cas interdits
        if (currentAdminId.equals(targetMemberId)) {
            throw new IllegalArgumentException("L'administrateur ne peut pas changer son propre rôle via cette méthode."); // -> 400
        }
        if (currentRole == Role.ADMIN) {
            throw new IllegalStateException("Impossible de changer le rôle d'un administrateur (ADMIN)."); // -> 409
        }
        if (currentRole == newRole) {
            return targetMember; // Pas de changement, on retourne le membre actuel (ou 409?)
            // throw new IllegalStateException("Le membre cible a déjà le rôle " + newRole + "."); // -> 409
        }

        // 5. Logique de changement
        if (newRole == Role.RESERVATION) { // MEMBRE -> RESERVATION
            if (currentRole != Role.MEMBRE)
                throw new IllegalStateException("Seul un MEMBRE peut être promu RESERVATION."); // -> 409
            // Vérifier club unique
            if (adhesionRepository.countByMembreId(targetMemberId) > 1) {
                throw new IllegalStateException("Impossible de promouvoir RESERVATION un membre appartenant à plusieurs clubs."); // -> 409
            }
            targetMember.setRole(Role.RESERVATION);
        } else { // RESERVATION -> MEMBRE
            if (currentRole != Role.RESERVATION)
                throw new IllegalStateException("Seul un RESERVATION peut être rétrogradé MEMBRE."); // -> 409
            targetMember.setRole(Role.MEMBRE);
        }

        return membreRepository.save(targetMember);
    }

    /**
     * Récupère les 5 derniers membres inscrits pour le club GÉRÉ par l'utilisateur actuellement connecté.
     * Vérifie d'abord que l'utilisateur connecté a les droits nécessaires (rôle ADMIN ou RESERVATION
     * associé à un club spécifique) via SecurityService. Si autorisé, récupère les données.
     *
     * @return La liste des 5 derniers membres inscrits dans le club géré par l'utilisateur.
     * @throws AccessDeniedException Si l'utilisateur n'est pas connecté, n'a pas le rôle requis
     *                               ou n'est pas associé à un club gérable.
     */
    @Transactional(readOnly = true)
    public List<Membre> getLatestMembersForManagedClub() { // PAS d'argument clubId ici !

        // 1. Utiliser SecurityService pour obtenir l'ID du club GÉRÉ par l'utilisateur courant.
        //    C'est CETTE méthode de SecurityService qui fait la vérification des droits (ADMIN/RESERVATION)
        //    et trouve le club associé. Elle DOIT lever AccessDeniedException si non autorisé.
        Integer managedClubId = securityService.getCurrentUserManagedClubIdOrThrow(); // MÉTHODE À CRÉER/CONFIRMER dans SecurityService

        // 2. Récupération des données via AdhesionRepository en utilisant l'ID obtenu
        List<Adhesion> latestAdhesions = adhesionRepository.findTop5ByClubIdOrderByDateAdhesionDesc(managedClubId);

        // 3. Transformation en List<Membre>
        return latestAdhesions.stream()
                .map(Adhesion::getMembre)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}

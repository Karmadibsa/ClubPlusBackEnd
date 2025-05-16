package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier pour les entités {@link Club}.
 * Fournit des opérations pour créer, lire, mettre à jour et désactiver (suppression logique) les clubs,
 * ainsi que pour gérer l'administrateur initial et récupérer des informations liées
 * comme la liste des membres ou l'administrateur d'un club.
 * Intègre des vérifications de sécurité contextuelles via {@link SecurityService}.
 * Les opérations modifiant la base de données sont transactionnelles.
 *
 * @see Club
 * @see ClubDao
 * @see AdhesionDao
 * @see MembreDao
 * @see EventDao
 * @see SecurityService
 * @see CreateClubRequestDto
 * @see UpdateClubDto
 */
@Service
@RequiredArgsConstructor // Lombok: Injecte les dépendances final via le constructeur.
@Transactional // Transaction par défaut pour toutes les méthodes publiques (read-write).
public class ClubService {

    // --- Dépendances ---
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final EmailService emailService; // Injection de EmailService


    // --- Récupération de base (Sans sécurité contextuelle appliquée ici) ---


    /**
     * Récupère un club actif par son ID.
     * Lance une {@link EntityNotFoundException} si le club n'est pas trouvé ou n'est pas actif.
     * Aucune vérification de sécurité spécifique n'est appliquée ici.
     *
     * @param id L'identifiant du club.
     * @return Le {@link Club} actif trouvé.
     * @throws EntityNotFoundException si aucun club actif n'est trouvé pour cet ID.
     */
    // Pas besoin de @Transactional ici si findById est déjà transactionnel (ce qui est le cas par défaut).
    public Club getClubByIdOrThrow(Integer id) {
        // findById prend en compte le @Where("actif = true") sur l'entité Club
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club actif non trouvé avec l'ID : " + id));
    }


    // --- Récupération avec Sécurité Contextuelle ---

    /**
     * Récupère les détails d'un club actif par son ID, mais seulement si l'utilisateur
     * actuellement authentifié est membre de ce club.
     * <p>
     * Sécurité : Vérifie d'abord l'appartenance au club via {@link SecurityService}.
     * </p>
     *
     * @param id L'identifiant du club à récupérer.
     * @return Le {@link Club} actif correspondant à l'ID.
     * @throws EntityNotFoundException si aucun club actif n'est trouvé pour cet ID (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas membre du club (Statut HTTP 403).
     */
    @Transactional(readOnly = true)
    public Club getClubByIdWithSecurityCheck(Integer id) {
        // 1. Vérification de Sécurité : L'utilisateur est-il membre du club ?
        // Cette vérification implique que le club et l'adhésion existent.
        securityService.checkIsCurrentUserMemberOfClubOrThrow(id); // Lance 403 si non membre.

        // 2. Récupération du Club : Si la sécurité passe, le club actif doit exister.
        // Utilise getClubByIdOrThrow pour récupérer le club et lever 404 en cas de problème rare.
        return getClubByIdOrThrow(id);
    }

    // --- Création ---

    /**
     * Crée un nouveau club ainsi que son premier membre administrateur ({@code Role.ADMIN})
     * à partir des informations fournies dans le DTO {@link CreateClubRequestDto}.
     * Lie automatiquement l'administrateur créé au club via une nouvelle {@link Adhesion}.
     * <p>
     * Règles métier :
     * <ul>
     *     <li>L'email fourni pour l'administrateur doit être unique parmi tous les membres.</li>
     *     <li>L'email fourni pour le club doit être unique parmi tous les clubs.</li>
     * </ul>
     * <p>
     * Sécurité : Aucune vérification de sécurité contextuelle n'est appliquée ici (par exemple,
     * on ne vérifie pas le rôle de l'utilisateur qui appelle cette méthode). La restriction d'accès
     * doit être gérée au niveau du contrôleur (ex: @PreAuthorize ou configuration HttpSecurity).
     * </p>
     *
     * @param dto Le DTO contenant les informations du club et de l'administrateur à créer.
     *            Les validations de base (@NotBlank, @Size, @Email...) sont supposées avoir été faites
     *            via @Valid au niveau du contrôleur.
     * @return Le {@link Club} nouvellement créé et persisté.
     * @throws IllegalArgumentException si l'email de l'administrateur ou du club est déjà utilisé
     *                                  (sera typiquement mappé à HTTP 409 Conflict).
     */
    @Transactional // Assure l'atomicité de la création (Membre + Club + Adhesion).
    public Club createClubAndRegisterAdmin(CreateClubRequestDto dto) {

        // 1. Extraction et Normalisation des données sensibles (emails)
        CreateClubRequestDto.AdminInfo adminInfo = dto.getAdmin(); // Garanti non null par @Valid sur le DTO
        String adminEmail = adminInfo.getEmail().toLowerCase().trim();
        String clubEmail = dto.getEmail().toLowerCase().trim();

        // 2. Validations Métier (Unicité des emails)
        if (membreRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("L'email fourni pour l'administrateur est déjà utilisé : " + adminInfo.getEmail());
        }
        if (clubRepository.findByEmail(clubEmail).isPresent()) { // findByEmail respecte @Where("actif=true")
            throw new IllegalArgumentException("L'email fourni pour le club est déjà utilisé par un club actif : " + dto.getEmail());
        }

        // 3. Création et Sauvegarde du Membre Administrateur
        Membre adminToSave = mapAdminInfoToMembre(adminInfo); // Utilisation d'une méthode helper pour le mapping
        adminToSave.setPassword(passwordEncoder.encode(adminInfo.getPassword())); // Hachage du mot de passe
        adminToSave.setRole(Role.ADMIN); // Attribution du rôle ADMIN
        Membre savedAdmin = membreRepository.save(adminToSave);
        adminToSave.setVerified(false); // Le compte admin n'est pas vérifié initialement
        String verificationToken = UUID.randomUUID().toString();
        adminToSave.setVerificationToken(verificationToken);
        // 4. Création et Sauvegarde du Club
        Club clubToSave = mapDtoToClub(dto); // Utilisation d'une méthode helper
        Club savedClubWithId = clubRepository.save(clubToSave);
        // Note: Le codeClub sera généré par @PostPersist après cette sauvegarde.
        try {
            // Récupère l'ID généré
            Integer clubId = savedClubWithId.getId();
            if (clubId == null) {
                // Sécurité : si l'ID n'est pas généré comme attendu
                throw new IllegalStateException("Impossible de générer le code club : l'ID du club est null après sauvegarde.");
            }
            // Formate le code en utilisant l'ID (ex: CLUB-0001)
            // %04d signifie : pad avec des 0, largeur minimale de 4, pour un entier décimal (d)
            String formattedCode = String.format("CLUB-%04d", clubId); // [2][3][4]
            System.out.println(formattedCode);
            savedClubWithId.setCodeClub(formattedCode); // Assigne le code à l'entité *en mémoire*

        } catch (Exception e) {
            // Loguez l'erreur et décidez si vous devez annuler la transaction
            // Par exemple, lever une RuntimeException pour forcer le rollback
            // Logger.error("Erreur critique lors de la génération/assignation du codeClub pour ID={}", savedClubWithId.getId(), e);
            throw new RuntimeException("Erreur lors de la finalisation de la création du club (codeClub).", e);
        }
        // 5. Création et Sauvegarde de l'Adhesion liant l'admin au club
        Adhesion adminAdhesion = new Adhesion(savedAdmin, savedClubWithId);
        adhesionRepository.save(adminAdhesion);

        try {
            emailService.sendVerificationEmail(savedAdmin); // Utilise la méthode existante de EmailService
        } catch (Exception e) {
            // Log l'erreur d'envoi d'email.
            // Décidez de la stratégie :
            // - Continuer la création du club et l'admin devra demander un nouveau lien/être vérifié manuellement.
            // - Ou annuler la transaction (throw new RuntimeException("Erreur envoi email", e);)
            System.err.println("CRITICAL: Échec de l'envoi de l'email de vérification pour l'admin du club " +
                    savedAdmin.getEmail() + ". Le compte a été créé mais n'est pas vérifié. Détails: " + e.getMessage());
            // Si l'envoi d'email est critique et doit annuler l'inscription :
            // throw new RuntimeException("Échec de l'envoi de l'email de vérification. L'inscription du club a été annulée.", e);
        }
        // 6. Retourner le club créé (avec son ID et potentiellement son codeClub généré)
        return savedClubWithId;
    }

    /**
     * Helper privé pour mapper AdminInfo (du DTO) vers une entité Membre.
     */
    private Membre mapAdminInfoToMembre(CreateClubRequestDto.AdminInfo adminInfo) {
        Membre membre = new Membre();
        membre.setNom(adminInfo.getNom());
        membre.setPrenom(adminInfo.getPrenom());
        membre.setDate_naissance(adminInfo.getDate_naissance());
        membre.setTelephone(adminInfo.getTelephone());
        membre.setEmail(adminInfo.getEmail().toLowerCase().trim()); // Email normalisé
        membre.setDate_inscription(LocalDate.now()); // Date inscription système
        membre.setActif(true); // Actif par défaut
        // Le rôle et le mot de passe sont définis dans la méthode appelante
        return membre;
    }

    /**
     * Helper privé pour mapper CreateClubRequestDto vers une entité Club.
     */
    private Club mapDtoToClub(CreateClubRequestDto dto) {
        Club club = new Club();
        club.setNom(dto.getNom());
        club.setEmail(dto.getEmail().toLowerCase().trim()); // Email normalisé
        club.setNumero_voie(dto.getNumero_voie());
        club.setRue(dto.getRue());
        club.setCodepostal(dto.getCodepostal());
        club.setVille(dto.getVille());
        club.setTelephone(dto.getTelephone());
        club.setDate_creation(dto.getDate_creation()); // Date "réelle" fournie
        club.setDate_inscription(LocalDate.now()); // Date inscription système
        club.setActif(true); // Actif par défaut
        club.setAdhesions(new HashSet<>()); // Initialiser les collections
        club.setEvenements(new ArrayList<>());
        return club;
    }

    // --- Mise à Jour ---

    /**
     * Met à jour les informations modifiables (nom, adresse, email, téléphone) d'un club existant.
     * <p>
     * Sécurité : Seul l'administrateur spécifique ({@code Role.ADMIN}) de ce club
     * (identifié par {@code id}) peut effectuer cette opération. La vérification est faite
     * via {@link SecurityService#checkIsActualAdminOfClubOrThrow}.
     * </p>
     * <p>
     * Règles métier :
     * <ul>
     *     <li>Le club doit exister et être actif.</li>
     *     <li>Si l'email est modifié, le nouvel email ne doit pas être déjà utilisé par un *autre* club.</li>
     * </ul>
     *
     * @param id        L'identifiant du club à mettre à jour.
     * @param updateDto Le DTO {@link UpdateClubDto} contenant les nouvelles valeurs. Les champs non fournis
     *                  ou vides dans le DTO n'écraseront pas les valeurs existantes (logique de mise à jour partielle).
     * @return Le {@link Club} mis à jour et persisté.
     * @throws EntityNotFoundException  si aucun club actif n'est trouvé pour l'ID fourni (Statut HTTP 404).
     * @throws AccessDeniedException    si l'utilisateur courant n'est pas l'administrateur de ce club (Statut HTTP 403).
     * @throws IllegalArgumentException si le nouvel email fourni est déjà utilisé par un autre club
     *                                  (sera typiquement mappé à HTTP 409 Conflict).
     */
    @Transactional // Read-write.
    public Club updateClub(Integer id, UpdateClubDto updateDto) {
        // 1. Vérification de Sécurité : Seul l'admin de CE club peut le modifier.
        securityService.checkIsActualAdminOfClubOrThrow(id); // Lance 403 si non admin.

        // 2. Récupération du Club existant (et actif).
        Club existingClub = getClubByIdOrThrow(id); // Lance 404 si non trouvé/actif.

        boolean updated = false; // Pour optimiser la sauvegarde.

        // 3. Mise à jour conditionnelle des champs fournis dans le DTO.
        if (updateDto.getNom() != null && !updateDto.getNom().isBlank()) {
            existingClub.setNom(updateDto.getNom().trim()); // Trim pour nettoyer
            updated = true;
        }
        if (updateDto.getNumero_voie() != null && !updateDto.getNumero_voie().isBlank()) {
            existingClub.setNumero_voie(updateDto.getNumero_voie().trim());
            updated = true;
        }
        if (updateDto.getRue() != null && !updateDto.getRue().isBlank()) {
            existingClub.setRue(updateDto.getRue().trim());
            updated = true;
        }
        if (updateDto.getCodepostal() != null && !updateDto.getCodepostal().isBlank()) {
            existingClub.setCodepostal(updateDto.getCodepostal().trim());
            updated = true;
        }
        if (updateDto.getVille() != null && !updateDto.getVille().isBlank()) {
            existingClub.setVille(updateDto.getVille().trim());
            updated = true;
        }
        if (updateDto.getTelephone() != null && !updateDto.getTelephone().isBlank()) {
            existingClub.setTelephone(updateDto.getTelephone().trim());
            updated = true;
        }

        // Traitement spécifique pour l'email (avec vérification d'unicité).
        String newEmail = updateDto.getEmail();
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(existingClub.getEmail())) {
            String normalizedNewEmail = newEmail.toLowerCase().trim();
            // Vérifie si le nouvel email est déjà pris par un AUTRE club.
            if (clubRepository.existsByEmailAndIdNot(normalizedNewEmail, id)) {
                throw new IllegalArgumentException("L'email '" + newEmail + "' est déjà utilisé par un autre club.");
            }
            existingClub.setEmail(normalizedNewEmail);
            updated = true;
        }

        // 4. Sauvegarde si des modifications ont été apportées.
        if (updated) {
            return clubRepository.save(existingClub);
        }
        return existingClub; // Retourne l'entité non modifiée si rien n'a changé.
    }

    // --- Suppression (Logique) ---

    /**
     * Désactive un club (suppression logique / soft delete).
     * Modifie le statut {@code actif} à {@code false} et anonymise certaines données via
     * {@link Club#prepareForDeactivation()}.
     * <p>
     * Sécurité : Seul l'administrateur spécifique ({@code Role.ADMIN}) de ce club
     * (identifié par {@code id}) peut effectuer cette opération.
     * </p>
     * <p>
     * Règles métier :
     * <ul>
     *     <li>Le club doit exister et être actuellement actif.</li>
     *     <li>La désactivation est **empêchée** si le club a encore des événements futurs qui sont marqués comme actifs.
     *         Ces événements doivent être annulés ou supprimés au préalable.</li>
     * </ul>
     *
     * @param id L'identifiant du club à désactiver.
     * @throws EntityNotFoundException si aucun club actif n'est trouvé pour l'ID fourni (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas l'administrateur de ce club (Statut HTTP 403).
     * @throws IllegalStateException   si le club a encore des événements futurs actifs, empêchant la désactivation
     *                                 (sera typiquement mappé à HTTP 409 Conflict).
     */
    @Transactional // Read-write.
    public void deactivateClub(Integer id) {
        // 1. Vérification de Sécurité : Seul l'admin de CE club.
        securityService.checkIsActualAdminOfClubOrThrow(id);

        // 2. Récupérer le club actif à désactiver.
        Club clubToDeactivate = getActiveClubByIdOrThrow(id); // Utilise la méthode helper qui gère @Where

        // 3. Validation Métier : Y a-t-il des événements futurs actifs ?
        LocalDateTime now = LocalDateTime.now();
        // Attention: clubToDeactivate.getEvenements() peut charger beaucoup d'événements si LAZY.
        List<Event> activeFutureEvents = clubToDeactivate.getEvenements().stream()
                .filter(event -> event != null && event.getActif() != null && event.getActif()) // Filtre sur event actif
                .filter(event -> event.getStartTime() != null && event.getStartTime().isAfter(now))    // Filtre sur date future
                .toList(); // Collecte les événements bloquants.

        if (!activeFutureEvents.isEmpty()) {
            // Construit un message d'erreur listant les événements bloquants.
            String eventNames = activeFutureEvents.stream()
                    .map(event -> "'" + event.getNom() + "' (ID: " + event.getId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Impossible de désactiver le club : Des événements futurs actifs existent encore : " + eventNames
                    + ". Veuillez d'abord les annuler ou les supprimer.");
        }

        // 4. Procéder à la désactivation logique si aucune règle n'est violée.
        clubToDeactivate.prepareForDeactivation(); // Modifie nom, email, date désactivation
        clubToDeactivate.setActif(false);          // Met le flag principal à false
        clubRepository.save(clubToDeactivate);     // Persiste les changements (UPDATE)
    }

    /**
     * Méthode helper pour récupérer un club par ID en s'assurant qu'il est actif.
     * Cette méthode repose sur le filtre {@code @Where(clause = "actif = true")} défini
     * sur l'entité {@link Club}.
     *
     * @param id L'identifiant du club.
     * @return Le {@link Club} actif.
     * @throws EntityNotFoundException si aucun club actif n'est trouvé pour cet ID.
     */
    public Club getActiveClubByIdOrThrow(Integer id) {
        // findById respecte le @Where global sur l'entité.
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club actif non trouvé avec l'ID : " + id));
    }

    // --- Récupération d'Informations Liées ---

    /**
     * Récupère l'ensemble des membres actifs associés à un club spécifique (via leurs adhésions).
     * <p>
     * Sécurité : Seul un utilisateur authentifié qui est lui-même membre de ce club
     * (quel que soit son rôle) peut consulter cette liste. La vérification est faite
     * via {@link SecurityService#checkIsCurrentUserMemberOfClubOrThrow}.
     * </p>
     *
     * @param clubId L'identifiant du club dont on veut les membres.
     * @return Un {@link Set<Membre>} contenant les membres actifs du club.
     * Retourne un Set vide si le club n'a pas de membres actifs ou si le club n'existe pas (après la vérif sécurité).
     * @throws EntityNotFoundException si aucun club (actif ou inactif) n'est trouvé pour {@code clubId}
     *                                 *avant* la vérification de sécurité (cas rare), ou si le club existe mais
     *                                 la requête pour les membres échoue (cas encore plus rare).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas membre du club (Statut HTTP 403).
     */
    @Transactional(readOnly = true)
    public Set<Membre> findMembresForClub(Integer clubId) {
        // 1. Vérification de Sécurité : L'utilisateur est-il membre ?
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance 403 si non.

        // 2. Vérifier l'existence du club (sécurité supplémentaire, pourrait être omis si on fait confiance à la 1ère étape).
        // Utiliser existsById qui ne tient PAS compte du @Where par défaut.
        if (!clubRepository.existsById(clubId)) {
            // Ce cas est peu probable si l'étape 1 a réussi, mais gère une éventuelle race condition.
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }

        // 3. Récupération des Membres actifs du club via MembreDao.
        // La méthode findByAdhesionsClubId devrait implicitement bénéficier du @Where sur Membre.
        List<Membre> membresList = membreRepository.findByAdhesionsClubId(clubId);

        // 4. Conversion en Set.
        return new HashSet<>(membresList);
    }
}

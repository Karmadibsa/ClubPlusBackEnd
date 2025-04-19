package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Gère l'injection des dépendances final
@Transactional // Transaction par défaut pour toutes les méthodes publiques
public class ClubService {

    // --- Dépendances ---
    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService; // Service pour la sécurité contextuelle
    // private final EventDao eventRepository; // Injecter si besoin pour des vérifications sur les événements

    // --- Récupération de base (Pas de sécurité contextuelle ici) ---

    /**
     * Récupère tous les clubs (informations de base).
     */
    @Transactional(readOnly = true)
    public List<Club> findAllClubs() {
        return clubRepository.findAll();
    }

    /**
     * Récupère un club par son ID (sans garantie d'existence).
     */
    @Transactional(readOnly = true)
    public Optional<Club> findClubById(Integer id) {
        return clubRepository.findById(id);
    }

    /**
     * Récupère un club par son code unique (sans garantie d'existence).
     */
    @Transactional(readOnly = true)
    public Optional<Club> findClubByCode(String codeClub) {
        return clubRepository.findByCodeClub(codeClub);
    }

    /**
     * Récupère un club par ID ou lance EntityNotFoundException.
     */
    public Club getClubByIdOrThrow(Integer id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un club par code ou lance EntityNotFoundException.
     */
    public Club getClubByCodeOrThrow(String codeClub) {
        return clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub));
    }

    // --- Récupération avec Sécurité Contextuelle ---

    /**
     * Récupère un club par ID et vérifie si l'utilisateur courant est membre.
     * Lance EntityNotFoundException (404) ou AccessDeniedException (403).
     */
    @Transactional(readOnly = true)
    public Club getClubByIdWithSecurityCheck(Integer id) {
        // 1. Vérifier l'appartenance de l'utilisateur au club.
        securityService.checkIsCurrentUserMemberOfClubOrThrow(id); // Lance 403 si non membre

        // 2. Si la sécurité est passée, récupérer le club.
        // Le club doit exister, car sinon la vérification d'adhésion aurait échoué ou donné un résultat inattendu.
        // Mais getClubByIdOrThrow reste la manière la plus sûre de le récupérer.
        return getClubByIdOrThrow(id); // Lance 404 si problème rare (club supprimé entre temps)
    }

    // --- Création ---

    /**
     * Crée un Club et son premier Membre administrateur à partir d'un DTO.
     * Lie les deux via une Adhesion.
     * Aucune sécurité contextuelle spécifique ici.
     * Lance IllegalArgumentException (400/409) si données invalides ou conflits (emails).
     * La validation @Valid sur le DTO dans le contrôleur a déjà vérifié les contraintes de base.
     */
    @Transactional // Atomicité
    public Club createClubAndRegisterAdmin(CreateClubRequestDto dto) { // Accepte le DTO

        // --- 1. Extraire les données Admin du DTO ---
        CreateClubRequestDto.AdminInfo adminInfo = dto.getAdmin();
        // La validation @NotNull sur dto.admin garantit que adminInfo n'est pas null ici.

        // --- 2. Validations Métier (Conflits) ---
        String adminEmail = adminInfo.getEmail().toLowerCase().trim();
        String clubEmail = dto.getEmail().toLowerCase().trim(); // Email du club depuis le DTO principal

        membreRepository.findByEmail(adminEmail).ifPresent(m -> {
            throw new IllegalArgumentException("Email admin déjà utilisé: " + adminInfo.getEmail()); // -> 409
        });
        clubRepository.findByEmail(clubEmail).ifPresent(c -> {
            throw new IllegalArgumentException("Email club déjà utilisé: " + dto.getEmail()); // -> 409
        });

        // --- 3. Création du Membre Admin (Mapping DTO -> Entité) ---
        Membre adminToSave = new Membre();
        adminToSave.setNom(adminInfo.getNom());
        adminToSave.setPrenom(adminInfo.getPrenom());
        adminToSave.setDate_naissance(adminInfo.getDate_naissance());
        adminToSave.setNumero_voie(adminInfo.getNumero_voie());
        adminToSave.setRue(adminInfo.getRue());
        adminToSave.setCodepostal(adminInfo.getCodepostal());
        adminToSave.setVille(adminInfo.getVille());
        adminToSave.setTelephone(adminInfo.getTelephone());
        adminToSave.setEmail(adminEmail); // Email normalisé
        adminToSave.setPassword(passwordEncoder.encode(adminInfo.getPassword())); // Hachage
        adminToSave.setDate_inscription(LocalDate.now()); // Date inscription MEMBRE
        adminToSave.setRole(Role.ADMIN); // Rôle
        adminToSave.setId(null);
        Membre savedAdmin = membreRepository.save(adminToSave);

        // --- 4. Création du Club (Mapping DTO -> Entité) ---
        Club clubToSave = new Club();
        clubToSave.setNom(dto.getNom());
        clubToSave.setEmail(clubEmail); // Email normalisé
        clubToSave.setNumero_voie(dto.getNumero_voie());
        clubToSave.setRue(dto.getRue());
        clubToSave.setCodepostal(dto.getCodepostal());
        clubToSave.setVille(dto.getVille());
        clubToSave.setTelephone(dto.getTelephone());
        clubToSave.setDate_creation(dto.getDate_creation()); // Date réelle du club depuis DTO
        clubToSave.setDate_inscription(LocalDate.now()); // Date inscription CLUB dans l'app
        clubToSave.setCodeClub(null); // Généré par @PostPersist
        clubToSave.setId(null);
        clubToSave.setAdhesions(new HashSet<>());
        clubToSave.setEvenements(new ArrayList<>());
        Club savedClub = clubRepository.save(clubToSave);

        // --- 5. Création de l'Adhesion ---
        Adhesion adminAdhesion = new Adhesion(savedAdmin, savedClub);
        adhesionRepository.save(adminAdhesion);

        // --- 6. Retourner le club créé ---
        return savedClub;
    }

    // --- Mise à Jour ---

    /**
     * Met à jour les informations d'un club.
     * Sécurité: Seul l'ADMINISTRATEUR spécifique de ce club peut le faire.
     * Lance EntityNotFoundException (404), AccessDeniedException (403), IllegalArgumentException (409).
     */
    public Club updateClub(Integer id, UpdateClubDto updateDto) {
        // 1. Vérification Sécurité Contextuelle : Est-ce l'admin DE CE club ?
        securityService.checkIsActualAdminOfClubOrThrow(id); // Lance 403 si non admin du club

        // 2. Récupérer le club existant.
        Club existingClub = getClubByIdOrThrow(id); // Lance 404 si non trouvé

        boolean updated = false;

        // 3. Mettre à jour les champs modifiables (adresse, contact...)
        if (updateDto.getNom() != null && !updateDto.getNom().isBlank()) {
            existingClub.setNom(updateDto.getNom());
            updated = true;
        }
        // ... (Mettre à jour numero_voie, rue, codepostal, ville, telephone pareil) ...
        if (updateDto.getNumero_voie() != null) {
            existingClub.setNumero_voie(updateDto.getNumero_voie());
            updated = true;
        }
        if (updateDto.getRue() != null) {
            existingClub.setRue(updateDto.getRue());
            updated = true;
        }
        if (updateDto.getCodepostal() != null) {
            existingClub.setCodepostal(updateDto.getCodepostal());
            updated = true;
        }
        if (updateDto.getVille() != null) {
            existingClub.setVille(updateDto.getVille());
            updated = true;
        }
        if (updateDto.getTelephone() != null) {
            existingClub.setTelephone(updateDto.getTelephone());
            updated = true;
        }

        // Mettre à jour l'email (avec vérification d'unicité)
        String newEmail = updateDto.getEmail();
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(existingClub.getEmail())) {
            String normalizedNewEmail = newEmail.toLowerCase().trim();
            if (clubRepository.existsByEmailAndIdNot(normalizedNewEmail, id)) {
                throw new IllegalArgumentException("Email déjà utilisé par un autre club: " + newEmail); // -> 409 Conflict
            }
            existingClub.setEmail(normalizedNewEmail);
            updated = true;
        }

        // 4. Sauvegarder si des changements ont eu lieu.
        if (updated) {
            return clubRepository.save(existingClub);
        }
        return existingClub;
    }

    // --- Suppression ---

    /**
     * Désactive un club après vérifications de sécurité et métier.
     * Remplace la suppression physique par une désactivation logique.
     * Empêche la désactivation si le club a des événements futurs qui sont encore ACTIFS.
     * Ne désactive PAS les événements en cascade.
     *
     * @param id L'ID du club à désactiver.
     * @throws EntityNotFoundException si le club actif n'est pas trouvé.
     * @throws SecurityException       (ou similaire) si l'utilisateur n'est pas admin du club.
     * @throws IllegalStateException   si le club a des événements futurs actifs.
     */
    @Transactional
    public void deactivateClub(Integer id) {
        // 1. Vérification Sécurité Contextuelle
        securityService.checkIsActualAdminOfClubOrThrow(id);

        // 2. Récupérer le club ACTIF
        Club clubToDeactivate = getActiveClubByIdOrThrow(id);

        // 3. Validation Métier : Vérifier l'existence d'événements futurs ACTIFS
        LocalDateTime now = LocalDateTime.now();
        // IMPORTANT: clubToDeactivate.getEvenements() charge TOUS les événements (actifs/inactifs)
        // car @Where a été retiré de Event. Il faut donc filtrer explicitement ici.
        List<Event> activeFutureEvents = clubToDeactivate.getEvenements().stream()
                .filter(Event::getActif) // Ne considère que les événements actifs
                .filter(event -> event.getStart() != null && event.getStart().isAfter(now)) // Qui sont dans le futur
                .collect(Collectors.toList()); // Récupère la liste des événements correspondants

        if (!activeFutureEvents.isEmpty()) {
            // Construit un message d'erreur informatif
            String eventNames = activeFutureEvents.stream()
                    .map(event -> "'" + event.getNom() + "' (ID: " + event.getId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Impossible de désactiver le club : Des événements futurs actifs existent encore : " + eventNames
                    + ". Veuillez d'abord les annuler ou les supprimer."); // -> 409 Conflict
        }

        // 4. Si la vérification passe (pas d'événements futurs actifs), procéder à la Désactivation du Club
        clubToDeactivate.prepareForDeactivation(); // Modifie nom, email, met date
        clubToDeactivate.setActif(false);          // Met le flag à false
        clubRepository.save(clubToDeactivate);    // Sauvegarde (UPDATE)
    }

    // Récupère un club ACTIF (grâce au @Where sur Club)
    public Club getActiveClubByIdOrThrow(Integer id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club actif non trouvé avec l'ID : " + id));
    }


    // --- Récupération d'informations liées ---

    /**
     * Récupère la liste des membres (via les adhésions) d'un club.
     * Sécurité: Seul un MEMBRE (ou plus) de ce club peut voir la liste.
     * Lance EntityNotFoundException (404) ou AccessDeniedException (403).
     */
    @Transactional(readOnly = true)
    public Set<Membre> findMembresForClub(Integer clubId) {
        // 1. Vérification Sécurité Contextuelle
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        // 2. Vérifier l'existence du club
        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }

        // 3. Récupération via la nouvelle méthode (retourne List<Membre>)
        List<Membre> membresList = membreRepository.findByAdhesionsClubId(clubId);

        // Conversion en Set pour correspondre au type de retour
        return new HashSet<>(membresList);
    }


    /**
     * Récupère le membre ADMIN spécifique d'un club.
     * Sécurité: Seul un MEMBRE (ou plus) de ce club peut voir qui est l'admin.
     * Lance EntityNotFoundException (404 pour club ou admin) ou AccessDeniedException (403).
     */
    @Transactional(readOnly = true)
    public Membre getAdminForClubOrThrow(Integer clubId) {
        // 1. Vérification Sécurité Contextuelle : Est-ce un membre du club ?
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance 403 si non membre

        // 2. Récupérer l'admin via une requête spécifique (supposée exister dans MembreDao).
        return membreRepository.findAdminByClubId(clubId) // Adapter le nom si nécessaire
                .orElseThrow(() -> new EntityNotFoundException("Administrateur non trouvé pour le club ID: " + clubId)); // Lance 404
    }

}

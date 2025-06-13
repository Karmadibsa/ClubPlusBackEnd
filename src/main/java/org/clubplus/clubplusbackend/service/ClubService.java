package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier pour les entités {@link Club}.
 * <p>
 * Ce service assure les opérations CRUD pour les clubs, la gestion des administrateurs,
 * et applique les règles métier et les vérifications de sécurité nécessaires.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClubService {

    private static final Logger logger = LoggerFactory.getLogger(ClubService.class);

    private final ClubDao clubRepository;
    private final AdhesionDao adhesionRepository;
    private final MembreDao membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final EmailService emailService;

    /**
     * Récupère un club par son ID.
     * <p>
     * Ne contient pas de vérification de sécurité contextuelle.
     *
     * @param id L'identifiant du club.
     * @return Le {@link Club} trouvé.
     * @throws EntityNotFoundException si aucun club actif n'est trouvé pour cet ID.
     */
    public Club getClubByIdOrThrow(Integer id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club actif non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un club par son ID, après avoir vérifié que l'utilisateur courant en est membre.
     *
     * @param id L'identifiant du club.
     * @return Le {@link Club} trouvé.
     * @throws EntityNotFoundException si le club n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public Club getClubByIdWithSecurityCheck(Integer id) {
        securityService.checkIsCurrentUserMemberOfClubOrThrow(id);
        return getClubByIdOrThrow(id);
    }

    /**
     * Crée un nouveau club et son administrateur initial en une seule transaction.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>Les emails du club et de l'administrateur doivent être uniques.</li>
     * <li>Un email de vérification est envoyé à l'administrateur.</li>
     * </ul>
     *
     * @param dto Le DTO contenant les informations du club et de l'administrateur.
     * @return Le nouveau club créé.
     * @throws IllegalArgumentException si un email est déjà utilisé.
     */
    public Club createClubAndRegisterAdmin(CreateClubRequestDto dto) {
        CreateClubRequestDto.AdminInfo adminInfo = dto.getAdmin();
        String adminEmail = adminInfo.getEmail().toLowerCase().trim();
        String clubEmail = dto.getEmail().toLowerCase().trim();

        if (membreRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("L'email fourni pour l'administrateur est déjà utilisé : " + adminInfo.getEmail());
        }
        if (clubRepository.findByEmail(clubEmail).isPresent()) {
            throw new IllegalArgumentException("L'email fourni pour le club est déjà utilisé par un club actif : " + dto.getEmail());
        }

        Membre adminToSave = mapAdminInfoToMembre(adminInfo);
        adminToSave.setPassword(passwordEncoder.encode(adminInfo.getPassword()));
        adminToSave.setRole(Role.ADMIN);
        adminToSave.setVerified(false);
        adminToSave.setVerificationToken(UUID.randomUUID().toString());
        Membre savedAdmin = membreRepository.save(adminToSave);

        Club clubToSave = mapDtoToClub(dto);
        Club savedClubWithId = clubRepository.save(clubToSave);

        try {
            Integer clubId = savedClubWithId.getId();
            if (clubId == null) {
                throw new IllegalStateException("Impossible de générer le code club : l'ID du club est null après sauvegarde.");
            }
            String formattedCode = String.format("CLUB-%04d", clubId);
            savedClubWithId.setCodeClub(formattedCode);
        } catch (Exception e) {
            logger.error("Erreur critique lors de la génération du codeClub pour ID={}", savedClubWithId.getId(), e);
            throw new RuntimeException("Erreur lors de la finalisation de la création du club (codeClub).", e);
        }

        Adhesion adminAdhesion = new Adhesion(savedAdmin, savedClubWithId);
        adhesionRepository.save(adminAdhesion);

        try {
            emailService.sendVerificationEmail(savedAdmin);
        } catch (Exception e) {
            logger.error("CRITICAL: Échec de l'envoi de l'email de vérification pour l'admin {}. Le compte a été créé mais n'est pas vérifié. Détails: {}", savedAdmin.getEmail(), e.getMessage());
            // Selon la politique, on pourrait vouloir annuler la transaction ici.
            // throw new RuntimeException("Échec de l'envoi de l'email de vérification.", e);
        }
        return savedClubWithId;
    }

    /**
     * Met à jour les informations d'un club existant.
     * <p>
     * <b>Sécurité :</b> Seul l'administrateur du club peut effectuer cette opération.
     *
     * @param id        L'ID du club à mettre à jour.
     * @param updateDto Le DTO contenant les nouvelles valeurs.
     * @return Le club mis à jour.
     * @throws EntityNotFoundException  si le club n'est pas trouvé.
     * @throws AccessDeniedException    si l'utilisateur n'est pas l'administrateur du club.
     * @throws IllegalArgumentException si le nouvel email est déjà utilisé par un autre club.
     */
    public Club updateClub(Integer id, UpdateClubDto updateDto) {
        securityService.checkIsActualAdminOfClubOrThrow(id);
        Club existingClub = getClubByIdOrThrow(id);

        boolean updated = false;

        if (updateDto.getNom() != null && !updateDto.getNom().isBlank()) {
            existingClub.setNom(updateDto.getNom().trim());
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

        String newEmail = updateDto.getEmail();
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(existingClub.getEmail())) {
            String normalizedNewEmail = newEmail.toLowerCase().trim();
            if (clubRepository.existsByEmailAndIdNot(normalizedNewEmail, id)) {
                throw new IllegalArgumentException("L'email '" + newEmail + "' est déjà utilisé par un autre club.");
            }
            existingClub.setEmail(normalizedNewEmail);
            updated = true;
        }

        if (updated) {
            return clubRepository.save(existingClub);
        }
        return existingClub;
    }

    /**
     * Désactive un club (suppression logique).
     * <p>
     * <b>Sécurité :</b> Seul l'administrateur du club peut effectuer cette opération.
     * <p>
     * <b>Règle métier :</b> La désactivation est impossible si le club a des événements futurs actifs.
     *
     * @param id L'ID du club à désactiver.
     * @throws EntityNotFoundException si le club n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas l'administrateur du club.
     * @throws IllegalStateException   si des événements futurs actifs empêchent la désactivation.
     */
    public void deactivateClub(Integer id) {
        securityService.checkIsActualAdminOfClubOrThrow(id);
        Club clubToDeactivate = getActiveClubByIdOrThrow(id);

        List<Event> activeFutureEvents = clubToDeactivate.getEvenements().stream()
                .filter(event -> event != null && event.getActif() && event.getStartTime().isAfter(Instant.now()))
                .toList();

        if (!activeFutureEvents.isEmpty()) {
            String eventNames = activeFutureEvents.stream()
                    .map(event -> String.format("'%s' (ID: %d)", event.getNom(), event.getId()))
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Impossible de désactiver le club : des événements futurs actifs existent : " + eventNames);
        }

        clubToDeactivate.prepareForDeactivation();
        clubToDeactivate.setActif(false);
        clubRepository.save(clubToDeactivate);
    }

    /**
     * Récupère les membres d'un club.
     * <p>
     * <b>Sécurité :</b> Seul un membre du club peut consulter cette liste.
     *
     * @param clubId L'ID du club.
     * @return Un ensemble des membres actifs du club.
     * @throws AccessDeniedException si l'utilisateur n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public Set<Membre> findMembresForClub(Integer clubId) {
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        if (!clubRepository.existsById(clubId)) {
            throw new EntityNotFoundException("Club non trouvé avec l'ID : " + clubId);
        }

        List<Membre> membresList = membreRepository.findByAdhesionsClubId(clubId);
        return new HashSet<>(membresList);
    }

    // --- Méthodes privées ---

    private Membre mapAdminInfoToMembre(CreateClubRequestDto.AdminInfo adminInfo) {
        Membre membre = new Membre();
        membre.setNom(adminInfo.getNom());
        membre.setPrenom(adminInfo.getPrenom());
        membre.setDate_naissance(adminInfo.getDate_naissance());
        membre.setTelephone(adminInfo.getTelephone());
        membre.setEmail(adminInfo.getEmail().toLowerCase().trim());
        membre.setDate_inscription(LocalDate.now());
        membre.setActif(true);
        return membre;
    }

    private Club mapDtoToClub(CreateClubRequestDto dto) {
        Club club = new Club();
        club.setNom(dto.getNom());
        club.setEmail(dto.getEmail().toLowerCase().trim());
        club.setNumero_voie(dto.getNumero_voie());
        club.setRue(dto.getRue());
        club.setCodepostal(dto.getCodepostal());
        club.setVille(dto.getVille());
        club.setTelephone(dto.getTelephone());
        club.setDate_creation(dto.getDate_creation());
        club.setDate_inscription(LocalDate.now());
        club.setActif(true);
        club.setAdhesions(new HashSet<>());
        club.setEvenements(new ArrayList<>());
        return club;
    }

    private Club getActiveClubByIdOrThrow(Integer id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club actif non trouvé avec l'ID : " + id));
    }
}

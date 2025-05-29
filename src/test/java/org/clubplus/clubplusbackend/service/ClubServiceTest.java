package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour le service {@link ClubService}.
 * <p>
 * Cette classe utilise Mockito pour créer des simulations (mocks) des dépendances externes
 * de {@code ClubService}, telles que les DAOs ({@link ClubDao}, {@link MembreDao}, etc.)
 * et d'autres services ({@link SecurityService}, {@link EmailService}).
 * L'objectif est de tester la logique métier de {@code ClubService} de manière isolée,
 * en contrôlant le comportement de ses dépendances.
 * </p>
 * <p>
 * L'annotation {@code @ExtendWith(MockitoExtension.class)} intègre JUnit 5 avec Mockito,
 * ce qui permet l'initialisation automatique des champs annotés avec {@code @Mock} et
 * l'injection de ces mocks dans l'instance de {@code ClubService} annotée avec {@code @InjectMocks}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    /**
     * Mock pour le Data Access Object (DAO) des clubs.
     * Permet de simuler les interactions avec la base de données pour les entités {@link Club}.
     */
    @Mock
    private ClubDao clubRepository;

    /**
     * Mock pour le DAO des adhésions.
     * Utilisé pour simuler la création et la recherche d'adhésions entre membres et clubs.
     */
    @Mock
    private AdhesionDao adhesionRepository;

    /**
     * Mock pour le DAO des membres.
     * Utilisé pour simuler la création, la recherche et la vérification d'existence des membres.
     */
    @Mock
    private MembreDao membreRepository;

    /**
     * Mock pour l'encodeur de mots de passe.
     * Utilisé pour simuler l'encodage des mots de passe lors de la création de membres.
     */
    @Mock
    private PasswordEncoder passwordEncoder;

    /**
     * Mock pour le service de sécurité.
     * Utilisé pour simuler la récupération de l'utilisateur courant et les vérifications d'autorisation.
     */
    @Mock
    private SecurityService securityService;

    /**
     * Mock pour le service d'envoi d'emails.
     * Utilisé pour simuler l'envoi d'emails de vérification ou de notification.
     */
    @Mock
    private EmailService emailService;

    /**
     * Mock pour le DAO des événements.
     * Utilisé pour simuler les interactions avec la base de données pour les entités {@link Event},
     * notamment pour vérifier les événements futurs actifs lors de la désactivation d'un club.
     */
    @Mock
    private EventDao eventRepository;

    /**
     * Instance de {@link ClubService} à tester.
     * Les mocks déclarés ci-dessus seront injectés dans cette instance par Mockito.
     */
    @InjectMocks
    private ClubService clubService;

    // Objets de test de base, réinitialisés avant chaque test pour assurer l'indépendance.
    private Club clubTest;                // Un club de test générique.
    private Membre adminTest;             // Un membre administrateur de test.
    private CreateClubRequestDto createClubDto; // DTO pour les requêtes de création de club.
    private UpdateClubDto updateClubDto;     // DTO pour les requêtes de mise à jour de club.

    /**
     * Méthode de configuration exécutée avant chaque méthode de test (annotée avec {@code @Test}).
     * Initialise les objets de test communs ({@link Club}, {@link Membre}, DTOs) avec des valeurs
     * par défaut pour faciliter l'écriture des tests.
     */
    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now(); // Représente la date actuelle en UTC car la JVM est en UTC

// 2. Soustraire un an pour obtenir la LocalDate d'il y a un an
        LocalDate oneYearAgoDate = today.minusYears(1);

// 3. Convertir cette LocalDate en un Instant au début de cette journée en UTC
        Instant oneYearAgoInstant = oneYearAgoDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        // Initialisation d'un Club de test de base
        clubTest = new Club();
        clubTest.setId(1);
        clubTest.setNom("Club de Test Initial");
        clubTest.setEmail("initial@club.com");
        clubTest.setCodeClub("CLUB-0001"); // Format respectant les contraintes (ex: 9 chars max)
        clubTest.setActif(true);
        clubTest.setDate_creation((LocalDate.of(2024, 1, 15)));
        clubTest.setDate_inscription(oneYearAgoInstant);
        clubTest.setNumero_voie("1");
        clubTest.setRue("Rue Initiale");
        clubTest.setCodepostal("75000");
        clubTest.setVille("InitialVille");
        clubTest.setTelephone("0100000000");
        clubTest.setEvenements(new ArrayList<>()); // Important pour éviter NullPointerException

        // Initialisation d'un Membre admin de test de base
        adminTest = new Membre();
        adminTest.setId(10);
        adminTest.setEmail("admin.test@example.com");
        adminTest.setNom("AdminNom");
        adminTest.setPrenom("AdminPrenom");
        adminTest.setRole(Role.ADMIN);
        adminTest.setActif(true);

        LocalDate dateNaissanceApprox = LocalDate.now().minusYears(30);
        
        // Initialisation d'un DTO pour la création de club, utilisant les setters
        CreateClubRequestDto.AdminInfo adminInfo = new CreateClubRequestDto.AdminInfo();
        adminInfo.setNom("AdminNomDto");
        adminInfo.setPrenom("AdminPrenomDto");
        adminInfo.setDate_naissance(LocalDate.of(2000, 1, 15));
        adminInfo.setEmail("admin.dto@example.com");
        adminInfo.setTelephone("0200000000");
        adminInfo.setPassword("AdminPassDto1!"); // Mot de passe conforme aux règles de complexité

        createClubDto = new CreateClubRequestDto();
        createClubDto.setNom("Nouveau Club DTO");
        createClubDto.setDate_creation(LocalDate.of(2020, 1, 15));
        createClubDto.setNumero_voie("123");
        createClubDto.setRue("Rue DTO");
        createClubDto.setCodepostal("75002");
        createClubDto.setVille("DtoVille");
        createClubDto.setTelephone("0300000000");
        createClubDto.setEmail("nouveau.club.dto@example.com");
        createClubDto.setAdmin(adminInfo);

        // Initialisation d'un DTO pour la mise à jour de club
        updateClubDto = new UpdateClubDto();
        updateClubDto.setNom("Club Mis à Jour");
        updateClubDto.setEmail("updated.club@example.com");
        // Remplir tous les champs requis par UpdateClubDto si la validation les exige.
        // Exemple:
        updateClubDto.setNumero_voie("1Bis");
        updateClubDto.setRue("Rue Modifiée");
        updateClubDto.setCodepostal("75003");
        updateClubDto.setVille("Ville Modifiée");
        updateClubDto.setTelephone("0400000000");
    }

    // --- Tests pour la méthode getClubByIdOrThrow ---

    /**
     * Teste que {@link ClubService#getClubByIdOrThrow(Integer)} retourne le club
     * lorsque celui-ci existe dans la base de données et est actif (si @Where est appliqué par le DAO).
     */
    @Test
    @DisplayName("getClubByIdOrThrow - Récupération d'un club existant et actif")
    void getClubByIdOrThrow_quandClubExisteEtActif_devraitRetournerClub() {
        // Given: Le clubRepository retournera clubTest lorsque findById(1) est appelé.
        when(clubRepository.findById(1)).thenReturn(Optional.of(clubTest));

        // When: La méthode du service est appelée.
        Club found = clubService.getClubByIdOrThrow(1);

        // Then: Le club retourné doit être celui attendu.
        assertNotNull(found, "Le club trouvé ne devrait pas être nul.");
        assertEquals(clubTest.getId(), found.getId(), "L'ID du club trouvé ne correspond pas.");
    }

    /**
     * Teste que {@link ClubService#getClubByIdOrThrow(Integer)} lève une {@link EntityNotFoundException}
     * lorsque le club avec l'ID spécifié n'existe pas.
     */
    @Test
    @DisplayName("getClubByIdOrThrow - Tentative de récupération d'un club inexistant")
    void getClubByIdOrThrow_quandClubInexistant_devraitLeverEntityNotFoundException() {
        // Given: Le clubRepository retournera un Optional vide pour l'ID 99.
        when(clubRepository.findById(99)).thenReturn(Optional.empty());

        // When & Then: S'attend à ce qu'une EntityNotFoundException soit levée.
        assertThrows(EntityNotFoundException.class, () -> clubService.getClubByIdOrThrow(99),
                "Une EntityNotFoundException était attendue pour un ID de club inexistant.");
    }

    // --- Tests pour la méthode getClubByIdWithSecurityCheck ---

    /**
     * Teste que {@link ClubService#getClubByIdWithSecurityCheck(Integer)} retourne le club
     * lorsque l'utilisateur courant est membre du club demandé (la vérification de sécurité passe).
     */
    @Test
    @DisplayName("getClubByIdWithSecurityCheck - Accès autorisé car l'utilisateur est membre du club")
    void getClubByIdWithSecurityCheck_quandUtilisateurEstMembre_devraitRetournerClub() {
        // Given: La vérification de sécurité par securityService ne lèvera pas d'exception.
        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(1);
        // Et le club est trouvé par le repository.
        when(clubRepository.findById(1)).thenReturn(Optional.of(clubTest));

        // When: La méthode du service est appelée.
        Club found = clubService.getClubByIdWithSecurityCheck(1);

        // Then: Le club est retourné et la vérification de sécurité a été appelée.
        assertNotNull(found);
        assertEquals(clubTest.getId(), found.getId());
        verify(securityService).checkIsCurrentUserMemberOfClubOrThrow(1); // Vérifie que la méthode de sécurité a été invoquée.
    }

    /**
     * Teste que {@link ClubService#getClubByIdWithSecurityCheck(Integer)} lève une {@link AccessDeniedException}
     * lorsque l'utilisateur courant n'est pas membre du club demandé.
     */
    @Test
    @DisplayName("getClubByIdWithSecurityCheck - Accès refusé car l'utilisateur n'est pas membre du club")
    void getClubByIdWithSecurityCheck_quandUtilisateurNonMembre_devraitLeverAccessDeniedException() {
        // Given: La vérification de sécurité par securityService lèvera une AccessDeniedException.
        doThrow(new AccessDeniedException("Non membre")).when(securityService).checkIsCurrentUserMemberOfClubOrThrow(1);

        // When & Then: S'attend à ce qu'une AccessDeniedException soit levée.
        assertThrows(AccessDeniedException.class, () -> clubService.getClubByIdWithSecurityCheck(1),
                "Une AccessDeniedException était attendue car l'utilisateur n'est pas membre.");
        verify(securityService).checkIsCurrentUserMemberOfClubOrThrow(1);
        // Vérifie que findById n'est pas appelé si la sécurité échoue en premier (selon la logique du service).
        verify(clubRepository, never()).findById(anyInt());
    }


    // --- Tests pour la méthode createClubAndRegisterAdmin ---

    /**
     * Teste la création réussie d'un club et de son administrateur initial
     * lorsque toutes les données d'entrée sont valides et uniques.
     */
    @Test
    @DisplayName("createClubAndRegisterAdmin - Création réussie avec données valides")
    void creerClubEtAdmin_quandDonneesValides_devraitCreerClubAdminEtAdhesion() {
        // ... (mocks pour existsByEmail, findByEmail, passwordEncoder) ...

        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> {
            Membre membreSauvegarde = invocation.getArgument(0);
            membreSauvegarde.setId(20);
            membreSauvegarde.setCodeAmi("AMIS-DTOADM");
            return membreSauvegarde;
        });

        // Le mock pour clubRepository.save() est appelé UNE SEULE FOIS.
        // @PostPersist est une logique Hibernate, pas directement testable via le mock de save()
        // de cette manière. On se fie au fait qu'Hibernate l'appellera.
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> {
            Club clubSauvegarde = invocation.getArgument(0);
            clubSauvegarde.setId(200); // Simuler l'ID généré
            // Simuler l'effet de @PostPersist pour la vérification dans le test
            // Si on ne fait pas ça, createdClub.getCodeClub() sera null dans le test
            // car le mock ne sait rien de @PostPersist.
            clubSauvegarde.setCodeClub(String.format("CLUB-%04d", clubSauvegarde.getId()));
            return clubSauvegarde;
        });

        Club createdClub = clubService.createClubAndRegisterAdmin(createClubDto);

        assertNotNull(createdClub);
        assertNotNull(createdClub.getId());
        // La vérification du codeClub dépendra de la simulation dans le mock ci-dessus.
        assertEquals(String.format("CLUB-%04d", 200), createdClub.getCodeClub());
        assertEquals(createClubDto.getNom(), createdClub.getNom());

        verify(membreRepository).save(any(Membre.class));
        verify(clubRepository, times(1)).save(any(Club.class)); // <<<--- CORRECTION: times(1)
        verify(adhesionRepository).save(any(Adhesion.class));
    }

    /**
     * Teste que la création du club échoue avec une {@link IllegalArgumentException}
     * si l'email fourni pour l'administrateur est déjà utilisé.
     */
    @Test
    @DisplayName("createClubAndRegisterAdmin - Échec si email de l'admin déjà utilisé")
    void creerClubEtAdmin_quandEmailAdminExistant_devraitLeverIllegalArgumentException() {
        // Given: Simule que l'email de l'admin existe déjà.
        when(membreRepository.existsByEmail(createClubDto.getAdmin().getEmail().toLowerCase().trim())).thenReturn(true);

        // When & Then: S'attend à une IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> clubService.createClubAndRegisterAdmin(createClubDto),
                "Une IllegalArgumentException était attendue car l'email de l'admin est déjà utilisé.");
        // Vérifie que le club n'a pas été sauvegardé si la validation de l'admin échoue en premier.
        verify(clubRepository, never()).save(any(Club.class));
    }

    /**
     * Teste que la création du club échoue avec une {@link IllegalArgumentException}
     * si l'email fourni pour le club est déjà utilisé.
     */
    @Test
    @DisplayName("createClubAndRegisterAdmin - Échec si email du club déjà utilisé")
    void creerClubEtAdmin_quandEmailClubExistant_devraitLeverIllegalArgumentException() {
        // Given: L'email de l'admin est unique.
        when(membreRepository.existsByEmail(createClubDto.getAdmin().getEmail().toLowerCase().trim())).thenReturn(false);
        // Mais l'email du club est déjà pris.
        when(clubRepository.findByEmail(createClubDto.getEmail().toLowerCase().trim())).thenReturn(Optional.of(new Club())); // Simule l'existence.

        // When & Then: S'attend à une IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> clubService.createClubAndRegisterAdmin(createClubDto),
                "Une IllegalArgumentException était attendue car l'email du club est déjà utilisé.");
        // Selon la logique du service, l'admin peut ou ne peut pas être sauvegardé avant cet échec.
        // La correction précédente assure que l'admin n'est PAS sauvegardé si l'email du club existe déjà.
        verify(membreRepository, never()).save(any(Membre.class));
        verify(clubRepository, never()).save(any(Club.class));
    }


    // --- Tests pour la méthode updateClub ---

    /**
     * Teste la mise à jour réussie d'un club par son administrateur avec des données valides.
     */
    @Test
    @DisplayName("updateClub - Mise à jour réussie par l'admin du club")
    void mettreAJourClub_quandAdminDuClubEtDonneesValides_devraitMettreAJourClub() {
        // Given: La vérification de sécurité (l'utilisateur est l'admin du club) passe.
        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        // Le club à mettre à jour est trouvé.
        when(clubRepository.findById(clubTest.getId())).thenReturn(Optional.of(clubTest));
        // Le nouvel email (s'il change) n'est pas pris par un autre club.
        when(clubRepository.existsByEmailAndIdNot(updateClubDto.getEmail().toLowerCase().trim(), clubTest.getId())).thenReturn(false);
        // Simule la sauvegarde du club mis à jour.
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: La méthode de mise à jour du service est appelée.
        Club updated = clubService.updateClub(clubTest.getId(), updateClubDto);

        // Then: Vérifie que le club retourné contient les modifications.
        assertNotNull(updated, "Le club mis à jour ne devrait pas être nul.");
        assertEquals(updateClubDto.getNom(), updated.getNom(), "Le nom du club ne correspond pas à la mise à jour.");
        assertEquals(updateClubDto.getEmail().toLowerCase().trim(), updated.getEmail(), "L'email du club ne correspond pas à la mise à jour.");
        verify(clubRepository).save(clubTest); // Vérifie que la sauvegarde a été appelée sur l'objet clubTest modifié.
    }

    /**
     * Teste que la mise à jour du club est refusée si l'utilisateur authentifié n'est pas
     * l'administrateur du club concerné.
     */
    @Test
    @DisplayName("updateClub - Tentative de mise à jour par un non-administrateur du club")
    void mettreAJourClub_quandNonAdminDuClub_devraitLeverAccessDeniedException() {
        // Given: La vérification de sécurité lève une AccessDeniedException.
        doThrow(new AccessDeniedException("Pas l'admin du club")).when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());

        // When & Then: S'attend à une AccessDeniedException.
        assertThrows(AccessDeniedException.class, () -> clubService.updateClub(clubTest.getId(), updateClubDto),
                "Une AccessDeniedException était attendue car l'utilisateur n'est pas l'admin du club.");
        verify(clubRepository, never()).save(any(Club.class)); // La sauvegarde ne doit pas avoir lieu.
    }

    // Ajouter des tests pour updateClub:
    // - quandClubInexistant_devraitLeverEntityNotFoundException (similaire à getClubByIdOrThrow)
    // - quandEmailPrisParAutreClub_devraitLeverIllegalArgumentException (similaire à createClub)


    // --- Tests pour la méthode deactivateClub ---

    /**
     * Teste la désactivation réussie d'un club par son administrateur,
     * en supposant qu'il n'y a pas d'événements futurs actifs.
     */
    @Test
    @DisplayName("deactivateClub - Succès par admin (pas d'événements futurs actifs)")
    void desactiverClub_parAdminSansEvenementsFutursActifs_devraitReussir() {
        // Given: Sécurité OK, club trouvé, pas d'événements futurs actifs.
        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        when(clubRepository.findById(clubTest.getId())).thenReturn(Optional.of(clubTest));
        // clubTest.getEvenements() est initialisé à une liste vide dans setUp(), donc pas besoin de mocker EventDao pour ce cas.
        when(clubRepository.save(any(Club.class))).thenReturn(clubTest); // Mocker la sauvegarde du club désactivé.

        // When: La méthode de désactivation est appelée.
        assertDoesNotThrow(() -> clubService.deactivateClub(clubTest.getId()),
                "La désactivation ne devrait pas lever d'exception.");

        // Then: Vérifie l'état du club après désactivation.
        assertFalse(clubTest.getActif(), "Le club devrait être marqué comme inactif.");
        assertNotNull(clubTest.getDesactivationDate(), "La date de désactivation devrait être définie.");
        assertTrue(clubTest.getNom().startsWith("[Désactivé]"), "Le nom du club devrait être préfixé.");
        verify(clubRepository).save(clubTest); // Vérifie la sauvegarde.
    }

    /**
     * Teste que la désactivation d'un club échoue avec une {@link IllegalStateException}
     * si le club a des événements futurs actifs.
     */
    @Test
    @DisplayName("deactivateClub - Échec si le club a des événements futurs actifs")
    void desactiverClub_quandEvenementsFutursActifs_devraitLeverIllegalStateException() {
        // Given: Sécurité OK, club trouvé.
        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        when(clubRepository.findById(clubTest.getId())).thenReturn(Optional.of(clubTest));
        LocalDateTime tomorrowLocalDateTime = LocalDateTime.now().plusDays(1);

// 2. Convertir ce LocalDateTime (qui est un moment UTC) en un Instant
        Instant tomorrowInstant = tomorrowLocalDateTime.toInstant(ZoneOffset.UTC);
        // Simuler un événement futur actif pour le club.
        Event futureEvent = new Event();
        futureEvent.setId(500);
        futureEvent.setNom("Événement Futur Important");
        futureEvent.setActif(true);
        futureEvent.setStartTime(tomorrowInstant);
        clubTest.getEvenements().add(futureEvent); // Ajoute à la liste (mockée ou réelle) du club.

        // When & Then: S'attend à une IllegalStateException.
        assertThrows(IllegalStateException.class, () -> clubService.deactivateClub(clubTest.getId()),
                "Une IllegalStateException était attendue car le club a des événements futurs actifs.");
        verify(clubRepository, never()).save(any(Club.class)); // Pas de sauvegarde si échec.
    }

    // --- Tests pour la méthode findMembresForClub ---

    /**
     * Teste la récupération des membres d'un club lorsque l'utilisateur authentifié
     * est membre de ce club.
     */
    @Test
    @DisplayName("findMembresForClub - Récupération réussie par un membre du club")
    void trouverMembresPourClub_quandUtilisateurMembre_devraitRetournerListeDesMembres() {
        // Given: Sécurité OK, le club existe.
        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(clubTest.getId());
        when(clubRepository.existsById(clubTest.getId())).thenReturn(true);

        // Simuler les membres retournés par le DAO pour ce club.
        Membre membreA = new Membre();
        membreA.setId(30);
        membreA.setNom("MembreA");
        Membre membreB = new Membre();
        membreB.setId(31);
        membreB.setNom("MembreB");
        when(membreRepository.findByAdhesionsClubId(clubTest.getId())).thenReturn(Arrays.asList(membreA, membreB));

        // When: La méthode du service est appelée.
        Set<Membre> membres = clubService.findMembresForClub(clubTest.getId());

        // Then: Vérifie que la liste des membres est correcte.
        assertNotNull(membres, "L'ensemble des membres ne devrait pas être nul.");
        assertEquals(2, membres.size(), "Le nombre de membres retournés ne correspond pas.");
        // Vérifier que les membres attendus sont présents (plus robuste que l'ordre).
        assertTrue(membres.stream().anyMatch(m -> m.getId().equals(30) && m.getNom().equals("MembreA")));
        assertTrue(membres.stream().anyMatch(m -> m.getId().equals(31) && m.getNom().equals("MembreB")));

        verify(securityService).checkIsCurrentUserMemberOfClubOrThrow(clubTest.getId());
        verify(membreRepository).findByAdhesionsClubId(clubTest.getId());
    }

    /**
     * Teste que {@link ClubService#findMembresForClub(Integer)} lève une {@link EntityNotFoundException}
     * si le club n'existe pas (après que la vérification de sécurité initiale soit passée, par exemple pour un admin).
     */
    @Test
    @DisplayName("findMembresForClub - Échec si le club est introuvable après vérification de sécurité")
    void trouverMembresPourClub_quandClubInexistantApresSecurite_devraitLeverEntityNotFound() {
        // Given: Sécurité OK (ex: admin global), mais le club n'existe pas.
        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(99);
        when(clubRepository.existsById(99)).thenReturn(false); // Simule que le club n'existe pas.

        // When & Then: S'attend à une EntityNotFoundException.
        assertThrows(EntityNotFoundException.class, () -> clubService.findMembresForClub(99),
                "Une EntityNotFoundException était attendue car le club n'existe pas.");
    }
}

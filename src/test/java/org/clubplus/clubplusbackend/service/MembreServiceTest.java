package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour {@link MembreService}.
 * Utilise Mockito pour mocker les dépendances (DAOs, autres services) afin de tester
 * la logique métier de MembreService en isolation.
 * {@code @ExtendWith(MockitoExtension.class)} active l'intégration de Mockito avec JUnit 5.
 */
@ExtendWith(MockitoExtension.class)
class MembreServiceTest {

    // Mocks pour les dépendances de MembreService.
    @Mock
    private MembreDao membreRepository;
    @Mock
    private ClubDao clubRepository;
    @Mock
    private AdhesionDao adhesionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService; // Supposons que ce service existe et est utilisé.
    @Mock
    private SecurityService securityService;

    // Injecte les mocks ci-dessus dans une instance réelle de MembreService.
    @InjectMocks
    private MembreService membreService;

    // Objets de test de base, réinitialisés avant chaque test.
    private Membre membreTest;
    private Club clubTest;

    /**
     * Méthode de configuration exécutée avant chaque test.
     * Initialise les objets de test {@code membreTest} et {@code clubTest}.
     */
    @BeforeEach
    void setUp() {
        membreTest = new Membre();
        membreTest.setId(1);
        membreTest.setEmail("test@example.com");
        membreTest.setNom("TestNom");
        membreTest.setPrenom("TestPrenom");
        membreTest.setPassword("MotDePasseHache"); // Le contenu exact importe peu ici, car PasswordEncoder est mocké.
        membreTest.setRole(Role.MEMBRE);
        membreTest.setActif(true);
        membreTest.setDate_naissance(LocalDate.of(2005, 1, 15));
        membreTest.setAdhesions(new HashSet<>());

        clubTest = new Club();
        clubTest.setId(100);
        clubTest.setCodeClub("CLUB100");
        clubTest.setActif(true);
        // Initialiser d'autres champs de clubTest si nécessaire pour les tests.
    }

    // --- Tests pour getMembreByIdOrThrow ---

    /**
     * Teste que {@code getMembreByIdOrThrow} retourne le membre si celui-ci existe.
     */
    @Test
    @DisplayName("getMembreByIdOrThrow - Membre existant")
    void getMembreByIdOrThrow_quandMembreExiste_devraitRetournerMembre() {
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        Membre found = membreService.getMembreByIdOrThrow(1);
        assertNotNull(found, "Le membre trouvé ne devrait pas être nul.");
        assertEquals(1, found.getId(), "L'ID du membre trouvé ne correspond pas.");
    }

    /**
     * Teste que {@code getMembreByIdOrThrow} lève EntityNotFoundException si le membre n'existe pas.
     */
    @Test
    @DisplayName("getMembreByIdOrThrow - Membre inexistant")
    void getMembreByIdOrThrow_quandMembreInexistant_devraitLeverEntityNotFoundException() {
        when(membreRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> {
            membreService.getMembreByIdOrThrow(1);
        }, "Une EntityNotFoundException était attendue.");
    }

    // --- Tests pour getMembreByIdWithSecurityCheck ---

    /**
     * Teste l'accès à son propre profil via {@code getMembreByIdWithSecurityCheck}.
     */
    @Test
    @DisplayName("getMembreByIdWithSecurityCheck - Accès à son propre profil")
    void getMembreByIdWithSecurityCheck_quandUtilisateurCourantEstCible_devraitRetournerMembre() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        // Important: la méthode du service va chercher l'utilisateur courant et l'utilisateur cible
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest)); // Retourne membreTest pour ID 1 (courant ET cible)

        Membre found = membreService.getMembreByIdWithSecurityCheck(1);
        assertNotNull(found);
        assertEquals(1, found.getId());
    }

    /**
     * Teste l'accès au profil d'un autre membre s'ils partagent un club actif.
     * Note: Cette logique dépend de l'implémentation exacte de findActiveClubIdsForMember.
     * Pour un test unitaire plus fin, on mockerait findActiveClubIdsForMember,
     * mais ici on mocke l'appel à adhesionRepository qu'il utiliserait.
     */
    @Test
    @DisplayName("getMembreByIdWithSecurityCheck - Partage d'un club actif")
    void getMembreByIdWithSecurityCheck_quandPartageClubActif_devraitRetournerMembre() {
        Membre targetUser = new Membre(); // Membre cible différent de membreTest.
        targetUser.setId(2);
        targetUser.setRole(Role.MEMBRE); // Nécessaire si la logique de service vérifie le rôle
        targetUser.setActif(true);

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1); // Utilisateur courant est membreTest (ID 1).
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest)); // Mocker la récupération de l'utilisateur courant.
        when(membreRepository.findById(2)).thenReturn(Optional.of(targetUser)); // Mocker la récupération de l'utilisateur cible.

        // Simuler que membreTest et targetUser partagent un club actif (ID de club 100).
        // Note: si MembreService appelle une méthode comme `findActiveClubIdsForMember` en interne,
        // il faudrait mocker cette méthode si elle était publique ou tester ses composants.
        // Ici, nous supposons que la méthode se base sur adhesionRepository.findActiveClubIdsByMembreId.
        when(membreService.findActiveClubIdsForMember(1)).thenReturn(Collections.singletonList(100));
        when(membreService.findActiveClubIdsForMember(2)).thenReturn(Collections.singletonList(100));


        Membre found = membreService.getMembreByIdWithSecurityCheck(2);
        assertNotNull(found);
        assertEquals(2, found.getId());
    }

    /**
     * Teste que l'accès est refusé si l'utilisateur n'est pas le propriétaire
     * et ne partage pas de club actif avec la cible.
     */
    @Test
    @DisplayName("getMembreByIdWithSecurityCheck - Pas propriétaire et pas de club commun")
    void getMembreByIdWithSecurityCheck_quandNonProprietaireEtPasDeClubCommun_devraitLeverAccessDenied() {
        Membre targetUser = new Membre();
        targetUser.setId(2);
        targetUser.setRole(Role.MEMBRE);
        targetUser.setActif(true);

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        when(membreRepository.findById(2)).thenReturn(Optional.of(targetUser));

        // Simuler qu'ils NE partagent PAS de club actif.
        when(membreService.findActiveClubIdsForMember(1)).thenReturn(Collections.singletonList(100)); // Club de l'utilisateur courant
        when(membreService.findActiveClubIdsForMember(2)).thenReturn(Collections.singletonList(200)); // Club différent pour la cible

        assertThrows(AccessDeniedException.class, () -> {
            membreService.getMembreByIdWithSecurityCheck(2);
        });
    }

    /**
     * Teste que EntityNotFoundException est levée si l'utilisateur cible n'existe pas,
     * même si l'utilisateur courant existe.
     */
    @Test
    @DisplayName("getMembreByIdWithSecurityCheck - Cible non trouvée")
    void getMembreByIdWithSecurityCheck_quandCibleNonTrouvee_devraitLeverEntityNotFound() {
        Integer currentUserId = 1;
        Membre mockCurrentUser = new Membre();
        mockCurrentUser.setId(currentUserId);
        mockCurrentUser.setRole(Role.MEMBRE); // Ou ADMIN si pertinent pour le chemin de code.

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(membreRepository.findById(currentUserId)).thenReturn(Optional.of(mockCurrentUser)); // Utilisateur courant trouvé.

        Integer targetUserId = 2;
        when(membreRepository.findById(targetUserId)).thenReturn(Optional.empty()); // Utilisateur cible NON trouvé.

        assertThrows(EntityNotFoundException.class, () -> {
            membreService.getMembreByIdWithSecurityCheck(targetUserId);
        });

        verify(securityService).getCurrentUserIdOrThrow();
        verify(membreRepository).findById(currentUserId);
        verify(membreRepository).findById(targetUserId);
    }

    /**
     * Teste que si l'utilisateur courant est ADMIN, il peut voir le profil d'un autre membre
     * même sans être propriétaire ou partager un club (selon la logique service modifiée).
     */
    @Test
    @DisplayName("getMembreByIdWithSecurityCheck - En tant qu'ADMIN voyant un autre profil")
    void getMembreByIdWithSecurityCheck_enTantQuAdminVoyantAutreProfil_devraitRetournerMembre() {
        Membre adminUser = new Membre(); // L'utilisateur courant est un admin.
        adminUser.setId(1);
        adminUser.setRole(Role.ADMIN);

        Membre targetUser = new Membre(); // L'utilisateur cible.
        targetUser.setId(2);
        targetUser.setRole(Role.MEMBRE);
        targetUser.setActif(true);

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(adminUser.getId());
        when(membreRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser)); // Récupération de l'admin.
        when(membreRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser)); // Récupération de la cible.

        // La logique de club commun ne devrait pas être invoquée si l'admin a un passe-droit.
        // Si la méthode findActiveClubIdsForMember est publique et appelée, il faudrait la mocker aussi.
        // Ici, on suppose que le rôle ADMIN bypass cette vérification.

        Membre found = membreService.getMembreByIdWithSecurityCheck(targetUser.getId());
        assertNotNull(found);
        assertEquals(targetUser.getId(), found.getId());
    }


    // --- Tests pour updateMyProfile ---

    /**
     * Teste la mise à jour du profil avec un DTO valide.
     * Vérifie que les champs sont mis à jour et que la méthode save du repository est appelée.
     */
    @Test
    @DisplayName("updateMyProfile - Données valides")
    void mettreAJourProfil_quandDtoValide_devraitMettreAJourEtSauvegarderMembre() {
        UpdateMembreDto dto = new UpdateMembreDto();
        dto.setNom("NouveauNom");
        dto.setPrenom("NouveauPrenom");
        dto.setEmail("nouveau@example.com");
        dto.setTelephone("0987654321");
        dto.setDate_naissance(LocalDate.of(1995, 5, 5));

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membreTest.getId());
        when(membreRepository.findById(membreTest.getId())).thenReturn(Optional.of(membreTest));
        // Supposer que le nouvel email n'est pas déjà pris par un autre utilisateur.
        when(membreRepository.existsByEmailAndIdNot(dto.getEmail(), membreTest.getId())).thenReturn(false);
        // Mocker save pour retourner l'argument qui lui est passé (simule la sauvegarde).
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Membre updatedMembre = membreService.updateMyProfile(dto);

        assertEquals("NouveauNom", updatedMembre.getNom(), "Le nom devrait être mis à jour.");
        assertEquals("nouveau@example.com", updatedMembre.getEmail(), "L'email devrait être mis à jour.");
        verify(membreRepository, times(1)).save(membreTest); // Vérifie que save a été appelé une fois.
    }

    /**
     * Teste la mise à jour du profil lorsque l'email souhaité existe déjà pour un autre utilisateur.
     * S'attend à une IllegalArgumentException.
     */
    @Test
    @DisplayName("updateMyProfile - Email déjà utilisé par un autre")
    void mettreAJourProfil_quandEmailExistePourAutreUtilisateur_devraitLeverIllegalArgumentException() {
        UpdateMembreDto dto = new UpdateMembreDto();
        dto.setEmail("existant@example.com"); // Email qui serait déjà pris.
        // Les autres champs du DTO doivent être initialisés si la logique du service les lit avant la vérification de l'email.
        dto.setNom(membreTest.getNom()); // Garder le nom actuel pour cet exemple
        dto.setPrenom(membreTest.getPrenom());
        dto.setTelephone(membreTest.getTelephone());
        dto.setDate_naissance(membreTest.getDate_naissance());


        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membreTest.getId());
        when(membreRepository.findById(membreTest.getId())).thenReturn(Optional.of(membreTest));
        // Simuler que l'email "existant@example.com" est déjà utilisé par un autre ID.
        when(membreRepository.existsByEmailAndIdNot("existant@example.com", membreTest.getId())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            membreService.updateMyProfile(dto);
        });
        verify(membreRepository, never()).save(any(Membre.class)); // save ne doit pas être appelé.
    }

    // --- Tests pour deleteMyAccount ---

    /**
     * Teste la suppression (anonymisation) du compte d'un membre standard.
     * Vérifie que le membre devient inactif et que ses informations sont anonymisées.
     */
    @Test
    @DisplayName("deleteMyAccount - Utilisateur MEMBRE")
    void supprimerMonCompte_quandUtilisateurEstMembre_devraitAnonymiserEtRendreInactif() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest)); // membreTest a le rôle MEMBRE.
        // Mocker save pour qu'il n'y ait pas d'erreur et qu'on puisse vérifier l'état de membreTest.
        when(membreRepository.save(any(Membre.class))).thenReturn(membreTest);


        membreService.deleteMyAccount();

        assertFalse(membreTest.getActif(), "Le membre devrait être inactif.");
        // Vérifier que des champs sensibles ont été anonymisés (ex: l'email).
        // La logique exacte d'anonymisation dépend de votre implémentation dans MembreService.
        assertTrue(membreTest.getEmail().startsWith("anonymized_") || membreTest.getEmail().contains("@deleted.user"),
                "L'email devrait être anonymisé.");
        verify(membreRepository, times(1)).save(membreTest);
    }

    /**
     * Teste que la suppression du compte est refusée si l'utilisateur est un ADMIN
     * et gère au moins un club (a des adhésions).
     */
    @Test
    @DisplayName("deleteMyAccount - ADMIN gérant un club")
    void supprimerMonCompte_quandUtilisateurEstAdminEtGereClub_devraitLeverIllegalStateException() {
        membreTest.setRole(Role.ADMIN); // Changer le rôle de membreTest en ADMIN.
        // Simuler que cet admin est lié à un club via une adhésion.
        Adhesion adhesion = new Adhesion(membreTest, clubTest); // clubTest est initialisé dans setUp.
        membreTest.getAdhesions().add(adhesion);

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        // Supposons que la vérification se fait en comptant les adhésions de l'admin.
        // Vous pourriez avoir besoin de mocker `adhesionRepository.countByMembreIdAndClubActif(...)`
        // ou une logique similaire dans votre service si elle est utilisée pour déterminer s'il gère un club.
        // Pour cet exemple, on se base sur la présence d'adhésions.

        assertThrows(IllegalStateException.class, () -> {
            membreService.deleteMyAccount();
        });
        verify(membreRepository, never()).save(any(Membre.class)); // save ne doit pas être appelé.
    }

}

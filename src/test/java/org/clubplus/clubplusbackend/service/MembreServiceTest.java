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

@ExtendWith(MockitoExtension.class) // Permet d'utiliser les annotations Mockito
class MembreServiceTest {

    @Mock
    private MembreDao membreRepository;
    @Mock
    private ClubDao clubRepository;
    @Mock
    private AdhesionDao adhesionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private SecurityService securityService;

    @InjectMocks // Crée une instance de MembreService et injecte les mocks déclarés ci-dessus
    private MembreService membreService;

    private Membre membreTest;
    private Club clubTest;

    @BeforeEach
    void setUp() {
        membreTest = new Membre();
        membreTest.setId(1);
        membreTest.setEmail("test@example.com");
        membreTest.setNom("TestNom");
        membreTest.setPrenom("TestPrenom");
        membreTest.setPassword("hashedPassword");
        membreTest.setRole(Role.MEMBRE);
        membreTest.setActif(true);
        membreTest.setDate_naissance(LocalDate.now().minusYears(20));
        membreTest.setAdhesions(new HashSet<>()); // Important pour éviter NullPointerException

        clubTest = new Club();
        clubTest.setId(100);
        clubTest.setCodeClub("CLUB100");
        clubTest.setActif(true);
    }

    // --- Tests pour getMembreByIdOrThrow ---
    @Test
    void getMembreByIdOrThrow_whenMembreExists_thenReturnMembre() {
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        Membre found = membreService.getMembreByIdOrThrow(1);
        assertNotNull(found);
        assertEquals(1, found.getId());
    }

    @Test
    void getMembreByIdOrThrow_whenMembreNotExists_thenThrowEntityNotFoundException() {
        when(membreRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> membreService.getMembreByIdOrThrow(1));
    }

    // --- Tests pour getMembreByIdWithSecurityCheck ---
    @Test
    void getMembreByIdWithSecurityCheck_whenCurrentUserIsTarget_thenReturnMembre() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));

        Membre found = membreService.getMembreByIdWithSecurityCheck(1);
        assertNotNull(found);
        assertEquals(1, found.getId());
    }

    @Test
    void getMembreByIdWithSecurityCheck_whenShareActiveClub_thenReturnMembre() {
        Membre targetUser = new Membre();
        targetUser.setId(2);
        targetUser.setActif(true);

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest)); // Current user
        when(membreRepository.findById(2)).thenReturn(Optional.of(targetUser)); // Target user

        // Simuler qu'ils partagent un club actif
        when(adhesionRepository.findActiveClubIdsByMembreId(1)).thenReturn(Collections.singletonList(100));
        when(adhesionRepository.findActiveClubIdsByMembreId(2)).thenReturn(Collections.singletonList(100));

        Membre found = membreService.getMembreByIdWithSecurityCheck(2);
        assertNotNull(found);
        assertEquals(2, found.getId());
    }

    @Test
    void getMembreByIdWithSecurityCheck_whenNoSharedClubAndNotOwner_thenThrowAccessDenied() {
        Membre targetUser = new Membre();
        targetUser.setId(2);
        targetUser.setActif(true);

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        when(membreRepository.findById(2)).thenReturn(Optional.of(targetUser));

        // Simuler qu'ils NE partagent PAS de club actif
        when(adhesionRepository.findActiveClubIdsByMembreId(1)).thenReturn(Collections.singletonList(100));
        when(adhesionRepository.findActiveClubIdsByMembreId(2)).thenReturn(Collections.singletonList(200)); // Club différent

        assertThrows(AccessDeniedException.class, () -> membreService.getMembreByIdWithSecurityCheck(2));
    }

    @Test
    void getMembreByIdWithSecurityCheck_whenTargetNotFound_thenThrowEntityNotFound() {
        // Configuration pour l'utilisateur courant
        Integer currentUserId = 1;
        Membre mockCurrentUser = new Membre(); // Un utilisateur courant valide simple
        mockCurrentUser.setId(currentUserId);
        mockCurrentUser.setRole(Role.MEMBRE); // ou ADMIN, selon le scénario testé pour cette partie
        // Si on teste seulement "target not found", le rôle de currentUser importe peu
        // tant qu'il n'est pas admin et que la logique de club commun n'interfère pas.
        // Pour simplifier, si vous avez la logique admin qui bypass, mockez un rôle MEMBRE.

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(membreRepository.findById(currentUserId)).thenReturn(Optional.of(mockCurrentUser)); // Mock pour trouver l'utilisateur courant

        // Configuration pour l'utilisateur cible non trouvé
        Integer targetUserId = 2;
        when(membreRepository.findById(targetUserId)).thenReturn(Optional.empty()); // Target user non trouvé

        // Exécution et assertion
        assertThrows(EntityNotFoundException.class, () -> {
            membreService.getMembreByIdWithSecurityCheck(targetUserId);
        });

        // Vérifications optionnelles
        verify(securityService).getCurrentUserIdOrThrow();
        verify(membreRepository).findById(currentUserId); // Vérifier l'appel pour l'utilisateur courant
        verify(membreRepository).findById(targetUserId);   // Vérifier l'appel pour l'utilisateur cible
    }

    // --- Tests pour updateMyProfile ---
    @Test
    void updateMyProfile_whenValidDto_thenUpdateAndSaveMembre() {
        UpdateMembreDto dto = new UpdateMembreDto();
        dto.setNom("NouveauNom");
        dto.setEmail("nouveau@example.com");
        // ... initialiser d'autres champs du DTO

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        when(membreRepository.existsByEmailAndIdNot("nouveau@example.com", 1)).thenReturn(false);
        // save retourne l'entité sauvegardée
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));


        Membre updatedMembre = membreService.updateMyProfile(dto);

        assertEquals("NouveauNom", updatedMembre.getNom());
        assertEquals("nouveau@example.com", updatedMembre.getEmail());
        verify(membreRepository, times(1)).save(membreTest);
    }

    @Test
    void updateMyProfile_whenEmailExistsForAnotherUser_thenThrowIllegalArgumentException() {
        UpdateMembreDto dto = new UpdateMembreDto();
        dto.setEmail("existant@example.com");

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        when(membreRepository.existsByEmailAndIdNot("existant@example.com", 1)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> membreService.updateMyProfile(dto));
        verify(membreRepository, never()).save(any(Membre.class));
    }

    @Test
    void updateMyProfile_whenNoChangesInDto_thenDoNotSave() {
        UpdateMembreDto dto = new UpdateMembreDto();
        // DTO avec les mêmes valeurs que membreTest ou des champs null/blancs
        dto.setNom(membreTest.getNom());
        dto.setEmail(membreTest.getEmail());
        // etc.

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        // S'assurer que existsByEmailAndIdNot n'est pas appelé si l'email ne change pas
        // ou qu'il retourne false s'il est appelé avec le même email normalisé

        Membre resultMembre = membreService.updateMyProfile(dto);

        assertSame(membreTest, resultMembre); // Devrait retourner l'instance existante
        verify(membreRepository, never()).save(any(Membre.class));
    }

    // --- Tests pour deleteMyAccount ---
    @Test
    void deleteMyAccount_whenUserIsMembre_thenAnonymizeAndSetInactive() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest)); // Role MEMBRE par défaut

        membreService.deleteMyAccount();

        assertFalse(membreTest.getActif());
        // Vérifier que des champs ont été anonymisés, par ex. l'email
        assertTrue(membreTest.getEmail().startsWith("anonymized_"));
        verify(membreRepository, times(1)).save(membreTest);
    }

    @Test
    void deleteMyAccount_whenUserIsAdminOfClub_thenThrowIllegalStateException() {
        membreTest.setRole(Role.ADMIN);
        // Simuler une adhésion (signifiant qu'il gère un club)
        Adhesion adhesion = new Adhesion(membreTest, clubTest);
        membreTest.getAdhesions().add(adhesion);


        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));

        assertThrows(IllegalStateException.class, () -> membreService.deleteMyAccount());
        verify(membreRepository, never()).save(any(Membre.class));
    }

    @Test
    void deleteMyAccount_whenUserIsAdminNotOfClub_thenAnonymizeAndSetInactive() {
        membreTest.setRole(Role.ADMIN);
        // Pas d'adhésion = ne gère aucun club
        membreTest.setAdhesions(new HashSet<>());

        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));

        membreService.deleteMyAccount();

        assertFalse(membreTest.getActif());
        assertTrue(membreTest.getEmail().startsWith("anonymized_"));
        verify(membreRepository).save(membreTest);
    }

    // --- Tests pour joinClub ---
    @Test
    void joinClub_whenMembreRoleAndClubExistsAndNotMember_thenCreateAdhesion() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest)); // Role MEMBRE
        when(clubRepository.findByCodeClub("CLUB100")).thenReturn(Optional.of(clubTest));
        when(adhesionRepository.existsByMembreIdAndClubId(1, 100)).thenReturn(false);
        when(adhesionRepository.save(any(Adhesion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Adhesion adhesion = membreService.joinClub("CLUB100");

        assertNotNull(adhesion);
        assertEquals(membreTest, adhesion.getMembre());
        assertEquals(clubTest, adhesion.getClub());
        verify(adhesionRepository, times(1)).save(any(Adhesion.class));
    }

    @Test
    void joinClub_whenAdminRole_thenThrowIllegalStateException() {
        membreTest.setRole(Role.ADMIN);
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        // clubRepository.findByCodeClub n'est pas appelé, donc pas besoin de mocker pour ce test précis

        assertThrows(IllegalStateException.class, () -> membreService.joinClub("CLUB100"));
        verify(adhesionRepository, never()).save(any(Adhesion.class));
    }

    @Test
    void joinClub_whenClubNotFound_thenThrowEntityNotFoundException() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        when(clubRepository.findByCodeClub("UNKNOWN_CLUB")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> membreService.joinClub("UNKNOWN_CLUB"));
    }

    @Test
    void joinClub_whenAlreadyMember_thenThrowIllegalStateException() {
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(1);
        when(membreRepository.findById(1)).thenReturn(Optional.of(membreTest));
        when(clubRepository.findByCodeClub("CLUB100")).thenReturn(Optional.of(clubTest));
        when(adhesionRepository.existsByMembreIdAndClubId(1, 100)).thenReturn(true); // Déjà membre

        assertThrows(IllegalStateException.class, () -> membreService.joinClub("CLUB100"));
    }


    // --- Tests pour registerMembreAndJoinClub ---
    @Test
    void registerMembreAndJoinClub_whenValidData_thenSuccess() throws Exception {
        Membre newMembreData = new Membre();
        newMembreData.setEmail("newuser@example.com");
        newMembreData.setPassword("Password123!");
        // autres champs...

        when(membreRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(clubRepository.findByCodeClub("CLUB100")).thenReturn(Optional.of(clubTest));
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> {
            Membre m = invocation.getArgument(0);
            m.setId(2); // Simuler la génération d'ID
            m.generateCodeAmi(); // Simuler le @PostPersist
            return m;
        });
        when(adhesionRepository.save(any(Adhesion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // doNothing().when(emailService).sendVerificationEmail(any(Membre.class)); // Si sendVerificationEmail ne retourne rien

        Membre registeredMembre = membreService.registerMembreAndJoinClub(newMembreData, "CLUB100");

        assertNotNull(registeredMembre);
        assertEquals("newuser@example.com", registeredMembre.getEmail());
        assertEquals("encodedPassword", registeredMembre.getPassword());
        assertEquals(Role.MEMBRE, registeredMembre.getRole());
        assertFalse(registeredMembre.isVerified());
        assertNotNull(registeredMembre.getVerificationToken());
        assertNotNull(registeredMembre.getCodeAmi()); // Vérifier que le code ami est généré

        verify(emailService, times(1)).sendVerificationEmail(any(Membre.class));
        verify(adhesionRepository, times(1)).save(any(Adhesion.class));
    }

    @Test
    void registerMembreAndJoinClub_whenEmailExists_thenThrowIllegalArgumentException() {
        Membre newMembreData = new Membre();
        newMembreData.setEmail("test@example.com"); // Email de membreTest

        when(membreRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> membreService.registerMembreAndJoinClub(newMembreData, "CLUB100"));
    }

    // --- Tests pour verifyUserValidationToken ---
    @Test
    void verifyUserValidationToken_whenTokenIsValidAndUserNotVerified_thenVerifyAndInvalidateToken() {
        String token = "valid-token";
        membreTest.setVerified(false);
        membreTest.setVerificationToken(token);

        when(membreRepository.findByVerificationToken(token)).thenReturn(Optional.of(membreTest));
        when(membreRepository.save(any(Membre.class))).thenReturn(membreTest);

        boolean result = membreService.verifyUserValidationToken(token);

        assertTrue(result);
        assertTrue(membreTest.isVerified());
        assertNull(membreTest.getVerificationToken());
        verify(membreRepository).save(membreTest);
    }

    @Test
    void verifyUserValidationToken_whenTokenInvalid_thenReturnFalse() {
        when(membreRepository.findByVerificationToken("invalid-token")).thenReturn(Optional.empty());
        boolean result = membreService.verifyUserValidationToken("invalid-token");
        assertFalse(result);
    }

    @Test
    void verifyUserValidationToken_whenUserAlreadyVerified_thenReturnTrueAndDoNotSave() {
        String token = "valid-token-already-verified";
        membreTest.setVerified(true); // Déjà vérifié
        membreTest.setVerificationToken(token); // Token encore présent (pourrait arriver)

        when(membreRepository.findByVerificationToken(token)).thenReturn(Optional.of(membreTest));
        // PAS d'appel à save si déjà vérifié

        boolean result = membreService.verifyUserValidationToken(token);

        assertTrue(result);
        assertTrue(membreTest.isVerified()); // Toujours vérifié
        // Le token pourrait rester ou être null selon la logique, ici on ne mocke pas de save.
        // Si vous voulez que le token soit nullifié même si déjà vérifié, ajoutez cette logique et le save.
        verify(membreRepository, never()).save(any(Membre.class)); // Important
    }

    // --- Tests pour changeMemberRoleInClub ---
    @Test
    void changeMemberRoleInClub_promoteMembreToReservation_whenValid() {
        Membre targetMembre = new Membre();
        targetMembre.setId(2);
        targetMembre.setRole(Role.MEMBRE);
        targetMembre.setActif(true);

        // L'admin appelant a l'ID 1 (membreTest)
        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        when(membreRepository.findById(targetMembre.getId())).thenReturn(Optional.of(targetMembre));
        when(adhesionRepository.existsByMembreIdAndClubId(targetMembre.getId(), clubTest.getId())).thenReturn(true);
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membreTest.getId()); // Admin
        when(adhesionRepository.countByMembreId(targetMembre.getId())).thenReturn(1L); // Appartient à 1 seul club
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Membre updatedMembre = membreService.changeMemberRoleInClub(targetMembre.getId(), clubTest.getId(), Role.RESERVATION);

        assertEquals(Role.RESERVATION, updatedMembre.getRole());
        verify(membreRepository).save(targetMembre);
    }

    @Test
    void changeMemberRoleInClub_demoteReservationToMembre_whenValid() {
        Membre targetMembre = new Membre();
        targetMembre.setId(2);
        targetMembre.setRole(Role.RESERVATION);
        targetMembre.setActif(true);

        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        when(membreRepository.findById(targetMembre.getId())).thenReturn(Optional.of(targetMembre));
        when(adhesionRepository.existsByMembreIdAndClubId(targetMembre.getId(), clubTest.getId())).thenReturn(true);
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membreTest.getId());
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Membre updatedMembre = membreService.changeMemberRoleInClub(targetMembre.getId(), clubTest.getId(), Role.MEMBRE);

        assertEquals(Role.MEMBRE, updatedMembre.getRole());
        verify(membreRepository).save(targetMembre);
    }

    @Test
    void changeMemberRoleInClub_whenTargetIsAdmin_thenThrowIllegalState() {
        Membre targetAdmin = new Membre();
        targetAdmin.setId(2);
        targetAdmin.setRole(Role.ADMIN); // La cible est un admin
        targetAdmin.setActif(true);

        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        when(membreRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));
        when(adhesionRepository.existsByMembreIdAndClubId(targetAdmin.getId(), clubTest.getId())).thenReturn(true);
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membreTest.getId()); // L'appelant

        assertThrows(IllegalStateException.class,
                () -> membreService.changeMemberRoleInClub(targetAdmin.getId(), clubTest.getId(), Role.MEMBRE));
    }

    @Test
    void changeMemberRoleInClub_whenPromoteToReservationButMultiClub_thenThrowIllegalState() {
        Membre targetMembre = new Membre();
        targetMembre.setId(2);
        targetMembre.setRole(Role.MEMBRE);
        targetMembre.setActif(true);

        doNothing().when(securityService).checkIsActualAdminOfClubOrThrow(clubTest.getId());
        when(membreRepository.findById(targetMembre.getId())).thenReturn(Optional.of(targetMembre));
        when(adhesionRepository.existsByMembreIdAndClubId(targetMembre.getId(), clubTest.getId())).thenReturn(true);
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membreTest.getId());
        when(adhesionRepository.countByMembreId(targetMembre.getId())).thenReturn(2L); // Appartient à > 1 club

        assertThrows(IllegalStateException.class,
                () -> membreService.changeMemberRoleInClub(targetMembre.getId(), clubTest.getId(), Role.RESERVATION));
    }

}

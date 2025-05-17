package org.clubplus.clubplusbackend.controller;

// Imports pour Jackson (sérialisation/désérialisation JSON)

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe de test d'intégration pour MembreController.
 * Utilise @SpringBootTest pour charger le contexte complet de l'application.
 * Utilise @Transactional pour s'assurer que chaque test s'exécute dans sa propre transaction
 * qui est annulée (rollback) à la fin, gardant la base de données propre entre les tests.
 */
@SpringBootTest
@Transactional
class MembreControllerTest {

    // Injecte le contexte de l'application web pour configurer MockMvc.
    @Autowired
    private WebApplicationContext context;

    // Instance de MockMvc pour simuler les requêtes HTTP vers le contrôleur.
    private MockMvc mockMvc;

    // Injecte les dépendances nécessaires pour la configuration des données de test.
    @Autowired
    private MembreDao membreRepository; // Pour interagir avec la base de données des membres.
    @Autowired
    private PasswordEncoder passwordEncoder; // Pour encoder les mots de passe des utilisateurs de test.
    @Autowired
    private ObjectMapper objectMapper; // Pour convertir les objets Java (DTOs) en chaînes JSON.

    // Entités Membre qui seront créées dans setUp() et utilisées dans les tests.
    private Membre bobMembreEntity;     // Utilisateur avec le rôle MEMBRE.
    private Membre adminUserEntity;   // Utilisateur avec le rôle ADMIN.
    private Membre aliceMembreEntity; // Un autre utilisateur MEMBRE, souvent utilisé comme cible.

    // Constantes pour les emails, facilitant la lisibilité et la maintenance.
    private final String BOB_EMAIL = "bob.membre@email.com";
    private final String ADMIN_EMAIL = "admin.user@email.com";
    private final String ALICE_EMAIL = "alice.profil@email.com";

    /**
     * Méthode exécutée avant chaque test (@Test).
     * Configure MockMvc et initialise les données de test nécessaires (utilisateurs).
     */
    @BeforeEach
    void setUp() {
        // Initialise MockMvc avec le contexte de l'application et intègre Spring Security.
        // springSecurity() assure que les filtres et la configuration de Spring Security sont appliqués.
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // --- Création des utilisateurs de test dans la base de données ---
        // Ces utilisateurs sont nécessaires pour que @WithUserDetails puisse les charger
        // et pour simuler différents scénarios d'accès et de modification.
        // L'utilisation de saveAndFlush() assure que les données sont immédiatement écrites
        // en base et que les IDs générés sont disponibles.

        // Création de Bob (MEMBRE)
        Membre bob = new Membre();
        bob.setEmail(BOB_EMAIL);
        bob.setNom("Bob");
        bob.setPrenom("LeMembre");
        bob.setPassword(passwordEncoder.encode("securePassword123")); // Les mots de passe doivent être encodés.
        bob.setRole(Role.MEMBRE);
        bob.setActif(true);         // Important pour que l'utilisateur puisse être chargé par UserDetailsService.
        bob.setVerified(true);      // Autre statut potentiellement vérifié.
        bob.setDate_naissance(LocalDate.now().minusYears(25));
        bob.setDate_inscription(LocalDate.now().minusDays(10));
        bob.setTelephone("0102030405"); // Assurer que tous les champs non nuls sont renseignés.
        bob.setAdhesions(new HashSet<>()); // Initialiser les collections pour éviter les NullPointerExceptions.
        bobMembreEntity = membreRepository.saveAndFlush(bob);

        // Création d'un Admin
        Membre admin = new Membre();
        admin.setEmail(ADMIN_EMAIL);
        admin.setNom("Admin");
        admin.setPrenom("User");
        admin.setPassword(passwordEncoder.encode("adminPassword123"));
        admin.setRole(Role.ADMIN); // Rôle crucial pour les tests d'autorisation.
        admin.setActif(true);
        admin.setVerified(true);
        admin.setDate_naissance(LocalDate.now().minusYears(35));
        admin.setDate_inscription(LocalDate.now().minusDays(100));
        admin.setTelephone("0908070605");
        admin.setAdhesions(new HashSet<>());
        adminUserEntity = membreRepository.saveAndFlush(admin);

        // Création d'Alice (MEMBRE), souvent utilisée comme profil cible.
        Membre alice = new Membre();
        alice.setEmail(ALICE_EMAIL);
        alice.setNom("Alice");
        alice.setPrenom("Profil");
        alice.setPassword(passwordEncoder.encode("passwordAlice"));
        alice.setRole(Role.MEMBRE);
        alice.setActif(true);
        alice.setVerified(true);
        alice.setDate_naissance(LocalDate.now().minusYears(30));
        alice.setTelephone("0655667788");
        alice.setDate_inscription(LocalDate.now().minusDays(1));
        aliceMembreEntity = membreRepository.saveAndFlush(alice);
    }

    /**
     * Teste si un membre authentifié peut récupérer son propre profil.
     *
     * @WithUserDetails charge l'utilisateur "bob.membre@email.com" via UserDetailsService.
     * setupBefore = TestExecutionEvent.TEST_EXECUTION assure que @BeforeEach est exécuté avant @WithUserDetails.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getMyProfile_AsMember_shouldReturnOwnProfile() throws Exception {
        mockMvc.perform(get("/membres/profile") // Endpoint pour récupérer le profil de l'utilisateur courant.
                        .accept(MediaType.APPLICATION_JSON)) // Indique que la réponse attendue est en JSON.
                .andExpect(status().isOk()) // Vérifie que le statut HTTP est 200 OK.
                // Vérifie les champs spécifiques dans la réponse JSON.
                .andExpect(jsonPath("$.id").value(bobMembreEntity.getId()))
                .andExpect(jsonPath("$.email").value(BOB_EMAIL));
        // Ajoutez d'autres jsonPath pour les champs attendus (nom, prenom, role, etc.).
    }

    /**
     * Teste la mise à jour du profil d'un membre authentifié avec des données valides.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateMyProfile_WithValidData_shouldReturnUpdatedMembre() throws Exception {
        UpdateMembreDto updateDto = new UpdateMembreDto(); // DTO pour la mise à jour.
        updateDto.setNom("Robert");
        updateDto.setPrenom("LeMembreMisAJour");
        updateDto.setTelephone("0607080910");
        updateDto.setDate_naissance(LocalDate.now().minusYears(26));
        updateDto.setEmail(BOB_EMAIL); // L'email peut être le même ou un nouveau valide.

        mockMvc.perform(put("/membres/profile") // Requête PUT vers l'endpoint de mise à jour.
                        .with(csrf()) // Ajoute un token CSRF si la protection CSRF est activée.
                        .contentType(MediaType.APPLICATION_JSON) // Indique que le corps de la requête est en JSON.
                        .content(objectMapper.writeValueAsString(updateDto)) // Sérialise le DTO en chaîne JSON.
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()) // Affiche les détails de la requête et de la réponse dans la console (utile pour le débogage).
                .andExpect(status().isOk()) // Vérifie le statut 200 OK.
                .andExpect(jsonPath("$.id").value(bobMembreEntity.getId()))
                .andExpect(jsonPath("$.nom").value("Robert"))
                .andExpect(jsonPath("$.prenom").value("LeMembreMisAJour"))
                .andExpect(jsonPath("$.telephone").value("0607080910"));

        // Vérification supplémentaire directement en base de données pour confirmer la persistance.
        Optional<Membre> updatedMembreOpt = membreRepository.findById(bobMembreEntity.getId());
        assertTrue(updatedMembreOpt.isPresent(), "Le membre mis à jour devrait être trouvé en base.");
        assertEquals("Robert", updatedMembreOpt.get().getNom(), "Le nom du membre devrait être mis à jour en base.");
    }

    /**
     * Teste la mise à jour du profil avec un email qui est déjà utilisé par un autre membre.
     * S'attend à un statut de conflit (409).
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateMyProfile_WithEmailAlreadyTakenByAnotherUser_shouldReturnConflict() throws Exception {
        // aliceMembreEntity (avec ALICE_EMAIL) existe déjà grâce à setUp().
        UpdateMembreDto updateDto = new UpdateMembreDto();
        updateDto.setNom("BobQuiChangeEmail");
        updateDto.setPrenom(bobMembreEntity.getPrenom());
        updateDto.setTelephone(bobMembreEntity.getTelephone());
        updateDto.setDate_naissance(bobMembreEntity.getDate_naissance());
        updateDto.setEmail(ALICE_EMAIL); // Bob essaie de prendre l'email d'Alice.

        mockMvc.perform(put("/membres/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                // Le statut exact (409 Conflict ou 400 Bad Request) dépend de la gestion d'erreur du service/contrôleur.
                .andExpect(status().isConflict());
    }

    /**
     * Teste la mise à jour du profil avec des données invalides (ex: nom vide).
     * S'attend à un statut Bad Request (400) si la validation du DTO (@Valid) échoue.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateMyProfile_WithInvalidData_shouldReturnBadRequest() throws Exception {
        UpdateMembreDto updateDto = new UpdateMembreDto();
        updateDto.setNom(""); // Nom invalide (supposant une contrainte @NotBlank sur le DTO).
        updateDto.setPrenom("ValidPrenom"); // Les autres champs doivent être valides pour isoler l'erreur.
        updateDto.setEmail(BOB_EMAIL);
        updateDto.setTelephone("1234567890");
        updateDto.setDate_naissance(LocalDate.now().minusYears(25));

        mockMvc.perform(put("/membres/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest()); // La validation des contraintes du DTO devrait entraîner un 400.
    }

    /**
     * Teste qu'un membre ne peut pas voir le profil d'un autre membre.
     * S'attend à un statut Forbidden (403).
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getOtherUserProfile_AsMember_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/membres/" + aliceMembreEntity.getId()) // Bob (MEMBRE) essaie de voir le profil d'Alice (MEMBRE).
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // Accès refusé.
    }

    /**
     * Teste qu'un administrateur peut voir le profil d'un autre membre.
     * S'attend à un statut OK (200).
     */
    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    // Authentifié en tant qu'ADMIN.
    void getOtherUserProfile_AsAdmin_shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/membres/" + aliceMembreEntity.getId()) // Admin essaie de voir le profil d'Alice.
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk()) // Accès autorisé pour l'admin.
                .andExpect(jsonPath("$.id").value(aliceMembreEntity.getId()))
                .andExpect(jsonPath("$.email").value(ALICE_EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist()); // Le mot de passe ne doit jamais être exposé.
    }

    /**
     * Teste la tentative de récupération d'un profil d'utilisateur qui n'existe pas.
     * S'attend à un statut Not Found (404), que l'appelant soit membre ou admin.
     */
    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    // Peut aussi être BOB_EMAIL.
    void getOtherUserProfile_WhenNotExists_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/membres/99999") // ID inexistant.
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                // Si la ressource n'existe pas, un 404 est attendu, avant même les vérifications de droits spécifiques.
                .andExpect(status().isNotFound());
    }
}

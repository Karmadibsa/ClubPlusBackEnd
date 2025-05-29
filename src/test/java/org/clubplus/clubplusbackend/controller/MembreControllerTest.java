package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.UpdateMembreDto;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * Classe de test d'intégration pour {@link MembreController}.
 * Utilise {@code @SpringBootTest} pour charger le contexte complet de l'application Spring,
 * permettant de tester les interactions entre le contrôleur, les services, et la base de données.
 * {@code @Transactional} est utilisé pour s'assurer que chaque test s'exécute dans sa propre
 * transaction qui est annulée (rollback) après l'exécution du test, maintenant ainsi
 * la base de données dans un état propre entre les tests.
 */
@SpringBootTest
@Transactional
class MembreControllerTest {

    /**
     * Contexte de l'application web injecté par Spring, utilisé pour construire {@link MockMvc}.
     */
    @Autowired
    private WebApplicationContext context;

    /**
     * Instance de {@link MockMvc} utilisée pour simuler des requêtes HTTP vers le contrôleur.
     */
    private MockMvc mockMvc;

    /**
     * DAO pour l'entité {@link Membre}, injecté pour la configuration des données de test
     * et la vérification de l'état de la base de données après les opérations.
     */
    @Autowired
    private MembreDao membreRepository;

    /**
     * Encodeur de mots de passe, injecté pour créer des utilisateurs de test avec des mots de passe encodés.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Utilitaire Jackson pour convertir des objets Java en chaînes JSON et vice-versa.
     * Principalement utilisé ici pour sérialiser les DTOs envoyés dans le corps des requêtes.
     */
    @Autowired
    private ObjectMapper objectMapper;

    // Entités Membre créées dans setUp() et réutilisées à travers les tests.
    private Membre bobMembreEntity;     // Utilisateur de test avec le rôle MEMBRE.
    private Membre adminUserEntity;   // Utilisateur de test avec le rôle ADMIN.
    private Membre aliceMembreEntity; // Autre utilisateur MEMBRE, souvent utilisé comme cible dans les tests.

    // Constantes pour les emails des utilisateurs de test pour améliorer la lisibilité.
    private final String BOB_EMAIL = "bob.membre@email.com";
    private final String ADMIN_EMAIL = "admin.user@email.com";
    private final String ALICE_EMAIL = "alice.profil@email.com";

    /**
     * Méthode de configuration exécutée avant chaque méthode de test ({@code @Test}).
     * Elle initialise {@link MockMvc} en intégrant la configuration de Spring Security
     * et crée les utilisateurs de test nécessaires dans la base de données.
     */

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context) // Assurez-vous que 'context' est injecté via @Autowired
                .apply(springSecurity())
                .build();

        // Création et persistance des utilisateurs de test.

        // Création de Bob (MEMBRE)
        Membre bob = new Membre();
        bob.setEmail(BOB_EMAIL); // Assurez-vous que BOB_EMAIL est défini
        bob.setNom("Bob");
        bob.setPrenom("LeMembre");
        bob.setPassword(passwordEncoder.encode("ValidPass1!")); // Assurez-vous que passwordEncoder est injecté
        bob.setRole(Role.MEMBRE);
        bob.setActif(true);
        bob.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        bob.setDate_naissance(LocalDate.of(2000, 1, 15));
        bob.setDate_inscription(LocalDate.now().minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant());
        bob.setTelephone("0102030405");
        bob.setAdhesions(new HashSet<>()); // Initialisation pour éviter NullPointerException si le constructeur ne le fait pas
        bobMembreEntity = membreRepository.saveAndFlush(bob); // Assurez-vous que membreRepository est injecté

        // Création d'un Admin
        Membre admin = new Membre();
        admin.setEmail(ADMIN_EMAIL); // Assurez-vous que ADMIN_EMAIL est défini
        admin.setNom("Admin");
        admin.setPrenom("User");
        admin.setPassword(passwordEncoder.encode("AdminPass1!"));
        admin.setRole(Role.ADMIN);
        admin.setActif(true);
        admin.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        admin.setDate_naissance(LocalDate.of(1990, 1, 15));
        admin.setDate_inscription(LocalDate.now().minusDays(100).atStartOfDay(ZoneOffset.UTC).toInstant());
        admin.setTelephone("0908070605");
        admin.setAdhesions(new HashSet<>());
        adminUserEntity = membreRepository.saveAndFlush(admin);

        // Création d'Alice (MEMBRE)
        Membre alice = new Membre();
        alice.setEmail(ALICE_EMAIL); // Assurez-vous que ALICE_EMAIL est défini
        alice.setNom("Alice");
        alice.setPrenom("Profil");
        alice.setPassword(passwordEncoder.encode("AlicePass1!"));
        alice.setRole(Role.MEMBRE);
        alice.setActif(true);
        alice.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        alice.setDate_naissance(LocalDate.of(1990, 1, 15));
        alice.setDate_inscription(LocalDate.now().minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        alice.setTelephone("0655667788");
        // Si Adhesions n'est pas initialisé dans le constructeur de Membre et que vous y accédez plus tard, initialisez-le.
        // alice.setAdhesions(new HashSet<>()); // Si nécessaire
        aliceMembreEntity = membreRepository.saveAndFlush(alice);
    }

    /**
     * Teste la récupération du profil de l'utilisateur actuellement authentifié.
     * L'utilisateur "Bob" (MEMBRE) est authentifié via {@code @WithUserDetails}.
     * S'attend à un statut HTTP 200 (OK) et aux informations de profil de Bob.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération de son propre profil en tant que membre")
    void recupererMonProfil_enTantQueMembre_devraitRetournerProfilUtilisateur() throws Exception {
        mockMvc.perform(get("/membres/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bobMembreEntity.getId()))
                .andExpect(jsonPath("$.email").value(BOB_EMAIL))
                .andExpect(jsonPath("$.nom").value("Bob")); // Exemple d'autres vérifications
    }

    /**
     * Teste la mise à jour du profil de l'utilisateur authentifié avec des données valides.
     * L'utilisateur "Bob" est authentifié.
     * S'attend à un statut HTTP 200 (OK) et à ce que le profil retourné contienne les modifications.
     * Vérifie également que les modifications sont persistées en base de données.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Mise à jour de son profil avec données valides")
    void mettreAJourMonProfil_avecDonneesValides_devraitRetournerMembreMisAJour() throws Exception {

// 2. Convertir cette LocalDate en un Instant représentant le début de cette journée en UTC
        UpdateMembreDto updateDto = new UpdateMembreDto();
        updateDto.setNom("Robert");
        updateDto.setPrenom("LeMembreModifie");
        updateDto.setTelephone("0607080910");
        updateDto.setDate_naissance(LocalDate.of(2000, 1, 15));
        updateDto.setEmail(BOB_EMAIL); // Email inchangé ou nouveau valide

        mockMvc.perform(put("/membres/profile")
                        .with(csrf()) // Nécessaire si la protection CSRF est activée
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()) // Utile pour le débogage: affiche la requête et la réponse
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Robert"))
                .andExpect(jsonPath("$.prenom").value("LeMembreModifie"));

        Optional<Membre> updatedMembreOpt = membreRepository.findById(bobMembreEntity.getId());
        assertTrue(updatedMembreOpt.isPresent());
        assertEquals("Robert", updatedMembreOpt.get().getNom());
    }

    /**
     * Teste la tentative de mise à jour du profil avec un email déjà utilisé par un autre utilisateur.
     * L'utilisateur "Bob" essaie de prendre l'email d'Alice.
     * S'attend à un statut HTTP 409 (Conflict) ou 400 (Bad Request) selon la gestion d'erreur du service.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Mise à jour du profil avec email déjà utilisé par un autre")
    void mettreAJourMonProfil_avecEmailExistantAutreUtilisateur_devraitRetournerConflit() throws Exception {
        UpdateMembreDto updateDto = new UpdateMembreDto();
        updateDto.setNom("BobConflit");
        updateDto.setPrenom(bobMembreEntity.getPrenom());
        updateDto.setTelephone(bobMembreEntity.getTelephone());
        updateDto.setDate_naissance(bobMembreEntity.getDate_naissance());
        updateDto.setEmail(ALICE_EMAIL); // Tentative de prendre l'email d'Alice

        mockMvc.perform(put("/membres/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isConflict()); // Ou isBadRequest()
    }

    /**
     * Teste la mise à jour du profil avec des données invalides (ex: nom vide).
     * S'attend à un statut HTTP 400 (Bad Request) dû à l'échec de la validation du DTO.
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Mise à jour du profil avec données invalides")
    void mettreAJourMonProfil_avecDonneesInvalides_devraitRetournerBadRequest() throws Exception {
        LocalDate dateNaissanceApprox = LocalDate.now().minusYears(25);

// 2. Convertir cette LocalDate en un Instant représentant le début de cette journée en UTC
        Instant instantNaissance = dateNaissanceApprox.atStartOfDay(ZoneOffset.UTC).toInstant();
        UpdateMembreDto updateDto = new UpdateMembreDto();
        updateDto.setNom(""); // Nom invalide (supposant @NotBlank)
        updateDto.setPrenom("PrenomValide");
        updateDto.setEmail(BOB_EMAIL);
        updateDto.setTelephone("0102030405");
        updateDto.setDate_naissance(LocalDate.of(2000, 1, 15));

        mockMvc.perform(put("/membres/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Teste qu'un utilisateur avec le rôle MEMBRE ne peut pas voir le profil d'un autre membre.
     * L'utilisateur "Bob" (MEMBRE) tente de voir le profil d'Alice.
     * S'attend à un statut HTTP 403 (Forbidden).
     */
    @Test
    @WithUserDetails(value = BOB_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération profil autre membre en tant que MEMBRE")
    void recupererProfilAutreMembre_enTantQueMembre_devraitRetournerInterdit() throws Exception {
        mockMvc.perform(get("/membres/" + aliceMembreEntity.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * Teste qu'un utilisateur avec le rôle ADMIN peut voir le profil d'un autre membre.
     * L'utilisateur Admin est authentifié et tente de voir le profil d'Alice.
     * S'attend à un statut HTTP 200 (OK) et aux informations du profil d'Alice.
     */
    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération profil autre membre en tant qu'ADMIN")
    void recupererProfilAutreMembre_enTantQueAdmin_devraitRetournerProfil() throws Exception {
        mockMvc.perform(get("/membres/" + aliceMembreEntity.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceMembreEntity.getId()))
                .andExpect(jsonPath("$.email").value(ALICE_EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist()); // Le mot de passe ne doit jamais être exposé.
    }

    /**
     * Teste la tentative de récupération d'un profil d'utilisateur qui n'existe pas.
     * Que l'appelant soit membre ou admin, une ressource inexistante doit retourner 404.
     * S'attend à un statut HTTP 404 (Not Found).
     */
    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    // L'authentification est nécessaire pour atteindre le point de logique.
    @DisplayName("Récupération profil inexistant")
    void recupererProfilAutreMembre_quandInexistant_devraitRetournerNonTrouve() throws Exception {
        mockMvc.perform(get("/membres/99999") // ID qui n'existe pas.
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}

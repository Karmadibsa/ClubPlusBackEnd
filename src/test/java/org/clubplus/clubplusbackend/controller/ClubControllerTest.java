package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clubplus.clubplusbackend.dao.AdhesionDao;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
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

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe de test d'intégration pour {@link ClubController}.
 * <p>
 * Utilise {@code @SpringBootTest} pour charger le contexte complet de l'application Spring,
 * ce qui permet de tester les interactions réelles entre le contrôleur, les services,
 * les DAOs et la base de données (H2 en mémoire pour les tests).
 * </p>
 * <p>
 * {@code @Transactional} est appliqué à la classe pour s'assurer que chaque méthode de test
 * s'exécute dans sa propre transaction. Par défaut, Spring Test annule (rollback)
 * cette transaction après chaque test, garantissant ainsi que la base de données
 * est remise à son état initial avant le test suivant, ce qui assure l'indépendance des tests.
 * </p>
 */
@SpringBootTest
@Transactional
class ClubControllerTest {

    /**
     * Contexte de l'application web injecté par Spring.
     * Il est utilisé pour construire l'instance de {@link MockMvc}.
     */
    @Autowired
    private WebApplicationContext context;

    /**
     * Instance de {@link MockMvc} utilisée pour simuler des requêtes HTTP vers le {@link ClubController}
     * et vérifier les réponses (statut, en-têtes, corps JSON).
     */
    private MockMvc mockMvc;

    // --- Injection des DAOs et services nécessaires pour la configuration des données de test ---
    @Autowired
    private ClubDao clubRepository;
    @Autowired
    private MembreDao membreRepository;
    @Autowired
    private AdhesionDao adhesionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder; // Pour encoder les mots de passe des utilisateurs de test.
    @Autowired
    private ObjectMapper objectMapper;    // Pour sérialiser les DTOs en JSON pour les corps de requête.
    // @Autowired private EventDao eventRepository; // Décommentez si vous testez les endpoints d'événements.

    // --- Entités de test créées dans setUp() et utilisées à travers plusieurs tests ---
    private Club clubTestAlpha;
    private Membre adminClubAlpha;      // Administrateur du clubTestAlpha.
    private Membre membreClubAlpha;     // Membre simple du clubTestAlpha.
    private Membre utilisateurReservation; // Utilisateur avec le rôle RESERVATION pour clubTestAlpha.
    private Membre autreAdmin;          // Un autre admin, non lié à clubTestAlpha par défaut.

    // --- Constantes pour les emails des utilisateurs de test, utilisées avec @WithUserDetails ---
    private final String ADMIN_ALPHA_EMAIL = "admin.alpha@club.com";
    private final String MEMBRE_ALPHA_EMAIL = "membre.alpha@club.com";
    private final String RESERVATION_USER_EMAIL = "reservation.user@club.com";
    private final String AUTRE_ADMIN_EMAIL = "autre.admin@club.com";

    /**
     * Méthode de configuration exécutée avant chaque méthode de test (annotée avec {@code @Test}).
     * <p>
     * Elle initialise {@link MockMvc} en intégrant la configuration de Spring Security pour que
     * les mécanismes d'authentification et d'autorisation soient pris en compte.
     * Elle crée également un ensemble de données de test de base (clubs, membres, adhésions)
     * qui seront persistées en base de données avant chaque test.
     * </p>
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // --- Création et persistance des données de test ---

        // Création du Club de test "Alpha"
        clubTestAlpha = new Club();
        clubTestAlpha.setNom("Club Alpha de Test");
        clubTestAlpha.setEmail("alpha.test@clubexample.com");
        clubTestAlpha.setCodeClub("CLUB-001");
        clubTestAlpha.setActif(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        clubTestAlpha.setDate_creation(LocalDate.of(2020, 1, 15));
        clubTestAlpha.setDate_inscription(LocalDate.now().minusYears(1));
        clubTestAlpha.setNumero_voie("1A");
        clubTestAlpha.setRue("Rue Alpha Test");
        clubTestAlpha.setCodepostal("75001");
        clubTestAlpha.setVille("Paris");
        clubTestAlpha.setTelephone("0101010101");
        clubRepository.saveAndFlush(clubTestAlpha);

        // Création de l'administrateur pour Club Alpha
        adminClubAlpha = new Membre();
        adminClubAlpha.setEmail(ADMIN_ALPHA_EMAIL);
        adminClubAlpha.setNom("AdminAlphaNom");
        adminClubAlpha.setPrenom("AdminAlphaPrenom");
        adminClubAlpha.setPassword(passwordEncoder.encode("AdminPass123!")); // Assurez-vous que passwordEncoder est injecté
        adminClubAlpha.setRole(Role.ADMIN);
        adminClubAlpha.setActif(true);
        adminClubAlpha.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        adminClubAlpha.setDate_naissance(LocalDate.of(1995, 1, 15));
        adminClubAlpha.setDate_inscription(LocalDate.now().minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant());
        adminClubAlpha.setTelephone("0202020202");
        membreRepository.saveAndFlush(adminClubAlpha);
        adhesionRepository.saveAndFlush(new Adhesion(adminClubAlpha, clubTestAlpha));

        // Création d'un membre simple pour Club Alpha
        membreClubAlpha = new Membre();
        membreClubAlpha.setEmail(MEMBRE_ALPHA_EMAIL);
        membreClubAlpha.setNom("MembreAlphaNom");
        membreClubAlpha.setPrenom("MembreAlphaPrenom");
        membreClubAlpha.setPassword(passwordEncoder.encode("MembrePass123!"));
        membreClubAlpha.setRole(Role.MEMBRE);
        membreClubAlpha.setActif(true);
        membreClubAlpha.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        membreClubAlpha.setDate_naissance(LocalDate.of(2000, 1, 15));
        membreClubAlpha.setDate_inscription(LocalDate.now().minusDays(5).atStartOfDay(ZoneOffset.UTC).toInstant());
        membreClubAlpha.setTelephone("0303030303");
        membreRepository.saveAndFlush(membreClubAlpha);
        adhesionRepository.saveAndFlush(new Adhesion(membreClubAlpha, clubTestAlpha));

        // Création d'un utilisateur avec rôle RESERVATION pour Club Alpha
        utilisateurReservation = new Membre();
        utilisateurReservation.setEmail(RESERVATION_USER_EMAIL);
        utilisateurReservation.setNom("UserResaNom");
        utilisateurReservation.setPrenom("UserResaPrenom");
        utilisateurReservation.setPassword(passwordEncoder.encode("ResaPass123!"));
        utilisateurReservation.setRole(Role.RESERVATION);
        utilisateurReservation.setActif(true);
        utilisateurReservation.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        utilisateurReservation.setDate_naissance(LocalDate.of(1998, 1, 15));
        utilisateurReservation.setDate_inscription(LocalDate.now().minusDays(8).atStartOfDay(ZoneOffset.UTC).toInstant());
        utilisateurReservation.setTelephone("0404040404");
        membreRepository.saveAndFlush(utilisateurReservation);
        adhesionRepository.saveAndFlush(new Adhesion(utilisateurReservation, clubTestAlpha));

        // Création d'un autre admin
        autreAdmin = new Membre();
        autreAdmin.setEmail(AUTRE_ADMIN_EMAIL);
        autreAdmin.setNom("AutreAdminNom");
        autreAdmin.setPrenom("AutreAdminPrenom");
        autreAdmin.setPassword(passwordEncoder.encode("AutreAdminPass123!"));
        autreAdmin.setRole(Role.ADMIN);
        autreAdmin.setActif(true);
        autreAdmin.setVerified(true);
        // Conversion de LocalDate en Instant (début du jour UTC)
        autreAdmin.setDate_naissance(LocalDate.of(1985, 1, 15));
        autreAdmin.setDate_inscription(LocalDate.now().minusDays(20).atStartOfDay(ZoneOffset.UTC).toInstant());
        autreAdmin.setTelephone("0505050505");
        membreRepository.saveAndFlush(autreAdmin);
    }

    /**
     * Teste l'endpoint {@code GET /clubs/{id}}.
     * Scénario: Un utilisateur avec le rôle {@code RESERVATION} qui est membre du club demandé.
     * Attente: Statut 200 (OK) et les détails du club.
     */
    @Test
    @WithUserDetails(value = RESERVATION_USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération d'un club par son ID (utilisateur RESERVATION du club)")
    void recupererClubParId_enTantQueUtilisateurReservationDuClub_devraitRetournerClub() throws Exception {
        mockMvc.perform(get("/clubs/" + clubTestAlpha.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()) // Affiche les détails de la requête et de la réponse, utile pour le débogage.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clubTestAlpha.getId()))
                .andExpect(jsonPath("$.nom").value(clubTestAlpha.getNom()))
                .andExpect(jsonPath("$.email").value(clubTestAlpha.getEmail()));
    }

    /**
     * Teste l'endpoint {@code GET /clubs/{id}}.
     * Scénario: Un utilisateur avec le rôle {@code MEMBRE} (non suffisant pour cet endpoint spécifique).
     * Attente: Statut 403 (Forbidden).
     */
    @Test
    @WithUserDetails(value = MEMBRE_ALPHA_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération d'un club par son ID (MEMBRE simple, non autorisé)")
    void recupererClubParId_enTantQueMembreSimple_devraitRetournerInterdit() throws Exception {
        mockMvc.perform(get("/clubs/" + clubTestAlpha.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * Teste l'endpoint {@code GET /clubs/{id}}.
     * Scénario: Un utilisateur {@code ADMIN} qui n'est PAS membre du club demandé.
     * La logique de service ({@code ClubService.getClubByIdWithSecurityCheck}) doit vérifier l'appartenance.
     * Attente: Statut 403 (Forbidden) si la règle est qu'il faut être membre pour voir via cet endpoint.
     */
    @Test
    @WithUserDetails(value = AUTRE_ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération d'un club par son ID (ADMIN non membre du club)")
    void recupererClubParId_enTantQuAdminNonMembre_devraitRetournerInterdit() throws Exception {
        mockMvc.perform(get("/clubs/" + clubTestAlpha.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * Teste l'endpoint {@code GET /clubs/{id}}.
     * Scénario: Tentative de récupération d'un club avec un ID qui n'existe pas.
     * L'utilisateur est un admin pour passer les premiers filtres de rôle potentiels.
     * Attente: Statut 404 (Not Found) car la ressource elle-même n'existe pas.
     * Ceci suppose que la vérification d'existence précède les vérifications d'autorisation spécifiques.
     */
    @Test
    @WithUserDetails(value = ADMIN_ALPHA_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération d'un club par un ID inexistant (comportement actuel)")
    void recupererClubParId_quandClubInexistant_devraitRetournerInterditActuellement() throws Exception {
        mockMvc.perform(get("/clubs/99999")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // S'attend à 403
    }

    /**
     * Teste l'endpoint {@code PUT /clubs/{id}}.
     * Scénario: Mise à jour d'un club par son administrateur ({@code ADMIN_ALPHA_EMAIL}) avec des données valides.
     * Attente: Statut 200 (OK) et le club mis à jour retourné dans le corps de la réponse.
     */
    @Test
    @WithUserDetails(value = ADMIN_ALPHA_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Mise à jour d'un club par son admin avec des données valides")
    void mettreAJourClub_parSonAdminAvecDonneesValides_devraitRetournerClubMisAJour() throws Exception {
        UpdateClubDto updateDto = new UpdateClubDto();
        updateDto.setNom("Club Alpha Renommé par Admin");
        updateDto.setTelephone("0199999999");
        // CORRECTION: Fournir TOUS les champs obligatoires du UpdateClubDto.
        // S'ils ne sont pas modifiés, utiliser les valeurs existantes de clubTestAlpha.
        updateDto.setEmail(clubTestAlpha.getEmail()); // Supposons que l'email ne change pas, mais est requis.
        updateDto.setNumero_voie(clubTestAlpha.getNumero_voie());
        updateDto.setRue(clubTestAlpha.getRue());
        updateDto.setCodepostal(clubTestAlpha.getCodepostal());
        updateDto.setVille(clubTestAlpha.getVille());
        // Ajouter d'autres champs de UpdateClubDto s'ils sont annotés @NotBlank/@NotNull.

        mockMvc.perform(put("/clubs/" + clubTestAlpha.getId())
                        .with(csrf()) // Protection CSRF.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)) // Corps de la requête avec le DTO sérialisé.
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Club Alpha Renommé par Admin"))
                .andExpect(jsonPath("$.telephone").value("0199999999"));
    }

    /**
     * Teste l'endpoint {@code PUT /clubs/{id}}.
     * Scénario: Tentative de mise à jour d'un club par un administrateur ({@code AUTRE_ADMIN_EMAIL})
     * qui n'est PAS l'administrateur de ce club spécifique.
     * Attente: Statut 403 (Forbidden).
     */
    @Test
    @WithUserDetails(value = AUTRE_ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Mise à jour d'un club par un admin non autorisé (pas l'admin du club)")
    void mettreAJourClub_parAutreAdmin_devraitRetournerInterdit() throws Exception {
        UpdateClubDto updateDto = new UpdateClubDto();
        updateDto.setNom("Tentative de Renommage par Autre Admin");
        // CORRECTION: Fournir TOUS les champs obligatoires du UpdateClubDto pour qu'il soit valide
        // et que la logique de sécurité soit atteinte.
        updateDto.setEmail(clubTestAlpha.getEmail());
        updateDto.setNumero_voie(clubTestAlpha.getNumero_voie());
        updateDto.setRue(clubTestAlpha.getRue());
        updateDto.setCodepostal(clubTestAlpha.getCodepostal());
        updateDto.setVille(clubTestAlpha.getVille());
        updateDto.setTelephone(clubTestAlpha.getTelephone());

        mockMvc.perform(put("/clubs/" + clubTestAlpha.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // L'assertion attendue est 403.
    }

    /**
     * Teste l'endpoint {@code PUT /clubs/{id}}.
     * Scénario: Mise à jour d'un club avec des données invalides (ex: nom vide).
     * L'utilisateur est l'admin du club pour que la requête atteigne la validation du DTO.
     * Attente: Statut 400 (Bad Request) en raison de l'échec de la validation du {@link UpdateClubDto}.
     */
    @Test
    @WithUserDetails(value = ADMIN_ALPHA_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Mise à jour d'un club avec des données invalides (nom vide)")
    void mettreAJourClub_avecDonneesInvalides_devraitRetournerBadRequest() throws Exception {
        UpdateClubDto updateDto = new UpdateClubDto();
        updateDto.setNom(""); // Nom invalide (supposant une contrainte @NotBlank sur le DTO).
        // Remplir les autres champs nécessaires du DTO pour éviter d'autres erreurs de validation non liées au nom.
        updateDto.setEmail(clubTestAlpha.getEmail());
        updateDto.setNumero_voie(clubTestAlpha.getNumero_voie());
        updateDto.setRue(clubTestAlpha.getRue());
        updateDto.setCodepostal(clubTestAlpha.getCodepostal());
        updateDto.setVille(clubTestAlpha.getVille());
        updateDto.setTelephone(clubTestAlpha.getTelephone());

        mockMvc.perform(put("/clubs/" + clubTestAlpha.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Teste l'endpoint {@code DELETE /clubs/{id}}.
     * Scénario: Désactivation (suppression logique) d'un club par son administrateur.
     * Attente: Statut 204 (No Content) indiquant le succès de l'opération sans corps de réponse.
     * Condition: Le club ne doit pas avoir d'événements futurs actifs qui empêcheraient sa désactivation.
     */
    @Test
    @WithUserDetails(value = ADMIN_ALPHA_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Désactivation d'un club par son administrateur")
    void desactiverClub_parSonAdmin_devraitRetournerNoContent() throws Exception {
        // Pour ce test, nous supposons que clubTestAlpha n'a pas d'événements futurs actifs.
        // Si la logique de désactivation en dépend, il faudrait s'assurer de cet état
        // (par exemple, en ne créant pas d'événements pour ce club, ou en les supprimant/modifiant).

        mockMvc.perform(delete("/clubs/" + clubTestAlpha.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    /**
     * Teste l'endpoint {@code GET /clubs/{id}/membres}.
     * Scénario: Récupération des membres d'un club par un utilisateur ayant le rôle {@code RESERVATION}
     * et étant membre de ce club.
     * Attente: Statut 200 (OK) et la liste des membres du club.
     */
    @Test
    @WithUserDetails(value = RESERVATION_USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération des membres d'un club (utilisateur RESERVATION du club)")
    void recupererMembresClub_parUtilisateurReservation_devraitRetournerListeMembres() throws Exception {
        mockMvc.perform(get("/clubs/" + clubTestAlpha.getId() + "/membres")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // Vérifie que la liste JSON contient 3 membres (adminClubAlpha, membreClubAlpha, utilisateurReservation).
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.email == '%s')]", ADMIN_ALPHA_EMAIL).exists())
                .andExpect(jsonPath("$[?(@.email == '%s')]", MEMBRE_ALPHA_EMAIL).exists())
                .andExpect(jsonPath("$[?(@.email == '%s')]", RESERVATION_USER_EMAIL).exists());
    }

    /**
     * Teste l'endpoint {@code GET /clubs/{id}/events}.
     * Scénario: Récupération des événements d'un club par un membre connecté de ce club.
     * Attente: Statut 200 (OK) et la liste des événements du club.
     * Pour cet exemple, on s'attend à une liste vide si aucun événement n'est créé.
     */
    @Test
    @WithUserDetails(value = MEMBRE_ALPHA_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupération des événements d'un club (membre du club)")
    void recupererEvenementsClub_parMembreDuClub_devraitRetournerListeEvenements() throws Exception {
        // Prérequis pour un test plus complet :
        // Créer des entités Event associées à clubTestAlpha dans la base de données
        // soit dans setUp(), soit au début de cette méthode de test.
        // Ex: Event event1 = new Event(...); event1.setOrganisateur(clubTestAlpha); eventRepository.save(event1);

        mockMvc.perform(get("/clubs/" + clubTestAlpha.getId() + "/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // S'attend à 0 événements si aucun n'a été explicitement créé pour clubTestAlpha.
                // Si vous créez des événements, ajustez hasSize() en conséquence.
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clubplus.clubplusbackend.dao.*;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ReservationControllerTest {

    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired
    private ReservationDao reservationRepository;
    @Autowired
    private ClubDao clubRepository;
    @Autowired
    private MembreDao membreRepository;
    @Autowired
    private AdhesionDao adhesionRepository;
    @Autowired
    private EventDao eventRepository;
    @Autowired
    private CategorieDao categorieRepository;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private Membre membreSimple;
    private Membre gestionnaireReservation;
    private Club clubTest;
    private Event eventTest;
    private Categorie categorieTest;
    private Reservation reservationExistante;

    private final String MEMBRE_EMAIL = "membre.test@club.com";
    private final String GESTIONNAIRE_EMAIL = "gestionnaire.test@club.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        clubTest = new Club();
        clubTest.setNom("Club de Test pour Réservations");
        clubTest.setCodeClub("CLUB-RESA");
        clubTest.setDate_inscription(LocalDate.now());
        clubTest.setActif(true);
        clubTest.setEmail("resa.club@test.com");
        clubTest.setTelephone("0102030405");
        clubTest.setNumero_voie("123");
        clubTest.setRue("Rue des Tests");
        clubTest.setCodepostal("75000");
        clubTest.setVille("Testville");
        clubTest.setDate_creation(LocalDate.now().minusYears(1));
        clubRepository.saveAndFlush(clubTest);

        membreSimple = new Membre();
        membreSimple.setEmail(MEMBRE_EMAIL);
        membreSimple.setNom("Test");
        membreSimple.setPrenom("Membre");
        membreSimple.setPassword(passwordEncoder.encode("Password123!"));
        membreSimple.setRole(Role.MEMBRE);
        membreSimple.setVerified(true);
        membreSimple.setDate_naissance(LocalDate.of(1990, 1, 1));
        membreSimple.setDate_inscription(LocalDate.now());
        membreSimple.setTelephone("0102030405");
        membreRepository.saveAndFlush(membreSimple);
        adhesionRepository.saveAndFlush(new Adhesion(membreSimple, clubTest));

        gestionnaireReservation = new Membre();
        gestionnaireReservation.setEmail(GESTIONNAIRE_EMAIL);
        gestionnaireReservation.setNom("Test");
        gestionnaireReservation.setPrenom("Gestionnaire");
        gestionnaireReservation.setPassword(passwordEncoder.encode("Password123!"));
        gestionnaireReservation.setRole(Role.RESERVATION);
        gestionnaireReservation.setVerified(true);
        gestionnaireReservation.setDate_naissance(LocalDate.of(1985, 1, 1));
        gestionnaireReservation.setDate_inscription(LocalDate.now());
        gestionnaireReservation.setTelephone("0607080910");
        membreRepository.saveAndFlush(gestionnaireReservation);
        adhesionRepository.saveAndFlush(new Adhesion(gestionnaireReservation, clubTest));

        eventTest = new Event();
        eventTest.setNom("Événement de Test");
        // Par défaut, l'événement est dans le futur, ce qui est correct pour les tests de création/annulation.
        eventTest.setStartTime(Instant.now().plus(10, ChronoUnit.DAYS));
        eventTest.setEndTime(Instant.now().plus(11, ChronoUnit.DAYS));
        eventTest.setDescription("Description de l'événement de test");
        eventTest.setOrganisateur(clubTest);
        eventTest.setActif(true);
        eventRepository.saveAndFlush(eventTest);

        categorieTest = new Categorie();
        categorieTest.setNom("Catégorie Standard");
        categorieTest.setCapacite(50);
        categorieTest.setEvent(eventTest);
        categorieRepository.saveAndFlush(categorieTest);

        reservationExistante = new Reservation(membreSimple, eventTest, categorieTest);
        reservationRepository.saveAndFlush(reservationExistante);
    }

    @Test
    @WithUserDetails(value = MEMBRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Un membre connecté doit pouvoir créer une réservation")
    void createMyReservation_ShouldReturn201_WhenUserIsMember() throws Exception {
        mockMvc.perform(post("/reservations")
                        .with(csrf())
                        .param("eventId", String.valueOf(eventTest.getId()))
                        .param("categorieId", String.valueOf(categorieTest.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails(value = MEMBRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Tenter de créer une réservation pour un événement inactif doit retourner 409 Conflict")
    void createMyReservation_ShouldReturn409_WhenEventIsInactive() throws Exception {
        eventTest.setActif(false);
        eventRepository.save(eventTest);

        mockMvc.perform(post("/reservations")
                        .with(csrf())
                        .param("eventId", String.valueOf(eventTest.getId()))
                        .param("categorieId", String.valueOf(categorieTest.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails(value = MEMBRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Récupérer ses réservations filtrées par statut")
    void getMyReservations_ShouldReturnFilteredList() throws Exception {
        mockMvc.perform(get("/reservations/me").param("status", "CONFIRME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithUserDetails(value = GESTIONNAIRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Un gestionnaire doit pouvoir récupérer les réservations d'une catégorie")
    void getReservationsByCategorie_ShouldReturnList_WhenUserIsManager() throws Exception {
        mockMvc.perform(get("/reservations/categorie/{categorieId}", categorieTest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithUserDetails(value = MEMBRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Un membre ne doit pas pouvoir récupérer les réservations d'une catégorie")
    void getReservationsByCategorie_ShouldReturn403_WhenUserIsMember() throws Exception {
        mockMvc.perform(get("/reservations/categorie/{categorieId}", categorieTest.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = MEMBRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Un membre doit pouvoir annuler sa propre réservation")
    void cancelReservation_ShouldReturn204_WhenUserIsOwner() throws Exception {
        mockMvc.perform(put("/reservations/{id}/cancel", reservationExistante.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails(value = MEMBRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Tenter d'annuler une réservation déjà annulée doit retourner 409 Conflict")
    void cancelReservation_ShouldReturn409_WhenAlreadyCancelled() throws Exception {
        reservationService.cancelReservationById(reservationExistante.getId());

        mockMvc.perform(put("/reservations/{id}/cancel", reservationExistante.getId())
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails(value = GESTIONNAIRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Un gestionnaire doit pouvoir marquer une réservation comme utilisée")
    void markReservationUsed_ShouldReturn200_WhenUserIsManager() throws Exception {
        // --- SOLUTION SIMPLIFIÉE GRÂCE À LA NOUVELLE RÈGLE MÉTIER ---
        // 1. On positionne l'événement dans le futur proche (dans 30 minutes).
        //    Cette date satisfait à la fois la validation @FutureOrPresent ET la règle
        //    métier "moins d'une heure avant l'événement".
        eventTest.setStartTime(Instant.now().plus(30, ChronoUnit.MINUTES));
        eventTest.setEndTime(Instant.now().plus(2, ChronoUnit.HOURS)); // On s'assure que la fin est après le début

        // 2. On peut maintenant sauvegarder via le repository sans crainte,
        //    car le validateur sera satisfait.
        eventRepository.saveAndFlush(eventTest);

        // 3. La requête du contrôleur peut maintenant s'exécuter avec succès.
        mockMvc.perform(patch("/reservations/uuid/{uuid}/use", reservationExistante.getReservationUuid())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UTILISE")));
    }


    @Test
    @WithUserDetails(value = GESTIONNAIRE_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Tenter de marquer une réservation avec un UUID inexistant doit retourner 404")
    void markReservationUsed_ShouldReturn404_WhenUuidNotFound() throws Exception {
        mockMvc.perform(patch("/reservations/uuid/{uuid}/use", "uuid-inexistant-1234")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un utilisateur non authentifié doit recevoir 403 Forbidden sur un POST")
    void createReservation_WhenNotAuthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/reservations")
                        .param("eventId", String.valueOf(eventTest.getId()))
                        .param("categorieId", String.valueOf(categorieTest.getId())))
                .andExpect(status().isForbidden());
    }
}

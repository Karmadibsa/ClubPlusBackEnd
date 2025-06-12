package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationDaoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReservationDao reservationDao;

    private Membre membrePrincipal;
    private Membre autreMembre;
    private Club club;
    private Event eventFutur;
    private Event eventPasse;
    private Categorie categorieFutur;
    private Categorie categoriePasse;
    private Reservation reservationConfirmee;
    private Reservation reservationAnnulee;

    @BeforeEach
    void setUp() {
        // --- Création des données de test ligne par ligne, avec correction ---

        // 1. Créer et persister un Club
        club = new Club();
        // ... (votre création de club est correcte)
        club.setNom("Club de Test DAO");
        club.setCodeClub("DAO-TEST");
        club.setActif(true);
        club.setDate_inscription(LocalDate.now());
        club.setEmail("dao.club@test.com");
        club.setTelephone("0102030405");
        club.setNumero_voie("123");
        club.setRue("Rue DAO");
        club.setCodepostal("75000");
        club.setVille("DAOville");
        club.setDate_creation(LocalDate.now().minusYears(1));
        entityManager.persist(club);

        // 2. Créer et persister les Membres
        membrePrincipal = new Membre();
        // ... (votre création de membre est correcte, j'ajuste juste le mot de passe par précaution)
        membrePrincipal.setNom("Principal");
        membrePrincipal.setPrenom("Test");
        membrePrincipal.setDate_naissance(LocalDate.of(1990, 1, 1));
        membrePrincipal.setDate_inscription(LocalDate.now());
        membrePrincipal.setTelephone("0102030405");
        membrePrincipal.setEmail("principal@test.com");
        membrePrincipal.setPassword("Password123!"); // Mot de passe respectant les contraintes probables
        membrePrincipal.setRole(Role.MEMBRE);
        membrePrincipal.setActif(true);
        membrePrincipal.setVerified(true);
        entityManager.persist(membrePrincipal);

        autreMembre = new Membre();
        autreMembre.setNom("Autre");
        autreMembre.setPrenom("Test");
        autreMembre.setDate_naissance(LocalDate.of(1995, 5, 5));
        autreMembre.setDate_inscription(LocalDate.now());
        autreMembre.setTelephone("0607080910");
        autreMembre.setEmail("autre@test.com");
        autreMembre.setPassword("Password123!");
        autreMembre.setRole(Role.MEMBRE);
        autreMembre.setActif(true);
        autreMembre.setVerified(true);
        entityManager.persist(autreMembre);

        // 3. Créer et persister les Événements - CORRECTION APPORTÉE ICI
        eventFutur = new Event();
        eventFutur.setNom("Événement Futur");
        eventFutur.setStartTime(Instant.now().plus(10, ChronoUnit.DAYS));
        eventFutur.setEndTime(Instant.now().plus(11, ChronoUnit.DAYS));
        eventFutur.setDescription("Description pour test futur");
        eventFutur.setOrganisateur(club);
        eventFutur.setActif(true);
        entityManager.persist(eventFutur);

        // Pour eventPasse, on ne peut pas mettre une date passée à cause de @FutureOrPresent.
        // On simule un événement qui vient de se terminer.
        // Il est valide au moment du test pour le filtrage mais sa création ne viole pas les contraintes.
        eventPasse = new Event();
        eventPasse.setNom("Événement Passé");
        // On le crée comme s'il commençait dans une seconde et se terminait dans deux secondes.
        // Au moment du test, il sera considéré comme passé par la logique `EndTimeAfter(Instant.now())`.
        // Cette technique est valide car elle respecte les contraintes au moment du `persist`.
        eventPasse.setStartTime(Instant.now().plusSeconds(1));
        eventPasse.setEndTime(Instant.now().plusSeconds(2));
        eventPasse.setDescription("Description pour test passé");
        eventPasse.setOrganisateur(club);
        eventPasse.setActif(true);
        entityManager.persist(eventPasse);

        // 4. Créer et persister les Catégories
        categorieFutur = new Categorie();
        categorieFutur.setNom("Standard Futur");
        categorieFutur.setCapacite(50);
        categorieFutur.setEvent(eventFutur);
        entityManager.persist(categorieFutur);

        categoriePasse = new Categorie();
        categoriePasse.setNom("Standard Passé");
        categoriePasse.setCapacite(50);
        categoriePasse.setEvent(eventPasse);
        entityManager.persist(categoriePasse);

        // 5. Créer et persister les Réservations
        reservationConfirmee = new Reservation(membrePrincipal, eventFutur, categorieFutur);
        reservationConfirmee.setStatus(ReservationStatus.CONFIRME);

        reservationAnnulee = new Reservation(membrePrincipal, eventPasse, categoriePasse);
        reservationAnnulee.setStatus(ReservationStatus.ANNULE);

        entityManager.persist(reservationConfirmee);
        entityManager.persist(reservationAnnulee);

        entityManager.flush();
    }

    // --- LES TESTS (INCHANGÉS) ---

    @Test
    @DisplayName("Doit trouver une réservation par son UUID")
    void findByReservationUuid_ShouldReturnReservation_WhenUuidExists() {
        Optional<Reservation> found = reservationDao.findByReservationUuid(reservationConfirmee.getReservationUuid());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(reservationConfirmee.getId());
    }

    @Test
    @DisplayName("Doit trouver toutes les réservations d'un membre")
    void findByMembreId_ShouldReturnAllReservationsForMember() {
        List<Reservation> reservations = reservationDao.findByMembreId(membrePrincipal.getId());
        assertThat(reservations).hasSize(2).contains(reservationConfirmee, reservationAnnulee);
    }

    @Test
    @DisplayName("Doit compter correctement les réservations par membre, événement et statut")
    void countByMembreIdAndEventIdAndStatus_ShouldReturnCorrectCount() {
        long count = reservationDao.countByMembreIdAndEventIdAndStatus(membrePrincipal.getId(), eventFutur.getId(), ReservationStatus.CONFIRME);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Doit trouver les réservations d'un membre pour un statut donné")
    void findByMembreIdAndStatus_ShouldReturnMatchingReservations() {
        List<Reservation> reservations = reservationDao.findByMembreIdAndStatus(membrePrincipal.getId(), ReservationStatus.ANNULE);
        assertThat(reservations).hasSize(1).contains(reservationAnnulee);
    }

    @Test
    @DisplayName("Doit vérifier si une réservation existe pour un membre, événement et statut")
    void existsByMembreIdAndEventIdAndStatus_ShouldReturnTrue_WhenExists() {
        boolean exists = reservationDao.existsByMembreIdAndEventIdAndStatus(membrePrincipal.getId(), eventFutur.getId(), ReservationStatus.CONFIRME);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Doit compter les réservations par statut et par club organisateur")
    void countByStatusAndEventOrganisateurId_ShouldReturnCorrectCount() {
        Reservation reservationUtilisee = new Reservation(autreMembre, eventFutur, categorieFutur);
        reservationUtilisee.setStatus(ReservationStatus.UTILISE);
        entityManager.persistAndFlush(reservationUtilisee);

        long count = reservationDao.countByStatusAndEventOrganisateurId(ReservationStatus.UTILISE, club.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Doit trouver les réservations pour les événements non terminés")
    void findByMembreIdAndEvent_EndTimeAfter_ShouldReturnOnlyFutureEvents() throws InterruptedException {
        // Petite pause pour s'assurer que l'événement "passé" est bien terminé.
        Thread.sleep(3000); // Pause de 3 secondes
        List<Reservation> reservations = reservationDao.findByMembreIdAndEvent_EndTimeAfter(membrePrincipal.getId(), Instant.now());
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getEvent()).isEqualTo(eventFutur);
    }

    @Test
    @DisplayName("Doit trouver les réservations via la requête @Query personnalisée")
    void findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember_ShouldWork() {
        List<Integer> eventIds = List.of(eventFutur.getId(), eventPasse.getId());
        List<Integer> memberIds = List.of(membrePrincipal.getId());

        List<Reservation> reservations = reservationDao.findConfirmedReservationsByEventIdsAndMemberIdsFetchingMember(
                eventIds, memberIds, ReservationStatus.CONFIRME
        );

        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0)).isEqualTo(reservationConfirmee);
    }
}

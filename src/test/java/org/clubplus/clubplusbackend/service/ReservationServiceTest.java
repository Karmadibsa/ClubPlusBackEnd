package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// --- CONFIGURATION DE LA CLASSE DE TEST DE SERVICE (Identique à votre MembreServiceTest) ---
// @ExtendWith(MockitoExtension.class) active l'utilisation des annotations Mockito comme @Mock et @InjectMocks. [2]
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    // --- DÉPENDANCES MOCKÉES ---
    // @Mock crée une simulation (un "mock") de chaque dépendance. Nous contrôlerons leur comportement. [2]
    @Mock
    private ReservationDao reservationRepository;
    @Mock
    private MembreDao membreRepository;
    @Mock
    private EventDao eventRepository; // Bien que non utilisé directement, il est bon de le lister si c'est une dépendance.
    @Mock
    private CategorieDao categorieRepository;
    @Mock
    private SecurityService securityService;

    // --- CLASSE SOUS TEST ---
    // @InjectMocks crée une instance réelle de ReservationService et y injecte les mocks ci-dessus. [2]
    @InjectMocks
    private ReservationService reservationService;

    // --- Données de test réutilisables ---
    private Membre membre;
    private Event event;
    private Categorie categorie;
    private Club club;

    // Méthode de configuration exécutée avant chaque test. [2]
    @BeforeEach
    void setUp() {
        // Initialisation de nos objets de test (simples POJOs)
        club = new Club();
        club.setId(1);
        club.setNom("Test Club");

        membre = new Membre();
        membre.setId(100);
        membre.setNom("Testeur");

        event = new Event();
        event.setId(1000);
        event.setNom("Super Event");
        event.setActif(true);
        // Événement futur pour permettre la réservation [1]
        event.setStartTime(Instant.now().plus(10, ChronoUnit.DAYS));
        event.setOrganisateur(club);

        categorie = new Categorie();
        categorie.setId(500);
        categorie.setNom("Standard");
        categorie.setEvent(event); // La catégorie appartient bien à l'événement
    }

    // --- TESTS POUR createMyReservation ---

    @Test
    @DisplayName("createMyReservation - Doit réussir quand toutes les conditions sont remplies")
    void createMyReservation_ShouldSucceed_WhenAllConditionsAreMet() {

        // 1. L'utilisateur est connecté et on connaît son ID
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());

        // 2. Les entités Membre et Categorie existent et sont trouvées
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(categorie));

        // 3. La vérification de sécurité (membre du club) passe sans erreur
        doNothing().when(securityService).checkMemberOfEventClubOrThrow(event.getId());

        // 4. Le membre n'a pas encore atteint sa limite de réservation pour cet événement
        when(reservationRepository.countByMembreIdAndEventIdAndStatus(membre.getId(), event.getId(), ReservationStatus.CONFIRME)).thenReturn(0L);

        // 5. La catégorie a des places disponibles
        // Pour tester getPlaceDisponible(), on doit mocker la catégorie retournée par le DAO
        // car cette méthode a sa propre logique.
        Categorie mockCategorie = mock(Categorie.class);
        when(mockCategorie.getEvent()).thenReturn(event); // Assurer la cohérence
        when(mockCategorie.getPlaceDisponible()).thenReturn(10); // Il reste des places
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(mockCategorie));

        // 6. On simule la sauvegarde dans le DAO, qui retourne la réservation avec un ID
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation res = invocation.getArgument(0);
            res.setId(999); // Simuler l'attribution d'un ID par la BDD
            return res;
        });

        // Act
        Reservation result = reservationService.createMyReservation(event.getId(), categorie.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRME);
        assertThat(result.getMembre()).isEqualTo(membre);
        // Vérifier que la méthode save a bien été appelée une fois. [2]
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("createMyReservation - Doit lancer IllegalStateException si la limite de réservation est atteinte")
    void createMyReservation_ShouldThrowIllegalStateException_WhenReservationLimitIsReached() {
        // Arrange
        // On configure les mocks pour les premières étapes
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(categorie));
        doNothing().when(securityService).checkMemberOfEventClubOrThrow(event.getId());

        // La condition clé : le DAO rapporte que le membre a déjà 2 réservations (la limite) [1][3]
        when(reservationRepository.countByMembreIdAndEventIdAndStatus(membre.getId(), event.getId(), ReservationStatus.CONFIRME)).thenReturn(2L);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reservationService.createMyReservation(event.getId(), categorie.getId());
        });

        assertThat(exception.getMessage()).contains("Limite de 2 réservations confirmées par membre atteinte");
        verify(reservationRepository, never()).save(any()); // Vérifier que save n'est jamais appelée [2]
    }

    @Test
    @DisplayName("createMyReservation - Doit lancer IllegalStateException si la capacité de la catégorie est nulle")
    void createMyReservation_ShouldThrowIllegalStateException_WhenNoCapacity() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));
        when(reservationRepository.countByMembreIdAndEventIdAndStatus(anyInt(), anyInt(), any())).thenReturn(0L); // Pas de réservation existante
        doNothing().when(securityService).checkMemberOfEventClubOrThrow(event.getId());

        // La condition clé : on mock la catégorie pour qu'elle n'ait plus de place
        Categorie mockCategoriePleine = mock(Categorie.class);
        when(mockCategoriePleine.getEvent()).thenReturn(event);
        when(mockCategoriePleine.getPlaceDisponible()).thenReturn(0);
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(mockCategoriePleine));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reservationService.createMyReservation(event.getId(), categorie.getId());
        });

        assertThat(exception.getMessage()).contains("Capacité maximale (confirmée) atteinte");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMyReservation - Doit lancer EntityNotFoundException si la catégorie n'existe pas")
    void createMyReservation_ShouldThrowEntityNotFoundException_WhenCategorieDoesNotExist() {
        // Arrange
        // 1. On prépare la première étape : trouver l'utilisateur courant.
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());

        // --- CORRECTION APPORTÉE ICI ---
        // 2. On prépare la deuxième étape : s'assurer que le membre est trouvé.
        //    Sans cette ligne, le service échouerait ici et ne testerait jamais la catégorie.
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));

        // 3. On prépare l'étape que l'on veut réellement tester : la catégorie n'est PAS trouvée.
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            reservationService.createMyReservation(event.getId(), categorie.getId());
        });

        // Assertion supplémentaire pour être sûr que c'est bien la bonne exception
        assertThat(exception.getMessage()).contains("Catégorie non trouvée");
    }


    @Test
    @DisplayName("createMyReservation - Doit lancer IllegalArgumentException si l'ID de l'événement ne correspond pas à celui de la catégorie")
    void createMyReservation_ShouldThrowIllegalArgumentException_WhenEventIdMismatch() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(categorie)); // La catégorie est liée à eventId 1000

        // Act & Assert
        // On appelle le service avec un eventId différent (9999)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createMyReservation(9999, categorie.getId());
        });

        assertThat(exception.getMessage()).contains("n'appartient pas à l'événement");
    }

    // --- AUTRES TESTS IMPORTANTS ---

    @Test
    @DisplayName("cancelReservationById - Doit réussir quand les conditions sont valides")
    void cancelReservationById_ShouldSucceed_WhenConditionsAreValid() {
        // Arrange
        Reservation reservation = new Reservation(membre, event, categorie);
        reservation.setStatus(ReservationStatus.CONFIRME);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        doNothing().when(securityService).checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation);

        // Act
        reservationService.cancelReservationById(1);

        // Assert
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ANNULE);
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    @DisplayName("cancelReservationById - Doit lancer IllegalStateException si l'événement a déjà commencé")
    void cancelReservationById_ShouldThrowIllegalStateException_WhenEventHasStarted() {
        // Arrange
        event.setStartTime(Instant.now().minus(1, ChronoUnit.HOURS)); // L'événement est dans le passé
        Reservation reservation = new Reservation(membre, event, categorie);
        reservation.setStatus(ReservationStatus.CONFIRME);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        doNothing().when(securityService).checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> reservationService.cancelReservationById(1));
    }
}

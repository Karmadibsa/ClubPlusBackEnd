package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import org.clubplus.clubplusbackend.dao.CategorieDao;
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

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationDao reservationRepository;
    @Mock
    private MembreDao membreRepository;
    @Mock
    private CategorieDao categorieRepository;
    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ReservationService reservationService;

    // Données de test réutilisables
    private Membre membre;
    private Event event;
    private Categorie categorie; // Conteneur de données simple
    private Club club;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.setId(1);

        membre = new Membre();
        membre.setId(100);

        event = new Event();
        event.setId(1000);
        event.setActif(true);
        event.setStartTime(Instant.now().plus(10, ChronoUnit.DAYS)); // Événement futur
        event.setOrganisateur(club);

        categorie = new Categorie();
        categorie.setId(500);
        categorie.setEvent(event);
    }

    @Test
    @DisplayName("createMyReservation - Doit réussir quand toutes les conditions sont remplies")
    void createMyReservation_ShouldSucceed_WhenAllConditionsAreMet() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));

        Categorie mockCategorie = mock(Categorie.class);
        when(mockCategorie.getEvent()).thenReturn(event);
        when(mockCategorie.getPlaceDisponible()).thenReturn(10); // Il y a de la place
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(mockCategorie));

        // CORRECTION : Nom de la méthode corrigé ici
        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(event.getId());

        when(reservationRepository.countByMembreIdAndEventIdAndStatus(membre.getId(), event.getId(), ReservationStatus.CONFIRME)).thenReturn(0L);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation res = invocation.getArgument(0);
            res.setId(999);
            return res;
        });

        // Act
        Reservation result = reservationService.createMyReservation(event.getId(), categorie.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRME);
        assertThat(result.getMembre()).isEqualTo(membre);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("createMyReservation - Doit lancer IllegalStateException si la limite de réservation est atteinte")
    void createMyReservation_ShouldThrowIllegalStateException_WhenReservationLimitIsReached() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));

        Categorie mockCategorie = mock(Categorie.class);
        when(mockCategorie.getEvent()).thenReturn(event);
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(mockCategorie));

        // CORRECTION : Nom de la méthode corrigé ici
        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(event.getId());

        when(reservationRepository.countByMembreIdAndEventIdAndStatus(membre.getId(), event.getId(), ReservationStatus.CONFIRME)).thenReturn(2L);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reservationService.createMyReservation(event.getId(), categorie.getId());
        });

        assertThat(exception.getMessage()).contains("Limite de 2 réservations confirmées atteinte");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMyReservation - Doit lancer IllegalStateException si la capacité de la catégorie est nulle")
    void createMyReservation_ShouldThrowIllegalStateException_WhenNoCapacity() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));
        when(reservationRepository.countByMembreIdAndEventIdAndStatus(anyInt(), anyInt(), any())).thenReturn(0L);

        // CORRECTION : Nom de la méthode corrigé ici
        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(event.getId());

        Categorie mockCategoriePleine = mock(Categorie.class);
        when(mockCategoriePleine.getEvent()).thenReturn(event);
        when(mockCategoriePleine.getPlaceDisponible()).thenReturn(0); // Pas de place
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(mockCategoriePleine));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reservationService.createMyReservation(event.getId(), categorie.getId());
        });

        assertThat(exception.getMessage()).contains("Capacité maximale atteinte");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMyReservation - Doit lancer EntityNotFoundException si la catégorie n'existe pas")
    void createMyReservation_ShouldThrowEntityNotFoundException_WhenCategorieDoesNotExist() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));

        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            reservationService.createMyReservation(event.getId(), categorie.getId());
        });

        assertThat(exception.getMessage()).contains("Catégorie non trouvée");
    }


    @Test
    @DisplayName("createMyReservation - Doit lancer IllegalArgumentException si l'ID de l'événement ne correspond pas")
    void createMyReservation_ShouldThrowIllegalArgumentException_WhenEventIdMismatch() {
        // Arrange
        when(securityService.getCurrentUserIdOrThrow()).thenReturn(membre.getId());
        when(membreRepository.findById(membre.getId())).thenReturn(Optional.of(membre));

        Categorie mockCategorie = mock(Categorie.class);
        when(mockCategorie.getEvent()).thenReturn(event);
        when(categorieRepository.findById(categorie.getId())).thenReturn(Optional.of(mockCategorie));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createMyReservation(9999, categorie.getId());
        });

        assertThat(exception.getMessage()).contains("n'appartient pas à l'événement");
    }

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

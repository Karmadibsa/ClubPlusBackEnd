package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.dao.ReservationDao;
import org.clubplus.clubplusbackend.model.*;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service gérant la logique métier liée aux réservations d'événements.
 * <p>
 * Ce service orchestre la création, la consultation et la modification du statut des réservations,
 * en appliquant les règles de gestion (limites, capacité, etc.) et de sécurité.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationDao reservationRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    private final CategorieDao categorieRepository;
    private final SecurityService securityService;

    private static final int RESERVATION_MAX_PER_EVENT_PER_MEMBER = 2;

    /**
     * Crée une nouvelle réservation pour l'utilisateur courant.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'événement doit être actif et futur.</li>
     * <li>L'utilisateur doit être membre du club organisateur.</li>
     * <li>La limite de réservations par membre pour l'événement ne doit pas être dépassée.</li>
     * <li>La catégorie doit avoir des places disponibles.</li>
     * </ul>
     *
     * @param eventId     L'ID de l'événement.
     * @param categorieId L'ID de la catégorie à réserver.
     * @return La nouvelle réservation créée.
     * @throws EntityNotFoundException  si une entité requise n'est pas trouvée.
     * @throws AccessDeniedException    si l'utilisateur n'est pas membre du club.
     * @throws IllegalStateException    si une règle métier est violée (événement passé, complet, etc.).
     * @throws IllegalArgumentException si la catégorie n'appartient pas à l'événement.
     */
    public Reservation createMyReservation(Integer eventId, Integer categorieId) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();
        Membre membre = membreRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Membre courant non trouvé (ID: " + currentUserId + ")"));
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId + ")"));
        Event event = categorie.getEvent();

        // Validations
        if (event == null) {
            throw new EntityNotFoundException("Événement lié à la catégorie (ID: " + categorieId + ") non trouvé.");
        }
        if (!Objects.equals(event.getId(), eventId)) {
            throw new IllegalArgumentException("La catégorie (ID " + categorieId + ") n'appartient pas à l'événement (ID " + eventId + ").");
        }
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de réserver : l'événement (ID: " + eventId + ") est annulé.");
        }
        if (event.getStartTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Impossible de réserver : l'événement (ID: " + eventId + ") est déjà commencé ou passé.");
        }

        securityService.checkMemberOfEventClubOrThrow(eventId);

        long existingReservationsCount = reservationRepository.countByMembreIdAndEventIdAndStatus(currentUserId, eventId, ReservationStatus.CONFIRME);
        if (existingReservationsCount >= RESERVATION_MAX_PER_EVENT_PER_MEMBER) {
            throw new IllegalStateException("Limite de " + RESERVATION_MAX_PER_EVENT_PER_MEMBER + " réservations confirmées atteinte pour cet événement.");
        }

        if (categorie.getPlaceDisponible() <= 0) {
            throw new IllegalStateException("Capacité maximale atteinte pour la catégorie.");
        }

        Reservation newReservation = new Reservation(membre, event, categorie);
        return reservationRepository.save(newReservation);
    }

    /**
     * Récupère une réservation par son ID, en vérifiant les droits d'accès.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être le propriétaire de la réservation ou un gestionnaire du club organisateur.
     *
     * @param reservationId L'ID de la réservation.
     * @return L'entité {@link Reservation}.
     * @throws EntityNotFoundException si la réservation n'est pas trouvée.
     * @throws AccessDeniedException   si l'accès est refusé.
     */
    @Transactional(readOnly = true)
    public Reservation getReservationByIdWithSecurityCheck(Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")"));

        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation);

        return reservation;
    }

    /**
     * Récupère les réservations de l'utilisateur courant, avec un filtre optionnel par statut.
     *
     * @param statusFilter Filtre "CONFIRME", "UTILISE", "ANNULE". Si null ou invalide, retourne tout.
     * @return La liste des réservations de l'utilisateur.
     */
    @Transactional(readOnly = true)
    public List<Reservation> findMyReservations(String statusFilter) {
        Integer currentUserId = securityService.getCurrentUserIdOrThrow();

        ReservationStatus status = null;
        try {
            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Statut de réservation invalide fourni pour findMyReservations: '{}'. Retourne une liste vide.", statusFilter);
            return Collections.emptyList();
        }

        if (status != null) {
            return reservationRepository.findByMembreIdAndStatusAndEvent_EndTimeAfter(currentUserId, status, Instant.now());
        } else {
            return reservationRepository.findByMembreIdAndEvent_EndTimeAfter(currentUserId, Instant.now());
        }
    }

    /**
     * Récupère les réservations pour un événement, avec vérification des droits et filtre de statut.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club organisateur.
     *
     * @param eventId      L'ID de l'événement.
     * @param statusFilter Filtre de statut.
     * @return Une liste de réservations pour l'événement.
     */
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsByEventIdWithSecurityCheck(Integer eventId, String statusFilter) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé (ID: " + eventId + ")"));

        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        ReservationStatus status = null;
        try {
            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                status = ReservationStatus.valueOf(statusFilter.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Statut de réservation invalide pour findReservationsByEventId: '{}'. Retourne une liste vide.", statusFilter);
            return Collections.emptyList();
        }

        if (status != null) {
            return reservationRepository.findByEventIdAndStatus(eventId, status);
        } else {
            return reservationRepository.findByEventId(eventId);
        }
    }

    /**
     * Récupère les réservations pour une catégorie, avec vérification des droits.
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club organisateur.
     *
     * @param categorieId L'ID de la catégorie.
     * @return Une liste de réservations pour la catégorie.
     */
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsByCategorieIdWithSecurityCheck(Integer categorieId) {
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie non trouvée (ID: " + categorieId + ")"));

        if (categorie.getEvent() == null || categorie.getEvent().getOrganisateur() == null) {
            throw new EntityNotFoundException("Impossible de vérifier les droits : événement ou organisateur non trouvé pour la catégorie (ID: " + categorieId + ")");
        }
        securityService.checkManagerOfClubOrThrow(categorie.getEvent().getOrganisateur().getId());

        return reservationRepository.findByCategorieId(categorieId);
    }

    /**
     * Annule une réservation spécifique.
     * <p>
     * <b>Sécurité :</b> L'action peut être effectuée par le propriétaire de la réservation ou un gestionnaire du club.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'événement ne doit pas être déjà commencé.</li>
     * <li>La réservation doit être au statut {@code CONFIRME}.</li>
     * </ul>
     *
     * @param reservationId L'ID de la réservation à annuler.
     */
    public void cancelReservationById(Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée (ID: " + reservationId + ")"));

        securityService.checkIsOwnerOrManagerOfAssociatedClubOrThrow(reservation);

        Event event = reservation.getEvent();
        if (event == null || event.getStartTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Annulation impossible : l'événement est déjà commencé ou passé.");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRME) {
            throw new IllegalStateException("Annulation impossible : la réservation n'est pas au statut CONFIRME.");
        }

        reservation.setStatus(ReservationStatus.ANNULE);
        reservationRepository.save(reservation);
    }

    /**
     * Marque une réservation comme 'UTILISE', typiquement via son UUID (QR Code).
     * <p>
     * <b>Sécurité :</b> L'utilisateur doit être un gestionnaire du club organisateur.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'action doit avoir lieu pendant la fenêtre de validation de l'événement (1h avant le début jusqu'à la fin).</li>
     * <li>La réservation doit être au statut {@code CONFIRME}.</li>
     * </ul>
     *
     * @param reservationUuid L'UUID de la réservation.
     * @return La réservation mise à jour.
     */
    public Reservation markReservationAsUsed(String reservationUuid) {
        Reservation reservation = reservationRepository.findByReservationUuid(reservationUuid)
                .orElseThrow(() -> new EntityNotFoundException("Réservation non trouvée avec l'UUID : " + reservationUuid));

        Event event = reservation.getEvent();
        if (event == null || event.getOrganisateur() == null) {
            throw new EntityNotFoundException("Impossible de vérifier les droits : événement ou organisateur non trouvé pour la réservation.");
        }
        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de marquer comme utilisée : l'événement lié est annulé.");
        }

        Instant now = Instant.now();
        Instant eventStart = event.getStartTime();
        Instant eventEnd = event.getEndTime();

        Instant scanWindowStart = eventStart.minus(1, ChronoUnit.HOURS);

        if (now.isBefore(scanWindowStart) || now.isAfter(eventEnd)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'le' dd/MM/yyyy").withZone(ZoneId.systemDefault());
            throw new IllegalStateException("Validation non autorisée. Fenêtre de validation : de "
                    + formatter.format(scanWindowStart) + " à "
                    + formatter.format(eventEnd) + ".");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRME) {
            throw new IllegalStateException("Impossible de marquer comme utilisée : la réservation n'est pas au statut CONFIRME.");
        }

        reservation.setStatus(ReservationStatus.UTILISE);
        return reservationRepository.save(reservation);
    }
}

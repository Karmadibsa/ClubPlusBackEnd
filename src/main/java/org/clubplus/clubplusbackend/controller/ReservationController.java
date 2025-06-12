package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ReservationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les opérations liées aux entités {@link Reservation}.
 * Fournit des endpoints pour la création, la lecture (individuelle, listes filtrées),
 * l'annulation et le marquage comme 'utilisé' des réservations.
 * La sécurité est appliquée via des annotations personnalisées et des vérifications
 * contextuelles dans le service {@link ReservationService}.
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /reservations} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see Reservation
 * @see ReservationService
 */
@RestController
@RequestMapping("/reservations") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor
@CrossOrigin // Activation de CORS si nécessaire
public class ReservationController {

    /**
     * Service contenant la logique métier pour la gestion des réservations.
     *
     * @see ReservationService
     */
    private final ReservationService reservationService;

    /**
     * Crée une nouvelle réservation pour l'utilisateur actuellement authentifié pour une catégorie spécifique d'un événement.
     * <p>
     * <b>Requête:</b> POST /reservations?eventId={eventId}&amp;categorieId={categorieId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit au moins MEMBRE ({@link IsMembre @IsMembre}).
     * Le service vérifie l'appartenance au club, la limite de réservations, la capacité, etc.
     * </p>
     *
     * @param eventId     L'ID (Integer) de l'événement concerné. Fourni comme paramètre de requête.
     * @param categorieId L'ID (Integer) de la catégorie pour laquelle réserver. Fourni comme paramètre de requête.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> La nouvelle {@link Reservation} créée, sérialisée selon {@link GlobalView.ReservationView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement, la catégorie ou le membre courant n'est pas trouvé (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas membre du club organisateur (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'événement est annulé/passé, si la limite de réservation est atteinte, ou si la capacité est pleine (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si la catégorie n'appartient pas à l'événement (levé par le service via {@link IllegalArgumentException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsMembre}).</li>
     * </ul>
     * @see ReservationService#createMyReservation(Integer, Integer)
     * @see GlobalView.ReservationView
     * @see IsMembre
     */
    @PostMapping
    @IsMembre // Requiert au moins le rôle MEMBRE
    @JsonView(GlobalView.ReservationView.class) // Vue JSON pour la réservation créée
    public ResponseEntity<Reservation> createMyReservation(@RequestParam Integer eventId,
                                                           @RequestParam Integer categorieId) {
        // @ResponseStatus retiré, HttpStatus.CREATED géré par ResponseEntity
        // Le service gère existence (-> 404), sécurité (-> 403), règles métier (-> 400, 409)
        Reservation newReservation = reservationService.createMyReservation(eventId, categorieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newReservation);
    }

    /**
     * Récupère la liste des réservations effectuées par l'utilisateur actuellement authentifié.
     * Permet de filtrer par statut.
     * <p>
     * <b>Requête:</b> GET /reservations/me?status={status}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsMembre @IsMembre} - même si @IsConnected suffirait techniquement, IsMembre est cohérent).
     * </p>
     *
     * @param status (Optionnel) Filtre (String) sur le statut ('CONFIRME', 'UTILISE', 'ANNULE', etc.).
     *               Si invalide, le service retourne une liste vide.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Reservation>} des réservations de l'utilisateur, filtrée par statut,
     *         sérialisée selon {@link GlobalView.ReservationView}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsMembre}).</li>
     * </ul>
     * @see ReservationService#findMyReservations(String)
     * @see GlobalView.ReservationView
     * @see IsMembre
     */
    @GetMapping("/me")
    @IsMembre // Cohérent avec les autres actions de l'utilisateur
    @JsonView(GlobalView.ReservationView.class) // Vue JSON pour la liste de réservations
    public ResponseEntity<List<Reservation>> getMyReservations(@RequestParam(required = false) String status) {
        List<Reservation> reservations = reservationService.findMyReservations(status);
        System.out.println(reservations);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Récupère une réservation spécifique par son identifiant numérique (ID).
     * <p>
     * <b>Requête:</b> GET /reservations/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * Le service ({@link ReservationService#getReservationByIdWithSecurityCheck}) vérifie si
     * l'utilisateur est le propriétaire OU un gestionnaire du club organisateur.
     * </p>
     *
     * @param id L'ID (Integer) de la réservation à récupérer. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> La {@link Reservation} trouvée, sérialisée selon {@link GlobalView.ReservationView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la réservation n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est ni le propriétaire, ni un gestionnaire autorisé (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected}).</li>
     * </ul>
     * @see ReservationService#getReservationByIdWithSecurityCheck(Integer)
     * @see GlobalView.ReservationView
     * @see IsConnected
     */
    @GetMapping("/{id}")
    @IsConnected // Rôle minimum pour vérifier si on est propriétaire ou manager
    @JsonView(GlobalView.ReservationView.class) // Vue JSON pour les détails
    public ResponseEntity<Reservation> getReservationById(@PathVariable Integer id) {
        // Service gère existence (-> 404) et sécurité (propriétaire/manager -> 403)
        Reservation reservation = reservationService.getReservationByIdWithSecurityCheck(id);
        return ResponseEntity.ok(reservation);
    }

    /**
     * Récupère la liste des réservations pour un événement spécifique. Réservé aux gestionnaires du club.
     * Permet de filtrer par statut.
     * <p>
     * <b>Requête:</b> GET /reservations/event/{eventId}?status={status}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link ReservationService#findReservationsByEventIdWithSecurityCheck}) vérifie
     * que l'utilisateur est gestionnaire du club organisateur.
     * </p>
     *
     * @param eventId L'ID (Integer) de l'événement concerné. Provient du chemin de l'URL.
     * @param status  (Optionnel) Filtre (String) sur le statut des réservations. Si invalide, retourne liste vide.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Reservation>} des réservations pour l'événement, filtrée,
     *         sérialisée selon {@link GlobalView.ReservationView}. Peut être vide.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire du club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see ReservationService#findReservationsByEventIdWithSecurityCheck(Integer, String)
     * @see GlobalView.ReservationView
     * @see IsReservation
     */
    @GetMapping("/event/{eventId}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.ReservationView.class) // Vue JSON pour la liste
    public ResponseEntity<List<Reservation>> getReservationsByEvent(@PathVariable Integer eventId,
                                                                    @RequestParam(required = false) String status) {
        // Service gère existence événement (-> 404), sécurité (manager -> 403), filtre statut
        List<Reservation> reservations = reservationService.findReservationsByEventIdWithSecurityCheck(eventId, status);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Récupère la liste des réservations pour une catégorie d'événement spécifique. Réservé aux gestionnaires du club.
     * <p>
     * <b>Requête:</b> GET /reservations/categorie/{categorieId}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link ReservationService#findReservationsByCategorieIdWithSecurityCheck}) vérifie
     * que l'utilisateur est gestionnaire du club (via l'événement de la catégorie).
     * </p>
     *
     * @param categorieId L'ID (Integer) de la catégorie concernée. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Reservation>} des réservations pour la catégorie,
     *         sérialisée selon {@link GlobalView.ReservationView}. Peut être vide.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la catégorie n'existe pas ou si les liens vers l'événement/club sont brisés (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire du club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see ReservationService#findReservationsByCategorieIdWithSecurityCheck(Integer)
     * @see GlobalView.ReservationView
     * @see IsReservation
     */
    @GetMapping("/categorie/{categorieId}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.ReservationView.class) // Vue JSON pour la liste
    public ResponseEntity<List<Reservation>> getReservationsByCategorie(@PathVariable Integer categorieId) {
        // Service gère existence catégorie (-> 404) et sécurité (manager -> 403)
        List<Reservation> reservations = reservationService.findReservationsByCategorieIdWithSecurityCheck(categorieId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Annule une réservation spécifique en passant son statut à {@code ANNULE}.
     * L'action peut être effectuée par le propriétaire de la réservation ou un gestionnaire du club.
     * <p>
     * <b>Requête:</b> PUT /reservations/{id}/cancel
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié ({@link IsConnected @IsConnected}).
     * Le service ({@link ReservationService#cancelReservationById}) vérifie que l'utilisateur
     * est le propriétaire OU un gestionnaire du club organisateur.
     * </p>
     *
     * @param id L'ID (Integer) de la réservation à annuler. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * (Modification de la version précédente pour être plus conforme aux standards REST pour une action sans retour de contenu).
     * @throws EntityNotFoundException Si la réservation n'existe pas (levé par le service -> 404).
     * @throws AccessDeniedException   Si l'utilisateur n'est ni le propriétaire, ni un gestionnaire autorisé (levé par le service -> 403).
     * @throws IllegalStateException   Si l'événement est déjà passé ou si la réservation n'est pas au statut 'CONFIRME' (levé par le service -> 409).
     * @throws AuthenticationException Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsConnected} -> 401).
     * @see ReservationService#cancelReservationById(Integer)
     * @see IsConnected
     */
    @PutMapping("/{id}/cancel")
    @IsConnected // Rôle minimum pour tenter (vérification propriétaire/manager dans le service)
    public ResponseEntity<Void> cancelReservation(@PathVariable Integer id) {
        // @ResponseStatus retiré, géré par ResponseEntity
        // Le service gère existence (-> 404), sécurité (-> 403), règles métier (-> 409)
        reservationService.cancelReservationById(id);
        // Retourne 204 No Content car l'action modifie l'état mais ne retourne pas de représentation spécifique.
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque une réservation comme 'UTILISE' via son identifiant UUID unique (typiquement scanné depuis un QR code).
     * Réservé aux gestionnaires du club organisateur pendant la fenêtre de validation de l'événement.
     * <p>
     * <b>Requête:</b> PUT /reservations/uuid/{uuid}/use
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link ReservationService#markReservationAsUsed}) vérifie que l'utilisateur
     * est gestionnaire du club et que l'action a lieu pendant la bonne fenêtre temporelle.
     * </p>
     *
     * @param uuid L'UUID (String) de la réservation à marquer comme utilisée. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> La {@link Reservation} mise à jour avec le statut 'UTILISE', sérialisée selon {@link GlobalView.ReservationView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si la réservation avec cet UUID n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire du club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'événement est annulé, si l'action est hors fenêtre de validation, ou si la réservation n'est pas 'CONFIRME' (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see ReservationService#markReservationAsUsed(String)
     * @see GlobalView.ReservationView
     * @see IsReservation
     */
    @PatchMapping("/uuid/{uuid}/use")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.ReservationView.class) // Vue JSON pour la réservation mise à jour
    public ResponseEntity<Reservation> markReservationUsed(@PathVariable String uuid) {
        // @ResponseStatus retiré, géré par ResponseEntity
        // Le service gère existence (-> 404), sécurité (-> 403), règles métier (-> 409)
        Reservation updatedReservation = reservationService.markReservationAsUsed(uuid);
        return ResponseEntity.ok(updatedReservation);
    }

}

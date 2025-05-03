package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateNotationDto;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Notation;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.NotationService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les opérations liées aux notations ({@link Notation}) des événements.
 * Les endpoints sont généralement imbriqués sous le chemin des événements.
 * Fournit des opérations pour créer et lire des notations, ainsi que pour lister
 * les événements qu'un utilisateur peut noter.
 * La sécurité est appliquée via des annotations personnalisées et des vérifications
 * contextuelles dans le service {@link NotationService}.
 * <p>
 * Le chemin de base pour les opérations sur les notations d'un événement spécifique est
 * {@code /events/{eventId}/notations} (le préfixe global `/api` est supposé être
 * configuré via {@code server.servlet.context-path=/api}).
 * Un endpoint spécifique existe également pour lister les événements non notés par l'utilisateur courant.
 * </p>
 *
 * @see Notation
 * @see Event
 * @see NotationService
 */
@RestController
// Le mapping de base est /events, les notations sont des sous-ressources
@RequestMapping("/events") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor
@CrossOrigin // Activation de CORS si nécessaire
public class NotationController {

    /**
     * Service contenant la logique métier pour la gestion des notations.
     *
     * @see NotationService
     */
    private final NotationService notationService;

    /**
     * Crée une nouvelle notation pour un événement spécifique, soumise par l'utilisateur actuellement authentifié.
     * <p>
     * <b>Requête:</b> POST /events/{eventId}/notations
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * Le service vérifie les règles métier (événement terminé, participation 'UTILISE', non déjà noté).
     * </p>
     * <p>
     * <b>Validation:</b> Le corps de la requête ({@link CreateNotationDto}) est validé via {@link Valid @Valid}.
     * </p>
     *
     * @param eventId     L'ID (Integer) de l'événement à noter. Provient du chemin de l'URL.
     * @param notationDto Un DTO contenant les notes attribuées. Reçu dans le corps et validé.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> La nouvelle {@link Notation} créée, sérialisée selon {@link GlobalView.NotationView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code notationDto} sont invalides (levé par {@link MethodArgumentNotValidException}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement ou l'utilisateur courant n'est pas trouvé (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'événement n'est pas terminé, si l'utilisateur n'a pas participé (statut UTILISE),
     *         ou s'il a déjà noté cet événement (levé par le service via {@link IllegalStateException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsMembre}).</li>
     * </ul>
     * @see NotationService#createMyNotation(CreateNotationDto, Integer)
     * @see CreateNotationDto
     * @see GlobalView.NotationView
     * @see IsMembre
     * @see Valid
     */
    @PostMapping("/{eventId}/notations")
    @IsMembre // Requiert rôle MEMBRE
    @JsonView(GlobalView.NotationView.class) // Vue JSON pour la notation créée
    public ResponseEntity<Notation> createMyNotation(@PathVariable Integer eventId,
                                                     @Valid @RequestBody CreateNotationDto notationDto) {
        // @ResponseStatus retiré, HttpStatus.CREATED géré par ResponseEntity
        // @Valid -> 400
        // Service gère existence (-> 404), règles métier (-> 409)
        Notation newNotation = notationService.createMyNotation(notationDto, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newNotation);
    }

    /**
     * Récupère la liste des notations soumises pour un événement spécifique.
     * L'accès est restreint aux gestionnaires du club organisateur. Les notations sont anonymisées.
     * <p>
     * <b>Requête:</b> GET /events/{eventId}/notations
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link NotationService#findNotationsByEventIdWithSecurityCheck}) vérifie
     * que l'utilisateur est gestionnaire du club organisateur.
     * </p>
     *
     * @param eventId L'ID (Integer) de l'événement dont les notations sont demandées. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Notation>} des notations pour l'événement,
     *         sérialisée selon {@link GlobalView.NotationView} (qui doit masquer l'auteur). Peut être vide.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'événement n'existe pas (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas gestionnaire du club organisateur (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié ou n'a pas le rôle requis (géré par Spring Security / {@code @IsReservation}).</li>
     * </ul>
     * @see NotationService#findNotationsByEventIdWithSecurityCheck(Integer)
     * @see GlobalView.NotationView
     * @see IsReservation
     */
    @GetMapping("/{eventId}/notations")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.NotationView.class) // Vue JSON (doit anonymiser)
    public ResponseEntity<List<Notation>> getNotationsByEvent(@PathVariable Integer eventId) {
        // Le service gère existence (-> 404) et sécurité (manager -> 403)
        List<Notation> notations = notationService.findNotationsByEventIdWithSecurityCheck(eventId);
        return ResponseEntity.ok(notations);
    }

    /**
     * Récupère la liste des événements auxquels l'utilisateur actuellement authentifié a participé
     * (réservation statut 'UTILISE') mais qu'il n'a pas encore notés.
     * <p>
     * <b>Requête:</b> GET /events/notations/me/participated-events-unrated
     * (Note: Ce chemin est relatif à '/events' défini au niveau de la classe).
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit authentifié avec le rôle MEMBRE ({@link IsMembre @IsMembre}).
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Event>} des événements non notés, sérialisée selon {@link GlobalView.Base}. Peut être vide.</li>
     *     <li><b>Erreur (401 Unauthorized):</b> Si l'utilisateur n'est pas authentifié (géré par Spring Security / {@code @IsMembre}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si l'utilisateur authentifié n'est pas trouvé en base (peu probable, levé par le service via {@link EntityNotFoundException}).</li>
     * </ul>
     * @see NotationService#getUnratedParticipatedEvents()
     * @see GlobalView.Base
     * @see IsMembre
     */
    // Le chemin est bien /events/notations/me/participated-events-unrated car il est relatif à /events
    @GetMapping("/notations/me/participated-events-unrated")
    @IsMembre // Requiert rôle MEMBRE
    @JsonView(GlobalView.Base.class) // Vue de base pour la liste d'événements
    public ResponseEntity<List<Event>> getMyUnratedEvents() {
        List<Event> events = notationService.getUnratedParticipatedEvents();
        return ResponseEntity.ok(events);
    }

    // Rappel : La gestion centralisée des exceptions via @ControllerAdvice est essentielle
    // pour mapper les exceptions (EntityNotFoundException, AccessDeniedException, IllegalStateException,
    // IllegalArgumentException, MethodArgumentNotValidException) aux bonnes réponses HTTP (404, 403, 409, 400).
}

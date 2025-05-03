package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST gérant les opérations relatives aux entités {@link Club}.
 * Fournit des endpoints pour la création, la lecture (individuelle, liste),
 * la mise à jour et la suppression (désactivation) des clubs. Gère également la récupération
 * des membres et des événements associés à un club spécifique.
 * La sécurité est appliquée via des annotations personnalisées et des vérifications
 * contextuelles dans les services appelés.
 * <p>
 * Le chemin de base pour ce contrôleur est {@code /clubs} (le préfixe global `/api`
 * est supposé être configuré via {@code server.servlet.context-path=/api}).
 * </p>
 *
 * @see Club
 * @see ClubService
 * @see EventService
 */
@RestController
@RequestMapping("/clubs") // Chemin de base SANS /api (configuré globalement)
@RequiredArgsConstructor // Injection via constructeur pour les champs final (Lombok)
@CrossOrigin // Activation de CORS (peut être géré globalement)
public class ClubController {

    /**
     * Service pour la logique métier liée aux clubs.
     *
     * @see ClubService
     */
    private final ClubService clubService;

    /**
     * Service pour la logique métier liée aux événements (utilisé pour lister les événements d'un club).
     *
     * @see EventService
     */
    private final EventService eventService;

    /**
     * Récupère les détails complets d'un club spécifique par son identifiant numérique.
     * <p>
     * <b>Requête:</b> GET /clubs/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link ClubService#getClubByIdWithSecurityCheck}) vérifie en plus
     * que l'utilisateur est membre du club concerné.
     * </p>
     *
     * @param id L'identifiant unique (Integer) du club à récupérer. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> L'objet {@link Club} complet correspondant à l'ID fourni, sérialisé selon {@link GlobalView.ClubView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si aucun club avec cet ID n'est trouvé (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'a pas le rôle requis ou n'est pas membre du club (levé par le service via {@link AccessDeniedException}).</li>
     * </ul>
     * @see ClubService#getClubByIdWithSecurityCheck(Integer)
     * @see GlobalView.ClubView
     * @see IsReservation
     */
    @GetMapping("/{id}")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club
    public ResponseEntity<Club> getClubById(@PathVariable Integer id) {
        // Le service gère la recherche, l'existence et la vérification de l'appartenance/rôle.
        Club club = clubService.getClubByIdWithSecurityCheck(id);
        return ResponseEntity.ok(club);
    }

    /**
     * Crée un nouveau club ainsi que son membre administrateur initial.
     * <p>
     * <b>Requête:</b> POST /clubs/inscription
     * </p>
     * <p>
     * <b>Sécurité:</b> Aucune annotation de sécurité spécifique ici. L'accès est potentiellement
     * public ou restreint par la configuration globale de Spring Security.
     * </p>
     * <p>
     * <b>Validation:</b> Les données reçues dans le DTO ({@link CreateClubRequestDto}) sont validées via {@link Valid @Valid}.
     * </p>
     *
     * @param creationDto Un DTO contenant les infos du club et de l'admin initial. Validé par {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (201 Created):</b> Le {@link Club} nouvellement créé, sérialisé selon {@link GlobalView.ClubView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code creationDto} sont invalides (levé par {@link MethodArgumentNotValidException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si l'email fourni pour l'admin ou le club est déjà utilisé (levé par le service via {@link IllegalArgumentException}).</li>
     * </ul>
     * @see ClubService#createClubAndRegisterAdmin(CreateClubRequestDto)
     * @see CreateClubRequestDto
     * @see GlobalView.ClubView
     * @see Valid
     */
    @PostMapping("/inscription")
    // @ResponseStatus retiré, géré par ResponseEntity
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club créé
    public ResponseEntity<Club> createClubAndAdmin(
            @Valid @RequestBody CreateClubRequestDto creationDto // Valide le DTO reçu
    ) {
        // @Valid gère la validation -> 400 si échec.
        // Le service gère la logique de création et les conflits potentiels (ex: email) -> 409.
        Club newClub = clubService.createClubAndRegisterAdmin(creationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newClub);
    }

    /**
     * Met à jour les informations d'un club existant.
     * <p>
     * <b>Requête:</b> PUT /clubs/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert le rôle ADMIN ({@link IsAdmin @IsAdmin}).
     * Le service ({@link ClubService#updateClub}) vérifie en plus que l'utilisateur
     * est bien l'administrateur *spécifique* de ce club.
     * </p>
     * <p>
     * <b>Validation:</b> Les données reçues dans le DTO ({@link UpdateClubDto}) sont validées via {@link Valid @Valid}.
     * </p>
     *
     * @param id        L'identifiant unique (Integer) du club à mettre à jour. Provient du chemin de l'URL.
     * @param updateDto Un DTO contenant les nouvelles informations. Validé par {@code @Valid}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Le {@link Club} mis à jour, sérialisé selon {@link GlobalView.ClubView}.</li>
     *     <li><b>Erreur (400 Bad Request):</b> Si les données dans {@code updateDto} sont invalides (levé par {@link MethodArgumentNotValidException}).</li>
     *     <li><b>Erreur (404 Not Found):</b> Si aucun club avec cet ID n'est trouvé (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'est pas l'administrateur de ce club (levé par le service via {@link AccessDeniedException}).</li>
     *     <li><b>Erreur (409 Conflict):</b> Si le nouvel email du club est déjà utilisé par un autre club (levé par le service via {@link IllegalArgumentException}).</li>
     * </ul>
     * @see ClubService#updateClub(Integer, UpdateClubDto)
     * @see UpdateClubDto
     * @see GlobalView.ClubView
     * @see IsAdmin
     * @see Valid
     */
    @PutMapping("/{id}")
    @IsAdmin // Requiert le rôle ADMIN
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club mis à jour
    public ResponseEntity<Club> updateClub(@PathVariable Integer id, @Valid @RequestBody UpdateClubDto updateDto) {
        // @Valid gère la validation -> 400.
        // Le service gère existence (-> 404), droits d'admin (-> 403), et conflits (email -> 409).
        Club updatedClub = clubService.updateClub(id, updateDto);
        return ResponseEntity.ok(updatedClub);
    }

    /**
     * Désactive (supprime logiquement) un club existant.
     * <p>
     * <b>Requête:</b> DELETE /clubs/{id}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert le rôle ADMIN ({@link IsAdmin @IsAdmin}).
     * Le service ({@link ClubService#deactivateClub}) vérifie en plus que l'utilisateur
     * est bien l'administrateur *spécifique* de ce club.
     * </p>
     * <p>
     * <b>Règle métier:</b> La désactivation peut être empêchée si le club a encore des événements futurs actifs.
     * </p>
     *
     * @param id L'identifiant unique (Integer) du club à désactiver. Provient du chemin de l'URL.
     * @return Une {@link ResponseEntity} vide avec le statut HTTP 204 (No Content) en cas de succès.
     * @throws EntityNotFoundException si aucun club avec cet ID n'est trouvé (levé par le service -> 404).
     * @throws AccessDeniedException   si l'utilisateur n'est pas l'administrateur de ce club (levé par le service -> 403).
     * @throws IllegalStateException   si la désactivation est impossible en raison d'événements futurs actifs (levé par le service -> 409).
     * @see ClubService#deactivateClub(Integer)
     * @see IsAdmin
     */
    @DeleteMapping("/{id}")
    @IsAdmin // Requiert le rôle ADMIN
    // @ResponseStatus retiré, géré par ResponseEntity
    public ResponseEntity<Void> deleteClub(@PathVariable Integer id) {
        // Le service gère existence (-> 404), droits d'admin (-> 403), et règles métier (-> 409).
        clubService.deactivateClub(id);
        return ResponseEntity.noContent().build(); // Retourne 204 No Content
    }

    /**
     * Récupère la liste des membres associés à un club spécifique.
     * <p>
     * <b>Requête:</b> GET /clubs/{id}/membres
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert un rôle RESERVATION ou ADMIN ({@link IsReservation @IsReservation}).
     * Le service ({@link ClubService#findMembresForClub}) vérifie en plus que l'utilisateur
     * est membre du club concerné.
     * </p>
     *
     * @param id L'identifiant unique (Integer) du club dont les membres doivent être récupérés.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Membre>} des membres, sérialisée selon {@link GlobalView.MembreView}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si aucun club avec cet ID n'est trouvé (levé par le service via {@link EntityNotFoundException}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur n'a pas le rôle requis ou n'est pas membre du club (levé par le service via {@link AccessDeniedException}).</li>
     * </ul>
     * @see ClubService#findMembresForClub(Integer)
     * @see GlobalView.MembreView
     * @see IsReservation
     */
    @GetMapping("/{id}/membres")
    @IsReservation // Requiert rôle RESERVATION ou ADMIN
    @JsonView(GlobalView.MembreView.class) // Vue JSON pour les membres listés
    public ResponseEntity<List<Membre>> getClubMembres(@PathVariable Integer id) {
        // Le service gère existence (-> 404) et sécurité d'accès (-> 403).
        Set<Membre> membresSet = clubService.findMembresForClub(id);
        // Conversion Set vers List pour la réponse JSON standard
        List<Membre> membresList = new ArrayList<>(membresSet);
        return ResponseEntity.ok(membresList);
    }

    // --- Endpoints liés aux événements (Délégation à EventService) ---

    /**
     * Récupère la liste des événements organisés par un club spécifique, avec un filtre optionnel par statut.
     * <p>
     * <b>Requête:</b> GET /clubs/{id}/events?status={status}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit connecté ({@link IsConnected @IsConnected}).
     * La vérification de l'appartenance au club est déléguée à {@link EventService#findEventsByOrganisateurWithSecurityCheck}.
     * </p>
     *
     * @param id     L'identifiant unique (Integer) du club organisateur.
     * @param status (Optionnel) Filtre (String) sur le statut des événements ('active', 'inactive', 'all').
     *               La logique est gérée par {@code EventService}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Event>} des événements, sérialisée selon {@link GlobalView.Base}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (peut être levé par {@code EventService}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur connecté n'est pas membre du club (levé par {@code EventService}).</li>
     * </ul>
     * @see EventService#findEventsByOrganisateurWithSecurityCheck(Integer, String)
     * @see GlobalView.Base
     * @see IsConnected
     */
    @GetMapping("/{id}/events")
    @IsConnected // Requiert que l'utilisateur soit connecté
    @JsonView(GlobalView.Base.class) // Vue JSON de base pour les événements listés
    public ResponseEntity<List<Event>> getClubEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        // Délégation à EventService qui gère la sécurité contextuelle (membre du club 'id').
        List<Event> events = eventService.findEventsByOrganisateurWithSecurityCheck(id, status);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupère la liste des événements *futurs* organisés par un club spécifique, avec un filtre optionnel par statut.
     * <p>
     * <b>Requête:</b> GET /clubs/{id}/events/upcoming?status={status}
     * </p>
     * <p>
     * <b>Sécurité:</b> Requiert que l'utilisateur soit connecté ({@link IsConnected @IsConnected}).
     * La vérification de l'appartenance au club est déléguée à {@link EventService#findUpcomingEventsByOrganisateurWithSecurityCheck}.
     * </p>
     *
     * @param id     L'identifiant unique (Integer) du club organisateur.
     * @param status (Optionnel) Filtre (String) sur le statut ('active', 'inactive', 'all').
     *               La logique est gérée par {@code EventService}.
     * @return Une {@link ResponseEntity} contenant :
     * <ul>
     *     <li><b>Succès (200 OK):</b> Une {@code List<Event>} des événements futurs, sérialisée selon {@link GlobalView.Base}.</li>
     *     <li><b>Erreur (404 Not Found):</b> Si le club n'existe pas (peut être levé par {@code EventService}).</li>
     *     <li><b>Erreur (403 Forbidden):</b> Si l'utilisateur connecté n'est pas membre du club (levé par {@code EventService}).</li>
     * </ul>
     * @see EventService#findUpcomingEventsByOrganisateurWithSecurityCheck(Integer, String)
     * @see GlobalView.Base
     * @see IsConnected
     */
    @GetMapping("/{id}/events/upcoming")
    @IsConnected // Requiert que l'utilisateur soit connecté
    @JsonView(GlobalView.Base.class) // Vue JSON de base pour les événements listés
    public ResponseEntity<List<Event>> getClubUpcomingEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        // Délégation à EventService qui gère la sécurité, le filtre 'upcoming' et le statut.
        List<Event> upcomingEvents = eventService.findUpcomingEventsByOrganisateurWithSecurityCheck(id, status);
        return ResponseEntity.ok(upcomingEvents);
    }

    // Rappel : La gestion centralisée des exceptions (ex: MethodArgumentNotValidException, EntityNotFoundException, etc.)
    // via une classe annotée @ControllerAdvice est la pratique recommandée pour mapper ces exceptions aux réponses HTTP.
}

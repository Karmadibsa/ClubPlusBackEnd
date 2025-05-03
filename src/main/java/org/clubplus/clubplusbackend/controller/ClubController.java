package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.CreateClubRequestDto;
import org.clubplus.clubplusbackend.dto.UpdateClubDto;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.annotation.IsAdmin;
import org.clubplus.clubplusbackend.security.annotation.IsConnected;
import org.clubplus.clubplusbackend.security.annotation.IsMembre;
import org.clubplus.clubplusbackend.security.annotation.IsReservation;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.service.EventService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST gérant les opérations relatives aux entités {@link Club}.
 * Fournit des endpoints pour la création, la lecture (individuelle, par code, liste),
 * la mise à jour et la suppression (désactivation) des clubs.
 * Gère également la récupération des membres, de l'administrateur et des événements
 * associés à un club spécifique.
 * La sécurité est appliquée via des annotations personnalisées et des vérifications
 * contextuelles dans les services appelés.
 * Le chemin de base pour ce contrôleur est "/api/clubs".
 */
@RestController
@RequestMapping("/api/clubs") // Chemin de base pour les opérations sur les clubs
@RequiredArgsConstructor // Injection via constructeur pour les champs final (Lombok)
@CrossOrigin // Activation de CORS si nécessaire au niveau du contrôleur (peut être géré globalement)
public class ClubController {

    /**
     * Service pour la logique métier liée aux clubs.
     */
    private final ClubService clubService;
    /**
     * Service pour la logique métier liée aux événements (utilisé pour lister les événements d'un club).
     */
    private final EventService eventService;

    /**
     * Récupère une liste de tous les clubs avec des informations de base.
     * Endpoint: GET /api/clubs
     * Sécurité: Requiert une authentification avec au moins le rôle MEMBRE ({@code @IsMembre}).
     *
     * @return Une {@code List<Club>} contenant les informations de base de tous les clubs,
     * sérialisée selon la vue {@link GlobalView.Base}.
     * @see ClubService#findAllClubs()
     * @see GlobalView.Base
     * @see IsMembre
     */
    @GetMapping
    @IsMembre // Requiert au minimum le rôle MEMBRE
    @JsonView(GlobalView.Base.class) // Vue JSON limitée aux infos de base
    public List<Club> getAllClubs() {
        return clubService.findAllClubs();
    }

    /**
     * Récupère les détails complets d'un club spécifique par son identifiant numérique.
     * Endpoint: GET /api/clubs/{id}
     * Sécurité: Requiert un rôle suffisant (ex: RESERVATION via {@code @IsReservation}) ET
     * que l'utilisateur soit membre du club concerné (vérification effectuée dans le service).
     *
     * @param id L'identifiant unique (Integer) du club à récupérer. Provient du chemin de l'URL.
     * @return L'objet {@link Club} complet correspondant à l'ID fourni, sérialisé selon la vue {@link GlobalView.ClubView}.
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException Si aucun club avec cet ID n'est trouvé (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException    Si l'utilisateur n'est pas membre du club
     *                                                                      ou n'a pas le rôle requis (géré globalement -> 403 Forbidden).
     * @see ClubService#getClubByIdWithSecurityCheck(Integer)
     * @see GlobalView.ClubView
     * @see IsReservation
     */
    @GetMapping("/{id}")
    @IsReservation // Requiert un rôle suffisant (RESERVATION ou ADMIN)
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club
    public Club getClubById(@PathVariable Integer id) {
        // Le service gère la recherche, l'existence et la vérification de l'appartenance au club.
        return clubService.getClubByIdWithSecurityCheck(id);
    }

    /**
     * Récupère les détails d'un club par son code d'invitation unique (chaîne de caractères).
     * Endpoint: GET /api/clubs/code/{codeClub}
     * Sécurité: Requiert une authentification minimale (rôle MEMBRE via {@code @IsMembre}).
     * Utile par exemple lors de l'inscription d'un membre pour vérifier le code club.
     *
     * @param codeClub Le code unique (String) du club à rechercher. Provient du chemin de l'URL.
     * @return L'objet {@link Club} correspondant au code fourni, sérialisé selon la vue {@link GlobalView.ClubView}.
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException Si aucun club ne correspond à ce code (géré globalement -> 404 Not Found).
     * @see ClubService#getClubByCodeOrThrow(String)
     * @see GlobalView.ClubView
     * @see IsMembre
     */
    @GetMapping("/code/{codeClub}")
    @IsMembre // Requiert au minimum le rôle MEMBRE
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée
    public Club getClubByCode(@PathVariable String codeClub) {
        // Le service gère la recherche par code et lève une exception si non trouvé.
        return clubService.getClubByCodeOrThrow(codeClub);
    }

    /**
     * Crée un nouveau club ainsi que son membre administrateur initial.
     * Endpoint: POST /api/clubs/inscription
     * Sécurité: Requiert que l'utilisateur soit authentifié (mécanisme à définir, potentiellement tout utilisateur connecté peut créer un club).
     * Validation: Les données reçues dans le DTO sont validées via {@code @Valid}.
     *
     * @param creationDto Un DTO ({@link CreateClubRequestDto}) contenant les informations du club à créer
     *                    et les informations de base de l'administrateur initial. Validé par {@code @Valid}.
     * @return Le {@link Club} nouvellement créé, incluant potentiellement l'admin, sérialisé selon {@link GlobalView.ClubView}.
     * Retourne un statut HTTP 201 (Created) en cas de succès.
     * @throws org.springframework.web.bind.MethodArgumentNotValidException                Si les données dans {@code creationDto} sont invalides (géré globalement -> 400 Bad Request).
     * @throws org.clubplus.clubplusbackend.security.exception.EmailAlreadyExistsException Si l'email fourni pour l'admin est déjà utilisé (géré globalement -> 409 Conflict).
     * @see ClubService#createClubAndRegisterAdmin(CreateClubRequestDto)
     * @see CreateClubRequestDto
     * @see GlobalView.ClubView
     * @see Valid
     */
    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED) // Réponse HTTP 201 en cas de succès
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club créé
    public Club createClubAndAdmin(
            @Valid @RequestBody CreateClubRequestDto creationDto // Valide le DTO reçu
    ) {
        // @Valid gère la validation -> 400 si échec.
        // Le service gère la logique de création et les conflits potentiels (ex: email admin) -> 409.
        return clubService.createClubAndRegisterAdmin(creationDto);
    }

    /**
     * Met à jour les informations d'un club existant.
     * Endpoint: PUT /api/clubs/{id}
     * Sécurité: Requiert que l'utilisateur soit authentifié ET soit l'ADMINISTRATEUR ({@code @IsAdmin})
     * spécifique du club ciblé (vérification effectuée dans le service).
     * Validation: Les données reçues dans le DTO sont validées via {@code @Valid}.
     *
     * @param id        L'identifiant unique (Integer) du club à mettre à jour. Provient du chemin de l'URL.
     * @param updateDto Un DTO ({@link UpdateClubDto}) contenant les nouvelles informations pour le club. Validé par {@code @Valid}.
     * @return Le {@link Club} mis à jour, sérialisé selon la vue {@link GlobalView.ClubView}.
     * Retourne un statut HTTP 200 (OK) par défaut en cas de succès.
     * @throws org.springframework.web.bind.MethodArgumentNotValidException Si les données dans {@code updateDto} sont invalides (géré globalement -> 400 Bad Request).
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException Si aucun club avec cet ID n'est trouvé (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException    Si l'utilisateur n'est pas l'administrateur de ce club (géré globalement -> 403 Forbidden).
     * @throws org.springframework.dao.DataIntegrityViolationException      Potentiellement si une contrainte d'unicité est violée (ex: code club, géré globalement -> 409 Conflict).
     * @see ClubService#updateClub(Integer, UpdateClubDto)
     * @see UpdateClubDto
     * @see GlobalView.ClubView
     * @see IsAdmin
     * @see Valid
     */
    @PutMapping("/{id}")
    @IsAdmin // Requiert le rôle ADMIN (le service vérifiera que c'est l'admin DE CE club)
    @JsonView(GlobalView.ClubView.class) // Vue JSON détaillée du club mis à jour
    public Club updateClub(@PathVariable Integer id, @Valid @RequestBody UpdateClubDto updateDto) {
        // @Valid gère la validation -> 400.
        // Le service gère existence (-> 404), droits d'admin (-> 403), et conflits (-> 409).
        return clubService.updateClub(id, updateDto);
    }

    /**
     * Désactive (supprime logiquement) un club existant.
     * Endpoint: DELETE /api/clubs/{id}
     * Sécurité: Requiert que l'utilisateur soit authentifié ET soit l'ADMINISTRATEUR ({@code @IsAdmin})
     * spécifique du club ciblé (vérification effectuée dans le service).
     * La suppression peut être empêchée par des règles métier (ex: événements futurs actifs).
     *
     * @param id L'identifiant unique (Integer) du club à désactiver. Provient du chemin de l'URL.
     * @return Rien (statut HTTP 204 No Content est retourné automatiquement en cas de succès sans corps de réponse).
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException Si aucun club avec cet ID n'est trouvé (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException    Si l'utilisateur n'est pas l'administrateur de ce club (géré globalement -> 403 Forbidden).
     * @throws org.clubplus.clubplusbackend.exception.ClubDeletionException Si la suppression est impossible en raison de contraintes métier
     *                                                                      (ex: événements futurs, géré globalement -> 409 Conflict).
     * @see ClubService#deactivateClub(Integer)
     * @see IsAdmin
     */
    @DeleteMapping("/{id}")
    @IsAdmin // Requiert le rôle ADMIN (le service vérifiera que c'est l'admin DE CE club)
    @ResponseStatus(HttpStatus.NO_CONTENT) // Réponse HTTP 204 en cas de succès
    public void deleteClub(@PathVariable Integer id) {
        // Le service gère existence (-> 404), droits d'admin (-> 403), et règles métier (-> 409).
        clubService.deactivateClub(id);
    }

    /**
     * Récupère la liste des membres associés à un club spécifique.
     * Endpoint: GET /api/clubs/{id}/membres
     * Sécurité: Requiert un rôle suffisant (ex: RESERVATION via {@code @IsReservation}) ET
     * que l'utilisateur soit membre du club concerné (vérification effectuée dans le service).
     *
     * @param id L'identifiant unique (Integer) du club dont les membres doivent être récupérés.
     * @return Une {@code List<Membre>} contenant les informations des membres du club,
     * sérialisée selon la vue {@link GlobalView.MembreView}. La liste est convertie depuis le Set retourné par le service.
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException Si aucun club avec cet ID n'est trouvé (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException    Si l'utilisateur n'est pas membre du club
     *                                                                      ou n'a pas le rôle requis (géré globalement -> 403 Forbidden).
     * @see ClubService#findMembresForClub(Integer)
     * @see GlobalView.MembreView
     * @see IsReservation
     */
    @GetMapping("/{id}/membres")
    @IsReservation // Requiert un rôle suffisant (RESERVATION ou ADMIN)
    @JsonView(GlobalView.MembreView.class) // Vue JSON pour les membres listés
    public List<Membre> getClubMembres(@PathVariable Integer id) {
        // Le service gère existence (-> 404) et sécurité d'accès (-> 403).
        Set<Membre> membresSet = clubService.findMembresForClub(id);
        return new ArrayList<>(membresSet); // Conversion Set vers List pour la réponse JSON
    }

    /**
     * Récupère les informations de l'administrateur d'un club spécifique.
     * Endpoint: GET /api/clubs/{id}/admin
     * Sécurité: Requiert que l'utilisateur soit connecté ({@code @IsConnected}) ET
     * membre du club concerné (vérification effectuée dans le service).
     *
     * @param id L'identifiant unique (Integer) du club dont l'administrateur doit être récupéré.
     * @return L'objet {@link Membre} représentant l'administrateur du club, sérialisé selon {@link GlobalView.MembreView}.
     * @throws org.clubplus.clubplusbackend.exception.ClubNotFoundException  Si le club n'existe pas (géré globalement -> 404 Not Found).
     * @throws org.clubplus.clubplusbackend.exception.AdminNotFoundException Si le club n'a pas d'administrateur défini (géré globalement -> 404 Not Found).
     * @throws org.springframework.security.access.AccessDeniedException     Si l'utilisateur n'est pas membre du club (géré globalement -> 403 Forbidden).
     * @see ClubService#getAdminForClubOrThrow(Integer)
     * @see GlobalView.MembreView
     * @see IsConnected
     */
    @GetMapping("/{id}/admin")
    @IsConnected // Requiert que l'utilisateur soit connecté
    @JsonView(GlobalView.MembreView.class) // Vue JSON détaillée de l'administrateur
    public Membre getClubAdmin(@PathVariable Integer id) {
        // Le service gère existence club/admin (-> 404) et sécurité d'accès (-> 403).
        return clubService.getAdminForClubOrThrow(id);
    }

    // --- Endpoints liés aux événements (Délégation à EventService) ---
    // Note: La vérification de l'appartenance de l'utilisateur au club pour voir ses événements
    // est supposée être gérée DANS les méthodes appelées de EventService.

    /**
     * Récupère la liste des événements organisés par un club spécifique, avec un filtre optionnel par statut.
     * Endpoint: GET /api/clubs/{id}/events
     * Sécurité: Requiert que l'utilisateur soit connecté ({@code @IsConnected}).
     * La vérification de l'appartenance au club est déléguée à {@link EventService}.
     *
     * @param id     L'identifiant unique (Integer) du club organisateur.
     * @param status (Optionnel) Un filtre (String) sur le statut des événements à retourner (ex: 'PASSED', 'UPCOMING', 'ACTIVE').
     *               La logique de filtrage est gérée par le {@code EventService}.
     * @return Une {@code List<Event>} contenant les événements correspondants, sérialisée selon {@link GlobalView.Base}.
     * @throws org.springframework.security.access.AccessDeniedException Si l'utilisateur connecté n'est pas membre du club
     *                                                                   (géré par {@code EventService} -> 403 Forbidden).
     * @throws Exception                                                 Autres exceptions potentielles levées par {@code EventService}.
     * @see EventService#findEventsByOrganisateurWithSecurityCheck(Integer, String)
     * @see GlobalView.Base
     * @see IsConnected
     */
    @GetMapping("/{id}/events")
    @IsConnected // Requiert que l'utilisateur soit connecté
    @JsonView(GlobalView.Base.class) // Vue JSON de base pour les événements listés
    public List<Event> getClubEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        // Délégation à EventService qui doit gérer la sécurité contextuelle (membre du club 'id').
        return eventService.findEventsByOrganisateurWithSecurityCheck(id, status);
    }

    /**
     * Récupère la liste des événements futurs organisés par un club spécifique.
     * Endpoint: GET /api/clubs/{id}/events/upcoming
     * Sécurité: Requiert que l'utilisateur soit connecté ({@code @IsConnected}).
     * La vérification de l'appartenance au club est déléguée à {@link EventService}.
     *
     * @param id     L'identifiant unique (Integer) du club organisateur.
     * @param status (Optionnel) Un filtre additionnel (String) sur le statut (peut être redondant ou pour affiner, ex: 'PUBLISHED').
     *               La logique exacte est gérée par {@code EventService}.
     * @return Une {@code List<Event>} contenant les événements futurs correspondants, sérialisée selon {@link GlobalView.Base}.
     * @throws org.springframework.security.access.AccessDeniedException Si l'utilisateur connecté n'est pas membre du club
     *                                                                   (géré par {@code EventService} -> 403 Forbidden).
     * @throws Exception                                                 Autres exceptions potentielles levées par {@code EventService}.
     * @see EventService#findUpcomingEventsByOrganisateurWithSecurityCheck(Integer, String) // Nom de méthode supposé
     * @see GlobalView.Base
     * @see IsConnected
     */
    @GetMapping("/{id}/events/upcoming")
    @IsConnected // Requiert que l'utilisateur soit connecté
    @JsonView(GlobalView.Base.class) // Vue JSON de base pour les événements listés
    public List<Event> getClubUpcomingEvents(@PathVariable Integer id, @RequestParam(required = false) String status) {
        // Délégation à EventService qui doit gérer la sécurité et la logique "upcoming".
        // Le nom de la méthode appelée dans EventService peut varier.
        return eventService.findUpcomingEventsByOrganisateurWithSecurityCheck(id, status);
    }

    // Rappel : La gestion centralisée des exceptions (ex: MethodArgumentNotValidException, NotFoundException, etc.)
    // via une classe annotée @ControllerAdvice est la pratique recommandée.
}

package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dto.CreateCategorieDto;
import org.clubplus.clubplusbackend.dto.UpdateCategorieDto;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service gérant la logique métier pour les entités {@link Categorie}.
 * Fournit des opérations CRUD (Créer, Lire, Mettre à jour, Supprimer) pour les catégories,
 * en appliquant les règles métier et les vérifications de sécurité contextuelles nécessaires.
 * Toutes les opérations publiques sont transactionnelles par défaut grâce à l'annotation
 * {@link Transactional @Transactional} au niveau de la classe. Les opérations de lecture
 * sont optimisées avec {@code readOnly = true}.
 *
 * @see Categorie
 * @see CategorieDao
 * @see EventDao
 * @see SecurityService
 * @see CreateCategorieDto
 * @see UpdateCategorieDto
 */
@Service
@RequiredArgsConstructor // Lombok: Injecte les dépendances final via le constructeur.
@Transactional // Gestion transactionnelle par défaut pour toutes les méthodes publiques.
public class CategorieService {

    /**
     * DAO pour l'accès aux données des catégories.
     */
    private final CategorieDao categorieRepository;
    /**
     * DAO pour l'accès aux données des événements (nécessaire pour lier/vérifier les événements parents).
     */
    private final EventDao eventRepository;
    /**
     * Service pour effectuer les vérifications de sécurité contextuelles (rôles, appartenance).
     */
    private final SecurityService securityService;

    /**
     * Récupère la liste de toutes les catégories associées à un événement spécifique.
     * <p>
     * Sécurité : Vérifie que l'utilisateur actuellement authentifié est au moins membre
     * du club organisateur de l'événement (via {@link SecurityService#checkIsCurrentUserMemberOfClubOrThrow}).
     * </p>
     *
     * @param eventId L'identifiant unique de l'{@link Event} dont les catégories sont recherchées.
     * @return Une {@code List<Categorie>} contenant toutes les catégories de l'événement.
     * Retourne une liste vide si l'événement n'a pas de catégories.
     * @throws EntityNotFoundException si aucun événement n'est trouvé pour l'{@code eventId} fourni (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas membre du club organisateur (Statut HTTP 403).
     */
    @Transactional(readOnly = true) // Optimisation pour une opération de lecture seule.
    public List<Categorie> findCategoriesByEventId(Integer eventId) {
        // 1. Vérifier l'existence de l'événement parent.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        // 2. Vérifier les droits d'accès de l'utilisateur au club organisateur.
        // Récupère l'ID du club depuis l'événement chargé. Suppose que event.getOrganisateur() ne retourne pas null.
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance AccessDeniedException si non autorisé.

        // 3. Si les vérifications passent, récupérer et retourner les catégories.
        return categorieRepository.findByEventId(eventId);
    }

    /**
     * Récupère une catégorie spécifique par son ID, en vérifiant qu'elle appartient bien
     * à l'événement spécifié par {@code eventId}.
     * <p>
     * Sécurité : Vérifie que l'utilisateur actuellement authentifié est au moins membre
     * du club organisateur de l'événement (via {@link SecurityService#checkIsCurrentUserMemberOfClubOrThrow}).
     * </p>
     *
     * @param eventId     L'identifiant de l'{@link Event} parent attendu.
     * @param categorieId L'identifiant de la {@link Categorie} à récupérer.
     * @return L'entité {@link Categorie} correspondante.
     * @throws EntityNotFoundException si la catégorie n'est pas trouvée ou n'appartient pas
     *                                 à l'événement spécifié (Statut HTTP 404).
     * @throws AccessDeniedException   si l'utilisateur courant n'est pas membre du club organisateur (Statut HTTP 403).
     */
    @Transactional(readOnly = true)
    public Categorie getCategorieByIdAndEventIdWithSecurityCheck(Integer eventId, Integer categorieId) {
        // 1. Récupérer la catégorie en vérifiant l'appartenance à l'événement via le DAO.
        Categorie categorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Catégorie ID %d non trouvée ou n'appartient pas à l'événement ID %d", categorieId, eventId)
                ));

        // 2. Vérifier les droits d'accès de l'utilisateur au club organisateur.
        // Récupère l'ID du club depuis la catégorie chargée (via sa relation event).
        Integer clubId = categorie.getEvent().getOrganisateur().getId();
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance AccessDeniedException si non autorisé.

        // 3. Retourner la catégorie trouvée et validée.
        return categorie;
    }

    /**
     * Ajoute une nouvelle catégorie à un événement existant.
     * <p>
     * Sécurité : Vérifie que l'utilisateur actuellement authentifié a les droits de gestion
     * (rôle ADMIN ou RESERVATION) sur le club organisateur de l'événement
     * (via {@link SecurityService#checkManagerOfClubOrThrow}).
     * </p>
     * <p>
     * Règles métier :
     * <ul>
     *     <li>L'événement parent doit exister et être actif (non terminé et {@code actif = true}).</li>
     *     <li>Le nom de la nouvelle catégorie doit être unique au sein de cet événement (insensible à la casse).</li>
     *     <li>La capacité fournie doit être positive ou nulle (selon la validation DTO/service).</li>
     * </ul>
     * </p>
     *
     * @param eventId      L'identifiant de l'{@link Event} auquel ajouter la catégorie.
     * @param categorieDto Le DTO {@link CreateCategorieDto} contenant les informations de la catégorie à créer.
     * @return La nouvelle entité {@link Categorie} persistée.
     * @throws EntityNotFoundException  si l'événement parent n'est pas trouvé (Statut HTTP 404).
     * @throws IllegalStateException    si l'événement est terminé, inactif, ou si une catégorie du même nom existe déjà
     *                                  pour cet événement (Statut HTTP 409 Conflict).
     * @throws IllegalArgumentException si la capacité fournie est invalide (ex: négative), bien que cela
     *                                  devrait être idéalement intercepté par la validation du DTO (Statut HTTP 400 Bad Request).
     * @throws AccessDeniedException    si l'utilisateur n'a pas les droits de gestion requis (Statut HTTP 403).
     */
    @Transactional // Assure l'atomicité de l'opération (vérifications + sauvegarde). Read-write.
    public Categorie addCategorieToEvent(Integer eventId, CreateCategorieDto categorieDto) {
        // 1. Vérifier l'existence et le statut de l'événement parent.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'ajouter la catégorie: Événement non trouvé (ID: " + eventId + ")"));

        // Vérification métier: l'événement ne doit pas être terminé.
        if (event.getEnd() != null && event.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement qui est déjà terminé (ID: " + eventId + ").");
        }
        // Vérification métier: l'événement doit être actif.
        if (event.getActif() == null || !event.getActif()) {
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement inactif ou annulé (ID: " + eventId + ").");
        }

        // 2. Vérifier les droits de gestion de l'utilisateur sur le club organisateur.
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId); // Lance AccessDeniedException si non autorisé.

        // 3. Vérifier l'unicité du nom de la catégorie pour cet événement.
        categorieRepository.findByEventIdAndNomIgnoreCase(eventId, categorieDto.getNom())
                .ifPresent(existing -> {
                    // Lance une exception de conflit si le nom est déjà pris.
                    throw new IllegalStateException("Une catégorie nommée '" + categorieDto.getNom() + "' existe déjà pour cet événement.");
                });

        // 4. Vérification de la capacité (sécurité supplémentaire).
        if (categorieDto.getCapacite() == null || categorieDto.getCapacite() < 0) {
            // Normalement géré par @Valid @Min sur le DTO, mais double vérification ici.
            throw new IllegalArgumentException("La capacité de la catégorie doit être un nombre positif ou nul.");
        }

        // 5. Créer et peupler la nouvelle entité Categorie.
        Categorie newCategorie = new Categorie();
        newCategorie.setNom(categorieDto.getNom());
        newCategorie.setCapacite(categorieDto.getCapacite());
        newCategorie.setEvent(event); // Lier à l'événement parent.

        // 6. Persister la nouvelle catégorie.
        return categorieRepository.save(newCategorie);
    }

    /**
     * Met à jour une catégorie existante (nom et/ou capacité).
     * <p>
     * Sécurité : Vérifie que l'utilisateur actuellement authentifié a les droits de gestion
     * (rôle ADMIN ou RESERVATION) sur le club organisateur de l'événement
     * (via {@link SecurityService#checkManagerOfClubOrThrow}).
     * </p>
     * <p>
     * Règles métier :
     * <ul>
     *     <li>La catégorie et l'événement parent doivent exister.</li>
     *     <li>L'événement parent doit être actif et non terminé.</li>
     *     <li>Si le nom est modifié, il doit rester unique au sein de l'événement.</li>
     *     <li>Si la capacité est modifiée, elle ne peut pas être inférieure au nombre de réservations déjà confirmées.</li>
     * </ul>
     * </p>
     *
     * @param eventId     L'identifiant de l'{@link Event} parent.
     * @param categorieId L'identifiant de la {@link Categorie} à mettre à jour.
     * @param dto         Le DTO {@link UpdateCategorieDto} contenant les nouvelles valeurs (partielles ou complètes).
     * @return L'entité {@link Categorie} mise à jour et persistée.
     * @throws EntityNotFoundException  si la catégorie n'est pas trouvée ou n'appartient pas à l'événement (Statut HTTP 404).
     * @throws IllegalStateException    si l'événement est terminé/inactif, si le nouveau nom est déjà pris par une autre catégorie
     *                                  du même événement, ou si la nouvelle capacité est insuffisante (Statut HTTP 409 Conflict).
     * @throws IllegalArgumentException si une nouvelle capacité négative est fournie (Statut HTTP 400 Bad Request).
     * @throws AccessDeniedException    si l'utilisateur n'a pas les droits de gestion requis (Statut HTTP 403).
     */
    @Transactional // Read-write.
    public Categorie updateCategorie(Integer eventId, Integer categorieId, UpdateCategorieDto dto) {
        // 1. Récupérer la catégorie existante, vérifiant l'appartenance à l'événement.
        Categorie existingCategorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Catégorie ID %d non trouvée ou n'appartient pas à l'événement ID %d", categorieId, eventId)
                ));
        Event event = existingCategorie.getEvent(); // Récupère l'événement lié.

        // 2. Vérifier le statut/date de l'événement parent.
        if (event.getEnd() != null && event.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement qui est déjà terminé (ID: " + eventId + ").");
        }
        if (event.getActif() == null || !event.getActif()) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement inactif ou annulé (ID: " + eventId + ").");
        }

        // 3. Vérifier les droits de gestion de l'utilisateur.
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId);

        boolean updated = false; // Indicateur pour savoir si une sauvegarde est nécessaire.

        // 4. Traiter la mise à jour du nom, si fourni dans le DTO.
        String newNom = dto.getNom();
        if (newNom != null) { // Le client souhaite potentiellement changer le nom.
            String trimmedNom = newNom.trim();
            // Vérifie si le nouveau nom est valide, différent de l'ancien, et unique.
            if (!trimmedNom.isEmpty() && !trimmedNom.equalsIgnoreCase(existingCategorie.getNom())) {
                categorieRepository.findByEventIdAndNomIgnoreCase(eventId, trimmedNom)
                        // Exclut la catégorie actuelle de la vérification d'unicité.
                        .filter(conflict -> !conflict.getId().equals(categorieId))
                        .ifPresent(conflict -> {
                            throw new IllegalStateException("Une autre catégorie nommée '" + trimmedNom + "' existe déjà pour cet événement.");
                        });
                existingCategorie.setNom(trimmedNom);
                updated = true;
            }
        }

        // 5. Traiter la mise à jour de la capacité, si fournie dans le DTO.
        Integer newCapacite = dto.getCapacite();
        if (newCapacite != null) { // Le client souhaite potentiellement changer la capacité.
            // Validation de base (négatif). Devrait être attrapé par @Valid sur le DTO.
            if (newCapacite < 0) {
                throw new IllegalArgumentException("La nouvelle capacité ne peut pas être négative.");
            }
            // Met à jour seulement si la capacité a réellement changé.
            if (!newCapacite.equals(existingCategorie.getCapacite())) {
                // Règle métier : la nouvelle capacité ne peut être inférieure aux places déjà réservées (confirmées).
                // Utilise la méthode calculée de l'entité qui compte les réservations confirmées.
                int placesConfirmees = existingCategorie.getPlaceReserve();
                if (newCapacite < placesConfirmees) {
                    throw new IllegalStateException(String.format(
                            "Impossible de réduire la capacité à %d car %d place(s) sont déjà confirmée(s).",
                            newCapacite, placesConfirmees
                    ));
                }
                existingCategorie.setCapacite(newCapacite);
                updated = true;
            }
        }

        // 6. Sauvegarder si des changements ont eu lieu.
        if (updated) {
            return categorieRepository.save(existingCategorie);
        } else {
            return existingCategorie; // Retourne l'entité non modifiée si aucun changement.
        }
    }

    /**
     * Supprime une catégorie existante.
     * <p>
     * Sécurité : Vérifie que l'utilisateur actuellement authentifié a les droits de gestion
     * (rôle ADMIN ou RESERVATION) sur le club organisateur de l'événement
     * (via {@link SecurityService#checkManagerOfClubOrThrow}).
     * </p>
     * <p>
     * Règles métier :
     * <ul>
     *     <li>La catégorie et l'événement parent doivent exister.</li>
     *     <li>L'événement parent doit être actif et non terminé.</li>
     *     <li>La catégorie ne peut être supprimée si elle contient des réservations confirmées.</li>
     * </ul>
     * </p>
     *
     * @param eventId     L'identifiant de l'{@link Event} parent.
     * @param categorieId L'identifiant de la {@link Categorie} à supprimer.
     * @throws EntityNotFoundException si la catégorie n'est pas trouvée ou n'appartient pas à l'événement (Statut HTTP 404).
     * @throws IllegalStateException   si l'événement est terminé/inactif, ou si la catégorie contient
     *                                 des réservations confirmées (Statut HTTP 409 Conflict).
     * @throws AccessDeniedException   si l'utilisateur n'a pas les droits de gestion requis (Statut HTTP 403).
     */
    @Transactional // Read-write.
    public void deleteCategorie(Integer eventId, Integer categorieId) {
        // 1. Récupérer la catégorie en chargeant impérativement les réservations (JOIN FETCH).
        // Ceci est crucial pour vérifier s'il y a des réservations sans erreur de lazy loading.
        Categorie categorieToDelete = categorieRepository.findByIdAndEventIdFetchingReservations(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Catégorie ID %d non trouvée ou n'appartient pas à l'événement ID %d", categorieId, eventId)
                ));
        Event event = categorieToDelete.getEvent(); // Récupère l'événement lié.

        // 2. Vérifier le statut/date de l'événement parent.
        if (event.getEnd() != null && event.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de supprimer une catégorie d'un événement qui est déjà terminé (ID: " + eventId + ").");
        }
        if (event.getActif() == null || !event.getActif()) {
            throw new IllegalStateException("Impossible de supprimer une catégorie d'un événement inactif ou annulé (ID: " + eventId + ").");
        }

        // 3. Vérifier les droits de gestion de l'utilisateur.
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId);

        // 4. Vérification métier : La catégorie contient-elle des réservations confirmées ?
        // Utilise la méthode @Transient getPlaceReserve() qui compte les CONFIRME.
        int placesConfirmees = categorieToDelete.getPlaceReserve();
        if (placesConfirmees > 0) {
            throw new IllegalStateException(String.format(
                    "Impossible de supprimer la catégorie '%s' car elle contient %d réservation(s) confirmée(s).",
                    categorieToDelete.getNom(), placesConfirmees
            ));
        }

        // 5. Si toutes les vérifications sont passées, supprimer la catégorie.
        // La cascade configurée sur Categorie.reservations (si présente) gèrerait les résas,
        // mais ici on s'assure qu'il n'y en a pas de confirmées.
        categorieRepository.delete(categorieToDelete);
    }
}

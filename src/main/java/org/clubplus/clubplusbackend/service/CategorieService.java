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

import java.time.Instant;
import java.util.List;

/**
 * Service gérant la logique métier pour les {@link Categorie}s d'événements.
 * <p>
 * Ce service assure les opérations CRUD pour les catégories, en appliquant
 * les règles métier (ex: unicité du nom, statut de l'événement) et les
 * vérifications de sécurité (ex: droits de l'utilisateur).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategorieService {

    private final CategorieDao categorieRepository;
    private final EventDao eventRepository;
    private final SecurityService securityService;

    /**
     * Récupère toutes les catégories d'un événement spécifique.
     * <p>
     * <b>Sécurité :</b> Vérifie que l'utilisateur courant est membre du club organisateur.
     *
     * @param eventId L'ID de l'événement.
     * @return La liste des catégories de l'événement.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws AccessDeniedException   si l'utilisateur n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public List<Categorie> findCategoriesByEventId(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        Integer clubId = event.getOrganisateur().getId();
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        return categorieRepository.findByEventId(eventId);
    }

    /**
     * Récupère une catégorie par son ID, en s'assurant qu'elle appartient à l'événement spécifié.
     * <p>
     * <b>Sécurité :</b> Vérifie que l'utilisateur courant est membre du club organisateur.
     *
     * @param eventId     L'ID de l'événement parent.
     * @param categorieId L'ID de la catégorie à récupérer.
     * @return L'entité {@link Categorie} trouvée.
     * @throws EntityNotFoundException si la catégorie ou sa liaison à l'événement n'est pas trouvée.
     * @throws AccessDeniedException   si l'utilisateur n'est pas membre du club.
     */
    @Transactional(readOnly = true)
    public Categorie getCategorieByIdAndEventIdWithSecurityCheck(Integer eventId, Integer categorieId) {
        Categorie categorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Catégorie ID %d non trouvée ou n'appartient pas à l'événement ID %d", categorieId, eventId)
                ));

        Integer clubId = categorie.getEvent().getOrganisateur().getId();
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        return categorie;
    }

    /**
     * Ajoute une nouvelle catégorie à un événement.
     * <p>
     * <b>Sécurité :</b> Vérifie que l'utilisateur courant a un rôle de gestionnaire (ADMIN ou RESERVATION) dans le club.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'événement doit être actif et ne doit pas être terminé.</li>
     * <li>Le nom de la catégorie doit être unique pour cet événement.</li>
     * </ul>
     *
     * @param eventId      L'ID de l'événement parent.
     * @param categorieDto Le DTO contenant les informations de la nouvelle catégorie.
     * @return La nouvelle catégorie persistée.
     * @throws EntityNotFoundException si l'événement n'est pas trouvé.
     * @throws IllegalStateException   si l'événement est terminé, inactif, ou si le nom de la catégorie est déjà pris.
     * @throws AccessDeniedException   si l'utilisateur n'a pas les droits de gestion.
     */
    public Categorie addCategorieToEvent(Integer eventId, CreateCategorieDto categorieDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'ajouter la catégorie: Événement non trouvé (ID: " + eventId + ")"));

        if (event.getEndTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement déjà terminé (ID: " + eventId + ").");
        }
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement inactif ou annulé (ID: " + eventId + ").");
        }

        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        categorieRepository.findByEventIdAndNomIgnoreCase(eventId, categorieDto.getNom())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Une catégorie nommée '" + categorieDto.getNom() + "' existe déjà pour cet événement.");
                });

        if (categorieDto.getCapacite() == null || categorieDto.getCapacite() < 0) {
            throw new IllegalArgumentException("La capacité de la catégorie doit être un nombre positif ou nul.");
        }

        Categorie newCategorie = new Categorie();
        newCategorie.setNom(categorieDto.getNom());
        newCategorie.setCapacite(categorieDto.getCapacite());
        newCategorie.setEvent(event);

        return categorieRepository.save(newCategorie);
    }

    /**
     * Met à jour une catégorie existante.
     * <p>
     * <b>Sécurité :</b> Vérifie que l'utilisateur est un gestionnaire du club organisateur.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'événement doit être actif et non terminé.</li>
     * <li>Le nouveau nom, s'il est fourni, doit rester unique.</li>
     * <li>La nouvelle capacité ne peut être inférieure au nombre de réservations confirmées.</li>
     * </ul>
     *
     * @param eventId     L'ID de l'événement parent.
     * @param categorieId L'ID de la catégorie à mettre à jour.
     * @param dto         Le DTO contenant les nouvelles valeurs.
     * @return La catégorie mise à jour.
     * @throws EntityNotFoundException si la catégorie ou sa liaison à l'événement n'est pas trouvée.
     * @throws IllegalStateException   si une règle métier est violée (événement terminé, nom dupliqué, capacité insuffisante).
     * @throws AccessDeniedException   si l'utilisateur n'a pas les droits de gestion.
     */
    public Categorie updateCategorie(Integer eventId, Integer categorieId, UpdateCategorieDto dto) {
        Categorie existingCategorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Catégorie ID %d non trouvée ou n'appartient pas à l'événement ID %d", categorieId, eventId)
                ));
        Event event = existingCategorie.getEvent();

        if (event.getEndTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement déjà terminé (ID: " + eventId + ").");
        }
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement inactif ou annulé (ID: " + eventId + ").");
        }

        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        boolean updated = false;

        // Mise à jour du nom
        String newNom = dto.getNom();
        if (newNom != null) {
            String trimmedNom = newNom.trim();
            if (!trimmedNom.isEmpty() && !trimmedNom.equalsIgnoreCase(existingCategorie.getNom())) {
                categorieRepository.findByEventIdAndNomIgnoreCase(eventId, trimmedNom)
                        .filter(conflict -> !conflict.getId().equals(categorieId))
                        .ifPresent(conflict -> {
                            throw new IllegalStateException("Une autre catégorie nommée '" + trimmedNom + "' existe déjà pour cet événement.");
                        });
                existingCategorie.setNom(trimmedNom);
                updated = true;
            }
        }

        // Mise à jour de la capacité
        Integer newCapacite = dto.getCapacite();
        if (newCapacite != null) {
            if (newCapacite < 0) {
                throw new IllegalArgumentException("La nouvelle capacité ne peut pas être négative.");
            }
            if (!newCapacite.equals(existingCategorie.getCapacite())) {
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

        if (updated) {
            return categorieRepository.save(existingCategorie);
        }
        return existingCategorie;
    }

    /**
     * Supprime une catégorie.
     * <p>
     * <b>Sécurité :</b> Vérifie que l'utilisateur est un gestionnaire du club organisateur.
     * <p>
     * <b>Règles métier :</b>
     * <ul>
     * <li>L'événement doit être actif et non terminé.</li>
     * <li>La catégorie ne peut être supprimée si elle contient des réservations confirmées.</li>
     * </ul>
     *
     * @param eventId     L'ID de l'événement parent.
     * @param categorieId L'ID de la catégorie à supprimer.
     * @throws EntityNotFoundException si la catégorie ou sa liaison à l'événement n'est pas trouvée.
     * @throws IllegalStateException   si une règle métier est violée.
     * @throws AccessDeniedException   si l'utilisateur n'a pas les droits de gestion.
     */
    public void deleteCategorie(Integer eventId, Integer categorieId) {
        Categorie categorieToDelete = categorieRepository.findByIdAndEventIdFetchingReservations(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Catégorie ID %d non trouvée ou n'appartient pas à l'événement ID %d", categorieId, eventId)
                ));
        Event event = categorieToDelete.getEvent();

        if (event.getEndTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Impossible de supprimer une catégorie d'un événement déjà terminé (ID: " + eventId + ").");
        }
        if (!event.getActif()) {
            throw new IllegalStateException("Impossible de supprimer une catégorie d'un événement inactif ou annulé (ID: " + eventId + ").");
        }

        securityService.checkManagerOfClubOrThrow(event.getOrganisateur().getId());

        int placesConfirmees = categorieToDelete.getPlaceReserve();
        if (placesConfirmees > 0) {
            throw new IllegalStateException(String.format(
                    "Impossible de supprimer la catégorie '%s' car elle contient %d réservation(s) confirmée(s).",
                    categorieToDelete.getNom(), placesConfirmees
            ));
        }

        categorieRepository.delete(categorieToDelete);
    }
}

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor // Injecte les dépendances final via le constructeur
@Transactional // Applique la gestion transactionnelle à toutes les méthodes publiques par défaut
public class CategorieService {

    private final CategorieDao categorieRepository;
    private final EventDao eventRepository;
    private final SecurityService securityService; // Service pour les vérifications de sécurité contextuelles

    /**
     * Récupère toutes les catégories pour un événement donné.
     * Sécurité : Vérifie que l'utilisateur courant est au moins MEMBRE du club organisateur.
     */
    @Transactional(readOnly = true) // Optimisation pour les lectures seules
    public List<Categorie> findCategoriesByEventId(Integer eventId) {
        // 1. Vérifier que l'événement existe pour éviter les erreurs inutiles.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId));

        // 2. Vérification de Sécurité Contextuelle : L'utilisateur peut-il voir les infos de ce club ?
        // On récupère l'ID du club organisateur DEPUIS l'événement trouvé.
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId); // Lance AccessDeniedException (403) si non membre

        // 3. Si sécurité OK, récupérer et retourner les catégories.
        return categorieRepository.findByEventId(eventId);
    }

    /**
     * Récupère une catégorie spécifique par ID, en s'assurant qu'elle appartient à l'événement donné.
     * Sécurité : Vérifie que l'utilisateur courant est au moins MEMBRE du club organisateur.
     */
    @Transactional(readOnly = true)
    public Categorie getCategorieByIdAndEventIdWithSecurityCheck(Integer eventId, Integer categorieId) {
        // 1. Tenter de récupérer la catégorie via le DAO qui vérifie aussi le lien avec l'event.
        Categorie categorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie ID " + categorieId + " non trouvée ou n'appartient pas à l'événement ID " + eventId));

        // 2. Vérification de Sécurité Contextuelle (idem que findCategoriesByEventId).
        Integer clubId = categorie.getEvent().getOrganisateur().getId();
        securityService.checkIsCurrentUserMemberOfClubOrThrow(clubId);

        // 3. Retourner la catégorie si tout est OK.
        return categorie;
    }

    /**
     * Ajoute une nouvelle catégorie à un événement existant.
     * Sécurité : Vérifie que l'utilisateur courant est MANAGER (ADMIN ou RESERVATION) du club organisateur.
     */
    @Transactional // Assure que toutes les opérations (vérifications, sauvegarde) sont dans une seule transaction
    public Categorie addCategorieToEvent(Integer eventId, CreateCategorieDto categorieDto) {
        // 1. Vérifier que l'événement parent existe et est actif.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'ajouter la catégorie: Événement non trouvé (ID: " + eventId + ")"));

        // Vérifier si la date de fin de l'événement est passée.
        // Assurez-vous que votre champ 'end' dans l'entité Event est de type LocalDateTime ou similaire.
        if (event.getEnd() != null && event.getEnd().isBefore(LocalDateTime.now())) {
            // Lève une exception si l'événement est terminé.
            // IllegalStateException est appropriée ici car l'état de l'event rend l'opération illégale.
            // Votre GlobalExceptionHandler devrait la mapper en 409 Conflict ou 400 Bad Request.
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement qui est déjà terminé (ID: " + eventId + ").");
        }
        Boolean isActif = event.getActif(); // Utilise le getter Lombok pour le champ 'actif'
        if (isActif == null || !isActif) {
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement inactif ou annulé (ID: " + eventId + ")."); // -> 409 ou 400
        }

        // 2. Vérification de Sécurité Contextuelle : L'utilisateur peut-il gérer les catégories pour ce club ?
        // Note: Le fichier SQL montre event.organisateur_id. Assurez-vous que votre entité Event
        // a une relation @ManyToOne nommée 'organisateur' vers l'entité Club.
        Integer clubId = event.getOrganisateur().getId(); // Récupère l'ID du Club organisateur via la relation
        securityService.checkManagerOfClubOrThrow(clubId); // Lance AccessDeniedException (403) si non manager

        // 3. Validation Métier : Le nom de la catégorie est-il unique pour CET événement ?
        // Utilisation de categorieDto.getNom() venant du DTO
        categorieRepository.findByEventIdAndNomIgnoreCase(eventId, categorieDto.getNom())
                .ifPresent(existing -> {
                    // Utilisation d'une exception plus spécifique pour le conflit (409)
                    throw new IllegalArgumentException("Une catégorie nommée '" + categorieDto.getNom() + "' existe déjà pour cet événement.");
                });

        // 4. Validation Métier : La capacité est-elle valide ? (Double vérification, @Valid sur DTO est la première ligne)
        if (categorieDto.getCapacite() == null || categorieDto.getCapacite() < 0) {
            // Cette exception ne devrait idéalement pas être atteinte si @Valid fonctionne bien sur le DTO.
            throw new IllegalArgumentException("La capacité de la catégorie doit être un nombre positif ou nul."); // Sera transformée en 400 par GlobalExceptionHandler
        }

        // --- Création et mapping ---
        // 5. Créer une NOUVELLE instance de l'entité Categorie
        Categorie newCategorie = new Categorie();

        // 6. Copier les données du DTO vers la nouvelle entité
        newCategorie.setNom(categorieDto.getNom());
        newCategorie.setCapacite(categorieDto.getCapacite());
        // L'ID sera généré par la base, pas besoin de le mettre à null.
        // La liste des réservations est initialisée dans l'entité Categorie elle-même.

        // 7. Lier l'entité Event récupérée à la nouvelle catégorie
        newCategorie.setEvent(event);

        // 8. Sauvegarder la nouvelle entité Categorie.
        return categorieRepository.save(newCategorie);
    }

    /**
     * Met à jour les informations (nom, capacité) d'une catégorie existante.
     * Sécurité : Vérifie que l'utilisateur courant est MANAGER du club organisateur.
     */
    @Transactional
    public Categorie updateCategorie(Integer eventId, Integer categorieId, UpdateCategorieDto dto) { // Accepte le DTO
        // 1. Récupérer la catégorie existante et son événement.
        Categorie existingCategorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie ID " + categorieId + " non trouvée ou n'appartient pas à l'événement ID " + eventId));
        Event event = existingCategorie.getEvent();

        // --- Vérification Date Événement ---
        if (event.getEnd() != null && event.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement qui est déjà terminé (ID: " + eventId + ").");
        }

        // --- Vérification Événement Actif ---
        Boolean isActif = event.getActif();
        if (isActif == null || !isActif) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement inactif ou annulé (ID: " + eventId + ").");
        }

        // 2. Vérification de Sécurité Contextuelle.
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId);

        boolean updated = false;

        // 3. Traiter la mise à jour du nom (si fourni dans le DTO).
        String newNom = dto.getNom(); // Lire depuis le DTO
        if (newNom != null) { // Vérifier si le client a fourni un nom
            String trimmedNom = newNom.trim();
            if (!trimmedNom.isEmpty() && !trimmedNom.equalsIgnoreCase(existingCategorie.getNom())) {
                // Validation Métier : Nom unique (excluant soi-même).
                categorieRepository.findByEventIdAndNomIgnoreCase(eventId, trimmedNom)
                        .filter(conflict -> !conflict.getId().equals(categorieId))
                        .ifPresent(conflict -> {
                            // Utiliser une exception spécifique pour 409
                            throw new IllegalStateException("Une autre catégorie nommée '" + trimmedNom + "' existe déjà pour cet événement.");
                        });
                existingCategorie.setNom(trimmedNom);
                updated = true;
            }
        }

        // 4. Traiter la mise à jour de la capacité (si fournie dans le DTO).
        Integer newCapacite = dto.getCapacite(); // Lire depuis le DTO
        if (newCapacite != null) { // Vérifier si le client a fourni une capacité
            // La validation @Min(0) sur le DTO couvre le cas négatif -> 400 via MethodArgumentNotValidException
            // Cette vérification est une sécurité supplémentaire au cas où @Valid ne serait pas appliqué.
            if (newCapacite < 0) {
                throw new IllegalArgumentException("La nouvelle capacité ne peut pas être négative."); // Devrait être 400
            }

            if (!newCapacite.equals(existingCategorie.getCapacite())) { // Mettre à jour seulement si différente
                int placesConfirmees = existingCategorie.getPlaceReserve();
                if (newCapacite < placesConfirmees) {
                    // Utiliser une exception spécifique pour 409
                    throw new IllegalStateException("Impossible de réduire la capacité à " + newCapacite + " car " + placesConfirmees + " places sont déjà confirmées.");
                }
                existingCategorie.setCapacite(newCapacite);
                updated = true;
            }
        }
        // 6. Sauvegarder uniquement si des modifications ont été détectées.
        if (updated) {
            return categorieRepository.save(existingCategorie);
        } else {
            return existingCategorie; // Retourne l'objet non modifié si rien n'a changé
        }
    }

    /**
     * Supprime une catégorie.
     * Sécurité : Vérifie que l'utilisateur courant est MANAGER du club organisateur.
     * Règle métier : Empêche la suppression si des réservations existent pour cette catégorie.
     */
    public void deleteCategorie(Integer eventId, Integer categorieId) {
        // 1. Récupérer la catégorie en s'assurant de charger les réservations pour la vérification.
        // Utilise la méthode spécifique du DAO avec JOIN FETCH.
        Categorie categorieToDelete = categorieRepository.findByIdAndEventIdFetchingReservations(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie ID " + categorieId + " non trouvée ou n'appartient pas à l'événement ID " + eventId));

        Event event = categorieToDelete.getEvent();

        // --- AJOUT VÉRIFICATION DATE ÉVÉNEMENT ---
        if (event.getEnd() != null && event.getEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de supprimer une catégorie d'un événement qui est déjà terminé (ID: " + eventId + ")."); // -> 409 ou 400
        }

        if (!categorieToDelete.getEvent().getActif()) {
            throw new IllegalStateException("Impossible de supprimer une catégorie d'un événement annulé (ID: " + eventId + ")."); // -> 409
        }
        // 2. Vérification de Sécurité Contextuelle.
        Integer clubId = categorieToDelete.getEvent().getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId);

        // 3. Validation Métier : Y a-t-il des réservations existantes ?
        // Utilise la méthode getPlaceReserve() qui compte les confirmées.
        int placesConfirmees = categorieToDelete.getPlaceReserve();
        if (placesConfirmees > 0) {
            // Message d'erreur mis à jour
            throw new IllegalStateException("Impossible de supprimer la catégorie '" + categorieToDelete.getNom() + "' car elle contient " + placesConfirmees + " réservation(s) confirmée(s)."); // Sera transformée en 409
        }
        // 4. Si aucune réservation, procéder à la suppression.
        categorieRepository.delete(categorieToDelete);
    }
}

package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public Categorie addCategorieToEvent(Integer eventId, Categorie categorie) {
        // 1. Vérifier que l'événement parent existe.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'ajouter la catégorie: Événement non trouvé (ID: " + eventId + ")"));

        if (!event.getActif()) { // Utilise le getter de Lombok
            throw new IllegalStateException("Impossible d'ajouter une catégorie à un événement annulé (ID: " + eventId + ")."); // -> 409
        }
        // 2. Vérification de Sécurité Contextuelle : L'utilisateur peut-il gérer les catégories pour ce club ?
        Integer clubId = event.getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId); // Lance AccessDeniedException (403) si non manager

        // 3. Validation Métier : Le nom de la catégorie est-il unique pour CET événement ?
        categorieRepository.findByEventIdAndNomIgnoreCase(eventId, categorie.getNom())
                .ifPresent(existing -> {
                    // Si une catégorie avec ce nom existe déjà pour cet événement, lever une exception.
                    throw new IllegalArgumentException("Une catégorie nommée '" + categorie.getNom() + "' existe déjà pour cet événement."); // Sera transformée en 400/409 par GlobalExceptionHandler
                });

        // 4. Validation Métier : La capacité est-elle valide ?
        if (categorie.getCapacite() == null || categorie.getCapacite() < 0) {
            throw new IllegalArgumentException("La capacité de la catégorie doit être un nombre positif ou nul."); // Sera transformée en 400 par GlobalExceptionHandler
        }

        // 5. Préparation de la nouvelle catégorie avant sauvegarde.
        categorie.setEvent(event); // Lier à l'événement parent.
        categorie.setId(null);     // Assurer la création (JPA ignorera un ID fourni).
        if (categorie.getReservations() == null) { // Bonne pratique d'initialiser les collections.
            categorie.setReservations(new ArrayList<>());
        }

        // 6. Sauvegarder la nouvelle catégorie.
        return categorieRepository.save(categorie);
    }

    /**
     * Met à jour les informations (nom, capacité) d'une catégorie existante.
     * Sécurité : Vérifie que l'utilisateur courant est MANAGER du club organisateur.
     */
    public Categorie updateCategorie(Integer eventId, Integer categorieId, Categorie categorieDetails) {
        // 1. Récupérer la catégorie existante, en vérifiant qu'elle appartient bien à l'événement.
        Categorie existingCategorie = categorieRepository.findByIdAndEventId(categorieId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie ID " + categorieId + " non trouvée ou n'appartient pas à l'événement ID " + eventId));

        // Vérifier si l'événement associé est ACTIF
        if (!existingCategorie.getEvent().getActif()) {
            throw new IllegalStateException("Impossible de modifier une catégorie d'un événement annulé (ID: " + eventId + ")."); // -> 409
        }
        // 2. Vérification de Sécurité Contextuelle.
        Integer clubId = existingCategorie.getEvent().getOrganisateur().getId();
        securityService.checkManagerOfClubOrThrow(clubId);

        boolean updated = false; // Flag pour savoir si on doit sauvegarder

        // 3. Traiter la mise à jour du nom (si fourni et différent).
        String newNom = categorieDetails.getNom();
        if (newNom != null && !newNom.trim().isEmpty() && !newNom.equalsIgnoreCase(existingCategorie.getNom())) {
            // Validation Métier : Le nouveau nom est-il unique (en excluant la catégorie actuelle) ?
            categorieRepository.findByEventIdAndNomIgnoreCase(eventId, newNom.trim())
                    .filter(conflict -> !conflict.getId().equals(categorieId)) // Exclure la catégorie elle-même de la vérification
                    .ifPresent(conflict -> {
                        throw new IllegalArgumentException("Une autre catégorie nommée '" + newNom.trim() + "' existe déjà pour cet événement."); // 400/409
                    });
            existingCategorie.setNom(newNom.trim());
            updated = true; // Signale qu'une modification a eu lieu
        }

        // 4. Traiter la mise à jour de la capacité (si fournie et différente).
        Integer newCapacite = categorieDetails.getCapacite();
        if (newCapacite != null && !newCapacite.equals(existingCategorie.getCapacite())) {
            // Validation Métier : Capacité positive ?
            if (newCapacite < 0) {
                throw new IllegalArgumentException("La nouvelle capacité ne peut pas être négative."); // 400
            }
            // Utilise la méthode getPlaceReserve() qui compte les réservations CONFIRMED
            int placesConfirmees = existingCategorie.getPlaceReserve();
            if (newCapacite < placesConfirmees) {
                // Message d'erreur mis à jour pour refléter "confirmées"
                throw new IllegalArgumentException("Impossible de réduire la capacité à " + newCapacite + " car " + placesConfirmees + " places sont déjà confirmées."); // 409 Conflict
            }
            existingCategorie.setCapacite(newCapacite);
            updated = true; // Signale qu'une modification a eu lieu
        }

        // 5. Sauvegarder uniquement si des modifications ont été détectées.
        if (updated) {
            // Sauvegarde si le nom OU la capacité (ou les deux) ont changé
            return categorieRepository.save(existingCategorie);
        } else {
            // Retourne l'objet existant si aucune modification n'a été faite
            return existingCategorie;
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

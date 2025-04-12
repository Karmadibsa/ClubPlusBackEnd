package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.CategorieDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.model.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategorieService {

    private final CategorieDao categorieRepository;
    private final EventDao eventRepository; // Nécessaire pour lier à l'événement

    // --- Opérations dans le contexte d'un événement ---

    public List<Categorie> findCategoriesByEventId(Integer eventId) {
        // Vérifier si l'événement existe peut être une bonne idée
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Événement non trouvé avec l'ID : " + eventId);
        }
        return categorieRepository.findByEventId(eventId);
    }

    public Optional<Categorie> findCategorieByIdAndEventId(Integer eventId, Integer categorieId) {
        // Vérifie d'abord si la catégorie existe
        Optional<Categorie> categorieOpt = categorieRepository.findById(categorieId);
        if (categorieOpt.isPresent()) {
            // Vérifie si elle appartient bien à l'événement spécifié
            if (categorieOpt.get().getEvent() != null && categorieOpt.get().getEvent().getId().equals(eventId)) {
                return categorieOpt;
            } else {
                // Catégorie trouvée mais n'appartient pas au bon événement
                return Optional.empty();
                // Ou lancer une exception spécifique ? throw new SecurityException(...) ou IllegalArgumentException
            }
        }
        return Optional.empty(); // Catégorie non trouvée
    }

    public Categorie getCategorieByIdAndEventIdOrThrow(Integer eventId, Integer categorieId) {
        return findCategorieByIdAndEventId(eventId, categorieId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie avec ID " + categorieId + " non trouvée pour l'événement ID " + eventId));
        // Ou ResourceNotFoundException
    }


    public Categorie addCategorieToEvent(Integer eventId, Categorie categorie) {
        // 1. Récupérer l'événement parent
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Impossible d'ajouter la catégorie: Événement non trouvé avec l'ID " + eventId));

        // 2. Vérifier si une catégorie avec le même nom existe déjà pour cet événement
        categorieRepository.findByEventIdAndNomIgnoreCase(eventId, categorie.getNom())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Une catégorie nommée '" + categorie.getNom() + "' existe déjà pour cet événement.");
                    // Ou DataIntegrityViolationException
                });

        // 3. Lier la catégorie à l'événement
        categorie.setEvent(event);
        // Assurer que l'ID est null pour la création
        categorie.setId(null);

        // 4. Initialiser la liste des réservations si nécessaire
        if (categorie.getReservations() == null) {
            categorie.setReservations(new java.util.ArrayList<>());
        }

        // 5. Sauvegarder la nouvelle catégorie
        return categorieRepository.save(categorie);
    }

    public Categorie updateCategorie(Integer eventId, Integer categorieId, Categorie categorieDetails) {
        // 1. Récupérer la catégorie existante en s'assurant qu'elle appartient au bon événement
        Categorie existingCategorie = getCategorieByIdAndEventIdOrThrow(eventId, categorieId);

        // 2. Vérifier si le nom change et s'il entre en conflit avec une autre catégorie du même événement
        if (!existingCategorie.getNom().equalsIgnoreCase(categorieDetails.getNom())) {
            categorieRepository.findByEventIdAndNomIgnoreCase(eventId, categorieDetails.getNom())
                    .ifPresent(conflict -> {
                        if (!conflict.getId().equals(categorieId)) { // S'assure que ce n'est pas la même catégorie
                            throw new IllegalArgumentException("Une autre catégorie nommée '" + categorieDetails.getNom() + "' existe déjà pour cet événement.");
                            // Ou DataIntegrityViolationException
                        }
                    });
            existingCategorie.setNom(categorieDetails.getNom());
        }


        // 3. Mettre à jour la capacité (potentiellement vérifier vs réservations existantes ?)
        if (categorieDetails.getCapacite() < existingCategorie.getPlaceReserve()) {
            throw new IllegalArgumentException("La nouvelle capacité (" + categorieDetails.getCapacite() + ") est inférieure au nombre de places déjà réservées (" + existingCategorie.getPlaceReserve() + ").");
        }
        existingCategorie.setCapacite(categorieDetails.getCapacite());

        return categorieRepository.save(existingCategorie);
    }

    public void deleteCategorie(Integer eventId, Integer categorieId) {
        // 1. Récupérer la catégorie existante en s'assurant qu'elle appartient au bon événement
        Categorie categorieToDelete = getCategorieByIdAndEventIdOrThrow(eventId, categorieId);

        // 2. Vérifier s'il y a des réservations associées avant de supprimer ?
        if (!categorieToDelete.getReservations().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer la catégorie '" + categorieToDelete.getNom() + "' car elle a des réservations associées.");
        }

        // 3. Supprimer la catégorie
        categorieRepository.delete(categorieToDelete);
    }

}

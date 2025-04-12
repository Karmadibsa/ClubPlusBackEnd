package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieDao extends JpaRepository<Categorie, Integer> {

    // Trouver toutes les catégories pour un événement spécifique
    List<Categorie> findByEventId(Integer eventId);

    // Trouver une catégorie spécifique par son nom et l'ID de l'événement (pourrait être utile pour éviter les doublons)
    Optional<Categorie> findByEventIdAndNomIgnoreCase(Integer eventId, String nom);

}

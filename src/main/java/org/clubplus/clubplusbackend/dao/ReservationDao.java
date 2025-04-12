package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationDao extends JpaRepository<Reservation, Integer> {
    // Trouver toutes les réservations pour un membre spécifique
    List<Reservation> findByMembreId(Integer membreId);

    // Trouver toutes les réservations pour un événement spécifique
    List<Reservation> findByEventId(Integer eventId);

    // Trouver toutes les réservations pour une catégorie spécifique
    List<Reservation> findByCategorieId(Integer categorieId);

    // Trouver une réservation spécifique pour un membre et un événement donnés
    // Utile pour vérifier si un membre a déjà réservé pour cet événement (si 1 réservation/membre/événement)
    Optional<Reservation> findByMembreIdAndEventId(Integer membreId, Integer eventId);

    // Compter les réservations pour une catégorie (pour vérifier la capacité)
    // Note: On peut aussi obtenir la taille de la collection dans l'entité Categorie, mais ceci est une alternative BDD
    long countByCategorieId(Integer categorieId);

    long countByMembreIdAndEventId(Integer membreId, Integer eventId);
}

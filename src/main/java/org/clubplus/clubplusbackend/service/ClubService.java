package org.clubplus.clubplusbackend.service;

import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.model.Club;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClubService {
    private final ClubDao clubRepository;

    public ClubService(ClubDao clubRepository) {
        this.clubRepository = clubRepository;

    }

    // Récupère tous les clubs
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    // Récupère un club par son ID
    public Optional<Club> getClubById(Long id) {
        return clubRepository.findById(id);
    }

    // Sauvegarde un nouveau club ou met à jour un existant
    public Club save(Club club) {
        return clubRepository.save(club);
    }

    // Suppression d'un club
    public void deleteClub(Long id) {
        clubRepository.deleteById(id);
    }

    // Vérifie si un club existe par son ID
    public boolean existsById(Long id) {
        return clubRepository.existsById(id);
    }

}


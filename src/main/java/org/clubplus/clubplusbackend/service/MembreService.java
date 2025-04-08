package org.clubplus.clubplusbackend.service;

import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MembreService {
    private final MembreDao membreRepository;

    public MembreService(MembreDao membreRepository) {
        this.membreRepository = membreRepository;
    }

    // Récupère tous les membres
    public List<Membre> getAllMembres() {
        return membreRepository.findAll();
    }

    // Récupère un membre par son ID
    public Optional<Membre> getMembreById(Long id) {
        return membreRepository.findById(id);
    }

    // Sauvegarde un nouveau membre ou met à jour un existant
    public Membre save(Membre membre) {
        return membreRepository.save(membre);
    }

    // Suppression d'un membre
    public void deleteMembre(Long id) {
        membreRepository.deleteById(id);
    }

    // Vérifie si un membre existe par son ID
    public boolean existsById(Long id) {
        return membreRepository.existsById(id);
    }

    public Optional<Membre> findById(Long id) {
        return membreRepository.findById(id);
    }

}

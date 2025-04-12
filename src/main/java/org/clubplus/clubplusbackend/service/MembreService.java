package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor // Injection de dépendances via constructeur Lombok
@Transactional // Toutes les méthodes publiques seront transactionnelles par défaut
public class MembreService {

    private final MembreDao membreRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Membre> findAllMembres() {
        return membreRepository.findAll();
    }

    public Optional<Membre> findMembreById(Integer id) {
        return membreRepository.findById(id);
    }

    public Membre getMembreByIdOrThrow(Integer id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membre non trouvé avec l'ID : " + id));

    }

    public Optional<Membre> findMembreByEmail(String email) {
        return membreRepository.findByEmail(email);
    }


    public Membre createMembre(Membre membre) {
        // Logique avant la sauvegarde :
        // 1. Vérifier si l'email existe déjà
        membreRepository.findByEmail(membre.getEmail()).ifPresent(m -> {
            throw new IllegalArgumentException("Un membre avec l'email " + membre.getEmail() + " existe déjà.");
        });

        membre.setPassword(passwordEncoder.encode(membre.getPassword()));

        // 3. Définir la date d'inscription
        membre.setDate_inscription(LocalDate.now());

        // 4. Attribuer un rôle par défaut si nécessaire (ex: MEMBRE)
        if (membre.getRole() == null) {
            membre.setRole(Role.MEMBRE);
        }

        return membreRepository.save(membre);
    }

    public Membre updateMembre(Integer id, Membre membreDetails) {
        Membre existingMembre = getMembreByIdOrThrow(id);

        // Mettre à jour les champs modifiables
        existingMembre.setNom(membreDetails.getNom());
        existingMembre.setPrenom(membreDetails.getPrenom());
        existingMembre.setDate_naissance(membreDetails.getDate_naissance());
        existingMembre.setNumero_voie(membreDetails.getNumero_voie());
        existingMembre.setRue(membreDetails.getRue());
        existingMembre.setCodepostal(membreDetails.getCodepostal());
        existingMembre.setVille(membreDetails.getVille());
        existingMembre.setTelephone(membreDetails.getTelephone());

        // Gérer la mise à jour de l'email (vérifier l'unicité si l'email change)
        if (!existingMembre.getEmail().equals(membreDetails.getEmail())) {
            membreRepository.findByEmail(membreDetails.getEmail()).ifPresent(m -> {
                if (!m.getId().equals(id)) { // Assurez-vous que ce n'est pas le même utilisateur
                    throw new IllegalArgumentException("Un autre membre utilise déjà l'email " + membreDetails.getEmail());
                }
            });
            existingMembre.setEmail(membreDetails.getEmail());
        }

        // Gérer la mise à jour du rôle si nécessaire (peut nécessiter des droits spécifiques)
        existingMembre.setRole(membreDetails.getRole());


        return membreRepository.save(existingMembre);
    }

    public void deleteMembre(Integer id) {
        if (!membreRepository.existsById(id)) {
            throw new EntityNotFoundException("Impossible de supprimer, Membre non trouvé avec l'ID : " + id);
        }
        membreRepository.deleteById(id);
    }

    public List<Membre> findMembresByClubId(Integer clubId) {
        return membreRepository.findByClubId(clubId);
    }
}

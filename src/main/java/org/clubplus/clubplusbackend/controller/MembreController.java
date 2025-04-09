package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/membres")
public class MembreController {

    private final MembreService membreService;
    private final ClubDao clubRepository;
    private final MembreDao membreRepository;

    public MembreController(MembreService membreService, ClubDao clubRepository, MembreDao membreRepository) {
        this.membreService = membreService;
        this.clubRepository = clubRepository;
        this.membreRepository = membreRepository;
    }

    // Récupérer un membre par son ID
    @GetMapping("/{id}")
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> getMembre(@PathVariable Long id) {
        return membreService.getMembreById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Récupérer tous les membres
    @GetMapping
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<List<Membre>> getAllMembres() {
        List<Membre> membres = membreService.getAllMembres();
        return ResponseEntity.ok(membres);
    }

    // Créer un nouveau membre
    @PostMapping
    public ResponseEntity<Membre> createMembre(@RequestBody Membre membre) {
        Membre nouveauMembre = membreService.save(membre);
        return new ResponseEntity<>(nouveauMembre, HttpStatus.CREATED);
    }

    // Supprimer un membre
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembre(@PathVariable Long id) {
        if (!membreService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        membreService.deleteMembre(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Mettre à jour un membre
    @PutMapping("/{id}")
    public ResponseEntity<Membre> updateMembre(@PathVariable Long id, @RequestBody Membre membre) {
        if (!membreService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        membre.setId(id); // Assurer que l'ID est correctement défini
        Membre updatedMembre = membreService.save(membre);

        return ResponseEntity.ok(updatedMembre);
    }

    // VÉRIFIEZ QUE VOTRE CODE RESSEMBLE EXACTEMENT À CECI :
    @PostMapping("/inscription/{codeClub}") // <-- {codeClub} dans le chemin
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> registerMembre(
            @Valid @RequestBody Membre membre, // <- Corps JSON SANS codeClub
            @PathVariable String codeClub      // <- @PathVariable pour le code depuis l'URL
    ) {
        // 1. Trouver le club avec le codeClub de l'URL
        Club club = clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club introuvable avec code: " + codeClub));

        // 2. Associer le club et définir les autres champs serveur
        membre.setClub(club);
        membre.setDate_inscription(String.valueOf(LocalDate.now()));
        membre.setRole("membre");
        // Gérer le mot de passe...
        membre.setPassword(membre.getPassword()); // Hacher si nécessaire

        // 3. Sauvegarder le membre
        Membre savedMembre = membreRepository.save(membre);
        return ResponseEntity.ok(savedMembre);
    }
}

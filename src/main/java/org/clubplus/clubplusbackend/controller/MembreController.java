package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/membres")
public class MembreController {

    private final MembreService membreService;

    public MembreController(MembreService membreService) {
        this.membreService = membreService;
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

}

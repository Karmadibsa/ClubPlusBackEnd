package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // Dans votre MembreController.java
    @PatchMapping("/{id}")
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> updatePartialMembre(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        // Vérifier si le membre existe
        if (!membreService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Récupérer le membre existant
        Membre existingMembre = membreService.findById(id).orElseThrow();

        // Mettre à jour uniquement les champs fournis
        if (updates.containsKey("nom")) {
            existingMembre.setNom((String) updates.get("nom"));
        }
        if (updates.containsKey("prenom")) {
            existingMembre.setPrenom((String) updates.get("prenom"));
        }
        if (updates.containsKey("date_naissance")) {
            existingMembre.setDate_naissance((String) updates.get("date_naissance"));
        }
        if (updates.containsKey("email")) {
            existingMembre.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("telephone")) {
            existingMembre.setTelephone((String) updates.get("telephone"));
        }
        if (updates.containsKey("role")) {
            existingMembre.setRole((String) updates.get("role"));
            System.out.println("Role updated to: " + updates.get("role")); // Log pour vérifier
        }

        if (updates.containsKey("numero_voie")) {
            existingMembre.setNumero_voie((String) updates.get("numero_voie"));
        }
        if (updates.containsKey("rue")) {
            existingMembre.setRue((String) updates.get("rue"));
        }
        if (updates.containsKey("codepostal")) {
            existingMembre.setCodepostal((String) updates.get("codepostal"));
        }
        if (updates.containsKey("ville")) {
            existingMembre.setVille((String) updates.get("ville"));
        }

        // Sauvegarder les modifications
        Membre updatedMembre = membreService.save(existingMembre);
        return ResponseEntity.ok(updatedMembre);
    }


}

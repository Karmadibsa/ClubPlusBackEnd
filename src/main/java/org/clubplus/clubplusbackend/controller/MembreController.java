package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.service.MembreService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membres") // Préfixe commun pour les endpoints des membres
@RequiredArgsConstructor
@CrossOrigin
public class MembreController {

    private final MembreService membreService;

    // GET /api/membres - Récupérer tous les membres (vue de base)
    @GetMapping
    @JsonView(GlobalView.Base.class)
    public List<Membre> getAllMembres() {
        return membreService.findAllMembres();
    }

    // GET /api/membres/{id} - Récupérer un membre par ID (vue détaillée)
    @GetMapping("/{id}")
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> getMembreById(@PathVariable Integer id) {
        return membreService.findMembreById(id)
                .map(ResponseEntity::ok) // Si trouvé, retourne 200 OK avec le membre
                .orElse(ResponseEntity.notFound().build()); // Sinon, retourne 404 Not Found
    }

    // POST /api/membres - Créer un nouveau membre
    @PostMapping
    @JsonView(GlobalView.MembreView.class) // Retourner la vue détaillée du membre créé
    public ResponseEntity<Membre> createMembre(@Valid @RequestBody Membre membre) {
        Membre createdMembre = membreService.createMembre(membre);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMembre); // Alternative plus simple
    }

    // PUT /api/membres/{id} - Mettre à jour un membre existant
    @PutMapping("/{id}")
    @JsonView(GlobalView.MembreView.class) // Retourner la vue détaillée du membre mis à jour
    public ResponseEntity<Membre> updateMembre(@PathVariable Integer id, @Valid @RequestBody Membre membreDetails) {
        try {
            Membre updatedMembre = membreService.updateMembre(id, membreDetails);
            return ResponseEntity.ok(updatedMembre); // 200 OK
        } catch (EntityNotFoundException e) { // Ou ResourceNotFoundException
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // 400 Bad Request (ex: email déjà pris) - idéalement retourner un message d'erreur
        }
    }

    // DELETE /api/membres/{id} - Supprimer un membre
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembre(@PathVariable Integer id) {
        try {
            membreService.deleteMembre(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) { // Ou ResourceNotFoundException
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    // Ajoutez d'autres endpoints si nécessaire (ex: rechercher par email, etc.)
    // GET /api/membres/email/{email}
    @GetMapping("/email/{email}")
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<Membre> getMembreByEmail(@PathVariable String email) {
        return membreService.findMembreByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

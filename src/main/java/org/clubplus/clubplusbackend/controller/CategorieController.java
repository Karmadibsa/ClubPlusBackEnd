package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.clubplus.clubplusbackend.model.Categorie;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    // Récupérer un categorie par son ID
    @GetMapping("/{id}")
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<Categorie> getCategorie(@PathVariable Long id) {
        return categorieService.getCategorieById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Récupérer tous les categories
    @GetMapping
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<List<Categorie>> getAllCategories() {
        List<Categorie> categories = categorieService.getAllCategories();
        return ResponseEntity.ok(categories);
    }


    // Créer un nouveau categorie
    @PostMapping
    public ResponseEntity<Categorie> createCategorie(@RequestBody Categorie categorie) {
        Categorie nouveauCategorie = categorieService.save(categorie);
        return new ResponseEntity<>(nouveauCategorie, HttpStatus.CREATED);
    }

    // Supprimer un categorie
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategorie(@PathVariable Long id) {
        if (!categorieService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        categorieService.deleteCategorie(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Mettre à jour un categorie
    @PutMapping("/{id}")
    @JsonView(GlobalView.CategorieView.class)
    public ResponseEntity<Categorie> updateCategorie(@PathVariable Long id, @RequestBody Categorie categorie) {
        if (!categorieService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        categorie.setId(id); // Assurer que l'ID est correctement défini
        Categorie updatedCategorie = categorieService.save(categorie);

        return ResponseEntity.ok(updatedCategorie);
    }

}

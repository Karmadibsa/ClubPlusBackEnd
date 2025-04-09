package org.clubplus.clubplusbackend.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.transaction.Transactional;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.EventDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Event;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.service.CategorieService;
import org.clubplus.clubplusbackend.service.ClubService;
import org.clubplus.clubplusbackend.view.GlobalView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/club")
public class ClubController {

    private final ClubDao clubRepository;
    private final MembreDao membreRepository;
    private final EventDao eventRepository;
    //    private final PasswordEncoder passwordEncoder;
    private final ClubService clubService;
    private final CategorieService categorieService;

    public ClubController(ClubDao clubRepository, MembreDao membreRepository, EventDao eventRepository, ClubService clubService, CategorieService categorieService) {
        this.clubRepository = clubRepository;
        this.membreRepository = membreRepository;
        this.eventRepository = eventRepository;
        this.clubService = clubService;
        this.categorieService = categorieService;
    }


    @GetMapping
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<List<Club>> getAllClubs() {
        List<Club> clubs = clubService.getAllClubs();
        return ResponseEntity.ok(clubs);
    }

    @GetMapping("/{id}")
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> getClubById(@PathVariable Long id) {
        return clubRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club introuvable"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClub(@PathVariable Long id) {
        if (clubService.getClubById(id).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        clubService.deleteClub(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/inscription")
    @Transactional
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> registerClub(@RequestBody Club clubRequest) {
        // Validation obligatoire
        if (clubRequest.getAdmin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un administrateur est requis");
        }

        // Vérification du mot de passe
        Membre admin = clubRequest.getAdmin();
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe de l'administrateur est requis");
        }

        // Configuration des dates
        clubRequest.setDate_creation(String.valueOf(LocalDate.now()));
        clubRequest.setDate_inscription(String.valueOf(LocalDate.now()));

        // Configuration de l'admin
        admin.setRole("admin");
        admin.setDate_inscription(String.valueOf(LocalDate.now()));

        // Hashage du mot de passe (à décommenter après configuration)
        // admin.setPassword(passwordEncoder.encode(admin.getPassword()));

        // Sauvegarde en cascade
        Club savedClub = clubRepository.save(clubRequest);
        admin.setClub(savedClub);
        membreRepository.save(admin);

        return ResponseEntity.ok(savedClub);
    }


    @PatchMapping("/{id}")
    @JsonView(GlobalView.ClubView.class)
    public ResponseEntity<Club> patchClub(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        return clubRepository.findById(id)
                .map(existingClub -> {
                    // Mise à jour des champs en fonction des clés présentes dans le JSON
                    updates.forEach((key, value) -> {
                        switch (key) {
                            case "nom":
                                existingClub.setNom((String) value);
                                break;
                            case "numero_voie":
                                existingClub.setNumero_voie((String) value);
                                break;
                            case "rue":
                                existingClub.setRue((String) value);
                                break;
                            case "codepostal":
                                existingClub.setCodepostal((String) value);
                                break;
                            case "ville":
                                existingClub.setVille((String) value);
                                break;
                            case "telephone":
                                existingClub.setTelephone((String) value);
                                break;
                            case "email":
                                existingClub.setEmail((String) value);
                                break;
                            default:
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ non modifiable : " + key);
                        }
                    });

                    Club updatedClub = clubRepository.save(existingClub);
                    return ResponseEntity.ok(updatedClub);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/membres")
    @JsonView(GlobalView.MembreView.class)
    public ResponseEntity<List<Membre>> getMembresByClub(@PathVariable Long id) {
        List<Membre> membres = membreRepository.findByClubId(id);
        if (membres.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun membre trouvé pour ce club");
        }
        return ResponseEntity.ok(membres);
    }

    @GetMapping("/{id}/evenements")
    @JsonView(GlobalView.EventView.class)
    public ResponseEntity<List<Event>> getEvenementsByClub(@PathVariable Long id) {
        List<Event> evenements = eventRepository.findByOrganisateurId(id);
        if (evenements.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun événement trouvé pour ce club");
        }
        return ResponseEntity.ok(evenements);
    }

}

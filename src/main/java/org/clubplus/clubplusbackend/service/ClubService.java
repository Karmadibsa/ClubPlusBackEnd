package org.clubplus.clubplusbackend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dao.ClubDao;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubService {

    private final ClubDao clubRepository;
    private final MembreDao membreRepository; // Pour gérer l'admin
    private final PasswordEncoder passwordEncoder; // Pour gérer l'admin

    public List<Club> findAllClubs() {
        return clubRepository.findAll();
    }

    public Optional<Club> findClubById(Integer id) {
        return clubRepository.findById(id);
    }

    public Optional<Club> findClubByCode(String codeClub) {
        return clubRepository.findByCodeClub(codeClub);
    }

    public Club getClubByIdOrThrow(Integer id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec l'ID : " + id));
        // .orElseThrow(() -> new ResourceNotFoundException("Club", "id", id));
    }

    public Club getClubByCodeOrThrow(String codeClub) {
        return clubRepository.findByCodeClub(codeClub)
                .orElseThrow(() -> new EntityNotFoundException("Club non trouvé avec le code : " + codeClub));
        // .orElseThrow(() -> new ResourceNotFoundException("Club", "codeClub", codeClub));
    }

    public Club createClub(Club club, Integer adminId) {
        // 1. Vérifier l'unicité de l'email du club
        if (clubRepository.existsByEmail(club.getEmail())) {
            throw new IllegalArgumentException("Un club avec l'email " + club.getEmail() + " existe déjà.");
        }

        // 2. Récupérer et lier l'administrateur
        Membre admin = membreRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Membre administrateur non trouvé avec l'ID : " + adminId));

        club.setAdmin(admin);

        // 3. Définir les dates système
        club.setDate_inscription(LocalDate.now());

        // 4. Initialiser les listes pour éviter les NullPointerException
        if (club.getMembres() == null) {
            club.setMembres(new java.util.ArrayList<>());
        }
        if (club.getEvenements() == null) {
            club.setEvenements(new java.util.ArrayList<>());
        }

        // 5. Sauvegarder le club. Le codeClub sera généré par @PostPersist APRÈS cette sauvegarde.
        Club savedClub = clubRepository.save(club);

        // Note: Le codeClub est généré par @PostPersist dans l'entité Club elle-même.
        // Pas besoin de le générer explicitement ici. L'objet 'savedClub'
        // retourné par save() contiendra le code généré si la transaction est commitée
        // ou si l'entité est rafraîchie/récupérée après la persistance.

        return savedClub; // Retourne le club avec son ID et potentiellement son codeClub
    }

    public Club updateClub(Integer id, Club clubDetails) {
        Club existingClub = getClubByIdOrThrow(id);

        // Mettre à jour les champs modifiables
        existingClub.setNom(clubDetails.getNom());
        existingClub.setDate_creation(clubDetails.getDate_creation()); // Si modifiable
        existingClub.setNumero_voie(clubDetails.getNumero_voie());
        existingClub.setRue(clubDetails.getRue());
        existingClub.setCodepostal(clubDetails.getCodepostal());
        existingClub.setVille(clubDetails.getVille());
        existingClub.setTelephone(clubDetails.getTelephone());

        // Gérer la mise à jour de l'email (vérifier l'unicité si l'email change)
        if (!existingClub.getEmail().equals(clubDetails.getEmail())) {
            if (clubRepository.existsByEmail(clubDetails.getEmail())) {
                throw new IllegalArgumentException("Un autre club utilise déjà l'email " + clubDetails.getEmail());
            }
            existingClub.setEmail(clubDetails.getEmail());
        }

        return clubRepository.save(existingClub);
    }

    public void deleteClub(Integer id) {
        Club club = getClubByIdOrThrow(id);

        boolean hasFutureEvents = club.getEvenements().stream()
                .anyMatch(event -> event.getStart() != null && event.getStart().isAfter(LocalDateTime.now()));
        if (hasFutureEvents) {
            throw new IllegalStateException("Impossible de supprimer le club '" + club.getNom() + "' car il a des événements futurs planifiés.");
        }
        clubRepository.delete(club);
    }

    /**
     * Crée un Club et son Admin initial à partir d'un objet Club contenant
     * les détails de l'admin dans son champ 'admin'.
     * ATTENTION: Moins sécurisé et plus complexe que l'approche DTO.
     *
     * @param clubInput L'objet Club reçu de la requête, contenant un Membre transient dans .admin.
     * @return Le Club persisté.
     * @throws IllegalArgumentException Si emails dupliqués ou admin manquant/invalide.
     * @throws IllegalStateException    Si PasswordEncoder n'est pas configuré.
     */
    public Club createClubAndAdminFromInput(Club clubInput) {
        Membre adminTransient = clubInput.getAdmin();

        if (adminTransient == null) {
            throw new IllegalArgumentException("Les données de l'administrateur sont manquantes.");
        }
        if (adminTransient.getEmail() == null || adminTransient.getPassword() == null) {
            throw new IllegalArgumentException("L'email et le mot de passe de l'administrateur sont requis.");
        }


        // --- Validation Préalable ---
        membreRepository.findByEmail(adminTransient.getEmail()).ifPresent(m -> {
            throw new IllegalArgumentException("Un membre avec l'email " + adminTransient.getEmail() + " existe déjà.");
        });
        if (clubRepository.existsByEmail(clubInput.getEmail())) {
            throw new IllegalArgumentException("Un club avec l'email " + clubInput.getEmail() + " existe déjà.");
        }

        // --- Préparation et Sauvegarde du Membre Admin ---
        Membre adminToSave = new Membre();
        // Copier les champs depuis l'objet transient (plus sûr que d'utiliser l'objet transient directement)
        adminToSave.setNom(adminTransient.getNom());
        adminToSave.setPrenom(adminTransient.getPrenom());
        adminToSave.setDate_naissance(adminTransient.getDate_naissance());
        adminToSave.setNumero_voie(adminTransient.getNumero_voie());
        adminToSave.setRue(adminTransient.getRue());
        adminToSave.setCodepostal(adminTransient.getCodepostal());
        adminToSave.setVille(adminTransient.getVille());
        adminToSave.setTelephone(adminTransient.getTelephone());
        adminToSave.setEmail(adminTransient.getEmail());
        // Hacher le mot de passe !!!
        adminToSave.setPassword(passwordEncoder.encode(adminTransient.getPassword()));
        adminToSave.setDate_inscription(LocalDate.now());
        adminToSave.setRole(Role.ADMIN); // Forcer le rôle

        // Sauvegarder l'admin d'abord
        Membre savedAdmin = membreRepository.save(adminToSave);

        // --- Préparation et Sauvegarde du Club ---
        // Utiliser l'objet clubInput mais s'assurer que les champs sensibles sont corrects
        clubInput.setAdmin(savedAdmin); // Lier l'admin persisté
        clubInput.setDate_inscription(LocalDate.now()); // Définir la date d'inscription du club
        clubInput.setCodeClub(null); // S'assurer que le code n'est pas défini par le client
        clubInput.setId(null);       // S'assurer que l'ID n'est pas défini par le client
        // Initialiser les listes si elles sont nulles
        if (clubInput.getMembres() == null) {
            clubInput.setMembres(new java.util.ArrayList<>());
        }
        if (clubInput.getEvenements() == null) {
            clubInput.setEvenements(new java.util.ArrayList<>());
        }

        // Sauvegarder le club
        Club savedClub = clubRepository.save(clubInput); // @PostPersist générera le codeClub

        // --- Mise à jour de l'Admin avec le Club (relation bidirectionnelle) ---
        savedAdmin.setClub(savedClub);
        membreRepository.save(savedAdmin); // Mettre à jour l'admin avec la référence au club

        return savedClub; // Retourner le club sauvegardé
    }

}

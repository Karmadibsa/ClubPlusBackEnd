package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Classe de test pour l'interface {@link ClubDao}.
 * Utilise {@code @DataJpaTest} pour configurer un environnement de test JPA ciblé,
 * avec une base de données en mémoire (H2 par défaut) et une gestion transactionnelle
 * avec rollback automatique après chaque test. Cela garantit l'isolation des tests
 * et un état de base de données propre pour chaque exécution de test.
 */
@DataJpaTest
public class ClubDaoTest {

    /**
     * {@link TestEntityManager} est une alternative à l'EntityManager standard,
     * spécifiquement conçue pour les tests. Il est utilisé pour les opérations de persistance
     * de bas niveau telles que la création d'entités (persist), la synchronisation
     * avec la base de données (flush), et leur récupération (find).
     */
    @Autowired
    private TestEntityManager entityManager;

    /**
     * Injection de l'interface {@link ClubDao} à tester.
     * Spring Data JPA fournira une implémentation concrète de cette interface au moment de l'exécution.
     */
    @Autowired
    private ClubDao clubDao;

    // Entités de test qui peuvent être initialisées dans les méthodes de test.
    private Club club1;
    private Club club2;

    /**
     * Méthode utilitaire pour créer une instance de {@link Club} avec des valeurs par défaut valides.
     * Cette méthode centralise la création d'objets Club pour les tests, assurant la cohérence
     * et réduisant la duplication de code. Les valeurs fournies doivent respecter
     * les contraintes de validation de l'entité Club.
     *
     * @param nom      Le nom du club.
     * @param email    L'email unique du club.
     * @param codeClub Le code unique du club (format attendu: CLUB-XXXX, max 9 caractères).
     * @return Une nouvelle instance de {@link Club}, prête à être persistée ou utilisée dans les tests.
     */
    private Club createValidClubTemplate(String nom, String email, String codeClub) {
        Club club = new Club();
        club.setNom(nom);
        club.setEmail(email);

        // Le codeClub doit respecter la longueur maximale de 9 caractères définie dans l'entité.
        if (codeClub != null && codeClub.length() > 9) {
            throw new IllegalArgumentException("Le codeClub généré pour le test est trop long : '" + codeClub + "' (max 9 caractères).");
        }
        club.setCodeClub(codeClub);
        club.setActif(true);
        // Initialisation de tous les champs obligatoires (non nuls) de l'entité Club.
        club.setDate_creation(LocalDate.now().minusMonths(6).atStartOfDay(ZoneOffset.UTC).toInstant());

// Pour la date d'inscription (il y a 5 mois, début du jour UTC)
        club.setDate_inscription(LocalDate.now().minusMonths(5).atStartOfDay(ZoneOffset.UTC).toInstant());
        club.setNumero_voie("123");
        club.setRue("Rue du Test Valide");
        club.setCodepostal("75000");
        club.setVille("Testville");
        club.setTelephone("0123456789");
        return club;
    }

    /**
     * Configuration exécutée avant chaque méthode de test (@Test).
     * Dans le contexte de {@code @DataJpaTest} avec {@code @Transactional}, la base de données
     * est généralement propre avant chaque test grâce au rollback.
     * La création de données spécifiques est donc souvent faite directement dans chaque méthode de test
     * pour une meilleure clarté et isolation.
     */
    @BeforeEach
    void setUp() {
        // Aucune initialisation globale d'entités ici, pour favoriser l'indépendance des tests.
        // Les instances club1, club2 seront initialisées dans chaque test selon les besoins.
    }

    // --- Tests pour les méthodes CRUD de base (héritées de JpaRepository) ---

    /**
     * Teste la persistance d'un nouveau club.
     * Vérifie que le club est sauvegardé, qu'un ID est généré,
     * et que les données persistées correspondent aux données initiales.
     */
    @Test
    @DisplayName("Sauvegarde d'un nouveau club avec succès")
    void quandSauvegardeNouveauClub_alorsClubEstPersisteAvecId() {
        // Given: Crée une instance de Club valide.
        club1 = createValidClubTemplate("Super Club Test", "contact@superclub.com", "CLUB-0001");

        // When: Sauvegarde le club via le DAO.
        Club savedClub = clubDao.save(club1);

        // Then: Vérifie les propriétés du club sauvegardé.
        assertThat(savedClub).isNotNull();
        assertThat(savedClub.getId()).isNotNull().isPositive(); // ID auto-généré doit être positif.
        assertThat(savedClub.getNom()).isEqualTo("Super Club Test");
        assertThat(savedClub.getEmail()).isEqualTo("contact@superclub.com");
        assertThat(savedClub.getCodeClub()).isEqualTo("CLUB-0001");

        // Vérification optionnelle : récupérer directement de la base via TestEntityManager.
        Club foundInDbViaEm = entityManager.find(Club.class, savedClub.getId());
        assertThat(foundInDbViaEm).isNotNull();
        assertThat(foundInDbViaEm.getNom()).isEqualTo("Super Club Test");
    }

    /**
     * Teste la recherche d'un club par son ID lorsque le club existe.
     */
    @Test
    @DisplayName("Recherche par ID d'un club existant")
    void quandRechercheParId_siClubExiste_alorsRetourneClub() {
        // Given: Persiste un club de test.
        club1 = createValidClubTemplate("Club Cherchable", "search@club.com", "CLUB-0002");
        // Utilise TestEntityManager pour s'assurer que l'entité est bien dans le contexte de persistance et flushée.
        entityManager.persistAndFlush(club1);

        // When: Recherche le club par son ID.
        Optional<Club> foundClubOpt = clubDao.findById(club1.getId());

        // Then: Vérifie que le club est trouvé.
        assertThat(foundClubOpt).isPresent();
        assertThat(foundClubOpt.get().getNom()).isEqualTo("Club Cherchable");
        assertThat(foundClubOpt.get().getId()).isEqualTo(club1.getId());
    }

    /**
     * Teste la recherche d'un club par un ID qui n'existe pas.
     * S'attend à ce que le résultat soit un Optional vide.
     */
    @Test
    @DisplayName("Recherche par ID d'un club inexistant")
    void quandRechercheParId_siClubInexistant_alorsRetourneVide() {
        // When: Tente de trouver un club avec un ID hautement improbable.
        Optional<Club> foundClubOpt = clubDao.findById(Integer.MAX_VALUE);
        // Then: L'Optional doit être vide.
        assertThat(foundClubOpt).isEmpty();
    }

    // --- Tests pour les méthodes personnalisées de ClubDao ---

    /**
     * Teste la méthode {@code findByCodeClub} pour un code club existant.
     */
    @Test
    @DisplayName("Recherche par codeClub - Code existant")
    void quandRechercheParCodeClub_siCodeExiste_alorsRetourneClub() {
        // Given: Persiste un club avec un codeClub spécifique.
        String specificCode = "CLUB-SC01"; // 9 caractères
        club1 = createValidClubTemplate("Club Avec Code Spécifique", "code.specific@club.com", specificCode);
        entityManager.persistAndFlush(club1);

        // When: Recherche par ce codeClub.
        Optional<Club> found = clubDao.findByCodeClub(specificCode);

        // Then: Vérifie que le club est trouvé et que son code correspond.
        assertThat(found).isPresent();
        assertThat(found.get().getCodeClub()).isEqualTo(specificCode);
        assertThat(found.get().getId()).isEqualTo(club1.getId());
    }

    /**
     * Teste la méthode {@code findByCodeClub} pour un code club inexistant.
     */
    @Test
    @DisplayName("Recherche par codeClub - Code inexistant")
    void quandRechercheParCodeClub_siCodeInexistant_alorsRetourneVide() {
        // When: Recherche un club avec un code qui n'est pas censé exister.
        Optional<Club> found = clubDao.findByCodeClub("CLUB-XXXX");
        // Then: L'Optional doit être vide.
        assertThat(found).isEmpty();
    }

    /**
     * Teste la méthode {@code findByEmail} pour un email existant.
     */
    @Test
    @DisplayName("Recherche par email - Email existant")
    void quandRechercheParEmail_siEmailExiste_alorsRetourneClub() {
        // Given: Persiste un club avec un email spécifique.
        club1 = createValidClubTemplate("Club Email Test", "test.email@clubfind.com", "CLUB-EM01");
        entityManager.persistAndFlush(club1);

        // When: Recherche par cet email.
        Optional<Club> found = clubDao.findByEmail("test.email@clubfind.com");

        // Then: Vérifie que le club est trouvé.
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test.email@clubfind.com");
    }

    /**
     * Teste la méthode {@code existsByEmailAndIdNot} lorsqu'un autre club possède l'email.
     */
    @Test
    @DisplayName("Existence par email (excluant ID) - Email pris par un autre club")
    void quandExistenceParEmailEtIdExclu_siEmailPrisParAutre_alorsRetourneVrai() {
        // Given: Persiste deux clubs distincts.
        club1 = createValidClubTemplate("Club Email A", "email.a@club.com", "CLUB-EA01");
        entityManager.persistAndFlush(club1); // ID de club1 sera généré.
        club2 = createValidClubTemplate("Club Email B", "email.b@club.com", "CLUB-EB01");
        entityManager.persistAndFlush(club2); // ID de club2 sera généré, différent de club1.

        // When: Vérifie si l'email de club1 ("email.a@club.com") est utilisé par un club
        // dont l'ID N'EST PAS celui de club2.
        boolean exists = clubDao.existsByEmailAndIdNot(club1.getEmail(), club2.getId());

        // Then: Vrai, car club1 a cet email et son ID est différent de celui de club2.
        assertThat(exists).isTrue();
    }

    /**
     * Teste la méthode {@code existsByEmailAndIdNot} lorsque l'email n'appartient qu'au club
     * dont l'ID est exclu.
     */
    @Test
    @DisplayName("Existence par email (excluant ID) - Email appartenant seulement à l'ID exclu")
    void quandExistenceParEmailEtIdExclu_siEmailAppartientMemeIdExclu_alorsRetourneFaux() {
        // Given: Persiste un club.
        club1 = createValidClubTemplate("Club Unique Email", "unique.email@club.com", "CLUB-UE01");
        entityManager.persistAndFlush(club1);

        // When: Vérifie si l'email de club1 est utilisé par un club dont l'ID N'EST PAS celui de club1.
        boolean exists = clubDao.existsByEmailAndIdNot(club1.getEmail(), club1.getId());

        // Then: Faux, car l'email n'est utilisé par aucun *autre* club.
        assertThat(exists).isFalse();
    }

    // --- Tests de contraintes d'unicité (vérifiées par la base de données) ---

    /**
     * Teste qu'une {@link DataIntegrityViolationException} est levée lors de la tentative
     * de sauvegarde d'un club avec un email qui existe déjà.
     */
    @Test
    @DisplayName("Sauvegarde avec email dupliqué doit lever une exception d'intégrité")
    void quandSauvegardeClubAvecEmailDuplique_alorsLeveDataIntegrityViolationException() {
        // Given: Persiste un premier club.
        club1 = createValidClubTemplate("Club Initial Conflit Email", "conflit.email@club.com", "CLUB-CE01");
        clubDao.saveAndFlush(club1); // S'assurer qu'il est bien en base.

        // When & Then: Tente de persister un second club avec le même email.
        Club clubAvecMemeEmail = createValidClubTemplate("Second Club Conflit Email", "conflit.email@club.com", "CLUB-CE02");

        assertThrows(DataIntegrityViolationException.class, () -> {
            clubDao.saveAndFlush(clubAvecMemeEmail); // Le flush déclenche la vérification de la contrainte UNIQUE.
        });
    }

    /**
     * Teste qu'une {@link DataIntegrityViolationException} est levée lors de la tentative
     * de sauvegarde d'un club avec un codeClub qui existe déjà.
     */
    @Test
    @DisplayName("Sauvegarde avec codeClub dupliqué doit lever une exception d'intégrité")
    void quandSauvegardeClubAvecCodeClubDuplique_alorsLeveDataIntegrityViolationException() {
        // Given: Persiste un premier club.
        String codeDuplique = "CLUB-DUP1";
        club1 = createValidClubTemplate("Club Initial Conflit Code", "code.initial@club.com", codeDuplique);
        clubDao.saveAndFlush(club1);

        // When & Then: Tente de persister un second club avec le même codeClub.
        Club clubAvecMemeCode = createValidClubTemplate("Second Club Conflit Code", "code.second@club.com", codeDuplique);

        assertThrows(DataIntegrityViolationException.class, () -> {
            clubDao.saveAndFlush(clubAvecMemeCode);
        });
    }
}

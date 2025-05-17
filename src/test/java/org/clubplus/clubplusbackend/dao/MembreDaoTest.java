package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Classe de test pour l'interface {@link MembreDao}.
 * Utilise {@code @DataJpaTest} pour configurer un environnement de test JPA minimaliste,
 * avec une base de données en mémoire (H2 par défaut) et des transactions automatiques
 * avec rollback après chaque test. Cela garantit l'isolation des tests.
 */
@DataJpaTest
public class MembreDaoTest {

    /**
     * {@link TestEntityManager} fournit des méthodes pour travailler avec le contexte de persistance JPA
     * dans les tests, comme persister, trouver, et flusher des entités.
     * Il est particulièrement utile pour préparer l'état de la base de données avant d'appeler
     * les méthodes du DAO et pour vérifier l'état après.
     */
    @Autowired
    private TestEntityManager entityManager;

    /**
     * Injection de l'interface {@link MembreDao} à tester.
     * Spring Data JPA fournira une implémentation concrète de cette interface au moment de l'exécution.
     */
    @Autowired
    private MembreDao membreDao;

    // Entités de test qui peuvent être réutilisées ou initialisées dans les méthodes de test.
    private Membre membre1;
    private Membre membre2;
    private Membre adminMembre;
    private Club club1;
    private Club club2;

    /**
     * Méthode de configuration exécutée avant chaque méthode de test (@Test).
     * Utilisée ici pour créer et persister des entités Club qui sont des dépendances
     * pour certains tests de MembreDao (ex: recherche d'admin par club).
     * Grâce au rollback automatique de @DataJpaTest, la base est propre avant chaque test.
     */
    @BeforeEach
    void setUp() {
        // Création et persistance de club1
        club1 = new Club();
        club1.setNom("Club Alpha");
        club1.setCodeClub("ALPHA");
        club1.setActif(true);
        club1.setEmail("club.alpha@example.com");
        club1.setTelephone("111222333");
        club1.setNumero_voie("1A"); // Champ obligatoire
        club1.setRue("Rue Alpha");
        club1.setCodepostal("75001");
        club1.setVille("Paris");
        club1.setDate_creation(LocalDate.now().minusYears(2));
        club1.setDate_inscription(LocalDate.now().minusYears(2)); // Champ obligatoire pour Club
        entityManager.persistAndFlush(club1); // Persiste club1 et le rend visible pour les tests.

        // Création et persistance de club2
        club2 = new Club();
        club2.setNom("Club Beta");
        club2.setCodeClub("BETA");
        club2.setActif(true);
        club2.setEmail("club.beta@example.com");
        club2.setTelephone("444555666");
        club2.setNumero_voie("2B"); // Champ obligatoire
        club2.setRue("Rue Beta");
        club2.setCodepostal("75002");
        club2.setVille("Paris");
        club2.setDate_creation(LocalDate.now().minusYears(1));
        club2.setDate_inscription(LocalDate.now().minusYears(1)); // Champ obligatoire pour Club
        entityManager.persistAndFlush(club2);
    }

    /**
     * Méthode utilitaire pour créer une instance de {@link Membre} avec des valeurs par défaut valides.
     * Aide à réduire la duplication de code dans les tests.
     *
     * @param email   L'email du membre.
     * @param nom     Le nom du membre.
     * @param role    Le rôle du membre.
     * @param codeAmi Le code ami unique du membre.
     * @return Une instance de Membre prête à être persistée ou utilisée dans les tests.
     */
    private Membre createMembreTemplate(String email, String nom, Role role, String codeAmi) {
        Membre membre = new Membre();
        membre.setEmail(email);
        membre.setNom(nom);
        membre.setPrenom("PrenomDe" + nom);
        membre.setPassword("ValidPass1!"); // Doit respecter les contraintes de l'entité.
        membre.setRole(role);
        membre.setActif(true);
        membre.setVerified(true);
        membre.setDate_naissance(LocalDate.of(1990, 1, 15));
        membre.setDate_inscription(LocalDate.now().minusYears(1)); // Champ obligatoire.
        membre.setTelephone("0123456789"); // Doit respecter les contraintes.
        membre.setCodeAmi(codeAmi); // Doit respecter la taille max (11 caractères).
        // Les collections (comme adhesions) sont généralement initialisées par le constructeur de Membre.
        return membre;
    }


    // --- Tests pour les méthodes CRUD de base (héritées de JpaRepository) ---

    /**
     * Teste la sauvegarde d'un nouveau membre.
     * Vérifie que le membre est persisté et qu'un ID lui est assigné.
     */
    @Test
    @DisplayName("Sauvegarde d'un nouveau membre")
    void quandSauvegardeMembre_alorsMembreEstPersiste() {
        membre1 = createMembreTemplate("john.doe@example.com", "Doe", Role.MEMBRE, "AMIS-JD001");

        Membre savedMembre = membreDao.save(membre1);

        assertThat(savedMembre).isNotNull();
        assertThat(savedMembre.getId()).isNotNull().isPositive(); // Un ID généré devrait être positif.
        assertThat(savedMembre.getEmail()).isEqualTo("john.doe@example.com");

        Membre foundInDb = entityManager.find(Membre.class, savedMembre.getId());
        assertThat(foundInDb).isEqualTo(savedMembre); // Vérifie l'égalité des objets (si equals/hashCode sont bien implémentés).
    }

    /**
     * Teste la recherche d'un membre par son ID lorsqu'il existe.
     */
    @Test
    @DisplayName("Recherche par ID - Membre existant")
    void quandRechercheParId_alorsRetourneMembre() {
        membre1 = createMembreTemplate("jane.doe@example.com", "DoeJane", Role.MEMBRE, "AMIS-JD002");
        entityManager.persistAndFlush(membre1);

        Optional<Membre> foundMembreOpt = membreDao.findById(membre1.getId());

        assertThat(foundMembreOpt).isPresent();
        assertThat(foundMembreOpt.get().getEmail()).isEqualTo("jane.doe@example.com");
    }

    /**
     * Teste la recherche d'un membre par un ID qui n'existe pas.
     * S'attend à un Optional vide.
     */
    @Test
    @DisplayName("Recherche par ID - Membre inexistant")
    void quandRechercheParId_avecIdInexistant_alorsRetourneVide() {
        Optional<Membre> foundMembreOpt = membreDao.findById(99999);
        assertThat(foundMembreOpt).isEmpty();
    }

    // --- Tests pour les méthodes personnalisées de MembreDao ---

    /**
     * Teste la recherche d'un membre par email lorsque l'email existe.
     */
    @Test
    @DisplayName("Recherche par email - Email existant")
    void quandRechercheParEmail_avecEmailExistant_alorsRetourneMembre() {
        membre1 = createMembreTemplate("find.by.email@example.com", "EmailUser", Role.MEMBRE, "AMIS-EM001");
        entityManager.persistAndFlush(membre1);

        Optional<Membre> found = membreDao.findByEmail("find.by.email@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find.by.email@example.com");
    }

    /**
     * Teste la méthode {@code existsByEmail} lorsqu'un email existe.
     */
    @Test
    @DisplayName("Existence par email - Email existant")
    void quandExistenceParEmail_avecEmailExistant_alorsRetourneVrai() {
        membre1 = createMembreTemplate("exists.email@example.com", "ExistsMail", Role.MEMBRE, "AMIS-EX001");
        entityManager.persistAndFlush(membre1);

        boolean exists = membreDao.existsByEmail("exists.email@example.com");
        assertThat(exists).isTrue();
    }

    /**
     * Teste la méthode {@code existsByEmailAndIdNot} pour vérifier l'unicité de l'email
     * en excluant l'ID du membre lui-même (cas d'une mise à jour sans changement d'email).
     */
    @Test
    @DisplayName("Existence par email excluant ID - Email appartenant au même ID")
    void quandExistenceParEmailEtIdExclu_avecMemeId_alorsRetourneFaux() {
        membre1 = createMembreTemplate("unique.check.sameid@example.com", "UniqueSame", Role.MEMBRE, "AMIS-US001");
        entityManager.persistAndFlush(membre1);

        boolean exists = membreDao.existsByEmailAndIdNot("unique.check.sameid@example.com", membre1.getId());
        assertThat(exists).isFalse(); // L'email existe, mais seulement pour l'ID exclu.
    }

    /**
     * Teste la méthode {@code findAdminByClubId} lorsqu'un admin existe pour le club spécifié.
     * Nécessite de créer un Membre avec le rôle ADMIN et une Adhesion le liant au Club.
     */
    @Test
    @DisplayName("Recherche admin par ID de club - Admin existant")
    void quandRechercheAdminParIdClub_avecAdminExistantDansClub_alorsRetourneMembreAdmin() {
        adminMembre = createMembreTemplate("admin.club1@example.com", "AdminClubOne", Role.ADMIN, "AMIS-ADM001");
        entityManager.persistAndFlush(adminMembre);

        Adhesion adhesion = new Adhesion(adminMembre, club1); // Lier l'admin au club1.
        entityManager.persistAndFlush(adhesion);

        Optional<Membre> foundAdmin = membreDao.findAdminByClubId(club1.getId());

        assertThat(foundAdmin).isPresent();
        assertThat(foundAdmin.get().getId()).isEqualTo(adminMembre.getId());
        assertThat(foundAdmin.get().getRole()).isEqualTo(Role.ADMIN);
    }

    /**
     * Teste la méthode {@code findByAdhesionsClubId} pour récupérer tous les membres d'un club.
     */
    @Test
    @DisplayName("Recherche membres par ID de club - Membres présents")
    void quandRechercheParAdhesionsClubId_avecMembresDansClub_alorsRetourneListeMembres() {
        membre1 = createMembreTemplate("m1.club1@example.com", "Member1C1", Role.MEMBRE, "AMIS-M1C1");
        entityManager.persistAndFlush(membre1);
        entityManager.persistAndFlush(new Adhesion(membre1, club1));

        membre2 = createMembreTemplate("m2.club1@example.com", "Member2C1", Role.MEMBRE, "AMIS-M2C1");
        entityManager.persistAndFlush(membre2);
        entityManager.persistAndFlush(new Adhesion(membre2, club1));

        Membre membreAutreClub = createMembreTemplate("m.autreclub@example.com", "Autre", Role.MEMBRE, "AMIS-MAC1");
        entityManager.persistAndFlush(membreAutreClub);
        entityManager.persistAndFlush(new Adhesion(membreAutreClub, club2)); // Ce membre est dans club2.


        List<Membre> membersInClub1 = membreDao.findByAdhesionsClubId(club1.getId());

        assertThat(membersInClub1).hasSize(2);
        assertThat(membersInClub1).extracting(Membre::getId).containsExactlyInAnyOrder(membre1.getId(), membre2.getId());
    }

    /**
     * Teste la recherche d'un membre par son code ami lorsque celui-ci existe.
     */
    @Test
    @DisplayName("Recherche par code ami - Code existant")
    void quandRechercheParCodeAmi_avecCodeExistant_alorsRetourneMembre() {
        // "AMIS-" (5 chars) + 6 chars pour UUID = 11 chars max.
        String codeAmiUnique = "AMIS-" + UUID.randomUUID().toString().substring(0, 6);
        membre1 = createMembreTemplate("code.ami.user@example.com", "CodeUser", Role.MEMBRE, codeAmiUnique);
        entityManager.persistAndFlush(membre1);

        Optional<Membre> found = membreDao.findByCodeAmi(codeAmiUnique);

        assertThat(found).isPresent();
        assertThat(found.get().getCodeAmi()).isEqualTo(codeAmiUnique);
    }

    /**
     * Teste la contrainte d'unicité sur le champ email.
     * S'attend à une {@link DataIntegrityViolationException} lors de la tentative de sauvegarde
     * d'un membre avec un email déjà utilisé.
     */
    @Test
    @DisplayName("Sauvegarde avec email dupliqué")
    void quandSauvegardeMembreAvecEmailDuplique_alorsLeveDataIntegrityViolationException() {
        membre1 = createMembreTemplate("duplicate.email@example.com", "FirstUser", Role.MEMBRE, "AMIS-DP001");
        membreDao.saveAndFlush(membre1); // Persiste le premier membre.

        Membre membre2AvecMemeEmail = createMembreTemplate("duplicate.email@example.com", "SecondUser", Role.MEMBRE, "AMIS-DP002");

        // Tenter de persister le second membre avec le même email doit échouer.
        assertThrows(DataIntegrityViolationException.class, () -> {
            membreDao.saveAndFlush(membre2AvecMemeEmail);
            // Le flush est important ici pour forcer la vérification de la contrainte par la base de données.
        });
    }
}

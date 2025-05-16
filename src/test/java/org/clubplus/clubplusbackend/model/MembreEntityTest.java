package org.clubplus.clubplusbackend.model;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest // Configure un environnement de test JPA
public class MembreEntityTest {

    @Autowired
    private TestEntityManager entityManager; // Utilitaire pour manipuler les entités dans les tests

    // Méthode utilitaire pour créer un Membre valide de base
    private Membre createValidMembre() {
        Membre membre = new Membre();
        membre.setNom("TestNom");
        membre.setPrenom("TestPrenom");
        membre.setDate_naissance(LocalDate.of(1990, 1, 1));
        membre.setTelephone("0123456789");
        // Génère un email unique pour chaque test pour éviter les conflits de contrainte d'unicité
        membre.setEmail("test_" + UUID.randomUUID() + "@example.com");
        membre.setPassword("ValidPassword1!"); // Respecte le pattern
        membre.setRole(Role.MEMBRE);
        // actif et date_inscription ont des valeurs par défaut
        return membre;
    }

    @Test
    public void shouldPersistAndRetrieveMembre() {
        Membre membre = createValidMembre();

        Membre persistedMembre = entityManager.persistAndFlush(membre); // Persiste et force la synchro avec la BDD
        Membre foundMembre = entityManager.find(Membre.class, persistedMembre.getId());

        assertThat(foundMembre).isNotNull();
        assertThat(foundMembre.getNom()).isEqualTo("TestNom");
        assertThat(foundMembre.getEmail()).isEqualTo(membre.getEmail());
        assertThat(foundMembre.getActif()).isTrue(); // Vérifie la valeur par défaut
        assertThat(foundMembre.getDate_inscription()).isNotNull(); // Vérifie la valeur par défaut
    }

    @Test
    public void shouldGenerateCodeAmiAfterPersist() {
        Membre membre = createValidMembre();

        Membre persistedMembre = entityManager.persistAndFlush(membre);

        assertThat(persistedMembre.getId()).isNotNull();
        assertThat(persistedMembre.getCodeAmi()).isNotNull();
        assertThat(persistedMembre.getCodeAmi()).startsWith("AMIS-");
        // Vérifie que le codeAmi est formaté avec l'ID, par exemple "AMIS-000001" si l'ID est 1
        assertThat(persistedMembre.getCodeAmi()).isEqualTo(String.format("AMIS-%06d", persistedMembre.getId()));
    }

    @Test
    public void shouldFailToPersistMembreWithNullNom() {
        Membre membre = createValidMembre();
        membre.setNom(null); // Nom est @NotBlank et nullable=false

        // Selon la configuration, cela peut lever ConstraintViolationException (Bean Validation)
        // ou DataIntegrityViolationException (contrainte BDD not null) lors du flush.
        // Si Bean Validation est bien actif avant flush, ConstraintViolationException est attendue.
        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(membre);
        });
    }

    @Test
    public void shouldFailToPersistMembreWithInvalidEmailFormat() {
        Membre membre = createValidMembre();
        membre.setEmail("invalidemail"); // Email avec format invalide

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(membre);
        });
    }

    @Test
    public void shouldFailToPersistTwoMembresWithSameEmail() {
        Membre membre1 = createValidMembre();
        membre1.setEmail("unique.email@example.com");
        entityManager.persistAndFlush(membre1);

        Membre membre2 = createValidMembre();
        membre2.setEmail("unique.email@example.com"); // Même email

        // La base de données (même H2) devrait lever une exception à cause de la contrainte d'unicité
        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(membre2);
        });
    }

    @Test
    public void testAnonymizeDataLogic() {
        Membre membre = createValidMembre();
        Membre persistedMembre = entityManager.persistAndFlush(membre); // Nécessaire pour avoir un ID

        // Simuler des amis pour tester leur suppression
        Membre ami = createValidMembre();
        ami.setEmail("ami_" + UUID.randomUUID().toString() + "@example.com"); // Email unique pour l'ami
        Membre persistedAmi = entityManager.persistAndFlush(ami);

        persistedMembre.addAmi(persistedAmi);
        entityManager.flush(); // S'assurer que la relation d'amitié est persistée

        // Détacher et recharger pour s'assurer que l'état est propre avant l'anonymisation
        entityManager.detach(persistedMembre);
        entityManager.detach(persistedAmi);
        Membre membreToAnonymize = entityManager.find(Membre.class, persistedMembre.getId());
        assertThat(membreToAnonymize.getAmis()).isNotEmpty();

        membreToAnonymize.anonymizeData();

        // Vérifications après anonymisation (avant de re-persister)
        assertThat(membreToAnonymize.getNom()).isEqualTo("Utilisateur");
        assertThat(membreToAnonymize.getPrenom()).isEqualTo("Anonyme_id" + membreToAnonymize.getId());
        assertThat(membreToAnonymize.getEmail()).isEqualTo("anonymized_" + membreToAnonymize.getId() + "@example.com");
        assertThat(membreToAnonymize.getRole()).isEqualTo(Role.ANONYME);
        assertThat(membreToAnonymize.getAnonymizeDate()).isNotNull();
        assertThat(membreToAnonymize.getAmis()).isEmpty(); // Vérifie que la liste d'amis est vidée

        // Pour tester l'effet en BDD, il faudrait persister et marquer comme inactif
        // membreToAnonymize.setActif(false);
        // entityManager.persistAndFlush(membreToAnonymize);
        // Puis vérifier les données récupérées.
    }

    @Test
    public void testAddAndRemoveAmiBidirectionality() {
        Membre membre1 = createValidMembre();
        membre1.setEmail("membre1-" + UUID.randomUUID() + "@example.com");
        Membre membre2 = createValidMembre();
        membre2.setEmail("membre2-" + UUID.randomUUID() + "@example.com");

        Membre persistedMembre1 = entityManager.persistAndFlush(membre1);
        Membre persistedMembre2 = entityManager.persistAndFlush(membre2);

        // Ajout de l'ami
        persistedMembre1.addAmi(persistedMembre2);
        entityManager.flush(); // Persiste les changements

        // Recharger depuis la BDD pour vérifier
        Membre foundMembre1 = entityManager.find(Membre.class, persistedMembre1.getId());
        Membre foundMembre2 = entityManager.find(Membre.class, persistedMembre2.getId());

        assertThat(foundMembre1.getAmis()).contains(foundMembre2);
        assertThat(foundMembre2.getAmis()).contains(foundMembre1);

        // Retrait de l'ami
        foundMembre1.removeAmi(foundMembre2);
        entityManager.flush(); // Persiste les changements

        // Recharger à nouveau
        Membre reloadedMembre1 = entityManager.find(Membre.class, persistedMembre1.getId());
        Membre reloadedMembre2 = entityManager.find(Membre.class, persistedMembre2.getId());

        assertThat(reloadedMembre1.getAmis()).doesNotContain(reloadedMembre2);
        assertThat(reloadedMembre2.getAmis()).doesNotContain(reloadedMembre1);
    }
}

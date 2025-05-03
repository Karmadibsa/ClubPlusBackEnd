package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Interface Repository pour l'entité {@link Club}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} et des méthodes
 * personnalisées pour rechercher des clubs par des critères uniques ou vérifier l'existence.
 *
 * <p>Note: L'entité {@code Club} étant annotée avec {@code @Where(clause = "actif = true")},
 * toutes les méthodes de recherche (y compris celles héritées de JpaRepository comme {@code findById})
 * excluront par défaut les clubs marqués comme inactifs, sauf si la requête est explicitement
 * définie pour ignorer ou outrepasser cette clause (ce qui n'est pas le cas ici).</p>
 *
 * @see Club
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface ClubDao extends JpaRepository<Club, Integer> {

    /**
     * Recherche un club (actif par défaut, voir note sur @Where) par son code club unique.
     * Le code club est généralement généré après la création du club (ex: "CLUB-0001").
     *
     * @param codeClub Le code unique du club à rechercher.
     * @return Un {@link Optional} contenant le {@link Club} si trouvé (et actif), sinon un Optional vide.
     */
    Optional<Club> findByCodeClub(String codeClub);

    /**
     * Recherche un club (actif par défaut) par son adresse email exacte.
     * L'email est défini comme unique dans l'entité {@link Club}.
     *
     * @param email L'adresse email exacte du club à rechercher.
     * @return Un {@link Optional} contenant le {@link Club} si trouvé (et actif), sinon un Optional vide.
     */
    Optional<Club> findByEmail(String email);

    /**
     * Vérifie s'il existe un autre club (ayant un ID différent de celui fourni)
     * qui possède déjà l'adresse email spécifiée.
     * Cette méthode est cruciale pour valider l'unicité de l'email lors de la mise à jour
     * d'un club existant, en s'assurant que le nouvel email n'est pas déjà pris par un *autre* club.
     *
     * @param email L'adresse email à vérifier.
     * @param id    L'ID du club que l'on est en train de mettre à jour (à exclure de la vérification).
     * @return {@code true} si un autre club (actif ou inactif, selon l'implémentation exacte de @Where vs exists)
     * utilise déjà cet email, {@code false} sinon.
     */
    boolean existsByEmailAndIdNot(String email, Integer id);
}

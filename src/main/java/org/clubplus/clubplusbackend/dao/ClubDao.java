package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité {@link Club}.
 * Fournit les opérations CRUD et des requêtes pour trouver des clubs par des identifiants uniques.
 * <p>
 * Important: L'annotation {@code @Where(clause = "actif = true")} sur l'entité {@code Club}
 * filtre automatiquement toutes les requêtes pour ne retourner que les clubs actifs.
 * </p>
 */
@Repository
public interface ClubDao extends JpaRepository<Club, Integer> {

    /**
     * Recherche un club par son code unique.
     * <p>
     * Retourne un club actif uniquement (via l'annotation @Where sur l'entité).
     *
     * @param codeClub Le code unique du club.
     * @return Un {@link Optional} contenant le club s'il est trouvé.
     */
    Optional<Club> findByCodeClub(String codeClub);

    /**
     * Recherche un club par son adresse email unique.
     * <p>
     * Retourne un club actif uniquement (via l'annotation @Where sur l'entité).
     *
     * @param email L'adresse email du club.
     * @return Un {@link Optional} contenant le club s'il est trouvé.
     */
    Optional<Club> findByEmail(String email);

    /**
     * Vérifie si un email est déjà utilisé par un autre club (ID différent).
     * <p>
     * Essentiel pour la validation lors de la mise à jour d'un club,
     * pour éviter les doublons d'email.
     *
     * @param email L'adresse email à vérifier.
     * @param id    L'ID du club à exclure de la recherche.
     * @return {@code true} si l'email est déjà utilisé par un autre club, {@code false} sinon.
     */
    boolean existsByEmailAndIdNot(String email, Integer id);
}

package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface Repository pour l'entité {@link Membre}.
 * Fournit les méthodes CRUD de base via {@link JpaRepository} et des méthodes
 * personnalisées pour rechercher des membres par email, code ami, appartenance à un club,
 * ou rôle spécifique (admin), ainsi que pour vérifier l'unicité de l'email.
 *
 * <p>Note: L'entité {@code Membre} étant annotée avec {@code @Where(clause = "actif = true")},
 * la plupart des méthodes de recherche excluront par défaut les membres marqués comme inactifs.</p>
 *
 * @see Membre
 * @see JpaRepository
 */
@Repository // Indique un bean Repository géré par Spring.
public interface MembreDao extends JpaRepository<Membre, Integer> {

    /**
     * Recherche un membre (actif par défaut) par son adresse email exacte.
     * Principalement utilisé pour le processus de connexion et pour vérifier si un email existe déjà.
     *
     * @param email L'adresse email du membre à rechercher.
     * @return Un {@link Optional} contenant le {@link Membre} si trouvé (et actif), sinon un Optional vide.
     */
    Optional<Membre> findByEmail(String email);

    /**
     * Vérifie de manière optimisée si un membre (actif ou inactif, selon l'implémentation exacte de @Where vs exists)
     * existe déjà avec l'adresse email spécifiée.
     * Utile lors de la création d'un nouveau membre pour garantir l'unicité de l'email.
     *
     * @param email L'adresse email à vérifier.
     * @return {@code true} si l'email est déjà utilisé, {@code false} sinon.
     */
    boolean existsByEmail(String email);

    /**
     * Vérifie s'il existe un *autre* membre (ayant un ID différent de celui fourni)
     * qui possède déjà l'adresse email spécifiée.
     * Très utile pour valider l'unicité de l'email lors de la mise à jour du profil d'un membre existant.
     *
     * @param email L'adresse email à vérifier.
     * @param id    L'ID du membre que l'on met à jour (à exclure de la vérification).
     * @return {@code true} si un autre membre utilise déjà cet email, {@code false} sinon.
     */
    boolean existsByEmailAndIdNot(String email, Integer id);

    /**
     * Recherche le membre unique qui a le rôle {@code ADMIN} et qui est associé
     * (via une {@link org.clubplus.clubplusbackend.model.Adhesion}) au club spécifié.
     * <p>
     * Suppose qu'il ne peut y avoir qu'un seul administrateur par club selon ce modèle.
     * </p>
     *
     * @param clubId L'ID du {@link Club} dont on cherche l'administrateur.
     * @return Un {@link Optional} contenant le {@link Membre} administrateur si trouvé, sinon un Optional vide.
     * @see Role#ADMIN
     */
    // Correction appliquée pour utiliser la chaîne 'ADMIN' ou un paramètre Enum
    @Query("SELECT m FROM Membre m JOIN m.adhesions a WHERE a.club.id = :clubId AND m.role = 'ADMIN'")
    // Ou, si vous préférez passer l'Enum (recommandé) :
    // @Query("SELECT m FROM Membre m JOIN m.adhesions a WHERE a.club.id = :clubId AND m.role = :roleAdmin")
    // Optional<Membre> findAdminByClubId(@Param("clubId") Integer clubId, @Param("roleAdmin") Role roleAdmin); // Si vous utilisez la version avec paramètre
    Optional<Membre> findAdminByClubId(@Param("clubId") Integer clubId); // Version avec 'ADMIN' codé en dur


    /**
     * Recherche tous les membres (actifs par défaut) qui ont une adhésion au club spécifié.
     * Méthode dérivée automatiquement par Spring Data JPA basée sur le nom et la structure des entités.
     *
     * @param clubId L'ID du {@link Club}.
     * @return Une liste des {@link Membre}s appartenant au club.
     */
    List<Membre> findByAdhesionsClubId(Integer clubId);

    /**
     * Recherche un membre (actif par défaut) par son code ami unique.
     *
     * @param codeAmi Le code ami unique à rechercher (ex: "AMIS-000123").
     * @return Un {@link Optional} contenant le {@link Membre} si trouvé (et actif), sinon un Optional vide.
     */
    Optional<Membre> findByCodeAmi(String codeAmi);

}

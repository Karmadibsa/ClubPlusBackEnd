package org.clubplus.clubplusbackend.dao;

import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité {@link Membre}.
 * Fournit les opérations CRUD et des requêtes pour trouver des membres par critères uniques.
 * <p>
 * Note: L'annotation {@code @Where(clause = "actif = true")} sur l'entité {@code Membre}
 * filtre la plupart des requêtes pour ne retourner que les membres actifs.
 * </p>
 */
@Repository
public interface MembreDao extends JpaRepository<Membre, Integer> {

    /**
     * Recherche un membre par son adresse email.
     * <p>
     * Utilisé pour la connexion et pour vérifier l'existence d'un email.
     *
     * @param email L'adresse email du membre.
     * @return Un {@link Optional} contenant le membre s'il est trouvé.
     */
    Optional<Membre> findByEmail(String email);

    /**
     * Vérifie si un email est déjà utilisé.
     *
     * @param email L'adresse email à vérifier.
     * @return {@code true} si l'email existe, {@code false} sinon.
     */
    boolean existsByEmail(String email);

    /**
     * Vérifie si un email est déjà utilisé par un autre membre (ID différent).
     * <p>
     * Utile pour la validation lors de la mise à jour d'un profil.
     *
     * @param email L'adresse email à vérifier.
     * @param id    L'ID du membre à exclure de la recherche.
     * @return {@code true} si l'email est utilisé par un autre membre, {@code false} sinon.
     */
    boolean existsByEmailAndIdNot(String email, Integer id);

    /**
     * Recherche l'administrateur (rôle ADMIN) d'un club spécifique.
     *
     * @param clubId L'ID du club.
     * @return Un {@link Optional} contenant le membre administrateur.
     */
    @Query("SELECT m FROM Membre m JOIN m.adhesions a WHERE a.club.id = :clubId AND m.role = 'ADMIN'")
    Optional<Membre> findAdminByClubId(@Param("clubId") Integer clubId);


    /**
     * Recherche tous les membres d'un club spécifique.
     *
     * @param clubId L'ID du club.
     * @return Une liste des membres du club.
     */
    List<Membre> findByAdhesionsClubId(Integer clubId);

    /**
     * Recherche un membre par son code ami unique.
     *
     * @param codeAmi Le code ami à rechercher.
     * @return Un {@link Optional} contenant le membre s'il est trouvé.
     */
    Optional<Membre> findByCodeAmi(String codeAmi);

    /**
     * Recherche un membre par son token de vérification d'email.
     *
     * @param verificationToken Le token de vérification.
     * @return Un {@link Optional} contenant le membre associé au token.
     */
    Optional<Membre> findByVerificationToken(String verificationToken);

    /**
     * Recherche un membre par son token de réinitialisation de mot de passe.
     *
     * @param resetPasswordToken Le token de réinitialisation.
     * @return Un {@link Optional} contenant le membre associé au token.
     */
    Optional<Membre> findByResetPasswordToken(String resetPasswordToken);
}

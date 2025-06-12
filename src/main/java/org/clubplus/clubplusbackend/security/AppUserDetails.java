package org.clubplus.clubplusbackend.security;

import lombok.Getter;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Implémentation personnalisée de l'interface {@link UserDetails} de Spring Security.
 * <p>
 * Cette classe fait le pont entre l'entité {@link Membre} de notre application et le framework Spring Security.
 * Elle fournit les informations essentielles (nom d'utilisateur, mot de passe, rôles, statut du compte)
 * nécessaires à Spring pour gérer l'authentification et l'autorisation.
 */
@Getter
public class AppUserDetails implements UserDetails {

    private static final Logger logger = LoggerFactory.getLogger(AppUserDetails.class);
    private final Membre membre;

    /**
     * Construit une instance de {@code AppUserDetails} à partir d'une entité {@link Membre}.
     *
     * @param membre L'entité Membre à encapsuler. Ne doit pas être null.
     */
    public AppUserDetails(Membre membre) {
        Objects.requireNonNull(membre, "L'entité Membre ne peut pas être null pour créer AppUserDetails.");
        this.membre = membre;
    }

    /**
     * Retourne les autorités (rôles) accordées à l'utilisateur.
     * Le rôle de l'entité Membre est converti en {@link GrantedAuthority} avec le préfixe "ROLE_".
     *
     * @return Une collection contenant l'autorité de l'utilisateur (ex: "ROLE_ADMIN").
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.membre == null || this.membre.getRole() == null) {
            return Collections.emptyList();
        }
        String roleName = "ROLE_" + this.membre.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    /**
     * Retourne le mot de passe de l'utilisateur (doit être le hash stocké).
     */
    @Override
    public String getPassword() {
        return this.membre.getPassword();
    }

    /**
     * Retourne le nom d'utilisateur. Dans notre cas, c'est l'adresse email.
     */
    @Override
    public String getUsername() {
        return this.membre.getEmail();
    }

    /**
     * Méthode utilitaire pour récupérer l'ID de l'entité {@link Membre}.
     *
     * @return L'ID de l'utilisateur.
     */
    public Integer getId() {
        if (this.membre == null) {
            throw new IllegalStateException("Impossible de récupérer l'ID : l'entité Membre est null.");
        }
        return membre.getId();
    }

    /**
     * Méthode utilitaire pour récupérer l'ID du club géré par l'utilisateur (s'il a un rôle ADMIN ou RESERVATION).
     * <p>
     * <b>Hypothèse :</b> Un gestionnaire (ADMIN/RESERVATION) ne gère qu'un seul club.
     * La méthode retourne l'ID du club de la première adhésion trouvée.
     *
     * @return L'ID du club géré, ou {@code null} si l'utilisateur n'est pas un gestionnaire ou en cas d'anomalie.
     */
    public Integer getManagedClubId() {
        if (this.membre == null || this.membre.getRole() == null) {
            return null;
        }

        Role userRole = this.membre.getRole();

        if (userRole == Role.ADMIN || userRole == Role.RESERVATION) {
            Set<Adhesion> adhesions = this.membre.getAdhesions();

            if (adhesions != null && !adhesions.isEmpty()) {
                return adhesions.stream()
                        .filter(Objects::nonNull)
                        .map(Adhesion::getClub)
                        .filter(Objects::nonNull)
                        .map(Club::getId)
                        .findFirst()
                        .orElse(null); // Retourne null si aucune adhésion n'a de club valide
            }
            logger.warn("ANOMALIE: Utilisateur {} ({}) avec un rôle de gestion mais sans adhésion valide.", getUsername(), userRole);
            return null;
        }
        return null;
    }

    // --- Statut du Compte UserDetails ---

    /**
     * Indique si le compte de l'utilisateur a expiré.
     *
     * @return toujours {@code true} (pas de gestion d'expiration).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indique si l'utilisateur est verrouillé.
     *
     * @return toujours {@code true} (pas de gestion de verrouillage).
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indique si les informations d'identification (mot de passe) ont expiré.
     *
     * @return toujours {@code true} (pas de gestion d'expiration de mot de passe).
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indique si l'utilisateur est activé. Un utilisateur désactivé ne peut pas s'authentifier.
     *
     * @return {@code true} si le compte de l'utilisateur a été vérifié par email, {@code false} sinon.
     */
    @Override
    public boolean isEnabled() {
        return this.membre.isVerified();
    }
}

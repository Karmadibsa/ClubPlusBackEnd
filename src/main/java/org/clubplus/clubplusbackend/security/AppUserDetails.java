package org.clubplus.clubplusbackend.security;

import lombok.Getter;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Getter
// Cette classe implémente l'interface UserDetails de Spring Security [4]
public class AppUserDetails implements UserDetails {

    // Référence à l'entité Membre [7][8]
    // protected Membre membre; // 'protected' est inhabituel, 'private final' est plus courant
    private final Membre membre; // Utiliser private final pour l'immutabilité après construction

    // Constructeur qui initialise avec Membre [3][5]
    public AppUserDetails(Membre membre) {
        // BONNE PRATIQUE: Ajouter un null check
        Objects.requireNonNull(membre, "Membre ne peut pas être null pour AppUserDetails");
        this.membre = membre;
    }

    // Retourne les autorisations/rôles [2][4]
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Vérification si rôle est null
        if (this.membre == null || this.membre.getRole() == null) {
            return Collections.emptyList(); // Pas d'autorités si pas de rôle
        }
        // Utilise le rôle global du Membre, préfixé par "ROLE_" (Convention Spring Security)
        String roleName = "ROLE_" + this.membre.getRole().name(); // .name() donne "ADMIN", "MEMBRE", etc.
        return Collections.singletonList(new SimpleGrantedAuthority(roleName)); // CORRECT
    }

    // Retourne le mot de passe encodé [4][5]
    @Override
    public String getPassword() {
        // Vérification nullité membre
        if (this.membre == null) {
            return null;
        }
        return membre.getPassword(); // CORRECT
    }

    // Retourne l'identifiant (email ici) [4][5]
    @Override
    public String getUsername() {
        // Vérification nullité membre
        if (this.membre == null) {
            return null;
        }
        return membre.getEmail(); // CORRECT
    }

    // Méthode ajoutée pour obtenir l'ID interne du Membre (pour nos services)
    public Integer getId() {
        // Vérification nullité membre
        if (this.membre == null) {
            // Lever une exception est probablement mieux ici car getId ne devrait pas être appelé
            // sur un UserDetails invalide créé à partir d'un membre null.
            throw new IllegalStateException("Membre associé à AppUserDetails est null lors de l'appel à getId().");
        }
        return membre.getId(); // CORRECT
    }

    // !!! MÉTHODES MANQUANTES DE L'INTERFACE UserDetails !!! [4]
    // Ces méthodes DOIVENT être implémentées.
    // Par défaut, si vous n'avez pas de logique spécifique, retournez 'true'.

    @Override
    public boolean isAccountNonExpired() {
        return true; // Mettre 'false' si vous gérez l'expiration des comptes
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Mettre 'false' si vous gérez le verrouillage des comptes (ex: trop d'échecs de login)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Mettre 'false' si vous gérez l'expiration des mots de passe
    }

    @Override
    public boolean isEnabled() {
        // Vous pourriez lier ceci à un champ 'actif' ou 'enabled' dans votre entité Membre si nécessaire.
        // if (this.membre != null) { return this.membre.isActif(); }
        return true; // Par défaut, compte activé
    }
}

package org.clubplus.clubplusbackend.security;

import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Cette classe implémente l'interface UserDetails de Spring Security UserDetails est une interface essentielle qui fournit les informations de base nécessaires à l'authentification et l'autorisation
public class AppUserDetails implements UserDetails {

    // Référence à l'entité Membre de l'application Cette classe sert d'adaptateur entre votre modèle métier (Membre) et le modèle de sécurité de Spring (UserDetails)
    protected Membre membre;

    // Constructeur qui initialise l'objet avec une instance de Membre Appelé par AppUserDetailService après avoir trouvé le membre en base de données
    public AppUserDetails(Membre membre) {
        this.membre = membre;
    }

    // Retourne la liste des autorisations/rôles de l'utilisateur Cette méthode est utilisée par Spring Security pour déterminer ce que l'utilisateur a le droit de faire dans l'application
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Crée une seule autorité basée sur le rôle du membre
        // Le préfixe "ROLE_" est une convention de Spring Security
        // La conversion en majuscules assure la cohérence
        return List.of(new SimpleGrantedAuthority("ROLE_" + membre.getRole().toUpperCase()));
    }

    // Retourne le mot de passe encodé pour la vérification d'authentification Spring Security compare ce mot de passe avec celui fourni lors de la connexion
    @Override
    public String getPassword() {
        return membre.getPassword();
    }

    // Retourne l'identifiant principal de l'utilisateur Dans ce cas, l'email est utilisé comme nom d'utilisateur
    @Override
    public String getUsername() {
        return membre.getEmail();
    }
}


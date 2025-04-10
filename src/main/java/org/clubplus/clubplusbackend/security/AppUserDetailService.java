package org.clubplus.clubplusbackend.security;

import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Annotation qui indique à Spring que cette classe est un service
// permettant son injection automatique dans d'autres composants
@Service
// Cette classe implémente l'interface UserDetailsService de Spring Security
// qui est responsable de charger les informations utilisateur lors de l'authentification
public class AppUserDetailService implements UserDetailsService {

    // Référence au DAO qui permet d'accéder aux données des membres
    // (interface qui étend généralement JpaRepository)
    protected MembreDao membreDao;

    // Constructeur avec injection de dépendances
    // L'annotation @Autowired permet à Spring d'injecter automatiquement
    // l'implémentation de MembreDao lors de la création de ce service
    @Autowired
    public AppUserDetailService(MembreDao membreDao) {
        this.membreDao = membreDao;
    }

    // Méthode obligatoire de l'interface UserDetailsService
    // C'est cette méthode qui est appelée par Spring Security pendant le processus d'authentification
    // Elle prend en paramètre l'identifiant de l'utilisateur (ici l'email)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Recherche du membre dans la base de données par son email
        // Optional est utilisé pour gérer proprement le cas où le membre n'existe pas
        Optional<Membre> membre = membreDao.findByEmail(email);

        // Si aucun membre n'est trouvé avec cet email, on lance une exception
        // qui sera interceptée par Spring Security pour signaler l'échec d'authentification
        if (membre.isEmpty()) {
            throw new UsernameNotFoundException(email);
        }

        // Si le membre existe, on le transforme en objet AppUserDetails
        // qui implémente l'interface UserDetails attendue par Spring Security
        // Cet objet contient les informations nécessaires à l'authentification (rôles, mot de passe, etc.)
        return new AppUserDetails(membre.get());
    }
}


package org.clubplus.clubplusbackend.security;

import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implémentant l'interface {@link UserDetailsService} de Spring Security.
 * <p>
 * Ce service est le point d'entrée pour Spring Security afin de charger les données
 * d'un utilisateur depuis la base de données lors d'une tentative d'authentification.
 * Il utilise l'email comme nom d'utilisateur.
 */
@Service
public class AppUserDetailService implements UserDetailsService {

    private final MembreDao membreDao;

    /**
     * Construit une nouvelle instance de AppUserDetailService.
     *
     * @param membreDao Le DAO pour accéder aux données des membres, injecté par Spring.
     */
    public AppUserDetailService(MembreDao membreDao) {
        this.membreDao = membreDao;
    }

    /**
     * Localise un utilisateur en se basant sur son email (utilisé comme nom d'utilisateur).
     * <p>
     * Cette méthode est appelée par le framework Spring Security lors du processus d'authentification.
     * Si l'utilisateur est trouvé, elle retourne un objet {@link AppUserDetails} que Spring
     * utilisera pour valider le mot de passe et charger les rôles.
     *
     * @param email L'email de l'utilisateur à rechercher.
     * @return Un objet {@link UserDetails} contenant les informations de l'utilisateur.
     * @throws UsernameNotFoundException si aucun utilisateur n'est trouvé avec l'email fourni.
     */
    @Override
    @Transactional(readOnly = true) // Optimisation pour une transaction en lecture seule.
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Membre membre = membreDao.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Aucun utilisateur trouvé avec l'email : " + email));

        return new AppUserDetails(membre);
    }
}

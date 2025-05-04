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
 * Ce service est responsable de charger les détails spécifiques à l'utilisateur
 * (dans ce cas, l'entité {@link Membre}) à partir de la base de données via {@link MembreDao}
 * lors d'une tentative d'authentification.
 *
 * <p>Il recherche un {@link Membre} par son email (utilisé comme nom d'utilisateur)
 * et, s'il est trouvé, le transforme en un objet {@link AppUserDetails} que Spring Security
 * peut utiliser pour valider les informations d'identification et déterminer les autorités (rôles).</p>
 *
 * @see UserDetailsService
 * @see MembreDao
 * @see Membre
 * @see AppUserDetails
 * @see UsernameNotFoundException
 */
@Service // Marque cette classe comme un bean de service Spring.
public class AppUserDetailService implements UserDetailsService {

    /**
     * Le Data Access Object (DAO) pour interagir avec la persistance des entités {@link Membre}.
     * Utilisé pour rechercher un membre par son email dans la méthode {@code loadUserByUsername}.
     * Déclaré 'final' pour l'immutabilité et pour permettre l'injection via le constructeur.
     */
    private final MembreDao membreDao;

    /**
     * Construit une nouvelle instance de AppUserDetailService en injectant la dépendance requise.
     * Ce constructeur est utilisé par le framework Spring pour créer le bean de service et
     * lui fournir l'instance nécessaire de {@link MembreDao}.
     *
     * @param membreDao Le DAO (Data Access Object) pour les entités {@link Membre},
     *                  requis pour charger les informations utilisateur depuis la base de données.
     *                  Ne doit pas être null (assuré par Spring lors de l'injection).
     */
    public AppUserDetailService(MembreDao membreDao) {
        this.membreDao = membreDao;
    }

    /**
     * Localise l'utilisateur basé sur son nom d'utilisateur (ici, l'email).
     * Appelée par Spring Security lors de l'authentification.
     *
     * <p>Recherche un {@link Membre} via {@link MembreDao}. Si trouvé, retourne un {@link AppUserDetails}.
     * Sinon, lève {@link UsernameNotFoundException}.</p>
     *
     * <p>L'annotation {@code @Transactional(readOnly = true)} optimise l'accès en lecture seule.</p>
     *
     * @param email L'email (nom d'utilisateur) recherché.
     * @return Un {@link UserDetails} ({@link AppUserDetails}) contenant les informations de l'utilisateur.
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé.
     */
    @Override
    @Transactional(readOnly = true) // Optimisation pour une lecture seule
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Membre membre = membreDao.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Aucun utilisateur trouvé avec l'email : " + email));
        return new AppUserDetails(membre);
    }
}

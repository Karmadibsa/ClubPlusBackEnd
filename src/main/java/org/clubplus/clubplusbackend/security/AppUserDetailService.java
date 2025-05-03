package org.clubplus.clubplusbackend.security;

import lombok.RequiredArgsConstructor;
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
@Service // Marque cette classe comme un bean de service Spring, la rendant détectable pour l'injection de dépendances.
@RequiredArgsConstructor
// Lombok: Génère un constructeur avec les champs 'final' requis (remplace le constructeur explicite).
public class AppUserDetailService implements UserDetailsService {

    /**
     * Le Data Access Object (DAO) pour interagir avec la persistance des entités {@link Membre}.
     * Utilisé pour rechercher un membre par son email dans la méthode {@code loadUserByUsername}.
     * Déclaré 'final' pour l'immutabilité et pour permettre l'injection via {@code @RequiredArgsConstructor}.
     */
    private final MembreDao membreDao;

    /**
     * Localise l'utilisateur basé sur son nom d'utilisateur (ici, l'email).
     * Cette méthode est appelée par le framework Spring Security lors du processus d'authentification
     * (par exemple, par le {@code DaoAuthenticationProvider}).
     *
     * <p>La méthode recherche un {@link Membre} dans la base de données correspondant à l'email fourni.
     * Si trouvé, elle encapsule ce {@code Membre} dans un objet {@link AppUserDetails}.
     * Si aucun utilisateur n'est trouvé, elle lève une {@link UsernameNotFoundException}.</p>
     *
     * <p>L'annotation {@code @Transactional(readOnly = true)} indique que cette opération
     * s'exécute dans une transaction de base de données en lecture seule, optimisant potentiellement
     * les performances et garantissant la cohérence des données lues.</p>
     *
     * @param email L'email (utilisé comme nom d'utilisateur) de l'utilisateur dont les détails sont demandés.
     * @return Un objet {@link UserDetails} (implémenté par {@link AppUserDetails}) contenant les informations
     * principales de l'utilisateur trouvé (email, mot de passe encodé, rôles, statut du compte).
     * Ne retourne jamais {@code null}.
     * @throws UsernameNotFoundException si aucun utilisateur ne peut être trouvé avec l'email fourni.
     *                                   Cette exception est attendue par Spring Security pour gérer le cas d'un utilisateur inconnu.
     */
    @Override
    @Transactional(readOnly = true) // Optimisation pour une opération de lecture seule
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Recherche le Membre par email en utilisant le MembreDao.
        // Utilise Optional.orElseThrow() pour gérer élégamment le cas où l'utilisateur n'est pas trouvé,
        // levant directement l'exception attendue par Spring Security.
        Membre membre = membreDao.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Aucun utilisateur trouvé avec l'email : " + email));

        // Si l'utilisateur est trouvé, crée et retourne une instance de AppUserDetails
        // qui encapsule l'entité Membre pour Spring Security.
        return new AppUserDetails(membre);
    }
}

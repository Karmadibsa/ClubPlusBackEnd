package org.clubplus.clubplusbackend.security;

import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// @RequiredArgsConstructor // Optionnel: Remplace le constructeur et @Autowired si le champ est 'final'
public class AppUserDetailService implements UserDetailsService {

    // Utiliser 'private final' pour l'immutabilité et permettre @RequiredArgsConstructor
    private final MembreDao membreDao;

    // Constructeur avec injection (ou généré par @RequiredArgsConstructor si champ final)
    // @Autowired // Plus nécessaire si @RequiredArgsConstructor est utilisé
    public AppUserDetailService(MembreDao membreDao) {
        this.membreDao = membreDao;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Recherche du membre (OK)
        // Optional<Membre> membre = membreDao.findByEmail(email); // Ligne originale

        // Version légèrement plus concise avec .orElseThrow()
        Membre membre = membreDao.findByEmail(email)
                .orElseThrow(() ->
                        // Message d'erreur un peu plus descriptif (optionnel)
                        new UsernameNotFoundException("Aucun utilisateur trouvé avec l'email : " + email));

        // Création de AppUserDetails (OK)
        return new AppUserDetails(membre);
    }
}

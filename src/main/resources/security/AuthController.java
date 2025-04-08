package security;

import jakarta.validation.Valid;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@CrossOrigin
@RestController
public class AuthController {

    protected MembreDao membreDao;
    protected PasswordEncoder passwordEncoder;
    protected AuthenticationProvider authenticationProvider;
    protected JwtUtils jwtUtils;

    @Autowired
    public AuthController(MembreDao membreDao, PasswordEncoder passwordEncoder, AuthenticationProvider authenticationProvider, JwtUtils jwtUtils) {
        this.membreDao = membreDao;
        this.passwordEncoder = passwordEncoder;
        this.authenticationProvider = authenticationProvider;
        this.jwtUtils = jwtUtils;
    }


    @PostMapping("/api/inscription")
    public ResponseEntity<Membre> inscription(@RequestBody @Valid Membre membreRequest) {
        // Création d'un nouveau membre avec les informations fournies
        Membre membre = new Membre();

        // Définition explicite de chaque champ
        membre.setNom(membreRequest.getNom());
        membre.setPrenom(membreRequest.getPrenom());
        membre.setDate_naissance(membreRequest.getDate_naissance());
        membre.setNumero_voie(membreRequest.getNumero_voie());
        membre.setRue(membreRequest.getRue());
        membre.setCodepostal(membreRequest.getCodepostal());
        membre.setVille(membreRequest.getVille());
        membre.setTelephone(membreRequest.getTelephone());
        membre.setEmail(membreRequest.getEmail());

        // Date d'inscription générée automatiquement (aujourd'hui)
        membre.setDate_inscription(LocalDate.now().format(DateTimeFormatter.ISO_DATE));

        // Attribution du rôle par défaut
        membre.setRole("membre");

        // Encodage du mot de passe
        membre.setPassword(passwordEncoder.encode(membreRequest.getPassword()));

        // Sauvegarde du membre
        membreDao.save(membre);

        // Masquage du mot de passe dans la réponse
        membre.setPassword(null);

        return new ResponseEntity<>(membre, HttpStatus.CREATED);
    }

    @PostMapping("/api/connexion")
    public ResponseEntity<String> connexion(@RequestBody Membre membreRequest) {

        try {
            AppUserDetails userDetails = (AppUserDetails) authenticationProvider
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    membreRequest.getEmail(),
                                    membreRequest.getPassword()))
                    .getPrincipal();

            return new ResponseEntity<>(jwtUtils.generateToken(userDetails), HttpStatus.OK);

        } catch (AuthenticationException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }


    }
}

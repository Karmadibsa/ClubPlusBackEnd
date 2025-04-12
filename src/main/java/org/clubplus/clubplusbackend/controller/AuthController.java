package org.clubplus.clubplusbackend.controller;

import jakarta.validation.Valid;
import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.JwtUtils;
import org.clubplus.clubplusbackend.security.Role;
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
    public ResponseEntity<Membre> inscription(@RequestBody @Valid Membre membre) {


        // Date d'inscription générée automatiquement (aujourd'hui)
        membre.setDate_inscription(LocalDate.now());

        // Attribution du rôle par défaut
        membre.setRole(Role.MEMBRE);

        // Encodage du mot de passe
        membre.setPassword(passwordEncoder.encode(membre.getPassword()));

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

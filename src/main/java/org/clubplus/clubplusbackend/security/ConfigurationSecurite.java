package org.clubplus.clubplusbackend.security;
// Imports des classes nécessaires de Spring Security et Spring Web

//

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/// / Configuration de la sécurité de l'application
@Configuration // Indique à Spring que cette classe fournit des configurations de beans
@EnableWebSecurity // Active la configuration de sécurité web de Spring Security
@EnableMethodSecurity
public class ConfigurationSecurite {
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;

    //
//    // Composants nécessaires pour gérer l'authentification
//    protected PasswordEncoder passwordEncoder; // Service pour encoder/décoder les mots de passe
//    protected UserDetailsService userDetailsService; // Service pour charger les données utilisateur
//    protected JwtFilter jwtFilter; // Service pour charger les données utilisateur
//
//    // Injection des dépendances par constructeur
    @Autowired
    public ConfigurationSecurite(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, JwtFilter jwtFilter) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    //
//
//    // Configuration du fournisseur d'authentification
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setPasswordEncoder(passwordEncoder); // Définit l'encodeur de mot de passe à utiliser
        auth.setUserDetailsService(userDetailsService); // Définit le service qui récupère les informations utilisateur
        return auth;
    }

    //
//    // Configuration de la chaîne de filtres de sécurité
    @Bean
    public SecurityFilterChain configureAuthentification(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable()) // Désactive la protection CSRF, généralement inutile pour les API REST
                .cors(c -> c.configurationSource(corsConfigurationSource())) // Configure CORS avec les paramètres définis ci-dessous
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Configuration sans état (pas de session), typique des API REST
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    //
//    // Configuration détaillée de CORS (Cross-Origin Resource Sharing)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of("*")); // Autorise les requêtes de n'importe quelle origine
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH")); // Autorise ces méthodes HTTP
        corsConfiguration.setAllowedHeaders(List.of("*")); // Autorise tous les types d'en-têtes

        // Applique cette configuration CORS à tous les chemins d'API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}

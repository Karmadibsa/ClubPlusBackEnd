package org.clubplus.clubplusbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Classe de configuration principale pour Spring Security.
 * <p>
 * Configure la sécurité web (filtrage HTTP), la sécurité au niveau des méthodes,
 * l'authentification, la gestion de session (stateless), la configuration CORS,
 * et l'intégration du filtre JWT personnalisé.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ConfigurationSecurite {

    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construit l'instance de configuration de sécurité avec les dépendances requises.
     *
     * @param userDetailsService Le service pour charger les détails de l'utilisateur.
     * @param jwtFilter          Le filtre personnalisé pour valider les tokens JWT.
     * @param passwordEncoder    Le bean pour encoder et vérifier les mots de passe.
     */
    public ConfigurationSecurite(UserDetailsService userDetailsService, JwtFilter jwtFilter, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Définit le fournisseur d'authentification principal.
     * Ce provider utilise le {@link UserDetailsService} pour trouver l'utilisateur et le
     * {@link PasswordEncoder} pour vérifier le mot de passe.
     *
     * @return Une instance configurée de {@link DaoAuthenticationProvider}.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(this.passwordEncoder);
        return authProvider;
    }

    /**
     * Expose le bean {@link AuthenticationManager} de Spring Security.
     * Il est nécessaire pour traiter les requêtes d'authentification.
     *
     * @param authenticationConfiguration La configuration d'authentification fournie par Spring.
     * @return L'instance de {@link AuthenticationManager}.
     * @throws Exception en cas d'erreur de configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configure la chaîne de filtres de sécurité HTTP.
     * <p>
     * Définit les règles suivantes :
     * <ul>
     * <li>Désactivation de CSRF (pour les API stateless).</li>
     * <li>Activation de CORS.</li>
     * <li>Gestion de session en mode STATELESS.</li>
     * <li>Autorisation des requêtes publiques pour /auth/** et /contact.</li>
     * <li>Authentification requise pour toutes les autres requêtes.</li>
     * <li>Ajout du filtre JWT avant le filtre d'authentification standard.</li>
     * </ul>
     *
     * @param http L'objet pour construire la configuration de sécurité.
     * @return La chaîne de filtres de sécurité construite.
     * @throws Exception en cas d'erreur de configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/contact").permitAll()
                        .requestMatchers("/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * Définit la configuration CORS (Cross-Origin Resource Sharing).
     * <p>
     * Autorise les requêtes provenant de domaines front-end spécifiques
     * pour interagir avec l'API.
     *
     * @return La source de configuration CORS.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // IMPORTANT : En production, soyez aussi spécifique que possible.
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000", "https://club-plus.netlify.app", "https://club-plus.onrender.com", "http://192.168.137.1:8080", "http://172.16.1.124:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

package org.clubplus.clubplusbackend.security; // Ou votre package config

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
 * Classe de configuration principale pour Spring Security dans l'application ClubPlus.
 * Configure la sécurité web (filtrage HTTP, autorisations de requêtes), la sécurité au niveau
 * des méthodes (via {@code @EnableMethodSecurity}), l'authentification (providers, manager),
 * la gestion de session (stateless pour JWT), la configuration CORS, et l'intégration du filtre JWT personnalisé.
 *
 * <p>Annotations clés :</p>
 * <ul>
 *     <li>{@link Configuration @Configuration} : Indique que cette classe contient des définitions de beans Spring.</li>
 *     <li>{@link EnableWebSecurity @EnableWebSecurity} : Active la prise en charge de la sécurité web par Spring Security.</li>
 *     <li>{@link EnableMethodSecurity @EnableMethodSecurity(prePostEnabled = true)} : Active la sécurité au niveau des méthodes,
 *         permettant l'utilisation d'annotations comme {@code @PreAuthorize}, {@code @PostAuthorize}.</li>
 * </ul>
 *
 * @see HttpSecurity
 * @see SecurityFilterChain
 * @see AuthenticationProvider
 * @see AuthenticationManager
 * @see JwtFilter
 * @see UserDetailsService
 * @see PasswordEncoder
 */
@Configuration
@EnableWebSecurity // Active la configuration de sécurité web de Spring
@EnableMethodSecurity(prePostEnabled = true) // Active la sécurité au niveau méthode (pour @PreAuthorize, etc.)
public class ConfigurationSecurite {

    /**
     * Le service responsable de charger les détails d'un utilisateur par son nom d'utilisateur (email).
     * Injecté par Spring pour être utilisé par le {@link DaoAuthenticationProvider}.
     *
     * @see AppUserDetailService
     */
    private final UserDetailsService userDetailsService;

    /**
     * Le filtre personnalisé qui intercepte les requêtes HTTP pour valider les tokens JWT
     * présents dans l'en-tête 'Authorization' et configurer le contexte de sécurité de Spring
     * si le token est valide.
     */
    private final JwtFilter jwtFilter;

    /**
     * Le bean responsable de l'encodage et de la vérification des mots de passe.
     * Doit être le même bean que celui utilisé lors de l'inscription ou de la mise à jour
     * des mots de passe des utilisateurs. Injecté par Spring depuis sa définition globale
     * (par exemple, dans la classe principale de l'application).
     *
     * @see PasswordEncoder
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Construit l'instance de configuration de sécurité avec les dépendances requises.
     * Utilise l'injection de dépendances via le constructeur gérée par Spring.
     *
     * @param userDetailsService Le service de détails utilisateur (implémentation de {@link UserDetailsService}).
     * @param jwtFilter          Le filtre JWT personnalisé (instance de {@link JwtFilter}).
     * @param passwordEncoder    Le bean d'encodage de mot de passe (implémentation de {@link PasswordEncoder}).
     */
    public ConfigurationSecurite(UserDetailsService userDetailsService, JwtFilter jwtFilter, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
        this.passwordEncoder = passwordEncoder; // Reçoit le bean PasswordEncoder défini ailleurs
    }

    // --- Le @Bean PasswordEncoder a été SUPPRIMÉ d'ici pour éviter une dépendance cyclique
    //     et centraliser sa définition (souvent dans la classe @SpringBootApplication). ---

    /**
     * Définit le bean pour le fournisseur d'authentification principal basé sur DAO (Data Access Object).
     * Ce provider utilise le {@link UserDetailsService} injecté pour charger les détails de l'utilisateur
     * et le {@link PasswordEncoder} injecté pour comparer le mot de passe fourni lors de la tentative
     * de connexion avec celui stocké (encodé) pour l'utilisateur.
     *
     * @return Une instance configurée de {@link DaoAuthenticationProvider}.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Configure le service qui charge les données utilisateur
        authProvider.setUserDetailsService(userDetailsService);
        // Configure l'encodeur de mot de passe utilisé pour la vérification
        authProvider.setPasswordEncoder(this.passwordEncoder); // Utilise l'instance injectée
        return authProvider;
    }

    /**
     * Expose le bean {@link AuthenticationManager} standard de Spring Security.
     * L'{@code AuthenticationManager} est l'interface principale pour traiter les requêtes d'authentification.
     * Il délègue généralement à un ou plusieurs {@link AuthenticationProvider} configurés (comme celui défini ci-dessus).
     * L'obtention via {@link AuthenticationConfiguration} est la manière moderne et recommandée.
     *
     * @param authenticationConfiguration La configuration d'authentification fournie par Spring.
     * @return L'instance de {@link AuthenticationManager} gérée par Spring.
     * @throws Exception si une erreur survient lors de la récupération du manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configure et définit la chaîne de filtres de sécurité HTTP principale.
     * Cette méthode utilise {@link HttpSecurity} pour définir les règles de sécurité pour les requêtes HTTP :
     * <ul>
     *     <li>Désactivation de CSRF (courant pour les API stateless basées sur JWT).</li>
     *     <li>Activation et configuration de CORS via {@link #corsConfigurationSource()}.</li>
     *     <li>Configuration de la gestion de session en mode STATELESS (aucune session serveur n'est créée ou utilisée).</li>
     *     <li>Définition des règles d'autorisation des requêtes HTTP ({@code authorizeHttpRequests}) :
     *         <ul>
     *             <li>Autorise l'accès public (sans authentification) aux endpoints d'inscription et de connexion.</li>
     *              <li>Autorise l'accès public aux endpoints de statistiques publiques.</li>
     *             <li>Autorise l'accès public à l'endpoint d'inscription de club.</li>
     *             <li>Exige une authentification pour toutes les autres requêtes sous "/api/**".</li>
     *             <li>Refuse toutes les autres requêtes non spécifiées ({@code anyRequest().denyAll()}) par sécurité.</li>
     *         </ul>
     *     </li>
     *     <li>Ajout du filtre {@link JwtFilter} personnalisé *avant* le filtre standard {@link UsernamePasswordAuthenticationFilter},
     *         pour traiter le token JWT et établir l'identité de l'utilisateur avant que Spring ne tente une authentification par formulaire.</li>
     * </ul>
     *
     * @param http L'objet {@link HttpSecurity} fourni par Spring pour la configuration.
     * @return L'objet {@link SecurityFilterChain} construit.
     * @throws Exception si une erreur survient pendant la configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactive la protection CSRF car nous utilisons JWT (stateless)
                .csrf(AbstractHttpConfigurer::disable)
                // Configure CORS en utilisant le bean corsConfigurationSource défini ci-dessous
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Définit la politique de gestion de session à STATELESS (pas de session côté serveur)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure les autorisations pour les requêtes HTTP
                .authorizeHttpRequests(auth -> auth
                        // Autorise l'accès public aux endpoints d'authentification/inscription
                        .requestMatchers("/api/auth/inscription/**").permitAll()
                        .requestMatchers("/api/auth/connexion").permitAll()
                        .requestMatchers("/api/auth/stats").permitAll()
                        // Autorise l'accès public à l'inscription de club
                        .requestMatchers("/api/clubs/inscription").permitAll()
                        // Exige une authentification pour toutes les autres requêtes API
                        .requestMatchers("/api/**").authenticated()
                        // Refuse toutes les autres requêtes non explicitement autorisées (principe de sécurité par défaut)
                        .anyRequest().denyAll()
                )
                // Ajoute notre filtre JWT avant le filtre standard d'authentification par formulaire/login
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Construit et retourne la chaîne de filtres configurée
        return http.build();
    }

    /**
     * Définit la configuration CORS (Cross-Origin Resource Sharing) pour l'application.
     * CORS est un mécanisme de sécurité de navigateur qui restreint les requêtes HTTP initiées
     * depuis un domaine différent de celui du serveur. Cette configuration spécifie quels domaines externes
     * (origines), méthodes HTTP, et en-têtes sont autorisés à interagir avec l'API sous "/api/**".
     *
     * @return Une source de configuration CORS ({@link CorsConfigurationSource}) basée sur les URLs.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Définit les origines autorisées (ex: votre frontend Angular/React)
        // IMPORTANT: Soyez spécifique en production, évitez "*"
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000", "http://votre-frontend.com")); // À adapter
        // Définit les méthodes HTTP autorisées
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // Définit les en-têtes HTTP autorisés (ex: Authorization, Content-Type). "*" autorise tous les en-têtes.
        configuration.setAllowedHeaders(List.of("*"));
        // Décommentez si vous devez envoyer des cookies ou des en-têtes d'autorisation via CORS
        // configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Applique cette configuration à toutes les requêtes commençant par "/api/"
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

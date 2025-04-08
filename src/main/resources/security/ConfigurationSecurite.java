package security;

// Imports des classes nécessaires de Spring Security et Spring Web

//

/// / Configuration de la sécurité de l'application
//@Configuration // Indique à Spring que cette classe fournit des configurations de beans
//@EnableWebSecurity // Active la configuration de sécurité web de Spring Security
public class ConfigurationSecurite {
//
//    // Composants nécessaires pour gérer l'authentification
//    protected PasswordEncoder passwordEncoder; // Service pour encoder/décoder les mots de passe
//    protected UserDetailsService userDetailsService; // Service pour charger les données utilisateur
//    protected JwtFilter jwtFilter; // Service pour charger les données utilisateur
//
//    // Injection des dépendances par constructeur
//    @Autowired
//    public ConfigurationSecurite(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, JwtFilter jwtFilter) {
//        this.passwordEncoder = passwordEncoder;
//        this.userDetailsService = userDetailsService;
//        this.jwtFilter = jwtFilter;
//    }
//
//
//    // Configuration du fournisseur d'authentification
//    @Bean
//    public AuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
//        auth.setPasswordEncoder(passwordEncoder); // Définit l'encodeur de mot de passe à utiliser
//        auth.setUserDetailsService(userDetailsService); // Définit le service qui récupère les informations utilisateur
//        return auth;
//    }
//
//    // Configuration de la chaîne de filtres de sécurité
//    @Bean
//    public SecurityFilterChain configureAuthentification(HttpSecurity http) throws Exception {
//        return http
//                .csrf(c -> c.disable()) // Désactive la protection CSRF, généralement inutile pour les API REST
//                .cors(c -> c.configurationSource(corsConfigurationSource())) // Configure CORS avec les paramètres définis ci-dessous
//                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Configuration sans état (pas de session), typique des API REST
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//                .build();
//    }
//
//    // Configuration détaillée de CORS (Cross-Origin Resource Sharing)
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.setAllowedOrigins(List.of("*")); // Autorise les requêtes de n'importe quelle origine
//        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH")); // Autorise ces méthodes HTTP
//        corsConfiguration.setAllowedHeaders(List.of("*")); // Autorise tous les types d'en-têtes
//
//        // Applique cette configuration CORS à tous les chemins d'API
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfiguration);
//        return source;
//    }
}

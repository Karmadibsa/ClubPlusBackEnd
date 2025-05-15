package org.clubplus.clubplusbackend.security.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global centralisé pour l'application REST ClubPlus.
 * Intercepte les exceptions spécifiques lancées par les contrôleurs ({@code @RestController})
 * ou les couches inférieures (services, sécurité) et les transforme en réponses HTTP {@link ResponseEntity}
 * standardisées avec un corps JSON structuré ({@link ErrorResponse} ou Map pour la validation).
 *
 * <p>Utilise {@link RestControllerAdvice @RestControllerAdvice} pour s'appliquer automatiquement
 * à tous les contrôleurs REST de l'application.</p>
 *
 * <p>Les handlers spécifiques interceptent les exceptions courantes (validation, authentification,
 * autorisation, ressource non trouvée, conflits métier) et retournent les codes HTTP appropriés
 * (400, 401, 403, 404, 409). Un handler générique capture toutes les autres exceptions
 * pour retourner un statut 500 Internal Server Error.</p>
 *
 * @see RestControllerAdvice
 * @see ExceptionHandler
 */
@RestControllerAdvice // Indique que c'est un gestionnaire d'exceptions global pour les @RestController
public class GlobalExceptionHandler {

    /**
     * Classe interne représentant la structure standard du corps JSON pour les réponses d'erreur.
     * Utilisée par la plupart des handlers d'exception pour fournir une réponse cohérente au client.
     */
    @Data // Annotation Lombok: génère automatiquement getters, setters, toString, equals, hashCode.
    @AllArgsConstructor // Annotation Lombok: génère un constructeur avec tous les champs comme arguments.
    public static class ErrorResponse {
        /**
         * L'horodatage exact de l'occurrence de l'erreur.
         */
        private LocalDateTime timestamp;
        /**
         * Le code de statut HTTP correspondant à l'erreur (ex: 404, 403).
         */
        private int status;
        /**
         * La phrase associée au statut HTTP (ex: "Not Found", "Forbidden").
         */
        private String error;
        /**
         * Un message descriptif fournissant plus de détails sur l'erreur spécifique.
         */
        private String message;
        /** (Optionnel) Le chemin URI de la requête qui a provoqué l'erreur. */
        // private String path;
    }

    /**
     * Gère les exceptions levées lorsque la validation des arguments annotés avec {@link jakarta.validation.Valid @Valid} échoue
     * (typiquement sur un {@code @RequestBody} ou {@code @RequestPart} dans un contrôleur).
     * Retourne une réponse HTTP 400 (Bad Request) avec une Map contenant les détails des erreurs de validation par champ.
     *
     * @param ex L'exception {@link MethodArgumentNotValidException} contenant les erreurs de validation.
     * @return Une {@code Map<String, String>} où la clé est le nom du champ invalide et la valeur est le message d'erreur de validation associé.
     * Le statut HTTP est défini à 400 Bad Request via {@link ResponseStatus @ResponseStatus}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Assigne directement le statut HTTP 400 à la réponse
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        // Le corps de la réponse est la Map des erreurs, le statut est 400.
        return errors;
    }

    /**
     * Gère les exceptions levées lors d'un échec d'authentification (ex: identifiants incorrects lors de la connexion).
     * Typiquement une instance de {@link AuthenticationException} ou une de ses sous-classes.
     * Retourne une réponse HTTP 401 (Unauthorized) avec un corps {@link ErrorResponse} standard.
     *
     * @param ex      L'exception {@link AuthenticationException} indiquant l'échec.
     * @param request L'objet {@link WebRequest} fournissant le contexte de la requête (optionnel, pour extraire le chemin par exemple).
     * @return Une {@link ResponseEntity} contenant le {@link ErrorResponse} et le statut HTTP 401.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(), // 401
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), // "Unauthorized"
                // Message générique pour ne pas donner trop d'infos en cas d'attaque
                "Échec de l'authentification : identifiants invalides ou token manquant/invalide."
                // , getPath(request) // Chemin optionnel
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Gère les exceptions levées lorsque l'utilisateur est authentifié mais n'a pas les autorisations nécessaires
     * pour accéder à une ressource ou effectuer une action (problème de rôle ou de droits spécifiques).
     * Typiquement une {@link AccessDeniedException} levée par Spring Security (ex: via {@code @PreAuthorize} ou vérifications manuelles).
     * Retourne une réponse HTTP 403 (Forbidden) avec un corps {@link ErrorResponse} standard.
     *
     * @param ex      L'exception {@link AccessDeniedException} indiquant le refus d'accès.
     * @param request L'objet {@link WebRequest} fournissant le contexte.
     * @return Une {@link ResponseEntity} contenant le {@link ErrorResponse} et le statut HTTP 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(), // 403
                HttpStatus.FORBIDDEN.getReasonPhrase(), // "Forbidden"
                // Utilise le message de l'exception s'il est défini, sinon un message générique.
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Accès non autorisé pour effectuer cette action ou accéder à cette ressource."
                // , getPath(request)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Gère les exceptions levées lorsqu'une entité (ex: Membre, Club, Event) attendue n'est pas trouvée en base de données.
     * Typiquement une {@link EntityNotFoundException} (ou une sous-classe personnalisée qui en hérite) levée par les services
     * lors d'une recherche par ID ou autre critère unique échouant.
     * Retourne une réponse HTTP 404 (Not Found) avec un corps {@link ErrorResponse} standard.
     *
     * @param ex      L'exception {@link EntityNotFoundException} indiquant que l'entité est introuvable.
     * @param request L'objet {@link WebRequest} fournissant le contexte.
     * @return Une {@link ResponseEntity} contenant le {@link ErrorResponse} et le statut HTTP 404.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(), // 404
                HttpStatus.NOT_FOUND.getReasonPhrase(), // "Not Found"
                // Utilise le message spécifique de l'exception (souvent informatif) ou un message par défaut.
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "La ressource demandée n'a pas été trouvée."
                // , getPath(request)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère les exceptions liées à un argument fourni invalide ou à une violation d'une contrainte métier
     * qui résulte en un conflit de données empêchant l'opération (ex: tentative de création avec un email déjà existant).
     * Typiquement une {@link IllegalArgumentException} levée par la logique métier dans les services.
     * Retourne une réponse HTTP 409 (Conflict) pour indiquer un conflit avec l'état actuel des ressources serveur.
     * (Note: 400 Bad Request pourrait aussi être utilisé, mais 409 est souvent préféré pour les conflits de données).
     *
     * @param ex      L'exception {@link IllegalArgumentException} indiquant l'argument ou la condition invalide.
     * @param request L'objet {@link WebRequest} fournissant le contexte.
     * @return Une {@link ResponseEntity} contenant le {@link ErrorResponse} et le statut HTTP 409.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        HttpStatus status = HttpStatus.CONFLICT; // 409 - Conflit
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(), // "Conflict"
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Requête invalide ou conflit avec l'état actuel des données."
                // , getPath(request)
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Gère les exceptions indiquant qu'une opération demandée est invalide ou impossible compte tenu de l'état actuel
     * de l'objet ou du système (ex: essayer d'accepter une demande d'ami déjà acceptée, supprimer un club avec des événements actifs).
     * Typiquement une {@link IllegalStateException} levée par la logique métier dans les services.
     * Retourne une réponse HTTP 409 (Conflict) pour indiquer que l'opération ne peut être réalisée dans l'état présent.
     *
     * @param ex      L'exception {@link IllegalStateException} indiquant l'état inapproprié.
     * @param request L'objet {@link WebRequest} fournissant le contexte.
     * @return Une {@link ResponseEntity} contenant le {@link ErrorResponse} et le statut HTTP 409.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        HttpStatus status = HttpStatus.CONFLICT; // 409 - Conflit
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(), // "Conflict"
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Opération non permise dans l'état actuel de la ressource."
                // , getPath(request)
        );
        return new ResponseEntity<>(errorResponse, status);
    }


    /**
     * Handler "fourre-tout" qui intercepte toutes les autres exceptions {@link Exception} non gérées
     * par les handlers plus spécifiques ci-dessus. Indique une erreur serveur inattendue.
     * Retourne une réponse HTTP 500 (Internal Server Error) avec un message générique pour le client.
     * **Crucial :** Logue l'exception complète côté serveur pour faciliter le débogage.
     *
     * @param ex      L'exception {@link Exception} ou une de ses sous-classes non gérée spécifiquement.
     * @param request L'objet {@link WebRequest} fournissant le contexte.
     * @return Une {@link ResponseEntity} contenant le {@link ErrorResponse} générique et le statut HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {

        // À défaut de logger configuré, on utilise System.err (NON RECOMMANDÉ EN PRODUCTION)
        System.err.println("--- ERREUR SERVEUR INATTENDUE (500) ---");
        System.err.println("Timestamp: " + LocalDateTime.now());
        System.err.println("Path: " + getPathSafe(request)); // Afficher le chemin si possible
        System.err.println("Exception Type: " + ex.getClass().getName());
        System.err.println("Message: " + ex.getMessage());
        ex.printStackTrace(System.err); // Imprime la stack trace sur la sortie d'erreur standard
        System.err.println("--------------------------------------");

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), // "Internal Server Error"
                // Message volontairement générique pour le client final
                "Une erreur interne inattendue est survenue. Veuillez réessayer plus tard ou contacter le support."
                // , getPath(request) // Inclure le chemin peut être utile aussi pour le client
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- Méthode Helper (Optionnelle) ---

    /**
     * Méthode utilitaire (privée) pour extraire le chemin URI de la requête de manière sûre.
     * Retourne "N/A" si le chemin ne peut pas être extrait.
     *
     * @param request L'objet WebRequest courant.
     * @return Le chemin URI de la requête ou "N/A" en cas d'erreur.
     */
    private String getPathSafe(WebRequest request) {
        try {
            // Tente de caster vers ServletWebRequest pour accéder à HttpServletRequest
            if (request instanceof org.springframework.web.context.request.ServletWebRequest) {
                return ((org.springframework.web.context.request.ServletWebRequest) request).getRequest().getRequestURI();
            }
        } catch (Exception e) {
            // Ignorer l'erreur et retourner "N/A"
        }
        return "N/A"; // Retourne "Non Applicable" si l'extraction échoue
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<String> handleDisabledException(DisabledException ex, WebRequest request) {
        // Vous pouvez personnaliser le message et le statut
        String errorMessage = "Le compte utilisateur n'a pas été vérifié. Veuillez consulter votre e-mail de vérification.";
        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED); // Ou FORBIDDEN (403) si vous préférez
    }
}

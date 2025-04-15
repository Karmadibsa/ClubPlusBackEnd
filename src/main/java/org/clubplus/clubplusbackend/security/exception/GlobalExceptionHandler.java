package org.clubplus.clubplusbackend.security.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
 * Gestionnaire d'exceptions global pour l'application REST.
 * Intercepte les exceptions lancées par les services et contrôleurs
 * et les transforme en réponses HTTP standardisées.
 * Utilise @RestControllerAdvice pour s'appliquer à tous les @RestController.
 */
@RestControllerAdvice // Combine @ControllerAdvice et @ResponseBody
public class GlobalExceptionHandler {

    /**
     * DTO simple pour les réponses d'erreur standard.
     */
    @Data // Lombok: Génère getters, setters, toString, etc.
    @AllArgsConstructor // Génère un constructeur avec tous les arguments
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error; // Type d'erreur HTTP (ex: "Not Found", "Forbidden")
        private String message; // Message descriptif de l'erreur
        // private String path; // Optionnel: Chemin de la requête (via WebRequest)
    }

    /**
     * Gère les exceptions lorsque @Valid échoue sur un @RequestBody ou @RequestPart.
     * Retourne un statut 400 Bad Request avec les détails des champs erronés.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Définit le statut HTTP directement
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors; // Retourne une map {champ -> message}
    }

    /**
     * Gère les exceptions lorsque l'authentification échoue (mauvais identifiants).
     * Retourne un statut 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), // "Unauthorized"
                "Échec de l'authentification : identifiants invalides." // Message générique
                // getPath(request) // Chemin optionnel
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Gère les exceptions lorsque l'accès est refusé (authentifié mais pas autorisé).
     * Typiquement lancé par SecurityService (check...OrThrow).
     * Retourne un statut 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(), // "Forbidden"
                ex.getMessage() != null ? ex.getMessage() : "Accès non autorisé."
                // getPath(request)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Gère les exceptions lorsqu'une entité attendue n'est pas trouvée (ex: via findById).
     * Typiquement lancé par les services (get...OrThrow).
     * Retourne un statut 404 Not Found.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), // "Not Found"
                ex.getMessage() != null ? ex.getMessage() : "Ressource non trouvée."
                // getPath(request)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère les exceptions liées à un argument invalide ou à une violation de contrainte métier
     * qui ne permet pas l'opération (ex: email dupliqué).
     * Retourne un statut 409 Conflict (ou 400 Bad Request selon le cas, 409 est souvent pour les conflits de données).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        // Utiliser 409 pour les conflits de données comme les emails dupliqués
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(), // "Conflict"
                ex.getMessage() != null ? ex.getMessage() : "Requête invalide ou conflit de données."
                // getPath(request)
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Gère les exceptions indiquant qu'une opération est invalide dans l'état actuel
     * (ex: supprimer un club avec événements, noter un événement non terminé).
     * Retourne un statut 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(), // "Conflict"
                ex.getMessage() != null ? ex.getMessage() : "Opération non autorisée dans l'état actuel."
                // getPath(request)
        );
        return new ResponseEntity<>(errorResponse, status);
    }


    /**
     * Gère toutes les autres exceptions non interceptées spécifiquement.
     * C'est un "catch-all" pour les erreurs serveur inattendues.
     * Retourne un statut 500 Internal Server Error.
     * **Important**: Loguer l'exception complète ici pour le débogage.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // !!! IMPORTANT: Loguer l'exception ici pour le débogage !!!
        // Dans une vraie application, utiliser un logger (ex: SLF4j) : log.error("Erreur inattendue : ", ex);
        // Conformément à votre contrainte "pas de logger", on utilise System.err, mais ce n'est pas recommandé.
        System.err.println("--- ERREUR SERVEUR INATTENDUE ---");
        ex.printStackTrace(); // Affiche la stack trace dans la console serveur
        System.err.println("-------------------------------");

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), // "Internal Server Error"
                "Une erreur interne est survenue." // Message générique pour le client
                // getPath(request)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- Méthode Helper (Optionnelle) ---
    /*
    private String getPath(WebRequest request) {
        // Utile pour ajouter le chemin de l'API dans la réponse d'erreur
        try {
            // Simple exemple, peut nécessiter plus de robustesse
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        } catch (Exception e) {
            return "N/A";
        }
    }
    */
}

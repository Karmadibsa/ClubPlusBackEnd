package org.clubplus.clubplusbackend.security.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global pour l'application.
 * <p>
 * Intercepte les exceptions spécifiques lancées à travers l'application et les transforme
 * en réponses HTTP structurées et cohérentes pour le client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Structure de réponse standard pour les erreurs API.
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
    }

    /**
     * Gère les erreurs de validation des DTOs (annotés avec @Valid).
     *
     * @param ex L'exception contenant les détails de la validation.
     * @return Une Map des champs et de leurs erreurs respectives (HTTP 400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }

    /**
     * Gère les échecs d'authentification (ex: identifiants incorrects, token invalide).
     *
     * @param ex L'exception d'authentification.
     * @return Une réponse d'erreur standard (HTTP 401 Unauthorized).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Échec de l'authentification : identifiants invalides ou token manquant/invalide."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Gère les refus d'accès pour des utilisateurs authentifiés mais non autorisés (problème de rôle).
     *
     * @param ex L'exception de refus d'accès.
     * @return Une réponse d'erreur standard (HTTP 403 Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Accès non autorisé pour cette ressource."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Gère les cas où une entité attendue n'est pas trouvée en base de données.
     *
     * @param ex L'exception indiquant que l'entité est introuvable.
     * @return Une réponse d'erreur standard (HTTP 404 Not Found).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "La ressource demandée n'a pas été trouvée."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère les conflits de données (ex: email déjà existant) ou les arguments invalides.
     *
     * @param ex L'exception indiquant un conflit ou un argument illégal.
     * @return Une réponse d'erreur standard (HTTP 409 Conflict).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Requête invalide ou conflit de données."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Gère les opérations non autorisées en raison de l'état actuel du système (ex: noter un événement non terminé).
     *
     * @param ex L'exception indiquant un état illégal.
     * @return Une réponse d'erreur standard (HTTP 409 Conflict).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Opération non permise dans l'état actuel de la ressource."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Gère le cas spécifique où un utilisateur tente de se connecter mais son compte n'est pas encore vérifié.
     *
     * @param ex L'exception de compte désactivé/non vérifié.
     * @return Un message d'erreur clair (HTTP 401 Unauthorized).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<String> handleDisabledException(DisabledException ex, WebRequest request) {
        String errorMessage = "Votre compte n'a pas été vérifié. Veuillez consulter l'email de vérification qui vous a été envoyé.";
        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Intercepte toutes les autres exceptions non gérées pour éviter de fuiter des détails techniques.
     *
     * @param ex L'exception inattendue.
     * @return Une réponse d'erreur générique (HTTP 500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String path = (request instanceof org.springframework.web.context.request.ServletWebRequest)
                ? ((org.springframework.web.context.request.ServletWebRequest) request).getRequest().getRequestURI()
                : "N/A";

        logger.error("Erreur inattendue (500) sur le chemin '{}' : {}", path, ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Une erreur interne inattendue est survenue. Veuillez réessayer plus tard."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

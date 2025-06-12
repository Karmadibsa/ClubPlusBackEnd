package org.clubplus.clubplusbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.ContactFormDto;
import org.clubplus.clubplusbackend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST gérant la soumission du formulaire de contact public.
 * <p>
 * Base URL: /contact
 * </p>
 */
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    private final EmailService emailService;

    /**
     * Gère la soumission d'un formulaire de contact.
     * <p>
     * Endpoint: POST /contact
     * <p>
     * Ce point d'accès est public. Il valide les données du formulaire,
     * puis délègue l'envoi de l'email au service compétent.
     *
     * @param contactFormDto Le DTO contenant les données du formulaire (nom, email, message).
     * @return Une réponse de succès (200 OK) ou une erreur serveur (500) si l'envoi échoue.
     */
    @PostMapping
    public ResponseEntity<String> handleContactForm(@Valid @RequestBody ContactFormDto contactFormDto) {
        try {
            emailService.sendContactFormEmail(contactFormDto);
            return ResponseEntity.ok("Message de contact envoyé avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'envoi de l'email de contact de la part de {}", contactFormDto.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'envoi du message.");
        }
    }
}

package org.clubplus.clubplusbackend.controller;

import jakarta.validation.Valid;
import org.clubplus.clubplusbackend.dto.ContactFormDto;
import org.clubplus.clubplusbackend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contact") // Endpoint de base pour les requêtes liées au contact
public class ContactController {

    private final EmailService emailService;

    @Autowired
    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<String> handleContactForm(@Valid @RequestBody ContactFormDto contactFormDto) {
        try {
            emailService.sendContactFormEmail(contactFormDto);
            return ResponseEntity.ok("Message de contact envoyé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'envoi du message.");
        }
    }
}

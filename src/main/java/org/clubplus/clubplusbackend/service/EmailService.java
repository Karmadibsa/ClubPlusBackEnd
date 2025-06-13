package org.clubplus.clubplusbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.clubplus.clubplusbackend.dto.ContactFormDto;
import org.clubplus.clubplusbackend.model.Membre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service gérant l'envoi d'emails transactionnels pour l'application.
 * <p>
 * Ce service utilise {@link JavaMailSender} pour l'envoi et {@link TemplateEngine} (Thymeleaf)
 * pour générer le contenu HTML des emails à partir de templates.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmailAddress;

    @Value("${app.backend.base.url}")
    private String backendBaseUrl;

    @Value("${app.contact.recipient-email}")
    private String contactRecipientEmail;

    @Value("${app.frontend.base.url}")
    private String frontendBaseUrl;

    /**
     * Envoie un email simple en format texte.
     *
     * @param toEmail L'adresse email du destinataire.
     * @param subject Le sujet de l'email.
     * @param body    Le corps de l'email.
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmailAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Email simple envoyé avec succès à {}", toEmail);
    }

    /**
     * Envoie un email de vérification de compte à un nouveau membre.
     * L'email est généré à partir du template HTML 'verification-email.html'.
     *
     * @param membre Le membre à qui envoyer l'email de vérification.
     * @throws MessagingException       si une erreur survient lors de la création ou de l'envoi de l'email.
     * @throws IllegalArgumentException si l'email ou le token de vérification du membre est null.
     */
    public void sendVerificationEmail(Membre membre) throws MessagingException {
        if (membre.getEmail() == null || membre.getVerificationToken() == null) {
            throw new IllegalArgumentException("L'email et le token de vérification du membre ne peuvent pas être nuls.");
        }
        log.debug("Préparation de l'envoi de l'email de vérification à {}", membre.getEmail());
        String verificationLink = backendBaseUrl + "/auth/verify-email?token=" + membre.getVerificationToken();

        Context context = new Context();
        context.setVariable("prenom", membre.getPrenom());
        context.setVariable("verificationLink", verificationLink);

        String htmlContent = templateEngine.process("verification-email.html", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmailAddress);
        helper.setTo(membre.getEmail());
        helper.setSubject("Bienvenue sur Club Plus - Veuillez vérifier votre adresse email");
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email de vérification envoyé avec succès à {}", membre.getEmail());
    }

    /**
     * Envoie un email de réinitialisation de mot de passe à un membre.
     * L'email est généré à partir du template HTML 'reset-password-email.html'.
     *
     * @param membre Le membre qui a demandé la réinitialisation.
     * @param token  Le token de réinitialisation généré.
     * @throws MessagingException       si une erreur survient lors de l'envoi de l'email.
     * @throws IllegalArgumentException si l'email ou le token est null.
     */
    public void sendPasswordResetEmail(Membre membre, String token) throws MessagingException {
        if (membre.getEmail() == null || token == null) {
            throw new IllegalArgumentException("L'email du membre et le token ne peuvent pas être nuls.");
        }
        log.debug("Préparation de l'envoi de l'email de réinitialisation à {}", membre.getEmail());
        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("prenom", membre.getPrenom());
        context.setVariable("resetLink", resetLink);

        String htmlContent = templateEngine.process("reset-password-email.html", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmailAddress);
        helper.setTo(membre.getEmail());
        helper.setSubject("Réinitialisation de votre mot de passe - Club Plus");
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email de réinitialisation de mot de passe envoyé à {}", membre.getEmail());
    }

    /**
     * Traite un formulaire de contact et envoie son contenu par email au destinataire configuré.
     * L'email est généré à partir du template HTML 'contact-email.html'.
     *
     * @param contactFormDto Les données du formulaire de contact.
     * @throws MessagingException si une erreur survient lors de l'envoi de l'email.
     */
    public void sendContactFormEmail(ContactFormDto contactFormDto) throws MessagingException {
        log.info("Réception d'un nouveau message de contact de la part de {}", contactFormDto.getEmail());
        Context context = new Context();
        context.setVariable("name", contactFormDto.getName());
        context.setVariable("email", contactFormDto.getEmail());
        context.setVariable("subject", contactFormDto.getSubject());
        context.setVariable("message", contactFormDto.getMessage());

        String emailBody = templateEngine.process("contact-email.html", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmailAddress);
        helper.setTo(contactRecipientEmail);
        helper.setSubject("Nouveau message de contact : " + contactFormDto.getSubject());
        helper.setText(emailBody, true);
        helper.setReplyTo(contactFormDto.getEmail());

        mailSender.send(mimeMessage);
        log.info("Email de contact de {} envoyé avec succès à {}", contactFormDto.getEmail(), contactRecipientEmail);
    }
}

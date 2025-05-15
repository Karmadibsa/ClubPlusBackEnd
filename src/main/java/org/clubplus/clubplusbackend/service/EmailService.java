package org.clubplus.clubplusbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender; // Injecté automatiquement par Spring grâce à la configuration

    @Autowired
    private TemplateEngine templateEngine; // Injectez le moteur de template


    @Value("${spring.mail.username}") // ou @Value("${spring.mail.from}")
    private String fromEmailAddress;

    @Value("${app.frontend.verification-url}")
    private String frontendVerificationUrl;

    /**
     * Envoie un e-mail simple.
     *
     * @param toEmail L'adresse e-mail du destinataire.
     * @param subject Le sujet de l'e-mail.
     * @param body    Le corps de l'e-mail.
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmailAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        System.out.println("E-mail envoyé avec succès à " + toEmail); // Pour le log/débogage
    }

    /**
     * Envoie un email de vérification à un membre nouvellement enregistré en utilisant un template HTML.
     *
     * @param membre Le membre à qui envoyer l'email.
     * @throws MessagingException Si l'envoi de l'email échoue.
     */
    public void sendVerificationEmail(Membre membre) throws MessagingException {
        if (membre.getEmail() == null || membre.getVerificationToken() == null) {
            throw new IllegalArgumentException("Membre email et token de vérification ne peuvent pas être nuls.");
        }

        String verificationLink = frontendVerificationUrl + "?token=" + membre.getVerificationToken();

        // Crée un contexte Thymeleaf pour passer les variables au template
        Context context = new Context();
        context.setVariable("prenom", membre.getPrenom());
        context.setVariable("verificationLink", verificationLink);

        // Rendre le template HTML en utilisant Thymeleaf
        String htmlContent = templateEngine.process("verification-email.html", context);

        // Créer un message MIME pour supporter le contenu HTML
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // true = multipart
        helper.setFrom(fromEmailAddress);
        helper.setTo(membre.getEmail());
        helper.setSubject("Bienvenue sur Club Plus - Veuillez vérifier votre adresse email");
        helper.setText(htmlContent, true); // true = isHtml

        mailSender.send(message);
        System.out.println("E-mail de vérification envoyé (HTML) à " + membre.getEmail());
    }
}

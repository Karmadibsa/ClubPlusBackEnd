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

    @Value("${APP_BACKEND_BASE_URL}") // Ceci devrait être l'URL de votre Netlify (ex: https://club-plus.netlify.app)
    private String backendBaseUrl;

    @Value("${APP_FRONTEND_BASE_URL}") // Ceci devrait être l'URL de votre Netlify (ex: https://club-plus.netlify.app)
    private String frontendBaseUrl;

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

        String verificationLink = backendBaseUrl + "/auth/verify-email?token=" + membre.getVerificationToken();

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

    public void sendPasswordResetEmail(Membre membre, String token) throws MessagingException {
        if (membre.getEmail() == null || token == null) {
            throw new IllegalArgumentException("L'email du membre et le token ne peuvent pas être nuls.");
        }

        String resetLink = frontendBaseUrl + "?token=" + token; // Le lien vers votre page Angular

        Context context = new Context();
        context.setVariable("prenom", membre.getPrenom());
        context.setVariable("resetLink", resetLink);

        String htmlContent = templateEngine.process("reset-password-email.html", context); // Nouveau template

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmailAddress);
        helper.setTo(membre.getEmail());
        helper.setSubject("Réinitialisation de votre mot de passe - Club Plus");
        helper.setText(htmlContent, true);

        mailSender.send(message);
        System.out.println("Email de réinitialisation de mot de passe envoyé à " + membre.getEmail());
    }
}

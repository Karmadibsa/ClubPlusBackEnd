package org.clubplus.clubplusbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.clubplus.clubplusbackend.dto.ContactFormDto;
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

    @Value("${app.backend.base.url}") // Ceci devrait être l'URL de votre Netlify (ex: https://club-plus.netlify.app)
    private String backendBaseUrl;

    @Value("${app.contact.recipient-email}") // L'email du destinataire (à ajouter dans application.properties)
    private String contactRecipientEmail;

    @Value("${app.frontend.base.url}") // Ceci devrait être l'URL de votre Netlify (ex: https://club-plus.netlify.app)
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
        System.out.println("BAckend : " + backendBaseUrl);
        System.out.println("Frontend : " + frontendBaseUrl);
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

        String resetLink = frontendBaseUrl + "/reset-password?token=" + token; // Le lien vers votre page Angular

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

    /**
     * Envoie un email contenant les données du formulaire de contact.
     *
     * @param contactFormDto Les données du formulaire de contact.
     * @throws MessagingException Si une erreur survient lors de la création ou de l'envoi de l'email.
     */
    public void sendContactFormEmail(ContactFormDto contactFormDto) throws MessagingException {
        Context context = new Context();
        context.setVariable("name", contactFormDto.getName());
        context.setVariable("email", contactFormDto.getEmail());
        context.setVariable("subject", contactFormDto.getSubject());
        context.setVariable("message", contactFormDto.getMessage());

        String emailBody = templateEngine.process("contact-email.html", context); // Chemin vers votre template

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // true pour multipart, UTF-8 pour l'encodage

        helper.setFrom(fromEmailAddress); // Ou contactFormDto.getEmail() si vous voulez que le "De" soit l'email de l'utilisateur, mais cela peut causer des problèmes de délivrabilité (SPF/DKIM). Il est souvent mieux d'envoyer depuis votre adresse et de mettre l'email de l'utilisateur dans le corps ou en "Reply-To".
        helper.setTo(contactRecipientEmail); // Adresse email où recevoir les messages de contact
        helper.setSubject("Nouveau message de contact : " + contactFormDto.getSubject());
        helper.setText(emailBody, true); // true pour indiquer que le corps est du HTML

        // Optionnel: Si vous voulez que le bouton "Répondre" réponde directement à l'utilisateur qui a rempli le formulaire
        helper.setReplyTo(contactFormDto.getEmail());

        mailSender.send(mimeMessage);
    }
}

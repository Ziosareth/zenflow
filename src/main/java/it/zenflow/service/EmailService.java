package it.zenflow.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final TemplateEngine templateEngine;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        log.info("Sending email to: {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /**
     * Send an invitation email with a temporary password
     */
    public void sendInvitationEmail(String to, String username, String temporaryPassword, Locale locale) {
        log.info("Sending invitation email to: {}", to);
        String subject = messageSource.getMessage("email.invitation.subject", null, locale);
        String content = messageSource.getMessage("email.invitation.content", 
                new Object[]{username, temporaryPassword}, locale);
        
        sendSimpleEmail(to, subject, content);
    }
    
    /**
     * Send an HTML email using Thymeleaf templates
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables, Locale locale) throws MessagingException {
        log.info("Sending HTML email to: {}", to);
        // Prepare the context with variables for the template
        Context context = new Context(locale);
        variables.forEach(context::setVariable);
        
        // Process the template
        String htmlContent = templateEngine.process(templateName, context);
        
        // Create the email message
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true indicates HTML content
        
        // Send the email
        mailSender.send(mimeMessage);
    }
}
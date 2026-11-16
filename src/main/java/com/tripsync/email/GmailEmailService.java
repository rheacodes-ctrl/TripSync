package com.tripsync.email;

import com.tripsync.config.EmailConfig;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends email through Gmail SMTP using Jakarta Mail.
 *
 * <p>Credentials come from {@link EmailConfig}, which reads the
 * {@code TRIPSYNC_EMAIL} and {@code TRIPSYNC_EMAIL_APP_PASSWORD}
 * environment variables. No credentials are stored in source code.
 *
 * <p>The service never throws on SMTP failure: errors are logged and
 * the send method returns false, so the rest of the application keeps
 * running. A real email is only attempted when the environment is
 * configured; nothing is sent from automated tests (those use
 * {@link MockEmailService}).
 */
public class GmailEmailService implements EmailService {

    private final EmailConfig config;

    /** Creates a service using Gmail credentials from the environment. */
    public GmailEmailService() {
        this.config = new EmailConfig();
    }

    @Override
    public boolean sendEmail(String to, String subject, String htmlBody) {

        if (to == null || to.isBlank()) {
            System.err.println("[email] Cannot send: recipient is empty.");
            return false;
        }

        if (!config.isConfigured()) {
            System.err.println(
                "[email] Gmail is not configured. "
                + "Set TRIPSYNC_EMAIL and TRIPSYNC_EMAIL_APP_PASSWORD.");
            return false;
        }

        try {
            MimeMessage message = buildMessage(to, subject, htmlBody);
            Transport.send(message, config.getEmail(),
                    config.getAppPassword());
            System.out.println(
                "[email] Sent to " + to + " | subject: " + subject);
            return true;
        } catch (MessagingException e) {
            System.err.println(
                "[email] SMTP failure for " + to + ": "
                + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            System.err.println(
                "[email] Unexpected error for " + to + ": "
                + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendInvitationEmail(String to, String tripName,
            String inviterName, String details,
            String acceptUrl, String rejectUrl) {

        String subject = InvitationEmailTemplate.buildSubject(tripName);
        String html = InvitationEmailTemplate.render(
            tripName, inviterName, details, acceptUrl, rejectUrl);
        return sendEmail(to, subject, html);
    }

    private MimeMessage buildMessage(String to, String subject,
            String htmlBody) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable",
                String.valueOf(config.isStarttlsEnabled()));
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", config.getSmtpHost());

        Session session = Session.getInstance(props);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getEmail()));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(to));
        message.setSubject(subject == null ? "" : subject, "UTF-8");
        message.setContent(
            htmlBody == null ? "" : htmlBody,
            "text/html; charset=utf-8");
        return message;
    }
}

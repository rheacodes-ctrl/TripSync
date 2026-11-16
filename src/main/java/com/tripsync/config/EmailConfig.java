package com.tripsync.config;

/**
 * Gmail SMTP configuration loaded from environment variables.
 *
 * <p>Credentials are never hardcoded or written to source control.
 * TripSync reads:
 * <ul>
 *   <li>{@code TRIPSYNC_EMAIL} - the Gmail address used as the SMTP
 *       login and the "From" address.</li>
 *   <li>{@code TRIPSYNC_EMAIL_APP_PASSWORD} - a 16-character Gmail
 *       App Password. Never use your normal Gmail password.</li>
 * </ul>
 *
 * <p>Email sending is optional: if the variables are not set,
 * {@link #isConfigured()} returns false and the application keeps
 * running without throwing.
 */
public class EmailConfig {

    // Public Gmail SMTP constants. These are not secrets.
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    private final String email;
    private final String appPassword;

    /** Reads Gmail credentials from the environment. */
    public EmailConfig() {
        this.email = System.getenv("TRIPSYNC_EMAIL");
        this.appPassword = System.getenv("TRIPSYNC_EMAIL_APP_PASSWORD");
    }

    public String getEmail() {
        return email;
    }

    public String getAppPassword() {
        return appPassword;
    }

    public String getSmtpHost() {
        return SMTP_HOST;
    }

    public int getSmtpPort() {
        return SMTP_PORT;
    }

    /** Gmail uses STARTTLS on port 587. */
    public boolean isStarttlsEnabled() {
        return true;
    }

    /** True only when both environment variables are present. */
    public boolean isConfigured() {
        return email != null && !email.isBlank()
                && appPassword != null && !appPassword.isBlank();
    }
}

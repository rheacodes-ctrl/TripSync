package com.tripsync.email;

/**
 * Sends email on behalf of TripSync.
 *
 * <p>Implementations must not crash the application on SMTP failure.
 * The send methods return {@code true} on success and {@code false}
 * on failure, logging the underlying error rather than throwing.
 */
public interface EmailService {

    /**
     * Sends an arbitrary HTML email.
     *
     * @param to       recipient email address
     * @param subject  email subject line
     * @param htmlBody HTML body content
     * @return true if the message was delivered, false otherwise
     */
    boolean sendEmail(String to, String subject, String htmlBody);

    /**
     * Sends a trip invitation email rendered from the reusable HTML
     * template.
     *
     * @param to          recipient email address
     * @param tripName    name of the trip
     * @param inviterName display name of the inviting user
     * @param details     extra invitation details (may be null/blank)
     * @param acceptUrl   URL the recipient follows to accept
     * @param rejectUrl   URL the recipient follows to decline
     * @return true if the message was delivered, false otherwise
     */
    boolean sendInvitationEmail(String to, String tripName,
            String inviterName, String details,
            String acceptUrl, String rejectUrl);
}

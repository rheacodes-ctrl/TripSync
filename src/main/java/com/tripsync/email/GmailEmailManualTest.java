package com.tripsync.email;

import com.tripsync.config.EmailConfig;

/**
 * Manual, explicit test for {@link GmailEmailService}.
 *
 * <p>This sends a REAL email to the configured Gmail address when
 * both environment variables are set. Run it only when you intend to
 * test Gmail delivery. It is never invoked by automated tests.
 *
 * <p>Set before running:
 * <pre>
 *   export TRIPSYNC_EMAIL='your.address@gmail.com'
 *   export TRIPSYNC_EMAIL_APP_PASSWORD='your-16-char-app-password'
 * </pre>
 */
public class GmailEmailManualTest {

    public static void main(String[] args) {

        System.out.println("========== GMAIL MANUAL EMAIL TEST ==========");
        System.out.println("This sends a REAL email. Run it only when you");
        System.out.println("intend to test Gmail delivery.");

        EmailConfig config = new EmailConfig();

        if (!config.isConfigured()) {
            System.err.println(
                "\nGmail is not configured. Set both environment");
            System.err.println("variables before running this test:");
            System.err.println("  TRIPSYNC_EMAIL");
            System.err.println("  TRIPSYNC_EMAIL_APP_PASSWORD");
            return;
        }

        GmailEmailService service = new GmailEmailService();

        // Send the test invitation to yourself, so no third party is
        // contacted during a manual check.
        boolean sent = service.sendInvitationEmail(
            config.getEmail(),
            "Goa Vacation",
            "Ishi",
            "This is a manual TripSync invitation test. "
                + "Accepting or declining this does nothing here.",
            "https://tripsync.local/invitations/MANUAL/accept",
            "https://tripsync.local/invitations/MANUAL/reject"
        );

        System.out.println(
            "Manual send result: " + (sent ? "SENT" : "FAILED"));

        System.out.println(
            "\n========== GMAIL MANUAL EMAIL TEST DONE ==========");
    }
}

package com.tripsync.email;

/**
 * Automated test for the email subsystem.
 *
 * <p>Uses only {@link MockEmailService}, so it never connects to
 * Gmail and never sends a real message. It verifies:
 * <ul>
 *   <li>invitation emails are recorded with the right fields,</li>
 *   <li>empty recipients are rejected,</li>
 *   <li>the template HTML-escapes untrusted text (XSS guard),</li>
 *   <li>{@code javascript:} links are not rendered,</li>
 *   <li>the subject falls back when the trip name is blank.</li>
 * </ul>
 */
public class EmailServiceTest {

    public static void main(String[] args) {

        System.out.println("========== EMAIL SERVICE TEST ==========");

        // 1. Send an invitation email and verify what was recorded.
        MockEmailService emailService = new MockEmailService(true);

        boolean sent = emailService.sendInvitationEmail(
            "friend@example.com",
            "Goa Vacation",
            "Ishi",
            "Join us in Goa from January 10 to 15, 2027.",
            "https://tripsync.local/invitations/abc/accept",
            "https://tripsync.local/invitations/abc/reject"
        );

        check(sent, "sendInvitationEmail should return true");
        check(emailService.getSentCount() == 1,
            "exactly one email should be recorded");

        SentEmail recorded = emailService.getSentEmails().get(0);

        check("friend@example.com".equals(recorded.to()),
            "recipient should match");
        check(recorded.subject() != null
                && recorded.subject().contains("Goa Vacation"),
            "subject should contain the trip name");
        check(recorded.htmlBody().contains("Goa Vacation"),
            "body should contain the trip name");
        check(recorded.htmlBody().contains("Ishi"),
            "body should contain the inviter name");
        check(recorded.htmlBody().contains(
                "https://tripsync.local/invitations/abc/accept"),
            "body should contain the accept link");
        check(recorded.htmlBody().contains(
                "https://tripsync.local/invitations/abc/reject"),
            "body should contain the reject link");

        // 2. Empty recipient must be rejected.
        boolean bad = emailService.sendEmail("", "Subject", "<p>hi</p>");
        check(!bad, "empty recipient should fail");
        check(emailService.getSentCount() == 1,
            "no email should be recorded for an empty recipient");

        // 3. Untrusted text and unsafe URLs must be neutralised.
        MockEmailService escapeService = new MockEmailService(false);
        escapeService.sendInvitationEmail(
            "x@example.com",
            "<script>alert(1)</script>",
            "Ishi",
            null,
            "javascript:alert(1)",
            "https://tripsync.local/reject"
        );
        SentEmail escaped = escapeService.getSentEmails().get(0);

        check(!escaped.htmlBody().contains("<script>alert(1)</script>"),
            "script tag must not appear unescaped");
        check(escaped.htmlBody().contains("&lt;script&gt;"),
            "script tag should be HTML-escaped");
        check(!escaped.htmlBody().contains("javascript:alert"),
            "javascript: URL must not be rendered as a link");

        // 4. Subject fallback for a blank trip name.
        String fallback = InvitationEmailTemplate.buildSubject(null);
        check(fallback.contains("a trip"),
            "subject should fall back when the trip name is blank");

        System.out.println(
            "\n========== EMAIL SERVICE TEST PASSED ==========");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(
                "Email service test failed: " + message);
        }
        System.out.println("  ok - " + message);
    }
}

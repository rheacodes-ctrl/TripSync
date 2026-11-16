package com.tripsync.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test double for {@link EmailService}.
 *
 * <p>Records every email it is asked to send in an in-memory list and
 * never opens a network connection. Automated tests assert on
 * {@link #getSentEmails()}, so they run fully offline and never send
 * real Gmail messages.
 */
public class MockEmailService implements EmailService {

    private final List<SentEmail> sentEmails = new ArrayList<>();
    private final boolean printToConsole;

    /** Creates a mock that prints a line for each recorded email. */
    public MockEmailService() {
        this(true);
    }

    /**
     * Creates a mock that optionally prints a line for each recorded
     * email.
     *
     * @param printToConsole whether to log recorded emails to stdout
     */
    public MockEmailService(boolean printToConsole) {
        this.printToConsole = printToConsole;
    }

    @Override
    public boolean sendEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            System.err.println(
                "[mock-email] Cannot record email: recipient is empty.");
            return false;
        }

        SentEmail email = new SentEmail(
            to,
            subject == null ? "" : subject,
            htmlBody == null ? "" : htmlBody
        );

        sentEmails.add(email);

        if (printToConsole) {
            System.out.println(
                "[mock-email] Recorded email to " + to
                + " | subject: " + subject);
        }

        return true;
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

    /** Returns an unmodifiable view of the recorded emails. */
    public List<SentEmail> getSentEmails() {
        return Collections.unmodifiableList(sentEmails);
    }

    /** Returns the number of emails recorded so far. */
    public int getSentCount() {
        return sentEmails.size();
    }

    /** Clears all recorded emails. */
    public void clear() {
        sentEmails.clear();
    }
}

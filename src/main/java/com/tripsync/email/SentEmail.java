package com.tripsync.email;

/**
 * Immutable snapshot of an email that was handed to an
 * {@link EmailService}. Used by {@link MockEmailService} so automated
 * tests can assert on what would have been sent, without any network
 * access.
 */
public record SentEmail(String to, String subject, String htmlBody) {
}

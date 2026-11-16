package com.tripsync.model;

import java.time.LocalDateTime;

/**
 * A trip invitation record.
 *
 * <p>Instances are immutable. The raw invitation token is never held
 * by this model; only the SHA-256 {@code tokenHash} is stored. The
 * raw token exists only briefly inside the manager while building the
 * invitation link and email, and is never logged or persisted.
 */
public class TripInvitation {

    private final String invitationId;
    private final String tripId;
    private final String inviterId;
    private final String inviteeEmail;
    private final String inviteeUserId;   // nullable
    private final String tokenHash;       // stored hash only
    private final InvitationStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime respondedAt; // nullable
    private final int resendCount;

    /**
     * Constructor for a brand-new invitation. Status defaults to
     * {@link InvitationStatus#PENDING}, created time to now, response
     * time to {@code null} and resend count to 0.
     */
    public TripInvitation(String invitationId, String tripId,
            String inviterId, String inviteeEmail,
            String inviteeUserId, String tokenHash,
            LocalDateTime expiresAt) {

        this(invitationId, tripId, inviterId, inviteeEmail, inviteeUserId,
                tokenHash, InvitationStatus.PENDING, LocalDateTime.now(),
                expiresAt, null, 0);
    }

    /** Constructor for reconstructing an invitation from the database. */
    public TripInvitation(String invitationId, String tripId,
            String inviterId, String inviteeEmail, String inviteeUserId,
            String tokenHash, InvitationStatus status,
            LocalDateTime createdAt, LocalDateTime expiresAt,
            LocalDateTime respondedAt, int resendCount) {

        if (invitationId == null || invitationId.isBlank()) {
            throw new IllegalArgumentException("Invitation id is required.");
        }
        if (tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException("Trip id is required.");
        }
        if (inviterId == null || inviterId.isBlank()) {
            throw new IllegalArgumentException("Inviter id is required.");
        }
        if (inviteeEmail == null || inviteeEmail.isBlank()) {
            throw new IllegalArgumentException("Invitee email is required.");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Token hash is required.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiry time is required.");
        }

        this.invitationId = invitationId;
        this.tripId = tripId;
        this.inviterId = inviterId;
        this.inviteeEmail = inviteeEmail;
        this.inviteeUserId = inviteeUserId;
        this.tokenHash = tokenHash;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.respondedAt = respondedAt;
        this.resendCount = resendCount;
    }

    public String getInvitationId() {
        return invitationId;
    }

    public String getTripId() {
        return tripId;
    }

    public String getInviterId() {
        return inviterId;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public String getInviteeUserId() {
        return inviteeUserId;
    }

    /** The stored SHA-256 hash. Internal use only; never expose to
     *  API responses. */
    public String getTokenHash() {
        return tokenHash;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public int getResendCount() {
        return resendCount;
    }
}

package com.tripsync.model;

/**
 * Lifecycle status of a trip invitation.
 *
 * <p>The {@link #name()} of each constant matches the MySQL
 * {@code ENUM} values in {@code trip_invitations.status}, so they can
 * be stored and read directly.
 */
public enum InvitationStatus {
    /** Created and emailed; awaiting the invitee's response. */
    PENDING,
    /** The invitee accepted and was added to the trip. */
    ACCEPTED,
    /** The invitee explicitly declined. */
    REJECTED,
    /** The invitation passed its expiry time without a response. */
    EXPIRED,
    /** The inviter or a trip admin cancelled a pending invitation. */
    CANCELLED
}

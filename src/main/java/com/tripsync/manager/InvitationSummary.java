package com.tripsync.manager;

import com.tripsync.model.InvitationStatus;

import java.time.LocalDateTime;

/**
 * A safe view of a trip invitation returned to callers. It is the
 * public projection of {@link com.tripsync.model.TripInvitation} with
 * the token hash removed, so token hashes are never exposed through
 * the manager API.
 */
public record InvitationSummary(
        String invitationId,
        String tripId,
        String inviterId,
        String inviteeEmail,
        String inviteeUserId,
        InvitationStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime respondedAt,
        int resendCount) {
}

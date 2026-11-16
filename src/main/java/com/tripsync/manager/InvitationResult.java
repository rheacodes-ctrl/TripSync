package com.tripsync.manager;

import com.tripsync.model.InvitationStatus;

/**
 * Outcome of an invitation command (send, accept, reject, cancel,
 * resend). It never contains the raw token or the token hash.
 */
public record InvitationResult(
        boolean success,
        String invitationId,
        InvitationStatus status,
        String message) {

    public static InvitationResult success(String invitationId,
            InvitationStatus status, String message) {
        return new InvitationResult(true, invitationId, status, message);
    }

    public static InvitationResult failure(String invitationId,
            InvitationStatus status, String message) {
        return new InvitationResult(false, invitationId, status, message);
    }
}

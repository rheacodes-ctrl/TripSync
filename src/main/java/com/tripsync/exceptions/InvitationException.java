package com.tripsync.exceptions;

import com.tripsync.model.InvitationStatus;

/**
 * Raised by the invitation DAO/manager when an invitation operation
 * cannot be completed for a business reason (invalid token, not
 * pending, expired, wrong user, already a member, etc.).
 *
 * <p>It carries the invitation id and the relevant status when known,
 * so callers can produce a meaningful result without re-querying.
 * It never carries the raw token or token hash.
 */
public class InvitationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String invitationId;
    private final InvitationStatus status;

    public InvitationException(String message, String invitationId,
            InvitationStatus status) {
        super(message);
        this.invitationId = invitationId;
        this.status = status;
    }

    public String getInvitationId() {
        return invitationId;
    }

    public InvitationStatus getStatus() {
        return status;
    }
}

-- ======================================================================
-- TripSync Phase 2 migration: trip_invitations
-- File: database/migrations/002_create_trip_invitations.sql
--
-- Safe to run on the existing database. It only creates the new table
-- if it does not already exist. It does not alter, drop, rename or
-- otherwise change any existing table or existing data.
--
-- Run once, e.g.:
--   mysql -u root -p tripsync < database/migrations/002_create_trip_invitations.sql
-- ======================================================================

USE tripsync;

CREATE TABLE IF NOT EXISTS trip_invitations (
    invitation_id    VARCHAR(36)  NOT NULL,
    trip_id          VARCHAR(36)  NOT NULL,
    inviter_id       VARCHAR(36)  NOT NULL,
    invitee_email    VARCHAR(150) NOT NULL,
    -- Populated only when the invitee is an existing registered user.
    -- NULL means the invitee has not registered yet.
    invitee_user_id  VARCHAR(36)  NULL,
    -- SHA-256 hash of the raw invitation token, hex-encoded (64 chars).
    -- The raw token is never stored.
    token_hash       CHAR(64)     NOT NULL,
    status           ENUM('PENDING','ACCEPTED','REJECTED','EXPIRED','CANCELLED')
                     NOT NULL DEFAULT 'PENDING',
    -- Counts how often the invitation has been resent, to cap abuse.
    resend_count     INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       TIMESTAMP    NOT NULL,
    responded_at     TIMESTAMP    NULL,

    PRIMARY KEY (invitation_id),

    FOREIGN KEY (trip_id)
        REFERENCES trips(trip_id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_user_id)
        REFERENCES users(user_id) ON DELETE SET NULL,

    -- At most one invitation per (trip, email, status). Because the
    -- status is part of the key, this prevents two PENDING invitations
    -- for the same trip + email (the manager also checks explicitly),
    -- while still allowing the full lifecycle PENDING -> ACCEPTED etc.
    UNIQUE KEY uq_invitation_trip_email_status
        (trip_id, invitee_email, status),

    -- Token hashes are unique and indexed for fast parameterized lookup
    -- by token when an invitee accepts or rejects.
    UNIQUE KEY uq_invitation_token_hash (token_hash),

    KEY idx_invitation_trip (trip_id),
    KEY idx_invitation_email (invitee_email),
    KEY idx_invitation_status (status),

    -- An invitation must expire after it was created.
    CONSTRAINT chk_invitation_expiry
        CHECK (expires_at > created_at)
);

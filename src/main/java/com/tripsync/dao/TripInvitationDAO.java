package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.exceptions.InvitationException;
import com.tripsync.model.InvitationStatus;
import com.tripsync.model.TripInvitation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for {@code trip_invitations}.
 *
 * <p>All SQL uses {@link PreparedStatement} with bound parameters; no
 * untrusted value is concatenated into a query. Accept and reject are
 * implemented as single JDBC transactions that lock the invitation row
 * ({@code SELECT ... FOR UPDATE}) so concurrent responses cannot race.
 */
public class TripInvitationDAO {

    // Reused for transactional membership insertion on accept.
    private final TripDAO tripDAO = new TripDAO();

    // ==========================================
    // CREATE
    // ==========================================

    public boolean createInvitation(TripInvitation invitation)
            throws SQLException {

        if (invitation == null) {
            throw new IllegalArgumentException("Invitation is required.");
        }

        String sql = """
                INSERT INTO trip_invitations
                (invitation_id, trip_id, inviter_id, invitee_email,
                 invitee_user_id, token_hash, status, resend_count,
                 expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, invitation.getInvitationId());
            statement.setString(2, invitation.getTripId());
            statement.setString(3, invitation.getInviterId());
            statement.setString(4, invitation.getInviteeEmail());

            if (invitation.getInviteeUserId() == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, invitation.getInviteeUserId());
            }

            statement.setString(6, invitation.getTokenHash());
            statement.setString(7, invitation.getStatus().name());
            statement.setInt(8, invitation.getResendCount());
            statement.setTimestamp(9,
                    Timestamp.valueOf(invitation.getExpiresAt()));

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // READ BY ID
    // ==========================================

    public TripInvitation getInvitationById(String invitationId)
            throws SQLException {

        if (invitationId == null || invitationId.isBlank()) {
            return null;
        }

        String sql = "SELECT * FROM trip_invitations WHERE invitation_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, invitationId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return map(result);
                }
            }
        }

        return null;
    }

    // ==========================================
    // READ BY TOKEN HASH
    // ==========================================

    public TripInvitation getInvitationByTokenHash(String tokenHash)
            throws SQLException {

        if (tokenHash == null || tokenHash.isBlank()) {
            return null;
        }

        String sql = "SELECT * FROM trip_invitations WHERE token_hash = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return map(result);
                }
            }
        }

        return null;
    }

    // ==========================================
    // LIST BY TRIP
    // ==========================================

    public List<TripInvitation> getInvitationsByTripId(String tripId)
            throws SQLException {

        List<TripInvitation> invitations = new ArrayList<>();

        String sql = """
                SELECT * FROM trip_invitations
                WHERE trip_id = ?
                ORDER BY created_at DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    invitations.add(map(result));
                }
            }
        }

        return invitations;
    }

    // ==========================================
    // PENDING INVITATIONS FOR AN EMAIL
    // ==========================================

    public List<TripInvitation> getPendingInvitationsByEmail(String email)
            throws SQLException {

        List<TripInvitation> invitations = new ArrayList<>();

        String sql = """
                SELECT * FROM trip_invitations
                WHERE invitee_email = ? AND status = 'PENDING'
                ORDER BY created_at DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    invitations.add(map(result));
                }
            }
        }

        return invitations;
    }

    // ==========================================
    // PENDING INVITATIONS FOR A TRIP
    // ==========================================

    public List<TripInvitation> getPendingInvitationsByTripId(String tripId)
            throws SQLException {

        List<TripInvitation> invitations = new ArrayList<>();

        String sql = """
                SELECT * FROM trip_invitations
                WHERE trip_id = ? AND status = 'PENDING'
                ORDER BY created_at DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    invitations.add(map(result));
                }
            }
        }

        return invitations;
    }

    // ==========================================
    // ALL INVITATIONS FOR AN EMAIL
    // ==========================================

    public List<TripInvitation> getInvitationsByEmail(String email)
            throws SQLException {

        List<TripInvitation> invitations = new ArrayList<>();

        String sql = """
                SELECT * FROM trip_invitations
                WHERE invitee_email = ?
                ORDER BY created_at DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    invitations.add(map(result));
                }
            }
        }

        return invitations;
    }

    // ==========================================
    // UPDATE STATUS
    // ==========================================

    public boolean updateStatus(String invitationId,
            InvitationStatus status) throws SQLException {

        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }

        String sql = """
                UPDATE trip_invitations
                SET status = ?
                WHERE invitation_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setString(2, invitationId);

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // CANCEL (only a pending invitation can be cancelled)
    // ==========================================

    public boolean cancelInvitation(String invitationId)
            throws SQLException {

        String sql = """
                UPDATE trip_invitations
                SET status = 'CANCELLED'
                WHERE invitation_id = ? AND status = 'PENDING'
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, invitationId);

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // MARK EXPIRED INVITATIONS (maintenance)
    // ==========================================

    public int markExpiredInvitations() throws SQLException {

        String sql = """
                UPDATE trip_invitations
                SET status = 'EXPIRED'
                WHERE status = 'PENDING'
                  AND expires_at <= CURRENT_TIMESTAMP
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            return statement.executeUpdate();
        }
    }

    // ==========================================
    // CHECK FOR AN EXISTING PENDING INVITATION
    // ==========================================

    public boolean hasPendingInvitation(String tripId, String email)
            throws SQLException {

        String sql = """
                SELECT 1 FROM trip_invitations
                WHERE trip_id = ? AND invitee_email = ? AND status = 'PENDING'
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);
            statement.setString(2, email);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // ==========================================
    // UPDATE FOR RESEND (new token, new expiry, increment count)
    // ==========================================

    public boolean updateForResend(String invitationId, String tokenHash,
            LocalDateTime expiresAt, int maxResends) throws SQLException {

        String sql = """
                UPDATE trip_invitations
                SET token_hash = ?, expires_at = ?,
                    resend_count = resend_count + 1
                WHERE invitation_id = ?
                  AND status = 'PENDING'
                  AND resend_count < ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);
            statement.setTimestamp(2, Timestamp.valueOf(expiresAt));
            statement.setString(3, invitationId);
            statement.setInt(4, maxResends);

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // DELETE (used by tests / admin purge; not part of the lifecycle)
    // ==========================================

    public boolean deleteInvitation(String invitationId)
            throws SQLException {

        String sql = "DELETE FROM trip_invitations WHERE invitation_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, invitationId);

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // ACCEPT (atomic: validate + add member + update status)
    // ==========================================

    public TripInvitation acceptInvitation(String tokenHash, String userId,
            String userEmail) throws SQLException {

        if (tokenHash == null || tokenHash.isBlank()
                || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Token hash and user id are required.");
        }

        String selectSql =
                "SELECT * FROM trip_invitations WHERE token_hash = ? FOR UPDATE";
        String memberCheckSql =
                "SELECT 1 FROM trip_members WHERE trip_id = ? AND user_id = ?";
        String updateSql = """
                UPDATE trip_invitations
                SET status = 'ACCEPTED', responded_at = CURRENT_TIMESTAMP
                WHERE invitation_id = ? AND status = 'PENDING'
                """;

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);
            TripInvitation result;

            try {
                TripInvitation invitation;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setString(1, tokenHash);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new InvitationException(
                                    "Invalid invitation token.",
                                    null, null);
                        }
                        invitation = map(rs);
                    }
                }

                if (invitation.getStatus() != InvitationStatus.PENDING) {
                    throw new InvitationException(
                            "Invitation is no longer pending (current status: "
                                    + invitation.getStatus() + ").",
                            invitation.getInvitationId(),
                            invitation.getStatus());
                }

                if (!invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
                    throw new InvitationException(
                            "Invitation has expired.",
                            invitation.getInvitationId(),
                            InvitationStatus.EXPIRED);
                }

                if (userEmail == null
                        || !userEmail.equalsIgnoreCase(
                                invitation.getInviteeEmail())) {
                    throw new InvitationException(
                            "This invitation was not sent to your email address.",
                            invitation.getInvitationId(),
                            invitation.getStatus());
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(memberCheckSql)) {

                    statement.setString(1, invitation.getTripId());
                    statement.setString(2, userId);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            throw new InvitationException(
                                    "You are already a member of this trip.",
                                    invitation.getInvitationId(),
                                    invitation.getStatus());
                        }
                    }
                }

                // Insert the trip member on the same connection.
                tripDAO.addMember(connection,
                        invitation.getTripId(), userId);

                int updated;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setString(1, invitation.getInvitationId());
                    updated = statement.executeUpdate();
                }

                if (updated == 0) {
                    throw new InvitationException(
                            "Invitation could not be accepted "
                                    + "(it is no longer pending).",
                            invitation.getInvitationId(), null);
                }

                connection.commit();

                result = new TripInvitation(
                        invitation.getInvitationId(),
                        invitation.getTripId(),
                        invitation.getInviterId(),
                        invitation.getInviteeEmail(),
                        invitation.getInviteeUserId(),
                        invitation.getTokenHash(),
                        InvitationStatus.ACCEPTED,
                        invitation.getCreatedAt(),
                        invitation.getExpiresAt(),
                        LocalDateTime.now(),
                        invitation.getResendCount());

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException ignore) {
                    // Best-effort rollback; propagate the original error.
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignore) {
                    // Best-effort reset.
                }
            }

            return result;
        }
    }

    // ==========================================
    // REJECT (atomic: validate + update status)
    // ==========================================

    public TripInvitation rejectInvitation(String tokenHash, String userId,
            String userEmail) throws SQLException {

        if (tokenHash == null || tokenHash.isBlank()
                || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Token hash and user id are required.");
        }

        String selectSql =
                "SELECT * FROM trip_invitations WHERE token_hash = ? FOR UPDATE";
        String updateSql = """
                UPDATE trip_invitations
                SET status = 'REJECTED', responded_at = CURRENT_TIMESTAMP
                WHERE invitation_id = ? AND status = 'PENDING'
                """;

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);
            TripInvitation result;

            try {
                TripInvitation invitation;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setString(1, tokenHash);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new InvitationException(
                                    "Invalid invitation token.",
                                    null, null);
                        }
                        invitation = map(rs);
                    }
                }

                if (invitation.getStatus() != InvitationStatus.PENDING) {
                    throw new InvitationException(
                            "Invitation is no longer pending (current status: "
                                    + invitation.getStatus() + ").",
                            invitation.getInvitationId(),
                            invitation.getStatus());
                }

                if (!invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
                    throw new InvitationException(
                            "Invitation has expired.",
                            invitation.getInvitationId(),
                            InvitationStatus.EXPIRED);
                }

                if (userEmail == null
                        || !userEmail.equalsIgnoreCase(
                                invitation.getInviteeEmail())) {
                    throw new InvitationException(
                            "This invitation was not sent to your email address.",
                            invitation.getInvitationId(),
                            invitation.getStatus());
                }

                int updated;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setString(1, invitation.getInvitationId());
                    updated = statement.executeUpdate();
                }

                if (updated == 0) {
                    throw new InvitationException(
                            "Invitation could not be rejected "
                                    + "(it is no longer pending).",
                            invitation.getInvitationId(), null);
                }

                connection.commit();

                result = new TripInvitation(
                        invitation.getInvitationId(),
                        invitation.getTripId(),
                        invitation.getInviterId(),
                        invitation.getInviteeEmail(),
                        invitation.getInviteeUserId(),
                        invitation.getTokenHash(),
                        InvitationStatus.REJECTED,
                        invitation.getCreatedAt(),
                        invitation.getExpiresAt(),
                        LocalDateTime.now(),
                        invitation.getResendCount());

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException ignore) {
                    // Best-effort rollback; propagate the original error.
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignore) {
                    // Best-effort reset.
                }
            }

            return result;
        }
    }

    // ==========================================
    // MAP A ROW TO A TRIP INVITATION
    // ==========================================

    private TripInvitation map(ResultSet rs) throws SQLException {

        Timestamp respondedAt = rs.getTimestamp("responded_at");

        return new TripInvitation(
                rs.getString("invitation_id"),
                rs.getString("trip_id"),
                rs.getString("inviter_id"),
                rs.getString("invitee_email"),
                rs.getString("invitee_user_id"),
                rs.getString("token_hash"),
                InvitationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                respondedAt == null ? null : respondedAt.toLocalDateTime(),
                rs.getInt("resend_count")
        );
    }
}

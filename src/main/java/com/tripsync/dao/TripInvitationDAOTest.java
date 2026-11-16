package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.exceptions.InvitationException;
import com.tripsync.model.InvitationStatus;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.TripInvitation;
import com.tripsync.model.Traveler;
import com.tripsync.util.InvitationTokenUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Integration test for {@link TripInvitationDAO}.
 *
 * <p>Requires MySQL. It creates its own temporary trip and invitations
 * and deletes them at the end, so seeded data is untouched.
 *
 * <p>Run with TRIPSYNC_DB_PASSWORD set; this does not send any email.
 */
public class TripInvitationDAOTest {

    public static void main(String[] args) throws Exception {

        TripInvitationDAO dao = new TripInvitationDAO();
        TripDAO tripDAO = new TripDAO();
        UserDAO userDAO = new UserDAO();

        String tripId = null;
        List<String> invitationIds = new ArrayList<>();

        System.out.println("========== TRIP INVITATION DAO TEST ==========");

        try {
            // ---- setup: a temporary trip whose admin is seeded U001 ----
            TripAdmin admin = (TripAdmin) userDAO.getUserById("U001");
            com.tripsync.model.Trip trip = new com.tripsync.model.Trip(
                    "DAO Invitation Test Trip",
                    "Temporary trip for TripInvitationDAO test",
                    LocalDate.of(2027, 3, 1),
                    LocalDate.of(2027, 3, 5),
                    new BigDecimal("10000.00"),
                    admin);
            check(tripDAO.createTrip(trip), "test trip created");
            tripId = trip.getTripId();
            System.out.println("Test trip id: " + tripId);

            // ---- CREATE + READ ----
            String raw1 = InvitationTokenUtil.generateToken();
            String hash1 = InvitationTokenUtil.hashToken(raw1);
            String id1 = UUID.randomUUID().toString();
            invitationIds.add(id1);

            TripInvitation inv1 = new TripInvitation(
                    id1, tripId, "U001",
                    "create@example.com", null, hash1,
                    LocalDateTime.now().plusDays(7));
            check(dao.createInvitation(inv1), "invitation created");

            TripInvitation loaded = dao.getInvitationById(id1);
            check(loaded != null, "invitation found by id");
            check(loaded.getStatus() == InvitationStatus.PENDING,
                    "initial status is PENDING");
            check(hash1.equals(loaded.getTokenHash()),
                    "token hash persisted");
            check(loaded.getInviteeUserId() == null,
                    "invitee user id null for unregistered email");

            check(dao.getInvitationByTokenHash(hash1) != null,
                    "invitation found by token hash");
            check(dao.getInvitationByTokenHash("0".repeat(64)) == null,
                    "unknown token hash returns null");

            // ---- LISTS ----
            check(dao.hasPendingInvitation(tripId, "create@example.com"),
                    "hasPendingInvitation true");
            check(!dao.hasPendingInvitation(tripId, "nobody@example.com"),
                    "hasPendingInvitation false for other email");
            check(dao.getInvitationsByTripId(tripId).size() == 1,
                    "getInvitationsByTripId");
            check(dao.getPendingInvitationsByTripId(tripId).size() == 1,
                    "getPendingInvitationsByTripId");
            check(dao.getPendingInvitationsByEmail("create@example.com")
                    .size() == 1, "getPendingInvitationsByEmail");
            check(dao.getInvitationsByEmail("create@example.com").size() == 1,
                    "getInvitationsByEmail");

            // ---- duplicate pending prevented (DB unique) ----
            boolean duplicate = false;
            try {
                TripInvitation dup = new TripInvitation(
                        UUID.randomUUID().toString(), tripId, "U001",
                        "create@example.com", null,
                        InvitationTokenUtil.hashToken(InvitationTokenUtil.generateToken()),
                        LocalDateTime.now().plusDays(7));
                dao.createInvitation(dup);
                invitationIds.add(dup.getInvitationId());
            } catch (SQLException e) {
                duplicate = true;
            }
            check(duplicate, "duplicate pending invitation rejected");

            // ---- UPDATE STATUS ----
            check(dao.updateStatus(id1, InvitationStatus.EXPIRED),
                    "updateStatus to EXPIRED");
            check(dao.getInvitationById(id1).getStatus()
                    == InvitationStatus.EXPIRED, "status updated");
            // Free the PENDING slot for this email again.
            check(!dao.hasPendingInvitation(tripId, "create@example.com"),
                    "no pending after expiry");

            // ---- CANCEL ----
            String id2 = UUID.randomUUID().toString();
            invitationIds.add(id2);
            check(dao.createInvitation(new TripInvitation(
                    id2, tripId, "U001", "cancel@example.com", null,
                    InvitationTokenUtil.hashToken(InvitationTokenUtil.generateToken()),
                    LocalDateTime.now().plusDays(7))), "cancel invitation created");
            check(dao.cancelInvitation(id2), "cancelInvitation succeeds");
            check(dao.getInvitationById(id2).getStatus()
                    == InvitationStatus.CANCELLED, "status CANCELLED");
            check(!dao.cancelInvitation(id2),
                    "cancelInvitation again fails (not pending)");

            // ---- MARK EXPIRED ----
            String id3 = UUID.randomUUID().toString();
            invitationIds.add(id3);
            check(dao.createInvitation(new TripInvitation(
                    id3, tripId, "U001", "markexpired@example.com", null,
                    InvitationTokenUtil.hashToken(InvitationTokenUtil.generateToken()),
                    LocalDateTime.now().plusDays(7))), "markexpired invitation created");
            setExpired(id3); // move its timestamps into the past
            int marked = dao.markExpiredInvitations();
            check(marked >= 1, "markExpiredInvitations returned >= 1");
            check(dao.getInvitationById(id3).getStatus()
                    == InvitationStatus.EXPIRED, "markexpired invitation is EXPIRED");

            // ---- ACCEPT (valid) ----
            String raw4 = InvitationTokenUtil.generateToken();
            String hash4 = InvitationTokenUtil.hashToken(raw4);
            String id4 = UUID.randomUUID().toString();
            invitationIds.add(id4);
            check(dao.createInvitation(new TripInvitation(
                    id4, tripId, "U001", "rhea@example.com", "U002",
                    hash4, LocalDateTime.now().plusDays(7))),
                    "accept invitation created");
            TripInvitation accepted = dao.acceptInvitation(hash4, "U002",
                    "rhea@example.com");
            check(accepted.getStatus() == InvitationStatus.ACCEPTED,
                    "accept returns ACCEPTED");
            check(isMember(tripId, "U002"), "U002 added to trip_members");
            check(dao.getInvitationById(id4).getStatus()
                    == InvitationStatus.ACCEPTED, "invitation status ACCEPTED");

            // ---- ACCEPT: already a member ----
            String raw5 = InvitationTokenUtil.generateToken();
            String hash5 = InvitationTokenUtil.hashToken(raw5);
            String id5 = UUID.randomUUID().toString();
            invitationIds.add(id5);
            check(dao.createInvitation(new TripInvitation(
                    id5, tripId, "U001", "rhea@example.com", "U002",
                    hash5, LocalDateTime.now().plusDays(7))),
                    "already-member invitation created");
            try {
                dao.acceptInvitation(hash5, "U002", "rhea@example.com");
                check(false, "accept by existing member should fail");
            } catch (InvitationException e) {
                check(e.getMessage().contains("already a member"),
                        "already-member rejected: " + e.getMessage());
            }
            dao.deleteInvitation(id5);
            invitationIds.remove(id5);

            // ---- ACCEPT: wrong user (email mismatch) ----
            String raw6 = InvitationTokenUtil.generateToken();
            String hash6 = InvitationTokenUtil.hashToken(raw6);
            String id6 = UUID.randomUUID().toString();
            invitationIds.add(id6);
            check(dao.createInvitation(new TripInvitation(
                    id6, tripId, "U001", "rhea@example.com", "U002",
                    hash6, LocalDateTime.now().plusDays(7))),
                    "wrong-user invitation created");
            try {
                dao.acceptInvitation(hash6, "U003", "shreya@example.com");
                check(false, "accept by wrong user should fail");
            } catch (InvitationException e) {
                check(e.getMessage().contains("not sent to your email"),
                        "wrong-user rejected: " + e.getMessage());
            }
            dao.deleteInvitation(id6);
            invitationIds.remove(id6);

            // ---- ACCEPT: invalid token ----
            try {
                dao.acceptInvitation(InvitationTokenUtil.hashToken("nope"),
                        "U002", "rhea@example.com");
                check(false, "accept with invalid token should fail");
            } catch (InvitationException e) {
                check(e.getMessage().contains("Invalid invitation token"),
                        "invalid token rejected: " + e.getMessage());
            }

            // ---- ACCEPT: expired ----
            String raw7 = InvitationTokenUtil.generateToken();
            String hash7 = InvitationTokenUtil.hashToken(raw7);
            String id7 = UUID.randomUUID().toString();
            invitationIds.add(id7);
            check(dao.createInvitation(new TripInvitation(
                    id7, tripId, "U001", "expired@example.com", null,
                    hash7, LocalDateTime.now().plusDays(7))),
                    "expired invitation created");
            setExpired(id7);
            try {
                dao.acceptInvitation(hash7, "U002", "rhea@example.com");
                check(false, "accept of expired invitation should fail");
            } catch (InvitationException e) {
                check(e.getMessage().contains("expired"),
                        "expired accept rejected: " + e.getMessage());
            }
            dao.deleteInvitation(id7);
            invitationIds.remove(id7);

            // ---- REJECT (valid) ----
            String raw8 = InvitationTokenUtil.generateToken();
            String hash8 = InvitationTokenUtil.hashToken(raw8);
            String id8 = UUID.randomUUID().toString();
            invitationIds.add(id8);
            check(dao.createInvitation(new TripInvitation(
                    id8, tripId, "U001", "shreya@example.com", "U003",
                    hash8, LocalDateTime.now().plusDays(7))),
                    "reject invitation created");
            TripInvitation rejected = dao.rejectInvitation(hash8, "U003",
                    "shreya@example.com");
            check(rejected.getStatus() == InvitationStatus.REJECTED,
                    "reject returns REJECTED");

            // ---- REJECT: repeated ----
            try {
                dao.rejectInvitation(hash8, "U003", "shreya@example.com");
                check(false, "repeated reject should fail");
            } catch (InvitationException e) {
                check(e.getMessage().contains("no longer pending"),
                        "repeated reject rejected: " + e.getMessage());
            }
            check(!isMember(tripId, "U003"),
                    "rejecting does not add membership");

            // ---- DELETE ----
            check(dao.deleteInvitation(id8), "invitation deleted");
            check(dao.getInvitationById(id8) == null,
                    "invitation gone after delete");
            invitationIds.remove(id8);

            System.out.println(
                    "\n========== TRIP INVITATION DAO TEST PASSED ==========");

        } catch (Exception e) {
            System.out.println("\nTRIP INVITATION DAO TEST FAILED!");
            e.printStackTrace();
            throw e;
        } finally {
            // Delete the temporary trip; FK ON DELETE CASCADE removes its
            // invitations and trip_members. Belt-and-braces: delete any
            // leftover invitations by id first.
            for (String id : new ArrayList<>(invitationIds)) {
                try {
                    dao.deleteInvitation(id);
                } catch (Exception ignore) {
                    // ignore
                }
            }
            if (tripId != null) {
                try {
                    tripDAO.deleteTrip(tripId);
                } catch (Exception ignore) {
                    // ignore
                }
            }
            System.out.println("TripInvitationDAO cleanup attempted.");
        }
    }

    // ---- helpers ----

    private static boolean isMember(String tripId, String userId)
            throws SQLException {
        for (Traveler m : new TripDAO().getTripMembers(tripId)) {
            if (m.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /** Moves an invitation's created_at/expires_at into the past so it
     *  counts as expired, while keeping expires_at > created_at. */
    private static void setExpired(String invitationId) throws SQLException {
        String sql = "UPDATE trip_invitations SET created_at = ?, expires_at = ? "
                + "WHERE invitation_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
            ps.setString(3, invitationId);
            ps.executeUpdate();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("DAO test failed: " + message);
        }
        System.out.println("  ok - " + message);
    }
}


package com.tripsync.manager;

import com.tripsync.config.DBConnection;
import com.tripsync.config.InvitationConfig;
import com.tripsync.dao.TripDAO;
import com.tripsync.dao.TripInvitationDAO;
import com.tripsync.dao.UserDAO;
import com.tripsync.email.EmailService;
import com.tripsync.email.MockEmailService;
import com.tripsync.email.SentEmail;
import com.tripsync.model.InvitationStatus;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.TripInvitation;
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
 * Integration test for TripInvitationManager.
 *
 * Requires MySQL. Uses MockEmailService so no real Gmail
 * messages are sent. It creates a temporary trip and invitations and
 * deletes them at the end, so seeded data is untouched.
 */
public class TripInvitationManagerTest {

    public static void main(String[] args) throws Exception {

        TripDAO tripDAO = new TripDAO();
        UserDAO userDAO = new UserDAO();
        TripInvitationDAO invitationDAO = new TripInvitationDAO();

        MockEmailService mock = new MockEmailService(true);
        TripInvitationManager manager =
                new TripInvitationManager(mock, new InvitationConfig());

        String tripId = null;
        List<String> invitationIds = new ArrayList<>();

        System.out.println("========== TRIP INVITATION MANAGER TEST ==========");

        try {
            // ---- setup: a temporary trip whose admin is seeded U001 ----
            TripAdmin admin = (TripAdmin) userDAO.getUserById("U001");

            com.tripsync.model.Trip trip = new com.tripsync.model.Trip(
                    "Manager Invitation Test Trip",
                    "Temporary trip for TripInvitationManager test",
                    LocalDate.of(2027, 4, 1),
                    LocalDate.of(2027, 4, 5),
                    new BigDecimal("10000.00"),
                    admin);

            check(tripDAO.createTrip(trip), "test trip created");
            tripId = trip.getTripId();

            System.out.println("Test trip id: " + tripId);

            // ===== SEND INVITATION =====

            // valid
            InvitationResult r = manager.sendInvitation(
                    tripId, "U001", "rhea@example.com");

            check(r.success(), "send valid: " + r.message());
            check(mock.getSentCount() == 1, "mock recorded one email");

            String id1 = r.invitationId();
            invitationIds.add(id1);

            String token1 = extractToken(mock.getSentEmails().get(0));

            TripInvitation persisted = invitationDAO.getInvitationById(id1);

            check(persisted != null
                    && InvitationTokenUtil.hashToken(token1)
                            .equals(persisted.getTokenHash()),
                    "token hash persisted matches extracted token");

            check(persisted.getInviteeUserId().equals("U002"),
                    "invitee linked to registered user U002");

            // invalid trip
            check(!manager.sendInvitation("NONEXISTENT", "U001",
                    "x@example.com").success(), "invalid trip rejected");

            // invalid inviter
            check(!manager.sendInvitation(tripId, "NONEXISTENT",
                    "x@example.com").success(), "invalid inviter rejected");

            // unauthorized inviter
            InvitationResult unauth = manager.sendInvitation(
                    tripId, "U003", "x@example.com");

            check(!unauth.success()
                    && unauth.message().contains("authorized"),
                    "unauthorized inviter rejected: " + unauth.message());

            // invalid email
            InvitationResult badEmail = manager.sendInvitation(
                    tripId, "U001", "not-an-email");

            check(!badEmail.success()
                    && badEmail.message().contains("valid"),
                    "invalid email rejected: " + badEmail.message());

            // ===== ACCEPT INVITATION =====

            // valid accept
            InvitationResult acc = manager.acceptInvitation(token1, "U002");

            check(acc.success() && acc.status() == InvitationStatus.ACCEPTED,
                    "accept valid: " + acc.message());

            check(isMember(tripId, "U002"), "U002 joined the trip");

            // already accepted
            InvitationResult accAgain =
                    manager.acceptInvitation(token1, "U002");

            check(!accAgain.success()
                    && accAgain.message().contains("no longer pending"),
                    "re-accept rejected: " + accAgain.message());

            // wrong token
            InvitationResult accBad =
                    manager.acceptInvitation("bad-token", "U002");

            check(!accBad.success()
                    && accBad.message().contains("Invalid invitation token"),
                    "wrong token rejected: " + accBad.message());

            // ===== REJECT INVITATION =====

            // send a Shreya invitation for reject / wrong-user cases
            InvitationResult shreyaSend = manager.sendInvitation(
                    tripId, "U001", "shreya@example.com");

            check(shreyaSend.success(), "shreya invitation sent");

            String idShreya = shreyaSend.invitationId();
            invitationIds.add(idShreya);

            String tokenShreya = extractTokenLast(mock);

            // wrong user: accept Shreya invitation as U002/Rhea
            InvitationResult accWrong =
                    manager.acceptInvitation(tokenShreya, "U002");

            check(!accWrong.success()
                    && accWrong.message().contains("not sent to your email"),
                    "wrong user rejected: " + accWrong.message());

            // valid reject
            InvitationResult rej =
                    manager.rejectInvitation(tokenShreya, "U003");

            check(rej.success() && rej.status() == InvitationStatus.REJECTED,
                    "reject valid: " + rej.message());

            check(!isMember(tripId, "U003"),
                    "rejecting does not add membership");

            // repeated reject
            InvitationResult rejAgain =
                    manager.rejectInvitation(tokenShreya, "U003");

            check(!rejAgain.success()
                    && rejAgain.message().contains("no longer pending"),
                    "repeated reject rejected: " + rejAgain.message());

            // accept a rejected invitation
            InvitationResult accRej =
                    manager.acceptInvitation(tokenShreya, "U003");

            check(!accRej.success()
                    && accRej.message().contains("no longer pending"),
                    "accept rejected invitation blocked: " + accRej.message());

            // ===== ALREADY A MEMBER (send blocked) =====

            InvitationResult sendMember = manager.sendInvitation(
                    tripId, "U001", "rhea@example.com");

            check(!sendMember.success()
                    && sendMember.message().contains("already a member"),
                    "already-member send rejected: " + sendMember.message());

            // ===== DUPLICATE PENDING =====

            check(manager.sendInvitation(tripId, "U001",
                    "dup@example.com").success(), "first dup invitation sent");

            invitationIds.add(
                    lastInvitationIdByEmail(invitationDAO, tripId, "dup@example.com"));

            InvitationResult dup = manager.sendInvitation(
                    tripId, "U001", "dup@example.com");

            check(!dup.success()
                    && dup.message().contains("pending invitation already exists"),
                    "duplicate pending rejected: " + dup.message());

            // ===== EMAIL FAILURE =====

            EmailService failing = new EmailService() {

                public boolean sendEmail(
                        String to, String subject, String htmlBody) {
                    return false;
                }

                public boolean sendInvitationEmail(
                        String to,
                        String tripName,
                        String inviterName,
                        String details,
                        String acceptUrl,
                        String rejectUrl) {
                    return false;
                }
            };

            TripInvitationManager failManager =
                    new TripInvitationManager(failing, new InvitationConfig());

            InvitationResult emailFail = failManager.sendInvitation(
                    tripId, "U001", "fail@example.com");

            check(!emailFail.success()
                    && emailFail.message().contains("email could not be sent"),
                    "email failure reported, not hidden: " + emailFail.message());

            check(emailFail.invitationId() != null
                    && invitationDAO.getInvitationById(emailFail.invitationId())
                            != null,
                    "invitation persisted despite email failure");

            invitationIds.add(emailFail.invitationId());

            // ===== ACCEPT EXPIRED =====

            String expToken = InvitationTokenUtil.generateToken();
            String expId = UUID.randomUUID().toString();

            invitationIds.add(expId);

            check(invitationDAO.createInvitation(new TripInvitation(
                    expId,
                    tripId,
                    "U001",
                    "expired@example.com",
                    null,
                    InvitationTokenUtil.hashToken(expToken),
                    LocalDateTime.now().plusDays(7))),
                    "expired invitation created");

            setExpired(expId);

            InvitationResult accExp =
                    manager.acceptInvitation(expToken, "U002");

            check(!accExp.success()
                    && accExp.message().contains("expired"),
                    "expired accept rejected: " + accExp.message());

            // ===== CANCEL INVITATION =====

            InvitationResult cancelSend = manager.sendInvitation(
                    tripId, "U001", "cancel@example.com");

            check(cancelSend.success(), "cancel invitation sent");

            String idCancel = cancelSend.invitationId();
            invitationIds.add(idCancel);

            // unauthorized cancel
            InvitationResult cancelUnauth =
                    manager.cancelInvitation(idCancel, "U003");

            check(!cancelUnauth.success()
                    && cancelUnauth.message().contains("authorized"),
                    "unauthorized cancel rejected: " + cancelUnauth.message());

            // authorized cancel
            InvitationResult cancelOk =
                    manager.cancelInvitation(idCancel, "U001");

            check(cancelOk.success()
                    && cancelOk.status() == InvitationStatus.CANCELLED,
                    "authorized cancel: " + cancelOk.message());

            // cancel non-pending: already accepted invitation
            InvitationResult cancelNonPending =
                    manager.cancelInvitation(id1, "U001");

            check(!cancelNonPending.success()
                    && cancelNonPending.message().contains("pending"),
                    "cancel non-pending rejected: "
                            + cancelNonPending.message());

            // ===== RESEND INVITATION =====

            InvitationResult resendSend = manager.sendInvitation(
                    tripId, "U001", "shreya@example.com");

            check(resendSend.success(), "resend-target invitation sent");

            String idResend = resendSend.invitationId();
            invitationIds.add(idResend);

            String oldToken = extractTokenLast(mock);
            int emailsBefore = mock.getSentCount();

            // unauthorized resend
            InvitationResult resendUnauth =
                    manager.resendInvitation(idResend, "U003");

            check(!resendUnauth.success()
                    && resendUnauth.message().contains("authorized"),
                    "unauthorized resend rejected: " + resendUnauth.message());

            // authorized resend
            InvitationResult resendOk =
                    manager.resendInvitation(idResend, "U001");

            check(resendOk.success(),
                    "resend pending: " + resendOk.message());

            check(mock.getSentCount() == emailsBefore + 1,
                    "resend sent a fresh email");

            String newToken = extractTokenLast(mock);

            // old token invalid after resend
            InvitationResult accOld =
                    manager.acceptInvitation(oldToken, "U003");

            check(!accOld.success()
                    && accOld.message().contains("Invalid invitation token"),
                    "old token invalid after resend: " + accOld.message());

            // new token valid after resend
            InvitationResult accNew =
                    manager.acceptInvitation(newToken, "U003");

            check(accNew.success()
                    && accNew.status() == InvitationStatus.ACCEPTED,
                    "new token valid after resend: " + accNew.message());

            check(isMember(tripId, "U003"),
                    "U003 joined after resend+accept");

            // resend expired
            String expResendId = UUID.randomUUID().toString();
            invitationIds.add(expResendId);

            check(invitationDAO.createInvitation(new TripInvitation(
                    expResendId,
                    tripId,
                    "U001",
                    "resend-expired@example.com",
                    null,
                    InvitationTokenUtil.hashToken(
                            InvitationTokenUtil.generateToken()),
                    LocalDateTime.now().plusDays(7))),
                    "resend-expired invitation created");

            invitationDAO.updateStatus(
                    expResendId, InvitationStatus.EXPIRED);

            InvitationResult resendExpired =
                    manager.resendInvitation(expResendId, "U001");

            check(!resendExpired.success()
                    && resendExpired.message().contains("pending"),
                    "resend expired rejected: " + resendExpired.message());

            // ===== LIST / DETAIL (no token hash exposed) =====

            List<InvitationSummary> tripInvitations =
                    manager.listInvitationsForTrip(tripId, "U001");

            check(!tripInvitations.isEmpty(),
                    "list invitations for trip");

            InvitationSummary detail =
                    manager.getInvitationDetails(id1, "U001");

            check(detail != null
                    && detail.invitationId().equals(id1),
                    "invitation details retrieved");

            // unauthorized list: U004 is not a trip member
            boolean listUnauthThrew = false;

            try {
                manager.listInvitationsForTrip(tripId, "U004");
            } catch (IllegalStateException e) {
                listUnauthThrew = e.getMessage().contains("authorized");
            }

            check(listUnauthThrew, "unauthorized list rejected");

            System.out.println(
                    "\n========== TRIP INVITATION MANAGER TEST PASSED ==========");

        } catch (Exception e) {
            System.out.println("\nTRIP INVITATION MANAGER TEST FAILED!");
            e.printStackTrace();
            throw e;

        } finally {
            // Delete the temporary trip; FK ON DELETE CASCADE removes its
            // invitations and trip_members. Also delete stray invitations.

            for (String id : new ArrayList<>(invitationIds)) {
                try {
                    invitationDAO.deleteInvitation(id);
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

            System.out.println("TripInvitationManager cleanup attempted.");
        }
    }

    // ---- helpers ----

    private static boolean isMember(String tripId, String userId)
            throws SQLException {

        for (com.tripsync.model.Traveler m :
                new TripDAO().getTripMembers(tripId)) {

            if (m.getUserId().equals(userId)) {
                return true;
            }
        }

        return false;
    }

    /** Extracts the raw token from the accept link in the last email. */
    private static String extractTokenLast(MockEmailService mock) {

        List<SentEmail> emails = mock.getSentEmails();
        SentEmail last = emails.get(emails.size() - 1);

        return extractToken(last);
    }

    private static String extractToken(SentEmail email) {

        String body = email.htmlBody();
        String marker = "/invitations/";

        int start = body.indexOf(marker) + marker.length();
        int end = body.indexOf("/accept", start);

        return body.substring(start, end);
    }

    private static String lastInvitationIdByEmail(
            TripInvitationDAO dao, String tripId, String email)
            throws SQLException {

        List<TripInvitation> list = dao.getInvitationsByTripId(tripId);

        for (TripInvitation i : list) {
            if (email.equalsIgnoreCase(i.getInviteeEmail())
                    && i.getStatus() == InvitationStatus.PENDING) {
                return i.getInvitationId();
            }
        }

        return null;
    }

    private static void setExpired(String invitationId) throws SQLException {

        String sql = "UPDATE trip_invitations SET created_at = ?, expires_at = ? "
                + "WHERE invitation_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setTimestamp(1,
                    Timestamp.valueOf(LocalDateTime.now().minusHours(2)));

            ps.setTimestamp(2,
                    Timestamp.valueOf(LocalDateTime.now().minusHours(1)));

            ps.setString(3, invitationId);
            ps.executeUpdate();
        }
    }

    private static void check(boolean condition, String message) {

        if (!condition) {
            throw new IllegalStateException(
                    "Manager test failed: " + message);
        }

        System.out.println("  ok - " + message);
    }
}

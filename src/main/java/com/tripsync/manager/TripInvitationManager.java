package com.tripsync.manager;

import com.tripsync.config.InvitationConfig;
import com.tripsync.dao.TripDAO;
import com.tripsync.dao.TripInvitationDAO;
import com.tripsync.dao.UserDAO;
import com.tripsync.email.EmailService;
import com.tripsync.email.GmailEmailService;
import com.tripsync.exceptions.InvitationException;
import com.tripsync.model.InvitationStatus;
import com.tripsync.model.Traveler;
import com.tripsync.model.Trip;
import com.tripsync.model.TripInvitation;
import com.tripsync.model.User;
import com.tripsync.util.InvitationTokenUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Coordinates trip invitation creation, persistence, email delivery
 * and responses.
 *
 * <p>Authorization model: this is a Core Java backend without a real
 * authentication layer in Phase 2. The manager enforces
 * <em>authorization</em> checks (an inviter must be the trip admin or a
 * member; only the invitee whose registered email matches can accept
 * or reject), but it cannot <em>authenticate</em> callers. A userId
 * passed to these methods is trusted only as far as the future HTTP API
 * layer vouches for it. That gap is documented in
 * {@code docs/TRIP_INVITATION_SYSTEM.md} and must be closed before
 * production use.
 */
public class TripInvitationManager {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
            Pattern.CASE_INSENSITIVE);

    private final TripDAO tripDAO;
    private final UserDAO userDAO;
    private final TripInvitationDAO invitationDAO;
    private final EmailService emailService;
    private final InvitationConfig config;

    /** Production constructor: Gmail email service, env-based config. */
    public TripInvitationManager() {
        this(new GmailEmailService(), new InvitationConfig());
    }

    /** Test / DI constructor. */
    public TripInvitationManager(EmailService emailService,
            InvitationConfig config) {

        if (emailService == null) {
            throw new IllegalArgumentException("EmailService is required.");
        }
        if (config == null) {
            throw new IllegalArgumentException("InvitationConfig is required.");
        }

        this.tripDAO = new TripDAO();
        this.userDAO = new UserDAO();
        this.invitationDAO = new TripInvitationDAO();
        this.emailService = emailService;
        this.config = config;
    }

    // ==========================================
    // A. SEND INVITATION
    // ==========================================

    public InvitationResult sendInvitation(String tripId, String inviterId,
            String inviteeEmail) {

        if (isBlank(tripId) || isBlank(inviterId)) {
            return InvitationResult.failure(null, null,
                    "Trip id and inviter id are required.");
        }
        if (!isValidEmail(inviteeEmail)) {
            return InvitationResult.failure(null, null,
                    "A valid invitee email is required.");
        }

        String email = normalizeEmail(inviteeEmail);

        try {
            // 2. Trip exists.
            Trip trip = tripDAO.getTripById(tripId);
            if (trip == null) {
                return InvitationResult.failure(null, null,
                        "Trip not found: " + tripId);
            }

            // 3. Inviter exists.
            User inviter = userDAO.getUserById(inviterId);
            if (inviter == null) {
                return InvitationResult.failure(null, null,
                        "Inviter not found: " + inviterId);
            }

            // 4. Inviter is authorized (trip admin or current member).
            if (!isAuthorized(trip, inviterId)) {
                return InvitationResult.failure(null, null,
                        "Inviter is not authorized to invite members "
                                + "to this trip.");
            }

            // 5/6. Link invitee to a registered user and check membership.
            User invitee = userDAO.getUserByEmail(email);
            String inviteeUserId = (invitee != null)
                    ? invitee.getUserId() : null;

            if (inviteeUserId != null && isMember(trip, inviteeUserId)) {
                return InvitationResult.failure(null, null,
                        "Invitee is already a member of this trip.");
            }

            // 7. No duplicate pending invitation.
            if (invitationDAO.hasPendingInvitation(tripId, email)) {
                return InvitationResult.failure(null, null,
                        "A pending invitation already exists for this "
                                + "trip and email.");
            }

            // 8-10. Generate token + hash + invitation with expiry.
            String rawToken = InvitationTokenUtil.generateToken();
            String tokenHash = InvitationTokenUtil.hashToken(rawToken);
            String invitationId = UUID.randomUUID().toString();
            LocalDateTime expiresAt =
                    LocalDateTime.now().plusDays(config.getExpiryDays());

            TripInvitation invitation = new TripInvitation(
                    invitationId, tripId, inviterId, email,
                    inviteeUserId, tokenHash, expiresAt);

            // 11. Persist.
            boolean created = invitationDAO.createInvitation(invitation);
            if (!created) {
                return InvitationResult.failure(null, null,
                        "Invitation could not be created.");
            }

            // 12-13. Build links and send email. The raw token is used
            // only here and never logged.
            String acceptUrl = config.getFrontendBaseUrl()
                    + "/invitations/" + rawToken + "/accept";
            String rejectUrl = config.getFrontendBaseUrl()
                    + "/invitations/" + rawToken + "/reject";
            String details = "This invitation expires on "
                    + expiresAt.toLocalDate()
                    + ". Please respond before then.";

            boolean emailSent = emailService.sendInvitationEmail(
                    email, trip.getTripName(), inviter.getName(),
                    details, acceptUrl, rejectUrl);

            // 14. Result. The invitation stays PENDING and retryable if
            // email delivery fails; it is not deleted, and resend reuses
            // the same record instead of creating a new pending one.
            if (emailSent) {
                return InvitationResult.success(invitationId,
                        InvitationStatus.PENDING,
                        "Invitation sent to " + email + ".");
            }
            return InvitationResult.failure(invitationId,
                    InvitationStatus.PENDING,
                    "Invitation created, but the email could not be sent. "
                            + "Use resendInvitation to retry.");

        } catch (SQLException e) {
            return InvitationResult.failure(null, null,
                    "Database error: " + e.getMessage());
        }
    }

    // ==========================================
    // B. ACCEPT INVITATION
    // ==========================================

    public InvitationResult acceptInvitation(String token, String userId) {

        if (isBlank(token) || isBlank(userId)) {
            return InvitationResult.failure(null, null,
                    "Token and user id are required.");
        }

        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                return InvitationResult.failure(null, null,
                        "User not found.");
            }
            if (!(user instanceof Traveler)) {
                return InvitationResult.failure(null, null,
                        "Only travelers can join a trip.");
            }

            String tokenHash = InvitationTokenUtil.hashToken(token.trim());

            TripInvitation updated = invitationDAO.acceptInvitation(
                    tokenHash, userId, user.getEmail());

            return InvitationResult.success(updated.getInvitationId(),
                    updated.getStatus(),
                    "Invitation accepted. You have joined the trip.");

        } catch (InvitationException e) {
            return InvitationResult.failure(e.getInvitationId(),
                    e.getStatus(), e.getMessage());
        } catch (SQLException e) {
            return InvitationResult.failure(null, null,
                    "Database error: " + e.getMessage());
        }
    }

    // ==========================================
    // C. REJECT INVITATION
    // ==========================================

    public InvitationResult rejectInvitation(String token, String userId) {

        if (isBlank(token) || isBlank(userId)) {
            return InvitationResult.failure(null, null,
                    "Token and user id are required.");
        }

        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                return InvitationResult.failure(null, null,
                        "User not found.");
            }

            String tokenHash = InvitationTokenUtil.hashToken(token.trim());

            TripInvitation updated = invitationDAO.rejectInvitation(
                    tokenHash, userId, user.getEmail());

            return InvitationResult.success(updated.getInvitationId(),
                    updated.getStatus(),
                    "Invitation rejected.");

        } catch (InvitationException e) {
            return InvitationResult.failure(e.getInvitationId(),
                    e.getStatus(), e.getMessage());
        } catch (SQLException e) {
            return InvitationResult.failure(null, null,
                    "Database error: " + e.getMessage());
        }
    }

    // ==========================================
    // D. CANCEL INVITATION
    // ==========================================

    public InvitationResult cancelInvitation(String invitationId,
            String requesterId) {

        if (isBlank(invitationId) || isBlank(requesterId)) {
            return InvitationResult.failure(null, null,
                    "Invitation id and requester id are required.");
        }

        try {
            TripInvitation invitation =
                    invitationDAO.getInvitationById(invitationId);
            if (invitation == null) {
                return InvitationResult.failure(null, null,
                        "Invitation not found.");
            }

            if (!isAuthorizedToModify(invitation, requesterId)) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Not authorized to cancel this invitation.");
            }

            if (invitation.getStatus() != InvitationStatus.PENDING) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Only pending invitations can be cancelled.");
            }

            boolean cancelled = invitationDAO.cancelInvitation(invitationId);
            if (!cancelled) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Invitation could not be cancelled "
                                + "(it may no longer be pending).");
            }

            return InvitationResult.success(invitationId,
                    InvitationStatus.CANCELLED, "Invitation cancelled.");

        } catch (SQLException e) {
            return InvitationResult.failure(null, null,
                    "Database error: " + e.getMessage());
        }
    }

    // ==========================================
    // E. RESEND INVITATION
    // ==========================================

    public InvitationResult resendInvitation(String invitationId,
            String requesterId) {

        if (isBlank(invitationId) || isBlank(requesterId)) {
            return InvitationResult.failure(null, null,
                    "Invitation id and requester id are required.");
        }

        try {
            TripInvitation invitation =
                    invitationDAO.getInvitationById(invitationId);
            if (invitation == null) {
                return InvitationResult.failure(null, null,
                        "Invitation not found.");
            }

            if (!isAuthorizedToModify(invitation, requesterId)) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Not authorized to resend this invitation.");
            }

            if (invitation.getStatus() != InvitationStatus.PENDING) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Only pending invitations can be resent.");
            }

            if (invitation.getResendCount() >= config.getMaxResends()) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Resend limit reached for this invitation.");
            }

            // New secure token; old token becomes invalid.
            String rawToken = InvitationTokenUtil.generateToken();
            String tokenHash = InvitationTokenUtil.hashToken(rawToken);
            LocalDateTime expiresAt =
                    LocalDateTime.now().plusDays(config.getExpiryDays());

            boolean updated = invitationDAO.updateForResend(
                    invitationId, tokenHash, expiresAt, config.getMaxResends());
            if (!updated) {
                return InvitationResult.failure(invitation.getInvitationId(),
                        invitation.getStatus(),
                        "Invitation could not be updated for resend "
                                + "(it may no longer be pending).");
            }

            String acceptUrl = config.getFrontendBaseUrl()
                    + "/invitations/" + rawToken + "/accept";
            String rejectUrl = config.getFrontendBaseUrl()
                    + "/invitations/" + rawToken + "/reject";
            String details = "This invitation expires on "
                    + expiresAt.toLocalDate()
                    + ". Please respond before then.";

            User inviter = userDAO.getUserById(invitation.getInviterId());
            String inviterName = (inviter != null) ? inviter.getName()
                    : "Someone";

            Trip trip = tripDAO.getTripById(invitation.getTripId());
            String tripName = (trip != null) ? trip.getTripName()
                    : "a trip";

            boolean emailSent = emailService.sendInvitationEmail(
                    invitation.getInviteeEmail(), tripName, inviterName,
                    details, acceptUrl, rejectUrl);

            if (emailSent) {
                return InvitationResult.success(invitationId,
                        InvitationStatus.PENDING,
                        "Invitation resent to "
                                + invitation.getInviteeEmail() + ".");
            }
            return InvitationResult.failure(invitationId,
                    InvitationStatus.PENDING,
                    "Invitation updated, but the email could not be sent.");

        } catch (SQLException e) {
            return InvitationResult.failure(null, null,
                    "Database error: " + e.getMessage());
        }
    }

    // ==========================================
    // F. LIST / DETAIL (never expose token hash)
    // ==========================================

    public List<InvitationSummary> listInvitationsForTrip(String tripId,
            String requesterId) {

        if (isBlank(tripId) || isBlank(requesterId)) {
            throw new IllegalArgumentException(
                    "Trip id and requester id are required.");
        }

        try {
            Trip trip = tripDAO.getTripById(tripId);
            if (trip == null) {
                throw new IllegalArgumentException("Trip not found.");
            }
            if (!isAuthorized(trip, requesterId)) {
                throw new IllegalStateException(
                        "Not authorized to view invitations for this trip.");
            }

            return invitationDAO.getInvitationsByTripId(tripId).stream()
                    .map(this::toSummary)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database error: " + e.getMessage(), e);
        }
    }

    public List<InvitationSummary> listPendingInvitationsForEmail(
            String email) {

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "A valid email is required.");
        }

        try {
            return invitationDAO
                    .getPendingInvitationsByEmail(normalizeEmail(email))
                    .stream()
                    .map(this::toSummary)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database error: " + e.getMessage(), e);
        }
    }

    public InvitationSummary getInvitationDetails(String invitationId,
            String requesterId) {

        if (isBlank(invitationId) || isBlank(requesterId)) {
            throw new IllegalArgumentException(
                    "Invitation id and requester id are required.");
        }

        try {
            TripInvitation invitation =
                    invitationDAO.getInvitationById(invitationId);
            if (invitation == null) {
                throw new IllegalArgumentException("Invitation not found.");
            }

            if (!isAuthorizedToModify(invitation, requesterId)) {
                throw new IllegalStateException(
                        "Not authorized to view this invitation.");
            }

            return toSummary(invitation);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database error: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // MAINTENANCE: MARK EXPIRED
    // ==========================================

    public int expireDueInvitations() {
        try {
            return invitationDAO.markExpiredInvitations();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database error: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private InvitationSummary toSummary(TripInvitation invitation) {
        return new InvitationSummary(
                invitation.getInvitationId(),
                invitation.getTripId(),
                invitation.getInviterId(),
                invitation.getInviteeEmail(),
                invitation.getInviteeUserId(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getRespondedAt(),
                invitation.getResendCount());
    }

    /** Inviter is authorized if they are the trip admin or a member. */
    private boolean isAuthorized(Trip trip, String userId) {
        if (trip.getAdmin() != null
                && trip.getAdmin().getUserId().equals(userId)) {
            return true;
        }
        for (Traveler member : trip.getMembers()) {
            if (member.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMember(Trip trip, String userId) {
        for (Traveler member : trip.getMembers()) {
            if (member.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /** Only the inviter or the trip admin may cancel / resend / view. */
    private boolean isAuthorizedToModify(TripInvitation invitation,
            String requesterId) {

        if (invitation.getInviterId().equals(requesterId)) {
            return true;
        }
        try {
            Trip trip = tripDAO.getTripById(invitation.getTripId());
            return trip != null && trip.getAdmin() != null
                    && trip.getAdmin().getUserId().equals(requesterId);
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}

package com.tripsync.email;

/**
 * Reusable HTML template for trip invitation emails.
 *
 * <p>All caller-provided text is HTML-escaped, and accept/reject
 * links are only rendered when they use an {@code http} or
 * {@code https} URL, so the template is safe against basic HTML and
 * {@code javascript:} injection.
 */
public final class InvitationEmailTemplate {

    private InvitationEmailTemplate() {
        // Utility class; no instances.
    }

    /** Builds a plain-text subject line for an invitation. */
    public static String buildSubject(String tripName) {
        String name = (tripName == null || tripName.isBlank())
                ? "a trip" : tripName;
        return "You're invited to join \"" + name + "\" on TripSync";
    }

    /** Builds the full HTML body for an invitation. */
    public static String render(String tripName, String inviterName,
            String details, String acceptUrl, String rejectUrl) {

        String trip = (tripName == null || tripName.isBlank())
                ? "this trip" : escape(tripName);

        String inviter = (inviterName == null || inviterName.isBlank())
                ? "Someone" : escape(inviterName);

        String detailsRow = "";
        if (details != null && !details.isBlank()) {
            detailsRow =
                "<tr><td style=\"padding:0 0 16px;color:#4b5563;"
                + "font-size:14px;line-height:1.6;\">"
                + escape(details)
                + "</td></tr>";
        }

        String acceptButton = button(acceptUrl, "Accept", "#16a34a");
        String rejectButton = button(rejectUrl, "Decline", "#6b7280");

        String separator =
                (!acceptButton.isEmpty() && !rejectButton.isEmpty())
                ? "<td style=\"width:12px;\"></td>"
                : "";

        String buttonsRow = "";
        if (!acceptButton.isEmpty() || !rejectButton.isEmpty()) {
            buttonsRow =
                "<tr><td style=\"padding:8px 0 24px;\">"
                + "<table role=\"presentation\" cellspacing=\"0\" "
                + "cellpadding=\"0\" border=\"0\"><tr>"
                + acceptButton + separator + rejectButton
                + "</tr></table></td></tr>";
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>TripSync Invitation</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f3f4f6;padding:24px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="560" cellspacing="0" cellpadding="0" border="0" style="background-color:#ffffff;border-radius:12px;max-width:560px;width:100%;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
                        <tr><td style="padding:28px 32px;background-color:#111827;">
                          <span style="font-size:20px;font-weight:700;color:#ffffff;letter-spacing:0.5px;">TripSync</span>
                        </td></tr>
                        <tr><td style="padding:32px;">
                          <h1 style="margin:0 0 12px;font-size:22px;color:#111827;font-weight:700;">You're invited!</h1>
                          <p style="margin:0 0 20px;color:#374151;font-size:15px;line-height:1.6;">
                            <strong>__INVITER__</strong> has invited you to join <strong>__TRIP__</strong> on TripSync.
                          </p>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                            __DETAILS__
                            __BUTTONS__
                          </table>
                          <p style="margin:24px 0 0;color:#6b7280;font-size:13px;line-height:1.6;">
                            If the buttons above don't work, copy and paste the link into your browser. This invitation will expire, so please respond soon.
                          </p>
                        </td></tr>
                        <tr><td style="padding:20px 32px;background-color:#f9fafb;border-top:1px solid #f3f4f6;">
                          <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.6;">
                            You received this email because someone invited you to collaborate on a trip using TripSync. If you don't recognise this invitation, you can safely ignore it.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """
                .replace("__INVITER__", inviter)
                .replace("__TRIP__", trip)
                .replace("__DETAILS__", detailsRow)
                .replace("__BUTTONS__", buttonsRow);
    }

    private static String button(String url, String label,
            String color) {
        if (url == null || url.isBlank() || !isSafeUrl(url)) {
            return "";
        }
        return "<td align=\"center\" bgcolor=\"" + color
                + "\" style=\"border-radius:8px;\">"
                + "<a href=\"" + escape(url) + "\" "
                + "style=\"display:inline-block;padding:12px 24px;"
                + "font-family:inherit;font-size:14px;font-weight:600;"
                + "color:#ffffff;text-decoration:none;border-radius:8px;\">"
                + escape(label) + "</a></td>";
    }

    private static boolean isSafeUrl(String url) {
        String lower = url.trim().toLowerCase();
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

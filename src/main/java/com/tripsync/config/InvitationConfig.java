package com.tripsync.config;

/**
 * Invitation configuration loaded from environment variables, with
 * sensible development defaults. No secrets are involved.
 *
 * <ul>
 *   <li>{@code TRIPSYNC_FRONTEND_URL} - base URL of the React frontend
 *       used to build accept/reject links. Defaults to the local Vite
 *       dev server. Production must override this.</li>
 *   <li>{@code TRIPSYNC_INVITATION_EXPIRY_DAYS} - number of days before
 *       a pending invitation expires. Defaults to 7.</li>
 * </ul>
 */
public class InvitationConfig {

    private static final int DEFAULT_EXPIRY_DAYS = 7;

    // Development default only. Production must set TRIPSYNC_FRONTEND_URL.
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:5173";

    private static final int MAX_RESENDS = 5;

    private final String frontendBaseUrl;
    private final int expiryDays;

    public InvitationConfig() {
        this.frontendBaseUrl =
                resolveFrontendUrl(System.getenv("TRIPSYNC_FRONTEND_URL"));
        this.expiryDays =
                resolveExpiryDays(System.getenv("TRIPSYNC_INVITATION_EXPIRY_DAYS"));
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public int getMaxResends() {
        return MAX_RESENDS;
    }

    private static String resolveFrontendUrl(String env) {
        String url = (env == null || env.isBlank())
                ? DEFAULT_FRONTEND_URL : env.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static int resolveExpiryDays(String env) {
        if (env == null || env.isBlank()) {
            return DEFAULT_EXPIRY_DAYS;
        }
        try {
            int days = Integer.parseInt(env.trim());
            return days >= 1 ? days : DEFAULT_EXPIRY_DAYS;
        } catch (NumberFormatException e) {
            return DEFAULT_EXPIRY_DAYS;
        }
    }
}

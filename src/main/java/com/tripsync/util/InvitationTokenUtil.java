package com.tripsync.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates and hashes invitation tokens.
 *
 * <p>Raw tokens are cryptographically random and URL-safe. Only the
 * SHA-256 hash of a token is ever stored. This class never logs raw
 * tokens; callers are responsible for not logging them either.
 */
public final class InvitationTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 256 bits of randomness per token. */
    private static final int TOKEN_BYTES = 32;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private InvitationTokenUtil() {
        // Utility class; no instances.
    }

    /**
     * Generates a cryptographically secure, URL-safe, unpadded token.
     *
     * @return a Base64-URL string (no padding) of 32 random bytes
     */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a raw token with SHA-256 and returns the hex encoding.
     *
     * @param token the raw token (must not be null/blank)
     * @return a 64-character lowercase hex string
     */
    public static String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    /**
     * Constant-time comparison of a raw supplied token against a known
     * stored hash. Both sides are hashed before comparison so the
     * comparison is between two hex strings of equal length.
     */
    public static boolean hashEquals(String knownHash, String suppliedToken) {
        if (knownHash == null || knownHash.isBlank()
                || suppliedToken == null || suppliedToken.isBlank()) {
            return false;
        }
        String suppliedHash = hashToken(suppliedToken);
        return MessageDigest.isEqual(
                knownHash.getBytes(StandardCharsets.UTF_8),
                suppliedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(out);
    }
}

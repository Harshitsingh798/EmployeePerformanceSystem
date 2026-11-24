package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing and verification.
 *
 * <p><b>Current scheme - PBKDF2-HMAC-SHA256.</b> Each password gets a random
 * 16-byte salt and is stretched with a high iteration count, so two users with
 * the same password produce different hashes and a stolen hash cannot be
 * reversed cheaply by brute force.
 *
 * <p>The result is stored in a single {@code VARCHAR} column using a
 * self-describing format, so no schema change is needed:
 *
 * <pre>{@code pbkdf2$<iterations>$<saltBase64>$<hashBase64>}</pre>
 *
 * <p><b>Legacy support.</b> Accounts created before this scheme (plain text or
 * a bare SHA-256 digest) are still verified, so nobody gets locked out. The
 * caller should then re-hash the password with {@link #hash(String)} and
 * persist the new value - see {@link #needsRehash(String)} and
 * {@code UserDAO.authenticate}.
 */
public final class PasswordUtil {

    /** JCA algorithm name for the key derivation function. */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Marker that identifies the current, self-describing hash format. */
    private static final String PREFIX = "pbkdf2";

    /** Iteration count (OWASP recommends 210,000 for PBKDF2-HMAC-SHA256). */
    private static final int ITERATIONS = 210_000;

    /** Derived key length in bits. */
    private static final int KEY_LENGTH_BITS = 256;

    /** Salt length in bytes. */
    private static final int SALT_LENGTH_BYTES = 16;

    /** Length of a raw SHA-256 digest rendered as lowercase hex. */
    private static final int LEGACY_SHA256_HEX_LENGTH = 64;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Utility class - not instantiable.
    }

    /**
     * Hashes a raw password with a freshly generated random salt.
     *
     * @param raw the plain-text password; must not be {@code null}
     * @return the encoded hash in {@code pbkdf2$iterations$salt$hash} form
     * @throws IllegalArgumentException if {@code raw} is {@code null}
     */
    public static String hash(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(raw.toCharArray(), salt, ITERATIONS);
        return String.join("$",
                PREFIX,
                String.valueOf(ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(derived));
    }

    /**
     * Verifies a raw password against a stored hash, transparently supporting
     * the legacy plain-text and SHA-256 formats.
     *
     * @param raw    the plain-text password to check
     * @param stored the value persisted in the {@code users.password} column
     * @return {@code true} when the password matches
     */
    public static boolean verify(String raw, String stored) {
        if (raw == null || stored == null || stored.isEmpty()) {
            return false;
        }
        if (stored.startsWith(PREFIX + "$")) {
            return verifyPbkdf2(raw, stored);
        }
        if (stored.length() == LEGACY_SHA256_HEX_LENGTH && isHex(stored)) {
            return constantTimeEquals(sha256Hex(raw), stored);
        }
        // Legacy plain-text row - still accepted so old accounts keep working.
        return constantTimeEquals(raw, stored);
    }

    /**
     * Indicates whether a stored hash should be replaced with a stronger one.
     * Used to upgrade accounts silently the first time they log in.
     *
     * @param stored the persisted hash
     * @return {@code true} when the value is not in the current format or uses
     *         an outdated iteration count
     */
    public static boolean needsRehash(String stored) {
        if (stored == null || !stored.startsWith(PREFIX + "$")) {
            return true;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Derives the key material for a password using PBKDF2-HMAC-SHA256.
     */
    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 is not available on this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifies a password against a {@code pbkdf2$...} encoded hash.
     */
    private static boolean verifyPbkdf2(String raw, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(raw.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // Malformed base64 or iteration count - treat as a failed match.
            return false;
        }
    }

    /**
     * Legacy SHA-256 helper retained only for verifying old rows.
     */
    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    /**
     * Length-independent, timing-safe string comparison.
     */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * @return {@code true} when every character is a hexadecimal digit
     */
    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}

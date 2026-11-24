package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PasswordUtil}.
 *
 * <p>These are the tests that matter most for security: they pin down that a
 * hash verifies only the right password, that the salt is random, and that the
 * legacy formats still work so nobody gets locked out.
 */
@DisplayName("PasswordUtil")
class PasswordUtilTest {

    @Test
    @DisplayName("a password verifies against its own hash")
    void passwordVerifiesAgainstItsHash() {
        String hash = PasswordUtil.hash("manager123");
        assertTrue(PasswordUtil.verify("manager123", hash));
    }

    @Test
    @DisplayName("a wrong password does not verify")
    void wrongPasswordDoesNotVerify() {
        String hash = PasswordUtil.hash("manager123");
        assertFalse(PasswordUtil.verify("manager124", hash));
        assertFalse(PasswordUtil.verify("", hash));
    }

    @Test
    @DisplayName("the hash is stored in the self-describing pbkdf2 format")
    void hashUsesTheSelfDescribingFormat() {
        String hash = PasswordUtil.hash("manager123");
        String[] parts = hash.split("\\$");
        assertEquals(4, parts.length, "Expected pbkdf2$iterations$salt$hash");
        assertEquals("pbkdf2", parts[0]);
        assertTrue(Integer.parseInt(parts[1]) >= 210_000,
                "The iteration count should meet the OWASP recommendation");
    }

    @Test
    @DisplayName("the same password hashes differently every time")
    void saltIsRandom() {
        assertNotEquals(PasswordUtil.hash("same"), PasswordUtil.hash("same"));
    }

    @Test
    @DisplayName("a null password is rejected")
    void nullPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }

    @Test
    @DisplayName("a null or empty stored value never verifies")
    void nullStoredValueNeverVerifies() {
        assertFalse(PasswordUtil.verify("password", null));
        assertFalse(PasswordUtil.verify("password", ""));
        assertFalse(PasswordUtil.verify(null, "hash"));
    }

    @Test
    @DisplayName("a legacy SHA-256 hash still verifies")
    void legacySha256StillVerifies() {
        // SHA-256 of "password".
        String legacy = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        assertTrue(PasswordUtil.verify("password", legacy));
        assertFalse(PasswordUtil.verify("not-password", legacy));
    }

    @Test
    @DisplayName("a legacy plain-text password still verifies")
    void legacyPlainTextStillVerifies() {
        assertTrue(PasswordUtil.verify("manager123", "manager123"));
        assertFalse(PasswordUtil.verify("wrong", "manager123"));
    }

    @Test
    @DisplayName("a legacy hash is flagged for an upgrade")
    void legacyHashIsFlaggedForUpgrade() {
        assertTrue(PasswordUtil.needsRehash("manager123"));
        assertTrue(PasswordUtil.needsRehash(
                "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"));
        assertTrue(PasswordUtil.needsRehash(null));
    }

    @Test
    @DisplayName("a current hash is not flagged for an upgrade")
    void currentHashIsNotFlagged() {
        assertFalse(PasswordUtil.needsRehash(PasswordUtil.hash("manager123")));
    }

    @Test
    @DisplayName("a hash with too few iterations is flagged for an upgrade")
    void weakIterationCountIsFlagged() {
        assertTrue(PasswordUtil.needsRehash("pbkdf2$1000$c2FsdA==$aGFzaA=="));
    }

    @Test
    @DisplayName("a malformed hash never verifies and never throws")
    void malformedHashIsHandled() {
        assertFalse(PasswordUtil.verify("password", "pbkdf2$notanumber$!!!$???"));
        assertFalse(PasswordUtil.verify("password", "pbkdf2$210000$only-three-parts"));
        assertFalse(PasswordUtil.verify("password", "pbkdf2$210000$not base64$not base64"));
    }
}

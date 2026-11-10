package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordUtil {

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static boolean verify(String raw, String stored) {
        if (stored == null) return false;
        if (stored.length() == 64) return hash(raw).equalsIgnoreCase(stored);
        return raw.equals(stored); // legacy plain-text rows still work
    }
}

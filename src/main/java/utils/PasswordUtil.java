package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil - Hashing dan verifikasi password menggunakan SHA-256 + Salt
 * Tidak membutuhkan library eksternal.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Hash password dengan salt acak.
     * Format tersimpan: "salt:hash" (kedua-duanya Base64)
     */
    public static String hashPassword(String plainPassword) {
        try {
            // Generate salt 16 bytes
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            String hash = computeHash(plainPassword, saltB64);
            return saltB64 + ":" + hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 tidak tersedia", e);
        }
    }

    /**
     * Verifikasi password plaintext terhadap hash yang tersimpan.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) return false;
        try {
            String[] parts = storedHash.split(":", 2);
            String salt  = parts[0];
            String expected = parts[1];
            String actual   = computeHash(plainPassword, salt);
            return expected.equals(actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static String computeHash(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        md.update(salt.getBytes());
        byte[] hashed = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashed);
    }
}

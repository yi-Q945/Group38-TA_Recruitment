package com.recruitassist.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return PREFIX + '$' + ITERATIONS + '$'
                + Base64.getEncoder().encodeToString(salt) + '$'
                + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean verify(String password, String storedPassword) {
        if (password == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (!isHashed(storedPassword)) {
            return constantTimeEquals(password, storedPassword);
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, iterations);
            return constantTimeEquals(actual, expected);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(PREFIX + '$');
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Password hashing is unavailable.", ex);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return constantTimeEquals(left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(byte[] left, byte[] right) {
        int diff = left.length ^ right.length;
        int limit = Math.min(left.length, right.length);
        for (int index = 0; index < limit; index++) {
            diff |= left[index] ^ right[index];
        }
        return diff == 0;
    }
}

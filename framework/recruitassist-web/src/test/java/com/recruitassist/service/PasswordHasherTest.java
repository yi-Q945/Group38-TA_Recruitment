package com.recruitassist.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    @DisplayName("PBKDF2 hashes should verify only the original password")
    void hashedPasswordVerifies() {
        String hash = PasswordHasher.hash("demo123");

        assertTrue(PasswordHasher.isHashed(hash));
        assertNotEquals("demo123", hash);
        assertTrue(PasswordHasher.verify("demo123", hash));
        assertFalse(PasswordHasher.verify("wrongpass", hash));
    }

    @Test
    @DisplayName("Legacy plaintext demo passwords remain compatible")
    void legacyPlaintextStillVerifies() {
        assertTrue(PasswordHasher.verify("demo123", "demo123"));
        assertFalse(PasswordHasher.verify("wrongpass", "demo123"));
    }
}

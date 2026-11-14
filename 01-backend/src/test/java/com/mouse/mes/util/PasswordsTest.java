package com.mouse.mes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordsTest {
    @Test
    void hashesWithIndependentSaltsAndVerifiesOnlyTheCorrectPassword() {
        String raw = "example-password";
        String first = Passwords.encode(raw);
        String second = Passwords.encode(raw);

        assertTrue(Passwords.isEncoded(first));
        assertNotEquals(first, second);
        assertTrue(Passwords.matches(raw, first));
        assertFalse(Passwords.matches("wrong-password", first));
    }

    @Test
    void acceptsExistingPlaintextButRejectsClearedSnapshotPasswords() {
        assertTrue(Passwords.matches("legacy-password", "legacy-password"));
        assertFalse(Passwords.matches("", ""));
        assertFalse(Passwords.matches(null, null));
        assertFalse(Passwords.matches("legacy-password", ""));
    }

    @Test
    void rejectsInputsAboveBcryptByteLimitRatherThanTruncating() {
        assertThrows(IllegalArgumentException.class, () -> Passwords.encode("密".repeat(25)));
        assertFalse(Passwords.matches("a".repeat(73), Passwords.encode("a".repeat(72))));
    }
}

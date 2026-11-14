package com.mouse.mes.service;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

class FaceAuthenticationPrimitivesTest {
    @Test
    void rejectsMissingWeakAndAmbiguousMatches() {
        var matcher = new FaceMatcher(new float[]{1, 0});
        assertTrue(matcher.result(0.8, 0.1).isEmpty());

        matcher.consider(1, FaceMatcher.encode(new float[]{0, 1}));
        assertTrue(matcher.result(0.8, 0.1).isEmpty());

        matcher.consider(2, FaceMatcher.encode(new float[]{1, 0}));
        assertEquals(2L, matcher.result(0.8, 0.1).orElseThrow());

        matcher.consider(3, FaceMatcher.encode(new float[]{1, 0.05f}));
        assertTrue(matcher.result(0.8, 0.1).isEmpty());
    }

    @Test
    void templateIsBoundToAccountAndModel() throws Exception {
        var cipher = new FaceTemplateCipher(new byte[32]);
        var template = FaceMatcher.encode(new float[]{1, 0});
        var encrypted = cipher.encrypt(7, "sface", template);
        assertArrayEquals(template, cipher.decrypt(7, "sface", encrypted));
        assertThrows(GeneralSecurityException.class, () -> cipher.decrypt(8, "sface", encrypted));
        assertThrows(GeneralSecurityException.class, () -> cipher.decrypt(7, "other", encrypted));
    }
}

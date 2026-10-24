package com.mouse.mes.unit;

import com.mouse.mes.util.MD5Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** MD5 compatibility and input-boundary regression tests. */
class MD5UtilUnitTest {
    @Test void emptyInput() { assertEquals("d41d8cd98f00b204e9800998ecf8427e", MD5Util.encrypt("")); }
    @Test void asciiInput() { assertEquals("900150983cd24fb0d6963f7d28e17f72", MD5Util.encrypt("abc")); }
    @Test void chineseInput() { assertEquals("a7bac2239fcdcb3a067903d8077c4a07", MD5Util.encrypt("中文")); }
    @Test void digestProviderFailureIsExplicit() throws Exception {
        try (org.mockito.MockedStatic<java.security.MessageDigest> digest = org.mockito.Mockito.mockStatic(java.security.MessageDigest.class)) {
            digest.when(() -> java.security.MessageDigest.getInstance("MD5"))
                    .thenThrow(new java.security.NoSuchAlgorithmException("MD5 unavailable"));
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> MD5Util.encrypt("abc"));
            assertInstanceOf(java.security.NoSuchAlgorithmException.class, error.getCause());
        }
    }
    @Test void nullInput() { assertThrows(IllegalArgumentException.class, () -> MD5Util.encrypt(null)); }
    @Test void once() { assertEquals(MD5Util.encrypt("abc"), MD5Util.encrypt("abc", 1)); }
    @Test void twice() { assertEquals(MD5Util.encrypt(MD5Util.encrypt("abc")), MD5Util.encrypt("abc", 2)); }
    @Test void threeTimes() { assertEquals(MD5Util.encrypt(MD5Util.encrypt(MD5Util.encrypt("abc"))), MD5Util.encrypt("abc", 3)); }
    @ParameterizedTest @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void invalidIterations(int times) { assertThrows(IllegalArgumentException.class, () -> MD5Util.encrypt("abc", times)); }
}

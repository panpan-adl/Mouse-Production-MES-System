package com.mouse.mes.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

public final class Passwords {
    private static final String PREFIX = "{bcrypt}";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private Passwords() {}

    public static String encode(String password) {
        if (password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密码不能为空且不能超过72个UTF-8字节");
        }
        return PREFIX + ENCODER.encode(password);
    }

    public static boolean isEncoded(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static boolean matches(String input, String stored) {
        if (input == null || stored == null || input.isBlank() || stored.isBlank()) {
            return false;
        }
        if (isEncoded(stored)) {
            return input.getBytes(StandardCharsets.UTF_8).length <= 72
                    && ENCODER.matches(input, stored.substring(PREFIX.length()));
        }
        return input.equals(stored);
    }
}

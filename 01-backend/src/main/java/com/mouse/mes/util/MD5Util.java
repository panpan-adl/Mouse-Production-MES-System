package com.mouse.mes.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Produces UTF-8 MD5 hashes for compatibility with existing stored values.
 * New password storage should use a password-specific hashing algorithm.
 * @author 第26组
 */
public final class MD5Util {

    private static final int HEX_MASK = 0xff;
    private static final int HEX_RADIX = 16;

    private MD5Util() {
    }

    /** Returns the lower-case MD5 digest of a non-null UTF-8 string. */
    public static String encrypt(String str) {
        if (str == null) {
            throw new IllegalArgumentException("加密内容不能为空");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                int unsigned = value & HEX_MASK;
                if (unsigned < HEX_RADIX) {
                    result.append('0');
                }
                result.append(Integer.toHexString(unsigned));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持MD5", exception);
        }
    }

    /** Applies the digest a positive number of times. */
    public static String encrypt(String str, int times) {
        if (times <= 0) {
            throw new IllegalArgumentException("加密次数必须大于0");
        }
        String result = encrypt(str);
        for (int iteration = 1; iteration < times; iteration++) {
            result = encrypt(result);
        }
        return result;
    }
}

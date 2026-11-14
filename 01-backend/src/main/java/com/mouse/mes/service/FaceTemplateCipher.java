package com.mouse.mes.service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

final class FaceTemplateCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec key;

    FaceTemplateCipher(byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("Face template encryption requires a 32-byte key");
        }
        this.key = new SecretKeySpec(key, "AES");
    }

    Encrypted encrypt(long userId, String model, byte[] embedding) throws GeneralSecurityException {
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);
        return new Encrypted(nonce, cipher(Cipher.ENCRYPT_MODE, userId, model, nonce).doFinal(embedding));
    }

    byte[] decrypt(long userId, String model, Encrypted template) throws GeneralSecurityException {
        return cipher(Cipher.DECRYPT_MODE, userId, model, template.nonce()).doFinal(template.ciphertext());
    }

    private Cipher cipher(int mode, long userId, String model, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
        cipher.updateAAD(model.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    record Encrypted(byte[] nonce, byte[] ciphertext) {}
}

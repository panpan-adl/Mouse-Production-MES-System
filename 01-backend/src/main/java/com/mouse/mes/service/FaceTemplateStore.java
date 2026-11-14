package com.mouse.mes.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.security.GeneralSecurityException;
import java.util.List;

final class FaceTemplateStore {
    private final JdbcTemplate db;
    private final FaceTemplateCipher cipher;
    private final String keyId;

    FaceTemplateStore(JdbcTemplate db, FaceTemplateCipher cipher, String keyId) {
        this.db = db;
        this.cipher = cipher;
        this.keyId = keyId;
    }

    void save(long userId, String model, byte[] embedding) throws GeneralSecurityException {
        var encrypted = cipher.encrypt(userId, model, embedding);
        db.update("""
                INSERT INTO mm_face_template(user_id,model_version,key_id,nonce,encrypted_embedding)
                VALUES(?,?,?,?,?)
                ON DUPLICATE KEY UPDATE model_version=VALUES(model_version),key_id=VALUES(key_id),
                    nonce=VALUES(nonce),encrypted_embedding=VALUES(encrypted_embedding),enrolled_at=NOW()
                """, userId, model, keyId, encrypted.nonce(), encrypted.ciphertext());
    }

    String enrolledAt(long userId, String model) {
        var rows = db.queryForList("""
                SELECT DATE_FORMAT(enrolled_at, '%Y-%m-%d %H:%i:%s')
                FROM mm_face_template WHERE user_id=? AND model_version=? AND key_id=?
                """, String.class, userId, model, keyId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    void remove(long userId) {
        db.update("DELETE FROM mm_face_template WHERE user_id=?", userId);
    }

    List<Candidate> candidates(String model) {
        return db.query("""
                SELECT f.user_id,f.nonce,f.encrypted_embedding
                FROM mm_face_template f JOIN t_user u ON u.id=f.user_id
                WHERE f.model_version=? AND f.key_id=? AND u.status=1 AND u.deleted=0
                """, (rs, row) -> new Candidate(rs.getLong("user_id"),
                new FaceTemplateCipher.Encrypted(rs.getBytes("nonce"), rs.getBytes("encrypted_embedding"))),
                model, keyId);
    }

    byte[] embedding(String model, Candidate candidate) throws GeneralSecurityException {
        return cipher.decrypt(candidate.userId(), model, candidate.template());
    }

    record Candidate(long userId, FaceTemplateCipher.Encrypted template) {}
}

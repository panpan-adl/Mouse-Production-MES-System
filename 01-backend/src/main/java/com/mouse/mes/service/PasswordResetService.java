package com.mouse.mes.service;

import com.mouse.mes.util.Passwords;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordResetService implements ApplicationRunner {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate db;

    public PasswordResetService(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public void run(ApplicationArguments args) {
        db.execute("""
                CREATE TABLE IF NOT EXISTS mm_password_reset (
                    user_id BIGINT PRIMARY KEY,
                    credential_hash VARCHAR(255) NOT NULL,
                    expires_at DATETIME NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    @Transactional
    public String issue(long userId) {
        var users = db.queryForList(
                "SELECT id FROM t_user WHERE id=? AND status=1 AND deleted=0 FOR UPDATE", userId);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("账号不存在或未启用");
        }
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String credential = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.update("""
                INSERT INTO mm_password_reset(user_id,credential_hash,expires_at)
                VALUES(?,?,DATE_ADD(NOW(), INTERVAL 15 MINUTE))
                ON DUPLICATE KEY UPDATE credential_hash=VALUES(credential_hash),expires_at=VALUES(expires_at)
                """, userId, Passwords.encode(credential));
        db.update("UPDATE t_user SET password='' WHERE id=?", userId);
        revokeSessions(userId);
        return credential;
    }

    @Transactional
    public void redeem(String userCode, String credential, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.equals(credential)) {
            throw new IllegalArgumentException("新密码至少6位且不能与临时凭据相同");
        }
        var users = db.queryForList(
                "SELECT id FROM t_user WHERE user_code=? AND status=1 AND deleted=0 FOR UPDATE",
                Long.class, userCode);
        if (users.isEmpty()) {
            throw invalidCredential();
        }
        long userId = users.get(0);
        var hashes = db.queryForList("""
                SELECT credential_hash FROM mm_password_reset
                WHERE user_id=? AND expires_at>NOW() FOR UPDATE
                """, String.class, userId);
        if (hashes.isEmpty() || !Passwords.matches(credential, hashes.get(0))) {
            throw invalidCredential();
        }
        db.update("UPDATE t_user SET password=?,update_time=NOW() WHERE id=?", Passwords.encode(newPassword), userId);
        db.update("DELETE FROM mm_password_reset WHERE user_id=?", userId);
        revokeSessions(userId);
    }

    private void revokeSessions(long userId) {
        db.update("INSERT INTO mm_account_settings(user_id,session_version) VALUES(?,1) "
                + "ON DUPLICATE KEY UPDATE session_version=session_version+1", userId);
    }

    private IllegalArgumentException invalidCredential() {
        return new IllegalArgumentException("临时凭据无效或已过期");
    }
}

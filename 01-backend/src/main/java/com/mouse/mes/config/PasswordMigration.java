package com.mouse.mes.config;

import com.mouse.mes.util.Passwords;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PasswordMigration implements ApplicationRunner {
    private final JdbcTemplate db;

    public PasswordMigration(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var accounts = db.query(
                "SELECT id, password FROM t_user WHERE password IS NOT NULL AND password <> ''",
                (rs, row) -> new LegacyPassword(rs.getLong("id"), rs.getString("password")));
        for (var account : accounts) {
            if (account.value().isBlank() || Passwords.isEncoded(account.value())) {
                continue;
            }
            db.update("UPDATE t_user SET password=? WHERE id=? AND BINARY password=BINARY ?",
                    Passwords.encode(account.value()), account.id(), account.value());
        }
    }

    private record LegacyPassword(long id, String value) {}
}

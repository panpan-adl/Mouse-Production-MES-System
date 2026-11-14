package com.mouse.mes.service;

import com.mouse.mes.mapper.UserMapper;
import com.mouse.mes.util.Passwords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "mes.face.enabled", havingValue = "true")
public class FaceLoginService {
    private final FaceChallengeService challenges;
    private final FaceInferenceClient inference;
    private final FaceTemplateStore templates;
    private final UserMapper users;
    private final String model;
    private final double threshold;
    private final double margin;

    public FaceLoginService(FaceChallengeService challenges, JdbcTemplate db, UserMapper users,
                            @Value("${mes.face.endpoint}") URI endpoint,
                            @Value("${mes.face.secret}") String secret,
                            @Value("${mes.face.model}") String model,
                            @Value("${mes.face.key}") String key,
                            @Value("${mes.face.key-id}") String keyId,
                            @Value("${mes.face.threshold}") double threshold,
                            @Value("${mes.face.margin}") double margin) {
        this.challenges = challenges;
        this.inference = new FaceInferenceClient(endpoint, secret, model);
        this.templates = new FaceTemplateStore(db, new FaceTemplateCipher(Base64.getDecoder().decode(key)), keyId);
        this.users = users;
        this.model = model;
        this.threshold = threshold;
        this.margin = margin;
    }

    public UUID challenge(Long employeeId) {
        return challenges.issue(employeeId);
    }

    public void enroll(long employeeId, String password, UUID challenge, List<String> frames)
            throws GeneralSecurityException {
        challenges.consume(challenge, employeeId);
        requirePassword(employeeId, password);
        byte[] embedding = FaceMatcher.encode(inference.embedding(challenge, frames));
        requirePassword(employeeId, password);
        templates.save(employeeId, model, embedding);
    }

    public String enrolledAt(long employeeId) {
        return templates.enrolledAt(employeeId, model);
    }

    public void remove(long employeeId, String password) {
        requirePassword(employeeId, password);
        templates.remove(employeeId);
    }

    public OptionalLong identify(UUID challenge, List<String> frames) throws GeneralSecurityException {
        challenges.consume(challenge, null);
        var matcher = new FaceMatcher(inference.embedding(challenge, frames));
        for (var candidate : templates.candidates(model)) {
            matcher.consider(candidate.userId(), templates.embedding(model, candidate));
        }
        return matcher.result(threshold, margin);
    }

    private void requirePassword(long employeeId, String password) {
        var user = users.findById(employeeId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || Integer.valueOf(1).equals(user.getDeleted())
                || !Passwords.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("请验证当前账号密码");
        }
    }
}

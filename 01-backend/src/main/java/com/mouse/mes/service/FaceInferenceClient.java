package com.mouse.mes.service;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

final class FaceInferenceClient {
    private final RestClient client;
    private final String modelVersion;

    FaceInferenceClient(URI endpoint, String secret, String modelVersion) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder()
                .baseUrl(endpoint.toString())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBearerAuth(secret))
                .build();
        this.modelVersion = modelVersion;
    }

    float[] embedding(UUID challenge, List<String> frames) {
        if (frames == null || frames.size() < 3 || frames.size() > 8
                || frames.stream().anyMatch(frame -> frame == null || frame.isBlank() || frame.length() > 350_000)) {
            throw new IllegalArgumentException("摄像头采集数据不完整或过大");
        }
        var result = client.post().uri("/infer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InferenceInput(challenge, frames))
                .retrieve().body(InferenceResult.class);
        if (result != null && !result.live() && result.reason() != null) {
            throw new IllegalArgumentException(result.reason());
        }
        if (result == null || !result.live() || !modelVersion.equals(result.modelVersion())
                || result.embedding() == null || result.embedding().length != 128) {
            throw new IllegalArgumentException("人脸验证未通过，请重试或使用密码");
        }
        return result.embedding();
    }

    private record InferenceInput(UUID challenge, List<String> frames) {}
    private record InferenceResult(boolean live, String modelVersion, float[] embedding, String reason) {}
}

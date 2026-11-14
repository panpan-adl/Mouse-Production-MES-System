package com.mouse.mes.service;

import java.nio.ByteBuffer;
import java.util.OptionalLong;

final class FaceMatcher {
    private final float[] probe;
    private double best = Double.NEGATIVE_INFINITY;
    private double second = Double.NEGATIVE_INFINITY;
    private long bestUser;

    FaceMatcher(float[] probe) {
        this.probe = normalized(probe);
    }

    void consider(long userId, byte[] embedding) {
        if (embedding.length != probe.length * Float.BYTES) {
            throw new IllegalArgumentException("Incompatible face template dimensions");
        }
        float[] candidate = new float[probe.length];
        ByteBuffer.wrap(embedding).asFloatBuffer().get(candidate);
        candidate = normalized(candidate);
        double score = 0;
        for (int i = 0; i < probe.length; i++) {
            score += (double) probe[i] * candidate[i];
        }
        if (score > best) {
            second = best;
            best = score;
            bestUser = userId;
        } else {
            second = Math.max(second, score);
        }
    }

    OptionalLong result(double threshold, double margin) {
        if (!(threshold > 0 && threshold <= 1 && margin > 0 && margin <= 2)) {
            throw new IllegalArgumentException("Invalid face matching thresholds");
        }
        return best >= threshold && best - second >= margin
                ? OptionalLong.of(bestUser) : OptionalLong.empty();
    }

    static byte[] encode(float[] embedding) {
        var buffer = ByteBuffer.allocate(embedding.length * Float.BYTES);
        buffer.asFloatBuffer().put(normalized(embedding));
        return buffer.array();
    }

    private static float[] normalized(float[] embedding) {
        double sum = 0;
        for (float value : embedding) {
            sum += (double) value * value;
        }
        if (!Double.isFinite(sum) || sum == 0) {
            throw new IllegalArgumentException("Invalid face embedding");
        }
        double norm = Math.sqrt(sum);
        float[] result = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            result[i] = (float) (embedding[i] / norm);
        }
        return result;
    }
}

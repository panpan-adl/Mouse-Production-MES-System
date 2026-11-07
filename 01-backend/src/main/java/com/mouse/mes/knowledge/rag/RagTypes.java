package com.mouse.mes.knowledge.rag;

import java.util.List;

public final class RagTypes {
    private RagTypes() {}
    public record Part(String text, String location) {}
    public record Chunk(String id, String documentId, int version, String title,
                        String text, String location) {}
    public record Source(String id, String documentId, String title, int version,
                         String chunkId, String text, String location, double score) {}
    public record Answer(String answer, List<Source> sources, List<Object> facts,
                         String retrieval, String model) {}
}

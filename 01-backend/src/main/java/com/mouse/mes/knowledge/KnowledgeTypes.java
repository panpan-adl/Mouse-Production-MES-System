package com.mouse.mes.knowledge;

import java.util.List;

/** Versioned contract shared with task two. Owner always comes from the session. */
public final class KnowledgeTypes {
    private KnowledgeTypes() {}
    public record DocumentInput(String documentId, String title, long version, String text) {}
    public record Question(String question, String mode, List<String> documentIds) {
        public Question(String question, String mode) { this(question, mode, null); }
    }
    public record Chunk(String id, String documentId, String title, long version, int ordinal, String text) {}
    public record Triple(String source, String relation, String target, String quote) {}
    public record Fact(String source, String relation, String target, String quote, String chunkId) {}
    public record Document(String documentId, String title, long version, String text, String buildId,
                           List<Chunk> chunks, String state) {}
    public record Evidence(String id, String documentId, String title, long version, String chunkId, String text) {}
    public record Graph(List<Fact> facts, boolean truncated) {}
    public record Answer(String answer, String mode, String retrieval, List<Evidence> sources,
                         List<Fact> facts, String model) {}
}

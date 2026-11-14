package com.mouse.mes.knowledge.rag;

/** Implemented by task three. Publication/revocation join the file transaction. Cleanup follows commit. */
public interface RagGraphBridge {
    record Published(String buildId,java.util.List<RagTypes.Chunk> chunks){}
    Published publish(long owner,String id,int version,String title,String text);
    void revoke(long owner,String id,int version);
    boolean cleanup(long owner,String id,int version);
}

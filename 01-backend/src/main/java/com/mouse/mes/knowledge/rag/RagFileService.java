package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;

@Service
public class RagFileService {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final TransactionTemplate progressTx;
    private final RagParser parser;
    private final RagModelClient models;
    private final ObjectMapper json;
    private final Path root;
    private RagGraphBridge graph;
    @Autowired(required=false) public void setGraphBridge(RagGraphBridge value){graph=value;}
    private void stage(long owner,String id,int version,String value){
        try{progressTx.executeWithoutResult(s->db.update("INSERT INTO rag_progress(owner,document_id,version,stage,updated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE stage=VALUES(stage),updated_at=CURRENT_TIMESTAMP",owner,id,version,value));}catch(org.springframework.dao.DataAccessException e){org.slf4j.LoggerFactory.getLogger(RagFileService.class).warn("Progress persistence unavailable; publication status remains authoritative");}
    }
    public boolean graphIntegrated(){return graph!=null;}
    private void graphState(long owner,String id,int version,String status,String error,boolean attempt){
        String message=Objects.toString(error,"");message=message.substring(0,Math.min(500,message.length()));
        db.update("INSERT INTO rag_graph_sync(owner,document_id,version,status,error,attempts) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE status=VALUES(status),error=VALUES(error),attempts=attempts+?",owner,id,version,status,message,attempt?1:0,attempt?1:0);
    }
    private String graphStatus(long owner,String id,int version){
        var rows=db.queryForList("SELECT status FROM rag_graph_sync WHERE owner=? AND document_id=? AND version=?",owner,id,version);
        return rows.isEmpty()?"PENDING_TASK3":rows.get(0).get("status").toString();
    }
    public RagFileService(JdbcTemplate db, PlatformTransactionManager manager, RagParser parser,
                          RagModelClient models, ObjectMapper json, RagSchema schema,
                          @Value("${MES_RAG_STORAGE:uploads/rag-files}") String storage) throws Exception {
        this.db = db; this.tx = new TransactionTemplate(manager); this.parser = parser; this.models = models; this.json = json;
        this.progressTx=new TransactionTemplate(manager);
        this.progressTx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        root = Path.of(storage).toAbsolutePath().normalize(); Files.createDirectories(root);
    }
    public static void validateId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("documentId须为1至64位ASCII字母、数字、下划线或短横线");
    }
    public boolean managed(long owner,String id){validateId(id);return db.queryForObject("SELECT COUNT(*) FROM rag_document WHERE owner=? AND document_id=?",Integer.class,owner,id)>0;}
    public List<Map<String,Object>> list(long owner) {
        // Recover an abandoned request even when the backend stayed running after the last restart.
        db.update("UPDATE rag_revision SET status='FAILED',error='处理超过30分钟未完成，可重试同一版本；旧可检索版本保留' WHERE owner=? AND status IN ('UPLOADED','INDEXING') AND COALESCE((SELECT p.updated_at FROM rag_progress p WHERE p.owner=rag_revision.owner AND p.document_id=rag_revision.document_id AND p.version=rag_revision.version),created_at)<TIMESTAMPADD(MINUTE,-30,CURRENT_TIMESTAMP)",owner);
        return db.queryForList("SELECT d.document_id AS documentId,d.version,d.active_version AS activeVersion,r.title,r.filename,r.status,r.error,r.attempts,r.created_at AS createdAt,COALESCE(g.status,'PENDING_TASK3') AS graphStatus,g.error AS graphError,CASE WHEN r.status IN ('READY','FAILED') THEN r.status ELSE COALESCE(p.stage,r.status) END AS stage FROM rag_document d JOIN rag_revision r ON r.owner=d.owner AND r.document_id=d.document_id AND r.version=d.version LEFT JOIN rag_graph_sync g ON g.owner=r.owner AND g.document_id=r.document_id AND g.version=r.version LEFT JOIN rag_progress p ON p.owner=r.owner AND p.document_id=r.document_id AND p.version=r.version WHERE d.owner=? AND d.deleted=FALSE ORDER BY r.created_at DESC", owner);
    }
    private Map<String,Object> revision(long owner, String id, int version) {
        validateId(id);
        var rows = db.queryForList("SELECT * FROM rag_revision WHERE owner=? AND document_id=? AND version=?", owner,id,version);
        if (rows.isEmpty()) throw new SecurityException("文件不存在或无权访问");
        return rows.get(0);
    }
    public Map<String,Object> detail(long owner, String id) {
        validateId(id);
        var rows = db.queryForList("SELECT r.document_id AS documentId,r.version,r.title,r.filename,r.parsed_text AS text,d.active_version AS activeVersion,r.status,r.error FROM rag_document d JOIN rag_revision r ON r.owner=d.owner AND r.document_id=d.document_id AND r.version=d.version WHERE d.owner=? AND d.document_id=? AND d.deleted=FALSE", owner,id);
        if (rows.isEmpty()) throw new SecurityException("文件不存在或无权访问");
        return rows.get(0);
    }
    public Map<String,Object> upload(long owner, String id, int version, String title, String filename, byte[] bytes) throws Exception {
        validateId(id);
        if (version < 1) throw new IllegalArgumentException("version须为正整数");
        if (title == null || title.isBlank() || title.length() > 200) throw new IllegalArgumentException("标题须为1至200字符");
        if (filename == null || filename.length() > 255 || bytes.length == 0 || bytes.length > RagParser.MAX_BYTES)
            throw new IllegalArgumentException("文件名或大小无效，最大2MB");
        filename = filename.replace('\\','/'); filename = filename.substring(filename.lastIndexOf('/')+1);
        String safeName = filename;
        Path storage = root.resolve(UUID.randomUUID() + ".bin");
        Files.write(storage, bytes, StandardOpenOption.CREATE_NEW);
        try {
            tx.executeWithoutResult(s -> {
                db.update("INSERT IGNORE INTO rag_document(owner,document_id) VALUES(?,?)", owner,id);
                var doc = db.queryForMap("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
                if (((Number)doc.get("version")).intValue()+1 != version)
                    throw new IllegalArgumentException("版本冲突：新版本必须为当前version加1；失败请重试原版本");
                var pending = db.queryForObject("SELECT COUNT(*) FROM rag_revision WHERE owner=? AND document_id=? AND status IN ('UPLOADED','INDEXING')",Integer.class,owner,id);
                if (pending != null && pending > 0) throw new IllegalArgumentException("此文件正在索引，请等待完成");
                db.update("INSERT INTO rag_revision(owner,document_id,version,title,filename,storage_path,status,build_id) VALUES(?,?,?,?,?,?,'UPLOADED',?)",owner,id,version,title,safeName,storage.toString(),UUID.randomUUID().toString());
                db.update("UPDATE rag_document SET version=?,deleted=FALSE WHERE owner=? AND document_id=?",version,owner,id);
            });
        } catch (Exception e) { Files.deleteIfExists(storage); throw e; }
        return process(owner,id,version);
    }
    public Map<String,Object> retry(long owner, String id, int version) throws Exception {
        var rev = revision(owner,id,version);
        if ("DELETE".equals(rev.get("action"))) return delete(owner,id,version);
        if ("READY".equals(rev.get("status"))) {
            if(graph!=null && !"READY".equals(graphStatus(owner,id,version)))return syncReady(owner,id,version);
            // Never acknowledge a stale ready event after the document has moved on.
            syncEvent(owner,id,version);
            return Map.of("documentId",id,"version",version,"unchanged",true);
        }
        return process(owner,id,version);
    }
    private Map<String,Object> syncReady(long owner,String id,int version){
        try {
            tx.executeWithoutResult(s->{
                var doc=db.queryForMap("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
                if(((Number)doc.get("version")).intValue()!=version || ((Number)doc.get("active_version")).intValue()!=version || Boolean.TRUE.equals(doc.get("deleted")))
                    throw new IllegalArgumentException("当前版本已变化，不能同步旧事件");
                var rev=revision(owner,id,version);
                alignSharedChunks(owner,id,version,graph.publish(owner,id,version,rev.get("title").toString(),rev.get("parsed_text").toString()));
                graphState(owner,id,version,"READY","",true);
            });
            return Map.of("documentId",id,"version",version,"status","READY","graphStatus","READY","unchanged",false);
        } catch(IllegalArgumentException|SecurityException e){throw e;}
        catch(Exception e){graphState(owner,id,version,"FAILED",e.getMessage(),true);throw new IllegalStateException("图谱同步失败，已保留原有索引；可重试当前版本");}
    }
    private Map<String,Object> process(long owner, String id, int version) throws Exception {
        Boolean claimed = tx.execute(s -> {
            var docs=db.queryForList("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
            if(docs.isEmpty()) throw new SecurityException("文件不存在或无权访问");
            var doc=docs.get(0);
            if (((Number)doc.get("version")).intValue()!=version || Boolean.TRUE.equals(doc.get("deleted")))
                throw new IllegalArgumentException("当前版本已变化，不能重试旧事件");
            return db.update("UPDATE rag_revision SET status='INDEXING',error='',attempts=attempts+1 WHERE owner=? AND document_id=? AND version=? AND status IN ('UPLOADED','FAILED')",owner,id,version)==1;
        });
        if (!Boolean.TRUE.equals(claimed)) throw new IllegalArgumentException("版本正在处理或已完成，请刷新列表");
        var rev = revision(owner,id,version);
        graphState(owner,id,version,graph==null?"PENDING_TASK3":"INDEXING","",true);
        try {
            stage(owner,id,version,"PARSING");
            var parts = parser.parse(rev.get("filename").toString(),Files.readAllBytes(storedPath(rev)));
            var chunks = parser.chunks(id,version,rev.get("title").toString(),rev.get("build_id").toString(),parts);
            String text = String.join("\n",parts.stream().map(RagTypes.Part::text).toList());
            // Persist extraction even if the remote embedding call fails, without publishing it.
            db.update("UPDATE rag_revision SET parsed_text=? WHERE owner=? AND document_id=? AND version=?",text,owner,id,version);
            stage(owner,id,version,"EMBEDDING");
            var embeddings = models.embed(owner,chunks.stream().map(RagTypes.Chunk::text).toList());
            List<String> encoded = new ArrayList<>();
            for (var embedding : embeddings) encoded.add(json.writeValueAsString(embedding));
            stage(owner,id,version,"GRAPH_BUILD");
            tx.executeWithoutResult(s -> {
                var doc = db.queryForMap("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
                if (((Number)doc.get("version")).intValue()!=version || Boolean.TRUE.equals(doc.get("deleted")))
                    throw new IllegalArgumentException("处理期间文件已更新或删除，本次结果未发布");
                // Graph publication joins this transaction. Failure keeps BOTH previous active versions.
                db.update("DELETE FROM rag_vector WHERE owner=? AND document_id=?",owner,id);
                for(int i=0;i<chunks.size();i++) {
                    var c=chunks.get(i);
                    db.update("INSERT INTO rag_vector(owner,document_id,version,chunk_id,title,content,location,embedding_model,embedding) VALUES(?,?,?,?,?,?,?,?,?)",owner,id,version,c.id(),c.title(),c.text(),c.location(),models.embeddingModel(),encoded.get(i));
                }
                db.update("UPDATE rag_document SET active_version=? WHERE owner=? AND document_id=?",version,owner,id);
                if(graph!=null)alignSharedChunks(owner,id,version,graph.publish(owner,id,version,rev.get("title").toString(),text));
                stage(owner,id,version,"PUBLISHING");
                db.update("UPDATE rag_revision SET status='READY',error='' WHERE owner=? AND document_id=? AND version=?",owner,id,version);
                graphState(owner,id,version,graph==null?"PENDING_TASK3":"READY","",false);
            });
            stage(owner,id,version,"READY");
            return Map.of("documentId",id,"version",version,"chunkCount",chunks.size(),"status","READY","graphStatus",graph==null?"PENDING_TASK3":"READY","unchanged",false);
        } catch (Exception e) {
            stage(owner,id,version,"FAILED");
            // Retain the previous active_version and its vectors on any failed replacement.
            String message = e instanceof IllegalArgumentException || e instanceof IllegalStateException ? Objects.toString(e.getMessage(),"处理失败，请重试") : "解析或索引失败，请确认文件有效后重试";
            db.update("UPDATE rag_revision SET status='FAILED',error=? WHERE owner=? AND document_id=? AND version=?",message.substring(0,Math.min(500,message.length())),owner,id,version);
            graphState(owner,id,version,graph==null?"PENDING_TASK3":"FAILED",message,false);
            return Map.of("documentId",id,"version",version,"status","FAILED","error",message,"graphStatus",graph==null?"PENDING_TASK3":"FAILED");
        }
    }
    private void alignSharedChunks(long owner,String id,int version,RagGraphBridge.Published published){
        var rows=new ArrayList<>(db.queryForList("SELECT chunk_id,content FROM rag_vector WHERE owner=? AND document_id=? AND version=?",owner,id,version));
        if(rows.size()!=published.chunks().size())throw new IllegalStateException("文件与图谱分块数量不一致，新版本未发布");
        for(var chunk:published.chunks()){
            var match=rows.stream().filter(row->chunk.text().equals(row.get("content"))).findFirst().orElseThrow(()->new IllegalStateException("文件与图谱片段正文不一致，新版本未发布"));
            if(!chunk.id().equals(match.get("chunk_id")))db.update("UPDATE rag_vector SET chunk_id=? WHERE owner=? AND document_id=? AND version=? AND chunk_id=?",chunk.id(),owner,id,version,match.get("chunk_id"));
            rows.remove(match);
        }
        db.update("UPDATE rag_revision SET build_id=? WHERE owner=? AND document_id=? AND version=?",published.buildId(),owner,id,version);
    }
    private Path storedPath(Map<String,Object> rev) {
        Path path=Path.of(rev.get("storage_path").toString()).toAbsolutePath().normalize();
        if(!path.startsWith(root)) throw new SecurityException("文件存储路径无效");
        return path;
    }
    public record Download(String filename, byte[] bytes) {}
    public Download download(long owner,String id) throws Exception {
        var item = detail(owner,id);
        int active=((Number)item.get("activeVersion")).intValue();
        // The current original remains downloadable when parsing/indexing failed, allowing repair.
        int version=active>0?active:((Number)item.get("version")).intValue();
        var rev=revision(owner,id,version);
        return new Download(rev.get("filename").toString(),Files.readAllBytes(storedPath(rev)));
    }
    public Map<String,Object> delete(long owner,String id,int version) {
        validateId(id);
        tx.executeWithoutResult(s -> {
            var docs=db.queryForList("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
            if(docs.isEmpty()) throw new SecurityException("文件不存在或无权访问");
            var doc=docs.get(0); int current=((Number)doc.get("version")).intValue();
            var rev=revision(owner,id,current);
            if (version==current && "DELETE".equals(rev.get("action"))) return;
            if(version!=current+1) throw new IllegalArgumentException("删除版本必须为当前version加1");
            if(graph!=null)graph.revoke(owner,id,version);
            db.update("INSERT INTO rag_revision(owner,document_id,version,title,filename,storage_path,action,status,build_id) VALUES(?,?,?,?,?,?,'DELETE','CLEANUP_PENDING',?)",owner,id,version,rev.get("title"),rev.get("filename"),rev.get("storage_path"),UUID.randomUUID().toString());
            db.update("UPDATE rag_document SET version=?,active_version=0,deleted=TRUE WHERE owner=? AND document_id=?",version,owner,id);
            db.update("DELETE FROM rag_vector WHERE owner=? AND document_id=?",owner,id);
            graphState(owner,id,version,graph==null?"PENDING_TASK3":"CLEANUP_PENDING","",true);
        });
        // Permission revocation has committed before filesystem cleanup. Never restore on failure.
        boolean pending=false;
        for(var rev:db.queryForList("SELECT * FROM rag_revision WHERE owner=? AND document_id=? AND version<=? AND action='UPSERT'",owner,id,version)) {
            try { Files.deleteIfExists(storedPath(rev)); } catch(Exception e) { pending=true; }
        }
        db.update("UPDATE rag_revision SET status=? WHERE owner=? AND document_id=? AND version=? AND action='DELETE'",pending?"CLEANUP_PENDING":"DELETED",owner,id,version);
        boolean graphPending=true;
        if(graph!=null) {
            try {graphPending=graph.cleanup(owner,id,version);}catch(Exception e){graphPending=true;}
            graphState(owner,id,version,graphPending?"CLEANUP_PENDING":"DELETED",graphPending?"图数据库清理待重试":"",false);
        }
        return Map.of("deleted",true,"cleanupPending",pending,"graphCleanupPending",graphPending,"version",version);
    }
    public List<Map<String,Object>> events(long owner) {
        return db.queryForList("SELECT r.document_id AS documentId,r.version,r.action,r.status,r.error,r.attempts,r.created_at AS createdAt,COALESCE(g.status,'PENDING_TASK3') AS graphStatus,g.error AS graphError FROM rag_revision r LEFT JOIN rag_graph_sync g ON g.owner=r.owner AND g.document_id=r.document_id AND g.version=r.version WHERE r.owner=? ORDER BY r.created_at DESC LIMIT 100",owner);
    }
    public Map<String,Object> syncPayload(long owner,String id) {
        var doc=detail(owner,id); int active=((Number)doc.get("activeVersion")).intValue();
        if(active==0) throw new IllegalArgumentException("尚无可发布给任务三的版本，请先完成向量索引");
        var rev=revision(owner,id,active);
        return Map.of("documentId",id,"version",active,"title",rev.get("title"),"text",rev.get("parsed_text"));
    }
    public Map<String,Object> syncEvent(long owner,String id,int version) {
        var rev=revision(owner,id,version);
        var doc=db.queryForMap("SELECT * FROM rag_document WHERE owner=? AND document_id=?",owner,id);
        if(((Number)doc.get("version")).intValue()!=version) throw new IllegalArgumentException("事件版本已过期，请使用当前版本");
        if("DELETE".equals(rev.get("action"))) return Map.of("action","DELETE","documentId",id,"version",version);
        if(((Number)doc.get("active_version")).intValue()!=version) throw new IllegalArgumentException("当前事件尚未完成向量索引，不可同步发布");
        return Map.of("action","UPSERT","payload",syncPayload(owner,id));
    }
}

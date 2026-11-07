package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;

@Service
public class RagFileService {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private RagKnowledgeBridge knowledge;
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final RagParser parser;
    private final RagModelClient models;
    private final ObjectMapper json;
    private final Path root;
    public RagFileService(JdbcTemplate db, PlatformTransactionManager manager, RagParser parser,
                          RagModelClient models, ObjectMapper json, RagSchema schema,
                          @Value("${MES_RAG_STORAGE:uploads/rag-files}") String storage) throws Exception {
        this.db = db; this.tx = new TransactionTemplate(manager); this.parser = parser; this.models = models; this.json = json;
        root = Path.of(storage).toAbsolutePath().normalize(); Files.createDirectories(root);
    }
    public static void validateId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("documentId须为1至64位ASCII字母、数字、下划线或短横线");
    }
    public List<Map<String,Object>> list(long owner) {
        var rows=db.queryForList("SELECT d.document_id AS documentId,d.version,d.active_version AS activeVersion,r.title,r.filename,r.status,r.error,r.attempts,r.created_at AS createdAt,(SELECT COUNT(*) FROM rag_vector v WHERE v.owner=d.owner AND v.document_id=d.document_id AND v.version=d.active_version) AS chunkCount FROM rag_document d JOIN rag_revision r ON r.owner=d.owner AND r.document_id=d.document_id AND r.version=d.version WHERE d.owner=? AND d.deleted=FALSE ORDER BY r.created_at DESC", owner);
        if(knowledge!=null)for(var row:rows)row.put("graphStatus",knowledge.current(owner,row.get("documentId").toString(),((Number)row.get("activeVersion")).intValue())?"READY":"PENDING");
        return rows;
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
        if(knowledge!=null&&!isManaged(owner,id)) {
            var legacy=knowledge.original(owner,id);
            if(legacy.isPresent()) {
                var original=legacy.get();
                Path originalPath=root.resolve(UUID.randomUUID()+".bin");
                Files.writeString(originalPath,original.text(),java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
                try {tx.executeWithoutResult(s->{
                    int inserted=db.update("INSERT IGNORE INTO rag_document(owner,document_id,version,active_version,deleted) VALUES(?,?,?,?,FALSE)",owner,id,original.version(),original.version());
                    if(inserted==1)db.update("INSERT INTO rag_revision(owner,document_id,version,title,filename,storage_path,status,build_id,parsed_text) VALUES(?,?,?,?,?,?,'READY',?,?)",owner,id,original.version(),original.title(),original.title()+".md",originalPath.toString(),original.buildId(),original.text());
                    else try{Files.deleteIfExists(originalPath);}catch(Exception ignored){}
                });}catch(Exception e){Files.deleteIfExists(originalPath);throw e;}
            }
        }
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
            if(knowledge==null||knowledge.current(owner,id,version)) return Map.of("documentId",id,"version",version,"unchanged",true);
            db.update("UPDATE rag_revision SET status='FAILED',error='合并后待同步双索引' WHERE owner=? AND document_id=? AND version=?",owner,id,version);
        }
        return process(owner,id,version);
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
        try {
            var parts = parser.parse(rev.get("filename").toString(),Files.readAllBytes(storedPath(rev)));
            var chunks = parser.chunks(id,version,rev.get("title").toString(),rev.get("build_id").toString(),parts);
            String text = String.join("\n",parts.stream().map(RagTypes.Part::text).toList());
            // Persist extraction even if the remote embedding call fails, without publishing it.
            db.update("UPDATE rag_revision SET parsed_text=? WHERE owner=? AND document_id=? AND version=?",text,owner,id,version);
            if(knowledge!=null) return publishUnified(owner,id,version,rev.get("title").toString(),text,parts);
            var embeddings = models.embed(owner,chunks.stream().map(RagTypes.Chunk::text).toList());
            List<String> encoded = new ArrayList<>();
            for (var embedding : embeddings) encoded.add(json.writeValueAsString(embedding));
            tx.executeWithoutResult(s -> {
                var doc = db.queryForMap("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
                if (((Number)doc.get("version")).intValue()!=version || Boolean.TRUE.equals(doc.get("deleted")))
                    throw new IllegalArgumentException("处理期间文件已更新或删除，本次结果未发布");
                db.update("DELETE FROM rag_vector WHERE owner=? AND document_id=?",owner,id);
                for(int i=0;i<chunks.size();i++) {
                    var c=chunks.get(i);
                    db.update("INSERT INTO rag_vector(owner,document_id,version,chunk_id,title,content,location,embedding_model,embedding) VALUES(?,?,?,?,?,?,?,?,?)",owner,id,version,c.id(),c.title(),c.text(),c.location(),models.embeddingModel(),encoded.get(i));
                }
                db.update("UPDATE rag_document SET active_version=? WHERE owner=? AND document_id=?",version,owner,id);
                db.update("UPDATE rag_revision SET status='READY',error='' WHERE owner=? AND document_id=? AND version=?",owner,id,version);
            });
            return Map.of("documentId",id,"version",version,"chunkCount",chunks.size(),"status","READY","graphStatus","PENDING_TASK3","unchanged",false);
        } catch (Exception e) {
            // Retain the previous active_version and its vectors on any failed replacement.
            String message = e instanceof IllegalArgumentException || e instanceof IllegalStateException ? e.getMessage() : "解析或索引失败，请确认文件有效后重试";
            db.update("UPDATE rag_revision SET status='FAILED',error=? WHERE owner=? AND document_id=? AND version=?",message.substring(0,Math.min(500,message.length())),owner,id,version);
            return Map.of("documentId",id,"version",version,"status","FAILED","error",message,"graphStatus","PENDING_TASK3");
        }
    }
    private Map<String,Object> publishUnified(long owner,String id,int version,String title,String text,List<RagTypes.Part> parts) {
        return tx.execute(s->{
            var doc=db.queryForMap("SELECT * FROM rag_document WHERE owner=? AND document_id=? FOR UPDATE",owner,id);
            if(((Number)doc.get("version")).intValue()!=version||Boolean.TRUE.equals(doc.get("deleted")))
                throw new IllegalArgumentException("文件已更新或删除，本次结果未发布");
            var published=knowledge.publish(owner,id,version,title,text);
            var locations=parser.chunks(id,version,title,published.buildId(),parts);
            if(locations.size()!=published.chunks().size()) throw new IllegalStateException("图谱与文件分块数量不一致，未发布新版本");
            db.update("DELETE FROM rag_vector WHERE owner=? AND document_id=?",owner,id);
            for(int i=0;i<published.chunks().size();i++) {
                var chunk=published.chunks().get(i);
                if(!locations.get(i).text().equals(chunk.text())) throw new IllegalStateException("图谱与向量原文片段不一致，未发布新版本");
                var vector=db.queryForMap("SELECT embedding,embedding_model FROM rag_authorized_vector WHERE owner=? AND document_id=? AND version=? AND chunk_id=?",owner,id,version,chunk.id());
                db.update("INSERT INTO rag_vector(owner,document_id,version,chunk_id,title,content,location,embedding_model,embedding) VALUES(?,?,?,?,?,?,?,?,?)",owner,id,version,chunk.id(),title,chunk.text(),locations.get(i).location(),vector.get("embedding_model"),vector.get("embedding").toString());
            }
            db.update("UPDATE rag_revision SET build_id=?,status='READY',error='' WHERE owner=? AND document_id=? AND version=?",published.buildId(),owner,id,version);
            db.update("UPDATE rag_document SET active_version=? WHERE owner=? AND document_id=?",version,owner,id);
            return Map.<String,Object>of("documentId",id,"version",version,"chunkCount",published.chunks().size(),"status","READY","graphStatus","READY","unchanged",false);
        });
    }
    public boolean isManaged(long owner,String id) {
        validateId(id);Integer count=db.queryForObject("SELECT COUNT(*) FROM rag_document WHERE owner=? AND document_id=?",Integer.class,owner,id);
        return count!=null&&count>0;
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
            db.update("INSERT INTO rag_revision(owner,document_id,version,title,filename,storage_path,action,status,build_id) VALUES(?,?,?,?,?,?,'DELETE','CLEANUP_PENDING',?)",owner,id,version,rev.get("title"),rev.get("filename"),rev.get("storage_path"),UUID.randomUUID().toString());
            db.update("UPDATE rag_document SET version=?,active_version=0,deleted=TRUE WHERE owner=? AND document_id=?",version,owner,id);
            db.update("DELETE FROM rag_vector WHERE owner=? AND document_id=?",owner,id);
            if(knowledge!=null) knowledge.tombstone(owner,id,version);
        });
        // Permission revocation has committed before filesystem cleanup. Never restore on failure.
        boolean pending=false;
        for(var rev:db.queryForList("SELECT * FROM rag_revision WHERE owner=? AND document_id=? AND version<=? AND action='UPSERT'",owner,id,version)) {
            try { Files.deleteIfExists(storedPath(rev)); } catch(Exception e) { pending=true; }
        }
        db.update("UPDATE rag_revision SET status=? WHERE owner=? AND document_id=? AND version=? AND action='DELETE'",pending?"CLEANUP_PENDING":"DELETED",owner,id,version);
        boolean graphPending=knowledge==null||Boolean.TRUE.equals(knowledge.cleanup(owner,id,version).get("cleanupPending"));
        if(graphPending)db.update("UPDATE rag_revision SET status='CLEANUP_PENDING' WHERE owner=? AND document_id=? AND version=?",owner,id,version);
        return Map.of("deleted",true,"cleanupPending",pending||graphPending,"graphCleanupPending",graphPending,"version",version);
    }
    public List<Map<String,Object>> events(long owner) {
        return db.queryForList("SELECT document_id AS documentId,version,action,status,error,attempts,created_at AS createdAt FROM rag_revision WHERE owner=? ORDER BY created_at DESC LIMIT 100",owner);
    }
    public List<Map<String,Object>> pendingCleanup(long owner){
        return db.queryForList("SELECT r.document_id AS documentId,r.version FROM rag_revision r JOIN rag_document d ON d.owner=r.owner AND d.document_id=r.document_id AND d.version=r.version WHERE r.owner=? AND d.deleted=TRUE AND r.action='DELETE' AND r.status='CLEANUP_PENDING'",owner);
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

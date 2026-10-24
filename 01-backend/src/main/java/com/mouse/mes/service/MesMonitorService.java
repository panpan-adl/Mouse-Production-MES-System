package com.mouse.mes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

/** Redis monitoring exposes counts and session identities, never keys, tokens or cached values. */
@Service
public class MesMonitorService {
    private final RedisTemplate<String,Object> redis;private final ObjectMapper json;
    public MesMonitorService(RedisTemplate<String,Object> redis,ObjectMapper json){this.redis=redis;this.json=json;}
    private String fingerprint(String key){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private List<String> sessionKeys(){return redis.execute((RedisCallback<List<String>>)connection->{List<String> keys=new ArrayList<>();try(var cursor=connection.keyCommands().scan(ScanOptions.scanOptions().match("user:token:*").count(100).build())){while(cursor.hasNext()){if(keys.size()>=5000)throw new IllegalArgumentException("在线会话超过监控上限，请缩小范围后检查");keys.add(new String(cursor.next(),StandardCharsets.UTF_8));}}return keys;});}
    public List<Map<String,Object>> sessions(){List<Map<String,Object>> result=new ArrayList<>();for(String key:sessionKeys()){Object value=redis.opsForValue().get(key);if(value==null)continue;LoginResponse user=json.convertValue(value,LoginResponse.class);Map<String,Object> row=new LinkedHashMap<>();row.put("session_id",fingerprint(key));row.put("user_id",user.getId());row.put("name",user.getRealName());row.put("role",user.getRoleName());row.put("ttl_seconds",redis.getExpire(key));result.add(row);}return result;}
    public void revoke(String fingerprint){boolean found=false;for(String key:sessionKeys())if(fingerprint(key).equals(fingerprint)){redis.delete(key);found=true;}if(!found)throw new IllegalArgumentException("会话已过期或不存在");}
    public Map<String,Object> cache(){return redis.execute((RedisCallback<Map<String,Object>>)connection->{Properties info=connection.serverCommands().info();Map<String,Object> result=new LinkedHashMap<>();for(String name:List.of("redis_version","uptime_in_seconds","connected_clients","used_memory","used_memory_peak","keyspace_hits","keyspace_misses","expired_keys"))if(info!=null)result.put(name,info.getProperty(name,"未知"));result.put("key_count",connection.serverCommands().dbSize());return result;});}
}

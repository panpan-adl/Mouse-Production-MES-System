package com.mouse.mes.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void set(String key, Object value);
    
    void set(String key, Object value, long timeout, TimeUnit unit);
    
    Object get(String key);
    
    boolean exists(String key);
    
    void delete(String key);
    
    void expire(String key, long timeout, TimeUnit unit);
}

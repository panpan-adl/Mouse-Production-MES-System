package com.mouse.mes.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Test
    public void testSetAndGet() {
        String key = "test:key";
        String value = "test:value";

        redisService.set(key, value);

        Object result = redisService.get(key);
        assertNotNull(result);
        assertEquals(value, result);
    }

    @Test
    public void testSetWithExpire() {
        String key = "test:expire";
        String value = "test:expire:value";

        redisService.set(key, value, 1, TimeUnit.SECONDS);

        Object result = redisService.get(key);
        assertNotNull(result);
        assertEquals(value, result);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        result = redisService.get(key);
        assertNull(result);
    }

    @Test
    public void testExists() {
        String key = "test:exists";
        String value = "test:exists:value";

        assertFalse(redisService.exists(key));

        redisService.set(key, value);
        assertTrue(redisService.exists(key));
    }

    @Test
    public void testDelete() {
        String key = "test:delete";
        String value = "test:delete:value";

        redisService.set(key, value);
        assertTrue(redisService.exists(key));

        redisService.delete(key);
        assertFalse(redisService.exists(key));
    }

    @Test
    public void testExpire() {
        String key = "test:manual:expire";
        String value = "test:manual:expire:value";

        redisService.set(key, value);
        redisService.expire(key, 1, TimeUnit.SECONDS);

        assertNotNull(redisService.get(key));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNull(redisService.get(key));
    }
}

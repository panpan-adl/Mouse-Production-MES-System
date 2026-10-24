package com.mouse.mes.service;

import com.mouse.mes.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testFindByUserCode() {
        User user = userService.findByUserCode("109001");
        assertNotNull(user);
        assertEquals("109001", user.getUserCode());
        assertEquals("admin", user.getUsername());
        assertEquals("张三", user.getRealName());
    }

    @Test
    public void testFindByUsername() {
        User user = userService.findByUsername("admin");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertEquals("109001", user.getUserCode());
    }

    @Test
    public void testFindById() {
        User user = userService.findById(1L);
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("109001", user.getUserCode());
    }

    @Test
    public void testFindAll() {
        assertNotNull(userService.findAll());
    }

    @Test
    public void testFindNonExistentUser() {
        User user = userService.findByUserCode("999999");
        assertNull(user);
    }
}

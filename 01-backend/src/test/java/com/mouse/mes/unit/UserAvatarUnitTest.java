package com.mouse.mes.unit;

import com.mouse.mes.controller.UserController;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Regression tests for avatar MIME detection and directory boundaries. */
class UserAvatarUnitTest {
    @TempDir Path dir;
    private UserController controller() {
        UserController controller=new UserController();
        ReflectionTestUtils.setField(controller,"uploadPath",dir.toString()); return controller;
    }
    @Test void missingFileReturns404() { assertEquals(404,controller().getAvatar("absent.png").getStatusCode().value()); }
    @Test void knownMimeReturns200() throws Exception {
        Path file=dir.resolve("avatar.png"); Files.write(file,new byte[]{1,2,3});
        try(MockedStatic<Files> files=mockStatic(Files.class,CALLS_REAL_METHODS)) {
            files.when(()->Files.probeContentType(file.toAbsolutePath())).thenReturn("image/png");
            var response=controller().getAvatar("avatar.png"); assertEquals(200,response.getStatusCode().value());
            assertEquals(MediaType.IMAGE_PNG,response.getHeaders().getContentType()); assertNotNull(response.getBody());
            assertEquals("max-age=3600",response.getHeaders().getFirst("Cache-Control"));
        }
    }
    @Test void unknownMimeUsesBinaryFallback() throws Exception {
        Path file=dir.resolve("avatar.unknown"); Files.write(file,new byte[]{1,2,3});
        try(MockedStatic<Files> files=mockStatic(Files.class,CALLS_REAL_METHODS)) {
            files.when(()->Files.probeContentType(file.toAbsolutePath())).thenReturn(null);
            var response=controller().getAvatar("avatar.unknown"); assertEquals(200,response.getStatusCode().value());
            assertEquals(MediaType.APPLICATION_OCTET_STREAM,response.getHeaders().getContentType());
        }
    }
    @Test void probingFailureReturns500() throws Exception {
        Path file=dir.resolve("avatar.png"); Files.write(file,new byte[]{1,2,3});
        try(MockedStatic<Files> files=mockStatic(Files.class,CALLS_REAL_METHODS)) {
            files.when(()->Files.probeContentType(file.toAbsolutePath())).thenThrow(new java.io.IOException("probe error"));
            assertEquals(500,controller().getAvatar("avatar.png").getStatusCode().value());
        }
    }
    @Test void traversalRejected() throws Exception {
        Path upload=Files.createDirectory(dir.resolve("uploads")); Files.write(dir.resolve("outside.png"),new byte[]{1});
        UserController controller=controller(); ReflectionTestUtils.setField(controller,"uploadPath",upload.toString());
        assertEquals(400,controller.getAvatar("../outside.png").getStatusCode().value());
    }
    @Test void absolutePathRejected() throws Exception {
        Path upload=Files.createDirectory(dir.resolve("uploads")); Path outside=dir.resolve("outside.png"); Files.write(outside,new byte[]{1});
        UserController controller=controller(); ReflectionTestUtils.setField(controller,"uploadPath",upload.toString());
        assertEquals(400,controller.getAvatar(outside.toString()).getStatusCode().value());
    }
}

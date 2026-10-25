package com.mouse.mes.service;

import com.mouse.mes.config.RolePermissions;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.entity.User;
import com.mouse.mes.mapper.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceUnitTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final UserMapper users=mock(UserMapper.class);
    private final AccountService service=new AccountService(db,users,mock(RoleMapper.class),mock(DepartmentMapper.class),mock(RoleAccessService.class));
    private User user() {User user=new User();user.setId(17L);user.setPassword("old-password");when(users.findById(17L)).thenReturn(user);return user;}
    @Test void rejectsInvalidProfileWithoutWriting() {
        assertThrows(IllegalArgumentException.class,()->service.updateProfile(17,"", "13800138001","test@example.com",1));
        assertThrows(IllegalArgumentException.class,()->service.updateProfile(17,"姓名", "12","test@example.com",1));
        assertThrows(IllegalArgumentException.class,()->service.updateProfile(17,"姓名", "13800138001","bad-email",1));
        assertThrows(IllegalArgumentException.class,()->service.updateProfile(17,"姓名", "13800138001","test@example.com",9));
        verifyNoInteractions(db,users);
    }
    @Test void wrongOldPasswordAndMismatchedConfirmationCannotChangeCredentials() {
        user();assertThrows(IllegalArgumentException.class,()->service.changePassword(17,"wrong","new-password","new-password"));
        assertThrows(IllegalArgumentException.class,()->service.changePassword(17,"old-password","new-password","different"));
        assertThrows(IllegalArgumentException.class,()->service.changePassword(17,"old-password","old-password","old-password"));verifyNoInteractions(db);
    }
    @Test void passwordChangeAtomicallyChecksOldPasswordAndBumpsSessionVersion() {
        user();String query="UPDATE t_user SET password=?,update_time=NOW() WHERE id=? AND password=? AND deleted=0";
        when(db.update(query,"new-password",17L,"old-password")).thenReturn(1);
        service.changePassword(17,"old-password","new-password","new-password");
        verify(db).update("INSERT INTO mm_account_settings(user_id,session_version) VALUES(?,1) ON DUPLICATE KEY UPDATE session_version=session_version+1",17L);
    }
    @Test void concurrentPasswordChangeDoesNotReportSuccess() {
        user();assertThrows(IllegalArgumentException.class,()->service.changePassword(17,"old-password","new-password","new-password"));
        verify(db,never()).update(startsWith("INSERT INTO mm_account_settings"),any(Object[].class));
    }
    @Test void oldSessionsAreRejectedButPreexistingSessionsRemainValidBeforeChange() {
        LoginResponse identity=new LoginResponse();identity.setId(17L);
        when(db.queryForList("SELECT session_version FROM mm_account_settings WHERE user_id=?",17L)).thenReturn(List.of());
        assertTrue(service.validSession(identity));
        when(db.queryForList("SELECT session_version FROM mm_account_settings WHERE user_id=?",17L)).thenReturn(List.of(Map.of("session_version",1L)));
        assertFalse(service.validSession(identity));identity.setSessionVersion(1L);assertTrue(service.validSession(identity));
    }
    @Test void workerCanUseSelfServiceButCannotWriteEmployeeAdministration() {
        assertTrue(RolePermissions.canRead(3,"/api/account/profile"));assertTrue(RolePermissions.canWrite(3,"/api/account/profile","PUT"));
        assertFalse(RolePermissions.canWrite(3,"/api/users/18","PUT"));
    }
    @Test void disabledProtectionDoesNotCreateAlerts() {service.recordLogin(17L,"127.0.0.1",0);verify(db,never()).update(startsWith("INSERT INTO mm_account_alert"),any(Object[].class));}
    @Test void failedLoginCreatesPrivateAlertWhenEnabled() {
        when(db.queryForList("SELECT login_protection FROM mm_account_settings WHERE user_id=?",17L)).thenReturn(List.of(Map.of("login_protection",1)));
        service.recordLogin(17L,"127.0.0.1",0);
        verify(db).update(eq("INSERT INTO mm_account_alert(user_id,content) VALUES(?,?)"),eq(17L),contains("登录失败"));
    }
    @Test void invalidOrOversizedAvatarNeverWrites(@TempDir Path dir) throws Exception {
        var storage=new AvatarStorageService(users,dir.toString());
        assertThrows(IllegalArgumentException.class,()->storage.upload(17,new MockMultipartFile("file","fake.png","image/png","bad-image".getBytes())));
        assertThrows(IllegalArgumentException.class,()->storage.upload(17,new MockMultipartFile("file","large.png","image/png",new byte[2*1024*1024+1])));
        verifyNoInteractions(users);try(var files=Files.list(dir)){assertEquals(0,files.count());}
    }
    @Test void validAvatarIsNormalizedAndStoredAndFailedDatabaseWriteLeavesNoFile(@TempDir Path dir) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(40,20,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var file=new MockMultipartFile("file","avatar.PNG","image/png",bytes.toByteArray());
        var storage=new AvatarStorageService(users,dir.toString());when(users.updateAvatar(eq(17L),anyString())).thenReturn(1);
        String url=storage.upload(17,file);assertTrue(url.startsWith("/api/users/avatar/"));
        var stored=ImageIO.read(dir.resolve(url.substring(url.lastIndexOf('/')+1)).toFile());assertEquals(256,stored.getWidth());assertEquals(256,stored.getHeight());
        assertThrows(IllegalArgumentException.class,()->storage.upload(18,file));try(var files=Files.list(dir)){assertEquals(1,files.count());}
    }
}

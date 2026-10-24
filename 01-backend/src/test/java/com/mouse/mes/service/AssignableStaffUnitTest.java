package com.mouse.mes.service;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AssignableStaffUnitTest {
 @Test void selectorUsesEnabledRolesAndActiveEmployeesIncludingCustomRoleProfiles(){
  JdbcTemplate db=mock(JdbcTemplate.class);var service=new AssignableStaffService(db);
  var eligible=List.<Map<String,Object>>of(Map.of("id",12L,"name","在职维护员"));
  when(db.queryForList(anyString(),eq(8))).thenReturn(eligible);
  assertEquals(12L,service.require(12,8).get("id"));
  verify(db).queryForList(argThat((String q)->q.contains("u.deleted=0")&&q.contains("u.status=1")&&q.contains("r.status=1")&&q.contains("COALESCE(p.base_role_id,r.id)=?")),eq(8));
 }
 @Test void absentInactiveOrWrongRoleEmployeeCannotBeAssigned(){
  JdbcTemplate db=mock(JdbcTemplate.class);var service=new AssignableStaffService(db);
  when(db.queryForList(anyString(),eq(8))).thenReturn(List.of());
  assertTrue(assertThrows(IllegalArgumentException.class,()->service.require(99,8)).getMessage().contains("设备维护员"));
 }
 @Test void unsupportedRoleDoesNotQueryDatabase(){
  JdbcTemplate db=mock(JdbcTemplate.class);assertThrows(IllegalArgumentException.class,()->new AssignableStaffService(db).list(9));verifyNoInteractions(db);
 }
}

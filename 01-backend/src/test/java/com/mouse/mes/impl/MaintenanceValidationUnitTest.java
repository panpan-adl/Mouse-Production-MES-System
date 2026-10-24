package com.mouse.mes.impl;
import com.mouse.mes.entity.Maintenance;
import com.mouse.mes.entity.User;
import com.mouse.mes.mapper.MaintenanceMapper;
import com.mouse.mes.mapper.UserMapper;
import com.mouse.mes.service.RoleAccessService;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MaintenanceValidationUnitTest {
 private Maintenance valid(){var m=new Maintenance();m.setMaintenanceNo("TEST-VALIDATION");m.setEquipmentId(1L);m.setMaintenanceType(2);m.setFaultDesc("设备异响");return m;}
 @Test void invalidDescriptionIsRejectedBeforeAnyPersistence(){var service=new MaintenanceServiceImpl();var mapper=mock(MaintenanceMapper.class);ReflectionTestUtils.setField(service,"maintenanceMapper",mapper);var m=valid();m.setFaultDesc("   ");assertTrue(assertThrows(IllegalArgumentException.class,()->service.create(m)).getMessage().contains("描述不能为空"));verifyNoInteractions(mapper);}
 @Test void impossibleDatesAreRejectedBeforePersistence(){var service=new MaintenanceServiceImpl();var m=valid();m.setStartTime(new Date(2000));m.setEndTime(new Date(1000));assertTrue(assertThrows(IllegalArgumentException.class,()->service.create(m)).getMessage().contains("结束时间"));}
 @Test void negativeCostIsRejectedBeforePersistence(){var service=new MaintenanceServiceImpl();var m=valid();m.setCost(new BigDecimal("-1"));assertTrue(assertThrows(IllegalArgumentException.class,()->service.create(m)).getMessage().contains("费用不能为负数"));}
 @Test void forgedAssigneeCannotCreateMaintenance(){var service=new MaintenanceServiceImpl();var users=mock(UserMapper.class);var roles=mock(RoleAccessService.class);var mapper=mock(MaintenanceMapper.class);ReflectionTestUtils.setField(service,"userMapper",users);ReflectionTestUtils.setField(service,"roleAccess",roles);ReflectionTestUtils.setField(service,"maintenanceMapper",mapper);var user=new User();user.setId(1L);user.setStatus(1);user.setRoleId(3L);when(users.findById(1L)).thenReturn(user);when(roles.effective(3L)).thenReturn(3L);var m=valid();m.setMaintenanceUserId(1L);assertTrue(assertThrows(IllegalArgumentException.class,()->service.create(m)).getMessage().contains("设备维护员"));verifyNoInteractions(mapper);}
}

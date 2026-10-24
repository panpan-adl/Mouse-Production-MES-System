package com.mouse.mes.service;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PeriodicMaintenanceTimeTest {
 @Test void acceptsBothJdbcTimeRepresentations(){var t=LocalDateTime.of(2026,10,24,8,0);assertEquals(t,PeriodicMaintenanceService.asTime(t));assertEquals(t,PeriodicMaintenanceService.asTime(Timestamp.valueOf(t)));}
}

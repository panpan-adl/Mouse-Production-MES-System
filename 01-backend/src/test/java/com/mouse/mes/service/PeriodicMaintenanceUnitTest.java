package com.mouse.mes.service;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PeriodicMaintenanceUnitTest {
 @Test void mysqlDatetimeSupportsBothJdbcRepresentations(){var date=LocalDateTime.of(2026,11,1,8,0);assertEquals(date,PeriodicMaintenanceService.dateTime(date));assertEquals(date,PeriodicMaintenanceService.dateTime(java.sql.Timestamp.valueOf(date)));}
 @Test void yearlyUsesAnchorAndRestoresLeapDay(){var anchor=LocalDateTime.of(2024,2,29,9,0);assertEquals(LocalDateTime.of(2025,2,28,9,0),PeriodicMaintenanceService.occurrence(anchor,1,1,"YEAR"));assertEquals(LocalDateTime.of(2028,2,29,9,0),PeriodicMaintenanceService.occurrence(anchor,4,1,"YEAR"));}
 @Test void monthlyDoesNotDriftAfterShortMonth(){var anchor=LocalDateTime.of(2026,1,31,8,0);assertEquals(LocalDateTime.of(2026,2,28,8,0),PeriodicMaintenanceService.occurrence(anchor,1,1,"MONTH"));assertEquals(LocalDateTime.of(2026,3,31,8,0),PeriodicMaintenanceService.occurrence(anchor,2,1,"MONTH"));}
 @Test void annualAndWeeklyCrossYear(){var anchor=LocalDateTime.of(2026,11,1,8,0);assertEquals(LocalDateTime.of(2027,11,1,8,0),PeriodicMaintenanceService.occurrence(anchor,1,1,"YEAR"));assertEquals(LocalDateTime.of(2027,1,10,8,0),PeriodicMaintenanceService.occurrence(anchor,5,2,"WEEK"));}
 @Test void unsupportedUnitAndOverflowAreRejected(){assertThrows(IllegalArgumentException.class,()->PeriodicMaintenanceService.occurrence(LocalDateTime.now(),1,1,"INVALID"));assertThrows(ArithmeticException.class,()->PeriodicMaintenanceService.occurrence(LocalDateTime.now(),Long.MAX_VALUE,2,"DAY"));}
}

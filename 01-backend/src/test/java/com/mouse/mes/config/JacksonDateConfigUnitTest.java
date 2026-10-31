package com.mouse.mes.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class JacksonDateConfigUnitTest {
 private ObjectMapper mapper(){SimpleModule module=new SimpleModule();module.addDeserializer(Date.class,new JacksonDateConfig.MultiFormatDateDeserializer());return new ObjectMapper().registerModule(module);}
 private Date parse(String input)throws Exception{return mapper().readValue("\""+input+"\"",Date.class);}
 @Test void preservesAttendanceOffsetClock()throws Exception{assertEquals(Instant.parse("2026-10-31T00:35:00Z"),parse("2026-10-31T08:35:00+08:00").toInstant());assertEquals(Instant.parse("2026-10-31T09:45:00Z"),parse("2026-10-31T17:45:00+08:00").toInstant());}
 @Test void equivalentOffsetsRepresentSameInstant()throws Exception{assertEquals(parse("2026-10-31T08:35:00+08:00"),parse("2026-10-31T00:35:00Z"));}
 @Test void supportsExistingChinaLocalFormats()throws Exception{assertEquals(parse("2026-10-31T08:35:00+08:00"),parse("2026-10-31 08:35:00"));assertEquals(Instant.parse("2026-10-30T16:00:00Z"),parse("2026-10-31").toInstant());}
 @Test void rejectsInvalidDatesAndTrailingGarbage(){assertThrows(Exception.class,()->parse("2026-02-30T08:35:00+08:00"));assertThrows(Exception.class,()->parse("2026-10-31 INVALID"));assertThrows(Exception.class,()->parse("2026-10-31 08:35:00 garbage"));}
}

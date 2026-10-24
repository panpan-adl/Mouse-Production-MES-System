package com.mouse.mes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SkillAgentServiceTest {
    JdbcTemplate db=mock(JdbcTemplate.class);
    IntelligenceService intelligence=mock(IntelligenceService.class);
    AgentModelGateway model=mock(AgentModelGateway.class);
    AutomationService automation=mock(AutomationService.class);
    SkillAgentService service;
    ObjectMapper json=new ObjectMapper();
    @BeforeEach void init() throws Exception {service=new SkillAgentService(db,json,intelligence,model,automation,mock(PlatformTransactionManager.class));}
    @Test void rolesOnlySeeGrantedSkills(){assertTrue(service.skills(3).isEmpty());assertEquals(List.of("maintenance"),service.skills(8).stream().map(s->s.get("id")).toList());assertEquals(5,service.skills(9).size());}
    @Test void localIntentsAreDistinct(){assertEquals("replenishment",SkillAgentService.localRoute("帮我补货"));assertEquals("maintenance",SkillAgentService.localRoute("查看保养任务"));assertEquals("shortage",SkillAgentService.localRoute("分析缺料"));assertEquals("",SkillAgentService.localRoute("发送邮件"));}
    @Test void unauthorizedSkillCannotCallTools(){assertThrows(SecurityException.class,()->service.message(Map.of("question","查库存","skillId","inventory"),3,8));verifyNoInteractions(intelligence,automation);}
    @Test void nonPurchaserCannotPrepareOrConfirm(){assertThrows(SecurityException.class,()->service.propose(1,1,5));assertThrows(SecurityException.class,()->service.confirm("x",1,5));verifyNoInteractions(db,automation);}
    @Test void foreignTaskCannotBeConfirmed(){when(db.queryForList(anyString(),eq("x"),eq(1L))).thenReturn(List.of());assertThrows(SecurityException.class,()->service.confirm("x",1,9));verifyNoInteractions(automation);}
    private Map<String,Object> run(String status,Object payload,LocalDateTime created) throws Exception {return new HashMap<>(Map.of("id","x","skill_id","replenishment","status",status,"payload",json.writeValueAsString(payload),"created_at",created,"result","{}"));}
    private Map<String,Object> material(int quantity){return new HashMap<>(Map.of("material_id",1L,"material_name","测试物料","current_quantity",new BigDecimal(quantity),"suggested_quantity",new BigDecimal(10-quantity),"status","SUGGESTED"));}
    @Test void expiredProposalDoesNotWriteBusiness() throws Exception {
        var run=run("PENDING",material(0),LocalDateTime.now().minusMinutes(11));when(db.queryForList(anyString(),eq("x"),eq(1L))).thenReturn(List.of(run));
        service.confirm("x",1,9);verifyNoInteractions(automation);verify(db).update(contains("SET status=?"),eq("EXPIRED"),anyString(),eq("x"));
    }
    @Test void changedInventoryRequiresFreshApproval() throws Exception {
        var run=run("PENDING",material(0),LocalDateTime.now());when(db.queryForList(anyString(),eq("x"),eq(1L))).thenReturn(List.of(run));when(automation.suggestions()).thenReturn(List.of(material(2)));
        service.confirm("x",1,9);verify(automation,never()).confirm(anyLong(),anyLong());verify(db).update(contains("SET status=?"),eq("STALE"),anyString(),eq("x"));
    }
    @Test void alreadySucceededIsIdempotent() throws Exception {
        var run=run("SUCCEEDED",material(0),LocalDateTime.now());when(db.queryForList(anyString(),eq("x"),eq(1L))).thenReturn(List.of(run));service.confirm("x",1,9);verifyNoInteractions(automation);
    }
    @Test void confirmedOtherProposalPreventsDuplicate() throws Exception {
        var run=run("PENDING",material(0),LocalDateTime.now());var current=material(0);current.put("status","CONFIRMED");when(db.queryForList(anyString(),eq("x"),eq(1L))).thenReturn(List.of(run));when(automation.suggestions()).thenReturn(List.of(current));service.confirm("x",1,9);verify(automation,never()).confirm(anyLong(),anyLong());
    }
    @Test void validProposalUsesExistingBusinessServiceAndAudit() throws Exception {
        var run=run("PENDING",material(0),LocalDateTime.now());when(db.queryForList(anyString(),eq("x"),eq(1L))).thenReturn(List.of(run));when(automation.suggestions()).thenReturn(List.of(material(0)));service.confirm("x",1,9);verify(automation).confirm(1L,1L);verify(db).update(contains("agent:replenishment"),eq(1L),eq(1L));
    }
}

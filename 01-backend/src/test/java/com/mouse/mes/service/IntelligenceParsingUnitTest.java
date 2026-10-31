package com.mouse.mes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class IntelligenceParsingUnitTest {
    @SuppressWarnings("unchecked")
    private Map<String,Object> parse(String question,Map<String,String> previous) throws Exception {
        var db=mock(JdbcTemplate.class);
        when(db.queryForList(anyString())).thenReturn(List.of());
        var service=new IntelligenceService(db,new ObjectMapper(),mock(BusinessQueryService.class),mock(PlatformTransactionManager.class));
        var method=IntelligenceService.class.getDeclaredMethod("local",String.class,Map.class);
        method.setAccessible(true);
        return (Map<String,Object>)method.invoke(service,question,previous);
    }
    @Test void monthSpokenInChineseAndRawMaterialAlias() throws Exception {
        var result=parse("盘点十月份原料库存",Map.of());
        assertEquals("INVENTORY",result.get("kind"));assertEquals("1",result.get("type"));
        assertEquals(LocalDate.now().getYear()+"-10-01",result.get("from"));
    }
    @Test void chineseDayRangeOverridesWholeMonth() throws Exception {
        var result=parse("查询2026年10月1日至2026年10月15日库存",Map.of());
        assertEquals("2026-10-01",result.get("from"));assertEquals("2026-10-15",result.get("to"));
    }
    @Test void recentDaysAreInclusive() throws Exception {
        var result=parse("最近7天库存",Map.of());
        assertEquals(LocalDate.now().minusDays(6).toString(),result.get("from"));
        assertEquals(LocalDate.now().toString(),result.get("to"));
    }
    @Test void followupReplacesMonthWithoutInventingMaterial() throws Exception {
        var result=parse("改查上月",Map.of("kind","INVENTORY","material","42"));
        assertEquals(LocalDate.now().minusMonths(1).withDayOfMonth(1).toString(),result.get("from"));
        assertFalse(result.containsKey("material"));assertFalse(result.containsKey("clarification"));
    }
    @Test void explicitNameDoesNotAbsorbNaturalLanguageSuffix() throws Exception {
        assertEquals("验收防滑脚贴",parse("查询本月物料：验收防滑脚贴的出入库",Map.of()).get("material"));
    }
    @Test void warehouseNameDoesNotOverrideFinishedGoodsCategory() throws Exception {
        assertEquals("3",parse("成品库存，只看A100原料仓",Map.of()).get("type"));
    }
    @Test void unknownNamedMaterialIsResolvedRatherThanSilentlyQueryingEverything() throws Exception {
        assertEquals("螺纹钢",parse("库存查询：请帮我盘点一下10月份的螺纹钢库存情况",Map.of()).get("material"));
        assertFalse(parse("查询本月原材料库存",Map.of()).containsKey("material"));
    }
    @Test void unsupportedQuestionRequiresClarification() throws Exception {
        assertTrue(parse("预测明年汽车销量",Map.of()).containsKey("clarification"));
    }
    @Test void categoryAndStocktakeWordingsSelectRealTools() {
        assertEquals("shortage",SkillAgentService.localRoute("哪些原料库存不足"));
        assertEquals("inventory",SkillAgentService.localRoute("盘点十月份的余额"));
    }
}

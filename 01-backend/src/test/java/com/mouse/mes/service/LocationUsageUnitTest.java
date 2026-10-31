package com.mouse.mes.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LocationUsageUnitTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final FulfillmentService fulfillment = mock(FulfillmentService.class);
    private final LocationUsageService service = new LocationUsageService(db, fulfillment);

    private void existingLocation() {
        when(db.queryForList(anyString(), eq(42L))).thenReturn(List.of(Map.of("id", 42L)));
        when(db.queryForObject(anyString(), eq(Long.class), eq(42L))).thenReturn(0L);
    }

    @Test void unusedLocationCanBeDeleted() {
        existingLocation();
        assertDoesNotThrow(() -> service.requireUnused(42L));
        verify(fulfillment).lock();
    }

    @ParameterizedTest
    @ValueSource(strings = {"t_inventory", "t_inventory_transaction", "t_stock_in_item", "t_stock_out_item",
        "t_material_issue_item", "mm_document_line", "mm_stock_posting"})
    void anyBusinessReferenceProtectsLocationEvenWhenBalanceIsZero(String table) {
        existingLocation();
        when(db.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE location_id=?", Long.class, 42L)).thenReturn(1L);
        var error = assertThrows(IllegalArgumentException.class, () -> service.requireUnused(42L));
        assertTrue(error.getMessage().contains("不能删除"));
    }

    @Test void missingOrAlreadyDeletedLocationIsRejected() {
        when(db.queryForList(anyString(), eq(42L))).thenReturn(List.of());
        assertEquals("库位不存在", assertThrows(IllegalArgumentException.class,
            () -> service.requireUnused(42L)).getMessage());
    }
}

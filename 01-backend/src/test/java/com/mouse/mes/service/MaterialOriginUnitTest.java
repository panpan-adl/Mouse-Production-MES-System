package com.mouse.mes.service;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialOriginUnitTest {
    @Test void importedStockCannotBeCertifiedAsEmptyHistory() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject("SELECT COUNT(*) FROM t_inventory WHERE material_id=?",Integer.class,42L)).thenReturn(1);
        assertThrows(IllegalArgumentException.class,()->new MaterialOriginService(db).created(42));
        verify(db,never()).update(anyString(),eq(42L));
    }
    @Test void zeroBalanceWithPreviousMovementsCannotBeCertifiedAsEmptyHistory() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject("SELECT COUNT(*) FROM t_inventory WHERE material_id=?",Integer.class,42L)).thenReturn(0);
        when(db.queryForObject("SELECT COUNT(*) FROM t_inventory_transaction WHERE material_id=?",Integer.class,42L)).thenReturn(2);
        assertThrows(IllegalArgumentException.class,()->new MaterialOriginService(db).created(42));
        verify(db,never()).update(anyString(),eq(42L));
    }
    @Test void newlyCreatedEmptyMaterialRecordsItsActualCreationTime() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(Integer.class),eq(42L))).thenReturn(0);
        new MaterialOriginService(db).created(42);
        verify(db).update("INSERT INTO mm_material_origin(material_id,created_at,ledger_id) SELECT ?,NOW(6),COALESCE(MAX(id),0) FROM t_inventory_transaction",42L);
    }
}

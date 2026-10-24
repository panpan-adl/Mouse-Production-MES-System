package com.mouse.mes.unit;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.InventoryTransaction;
import com.mouse.mes.impl.InventoryServiceImpl;
import com.mouse.mes.mapper.InventoryMapper;
import com.mouse.mes.mapper.InventoryTransactionMapper;
import com.mouse.mes.mapper.MaterialMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Isolated inventory business tests; never connect to a database. */
@ExtendWith(MockitoExtension.class)
class InventoryServiceUnitTest {
    @Mock InventoryMapper mapper;
    @Mock InventoryTransactionMapper transactions;
    @Mock MaterialMapper materials;
    @InjectMocks InventoryServiceImpl service;

    private Inventory stock(BigDecimal quantity) {
        Inventory inv = new Inventory(); inv.setId(10L); inv.setMaterialId(1L);
        inv.setWarehouseId(2L); inv.setQuantity(quantity); inv.setIsDeleted(0); return inv;
    }
    private Inventory change(boolean increase, Long material, Long warehouse, BigDecimal quantity) {
        return increase ? service.increaseStock(material, warehouse, null, null, quantity, "OTHER", 5L, "ORDER-1", 7L, "test")
                : service.decreaseStock(material, warehouse, null, null, quantity, "OTHER", 5L, "ORDER-1", 7L, "test");
    }
    static Stream<Arguments> invalidChanges() {
        return Stream.of(true, false).flatMap(in -> Stream.of(
            Arguments.of(in, null, 2L, BigDecimal.ONE), Arguments.of(in, 1L, null, BigDecimal.ONE),
            Arguments.of(in, 1L, 2L, null), Arguments.of(in, 1L, 2L, BigDecimal.ZERO),
            Arguments.of(in, 1L, 2L, new BigDecimal("-1"))));
    }
    @ParameterizedTest @MethodSource("invalidChanges")
    void rejectInvalidChanges(boolean in, Long material, Long warehouse, BigDecimal quantity) {
        assertThrows(IllegalArgumentException.class, () -> change(in, material, warehouse, quantity));
        verifyNoInteractions(mapper, transactions, materials);
    }
    @Test void availableMissing() { assertEquals(BigDecimal.ZERO, service.getAvailableQuantity(1L,2L,null,null)); }
    @Test void availableNullQuantity() { when(mapper.findOne(1L,2L,null,null)).thenReturn(stock(null)); assertEquals(BigDecimal.ZERO,service.getAvailableQuantity(1L,2L,null,null)); }
    @Test void availableExisting() { when(mapper.findOne(1L,2L,null,null)).thenReturn(stock(BigDecimal.TEN)); assertEquals(BigDecimal.TEN,service.getAvailableQuantity(1L,2L,null,null)); }
    @Test void increaseCreatesInventory() {
        when(mapper.insert(any())).thenAnswer(call -> { ((Inventory)call.getArgument(0)).setId(10L); return 1; });
        Inventory saved=stock(BigDecimal.TEN); when(mapper.findById(10L)).thenReturn(saved);
        assertSame(saved,change(true,1L,2L,BigDecimal.TEN));
        ArgumentCaptor<Inventory> inv=ArgumentCaptor.forClass(Inventory.class); verify(mapper).insert(inv.capture());
        assertEquals(BigDecimal.TEN,inv.getValue().getQuantity()); assertEquals(0,inv.getValue().getIsDeleted());
        assertNotNull(inv.getValue().getLastUpdateTime()); verify(materials).updateStock(1L,0);
        assertTransaction("IN",BigDecimal.ZERO,BigDecimal.TEN,BigDecimal.TEN);
    }
    @Test void increaseExisting() {
        Inventory inv=stock(BigDecimal.TEN); when(mapper.findOne(1L,2L,null,null)).thenReturn(inv);
        when(mapper.findById(10L)).thenReturn(inv); when(mapper.sumQuantityByMaterialId(1L)).thenReturn(new BigDecimal("12"));
        assertSame(inv,change(true,1L,2L,new BigDecimal("2"))); assertEquals(new BigDecimal("12"),inv.getQuantity());
        verify(mapper).update(inv); verify(mapper,never()).insert(any()); verify(materials).updateStock(1L,12);
        assertTransaction("IN",BigDecimal.TEN,new BigDecimal("12"),new BigDecimal("2"));
    }
    @Test void increaseNullQuantity() {
        Inventory inv=stock(null); when(mapper.findOne(1L,2L,null,null)).thenReturn(inv);
        change(true,1L,2L,BigDecimal.ONE); assertEquals(BigDecimal.ONE,inv.getQuantity());
    }
    @Test void decreaseSuccess() { decreaseSuccess(new BigDecimal("3"),new BigDecimal("7")); }
    @Test void decreaseExactBoundary() { decreaseSuccess(BigDecimal.TEN,BigDecimal.ZERO); }
    private void decreaseSuccess(BigDecimal amount, BigDecimal after) {
        Inventory inv=stock(BigDecimal.TEN); when(mapper.findOne(1L,2L,null,null)).thenReturn(inv); when(mapper.findById(10L)).thenReturn(inv);
        assertSame(inv,change(false,1L,2L,amount)); assertEquals(0,after.compareTo(inv.getQuantity()));
        verify(mapper).update(inv); assertTransaction("OUT",BigDecimal.TEN,after,amount);
    }
    @Test void decreaseInsufficient() { rejectDecrease(stock(new BigDecimal("2"))); }
    @Test void decreaseMissing() { rejectDecrease(null); }
    @Test void decreaseNullQuantity() { rejectDecrease(stock(null)); }
    private void rejectDecrease(Inventory inv) {
        when(mapper.findOne(1L,2L,null,null)).thenReturn(inv);
        assertEquals("库存不足，不能出库", assertThrows(IllegalArgumentException.class, () -> change(false,1L,2L,new BigDecimal("3"))).getMessage());
        verify(mapper,never()).update(any()); verifyNoInteractions(transactions,materials);
    }
    private void assertTransaction(String type,BigDecimal before,BigDecimal after,BigDecimal amount) {
        ArgumentCaptor<InventoryTransaction> captor=ArgumentCaptor.forClass(InventoryTransaction.class); verify(transactions).insert(captor.capture());
        InventoryTransaction tx=captor.getValue(); assertEquals(type,tx.getTransactionType());
        assertEquals(0,before.compareTo(tx.getBeforeQuantity())); assertEquals(0,after.compareTo(tx.getAfterQuantity()));
        assertEquals(amount,tx.getQuantity()); assertEquals(1L,tx.getMaterialId()); assertEquals(2L,tx.getWarehouseId());
        assertEquals("OTHER",tx.getBusinessType()); assertEquals(5L,tx.getSourceOrderId()); assertEquals("ORDER-1",tx.getSourceOrderNo());
        assertEquals(7L,tx.getCreatedBy()); assertEquals("test",tx.getRemark()); assertNotNull(tx.getCreatedAt());
        assertTrue(tx.getTransactionNo().startsWith("INV-"+type+"-"));
    }
    @Test void createDefaultQuantity() {
        Inventory inv=stock(null); when(mapper.findById(10L)).thenReturn(inv);
        assertSame(inv,service.create(inv)); assertEquals(BigDecimal.ZERO,inv.getQuantity()); verify(mapper).insert(inv);
    }
    @Test void createPositiveQuantity() { Inventory inv=stock(BigDecimal.TEN); service.create(inv); assertEquals(BigDecimal.TEN,inv.getQuantity()); }
    @Test void createNullRejected() { assertThrows(IllegalArgumentException.class,()->service.create(null)); verifyNoInteractions(mapper,materials); }
    @Test void createNegativeRejected() { assertThrows(IllegalArgumentException.class,()->service.create(stock(new BigDecimal("-1")))); verifyNoInteractions(mapper,materials); }
    @Test void updateNegativeRejected() { assertThrows(IllegalArgumentException.class,()->service.update(stock(new BigDecimal("-1")))); verifyNoInteractions(mapper,materials); }
    @Test void updateNullRejected() { assertThrows(IllegalArgumentException.class,()->service.update(null)); verifyNoInteractions(mapper,materials); }
    @Test void updateQuantity() {
        Inventory inv=stock(BigDecimal.TEN); when(mapper.findById(10L)).thenReturn(inv);
        assertSame(inv,service.update(inv)); verify(mapper).update(inv); assertNotNull(inv.getLastUpdateTime());
    }
    @Test void updateNullQuantityRejected() { assertThrows(IllegalArgumentException.class,()->service.update(stock(null))); verifyNoInteractions(mapper,materials); }
    @Test void deleteMissing() { service.delete(10L); verify(mapper,never()).update(any()); verifyNoInteractions(materials); }
    @Test void deleteExisting() { Inventory inv=stock(BigDecimal.TEN); when(mapper.findById(10L)).thenReturn(inv); service.delete(10L); assertEquals(1,inv.getIsDeleted()); verify(mapper).update(inv); verify(materials).updateStock(1L,0); }
    @Test void materialAbsentSkipsRefresh() { Inventory inv=stock(BigDecimal.TEN); inv.setMaterialId(null); service.create(inv); verifyNoInteractions(materials); }
    @Test void queryDelegation() {
        List<Inventory> rows=List.of(stock(BigDecimal.ONE)); when(mapper.findAll()).thenReturn(rows); when(mapper.findById(10L)).thenReturn(rows.get(0));
        when(mapper.findByWarehouseId(2L)).thenReturn(rows); when(mapper.findByMaterialId(1L)).thenReturn(rows); when(mapper.search(1L,2L,3L)).thenReturn(rows); when(mapper.findLowStock()).thenReturn(rows);
        List<InventoryTransaction> tx=List.of(new InventoryTransaction()); when(transactions.findAll()).thenReturn(tx);
        assertSame(rows,service.findAll()); assertSame(rows.get(0),service.findById(10L)); assertSame(rows,service.findByWarehouseId(2L));
        assertSame(rows,service.findByMaterialId(1L)); assertSame(rows,service.search(1L,2L,3L)); assertSame(rows,service.findLowStock()); assertSame(tx,service.findTransactions());
    }
    @Test void mapperFailureStopsFlow() {
        when(mapper.insert(any())).thenThrow(new IllegalStateException("database error"));
        assertThrows(IllegalStateException.class,()->change(true,1L,2L,BigDecimal.ONE)); verifyNoInteractions(transactions,materials);
    }
}

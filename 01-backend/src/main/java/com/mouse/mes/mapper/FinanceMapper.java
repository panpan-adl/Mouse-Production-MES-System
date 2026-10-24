package com.mouse.mes.mapper;

import com.mouse.mes.dto.FinanceGroupStat;
import com.mouse.mes.dto.FinanceTrendStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Mapper
public interface FinanceMapper {
    /**
     * 按可选时间范围统计销售收入。
     */
    BigDecimal sumSalesIncome(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按可选时间范围统计采购支出。
     */
    BigDecimal sumPurchaseExpense(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按可选时间范围统计维修支出。
     */
    BigDecimal sumMaintenanceExpense(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 统计当前库存总数量。
     */
    BigDecimal sumInventoryQuantity();

    /**
     * 统计当前库存金额。
     */
    BigDecimal sumInventoryAmount();

    /**
     * 统计低库存物料数量。
     */
    Integer countLowStockMaterial();

    /**
     * 统计质检不合格数量。
     */
    BigDecimal sumQcDefectiveQuantity();

    /**
     * 统计质检报废数量。
     */
    BigDecimal sumQcScrapQuantity();

    /**
     * 统计质检返工数量。
     */
    BigDecimal sumQcReworkQuantity();

    /**
     * 按月份统计采购金额。
     */
    List<FinanceGroupStat> sumPurchaseAmountByMonth();

    /**
     * 按供应商统计采购金额。
     */
    List<FinanceGroupStat> sumPurchaseAmountBySupplier();

    /**
     * 按物料统计采购金额。
     */
    List<FinanceGroupStat> sumPurchaseAmountByMaterial();

    /**
     * 查询月度收入、采购支出、维修支出和利润趋势。
     */
    List<FinanceTrendStat> findMonthlyTrend();

    /**
     * 按客户统计销售收入。
     */
    List<FinanceGroupStat> sumSalesIncomeByCustomer();

    /**
     * 按设备统计维修成本。
     */
    List<FinanceGroupStat> sumMaintenanceCostByEquipment();
}

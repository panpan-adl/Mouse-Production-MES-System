package com.mouse.mes.impl;

import com.mouse.mes.dto.FinanceGroupStat;
import com.mouse.mes.dto.FinanceInventoryValueStat;
import com.mouse.mes.dto.FinanceOverview;
import com.mouse.mes.dto.FinanceQualityLossStat;
import com.mouse.mes.dto.FinanceTrendStat;
import com.mouse.mes.mapper.FinanceMapper;
import com.mouse.mes.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

@Service
public class FinanceServiceImpl implements FinanceService {
    /**
     * 财务统计数据库访问对象，用于从采购、销售、库存、质检和维修表中汇总数据。
     */
    @Autowired
    private FinanceMapper financeMapper;

    /**
     * 月份参数格式，前端传入yyyy-MM时使用。
     */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 查询全部数据范围的财务总览。
     */
    @Override
    public FinanceOverview getOverview() {
        return buildOverview(null, null, true);
    }

    /**
     * 按月份查询财务总览，月份格式必须是yyyy-MM。
     */
    @Override
    public FinanceOverview getOverviewByMonth(String month) {
        DateRange dateRange = parseMonth(month);
        return buildOverview(dateRange.getStartTime(), dateRange.getEndTime(), false);
    }

    /**
     * 按月份统计采购金额。
     */
    @Override
    public List<FinanceGroupStat> getPurchaseAmountByMonth() {
        return financeMapper.sumPurchaseAmountByMonth();
    }

    /**
     * 按供应商统计采购金额。
     */
    @Override
    public List<FinanceGroupStat> getPurchaseAmountBySupplier() {
        return financeMapper.sumPurchaseAmountBySupplier();
    }

    /**
     * 按物料统计采购金额。
     */
    @Override
    public List<FinanceGroupStat> getPurchaseAmountByMaterial() {
        return financeMapper.sumPurchaseAmountByMaterial();
    }

    /**
     * 查询库存数量、库存金额和低库存物料数量。
     */
    @Override
    public FinanceInventoryValueStat getInventoryValueStat() {
        FinanceInventoryValueStat stat = new FinanceInventoryValueStat();
        stat.setCurrentQuantity(toDouble(financeMapper.sumInventoryQuantity()));
        stat.setCurrentAmount(toDouble(financeMapper.sumInventoryAmount()));
        stat.setLowStockCount(safeInteger(financeMapper.countLowStockMaterial()));
        return stat;
    }

    /**
     * 查询质检损失统计。
     */
    @Override
    public FinanceQualityLossStat getQualityLossStat() {
        FinanceQualityLossStat stat = new FinanceQualityLossStat();
        stat.setDefectiveQuantity(toDouble(financeMapper.sumQcDefectiveQuantity()));
        stat.setScrapQuantity(toDouble(financeMapper.sumQcScrapQuantity()));
        stat.setReworkQuantity(toDouble(financeMapper.sumQcReworkQuantity()));
        return stat;
    }

    /**
     * 查询月度趋势数据。
     */
    @Override
    public List<FinanceTrendStat> getMonthlyTrend() {
        return financeMapper.findMonthlyTrend();
    }

    /**
     * 按客户统计销售收入。
     */
    @Override
    public List<FinanceGroupStat> getSalesIncomeByCustomer() {
        return financeMapper.sumSalesIncomeByCustomer();
    }

    /**
     * 按设备统计维修成本。
     */
    @Override
    public List<FinanceGroupStat> getMaintenanceCostByEquipment() {
        return financeMapper.sumMaintenanceCostByEquipment();
    }

    /**
     * 根据时间范围组装财务总览数据。
     */
    private FinanceOverview buildOverview(Date startTime, Date endTime, boolean includeInventoryAmount) {
        BigDecimal salesIncome = safe(financeMapper.sumSalesIncome(startTime, endTime));
        BigDecimal purchaseExpense = safe(financeMapper.sumPurchaseExpense(startTime, endTime));
        BigDecimal maintenanceExpense = safe(financeMapper.sumMaintenanceExpense(startTime, endTime));
        BigDecimal inventoryAmount = includeInventoryAmount ? safe(financeMapper.sumInventoryAmount()) : BigDecimal.ZERO;

        FinanceOverview overview = new FinanceOverview();
        overview.setSalesIncome(toDouble(salesIncome));
        overview.setPurchaseExpense(toDouble(purchaseExpense));
        overview.setMaintenanceExpense(toDouble(maintenanceExpense));
        overview.setInventoryAmount(toDouble(inventoryAmount));
        overview.setGrossProfit(toDouble(salesIncome.subtract(purchaseExpense).subtract(maintenanceExpense)));
        return overview;
    }

    /**
     * 将yyyy-MM格式的月份解析为开始时间和结束时间。
     */
    private DateRange parseMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("月份不能为空");
        }
        try {
            LocalDate startDate = LocalDate.parse(month + "-01", MONTH_FORMATTER);
            LocalDate endDate = startDate.plusMonths(1);
            return new DateRange(toDate(startDate), toDate(endDate));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("月份格式必须为yyyy-MM");
        }
    }

    /**
     * 将LocalDate转换为数据库查询用的Date。
     */
    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将可能为null的金额转换为0。
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 将可能为null的整数转换为0。
     */
    private Integer safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将BigDecimal转换为Double，null转换为0.0。
     */
    private Double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    /**
     * 月份查询的时间范围。
     */
    private static class DateRange {
        /**
         * 月份开始时间，包含该时间。
         */
        private final Date startTime;

        /**
         * 下个月开始时间，不包含该时间。
         */
        private final Date endTime;

        /**
         * 创建月份查询时间范围。
         */
        private DateRange(Date startTime, Date endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        /**
         * 获取月份开始时间。
         */
        private Date getStartTime() {
            return startTime;
        }

        /**
         * 获取下个月开始时间。
         */
        private Date getEndTime() {
            return endTime;
        }
    }
}

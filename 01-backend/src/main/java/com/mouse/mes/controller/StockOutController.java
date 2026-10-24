package com.mouse.mes.controller;

import com.mouse.mes.entity.StockOut;
import com.mouse.mes.service.StockOutService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock-out")
public class StockOutController {

    /**
     * 出库业务服务，负责出库单、出库明细、库存校验、库存扣减和库存流水。
     */
    @Autowired
    private StockOutService stockOutService;

    /**
     * 查询全部出库单。
     */
    @GetMapping("/list")
    public JsonResult<List<StockOut>> findAll() {
        return JsonResult.success(stockOutService.findAll());
    }

    /**
     * 按出库单ID查询出库单。
     */
    @GetMapping("/{id}")
    public JsonResult<StockOut> findById(@PathVariable Long id) {
        StockOut stockOut = stockOutService.findById(id);
        if (stockOut == null) {
            return JsonResult.error("出库单不存在");
        }
        return JsonResult.success(stockOut);
    }

    /**
     * 按出库单号查询出库单。
     */
    @GetMapping("/no/{stockOutNo}")
    public JsonResult<StockOut> findByStockOutNo(@PathVariable String stockOutNo) {
        StockOut stockOut = stockOutService.findByStockOutNo(stockOutNo);
        if (stockOut == null) {
            return JsonResult.error("出库单不存在");
        }
        return JsonResult.success(stockOut);
    }

    /**
     * 新增出库单，出库前校验库存，库存不足时失败，库存充足时扣减库存并写流水。
     */
    @PostMapping("/add")
    public JsonResult<StockOut> create(@RequestBody StockOut stockOut, jakarta.servlet.http.HttpServletRequest http) {
        var actor=new com.fasterxml.jackson.databind.ObjectMapper().convertValue(http.getAttribute("userInfo"),com.mouse.mes.dto.LoginResponse.class);
        stockOut.setCreatedBy(actor.getId());
        try {
            return JsonResult.success(stockOutService.create(stockOut));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改出库单基础信息。
     */
    @PutMapping("/{id}")
    public JsonResult<StockOut> update(@PathVariable Long id, @RequestBody StockOut stockOut) {
        try {
            stockOut.setId(id);
            return JsonResult.success(stockOutService.update(stockOut));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 逻辑删除出库单。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        try {
            stockOutService.delete(id);
            return JsonResult.success();
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }
}

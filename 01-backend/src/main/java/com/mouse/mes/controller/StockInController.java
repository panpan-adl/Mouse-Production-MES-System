package com.mouse.mes.controller;

import com.mouse.mes.entity.StockIn;
import com.mouse.mes.service.StockInService;
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
@RequestMapping("/api/stock-in")
public class StockInController {

    /**
     * 入库业务服务，负责入库单、入库明细、库存增加和库存流水。
     */
    @Autowired
    private StockInService stockInService;

    /**
     * 查询全部入库单。
     */
    @GetMapping("/list")
    public JsonResult<List<StockIn>> findAll() {
        return JsonResult.success(stockInService.findAll());
    }

    /**
     * 按入库单ID查询入库单。
     */
    @GetMapping("/{id}")
    public JsonResult<StockIn> findById(@PathVariable Long id) {
        StockIn stockIn = stockInService.findById(id);
        if (stockIn == null) {
            return JsonResult.error("入库单不存在");
        }
        return JsonResult.success(stockIn);
    }

    /**
     * 按入库单号查询入库单。
     */
    @GetMapping("/no/{stockInNo}")
    public JsonResult<StockIn> findByStockInNo(@PathVariable String stockInNo) {
        StockIn stockIn = stockInService.findByStockInNo(stockInNo);
        if (stockIn == null) {
            return JsonResult.error("入库单不存在");
        }
        return JsonResult.success(stockIn);
    }

    /**
     * 新增入库单，入库后自动增加库存并写入库存流水。
     */
    @PostMapping("/add")
    public JsonResult<StockIn> create(@RequestBody StockIn stockIn, jakarta.servlet.http.HttpServletRequest http) {
        var actor=new com.fasterxml.jackson.databind.ObjectMapper().convertValue(http.getAttribute("userInfo"),com.mouse.mes.dto.LoginResponse.class);
        stockIn.setCreatedBy(actor.getId());
        try {
            return JsonResult.success(stockInService.create(stockIn));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改入库单基础信息。
     */
    @PutMapping("/{id}")
    public JsonResult<StockIn> update(@PathVariable Long id, @RequestBody StockIn stockIn) {
        try {
            stockIn.setId(id);
            return JsonResult.success(stockInService.update(stockIn));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 逻辑删除入库单。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        try {
            stockInService.delete(id);
            return JsonResult.success();
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }
}

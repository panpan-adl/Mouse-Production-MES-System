package com.mouse.mes.controller;

import com.mouse.mes.entity.InventoryOut;
import com.mouse.mes.service.InventoryOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-outs")
public class InventoryOutController {

    @Autowired
    private InventoryOutService inventoryOutService;

    @GetMapping
    public List<InventoryOut> getAllInventoryOuts() {
        return inventoryOutService.findAll();
    }

    @GetMapping("/{id}")
    public InventoryOut getInventoryOutById(@PathVariable Long id) {
        return inventoryOutService.findById(id);
    }

    @GetMapping("/outno/{outNo}")
    public InventoryOut getInventoryOutByOutNo(@PathVariable String outNo) {
        return inventoryOutService.findByOutNo(outNo);
    }

    @PostMapping
    public InventoryOut createInventoryOut(@RequestBody InventoryOut inventoryOut) {
        return inventoryOutService.create(inventoryOut);
    }

    @PutMapping("/{id}")
    public InventoryOut updateInventoryOut(@PathVariable Long id, @RequestBody InventoryOut inventoryOut) {
        inventoryOut.setId(id);
        return inventoryOutService.update(inventoryOut);
    }
}
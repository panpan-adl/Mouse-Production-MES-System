package com.mouse.mes.controller;

import com.mouse.mes.entity.InventoryIn;
import com.mouse.mes.service.InventoryInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-ins")
public class InventoryInController {

    @Autowired
    private InventoryInService inventoryInService;

    @GetMapping
    public List<InventoryIn> getAllInventoryIns() {
        return inventoryInService.findAll();
    }

    @GetMapping("/{id}")
    public InventoryIn getInventoryInById(@PathVariable Long id) {
        return inventoryInService.findById(id);
    }

    @GetMapping("/inno/{inNo}")
    public InventoryIn getInventoryInByInNo(@PathVariable String inNo) {
        return inventoryInService.findByInNo(inNo);
    }

    @PostMapping
    public InventoryIn createInventoryIn(@RequestBody InventoryIn inventoryIn) {
        return inventoryInService.create(inventoryIn);
    }

    @PutMapping("/{id}")
    public InventoryIn updateInventoryIn(@PathVariable Long id, @RequestBody InventoryIn inventoryIn) {
        inventoryIn.setId(id);
        return inventoryInService.update(inventoryIn);
    }
}
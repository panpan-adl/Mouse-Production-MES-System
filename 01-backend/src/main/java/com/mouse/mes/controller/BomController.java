package com.mouse.mes.controller;

import com.mouse.mes.entity.Bom;
import com.mouse.mes.service.BomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boms")
public class BomController {

    @Autowired
    private BomService bomService;

    @GetMapping
    public List<Bom> getAllBoms() {
        return bomService.findAll();
    }

    @GetMapping("/{id}")
    public Bom getBomById(@PathVariable Long id) {
        return bomService.findById(id);
    }

    @GetMapping("/product/{productId}")
    public List<Bom> getBomsByProductId(@PathVariable Long productId) {
        return bomService.findByProductId(productId);
    }

    @PostMapping
    public Bom createBom(@RequestBody Bom bom) {
        return bomService.create(bom);
    }

    @PutMapping("/{id}")
    public Bom updateBom(@PathVariable Long id, @RequestBody Bom bom) {
        bom.setId(id);
        return bomService.update(bom);
    }

    @DeleteMapping("/{id}")
    public void deleteBom(@PathVariable Long id) {
        bomService.delete(id);
    }
}
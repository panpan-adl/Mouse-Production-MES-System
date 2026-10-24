package com.mouse.mes.controller;

import com.mouse.mes.entity.Lot;
import com.mouse.mes.service.LotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
public class LotController {

    @Autowired
    private LotService lotService;

    @GetMapping
    public List<Lot> getAllLots() {
        return lotService.findAll();
    }

    @GetMapping("/{id}")
    public Lot getLotById(@PathVariable Long id) {
        return lotService.findById(id);
    }

    @GetMapping("/lot-no/{lotNo}")
    public Lot getLotByLotNo(@PathVariable String lotNo) {
        return lotService.findByLotNo(lotNo);
    }

    @GetMapping("/material/{materialId}")
    public List<Lot> getLotsByMaterialId(@PathVariable Long materialId) {
        return lotService.findByMaterialId(materialId);
    }

    @PostMapping
    public Lot createLot(@RequestBody Lot lot) {
        return lotService.create(lot);
    }

    @PutMapping("/{id}")
    public Lot updateLot(@PathVariable Long id, @RequestBody Lot lot) {
        lot.setId(id);
        return lotService.update(lot);
    }

    @DeleteMapping("/{id}")
    public void deleteLot(@PathVariable Long id) {
        lotService.delete(id);
    }
}
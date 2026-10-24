package com.mouse.mes.controller;

import com.mouse.mes.entity.Dict;
import com.mouse.mes.entity.DictItem;
import com.mouse.mes.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dicts")
public class DictController {

    @Autowired
    private DictService dictService;

    @GetMapping
    public List<Dict> getAllDicts() {
        return dictService.findAll();
    }

    @GetMapping("/{id}")
    public Dict getDictById(@PathVariable Long id) {
        return dictService.findById(id);
    }

    @GetMapping("/type/{dictType}")
    public Dict getDictByType(@PathVariable String dictType) {
        return dictService.findByType(dictType);
    }

    @GetMapping("/type/{dictType}/items")
    public List<DictItem> getDictItemsByType(@PathVariable String dictType) {
        return dictService.findItemsByType(dictType);
    }

    @PostMapping
    public Dict createDict(@RequestBody Dict dict) {
        return dictService.create(dict);
    }

    @PutMapping("/{id}")
    public Dict updateDict(@PathVariable Long id, @RequestBody Dict dict) {
        dict.setId(id);
        return dictService.update(dict);
    }

    @DeleteMapping("/{id}")
    public void deleteDict(@PathVariable Long id) {
        dictService.delete(id);
    }
}
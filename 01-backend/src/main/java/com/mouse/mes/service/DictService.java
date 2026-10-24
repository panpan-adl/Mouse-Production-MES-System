package com.mouse.mes.service;

import com.mouse.mes.entity.Dict;
import com.mouse.mes.entity.DictItem;

import java.util.List;

public interface DictService {
    List<Dict> findAll();
    Dict findById(Long id);
    Dict findByType(String dictType);
    Dict create(Dict dict);
    Dict update(Dict dict);
    void delete(Long id);
    List<DictItem> findItemsByType(String dictType);
}
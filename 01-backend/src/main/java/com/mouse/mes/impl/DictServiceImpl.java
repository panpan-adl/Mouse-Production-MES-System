package com.mouse.mes.impl;

import com.mouse.mes.entity.Dict;
import com.mouse.mes.entity.DictItem;
import com.mouse.mes.mapper.DictItemMapper;
import com.mouse.mes.mapper.DictMapper;
import com.mouse.mes.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private DictItemMapper dictItemMapper;

    @Override
    public List<Dict> findAll() {
        return dictMapper.findAll();
    }

    @Override
    public Dict findById(Long id) {
        return dictMapper.findById(id);
    }

    @Override
    public Dict findByType(String dictType) {
        return dictMapper.findByDictType(dictType);
    }

    @Override
    public Dict create(Dict dict) {
        dict.setCreateTime(new Date());
        dict.setUpdateTime(new Date());
        dictMapper.insert(dict);
        return dict;
    }

    @Override
    public Dict update(Dict dict) {
        dict.setUpdateTime(new Date());
        dictMapper.update(dict);
        return dictMapper.findById(dict.getId());
    }

    @Override
    public void delete(Long id) {
        dictMapper.delete(id);
    }

    @Override
    public List<DictItem> findItemsByType(String dictType) {
        return dictItemMapper.findByDictType(dictType);
    }
}
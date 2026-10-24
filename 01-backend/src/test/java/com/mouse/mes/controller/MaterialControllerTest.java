package com.mouse.mes.controller;

import com.mouse.mes.entity.Material;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MaterialControllerTest {

    @Autowired
    private MaterialController materialController;

    @Test
    public void testAddMaterial() {
        JsonResult<Material> result = materialController.createMaterial(buildMaterial("CTRL-ADD"));
        System.out.println("物料新增接口结果：" + result.getData().getMaterialCode());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertNotNull(result.getData().getId());
    }

    @Test
    public void testGetAllMaterials() {
        JsonResult<List<Material>> result = materialController.getAllMaterials();
        System.out.println("物料列表接口数量：" + result.getData().size());
        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    public void testGetMaterialById() {
        JsonResult<Material> savedResult = materialController.createMaterial(buildMaterial("CTRL-FIND"));
        Material saved = savedResult.getData();
        JsonResult<Material> result = materialController.getMaterialById(saved.getId());
        System.out.println("物料查询接口结果：" + result.getData().getMaterialCode());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(saved.getMaterialCode(), result.getData().getMaterialCode());
    }

    @Test
    public void testGetMaterialByMaterialCode() {
        JsonResult<Material> savedResult = materialController.createMaterial(buildMaterial("CTRL-FIND-CODE"));
        Material saved = savedResult.getData();
        JsonResult<Material> result = materialController.getMaterialByMaterialCode(saved.getMaterialCode());
        System.out.println("按编码查询物料接口结果：" + result.getData().getMaterialName());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(saved.getId(), result.getData().getId());
    }

    @Test
    public void testUpdateMaterial() {
        JsonResult<Material> savedResult = materialController.createMaterial(buildMaterial("CTRL-UPDATE"));
        Material saved = savedResult.getData();
        saved.setMaterialName("控制器修改物料");
        JsonResult<Material> result = materialController.updateMaterial(saved.getId(), saved);
        System.out.println("物料修改接口结果：" + result.getData().getMaterialName());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals("控制器修改物料", result.getData().getMaterialName());
    }

    @Test
    public void testUpdateStock() {
        JsonResult<Material> savedResult = materialController.createMaterial(buildMaterial("CTRL-STOCK"));
        Material saved = savedResult.getData();
        materialController.updateStock(saved.getId(), 50);
        JsonResult<Material> result = materialController.getMaterialById(saved.getId());
        System.out.println("库存更新接口结果：" + result.getData().getCurrentStock());
        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    public void testDeleteMaterial() {
        JsonResult<Material> savedResult = materialController.createMaterial(buildMaterial("CTRL-DELETE"));
        Material saved = savedResult.getData();
        materialController.deleteMaterial(saved.getId());
        JsonResult<Material> result = materialController.getMaterialById(saved.getId());
        System.out.println("物料删除接口结果：" + (result == null || result.getData() == null ? "已删除" : "未删除"));
    }

    private Material buildMaterial(String scene) {
        Material material = new Material();
        material.setMaterialCode("MAT-" + scene + "-" + System.nanoTime());
        material.setMaterialName("测试物料-" + scene);
        material.setMaterialType(1);
        material.setSpecification("标准规格");
        material.setUnit("PCS");
        material.setSafetyStock(10);
        material.setCurrentStock(0);
        material.setStatus(1);
        material.setRemark("物料控制器测试数据");
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        return material;
    }
}
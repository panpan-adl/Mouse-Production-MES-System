package com.mouse.mes.service;

import com.mouse.mes.entity.MaterialRequest;

import java.util.List;

public interface MaterialRequestService {
    List<MaterialRequest> findAll();

    MaterialRequest findById(Long id);

    MaterialRequest findByRequestNo(String requestNo);

    List<MaterialRequest> findByStatus(String requestStatus);

    MaterialRequest create(MaterialRequest materialRequest);

    MaterialRequest update(MaterialRequest materialRequest);

    MaterialRequest requestPurchase(Long id);

    MaterialRequest markPurchaseOrdered(Long id);

    com.mouse.mes.entity.PurchaseOrder createPurchaseOrder(Long id, com.mouse.mes.dto.PurchaseOrderRequest request, Long actor);

    void delete(Long id);
}

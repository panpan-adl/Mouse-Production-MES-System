package com.mouse.mes.dto;

import lombok.Data;

@Data
public class AcceptSalesOrderRequest {
    private Long supplierId;
    private String expectedDate;
}
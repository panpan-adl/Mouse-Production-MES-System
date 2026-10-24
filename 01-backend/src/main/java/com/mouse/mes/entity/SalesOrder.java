package com.mouse.mes.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SalesOrder {
    private Long id;
    private String soNo;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date orderDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date deliveryDate;
    private BigDecimal totalAmount;
    private Integer priority;
    private String status;
    private String materialStatus;
    private String remark;
    private Long createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date updatedAt;
    private Integer isDeleted;
}

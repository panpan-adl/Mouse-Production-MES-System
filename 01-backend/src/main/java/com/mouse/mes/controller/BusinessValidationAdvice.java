package com.mouse.mes.controller;

import com.mouse.mes.util.JsonResult;
import org.springframework.web.bind.annotation.*;

/** Expected business validation failures are presented as actionable messages. */
@RestControllerAdvice(assignableTypes={OrderController.class,ProductionPlanController.class,ProductionTaskController.class,ProductionDashboardController.class,PurchaseOrderController.class})
public class BusinessValidationAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Object invalid(IllegalArgumentException error){return JsonResult.error(400,error.getMessage());}
}

package com.mouse.mes.controller;

import com.mouse.mes.service.*;
import com.mouse.mes.util.JsonResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class ProductionDashboardController {
    private final CanonicalDashboardService canonical;
    private final OrderService orders;
    private final ProductionPlanService plans;
    public ProductionDashboardController(CanonicalDashboardService canonical,OrderService orders,ProductionPlanService plans){this.canonical=canonical;this.orders=orders;this.plans=plans;}
    @GetMapping("/production") public Object production(@RequestParam(defaultValue="") String date,@RequestParam(defaultValue="all") String shift,@RequestParam(defaultValue="all") String line){return JsonResult.success(canonical.production(date,shift,line));}
    @GetMapping("/summary") public Object summary(){return JsonResult.success(canonical.summary());}
    @GetMapping("/recent-work-orders") public Object works(){return JsonResult.success(canonical.recentWorkOrders());}
    @GetMapping("/recent-orders") public Object orders(){return JsonResult.success(orders.findAll().stream().limit(10).toList());}
    @GetMapping("/recent-plans") public Object plans(){return JsonResult.success(plans.findAll().stream().limit(10).toList());}
}

package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.FoundationService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dify/op30")
public class DifyQualityController {
    private static final URI ENDPOINT = URI.create("https://api.dify.ai/v1/workflows/run");
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final FoundationService production;

    public DifyQualityController(ObjectMapper json, FoundationService production) {
        this.json = json;
        this.production = production;
    }

    @GetMapping("/work-orders")
    public Object workOrderOptions(HttpServletRequest request) {
        requireProductionAccess(request);
        return JsonResult.success(production.productionWorkOrderOptions());
    }

    @GetMapping("/work-order")
    public Object workOrder(@RequestParam String code, HttpServletRequest request) {
        requireProductionAccess(request);
        code = code.strip();
        if (!code.matches("MO-[0-9]{1,24}")) return JsonResult.error(400, "请输入 MO- 开头的生产工单号。");
        var work = production.findProductionWorkOrder(code);
        if (work == null) return JsonResult.error(404, "未找到该生产工单。");
        var tasks = production.workTasks(((Number) work.get("id")).longValue()).stream()
                .map(task -> new Operation(task.get("task_no"), task.get("sequence_no"), task.get("operation_name")))
                .toList();
        return JsonResult.success(new WorkContext(work.get("code"), work.get("product_name"),
                work.get("quantity"), work.get("status"), tasks));
    }

    record Operation(Object taskNo, Object sequence, Object name) {}
    record WorkContext(Object code, Object product, Object quantity, Object status, java.util.List<Operation> operations) {}
    @GetMapping("/status")
    public Object status(HttpServletRequest request) {
        requireLogin(request);
        return JsonResult.success(Map.of("configured", !key().isBlank(), "productionConfigured", !productionKey().isBlank()));
    }

    public record Input(String workOrder, BigDecimal torque) {}
    record Result(String status, String report, String runId) {}

    @PostMapping("/run")
    public Object run(@RequestBody Input input, HttpServletRequest request) {
        requireLogin(request);
        if (input.workOrder() == null || !input.workOrder().strip().matches("DEMO-[A-Za-z0-9-]{1,35}")
                || input.torque() == null || input.torque().signum() < 0 || input.torque().compareTo(BigDecimal.TEN) > 0
                || input.torque().scale() > 4) {
            return JsonResult.error(400, "请输入 DEMO- 开头的演示工单，以及 0 至 10 N·m 的数值（最多四位小数）。");
        }
        return invoke(key(), Map.of("work_order", input.workOrder().strip(), "torque", input.torque()));
    }

    public record ProductionInput(String code, BigDecimal torque, BigDecimal lower, BigDecimal upper) {}

    @PostMapping("/production-run")
    public Object runProduction(@RequestBody ProductionInput input, HttpServletRequest request) {
        requireProductionAccess(request);
        if (input.code() == null || !input.code().matches("MO-[0-9]{1,24}")
                || !nonNegative(input.torque()) || !nonNegative(input.lower()) || !nonNegative(input.upper())
                || input.lower().compareTo(input.upper()) >= 0)
            return JsonResult.error(400, "请输入有效的工单号、实测扭矩和工艺上下限，上限必须大于下限。");
        var work = production.findProductionWorkOrder(input.code());
        if (work == null) return JsonResult.error(404, "未找到该生产工单。");
        boolean hasOp30 = production.workTasks(((Number) work.get("id")).longValue()).stream()
                .anyMatch(task -> task.get("sequence_no") instanceof Number sequence && sequence.intValue() == 30);
        if (!hasOp30) return JsonResult.error(400, "该工单没有第 30 道工序。");
        return invoke(productionKey(), Map.of("torque", input.torque(), "lower", input.lower(), "upper", input.upper()));
    }

    private static boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0 && value.precision() <= 12 && value.scale() <= 6
                && value.compareTo(new BigDecimal("1000000")) <= 0;
    }

    private Object invoke(String key, Map<String, ?> inputs) {
        if (key.isBlank()) return JsonResult.error(503, "请先给 MouseMES 后端配置对应的 Dify 工作流应用密钥。");
        try {
            var body = Map.of("inputs", inputs, "response_mode", "blocking", "user", "mousemes-" + UUID.randomUUID());
            var reply = http.send(HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(25))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (reply.statusCode() == 401 || reply.statusCode() == 403)
                return JsonResult.error(502, "Dify 拒绝了请求，请核对工作流的应用密钥及权限。");
            if (reply.statusCode() == 429) return JsonResult.error(502, "Dify 限流或配额不足，请检查工作区配额。");
            if (reply.statusCode() != 200)
                return JsonResult.error(502, "Dify 返回 HTTP " + reply.statusCode() + "，请确认工作流已发布，并查看 Dify 日志。");
            return JsonResult.success(readResult(json.readTree(reply.body())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JsonResult.error(502, "Dify 请求已中断。");
        } catch (IOException | IllegalStateException e) {
            return JsonResult.error(502, "Dify 未返回有效质检结果。请检查网络或在 Dify 工作室查看运行日志。");
        }
    }

    static Result readResult(JsonNode root) {
        if (root == null) throw new IllegalStateException();
        var data = root.path("data");
        var outputs = data.path("outputs");
        String runId = root.path("workflow_run_id").asText("");
        if (runId.isBlank()) runId = data.path("id").asText("");
        String status = outputs.path("status").asText("");
        String report = outputs.path("report").asText("");
        if (!"succeeded".equals(data.path("status").asText()) || runId.isBlank()
                || !outputs.path("status").isTextual() || status.isBlank()
                || !outputs.path("report").isTextual() || report.isBlank()) throw new IllegalStateException();
        return new Result(status, report, runId);
    }

    private void requireProductionAccess(HttpServletRequest request) {
        requireLogin(request);
        var user = json.convertValue(request.getAttribute("userInfo"), LoginResponse.class);
        if (user.getRoleId() == null || !java.util.Set.of(1L, 2L, 4L, 5L, 9L).contains(user.getRoleId().longValue()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private static void requireLogin(HttpServletRequest request) {
        if (request.getAttribute("userInfo") == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    private static String productionKey() {
        String value = System.getenv("MES_DIFY_PRODUCTION_API_KEY");
        return value == null ? "" : value.strip();
    }

    private static String key() {
        String value = System.getenv("MES_DIFY_API_KEY");
        return value == null ? "" : value.strip();
    }
}

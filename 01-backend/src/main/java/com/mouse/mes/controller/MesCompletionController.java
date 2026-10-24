package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.*;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mes")
public class MesCompletionController {
    @org.springframework.beans.factory.annotation.Autowired private MesMonitorService monitor;
    @org.springframework.beans.factory.annotation.Autowired private MesWorkBreakdownService breakdown;
    @org.springframework.beans.factory.annotation.Autowired private AutomationService automation;
    private final MesOperationsService ops;private final MesDocumentService documents;private final MesPackagingService packages;private final LineStockService line;private final MesRulesService rules;private final ObjectMapper json;private final JdbcTemplate db;
    public MesCompletionController(MesOperationsService ops,MesDocumentService documents,MesPackagingService packages,LineStockService line,MesRulesService rules,ObjectMapper json,JdbcTemplate db){this.ops=ops;this.documents=documents;this.packages=packages;this.line=line;this.rules=rules;this.json=json;this.db=db;}
    private LoginResponse user(HttpServletRequest r){return json.convertValue(r.getAttribute("userInfo"),LoginResponse.class);}
    private void allow(HttpServletRequest r,int...roles){int role=user(r).getRoleId().intValue();if(role!=9&&Arrays.stream(roles).noneMatch(x->x==role))throw new SecurityException("当前岗位没有此操作权限");}
    @GetMapping("/people")public Object people(){return JsonResult.success(db.queryForList("SELECT id,real_name realName,real_name name,user_code userCode,role_id roleId FROM t_user WHERE deleted=0 AND status<>0"));}
    @org.springframework.beans.factory.annotation.Autowired private MesReportService reports;
    @GetMapping("/reports") public Object reports(HttpServletRequest r){allow(r,1,2,3,4,5);return JsonResult.success(reports.list(user(r).getId(),user(r).getRoleId().intValue()));}
    @PostMapping("/reports") public Object reportDraft(@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,1,2,3);return JsonResult.success(reports.create(body,user(r).getId(),user(r).getRoleId().intValue()));}
    @PostMapping("/reports/{id}/confirm") public Object reportConfirm(@PathVariable long id,HttpServletRequest r){allow(r,1,2);reports.decide(id,true,null,user(r).getId());return JsonResult.success();}
    @PostMapping("/reports/{id}/reject") public Object reportReject(@PathVariable long id,@RequestBody Map<String,String> body,HttpServletRequest r){allow(r,1,2);reports.decide(id,false,body.get("reason"),user(r).getId());return JsonResult.success();}
    @GetMapping("/planning")public Object planning(HttpServletRequest r){allow(r,1,2,3,4,8);return JsonResult.success(ops.planning());}
    @PostMapping("/tasks/{id}/dispatch")public Object dispatch(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,1,2);ops.dispatch(id,body);return JsonResult.success();}
    @PostMapping("/tasks/{id}/split")public Object split(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,1,2);return JsonResult.success(ops.split(id,Math.toIntExact(FulfillmentService.n(body.get("quantity")))));}
    @PostMapping("/work-orders")public Object work(@RequestBody Map<String,Object> body,HttpServletRequest r)throws Exception{allow(r,1,2);return JsonResult.success(ops.createWork(body,user(r).getId()));}
    @PostMapping("/work-orders/{id}/decompose")public Object decompose(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r)throws Exception{allow(r,1,2);return JsonResult.success(breakdown.decompose(id,FulfillmentService.n(body.get("owner_id")),user(r).getId()));}
    @GetMapping("/work-relations")public Object relations(HttpServletRequest r){allow(r,1,2,3,5);return JsonResult.success(breakdown.relations());}
    @GetMapping("/calendar")public Object calendar(HttpServletRequest r){return JsonResult.success(ops.calendar(user(r).getId(),Set.of(1L,2L,9L,11L).contains(user(r).getRoleId())));}
    @PostMapping("/rotations/{id}/generate")public Object rotate(@PathVariable long id,HttpServletRequest r){allow(r,1,2);return JsonResult.success(ops.rotate(id,user(r).getId()));}
    @GetMapping("/checks")public Object checks(HttpServletRequest r){allow(r,1,2,8);return JsonResult.success(ops.checks());}
    @PostMapping("/checks/generate")public Object generateChecks(HttpServletRequest r){allow(r,8,11);if(db.update("UPDATE mm_job_config SET last_run=NULL WHERE code='EQUIPMENT_CHECK' AND enabled=1")==0)throw new IllegalArgumentException("请先启用设备到期任务调度");rules.periodicChecks();return JsonResult.success();}
    @GetMapping("/nonconformities")public Object nc(HttpServletRequest r){allow(r,1,2,3,4,5);return JsonResult.success(ops.nonconformities());}
    @PostMapping("/nonconformities/{id}/scrap")public Object scrap(@PathVariable long id,HttpServletRequest r){allow(r,5);ops.executeScrap(id,user(r).getId());return JsonResult.success();}
    @PostMapping("/nonconformities/{id}/disposition")public Object disposition(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,4);ops.disposition(id,body,user(r).getId());return JsonResult.success();}
    @PostMapping("/reworks/{id}/complete")public Object rework(@PathVariable long id,@RequestBody Map<String,String> body,HttpServletRequest r){allow(r,3);ops.finishRework(id,body.get("result"),user(r).getId());return JsonResult.success();}
    @GetMapping("/notices")public Object notices(HttpServletRequest r){return JsonResult.success(ops.notices(user(r).getId(),user(r).getRoleId()));}
    @PostMapping("/notices")public Object publish(@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,1,2,11);ops.publish(body,user(r).getId());return JsonResult.success();}
    @PostMapping("/notices/{id}/read")public Object read(@PathVariable long id,HttpServletRequest r){ops.readNotice(id,user(r).getId(),user(r).getRoleId());return JsonResult.success();}
    @GetMapping("/line-stock")public Object lineStock(HttpServletRequest r){allow(r,1,2,3,4,5,6,10);return JsonResult.success(line.stock());}
    @GetMapping("/line-movements/{id}")public Object lineMovements(@PathVariable long id,HttpServletRequest r){allow(r,1,2,3,4,5,6,10);return JsonResult.success(line.movements(id));}
    @GetMapping("/alerts")public Object alerts(HttpServletRequest r){allow(r,1,2,5,6,10);return JsonResult.success(ops.alerts());}
    @GetMapping("/documents")public Object docs(HttpServletRequest r){allow(r,1,2,4,5,6,7,10);return JsonResult.success(documents.documents());}
    @GetMapping("/return-lots")public Object returnLots(@RequestParam String kind,@RequestParam long source,HttpServletRequest r){allow(r,1,2,4,5,6,7,10);return JsonResult.success(documents.returnLots(kind,source));}
    @GetMapping("/documents/{id}/lines")public Object docLines(@PathVariable long id,HttpServletRequest r){allow(r,1,2,4,5,6,7,10);return JsonResult.success(documents.lines(id));}
    @PostMapping("/documents")public Object draft(@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,5);return JsonResult.success(documents.create(body,user(r).getId()));}
    @PutMapping("/documents/{id}")public Object edit(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,5);documents.edit(id,body,user(r).getId());return JsonResult.success();}
    @PostMapping("/documents/{id}/confirm")public Object confirm(@PathVariable long id,HttpServletRequest r){allow(r,5);documents.confirm(id);return JsonResult.success();}
    @PostMapping("/documents/{id}/cancel")public Object cancel(@PathVariable long id,HttpServletRequest r){allow(r,5);documents.cancel(id);return JsonResult.success();}
    @PostMapping("/documents/{id}/post")public Object post(@PathVariable long id,HttpServletRequest r){allow(r,5);documents.post(id,user(r).getId());return JsonResult.success();}
    @PostMapping("/document-lines/{id}/inspect")public Object inspectLine(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r)throws Exception{allow(r,4);documents.inspectLine(id,body,user(r).getId());return JsonResult.success();}
    @GetMapping("/packages")public Object packages(HttpServletRequest r){allow(r,1,2,5,7);return JsonResult.success(packages.packages());}
    @PostMapping("/packages")public Object createBox(@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,5);return JsonResult.success(packages.create(body,user(r).getId()));}
    @GetMapping("/packages/{id}/contents")public Object contents(@PathVariable long id,HttpServletRequest r){allow(r,1,2,5,7);return JsonResult.success(packages.contents(id));}
    @PostMapping("/packages/{id}/items")public Object item(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,5);packages.item(id,FulfillmentService.n(body.get("inventory_id")),new BigDecimal(body.get("quantity").toString()));return JsonResult.success();}
    @PostMapping("/packages/{id}/children")public Object child(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest r){allow(r,5);packages.child(id,FulfillmentService.n(body.get("child_id")));return JsonResult.success();}
    @PostMapping("/packages/{id}/confirm")public Object confirmBox(@PathVariable long id,HttpServletRequest r){allow(r,5);packages.confirm(id);return JsonResult.success();}
    @GetMapping("/barcode")public Object barcode(@RequestParam String kind,@RequestParam long id){return JsonResult.success(packages.barcode(kind,id));}
    @GetMapping("/resolve-barcode")public Object resolve(@RequestParam String content){return JsonResult.success(packages.resolve(content));}
    @PostMapping("/codes/{rule}")public Object code(@PathVariable String rule,HttpServletRequest r){allow(r,1,2,5,6,7,8,11);return JsonResult.success(rules.nextCode(rule));}
    @GetMapping("/monitor")public Object monitor(HttpServletRequest r){allow(r,11);return JsonResult.success(ops.monitor());}
    @GetMapping("/cache")public Object cache(HttpServletRequest r){allow(r,11);return JsonResult.success(monitor.cache());}
    @GetMapping("/jobs")public Object jobs(HttpServletRequest r){allow(r,11);return JsonResult.success(db.queryForList("SELECT * FROM mm_job_config ORDER BY code"));}
    @PostMapping("/jobs/{code}/run")public Object runJob(@PathVariable String code,HttpServletRequest r){allow(r,11);if(code.equals("REPORT_QC"))return JsonResult.success(automation.process());if(code.equals("EQUIPMENT_CHECK")){if(db.update("UPDATE mm_job_config SET last_run=NULL WHERE code=? AND enabled=1",code)==0)throw new IllegalArgumentException("请先启用此任务");rules.periodicChecks();return JsonResult.success();}throw new IllegalArgumentException("不支持执行此任务");}
    @GetMapping("/sessions")public Object sessions(HttpServletRequest r){allow(r,11);return JsonResult.success(monitor.sessions());}
    @PostMapping("/sessions/{fingerprint}/revoke")public Object revoke(@PathVariable String fingerprint,HttpServletRequest r){allow(r,11);monitor.revoke(fingerprint);return JsonResult.success();}
    @GetMapping("/menu-access")public Object menus(HttpServletRequest r){return JsonResult.success(db.queryForList("SELECT m.path FROM mm_role_menu a JOIN mm_menu m ON m.id=a.menu_id WHERE a.role_id=?",user(r).getRoleId()));}
    @GetMapping("/settings")public Object settings(){return JsonResult.success(db.queryForList("SELECT code,value FROM mm_parameter WHERE code IN('APP_NAME','NOTICE_LIMIT','DEFAULT_DURATION_MINUTES')"));}
    @GetMapping("/task-sops/{id}")public Object sops(@PathVariable long id){return JsonResult.success(ops.sops(id));}
    private Path imageRoot(){Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();if(root.getFileName().toString().equals("01-backend"))root=root.getParent();return root.resolve(".runtime/sop-images");}
    @PostMapping("/sop-images")public Object image(@RequestParam MultipartFile file,HttpServletRequest r)throws IOException{
        allow(r,1,2);if(file.getSize()>8*1024*1024||file.isEmpty())throw new IllegalArgumentException("SOP图片不能为空且不得超过8MB");
        var decoded=ImageIO.read(new ByteArrayInputStream(file.getBytes()));if(decoded==null||decoded.getWidth()>10000||decoded.getHeight()>10000)throw new IllegalArgumentException("请上传有效PNG或JPEG图片，尺寸不超过10000像素");
        Path root=imageRoot();Files.createDirectories(root);String name=UUID.randomUUID()+".png";ImageIO.write(decoded,"png",root.resolve(name).toFile());return JsonResult.success("/api/mes/sop-images/"+name);
    }
    @GetMapping("/sop-images/{filename}")public ResponseEntity<byte[]> image(@PathVariable String filename)throws IOException{
        if(!filename.matches("[a-f0-9-]+\\.png"))return ResponseEntity.badRequest().build();Path path=imageRoot().resolve(filename);if(!Files.exists(path))return ResponseEntity.notFound().build();return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(Files.readAllBytes(path));
    }
    @ExceptionHandler(SecurityException.class)@ResponseStatus(HttpStatus.FORBIDDEN)public Object denied(Exception e){return JsonResult.error(403,e.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,ArithmeticException.class,java.time.DateTimeException.class,org.springframework.dao.DataAccessException.class})@ResponseStatus(HttpStatus.BAD_REQUEST)public Object invalid(Exception e){return JsonResult.error(400,e instanceof org.springframework.dao.DataAccessException?"数据关联、数量或唯一性校验失败，请检查业务来源":e.getMessage());}
}

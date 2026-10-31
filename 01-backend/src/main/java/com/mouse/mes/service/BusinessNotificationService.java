package com.mouse.mes.service;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
@Service
public class BusinessNotificationService {
 private final JdbcTemplate db;
 public BusinessNotificationService(JdbcTemplate db){this.db=db;}
 @PostConstruct public void initialize(){db.execute("CREATE TABLE IF NOT EXISTS mm_business_notification (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,event_key VARCHAR(190) NOT NULL,title VARCHAR(200) NOT NULL,content TEXT NOT NULL,kind VARCHAR(16) NOT NULL,target_path VARCHAR(200) NOT NULL,is_read INT NOT NULL DEFAULT 0,created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),UNIQUE KEY uq_business_recipient_event(user_id,event_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");}
 private String num(Object value){return new BigDecimal(String.valueOf(value==null?0:value)).stripTrailingZeros().toPlainString();}
 private void add(long user,String key,String title,String content,String kind,String path){db.update("INSERT IGNORE INTO mm_business_notification(user_id,event_key,title,content,kind,target_path) VALUES(?,?,?,?,?,?)",user,key,title,content,kind,path);}
 @Transactional public List<Map<String,Object>> list(long user,long role){
  if(role!=9)return List.of();
  // Notifications describe the business state at generation time; repeated refreshes do not duplicate it.
  for(var m:db.queryForList("SELECT m.id,m.material_name,m.unit,m.safety_stock,COALESCE(SUM(CASE WHEN w.id IS NOT NULL AND w.warehouse_code<>'WH-A100-LINE' THEN i.quantity ELSE 0 END),0) quantity FROM t_material m LEFT JOIN t_inventory i ON i.material_id=m.id AND i.is_deleted=0 LEFT JOIN t_warehouse w ON w.id=i.warehouse_id WHERE m.status=1 AND m.safety_stock>0 GROUP BY m.id HAVING quantity<m.safety_stock")){
   add(user,"stock:"+m.get("id")+":"+num(m.get("quantity"))+":"+num(m.get("safety_stock")),"低库存预警 · "+m.get("material_name"),"生成时实体仓库存为 "+num(m.get("quantity"))+" "+m.get("unit")+"，安全库存 "+num(m.get("safety_stock"))+"。请在库存管理核对当前余额和补货需求。","warning","/inventory/warehouse");
  }
  for(var m:db.queryForList("SELECT id,maintenance_no,equipment_name,status,maintenance_type,maintenance_user_name,cost FROM t_maintenance WHERE status IN ('PENDING','PROCESSING','FINISHED') ORDER BY id DESC LIMIT 30")){
   String status=String.valueOf(m.get("status")),kind=status.equals("FINISHED")?"success":status.equals("PENDING")?"warning":"info";
   String state=status.equals("FINISHED")?"已完成":status.equals("PENDING")?"待处理":"执行中";
   String type=String.valueOf(m.get("maintenance_type")).equals("1")?"保养":"维修";
   add(user,"maintenance:"+m.get("id")+":"+status,"设备"+type+state+" · "+m.get("equipment_name"),"单据 "+m.get("maintenance_no")+"，生成时状态为"+state+"。负责人："+(m.get("maintenance_user_name")==null?"待派工":m.get("maintenance_user_name"))+(status.equals("FINISHED")?"；已登记费用 ¥"+num(m.get("cost")):"；请核对设备保障进度。"),kind,"/equipment/maintenance?tab="+(status.equals("FINISHED")?"bills":status.equals("PROCESSING")?"execution":"requests"));
  }
  for(var w:db.queryForList("SELECT w.id,w.code,w.quantity,w.status,p.product_name FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id WHERE w.status IN ('CREATED','IN_PROGRESS','PRODUCTION_COMPLETED') ORDER BY w.id DESC LIMIT 30")){
   String status=String.valueOf(w.get("status"));boolean complete=status.equals("PRODUCTION_COMPLETED");
   add(user,"work:"+w.get("id")+":"+status,(complete?"生产完成":"生产进度待关注")+" · "+w.get("code"),"产品："+w.get("product_name")+"，工单数量 "+num(w.get("quantity"))+"。"+(complete?"生产工序已完成，请结合入库、发货记录核对交付进度。":"请核对领料、排产及工序执行情况。"),complete?"success":"info","/manufacturing");
  }
  return db.queryForList("SELECT id,title,content,kind type,target_path targetPath,is_read isRead,created_at createdAt FROM mm_business_notification WHERE user_id=? ORDER BY id DESC LIMIT 100",user);
 }
 public void read(long id,long user){if(db.update("UPDATE mm_business_notification SET is_read=1 WHERE id=? AND user_id=?",id,user)==0)throw new SecurityException("通知不存在或不属于当前用户");}
}

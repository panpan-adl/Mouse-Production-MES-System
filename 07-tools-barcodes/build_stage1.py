"""Generate the reviewed stage-one catalog and additive migration. Never executes SQL."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
resources=[]
def field(name,label,type='text',ref=None,required=True,options=None):
    return dict(name=name,label=label,type=type,ref=ref,required=required,options=options)
def resource(key,label,table,fields,roles,unique=None,where='1=1',extra=None):
    resources.append(dict(key=key,label=label,table=table,fields=fields,roles=roles,unique=unique,where=where,extra=extra or {}))
F=field
resource('units','计量单位','t_uom',[F('uom_code','编码'),F('uom_name','名称')],[1,5], 'uom_code','is_deleted=0',{'status':'ACTIVE','is_deleted':0})
resource('materials','物料档案','t_material',[F('material_code','编码'),F('material_name','名称'),F('material_type','物料类型','select',options={'1':'原材料','2':'半成品','3':'成品'}),F('unit','单位'),F('specification','规格',required=False),F('safety_stock','安全库存','number',required=False)],[1,5,6],'material_code','status>=0',{'current_stock':0,'status':1})
resource('products','产品档案','t_product',[F('product_code','编码'),F('product_name','名称'),F('model','型号',required=False),F('unit','单位'),F('specification','规格',required=False),F('cost_price','成本价','number',required=False),F('selling_price','售价','number',required=False)],[1,7],'product_code',extra={'status':1})
resource('customers','客户','t_customer',[F('customer_code','编码'),F('customer_name','名称'),F('contact_person','联系人',required=False),F('phone','电话',required=False),F('address','地址',required=False)],[7],'customer_code','is_deleted=0',{'status':'1','is_deleted':0})
resource('suppliers','供应商','t_supplier',[F('supplier_code','编码'),F('supplier_name','名称'),F('contact_person','联系人',required=False),F('contact_phone','电话',required=False)],[6],'supplier_code',extra={'status':1})
resource('warehouses','仓库','t_warehouse',[F('warehouse_code','编码'),F('warehouse_name','名称'),F('location','位置',required=False)],[5],'warehouse_code',extra={'status':1})
resource('workshops','车间','mm_workshop',[F('code','编码'),F('name','名称')],[1,2],'code')
resource('stations','工作站','mm_station',[F('code','编码'),F('name','名称'),F('workshop_id','所属车间','ref','workshops'),F('equipment_id','设备','ref','equipment',False)],[1,2],'code')
resource('areas','库区','mm_area',[F('code','编码'),F('name','名称'),F('warehouse_id','所属仓库','ref','warehouses')],[5],'code')
resource('locations','库位','mm_location',[F('code','编码'),F('name','名称'),F('area_id','所属库区','ref','areas')],[5],'code')
resource('teams','班组','mm_team',[F('code','编码'),F('name','名称'),F('leader_id','班组长','ref','users')],[1,2],'code')
resource('members','班组成员','mm_member',[F('team_id','班组','ref','teams'),F('user_id','成员','ref','users')],[1,2])
resource('shifts','班次','mm_shift',[F('code','编码'),F('name','名称'),F('start_time','开始时间','time'),F('end_time','结束时间','time')],[1,2],'code')
resource('schedules','排班','mm_schedule',[F('team_id','班组','ref','teams'),F('shift_id','班次','ref','shifts'),F('station_id','工作站','ref','stations'),F('equipment_id','设备','ref','equipment',False),F('start_at','开始日期时间','datetime-local'),F('end_at','结束日期时间','datetime-local')],[1,2])
resource('equipment','设备台账','t_equipment',[F('equipment_code','编码'),F('equipment_name','名称'),F('model','型号',required=False),F('location','安装位置',required=False)],[8],'equipment_code','is_deleted=0',{'status':1,'is_deleted':0})
resource('checks','设备点检计划','mm_check_plan',[F('code','编码'),F('name','点检项目'),F('equipment_id','设备','ref','equipment'),F('interval_days','周期天数','positive'),F('standard','检查标准'),F('owner_id','负责人','ref','users')],[8],'code')
resource('check-records','点检执行记录','mm_check_record',[F('plan_id','点检计划','ref','checks'),F('checked_at','检查时间','datetime-local'),F('actual_value','实测结果'),F('result','判定','select',options={'PASS':'正常','FAIL':'异常'}),F('remark','备注',required=False)],[8])
resource('qc-items','检测项','mm_qc_item',[F('code','编码'),F('name','名称'),F('standard','判定标准')],[4],'code')
resource('qc-templates','检测模板','mm_qc_template',[F('code','编码'),F('name','名称'),F('product_id','产品','ref','products'),F('kind','检验类型','select',options={'INCOMING':'来料','PROCESS':'过程','SHIPPING':'出货'}),F('version','版本')],[4],'code')
resource('qc-lines','模板检测项','mm_qc_line',[F('template_id','模板','ref','qc-templates'),F('item_id','检测项','ref','qc-items')],[4])
resource('routes','工艺路线','mm_route',[F('code','编码'),F('name','名称'),F('product_id','产品','ref','products'),F('version','版本')],[1,2],'code')
resource('operations','工序','mm_operation',[F('code','编码'),F('name','名称'),F('route_id','工艺路线','ref','routes'),F('sequence_no','顺序','positive'),F('station_id','工作站','ref','stations'),F('predecessor_id','前置工序','ref','operations',False),F('standard_minutes','标准工时(分钟)','positive'),F('requires_qc','过程检验','select',options={'1':'需要','0':'不需要'})],[1,2],'code')
resource('bom-versions','BOM版本','mm_bom',[F('code','编码'),F('name','名称'),F('product_id','产品','ref','products'),F('root_material_id','成品物料','ref','materials'),F('version','版本')],[1],'code')
resource('bom-lines','多级BOM组成','mm_bom_line',[F('bom_id','BOM版本','ref','bom-versions'),F('parent_material_id','父物料','ref','materials'),F('material_id','子物料','ref','materials'),F('quantity','单位用量','positive')],[1])
resource('mappings','产品物料映射','mm_product_material',[F('product_id','产品','ref','products'),F('material_id','成品物料','ref','materials')],[1])

ddl={
'workshop':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL',
'station':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,workshop_id BIGINT NOT NULL,equipment_id BIGINT NULL',
'area':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,warehouse_id BIGINT NOT NULL',
'location':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,area_id BIGINT NOT NULL',
'team':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,leader_id BIGINT NOT NULL',
'member':'team_id BIGINT NOT NULL,user_id BIGINT NOT NULL,UNIQUE KEY(team_id,user_id)',
'shift':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,start_time TIME NOT NULL,end_time TIME NOT NULL',
'schedule':'team_id BIGINT NOT NULL,shift_id BIGINT NOT NULL,station_id BIGINT NOT NULL,equipment_id BIGINT NULL,start_at DATETIME NOT NULL,end_at DATETIME NOT NULL,INDEX(start_at,end_at)',
'check_plan':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,equipment_id BIGINT NOT NULL,interval_days INT NOT NULL,standard VARCHAR(500) NOT NULL,owner_id BIGINT NOT NULL',
'check_record':'plan_id BIGINT NOT NULL,checked_at DATETIME NOT NULL,actual_value VARCHAR(500) NOT NULL,result VARCHAR(20) NOT NULL,remark VARCHAR(500),actor_id BIGINT NOT NULL,maintenance_id BIGINT NULL',
'qc_item':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,standard VARCHAR(500) NOT NULL',
'qc_template':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,product_id BIGINT NOT NULL,kind VARCHAR(30) NOT NULL,version VARCHAR(30) NOT NULL',
'qc_line':'template_id BIGINT NOT NULL,item_id BIGINT NOT NULL,UNIQUE KEY(template_id,item_id)',
'route':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,product_id BIGINT NOT NULL,version VARCHAR(30) NOT NULL',
'operation':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,route_id BIGINT NOT NULL,sequence_no INT NOT NULL,station_id BIGINT NOT NULL,predecessor_id BIGINT NULL,standard_minutes DECIMAL(10,2) NOT NULL,requires_qc INT NOT NULL DEFAULT 1,UNIQUE KEY(route_id,sequence_no)',
'bom':'code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,product_id BIGINT NOT NULL,root_material_id BIGINT NOT NULL,version VARCHAR(30) NOT NULL',
'bom_line':'bom_id BIGINT NOT NULL,parent_material_id BIGINT NOT NULL,material_id BIGINT NOT NULL,quantity DECIMAL(14,4) NOT NULL,UNIQUE KEY(bom_id,parent_material_id,material_id)',
'product_material':'product_id BIGINT NOT NULL UNIQUE,material_id BIGINT NOT NULL UNIQUE',
'work_order':'code VARCHAR(80) NOT NULL UNIQUE,plan_id BIGINT NOT NULL UNIQUE,bom_id BIGINT NOT NULL,route_id BIGINT NOT NULL,quantity INT NOT NULL,bom_snapshot JSON NOT NULL,route_snapshot JSON NOT NULL,status VARCHAR(30) NOT NULL DEFAULT \'CREATED\',created_by BIGINT NOT NULL',
'task_context':'task_id BIGINT NOT NULL UNIQUE,work_order_id BIGINT NOT NULL,operation_id BIGINT NOT NULL,sequence_no INT NOT NULL,predecessor_id BIGINT NULL,requires_qc INT NOT NULL,is_terminal INT NOT NULL,station_id BIGINT NOT NULL,schedule_id BIGINT NULL,UNIQUE KEY(work_order_id,operation_id)',
'inspection':'code VARCHAR(80) NOT NULL UNIQUE,template_id BIGINT NOT NULL,kind VARCHAR(30) NOT NULL,source_type VARCHAR(30) NOT NULL,source_id BIGINT NOT NULL,quantity INT NOT NULL,qualified_quantity INT NOT NULL,results JSON NOT NULL,status VARCHAR(30) NOT NULL,actor_id BIGINT NOT NULL',
'legacy_link':'source_type VARCHAR(40) NOT NULL,source_id BIGINT NOT NULL,plan_id BIGINT NOT NULL,UNIQUE KEY(source_type,source_id)',
'audit':'actor_id BIGINT NOT NULL,resource VARCHAR(60) NOT NULL,action VARCHAR(30) NOT NULL,record_id BIGINT NULL,details TEXT NULL'
}
sql=['-- Stage one: additive migration. No DROP/TRUNCATE and no inventory posting.','SET NAMES utf8mb4;']
sql += ["SET @next_task_id=(SELECT GREATEST(COALESCE((SELECT MAX(id) FROM t_production_task),0),COALESCE((SELECT MAX(task_id) FROM t_production_report),0))+1);", "SET @task_sequence_sql=CONCAT('ALTER TABLE t_production_task AUTO_INCREMENT=',@next_task_id);", "PREPARE task_sequence FROM @task_sequence_sql;", "EXECUTE task_sequence;", "DEALLOCATE PREPARE task_sequence;"]
for t,cols in ddl.items():sql.append(f'CREATE TABLE IF NOT EXISTS mm_{t} (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,{cols},created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;')

def seed(table,values,key):
    cols=','.join(values);vals=','.join("NULL" if v is None else str(v) if isinstance(v,(int,float)) else "'"+v.replace("'","''")+"'" for v in values.values())
    pred=' AND '.join(k+'='+ (str(values[k]) if isinstance(values[k],(int,float)) else "'"+str(values[k]).replace("'","''")+"'") for k in key)
    sql.append(f'INSERT INTO {table} ({cols}) SELECT {vals} WHERE NOT EXISTS (SELECT 1 FROM {table} WHERE {pred});')
for name in ['只','个','块','根','颗','套','张']:
    seed('t_uom',{'uom_code':'S1-U'+str(['只','个','块','根','颗','套','张'].index(name)+1),'uom_name':name,'status':'ACTIVE','is_deleted':0},['uom_name'])
seed('t_product',dict(product_code='PRD-MOUSE-A100',product_name='A100有线办公鼠标',product_type='有线鼠标',model='A100',specification='USB有线、1200DPI、3键',unit='只',cost_price=42,selling_price=89,status=1),['product_code'])
materials=[('MAT-SHELL-TOP-A100','鼠标上盖','个',1),('MAT-SHELL-BOTTOM-A100','鼠标下盖','个',1),('MAT-PCB-A100','PCB主板','块',1),('MAT-SWITCH-01','微动开关','个',2),('MAT-ENCODER-01','滚轮编码器','个',1),('MAT-USB-CABLE-A100','USB连接线','根',1),('MAT-WHEEL-A100','滚轮组件','个',1),('MAT-LENS-A100','光学透镜','个',1),('MAT-SCREW-A100','固定螺钉','颗',4),('MAT-FEET-A100','防滑脚贴','套',1),('MAT-LABEL-A100','产品铭牌','张',1),('MAT-BOX-A100','包装盒','个',1)]
for code,name,unit,qty in materials+[('MAT-SEMI-E-A100','A100电子组件','个',1),('MAT-SEMI-S-A100','A100结构组件','个',1),('MAT-FG-A100','A100鼠标成品','只',1)]:
    seed('t_material',dict(material_code=code,material_name=name,material_type=2 if 'SEMI' in code else 3 if '-FG-' in code else 1,unit=unit,safety_stock=0,current_stock=0,status=1),['material_code'])
seed('mm_workshop',dict(code='WS-A100',name='鼠标装配车间'),['code'])
seed('t_equipment',dict(equipment_code='EQ-A100-01',equipment_name='A100功能测试台',model='USB测试台',location='总装测试站',status=1,is_deleted=0),['equipment_code'])
sql += ["SET @prod=(SELECT id FROM t_product WHERE product_code='PRD-MOUSE-A100');","SET @root=(SELECT id FROM t_material WHERE material_code='MAT-FG-A100');","SET @ws=(SELECT id FROM mm_workshop WHERE code='WS-A100');","SET @equipment=(SELECT id FROM t_equipment WHERE equipment_code='EQ-A100-01');","SET @owner=(SELECT id FROM t_user WHERE deleted=0 AND status<>0 ORDER BY id LIMIT 1);"]
sql += ["INSERT INTO mm_product_material(product_id,material_id) SELECT @prod,@root WHERE NOT EXISTS(SELECT 1 FROM mm_product_material WHERE product_id=@prod);",
"INSERT INTO mm_route(code,name,product_id,version) SELECT 'ROUTE-A100-V1','A100装配工艺',@prod,'1.0' WHERE NOT EXISTS(SELECT 1 FROM mm_route WHERE code='ROUTE-A100-V1');",
"SET @route=(SELECT id FROM mm_route WHERE code='ROUTE-A100-V1');",
"INSERT INTO mm_bom(code,name,product_id,root_material_id,version) SELECT 'BOM-A100-V1','A100多级BOM',@prod,@root,'1.0' WHERE NOT EXISTS(SELECT 1 FROM mm_bom WHERE code='BOM-A100-V1');",
"SET @bom=(SELECT id FROM mm_bom WHERE code='BOM-A100-V1');"]
for i,name in enumerate(['电子装配站','结构装配站','总装测试站','贴标包装站'],1):
    sql.append(f"INSERT INTO mm_station(code,name,workshop_id,equipment_id) SELECT 'ST-A100-{i}','{name}',@ws,"+('@equipment' if i==3 else 'NULL')+f" WHERE NOT EXISTS(SELECT 1 FROM mm_station WHERE code='ST-A100-{i}');")
    pre='NULL' if i<=2 else f"(SELECT id FROM (SELECT id FROM mm_operation WHERE code='OP-A100-{i-1}') p)"
    sql.append(f"INSERT INTO mm_operation(code,name,route_id,sequence_no,station_id,predecessor_id,standard_minutes,requires_qc) SELECT 'OP-A100-{i}','{name}',@route,{i*10},(SELECT id FROM mm_station WHERE code='ST-A100-{i}'),{pre},2,1 WHERE NOT EXISTS(SELECT 1 FROM mm_operation WHERE code='OP-A100-{i}');")
def edge(parent,child,qty):
    sql.append(f"INSERT INTO mm_bom_line(bom_id,parent_material_id,material_id,quantity) SELECT @bom,p.id,c.id,{qty} FROM t_material p JOIN t_material c ON c.material_code='{child}' WHERE p.material_code='{parent}' AND NOT EXISTS(SELECT 1 FROM mm_bom_line WHERE bom_id=@bom AND parent_material_id=p.id AND material_id=c.id);")
for semi in ['MAT-SEMI-E-A100','MAT-SEMI-S-A100']:edge('MAT-FG-A100',semi,1)
for i,(code,_,_,qty) in enumerate(materials):edge('MAT-SEMI-E-A100' if i in [2,3,4,5] else 'MAT-SEMI-S-A100' if i in [0,1,6,7,8] else 'MAT-FG-A100',code,qty)
for kind,label in [('INCOMING','来料'),('PROCESS','过程'),('SHIPPING','出货')]:
    sql.append(f"INSERT INTO mm_qc_template(code,name,product_id,kind,version) SELECT 'QC-A100-{kind}','A100{label}检测模板',@prod,'{kind}','1.0' WHERE NOT EXISTS(SELECT 1 FROM mm_qc_template WHERE code='QC-A100-{kind}');")
for code,name,std in [('QC-VISUAL','外观装配','零件完整、无明显裂纹'),('QC-USB','USB识别','连接后可识别输入设备'),('QC-KEY','按键滚轮','左右键与滚轮均有响应'),('QC-MOVE','指针移动','测试面移动可产生指针响应')]:
    seed('mm_qc_item',dict(code=code,name=name,standard=std),['code'])
sql.append("INSERT INTO mm_qc_line(template_id,item_id) SELECT t.id,i.id FROM mm_qc_template t CROSS JOIN mm_qc_item i WHERE t.code LIKE 'QC-A100-%' AND i.code LIKE 'QC-%' AND NOT EXISTS(SELECT 1 FROM mm_qc_line l WHERE l.template_id=t.id AND l.item_id=i.id);")
sql += ["INSERT INTO mm_team(code,name,leader_id) SELECT 'TEAM-A100','A100装配一班',@owner WHERE NOT EXISTS(SELECT 1 FROM mm_team WHERE code='TEAM-A100');",
"INSERT INTO mm_member(team_id,user_id) SELECT id,@owner FROM mm_team WHERE code='TEAM-A100' AND NOT EXISTS(SELECT 1 FROM mm_member m WHERE m.team_id=mm_team.id AND m.user_id=@owner);"]
seed('mm_shift',dict(code='DAY',name='白班',start_time='08:00',end_time='17:00'),['code'])
sql.append("INSERT INTO mm_check_plan(code,name,equipment_id,interval_days,standard,owner_id) SELECT 'CHECK-A100','USB功能测试台日点检',@equipment,1,'连接完整、供电正常、测试软件可识别',@owner WHERE NOT EXISTS(SELECT 1 FROM mm_check_plan WHERE code='CHECK-A100');")
for code,name in [('WH-A100-RM','A100原料仓'),('WH-A100-LINE','A100线边库'),('WH-A100-FG','A100成品仓')]:
    seed('t_warehouse',dict(warehouse_code=code,warehouse_name=name,status=1),['warehouse_code'])
    sql.append(f"INSERT INTO mm_area(code,name,warehouse_id) SELECT '{code}-A','默认库区',id FROM t_warehouse WHERE warehouse_code='{code}' AND NOT EXISTS(SELECT 1 FROM mm_area WHERE code='{code}-A');")
    sql.append(f"INSERT INTO mm_location(code,name,area_id) SELECT '{code}-01','一号库位',id FROM mm_area WHERE code='{code}-A' AND NOT EXISTS(SELECT 1 FROM mm_location WHERE code='{code}-01');")
sql.append("INSERT INTO mm_legacy_link(source_type,source_id,plan_id) SELECT 'ORDER',order_id,id FROM t_production_plan p WHERE order_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM mm_legacy_link l WHERE l.source_type='ORDER' AND l.source_id=p.order_id);")
sql += ["CREATE TABLE IF NOT EXISTS mm_guard(id INT PRIMARY KEY) ENGINE=InnoDB;", "INSERT IGNORE INTO mm_guard VALUES(1);", "INSERT INTO t_location(warehouse_id,location_code,location_name,status,is_deleted) SELECT a.warehouse_id,l.code,l.name,'1',0 FROM mm_location l JOIN mm_area a ON a.id=l.area_id WHERE NOT EXISTS(SELECT 1 FROM t_location old WHERE old.warehouse_id=a.warehouse_id AND old.location_code=l.code);"]
resdir=ROOT/'01-backend/src/main/resources';(resdir/'stage1-catalog.json').write_text(json.dumps(resources,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'04-database/stage1_foundation.sql').write_text('\n'.join(sql)+'\n',encoding='utf-8')
print('Generated catalog:',len(resources),'resources; migration:',len(sql),'statements')

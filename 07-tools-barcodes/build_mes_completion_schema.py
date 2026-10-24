"""Generate the additive MES completion migration and its editable resource catalog."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
catalog_path = ROOT/'01-backend/src/main/resources/stage1-catalog.json'
catalog = json.loads(catalog_path.read_text(encoding='utf-8'))
by_key = {r['key']: r for r in catalog}
by_key['equipment']['extra']['status']='NORMAL'
by_key['equipment']['where']='is_deleted=0'
ddl = []

def table(name, columns):
    ddl.append(f'CREATE TABLE IF NOT EXISTS {name}(id BIGINT PRIMARY KEY AUTO_INCREMENT,{columns}) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;')

def column(table_name, name, definition):
    ddl.append(f"CALL mm5_add_column('{table_name}','{name}','{definition.replace(chr(39), chr(39)*2)}');")

def field(name, label, kind='text', ref=None, required=False, options=None):
    return dict(name=name,label=label,type=kind,ref=ref,required=required,options=options)

def extend(key, fields):
    existing = {f['name'] for f in by_key[key]['fields']}
    by_key[key]['fields'].extend(f for f in fields if f['name'] not in existing)

def resource(key,label,table_name,fields,roles=(1,2),unique='code'):
    if key in by_key:
        return
    row=dict(key=key,label=label,table=table_name,fields=fields,roles=list(roles),unique=unique,where='1=1',extra={})
    catalog.append(row);by_key[key]=row

for tab,col,definition in [
 ('t_uom','main_unit_id','BIGINT NULL'),('t_uom','conversion_rate','DECIMAL(18,6) NOT NULL DEFAULT 1'),
 ('t_material','category_id','BIGINT NULL'),('t_material','max_stock','DECIMAL(18,2) NULL'),('t_product','category_id','BIGINT NULL'),
 ('t_material','sourcing_mode',"VARCHAR(20) NOT NULL DEFAULT 'AUTO'"),
 ('t_location','x_coordinate','INT NULL'),('t_location','y_coordinate','INT NULL'),('t_location','z_coordinate','INT NULL'),
 ('mm_location','x_coordinate','INT NULL'),('mm_location','y_coordinate','INT NULL'),('mm_location','z_coordinate','INT NULL'),
 ('mm_operation','is_key','INT NOT NULL DEFAULT 0'),('mm_operation','output_material_id','BIGINT NULL'),
 ('mm_qc_template','operation_id','BIGINT NULL'),('mm_qc_template','phase',"VARCHAR(20) NOT NULL DEFAULT 'FINAL'"),
 ('mm_qc_item','data_type',"VARCHAR(20) NOT NULL DEFAULT 'TEXT'"),('mm_qc_item','lower_limit','DECIMAL(18,6) NULL'),('mm_qc_item','upper_limit','DECIMAL(18,6) NULL'),
 ('mm_check_plan','enabled','INT NOT NULL DEFAULT 1'),('mm_check_plan','plan_type',"VARCHAR(20) NOT NULL DEFAULT 'CHECK'"),
 ('mm_rotation','cycle_unit',"VARCHAR(20) NOT NULL DEFAULT 'DAY'"),
 ('mm_equipment_placeholder','unused','INT NULL')]:
    if tab not in ('mm_equipment_placeholder','mm_rotation'): column(tab,col,definition)

table('mm_category','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,parent_id BIGINT NULL,kind VARCHAR(20) NOT NULL')
table('mm_station_resource','station_id BIGINT NOT NULL,resource_type VARCHAR(20) NOT NULL,equipment_id BIGINT NULL,user_id BIGINT NULL,name VARCHAR(100) NULL')
table('mm_sop','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,product_id BIGINT NOT NULL,operation_id BIGINT NOT NULL,version VARCHAR(32) NOT NULL,image_path VARCHAR(255) NOT NULL,enabled INT NOT NULL DEFAULT 1')
table('mm_code_rule','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,prefix VARCHAR(32) NOT NULL,width INT NOT NULL DEFAULT 6,next_value BIGINT NOT NULL DEFAULT 1')
table('mm_defect','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,severity VARCHAR(20) NOT NULL')
table('mm_check_subject','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,kind VARCHAR(20) NOT NULL,standard VARCHAR(500) NOT NULL')
table('mm_check_plan_subject','plan_id BIGINT NOT NULL,subject_id BIGINT NOT NULL,UNIQUE KEY uq_check_subject(plan_id,subject_id)')
table('mm_check_due','plan_id BIGINT NOT NULL,due_date DATE NOT NULL,status VARCHAR(20) NOT NULL DEFAULT \'PENDING\',record_id BIGINT NULL,UNIQUE KEY uq_due(plan_id,due_date)')
table('mm_holiday','work_date DATE NOT NULL UNIQUE,is_working INT NOT NULL,name VARCHAR(100) NOT NULL')
table('mm_rotation','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,start_date DATE NOT NULL,end_date DATE NOT NULL,station_id BIGINT NOT NULL,cycle_days INT NOT NULL DEFAULT 1,cycle_unit VARCHAR(20) NOT NULL DEFAULT \'DAY\',mode VARCHAR(20) NOT NULL,is_generated INT NOT NULL DEFAULT 0')
column('mm_rotation','cycle_unit',"VARCHAR(20) NOT NULL DEFAULT 'DAY'")
table('mm_rotation_team','rotation_id BIGINT NOT NULL,team_id BIGINT NOT NULL,sequence_no INT NOT NULL,UNIQUE KEY uq_rotation_team(rotation_id,team_id),UNIQUE KEY uq_rotation_seq(rotation_id,sequence_no)')
table('mm_rotation_shift','rotation_id BIGINT NOT NULL,shift_id BIGINT NOT NULL,sequence_no INT NOT NULL,UNIQUE KEY uq_rotation_shift(rotation_id,shift_id),UNIQUE KEY uq_rotation_shift_sequence(rotation_id,sequence_no)')
table('mm_position','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,department_id BIGINT NOT NULL')
table('mm_user_position','user_id BIGINT NOT NULL,position_id BIGINT NOT NULL,UNIQUE KEY uq_position(user_id,position_id)')
table('mm_menu','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,path VARCHAR(200) NOT NULL,parent_id BIGINT NULL')
table('mm_role_menu','role_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,UNIQUE KEY uq_role_menu(role_id,menu_id)')
table('mm_parameter','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,value VARCHAR(500) NOT NULL')
ddl.append("INSERT IGNORE INTO mm_parameter(code,name,value) VALUES('APP_NAME','系统显示名称','Mouse MES'),('NOTICE_LIMIT','公告最多显示条数','200'),('DEFAULT_DURATION_MINUTES','默认任务时长（分钟）','60');")
table('mm_notice','title VARCHAR(200) NOT NULL,content TEXT NOT NULL,kind VARCHAR(20) NOT NULL,target_role BIGINT NULL,published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,expires_at DATETIME NULL,actor_id BIGINT NOT NULL')
table('mm_notice_read','notice_id BIGINT NOT NULL,user_id BIGINT NOT NULL,read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uq_read(notice_id,user_id)')
table('mm_line_stock','work_order_id BIGINT NOT NULL,material_id BIGINT NOT NULL,lot_id BIGINT NOT NULL,quantity DECIMAL(18,6) NOT NULL DEFAULT 0,UNIQUE KEY uq_line(work_order_id,material_id,lot_id),CHECK(quantity>=0)')
table('mm_line_movement','work_order_id BIGINT NOT NULL,material_id BIGINT NOT NULL,lot_id BIGINT NOT NULL,quantity DECIMAL(18,6) NOT NULL,kind VARCHAR(32) NOT NULL,source_id BIGINT NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uq_line_move(kind,source_id,material_id,lot_id)')
table('mm_task_consumption','operation_id BIGINT NOT NULL,material_id BIGINT NOT NULL,quantity_per_unit DECIMAL(18,6) NOT NULL,UNIQUE KEY uq_consume(operation_id,material_id)')
table('mm_work_consumption','work_order_id BIGINT NOT NULL,operation_id BIGINT NOT NULL,material_id BIGINT NOT NULL,quantity_per_unit DECIMAL(18,6) NOT NULL,UNIQUE KEY uq_work_consume(work_order_id,operation_id,material_id)')
table('mm_work_output','work_order_id BIGINT NOT NULL,operation_id BIGINT NOT NULL,material_id BIGINT NULL,is_key INT NOT NULL,UNIQUE KEY uq_work_output(work_order_id,operation_id)')
table('mm_nc','inspection_id BIGINT NOT NULL UNIQUE,task_id BIGINT NULL,defect_id BIGINT NULL,quantity DECIMAL(18,2) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT \'OPEN\',decision VARCHAR(20) NULL,reason VARCHAR(500) NULL,approved_by BIGINT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP')
table('mm_rework','nc_id BIGINT NOT NULL UNIQUE,task_id BIGINT NOT NULL,status VARCHAR(20) NOT NULL DEFAULT \'PENDING\',result VARCHAR(500) NULL,completed_by BIGINT NULL,completed_at DATETIME NULL')
table('mm_document','code VARCHAR(80) NOT NULL UNIQUE,kind VARCHAR(32) NOT NULL,source_id BIGINT NULL,warehouse_id BIGINT NOT NULL,status VARCHAR(20) NOT NULL DEFAULT \'DRAFT\',reason VARCHAR(500) NULL,actor_id BIGINT NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,posted_at DATETIME NULL')
column('mm_document','name','VARCHAR(100) NULL')
column('mm_document','business_date','DATE NULL')
table('mm_report_sheet',"code VARCHAR(80) NOT NULL UNIQUE,task_id BIGINT NOT NULL,reporter_id BIGINT NOT NULL,good_quantity INT NOT NULL,bad_quantity INT NOT NULL,report_time DATETIME NOT NULL,remark VARCHAR(500) NULL,created_by BIGINT NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'PENDING',approved_by BIGINT NULL,approval_note VARCHAR(500) NULL,report_id BIGINT NULL,approved_at DATETIME NULL")
table('mm_document_line','document_id BIGINT NOT NULL,material_id BIGINT NOT NULL,lot_id BIGINT NOT NULL,location_id BIGINT NULL,quantity DECIMAL(18,2) NOT NULL,source_line_id BIGINT NULL,CHECK(quantity>0)')
column('mm_document_line','input_unit_id','BIGINT NULL')
column('mm_document_line','entered_quantity','DECIMAL(18,6) NULL')
table('mm_document_check','line_id BIGINT NOT NULL,template_id BIGINT NOT NULL,quantity DECIMAL(18,2) NOT NULL,qualified_quantity DECIMAL(18,2) NOT NULL,results JSON NOT NULL,status VARCHAR(20) NOT NULL,actor_id BIGINT NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP')
table('mm_package','code VARCHAR(80) NOT NULL UNIQUE,customer_id BIGINT NULL,order_id BIGINT NULL,length_mm DECIMAL(18,2) NOT NULL,width_mm DECIMAL(18,2) NOT NULL,height_mm DECIMAL(18,2) NOT NULL,net_weight DECIMAL(18,2) NOT NULL,gross_weight DECIMAL(18,2) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT \'DRAFT\',parent_id BIGINT NULL,created_by BIGINT NOT NULL')
table('mm_package_item','package_id BIGINT NOT NULL,inventory_id BIGINT NOT NULL,quantity DECIMAL(18,2) NOT NULL,UNIQUE KEY uq_package_item(package_id,inventory_id)')
table('mm_task_slot','task_id BIGINT NOT NULL UNIQUE,station_id BIGINT NOT NULL,owner_id BIGINT NOT NULL,start_at DATETIME NOT NULL,end_at DATETIME NOT NULL,quantity INT NOT NULL')
table('mm_work_relation','parent_work_order_id BIGINT NOT NULL,child_work_order_id BIGINT NOT NULL UNIQUE,material_id BIGINT NOT NULL')
table('mm_job_run','code VARCHAR(80) NOT NULL,started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,finished_at DATETIME NULL,status VARCHAR(20) NOT NULL,error VARCHAR(500) NULL')
table('mm_job_config','code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(100) NOT NULL,enabled INT NOT NULL DEFAULT 1,interval_seconds INT NOT NULL,last_run DATETIME NULL')
ddl.append("INSERT IGNORE INTO mm_job_config(code,name,interval_seconds) VALUES('EQUIPMENT_CHECK','设备到期任务',60),('REPORT_QC','报工质检事件',2);")

extend('units',[field('main_unit_id','主单位（留空表示主单位）','ref','units'),field('conversion_rate','相对主单位换算率','positive',required=True)])
extend('materials',[field('category_id','分类','ref','categories'),field('max_stock','最大库存','number')])
extend('materials',[field('sourcing_mode','供应方式','select',options={'AUTO':'按物料类型与BOM确定','BUY':'外购','MAKE':'自制','BOTH':'可外购可自制'})])
extend('products',[field('category_id','分类','ref','categories')])
extend('locations',[field(n,l,'number') for n,l in [('x_coordinate','货架X'),('y_coordinate','货架Y'),('z_coordinate','货架Z')]])
extend('operations',[field('is_key','关键工序','select',required=True,options={'0':'否','1':'是'}),field('output_material_id','产出半成品/成品','ref','materials')])
extend('qc-items',[field('data_type','结果类型','select',required=True,options={'TEXT':'人工判定','NUMBER':'数值范围'}),field('lower_limit','下限','decimal'),field('upper_limit','上限','decimal')])
extend('qc-templates',[field('operation_id','适用工序','ref','operations'),field('phase','检验阶段','select',required=True,options={'FIRST':'首检','LAST':'末检','PATROL':'巡检','FINAL':'成品/最终检验'})])
extend('checks',[field('enabled','是否启用','select',required=True,options={'0':'停用','1':'启用'}),field('plan_type','计划类型','select',required=True,options={'CHECK':'点检','MAINTAIN':'保养'})])
resource('categories','物料产品分类','mm_category',[field('code','编码',required=True),field('name','名称',required=True),field('parent_id','父分类','ref','categories'),field('kind','类别','select',required=True,options={'MATERIAL':'物料','PRODUCT':'产品'})],(1,5))
resource('station-resources','工作站资源','mm_station_resource',[field('station_id','工作站','ref','stations',True),field('resource_type','资源类型','select',required=True,options={'EQUIPMENT':'设备','USER':'人员','TOOL':'工装'}),field('equipment_id','设备','ref','equipment'),field('user_id','人员','ref','users'),field('name','工装名称')],unique=None)
resource('sops','工序作业指导书','mm_sop',[field('code','编码',required=True),field('name','名称',required=True),field('product_id','产品','ref','products',True),field('operation_id','工序','ref','operations',True),field('version','版本',required=True),field('image_path','SOP图片','image',required=True),field('enabled','启用','select',required=True,options={'0':'停用','1':'启用'})])
resource('code-rules','编码规则','mm_code_rule',[field('code','规则标识',required=True),field('name','名称',required=True),field('prefix','前缀',required=True),field('width','流水位数','positive',required=True),field('next_value','下一流水号','positive',required=True)],(11,))
resource('defects','常见缺陷','mm_defect',[field('code','编码',required=True),field('name','缺陷名称',required=True),field('severity','严重度','select',required=True,options={'MINOR':'轻微','MAJOR':'严重','CRITICAL':'致命'})],(4,))
resource('check-subjects','点检保养项目','mm_check_subject',[field('code','编码',required=True),field('name','名称',required=True),field('kind','类型','select',required=True,options={'CHECK':'点检','MAINTAIN':'保养'}),field('standard','执行标准',required=True)],(8,))
resource('check-plan-subjects','计划关联项目','mm_check_plan_subject',[field('plan_id','计划','ref','checks',True),field('subject_id','项目','ref','check-subjects',True)],(8,),None)
resource('holidays','节假日与调休','mm_holiday',[field('work_date','日期','date',required=True),field('is_working','工作状态','select',required=True,options={'0':'休假','1':'上班'}),field('name','说明',required=True)],(1,2),None)
resource('rotations','轮班计划','mm_rotation',[field('code','编码',required=True),field('name','名称',required=True),field('start_date','开始日期','date',required=True),field('end_date','结束日期','date',required=True),field('station_id','工作站','ref','stations',True),field('cycle_days','倒班周期数','positive',required=True),field('cycle_unit','周期单位','select',required=True,options={'DAY':'天','WEEK':'周','MONTH':'月','QUARTER':'季度'}),field('mode','轮班方式','select',required=True,options={'DAY':'长白班','TWO':'两班倒','THREE':'三班倒'})])
extend('rotations',[field('cycle_unit','周期单位','select',required=True,options={'DAY':'天','WEEK':'周','MONTH':'月','QUARTER':'季度'})])
resource('rotation-teams','轮班参与班组','mm_rotation_team',[field('rotation_id','轮班计划','ref','rotations',True),field('team_id','班组','ref','teams',True),field('sequence_no','轮班顺序','positive',required=True)],unique=None)
resource('rotation-shifts','轮班关联班次','mm_rotation_shift',[field('rotation_id','轮班计划','ref','rotations',True),field('shift_id','班次','ref','shifts',True),field('sequence_no','班次顺序','positive',required=True)],unique=None)
resource('consumption','工序物料消耗','mm_task_consumption',[field('operation_id','工序','ref','operations',True),field('material_id','消耗物料','ref','materials',True),field('quantity_per_unit','单件消耗','positive',required=True)],unique=None)
resource('positions','岗位','mm_position',[field('code','编码',required=True),field('name','名称',required=True),field('department_id','部门ID','positive',required=True)],(11,))
resource('user-positions','员工岗位','mm_user_position',[field('user_id','员工','ref','users',True),field('position_id','岗位','ref','positions',True)],(11,),None)
resource('menus','菜单配置','mm_menu',[field('code','编码',required=True),field('name','名称',required=True),field('path','页面路径',required=True),field('parent_id','父菜单','ref','menus')],(11,))
resource('role-menus','角色菜单授权','mm_role_menu',[field('role_id','角色ID','positive',required=True),field('menu_id','菜单','ref','menus',True)],(11,),None)
resource('parameters','系统参数','mm_parameter',[field('code','参数标识',required=True),field('name','参数名称',required=True),field('value','参数值',required=True)],(11,))
resource('jobs','后台任务调度','mm_job_config',[field('code','任务标识',required=True),field('name','任务名称',required=True),field('enabled','是否启用','select',required=True,options={'0':'停用','1':'启用'}),field('interval_seconds','执行间隔秒','positive',required=True)],(11,))
for key,name,ref in [('positions','department_id','departments'),('role-menus','role_id','roles')]:
    for f in by_key[key]['fields']:
        if f['name']==name:f.update(type='ref',ref=ref)

prefix = """-- Additive MES completion migration. Back up before applying. No existing business rows are removed.
DROP PROCEDURE IF EXISTS mm5_add_column;
DELIMITER $$
CREATE PROCEDURE mm5_add_column(IN tab_name VARCHAR(64),IN col_name VARCHAR(64),IN col_def TEXT)
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tab_name AND column_name=col_name) THEN
  SET @ddl=CONCAT('ALTER TABLE `',tab_name,'` ADD COLUMN `',col_name,'` ',col_def);
  PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
 END IF;
END$$
DELIMITER ;
"""
ddl.append("""SET @old_unique=(SELECT index_name FROM(SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) cols FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='mm_task_context' AND non_unique=0 AND index_name<>'PRIMARY' GROUP BY index_name)x WHERE cols='work_order_id,operation_id' LIMIT 1);
SET @drop_unique=IF(@old_unique IS NULL,'SELECT 1',CONCAT('ALTER TABLE mm_task_context DROP INDEX `',@old_unique,'`'));
PREPARE s FROM @drop_unique; EXECUTE s; DEALLOCATE PREPARE s;""")
ddl.append("UPDATE t_equipment SET status='NORMAL' WHERE status='1'; -- Preserve the documented meaning of legacy status 1.")
ddl.append("UPDATE mm_operation o JOIN(SELECT route_id,MAX(sequence_no) last_seq FROM mm_operation GROUP BY route_id HAVING SUM(is_key)=0)x ON x.route_id=o.route_id AND x.last_seq=o.sequence_no SET o.is_key=1; -- Legacy routes used the last operation as their completion measure.")
seeds=["INSERT IGNORE INTO t_role(id,role_code,role_name,role_desc,sort,status) VALUES(11,'111','系统管理员','系统配置与运行维护',11,1);"]
for code,prefix_value in [('ITEM_CODE','MAT-'),('VENDOR_CODE','SUP-'),('CLIENT_CODE','CUS-'),('WORKSHOP_CODE','WS-'),('WORKSTATION_CODE','ST-'),('WAREHOUSE_CODE','WH-'),('AREA_CODE','AR-'),('LOCATION_CODE','LOC-'),('ISSUE_CODE','ISS-'),('WORKORDER_CODE','MO-'),('PACKAGE_CODE','PKG-'),('DOCUMENT_CODE','DOC-')]:
    seeds.append(f"INSERT IGNORE INTO mm_code_rule(code,name,prefix,width,next_value) VALUES('{code}','{code}','{prefix_value}',6,1);")
for code,prefix_value in [('SUBJECT_CODE','SUBJ-'),('REPORT_CODE','RPT-'),('UOM_CODE','UOM-'),('PRODUCT_CODE','PRD-'),('PROCESS_CODE','OP-'),('ROUTE_CODE','ROUTE-'),('CHECKPLAN_CODE','CHECK-'),('MACHINERY_CODE','EQ-'),('QC_TEMPLATR_CODE','QCT-'),('QC_ITEM_CODE','QCI-'),('DEFECT_CODE','DF-'),('BOM_CODE','BOM-'),('TEAM_CODE','TEAM-'),('CAL_PLAN_CODE','CAL-'),('SOP_CODE','SOP-'),('ITEMRECPT_CODE','PR-'),('WM_RTVENDOR_CODE','P-RET-'),('RTISSUE_CODE','L-RET-'),('PRODUCTRECPT_CODE','FG-IN-'),('PRODUCTSALSE_CODE','SHIP-'),('RTSALSE_CODE','S-RET-'),('QC_IQC_CODE','IQC-'),('IPQC_CODE','IPQC-'),('OQC_CODE','OQC-'),('REPAIR_CODE','REPAIR-')]:
    seeds.append(f"INSERT IGNORE INTO mm_code_rule(code,name,prefix,width,next_value) VALUES('{code}','{code}','{prefix_value}',6,1);")
(ROOT/'04-database/mes_completion.sql').write_text(prefix+'\n'.join(ddl)+'\nDROP PROCEDURE mm5_add_column;\n'+'\n'.join(seeds)+'\n',encoding='utf-8')
catalog_path.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Generated additive migration and completion catalog.')

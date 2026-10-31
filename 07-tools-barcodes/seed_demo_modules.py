"""Top up the recorded isolated demo through business APIs; never targets production."""
from stage1_database import ROOT,sql,config,env
from pathlib import Path
import json,datetime,subprocess,urllib.request,urllib.error

DB=json.loads((ROOT/'.runtime/mes-completion-database.json').read_text())['database']
assert DB.startswith('mouse_mes_completion_test_')
BASE='http://127.0.0.1:8082/api'
stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
backup=ROOT/f'.runtime/backups/demo_before_topup_{stamp}.sql'
backup.parent.mkdir(parents=True,exist_ok=True)
with backup.open('wb') as out:
 p=subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot','--single-transaction','--default-character-set=utf8mb4',DB],env=env,stdout=out,stderr=subprocess.PIPE)
assert p.returncode==0 and backup.stat().st_size>1000,'Backup failed'
def api(path,body=None,method=None):
 req=urllib.request.Request(BASE+path,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),method=method or ('GET' if body is None else 'POST'),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if 'token' in globals() else {})})
 try:
  with urllib.request.urlopen(req,timeout=30) as r: data=json.load(r)
 except urllib.error.HTTPError as e:raise RuntimeError(path+': '+e.read().decode())
 if data.get('code',200)!=200:raise RuntimeError(path+': '+data.get('msg','failed'))
 return data.get('data',data)
token=api('/auth/login',{'userCode':'109001','password':'123456'})['token']
def scalar(q):return sql(q,DB).strip().splitlines()[-1]
def rows(q):return [json.loads(r) for r in sql(q,DB).splitlines()[1:]]
def get(k):return api('/foundation/'+k)
def save(k,p):return api('/foundation/'+k,p)
created={};failures={};counts={}
def top(k,make,target=10):
 try:
  existing=get(k)
  for i in range(len(existing)+1,target+1):
   save(k,make(i));created[k]=created.get(k,0)+1
  counts[k]=len(get(k))
 except Exception as e:failures[k]=str(e)
 checkpoint()
def checkpoint():
 (ROOT/'.runtime/demo-topup-report.json').write_text(json.dumps(dict(database=DB,backup=str(backup),created=created,counts=counts,failures=failures),ensure_ascii=False,indent=2),encoding='utf-8')
def idof(k,i=0):return get(k)[i%len(get(k))]['id']
people=api('/mes/people')
worker=next(p['id'] for p in people if p['roleId']==3)
engineer=next(p['id'] for p in people if p['roleId']==8)
admin=next(p['id'] for p in people if p['roleId']==9)
now=datetime.datetime.now();today=now.date()
unit=get('units')[0]['uom_name']
top('customers',lambda i:dict(customer_name=['华东智联科技','苏州云帆电子','杭州启明办公','深圳星河数码','南京锐捷商贸','合肥科创信息','成都远航科技','武汉蓝海电子','长沙联创办公','厦门海辰科技'][i-1],contact_person='演示联系人'+str(i),phone='1380000'+str(i).zfill(4),address='演示客户地址'))
# Complete demo customer email addresses without replacing existing contact data.
from fill_demo_customer_emails import fill_customer_emails
fill_customer_emails(DB)

top('suppliers',lambda i:dict(supplier_name='演示供应商·'+['精密塑胶','电路板','微动开关','光学传感','线材连接','电子元件','包装材料','紧固件','喷涂材料','五金模具'][i-1],contact_person='供应联系'+str(i),contact_phone='1390000'+str(i).zfill(4)))
top('products',lambda i:dict(product_name='演示M'+str(i)+'无线办公鼠标',model='M'+str(i),unit=unit,cost_price=35+i,selling_price=89+i,specification='2.4G / USB'))
top('equipment',lambda i:dict(equipment_name=['注塑成型机','自动锁螺丝机','激光打标机','按键寿命测试台','光学定位测试台','自动包装机','超声波焊接机','回流焊设备','精密点胶机','外观视觉检测台'][i-1]+'（演示）',model='MES-E'+str(i),location='鼠标制造演示车间'))
top('warehouses',lambda i:dict(warehouse_name='演示'+['原料','半成品','成品','退货','包装材料','备件','待检','不合格品','委外','辅料'][i-1]+'仓',location='演示园区'+str(i)+'区'))
top('workshops',lambda i:dict(name='演示'+['注塑','电子装配','整机组装','测试','包装','返修','线束','表面处理','自动化','试制'][i-1]+'车间'))
top('stations',lambda i:dict(name='演示工作站'+str(i),workshop_id=idof('workshops',i-1),equipment_id=idof('equipment',i-1)))
top('areas',lambda i:dict(name='演示库区'+str(i),warehouse_id=idof('warehouses',i-1)))
top('locations',lambda i:dict(name='演示货架位'+str(i),area_id=idof('areas',i-1),x_coordinate=i,y_coordinate=1,z_coordinate=1))
top('categories',lambda i:dict(name='演示分类'+str(i),kind='MATERIAL' if i%2 else 'PRODUCT'))
top('teams',lambda i:dict(name='演示生产班组'+str(i),leader_id=worker))
top('members',lambda i:dict(team_id=idof('teams',i-1),user_id=worker))
top('shifts',lambda i:dict(name='演示班次'+str(i),start_time=f'{(i+5)%24:02}:00',end_time=f'{(i+13)%24:02}:00'))
top('checks',lambda i:dict(name='演示设备'+str(i)+'日常点检',equipment_id=idof('equipment',i-1),owner_id=engineer,interval_days=7,standard='检查紧固、润滑、电气连接与安全防护；符合设备操作规范',enabled=1,plan_type='CHECK'))
top('qc-items',lambda i:dict(name='演示检验项'+str(i),standard='外观完整，装配牢固，功能正常',data_type='TEXT'))
top('defects',lambda i:dict(name=['外壳划伤','按键卡滞','滚轮异响','定位漂移','线材破损','接触不良','装配间隙','印字偏移','包装破损','配件缺失'][i-1],severity='MAJOR'))
top('check-subjects',lambda i:dict(code='DEMO-CHECK-'+str(i),name='演示点检项目'+str(i),kind='CHECK',standard='目视、手动及仪表确认，符合设备规范'))
top('check-plan-subjects',lambda i:dict(plan_id=idof('checks',i-1),subject_id=idof('check-subjects',i-1)))
top('check-records',lambda i:dict(plan_id=idof('checks',i-1),checked_at=(now-datetime.timedelta(days=i)).isoformat(timespec='seconds'),actual_value='演示点检：紧固正常，防护完好，无异常噪声',result='PASS',remark='答辩演示记录'))
top('maintenance-plans',lambda i:dict(name='演示周期保养计划'+str(i),equipment_id=idof('equipment',i-1),owner_id=engineer,anchor_at=(now-datetime.timedelta(days=1,hours=i)).isoformat(timespec='seconds'),frequency=1,cycle_unit='MONTH',remind_days=3,content='演示保养：清洁、润滑、紧固、电气检查与安全联锁测试',enabled='1'))
top('holidays',lambda i:dict(work_date=(today+datetime.timedelta(days=40+i)).isoformat(),is_working=1,name='演示生产日历'+str(i)))
top('positions',lambda i:dict(code='DEMO-POS-'+str(i),name='演示岗位'+str(i),department_id=idof('departments',i-1)))
top('user-positions',lambda i:dict(position_id=idof('positions',i-1),user_id=worker))
top('station-resources',lambda i:dict(station_id=idof('stations',i-1),resource_type='EQUIPMENT',equipment_id=idof('equipment',i-1),name='演示设备资源'+str(i)))
top('routes',lambda i:dict(name='演示工艺路线'+str(i),product_id=idof('products',i-1),version='DEMO-1'))
unused_routes=[r for r in get('routes') if r['version']=='DEMO-1']
top('operations',lambda i:dict(name='演示装配工序'+str(i),route_id=unused_routes[(i-1)%len(unused_routes)]['id'],sequence_no=10+i,station_id=idof('stations',i-1),standard_minutes=5,requires_qc=0,is_key=0))
top('schedules',lambda i:dict(team_id=idof('teams',i-1),shift_id=idof('shifts',i-1),station_id=idof('stations',i-1),start_at=(datetime.datetime.combine(today+datetime.timedelta(days=30+i),datetime.time(8))).isoformat(),end_at=(datetime.datetime.combine(today+datetime.timedelta(days=30+i),datetime.time(16))).isoformat()))
top('rotations',lambda i:dict(name='演示轮班计划'+str(i),start_date=(today+datetime.timedelta(days=60+i*3)).isoformat(),end_date=(today+datetime.timedelta(days=61+i*3)).isoformat(),station_id=idof('stations',i-1),cycle_days=1,mode='DAY',cycle_unit='DAY'))
top('rotation-teams',lambda i:dict(rotation_id=idof('rotations',i-1),team_id=idof('teams',i-1),sequence_no=1))
top('rotation-shifts',lambda i:dict(rotation_id=idof('rotations',i-1),shift_id=idof('shifts',i-1),sequence_no=1))

def business(label,count,make):
 try:
  n=count()
  for i in range(n+1,11):make(i);created[label]=created.get(label,0)+1
  counts[label]=count()
 except Exception as e:failures[label]=str(e)
 checkpoint()
def cnt(table,where='1=1'):return int(scalar(f'SELECT COUNT(*) FROM {table} WHERE {where}'))
customers=get('customers');products=get('products')
business('销售订单',lambda:cnt('t_order','deleted=0'),lambda i:api('/orders',dict(orderNo='DEMO-SO-'+stamp+'-'+str(i),customerName=customers[(i-1)%len(customers)]['customer_name'],productName=products[0]['product_name'],quantity=20+i*5,unitPrice=99,totalAmount=(20+i*5)*99)))
business('公告通知',lambda:cnt('mm_notice'),lambda i:api('/mes/notices',dict(title='演示公告'+str(i)+'：'+['生产排程安排','设备保养提醒','质量改善要点','仓储盘点安排','安全生产提示','交付计划更新','物料齐套提醒','新员工操作培训','班组交接提醒','包装规范更新'][i-1],content='答辩演示内容：请对应岗位查看当日任务，按标准流程执行并保留记录。')))
business('装箱管理',lambda:cnt('mm_package'),lambda i:api('/mes/packages',dict(customer_id=customers[(i-1)%len(customers)]['id'],length_mm=300,width_mm=200,height_mm=150,net_weight=1,gross_weight=1.2)))
try:api('/maintenances/periodic/generate',{});counts['周期保养任务']=cnt('mm_maintenance_occurrence')
except Exception as e:failures['周期保养任务']=str(e)

# Imported historical attendance is explicitly labeled demonstration, not generated audit events.
for i in range(cnt('t_attendance')+1,11):
 day=today-datetime.timedelta(days=i)
 sql(f"INSERT INTO t_attendance(user_id,user_name,attendance_date,check_in_time,check_out_time,work_hours,status,remark) SELECT id,real_name,'{day}','{day} 08:00:00','{day} 17:00:00',8,1,'答辩演示历史考勤' FROM t_user WHERE id={worker}",DB)
 created['考勤记录']=created.get('考勤记录',0)+1
counts['考勤记录']=cnt('t_attendance')
checkpoint()

# Fill relation lists using unused pairs rather than duplicating unique assignments.
for key,left,right in [('members','team_id','user_id'),('user-positions','user_id','position_id')]:
 try:
  for team in get('teams') if key=='members' else people:
   if len(get(key))>=10:break
   if key=='members' and int(scalar(f"SELECT COUNT(*) FROM mm_schedule WHERE team_id={team['id']}")):continue
   for person in people if key=='members' else get('positions'):
    a,b=team['id'],person['id']
    if any(r[left]==a and r[right]==b for r in get(key)):continue
    save(key,{left:a,right:b});created[key]=created.get(key,0)+1
    if len(get(key))>=10:break
  counts[key]=len(get(key));assert counts[key]>=10
  failures.pop(key,None)
 except Exception as e:failures[key]=str(e)
for key,column,ref in [('rotation-teams','team_id','teams'),('rotation-shifts','shift_id','shifts')]:
 try:
  for rotation in get('rotations'):
   if len(get(key))>=10:break
   if any(r['rotation_id']==rotation['id'] for r in get(key)):continue
   save(key,dict(rotation_id=rotation['id'],**{column:idof(ref)},sequence_no=1));created[key]=created.get(key,0)+1
  counts[key]=len(get(key));assert counts[key]>=10
  failures.pop(key,None)
 except Exception as e:failures[key]=str(e)
basebom=next(b for b in get('bom-versions') if b['code']=='BOM-A100-V1')
top('bom-versions',lambda i:dict(name='演示A100物料清单版本'+str(i),product_id=basebom['product_id'],root_material_id=basebom['root_material_id'],version='DEMO-'+str(i)))
for b in get('bom-versions'):
 if not b['version'].startswith('DEMO-'):continue
 if any(x['bom_id']==b['id'] for x in get('bom-lines')):continue
 for line in [x for x in get('bom-lines') if x['bom_id']==basebom['id']]:
  save('bom-lines',{k:line[k] for k in ['parent_material_id','material_id','quantity']}|{'bom_id':b['id']})
counts['bom-lines']=len(get('bom-lines'))
base_sop=get('sops')[0]
top('sops',lambda i:dict(name='演示装配作业指导书'+str(i),product_id=base_sop['product_id'],operation_id=base_sop['operation_id'],version='DEMO-'+str(i),image_path=base_sop['image_path'],enabled=0))

# Each warehouse module gets real source-linked drafts; no phantom stock movements.
for kind in ['PURCHASE_RECEIPT','PURCHASE_RETURN','LINE_ISSUE','LINE_RETURN','PRODUCTION_RECEIPT','SALES_SHIPMENT','SALES_RETURN']:
 try:
  existing=api('/mes/documents');candidates=[r for r in existing if r['kind']==kind and r['status']=='POSTED']
  assert candidates,'No valid source template'
  for i in range(len([r for r in existing if r['kind']==kind])+1,11):
   template=candidates[(i-1)%len(candidates)]
   lines=api('/mes/documents/'+str(template['id'])+'/lines')
   chosen=lines[0]
   payload=dict(kind=kind,source_id=template['source_id'],warehouse_id=template['warehouse_id'],name='演示'+kind+'草稿'+str(i),reason='答辩演示草稿：待核对后按业务流程处理',lines=[dict(material_id=chosen['material_id'],quantity=1,lot_id=chosen['lot_id'],location_id=chosen.get('location_id'))])
   if kind=='PURCHASE_RECEIPT':
    # Use a new approved purchase source with outstanding quantity.
    p=api('/purchase-order/add',dict(poNo='DEMO-PO-'+stamp+'-'+str(i),supplierId=idof('suppliers',i-1),status='DRAFT',items=[dict(materialId=chosen['material_id'],quantity=20,unitPrice=2,amount=40)]))
    api('/purchase-order/'+str(p['id'])+'/submit',{},'PUT');api('/purchase-order/'+str(p['id'])+'/approve',{},'PUT');payload['source_id']=p['id']
    payload['lines'][0].pop('lot_id');payload['lines'][0]['lot_no']='DEMO-LOT-'+stamp+'-'+str(i)
   if kind=='LINE_ISSUE':
    route=next(r for r in get('routes') if r['code']=='ROUTE-A100-V1')
    work=api('/mes/work-orders',dict(product_id=basebom['product_id'],bom_id=basebom['id'],route_id=route['id'],quantity=10,owner_id=worker))
    payload['source_id']=work['id']
   # Completed sales/purchase sources may have no remaining capacity. Search other originals.
   accepted=False;last_error=''
   for trial in [payload]+[dict(payload,source_id=t['source_id'],warehouse_id=t['warehouse_id'],lines=[dict(material_id=l['material_id'],quantity=0.1 if kind=='LINE_RETURN' else 1,lot_id=l['lot_id'],location_id=l.get('location_id'))]) for t in candidates for l in api('/mes/documents/'+str(t['id'])+'/lines')][:30]:
    try:api('/mes/documents',trial);accepted=True;break
    except Exception as e:last_error=str(e)
   if not accepted:raise RuntimeError(last_error)
   created[kind]=created.get(kind,0)+1
  counts[kind]=len([r for r in api('/mes/documents') if r['kind']==kind]);failures.pop(kind,None)
 except Exception as e:failures[kind]=str(e)
 checkpoint()

# Clearly labeled imported quality examples, not invented production report approvals.
product=get('products')[0];qc=next(p for p in people if p['roleId']==4)
for i in range(cnt('t_quality_standard')+1,11):
 sql(f"INSERT INTO t_quality_standard(product_id,product_name,standard_name,standard_content,qualified_threshold,status) VALUES({product['id']},'{product['product_name']}','演示质量标准{i}','按作业指导书检查外观、按键、滚轮、定位与包装',98,1)",DB)
for i in range(cnt('t_quality_check')+1,11):
 bad=1 if i%3==0 else 0
 sql(f"INSERT INTO t_quality_check(check_no,product_id,product_name,check_quantity,qualified_quantity,defect_quantity,qualified_rate,check_result,checker_id,checker_name,defect_desc,handle_result) SELECT 'DEMO-QC-{stamp}-{i}',{product['id']},'{product['product_name']}',50,{50-bad},{bad},{100-bad*2},{0 if bad else 1},id,real_name,'答辩演示抽检记录','演示记录，非正式生产批次放行依据' FROM t_user WHERE id={qc['id']}",DB)
for key,table in [('质量标准','t_quality_standard'),('历史质量抽检','t_quality_check')]:counts[key]=cnt(table)
for i in range(cnt('t_dict')+1,11):
 kind='demo_business_'+str(i)
 sql(f"INSERT INTO t_dict(dict_name,dict_type,status,remark) VALUES('演示业务字典{i}','{kind}',1,'答辩演示，不覆盖系统内置枚举'); INSERT INTO t_dict_item(dict_type,item_label,item_value,sort,status,remark) VALUES('{kind}','演示选项{i}','DEMO_{i}',{i},1,'答辩演示')",DB)
counts['字典类型']=cnt('t_dict');counts['字典明细']=cnt('t_dict_item')
checkpoint()
print(json.dumps(dict(created=created,counts=counts,failures=failures),ensure_ascii=True))

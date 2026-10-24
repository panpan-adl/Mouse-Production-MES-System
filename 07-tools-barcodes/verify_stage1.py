"""Destructive fixtures are allowed only in a separate, explicitly named test database."""
from stage1_database import ROOT,sql
import json,urllib.request,urllib.error,datetime
state=json.loads((ROOT/'.runtime/stage1-database.json').read_text(encoding='utf-8'))
DB=state['database'];assert DB.startswith('mouse_mes_stage1_test_'), 'Requires isolated test database'
checks=[]
def check(condition,label):
 assert condition,label
 checks.append(label);print('PASS '+label,flush=True)
def scalar(q):return sql(q,DB).strip().splitlines()[-1]
def rows(q):return [json.loads(x) for x in sql(q,DB).splitlines()[1:]]
def api(path,body=None,method='GET',token=None,ok=True):
 req=urllib.request.Request('http://127.0.0.1:8081/api'+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=20) as response:status=response.status;result=json.load(response)
 except urllib.error.HTTPError as e:status=e.code;result=json.load(e)
 success=status<400 and (isinstance(result,list) or isinstance(result,dict) and result.get('code',200)==200)
 if ok:assert success, f'{path}: {result}'
 else:assert not success, f'Expected rejection: {path}'
 return result.get('data',result) if isinstance(result,dict) else result
users=rows("SELECT JSON_OBJECT('id',id,'role',role_id,'code',user_code,'password',password) FROM t_user WHERE deleted=0 AND status<>0")
actors={}
for role in [9,3,4,8,7]:
 u=next(u for u in users if u['role']==role);login=api('/auth/login',{'userCode':u['code'],'password':u['password']},'POST');actors[role]=(u['id'],login['token'])
admin=actors[9][1];worker=actors[3][1];qc=actors[4][1];equipment=actors[8][1]
api('/foundation/catalog',ok=False)
catalog=api('/foundation/catalog',token=admin);check(len(catalog)==25,'25 foundation resources authenticated')
for resource in catalog:api('/foundation/'+resource['key'],token=admin)
check(True,'All configuration lists are readable')
def data(key):return api('/foundation/'+key,token=admin)
bom=next(x for x in data('bom-versions') if x['code']=='BOM-A100-V1');route=next(x for x in data('routes') if x['code']=='ROUTE-A100-V1')
leaves=api(f"/foundation/bom/{bom['id']}/expand?quantity=100",token=admin)
check(len(leaves)==12 and next(x for x in leaves if x['material_code']=='MAT-SCREW-A100')['required_quantity']==400,'Multi-level BOM: 12 leaves, 100 mice require 400 screws')
api('/foundation/workshops',{'code':'DENIED','name':'DENIED'},'POST',worker,ok=False);check(True,'Worker cannot maintain master data')
api('/orders',{},'POST',worker,ok=False);check(True,'Worker cannot write sales orders')
stamp=datetime.datetime.now().strftime('%H%M%S')
workshop=api('/foundation/workshops',{'code':'TEST-'+stamp,'name':'验收车间'},'POST',admin)
api('/foundation/workshops',{'code':'TEST-'+stamp,'name':'重复'},'POST',admin,ok=False)
api('/foundation/workshops/'+str(workshop['id']),method='DELETE',token=admin)
check(True,'Master data create, duplicate rejection, unreferenced deletion')
for key,payload in [
 ('units',{'uom_code':'TEST-'+stamp,'uom_name':'测试单位'+stamp}),
 ('materials',{'material_code':'TEST-'+stamp,'material_name':'测试原材料','material_type':'1','unit':'只','specification':'验收','safety_stock':0}),
 ('products',{'product_code':'TEST-'+stamp,'product_name':'测试产品','model':'TEST','unit':'只','specification':'验收','cost_price':1,'selling_price':2}),
 ('customers',{'customer_code':'TEST-'+stamp,'customer_name':'测试客户','contact_person':'测试联系人','contact_phone':'10000','address':'测试地址'}),
 ('suppliers',{'supplier_code':'TEST-'+stamp,'supplier_name':'测试供应商','contact_person':'测试联系人','contact_phone':'10000'}),
 ('warehouses',{'warehouse_code':'TEST-'+stamp,'warehouse_name':'测试仓库','location':'测试地址'}),
 ('equipment',{'equipment_code':'TEST-'+stamp,'equipment_name':'测试设备','model':'TEST','location':'测试地址'}),
]:
 created=api('/foundation/'+key,payload,'POST',admin)
 api('/foundation/'+key+'/'+str(created['id']),payload,'PUT',admin)
 api('/foundation/'+key+'/'+str(created['id']),method='DELETE',token=admin)
 check(True,key+' create, update and unreferenced delete')
api('/foundation/bom-lines',{'bom_id':bom['id'],'parent_material_id':bom['root_material_id'],'material_id':bom['root_material_id'],'quantity':1},'POST',admin,ok=False)
semi=next(x for x in data('materials') if x['material_code']=='MAT-SEMI-E-A100')
api('/foundation/bom-lines',{'bom_id':bom['id'],'parent_material_id':semi['id'],'material_id':bom['root_material_id'],'quantity':1},'POST',admin,ok=False)
check(True,'BOM self-reference and indirect cycle rejected')
team=api('/foundation/teams',{'code':'TEST-'+stamp,'name':'验收班组','leader_id':actors[3][0]},'POST',admin)
api('/foundation/members',{'team_id':team['id'],'user_id':actors[3][0]},'POST',admin)
station=data('stations')[0];shift=data('shifts')[0]
schedule_start=datetime.datetime(2031,1,1)+datetime.timedelta(days=int(stamp))
scheduling={'team_id':team['id'],'shift_id':shift['id'],'station_id':station['id'],'equipment_id':None,'start_at':schedule_start.isoformat(),'end_at':(schedule_start+datetime.timedelta(hours=9)).isoformat()}
schedule=api('/foundation/schedules',scheduling,'POST',admin)
api('/foundation/schedules',scheduling,'POST',admin,ok=False);check(True,'Overlapping personnel or station schedules rejected')
sql(f"INSERT INTO t_order(order_no,customer_name,product_name,quantity,unit_price,total_amount,order_date,order_status,create_by) VALUES('TEST-{stamp}','隔离测试客户','A100有线办公鼠标',100,89,8900,CURDATE(),1,{actors[9][0]});",DB)
order_id=int(scalar(f"SELECT id FROM t_order WHERE order_no='TEST-{stamp}'"))
new_plan=api(f'/production-plans/from-order/{order_id}',{},'POST',admin)
plan=new_plan['id']
check(new_plan['productId']==bom['product_id'],'Order creates plan with actual product instead of hard-coded product 1')
api(f'/production-plans/from-order/{order_id}',{},'POST',admin,ok=False)
check(True,'Order cannot dispatch duplicate plans')
payload={'plan_id':plan,'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0],'schedule_id':schedule['id']}
wo=api('/foundation/work-orders',payload,'POST',admin)
api('/foundation/work-orders',payload,'POST',admin,ok=False)
tasks=api(f"/foundation/work-orders/{wo['id']}/tasks",token=admin)
check(len(tasks)==4 and all(x['quantity']==100 for x in tasks),'One work order creates four full-quantity operations without duplicate dispatch')
api('/foundation/bom-versions/'+str(bom['id']),bom,'PUT',admin,ok=False)
api('/foundation/operations/'+str(tasks[0]['operation_id']),method='DELETE',token=admin,ok=False)
check(True,'Dispatched BOM and operation definitions frozen')
api(f"/production-tasks/{tasks[0]['id']}",{'taskStatus':4},'PUT',admin,ok=False)
api(f'/production-plans/{plan}',{'planQuantity':1},'PUT',admin,ok=False)
check(True,'Legacy update endpoints cannot alter dispatched work order quantities or release state')
api(f"/production-tasks/{tasks[2]['id']}/start",{},'PUT',worker,ok=False)
template=next(t for t in data('qc-templates') if t['kind']=='PROCESS' and t['product_id']==bom['product_id'])
items=[x['item_id'] for x in data('qc-lines') if x['template_id']==template['id']]
def inspection(task,passed):
 return {'template_id':template['id'],'source_type':'TASK','source_id':task['id'],'quantity':100,'qualified_quantity':100 if passed else 99,'results':{str(i):{'actual':'人工实测通过' if passed else '测试故障','result':'PASS' if passed else 'FAIL'} for i in items}}
for idx,task in enumerate(tasks):
 api(f"/production-tasks/{task['id']}/start",{},'PUT',worker)
 api(f"/production-tasks/{task['id']}/report",{'reportQuantity':101},'POST',worker,ok=False)
 api(f"/production-tasks/{task['id']}/report",{'reportQuantity':100},'POST',worker)
 if idx==0:
  check(int(scalar(f'SELECT completed_quantity FROM t_production_plan WHERE id={plan}'))==0,'Reporting does not count finished goods')
  blocked=api('/foundation/inspections',inspection(task,False),'POST',qc)
  check(blocked['status']=='BLOCKED','Failed process inspection blocks release')
 if idx==1:
  api(f"/production-tasks/{tasks[2]['id']}/start",{},'PUT',worker,ok=False)
  check(True,'Assembly waits for both earlier operation releases')
 released=api('/foundation/inspections',inspection(task,True),'POST',qc)
 check(released['status']=='RELEASED',f'Operation {idx+1} inspection release')
 if idx<3:check(int(scalar(f'SELECT completed_quantity FROM t_production_plan WHERE id={plan}'))==0,'Intermediate operations do not inflate plan completion '+str(idx+1))
check(int(scalar(f'SELECT completed_quantity FROM t_production_plan WHERE id={plan}'))==100,'Four operations yield exactly 100 finished products')
check(scalar(f'SELECT plan_status FROM t_production_plan WHERE id={plan}')=='2','Plan completes only after final release')
check(scalar(f"SELECT order_status FROM t_order WHERE order_no='TEST-{stamp}'")=='2','Production completion does not mark sales order shipped')
checkplan=data('checks')[0]
record=api('/foundation/check-records',{'plan_id':checkplan['id'],'checked_at':'2031-01-01T08:00','actual_value':'USB连接异常','result':'FAIL','remark':'隔离验收'},'POST',equipment)
check(int(scalar(f"SELECT COUNT(*) FROM t_maintenance WHERE id=(SELECT maintenance_id FROM mm_check_record WHERE id={record['id']})"))==1,'Failed equipment check creates maintenance request')
api('/foundation/check-records/'+str(record['id']),record,'PUT',equipment,ok=False)
check(True,'Equipment check execution history immutable')
check(all('password' not in u for u in api('/users',token=admin)),'User list never returns passwords')
report={'database':DB,'date':datetime.datetime.now().isoformat(),'passed':len(checks),'checks':checks,'production_database_test_fixtures':False}
(ROOT/'.runtime/stage1-acceptance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('TOTAL PASSED '+str(len(checks)))

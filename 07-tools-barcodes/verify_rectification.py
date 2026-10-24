"""Regression checks for the eleven-item repair. Writes ONLY to the recorded isolated test DB/server."""
from pathlib import Path
import sys,json,re,datetime,uuid,urllib.request,urllib.error,concurrent.futures
sys.path.insert(0,str(Path(__file__).resolve().parent))
from stage1_database import ROOT,sql
state=json.loads((ROOT/'.runtime/rectification-test.json').read_text(encoding='utf-8'));DB=state['database']
assert re.fullmatch(r'mouse_mes_rectification_test_[0-9_]+',DB),'Only an isolated repair database is allowed'
BASE='http://127.0.0.1:8087/api';stamp=uuid.uuid4().hex[:10];checks=[]
def scalar(q):return sql(q,DB).strip().splitlines()[-1]
def call(path,body=None,method='GET',token=None,ok=True):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=30) as r:status=r.status;v=json.load(r)
 except urllib.error.HTTPError as e:status=e.code;v=json.load(e)
 assert status<500,(path,status,v)
 passed=status<400 and (not isinstance(v,dict) or v.get('code',200)==200)
 assert passed if ok else not passed,(path,status,v)
 return v.get('data',v) if isinstance(v,dict) else v
users=[json.loads(x) for x in sql("SELECT JSON_OBJECT('id',id,'role',role_id,'code',user_code,'password',password) FROM t_user WHERE deleted=0 AND status=1",DB).splitlines()[1:]]
actors={}
for role in [9,3,4,5,6,8,11]:
 u=next(u for u in users if u['role']==role);actors[role]=(u['id'],call('/auth/login',{'userCode':u['code'],'password':u['password']},'POST')['token'])
admin=actors[9][1];worker=actors[3][1];qc=actors[4][1];store=actors[5][1];buyer=actors[6][1];equipment=actors[8][1];system=actors[11][1]
def get(k):return call('/foundation/'+k,token=admin)
def post(p,v=None,t=None,ok=True):return call(p,v or {},'POST',t or admin,ok)
def put(p,v=None,t=None,ok=True):return call(p,v or {},'PUT',t or admin,ok)
def save(k,v,t=None):return post('/foundation/'+k,v,t)
def check(condition,label):
 assert condition,label
 checks.append(label);print('PASS '+label,flush=True)
 (ROOT/'.runtime/rectification-acceptance.json').write_text(json.dumps({'database':DB,'passed':len(checks),'checks':checks},ensure_ascii=False,indent=2),encoding='utf-8')
# Automatic identifiers: no client code is needed; supplied archive codes cannot control identity.
a=save('workshops',{'name':'整改车间'+stamp});b=save('workshops',{'code':'CLIENT-CODE','name':'整改车间2'+stamp})
check(a['code'] and b['code']!='CLIENT-CODE' and a['code']!=b['code'],'Server allocates archive codes and ignores client identifiers')
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:records=list(pool.map(lambda i:save('workshops',{'name':'并发'+stamp+str(i)}),range(4)))
check(len({r['code'] for r in records})==4,'Concurrent archive creation produces unique codes')
# Partial purchase receipt, quality gate, two pages reading the same result, source-bound returns.
bom=next(r for r in get('bom-versions') if r['code']=='BOM-A100-V1');route=next(r for r in get('routes') if r['code']=='ROUTE-A100-V1')
material=int(scalar('SELECT material_id FROM mm_bom_line l JOIN t_material m ON m.id=l.material_id WHERE l.bom_id='+str(bom['id'])+' AND m.material_type=1 LIMIT 1'))
warehouse=next(w['id'] for w in get('warehouses') if w['warehouse_code']=='WH-A100-RM');location=next(l['id'] for l in call('/fulfillment/locations',token=admin) if l['warehouse_id']==warehouse)
supplier=save('suppliers',{'supplier_name':'整改供应商'+stamp},buyer)
def purchase():
 p=post('/purchase-order/add',{'supplierId':supplier['id'],'poNo':'FIX-'+uuid.uuid4().hex[:16],'status':'DRAFT','items':[{'materialId':material,'quantity':10,'unitPrice':1}]},buyer)
 put(f"/purchase-order/{p['id']}/submit",t=buyer);put(f"/purchase-order/{p['id']}/approve",t=buyer);return p
po=purchase();item=save('qc-items',{'name':'整改来料检测'+stamp,'standard':'外观完好','data_type':'TEXT'});template=save('qc-templates',{'name':'整改来料模板'+stamp,'product_id':bom['product_id'],'kind':'INCOMING','version':'1','phase':'FINAL'});save('qc-lines',{'template_id':template['id'],'item_id':item['id']})
def doc(kind,source,quantity,lot=None):
 line={'material_id':material,'quantity':quantity,'location_id':location}
 if lot:line['lot_id']=lot
 else:line['lot_no']='FIX-'+uuid.uuid4().hex[:18]
 return post('/mes/documents',{'kind':kind,'source_id':source,'warehouse_id':warehouse,'reason':'整改验收','lines':[line]},store)
first=None
for index,quantity in enumerate([4,6]):
 d=doc('PURCHASE_RECEIPT',po['id'],quantity);post(f'/mes/documents/{d}/confirm',t=store);post(f'/mes/documents/{d}/post',t=store,ok=False)
 line=call(f'/mes/documents/{d}/lines',token=qc)[0];first=first or line
 post(f"/mes/document-lines/{line['id']}/inspect",{'template_id':template['id'],'qualified_quantity':quantity,'results':{str(item['id']):{'actual':'外观完好','result':'PASS'}}},qc)
 post(f'/mes/documents/{d}/post',t=store)
 old=next(p for p in call('/purchase-order/list',token=buyer) if p['id']==po['id']);new=next(p for p in call('/fulfillment/purchases',token=store) if p['id']==po['id'])
 expected='PARTIAL_RECEIVED' if index==0 else 'COMPLETED'
 check(old['status']==new['status']==expected and sum(float(l['receivedQuantity']) for l in old['items'])==4+6*index,'Legacy and modern purchase views agree after receipt '+str(index+1))
 if index==0:check(any(p['id']==po['id'] for p in call('/fulfillment/inspection-sources?kind=INCOMING',token=qc)),'Partially received purchase remains selectable for incoming inspection')
 before=scalar('SELECT COUNT(*) FROM t_inventory_transaction');post(f'/mes/documents/{d}/post',t=store);check(scalar('SELECT COUNT(*) FROM t_inventory_transaction')==before,'Repeated purchase posting does not duplicate inventory')
lot=first['lot_id'];options=call(f"/mes/return-lots?kind=PURCHASE_RETURN&source={po['id']}",token=store)
check(any(o['lot_id']==lot and float(o['quantity'])==4 for o in options),'Supplier return options include original received material and batch')
before=scalar(f'SELECT SUM(quantity) FROM t_inventory WHERE lot_id={lot} AND warehouse_id={warehouse}')
r=doc('PURCHASE_RETURN',po['id'],2,lot)
check(scalar(f'SELECT SUM(quantity) FROM t_inventory WHERE lot_id={lot} AND warehouse_id={warehouse}')==before,'Return draft leaves inventory unchanged')
post(f'/mes/documents/{r}/confirm',t=store);post(f'/mes/documents/{r}/post',t=store);post(f'/mes/documents/{r}/post',t=store)
check(float(scalar(f'SELECT SUM(quantity) FROM t_inventory WHERE lot_id={lot} AND warehouse_id={warehouse}'))==float(before)-2,'Return confirmation and idempotent posting decrease the original batch exactly once')
post('/mes/documents',{'kind':'PURCHASE_RETURN','source_id':po['id'],'warehouse_id':warehouse,'reason':'超量','lines':[{'material_id':material,'lot_id':lot,'quantity':3}]},store,False)
other=purchase();post('/mes/documents',{'kind':'PURCHASE_RETURN','source_id':other['id'],'warehouse_id':warehouse,'reason':'错误来源','lines':[{'material_id':material,'lot_id':lot,'quantity':1}]},store,False)
check(True,'Returns reject excessive quantity and a batch from another purchase')
# Work-order material request, persisted date, authenticated creator and duplicate guard.
wo=post('/mes/work-orders',{'product_id':bom['product_id'],'quantity':1,'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]})
task=call(f"/foundation/work-orders/{wo['id']}/tasks",token=admin)[0];allowed=call(f"/material-requests/task-materials/{task['id']}",token=worker)
request={'sourceTaskId':task['id'],'materialId':allowed[0]['material_id'],'warehouseId':warehouse,'requiredQuantity':1,'requiredDate':'2026-11-01','remark':'整改用料','createdBy':actors[9][0]}
before=scalar('SELECT COUNT(*) FROM t_inventory_transaction');requested=post('/material-requests',request,worker)
check(requested['createdBy']==actors[3][0] and requested['requiredDate']=='2026-11-01' and scalar('SELECT COUNT(*) FROM t_inventory_transaction')==before,'Worker can register dated material request without changing inventory or spoofing creator')
post('/material-requests',request,worker,False);check(any(r['id']==requested['id'] for r in call('/material-requests',token=store)),'Warehouse sees persisted request and duplicate registration is rejected')
# Persistent anchored maintenance plan, deduplication, enable switch, planned vs actual times.
machine=save('equipment',{'equipment_name':'整改保养设备'+stamp,'model':'TEST','location':'验收车间'})
anchor=(datetime.datetime.now()-datetime.timedelta(hours=1)).replace(microsecond=0).isoformat()
plan=save('maintenance-plans',{'name':'年度保养'+stamp,'equipment_id':machine['id'],'owner_id':actors[8][0],'anchor_at':anchor,'frequency':1,'cycle_unit':'YEAR','remind_days':0,'content':'清洁润滑','enabled':1},equipment)
post('/maintenances/periodic/generate',t=equipment);post('/maintenances/periodic/generate',t=equipment)
occurrences=[o for o in call('/maintenances/periodic/tasks',token=equipment) if o['plan_id']==plan['id']]
check(len(occurrences)==1 and occurrences[0]['actual_start'] is None,'Persistent yearly plan generates exactly one pending task with separate planned time')
current=next(p for p in call('/maintenances/periodic/plans',token=equipment) if p['id']==plan['id']);check(current['next_at'].startswith(str(datetime.datetime.now().year+1)),'Next yearly occurrence remains anchored to the initial date')
put('/foundation/maintenance-plans/'+str(plan['id']),{**plan,'enabled':0},equipment)
post('/maintenances/periodic/generate',t=equipment);check(len([o for o in call('/maintenances/periodic/tasks',token=equipment) if o['plan_id']==plan['id']])==1,'Plan can be disabled after generation without changing its recurrence anchor')
put('/maintenances/'+str(occurrences[0]['maintenance_id'])+'/start',t=equipment)
check(next(o for o in call('/maintenances/periodic/tasks',token=equipment) if o['plan_id']==plan['id'])['actual_start'] is not None,'Actual maintenance start is recorded only on explicit start')
# Real custom role, assignment, effective permissions, deactivation and leave.
role=post('/roles/manage',{'role_name':'整改操作岗位'+stamp,'role_desc':'物料请求权限','base_role_id':3,'status':1},system)
u=post('/users',{'username':'fix-'+stamp,'password':'Repair12345','realName':'整改员工'+stamp,'roleId':role,'deptId':2},system)
login=call('/auth/login',{'userCode':u['userCode'],'password':'Repair12345'},'POST');token=login['token']
check(login['roleId']==role and login['permissionRoleId']==3 and len(u['userCode'])==6,'Custom role can be assigned to a new employee with server-generated employee number')
call('/roles/manage',token=token,ok=False);post('/mes/notices',{'title':'越权','content':'越权'},token,False);check(True,'Custom worker role cannot access role administration or publish announcements')
post('/roles/manage',{'id':role,'role_name':'整改采购岗位'+stamp,'role_desc':'调整权限','base_role_id':6,'status':1},system)
check(call('/mes/access-role',token=token)==6,'Existing login receives changed role permissions on the next request')
post('/material-requests',{**request,'materialId':allowed[-1]['material_id']},token,False)
post('/roles/manage',{'id':role,'role_name':'整改采购岗位'+stamp,'base_role_id':6,'status':0},system);call('/foundation/materials',token=token,ok=False);call('/auth/login',{'userCode':u['userCode'],'password':'Repair12345'},'POST',ok=False)
check(True,'Disabled custom role blocks both existing sessions and new logins')
post('/roles/manage',{'id':9,'role_name':'CEO','base_role_id':9,'status':0},system,False);check(True,'Built-in administrator cannot be disabled')
post('/roles/manage',{'id':role,'role_name':'整改操作岗位'+stamp,'base_role_id':3,'status':1},system)
login=call('/auth/login',{'userCode':u['userCode'],'password':'Repair12345'},'POST');post('/users/'+str(u['id'])+'/leave',t=system)
call('/foundation/materials',token=login['token'],ok=False);call('/auth/login',{'userCode':u['userCode'],'password':'Repair12345'},'POST',ok=False)
check(not any(p['id']==u['id'] for p in get('users')) and scalar('SELECT COUNT(*) FROM t_user WHERE id='+str(u['id']))=='1','Employee leave stops sessions and assignment while preserving historical identity')
# Schedule copy is atomic and reuses existing resource conflict checks.
team=save('teams',{'name':'整改排班'+stamp,'leader_id':actors[8][0]});save('members',{'team_id':team['id'],'user_id':actors[8][0]})
# Use independent equipment: the maintenance device above is currently under service.
machine2=save('equipment',{'equipment_name':'整改排班设备'+stamp,'model':'TEST','location':'验收车间'})
station=save('stations',{'name':'整改排班工作站'+stamp,'workshop_id':a['id'],'equipment_id':machine2['id']});shift=get('shifts')[0]
schedule=save('schedules',{'team_id':team['id'],'shift_id':shift['id'],'station_id':station['id'],'start_at':'2036-01-07T20:00','end_at':'2036-01-08T08:00'})
post('/foundation/schedule-planning/copy-week',{'week_start':'2036-01-07'})
check(scalar('SELECT COUNT(*) FROM mm_schedule WHERE team_id='+str(team['id']))=='2','Copy-week preserves overnight schedule and creates the following week')
post('/foundation/schedule-planning/copy-week',{'week_start':'2036-01-07'},ok=False)
check(scalar('SELECT COUNT(*) FROM mm_schedule WHERE team_id='+str(team['id']))=='2','Conflicting copy-week is rejected without partial schedule writes')
print('Eleven-item repair acceptance passed:',len(checks))

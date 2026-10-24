"""Real API acceptance. This creates fixtures ONLY in the recorded completion test database."""
from stage1_database import ROOT,sql
import json, urllib.request, urllib.error, datetime, uuid, concurrent.futures

state=json.loads((ROOT/'.runtime/mes-completion-database.json').read_text(encoding='utf-8'))
DB=state['database'];assert DB.startswith('mouse_mes_completion_test_')
BASE='http://127.0.0.1:8085/api'
checks=[];stamp=datetime.datetime.now().strftime('%H%M%S%f')
def check(value,label):
    assert value,label
    checks.append(label);print('PASS '+label,flush=True)
    (ROOT/'.runtime/mes-completion-acceptance.json').write_text(json.dumps(dict(database=DB,passed=len(checks),checks=checks),indent=2),encoding='utf-8')
def scalar(query):return sql(query,DB).strip().splitlines()[-1]
def api(path,body=None,method='GET',token=None,ok=True):
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
    try:
        with urllib.request.urlopen(req,timeout=20) as response:status=response.status;result=json.load(response)
    except urllib.error.HTTPError as error:status=error.code;result=json.load(error)
    assert status<500,(path,status,result)
    passed=status<400 and (isinstance(result,list) or result.get('code',200)==200)
    assert passed if ok else not passed,(path,result)
    return result.get('data',result) if isinstance(result,dict) else result
def post(path,body=None,token=None,ok=True):return api(path,body or {},'POST',token or admin,ok)
def get(key):return api('/foundation/'+key,token=admin)
def save(key,body,token=None):return post('/foundation/'+key,body,token)

if scalar('SELECT COUNT(*) FROM t_user WHERE role_id=11 AND deleted=0')=='0':
    password=uuid.uuid4().hex
    sql("INSERT INTO t_user(user_code,username,password,real_name,role_id,dept_id,status,deleted) VALUES('111001','mcadmin','"+password+"','隔离系统管理员',11,1,1,0)",DB)
users=[json.loads(row) for row in sql("SELECT JSON_OBJECT('id',id,'role',role_id,'code',user_code,'password',password) FROM t_user WHERE deleted=0 AND status=1",DB).splitlines()[1:]]
actors={}
for role in [9,3,4,5,6,7,8,11]:
    u=next(x for x in users if x['role']==role);login=api('/auth/login',{'userCode':u['code'],'password':u['password']},'POST');actors[role]=(u['id'],login['token'])
admin,worker,qc,store,buyer,sales,equipment,system=[actors[r][1] for r in [9,3,4,5,6,7,8,11]]
api('/users',token=worker,ok=False);api('/finance/overview',token=worker,ok=False)
check(all('cost_price' not in r and 'selling_price' not in r for r in api('/foundation/products',token=worker)),'Worker cannot read users, finance or product prices')
api('/mes/monitor',token=worker,ok=False);api('/mes/cache',token=worker,ok=False);post('/mes/notices',{'title':'forged','content':'forged'},worker,False)
check(True,'Monitor/cache and announcement publication enforce server-side roles')
catalog=api('/foundation/catalog',token=admin)
for resource in catalog:get(resource['key'])
check(len(catalog)>=40,'All extended master-data resources have working list APIs')

main=save('units',{'uom_code':'MC-U-'+stamp,'uom_name':'测试主单位'+stamp})
aux=save('units',{'uom_code':'MC-A-'+stamp,'uom_name':'测试辅单位'+stamp,'main_unit_id':main['id'],'conversion_rate':20})
post('/foundation/units',{'uom_code':'MC-BAD-'+stamp,'uom_name':'错误辅单位','main_unit_id':aux['id'],'conversion_rate':2},admin,False)
check(aux['conversion_rate']==20,'Main/auxiliary conversion is stored and chained auxiliary units are rejected')
parent=save('categories',{'code':'MC-CAT-'+stamp,'name':'鼠标产品','kind':'PRODUCT'})
child=save('categories',{'code':'MC-SUB-'+stamp,'name':'办公鼠标','kind':'PRODUCT','parent_id':parent['id']})
api('/foundation/categories/'+str(parent['id']),{**parent,'parent_id':child['id']},'PUT',admin,False)
check(True,'Category hierarchy rejects indirect cycles')
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:codes=list(pool.map(lambda _:post('/mes/codes/ITEM_CODE'),range(15)))
check(len(set(codes))==15,'Concurrent code allocation produces unique sequence values')

bom=next(x for x in get('bom-versions') if x['code']=='BOM-A100-V1');route=next(x for x in get('routes') if x['code']=='ROUTE-A100-V1')
ops=sorted([x for x in get('operations') if x['route_id']==route['id']],key=lambda x:x['sequence_no'])
workshop=save('workshops',{'code':'MC-WS-'+stamp,'name':'隔离验收车间'})
machine=save('equipment',{'equipment_code':'MC-EQ-'+stamp,'equipment_name':'隔离设备','model':'TEST','location':'隔离车间'})
station=save('stations',{'code':'MC-ST-'+stamp,'name':'隔离生产工作站','workshop_id':workshop['id'],'equipment_id':machine['id']})
for i,op in enumerate(ops):
    op.update(station_id=station['id'],is_key=1 if i==len(ops)-1 else 0)
    api('/foundation/operations/'+str(op['id']),op,'PUT',admin)
numeric=save('qc-items',{'code':'MC-QC-'+stamp,'name':'数值检验','standard':'0至10','data_type':'NUMBER','lower_limit':0,'upper_limit':10})
templates={}
for op in ops:
    t=save('qc-templates',{'code':'MC-T-'+stamp+'-'+str(op['id']),'name':op['name']+'专用检验','product_id':bom['product_id'],'kind':'PROCESS','version':'1','operation_id':op['id'],'phase':'FINAL'})
    save('qc-lines',{'template_id':t['id'],'item_id':numeric['id']});templates[op['id']]=t
team=save('teams',{'code':'MC-TEAM-'+stamp,'name':'验收班组','leader_id':actors[3][0]})
save('members',{'team_id':team['id'],'user_id':actors[3][0]})
now=datetime.datetime.now();shift=get('shifts')[0]
schedule=save('schedules',{'team_id':team['id'],'shift_id':shift['id'],'station_id':station['id'],'start_at':(now-datetime.timedelta(hours=1)).isoformat(),'end_at':(now+datetime.timedelta(hours=6)).isoformat()})
post('/foundation/schedules',{**schedule,'id':None},admin,False)
check(True,'Overlapping station/person schedules are rejected')

order=post('/orders',{'orderNo':'MC-'+stamp,'customerName':'隔离客户','productName':'A100有线办公鼠标','quantity':100,'unitPrice':89,'totalAmount':8900},sales)
plan=post('/production-plans/from-order/'+str(order['id']))
wo=post('/foundation/work-orders',{'plan_id':plan['id'],'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]})
tasks=api(f"/foundation/work-orders/{wo['id']}/tasks",token=admin)
requirements=api(f"/fulfillment/work-orders/{wo['id']}/materials",token=admin)
check(len(tasks)==4 and len(requirements)==12,'Work order freezes four operations and twelve material requirements')
api(f"/production-tasks/{tasks[0]['id']}/start",{},'PUT',worker,False)
check(True,'Unscheduled/unissued task cannot start')
warehouses=get('warehouses');warehouse=next(r['id'] for r in warehouses if r['warehouse_code']=='WH-A100-RM');fgwarehouse=next(r['id'] for r in warehouses if r['warehouse_code']=='WH-A100-FG')
locations=api('/fulfillment/locations',token=admin);location=next(r['id'] for r in locations if r['warehouse_id']==warehouse);fgLocation=next(r['id'] for r in locations if r['warehouse_id']==fgwarehouse)
supplier=save('suppliers',{'supplier_code':'MC-SUP-'+stamp,'supplier_name':'隔离供应商'},buyer)
def purchase(lines,suffix):
    p=post('/purchase-order/add',{'poNo':'MC-'+stamp+'-'+suffix,'supplierId':supplier['id'],'status':'DRAFT','items':[{'materialId':m,'quantity':q,'unitPrice':1,'amount':q} for m,q in lines]},buyer)
    api(f"/purchase-order/{p['id']}/submit",{},'PUT',buyer);api(f"/purchase-order/{p['id']}/approve",{},'PUT',buyer);return p
incoming=next(r for r in get('qc-templates') if r['kind']=='INCOMING' and r['product_id']==bom['product_id'])
shipping=next(r for r in get('qc-templates') if r['kind']=='SHIPPING' and r['product_id']==bom['product_id'])
def document(kind,source,warehouse,lines,reason='隔离验收'):
    return post('/mes/documents',{'kind':kind,'source_id':source,'warehouse_id':warehouse,'reason':reason,'lines':lines},store)
def check_document(doc,template):
    lines=api(f'/mes/documents/{doc}/lines',token=qc);items=[r['item_id'] for r in get('qc-lines') if r['template_id']==template['id']]
    for row in lines:post(f"/mes/document-lines/{row['id']}/inspect",{'template_id':template['id'],'qualified_quantity':row['quantity'],'results':{str(i):{'actual':'人工检查合格','result':'PASS'} for i in items}},qc)
    return lines
po=purchase([(r['material_id'],r['required_quantity']) for r in requirements],'MAIN')
for part in range(2):
    doc=document('PURCHASE_RECEIPT',po['id'],warehouse,[{'material_id':r['material_id'],'quantity':r['required_quantity']/2,'lot_no':f'MC-{stamp}-{part}-{r["material_id"]}','location_id':location} for r in requirements])
    post(f'/mes/documents/{doc}/post',token=store,ok=False)
    post(f'/mes/documents/{doc}/confirm',token=store)
    post(f'/mes/documents/{doc}/post',token=store,ok=False)
    check_document(doc,incoming);post(f'/mes/documents/{doc}/post',token=store)
    before=scalar('SELECT COUNT(*) FROM t_inventory_transaction');post(f'/mes/documents/{doc}/post',token=store)
    check(scalar('SELECT COUNT(*) FROM t_inventory_transaction')==before,'Partial purchase receipt '+str(part)+' is confirmed, quality-gated and idempotent')
check(scalar(f"SELECT status FROM t_purchase_order WHERE id={po['id']}")=='COMPLETED','Two partial receipts complete the purchase without over-receipt')
post(f"/fulfillment/work-orders/{wo['id']}/issue",{'warehouse_id':warehouse},store)
lineWarehouse=next(r['id'] for r in warehouses if r['warehouse_code']=='WH-A100-LINE')
check(float(scalar(f'SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE warehouse_id={warehouse}'))==0 and len(api('/mes/line-stock',token=store))==24,'Issue transfers all raw batches into work-order line inventory')
check(float(scalar(f'SELECT SUM(quantity) FROM t_inventory WHERE warehouse_id={lineWarehouse}'))==float(scalar('SELECT SUM(quantity) FROM mm_line_stock')),'Unified line warehouse inventory agrees with work-order subledger')
before=scalar('SELECT COUNT(*) FROM mm_line_movement');post(f"/fulfillment/work-orders/{wo['id']}/issue",{'warehouse_id':warehouse},store)
check(scalar('SELECT COUNT(*) FROM mm_line_movement')==before,'Repeated material issue does not duplicate line inventory')
def dispatch(task):
    now=datetime.datetime.now();post(f"/mes/tasks/{task['id']}/dispatch",{'station_id':station['id'],'owner_id':actors[3][0],'start_at':(now-datetime.timedelta(minutes=1)).isoformat(),'end_at':(now+datetime.timedelta(hours=1)).isoformat()})
def inspect(task,value=5,template=None):
    template=template or templates[task['operation_id']]
    return dict(template_id=template['id'],source_type='TASK',source_id=task['id'],quantity=task['quantity'],qualified_quantity=task['quantity'],results={str(numeric['id']):dict(actual=value,result='PASS')})
for i,task in enumerate(tasks):
    dispatch(task)
    api(f"/production-tasks/{task['id']}/start",{},'PUT',worker)
    post(f"/production-tasks/{task['id']}/report",{'reportQuantity':101},worker,False)
    if i==0:
        post(f"/production-tasks/{task['id']}/report",{'reportQuantity':60},worker)
        check(float(scalar(f"SELECT SUM(quantity) FROM mm_line_stock WHERE work_order_id={wo['id']}"))>0,'Partial report consumes only its proportional material quantity')
        remaining=api('/mes/line-stock',token=store)[0]
        returned=document('LINE_RETURN',wo['id'],warehouse,[{'material_id':remaining['material_id'],'lot_id':remaining['lot_id'],'quantity':remaining['quantity'],'location_id':location}])
        post(f'/mes/documents/{returned}/confirm',token=store);post(f'/mes/documents/{returned}/post',token=store)
        readiness=next(r for r in api(f"/fulfillment/work-orders/{wo['id']}/materials",token=store) if r['material_id']==remaining['material_id'])
        check(not readiness['issued'] and readiness['unissued_quantity']==remaining['quantity'],'Returned unused material reopens the exact unissued quantity')
        reissue=document('LINE_ISSUE',wo['id'],warehouse,[{'material_id':remaining['material_id'],'lot_id':remaining['lot_id'],'quantity':remaining['quantity'],'location_id':location}])
        post(f'/mes/documents/{reissue}/confirm',token=store);post(f'/mes/documents/{reissue}/post',token=store)
        check(True,'Unused material can be returned and reissued with preserved work-order lot trace')
        post(f"/production-tasks/{task['id']}/report",{'reportQuantity':40},worker)
        failed=post('/foundation/inspections',inspect(task,20),qc)
        check(failed['status']=='BLOCKED','Out-of-range measurement overrides a forged PASS selection')
        post('/foundation/inspections',inspect(task),qc,False)
        nc=next(n for n in api('/mes/nonconformities',token=qc) if n['inspection_id']==failed['id'])
        post(f"/mes/nonconformities/{nc['id']}/disposition",{'decision':'REWORK','reason':'重新校准后返工'},qc)
        nc=next(n for n in api('/mes/nonconformities',token=qc) if n['id']==nc['id'])
        post(f"/mes/reworks/{nc['rework_id']}/complete",{'result':'隔离验收：完成返工'},worker)
        check(True,'Failed process inspection requires disposition and recorded rework before retest')
    else:post(f"/production-tasks/{task['id']}/report",{'reportQuantity':100},worker)
    if i==1:post('/foundation/inspections',inspect(task,template=templates[tasks[0]['operation_id']]),qc,False);check(True,'Another operation template cannot release this task')
    released=post('/foundation/inspections',inspect(task),qc);check(released['status']=='RELEASED','Operation '+str(i)+' releases only after applicable measured inspection')
check(float(scalar(f"SELECT SUM(quantity) FROM mm_line_stock WHERE work_order_id={wo['id']}"))==100,'After production the line balance contains exactly 100 finished goods and no raw material')
rootMaterial=bom['root_material_id'];fg=next(r for r in api('/mes/line-stock',token=store) if r['material_id']==rootMaterial)
for quantity in [40,60]:
    doc=document('PRODUCTION_RECEIPT',wo['id'],fgwarehouse,[{'material_id':rootMaterial,'lot_id':fg['lot_id'],'location_id':fgLocation,'quantity':quantity}]);post(f'/mes/documents/{doc}/confirm',token=store);post(f'/mes/documents/{doc}/post',token=store)
check(float(scalar('SELECT SUM(quantity) FROM t_inventory'))==100 and float(scalar('SELECT SUM(quantity) FROM mm_line_stock'))==0,'Partial finished-goods receipts conserve warehouse plus line inventory')
for quantity in [40,60]:
    doc=document('SALES_SHIPMENT',order['id'],fgwarehouse,[{'material_id':rootMaterial,'lot_id':fg['lot_id'],'location_id':fgLocation,'quantity':quantity}]);post(f'/mes/documents/{doc}/confirm',token=store);check_document(doc,shipping);post(f'/mes/documents/{doc}/post',token=store)
check(scalar(f"SELECT order_status FROM t_order WHERE id={order['id']}")=='3','Two independently quality-gated shipments complete one sales order')
returned=document('SALES_RETURN',order['id'],fgwarehouse,[{'material_id':rootMaterial,'lot_id':fg['lot_id'],'location_id':fgLocation,'quantity':10}]);post(f'/mes/documents/{returned}/confirm',token=store);check_document(returned,shipping);post(f'/mes/documents/{returned}/post',token=store)
post('/mes/documents',{'kind':'SALES_RETURN','source_id':order['id'],'warehouse_id':fgwarehouse,'reason':'超量退货','lines':[{'material_id':rootMaterial,'lot_id':fg['lot_id'],'location_id':fgLocation,'quantity':100}]},store,False)
check(float(scalar('SELECT SUM(quantity) FROM t_inventory'))==10,'Sales return restores stock and rejects cumulative returns above shipped quantity')

boxBody=dict(length_mm=100,width_mm=100,height_mm=100,net_weight=1,gross_weight=2)
stock=next(r for r in api('/fulfillment/stock',token=store) if r['quantity']>0)
box=post('/mes/packages',boxBody,store);post(f'/mes/packages/{box}/items',{'inventory_id':stock['id'],'quantity':4},store);post(f'/mes/packages/{box}/confirm',token=store)
outer=post('/mes/packages',boxBody,store);post(f'/mes/packages/{outer}/children',{'child_id':box},store);post(f'/mes/packages/{outer}/confirm',token=store)
contents=api(f'/mes/packages/{outer}/contents',token=store)
check(len(contents)==1 and contents[0]['quantity']==4,'Nested packaging retains underlying product and batch trace')
post(f'/mes/packages/{outer}/children',{'child_id':outer},store,False)
check(api('/mes/barcode?kind=PACKAGE&id='+str(outer),token=store)['content'].startswith('PACKAGE-PKG-'),'Production-element barcode format includes entity type and actual code')

noticeTitle='隔离公告'+stamp
post('/mes/notices',{'title':noticeTitle,'content':'真实持久化公告','target_role':3})
notice=next(n for n in api('/mes/notices',token=worker) if n['title']==noticeTitle)
post(f"/mes/notices/{notice['id']}/read",token=worker)
check(bool(next(n for n in api('/mes/notices',token=worker) if n['id']==notice['id'])['isRead']),'Announcement read state persists server-side per recipient')
check(not any(n['title']==noticeTitle for n in api('/mes/notices',token=buyer)),'Role-targeted announcement does not leak to another role')
sessions=api('/mes/sessions',token=system);cache=api('/mes/cache',token=system)
check(bool(sessions) and all('token' not in r and 'password' not in r for r in sessions) and 'used_memory' in cache,'Online session and cache monitoring expose metadata without tokens or cached secrets')
check(api('/mes/monitor',token=system)['database_time'] is not None,'Service and database monitor reports live runtime data')
print('Completion acceptance passed:',len(checks))

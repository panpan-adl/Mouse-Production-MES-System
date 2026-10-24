"""Acceptance fixtures are restricted to an explicitly isolated stage-two database."""
from stage1_database import ROOT,sql
import json,urllib.request,urllib.error,datetime,concurrent.futures
state=json.loads((ROOT/'.runtime/stage2-database.json').read_text(encoding='utf-8'))
DB=state['database'];assert DB.startswith('mouse_mes_stage2_test_'),'Isolated acceptance database required'
checks=[];stamp=datetime.datetime.now().strftime('%H%M%S%f')
def check(v,label):
 assert v,label
 checks.append(label);print('PASS '+label,flush=True)
def scalar(q):return sql(q,DB).strip().splitlines()[-1]
def api(path,body=None,method='GET',token=None,ok=True):
 req=urllib.request.Request('http://127.0.0.1:8082/api'+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=25) as r:status=r.status;result=json.load(r)
 except urllib.error.HTTPError as e:status=e.code;result=json.load(e)
 passed=status<400 and (isinstance(result,list) or result.get('code',200)==200)
 if ok:assert passed,f'{path}: {result}'
 else:assert not passed,f'Expected rejection: {path} -> {result}'
 return result.get('data',result) if isinstance(result,dict) else result
users=[json.loads(x) for x in sql("SELECT JSON_OBJECT('id',id,'role',role_id,'code',user_code,'password',password) FROM t_user WHERE deleted=0 AND status=1",DB).splitlines()[1:]]
actors={}
for role in [9,3,4,5,6,7]:
 u=next(u for u in users if u['role']==role);login=api('/auth/login',{'userCode':u['code'],'password':u['password']},'POST');actors[role]=(u['id'],login['token'])
admin,worker,qc,store,buyer,sales=[actors[r][1] for r in [9,3,4,5,6,7]]
def get(key):return api('/foundation/'+key,token=admin)
today=datetime.date.today().isoformat()
initial=api('/dashboard/production?date='+today+'&shift=all&line=all',token=admin)
check(initial['workOrders']==[] and all(k['value']==0 for k in initial['kpis']),'Empty canonical dashboard has no invented production or legacy work orders')
check(initial['inventory']['finishedGoods']==0 and initial['materialShortages']==[],'Empty stock and shortage lists stay empty')
warehouse=next(x for x in get('warehouses') if x['warehouse_code']=='WH-A100-RM')['id']
fgwarehouse=next(x for x in get('warehouses') if x['warehouse_code']=='WH-A100-FG')['id']
locations=api('/fulfillment/locations',token=store)
location=next(x['id'] for x in locations if x['warehouse_id']==warehouse);fglocation=next(x['id'] for x in locations if x['warehouse_id']==fgwarehouse)
bom=next(x for x in get('bom-versions') if x['code']=='BOM-A100-V1');route=next(x for x in get('routes') if x['code']=='ROUTE-A100-V1')
order=api('/orders',{'orderNo':'S2-'+stamp,'customerName':'隔离验收客户','productName':'A100有线办公鼠标','quantity':100,'unitPrice':89,'totalAmount':8900,'orderStatus':3,'createBy':actors[7][0]},'POST',sales)
check(order['orderStatus']==0,'Client cannot create a sales order already completed')
plan=api('/production-plans/from-order/'+str(order['id']),{},'POST',admin)
wo=api('/foundation/work-orders',{'plan_id':plan['id'],'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]},'POST',admin)
tasks=api(f"/foundation/work-orders/{wo['id']}/tasks",token=worker)
requirements=api(f"/fulfillment/work-orders/{wo['id']}/materials?warehouse={warehouse}",token=store)
check(len(requirements)==12 and all(x['shortage_quantity']==x['required_quantity'] for x in requirements),'Frozen work order shows all twelve raw material shortages')
api(f"/production-tasks/{tasks[0]['id']}/start",{},'PUT',worker,False)
api(f"/production-tasks/{tasks[0]['id']}/report",{'reportQuantity':100},'POST',worker,False)
check(True,'Unissued work order cannot start or report production')
api(f"/fulfillment/work-orders/{wo['id']}/issue",{'warehouse_id':warehouse},'POST',worker,False)
check(True,'Worker cannot post warehouse movements')
api('/orders/'+str(order['id']),{'orderStatus':3},'PUT',sales,False)
check(True,'Legacy sales edit cannot bypass validated shipment completion')
supplier=api('/foundation/suppliers',{'supplier_code':'S2-'+stamp,'supplier_name':'隔离验收供应商','contact_person':'验收','contact_phone':'10000'},'POST',buyer)
templates=get('qc-templates');templateLines=get('qc-lines')
def inspect(kind,source,qty,pass_=True):
 t=next(t for t in templates if t['kind']==kind and t['product_id']==bom['product_id'])
 itemIds=[x['item_id'] for x in templateLines if x['template_id']==t['id']]
 body={'template_id':t['id'],'source_type':{'INCOMING':'PURCHASE','PROCESS':'TASK','SHIPPING':'ORDER'}[kind],'source_id':source,'quantity':qty,'qualified_quantity':qty if pass_ else qty-1,'results':{str(i):{'actual':'隔离人工测量合格' if pass_ else '隔离验收故障','result':'PASS' if pass_ else 'FAIL'} for i in itemIds}}
 return body
def create_po(lines,suffix):
 po=api('/purchase-order/add',{'poNo':'S2-'+stamp+'-'+suffix,'supplierId':supplier['id'],'status':'DRAFT','createdBy':actors[6][0],'items':[{'materialId':m,'quantity':q,'unitPrice':1,'amount':q} for m,q in lines]},'POST',buyer)
 api(f"/purchase-order/{po['id']}/submit",{},'PUT',buyer);api(f"/purchase-order/{po['id']}/approve",{},'PUT',buyer)
 return po
lines=[(r['material_id'],int(r['required_quantity'])-(1 if r['material_code']=='MAT-SCREW-A100' else 0)) for r in requirements]
po=create_po(lines,'MAIN');total=sum(q for _,q in lines)
api('/purchase-order/'+str(po['id']),{'status':'DRAFT'},'PUT',buyer,False)
check(True,'Approved purchase order cannot be changed after quality review')
api(f"/fulfillment/purchases/{po['id']}/receive",{'warehouse_id':warehouse,'location_id':location},'POST',store,False)
check(True,'Uninspected purchase cannot increase inventory')
api('/foundation/inspections',inspect('INCOMING',po['id'],total,False),'POST',qc)
api(f"/fulfillment/purchases/{po['id']}/receive",{'warehouse_id':warehouse,'location_id':location},'POST',store,False)
check(True,'Failed incoming inspection blocks purchase receipt')
api('/foundation/inspections',inspect('INCOMING',po['id'],total),'POST',qc)
api(f"/fulfillment/purchases/{po['id']}/receive",{'warehouse_id':warehouse,'location_id':fglocation},'POST',store,False)
check(True,'Warehouse and storage location must match')
received=api(f"/fulfillment/purchases/{po['id']}/receive",{'warehouse_id':warehouse,'location_id':location,'actor_id':actors[7][0]},'POST',store)
before=scalar('SELECT COUNT(*) FROM t_inventory_transaction')
again=api(f"/fulfillment/purchases/{po['id']}/receive",{'warehouse_id':warehouse,'location_id':location},'POST',store)
check(received['id']==again['id'] and scalar('SELECT COUNT(*) FROM t_inventory_transaction')==before,'Repeated purchase receipt returns the original posting without increasing stock')
check(received['actor_id']==actors[5][0],'Posting actor comes from authenticated warehouse account')
check(scalar(f"SELECT status FROM t_purchase_order WHERE id={po['id']}")=='COMPLETED','Successful receipt completes purchase quantities and status')
api('/purchase-order/'+str(po['id']),method='DELETE',token=buyer,ok=False)
check(True,'Received purchase source document cannot be deleted')
check(int(scalar('SELECT COUNT(*) FROM t_inventory WHERE quantity>0'))==12,'Purchase receipt creates twelve real raw-material balance rows and batches')
# Attempt a document whose individually valid rows exceed the same slot together: every write must roll back.
slots=api('/fulfillment/stock',token=store);slot=next(s for s in slots if s['material_code']=='MAT-PCB-A100')
beforeBalances=scalar('SELECT SUM(quantity) FROM t_inventory');beforeLedger=scalar('SELECT COUNT(*) FROM t_inventory_transaction');beforeDocs=scalar('SELECT COUNT(*) FROM t_stock_out')
bad={'stockOutNo':'ATOMIC-'+stamp,'stockOutType':'OTHER','warehouseId':warehouse,'items':[{'materialId':slot['material_id'],'locationId':slot['location_id'],'lotId':slot['lot_id'],'quantity':60} for _ in range(2)]}
api('/stock-out/add',bad,'POST',store,False)
check(scalar('SELECT SUM(quantity) FROM t_inventory')==beforeBalances and scalar('SELECT COUNT(*) FROM t_inventory_transaction')==beforeLedger and scalar('SELECT COUNT(*) FROM t_stock_out')==beforeDocs,'Partial stock-out failure rolls back document, ledger and prior row deductions')
api(f"/fulfillment/work-orders/{wo['id']}/issue",{'warehouse_id':warehouse},'POST',store,False)
check(scalar('SELECT SUM(quantity) FROM t_inventory')==beforeBalances,'Missing one screw prevents the complete material issue without changing balances')
screw=next(r for r in requirements if r['material_code']=='MAT-SCREW-A100')
topup=create_po([(screw['material_id'],1)],'TOPUP')
api('/foundation/inspections',inspect('INCOMING',topup['id'],1),'POST',qc)
api(f"/fulfillment/purchases/{topup['id']}/receive",{'warehouse_id':warehouse,'location_id':location},'POST',store)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 futures=[pool.submit(api,f"/fulfillment/work-orders/{wo['id']}/issue",{'warehouse_id':warehouse},'POST',store) for _ in range(2)]
 results=[f.result() for f in futures]
check(results[0]['id']==results[1]['id'],'Concurrent identical issue requests create exactly one posting')
check(float(scalar('SELECT SUM(quantity) FROM t_inventory'))==0,'Exactly the frozen BOM quantity is consumed across original and top-up lots')
check(int(scalar(f"SELECT COUNT(*) FROM t_stock_out_item WHERE stock_out_id={results[0]['document_id']}"))==13,'Issue splits material allocation across real source lots')
ready=api(f"/fulfillment/work-orders/{wo['id']}/materials?warehouse={warehouse}",token=store)
check(all(r['issued'] and r['shortage_quantity']==0 for r in ready),'Issued materials no longer appear as shortages for their own work order')
api(f"/fulfillment/work-orders/{wo['id']}/receive",{'warehouse_id':fgwarehouse,'location_id':fglocation},'POST',store,False)
check(True,'Unfinished production cannot be received as finished goods')
api('/stock-in/add',{'stockInNo':'BYPASS-'+stamp,'stockInType':'PRODUCTION_RECEIPT','sourceOrderId':wo['id'],'warehouseId':fgwarehouse,'items':[{'materialId':bom['root_material_id'],'quantity':100,'locationId':fglocation}]},'POST',store,False)
check(True,'Generic stock endpoints cannot forge linked production receipts')
api(f"/production-tasks/{tasks[2]['id']}/start",{},'PUT',worker,False)
for i,task in enumerate(tasks):
 api(f"/production-tasks/{task['id']}/start",{},'PUT',worker)
 api(f"/production-tasks/{task['id']}/report",{'reportQuantity':100,'userId':actors[9][0]},'POST',worker)
 if i==0:
  api('/foundation/inspections',inspect('PROCESS',task['id'],100,False),'POST',qc)
  api(f"/production-tasks/{tasks[2]['id']}/start",{},'PUT',worker,False)
  check(True,'Failed process inspection blocks later assembly')
 api('/foundation/inspections',inspect('PROCESS',task['id'],100),'POST',qc)
 if i<3:check(scalar(f"SELECT completed_quantity FROM t_production_plan WHERE id={plan['id']}")=='0','Intermediate operation completion does not count finished goods '+str(i+1))
check(scalar(f"SELECT completed_quantity FROM t_production_plan WHERE id={plan['id']}")=='100','Final operation release completes exactly one hundred products')
api('/foundation/inspections',inspect('SHIPPING',order['id'],100),'POST',qc,False)
check(True,'Shipping inspection requires physical finished-goods receipt first')
receipt=api(f"/fulfillment/work-orders/{wo['id']}/receive",{'warehouse_id':fgwarehouse,'location_id':fglocation},'POST',store)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 ids=[f.result()['id'] for f in [pool.submit(api,f"/fulfillment/work-orders/{wo['id']}/receive",{'warehouse_id':fgwarehouse,'location_id':fglocation},'POST',store) for _ in range(2)]]
check(ids==[receipt['id'],receipt['id']] and float(scalar(f"SELECT SUM(quantity) FROM t_inventory WHERE material_id={bom['root_material_id']}"))==100,'Repeated and concurrent completion receipts cannot double finished goods')
check(scalar(f"SELECT order_status FROM t_order WHERE id={order['id']}")=='2','Receiving finished goods does not mark the order delivered')
api(f"/fulfillment/orders/{order['id']}/ship",{'warehouse_id':fgwarehouse},'POST',store,False)
api('/foundation/inspections',inspect('SHIPPING',order['id'],100,False),'POST',qc)
api(f"/fulfillment/orders/{order['id']}/ship",{'warehouse_id':fgwarehouse},'POST',store,False)
check(True,'Missing or failed shipping inspection blocks dispatch')
api('/foundation/inspections',inspect('SHIPPING',order['id'],100),'POST',qc)
api(f"/fulfillment/orders/{order['id']}/ship",{'warehouse_id':warehouse},'POST',store,False)
check(True,'Shipment cannot deduct the finished batch from a different warehouse')
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 shipped=[f.result() for f in [pool.submit(api,f"/fulfillment/orders/{order['id']}/ship",{'warehouse_id':fgwarehouse},'POST',store) for _ in range(2)]]
check(shipped[0]['id']==shipped[1]['id'] and scalar(f"SELECT order_status FROM t_order WHERE id={order['id']}")=='3','Concurrent shipments create one deduction and complete the sales order')
check(float(scalar('SELECT SUM(quantity) FROM t_inventory'))==0 and scalar('SELECT COUNT(*) FROM t_inventory WHERE quantity<0')=='0','Final material and finished-goods balances reconcile without negatives')
check(scalar('SELECT COUNT(*) FROM t_inventory_transaction WHERE (transaction_type=\'IN\' AND before_quantity+quantity<>after_quantity) OR(transaction_type=\'OUT\' AND before_quantity-quantity<>after_quantity)')=='0','Every inventory ledger row has correct before and after balances')
trace=api(f"/fulfillment/work-orders/{wo['id']}/trace",token=store)
check(len(trace)==28 and all(r['lot_no'] for r in trace),'Trace joins thirteen raw receipt lots, thirteen issue rows, one finished receipt and one shipment')
api('/stock-in/'+str(receipt['document_id']),{},'PUT',store,False)
api('/stock-out/'+str(shipped[0]['document_id']),method='DELETE',token=store,ok=False)
api('/inventory/add',{'materialId':bom['root_material_id'],'warehouseId':fgwarehouse,'quantity':9999},'POST',store,False)
check(True,'Posted documents and stock balances cannot be rewritten or deleted through legacy endpoints')
dash=api('/dashboard/production?date='+today+'&shift=all&line=all',token=admin)
kpis={k['key']:k['value'] for k in dash['kpis']}
check(kpis['plan']==100 and kpis['done']==100 and len(dash['workOrders'])==1,'Canonical dashboard counts one planned batch and one terminal release, not four operation reports')
check(dash['inventory']['finishedGoods']==0 and dash['workOrders'][0]['status']=='已发货','Dashboard reflects actual dispatched stock and order state')
summary=api('/dashboard/summary',token=admin)
recent=api('/dashboard/recent-work-orders',token=admin)
check(summary['workOrders']['completed']==1 and len(recent)==1,'Summary and recent work orders share the canonical manufacturing source')
check(dash['materialReadinessRate']==100,'Completed material issue is shown as fully ready on the dashboard')
check(dash['materialShortages']==[] and dash['alerts']==[],'Dashboard preserves empty shortages and alerts after successful fulfillment')
check(sum(h['actual'] for h in dash['hourlyOutput'])==100,'Hourly output sums only final inspection release quantities')
other=api('/dashboard/production?date=2001-01-01&shift=all&line=all',token=admin)
check(other['workOrders']==[] and all(k['value']==0 for k in other['kpis']),'Dashboard date filter excludes other-day manufacturing data')
api('/dashboard/production?date='+today+'&shift=all&line=line-a',token=admin,ok=False)
check(True,'Dashboard rejects invented production-line options')
report=dict(database=DB,date=datetime.datetime.now().isoformat(),passed=len(checks),checks=checks,fixtures_in_production=False)
(ROOT/'.runtime/stage2-acceptance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('TOTAL PASSED '+str(len(checks)))

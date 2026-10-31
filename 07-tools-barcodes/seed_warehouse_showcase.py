"""Real API-only warehouse walkthrough. Restartable per completed batch; never fabricates stock."""
from pathlib import Path
import json,urllib.request,urllib.error,datetime,sys
BASE='http://127.0.0.1:8080/api'
statepath=Path(__file__).with_name('warehouse-showcase.json')
state=json.loads(statepath.read_text(encoding='utf-8')) if statepath.exists() else {'batches':[]}
def persist():statepath.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
def api(path,body=None,method='GET',token=None):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+(token or admin)} if globals().get('admin') else {})})
 try:
  with urllib.request.urlopen(req,timeout=45) as r:result=json.load(r)
 except urllib.error.HTTPError as e:raise RuntimeError(path+' '+e.read().decode())
 if result.get('code',200)!=200:raise RuntimeError(path+' '+str(result))
 return result.get('data',result)
def post(p,b=None):return api(p,b or {},'POST')
def get(k):return api('/foundation/'+k)
def save(k,b):return post('/foundation/'+k,b)
admin=api('/auth/login',{'userCode':'109001','password':'123456'},'POST')['token']
def log(s):print(s,flush=True)
# Rename existing storage records in place, preserving document references.
whs=get('warehouses');areas=get('areas');locs=get('locations')
specs={'WH-A100-RM':('原料收货一区','原料收货二区','RM-A01','RM-B01'),'WH-A100-LINE':('装配备料区','待退物料区','LINE-A01','LINE-R01'),'WH-A100-FG':('合格成品区','待发货区','FG-A01','FG-S01')}
for w in whs:
 if w['warehouse_code'] not in specs:continue
 names=specs[w['warehouse_code']]
 for i in range(2):
  a=next((a for a in areas if a['warehouse_id']==w['id'] and (a['name']==names[i] or i==0 and a['name']=='默认库区')),None)
  if a:
   if a['name']!=names[i]:a=api('/foundation/areas/'+str(a['id']),{**a,'name':names[i]},'PUT')
  else:a=save('areas',{'code':w['warehouse_code']+'-ZONE-'+str(i+1),'name':names[i],'warehouse_id':w['id']});areas.append(a)
  matching=[l for l in locs if l['area_id']==a['id']]
  if matching:
   l=matching[0];api('/foundation/locations/'+str(l['id']),{**l,'name':names[i]+' · '+names[i+2],'status':1,'x_coordinate':i+1,'y_coordinate':1,'z_coordinate':1},'PUT')
  else:save('locations',{'code':w['warehouse_code']+'-SLOT-'+str(i+1),'name':names[i]+' · '+names[i+2],'area_id':a['id'],'status':1,'x_coordinate':i+1,'y_coordinate':1,'z_coordinate':1})
log('Storage hierarchy renamed and expanded')
bom=get('bom-versions')[0];route=get('routes')[0];materials={m['id']:m for m in get('materials')}
users=api('/mes/assignable-people?role=3');worker=users[0]['id']
workerAccount=next(u for u in get('users') if u['id']==worker);workerToken=api('/auth/login',{'userCode':workerAccount['user_code'],'password':'123456'},'POST')['token']
warehouses=get('warehouses');rm=next(w['id'] for w in warehouses if w['warehouse_code']=='WH-A100-RM');fg=next(w['id'] for w in warehouses if w['warehouse_code']=='WH-A100-FG')
locations=api('/fulfillment/locations');rmLocs=[l['id'] for l in locations if l['warehouse_id']==rm];fgLocs=[l['id'] for l in locations if l['warehouse_id']==fg]
supplier=next(iter(get('suppliers')))
templates=get('qc-templates');incoming=next(t for t in templates if t['kind']=='INCOMING');shipping=next(t for t in templates if t['kind']=='SHIPPING')
generic=next(t for t in templates if t['kind']=='PROCESS');genericItems=[r for r in get('qc-lines') if r['template_id']==generic['id']]
for operation in [o for o in get('operations') if o['route_id']==route['id']]:
 if not any(t for t in templates if t['kind']=='PROCESS' and t.get('operation_id')==operation['id']):
  t=save('qc-templates',{'code':'QC-WH-DEMO-'+str(operation['id']),'name':operation['name']+' · 演示检验标准','kind':'PROCESS','product_id':bom['product_id'],'version':'1.0','operation_id':operation['id'],'phase':'FINAL'})
  for line in genericItems:save('qc-lines',{'template_id':t['id'],'item_id':line['item_id']})
  templates.append(t)
def document(kind,source,warehouse,lines,name):return post('/mes/documents',{'kind':kind,'source_id':source,'warehouse_id':warehouse,'reason':name,'name':name,'business_date':datetime.date.today().isoformat(),'lines':lines})
def posting(doc,template=None):
 data=next(d for d in api('/mes/documents') if d['id']==doc)
 if data['status']=='POSTED':return
 if data['status']=='DRAFT':post(f'/mes/documents/{doc}/confirm')
 if template:
  items=[r['item_id'] for r in get('qc-lines') if r['template_id']==template['id']]
  for line in api(f'/mes/documents/{doc}/lines'):
   if line.get('qc_status')=='PASS':continue
   post(f"/mes/document-lines/{line['id']}/inspect",{'template_id':template['id'],'qualified_quantity':line['quantity'],'results':{str(i):{'actual':'课程演示：实测符合标准','result':'PASS'} for i in items}})
 post(f'/mes/documents/{doc}/post')
def ensure_schedule(task):
 # One multi-process demonstration station avoids assigning the same person to four simultaneous shifts.
 station=get('stations')[-1]['id'];now=datetime.datetime.now();end=now+datetime.timedelta(hours=3)
 teams=get('teams');team=next((t for t in teams if t['name']=='仓储联调演示班组'),None)
 if not team:
  team=save('teams',{'code':'TEAM-WH-DEMO','name':'仓储联调演示班组','leader_id':worker});save('members',{'team_id':team['id'],'user_id':worker})
 schedules=get('schedules')
 covered=next((s for s in schedules if s['station_id']==station and s['team_id']==team['id'] and str(s['end_at']).replace('T',' ')>now.strftime('%Y-%m-%d %H:%M:%S')),None)
 if not covered:save('schedules',{'team_id':team['id'],'shift_id':get('shifts')[0]['id'],'station_id':station,'start_at':(now-datetime.timedelta(minutes=5)).isoformat(),'end_at':end.isoformat()})
 post(f"/mes/tasks/{task['id']}/dispatch",{'station_id':station,'owner_id':worker,'start_at':(now-datetime.timedelta(seconds=10)).isoformat(),'end_at':(now+datetime.timedelta(hours=1)).isoformat()})
if '--foundation-only' in sys.argv:sys.exit(0)
for index,qty in enumerate([100,80,120]):
 if index<len(state['batches']):b=state['batches'][index]
 else:
  order=post('/orders',{'orderNo':'DEMO-WH-20261031-'+str(index+1),'customerName':['青岛智造演示客户','校园办公采购客户','华东渠道演示客户'][index],'productName':'A100有线办公鼠标','quantity':qty,'unitPrice':89,'totalAmount':qty*89})
  plan=post('/production-plans/from-order/'+str(order['id']));wo=post('/foundation/work-orders',{'plan_id':plan['id'],'bom_id':bom['id'],'route_id':route['id'],'owner_id':worker})
  b={'orderId':order['id'],'planId':plan['id'],'workId':wo['id'],'workCode':wo['code'],'quantity':qty,'phase':'created'};state['batches'].append(b);persist()
 if b['phase']=='complete':continue
 work=b['workId'];requirements=api(f'/fulfillment/work-orders/{work}/materials')
 if 'purchaseId' not in b:
  # Buy surplus through a real approved purchase, leaving observable on-hand stock.
  lines=[{'materialId':r['material_id'],'quantity':r['required_quantity']*2,'unitPrice':1,'amount':r['required_quantity']*2} for r in requirements]
  po=post('/purchase-order/add',{'poNo':'DEMO-WH-PO-'+str(index+1),'supplierId':supplier['id'],'status':'DRAFT','items':lines});b['purchaseId']=po['id'];persist()
  api(f"/purchase-order/{po['id']}/submit",{},'PUT');api(f"/purchase-order/{po['id']}/approve",{},'PUT')
 if 'purchaseReceiptId' not in b:
  b['purchaseReceiptId']=document('PURCHASE_RECEIPT',b['purchaseId'],rm,[{'material_id':r['material_id'],'quantity':r['required_quantity']*2,'lot_no':f'DEMO-WH-{index+1}-{r["material_id"]}','location_id':rmLocs[j%len(rmLocs)]} for j,r in enumerate(requirements)],b['workCode']+' 配套物料到货');persist()
 posting(b['purchaseReceiptId'],incoming)
 if 'issueId' not in b:
  stock=api('/fulfillment/stock');lines=[]
  for r in requirements:
   remain=r['required_quantity']
   for st in stock:
    if st['warehouse_id']!=rm or st['material_id']!=r['material_id'] or st['quantity']<=0:continue
    take=min(remain,st['quantity'])
    if take>0:lines.append({'material_id':st['material_id'],'lot_id':st['lot_id'],'quantity':take,'location_id':st['location_id']});remain-=take
   assert remain==0,'Not enough inventory'
  b['issueId']=document('LINE_ISSUE',work,rm,lines,b['workCode']+' BOM成套领料');persist()
 posting(b['issueId'])
 if 'returnId' not in b:
  st=next(s for s in api('/mes/line-stock') if s['work_order_id']==work and s['quantity']>0)
  line={'material_id':st['material_id'],'lot_id':st['lot_id'],'quantity':1,'location_id':rmLocs[0]}
  b['returnLine']=line;b['returnId']=document('LINE_RETURN',work,rm,[line],b['workCode']+' 未用物料退库复核');persist()
 posting(b['returnId'])
 if 'reissueId' not in b:b['reissueId']=document('LINE_ISSUE',work,rm,[b['returnLine']],b['workCode']+' 退料复核后补领');persist()
 posting(b['reissueId'])
 for task in api(f'/foundation/work-orders/{work}/tasks'):
  if task['task_status']==4:continue
  if task['task_status']<3:
   ensure_schedule(task)
   if task['task_status']<2:api(f"/production-tasks/{task['id']}/start",{},'PUT',workerToken)
   reports=api('/mes/reports');report=next((r for r in reports if r['task_id']==task['id'] and r['status']!='REJECTED'),None)
   reportId=report['id'] if report else api('/mes/reports',{'task_id':task['id'],'good_quantity':qty,'bad_quantity':0,'remark':b['workCode']+' 仓储全流程演示报工'},'POST',workerToken)
   if not report or report['status']!='CONFIRMED':post(f'/mes/reports/{reportId}/confirm')
  template=next((t for t in templates if t['kind']=='PROCESS' and t.get('operation_id')==task['operation_id']),next(t for t in templates if t['kind']=='PROCESS'))
  items=[r for r in get('qc-lines') if r['template_id']==template['id']]
  post('/foundation/inspections',{'template_id':template['id'],'source_type':'TASK','source_id':task['id'],'quantity':qty,'qualified_quantity':qty,'results':{str(r['item_id']):{'actual':'实测合格','result':'PASS'} for r in items}})
  log(b['workCode']+' '+task['operation_name']+' released')
 if 'receiptIds' not in b:b['receiptIds']=[];persist()
 for part,amount in enumerate([qty//2,qty-qty//2]):
  if part>=len(b['receiptIds']):
   st=next(s for s in api('/mes/line-stock') if s['work_order_id']==work and s['material_id']==bom['root_material_id'] and s['quantity']>0)
   d=document('PRODUCTION_RECEIPT',work,fg,[{'material_id':st['material_id'],'lot_id':st['lot_id'],'quantity':amount,'location_id':fgLocs[part%len(fgLocs)]}],b['workCode']+' 成品第'+str(part+1)+'批入库');b['receiptIds'].append(d);persist()
  posting(b['receiptIds'][part])
 if 'shipmentId' not in b:
  stocks=[s for s in api('/fulfillment/stock') if s['warehouse_id']==fg and s['material_id']==bom['root_material_id'] and s['lot_no']=='FG-MO-'+str(work) and s['quantity']>0]
  st=stocks[0];b['shipmentId']=document('SALES_SHIPMENT',b['orderId'],fg,[{'material_id':st['material_id'],'lot_id':st['lot_id'],'quantity':qty//2,'location_id':st['location_id']}],b['workCode']+' 首批交付，余量待发');persist()
 posting(b['shipmentId'],shipping);b['phase']='complete';persist();log(b['workCode']+' complete: produced '+str(qty)+', shipped '+str(qty//2))
# Per-material thresholds are genuine persisted master data, with two demonstrable warnings.
stock=api('/fulfillment/stock');rmids={r['material_id'] for r in requirements}
for i,mid in enumerate(sorted(rmids)):
 m=materials[mid];balance=sum(s['quantity'] for s in stock if s['material_id']==mid and s['warehouse_id']==rm)
 threshold=balance+50 if i<2 else max(10,round(balance*0.25))
 api('/foundation/materials/'+str(mid),{**m,'safety_stock':threshold},'PUT')
state['thresholdRule']='前两种物料按当前余量+50设置演示预警，其他为余量25%且至少10；后续由物料安全库存配置维护';persist();log('Three complete API workflows and safety thresholds prepared')

"""Extended real acceptance, after the complete core API workflow, isolated database only."""
from verify_mes_completion import *
from io import BytesIO
from PIL import Image
today=datetime.date.today().isoformat()

# These checks exercise independently configurable features, not just catalog CRUD.
baseUnit=next(u for u in get('units') if u['uom_name']==requirements[0]['unit'])
boxUnit=save('units',{'uom_code':'MC-BOX-'+stamp,'uom_name':'验收箱'+stamp,'main_unit_id':baseUnit['id'],'conversion_rate':10})
extraPO=purchase([(requirements[0]['material_id'],20)],'CONVERT')
converted=document('PURCHASE_RECEIPT',extraPO['id'],warehouse,[{'material_id':requirements[0]['material_id'],'unit_id':boxUnit['id'],'quantity':2,'lot_no':'MC-BOXLOT-'+stamp,'location_id':location}])
convertedLines=api(f'/mes/documents/{converted}/lines',token=store)
check(convertedLines[0]['quantity']==20,'Warehouse document converts two auxiliary units into twenty base units')
post(f'/mes/documents/{converted}/confirm',token=store);check_document(converted,incoming);post(f'/mes/documents/{converted}/post',token=store)
lot=convertedLines[0]['lot_id'];material=convertedLines[0]['material_id']
returned=document('PURCHASE_RETURN',extraPO['id'],warehouse,[{'material_id':material,'lot_id':lot,'quantity':5,'location_id':location}]);post(f'/mes/documents/{returned}/confirm',token=store);post(f'/mes/documents/{returned}/post',token=store)
post('/mes/documents',{'kind':'PURCHASE_RETURN','source_id':po['id'],'warehouse_id':warehouse,'reason':'错误供应商批次','lines':[{'material_id':material,'lot_id':lot,'quantity':1,'location_id':location}]},store,False)
post('/mes/documents',{'kind':'PURCHASE_RETURN','source_id':extraPO['id'],'warehouse_id':warehouse,'reason':'超量','lines':[{'material_id':material,'lot_id':lot,'quantity':16,'location_id':location}]},store,False)
check(True,'Supplier return is bound to original purchase batch and cumulative received quantity')
remainingLots=api(f'/mes/return-lots?kind=PURCHASE_RETURN&source={extraPO["id"]}',token=store)
check(next(r for r in remainingLots if r['lot_id']==lot)['quantity']==15,'Return selector shows remaining returnable source quantity')

# Equipment due jobs, idempotence and the same repair state machine as manual repairs.
checkPlan=save('checks',{'code':'MC-CHECK-'+stamp,'name':'隔离周期点检','equipment_id':machine['id'],'interval_days':1,'standard':'供电及功能正常','owner_id':actors[8][0],'enabled':1,'plan_type':'CHECK'})
post('/mes/checks/generate',token=equipment);before=scalar(f'SELECT COUNT(*) FROM mm_check_due WHERE plan_id={checkPlan["id"]}')
post('/mes/checks/generate',token=equipment)
check(before=='1' and scalar(f'SELECT COUNT(*) FROM mm_check_due WHERE plan_id={checkPlan["id"]}')=='1','Periodic check generation is persistent and cannot duplicate one plan/date task')
record=dict(plan_id=checkPlan['id'],checked_at=datetime.datetime.now().isoformat(),actual_value='隔离验收异常，生成报修',result='FAIL')
first=save('check-records',record,equipment);second=save('check-records',record,equipment)
check(first['maintenance_id']==second['maintenance_id'] and scalar(f"SELECT status FROM t_equipment WHERE id={machine['id']}")=='REPAIR_REQUESTED','Repeated point-check failures reuse the open repair and update equipment state')
post('/foundation/schedules',{'team_id':team['id'],'shift_id':shift['id'],'station_id':station['id'],'start_at':(datetime.datetime.now()+datetime.timedelta(days=2)).isoformat(),'end_at':(datetime.datetime.now()+datetime.timedelta(days=2,hours=1)).isoformat()},admin,False)
check(True,'Faulty station equipment prevents new scheduling')
repair=first['maintenance_id']
api(f'/maintenances/{repair}/assign',{'userId':actors[8][0],'userName':'不可伪造的姓名'},'PUT',equipment)
api(f'/maintenances/{repair}/start',{},'PUT',equipment)
api(f'/maintenances/{repair}/finish',{'maintenanceContent':'隔离验收维修完成','result':'REPAIRED','cost':0,'scrapEquipment':False},'PUT',equipment)
check(scalar(f"SELECT status FROM t_equipment WHERE id={machine['id']}")=='NORMAL','Repair completion restores equipment availability')

# Calendar rotation has explicit shifts, two distinct teams and a working-holiday override.
otherPassword=uuid.uuid4().hex
sql("INSERT INTO t_user(user_code,username,password,real_name,role_id,dept_id,status,deleted) VALUES('103999','mcworker2','"+otherPassword+"','隔离第二操作工',3,1,1,0)",DB)
otherWorker=int(scalar("SELECT id FROM t_user WHERE user_code='103999'"))
team2=save('teams',{'code':'MC-TEAM2-'+stamp,'name':'第二验收班组','leader_id':otherWorker});save('members',{'team_id':team2['id'],'user_id':otherWorker})
dayShift=save('shifts',{'code':'MC-DAY-'+stamp,'name':'验收日班','start_time':'08:00','end_time':'20:00'})
nightShift=save('shifts',{'code':'MC-NIGHT-'+stamp,'name':'验收夜班','start_time':'20:00','end_time':'08:00'})
future=datetime.date(2032,1,10)
save('holidays',{'work_date':future.isoformat(),'is_working':1,'name':'隔离调休上班'})
rotation=save('rotations',{'code':'MC-ROT-'+stamp,'name':'隔离两班倒','start_date':future.isoformat(),'end_date':future.isoformat(),'station_id':station['id'],'cycle_days':1,'cycle_unit':'DAY','mode':'TWO'})
for i,t in enumerate([team,team2]):save('rotation-teams',{'rotation_id':rotation['id'],'team_id':t['id'],'sequence_no':i+1})
for i,s in enumerate([dayShift,nightShift]):save('rotation-shifts',{'rotation_id':rotation['id'],'shift_id':s['id'],'sequence_no':i+1})
generated=post(f'/mes/rotations/{rotation["id"]}/generate');post(f'/mes/rotations/{rotation["id"]}/generate',ok=False)
check(generated==2,'Two-shift rotation uses configured shifts and holiday override, with replay protection')
check(len(api('/mes/calendar',token=worker))>=2,'Personal calendar contains only actual member schedules')

# SOP images go through authenticated upload and are served only through the application.
image=Image.new('RGB',(200,80),'white');imageBytes=BytesIO();image.save(imageBytes,format='PNG')
boundary='MesCompletionBoundary'+uuid.uuid4().hex
multipart=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="isolated-sop.png"\r\nContent-Type: image/png\r\n\r\n').encode()+imageBytes.getvalue()+f'\r\n--{boundary}--\r\n'.encode()
req=urllib.request.Request(BASE+'/mes/sop-images',data=multipart,method='POST',headers={'Content-Type':'multipart/form-data; boundary='+boundary,'Authorization':'Bearer '+admin})
with urllib.request.urlopen(req,timeout=15) as response:sopPath=json.load(response)['data']
save('sops',{'code':'MC-SOP-'+stamp,'name':'隔离上传验收图','product_id':bom['product_id'],'operation_id':ops[0]['id'],'version':'1','image_path':sopPath,'enabled':1})
records=api('/mes/task-sops/'+str(tasks[0]['id']),token=worker)
check(any(r['image_path']==sopPath for r in records),'Task-specific SOP resolves by actual product and operation')
try:urllib.request.urlopen(BASE.removesuffix('/api')+sopPath,timeout=10);raise AssertionError('Unauthenticated SOP image accepted')
except urllib.error.HTTPError as error:check(error.code==401,'SOP image endpoint requires authentication')

# Create configured semi-products, then decompose a new unissued order.
allMaterials={m['id']:m for m in get('materials')}
parentLines=[l for l in get('bom-lines') if l['bom_id']==bom['id']]
semis=[m for m in allMaterials.values() if m['material_type']==2 and any(l['material_id']==m['id'] for l in parentLines)]
for semi in semis:
    product=save('products',{'product_code':'MC-SEMI-'+str(semi['id'])+'-'+stamp,'product_name':'隔离'+semi['material_name'],'unit':semi['unit'],'cost_price':1,'selling_price':1})
    save('mappings',{'product_id':product['id'],'material_id':semi['id']})
    semiBom=save('bom-versions',{'code':'MC-SB-'+str(semi['id'])+'-'+stamp,'name':'隔离半成品BOM','product_id':product['id'],'root_material_id':semi['id'],'version':'1'})
    reached={semi['id']};added=set()
    while True:
        changed=False
        for l in parentLines:
            if l['parent_material_id'] in reached and l['id'] not in added:
                save('bom-lines',{'bom_id':semiBom['id'],'parent_material_id':l['parent_material_id'],'material_id':l['material_id'],'quantity':l['quantity']});reached.add(l['material_id']);added.add(l['id']);changed=True
        if not changed:break
    semiRoute=save('routes',{'code':'MC-SR-'+str(semi['id'])+'-'+stamp,'name':'隔离半成品路线','product_id':product['id'],'version':'1'})
    op=save('operations',{'code':'MC-SO-'+str(semi['id'])+'-'+stamp,'name':'半成品制造','route_id':semiRoute['id'],'sequence_no':10,'station_id':station['id'],'standard_minutes':1,'requires_qc':1,'is_key':1})
    t=save('qc-templates',{'code':'MC-SQ-'+str(semi['id'])+'-'+stamp,'name':'半成品最终检验','product_id':product['id'],'operation_id':op['id'],'kind':'PROCESS','phase':'FINAL','version':'1'})
    save('qc-lines',{'template_id':t['id'],'item_id':numeric['id']});templates[op['id']]=t
stockWork=post('/mes/work-orders',{'product_id':bom['product_id'],'quantity':10,'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]})
children=post(f'/mes/work-orders/{stockWork["id"]}/decompose',{'owner_id':actors[3][0]})
post(f'/mes/work-orders/{stockWork["id"]}/decompose',{'owner_id':actors[3][0]},ok=False)
parentRequirements=api(f'/fulfillment/work-orders/{stockWork["id"]}/materials',token=store)
check(len(children)==len(semis)>0 and any(allMaterials[r['material_id']]['material_type']==2 for r in parentRequirements),'Recursive breakdown creates semi-product orders and replaces assembly raw consumption with semi-product consumption')
parentTasks=api(f'/foundation/work-orders/{stockWork["id"]}/tasks',token=admin)
splitTask=post(f'/mes/tasks/{parentTasks[0]["id"]}/split',{'quantity':4})
parentTasks=api(f'/foundation/work-orders/{stockWork["id"]}/tasks',token=admin)
check(sum(t['quantity'] for t in parentTasks if t['operation_id']==parentTasks[0]['operation_id'])==10,'Splitting an operation preserves its exact total production quantity')

def procure_and_issue(work):
    required=api(f'/fulfillment/work-orders/{work}/materials',token=store)
    stock=api('/fulfillment/stock',token=store)
    missing=[]
    for r in required:
        available=sum(s['quantity'] for s in stock if s['material_id']==r['material_id'])
        shortage=max(0,r['required_quantity']-available)
        if shortage and allMaterials[r['material_id']]['material_type']==1:missing.append((r['material_id'],shortage))
    if missing:
        p=purchase(missing,'WO-'+str(work));doc=document('PURCHASE_RECEIPT',p['id'],warehouse,[{'material_id':m,'quantity':q,'lot_no':'MC-WO-'+stamp+'-'+str(work)+'-'+str(m),'location_id':location} for m,q in missing]);post(f'/mes/documents/{doc}/confirm',token=store);check_document(doc,incoming);post(f'/mes/documents/{doc}/post',token=store)
    stock=api('/fulfillment/stock',token=store);byWarehouse={}
    for r in required:
        left=r['required_quantity']
        for s in stock:
            if s['material_id']!=r['material_id'] or s['quantity']<=0:continue
            take=min(left,s['quantity'])
            if take>0:byWarehouse.setdefault(s['warehouse_id'],[]).append(dict(material_id=s['material_id'],lot_id=s['lot_id'],quantity=take,location_id=s['location_id']));left-=take
            if not left:break
        assert left==0,('Missing semi-product inventory',r)
    for w,lines in byWarehouse.items():
        doc=document('LINE_ISSUE',work,w,lines);post(f'/mes/documents/{doc}/confirm',token=store);post(f'/mes/documents/{doc}/post',token=store)
    check(all(r['issued'] and r['shortage_quantity']==0 for r in api(f'/fulfillment/work-orders/{work}/materials',token=store)),'Modern issues correctly update readiness for work '+str(work))

def manufacture(work):
    taskRows=api(f'/foundation/work-orders/{work}/tasks',token=admin)
    for task in taskRows:
        dispatch(task);api(f'/production-tasks/{task["id"]}/start',{},'PUT',worker);post(f'/production-tasks/{task["id"]}/report',{'reportQuantity':task['quantity']},worker);post('/foundation/inspections',inspect(task),qc)
    return post(f'/fulfillment/work-orders/{work}/receive',{'warehouse_id':fgwarehouse,'location_id':fgLocation},store)

for child in children:procure_and_issue(child);manufacture(child)
procure_and_issue(stockWork['id']);manufacture(stockWork['id'])
check(scalar(f"SELECT status FROM mm_work_order WHERE id={stockWork['id']}")=='PRODUCTION_COMPLETED','Semi-products can be produced, received, issued into parent and completed through split assembly tasks')

# Multiple work orders can fulfil a single sales order without exceeding the order quantity.
multiOrder=post('/orders',{'orderNo':'MC-MULTI-'+stamp,'customerName':'隔离多工单客户','productName':'A100有线办公鼠标','quantity':2,'unitPrice':89,'totalAmount':178},sales)
works=[]
for _ in range(2):works.append(post('/mes/work-orders',{'product_id':bom['product_id'],'quantity':1,'order_id':multiOrder['id'],'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]}))
post('/mes/work-orders',{'product_id':bom['product_id'],'quantity':1,'order_id':multiOrder['id'],'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]},ok=False)
check(True,'Multiple work-order planning cannot exceed the sales order quantity')
shipLines=[]
for w in works:
    procure_and_issue(w['id']);manufacture(w['id'])
    lot=int(scalar("SELECT id FROM t_lot WHERE lot_no='FG-MO-"+str(w['id'])+"'"));shipLines.append(dict(material_id=rootMaterial,lot_id=lot,location_id=fgLocation,quantity=1))
doc=document('SALES_SHIPMENT',multiOrder['id'],fgwarehouse,shipLines);post(f'/mes/documents/{doc}/confirm',token=store);check_document(doc,shipping);post(f'/mes/documents/{doc}/post',token=store)
check(scalar(f"SELECT order_status FROM t_order WHERE id={multiOrder['id']}")=='3','A sales shipment can aggregate two independently completed work-order batches')

# Dashboard and trace must include the new document path, not just the old full-batch path.
dashboard=api('/dashboard/production?date='+today+'&shift=all&line=all',token=admin)
check(next(w for w in dashboard['workOrders'] if w['id']==wo['code'])['status']=='已发货','Dashboard recognises completed receipt and shipment through new partial documents')
check(len(api(f'/fulfillment/work-orders/{wo["id"]}/trace',token=store))>24,'Work-order trace includes partial purchase receipts, returns and shipments')
check(api('/mes/work-relations',token=admin),'Parent/child work-order relations are queryable for trace')
check(all('costPrice' not in r and 'sellingPrice' not in r for r in api('/products',token=worker)),'Legacy product endpoint applies the same price visibility boundary')
print('Extended completion acceptance passed:',len(checks))

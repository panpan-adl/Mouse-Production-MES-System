"""Add linked purchase demonstration records through business APIs in recorded isolated DBs only."""
import datetime,json,subprocess,urllib.request,socket
from pathlib import Path
from stage1_database import ROOT,sql,config,env
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
results=[]
for filename,port in [('.runtime/four-tier-20261030/state.json',8093),('.runtime/mes-completion-database.json',8082)]:
 try:
  with socket.create_connection(('127.0.0.1',port),timeout=2):pass
 except OSError:
  print(f'{port}: service offline, skipped without business writes',flush=True);continue
 database=json.loads((ROOT/filename).read_text())['database']
 assert database.startswith(('mouse_mes_four_tier_','mouse_mes_completion_test_'))
 backup=ROOT/'.runtime/backups'/f'{database}_purchase_samples_{datetime.datetime.now():%Y%m%d_%H%M%S}.sql'
 with backup.open('wb') as output:
  proc=subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot','--single-transaction','--default-character-set=utf8mb4',database],env=env,stdout=output,stderr=subprocess.PIPE)
 assert proc.returncode==0 and backup.stat().st_size>1000
 token=None
 def api(path,body=None,method=None):
  request=urllib.request.Request(f'http://127.0.0.1:{port}/api'+path,data=None if body is None else json.dumps(body).encode(),method=method or ('GET' if body is None else 'POST'),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
  with opener.open(request,timeout=20) as response:r=json.load(response)
  assert r['code']==200,(path,r)
  return r['data']
 token=api('/auth/login',{'userCode':'109001','password':'123456'})['token']
 inventory_before=sql('SELECT COUNT(*),COALESCE(SUM(quantity),0) FROM t_inventory;SELECT COUNT(*) FROM t_inventory_transaction',database)
 marker='采购申请展示样例20261031'
 existing=api('/material-requests')
 if sum(marker in (r.get('remark') or '') for r in existing)>=12:
  print(f'{port}: sample set already exists; skipped',flush=True);continue
 created=[]
 candidates=sql("SELECT t.id,m.material_id,COALESCE((SELECT SUM(i.quantity) FROM t_inventory i WHERE i.material_id=m.material_id),0) stock FROM t_production_task t JOIN mm_task_context c ON c.task_id=t.id JOIN mm_work_material m ON m.work_order_id=c.work_order_id WHERE t.task_status<4 ORDER BY m.material_id,t.id",database).strip().splitlines()[1:]
 suppliers=[s for s in api('/suppliers') if s.get('status')==1]
 used=set()
 for raw in candidates:
  if len(created)>=12:break
  task,material,stock=raw.split();task=int(task);material=int(material)
  if material in used:continue
  if any(r.get('sourceTaskId')==task and r.get('materialId')==material and not r.get('warehouseId') and r.get('requestStatus')!='PURCHASE_ORDERED' for r in existing):continue
  if any(r.get('sourceTaskId')==task and r.get('materialId')==material and marker in (r.get('remark') or '') for r in existing):continue
  i=len(created)
  request=api('/material-requests',{'sourceTaskId':task,'materialId':material,'requiredQuantity':float(stock)+(i+1)*10,'requiredDate':str(datetime.date(2026,11,1)+datetime.timedelta(days=i)),'remark':marker+'；生产备料需求'})
  used.add(material);existing.append(request)
  api(f"/material-requests/{request['id']}/request-purchase",{},'PUT')
  row={'requestId':request['id'],'requestNo':request['requestNo'],'material':request['materialName'],'status':'PURCHASE_REQUESTED'}
  if i>=8:
   order=api(f"/material-requests/{request['id']}/purchase-order",{'supplierId':suppliers[i%len(suppliers)]['id'],'expectedDate':str(datetime.date(2026,11,1)+datetime.timedelta(days=i)),'remark':marker,'items':[{'unitPrice':round(0.5+i*0.75,2)}]})
   row.update(orderId=order['id'],orderNo=order['poNo'],status='PURCHASE_ORDERED')
   if i>=9:api(f"/purchase-order/{order['id']}/submit",{},'PUT')
   if i>=11:api(f"/purchase-order/{order['id']}/approve",{},'PUT')
  created.append(row)
 assert len(created)==12,(database,len(created))
 assert sql('SELECT COUNT(*),COALESCE(SUM(quantity),0) FROM t_inventory;SELECT COUNT(*) FROM t_inventory_transaction',database)==inventory_before
 refreshed=api('/material-requests')
 for row in created:
  current=next(r for r in refreshed if r['id']==row['requestId'])
  assert current['requestStatus']==row['status']
  if 'orderId' in row:assert current['purchaseOrderId']==row['orderId']
 results.append({'database':database,'port':port,'backup':str(backup),'created':created,'inventory_unchanged':True})
 (ROOT/'.runtime/purchase-application-samples.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
 print(f'{port}: added 12 applications, 8 pending and 4 linked orders; inventory unchanged',flush=True)

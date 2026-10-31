"""Add traceable simulation business through MES APIs without rewriting history."""
import datetime as dt
import json
import subprocess
import urllib.request
import urllib.error
import sys
from pathlib import Path
from stage1_database import ROOT, sql, config, env

STATE=json.loads((ROOT/'.runtime/four-tier-20261030/state.json').read_text())
DB=STATE['database'];assert DB=='mouse_mes_four_tier_20261030_171925_883824'
BASE='http://127.0.0.1:5175/api'
resume=Path(sys.argv[sys.argv.index('--resume')+1]) if '--resume' in sys.argv else None
stamp=resume.name.removeprefix('factory-enrichment-') if resume else dt.datetime.now().strftime('%Y%m%d_%H%M%S')
out=resume or ROOT/'.runtime'/('factory-enrichment-'+stamp);out.mkdir(exist_ok=True)
report=json.loads((out/'report.json').read_text(encoding='utf-8')) if resume else {'database':DB,'created':[],'progress':[],'complete':False}
def checkpoint(): (out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
def log(message):
    report['progress'].append(message);checkpoint();print(message,flush=True)
backup=out/'before.sql'
if not resume:
    with backup.open('wb') as f:
        result=subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot','--single-transaction','--routines','--triggers','--default-character-set=utf8mb4',DB],env=env,stdout=f,stderr=subprocess.PIPE)
    assert result.returncode==0 and backup.stat().st_size>1000,'Backup failed'
report['backup']=str(backup);checkpoint()

def api(path,body=None,method=None,token=None):
    req=urllib.request.Request(BASE+path,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),method=method or ('POST' if body is not None else 'GET'),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+(token or admin)} if token or 'admin' in globals() else {})})
    try:
        with urllib.request.urlopen(req,timeout=30) as r: result=json.load(r)
    except urllib.error.HTTPError as e: raise RuntimeError(path+': '+e.read().decode())
    if result.get('code',200)!=200:raise RuntimeError(path+': '+str(result.get('msg')))
    return result.get('data',result)
def records(q):return [json.loads(line) for line in sql(q,DB).splitlines()[1:] if line.strip()]
users=records("SELECT JSON_OBJECT('id',id,'role',role_id,'code',user_code,'password',password) FROM t_user WHERE deleted=0 AND status=1")
tokens={}
for role in [9,3,4,5,6,7]:
    user=next(u for u in users if u['role']==role)
    try:login=api('/auth/login',{'userCode':user['code'],'password':'123456'})
    except RuntimeError:login=api('/auth/login',{'userCode':user['code'],'password':user['password']})
    tokens[role]=(user['id'],login['token'])
admin=tokens[9][1];worker=tokens[3][1];qc=tokens[4][1];store=tokens[5][1];buyer=tokens[6][1];sales=tokens[7][1]
def get(key):return api('/foundation/'+key)
def save(key,body):return api('/foundation/'+key,body)
materials={m['id']:m for m in get('materials')}
products={p['id']:p for p in get('products')}
warehouses=get('warehouses');warehouse=next(w['id'] for w in warehouses if w['warehouse_code']=='WH-A100-RM');fgwarehouse=next(w['id'] for w in warehouses if w['warehouse_code']=='WH-A100-FG')
locations=api('/fulfillment/locations');location=next(l['id'] for l in locations if l['warehouse_id']==warehouse and str(l.get('status','1'))!='0');fgLocation=next(l['id'] for l in locations if l['warehouse_id']==fgwarehouse and str(l.get('status','1'))!='0')
supplier=get('suppliers')[0];customers=get('customers')
templates=get('qc-templates');templateLines=get('qc-lines');items={i['id']:i for i in get('qc-items')}
incoming=next(t for t in templates if t['kind']=='INCOMING' and t['product_id']==6)
shipping=next(t for t in templates if t['kind']=='SHIPPING' and t['product_id']==6)
boms={b['id']:b for b in get('bom-versions')}

def results(template):
    data={}
    for line in templateLines:
        if line['template_id']!=template['id']:continue
        item=items[line['item_id']]
        actual=(float(item.get('lower_limit') or 0)+float(item.get('upper_limit') or 10))/2 if item.get('data_type')=='NUMBER' else '外观完整、尺寸与功能符合工艺要求'
        data[str(item['id'])]={'actual':actual,'result':'PASS'}
    assert data,'Missing template items';return data
def document(kind,source,wh,lines,reason):
    return api('/mes/documents',{'kind':kind,'source_id':source,'warehouse_id':wh,'reason':reason,'lines':lines},token=store)
def post_document(doc,template=None):
    api(f'/mes/documents/{doc}/confirm',{},token=store)
    if template:
        for row in api(f'/mes/documents/{doc}/lines',token=qc):api(f"/mes/document-lines/{row['id']}/inspect",{'template_id':template['id'],'qualified_quantity':row['quantity'],'results':results(template)},token=qc)
    api(f'/mes/documents/{doc}/post',{},token=store)

def procure(work,buffer=0):
    required=api(f'/fulfillment/work-orders/{work}/materials');stock=api('/fulfillment/stock')
    lines=[]
    for r in required:
        available=sum(float(s['quantity']) for s in stock if s['material_id']==r['material_id'] and s['warehouse_id']==warehouse)
        shortage=max(0,float(r['required_quantity'])-available)+buffer
        if shortage:lines.append((r['material_id'],shortage))
    if lines:
        po=api('/purchase-order/add',{'poNo':f'PO-{stamp}-{work}','supplierId':supplier['id'],'status':'DRAFT','orderDate':dt.date.today().isoformat(),'expectedDate':dt.date.today().isoformat(),'items':[{'materialId':m,'quantity':q,'unitPrice':round(0.35+(m%9)*.65,2),'amount':round(q*(.35+(m%9)*.65),2)} for m,q in lines]},token=buyer)
        for action in ['submit','approve']:api(f"/purchase-order/{po['id']}/{action}",{},'PUT',buyer)
        doc=document('PURCHASE_RECEIPT',po['id'],warehouse,[{'material_id':m,'quantity':q,'lot_no':f'LOT-{stamp}-{work}-{m}','location_id':location} for m,q in lines],'鼠标产线备料采购到货')
        post_document(doc,incoming)
        report['created'].append({'purchase':po['id'],'receipt':doc});checkpoint()
    api(f'/fulfillment/work-orders/{work}/issue',{'warehouse_id':warehouse},token=store)

workshop=next(w for w in get('workshops') if w['name']=='鼠标装配车间')
if resume:
    station=next(s for s in get('stations') if s['name']=='A100柔性装配二线')
    schedule=next(s for s in get('schedules') if s['station_id']==station['id'])
    begin=dt.datetime.fromisoformat(schedule['start_at'])+dt.timedelta(seconds=1)
    end=dt.datetime.fromisoformat(schedule['end_at'])-dt.timedelta(seconds=1)
else:
    station=save('stations',{'name':'A100柔性装配二线','workshop_id':workshop['id']})
    team=save('teams',{'name':'柔性装配协作班','leader_id':tokens[3][0]});save('members',{'team_id':team['id'],'user_id':tokens[3][0]})
    now=dt.datetime.now().replace(microsecond=0);begin=now-dt.timedelta(minutes=2);end=now+dt.timedelta(hours=6)
    save('schedules',{'team_id':team['id'],'shift_id':get('shifts')[0]['id'],'station_id':station['id'],'start_at':begin.isoformat(),'end_at':end.isoformat()})
log('已创建柔性装配二线及覆盖当前任务的人员排班')

def produce(wo,number=None,partial=None):
    tasks=sorted(api(f"/foundation/work-orders/{wo['id']}/tasks"),key=lambda t:t.get('sequence_no',0))
    limit=number if number is not None else len(tasks)
    for index,task in enumerate(tasks[:limit]):
        if task.get('task_status')==4:continue
        api(f"/mes/tasks/{task['id']}/dispatch",{'station_id':station['id'],'owner_id':tokens[3][0],'start_at':begin.isoformat(),'end_at':end.isoformat()})
        api(f"/production-tasks/{task['id']}/start",{},'PUT',worker)
        amount=partial if partial is not None and index==limit-1 else task['quantity']
        api(f"/production-tasks/{task['id']}/report",{'reportQuantity':amount},token=worker)
        if amount<task['quantity']:continue
        applicable=next(t for t in templates if t['kind']=='PROCESS' and t['phase']=='FINAL' and t['operation_id']==task['operation_id'])
        api('/foundation/inspections',{'template_id':applicable['id'],'source_type':'TASK','source_id':task['id'],'quantity':task['quantity'],'qualified_quantity':task['quantity'],'results':results(applicable)},token=qc)

scenarios=[
    {'qty':80,'bom':1,'route':1,'mode':'ship','label':'办公渠道补货'},
    {'qty':60,'bom':1,'route':1,'mode':'receipt','label':'企业办公设备配套'},
    {'qty':200,'bom':2,'route':2,'mode':'complete','label':'上壳组件备货'},
    {'qty':180,'bom':3,'route':3,'mode':'complete','label':'主板组件备货'},
    {'qty':90,'bom':1,'route':1,'mode':'issued','label':'电商渠道生产备料'},
    {'qty':120,'bom':1,'route':1,'mode':'pending','label':'校园采购批次'},
    {'qty':150,'bom':1,'route':1,'mode':'pending','label':'区域经销商补货'},
    {'qty':100,'bom':1,'route':1,'mode':'partial','label':'办公静音系列增补'},
    {'qty':45,'bom':1,'route':1,'mode':'pending_qc','label':'海外渠道首件复核批次'},
    {'qty':12,'bom':1,'route':1,'mode':'failed_qc','label':'按键装配异常复核批次'},
]
for index,s in enumerate(scenarios):
    bom=boms[s['bom']];product=products[bom['product_id']]
    customer=customers[index%len(customers)]
    existing=next((r for r in report['created'] if r.get('quantity')==s['qty'] and r.get('scenario')==s['mode']),None)
    if existing:
        wo={'id':existing['work']};order={'id':existing['order']}
    else:
        order=api('/orders',{'orderNo':f'SO-{stamp}-{index+1:02d}','customerName':customer['customer_name'],'productName':product['product_name'],'quantity':s['qty'],'unitPrice':product.get('selling_price') or 89,'totalAmount':s['qty']*float(product.get('selling_price') or 89),'remark':s['label']},token=sales)
        plan=api(f"/production-plans/from-order/{order['id']}",{})
        wo=api('/foundation/work-orders',{'plan_id':plan['id'],'bom_id':bom['id'],'route_id':s['route'],'owner_id':tokens[3][0]})
        report['created'].append({'order':order['id'],'plan':plan['id'],'work':wo['id'],'quantity':s['qty'],'scenario':s['mode']});checkpoint()
    if existing and existing.get('done'):continue
    issued=int(sql(f"SELECT COUNT(*) FROM mm_stock_posting WHERE kind='ISSUE' AND source_id={wo['id']}",DB).strip().splitlines()[-1])>0
    if s['mode']!='pending' and not issued:procure(wo['id'],10)
    if s['mode'] in ['ship','receipt','complete']:produce(wo)
    elif s['mode']=='partial':produce(wo,3,40)
    elif s['mode'] in ['pending_qc','failed_qc']:
        staff=next(u for u in users if u['id']==(13 if s['mode']=='pending_qc' else 19))
        try:staffToken=api('/auth/login',{'userCode':staff['code'],'password':'123456'})['token']
        except RuntimeError:staffToken=api('/auth/login',{'userCode':staff['code'],'password':staff['password']})['token']
        extraStation=save('stations',{'name':'电子装配复核工位' if s['mode']=='pending_qc' else '按键装配复检工位','workshop_id':workshop['id']})
        extraTeam=save('teams',{'name':'首件复核协作班' if s['mode']=='pending_qc' else '质量返修协作班','leader_id':staff['id']})
        save('members',{'team_id':extraTeam['id'],'user_id':staff['id']})
        start=dt.datetime.now().replace(microsecond=0)-dt.timedelta(minutes=1);finish=start+dt.timedelta(hours=4)
        save('schedules',{'team_id':extraTeam['id'],'shift_id':get('shifts')[0]['id'],'station_id':extraStation['id'],'start_at':start.isoformat(),'end_at':finish.isoformat()})
        task=api(f"/foundation/work-orders/{wo['id']}/tasks")[0]
        api(f"/mes/tasks/{task['id']}/dispatch",{'station_id':extraStation['id'],'owner_id':staff['id'],'start_at':start.isoformat(),'end_at':finish.isoformat()})
        api(f"/production-tasks/{task['id']}/start",{},'PUT',staffToken)
        api(f"/production-tasks/{task['id']}/report",{'reportQuantity':task['quantity']},token=staffToken)
        if s['mode']=='failed_qc':
            applicable=next(t for t in templates if t['kind']=='PROCESS' and t['phase']=='FINAL' and t['operation_id']==task['operation_id'])
            measured=results(applicable)
            numeric=next(id for id in measured if items[int(id)].get('data_type')=='NUMBER')
            measured[numeric]={'actual':float(items[int(numeric)].get('upper_limit') or 10)+2,'result':'FAIL'}
            inspection=api('/foundation/inspections',{'template_id':applicable['id'],'source_type':'TASK','source_id':task['id'],'quantity':task['quantity'],'qualified_quantity':0,'results':measured},token=qc)
            assert inspection['status']=='BLOCKED'
            nc=next(n for n in api('/mes/nonconformities',token=qc) if n['inspection_id']==inspection['id'])
            api(f"/mes/nonconformities/{nc['id']}/disposition",{'decision':'REWORK','reason':'按键装配检验超差，转返修工位复核并重新检验'},token=qc)
    if s['mode'] in ['ship','receipt']:
        root=bom['root_material_id'];line=next(r for r in api('/mes/line-stock',token=store) if r['work_order_id']==wo['id'] and r['material_id']==root and r['quantity']>0)
        doc=document('PRODUCTION_RECEIPT',wo['id'],fgwarehouse,[{'material_id':root,'lot_id':line['lot_id'],'location_id':fgLocation,'quantity':s['qty']}],'产线终检合格，成品入库');post_document(doc)
        if s['mode']=='ship':
            doc=document('SALES_SHIPMENT',order['id'],fgwarehouse,[{'material_id':root,'lot_id':line['lot_id'],'location_id':fgLocation,'quantity':40}],'客户订单首批发货');post_document(doc,shipping)
    entry=next(r for r in report['created'] if r.get('work')==wo['id']);entry['done']=True
    log(f"已完成 {s['label']}：{s['qty']} 件，场景 {s['mode']}")

dashboard=api('/dashboard/production?date='+dt.date.today().isoformat()+'&shift=all&line=all')
assert len(dashboard['workOrders'])>=len(scenarios)
assert next(k['value'] for k in dashboard['kpis'] if k['key']=='done')>0
assert sum(t['passed']+t['failed'] for t in dashboard['qualityTrend'])>0
report['dashboard']=dashboard;report['complete']=True;checkpoint()
log('实际看板接口验证通过：生产计划、关键工序放行、工单状态、质检与库存均已有真实关联记录')
print('REPORT',out/'report.json',flush=True)

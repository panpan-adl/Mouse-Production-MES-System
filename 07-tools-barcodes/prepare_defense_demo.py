"""Prepare a draft-only A100 defense batch; never post receipts or execute production."""
import datetime as dt
import json
import subprocess
import urllib.request
from collections import defaultdict
from decimal import Decimal
from pathlib import Path
from stage1_database import ROOT, sql, config, env


def main():
    database = json.loads((ROOT/'.runtime/four-tier-20261030/state.json').read_text())['database']
    assert database.startswith('mouse_mes_four_tier_')
    backup = ROOT/'.runtime/backups'/f'live_demo_before_{dt.datetime.now():%Y%m%d_%H%M%S}.sql'
    with backup.open('wb') as out:
        r = subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'), '-h127.0.0.1', '-uroot',
            '--single-transaction', '--default-character-set=utf8mb4', database], env=env, stdout=out, stderr=subprocess.PIPE)
    assert r.returncode == 0 and backup.stat().st_size > 1000
    token = None
    def api(path, body=None, method=None):
        req = urllib.request.Request('http://127.0.0.1:5175/api'+path,
            data=json.dumps(body, ensure_ascii=False).encode() if body is not None else None,
            method=method or ('POST' if body is not None else 'GET'),
            headers={'Content-Type':'application/json', **({'Authorization':'Bearer '+token} if token else {})})
        with urllib.request.urlopen(req, timeout=30) as response:
            result = json.load(response)
        assert result.get('code',200)==200, (path,result)
        return result.get('data', result)
    token = api('/auth/login', {'userCode':'109001','password':'123456'})['token']
    get = lambda k: api('/foundation/'+k)
    bom = next(b for b in get('bom-versions') if b['code']=='BOM-A100-V1')
    product = next(p for p in get('products') if p['id']==bom['product_id'])
    route = next(r for r in get('routes') if r['code']=='ROUTE-A100-V1')
    customer = next(c for c in api('/customer/list') if c['customerName']=='深圳星河数码' and str(c['status']) in ('1','ACTIVE'))
    supplier = next(s for s in get('suppliers') if '诚品' in s['supplier_name'])
    warehouse = next(w for w in get('warehouses') if w['warehouse_code']=='WH-A100-RM')
    location = next(l for l in api('/fulfillment/locations') if l['warehouse_id']==warehouse['id'])
    materials = {m['id']:m for m in get('materials')}
    lines = [l for l in get('bom-lines') if l['bom_id']==bom['id']]
    requirements = defaultdict(Decimal)
    def expand(node, quantity, active):
        assert node not in active, 'Circular BOM'
        children = [l for l in lines if l['parent_material_id']==node]
        if not children:
            requirements[node] += quantity
        for line in children:
            expand(line['material_id'], quantity*Decimal(str(line['quantity'])), active|{node})
    expand(bom['root_material_id'], Decimal(10), set())
    prices = {'PCB':8.6,'微动':0.65,'编码器':1.25,'USB':2.4,'上盖':1.8,'下盖':1.6,
              '滚轮':0.78,'透镜':0.42,'螺钉':0.035,'脚贴':0.26,'铭牌':0.12,'包装盒':0.95}
    items = []
    manifest_lines = []
    for material_id, quantity in sorted(requirements.items()):
        m = materials[material_id]
        price = next(v for k,v in prices.items() if k in m['material_name'])
        # The purchase schema stores unit prices with two decimal places.
        price = round(price, 2)
        items.append(dict(materialId=material_id,quantity=float(quantity),unitPrice=price,amount=round(float(quantity)*price,2)))
        manifest_lines.append(dict(id=material_id,code=m['material_code'],name=m['material_name'],
            unit=m['unit'],quantity=float(quantity),unit_price=price,lot=f'DEF-20261101-A100-{material_id:03}'))
    order_no = 'SO-20261101-DEF01'
    order = next((o for o in api('/orders') if o['orderNo']==order_no), None)
    if order is None:
        order = api('/orders', dict(orderNo=order_no, customerName=customer['customerName'],
            customerPhone=customer['phone'],productName=product['product_name'],quantity=10,
            unitPrice=89,totalAmount=890,orderDate='2026-11-01',deliveryDate='2026-11-03',priority=2,
            remark='办公设备配套采购，10只A100有线鼠标，同一订单关联生产及发货'))
    po_no = 'PO-20261101-DEF01'
    purchases = api('/fulfillment/purchases')
    purchase = next((p for p in purchases if p['po_no']==po_no), None)
    if purchase is None:
        purchase = api('/purchase-order/add', dict(poNo=po_no,supplierId=supplier['id'],status='DRAFT',
            orderDate='2026-11-01',expectedDate='2026-11-01',remark='A100整机10套配套物料，逐批检验后入库',items=items))
    purchase_id = purchase['id']
    doc = {'code':'演示时新建','status':'尚未创建'}
    # Reserve an actual staffed workstation for today's live execution.
    worker = next(u for u in api('/mes/assignable-people?role=3') if u['id']==4)
    station = next(s for s in get('stations') if s['id']==20)
    team = next(t for t in get('teams') if t['name']=='A100订单交付演示班组')
    schedule = next(s for s in get('schedules') if s['team_id']==team['id'] and str(s['start_at']).startswith('2026-11-01'))
    result = dict(database=database,backup=str(backup),sales_order=order_no,sales_order_id=order['id'],
        purchase_order=po_no,purchase_order_id=purchase_id,receipt=doc['code'],receipt_status=doc['status'],
        product=product['product_name'],quantity=10,amount=890,customer=customer['customerName'],
        contact=customer['contactPerson'],supplier=supplier['supplier_name'],warehouse=warehouse['warehouse_name'],
        location=location.get('location_name',location.get('name')),bom=bom['code'],route=route['code'],materials=manifest_lines,
        station=station['name'],station_id=station['id'],team=team['name'],schedule_id=schedule['id'],
        worker='103001 赵宇航',schedule_start=str(schedule['start_at']),schedule_end=str(schedule['end_at']))
    (ROOT/'.runtime/defense-demo-batch.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    result['url']='http://127.0.0.1:5175/login'
    result['qc_templates']=[t for t in get('qc-templates') if t['product_id']==product['id']]
    result['qc_items']=get('qc-items')
    result['qc_lines']=get('qc-lines')
    (ROOT/'.runtime/defense-demo-batch.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps({k:result[k] for k in ['url','sales_order','purchase_order','receipt_status','station']},ensure_ascii=False))


if __name__=='__main__':
    main()

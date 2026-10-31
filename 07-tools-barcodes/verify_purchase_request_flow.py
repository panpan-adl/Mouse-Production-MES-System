"""Exercise provenance and atomic conversion only in the recorded disposable four-tier DB."""
import concurrent.futures
import datetime
import json
import urllib.request
from decimal import Decimal
from stage1_database import ROOT, sql

state = json.loads((ROOT/'.runtime/four-tier-20261030/state.json').read_text())
database = state['database']
assert database.startswith('mouse_mes_four_tier_')
base = 'http://127.0.0.1:8093/api'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
token = None

def call(path, body=None, method=None, success=True):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(base+path, headers=headers,
        data=None if body is None else json.dumps(body).encode(), method=method or ('GET' if body is None else 'POST'))
    with opener.open(request, timeout=20) as response:
        result = json.load(response)
    if success:
        assert result['code'] == 200, (path, result)
        return result['data']
    assert result['code'] != 200, (path, result)
    return result

def scalar(query):
    return Decimal(sql(query, database).strip().splitlines()[-1])

token = call('/auth/login', {'userCode': '109001', 'password': '123456'})['token']
task, material = map(int, sql('SELECT t.id,m.material_id FROM t_production_task t JOIN mm_task_context c ON c.task_id=t.id JOIN mm_work_material m ON m.work_order_id=c.work_order_id WHERE t.task_status<4 LIMIT 1', database).strip().splitlines()[-1].split())
stock = scalar(f'SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE material_id={material}')
source = call('/material-requests', {'sourceTaskId':task,'materialId':material,'requiredQuantity':float(stock+100),
    'requiredDate':str(datetime.date.today()), 'remark':'采购流程关联与重试验收'})
assert source['requestStatus'] == 'SHORTAGE'
endpoint = f"/material-requests/{source['id']}"
supplier = next(s for s in call('/suppliers') if s.get('status') == 1)
body = {'supplierId':supplier['id'],'items':[{'unitPrice':2}]}
before = scalar('SELECT COUNT(*) FROM t_purchase_order')
call(endpoint+'/purchase-order', body, success=False)
call(endpoint+'/purchase-ordered', {}, 'PUT', success=False)
assert scalar('SELECT COUNT(*) FROM t_purchase_order') == before
sent = call(endpoint+'/request-purchase', {}, 'PUT')
assert sent['requestStatus'] == 'PURCHASE_REQUESTED'
call(endpoint+'/purchase-order', {**body,'supplierId':-1}, success=False)
assert scalar('SELECT COUNT(*) FROM t_purchase_order') == before
assert call(endpoint)['requestStatus'] == 'PURCHASE_REQUESTED'
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
    orders = list(executor.map(lambda _: call(endpoint+'/purchase-order', body), range(2)))
assert orders[0]['id'] == orders[1]['id']
assert scalar('SELECT COUNT(*) FROM t_purchase_order') == before+1
linked = call(endpoint)
assert linked['purchaseOrderId'] == orders[0]['id']
assert linked['purchaseOrderNo'] == orders[0]['poNo']
assert linked['requestStatus'] == 'PURCHASE_ORDERED'
assert orders[0]['items'][0]['quantity'] == sent['shortageQuantity']
call(endpoint+'/request-purchase', {}, 'PUT', success=False)
assert scalar(f'SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE material_id={material}') == stock
result = dict(database=database, request=linked, order=orders[0], passed=True,
    checks=['unsent blocked','manual status blocked','invalid supplier rolls back','sent before ordering',
            'concurrent retries one order','explicit source link','server calculates quantity','no inventory change','ordered cannot resend'])
(ROOT/'.runtime/purchase-flow-verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS: 9 purchase flow checks; ' + source['requestNo'] + ' -> ' + orders[0]['poNo'])

"""Create fictional November material supply through approval, inspection and posting APIs."""
import json
import urllib.request
from realistic_business_data import quote
from stage1_database import ROOT, sql


def main():
    database = json.loads((ROOT / '.runtime/four-tier-20261030/state.json').read_text())['database']
    assert database.startswith('mouse_mes_four_tier_')
    token = None

    def api(path, body=None, method=None):
        req = urllib.request.Request('http://127.0.0.1:8093/api' + path,
            data=json.dumps(body, ensure_ascii=False).encode() if body is not None else None,
            method=method or ('POST' if body is not None else 'GET'),
            headers={'Content-Type': 'application/json', **({'Authorization': 'Bearer '+token} if token else {})})
        with urllib.request.urlopen(req, timeout=30) as response:
            result = json.load(response)
        if result.get('code', 200) != 200:
            raise RuntimeError(path + ': ' + str(result.get('msg', result)))
        return result.get('data', result)

    token = api('/auth/login', {'userCode': '109001', 'password': '123456'})['token']
    get = lambda key: api('/foundation/' + key)
    bom = next(b for b in get('bom-versions') if b['code'] == 'BOM-A100-V1')
    materials = {m['id']: m for m in get('materials')}
    raw = {l['material_id'] for l in get('bom-lines') if l['bom_id'] == bom['id'] and int(materials[l['material_id']]['material_type']) == 1}
    warehouse = next(w['id'] for w in get('warehouses') if w['warehouse_code'] == 'WH-A100-RM')
    location = next(l['id'] for l in api('/fulfillment/locations') if l['warehouse_id'] == warehouse)
    template = next(t for t in get('qc-templates') if t['kind'] == 'INCOMING' and t['product_id'] == bom['product_id'])
    item_ids = [l['item_id'] for l in get('qc-lines') if l['template_id'] == template['id']]
    qc_items = {r['id']: r for r in get('qc-items')}
    suppliers = get('suppliers')
    prices = {'PCB': 8.6, '微动': 0.65, '编码器': 1.25, 'USB': 2.4, '上盖': 1.8, '下盖': 1.6,
              '滚轮': 0.78, '透镜': 0.42, '螺钉': 0.035, '脚贴': 0.26, '铭牌': 0.12, '包装盒': 0.95}
    groups = [([], '恒信', '塑胶结构件及五金配套'), ([], '瑞芯', 'PCBA及电子线材配套'), ([], '嘉和', '包装材料及标识配套')]
    for material_id in sorted(raw):
        name = materials[material_id]['material_name']
        group = 2 if any(s in name for s in ['脚贴', '铭牌', '包装盒']) else (0 if any(s in name for s in ['上盖', '下盖', '滚轮组件', '透镜', '螺钉']) else 1)
        quantity = 2400 if '螺钉' in name else (1200 if '微动' in name else 600)
        price = next((v for k, v in prices.items() if k in name), 1.0)
        groups[group][0].append(dict(materialId=material_id, quantity=quantity, unitPrice=price, amount=round(quantity*price, 2)))
    evidence = []
    for i, (items, supplier_keyword, label) in enumerate(groups, 1):
        order_no = f'PO-20261031-NOV{i:02}'
        existing = sql('SELECT id,status FROM t_purchase_order WHERE po_no='+quote(order_no), database).splitlines()[1:]
        existing = [l for l in existing if l.strip()]
        if existing:
            purchase_id, status = existing[0].split('\t')
            purchase_id = int(purchase_id)
        else:
            supplier = next(s for s in suppliers if supplier_keyword in s['supplier_name'])
            purchase = api('/purchase-order/add', {'poNo': order_no, 'supplierId': supplier['id'],
                'status': 'DRAFT', 'orderDate': '2026-10-31', 'expectedDate': '2026-11-03',
                'remark': '11月首周A100产线备料·'+label+'，按当前图纸版本供货', 'items': items})
            purchase_id, status = purchase['id'], 'DRAFT'
        if status == 'DRAFT':
            api(f'/purchase-order/{purchase_id}/submit', {}, 'PUT')
            status = 'SUBMITTED'
        if status not in ('APPROVED', 'COMPLETED', 'PARTIAL_RECEIVED'):
            api(f'/purchase-order/{purchase_id}/approve', {}, 'PUT')
        documents = api('/mes/documents')
        document = next((d for d in documents if d['kind'] == 'PURCHASE_RECEIPT' and d['source_id'] == purchase_id), None)
        if document is None:
            document_id = api('/mes/documents', {'kind': 'PURCHASE_RECEIPT', 'source_id': purchase_id,
                'warehouse_id': warehouse, 'name': '11月首周备料收货·'+label,
                'reason': '按采购订单收货，核对外观、规格、数量与供应商批次后检验入库',
                'lines': [dict(material_id=r['materialId'], quantity=r['quantity'],
                    lot_no=f'LOT-20261031-S{i:02}-{r["materialId"]:03}', location_id=location) for r in items]})
            document = dict(id=document_id, status='DRAFT')
        document_id = document['id']
        if document['status'] != 'POSTED':
            if document['status'] == 'DRAFT':
                api(f'/mes/documents/{document_id}/confirm', {})
            for line in api(f'/mes/documents/{document_id}/lines'):
                results = {}
                for item_id in item_ids:
                    item = qc_items[item_id]
                    actual = '外观无破损，规格与采购单及图纸一致，包装标识清晰'
                    if item['data_type'] == 'NUMBER':
                        actual = (float(item.get('lower_limit') or 0)+float(item.get('upper_limit') or 10))/2
                    results[str(item_id)] = dict(actual=actual, result='PASS')
                api(f'/mes/document-lines/{line["id"]}/inspect', {'template_id': template['id'],
                    'qualified_quantity': line['quantity'], 'results': results})
            api(f'/mes/documents/{document_id}/post', {})
        evidence.append(dict(purchase_order=order_no, document_id=document_id,
                             material_types=len(items), total_quantity=sum(x['quantity'] for x in items)))
        (ROOT / '.runtime/realistic-supply-report.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding='utf8')
    print(json.dumps(evidence, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()

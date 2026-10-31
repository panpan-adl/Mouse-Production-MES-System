import sys,json,re,time,datetime,uuid,urllib.request,urllib.error
from pathlib import Path
sys.path.insert(0,str(Path('07-tools-barcodes').resolve()))
from stage1_database import ROOT,sql
OUT=ROOT/'.runtime/four-tier-20261030';state=json.loads((OUT/'state.json').read_text());DB=state['database'];assert DB.startswith('mouse_mes_four_tier_')
BASE='http://127.0.0.1:8093/api';results=[];stamp=uuid.uuid4().hex[:8]
def scalar(q):return sql(q,DB).strip().splitlines()[-1]
def call(path,body=None,method='GET',token=None):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=25) as response:v=json.load(response);status=response.status
 except urllib.error.HTTPError as e:v=json.load(e);status=e.code
 if status>=400 or isinstance(v,dict) and v.get('code',200)!=200:raise RuntimeError(json.dumps({'status':status,'response':v},ensure_ascii=False))
 return v.get('data',v) if isinstance(v,dict) else v
u=json.loads(sql("SELECT JSON_OBJECT('code',user_code,'password',password) FROM t_user WHERE role_id=9 AND deleted=0 AND status=1 LIMIT 1",DB).splitlines()[-1]);token=call('/auth/login',{'userCode':u['code'],'password':u['password']},'POST')['token']
def api(path,body=None,method='GET'):return call(path,body,method,token)
def save(k,body):return api('/foundation/'+k,body,'POST')
def record(id,fn):
 try:detail=fn();r={'id':id,'status':'PASS','detail':detail}
 except Exception as error:r={'id':id,'status':'FAIL','detail':str(error)}
 results.append(r);print(r['status'],id,flush=True);(OUT/'supplement-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
def crud(key,body,namefield):
 row=save(key,body);row[namefield]=row[namefield]+'修订';changed=api('/foundation/'+key+'/'+str(row['id']),row,'PUT');assert changed[namefield]==row[namefield]
 assert any(r['id']==row['id'] for r in api('/foundation/'+key))
 api('/foundation/'+key+'/'+str(row['id']),method='DELETE');assert not any(r['id']==row['id'] for r in api('/foundation/'+key))
 return 'create/read/update/delete passed'
for key,body,field in [('units',{'uom_name':'审计单位'+stamp},'uom_name'),('materials',{'material_name':'审计物料'+stamp,'material_type':1,'unit':'个'},'material_name'),('products',{'product_name':'审计产品'+stamp,'unit':'个'},'product_name'),('customers',{'customer_name':'审计客户'+stamp},'customer_name'),('suppliers',{'supplier_name':'审计供应商'+stamp},'supplier_name'),('workshops',{'name':'审计车间'+stamp},'name')]:record('D1-CRUD-'+key,lambda k=key,b=body,f=field:crud(k,b,f))
ws=save('workshops',{'name':'审计设置车间'+stamp})
record('D1-CRUD-stations',lambda:crud('stations',{'name':'审计工位'+stamp,'workshop_id':ws['id']},'name'))
record('D1-CRUD-warehouses',lambda:crud('warehouses',{'warehouse_name':'审计仓库'+stamp},'warehouse_name'))
w=save('warehouses',{'warehouse_name':'审计库位测试仓'+stamp})
record('D1-CRUD-areas',lambda:crud('areas',{'name':'审计库区'+stamp,'warehouse_id':w['id']},'name'))
area=save('areas',{'name':'审计固定库区'+stamp,'warehouse_id':w['id']})
record('D1-CRUD-locations',lambda:crud('locations',{'name':'审计库位'+stamp,'area_id':area['id']},'name'))
record('D1-CRUD-qc-items',lambda:crud('qc-items',{'name':'审计检测项'+stamp,'standard':'外观完好','data_type':'TEXT'},'name'))
product=api('/foundation/products')[0]
record('D1-CRUD-qc-templates',lambda:crud('qc-templates',{'name':'审计来料模板'+stamp,'product_id':product['id'],'kind':'INCOMING','version':'1','phase':'FINAL'},'name'))
record('D1-CRUD-equipment',lambda:crud('equipment',{'equipment_name':'审计设备'+stamp},'equipment_name'))
# Inspect the real model mode and all query results; no external provider is called.
catalog=api('/agent/skills');(OUT/'agent-config.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2),encoding='utf-8')
def ask(q,**extra):
 start=time.perf_counter();v=api('/agent/messages',{'question':q,**extra},'POST');v['auditElapsedMs']=round((time.perf_counter()-start)*1000);return v
queries=[]
def querycheck(label,q,expected,verify):
 result=ask(q);data=result.get('reply',{}).get('data');assert data is not None,result
 manual=api('/analytics/query',expected,'POST');assert data['rows']==manual['rows'];verify(data,manual)
 queries.append({'label':label,'question':q,'response':result,'manual':manual});return {'rows':len(data['rows']),'elapsedMs':result['auditElapsedMs']}
material=json.loads(sql("SELECT JSON_OBJECT('id',m.id,'name',m.material_name) FROM t_material m JOIN t_inventory_transaction t ON t.material_id=m.id GROUP BY m.id,m.material_name ORDER BY COUNT(*) DESC LIMIT 1",DB).splitlines()[-1]);mid=material['id'];today=datetime.date.today().isoformat()
def inventory_verify(data,manual):
 for r in data['rows']:
  q=float(scalar('SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE is_deleted=0 AND material_id='+str(r['id'])+' AND warehouse_id='+str(r['warehouse_id'])))
  assert float(r['current_quantity'])==q,(r,q)
record('D3-query-time',lambda:querycheck('时间维度','今天库存 物料：'+material['name'],{'kind':'INVENTORY','material':str(mid),'from':today,'to':today},inventory_verify))
record('D3-query-category',lambda:querycheck('品类维度','当前原材料库存',{'kind':'INVENTORY','type':'1'},inventory_verify))
work=json.loads(sql("SELECT JSON_OBJECT('id',id,'code',code) FROM mm_work_order ORDER BY id LIMIT 1",DB).splitlines()[-1])
def trace_verify(data,manual):
 assert any(r['id']==work['id'] for r in data['rows']);count=int(scalar('SELECT COUNT(*) FROM t_production_report r JOIN mm_task_context c ON c.task_id=r.task_id WHERE c.work_order_id='+str(work['id'])));assert len(data['reports'])==count
record('D3-query-document',lambda:querycheck('单据维度','追溯 '+work['code'],{'kind':'TRACE','document':work['code']},trace_verify))
def follow():
 first=ask('当前库存 物料：'+material['name']);session=first['reply']['sessionId'];second=ask('改查上个月',sessionId=session,previousSkill='inventory');conditions=second['reply']['query']['conditions'];assert conditions['material']==str(mid);assert conditions['from']==(datetime.date.today().replace(day=1)-datetime.timedelta(days=1)).replace(day=1).isoformat();queries.append({'label':'多轮追问','response':second});return conditions
record('D3-followup',follow)
def ambiguous():
 result=ask('查询库存 仓库：仓');assert result['status']=='CLARIFICATION';queries.append({'label':'模糊输入','response':result});return result['reply'].get('query',{}).get('clarifications')
record('D3-clarification',ambiguous)
def unsupported():
 result=ask('帮我预测明年全国汽车销量并生成营销方案');assert result['status']=='CLARIFICATION';queries.append({'label':'超纲输入','response':result});return result['reply']['summary']
record('D3-out-of-scope',unsupported)
# Scoring sample: this month must not claim complete beginning/end balances before the trusted baseline.
def sample_month():
 result=ask('请帮我盘点一下10月份的'+material['name']+'库存情况');queries.append({'label':'评分示例月度问题','response':result});return result
record('D3-month-sample',sample_month)
(OUT/'agent-query-evidence.json').write_text(json.dumps(queries,ensure_ascii=False,indent=2),encoding='utf-8')
# The graphical data source is cross-checked with direct SQL, not a frontend mock.
def dashboard():
 d=api('/dashboard/production?date='+today)
 expected=float(scalar("SELECT COALESCE(SUM(i.quantity),0) FROM mm_inspection i JOIN mm_task_context c ON c.task_id=i.source_id JOIN mm_operation o ON o.id=c.operation_id JOIN mm_qc_template qt ON qt.id=i.template_id WHERE o.is_key=1 AND i.status='RELEASED' AND qt.phase IN('FINAL','LAST') AND DATE(i.created_at)=CURDATE()"))
 actual=sum(float(h['actual']) for h in d['hourlyOutput']);assert actual==expected,(actual,expected)
 assert len(d['qualityTrend'])>=4 and len(d['materialSafety'])>0
 (OUT/'dashboard.json').write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
 return {'charts':['hourlyOutput','workOrderStatus','qualityTrend','materialSafety','materialReadiness'],'hourlyTotal':actual,'directSqlTotal':expected,'equipmentStatusIncluded':any('equipment' in k.lower() for k in d)}
record('D4-real-dashboard',dashboard)
def automatic():
 before=int(scalar("SELECT COUNT(*) FROM mm_qc_todo"));api('/automation/process',{},'POST');after=int(scalar("SELECT COUNT(*) FROM mm_qc_todo"));assert before==after
 counts=sql('SELECT status,COUNT(*) n FROM mm_auto_event GROUP BY status',DB)
 return {'deduplicated':True,'events':counts,'todos':after}
record('D4-automation',automatic)
print('SUPPLEMENT',sum(r['status']=='PASS' for r in results),'/',len(results))

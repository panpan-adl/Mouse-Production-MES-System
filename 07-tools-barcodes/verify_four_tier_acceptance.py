import ast,sys,json,datetime,time,uuid,urllib.request,urllib.error,concurrent.futures
from pathlib import Path
p=Path('.runtime/four-tier-20261030');s=(p/'suite/verify_mes_completion.py').read_text(encoding='utf-8');exec(compile(s.split("if scalar('SELECT COUNT(*) FROM t_user")[0],'helpers','exec'))
for f in ['verify_mes_completion.py','verify_mes_completion_extra.py']:
 for n in ast.parse((p/'suite'/f).read_text(encoding='utf-8')).body:
  if isinstance(n,ast.FunctionDef) and n.name in ['purchase','document','check_document','dispatch','inspect','procure_and_issue']:exec(compile(ast.Module(body=[n],type_ignores=[]),'workflow-helper','exec'))
checks=[]
def check(v,label):
 assert v,label
 checks.append(label);print('PASS',label,flush=True);(p/'four-tier-results.json').write_text(json.dumps({'database':DB,'passed':len(checks),'checks':checks},ensure_ascii=False,indent=2),encoding='utf-8')
users=[json.loads(l) for l in sql("SELECT JSON_OBJECT('id',id,'role',role_id,'code',user_code,'password',password) FROM t_user WHERE deleted=0 AND status=1",DB).splitlines()[1:]];actors={}
for role in [9,3,4,5,6]:
 u=next(u for u in users if u['role']==role);actors[role]=(u['id'],api('/auth/login',{'userCode':u['code'],'password':u['password']},'POST')['token'])
admin,worker,qc,store,buyer=[actors[r][1] for r in [9,3,4,5,6]]
bom=next(b for b in get('bom-versions') if b['code']=='BOM-A100-V1');route=next(r for r in get('routes') if r['code']=='ROUTE-A100-V1');warehouse=next(w['id'] for w in get('warehouses') if w['warehouse_code']=='WH-A100-RM');location=next(l['id'] for l in api('/fulfillment/locations',token=admin) if l['warehouse_id']==warehouse);supplier=get('suppliers')[0];incoming=next(t for t in get('qc-templates') if t['kind']=='INCOMING' and t['product_id']==bom['product_id']);allMaterials={m['id']:m for m in get('materials')};fixture=json.loads((p/'material-fixtures.json').read_text(encoding='utf-8'))['materials'][0];material=fixture['id']
area=next(a for a in get('areas') if a['warehouse_id']==warehouse);loc=save('locations',{'name':'四档启停验证'+stamp,'area_id':area['id']});physical=int(scalar("SELECT id FROM t_location WHERE warehouse_id="+str(warehouse)+" AND location_code='"+loc['code']+"'"));api('/foundation/locations/'+str(loc['id']),{**loc,'status':0},'PUT',admin)
check(all(l['id']!=physical for l in api('/fulfillment/locations',token=admin)),'停用库位从业务候选项移除')
api('/locations/'+str(physical),{'status':'1'},'PUT',admin);check(next(l for l in get('locations') if l['id']==loc['id'])['status']==1,'旧接口启用同步基础配置')
po=purchase([(material,3)],'DISABLED');doc=document('PURCHASE_RECEIPT',po['id'],warehouse,[{'material_id':material,'quantity':3,'lot_no':'FOUR-'+stamp,'location_id':physical}]);post(f'/mes/documents/{doc}/confirm',token=store);check_document(doc,incoming);api('/foundation/locations/'+str(loc['id']),{**loc,'status':0},'PUT',admin);before=scalar('SELECT COUNT(*) FROM t_inventory_transaction');post(f'/mes/documents/{doc}/post',token=store,ok=False);check(scalar('SELECT COUNT(*) FROM t_inventory_transaction')==before,'草稿确认后停用库位仍阻止过账，失败无库存流水')
api('/foundation/locations/'+str(loc['id']),{**loc,'status':1},'PUT',admin);post(f'/mes/documents/{doc}/post',token=store);check(True,'重新启用库位后原单可正常过账')
api('/foundation/locations/'+str(loc['id']),method='DELETE',token=admin,ok=False);api('/locations/'+str(physical),method='DELETE',token=admin,ok=False);check(True,'已有单据及库存的库位两套接口均禁止删除')
# Month balances for real newly-created materials are reconstructed without backdating a baseline.
now=datetime.date.today();start=now.replace(day=1).isoformat();today=now.isoformat();manual=post('/analytics/query',{'kind':'INVENTORY','material':str(material),'from':start,'to':today})
for r in manual['rows']:
 actual=float(scalar('SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE is_deleted=0 AND material_id='+str(material)+' AND warehouse_id='+str(r['warehouse_id'])))
 check(r['opening_quantity']==0 and r['closing_quantity'] is not None and float(r['closing_quantity'])==actual and float(r['in_quantity'])-float(r['out_quantity'])==actual,'整月收发存逐仓核对 '+r['warehouse_name'])
questions=['请帮我盘点一下'+str(now.month)+'月份的'+fixture['material_name']+'库存情况','看看'+str(now.month)+'月'+fixture['material_name']+'结存','查询本月物料：'+fixture['material_name']+'的出入库']
answers=[]
for question in questions:
 response=post('/agent/messages',{'question':question});check(response['status']=='SUCCEEDED' and response['reply']['data']['rows']==manual['rows'],'随机措辞与人工同口径：'+question);answers.append(response)
follow=post('/agent/messages',{'question':'只看A100原料仓','sessionId':answers[0]['reply']['sessionId'],'previousSkill':'inventory'});conditions=follow['reply']['query']['conditions'];check(conditions['warehouse']==str(warehouse) and conditions['material']==str(material) and conditions['from']==start,'多轮只改仓库，保留物料与月份')
old=next(m for m in get('materials') if not m['material_name'].startswith('验收') and m['material_name']=='鼠标上盖');unknown=post('/analytics/query',{'kind':'INVENTORY','material':str(old['id']),'from':start,'to':today});check(all(r['opening_quantity'] is None for r in unknown['rows']),'旧资料无完整历史时仍明确无法核实，不制造期初')
(p/'monthly-evidence.json').write_text(json.dumps({'manual':manual,'agent':answers,'followup':follow,'legacy':unknown},ensure_ascii=False,indent=2),encoding='utf-8')
# Equipment chart changes with actual equipment state and matches SQL.
equipment=save('equipment',{'equipment_name':'四档状态验证设备'+stamp});dash=api('/dashboard/production?date='+today,token=admin);check(sum(r['value'] for r in dash['equipmentStatus'])==int(scalar('SELECT COUNT(*) FROM t_equipment WHERE is_deleted=0')),'设备状态图台数与真实台账一致')
api('/equipments/'+str(equipment['id']),{'status':'REPAIR_REQUESTED'},'PUT',admin);updated=api('/dashboard/production?date='+today,token=admin);check(next(e for e in updated['equipmentDetails'] if e['id']==equipment['id'])['availability']=='待处理','修改设备状态后看板实际更新');api('/equipments/'+str(equipment['id']),{'status':'NORMAL'},'PUT',admin)
check(len(updated['hourlyOutput'])>0 and len(updated['workOrderStatus'])>0 and len(updated['materialSafety'])>0,'四类必需主题均有真实业务数据')
# Actual manual and background workflows; no invented human performance claim.
station=next(s for s in get('stations') if s['name']=='隔离生产工作站');numeric=next(i for i in get('qc-items') if i['name']=='数值检验');templates={};numeric_templates={l['template_id'] for l in get('qc-lines') if l['item_id']==numeric['id']}
for template in get('qc-templates'):
 if template['kind']=='PROCESS' and template['phase']=='FINAL' and template['id'] in numeric_templates and template.get('operation_id') and template['operation_id'] not in templates:templates[template['operation_id']]=template;allMaterials={m['id']:m for m in get('materials')}
def new_report():
 w=post('/mes/work-orders',{'product_id':bom['product_id'],'quantity':1,'bom_id':bom['id'],'route_id':route['id'],'owner_id':actors[3][0]});procure_and_issue(w['id']);t=api(f'/foundation/work-orders/{w["id"]}/tasks',token=admin)[0];dispatch(t);api(f'/production-tasks/{t["id"]}/start',{},'PUT',worker);post(f'/production-tasks/{t["id"]}/report',{'reportQuantity':1},worker);return w,t
post('/automation/rule',{'enabled':False});w,t=new_report();before=int(scalar('SELECT COUNT(*) FROM mm_qc_todo'));post(f'/automation/comparisons/{t["id"]}/complete',token=qc,ok=False);post(f'/automation/comparisons/{t["id"]}/start',token=qc);post(f'/automation/comparisons/{t["id"]}/complete',token=worker,ok=False);post(f'/automation/comparisons/{t["id"]}/complete',token=qc);post(f'/automation/comparisons/{t["id"]}/complete',token=qc);check(int(scalar('SELECT COUNT(*) FROM mm_qc_todo'))==before+1,'人工对比创建真实待办、鉴权与重复提交保护');post('/foundation/inspections',inspect(t),qc)
post('/automation/rule',{'enabled':True});w,t=new_report();began=time.monotonic()
while time.monotonic()-began<12:
 state=api('/automation',token=admin)
 if any(q['task_id']==t['id'] for q in state['todos']):break
 time.sleep(.4)
check(any(q['task_id']==t['id'] for q in state['todos']),'完整报工后后台自动生成待办，未手工处理队列');count=int(scalar('SELECT COUNT(*) FROM mm_qc_todo'));post('/automation/process');check(int(scalar('SELECT COUNT(*) FROM mm_qc_todo'))==count,'自动化重复触发不重复建待办');post('/foundation/inspections',inspect(t),qc);check(next(q for q in api('/automation',token=admin)['todos'] if q['task_id']==t['id'])['status']=='DONE','自动生成待办经独立检验关闭，完整链路通过')
comparison=api('/automation/comparisons',token=admin);check(any(r['finished_at'] and float(r['elapsed_seconds'])>=0 for r in comparison['manual']) and any(r['task_id']==t['id'] for r in comparison['automatic']),'前后对比读取真实操作时间戳与关联单据')
unknown_name=post('/agent/messages',{'question':'请帮我盘点一下10月份的不存在评分物料库存情况'});check(unknown_name['status']=='CLARIFICATION' and 'data' not in unknown_name['reply'],'不存在物料应澄清，不能偷换为全部库存')
(p/'comparison-evidence.json').write_text(json.dumps(comparison,ensure_ascii=False,indent=2),encoding='utf-8');(p/'dashboard-evidence.json').write_text(json.dumps(updated,ensure_ascii=False,indent=2),encoding='utf-8');print('FOUR-TIER SPECIAL CHECKS:',len(checks))

from demo_api import api,login
from pathlib import Path
import json,datetime
admin=login();maint=login('108001')
def call(p,b=None,m='GET',token=admin):return api(p,b,m,token)
def get(k):return call('/foundation/'+k)
def save(k,b):return call('/foundation/'+k,b,'POST')
path=Path(__file__).with_name('equipment-showcase.json');state=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
def persist():path.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
owner=call('/mes/assignable-people?role=8')[0]
specs=[('DEMO-EQ-01','鼠标装配电批','电子装配站','EB-16','扭矩偏差，紧固后螺钉松动','FINISHED',85),('DEMO-EQ-02','壳体点胶机','结构装配站','GL-200','点胶针头堵塞，出胶量不稳定','PENDING',0),('DEMO-EQ-03','自动贴标机','贴标包装站','LB-100','标签定位传感器偏移，贴标偏斜','PROCESSING',0),('DEMO-EQ-04','USB功能检测仪','总装测试站','USB-T8','USB接口接触不良，检测偶发掉线','FINISHED',120),('DEMO-EQ-05','包装电子秤','包装复核区','SC-5K','周期校准与清洁','NORMAL',0)]
for code,name,location,model,desc,target,cost in specs:
 equipment=next((e for e in call('/equipments') if e['equipmentCode']==code),None)
 if not equipment:equipment=call('/equipments',{'equipmentCode':code,'equipmentName':name,'equipmentType':'鼠标制造辅助设备','model':model,'manufacturer':'课程演示设备供应商','purchaseDate':'2026-09-01','warrantyPeriod':12,'location':location,'responsibleId':owner['id'],'responsibleName':owner.get('realName','黄十二'),'status':'NORMAL','remark':'课程答辩可追溯设备演示'},'POST')
 state.setdefault(code,{'equipmentId':equipment['id'],'name':name});entry=state[code];persist()
 if target=='NORMAL':continue
 if code=='DEMO-EQ-01':
  plan=next((p for p in get('checks') if p['name']==name+' 扭矩日检'),None)
  if not plan:
   plan=save('checks',{'name':name+' 扭矩日检','code':'CHECK-'+code,'equipment_id':equipment['id'],'interval_days':1,'standard':'设定扭矩0.6N·m，允许偏差±0.05N·m','owner_id':owner['id'],'enabled':1,'plan_type':'CHECK'})
   subject=save('check-subjects',{'code':'SUB-'+code,'name':'电批扭矩及螺钉紧固检查','kind':'CHECK','standard':'0.55–0.65N·m，试拧无松动'});save('check-plan-subjects',{'plan_id':plan['id'],'subject_id':subject['id']})
  entry['checkPlanId']=plan['id'];persist()
  rec=next((r for r in get('check-records') if r['plan_id']==plan['id']),None)
  if not rec:rec=save('check-records',{'plan_id':plan['id'],'checked_at':datetime.datetime.now().isoformat(),'actual_value':'实测0.48N·m，低于标准下限','result':'FAIL','remark':'课程演示：异常点检自动关联报修'})
  repair=call('/maintenances/'+str(rec['maintenance_id']));entry['checkRecordId']=rec['id']
 else:
  repair=next((m for m in call('/maintenances') if m['equipmentId']==equipment['id'] and m['maintenanceNo']=='DEMO-MR-'+code[-2:]),None)
  if not repair:repair=call('/maintenances',{'maintenanceNo':'DEMO-MR-'+code[-2:],'equipmentId':equipment['id'],'maintenanceType':2,'faultDesc':desc,'remark':'课程答辩：现场报修 → 派工 → 维修执行 → 完成记录'},'POST')
 entry['maintenanceId']=repair['id'];entry['target']=target;persist()
 if repair['status']=='FINISHED':continue
 if target=='PENDING':continue
 if not repair.get('maintenanceUserId'):repair=call(f"/maintenances/{repair['id']}/assign",{'userId':owner['id'],'userName':owner.get('realName','')},'PUT')
 if repair['status']=='PENDING':repair=call(f"/maintenances/{repair['id']}/start",{},'PUT',maint)
 if target=='FINISHED':call(f"/maintenances/{repair['id']}/finish",{'maintenanceContent':'清洁连接部位、校准参数并更换磨损件；按原检查标准复测，连续运行10次合格。','result':'REPAIRED','cost':cost,'scrapEquipment':False},'PUT',maint)
 print(name,target,flush=True)
# Normal check records and due periodic work linked to real plans.
for code in ['DEMO-EQ-04','DEMO-EQ-05']:
 entry=state[code];name=entry['name'];plan=next((p for p in get('checks') if p['name']==name+' 日常检查'),None)
 if not plan:
  plan=save('checks',{'code':'CK-'+code,'name':name+' 日常检查','equipment_id':entry['equipmentId'],'interval_days':1,'standard':'供电正常、校准合格、外观无损伤','owner_id':owner['id'],'enabled':1,'plan_type':'CHECK'})
  sub=save('check-subjects',{'code':'CS-'+code,'name':name+' 供电与精度','kind':'CHECK','standard':'标准件复测合格，显示稳定'});save('check-plan-subjects',{'plan_id':plan['id'],'subject_id':sub['id']})
 if not any(r['plan_id']==plan['id'] for r in get('check-records')):save('check-records',{'plan_id':plan['id'],'checked_at':datetime.datetime.now().isoformat(),'actual_value':'标准件复测3次，结果均符合标准','result':'PASS','remark':'课程演示：设备修复后/开班前复核'})
 pm=next((p for p in get('maintenance-plans') if p['name']==name+' 月度保养'),None)
 if not pm:pm=save('maintenance-plans',{'name':name+' 月度保养','equipment_id':entry['equipmentId'],'owner_id':owner['id'],'anchor_at':datetime.datetime.now().isoformat(),'frequency':1,'cycle_unit':'MONTH','remind_days':1,'content':'断电清洁、连接检查、标准件校准及试运行，填写实际结果和费用','enabled':1})
 entry['periodicPlanId']=pm['id'];persist()
call('/maintenances/periodic/generate',{},'POST')
tasks=call('/maintenances/periodic/tasks')
task=next(t for t in tasks if t['plan_id']==state['DEMO-EQ-04']['periodicPlanId']);repair=call('/maintenances/'+str(task['maintenance_id']))
if repair['status']=='PENDING':call(f"/maintenances/{repair['id']}/start",{},'PUT',maint)
if repair['status']!='FINISHED':call(f"/maintenances/{repair['id']}/finish",{'maintenanceContent':'完成接口清洁与标准件校准，连续试运行稳定，保养验收合格。','result':'REPAIRED','cost':35,'scrapEquipment':False},'PUT',maint)
state['periodicCompletedId']=repair['id'];persist();print('Equipment API workflows and check/periodic trace prepared',flush=True)

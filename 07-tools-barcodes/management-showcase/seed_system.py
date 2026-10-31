from demo_api import api,login
from pathlib import Path
import json,datetime
token=login()
def call(p,b=None,m='GET'):return api(p,b,m,token)
def get(k):return call('/foundation/'+k)
def save(k,b):return call('/foundation/'+k,b,'POST')
manifest=Path(__file__).with_name('system-showcase.json');state=json.loads(manifest.read_text(encoding='utf-8')) if manifest.exists() else {}
def persist():manifest.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
specs=[('109','经营管理部',[9]),('101','生产制造部',[1,2,3]),('104','质量管理部',[4]),('105','仓储物流部',[5]),('106','采购供应部',[6]),('107','销售业务部',[7]),('108','设备工程部',[8]),('110','财务管理部',[10]),('111','人力与系统管理部',[11])]
departments=call('/departments');byRole={}
for i,(code,name,roles) in enumerate(specs):
 d=next((d for d in departments if d['deptCode']==code),None)
 if not d:
  call('/departments',{'deptCode':code,'deptName':name,'parentId':0,'sort':i+1,'status':1},'POST')
  d=next(d for d in call('/departments') if d['deptCode']==code);departments.append(d)
 for role in roles:byRole[role]=d
users=call('/users');positions=get('positions');assignments=get('user-positions');roleNames={r['id']:r['name'] for r in get('roles')}
state.setdefault('employees',[])
state['employees']=[e for e in state['employees'] if e.get('departmentId')]
for u in users:
 if u.get('deleted')==1:continue
 d=byRole.get(u['roleId'])
 if not d:continue
 old=u['deptId']
 if not any(e['userId']==u['id'] for e in state['employees']):
  call('/users/'+str(u['id']),{'deptId':d['id']},'PUT')
  state['employees'].append({'userId':u['id'],'userCode':u['userCode'],'name':u['realName'],'roleId':u['roleId'],'role':roleNames.get(u['roleId']),'oldDepartmentId':old,'departmentId':d['id'],'department':d['deptName']});persist()
 pos=next((p for p in positions if p['name']==roleNames[u['roleId']] and p['department_id']==d['id']),None)
 if not pos:pos=save('positions',{'code':'POS-DEMO-'+str(u['roleId']),'name':roleNames[u['roleId']],'department_id':d['id']});positions.append(pos)
 if not any(a['user_id']==u['id'] and a['position_id']==pos['id'] for a in assignments):assignments.append(save('user-positions',{'user_id':u['id'],'position_id':pos['id']}))
for role,d in byRole.items():
 if role in roleNames and not any(p['name']==roleNames[role] and p['department_id']==d['id'] for p in positions):positions.append(save('positions',{'code':'POS-DEMO-'+str(role),'name':roleNames[role],'department_id':d['id']}))
# Assign each actual department head only after their staff department is saved.
for code,name,roles in specs:
 d=next(d for d in departments if d['deptCode']==code);leader=next((u for u in users if u['roleId']==roles[0] and u.get('status')==1),None)
 if leader:call('/departments/'+str(d['id']),{**d,'leaderId':leader['id'],'leaderName':leader['realName']},'PUT')
print('Departments, staff assignments and persisted history prepared',flush=True)
categories=get('categories')
def category(code,name,kind='MATERIAL',parent=None):
 c=next((c for c in categories if c['name']==name and c['kind']==kind),None)
 if not c:c=save('categories',{'code':code,'name':name,'kind':kind,'parent_id':parent});categories.append(c)
 elif c['kind']!=kind:c=call('/foundation/categories/'+str(c['id']),{**c,'kind':kind},'PUT')
 return c['id']
raw=category('CAT-RAW','原材料与零部件');electronic=category('CAT-ELEC','电子元器件',parent=raw);structure=category('CAT-STRUCT','结构与紧固件',parent=raw);pack=category('CAT-PACK','标签与包装耗材',parent=raw);semi=category('CAT-SEMI','装配半成品','PRODUCT')
finished=category('CAT-PRODUCT','鼠标产品','PRODUCT');office=category('CAT-OFFICE','有线办公鼠标','PRODUCT',finished);category('CAT-GAMING','游戏鼠标（产品规划）','PRODUCT',finished)
for m in get('materials'):
 name=m['material_name'];cid=office if m.get('material_type')==3 else semi if m.get('material_type')==2 else pack if any(w in name for w in ['包装','标签','铭牌','盒']) else electronic if any(w in name for w in ['PCB','主板','开关','编码','连接线','线缆','USB','传感','电阻']) else structure
 call('/foundation/materials/'+str(m['id']),{**m,'category_id':cid},'PUT')
for p in get('products'):call('/foundation/products/'+str(p['id']),{**p,'category_id':office},'PUT')
print('Business categories linked to current material and product records',flush=True)
menus=get('menus');grants=get('role-menus');known={(g['role_id'],g['menu_id']) for g in grants}
policy=json.loads(Path(__file__).with_name('menu-policy.json').read_text(encoding='utf-8'))
# Include real master-data pages; these retain the same pre-existing canVisit foundation rule.
for c in call('/foundation/catalog'):
 path='/foundation/'+c['key']
 if not any(p['path']==path for p in policy):policy.append({'path':path,'name':c['label'],'roles':list(range(1,12))})
for index,p in enumerate(policy):
 m=next((m for m in menus if m['path']==p['path']),None)
 if not m:m=save('menus',{'code':'MENU-DEMO-'+str(index+1),'name':p['name'],'path':p['path']});menus.append(m)
 # CEO already bypasses menu filtering; explicit records document that existing access.
 # Leave every other role untouched so this data setup cannot broaden their permissions.
 for role in [9]:
  if (role,m['id']) not in known:save('role-menus',{'role_id':role,'menu_id':m['id']});known.add((role,m['id']))
state.update(departments=[{'id':d['id'],'name':d['deptName'],'code':d['deptCode']} for d in departments],categoryCount=len(categories),positions=len(positions),employeePositions=len(assignments),menus=len(menus),menuGrants=len(known));persist()
for u in state['employees']:assert call('/users/'+str(u['userId'])+'/history'),'Missing persisted employee history'
print({k:v for k,v in state.items() if k not in ['employees','departments']},flush=True)

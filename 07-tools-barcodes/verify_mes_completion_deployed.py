"""Read-only post-deployment checks, apart from creating login sessions."""
from stage1_database import ROOT,sql
import json,urllib.request,urllib.error

def call(base,path,token=None,body=None,expect=200):
    req=urllib.request.Request(base+'/api'+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
    try:
        with urllib.request.urlopen(req,timeout=15) as r:status=r.status;value=json.load(r)
    except urllib.error.HTTPError as e:status=e.code;value=json.load(e)
    assert status==expect,(path,status)
    if expect==200:assert isinstance(value,list) or value.get('code',200)==200,(path,value)
    return value.get('data',value) if isinstance(value,dict) else value

state=json.loads((ROOT/'.runtime/mes-completion-database.json').read_text(encoding='utf-8'))
results=[]
for backend,frontend,database in [(8080,5173,'mouse_mes'),(8082,5174,state['database'])]:
    base=f'http://127.0.0.1:{backend}'
    user=json.loads(sql("SELECT JSON_OBJECT('code',user_code,'password',password) FROM t_user WHERE role_id=9 AND deleted=0 AND status=1 LIMIT 1",database).splitlines()[-1])
    token=call(base,'/auth/login',body={'userCode':user['code'],'password':user['password']})['token']
    for path in ['/mes/planning','/mes/reports','/mes/documents','/mes/nonconformities','/mes/packages','/mes/line-stock','/mes/calendar','/mes/checks','/mes/notices','/mes/alerts','/mes/cache','/mes/sessions','/mes/jobs','/mes/settings','/mes/menu-access','/mes/monitor','/foundation/catalog']:
        call(base,path,token)
    with urllib.request.urlopen(f'http://127.0.0.1:{frontend}/mes',timeout=10) as response:assert response.status==200
    call(base,'/mes/planning',expect=401)
    user=json.loads(sql("SELECT JSON_OBJECT('code',user_code,'password',password) FROM t_user WHERE role_id=3 AND deleted=0 AND status=1 LIMIT 1",database).splitlines()[-1])
    worker=call(base,'/auth/login',body={'userCode':user['code'],'password':user['password']})['token']
    call(base,'/users',worker,expect=403);call(base,'/finance/overview',worker,expect=403);call(base,'/mes/monitor',worker,expect=403)
    assert all('cost_price' not in p and 'selling_price' not in p for p in call(base,'/foundation/products',worker))
    results.append(dict(backend_port=backend,frontend_port=frontend,read_endpoints=17,permissions_verified=True,database=database))
(ROOT/'.runtime/mes-completion-deployment.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
print('Both deployed websites and 34 authenticated feature endpoints passed; worker permission boundaries verified.')

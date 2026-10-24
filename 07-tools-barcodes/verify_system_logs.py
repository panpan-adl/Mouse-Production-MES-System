"""Read-only system log acceptance against an isolated or deployed backend."""
from stage1_database import ROOT,sql
import json,urllib.request,urllib.error,datetime,uuid,sys
DB=json.loads((ROOT/'.runtime/mes-completion-database.json').read_text(encoding='utf-8'))['database'];assert DB.startswith('mouse_mes_completion_test_')
BASE='http://127.0.0.1:'+ (sys.argv[1] if len(sys.argv)>1 else '8085') + '/api'
checks=[]
def call(path,token=None,body=None,expect=200):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=15) as r:status=r.status;result=json.load(r)
 except urllib.error.HTTPError as e:status=e.code;result=json.load(e)
 assert status==expect,(path,status,result)
 if expect==200:assert isinstance(result,list) or result.get('code',200)==200
 return result.get('data',result) if isinstance(result,dict) else result

def login(role):
 user=json.loads(sql(f"SELECT JSON_OBJECT('code',user_code,'password',password) FROM t_user WHERE role_id={role} AND status=1 AND deleted=0 LIMIT 1",DB).splitlines()[-1])
 return call('/auth/login',body={'userCode':user['code'],'password':user['password']})['token']
def check(condition,name):assert condition,name;checks.append(name);print('PASS',name,flush=True)
admin=login(9);worker=login(3);system=login(11)
call('/mes/system-logs',expect=401);call('/mes/system-logs',worker,expect=403)
check(True,'Unauthenticated and ordinary-worker access is rejected by the server')
call('/mes/system-logs',system);check(True,'System administrator can read audit logs')
for kind in ['login','operation','audit']:
 first=call('/mes/system-logs?kind='+kind+'&size=7',admin)
 check(first['total']>0 and 0<len(first['items'])<=7,kind+' reads existing database records with bounded page size')
 keys=set().union(*(r.keys() for r in first['items']))
 check(not(keys&{'password','request_param','requestParam','response_result','responseResult','token','details'}),kind+' excludes raw request/response bodies and credential fields')
 second=call('/mes/system-logs?kind='+kind+'&size=7&page=2',admin)
 check(not({r['id'] for r in first['items']}&{r['id'] for r in second['items']}),kind+' pagination returns distinct records')
 empty=call('/mes/system-logs?kind='+kind+'&keyword='+uuid.uuid4().hex,admin)
 check(empty['total']==0 and empty['items']==[],kind+' keyword filter returns an actual empty result')
 today=datetime.date.today().isoformat();dated=call('/mes/system-logs?kind='+kind+'&startDate='+today+'&endDate='+today,admin)
 check(all(str(r['event_time']).startswith(today) for r in dated['items']),kind+' date range includes only matching records')
 if kind!='audit':
  successes=call('/mes/system-logs?kind='+kind+'&status=1',admin);check(all(str(r['status'])=='1' for r in successes['items']),kind+' status filter is applied by the database')
for query in ['kind=unknown','page=0','size=101','startDate=wrong','startDate=2026-10-24&endDate=2026-10-23','kind=audit&status=1']:
 call('/mes/system-logs?'+query,admin,expect=400)
check(True,'Invalid log types, dates, status and pagination are rejected')
result={'passed':len(checks),'checks':checks,'backend':BASE,'database':DB}
(ROOT/'.runtime/system-logs-acceptance.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print('System log acceptance passed:',len(checks))

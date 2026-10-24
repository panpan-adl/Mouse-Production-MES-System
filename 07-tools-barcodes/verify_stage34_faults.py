"""Failure injection only on the completed isolated fixture; restore all altered rows."""
from stage1_database import ROOT,sql
import json,urllib.request,urllib.error,datetime
DB=json.loads((ROOT/'.runtime/stage34-test-database.json').read_text(encoding='utf-8'))['database'];assert DB.startswith('mouse_mes_stage34_test_')
def scalar(q):return sql(q,DB).strip().splitlines()[-1]
def api(path,body=None,token=None,ok=True):
 req=urllib.request.Request('http://127.0.0.1:8082/api'+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:r=json.load(urllib.request.urlopen(req,timeout=20))
 except urllib.error.HTTPError as e:r=json.load(e)
 assert (r.get('code')==200)==ok,r
 return r.get('data',r)
checks=[]
def check(v,label):
 assert v,label
 checks.append(label);print('PASS '+label)
u=json.loads(scalar("SELECT JSON_OBJECT('code',user_code,'password',password) FROM t_user WHERE role_id=9 LIMIT 1"));token=api('/auth/login',{'userCode':u['code'],'password':u['password']})['token']
task=int(scalar('SELECT MIN(task_id) FROM mm_qc_todo'));event=int(scalar(f'SELECT event_id FROM mm_qc_todo WHERE task_id={task}'))
templates=[int(x) for x in sql(f"SELECT q.id FROM mm_qc_template q JOIN t_production_task t ON t.product_id=q.product_id WHERE t.id={task} AND q.kind='PROCESS'",DB).strip().splitlines()[1:]]
ids=','.join(map(str,templates));lines=sql(f'SELECT id,template_id,item_id FROM mm_qc_line WHERE template_id IN ({ids})',DB).strip().splitlines()[1:]
try:
 sql(f"UPDATE t_production_task SET task_status=3 WHERE id={task}; UPDATE mm_auto_event SET status='PENDING',attempts=0 WHERE id={event}; DELETE FROM mm_qc_line WHERE template_id IN ({ids});",DB)
 for attempt in range(1,4):
  api('/automation/process',{},token)
  check(scalar(f"SELECT CONCAT(status,':',attempts) FROM mm_auto_event WHERE id={event}")==f'FAILED:{attempt}','Missing QC template records actionable failure attempt '+str(attempt))
  if attempt<3:api('/automation/retry/'+str(event),{},token)
 api('/automation/retry/'+str(event),{},token,False);check(True,'Three failed executions exhaust retry allowance')
 sql('INSERT INTO mm_qc_line(id,template_id,item_id) VALUES '+','.join('('+','.join(line.split('\t'))+')' for line in lines),DB)
 sql(f"UPDATE mm_auto_event SET status='PENDING',attempts=0 WHERE id={event}",DB)
 api('/automation/process',{},token)
 check(scalar(f"SELECT status FROM mm_auto_event WHERE id={event}")=='SUCCEEDED','Correcting template configuration allows successful event recovery')
 check(scalar(f'SELECT COUNT(*) FROM mm_qc_todo WHERE task_id={task}')=='1','Recovery retains exactly one QC todo')
finally:
 if scalar(f'SELECT COUNT(*) FROM mm_qc_line WHERE template_id IN ({ids})')=='0' and lines:sql('INSERT INTO mm_qc_line(id,template_id,item_id) VALUES '+','.join('('+','.join(line.split('\t'))+')' for line in lines),DB)
 sql(f"UPDATE t_production_task SET task_status=4 WHERE id={task}; UPDATE mm_auto_event SET status='SUCCEEDED',attempts=1,error=NULL WHERE id={event}",DB)
(ROOT/'.runtime/stage34-fault-acceptance.json').write_text(json.dumps({'passed':len(checks),'checks':checks,'failure_injection':'isolated database only','date':datetime.datetime.now().isoformat()},ensure_ascii=False,indent=2),encoding='utf-8')

"""OpenAI-compatible adapter contract tests against a local stub, never a live model."""
from stage1_database import ROOT,sql,config
import json,urllib.request,urllib.error,threading,subprocess,time,os,datetime
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
state=json.loads((ROOT/'.runtime/stage34-test-database.json').read_text(encoding='utf-8'));DB=state['database'];assert DB.startswith('mouse_mes_stage34_test_')
checks=[]
def check(v,label):
 assert v,label
 checks.append(label);print('PASS '+label,flush=True)
class Stub(BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_POST(self):
  body=json.loads(self.rfile.read(int(self.headers['Content-Length'])));q=body['messages'][-1]['content']
  if '超时' in q:time.sleep(11)
  if '不可用' in q:self.send_response(503);self.end_headers();return
  content=json.dumps({'kind':'INVENTORY'} if '非法' not in q else {'kind':'INVENTORY','sql':'SELECT secret'},ensure_ascii=False)
  response={'choices':[{'finish_reason':'stop','message':{'content':'{' if '坏格式' in q else content}}],'usage':{'prompt_tokens':23,'completion_tokens':7}}
  self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers()
  try:self.wfile.write(json.dumps(response).encode())
  except BrokenPipeError:pass
server=ThreadingHTTPServer(('127.0.0.1',8084),Stub);threading.Thread(target=server.serve_forever,daemon=True).start()
env=os.environ.copy();env.update(MES_AI_MODE='remote',MES_AI_BASE_URL='http://127.0.0.1:8084/v1',MES_AI_MODEL='local-contract-stub',MES_AI_API_KEY='stub-only-no-real-key')
url=f'jdbc:mysql://127.0.0.1:3306/{DB}?useUnicode=true&useSSL=false&characterEncoding=utf-8&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true'
with (ROOT/'.runtime/stage34-remote.stdout.log').open('w',encoding='utf-8') as log:
 proc=subprocess.Popen([str(__import__('pathlib').Path(config['javaHome'])/'bin/java.exe'),'-jar',str(ROOT/'01-backend/target/mouse-mes-1.0.0.jar'),'--server.port=8083','--spring.datasource.url='+url],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
 try:
  for _ in range(60):
   try:urllib.request.urlopen('http://127.0.0.1:8083/api/ai/config',timeout=1);break
   except urllib.error.HTTPError:break
   except OSError:time.sleep(.5)
  def api(path,body=None,token=None,ok=True):
   req=urllib.request.Request('http://127.0.0.1:8083/api'+path,data=json.dumps(body).encode() if body else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
   try:r=json.load(urllib.request.urlopen(req,timeout=16))
   except urllib.error.HTTPError as e:r=json.load(e)
   if ok:assert r.get('code')==200,r
   else:assert r.get('code')!=200,r
   return r.get('data') if r.get('code')==200 else r
  user=json.loads(sql("SELECT JSON_OBJECT('id',id,'code',user_code,'password',password) FROM t_user WHERE role_id=9 LIMIT 1",DB).splitlines()[-1]);token=api('/auth/login',{'userCode':user['code'],'password':user['password']})['token']
  result=api('/ai/query',{'question':'查询库存'},token)
  check(result['mode']=='remote' and result['inputTokens']==23 and result['outputTokens']==7,'Remote adapter records provider token usage with structured tool execution')
  for q in ['查询非法字段库存','查询坏格式库存','查询不可用库存','查询超时库存']:
   result=api('/ai/query',{'question':q},token,False);check('result' not in result,'Explicit error without local fallback: '+q)
  sql(f"UPDATE mm_ai_budget SET calls=20 WHERE user_id={user['id']} AND day=CURDATE()",DB)
  api('/ai/query',{'question':'查询库存'},token,False);check(True,'Daily call cap rejects the twenty-first remote request')
  report={'passed':len(checks),'checks':checks,'provider':'local protocol stub','live_model_verified':False,'date':datetime.datetime.now().isoformat()}
  (ROOT/'.runtime/stage34-remote-acceptance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
 finally:proc.terminate();proc.wait(timeout=20);server.shutdown()

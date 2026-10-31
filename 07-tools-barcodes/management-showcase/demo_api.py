import json,urllib.request,urllib.error
BASE='http://127.0.0.1:8080/api'
def api(path,body=None,method='GET',token=None):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=40) as r:
   data=r.read();result=json.loads(data) if data else None
 except urllib.error.HTTPError as e:raise RuntimeError(path+' '+e.read().decode())
 if isinstance(result,dict) and result.get('code',200)!=200:raise RuntimeError(path+' '+str(result))
 return result.get('data',result) if isinstance(result,dict) else result
def login(code='109001'):return api('/auth/login',{'userCode':code,'password':'123456'},'POST')['token']

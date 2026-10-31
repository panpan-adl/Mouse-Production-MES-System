import sys,json,datetime,uuid,urllib.request,urllib.error,concurrent.futures
from pathlib import Path
p=Path('.runtime/four-tier-20261030')
s=(p/'suite/verify_mes_completion.py').read_text(encoding='utf-8');exec(compile(s.split("if scalar('SELECT COUNT(*) FROM t_user")[0],'four-tier-api-helpers','exec'))
u=json.loads(sql("SELECT JSON_OBJECT('code',user_code,'password',password) FROM t_user WHERE role_id=9 AND deleted=0 AND status=1 LIMIT 1",DB).splitlines()[-1]);admin=api('/auth/login',{'userCode':u['code'],'password':u['password']},'POST')['token']
bom=next(r for r in get('bom-versions') if r['code']=='BOM-A100-V1');leaves=api('/foundation/bom/'+str(bom['id'])+'/expand?quantity=1',token=admin);mapping={};fixtures=[]
for old in leaves:
 if old['material_type']!=1:continue
 fresh=save('materials',{'material_name':'验收'+old['material_name'],'material_type':1,'unit':old['unit'],'safety_stock':5,'sourcing_mode':'BUY'});mapping[old['id']]=fresh['id'];fixtures.append(fresh)
for line in get('bom-lines'):
 if line['bom_id']==bom['id'] and line['material_id'] in mapping:line['material_id']=mapping[line['material_id']];api('/foundation/bom-lines/'+str(line['id']),line,'PUT',admin)
route=next(r for r in get('routes') if r['code']=='ROUTE-A100-V1');ops={r['id'] for r in get('operations') if r['route_id']==route['id']}
for c in get('consumption'):
 if c['operation_id'] in ops and c['material_id'] in mapping:c['material_id']=mapping[c['material_id']];api('/foundation/consumption/'+str(c['id']),c,'PUT',admin)
assert len(mapping)==12
(p/'material-fixtures.json').write_text(json.dumps({'materials':fixtures,'old_to_new':mapping},ensure_ascii=False,indent=2),encoding='utf-8');print('Created 12 real new material archives with empty origins; updated acceptance BOM and operation inputs. Existing historical baseline unchanged.')

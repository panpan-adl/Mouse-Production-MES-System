// Explicit opt-in real-model acceptance. Credentials stay in memory; only fictional fixtures are modified.
const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const ts=require('../02-frontend-web/node_modules/typescript');
const fixtures={exports:{}};new Function('exports',ts.transpileModule(fs.readFileSync('02-frontend-web/src/data/knowledgeEvaluation.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText)(fixtures.exports);
const {evaluationFiles,evaluationCases}=fixtures.exports;
if(!process.argv.includes('--run-live'))throw Error('--run-live required: real paid model calls');
const out='.runtime/knowledge-lab-live';fs.mkdirSync(out,{recursive:true});
const checks=[],responses={},created=[],cleanup=[];let token,browser,identity,failure,originalReviews;
const base='http://127.0.0.1:5173/api';
async function api(url,body,method,auth=token){
 const headers={};if(auth)headers.Authorization='Bearer '+auth;
 let payload;if(body instanceof FormData)payload=body;else if(body!==undefined){payload=JSON.stringify(body);headers['Content-Type']='application/json';}
 const r=await fetch(base+url,{method:method||(body!==undefined?'POST':'GET'),headers,body:payload,signal:AbortSignal.timeout(1000000)});
 return r.json();
}
function check(name,condition){checks.push({name,passed:!!condition});console.log((condition?'PASS ':'FAIL ')+name);assert(condition,name);}
async function upload(f,version,text){const data=new FormData();for(const [key,value]of Object.entries({documentId:f.id,version:String(version),title:f.title}))data.append(key,value);data.append('file',new Blob([text],{type:'text/plain'}),f.key+'.txt');return api('/rag/documents',data);}
(async()=>{
 try{
  identity=(await api('/auth/login',{userCode:process.env.MES_VERIFY_USER||'109001',password:process.env.MES_VERIFY_PASSWORD||'123456'})).data;token=identity?.token;check('登录并装配加分功能',token&&(await api('/knowledge/lab/quality')).code===200);
  const existing=(await api('/rag/documents')).data;const events=(await api('/rag/events')).data;
  check('专用样例编号无现有资料冲突',evaluationFiles.every(f=>!existing.some(d=>d.documentId===f.id)));
  for(const f of evaluationFiles){const version=Math.max(0,...events.filter(e=>e.documentId===f.id).map(e=>e.version))+1;created.push({...f,version});const r=await upload(f,version,f.text);check(f.key+'样例真实共同发布',r.code===200&&r.data.status==='READY'&&r.data.graphStatus==='READY');}
  const ids=created.map(f=>f.id),scope={documentIds:ids};
  const comparison=await api('/knowledge/lab/compare',{question:'点胶工序使用的设备由谁负责？',...scope});responses.comparison=comparison;
  check('同题三模式各有真实答案、来源和耗时',comparison.code===200&&comparison.data.results.length===3&&comparison.data.results.every(r=>r.success&&r.milliseconds>=0&&r.result.answer));
  const first=await api('/knowledge/lab/dialogue',{question:'点胶机DG01由谁负责？',mode:'GRAPHRAG',...scope});responses.firstTurn=first;
  check('第一轮问答建立服务端话题',first.code===200&&first.data.contextId&&first.data.result.answer.includes('张师傅'));
  const next=await api('/knowledge/lab/dialogue',{question:'它每班做什么点检？',mode:'GRAPHRAG',contextId:first.data.contextId,...scope});responses.followup=next;
  check('连续追问消解设备指代并重新检索',next.code===200&&next.data.resolvedQuestion.includes('DG01')&&next.data.result.answer.includes('喷嘴'));
  originalReviews=(await api('/knowledge/lab/quality')).data;
  for(const name of ['点胶机DG01','点胶设备DG01'])check('人工确认设备类型：'+name,(await api('/knowledge/lab/review',{name,type:'EQUIPMENT'})).code===200);
  check('同型号别名经人工确认', (await api('/knowledge/lab/review',{name:'点胶设备DG01',canonical:'点胶机DG01'})).code===200);
  const quality=await api('/knowledge/lab/quality?'+ids.map(id=>'documentIds='+id).join('&'));responses.quality=quality;
  check('实体类型与别名审核持久化',quality.code===200&&quality.data.aliases.some(a=>a.alias==='点胶设备DG01'&&a.canonical==='点胶机DG01'));
  const relation=await api('/knowledge/lab/association?entity='+encodeURIComponent('点胶机DG01')+'&'+ids.map(id=>'documentIds='+id).join('&'));responses.association=relation;
  check('MES制造关联含两跳关系和文件证据',relation.code===200&&relation.data.graph.facts.length>0&&relation.data.graph.sources.length>0);
  browser=await chromium.launch({channel:'chrome',headless:true});const page=await browser.newPage({viewport:{width:1440,height:1050}}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.addInitScript(user=>localStorage.setItem('ptl_auth',JSON.stringify({currentUser:user})),identity);
  await page.goto('http://127.0.0.1:5173/knowledge');const lab=page.locator('.knowledge-lab');await lab.waitFor();
  await lab.getByLabel('同题对比问题').fill('点胶机DG01由谁负责？');await lab.getByLabel('实验资料范围').selectOption('course-eval-equipment');await lab.getByRole('button',{name:'运行三模式对比',exact:true}).click();await lab.locator('.compare-grid article').nth(2).waitFor({timeout:180000});await page.screenshot({path:out+'/comparison.png',fullPage:true});check('真实界面并排展示三模式',await lab.locator('.compare-grid article').count()===3);
  await lab.getByRole('button',{name:'检索评估',exact:true}).click();await page.waitForFunction(()=>document.querySelector('.eval-cases input')&&!document.querySelector('.eval-cases input').disabled);
  for(let i=0;i<16;i++)await lab.locator('.eval-cases input').nth(i).check();
  await lab.getByRole('button',{name:'运行所选评估',exact:true}).click();await page.waitForFunction(()=>document.querySelectorAll('.eval-result').length===16&&!Array.from(document.querySelectorAll('button')).some(b=>b.textContent.includes('当前问题完成后停止')),{},{timeout:240000});
  check('16道第一阶段真实评估完成且未隐藏失败',await lab.locator('.eval-result').count()===16&&!await lab.locator('.eval-result').filter({hasText:'调用已达'}).count());
  let downloaded=page.waitForEvent('download');await lab.getByRole('button',{name:'导出评估报告',exact:true}).click();await(await downloaded).saveAs(out+'/evaluation-v1.json');
  const report=JSON.parse(fs.readFileSync(out+'/evaluation-v1.json','utf8'));responses.evaluationV1=report;check('评估导出包含真实答案与自动指标',report.rows.length===16&&report.rows.every(r=>r.success&&r.result.sources)&&report.summary.succeeded===16);
  await lab.getByRole('button',{name:'发布负责人变更为第二版',exact:true}).click();await lab.getByText('设备负责人已更新为赵师傅，选择Q17、Q18检验新版本',{exact:true}).waitFor({timeout:120000});
  const equip=created.find(f=>f.key==='equipment'),updatedVersion=equip.version+1;
  const diff=await api('/knowledge/lab/diff/'+equip.id+'?before='+equip.version+'&after='+updatedVersion);responses.diff=diff;
  check('更新差异含移除旧负责人及新增负责人',diff.code===200&&diff.data.added.some(f=>f.target.includes('赵师傅'))&&diff.data.removed.some(f=>f.target.includes('张师傅')));
  const stale=await api('/knowledge/lab/dialogue',{question:'它呢？',mode:'GRAPHRAG',contextId:first.data.contextId,...scope});check('旧版本追问拒绝复用',stale.code===503);
  for(let i=16;i<18;i++)await lab.locator('.eval-cases input').nth(i).check();
  await lab.getByRole('button',{name:'运行所选评估',exact:true}).click();await page.waitForFunction(()=>document.querySelectorAll('.eval-result').length===2&&!Array.from(document.querySelectorAll('button')).some(b=>b.textContent.includes('当前问题完成后停止')),{},{timeout:120000});
  downloaded=page.waitForEvent('download');await lab.getByRole('button',{name:'导出评估报告',exact:true}).click();await(await downloaded).saveAs(out+'/evaluation-v2.json');const v2=JSON.parse(fs.readFileSync(out+'/evaluation-v2.json','utf8'));responses.evaluationV2=v2;check('两道更新后评估使用新负责人且来源为新版本',v2.rows.length===2&&v2.rows.every(r=>r.success&&r.result.answer.includes('赵师傅')&&r.result.sources.filter(s=>s.documentId===equip.id).every(s=>s.version===updatedVersion)));
  await lab.getByRole('button',{name:'版本差异',exact:true}).click();await lab.getByLabel('选择文件').selectOption(equip.id);await lab.getByRole('button',{name:'查看图谱差异',exact:true}).click();await lab.locator('.graph-edge.removed').first().waitFor();await page.screenshot({path:out+'/difference.png',fullPage:true});check('真实图谱新增高亮与移除虚线展示',await lab.locator('.graph-edge.highlighted').count()>0&&await lab.locator('.graph-edge.removed').count()>0);
  await page.setViewportSize({width:390,height:844});await page.waitForFunction(()=>document.documentElement.scrollWidth<=innerWidth+2);await page.screenshot({path:out+'/mobile.png',fullPage:true});check('加分工作台手机宽度无溢出',true);check('真实界面无脚本异常',errors.length===0);
  const foreign=(await api('/auth/login',{userCode:'108001',password:process.env.MES_VERIFY_OTHER_PASSWORD||'123456'})).data;
  check('其他账号不能读取历史差异',(await api('/knowledge/lab/diff/'+equip.id+'?before='+equip.version+'&after='+updatedVersion,undefined,'GET',foreign.token)).code===403);
  check('其他账号不能复用追问话题',(await api('/knowledge/lab/dialogue',{question:'谁负责？',mode:'GRAPHRAG',contextId:next.data.contextId},'POST',foreign.token)).code===403);
 }catch(e){failure=e.message;console.error(e.message);}
 finally{
  if(browser)await browser.close();
  if(token){
   if(originalReviews){try{
    await api('/knowledge/lab/review',{name:'点胶设备DG01',canonical:''});
    for(const name of ['点胶机DG01','点胶设备DG01'])await api('/knowledge/lab/review',{name,type:originalReviews.entities.find(n=>n.name===name)?.type||'OTHER'});
    const original=originalReviews.aliases.find(a=>a.alias==='点胶设备DG01');if(original)await api('/knowledge/lab/review',{name:original.alias,canonical:original.canonical});
   }catch(e){failure=(failure||'')+'; review restoration failed';}}
   for(const f of created){try{const docs=(await api('/rag/documents')).data;const active=docs.find(d=>d.documentId===f.id);if(active){const r=await api('/rag/documents/'+f.id+'?version='+(active.version+1),undefined,'DELETE');cleanup.push({id:f.id,success:r.code===200&&!r.data.cleanupPending&&!r.data.graphCleanupPending});}}catch(e){cleanup.push({id:f.id,success:false,error:e.message});}}
   if(cleanup.some(c=>!c.success))failure=(failure||'')+'; fixture cleanup incomplete';}
  fs.writeFileSync(out+'/acceptance.json',JSON.stringify({checks,responses,cleanup,success:!failure,failure:failure||null},null,2));
 }
 if(failure)process.exitCode=1;
})().catch(e=>{console.error(e.message);process.exitCode=1;});

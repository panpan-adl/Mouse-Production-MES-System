// Failure and recovery contracts with explicit fixtures; does not call a paid provider.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('assert');
const base=process.env.KNOWLEDGE_UI_URL||'http://127.0.0.1:5173',out='.runtime/knowledge-lab-ui';
(async()=>{
 fs.mkdirSync(out,{recursive:true});const browser=await chromium.launch({channel:'chrome',headless:true}),page=await browser.newPage({viewport:{width:1440,height:1050}}),checks=[],errors=[],contexts=[];
 const docs=['process','equipment','inspection'].map(key=>({documentId:'course-eval-'+key,title:'评估样例：'+key,version:1,chunkCount:1}));let messages=0,dialogue=0;
 const fact={source:'点胶机DG01',relation:'负责人',target:'张师傅',quote:'点胶机DG01由张师傅负责。',chunkId:'build:0'};
 const answer=(mode='GRAPHRAG')=>({answer:'点胶和组装工序。[S1]',sources:[{id:'S1',documentId:docs[0].documentId,title:docs[0].title,version:1,chunkId:'build:0',text:'A100鼠标经过点胶工序和组装工序。'}],facts:[fact],mode,model:'fixture',retrieval:'fixture'});
 await page.addInitScript(()=>localStorage.setItem('ptl_auth',JSON.stringify({currentUser:{id:7,roleId:9,permissionRoleId:9,realName:'实验测试',token:'fixture-only'}})));
 await page.route(base+'/api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),path=url.pathname;let data=[],status=200,msg='success';
  if(path==='/api/mes/access-role')data=9;
  if(path==='/api/knowledge/status')data={enabled:true,modelConfigured:true,model:'fixture',ragProvider:'fixture',neo4j:'已连接'};
  if(path==='/api/knowledge/documents')data=docs;
  if(path==='/api/knowledge/graph')data={facts:[fact],sources:answer().sources,truncated:false};
  if(path==='/api/rag/status')data={embeddingConfigured:true,embeddingModel:'fixture'};
  if(path==='/api/rag/documents/course-eval-equipment')data={text:'点胶机DG01由张师傅负责。'};
  if(path==='/api/knowledge/lab/compare')data={question:req.postDataJSON().question,versions:docs,results:[{mode:'RAG',success:false,error:'嵌入模型调用已达上限',milliseconds:10},{mode:'GRAPHRAG',success:true,result:answer(),milliseconds:15},{mode:'HYBRID',success:true,result:answer('HYBRID'),milliseconds:20}]};
  if(path==='/api/knowledge/messages'){messages++;if(messages===2){status=503;msg='真实调用限额示例';data=null;}else data=answer();}
  if(path==='/api/knowledge/lab/dialogue'){const body=req.postDataJSON();contexts.push(body.contextId);dialogue++;if(dialogue===2){status=503;msg='追问资料范围或版本已变化，请开启新话题';data=null;}else data={result:answer(),contextId:'fixture-context-'+dialogue,resolvedQuestion:dialogue===1?body.question:'点胶机DG01做什么点检？'};}
  await route.fulfill({status,json:{code:status,msg,data}});
 });
 try{
  await page.goto(base+'/knowledge');const lab=page.locator('.knowledge-lab');await lab.waitFor();
  await lab.getByLabel('同题对比问题').fill('工序设备谁负责？');await lab.getByRole('button',{name:'运行三模式对比',exact:true}).click();await lab.locator('.compare-grid article').nth(2).waitFor();
  assert.equal(await lab.locator('.compare-grid article').count(),3);await lab.getByText('嵌入模型调用已达上限',{exact:true}).waitFor();checks.push('对比保留一个模式失败及另外两个成功结果');
  let downloaded=page.waitForEvent('download');await lab.getByRole('button',{name:'导出对比结果',exact:true}).click();await(await downloaded).saveAs(out+'/comparison.json');assert.equal(JSON.parse(fs.readFileSync(out+'/comparison.json','utf8')).results[0].success,false);checks.push('导出保留真实失败标记和耗时');
  await lab.getByRole('button',{name:'检索评估',exact:true}).click();await lab.getByRole('button',{name:'运行所选评估',exact:true}).click();await page.waitForFunction(()=>document.querySelectorAll('.eval-result').length===2&&!Array.from(document.querySelectorAll('button')).some(b=>b.textContent.includes('当前问题完成后停止')));
  assert.equal(messages,2);await lab.getByText('真实调用限额示例',{exact:true}).waitFor();downloaded=page.waitForEvent('download');await lab.getByRole('button',{name:'导出评估报告',exact:true}).click();await(await downloaded).saveAs(out+'/evaluation.json');const report=JSON.parse(fs.readFileSync(out+'/evaluation.json','utf8'));assert.equal(report.summary.errors,1);assert.equal(report.summary.succeeded,1);assert.equal(report.summary.manualReviewed,0);checks.push('评估遇到限额停止，成功和错误分别统计，不冒充人工正确率');
  await page.getByLabel('连续追问',{exact:true}).check();await page.getByLabel('知识库问题').fill('点胶机由谁负责？');await page.getByRole('button',{name:'提问',exact:true}).click();await page.locator('.chat-turn').first().waitFor();
  await page.getByLabel('知识库问题').fill('它呢？');await page.getByRole('button',{name:'提问',exact:true}).click();await page.getByText('追问资料范围或版本已变化，请开启新话题',{exact:true}).waitFor();assert.equal(contexts[0],null);assert.equal(contexts[1],'fixture-context-1');checks.push('追问携带服务端会话编号，资料变化后显示重新开话题提示');
  await page.getByRole('button',{name:'开启新话题',exact:true}).click();await page.getByRole('button',{name:'提问',exact:true}).click();await page.locator('.chat-turn').nth(1).waitFor();assert.equal(contexts[2],null);
  await page.getByRole('button',{name:'清空本页对话',exact:true}).click();await page.getByLabel('知识库问题').fill('设备做什么点检？');await page.getByRole('button',{name:'提问',exact:true}).click();await page.locator('.chat-turn').first().waitFor();assert.equal(contexts[3],null);checks.push('新话题和清空对话均撤销浏览器的旧上下文引用');
  await page.setViewportSize({width:390,height:844});await page.waitForFunction(()=>document.documentElement.scrollWidth<=innerWidth+2);checks.push('评估错误和追问控件在手机宽度无溢出');assert.deepEqual(errors,[]);fs.writeFileSync(out+'/checks.json',JSON.stringify({fixture:true,checks,errors},null,2));console.log(JSON.stringify({fixture:true,checks,errors},null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

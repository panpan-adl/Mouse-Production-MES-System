// Explicit UI fixtures; no real embeddings, model calls or business data writes.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('assert');
const base=process.env.KNOWLEDGE_UI_URL||'http://127.0.0.1:5176',out='.runtime/joint-knowledge-ui';
(async()=>{
 fs.mkdirSync(out,{recursive:true});const browser=await chromium.launch({channel:'chrome',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1050}}),errors=[],checks=[],modes=[],sourceIds=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>localStorage.setItem('ptl_auth',JSON.stringify({currentUser:{id:1,roleId:9,permissionRoleId:9,realName:'联合界面测试',token:'joint-ui-fixture'}})));
 let doc,graphDoc,deleteEvent,original=Buffer.from('点胶机由甲负责。'),failReplacement=true;
 const fact=()=>({source:'点胶机',relation:'负责人',target:graphDoc?.version===2?'乙':'甲',quote:graphDoc?.text||'',chunkId:'shared-v'+(graphDoc?.version||1)+':0'});
 await page.route(base+'/api/**',async route=>{
  const request=route.request(),url=new URL(request.url()),path=url.pathname;let data=[];
  if(path==='/api/mes/access-role')data=9;
  if(path==='/api/rag/status')data={embeddingModel:'fixture-dense',embeddingConfigured:true,dimensions:1024,graphAvailable:true};
  if(path==='/api/knowledge/status')data={enabled:true,modelConfigured:true,model:'联合接口替身',ragProvider:'向量检索 · fixture-dense',neo4j:'已连接',vectorRetrieverConnected:true,revision:'task2-task3-r1'};
  if(path==='/api/rag/documents'&&request.method()==='GET')data=doc?[doc]:[];
  if(path==='/api/rag/events')data=deleteEvent?[deleteEvent]:[];
  if(path==='/api/knowledge/documents')data=graphDoc?[{...graphDoc,chunkCount:1}]:[];
  if(path==='/api/knowledge/graph')data={facts:graphDoc?[fact(),{...fact(),relation:'关联'}]:[],truncated:false,sources:graphDoc?[{id:'S1',documentId:graphDoc.documentId,title:graphDoc.title,version:graphDoc.version,chunkId:fact().chunkId,text:graphDoc.text,location:'第2页'}]:[]};
  if(path==='/api/rag/documents'&&request.method()==='POST'){
   const body=request.postDataBuffer().toString('utf8'),field=name=>body.match(new RegExp('name="'+name+'"\\r\\n\\r\\n([^\\r]+)'))?.[1];
   const id=field('documentId'),version=Number(field('version')),title=field('title');assert(id&&version&&title);
   doc={documentId:id,version,activeVersion:version===2&&failReplacement?1:version,title,filename:'joint.txt',status:version===2&&failReplacement?'FAILED':'READY',graphStatus:version===2&&failReplacement?'FAILED':'READY',error:version===2&&failReplacement?'图数据库离线，旧版本保留':''};
   if(doc.status==='READY')graphDoc={documentId:id,version,title,text:'点胶机由甲负责。'};
   data={...doc};
  }
  if(path.endsWith('/download')){assert(request.headers().authorization);await route.fulfill({contentType:'application/octet-stream',body:original});return;}
  if(path.startsWith('/api/rag/documents/')&&request.method()==='GET'&&!path.endsWith('/download'))data={...doc,text:graphDoc?.text||''};
  if(path.startsWith('/api/rag/documents/')&&path.endsWith('/retry')){
   if(doc){failReplacement=false;doc.status='READY';doc.graphStatus='READY';doc.error='';doc.activeVersion=2;graphDoc={documentId:doc.documentId,version:2,title:doc.title,text:'点胶机由乙负责。'};original=Buffer.from(graphDoc.text);data={status:'READY',graphStatus:'READY'};}
   else{deleteEvent.status='DELETED';deleteEvent.graphStatus='DELETED';data={deleted:true,cleanupPending:false,graphCleanupPending:false};}
  }
  if(path.startsWith('/api/rag/documents/')&&request.method()==='DELETE'){
   deleteEvent={documentId:doc.documentId,version:doc.version+1,action:'DELETE',status:'CLEANUP_PENDING',graphStatus:'CLEANUP_PENDING'};doc=undefined;graphDoc=undefined;data={deleted:true,cleanupPending:true,graphCleanupPending:true};
  }
  if(path==='/api/knowledge/messages'){
   const input=request.postDataJSON();modes.push(input.mode);assert(graphDoc);
   const source={id:'S1',documentId:graphDoc.documentId,title:graphDoc.title,version:graphDoc.version,chunkId:fact().chunkId,text:graphDoc.text,location:'第2页'};sourceIds.push(source.chunkId);
   data={answer:graphDoc.text+'[S1]',mode:input.mode,retrieval:input.mode==='GRAPHRAG'?'图谱两跳检索':'真实向量接口替身',sources:[source],facts:input.mode==='RAG'?[]:[fact(),{...fact(),relation:'关联'}],model:'fixture'};
  }
  await route.fulfill({json:{code:200,msg:'success',data}});
 });
 try{
  await page.goto(base+'/knowledge');const library=page.locator('.private-library');
  await library.getByRole('button',{name:'上传文件',exact:true}).click();
  await library.getByLabel('文件标题').fill('联合工艺资料');await library.getByLabel('选择原始文件').setInputFiles({name:'joint.txt',mimeType:'text/plain',buffer:original});
  await library.getByRole('button',{name:'保存并同步索引'}).click();await library.getByText('文件、向量和图谱已共同发布。',{exact:true}).waitFor();
  await page.getByRole('button',{name:'查看点胶机的关系',exact:true}).waitFor();assert.equal(await page.locator('.document-card .doc-actions button').count(),0);checks.push('上传原件后统一发布文件、向量和图谱，图谱区不再提供绕过文件库的编辑删除');
  const downloaded=page.waitForEvent('download');await library.getByRole('button',{name:'下载原件',exact:true}).click();await(await downloaded).saveAs(out+'/original.txt');assert(fs.readFileSync(out+'/original.txt').equals(original));checks.push('下载保留原件字节并携带登录身份');
  for(const mode of ['RAG','GRAPHRAG','HYBRID']){await page.getByLabel('检索方式').selectOption(mode);await page.getByLabel('知识库问题').fill('点胶机由谁负责？');await page.getByRole('button',{name:'提问',exact:true}).click();await page.locator('.chat-turn').nth(modes.length-1).waitFor();}
  assert.deepEqual(modes,['RAG','GRAPHRAG','HYBRID']);assert.equal(new Set(sourceIds).size,1);await page.locator('.chat-turn').last().getByText(/第2页/).waitFor();checks.push('三种问答模式在同一页面工作，并返回共享片段编号与原文页码');
  await page.getByRole('button',{name:'查看本次回答子图',exact:true}).last().click();
  await page.locator('.graph-edge.highlighted').first().waitFor();assert.equal(await page.locator('.graph-edge.highlighted').count(),2);assert.equal(await page.locator('.graph-node.highlighted').count(),2);
  const paths=await page.locator('.graph-edge .edge-line').evaluateAll(items=>items.map(i=>i.getAttribute('d')));assert.equal(new Set(paths).size,2);
  await page.locator('.graph-edge').first().click();await page.locator('.relation-evidence').getByText(/联合工艺资料 · 版本 1 · 第2页/).waitFor();
  checks.push('回答证据子图高亮，平行关系曲线分离，点击关系定位文件、版本和页码');
  await page.screenshot({path:out+'/desktop.png',fullPage:true});
  await page.getByRole('button',{name:'返回回答',exact:true}).click();graphDoc.version=2;
  await page.getByRole('button',{name:'查看本次回答子图',exact:true}).last().click();await page.getByText('资料已经更新或删除，请重新提问后查看子图',{exact:true}).waitFor();graphDoc.version=1;
  checks.push('打开回答子图前重新核对服务端版本，外部更新后拒绝旧答案子图');
  await library.getByRole('button',{name:'替换原件'}).click();await library.getByLabel('选择原始文件').setInputFiles({name:'joint.txt',mimeType:'text/plain',buffer:Buffer.from('点胶机由乙负责。')});await library.getByRole('button',{name:'保存并同步索引'}).click();
  await library.getByText('图数据库离线，旧版本保留',{exact:true}).waitFor();assert.equal(graphDoc.version,1);assert.equal(await page.locator('.chat-turn').count(),0);checks.push('失败更新明确显示旧可检索版本，并清空页面旧回答');
  await library.getByRole('button',{name:'重试同步',exact:true}).click();await library.getByText('已重试，请核对文件与图谱状态。',{exact:true}).waitFor();assert.equal(graphDoc.version,2);checks.push('失败版本可重试同步，无需再次递增版本');
  doc.status='INDEXING';doc.stage='GRAPH_BUILD';doc.graphStatus='INDEXING';await page.reload();await library.getByText(/服务器处理中 · 抽取关系与构建图谱/).waitFor();
  doc.status='READY';doc.stage='READY';doc.graphStatus='READY';await page.waitForFunction(()=>!document.querySelector('.stage-list .current'));checks.push('刷新后恢复服务器处理阶段，并自动轮询完成状态');
  await page.setViewportSize({width:390,height:844});await page.waitForFunction(()=>document.querySelector('.mes-sidebar').getBoundingClientRect().width<80);await page.waitForFunction(()=>document.documentElement.scrollWidth<=innerWidth+2);await page.screenshot({path:out+'/mobile.png',fullPage:true});checks.push('统一文件库和图谱页面在手机宽度无横向溢出');
  await library.getByRole('button',{name:'删除文件',exact:true}).click();await page.getByRole('button',{name:'确认删除文件',exact:true}).click();await library.getByText('已退出两类检索，物理清理待重试。',{exact:true}).waitFor();
  await library.locator('.file-events summary').click();await library.getByRole('button',{name:'重试删除清理',exact:true}).click();await library.getByText('已重试，请核对文件与图谱状态。',{exact:true}).waitFor();assert.equal(deleteEvent.graphStatus,'DELETED');checks.push('删除立即退出图谱和文件列表，处理记录中可重试双侧清理');
  assert.deepEqual(errors,[]);fs.writeFileSync(out+'/checks.json',JSON.stringify({fixture:true,checks,errors},null,2));console.log(JSON.stringify({fixture:true,checks,errors},null,2));
 }catch(error){await page.screenshot({path:out+'/failure.png',fullPage:true});throw error;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

// Real browser evidence. Auth is passed through the child process environment, never saved.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('assert');
(async()=>{
 const auth=JSON.parse(process.env.MES_CAPTURE_AUTH||'null');if(!auth?.token)throw Error('Capture requires a logged-in session');
 const browser=await chromium.launch({channel:'chrome',headless:true}),page=await browser.newPage({viewport:{width:1440,height:1050}});
 const out='.runtime/task3-final-live',errors=[];fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(identity=>localStorage.setItem('ptl_auth',JSON.stringify({currentUser:identity})),auth);
 try{
  await page.goto('http://127.0.0.1:5173/knowledge');await page.locator('.graph-node').first().waitFor();
  await page.locator('.graph-edge').first().click();await page.locator('.relation-evidence').waitFor();await page.screenshot({path:out+'/graph.png',fullPage:true});
  await page.getByLabel('检索方式').selectOption('GRAPHRAG');await page.getByLabel('知识库问题').fill(process.env.MES_CAPTURE_QUESTION);
  await page.getByRole('button',{name:'提问',exact:true}).click();await page.locator('.answer-text').waitFor({timeout:120000});
  await page.locator('.source-card').first().locator('summary').click();await page.screenshot({path:out+'/answer.png',fullPage:true});
  await page.getByRole('button',{name:'查看本次回答子图',exact:true}).click();await page.locator('.graph-edge.highlighted').first().waitFor();assert(await page.locator('.graph-edge.highlighted').count()>0);
  await page.locator('.graph-edge.highlighted').first().click();await page.screenshot({path:out+'/subgraph.png',fullPage:true});
  await page.setViewportSize({width:390,height:844});await page.waitForFunction(()=>document.documentElement.scrollWidth<=innerWidth+2);await page.screenshot({path:out+'/mobile.png',fullPage:true});
  assert.deepEqual(errors,[]);console.log('PASS real UI graph, answer, highlighted evidence, mobile layout');
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});

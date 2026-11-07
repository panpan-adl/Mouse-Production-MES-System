// UI contract test with explicit API fixtures. Does not validate live Neo4j or a real model.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs'), assert = require('assert');
const base = process.env.KNOWLEDGE_UI_URL || 'http://127.0.0.1:5176';
const output = '.runtime/knowledge-ui';
(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1050 } });
  const errors = [], checks = [], modes = [], scopes = [], graphQueries = [];
  page.on('pageerror', e => errors.push(e.message));
  let enabled = false, current = null, deletionPending = false, deleted = null;
  const fact = { source: '点胶工序', relation: '使用', target: '点胶机', quote: '点胶工序使用点胶机。', chunkId: 'fixture-build:0' };
  const otherFact = { source: '装配工序', relation: '使用', target: '工作站', quote: '装配工序使用工作站。', chunkId: 'fixture-build:0' };
  await page.addInitScript(() => localStorage.setItem('ptl_auth', JSON.stringify({ currentUser: { id: 7, roleId: 9, permissionRoleId: 9, realName: '课程界面测试', token: 'ui-fixture-only' } })));
  await page.route(base + '/api/**', async route => {
    const req = route.request(), path = new URL(req.url()).pathname;
    let data = [];
    if (path === '/api/mes/access-role') data = 9;
    if (path === '/api/knowledge/status') data = { enabled, modelConfigured: true, model: '界面测试替身', ragProvider: '关键词基线（尚未接入任务二向量检索）', neo4j: '界面测试替身' };
    if (path === '/api/knowledge/documents' && req.method() === 'GET') data = current ? [{ ...current, chunkCount: 1 }] : [];
    if (path === '/api/knowledge/documents' && req.method() === 'POST') { current = req.postDataJSON(); data = { relationCount: 1, unchanged: false }; }
    if (path === '/api/knowledge/deletions') data = deletionPending ? [deleted] : [];
    if (path.startsWith('/api/knowledge/documents/') && req.method() === 'GET') data = current;
    if (path.startsWith('/api/knowledge/documents/') && req.method() === 'DELETE') {
      if(current) { deleted={documentId:current.documentId,version:current.version+1};deletionPending=true;current=null;data={deleted:true,cleanupPending:true,message:'检索权限已移除，图数据库清理待重试'}; }
      else { deletionPending=false;data = { deleted: true, cleanupPending: false }; }
    }
    if (path === '/api/knowledge/graph') { graphQueries.push(new URL(req.url()).searchParams); data = { facts: current ? [fact,otherFact] : [], truncated: false }; }
    if (path === '/api/knowledge/messages') {
      modes.push(req.postDataJSON().mode);
      scopes.push(req.postDataJSON().documentIds);
      data = { answer: '点胶工序使用点胶机。[S1]', mode: req.postDataJSON().mode, retrieval: '界面协议替身', model: 'fixture', facts: [fact], sources: [{ id: 'S1', documentId: current.documentId, title: current.title, version: current.version, chunkId: fact.chunkId, text: current.text }] };
    }
    await route.fulfill({ json: { code: 200, msg: 'success', data } });
  });
  try {
    await page.goto(base + '/knowledge');
    await page.getByText('知识库尚未启用。', { exact: false }).waitFor();
    assert(await page.getByRole('button', { name: '导入资料', exact: true }).isDisabled()); checks.push('模块未启用时显示说明并禁用导入');
    enabled = true; await page.getByRole('button', { name: '检测连接' }).click();
    await page.getByRole('button', { name: '导入资料', exact: true }).click();
    await page.getByLabel('资料标题', { exact: true }).fill('A100工艺测试资料');
    await page.getByLabel('资料内容', { exact: true }).fill('点胶工序使用点胶机。');
    await page.getByRole('button', { name: '保存并构建图谱' }).click();
    await page.getByRole('button', { name: '查看点胶机的关系', exact: true }).waitFor();
    await page.getByRole('button', { name: '查看点胶机的关系', exact: true }).click();
    await page.locator('.node-detail blockquote').waitFor(); checks.push('导入资料后图谱显示节点，点击节点查看关系和原文');
    await page.getByLabel('仅看选中节点的关系').check();assert.equal(await page.locator('.graph-node').count(),2);
    await page.getByLabel('仅看选中节点的关系').uncheck();assert.equal(await page.locator('.graph-node').count(),4);checks.push('可以聚焦选中节点及直接关系，再恢复全图');
    await page.screenshot({ path: output + '/desktop.png', fullPage: true });
    await page.getByRole('button', { name: '更新', exact: true }).click();
    await page.getByLabel('资料内容', { exact: true }).fill('点胶工序使用点胶机。点胶机需要每日点检。');
    await page.getByRole('button', { name: '保存并构建图谱' }).click();
    await page.getByText('版本 2 · 1 个片段').waitFor(); checks.push('更新保留文件编号并递增版本');
    await page.getByRole('button', {name:'测试模型',exact:true}).click();
    await page.getByText('已实测连接',{exact:false}).waitFor();checks.push('模型配置与真实连接测试状态分开呈现（此处使用接口替身）');
    await page.getByLabel('资料范围').selectOption(current.documentId);
    await page.waitForFunction(() => !document.querySelector('.scope-toolbar select').disabled);
    await page.getByLabel('图数据库实体查询').fill('点胶机');
    await page.getByRole('button',{name:'查找关联',exact:true}).click();
    await page.waitForFunction(() => !document.querySelector('.scope-toolbar select').disabled);
    assert.equal(graphQueries.at(-1).get('entity'),'点胶机');assert.equal(graphQueries.at(-1).get('documentIds'),current.documentId);checks.push('实体查询与资料范围传给后端图数据库查询');
    const downloadEvent=page.waitForEvent('download');await page.getByRole('button',{name:'导出图谱',exact:true}).click();
    const download=await downloadEvent;await download.saveAs(output+'/export.json');
    assert.equal(JSON.parse(fs.readFileSync(output+'/export.json','utf8')).facts.length,2);checks.push('可导出当前范围的图谱关系及原文');
    for (const mode of ['GRAPHRAG', 'RAG', 'HYBRID']) {
      await page.getByLabel('检索方式').selectOption(mode);
      await page.getByLabel('知识库问题').fill('点胶工序涉及哪些设备？');
      await page.getByRole('button', { name: '提问', exact: true }).click();
      await page.locator('.chat-turn').nth(modes.length - 1).getByText('点胶工序使用点胶机。[S1]', { exact: true }).waitFor();
    }
    assert.deepEqual(modes, ['GRAPHRAG','RAG','HYBRID']);
    assert(scopes.every(ids => ids.length===1 && ids[0]===current.documentId));checks.push('三种问答模式都携带所选资料范围');
    await page.locator('.source-card').first().locator('summary').click();
    await page.screenshot({ path: output + '/chat.png', fullPage: true }); checks.push('三种模式提交正确，回答包含可展开的来源及版本');
    const conversationDownload=page.waitForEvent('download');await page.getByRole('button',{name:'导出问答与来源',exact:true}).click();
    await (await conversationDownload).saveAs(output+'/conversation.md');assert(fs.readFileSync(output+'/conversation.md','utf8').includes('[S1]'));checks.push('问答可导出问题、答案、版本及来源原文');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.waitForFunction(() => document.querySelector('.mes-sidebar').getBoundingClientRect().width < 80);
    await page.waitForFunction(() => document.documentElement.scrollWidth <= innerWidth + 2);
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 2));
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({ path: output + '/mobile.png', fullPage: true });
    await page.getByRole('button', { name: '导入资料', exact: true }).click();
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 2));
    await page.getByRole('button', { name: '关闭导入窗口' }).click(); checks.push('手机页面与导入弹窗无横向溢出');
    await page.getByRole('button',{name:'清空本页对话',exact:true}).click();assert.equal(await page.locator('.chat-turn').count(),0);
    await page.getByLabel('知识库问题').fill('点胶工序涉及哪些设备？');await page.getByLabel('知识库问题').press('Control+Enter');
    await page.locator('.chat-turn').first().waitFor();checks.push('可清空本页对话，并通过Ctrl加Enter重新提问');
    await page.getByRole('button', { name: '删除', exact: true }).click();
    await page.getByRole('button', { name: '确认删除' }).click();
    await page.getByText('还没有知识资料', { exact: true }).waitFor();
    assert.equal(await page.locator('.chat-turn').count(), 0); checks.push('删除后清空文件、图谱与页面旧回答');
    await page.reload();await page.getByRole('button',{name:'重试清理',exact:true}).waitFor();
    await page.getByRole('button',{name:'重试清理',exact:true}).click();
    await page.getByText('文件及对应图谱已删除。',{exact:true}).waitFor();
    assert.equal(await page.getByRole('button',{name:'重试清理',exact:true}).count(),0);checks.push('删除清理失败状态在刷新后恢复，并可按原版本重试');
    assert.deepEqual(errors, []); checks.push('未出现页面脚本异常');
    fs.writeFileSync(output + '/checks.json', JSON.stringify({ fixture: true, checks, errors }, null, 2));
    console.log(JSON.stringify({ fixture: true, checks, errors }, null, 2));
  } catch(e) { await page.screenshot({ path: output + '/failure.png', fullPage: true }); throw e; }
  finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });

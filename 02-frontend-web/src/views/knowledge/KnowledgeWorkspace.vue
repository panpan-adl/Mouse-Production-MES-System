<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Network, FileText, Plus, Send, RefreshCw, Trash2, Pencil, X, Search, BookOpen, ShieldCheck, Download } from 'lucide-vue-next'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'
import {useAuthStore} from '@/stores/auth'

interface Doc { documentId: string; title: string; version: number; chunkCount: number; filename?:string; status?:string; activeVersion?:number; error?:string; graphStatus?:string }
interface Fact { source: string; target: string; relation: string; quote: string; chunkId: string }
interface Evidence { id: string; documentId: string; title: string; version: number; chunkId: string; text: string; location?:string }
interface Answer { answer: string; mode: string; retrieval: string; sources: Evidence[]; facts: Fact[]; model: string }
interface Status { enabled: boolean; modelConfigured: boolean; model: string; ragProvider: string; neo4j: string }
const status = ref<Status>(), docs = ref<Doc[]>([]), facts = ref<Fact[]>([]), truncated = ref(false)
const busy = ref(false), error = ref(''), notice = ref(''), mode = ref('GRAPHRAG'), question = ref('')
const messages = ref<{ question: string; result: Answer }[]>([]), tab = ref('graph'), filter = ref(''), selected = ref(''), zoom = ref(1)
const documentScope = ref(''), entityQuery = ref(''), modelConnection = ref('未实测')
const focusNode = ref(false), conversation = ref<HTMLElement>()
const editor = ref(false), draft = ref({ documentId: '', title: '', version: 1, text: '' }), editing = ref(false)
const uploadedFile=ref<File>()
const stateLabels:Record<string,string>={READY:'双索引可检索',FAILED:'失败待重试',UPLOADED:'已上传',INDEXING:'索引中'}
const deleting = ref<Doc>(), pendingCleanup = ref<{documentId:string;version:number}[]>([])
const related = computed(() => facts.value.filter(f => f.source === selected.value || f.target === selected.value))
const shownFacts = computed(() => facts.value.filter(f => (!filter.value || `${f.source} ${f.target} ${f.relation}`.toLowerCase().includes(filter.value.toLowerCase())) && (!focusNode.value || !selected.value || f.source === selected.value || f.target === selected.value)))
const nodes = computed(() => [...new Set(shownFacts.value.flatMap(f => [f.source, f.target]))].map((name, i, all) => ({
  name, x: 350 + Math.cos(i * Math.PI * 2 / all.length - Math.PI / 2) * Math.min(260, 100 + all.length * 7),
  y: 240 + Math.sin(i * Math.PI * 2 / all.length - Math.PI / 2) * Math.min(190, 80 + all.length * 5)
})))
const positions = computed(() => new Map(nodes.value.map(n => [n.name, n])))
const edges = computed(() => shownFacts.value.map((f, i) => ({ ...f, key: i, a: positions.value.get(f.source)!, b: positions.value.get(f.target)! })))
async function refreshGraph() {
  const g = await request.get<{ facts: Fact[]; truncated: boolean }>('/knowledge/graph', {
    params: { ...(documentScope.value ? { documentIds: documentScope.value } : {}), entity: entityQuery.value }, timeout: 30000
  })
  facts.value = g.facts; truncated.value = g.truncated
  if (!facts.value.some(f => f.source === selected.value || f.target === selected.value)) selected.value = ''
}
async function changeScope() {
  if (busy.value) return
  busy.value = true; error.value = ''; messages.value = []; filter.value = ''; selected.value = ''; focusNode.value = false
  try { await refreshGraph() } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
async function probeModel() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await request.post('/knowledge/model-probe', {}, { timeout: 65000 }); modelConnection.value = '已实测连接' }
  catch (e: any) { modelConnection.value = '连接失败'; error.value = e.message } finally { busy.value = false }
}
function exportGraph() {
  const data = JSON.stringify({ documentId: documentScope.value || null, entity: entityQuery.value, truncated: truncated.value, facts: facts.value }, null, 2)
  const url = URL.createObjectURL(new Blob([data], { type: 'application/json;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = 'MouseMES-知识图谱.json'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
function exportConversation() {
  const lines = ['# MouseMES 私有知识问答记录', '', '以下内容来自本页实际问答，资料版本与来源随各条回答保留。', '']
  for (const turn of messages.value) {
    lines.push('## 问题', '', turn.question, '', `检索方式：${turn.result.mode} · ${turn.result.retrieval}`, '', turn.result.answer, '', '### 来源', '')
    for (const source of turn.result.sources) lines.push(`[${source.id}] ${source.title} · 版本 ${source.version} · 片段 ${source.chunkId}`, '', source.text, '')
  }
  const url = URL.createObjectURL(new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = 'MouseMES-问答与来源.md'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
async function load(probe = false) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    status.value = await request.get<Status>('/knowledge/status', { params: { probe }, timeout: 30000 })
    if (status.value.enabled) { docs.value = await request.get<Doc[]>('/knowledge/documents'); pendingCleanup.value = await request.get('/knowledge/deletions'); await refreshGraph() }
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
function newDocument() {
  uploadedFile.value=undefined
  editing.value = false; draft.value = { documentId: crypto.randomUUID(), title: '', version: 1, text: '' }; editor.value = true; error.value = ''; notice.value = ''
}
async function editDocument(doc: Doc) {
  uploadedFile.value=undefined
  busy.value = true; error.value = ''
  try {
    const data = await request.get<typeof draft.value>((doc.filename?'/rag/documents/':'/knowledge/documents/') + doc.documentId)
    draft.value = { ...data, version: data.version + 1 }; editing.value = true; editor.value = true
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
async function readFile(event: Event) {
  const input = event.target as HTMLInputElement, file = input.files?.[0]
  if (!file) return
  uploadedFile.value=undefined
  error.value = ''
  try {
    if (!/\.(pdf|docx|md|txt|csv)$/i.test(file.name) || file.size > 2*1024*1024) throw new Error('支持2MB以内的PDF、DOCX、UTF-8 TXT、Markdown、CSV')
    uploadedFile.value=file
    if (/\.(md|txt|csv)$/i.test(file.name)) draft.value.text=await file.text()
    else draft.value.text=''
    if (!draft.value.title) draft.value.title = file.name
  } catch (e: any) { error.value = e.message } finally { input.value = '' }
}
async function save() {
  if (busy.value) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const body=new FormData();body.append('documentId',draft.value.documentId);body.append('version',String(draft.value.version));body.append('title',draft.value.title)
    body.append('file',uploadedFile.value||new File([draft.value.text],draft.value.title+'.md',{type:'text/markdown'}))
    const result = await request.post<{status:string;error?:string}>('/rag/documents',body,{timeout:600000})
    editor.value = false; notice.value=result.status==='READY'?'图谱和向量索引已同步更新。':'原件已保存，索引失败：'+result.error
    if(result.status==='READY')modelConnection.value='已实测连接'
    messages.value = []; docs.value = await request.get<Doc[]>('/knowledge/documents'); await refreshGraph()
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
async function retry(doc:Doc){if(busy.value)return;busy.value=true;error.value='';try{const r=await request.post<{status:string;error?:string}>('/rag/documents/'+doc.documentId+'/retry',null,{params:{version:doc.version},timeout:600000});if(r.status==='FAILED')throw Error(r.error);notice.value='图谱与向量同步完成。';docs.value=await request.get('/knowledge/documents');await refreshGraph()}catch(e:any){error.value=e.message}finally{busy.value=false}}
async function download(doc:Doc){try{let blob:Blob;if(doc.filename)blob=await request.get<Blob>('/rag/documents/'+doc.documentId+'/download',{responseType:'blob'});else{const d=await request.get<{text:string}>('/knowledge/documents/'+doc.documentId);blob=new Blob([d.text],{type:'text/markdown'})}const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=doc.filename||doc.title+'.md';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}catch(e:any){error.value=e.message}}
async function remove(id: string, version: number) {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const result = await request.delete<{cleanupPending:boolean;message?:string}>(`/knowledge/documents/${id}`, { params: { version }, timeout: 60000 })
    pendingCleanup.value = pendingCleanup.value.filter(d => d.documentId !== id)
    if (result.cleanupPending) pendingCleanup.value.push({documentId:id,version})
    notice.value = result.message || '文件及对应图谱已删除。'; deleting.value = undefined; messages.value = []
    docs.value = await request.get<Doc[]>('/knowledge/documents'); if (documentScope.value === id) documentScope.value = ''; await refreshGraph()
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
async function ask() {
  if (busy.value || !question.value.trim()) return
  const text = question.value.trim(); busy.value = true; error.value = ''; notice.value = ''
  try {
    const result = await request.post<Answer>('/knowledge/messages', { question: text, mode: mode.value, documentIds: documentScope.value ? [documentScope.value] : [] }, { timeout: 240000 })
    if(result.model)modelConnection.value='已实测连接'
    messages.value.push({ question: text, result }); question.value = ''; tab.value = 'chat'
    await nextTick(); conversation.value?.scrollTo({ top: conversation.value.scrollHeight, behavior: 'smooth' })
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
const auth=useAuthStore()
watch(()=>auth.currentUser?.id,()=>{docs.value=[];facts.value=[];messages.value=[];pendingCleanup.value=[];draft.value={documentId:'',title:'',version:1,text:''};editor.value=false;uploadedFile.value=undefined})
onMounted(() => load(true))
</script>

<template>
  <MainLayout hide-agent>
    <div class="knowledge-page">
      <header class="knowledge-hero">
        <div class="hero-symbol"><Network :size="30" /></div>
        <div class="hero-copy"><p class="eyebrow">MOUSE MES · KNOWLEDGE LAB</p><h1>制造知识，连接成图</h1><p>从私有资料中发现实体与关系，让每一次回答都有据可查。</p></div>
        <span class="private-tag"><ShieldCheck :size="15" /> 我的私有知识库</span>
      </header>
      <div v-if="error" class="feedback error" role="alert">{{ error }}</div>
      <div v-if="notice" class="feedback success" role="status">{{ notice }}</div>
      <div v-if="status && !status.enabled" class="feedback warning">知识库尚未启用。请按照任务三部署说明配置服务，再点击检测连接。</div>
      <div class="connection-bar"><span :class="{ready:status?.modelConfigured}"><i />{{status?.modelConfigured ? `模型：${status.model}` : '大模型待配置'}} · {{modelConnection}}</span><span>Neo4j：{{status?.neo4j || '未检测'}}</span><button :disabled="busy || !status?.enabled || !status?.modelConfigured" @click="probeModel">测试模型</button><button :disabled="busy" @click="load(true)"><RefreshCw :size="14" />检测连接</button></div>
      <div class="knowledge-grid">
        <aside class="library panel">
          <div class="section-heading"><h2><BookOpen :size="17" />私有资料</h2><span>{{docs.length}} 份</span></div>
          <p class="muted intro">导入工艺、SOP与设备说明，构建属于你的制造知识图谱。</p>
          <button class="primary import-button" :disabled="busy || !status?.enabled" @click="newDocument"><Plus :size="16" />导入资料</button>
          <div v-if="!docs.length" class="empty-library"><FileText :size="30" /><p>还没有知识资料</p><small>支持PDF、DOCX、TXT、Markdown、CSV</small></div>
          <article v-for="doc in docs" :key="doc.documentId" class="document-card"><FileText :size="18" /><div><h3>{{doc.title}}</h3><small>版本 {{doc.version}} · {{doc.chunkCount}} 个片段 · {{doc.status==='READY' && doc.graphStatus==='PENDING' ? '待同步图谱' : stateLabels[doc.status || 'READY'] || doc.status}}</small><p v-if="doc.error" class="document-error">{{doc.error}}</p><div class="doc-actions"><button :disabled="busy" @click="download(doc)"><Download :size="12" />下载</button><button v-if="doc.filename && (doc.status === 'FAILED' || doc.graphStatus === 'PENDING')" :disabled="busy" @click="retry(doc)">同步/重试</button><button :disabled="busy" @click="editDocument(doc)"><Pencil :size="12" />更新</button><button :disabled="busy" @click="deleting=doc"><Trash2 :size="12" />删除</button></div></div></article>
          <div v-for="item in pendingCleanup" :key="item.documentId" class="cleanup"><p>文件已退出检索，图数据库清理待重试。</p><button :disabled="busy" @click="remove(item.documentId,item.version)">重试清理</button></div>
          <footer class="library-foot"><ShieldCheck :size="15" /><span>仅检索当前登录用户的资料；更新与删除会切换可检索版本。</span></footer>
        </aside>
        <main class="workspace panel">
          <div class="workspace-tabs"><button :class="{active:tab==='graph'}" @click="tab='graph'"><Network :size="16" />知识图谱</button><button :class="{active:tab==='chat'}" @click="tab='chat'"><BookOpen :size="16" />有据问答</button><span>{{facts.length}} 条可见关系</span></div>
          <div class="scope-toolbar"><label>资料范围<select v-model="documentScope" :disabled="busy || !status?.enabled" @change="changeScope"><option value="">全部私有资料</option><option v-for="doc in docs.filter(d=>d.activeVersion===undefined || d.activeVersion>0)" :key="doc.documentId" :value="doc.documentId">{{doc.title}}</option></select></label><button v-if="facts.length" @click="exportGraph">导出图谱</button></div>
          <section v-if="tab==='graph'" class="graph-section">
            <form class="entity-search" @submit.prevent="changeScope"><input v-model="entityQuery" maxlength="120" placeholder="从整个图数据库查找实体关联" aria-label="图数据库实体查询" :disabled="busy" /><button :disabled="busy || !status?.enabled">查找关联</button><button v-if="entityQuery" type="button" :disabled="busy" @click="entityQuery='';changeScope()">显示全部</button></form>
            <div class="graph-toolbar"><label><Search :size="15" /><input v-model="filter" placeholder="搜索实体或关系" aria-label="搜索实体或关系" /></label><div><button aria-label="缩小图谱" @click="zoom=Math.max(.4,zoom-.2)">−</button><button @click="zoom=1">重置</button><button aria-label="放大图谱" @click="zoom=Math.min(2.5,zoom+.2)">＋</button></div></div>
            <div v-if="facts.length" class="graph-options"><span>{{nodes.length}} 个可见实体 · {{shownFacts.length}} 条关系</span><label><input v-model="focusNode" type="checkbox" :disabled="!selected" />仅看选中节点的关系</label></div>
            <div v-if="!facts.length" class="graph-empty"><div><Network :size="48" /></div><h2>从一份资料开始连接</h2><p>导入文件后，提取的实体与关系会显示在这里。<br />点击节点，查看关系及其原文依据。</p></div>
            <template v-else>
              <div class="graph-canvas"><svg viewBox="0 0 700 480" role="img" aria-label="私有资料知识图谱"><defs><marker id="kg-arrow" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#9e7a52" /></marker></defs><g :transform="`translate(350 240) scale(${zoom}) translate(-350 -240)`"><g v-for="e in edges" :key="e.key"><line :x1="e.a.x" :y1="e.a.y" :x2="e.b.x" :y2="e.b.y" stroke="#c7aa85" stroke-width="1.5" marker-end="url(#kg-arrow)" /><text :x="(e.a.x+e.b.x)/2" :y="(e.a.y+e.b.y)/2-5" class="edge-label">{{e.relation}}</text></g><g v-for="n in nodes" :key="n.name" class="graph-node" :class="{chosen:selected===n.name}" role="button" tabindex="0" :aria-label="`查看${n.name}的关系`" @click="selected=n.name" @keydown.enter="selected=n.name" @keydown.space.prevent="selected=n.name"><circle :cx="n.x" :cy="n.y" r="24" /><text :x="n.x" :y="n.y+42">{{n.name.length>12?n.name.slice(0,12)+'…':n.name}}</text><title>{{n.name}}</title></g></g></svg></div>
              <p v-if="truncated" class="graph-caption">当前仅显示前150条关系，可按文件拆分演示；问答使用独立的相关子图检索。</p>
              <div v-if="selected" class="node-detail"><div class="section-heading"><h3>{{selected}}</h3><button aria-label="关闭节点详情" @click="selected=''"><X :size="16" /></button></div><article v-for="(f,i) in related" :key="i"><strong>{{f.source}} → {{f.relation}} → {{f.target}}</strong><blockquote>{{f.quote}}</blockquote></article></div>
              <details class="relation-table"><summary>查看关系与原文列表（{{shownFacts.length}}）</summary><article v-for="(f,i) in shownFacts" :key="i"><strong>{{f.source}} → {{f.relation}} → {{f.target}}</strong><p>{{f.quote}}</p></article></details>
            </template>
          </section>
          <section v-else ref="conversation" class="chat-section" aria-live="polite">
            <div v-if="messages.length" class="chat-actions"><span>每次提问独立检索资料</span><button @click="exportConversation">导出问答与来源</button><button :disabled="busy" @click="messages=[];question=''">清空本页对话</button></div>
            <div v-if="!messages.length" class="chat-empty"><h2>沿着关系，找到答案</h2><p>试着询问资料中某个产品涉及的工序，或某台设备关联的检验要求。</p><div class="examples"><button @click="question='A100鼠标需要经过哪些工序？'">A100鼠标需要经过哪些工序？</button><button @click="question='点胶工序涉及哪些设备和检验要求？'">点胶工序涉及哪些设备和检验要求？</button></div></div>
            <article v-for="(turn,i) in messages" :key="i" class="chat-turn"><p class="user-question">{{turn.question}}</p><span class="answer-meta">{{turn.result.mode}} · {{turn.result.retrieval}}</span><p class="answer-text">{{turn.result.answer}}</p><details v-for="source in turn.result.sources" :key="source.id" class="source-card"><summary>[{{source.id}}] {{source.title}} · 版本 {{source.version}} · {{source.location}}</summary><p>{{source.text}}</p><small>片段 {{source.chunkId}}</small></details><details v-if="turn.result.facts.length" class="source-card"><summary>检索到的关系路径</summary><p v-for="(f,j) in turn.result.facts" :key="j">{{f.source}} → {{f.relation}} → {{f.target}}</p></details></article>
          </section>
          <form class="question-form" @submit.prevent="ask"><div class="mode-row"><label>检索方式<select v-model="mode" :disabled="busy"><option value="GRAPHRAG">GraphRAG · 图谱检索</option><option value="RAG">RAG · 向量检索</option><option value="HYBRID">融合检索</option></select></label><span>{{mode==='RAG' ? status?.ragProvider : '实体关联 · 两跳路径 · 来源引用'}}</span></div><div class="question-row"><textarea v-model="question" maxlength="1000" rows="2" placeholder="输入关于私有资料的问题…" aria-label="知识库问题" @keydown.ctrl.enter.prevent="ask" /><button class="primary" :disabled="busy || !question.trim() || !status?.enabled"><Send :size="17" />{{busy?'处理中…':'提问'}}</button></div><small>Ctrl＋Enter 提问 · 本页对话刷新后不保留，可先导出。实时业务请使用业务智能体。</small></form>
        </main>
      </div>
      <div v-if="editor" class="modal-backdrop"><section class="editor panel" role="dialog" aria-modal="true" aria-labelledby="kg-editor-title"><div class="section-heading"><h2 id="kg-editor-title">{{editing?'更新资料':'导入资料'}}</h2><button :disabled="busy" aria-label="关闭导入窗口" @click="editor=false"><X :size="20" /></button></div><p class="muted">发布前会调用大模型提取关系，长文可能需要数分钟；失败时保留原有版本。</p><p v-if="error" class="feedback error" role="alert">{{error}}</p><form @submit.prevent="save"><label>资料标题<input v-model="draft.title" maxlength="200" required :disabled="busy" /></label><label class="file-picker">选择文件（PDF / DOCX / TXT / Markdown / CSV）<input type="file" accept=".pdf,.docx,.txt,.md,.csv" :disabled="busy" @change="readFile" /></label><label>资料内容<textarea v-model="draft.text" maxlength="12000" rows="12" :required="!uploadedFile" :disabled="busy || !!uploadedFile" /></label><div class="editor-footer"><span>{{draft.text.length}} / 12000 字 · 版本 {{draft.version}}</span><button class="primary" :disabled="busy || (!uploadedFile && !draft.text.trim()) || !draft.title.trim()">{{busy?'正在构建双索引…':'保存并构建双索引'}}</button></div></form></section></div>
      <div v-if="deleting" class="modal-backdrop"><section class="confirm panel" role="dialog" aria-modal="true" aria-labelledby="kg-delete-title"><h2 id="kg-delete-title">删除“{{deleting.title}}”？</h2><p>该资料将退出检索，并清理对应图谱关系。</p><div><button :disabled="busy" @click="deleting=undefined">取消</button><button class="danger" :disabled="busy" @click="remove(deleting.documentId,deleting.version+1)">确认删除</button></div></section></div>
    </div>
  </MainLayout>
</template>

<style scoped>
.graph-options,.chat-actions{display:flex;gap:12px;align-items:center;justify-content:space-between;padding:0 22px 14px;font-size:11px;color:#77634f;flex-wrap:wrap}.graph-options label{display:flex;gap:5px;align-items:center}.graph-options input{accent-color:#865f3d}.chat-actions{padding:0 0 18px;justify-content:flex-end}.chat-actions>span{margin-right:auto}.chat-actions button{color:#865f3d}
.scope-toolbar{padding:14px 24px;border-bottom:1px solid #f7f1e6;display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:12px;color:#77634f}.scope-toolbar label{display:flex;align-items:center;gap:10px;min-width:0}.scope-toolbar select{max-width:220px;min-width:0;border:1px solid #e7dccb;border-radius:6px;padding:6px 8px;background:white}.scope-toolbar>button{white-space:nowrap;color:#865f3d}.entity-search{display:flex;align-items:center;gap:10px;padding:16px 22px 0;font-size:12px}.entity-search input{flex:1;min-width:0;border:1px solid #e7dccb;padding:8px 10px;border-radius:6px}.entity-search button{white-space:nowrap;color:#865f3d}.connection-bar button{white-space:nowrap}.connection-bar button+button{margin-left:0}@media(max-width:720px){.scope-toolbar{padding:12px 16px;flex-wrap:wrap}.scope-toolbar select{max-width:160px}.entity-search{padding:12px 15px 0;gap:6px;flex-wrap:wrap}.entity-search input{flex-basis:100%}}
.knowledge-page{max-width:1440px;margin:auto;color:#60462f}.knowledge-hero{display:flex;align-items:center;gap:20px;padding:30px 32px;border:1px solid #e7dccb;border-radius:18px;background:linear-gradient(110deg,#f2e8d6,#fdfaf3 70%);margin-bottom:18px}.hero-symbol{display:grid;place-items:center;flex-shrink:0;width:62px;height:62px;border-radius:18px;background:#865f3d;color:white;box-shadow:0 6px 18px #865f3d25}.eyebrow{font-size:10px;letter-spacing:2px;color:#865f3d;font-weight:700;margin-bottom:5px}.hero-copy h1{font-size:26px;font-weight:700;letter-spacing:1px}.hero-copy>p:last-child{font-size:13px;color:#77634f;margin-top:8px}.private-tag{margin-left:auto;display:flex;align-items:center;gap:6px;white-space:nowrap;font-size:12px;color:#865f3d}.connection-bar{display:flex;gap:24px;align-items:center;padding:8px 3px 18px;font-size:12px;color:#77634f}.connection-bar>span:first-child{display:flex;align-items:center;gap:7px}.connection-bar i{width:7px;height:7px;background:#c29b4f;border-radius:50%}.connection-bar .ready i{background:#b6854e}.connection-bar button{display:flex;align-items:center;gap:5px;margin-left:auto;color:#865f3d}.knowledge-grid{display:grid;grid-template-columns:260px minmax(0,1fr);gap:20px}.panel{background:white;border:1px solid #e7dccb;border-radius:14px}.library{padding:22px 18px;align-self:start}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.section-heading h2{display:flex;align-items:center;gap:8px;font-size:15px;font-weight:700}.section-heading>span{font-size:12px;color:#77634f}.muted{color:#77634f;font-size:12px;line-height:1.8}.intro{margin:14px 0}.primary{display:inline-flex;align-items:center;justify-content:center;gap:7px;background:#865f3d;color:white;border-radius:8px;padding:10px 18px;font-size:13px;font-weight:600}.import-button{width:100%}button:disabled{opacity:.45;cursor:not-allowed}.empty-library{text-align:center;padding:40px 0;color:#9e7a52}.empty-library svg{margin:auto auto 14px}.empty-library p{font-size:13px;color:#77634f}.empty-library small{font-size:11px}.document-card{display:flex;gap:10px;padding:18px 0;border-bottom:1px solid #f7f1e6}.document-card>svg{color:#865f3d;flex-shrink:0;margin-top:3px}.document-card h3{font-size:13px;font-weight:600;overflow-wrap:anywhere}.document-card small{font-size:11px;color:#77634f}.doc-actions{display:flex;gap:16px;margin-top:9px}.doc-actions button{display:flex;align-items:center;gap:4px;font-size:11px;color:#77634f}.library-foot{display:flex;gap:7px;margin-top:26px;color:#77634f;font-size:11px;line-height:1.7}.library-foot svg{flex-shrink:0;margin-top:2px}.workspace{overflow:hidden;min-width:0}.workspace-tabs{display:flex;align-items:center;gap:24px;border-bottom:1px solid #e7dccb;padding:0 24px}.workspace-tabs button{display:flex;gap:7px;align-items:center;font-size:13px;padding:20px 0;border-bottom:2px solid transparent;color:#77634f}.workspace-tabs .active{color:#865f3d;border-color:#865f3d;font-weight:700}.workspace-tabs>span{margin-left:auto;font-size:11px;color:#77634f}.graph-toolbar{display:flex;align-items:center;justify-content:space-between;padding:18px 22px}.graph-toolbar label{display:flex;align-items:center;gap:6px;color:#77634f}.graph-toolbar input{border:0;font-size:12px;max-width:180px;padding:6px;background:transparent}.graph-toolbar button{font-size:12px;background:#f7f1e6;padding:5px 9px;margin-left:4px;border-radius:4px}.graph-canvas{background-color:#fdfaf3;background-image:radial-gradient(#e7dccb .8px,transparent .8px);background-size:20px 20px;overflow:hidden}.graph-canvas svg{width:100%;min-height:300px;max-height:500px}.graph-node{cursor:pointer;outline:none}.graph-node circle{fill:#f2e8d6;stroke:#865f3d;stroke-width:2}.graph-node.chosen circle,.graph-node:focus circle{fill:#865f3d;stroke:#e4d1ad;stroke-width:5}.graph-node text{font-size:11px;fill:#60462f;text-anchor:middle}.edge-label{font-size:9px;fill:#77634f;text-anchor:middle;paint-order:stroke;stroke:#fdfaf3;stroke-width:3px}.graph-empty{min-height:410px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center}.graph-empty>div{display:grid;place-items:center;background:#f2e8d6;color:#b6854e;width:100px;height:100px;border-radius:50%;margin-bottom:22px}.graph-empty h2,.chat-empty h2{font-size:20px;font-weight:600}.graph-empty p,.chat-empty p{color:#77634f;font-size:13px;line-height:1.9;margin-top:12px}.node-detail{margin:18px 22px;padding:16px;background:#f7f1e6;border:1px solid #e7dccb;border-radius:10px}.node-detail h3{font-weight:700;font-size:14px}.node-detail article{font-size:12px;padding-top:12px}.node-detail blockquote{color:#77634f;border-left:2px solid #e4d1ad;padding-left:10px;margin-top:7px;white-space:pre-wrap}.relation-table{margin:18px 22px;font-size:12px}.relation-table summary{cursor:pointer;color:#865f3d}.relation-table article{padding:12px 0;border-bottom:1px solid #f7f1e6}.relation-table p{margin-top:6px;color:#77634f}.question-form{padding:20px 24px;background:#fdfaf3;border-top:1px solid #e7dccb}.mode-row{display:flex;align-items:center;justify-content:space-between;gap:10px;font-size:11px;color:#77634f;margin-bottom:12px}.mode-row label{display:flex;gap:10px;align-items:center;color:#77634f}.mode-row select{background:white;border:1px solid #e7dccb;border-radius:5px;padding:5px;font-size:12px;color:#865f3d;max-width:210px}.question-row{display:flex;gap:12px;align-items:center}.question-row textarea{width:100%;resize:vertical;border:1px solid #e7dccb;border-radius:9px;padding:12px;font-size:13px;line-height:1.6}.question-row button{white-space:nowrap}.question-form>small{font-size:10px;color:#9e7a52;display:block;margin-top:10px}.chat-section{padding:24px;max-height:650px;min-height:420px;overflow:auto}.chat-empty{padding:50px 15px}.examples{display:flex;gap:10px;flex-wrap:wrap;margin-top:25px}.examples button{background:#f7f1e6;border:1px solid #e7dccb;border-radius:8px;padding:12px;color:#865f3d;font-size:12px}.chat-turn{padding:8px 0 24px;margin-bottom:20px;border-bottom:1px solid #e7dccb}.user-question{background:#f2e8d6;border-radius:10px;padding:14px;margin-bottom:18px;white-space:pre-wrap;font-size:14px}.answer-meta{font-size:10px;color:#865f3d}.answer-text{white-space:pre-wrap;font-size:13px;line-height:1.9;margin:12px 0}.source-card{margin:8px 0;border:1px solid #e7dccb;border-radius:6px;padding:10px 12px;font-size:11px}.source-card summary{color:#865f3d;cursor:pointer}.source-card p{white-space:pre-wrap;line-height:1.9;color:#77634f;margin:10px 0;overflow-wrap:anywhere}.source-card small{color:#9e7a52;overflow-wrap:anywhere}.feedback{border-radius:8px;padding:12px 16px;font-size:13px;margin-bottom:14px}.error{background:#fff0ef;color:#b5483e}.success{background:#eaf6f0;color:#267655}.warning{background:#fff8e7;color:#947330}.graph-caption{font-size:11px;color:#a08451;padding:10px 22px}.modal-backdrop{position:fixed;inset:0;z-index:200;background:#46301f66;display:grid;place-items:center;padding:20px}.editor{width:min(720px,100%);max-height:90vh;overflow:auto;padding:26px}.editor>.muted{margin:12px 0}.editor form>label{display:block;margin-top:15px;font-size:13px;color:#77634f}.editor input:not([type=file]),.editor textarea{display:block;width:100%;border:1px solid #e7dccb;border-radius:7px;padding:10px;margin-top:7px;font-size:13px}.file-picker input{display:block;margin-top:8px;font-size:12px}.editor-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:18px}.editor-footer>span{font-size:11px;color:#77634f}.confirm{max-width:440px;padding:26px}.confirm h2{font-size:18px;font-weight:600}.confirm p{font-size:13px;color:#77634f;margin-top:15px}.confirm>div{display:flex;justify-content:flex-end;gap:20px;margin-top:25px}.danger{background:#b84940;color:white;border-radius:7px;padding:8px 15px;font-size:13px}.cleanup{font-size:11px;color:#9b7640;padding:12px;background:#fff8eb;margin-top:12px;border-radius:6px}.cleanup button{color:#865f3d;margin-top:6px}
@media(max-width:1000px){.knowledge-grid{grid-template-columns:220px minmax(0,1fr)}.private-tag{display:none}.mode-row>span{display:none}.knowledge-hero{padding:22px}.hero-copy h1{font-size:22px}}
@media(max-width:720px){.knowledge-grid{grid-template-columns:1fr}.knowledge-hero{gap:12px;padding:20px 16px}.hero-symbol{width:44px;height:44px;border-radius:12px}.hero-copy h1{font-size:19px}.hero-copy>p:last-child{font-size:11px}.eyebrow{font-size:8px}.library{padding:16px}.empty-library{padding:20px}.library-foot{margin-top:15px}.connection-bar{gap:10px;flex-wrap:wrap}.workspace-tabs{padding:0 16px;gap:18px}.workspace-tabs>span{display:none}.question-form{padding:16px}.question-row{gap:8px}.question-row button{padding:12px}.graph-empty{min-height:300px}.graph-toolbar{padding:15px}.chat-section{padding:16px}.modal-backdrop{padding:12px}.editor{padding:18px}.editor-footer{align-items:flex-start;flex-direction:column}.graph-canvas svg{min-height:250px}.examples{display:grid}.chat-empty{padding:30px 0}}
</style>

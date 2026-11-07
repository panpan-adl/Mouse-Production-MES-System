<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { Network, FileText, Plus, Send, RefreshCw, Trash2, Pencil, X, Search, BookOpen, ShieldCheck } from 'lucide-vue-next'
import MainLayout from '@/components/MainLayout.vue'
import PrivateFileLibrary from '@/components/PrivateFileLibrary.vue'
import request from '@/utils/request'

interface Doc { documentId: string; title: string; version: number; chunkCount: number }
interface Fact { source: string; target: string; relation: string; quote: string; chunkId: string }
interface Evidence { id: string; documentId: string; title: string; version: number; chunkId: string; text: string; location?: string }
interface Answer { answer: string; mode: string; retrieval: string; sources: Evidence[]; facts: Fact[]; model: string }
interface Status { enabled: boolean; modelConfigured: boolean; model: string; ragProvider: string; neo4j: string }
const status = ref<Status>(), docs = ref<Doc[]>([]), facts = ref<Fact[]>([]), truncated = ref(false)
const busy = ref(false), error = ref(''), notice = ref(''), mode = ref('GRAPHRAG'), question = ref('')
const messages = ref<{ question: string; result: Answer }[]>([]), tab = ref('graph'), filter = ref(''), selected = ref(''), zoom = ref(1)
const documentScope = ref(''), entityQuery = ref(''), modelConnection = ref('未实测')
const focusNode = ref(false), conversation = ref<HTMLElement>()
const managedFiles=ref<string[]>([]),libraryBusy=ref(false)
async function libraryChanged(ids:string[]){managedFiles.value=ids;messages.value=[];await load()}
const editor = ref(false), draft = ref({ documentId: '', title: '', version: 1, text: '' }), editing = ref(false)
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
    for (const source of turn.result.sources) lines.push(`[${source.id}] ${source.title} · 版本 ${source.version} · ${source.location||'原文片段'} · ${source.chunkId}`, '', source.text, '')
  }
  const url = URL.createObjectURL(new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = 'MouseMES-问答与来源.md'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
async function load(probe = false) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    status.value = await request.get<Status>('/knowledge/status', { params: { probe }, timeout: 30000 })
    if (status.value.enabled) { docs.value = await request.get<Doc[]>('/knowledge/documents'); if(documentScope.value&&!docs.value.some(d=>d.documentId===documentScope.value))documentScope.value=''; pendingCleanup.value = await request.get('/knowledge/deletions'); await refreshGraph() }
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
function newDocument() {
  editing.value = false; draft.value = { documentId: crypto.randomUUID(), title: '', version: 1, text: '' }; editor.value = true; error.value = ''; notice.value = ''
}
async function editDocument(doc: Doc) {
  busy.value = true; error.value = ''
  try {
    const data = await request.get<typeof draft.value>('/knowledge/documents/' + doc.documentId)
    draft.value = { ...data, version: data.version + 1 }; editing.value = true; editor.value = true
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
async function readFile(event: Event) {
  const input = event.target as HTMLInputElement, file = input.files?.[0]
  if (!file) return
  error.value = ''
  try {
    if (!/\.(md|txt)$/i.test(file.name) || file.size > 60000) throw new Error('请选择60KB以内的UTF-8文本或Markdown文件')
    const text = await file.text()
    if (text.includes('\uFFFD')) throw new Error('文件编码无法识别，请另存为UTF-8后导入')
    if (text.length > 12000) throw new Error('单份文本最多12000字，请拆分后导入')
    draft.value.text = text; if (!draft.value.title) draft.value.title = file.name
  } catch (e: any) { error.value = e.message } finally { input.value = '' }
}
async function save() {
  if (busy.value) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const result = await request.post<{relationCount:number;unchanged:boolean}>('/knowledge/documents', draft.value, { timeout: 600000 })
    editor.value = false; notice.value = result.unchanged ? '此版本已存在，未重复构建。' : `图谱已更新，提取 ${result.relationCount} 条关系。`
    messages.value = []; docs.value = await request.get<Doc[]>('/knowledge/documents'); await refreshGraph()
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
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
  if (busy.value || libraryBusy.value || !question.value.trim()) return
  const text = question.value.trim(); busy.value = true; error.value = ''
  try {
    const result = await request.post<Answer>('/knowledge/messages', { question: text, mode: mode.value, documentIds: documentScope.value ? [documentScope.value] : [] }, { timeout: 240000 })
    messages.value.push({ question: text, result }); question.value = ''; tab.value = 'chat'
    await nextTick(); conversation.value?.scrollTo({ top: conversation.value.scrollHeight, behavior: 'smooth' })
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
onMounted(() => load())
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
      <PrivateFileLibrary v-if="status?.enabled" :disabled="busy" @managed="managedFiles=$event" @working="libraryBusy=$event" @changed="libraryChanged" />
      <div class="knowledge-grid">
        <aside class="library panel">
          <div class="section-heading"><h2><BookOpen :size="17" />私有资料</h2><span>{{docs.length}} 份</span></div>
          <p class="muted intro">导入工艺、SOP与设备说明，构建属于你的制造知识图谱。</p>
          <button class="primary import-button" :disabled="busy || libraryBusy || !status?.enabled" @click="newDocument"><Plus :size="16" />导入资料</button>
          <div v-if="!docs.length" class="empty-library"><FileText :size="30" /><p>还没有知识资料</p><small>支持文本和 Markdown</small></div>
          <article v-for="doc in docs" :key="doc.documentId" class="document-card"><FileText :size="18" /><div><h3>{{doc.title}}</h3><small>版本 {{doc.version}} · {{doc.chunkCount}} 个片段</small><small v-if="managedFiles.includes(doc.documentId)" class="block">通过上方文件库管理原件及索引</small><div v-else class="doc-actions"><button :disabled="busy||libraryBusy" @click="editDocument(doc)"><Pencil :size="12" />更新</button><button :disabled="busy||libraryBusy" @click="deleting=doc"><Trash2 :size="12" />删除</button></div></div></article>
          <div v-for="item in pendingCleanup" :key="item.documentId" class="cleanup"><p>文件已退出检索，图数据库清理待重试。</p><button :disabled="busy" @click="remove(item.documentId,item.version)">重试清理</button></div>
          <footer class="library-foot"><ShieldCheck :size="15" /><span>仅检索当前登录用户的资料；更新与删除会切换可检索版本。</span></footer>
        </aside>
        <main class="workspace panel">
          <div class="workspace-tabs"><button :class="{active:tab==='graph'}" @click="tab='graph'"><Network :size="16" />知识图谱</button><button :class="{active:tab==='chat'}" @click="tab='chat'"><BookOpen :size="16" />有据问答</button><span>{{facts.length}} 条可见关系</span></div>
          <div class="scope-toolbar"><label>资料范围<select v-model="documentScope" :disabled="busy || !status?.enabled" @change="changeScope"><option value="">全部私有资料</option><option v-for="doc in docs" :key="doc.documentId" :value="doc.documentId">{{doc.title}}</option></select></label><button v-if="facts.length" @click="exportGraph">导出图谱</button></div>
          <section v-if="tab==='graph'" class="graph-section">
            <form class="entity-search" @submit.prevent="changeScope"><input v-model="entityQuery" maxlength="120" placeholder="从整个图数据库查找实体关联" aria-label="图数据库实体查询" :disabled="busy" /><button :disabled="busy || !status?.enabled">查找关联</button><button v-if="entityQuery" type="button" :disabled="busy" @click="entityQuery='';changeScope()">显示全部</button></form>
            <div class="graph-toolbar"><label><Search :size="15" /><input v-model="filter" placeholder="搜索实体或关系" aria-label="搜索实体或关系" /></label><div><button aria-label="缩小图谱" @click="zoom=Math.max(.4,zoom-.2)">−</button><button @click="zoom=1">重置</button><button aria-label="放大图谱" @click="zoom=Math.min(2.5,zoom+.2)">＋</button></div></div>
            <div v-if="facts.length" class="graph-options"><span>{{nodes.length}} 个可见实体 · {{shownFacts.length}} 条关系</span><label><input v-model="focusNode" type="checkbox" :disabled="!selected" />仅看选中节点的关系</label></div>
            <div v-if="!facts.length" class="graph-empty"><div><Network :size="48" /></div><h2>从一份资料开始连接</h2><p>导入文件后，提取的实体与关系会显示在这里。<br />点击节点，查看关系及其原文依据。</p></div>
            <template v-else>
              <div class="graph-canvas"><svg viewBox="0 0 700 480" role="img" aria-label="私有资料知识图谱"><defs><marker id="kg-arrow" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#91a8bd" /></marker></defs><g :transform="`translate(350 240) scale(${zoom}) translate(-350 -240)`"><g v-for="e in edges" :key="e.key"><line :x1="e.a.x" :y1="e.a.y" :x2="e.b.x" :y2="e.b.y" stroke="#b6c9d9" stroke-width="1.5" marker-end="url(#kg-arrow)" /><text :x="(e.a.x+e.b.x)/2" :y="(e.a.y+e.b.y)/2-5" class="edge-label">{{e.relation}}</text></g><g v-for="n in nodes" :key="n.name" class="graph-node" :class="{chosen:selected===n.name}" role="button" tabindex="0" :aria-label="`查看${n.name}的关系`" @click="selected=n.name" @keydown.enter="selected=n.name" @keydown.space.prevent="selected=n.name"><circle :cx="n.x" :cy="n.y" r="24" /><text :x="n.x" :y="n.y+42">{{n.name.length>12?n.name.slice(0,12)+'…':n.name}}</text><title>{{n.name}}</title></g></g></svg></div>
              <p v-if="truncated" class="graph-caption">当前仅显示前150条关系，可按文件拆分演示；问答使用独立的相关子图检索。</p>
              <div v-if="selected" class="node-detail"><div class="section-heading"><h3>{{selected}}</h3><button aria-label="关闭节点详情" @click="selected=''"><X :size="16" /></button></div><article v-for="(f,i) in related" :key="i"><strong>{{f.source}} → {{f.relation}} → {{f.target}}</strong><blockquote>{{f.quote}}</blockquote></article></div>
              <details class="relation-table"><summary>查看关系与原文列表（{{shownFacts.length}}）</summary><article v-for="(f,i) in shownFacts" :key="i"><strong>{{f.source}} → {{f.relation}} → {{f.target}}</strong><p>{{f.quote}}</p></article></details>
            </template>
          </section>
          <section v-else ref="conversation" class="chat-section" aria-live="polite">
            <div v-if="messages.length" class="chat-actions"><span>每次提问独立检索资料</span><button @click="exportConversation">导出问答与来源</button><button :disabled="busy" @click="messages=[];question=''">清空本页对话</button></div>
            <div v-if="!messages.length" class="chat-empty"><h2>沿着关系，找到答案</h2><p>试着询问资料中某个产品涉及的工序，或某台设备关联的检验要求。</p><div class="examples"><button @click="question='A100鼠标需要经过哪些工序？'">A100鼠标需要经过哪些工序？</button><button @click="question='点胶工序涉及哪些设备和检验要求？'">点胶工序涉及哪些设备和检验要求？</button></div></div>
            <article v-for="(turn,i) in messages" :key="i" class="chat-turn"><p class="user-question">{{turn.question}}</p><span class="answer-meta">{{turn.result.mode}} · {{turn.result.retrieval}}</span><p class="answer-text">{{turn.result.answer}}</p><details v-for="source in turn.result.sources" :key="source.id" class="source-card"><summary>[{{source.id}}] {{source.title}} · 版本 {{source.version}} · {{source.location||'原文片段'}}</summary><p>{{source.text}}</p><small>片段 {{source.chunkId}}</small></details><details v-if="turn.result.facts.length" class="source-card"><summary>检索到的关系路径</summary><p v-for="(f,j) in turn.result.facts" :key="j">{{f.source}} → {{f.relation}} → {{f.target}}</p></details></article>
          </section>
          <form class="question-form" @submit.prevent="ask"><div class="mode-row"><label>检索方式<select v-model="mode" :disabled="busy"><option value="GRAPHRAG">GraphRAG · 图谱检索</option><option value="RAG">RAG · 文本检索</option><option value="HYBRID">融合检索</option></select></label><span>{{mode==='RAG' ? status?.ragProvider : '实体关联 · 两跳路径 · 来源引用'}}</span></div><div class="question-row"><textarea v-model="question" maxlength="1000" rows="2" placeholder="输入关于私有资料的问题…" aria-label="知识库问题" @keydown.ctrl.enter.prevent="ask" /><button class="primary" :disabled="busy || !question.trim() || !status?.enabled"><Send :size="17" />{{busy?'处理中…':'提问'}}</button></div><small>Ctrl＋Enter 提问 · 本页对话刷新后不保留，可先导出。实时业务请使用业务智能体。</small></form>
        </main>
      </div>
      <div v-if="editor" class="modal-backdrop"><section class="editor panel" role="dialog" aria-modal="true" aria-labelledby="kg-editor-title"><div class="section-heading"><h2 id="kg-editor-title">{{editing?'更新资料':'导入资料'}}</h2><button :disabled="busy" aria-label="关闭导入窗口" @click="editor=false"><X :size="20" /></button></div><p class="muted">发布前会调用大模型提取关系，长文可能需要数分钟；失败时保留原有版本。</p><p v-if="error" class="feedback error" role="alert">{{error}}</p><form @submit.prevent="save"><label>资料标题<input v-model="draft.title" maxlength="200" required :disabled="busy" /></label><label class="file-picker">读取文本或Markdown<input type="file" accept=".txt,.md" :disabled="busy" @change="readFile" /></label><label>资料内容<textarea v-model="draft.text" maxlength="12000" rows="12" required :disabled="busy" /></label><div class="editor-footer"><span>{{draft.text.length}} / 12000 字 · 版本 {{draft.version}}</span><button class="primary" :disabled="busy || !draft.text.trim() || !draft.title.trim()">{{busy?'正在构建图谱…':'保存并构建图谱'}}</button></div></form></section></div>
      <div v-if="deleting" class="modal-backdrop"><section class="confirm panel" role="dialog" aria-modal="true" aria-labelledby="kg-delete-title"><h2 id="kg-delete-title">删除“{{deleting.title}}”？</h2><p>该资料将退出检索，并清理对应图谱关系。</p><div><button :disabled="busy" @click="deleting=undefined">取消</button><button class="danger" :disabled="busy" @click="remove(deleting.documentId,deleting.version+1)">确认删除</button></div></section></div>
    </div>
  </MainLayout>
</template>

<style scoped>
.graph-options,.chat-actions{display:flex;gap:12px;align-items:center;justify-content:space-between;padding:0 22px 14px;font-size:11px;color:#78909f;flex-wrap:wrap}.graph-options label{display:flex;gap:5px;align-items:center}.graph-options input{accent-color:#0b716d}.chat-actions{padding:0 0 18px;justify-content:flex-end}.chat-actions>span{margin-right:auto}.chat-actions button{color:#0b716d}
.scope-toolbar{padding:14px 24px;border-bottom:1px solid #edf2f4;display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:12px;color:#6a8392}.scope-toolbar label{display:flex;align-items:center;gap:10px;min-width:0}.scope-toolbar select{max-width:220px;min-width:0;border:1px solid #dce7e9;border-radius:6px;padding:6px 8px;background:white}.scope-toolbar>button{white-space:nowrap;color:#0b716d}.entity-search{display:flex;align-items:center;gap:10px;padding:16px 22px 0;font-size:12px}.entity-search input{flex:1;min-width:0;border:1px solid #dce7e9;padding:8px 10px;border-radius:6px}.entity-search button{white-space:nowrap;color:#0b716d}.connection-bar button{white-space:nowrap}.connection-bar button+button{margin-left:0}@media(max-width:720px){.scope-toolbar{padding:12px 16px;flex-wrap:wrap}.scope-toolbar select{max-width:160px}.entity-search{padding:12px 15px 0;gap:6px;flex-wrap:wrap}.entity-search input{flex-basis:100%}}
.knowledge-page{max-width:1440px;margin:auto;color:#21384c}.knowledge-hero{display:flex;align-items:center;gap:20px;padding:30px 32px;border:1px solid #d8e8e9;border-radius:18px;background:linear-gradient(110deg,#eef7f6,#f7fafc 70%);margin-bottom:18px}.hero-symbol{display:grid;place-items:center;flex-shrink:0;width:62px;height:62px;border-radius:18px;background:#0b716d;color:white;box-shadow:0 6px 18px #0b716d25}.eyebrow{font-size:10px;letter-spacing:2px;color:#487b7b;font-weight:700;margin-bottom:5px}.hero-copy h1{font-size:26px;font-weight:700;letter-spacing:1px}.hero-copy>p:last-child{font-size:13px;color:#67818c;margin-top:8px}.private-tag{margin-left:auto;display:flex;align-items:center;gap:6px;white-space:nowrap;font-size:12px;color:#0b716d}.connection-bar{display:flex;gap:24px;align-items:center;padding:8px 3px 18px;font-size:12px;color:#718594}.connection-bar>span:first-child{display:flex;align-items:center;gap:7px}.connection-bar i{width:7px;height:7px;background:#c29b4f;border-radius:50%}.connection-bar .ready i{background:#149482}.connection-bar button{display:flex;align-items:center;gap:5px;margin-left:auto;color:#0b716d}.knowledge-grid{display:grid;grid-template-columns:260px minmax(0,1fr);gap:20px}.panel{background:white;border:1px solid #e1e8ee;border-radius:14px}.library{padding:22px 18px;align-self:start}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.section-heading h2{display:flex;align-items:center;gap:8px;font-size:15px;font-weight:700}.section-heading>span{font-size:12px;color:#81929e}.muted{color:#7b8c98;font-size:12px;line-height:1.8}.intro{margin:14px 0}.primary{display:inline-flex;align-items:center;justify-content:center;gap:7px;background:#0b716d;color:white;border-radius:8px;padding:10px 18px;font-size:13px;font-weight:600}.import-button{width:100%}button:disabled{opacity:.45;cursor:not-allowed}.empty-library{text-align:center;padding:40px 0;color:#a4b3bf}.empty-library svg{margin:auto auto 14px}.empty-library p{font-size:13px;color:#738899}.empty-library small{font-size:11px}.document-card{display:flex;gap:10px;padding:18px 0;border-bottom:1px solid #eef2f5}.document-card>svg{color:#438b88;flex-shrink:0;margin-top:3px}.document-card h3{font-size:13px;font-weight:600;overflow-wrap:anywhere}.document-card small{font-size:11px;color:#8095a4}.doc-actions{display:flex;gap:16px;margin-top:9px}.doc-actions button{display:flex;align-items:center;gap:4px;font-size:11px;color:#6b8293}.library-foot{display:flex;gap:7px;margin-top:26px;color:#8ba0aa;font-size:11px;line-height:1.7}.library-foot svg{flex-shrink:0;margin-top:2px}.workspace{overflow:hidden;min-width:0}.workspace-tabs{display:flex;align-items:center;gap:24px;border-bottom:1px solid #e8eef2;padding:0 24px}.workspace-tabs button{display:flex;gap:7px;align-items:center;font-size:13px;padding:20px 0;border-bottom:2px solid transparent;color:#788d9c}.workspace-tabs .active{color:#0b716d;border-color:#0b716d;font-weight:700}.workspace-tabs>span{margin-left:auto;font-size:11px;color:#8a9da9}.graph-toolbar{display:flex;align-items:center;justify-content:space-between;padding:18px 22px}.graph-toolbar label{display:flex;align-items:center;gap:6px;color:#8a9da9}.graph-toolbar input{border:0;font-size:12px;max-width:180px;padding:6px;background:transparent}.graph-toolbar button{font-size:12px;background:#f4f7f9;padding:5px 9px;margin-left:4px;border-radius:4px}.graph-canvas{background-color:#f9fcfc;background-image:radial-gradient(#cddde2 .8px,transparent .8px);background-size:20px 20px;overflow:hidden}.graph-canvas svg{width:100%;min-height:300px;max-height:500px}.graph-node{cursor:pointer;outline:none}.graph-node circle{fill:#e5f2f0;stroke:#3b938c;stroke-width:2}.graph-node.chosen circle,.graph-node:focus circle{fill:#0b716d;stroke:#86d3c9;stroke-width:5}.graph-node text{font-size:11px;fill:#33566b;text-anchor:middle}.edge-label{font-size:9px;fill:#6c8896;text-anchor:middle;paint-order:stroke;stroke:#f9fcfc;stroke-width:3px}.graph-empty{min-height:410px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center}.graph-empty>div{display:grid;place-items:center;background:#eff7f6;color:#69aaa3;width:100px;height:100px;border-radius:50%;margin-bottom:22px}.graph-empty h2,.chat-empty h2{font-size:20px;font-weight:600}.graph-empty p,.chat-empty p{color:#8b9daa;font-size:13px;line-height:1.9;margin-top:12px}.node-detail{margin:18px 22px;padding:16px;background:#f2f8f7;border:1px solid #d9eae7;border-radius:10px}.node-detail h3{font-weight:700;font-size:14px}.node-detail article{font-size:12px;padding-top:12px}.node-detail blockquote{color:#6c8493;border-left:2px solid #98c6bf;padding-left:10px;margin-top:7px;white-space:pre-wrap}.relation-table{margin:18px 22px;font-size:12px}.relation-table summary{cursor:pointer;color:#0b716d}.relation-table article{padding:12px 0;border-bottom:1px solid #edf2f4}.relation-table p{margin-top:6px;color:#7a8d9b}.question-form{padding:20px 24px;background:#fbfcfd;border-top:1px solid #e5edf1}.mode-row{display:flex;align-items:center;justify-content:space-between;gap:10px;font-size:11px;color:#81939f;margin-bottom:12px}.mode-row label{display:flex;gap:10px;align-items:center;color:#486676}.mode-row select{background:white;border:1px solid #dce7e9;border-radius:5px;padding:5px;font-size:12px;color:#0b716d;max-width:210px}.question-row{display:flex;gap:12px;align-items:center}.question-row textarea{width:100%;resize:vertical;border:1px solid #dce6eb;border-radius:9px;padding:12px;font-size:13px;line-height:1.6}.question-row button{white-space:nowrap}.question-form>small{font-size:10px;color:#97a6af;display:block;margin-top:10px}.chat-section{padding:24px;max-height:650px;min-height:420px;overflow:auto}.chat-empty{padding:50px 15px}.examples{display:flex;gap:10px;flex-wrap:wrap;margin-top:25px}.examples button{background:#f1f7f6;border:1px solid #dfebe8;border-radius:8px;padding:12px;color:#3b7974;font-size:12px}.chat-turn{padding:8px 0 24px;margin-bottom:20px;border-bottom:1px solid #e9eff3}.user-question{background:#edf5f5;border-radius:10px;padding:14px;margin-bottom:18px;white-space:pre-wrap;font-size:14px}.answer-meta{font-size:10px;color:#52867d}.answer-text{white-space:pre-wrap;font-size:13px;line-height:1.9;margin:12px 0}.source-card{margin:8px 0;border:1px solid #e2eaf0;border-radius:6px;padding:10px 12px;font-size:11px}.source-card summary{color:#387e7b;cursor:pointer}.source-card p{white-space:pre-wrap;line-height:1.9;color:#6e8491;margin:10px 0;overflow-wrap:anywhere}.source-card small{color:#a0adb4;overflow-wrap:anywhere}.feedback{border-radius:8px;padding:12px 16px;font-size:13px;margin-bottom:14px}.error{background:#fff0ef;color:#b5483e}.success{background:#eaf6f0;color:#267655}.warning{background:#fff8e7;color:#947330}.graph-caption{font-size:11px;color:#a08451;padding:10px 22px}.modal-backdrop{position:fixed;inset:0;z-index:200;background:#172f4566;display:grid;place-items:center;padding:20px}.editor{width:min(720px,100%);max-height:90vh;overflow:auto;padding:26px}.editor>.muted{margin:12px 0}.editor form>label{display:block;margin-top:15px;font-size:13px;color:#486475}.editor input:not([type=file]),.editor textarea{display:block;width:100%;border:1px solid #dce5eb;border-radius:7px;padding:10px;margin-top:7px;font-size:13px}.file-picker input{display:block;margin-top:8px;font-size:12px}.editor-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:18px}.editor-footer>span{font-size:11px;color:#8b9ca8}.confirm{max-width:440px;padding:26px}.confirm h2{font-size:18px;font-weight:600}.confirm p{font-size:13px;color:#718795;margin-top:15px}.confirm>div{display:flex;justify-content:flex-end;gap:20px;margin-top:25px}.danger{background:#b84940;color:white;border-radius:7px;padding:8px 15px;font-size:13px}.cleanup{font-size:11px;color:#9b7640;padding:12px;background:#fff8eb;margin-top:12px;border-radius:6px}.cleanup button{color:#0b716d;margin-top:6px}
@media(max-width:1000px){.knowledge-grid{grid-template-columns:220px minmax(0,1fr)}.private-tag{display:none}.mode-row>span{display:none}.knowledge-hero{padding:22px}.hero-copy h1{font-size:22px}}
@media(max-width:720px){.knowledge-grid{grid-template-columns:1fr}.knowledge-hero{gap:12px;padding:20px 16px}.hero-symbol{width:44px;height:44px;border-radius:12px}.hero-copy h1{font-size:19px}.hero-copy>p:last-child{font-size:11px}.eyebrow{font-size:8px}.library{padding:16px}.empty-library{padding:20px}.library-foot{margin-top:15px}.connection-bar{gap:10px;flex-wrap:wrap}.workspace-tabs{padding:0 16px;gap:18px}.workspace-tabs>span{display:none}.question-form{padding:16px}.question-row{gap:8px}.question-row button{padding:12px}.graph-empty{min-height:300px}.graph-toolbar{padding:15px}.chat-section{padding:16px}.modal-backdrop{padding:12px}.editor{padding:18px}.editor-footer{align-items:flex-start;flex-direction:column}.graph-canvas svg{min-height:250px}.examples{display:grid}.chat-empty{padding:30px 0}}
</style>

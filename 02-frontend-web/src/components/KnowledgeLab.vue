<script setup lang="ts">
import {computed,ref,watch} from 'vue'
import request from '@/utils/request'
import KnowledgeGraphCanvas from './KnowledgeGraphCanvas.vue'
import {evaluationFiles,evaluationCases,type EvalCase} from '@/data/knowledgeEvaluation'
interface Fact {source:string;relation:string;target:string;quote:string;chunkId:string}
interface Source {id:string;documentId:string;title:string;version:number;chunkId:string;text:string;location?:string}
interface Answer {answer:string;sources:Source[];facts:Fact[];mode:string;model:string;retrieval:string}
interface Doc {documentId:string;title:string;version:number}
interface Quality {entities:{name:string;type:string;suggestedType:string}[];aliases:{alias:string;canonical:string}[];suggestions:{alias:string;canonical:string;reason:string}[];candidateLimit:number}
const props=defineProps<{documents:Doc[];disabled:boolean}>()
const emit=defineEmits<{working:[value:boolean];changed:[];types:[value:Record<string,string>]}>()
const tab=ref('compare'),busy=ref(false),error=ref(''),notice=ref(''),scope=ref(''),question=ref('')
const tabs=[['compare','同题对比'],['diff','版本差异'],['association','MES知识关联'],['quality','实体审核'],['evaluation','检索评估']]
const types:Record<string,string>={EQUIPMENT:'设备',PROCESS:'工序',PRODUCT:'产品',PERSON:'人员',INSPECTION:'检验',OTHER:'其他'}
const comparison=ref<{question:string;versions:{documentId:string;version:number}[];results:{mode:string;success:boolean;milliseconds:number;result?:Answer;error?:string}[]}>()
const scopeIds=()=>scope.value?[scope.value]:[]
async function run(action:()=>Promise<void>){if(busy.value||props.disabled)return;busy.value=true;emit('working',true);error.value='';notice.value='';try{await action()}catch(e:any){error.value=e.message||'操作失败'}finally{busy.value=false;emit('working',false)}}
function download(name:string,data:unknown){const url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json;charset=utf-8'})),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
async function compare(){await run(async()=>{if(!question.value.trim())throw Error('请填写同一个对比问题');comparison.value=undefined;comparison.value=await request.post('/knowledge/lab/compare',{question:question.value,documentIds:scopeIds()},{timeout:800000})})}
const diffId=ref(''),versions=ref<{version:number;title:string}[]>([]),before=ref(0),after=ref(0),difference=ref<{before:number;after:number;added:Fact[];removed:Fact[];retained:Fact[]}>(),diffView=ref('all'),selectedFact=ref<Fact>()
const diffFacts=computed(()=>{const d=difference.value;if(!d)return [];return diffView.value==='added'?d.added:diffView.value==='removed'?d.removed:diffView.value==='retained'?d.retained:[...d.retained,...d.removed,...d.added]})
async function loadVersions(){if(!diffId.value){versions.value=[];difference.value=undefined;return}await run(async()=>{difference.value=undefined;versions.value=await request.get('/knowledge/lab/versions/'+diffId.value);after.value=versions.value[0]?.version||0;before.value=versions.value[1]?.version||0})}
async function diff(){await run(async()=>{selectedFact.value=undefined;difference.value=await request.get('/knowledge/lab/diff/'+diffId.value,{params:{before:before.value,after:after.value}})})}
const quality=ref<Quality>(),entity=ref(''),entityName=ref(''),entityType=ref('OTHER'),canonical=ref(''),association=ref<{entity:string;notice:string;groups:Record<string,string[]>;graph:{facts:Fact[];sources:Source[];truncated:boolean}}>()
const approvedTypes=computed(()=>Object.fromEntries((quality.value?.entities||[]).map(e=>[e.name,e.type])))
async function refreshQuality(){quality.value=await request.get<Quality>('/knowledge/lab/quality',{params:{documentIds:scopeIds()}});emit('types',approvedTypes.value)}
async function loadQuality(){await run(refreshQuality)}
async function saveType(){await run(async()=>{await request.post('/knowledge/lab/review',{name:entityName.value,type:entityType.value});await refreshQuality();notice.value='实体类型已确认'})}
async function alias(alias:string,target:string){await run(async()=>{await request.post('/knowledge/lab/review',{name:alias,canonical:target});await refreshQuality();notice.value=target?'别名称呼已确认并用于图谱检索，原始关系和引用保留':'已撤销别名称呼'})}
async function associate(){await run(async()=>{association.value=await request.get('/knowledge/lab/association',{params:{entity:entity.value,documentIds:scopeIds()}})})}
watch(()=>props.documents.map(d=>d.documentId+':'+d.version).join(','),()=>{comparison.value=undefined;difference.value=undefined;association.value=undefined;quality.value=undefined;versions.value=[]})
watch(scope,()=>{comparison.value=undefined;quality.value=undefined;association.value=undefined})
const evalSelected=ref(['Q01','Q08']),evalModes=ref(['GRAPHRAG']),evalRows=ref<any[]>([]),stop=ref(false),evalRunning=ref(false),evalManifest=ref<Doc[]>([]),manual=ref<Record<string,boolean>>({})
const evalDocs=computed(()=>evaluationFiles.map(f=>props.documents.find(d=>d.documentId===f.id)).filter((d):d is Doc=>!!d))
const evalPhase=ref(0)
watch(evalPhase,phase=>{evalSelected.value=phase===2?['Q17','Q18']:['Q01','Q08']})
const v2=computed(()=>evalPhase.value)
const equipmentVersion=computed(()=>evalDocs.value.find(d=>d.documentId==='course-eval-equipment')?.version||0)
async function loadEvalPhase(){try{const doc=await request.get<{text:string}>('/rag/documents/course-eval-equipment');evalPhase.value=doc.text.includes('赵师傅')?2:doc.text.includes('张师傅')?1:0}catch{evalPhase.value=0}}
watch(tab,value=>{if(value==='evaluation')loadEvalPhase()})
const evalSummary=computed(()=>{
 const good=evalRows.value.filter(r=>r.success);return {attempted:evalRows.value.length,succeeded:good.length,errors:evalRows.value.length-good.length,termCoverage:good.length?good.reduce((s,r)=>s+r.termCoverage,0)/good.length:0,sourceRecall:good.filter(r=>r.expectedFiles.length).length?good.filter(r=>r.expectedFiles.length).reduce((s,r)=>s+r.sourceRecall,0)/good.filter(r=>r.expectedFiles.length).length:0,citationsValid:good.filter(r=>r.citationsValid).length,manualReviewed:Object.values(manual.value).filter(Boolean).length}
})
async function importEvaluation(update=false){await run(async()=>{
 const raw=await request.get<{documentId:string;version:number;status:string;activeVersion:number;title:string}[]>('/rag/documents')
 const file=raw.find(d=>d.documentId==='course-eval-equipment')
 const events=await request.get<{documentId:string;version:number}[]>('/rag/events')
 await loadEvalPhase()
 if(update&&(!file||v2.value!==1||file.status!=='READY'))throw Error('更新题需要先成功导入第一版评估样例，且当前为第一阶段资料')
 for(const f of evaluationFiles){
  const existing=raw.find(d=>d.documentId===f.id);if(existing&&!existing.title?.startsWith('评估样例：'))throw Error('样例编号已由其他资料使用，请先核对原件');if(update&&f.key!=='equipment')continue
  if(!update&&existing){if(existing.status!=='READY')throw Error('评估样例有失败版本，请在上方文件库重试');continue}
  const data=new FormData();data.append('documentId',f.id);data.append('version',String(Math.max(existing?.version||0,...events.filter(e=>e.documentId===f.id).map(e=>e.version),0)+1));data.append('title',f.title)
  data.append('file',new File([update?f.text.replace('张师傅','赵师傅'):f.text],f.key+'.txt',{type:'text/plain'}))
  const r=await request.post<{status:string;graphStatus:string;error?:string}>('/rag/documents',data,{timeout:1000000});if(r.status!=='READY'||r.graphStatus!=='READY')throw Error(r.error||'评估样例未共同发布')
 }
 await loadEvalPhase();emit('changed');notice.value=update?'设备负责人已更新为赵师傅，选择Q17、Q18检验新版本':'评估样例已导入。请选择少量问题开始，真实运行会调用模型'
})}
function available(c:EvalCase){return c.version===2?v2.value===2:v2.value===1}
async function evaluate(){await run(async()=>{
 if(evalDocs.value.length!==3)throw Error('请先导入三份专用评估样例')
 await loadEvalPhase()
 const cases=evaluationCases.filter(c=>evalSelected.value.includes(c.id));if(!cases.length||!evalModes.value.length)throw Error('请选择问题和模式')
 if(cases.some(c=>!available(c)))throw Error('题目版本与评估样例不符，第一版使用Q01—Q16，第二版使用Q17—Q18')
 evalManifest.value=evalDocs.value.map(d=>({...d}));evalRows.value=[];manual.value={};stop.value=false;evalRunning.value=true
 try{for(const c of cases){for(const mode of evalModes.value){
  if(stop.value)return
  const started=performance.now(),base={caseId:c.id,question:c.question,mode,expectedFiles:c.files,terms:c.terms,refuse:c.refuse||false,version:c.version}
  try{
   const now=await request.get<Doc[]>('/knowledge/documents');if(evalManifest.value.some(d=>!now.some(n=>n.documentId===d.documentId&&n.version===d.version)))throw Error('评估资料版本改变，请重新开始')
   const answer=await request.post<Answer>('/knowledge/messages',{question:c.question,mode,documentIds:evalManifest.value.map(d=>d.documentId)},{timeout:240000})
   const text=answer.answer.toLowerCase(),refs=[...answer.answer.matchAll(/\[(S\d+)\]/g)].map(m=>m[1]),allowed=new Set(answer.sources.map(s=>s.id))
   const expected=c.files.map(k=>evaluationFiles.find(f=>f.key===k)!.id),actual=new Set(answer.sources.map(s=>s.documentId)),refused=answer.answer==='当前资料不足，无法回答该问题。'
   const coverage=c.refuse?Number(refused):c.terms.length?c.terms.filter(group=>group.some(t=>text.includes(t.toLowerCase()))).length/c.terms.length:1
   evalRows.value.push({...base,success:true,milliseconds:Math.round(performance.now()-started),result:answer,termCoverage:coverage,sourceRecall:expected.length?expected.filter(id=>actual.has(id)).length/expected.length:0,citationsValid:refused||(refs.length>0&&refs.every(r=>allowed.has(r))),refused})
  }catch(e:any){evalRows.value.push({...base,success:false,error:e.message,milliseconds:Math.round(performance.now()-started)});stop.value=true;return}
 }} }finally{evalRunning.value=false}
})}
function exportEvaluation(){download('MouseMES-检索评估.json',{generatedAt:new Date().toISOString(),manifest:evalManifest.value,summary:evalSummary.value,rows:evalRows.value,manual:manual.value,limitations:'关键词覆盖和来源召回是自动代理指标，语义正确性需人工复核；中断与失败均保留。'})}
</script>
<template>
<section class="knowledge-lab" aria-label="知识实验与关联分析">
 <header><div><h2>知识实验与关联分析</h2><p>对比检索效果，查看版本变化，核对制造资料中的关联。</p></div><select v-model="scope" aria-label="实验资料范围" :disabled="busy||disabled"><option value="">全部私有资料</option><option v-for="d in documents" :value="d.documentId" :key="d.documentId">{{d.title}}</option></select></header>
 <nav><button v-for="item in tabs" :key="item[0]" :class="{active:tab===item[0]}" :disabled="busy||disabled" @click="tab=item[0]">{{item[1]}}</button></nav>
 <p v-if="error" role="alert" class="lab-error">{{error}}</p><p v-if="notice" role="status">{{notice}}</p><p v-if="busy" role="status">正在处理，请稍候；模型请求可能需要较长时间。</p>
 <section v-if="tab==='compare'">
  <p>依次运行三种模式，保持问题和资料版本一致；可能产生多次模型调用。保留真实差异及失败结果。</p>
  <form @submit.prevent="compare"><input v-model="question" maxlength="1000" required aria-label="同题对比问题" placeholder="例如：点胶工序使用的设备由谁负责？" :disabled="busy||disabled"/><button :disabled="busy||disabled||!question.trim()">运行三模式对比</button></form>
  <button v-if="comparison" @click="download('MouseMES-三模式对比.json',comparison)">导出对比结果</button>
  <div v-if="comparison" class="compare-grid"><article v-for="row in comparison.results" :key="row.mode"><h3>{{row.mode}} · {{row.milliseconds}}ms</h3><p v-if="!row.success" class="lab-error">{{row.error}}</p><template v-if="row.result"><p>{{row.result.answer}}</p><small>{{row.result.sources.length}} 个来源片段 · {{row.result.facts.length}} 条关系</small><details v-for="s in row.result.sources" :key="s.id"><summary>[{{s.id}}] {{s.title}} · v{{s.version}} · {{s.location}}</summary><p>{{s.text}}</p></details><details v-if="row.result.facts.length"><summary>召回关系</summary><p v-for="(f,i) in row.result.facts" :key="i">{{f.source}} → {{f.relation}} → {{f.target}}</p></details></template></article></div>
 </section>
 <section v-if="tab==='diff'">
  <p>比较成功发布版本的关系集合。新增为金色、移除为红色；保留按“起点＋关系＋终点”判断，原文变化请展开核对；模型抽取表述变化也会计入差异。删除资料会一并撤销历史访问。</p>
  <label>选择文件<select v-model="diffId" :disabled="busy||disabled" @change="loadVersions"><option value="">请选择</option><option v-for="d in documents" :key="d.documentId" :value="d.documentId">{{d.title}}</option></select></label>
  <form v-if="versions.length>=2" @submit.prevent="diff"><label>旧版本<select v-model="before"><option v-for="v in versions" :key="v.version" :value="v.version">v{{v.version}}</option></select></label><label>新版本<select v-model="after"><option v-for="v in versions" :key="v.version" :value="v.version">v{{v.version}}</option></select></label><button :disabled="busy||disabled||before>=after">查看图谱差异</button></form>
  <p v-else>该文件至少需要两个本功能启用后成功发布的版本。已有早期版本不补造历史。</p>
  <template v-if="difference"><p>v{{difference.before}} → v{{difference.after}} · 新增{{difference.added.length}} · 移除{{difference.removed.length}} · 保留{{difference.retained.length}}</p><select v-model="diffView" aria-label="差异关系范围"><option value="all">全部变化</option><option value="added">新增</option><option value="removed">移除</option><option value="retained">保留</option></select><button @click="download('MouseMES-图谱差异.json',difference)">导出差异</button><KnowledgeGraphCanvas :facts="diffFacts" :highlighted="difference.added" :removed="difference.removed" :selected="''" :zoom="1" @edge="selectedFact=$event"/><blockquote v-if="selectedFact">{{selectedFact.source}} → {{selectedFact.relation}} → {{selectedFact.target}}<p>{{selectedFact.quote}}</p></blockquote><details v-for="kind in (['added','removed','retained'] as const)" :key="kind"><summary>{{{added:'新增',removed:'移除',retained:'保留'}[kind]}}（{{difference[kind].length}}）</summary><p v-for="(f,i) in difference[kind]" :key="i">{{f.source}} → {{f.relation}} → {{f.target}}<br/>{{f.quote}}</p></details></template>
 </section>
 <section v-if="tab==='association'">
  <p>从资料中的设备、工序或产品出发，查看两跳内关联与原文；不代表实时生产状态。</p><button :disabled="busy||disabled" @click="loadQuality">加载当前实体</button><form @submit.prevent="associate"><select v-model="entity" aria-label="关联起始实体"><option value="">请选择实体</option><option v-for="n in quality?.entities" :key="n.name" :value="n.name">{{n.name}} · {{types[n.type==='OTHER'?n.suggestedType:n.type]}}</option></select><button :disabled="busy||disabled||!entity">查询制造关联</button></form>
  <template v-if="association"><p>{{association.notice}}</p><div class="group-grid"><article v-for="(names,type) in association.groups" :key="type"><h3>{{types[type]}}</h3><p>{{names.join('、')||'暂无关联'}}</p></article></div><KnowledgeGraphCanvas :facts="association.graph.facts" :highlighted="[]" :selected="entity" :entity-types="approvedTypes" :zoom="1"/><p v-if="association.graph.truncated">仅显示前150条关系，请缩小资料范围。</p><details v-for="s in association.graph.sources" :key="s.id"><summary>{{s.title}} · v{{s.version}} · {{s.location}}</summary><p>{{s.text}}</p></details></template>
 </section>
 <section v-if="tab==='quality'">
  <p>建议类型来自名称规则，需人工确认。别名只扩展检索种子，保留原始节点和引用；型号数字不符、类型冲突、别名链均拒绝。</p><button :disabled="busy||disabled" @click="loadQuality">检查图谱质量</button>
  <template v-if="quality"><p>当前最多{{quality.candidateLimit}}个候选实体；建议未经确认不自动应用。</p><div class="group-grid"><article v-for="n in quality.entities" :key="n.name"><strong>{{n.name}}</strong><p>已确认：{{types[n.type]}} · 建议：{{types[n.suggestedType]}}</p></article></div><form @submit.prevent="saveType"><select v-model="entityName" aria-label="审核实体"><option value="">请选择实体</option><option v-for="n in quality.entities" :key="n.name">{{n.name}}</option></select><select v-model="entityType" aria-label="确认实体类型"><option v-for="(name,key) in types" :key="key" :value="key">{{name}}</option></select><button :disabled="busy||disabled||!entityName">确认类型</button></form><form @submit.prevent="alias(entityName,canonical)"><select v-model="canonical" aria-label="规范实体名称"><option value="">请选择规范名称</option><option v-for="n in quality.entities.filter(n=>n.name!==entityName)" :key="n.name">{{n.name}}</option></select><button :disabled="busy||disabled||!entityName||!canonical">确认这两个称呼指同一实体</button></form><p v-for="s in quality.suggestions" :key="s.alias+s.canonical">候选：{{s.alias}} → {{s.canonical}} · {{s.reason}}</p><p v-for="a in quality.aliases" :key="a.alias">已确认：{{a.alias}} → {{a.canonical}} <button :disabled="busy||disabled" @click="alias(a.alias,'')">撤销别名</button></p></template>
 </section>
 <section v-if="tab==='evaluation'">
  <p>18题专用评估集：单文件、跨文件、同义词、无答案和更新后问题。自动指标为关键词覆盖与来源召回，语义正确性需人工复核。每次回答都会调用模型，遇到错误即停止并保留结果。</p>
  <div class="actions"><button :disabled="busy||disabled" @click="importEvaluation()">导入三份评估样例（真实建图）</button><button :disabled="busy||disabled||v2!==1" @click="importEvaluation(true)">发布负责人变更为第二版</button><button v-if="evalRows.length" @click="exportEvaluation">导出评估报告</button></div><p>专用样例已就绪 {{evalDocs.length}}/3 · 设备版本{{equipmentVersion||'尚无'}} · 评估阶段{{v2||'尚无'}}。评估仅查询这三份样例，顶部资料范围不参与。</p>
  <div class="eval-cases"><label v-for="c in evaluationCases" :key="c.id"><input v-model="evalSelected" type="checkbox" :value="c.id" :disabled="busy||!available(c)"/>{{c.id}} · {{c.category}} · {{c.question}}</label></div>
  <div class="actions"><label v-for="m in ['RAG','GRAPHRAG','HYBRID']" :key="m"><input v-model="evalModes" type="checkbox" :value="m" :disabled="busy"/>{{m}}</label><button :disabled="busy||disabled||evalDocs.length!==3" @click="evaluate">运行所选评估</button><button v-if="evalRunning" @click="stop=true">当前问题完成后停止</button></div>
  <p v-if="evalRows.length">尝试{{evalSummary.attempted}} · 成功{{evalSummary.succeeded}} · 错误{{evalSummary.errors}} · 平均关键词覆盖{{(evalSummary.termCoverage*100).toFixed(0)}}% · 预期来源召回{{(evalSummary.sourceRecall*100).toFixed(0)}}% · 引用检查通过{{evalSummary.citationsValid}} · 人工标为正确{{evalSummary.manualReviewed}}</p>
  <article v-for="r in evalRows" :key="r.caseId+r.mode" class="eval-result"><h3>{{r.caseId}} · {{r.mode}} · {{r.milliseconds}}ms</h3><p>{{r.result?.answer||r.error}}</p><small v-if="r.success">关键词覆盖{{(r.termCoverage*100).toFixed(0)}}% · 来源召回{{(r.sourceRecall*100).toFixed(0)}}% · 引用{{r.citationsValid?'有效':'待核对'}}</small><label v-if="r.success"><input v-model="manual[r.caseId+r.mode]" type="checkbox"/>人工核对原文后标为语义正确</label><details v-for="s in r.result?.sources" :key="s.id"><summary>[{{s.id}}] {{s.title}} · v{{s.version}}</summary><p>{{s.text}}</p></details></article>
 </section>
</section>
</template>
<style scoped>
.knowledge-lab{border:1px solid var(--knowledge-line);border-radius:14px;background:white;padding:22px;margin:22px 0;color:var(--knowledge-ink)}.knowledge-lab header{display:flex;justify-content:space-between;align-items:center;gap:12px}.knowledge-lab h2{font-size:17px}.knowledge-lab h3{font-size:13px;margin-bottom:10px}.knowledge-lab p{font-size:12px;line-height:1.9;white-space:pre-wrap;overflow-wrap:anywhere;margin:10px 0}.knowledge-lab nav{display:flex;flex-wrap:wrap;gap:6px;margin:16px 0}.knowledge-lab button,.knowledge-lab select,.knowledge-lab input:not([type=checkbox]){border:1px solid var(--knowledge-line);border-radius:6px;padding:8px 10px;font-size:12px;background:white;color:var(--knowledge-accent);max-width:100%}.knowledge-lab button{cursor:pointer}.knowledge-lab button.active{background:var(--knowledge-accent);color:white}.knowledge-lab button:disabled{opacity:.45;cursor:default}.knowledge-lab form,.actions{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin:12px 0}.knowledge-lab form input:not([type=checkbox]){flex:1;min-width:150px}.knowledge-lab label{font-size:12px;display:inline-flex;align-items:center;gap:6px}.compare-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-top:12px}.compare-grid article,.group-grid article,.eval-result{padding:14px;border:1px solid var(--knowledge-line);border-radius:9px;min-width:0}.group-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:8px;margin:12px 0}.knowledge-lab small{font-size:11px;color:var(--knowledge-muted)}.knowledge-lab details{margin:10px 0;font-size:12px}.knowledge-lab summary{cursor:pointer;color:var(--knowledge-accent)}.knowledge-lab .lab-error{color:#b44e40;background:#fff0ed;padding:10px}.knowledge-lab blockquote{border-left:3px solid #d8a246;margin:10px 0;padding:12px;font-size:12px}.eval-cases{max-height:260px;overflow:auto;border:1px solid var(--knowledge-line);padding:12px;border-radius:8px;display:grid;gap:10px}.eval-result{margin-top:10px}.eval-result>label{display:flex;margin:10px 0}@media(max-width:900px){.compare-grid{grid-template-columns:1fr}}@media(max-width:720px){.knowledge-lab{padding:15px}.knowledge-lab header{align-items:flex-start;flex-direction:column}.knowledge-lab select{max-width:100%;min-width:0}.knowledge-lab form{align-items:stretch;flex-direction:column}.knowledge-lab form input{width:100%}.group-grid{grid-template-columns:minmax(0,1fr)}}

.knowledge-lab{padding:22px 24px;border-radius:10px;background:var(--knowledge-surface,#fffdf9);color:var(--knowledge-ink)}.knowledge-lab h2{font-size:16px;font-weight:600}.knowledge-lab nav{gap:4px;border-bottom:1px solid var(--knowledge-line);padding-bottom:0;margin:18px 0}.knowledge-lab nav button{border:none;border-bottom:2px solid transparent;border-radius:6px 6px 0 0;background:transparent;color:var(--knowledge-muted);padding:11px 15px;min-height:40px}.knowledge-lab nav button.active{border-color:var(--knowledge-accent);background:var(--knowledge-soft);color:var(--knowledge-accent);font-weight:600}.knowledge-lab :is(button,select,input:not([type=checkbox])){min-height:36px;background:#fffdf9;border-color:var(--knowledge-line);border-radius:7px;font-size:13px}.knowledge-lab button:hover:not(:disabled){background:var(--knowledge-soft);border-color:#cdb99b}.knowledge-lab form>button:not(:disabled){background:var(--knowledge-accent);border-color:var(--knowledge-accent);color:#fffaf1}.compare-grid article,.group-grid article,.eval-result{background:#fffdf9;border-color:var(--knowledge-line)}.compare-grid h3{padding-bottom:10px;border-bottom:1px solid var(--knowledge-line);color:var(--knowledge-accent);font-size:14px}.knowledge-lab details{padding:9px 10px;background:#faf7f1;border:1px solid var(--knowledge-line);border-radius:6px}.knowledge-lab details p{color:var(--knowledge-ink)}.eval-cases{background:#faf7f1}.eval-cases label{padding:7px;border-bottom:1px solid #eee7dc;line-height:1.7}.group-grid p{color:var(--knowledge-muted)}@media(max-width:720px){.knowledge-lab{padding:16px}.knowledge-lab nav{gap:0}.knowledge-lab nav button{padding:10px;font-size:12px}}
</style>

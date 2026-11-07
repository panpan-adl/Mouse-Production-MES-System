<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
interface FileDoc {documentId:string;version:number;activeVersion:number;title:string;filename:string;status:string;error?:string;graphStatus?:string;graphError?:string}
interface FileEvent {documentId:string;version:number;action:string;status:string;error?:string;graphStatus?:string}
defineProps<{disabled?:boolean}>()
const emit=defineEmits<{changed:[ids:string[]];managed:[ids:string[]];working:[value:boolean]}>()
const docs=ref<FileDoc[]>([]),events=ref<FileEvent[]>([]),busy=ref(false),error=ref(''),notice=ref('')
interface ModelStatus {embeddingModel?:string;embeddingConfigured?:boolean;dimensions?:number}
const modelStatus=ref<ModelStatus>({})
const editing=ref<FileDoc>(),title=ref(''),file=ref<File>(),input=ref<HTMLInputElement>(),form=ref(false),preview=ref(''),deleting=ref<FileDoc>()
const labels:Record<string,string>={READY:'已发布',FAILED:'失败待重试',INDEXING:'索引中',UPLOADED:'已上传',DELETED:'已删除',CLEANUP_PENDING:'清理待重试',PENDING_TASK3:'待同步图谱'}
async function refresh(){
  [docs.value,events.value,modelStatus.value]=await Promise.all([request.get<FileDoc[]>('/rag/documents'),request.get<FileEvent[]>('/rag/events'),request.get<ModelStatus>('/rag/status')])
  emit('managed',docs.value.map(d=>d.documentId))
}
async function run(action:()=>Promise<void>){if(busy.value)return;busy.value=true;emit('working',true);error.value='';notice.value='';try{await action()}catch(e:any){error.value=e.message||'文件处理失败'}finally{busy.value=false;emit('working',false)}}
async function changed(){await refresh();emit('changed',docs.value.map(d=>d.documentId))}
function open(doc?:FileDoc){editing.value=doc;title.value=doc?.title||'';file.value=undefined;form.value=true;preview.value='';if(input.value)input.value.value=''}
function choose(e:Event){file.value=(e.target as HTMLInputElement).files?.[0];if(file.value&&!title.value)title.value=file.value.name.replace(/\.[^.]+$/,'')}
async function upload(){await run(async()=>{
  if(!file.value||!title.value.trim())throw Error('请选择文件并填写资料标题')
  if(file.value.size>2*1024*1024)throw Error('文件最大2MB')
  const data=new FormData();data.append('documentId',editing.value?.documentId||crypto.randomUUID());data.append('version',String((editing.value?.version||0)+1));data.append('title',title.value.trim());data.append('file',file.value)
  const result=await request.post<{status:string;graphStatus:string;error?:string}>('/rag/documents',data,{timeout:1000000})
  notice.value=result.status==='READY'&&result.graphStatus==='READY'?'文件、向量和图谱已共同发布。':result.status==='FAILED'?'新版本未发布，旧可检索版本保留。'+(result.error||''):'文件已保存，请查看索引状态。'
  form.value=false;await changed()
})}
async function retry(id:string,version:number){await run(async()=>{await request.post('/rag/documents/'+id+'/retry',null,{params:{version},timeout:1000000});await changed();notice.value='已重试，请核对文件与图谱状态。'})}
async function remove(){const doc=deleting.value;if(!doc)return;await run(async()=>{
  const r=await request.delete<{cleanupPending:boolean;graphCleanupPending:boolean}>('/rag/documents/'+doc.documentId,{params:{version:doc.version+1},timeout:65000})
  deleting.value=undefined;preview.value='';notice.value=r.cleanupPending||r.graphCleanupPending?'已退出两类检索，物理清理待重试。':'原件、向量和图谱关系已删除。';await changed()
})}
async function download(doc:FileDoc){await run(async()=>{const blob=await request.get<Blob>('/rag/documents/'+doc.documentId+'/download',{responseType:'blob'});const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=doc.filename;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)})}
async function inspect(doc:FileDoc){await run(async()=>{const d=await request.get<{text:string}>('/rag/documents/'+doc.documentId);preview.value=d.text||'尚未解析出正文，请查看错误信息。'})}
onMounted(()=>run(refresh))
</script>

<template>
<section class="private-library" aria-label="统一私有文件库">
  <header><div><strong>文件资料库</strong><p>PDF · DOCX · TXT · Markdown · CSV，最大2MB、正文12000字符</p></div><div><button :disabled="busy||disabled" @click="run(refresh)">刷新文件</button><button class="file-primary" :disabled="busy||disabled" @click="open()">上传文件</button></div></header>
  <p>嵌入模型：{{modelStatus.embeddingModel||'待读取'}} · {{modelStatus.dimensions||1024}}维 · {{modelStatus.embeddingConfigured?'配置已填写，调用权限以上传结果为准':'等待配置嵌入模型密钥'}}</p>
  <p v-if="error" class="file-error" role="alert">{{error}}</p><p v-if="notice" class="file-notice" role="status">{{notice}}</p>
  <p v-if="busy" class="working" role="status">正在处理文件；解析、向量及图谱发布可能需要数分钟，请保留页面。</p>
  <form v-if="form" class="file-form" @submit.prevent="upload"><label>文件标题<input v-model="title" maxlength="200" required :disabled="busy" /></label><label>选择原始文件<input ref="input" type="file" accept=".pdf,.docx,.txt,.md,.csv" required :disabled="busy" @change="choose" /></label><div><span>{{editing?'更新到版本 '+(editing.version+1):'创建第一版'}} · 两类索引均成功后发布</span><button type="button" :disabled="busy" @click="form=false">取消上传</button><button class="file-primary" :disabled="busy||!file">保存并同步索引</button></div></form>
  <div v-if="!docs.length&&!busy" class="file-empty">尚未上传原始文件；现有图谱文本资料仍可在下方查看。</div>
  <div class="file-list"><article v-for="doc in docs" :key="doc.documentId"><div class="file-title"><strong>{{doc.title}}</strong><span :class="{failed:doc.status==='FAILED'}">{{labels[doc.status]||doc.status}}</span></div><p>{{doc.filename}} · 最新操作 v{{doc.version}} · 可检索 v{{doc.activeVersion||'尚无'}} · 图谱：{{labels[doc.graphStatus||'']||doc.graphStatus||'待同步'}}</p><p v-if="doc.error||doc.graphError" class="file-error">{{doc.error||doc.graphError}}</p><div class="file-actions"><button :disabled="busy||disabled" @click="inspect(doc)">查看正文</button><button :disabled="busy||disabled" @click="download(doc)">下载原件</button><button :disabled="busy||disabled" @click="open(doc)">替换原件</button><button v-if="doc.status==='FAILED'||(doc.status==='READY'&&doc.graphStatus!=='READY')" :disabled="busy||disabled" @click="retry(doc.documentId,doc.version)">重试同步</button><button :disabled="busy||disabled" @click="deleting=doc">删除文件</button></div></article></div>
  <details v-if="events.length" class="file-events"><summary>文件处理记录（{{events.length}}）</summary><article v-for="event in events" :key="event.documentId+':'+event.version"><p>{{event.documentId}} · v{{event.version}} · {{event.action}} · {{labels[event.status]||event.status}} · 图谱 {{labels[event.graphStatus||'']||event.graphStatus}}</p><small v-if="event.error">{{event.error}}</small><button v-if="event.action==='DELETE'&&(event.status==='CLEANUP_PENDING'||event.graphStatus==='CLEANUP_PENDING')" :disabled="busy||disabled" @click="retry(event.documentId,event.version)">重试删除清理</button></article></details>
  <details v-if="preview" open class="file-preview"><summary>解析正文</summary><pre>{{preview}}</pre><button @click="preview=''">关闭正文</button></details>
  <div v-if="deleting" class="file-modal"><section role="dialog" aria-modal="true" aria-labelledby="file-delete-title"><h2 id="file-delete-title">删除“{{deleting.title}}”？</h2><p>原件将退出文件库，同时撤销向量和图谱检索权限。</p><div><button :disabled="busy" @click="deleting=undefined">取消删除</button><button class="file-primary" :disabled="busy" @click="remove">确认删除文件</button></div></section></div>
</section>
</template>

<style scoped>
.private-library{background:white;border:1px solid #dfe8ec;border-radius:14px;margin:0 0 20px;padding:20px 24px;color:#365165}.private-library header{display:flex;align-items:center;justify-content:space-between;gap:15px}.private-library header>div:last-child{display:flex;gap:8px;flex-shrink:0}.private-library header strong{font-size:15px}.private-library p{font-size:12px;color:#82939d;line-height:1.8;margin-top:6px}.private-library button{border:1px solid #dce6ea;border-radius:6px;padding:7px 10px;font-size:12px;background:white;color:#466778}.private-library button:disabled{opacity:.45;cursor:not-allowed}.private-library .file-primary{background:#0b716d;color:white;border-color:#0b716d}.private-library .file-error{color:#b34d43;font-size:12px;margin:10px 0}.file-notice,.working{padding:10px 12px;background:#eef7f4;border-radius:6px}.file-empty{font-size:12px;color:#8c9ca5;margin-top:18px}.file-list{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:12px;margin-top:15px}.file-list article{min-width:0;border:1px solid #e5edef;border-radius:8px;padding:14px}.file-title{display:flex;gap:10px;align-items:center;justify-content:space-between}.file-title strong{font-size:13px;overflow-wrap:anywhere}.file-title span{font-size:10px;white-space:nowrap;color:#33846b}.file-title .failed{color:#b34d43}.file-actions{display:flex;gap:5px;flex-wrap:wrap;margin-top:10px}.file-actions button{font-size:11px;padding:5px 7px}.file-form{background:#f5f9f8;border-radius:9px;padding:16px;margin-top:16px;display:grid;gap:14px}.file-form label{display:grid;gap:7px;font-size:12px}.file-form input:not([type=file]){border:1px solid #dce5e9;border-radius:5px;padding:8px}.file-form input[type=file]{max-width:100%;font-size:12px}.file-form>div{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.file-form span{font-size:11px;color:#81969f;margin-right:auto}.file-events,.file-preview{margin-top:14px;font-size:12px}.file-events summary,.file-preview summary{cursor:pointer;color:#0b716d}.file-events article{padding:10px 0;border-bottom:1px solid #eef2f4}.file-events p{overflow-wrap:anywhere}.file-events small{color:#b34d43}.file-preview pre{white-space:pre-wrap;overflow-wrap:anywhere;font:12px/1.8 inherit;max-height:320px;overflow:auto;margin:10px 0}.file-modal{position:fixed;inset:0;background:#17334b66;z-index:210;display:grid;place-items:center;padding:20px}.file-modal section{background:white;padding:25px;border-radius:12px;max-width:460px}.file-modal h2{font-size:18px;font-weight:600}.file-modal section>div{display:flex;justify-content:flex-end;gap:10px;margin-top:22px}@media(max-width:720px){.private-library{padding:16px}.private-library header{align-items:flex-start;flex-direction:column}.file-list{grid-template-columns:minmax(0,1fr)}}
</style>

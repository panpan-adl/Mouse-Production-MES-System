<script setup lang="ts">
import {computed,nextTick,onMounted,ref,watch} from 'vue'
import {useRoute} from 'vue-router'
import MesRobot from '@/components/MesRobot.vue'
import {Bot,Sparkles,Send,Plus,Check,Clock3,ArrowRight,ShieldCheck} from 'lucide-vue-next'
import MainLayout from '@/components/MainLayout.vue'
import BusinessQueryPanel from '@/components/BusinessQueryPanel.vue'
import QueryTable from '@/components/QueryTable.vue'
import request from '@/utils/request'
type Row=Record<string,any>
const catalog=ref<Row>({skills:[],model:{}}),runs=ref<Row[]>([]),messages=ref<Row[]>([])
const question=ref(''),selected=ref(''),busy=ref(false),error=ref(''),session=ref(''),previous=ref(''),bottom=ref<HTMLElement>()
const names:Row={PENDING:'等待确认',SUCCEEDED:'已完成',CLARIFICATION:'待补充',FAILED:'执行失败',CANCELLED:'已取消',EXPIRED:'已过期',STALE:'需重新核对'}
const maintenanceColumns=[{key:'code',label:'计划编码'},{key:'name',label:'计划名称'},{key:'equipment_name',label:'设备'},{key:'plan_type',label:'类型'},{key:'due_date',label:'到期日'},{key:'status',label:'状态'},{key:'owner_name',label:'负责人'}]
const activeSkill=computed(()=>catalog.value.skills.find((s:Row)=>s.id===selected.value))
const route=useRoute()
async function refresh(){runs.value=await request.get<Row[]>('/agent/runs')}
async function add(run:Row){const index=messages.value.findIndex(m=>m.id===run.id);if(index<0)messages.value.push(run);else messages.value[index]=run;await refresh();await nextTick();bottom.value?.parentElement?.scrollTo({top:bottom.value.parentElement.scrollHeight,behavior:'smooth'})}
async function load(){try{catalog.value=await request.get('/agent/skills');await refresh()}catch(e:any){error.value=e.message}}
async function ask(){if(busy.value||!question.value.trim())return;busy.value=true;error.value='';try{const result=await request.post<Row>('/agent/messages',{question:question.value.trim(),skillId:selected.value,previousSkill:previous.value,...(session.value?{sessionId:session.value}:{})},{timeout:35000});if(result.reply.sessionId){session.value=result.reply.sessionId;previous.value=result.skill_id}await add(result);question.value=''}catch(e:any){error.value=e.message;await refresh().catch(()=>{})}finally{busy.value=false}}
function reset(){messages.value=[];session.value='';previous.value='';error.value='';question.value='';selected.value=''}
function choose(skill:Row){selected.value=skill.id;question.value=skill.example}
async function action(url:string){if(busy.value)return;busy.value=true;error.value='';try{await add(await request.post<Row>(url))}catch(e:any){error.value=e.message}finally{busy.value=false}}
async function restore(id:string){if(busy.value)return;busy.value=true;error.value='';try{const run=await request.get<Row>('/agent/runs/'+id);if(run.reply.sessionId){session.value=run.reply.sessionId;previous.value=run.skill_id}await add(run)}catch(e:any){error.value=e.message}finally{busy.value=false}}
async function openLinkedTask(){const id=route.query.run;if(typeof id==='string'&&/^[a-f0-9-]{36}$/.test(id))await restore(id)}
onMounted(async()=>{await load();await openLinkedTask()})
watch(()=>route.query.run,openLinkedTask)
</script>

<template><MainLayout><div class="agent-page max-w-7xl mx-auto space-y-5">
  <section class="agent-hero"><div class="agent-emblem"><MesRobot/></div><div><p class="agent-eyebrow">MOUSE MES / SKILL AGENT</p><h1>你的制造业务协作助手</h1><p class="text-slate-500 text-sm mt-2">描述任务，由技能连接真实业务数据；需要写入时，先核对再确认。</p></div><span class="agent-mode"><i></i>{{catalog.model.mode==='remote'?(catalog.model.configured?'大模型已配置':'等待大模型配置'):'本地技能模式'}}</span></section>
  <BusinessQueryPanel/>
  <div class="agent-grid">
    <aside class="space-y-4">
      <section class="bg-white border rounded-xl p-4"><div class="flex items-center gap-2 mb-4"><Sparkles :size="17" class="text-blue-600"/><h2 class="font-semibold">技能库</h2><span class="text-xs text-slate-500 ml-auto">{{catalog.skills.length}} 项可用</span></div>
        <button :disabled="busy" class="skill-choice" :class="{chosen:!selected}" @click="selected=''">自动识别任务<small>让助手选择适合的技能</small></button>
        <button v-for="skill in catalog.skills" :key="skill.id" :disabled="busy" class="skill-choice" :class="{chosen:selected===skill.id}" @click="choose(skill)"><span>{{skill.name}}<span v-if="skill.write" class="skill-tag">需确认</span></span><small>{{skill.description}}</small></button>
        <p v-if="!catalog.skills.length" class="text-sm text-slate-500 mt-3">当前岗位暂无已授权技能，请联系管理员核对岗位权限。</p>
      </section>
      <section class="bg-white border rounded-xl p-4 space-y-3"><h2 class="font-semibold flex items-center gap-2"><Clock3 :size="16"/>最近任务</h2><p v-if="!runs.length" class="text-xs text-slate-500">开始对话后，任务会保存在这里。</p><button v-for="run in runs" :key="run.id" :disabled="busy" class="agent-history" @click="restore(run.id)"><span>{{run.question}}</span><small>{{names[run.status]||run.status}} · {{String(run.created_at).replace('T',' ').slice(5,16)}}</small></button></section>
    </aside>
    <div class="min-w-0 space-y-4">
      <section class="bg-white border rounded-xl overflow-hidden">
        <header class="p-5 border-b flex items-center justify-between"><div><h2 class="font-semibold">任务对话</h2><p class="text-xs text-slate-500 mt-1">{{activeSkill?.name||'自动选择技能'}} · 查询结果保留来源和时间</p></div><button :disabled="busy" class="text-blue-600 text-sm flex items-center gap-1" @click="reset"><Plus :size="16"/>新会话</button></header>
        <div class="p-5 space-y-5 agent-conversation" aria-live="polite" :aria-busy="busy">
          <div v-if="!messages.length" class="agent-welcome"><span class="agent-welcome-icon"><Bot :size="34"/></span><h3>今天需要我协助什么？</h3><p>例如“分析当前缺料”，或选择左侧技能开始。<br/>查询可继续补充条件，操作任务会显示确认卡片。</p><div class="flex flex-wrap justify-center gap-2"><button v-for="s in catalog.skills.slice(0,3)" :key="s.id" class="agent-example" @click="choose(s)">{{s.example}}<ArrowRight :size="12"/></button></div></div>
          <article v-for="run in messages" :key="run.id" class="agent-turn">
            <div class="agent-question">{{run.question}}</div>
            <div class="agent-answer space-y-3"><div class="flex items-center flex-wrap gap-2 text-xs"><Bot :size="16" class="text-blue-600"/><strong>{{run.reply.skillName||catalog.skills.find((s:Row)=>s.id===run.skill_id)?.name||'任务助手'}}</strong><span class="agent-status" :class="{'pending':run.status==='PENDING'}">{{names[run.status]||run.status}}</span></div><p class="text-sm leading-7">{{run.reply.summary}}</p>
              <p v-for="c in run.reply.query?.clarifications||[]" :key="c" class="text-sm text-amber-700">{{c}}</p>
              <div v-if="run.reply.data" class="space-y-3"><p class="text-xs text-slate-500 leading-6">{{run.reply.data.evidence}}<br/>查询时间：{{run.reply.data.updatedAt}}</p><p v-if="run.reply.data.conditions" class="text-xs text-slate-500 break-all">查询条件：{{run.reply.data.conditions}}</p><QueryTable :rows="run.reply.data.rows||[]" :columns="run.skill_id==='maintenance'?maintenanceColumns:undefined"><template #default="{row}"><button v-if="run.reply.canPropose&&row.status!=='CONFIRMED'" :disabled="busy" class="text-blue-600 text-xs whitespace-nowrap" @click="action('/agent/proposals/'+row.material_id)">生成确认任务</button></template></QueryTable>
                <template v-if="run.reply.data.reports"><h4 class="text-sm font-semibold">报工记录</h4><QueryTable :rows="run.reply.data.reports"/><h4 class="text-sm font-semibold">过程检验</h4><QueryTable :rows="run.reply.data.inspections||[]"/></template>
                <details v-if="run.reply.data.sources" class="text-xs text-slate-500"><summary class="cursor-pointer py-2">查看来源流水（最多200条）</summary><QueryTable :rows="run.reply.data.sources.slice(0,200)"/></details>
              </div>
              <div v-if="run.reply.preview" class="agent-confirm"><h4 class="font-semibold text-sm flex items-center gap-2"><ShieldCheck :size="16"/>补货建议核对</h4><dl class="grid grid-cols-2 gap-3 text-sm mt-4"><div><dt>物料</dt><dd>{{run.reply.preview.material_name}} · {{run.reply.preview.material_code}}</dd></div><div><dt>当前库存</dt><dd>{{run.reply.preview.current_quantity}} {{run.reply.preview.unit}}</dd></div><div><dt>安全库存</dt><dd>{{run.reply.preview.safety_stock}} {{run.reply.preview.unit}}</dd></div><div><dt>建议补货量</dt><dd>{{run.reply.preview.suggested_quantity}} {{run.reply.preview.unit}}</dd></div></dl><div v-if="run.status==='PENDING'" class="flex flex-wrap gap-3 mt-5"><button :disabled="busy" class="bg-blue-600 text-white rounded-lg px-4 py-2 text-sm flex items-center gap-2" @click="action('/agent/runs/'+run.id+'/confirm')"><Check :size="16"/>确认登记补货建议</button><button :disabled="busy" class="border rounded-lg px-4 py-2 text-sm" @click="action('/agent/runs/'+run.id+'/cancel')">取消任务</button></div></div>
              <p class="text-xs text-slate-400 break-all">任务编号 {{run.id}}</p>
            </div>
          </article><div ref="bottom"></div>
        </div>
        <form class="agent-composer" @submit.prevent="ask"><label for="agent-question" class="sr-only">任务描述</label><textarea id="agent-question" v-model="question" maxlength="1200" rows="3" :disabled="busy" placeholder="描述任务，例如：查询当前库存、追溯 MO-000001、检查到期保养任务…" @keydown.ctrl.enter.prevent="ask"></textarea><div class="flex flex-wrap gap-3 items-center justify-between"><span class="text-xs text-slate-500">{{question.length}} / 1200 · Ctrl + Enter 发送</span><button :disabled="busy||!question.trim()" class="bg-blue-600 text-white px-5 py-2 rounded-lg flex items-center gap-2 text-sm disabled:opacity-50"><Send :size="15"/>{{busy?'正在处理…':'发送任务'}}</button></div></form>
      </section>
      <p v-if="error" role="alert" class="bg-red-50 text-red-700 border rounded-lg p-4 text-sm">{{error}}</p>
      <details class="bg-white border rounded-xl p-4 text-sm text-slate-500"><summary class="cursor-pointer">模型接入与能力说明</summary><div class="mt-3 space-y-2 leading-7"><p>{{catalog.model.mode==='remote'?'当前使用远程模型，配置完成后可识别任务和解析查询条件。':'当前使用本地规则选择技能，尚未调用大模型；不消耗模型费用。'}}</p><p>支持 DeepSeek 等兼容 Chat Completions 的接口。接口地址、模型名和密钥由管理员在服务器配置，密钥不保存在网页。每人每天最多20次模型调用；自动识别加查询解析可能使用2次调用。</p><p>当前五项技能覆盖查询、待办和补货建议确认。暂不自动创建采购订单、改变库存或代填保养记录。结果以系统数据为准，无法核实的历史余额会标明。</p></div></details>
    </div>
  </div>
</div></MainLayout></template>

<style scoped>
.agent-hero{display:flex;align-items:center;gap:20px;padding:25px 28px;border:1px solid #b2d8ef;border-radius:18px;background:linear-gradient(115deg,#f7fcffdd,#dbeeffb5);box-shadow:0 8px 35px #3f88be0c}.agent-emblem{display:grid;place-items:center;width:64px;height:64px;border:1px solid #9bcce7;border-radius:20px;color:#2d8fbd;background:linear-gradient(140deg,#e3f7ff,#c5e3ff);box-shadow:0 0 0 6px #b8ddf71f;flex-shrink:0}.agent-eyebrow{font-size:9px;letter-spacing:.2em;color:#3189b1;margin-bottom:6px}.agent-hero h1{font-size:25px;font-weight:650}.agent-mode{margin-left:auto;display:flex;align-items:center;gap:7px;font-size:11px;color:#5484a5;white-space:nowrap}.agent-mode i{width:6px;height:6px;border-radius:50%;background:#43b8c6;box-shadow:0 0 10px #45bce54d}.agent-grid{display:grid;grid-template-columns:270px minmax(0,1fr);gap:20px}.skill-choice{width:100%;text-align:left;padding:13px 12px;border:1px solid transparent;border-radius:10px;margin-bottom:6px;color:#436d92;font-size:13px;transition:.2s}.skill-choice:hover,.skill-choice.chosen{background:linear-gradient(110deg,#dff2ff,#edf5ff);border-color:#a8d0ec}.skill-choice small{display:block;font-size:11px;line-height:1.7;color:#7292ae;margin-top:5px}.skill-tag{font-size:9px;margin-left:8px;padding:2px 5px;border-radius:5px;background:#fff2d8;color:#a98135}.agent-history{display:block;width:100%;text-align:left;padding:9px 0;border-bottom:1px solid #dcebf5;font-size:11px}.agent-history>span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#476f93}.agent-history small{display:block;color:#849eb6;margin-top:4px}.agent-conversation{min-height:300px}.agent-welcome{text-align:center;padding:35px 10px}.agent-welcome-icon{display:inline-grid;place-items:center;width:72px;height:72px;background:radial-gradient(#cce9ff,#edf8ff);color:#3698c5;border-radius:24px;margin-bottom:16px}.agent-welcome h3{font-size:19px;color:#3c6d96}.agent-welcome p{font-size:12px;line-height:2;color:#7897b2;margin:12px 0 22px}.agent-example{display:flex;align-items:center;gap:6px;border:1px solid #b9d8ed;background:#f1faff;padding:9px 12px;border-radius:10px;font-size:11px;color:#5485a9}.agent-question{margin-left:auto;width:fit-content;max-width:90%;white-space:pre-wrap;overflow-wrap:anywhere;background:linear-gradient(110deg,#d2ebff,#dce9ff);padding:13px 17px;border-radius:14px 14px 3px 14px;color:#33688f;font-size:13px}.agent-answer{margin-top:15px;min-width:0;border:1px solid #d2e5f4;border-radius:3px 14px 14px 14px;padding:18px;background:#f7fcffb0;color:#4c7292}.agent-status{font-size:10px;padding:3px 7px;background:#e0f5ee;border-radius:6px;color:#3d8a76}.agent-status.pending{background:#fff0d3;color:#9d772d}.agent-composer{border-top:1px solid #c4def1;padding:18px;background:#eef7ff8c}.agent-composer textarea{width:100%;padding:12px;border:1px solid #abcfe7;border-radius:10px;resize:vertical;min-height:86px;margin-bottom:12px}.agent-confirm{padding:17px;border:1px solid #abd1e9;background:linear-gradient(115deg,#e5f4ff,#f4faff);border-radius:12px}.agent-confirm dt{font-size:10px;color:#7a97b0}.agent-confirm dd{margin-top:4px;color:#3e7196;overflow-wrap:anywhere}.agent-page button:disabled{cursor:wait;opacity:.55}@media(max-width:1100px){.agent-grid{grid-template-columns:220px minmax(0,1fr)}.agent-mode{display:none}.agent-hero{padding:20px}.agent-hero h1{font-size:22px}}@media(max-width:760px){.agent-grid{grid-template-columns:1fr}.agent-grid>aside{display:flex;flex-direction:column}.agent-grid>aside>section:last-child{max-height:180px;overflow:auto}.agent-hero{gap:12px;padding:18px}.agent-emblem{width:46px;height:46px;border-radius:14px}.agent-hero h1{font-size:18px}.agent-eyebrow{font-size:7px}.agent-hero p.text-sm{font-size:11px;line-height:1.8}.agent-answer{padding:12px}.agent-grid>aside .skill-choice{padding:9px 10px}.skill-choice small{font-size:10px}.agent-confirm dl{grid-template-columns:1fr}.agent-composer{padding:12px}}
</style>
<style scoped>
.agent-conversation{max-height:680px;overflow:auto;scrollbar-color:#afd4ed transparent;scrollbar-width:thin}
.agent-grid>aside>section:last-child{max-height:430px;overflow-y:auto}
@media(max-width:760px){.agent-grid{display:flex;flex-direction:column}.agent-grid>aside{order:2}.agent-grid>div{order:1}.agent-conversation{max-height:600px}.agent-grid>aside>section:last-child{max-height:160px}}
</style>

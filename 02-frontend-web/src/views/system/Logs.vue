<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'

type Log = Record<string, any>
const tabs = [{ key: 'audit', name: '业务审计' }, { key: 'operation', name: '操作日志' }, { key: 'login', name: '登录日志' }]
const kind = ref('audit'), keyword = ref(''), status = ref(''), startDate = ref(''), endDate = ref('')
const rows = ref<Log[]>([]), total = ref(0), page = ref(1), size = ref(20)
const loading = ref(false), error = ref(''), selected = ref<Log>(), updated = ref('')
let sequence = 0
const resourceNames: Record<string, string> = { workshops:'车间', stations:'工作站', materials:'物料', products:'产品', customers:'客户', suppliers:'供应商', units:'计量单位', categories:'分类', inspections:'质量检验', quality:'不合格处置', 'bom-versions':'BOM版本', 'bom-lines':'BOM组成', routes:'工艺路线', operations:'工序', schedules:'排班', members:'班组成员', teams:'班组', shifts:'班次', 'check-records':'点检执行', checks:'点检保养计划', parameters:'系统参数', menus:'菜单', 'role-menus':'菜单授权', positions:'岗位', 'code-rules':'编码规则', 'mes:document':'仓储单据', 'mes:draft':'仓储草稿', 'mes:post':'仓储过账', 'mes:confirm':'仓储确认', 'mes:dispatch':'排产', 'mes:reportDraft':'报工单', 'mes:reportConfirm':'报工审批', 'mes:disposition':'不合格处置', 'mes:rework':'返工', 'mes:scrap':'报废', 'mes:publish':'通知公告' }
const actions: Record<string,string> = { SAVE:'保存', DELETE:'删除', EXECUTE:'执行', EDIT:'编辑', POST:'提交操作', SCRAP:'执行报废' }
const resource = (value: string) => resourceNames[value] || value || '—'
const action = (value: string) => actions[value] || value || '—'
const time = (value: any) => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
const state = (value: any) => String(value) === '1' ? '成功' : String(value) === '0' ? '失败' : '已记录'
async function load(reset = false) {
  if (reset) page.value = 1
  const current = ++sequence
  loading.value = true; error.value = ''; selected.value = undefined
  try {
    const data = await request.get<{items: Log[]; total: number}>('/mes/system-logs', {params:{kind:kind.value,keyword:keyword.value,status:status.value,startDate:startDate.value,endDate:endDate.value,page:page.value,size:size.value}})
    if(current !== sequence) return
    rows.value = data.items; total.value = data.total; updated.value = new Date().toLocaleTimeString('zh-CN')
  } catch(e:any) {
    if(current !== sequence) return
    error.value = e.message || '日志加载失败'; rows.value = []; total.value = 0
  } finally { if(current === sequence) loading.value = false }
}
function switchTab(key:string){kind.value=key;status.value='';load(true)}
function reset(){keyword.value='';status.value='';startDate.value='';endDate.value='';load(true)}
function turn(delta:number){page.value+=delta;load()}
onMounted(()=>load())
</script>

<template>
  <MainLayout>
    <div class="space-y-5">
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div><p class="text-sm text-blue-600 mb-1">系统管理</p><h1 class="text-2xl font-bold text-slate-800">日志管理</h1><p class="text-sm text-slate-500 mt-2">查看真实登录、操作和业务审计记录，按操作人、业务及日期检索。</p></div>
        <button :disabled="loading" class="border bg-white rounded-lg px-4 py-2 disabled:opacity-50" @click="load()">{{loading?'正在加载…':'刷新日志'}}</button>
      </header>
      <div class="flex flex-wrap gap-2" role="tablist" aria-label="日志类型">
        <button v-for="t in tabs" :key="t.key" role="tab" :aria-selected="kind===t.key" class="rounded-lg px-5 py-2 border" :class="kind===t.key?'bg-blue-600 text-white border-blue-600':'bg-white text-slate-600'" @click="switchTab(t.key)">{{t.name}}</button>
      </div>
      <form class="bg-white border rounded-xl p-4 flex flex-wrap items-end gap-3" @submit.prevent="load(true)">
        <label class="text-sm text-slate-600 flex-1 min-w-52">关键词<input v-model="keyword" maxlength="100" placeholder="姓名、账号、模块、动作或关联编号" class="block border rounded-lg p-2 w-full mt-1" /></label>
        <label class="text-sm text-slate-600">开始日期<input v-model="startDate" type="date" class="block border rounded-lg p-2 mt-1" /></label>
        <label class="text-sm text-slate-600">结束日期<input v-model="endDate" type="date" class="block border rounded-lg p-2 mt-1" /></label>
        <label v-if="kind!=='audit'" class="text-sm text-slate-600">结果<select v-model="status" class="block border rounded-lg p-2 mt-1"><option value="">全部</option><option value="1">成功</option><option value="0">失败</option></select></label>
        <button :disabled="loading" class="bg-blue-600 text-white rounded-lg px-4 py-2 disabled:opacity-50">查询</button>
        <button type="button" class="border rounded-lg px-4 py-2" @click="reset">重置</button>
      </form>
      <p v-if="kind==='audit'" class="text-sm text-slate-500">业务审计展示已留痕的关键操作；“已记录”表示有审计凭据，不代表所有业务步骤或失败操作均已覆盖。</p>
      <p v-if="error" role="alert" class="bg-red-50 text-red-700 rounded-lg p-3">{{error}}</p>
      <section class="bg-white border rounded-xl overflow-hidden" :aria-busy="loading">
        <div class="overflow-x-auto"><table class="min-w-full text-sm text-left"><thead class="bg-slate-50 text-slate-600"><tr><th class="px-4 py-3">时间</th><th class="px-4 py-3">操作人／账号</th><th class="px-4 py-3">模块／业务</th><th class="px-4 py-3">动作</th><th class="px-4 py-3">关联记录</th><th class="px-4 py-3">结果</th><th class="px-4 py-3">操作</th></tr></thead>
          <tbody><tr v-for="r in rows" :key="r.id" class="border-t hover:bg-slate-50"><td class="px-4 py-3 whitespace-nowrap">{{time(r.event_time)}}</td><td class="px-4 py-3"><div>{{r.actor_name||'未知用户'}}</div><div class="text-slate-400 text-xs mt-1">{{r.user_code||'—'}}</div></td><td class="px-4 py-3">{{resource(r.resource)}}</td><td class="px-4 py-3">{{action(r.action)}}</td><td class="px-4 py-3">{{r.record_id>0?r.record_id:'—'}}</td><td class="px-4 py-3"><span class="inline-block text-xs rounded-full px-2 py-1" :class="String(r.status)==='0'?'bg-red-50 text-red-700':String(r.status)==='1'?'bg-green-50 text-green-700':'bg-blue-50 text-blue-700'">{{state(r.status)}}</span></td><td class="px-4 py-3"><button class="text-blue-600 whitespace-nowrap" @click="selected=r">查看详情</button></td></tr></tbody>
        </table></div>
        <p v-if="!rows.length" class="p-10 text-center text-slate-500">{{loading?'正在读取日志…':'当前条件下没有日志记录'}}</p>
        <footer class="border-t px-4 py-3 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600"><span>共 {{total}} 条 · 第 {{page}} / {{Math.max(1,Math.ceil(total/size))}} 页<span v-if="updated"> · 更新于 {{updated}}</span></span><div class="flex items-center gap-3"><select v-model="size" aria-label="每页条数" class="border rounded p-1" @change="load(true)"><option :value="20">20条／页</option><option :value="50">50条／页</option><option :value="100">100条／页</option></select><button :disabled="loading||page===1" class="border rounded px-3 py-1 disabled:opacity-40" @click="turn(-1)">上一页</button><button :disabled="loading||page*size>=total" class="border rounded px-3 py-1 disabled:opacity-40" @click="turn(1)">下一页</button></div></footer>
      </section>
      <div v-if="selected" class="modal-mask fixed inset-0 z-[60] bg-black/40 flex items-center justify-center p-5" @click.self="selected=undefined"><section role="dialog" aria-modal="true" aria-labelledby="log-detail-title" class="modal-card bg-white rounded-xl max-w-2xl w-full max-h-[85vh] overflow-auto p-6"><div class="flex justify-between items-center mb-5"><h2 id="log-detail-title" class="text-lg font-semibold">日志详情 #{{selected.id}}</h2><button class="border rounded px-3 py-1" @click="selected=undefined">关闭</button></div><dl class="grid grid-cols-[100px_1fr] gap-3 text-sm"><dt>日志类型</dt><dd>{{tabs.find(t=>t.key===kind)?.name}}</dd><dt>时间</dt><dd>{{time(selected.event_time)}}</dd><dt>操作人</dt><dd>{{selected.actor_name||'—'}} · {{selected.user_code||'—'}}</dd><dt>模块／业务</dt><dd>{{resource(selected.resource)}}<span class="text-slate-400 ml-2">{{selected.resource}}</span></dd><dt>动作</dt><dd>{{action(selected.action)}}</dd><dt>关联编号</dt><dd>{{selected.record_id>0?selected.record_id:'未提供'}}</dd><dt>结果</dt><dd>{{state(selected.status)}}</dd><dt v-if="selected.ip">来源IP</dt><dd v-if="selected.ip">{{selected.ip}}</dd><dt v-if="selected.request_url">请求地址</dt><dd v-if="selected.request_url" class="break-all">{{selected.method}} {{selected.request_url}}</dd><dt v-if="selected.browser">浏览器</dt><dd v-if="selected.browser" class="break-all">{{selected.browser}}</dd><dt v-if="selected.os">操作系统</dt><dd v-if="selected.os">{{selected.os}}</dd><dt v-if="selected.message">说明</dt><dd v-if="selected.message" class="whitespace-pre-wrap break-words">{{selected.message}}</dd></dl><p class="text-xs text-slate-400 mt-6">此页面只读，不提供日志修改或删除。</p></section></div>
    </div>
  </MainLayout>
</template>

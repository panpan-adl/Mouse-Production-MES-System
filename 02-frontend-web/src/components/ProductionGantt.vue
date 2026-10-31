<script setup lang="ts">
import { computed, ref } from 'vue'
type Row = Record<string, any>
const props = defineProps<{ rows: Row[]; workOrders: Row[] }>()
const selectedWork = ref('')
const workName = (id: number) => props.workOrders.find(w => Number(w.id) === id)?.code || `工单 #${id}`
const options = computed(() => [...new Set(props.rows.map(r => Number(r.work_order_id)))].map(id => ({ id, name: workName(id) })))
const filtered = computed(() => props.rows.filter(r => !selectedWork.value || String(r.work_order_id) === selectedWork.value))
const valid = computed(() => filtered.value.filter(r => Number.isFinite(new Date(r.start_at).getTime()) && r.start_at && new Date(r.end_at).getTime() > new Date(r.start_at).getTime()).sort((a,b) => new Date(a.start_at).getTime() - new Date(b.start_at).getTime()))
const bounds = computed(() => {
  if (!valid.value.length) return { from: 0, to: 1 }
  const hour = 3600000
  return { from: Math.floor(Math.min(...valid.value.map(r => new Date(r.start_at).getTime())) / hour) * hour, to: Math.ceil(Math.max(...valid.value.map(r => new Date(r.end_at).getTime())) / hour) * hour }
})
const span = computed(() => Math.max(1, bounds.value.to - bounds.value.from))
const format = (value: number | string) => { const d = new Date(value); return `${String(d.getMonth()+1).padStart(2,'0')}/${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }
const ticks = computed(() => Array.from({length:5}, (_,i) => ({position:i*25, label:format(bounds.value.from + span.value*i/4)})))
const bars = computed<Row[]>(() => valid.value.map(r => ({...r, left:(new Date(r.start_at).getTime()-bounds.value.from)/span.value*100, width:(new Date(r.end_at).getTime()-new Date(r.start_at).getTime())/span.value*100})))
const statuses = ['待领取','已派发','生产中','待质检','已完成','已取消']
const colors = ['#64748b','#3478ed','#d28b16','#8b5ecb','#2b9276','#94a3b8']
</script>

<template>
  <section class="production-gantt" data-testid="production-gantt">
    <div class="gantt-heading"><div><h2>生产任务甘特图</h2><p>色条表示计划时段，颜色表示任务状态</p></div><label>生产工单 <select v-model="selectedWork" aria-label="甘特图工单筛选"><option value="">全部工单</option><option v-for="w in options" :key="w.id" :value="String(w.id)">{{w.name}}</option></select></label></div>
    <div v-if="bars.length" class="gantt-scroll"><div class="gantt-chart">
      <div class="gantt-axis"><span>工序任务 / 负责人</span><div><span v-for="t in ticks" :key="t.position" :style="{left:t.position+'%'}">{{t.label}}</span></div></div>
      <div v-for="r in bars" :key="r.id" class="gantt-row" :data-task-id="r.id">
        <div class="gantt-label"><b>{{r.operation_name}}</b><small>{{r.task_no}} · {{r.assigned_user_name || '未分派'}} · {{r.quantity}}只</small></div>
        <div class="gantt-time"><div class="gantt-track"><i v-for="t in ticks" :key="t.position" :style="{left:t.position+'%'}"></i><div class="gantt-bar" :style="{left:r.left+'%',width:r.width+'%',background:colors[Number(r.task_status)] || colors[0]}" :title="format(r.start_at)+' — '+format(r.end_at)"></div></div><small>{{format(r.start_at)}} — {{format(r.end_at)}}<em :style="{color:colors[Number(r.task_status)]}">{{statuses[Number(r.task_status)] || '未知状态'}}</em></small></div>
      </div>
    </div></div>
    <p v-else class="gantt-empty">当前工单暂无有效排产时段，请先分派任务并设置起止时间。</p>
    <div class="gantt-legend"><span v-for="i in [1,2,3,4]" :key="i"><i :style="{background:colors[i]}"></i>{{statuses[i]}}</span><span v-if="filtered.length > valid.length">另有 {{filtered.length-valid.length}} 条任务未设置有效时段</span></div>
  </section>
</template>

<style scoped>
.production-gantt{padding:24px;background:#fff;border:1px solid #d5dfe9;border-radius:12px;margin-bottom:20px;color:#243c53}.gantt-heading{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:26px}.gantt-heading h2{font-size:20px;font-weight:700;margin:0 0 6px}.gantt-heading p{font-size:13px;color:#60768b;margin:0}.gantt-heading label{font-size:13px;white-space:nowrap}.gantt-heading select{border:1px solid #cbd5e1;border-radius:6px;padding:7px;margin-left:8px;background:white}.gantt-scroll{overflow-x:auto;padding:0 40px 0 0}.gantt-chart{min-width:650px}.gantt-axis,.gantt-row{display:grid;grid-template-columns:220px 1fr;gap:22px}.gantt-axis{font-size:12px;color:#60768b;margin-bottom:18px}.gantt-axis>div{position:relative;height:24px}.gantt-axis>div>span{position:absolute;transform:translateX(-50%);white-space:nowrap;top:0}.gantt-axis>div>span:first-child{transform:none}.gantt-axis>div>span:last-child{transform:translateX(-100%)}.gantt-row{padding:12px 0;border-top:1px solid #edf1f5;align-items:center}.gantt-label b{display:block;font-size:15px}.gantt-label small{display:block;margin-top:7px;color:#6c7f90;font-size:12px}.gantt-track{height:26px;position:relative;background:#f4f7fa;border-radius:4px}.gantt-track>i{position:absolute;top:-12px;bottom:-12px;border-left:1px dashed #d6e0e9}.gantt-bar{position:absolute;top:3px;height:20px;border-radius:4px;min-width:2px;box-shadow:0 2px 4px #1e40af16}.gantt-time>small{display:flex;justify-content:space-between;gap:10px;color:#6c7f90;font-size:12px;margin-top:8px}.gantt-time em{font-style:normal;font-weight:600}.gantt-legend{display:flex;gap:22px;align-items:center;flex-wrap:wrap;margin-top:22px;font-size:12px;color:#64748b}.gantt-legend span{display:flex;gap:6px;align-items:center}.gantt-legend i{width:10px;height:10px;border-radius:3px}.gantt-empty{padding:24px;color:#64748b}
</style>

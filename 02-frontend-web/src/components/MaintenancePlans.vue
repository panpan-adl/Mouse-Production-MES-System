<script setup lang="ts">
import {onMounted,ref} from 'vue'
import request from '@/utils/request'
type Row=Record<string,any>
const tasks=ref<Row[]>([]),plans=ref<Row[]>([]),error=ref(''),busy=ref(false)
const units:Record<string,string>={DAY:'天',WEEK:'周',MONTH:'月',YEAR:'年'}
async function load(){try{[plans.value,tasks.value]=await Promise.all([request.get<Row[]>('/maintenances/periodic/plans'),request.get<Row[]>('/maintenances/periodic/tasks')])}catch(e:any){error.value=e.message}}
async function generate(){busy.value=true;try{await request.post('/maintenances/periodic/generate');await load()}catch(e:any){error.value=e.message}finally{busy.value=false}}
async function toggle(p:Row){busy.value=true;try{await request.put(`/foundation/maintenance-plans/${p.id}`,{...p,enabled:p.enabled?0:1});await load()}catch(e:any){error.value=e.message}finally{busy.value=false}}
onMounted(load)
</script>
<template><section class="panel p-5 space-y-4"><div class="flex justify-between items-center"><div><h2 class="text-xl font-bold">周期保养计划</h2><p class="text-gray-500">按首次日期计算每轮计划，后台每分钟检查；提前生成待保养单，逾期保留待办。</p></div><router-link class="btn-primary" to="/foundation/maintenance-plans">配置保养周期</router-link></div><p v-if="error" role="alert" class="text-red-600">{{error}}</p><table class="data-table"><thead><tr><th>计划／设备</th><th>频率</th><th>负责人</th><th>下轮计划</th><th>提醒</th><th>状态与操作</th></tr></thead><tbody><tr v-for="p in plans" :key="p.id"><td>{{p.name}}／{{p.equipment_name}}</td><td>每{{p.frequency}}{{units[p.cycle_unit]}}一次</td><td>{{p.owner_name}}</td><td>{{String(p.next_at).replace('T',' ')}}</td><td>提前{{p.remind_days}}天</td><td><button :disabled="busy" @click="toggle(p)">{{p.enabled?'停用':'启用'}}</button><p class="text-red-600">{{p.last_error}}</p></td></tr><tr v-if="!plans.length"><td colspan="6">暂无周期计划，请先配置首次执行日期和频率。</td></tr></tbody></table><h3 class="font-bold mt-4">周期任务计划与实际执行</h3><table class="data-table"><thead><tr><th>保养计划</th><th>应执行时间</th><th>实际开始</th><th>实际结束</th><th>状态</th></tr></thead><tbody><tr v-for="t in tasks" :key="t.id"><td>{{t.plan_name}}</td><td>{{t.planned_at}}</td><td>{{t.actual_start||'未开始'}}</td><td>{{t.actual_end||'未完成'}}</td><td>{{t.status}}</td></tr></tbody></table><button :disabled="busy" class="btn-secondary" @click="generate">检查到期计划／重试失败任务</button></section></template>

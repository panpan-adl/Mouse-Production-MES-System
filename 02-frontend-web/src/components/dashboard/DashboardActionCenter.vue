<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
const auth=useAuthStore(),route=useRoute()
const loading=ref(false),counts=ref<Record<string,number|null>>({}),errors=ref<string[]>([])
const actions=computed(()=>[
  {key:'planning',title:'待排产任务',roles:[1,2,3,4,8],pending:(r:any)=>Number(r.task_status)<=1},
  {key:'reports',title:'待审批报工',roles:[1,2,3,4,5],pending:(r:any)=>r.status==='PENDING'},
  {key:'nonconformities',title:'不合格与返工',roles:[1,2,3,4,5],pending:(r:any)=>['OPEN','SCRAP_PENDING'].includes(r.status)||r.rework_status==='PENDING'},
  {key:'documents',title:'待处理仓储单据',roles:[1,2,4,5,6,7,10],pending:(r:any)=>['DRAFT','CONFIRMED'].includes(r.status)},
  {key:'checks',title:'到期点检保养',roles:[1,2,8],pending:(r:any)=>!['COMPLETED','CANCELLED','DONE'].includes(r.status)},
  {key:'alerts',title:'库存预警',roles:[1,2,5,6,10],pending:()=>true},
].filter(a=>Number(auth.effectiveRoleId)===9||a.roles.includes(Number(auth.effectiveRoleId))))
async function load(){
  if(loading.value)return
  loading.value=true;errors.value=[]
  await Promise.all(actions.value.map(async action=>{
    try{const rows=await request.get<Record<string,any>[]>(`/mes/${action.key}`);counts.value[action.key]=rows.filter(action.pending).length}
    catch{counts.value[action.key]=null;errors.value.push(action.title)}
  }))
  loading.value=false
}
onMounted(load)
</script>
<template>
  <section class="panel space-y-4 business-section action-center" aria-labelledby="action-title">
    <div class="flex justify-between gap-4 flex-wrap"><div><h2 id="action-title" class="text-xl font-bold">待办与异常</h2><p class="text-sm text-gray-500 mt-2">当前业务待办，不受上方统计日期影响。点击卡片查看并处理。</p></div><button class="btn-secondary" :disabled="loading" @click="load">{{loading?'正在刷新…':'刷新待办'}}</button></div>
    <p v-if="errors.length" role="status" class="text-amber-700 text-sm">{{errors.join('、')}}暂未加载，可点击卡片查看。</p>
    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-3"><router-link v-for="action in actions" :key="action.key" :to="{path:'/dashboard',query:{...route.query,view:'operations',tab:action.key}}" class="action-card border rounded-xl p-4 flex justify-between items-center"><span>{{action.title}}</span><strong class="text-2xl text-blue-600">{{counts[action.key]??'—'}} →</strong></router-link></div>
    <router-link :to="{path:'/dashboard',query:{...route.query,view:'operations',tab:'calendar'}}" class="text-sm text-blue-600">查看排班、线边库存及其他业务操作 →</router-link>
  </section>
</template>

<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import BusinessQueryPanel from '@/components/BusinessQueryPanel.vue'
import QueryTable from '@/components/QueryTable.vue'
import request from '@/utils/request'
type Row=Record<string,any>
const stock=ref<Row[]>([]),documents=ref<Row[]>([]),works=ref<Row[]>([]),trace=ref<Row[]>([]),selected=ref(''),search=ref(''),error=ref('')
const balances=computed(()=>stock.value.filter(r=>[r.material_name,r.warehouse_name,r.lot_no].join(' ').includes(search.value)).map(r=>({'物料':r.material_name,'仓库':r.warehouse_name,'库位':r.location_name,'批次':r.lot_no,'数量':r.quantity,'单位':r.unit})))
async function load(){try{[stock.value,documents.value,works.value]=await Promise.all([request.get<Row[]>('/fulfillment/stock'),request.get<Row[]>('/mes/documents'),request.get<Row[]>('/fulfillment/work-orders')])}catch(e:any){error.value=e.message}}
async function show(){if(!selected.value){trace.value=[];return}try{trace.value=await request.get(`/fulfillment/work-orders/${selected.value}/trace`)}catch(e:any){error.value=e.message}}
onMounted(load)
</script>
<template><MainLayout><div class="space-y-5"><h1 class="text-2xl font-bold">库存与批次追溯</h1><p class="text-gray-500">余额来自实际过账；草稿和未过账单据不改变库存。</p><p v-if="error" class="text-red-600">{{error}}</p><div class="flex gap-3"><input v-model="search" placeholder="按物料名称、仓库或批次搜索" class="input"/><button @click="load" class="btn-secondary">刷新</button></div><BusinessQueryPanel/><section class="panel p-4"><h2 class="font-bold mb-3">库存余额</h2><QueryTable :rows="balances"/></section><section class="panel p-4"><h2 class="font-bold mb-3">关联仓储单据</h2><QueryTable :rows="documents.filter(d=>!search||[d.code,d.name,d.warehouse_name].join(' ').includes(search)).map(d=>({'单号':d.code,'名称':d.name,'仓库':d.warehouse_name,'状态':d.status,'过账时间':d.posted_at}))"/></section><section class="panel p-4"><h2 class="font-bold mb-3">工单批次追溯</h2><select v-model="selected" @change="show" class="input mb-4"><option value="">选择工单查看关联流水</option><option v-for="w in works" :key="w.id" :value="w.id">{{w.code}} · {{w.product_name}}</option></select><QueryTable :rows="trace"/></section></div></MainLayout></template>

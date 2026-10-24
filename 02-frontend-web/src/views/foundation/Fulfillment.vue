<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'
import {useAuthStore} from '@/stores/auth'
import {canVisit} from '@/utils/permissions'
type Row=Record<string,any>
const auth=useAuthStore(),route=useRoute(),router=useRouter(),works=ref<Row[]>([]),purchases=ref<Row[]>([]),docs=ref<Row[]>([]),error=ref('')
const cards=computed(()=>[
 {title:'采购收货与退货',text:'已审批采购 → 到货登记 → 来料检验 → 收货过账；退货关联原采购批次。',links:[['采购订单','/purchase/order'],['采购收货','/purchase/receipts'],['供应商退货','/purchase/returns']],count:purchases.value.filter(p=>['APPROVED','PARTIAL_RECEIVED'].includes(p.status)).length,unit:'张采购单待收齐'},
 {title:'生产领料与退料',text:'生产工单 → 按BOM领料 → 工序生产；剩余原材料按工单批次退回。',links:[['生产工单','/production/work-orders'],['物料请求','/inventory/material-requests'],['生产领料','/inventory/production-issue'],['生产退料','/inventory/production-return']],count:works.value.filter(w=>['CREATED','IN_PROGRESS'].includes(w.status)).length,unit:'张工单进行中'},
 {title:'成品入库与发货',text:'制造与检验放行 → 成品入库 → 出货检验 → 发货过账。',links:[['制造与质量放行','/manufacturing'],['成品入库','/inventory/product-receipts'],['销售发货','/inventory/shipments'],['装箱管理','/inventory/packages']],count:works.value.filter(w=>w.status==='PRODUCTION_COMPLETED').length,unit:'张工单已生产完成'},
 {title:'库存与批次追溯',text:'按物料、仓库和批次查询余额，通过关联单据追溯每次库存变动。',links:[['库存与批次追溯','/inventory/trace']],count:docs.value.filter(d=>['DRAFT','CONFIRMED'].includes(d.status)).length,unit:'张仓储单据待处理'}].map(c=>({...c,links:c.links.filter(l=>canVisit(auth.effectiveRoleId,l[1]))})).filter(c=>c.links.length))
async function load(){try{[works.value,purchases.value,docs.value]=await Promise.all([request.get<Row[]>('/fulfillment/work-orders'),request.get<Row[]>('/fulfillment/purchases'),request.get<Row[]>('/mes/documents')])}catch(e:any){error.value=e.message}}
onMounted(()=>{const legacy:Record<string,string>={purchases:'/purchase/receipts',stock:'/inventory/trace',postings:'/inventory/trace',works:'/production/work-orders'};if(legacy[String(route.query.tab)])router.replace(legacy[String(route.query.tab)]);else load()})
</script>
<template><MainLayout><div class="space-y-6"><div class="flex justify-between"><div><h1 class="text-2xl font-bold">生产与仓储流程总览</h1><p class="text-gray-500 mt-2">根据业务阶段进入对应工作台，每张单据只登记一次，确认过账后同步库存与状态。</p></div><button @click="load" class="btn-secondary">刷新待办</button></div><p v-if="error" class="text-red-600" role="alert">{{error}}</p><div class="grid md:grid-cols-2 gap-5"><section v-for="c in cards" :key="c.title" class="panel p-6 space-y-4"><h2 class="font-bold text-xl">{{c.title}}</h2><p class="text-gray-500">{{c.text}}</p><p><strong class="text-3xl text-blue-600">{{c.count}}</strong> {{c.unit}}</p><div class="flex flex-wrap gap-3"><router-link v-for="l in c.links" :key="l[1]" :to="l[1]" class="btn-secondary">{{l[0]}} →</router-link></div></section></div></div></MainLayout></template>

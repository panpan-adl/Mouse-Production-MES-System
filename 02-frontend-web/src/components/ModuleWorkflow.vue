<script setup lang="ts">
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {canVisit} from '@/utils/permissions'
import {warehouseSections} from '@/utils/warehouseNavigation'
import {productionSections} from '@/utils/productionNavigation'
const route=useRoute(),auth=useAuthStore()
const group=computed(()=>{
 if(warehouseSections.some(s=>s.tabs.some(t=>t.path===route.path)))return {name:'仓储作业',note:'先维护存放位置，再办理单据；执行过账后核对库存和批次。',steps:[{title:'基础资料',detail:'仓库、库区、库位和物料',path:'/foundation/warehouses'},{title:'收发作业',detail:'入库、领退料、发货和装箱',path:'/inventory/in'},{title:'库存核对',detail:'余额、安全库存与批次来源',path:'/inventory/warehouse'}],rule:'保存草稿不改变库存；确认、检验与过账按单据要求依次完成。'}
 if(productionSections.some(s=>s.module==='设备管理'&&s.tabs.some(t=>t.path===route.path)))return {name:'设备保障',note:'以设备档案为起点，分别处理日常预防与故障维修。',steps:[{title:'设备档案',detail:'登记设备和查看运行状态',path:'/equipment/list'},{title:'日常预防',detail:'点检标准、周期计划及保养执行',path:'/equipment/periodic'},{title:'故障闭环',detail:'报修、分派、维修和费用记录',path:'/equipment/maintenance'}],rule:'点检保养与故障维修是两条处理路径；设备是否可用以实际业务状态为准。'}
 if(route.path==='/attendance')return {name:'考勤工作台',note:'先选业务日期与员工范围，再核对状态和工时，最后登记或修正明细。',steps:[{title:'范围与概况',detail:'统计当前载入的员工与考勤状态',path:''},{title:'筛选与核对',detail:'按员工、日期和状态查找记录',path:''},{title:'登记与修正',detail:'新增考勤或修改对应员工明细',path:''}],rule:'考勤记录用于人员出勤管理，不自动代替生产排班，也不自动生成工资。'}
 return null
})
const visible=computed(()=>group.value?.steps.map(s=>({...s,allowed:!s.path||(canVisit(Number(auth.effectiveRoleId),s.path)&&auth.hasMenu(s.path))}))||[])
</script>
<template><details v-if="group" class="module-workflow"><summary>{{group.name}} · 操作指引</summary><header><strong>{{group.name}}</strong><p>{{group.note}}</p></header><div class="workflow-steps"><div v-for="(step,i) in visible" :key="step.title"><span class="step-number">{{String(i+1).padStart(2,'0')}}</span><div><router-link v-if="step.path&&step.allowed" :to="step.path">{{step.title}} <span aria-hidden="true">↗</span></router-link><strong v-else>{{step.title}}</strong><p>{{step.detail}}</p></div></div></div><p class="workflow-rule">{{group.rule}}</p></details></template>
<style scoped>
.module-workflow{margin-bottom:24px;border:1px solid #e2d4bf;border-radius:14px;background:linear-gradient(115deg,#faf7f1,#f1e8da);padding:20px 24px;color:#66503a}.module-workflow summary{cursor:pointer;font-size:13px;font-weight:600;color:#80613e}.module-workflow[open] summary{margin-bottom:14px}.module-workflow header{display:flex;align-items:baseline;gap:20px;flex-wrap:wrap}.module-workflow header>strong{font-size:17px}.module-workflow p{font-size:12px;line-height:1.8;color:#8b7861;margin:0}.workflow-steps{display:grid;grid-template-columns:repeat(3,1fr);gap:20px;margin:18px 0}.workflow-steps>div{display:flex;gap:12px;padding-right:16px;border-right:1px solid #e3d5c0}.workflow-steps>div:last-child{border:0}.step-number{font-size:22px;color:#b29a78;font-family:Georgia,serif}.workflow-steps a,.workflow-steps strong{font-size:14px;font-weight:600}.workflow-steps a:hover{text-decoration:underline}.workflow-steps a:focus-visible{outline:2px solid #92734c;outline-offset:4px}.workflow-rule{border-top:1px solid #e3d5c0;padding-top:10px}@media(max-width:760px){.workflow-steps{grid-template-columns:1fr;gap:12px}.workflow-steps>div{border:0}.module-workflow{padding:18px}}
</style>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUpRight, Activity, PackageCheck, ShieldCheck, TriangleAlert } from 'lucide-vue-next'
import WarmFactoryScene from './WarmFactoryScene.vue'
import type { DashboardData } from '@/mock/dashboard'
const props=defineProps<{data:DashboardData;activeNode:string}>()
const emit=defineEmits<{select:[key:string]}>()
const totalInspected=computed(()=>props.data.qualityTrend.reduce((n,row)=>n+row.passed+row.failed,0))
const passed=computed(()=>props.data.qualityTrend.reduce((n,row)=>n+row.passed,0))
const yieldRate=computed(()=>totalInspected.value?Math.round(passed.value/totalInspected.value*1000)/10:null)
const equipmentTotal=computed(()=>(props.data.equipmentStatus||[]).reduce((n,row)=>n+row.value,0))
const equipmentNormal=computed(()=>(props.data.equipmentStatus||[]).filter(row=>/正常|运行|可用/.test(row.label)).reduce((n,row)=>n+row.value,0))
const riskOrders=computed(()=>props.data.workOrders.filter(row=>row.statusTone==='red'||row.statusTone==='yellow'))
const actual=computed(()=>props.data.hourlyOutput.reduce((n,row)=>n+row.actual,0))
const plan=computed(()=>props.data.hourlyOutput.reduce((n,row)=>n+row.plan,0))
const scheduledPlan=computed(()=>props.data.kpis.find(k=>k.key==='plan')?.value||plan.value)
const chartMax=computed(()=>Math.max(1,...props.data.hourlyOutput.flatMap(row=>[row.actual,row.plan])))
const curve=(key:'actual'|'plan')=>props.data.hourlyOutput.map((row,i)=>`${i*240/Math.max(1,props.data.hourlyOutput.length-1)},${70-row[key]/chartMax.value*58}`).join(' ')
</script>
<template>
  <div class="cockpit-grid">
    <aside class="cockpit-column">
      <section class="warm-card production-card"><div class="card-heading"><h2><Activity :size="14"/>产出与计划</h2><span>所选日期</span></div>
        <div class="output-summary"><strong>{{actual.toLocaleString()}}</strong><span>关键工序放行</span></div>
        <div class="target-row"><span>排程计划 {{scheduledPlan.toLocaleString()}}</span><strong>{{scheduledPlan?`${Math.round(actual/scheduledPlan*100)}%`:'暂无计划'}}</strong></div>
        <svg viewBox="0 0 240 85" class="mini-output" role="img" aria-label="计划与放行产量趋势"><path d="M0 70H240 M0 40H240 M0 10H240" stroke="#eee7dc" fill="none"/><polyline :points="curve('plan')" fill="none" stroke="#d5c6ad" stroke-width="1.5" stroke-dasharray="4 4"/><polyline :points="curve('actual')" fill="none" stroke="#b77945" stroke-width="2.5" stroke-linejoin="round"/></svg>
        <div class="chart-legend"><span><i></i>放行产量</span><span><i class="plan"></i>计划产量</span></div>
      </section>
      <section class="warm-card"><div class="card-heading"><h2><PackageCheck :size="14"/>物料与库存</h2><span>当前余额</span></div>
        <div class="readiness"><span>物料齐套率</span><strong>{{data.workOrders.length?data.materialReadinessRate:'—'}}<small v-if="data.workOrders.length">%</small></strong></div><div class="readiness-bar"><i :style="{width:`${Math.min(100,Math.max(0,data.materialReadinessRate))}%`}"></i></div>
        <div class="inventory-tiles"><div><strong>{{data.inventory.rawMaterial.toLocaleString()}}</strong><span>原材料</span></div><div><strong>{{data.inventory.wip.toLocaleString()}}</strong><span>在制品</span></div><div><strong>{{data.inventory.finishedGoods.toLocaleString()}}</strong><span>成品</span></div></div>
        <div class="inventory-warning"><TriangleAlert :size="12"/>{{data.inventory.lowStockCount}} 项低库存物料</div>
      </section>
    </aside>
    <WarmFactoryScene :data="data" :active-node="activeNode" @select="emit('select',$event)"/>
    <aside class="cockpit-column">
      <section class="warm-card"><div class="card-heading"><h2><ShieldCheck :size="14"/>质量与设备</h2><span>生产保障</span></div>
        <div class="assurance-grid"><div><strong>{{yieldRate===null?'—':yieldRate}}<small v-if="yieldRate!==null">%</small></strong><span>趋势样本合格率</span></div><div><strong>{{equipmentNormal}}<small>/{{equipmentTotal}}</small></strong><span>正常设备</span></div></div>
        <div class="equipment-state" v-for="row in data.equipmentStatus||[]" :key="row.label"><span>{{row.label}}</span><div><i :style="{width:`${equipmentTotal?row.value/equipmentTotal*100:0}%`}" :class="{fault:/故障|维修|异常/.test(row.label)}"></i></div><strong>{{row.value}}</strong></div>
        <p v-if="!equipmentTotal" class="no-data">暂无设备状态数据</p>
      </section>
      <section class="warm-card risk-card"><div class="card-heading"><h2><TriangleAlert :size="14"/>工单关注</h2><span>{{riskOrders.length}} 单</span></div>
        <div v-for="row in riskOrders.slice(0,3)" :key="row.id" class="risk-order"><div><strong>{{row.productModel}}</strong><span :class="{danger:row.statusTone==='red'}">{{row.status}}</span></div><small>{{row.id}}<ArrowUpRight :size="12"/></small><div class="risk-progress"><i :style="{width:`${row.planQuantity?Math.min(100,row.completedQuantity/row.planQuantity*100):0}%`}"></i></div></div>
        <p v-if="!riskOrders.length" class="no-data">当前工单暂无红黄状态提醒</p>
      </section>
    </aside>
  </div>
</template>
<style scoped>
.cockpit-grid{display:grid;grid-template-columns:minmax(205px,.85fr) minmax(420px,2.5fr) minmax(205px,.85fr);gap:16px;align-items:stretch}.cockpit-column{display:flex;flex-direction:column;gap:16px;min-width:0}.warm-card{background:#fffdf9;border:1px solid #e9e0d3;border-radius:14px;padding:19px;flex:1;min-width:0}.card-heading{display:flex;justify-content:space-between;align-items:center;gap:6px}.card-heading h2{display:flex;gap:7px;align-items:center;color:#786249;font-size:12px;font-weight:600}.card-heading>span{font-size:9px;color:#b2a38e;white-space:nowrap}.output-summary{margin-top:22px;display:flex;align-items:baseline;gap:10px}.output-summary strong{font-size:34px;color:#6d5034;letter-spacing:-.04em}.output-summary span{font-size:10px;color:#aa9981}.target-row{display:flex;justify-content:space-between;margin-top:6px;font-size:10px;color:#aa9981}.target-row strong{font-weight:500;color:#b77945}.mini-output{width:100%;height:85px;margin-top:14px}.chart-legend{display:flex;gap:18px;font-size:9px;color:#ac9c86}.chart-legend span{display:flex;align-items:center;gap:5px}.chart-legend i{width:12px;height:2px;background:#b77945}.chart-legend .plan{background:#d5c6ad}.readiness{display:flex;align-items:center;justify-content:space-between;margin-top:23px;font-size:11px;color:#9d8c75}.readiness strong{font-size:25px;color:#88714c;font-weight:500}.readiness small{font-size:12px}.readiness-bar,.risk-progress{height:4px;border-radius:3px;background:#eee6d9;overflow:hidden;margin-top:9px}.readiness-bar i{display:block;height:100%;background:#bba071}.inventory-tiles{display:grid;grid-template-columns:repeat(3,1fr);gap:4px;margin-top:24px}.inventory-tiles>div{display:flex;flex-direction:column;gap:7px}.inventory-tiles strong{font-size:16px;font-weight:500;color:#73634d}.inventory-tiles span{font-size:9px;color:#b2a18b}.inventory-warning{display:flex;align-items:center;gap:5px;font-size:10px;color:#b39159;margin-top:22px;padding-top:12px;border-top:1px solid #f0e9de}.assurance-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:23px 0}.assurance-grid>div{display:flex;flex-direction:column;gap:8px}.assurance-grid strong{font-size:25px;color:#778469;font-weight:500}.assurance-grid small{font-size:12px;color:#a99a85;margin-left:2px}.assurance-grid span{font-size:9px;color:#aa9c87}.equipment-state{display:flex;gap:9px;align-items:center;font-size:10px;margin-top:12px;color:#a49178}.equipment-state>div{flex:1;height:4px;background:#eee8df;border-radius:3px;overflow:hidden}.equipment-state i{display:block;height:100%;background:#99a288}.equipment-state i.fault{background:#bd8978}.equipment-state strong{font-weight:400}.risk-order{margin-top:19px;padding-bottom:12px;border-bottom:1px solid #f1e9dd}.risk-order:last-child{border:0}.risk-order>div:first-child{display:flex;justify-content:space-between;gap:6px;align-items:center}.risk-order strong{font-size:10px;font-weight:500;color:#826d50;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.risk-order span{font-size:9px;color:#af8a46;white-space:nowrap}.risk-order span.danger{color:#b77361}.risk-order small{display:flex;justify-content:space-between;color:#b2a38e;font-size:8px;margin-top:7px}.risk-progress{height:3px}.risk-progress i{display:block;height:100%;background:#c2a16c}.no-data{font-size:11px;color:#b0a08a;line-height:1.8;padding:25px 0}
@media(min-width:1600px){.cockpit-grid{grid-template-columns:minmax(220px,.85fr) minmax(550px,2.7fr) minmax(220px,.85fr)}}@media(max-width:1200px){.cockpit-grid{grid-template-columns:1fr 1fr}.cockpit-grid>:nth-child(2){grid-column:1/-1;grid-row:1}.cockpit-column{display:grid;grid-template-columns:1fr 1fr;grid-column:1/-1}}@media(max-width:650px){.cockpit-column{grid-template-columns:1fr}.warm-card{padding:17px}}
</style>

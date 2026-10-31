<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import MainLayout from '@/components/MainLayout.vue'
const OperationsWorkspace = defineAsyncComponent(()=>import('@/views/foundation/MesOperations.vue'))
const FulfillmentOverview = defineAsyncComponent(()=>import('@/views/foundation/Fulfillment.vue'))
const homeRoute = useRoute(), homeRouter = useRouter(), homeAuth = useAuthStore()
const canOpenOperations = computed(()=>canVisit(Number(homeAuth.effectiveRoleId),'/mes')&&homeAuth.hasMenu('/mes'))
const canOpenFlow = computed(()=>canVisit(Number(homeAuth.effectiveRoleId),'/fulfillment')&&homeAuth.hasMenu('/fulfillment'))
const operationOpen = computed(()=>canOpenOperations.value && (homeRoute.query.view==='operations'||Boolean(homeRoute.query.tab)))
const businessRefreshKey = ref(0)
watch(operationOpen,(open,previous)=>{if(previous&&!open){businessRefreshKey.value++;loadDashboard()}})
function closeOperations(){
  const {view,tab,...query}=homeRoute.query
  homeRouter.replace({path:'/dashboard',query})
}
import DashboardActionCenter from '@/components/dashboard/DashboardActionCenter.vue'
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue'
import DashboardKpiRow from '@/components/dashboard/DashboardKpiRow.vue'
import HourlyOutputChart from '@/components/dashboard/HourlyOutputChart.vue'
import WorkOrderStatusChart from '@/components/dashboard/WorkOrderStatusChart.vue'
import MaterialReadiness from '@/components/dashboard/MaterialReadiness.vue'
import WarmProductionCockpit from '@/components/dashboard/WarmProductionCockpit.vue'
import ActiveWorkOrderTable from '@/components/dashboard/ActiveWorkOrderTable.vue'
import QualityAnalysis from '@/components/dashboard/QualityAnalysis.vue'
import StockSafetyChart from '@/components/dashboard/StockSafetyChart.vue'
import EquipmentStatusChart from '@/components/dashboard/EquipmentStatusChart.vue'
import InventoryOverview from '@/components/dashboard/InventoryOverview.vue'
import RealtimeAlertList from '@/components/dashboard/RealtimeAlertList.vue'
import { getProductionDashboard } from '@/api/dashboard'
import type { DashboardData, DashboardFilters } from '@/mock/dashboard'
import request from '@/utils/request'

const localNow=new Date(); const today = `${localNow.getFullYear()}-${String(localNow.getMonth()+1).padStart(2,'0')}-${String(localNow.getDate()).padStart(2,'0')}`
const filters = ref<DashboardFilters>({
  date: today,
  shift: 'all',
  line: 'all',
})
const dashboard = ref<DashboardData | null>(null)
const loadError = ref('')
const currentTime = ref('')
const activeNode = ref('')
const resizeKey = ref(0)
const dateNotice = ref('')
const selectedDayHasWork = computed(()=>Boolean(dashboard.value?.workOrders.length))
const selectedDayHasInspection = computed(()=>Boolean(dashboard.value?.kpis.find(k=>k.key==='pass')?.value)||Boolean(dashboard.value?.qualityTrend.find(row=>row.label===filters.value.date.slice(5)&&row.passed+row.failed>0)))
const displayedKpis = computed(()=>dashboard.value?.kpis.map(k=>({...k,unavailable:(k.key==='rate'&&!selectedDayHasWork.value)||(k.key==='pass'&&!selectedDayHasInspection.value)}))||[])

async function initializeDashboard(){
  try{
    const [plans,works]=await Promise.all([request.get<Record<string,any>[]>('/production-plans'),request.get<Record<string,any>[]>('/fulfillment/work-orders')])
    const linked=new Set(works.map(work=>Number(work.plan_id)))
    const days=plans.filter(plan=>linked.has(Number(plan.id))).map(plan=>String(plan.startDate||plan.start_date||'').slice(0,10)).filter(day=>/^\d{4}-\d{2}-\d{2}$/.test(day)&&day<=today).sort()
    const latest=days.slice(-1)[0]
    if(latest&&latest!==today){filters.value.date=latest;dateNotice.value=`当前展示最近有生产记录的日期 ${latest}；今天为 ${today}。可通过右上方日期切换。`}
  }catch{/* A role without plan-list access can still use the dashboard date filter. */}
  await loadDashboard()
}


let refreshTimer = 0
let clockTimer = 0
const filteredWorkOrders = computed(() => {
  if (!dashboard.value) return []
  if (!activeNode.value) return dashboard.value.workOrders
  return dashboard.value.workOrders.filter((item) => item.nodeKey === activeNode.value)
})

function tickClock() {
  currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

async function loadDashboard() {
  try { dashboard.value = await getProductionDashboard(filters.value); loadError.value='' } catch(e:any) { dashboard.value=null; loadError.value=e.message || '看板数据加载失败' }
}

function updateFilters(nextFilters: DashboardFilters) {
  dateNotice.value=''
  filters.value = nextFilters
  activeNode.value = ''
  loadDashboard()
}

function startAutoRefresh() {
  window.clearInterval(refreshTimer)
  refreshTimer = window.setInterval(() => {
    if (document.visibilityState === 'visible') loadDashboard()
  }, 30000)
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    loadDashboard()
    startAutoRefresh()
  } else {
    window.clearInterval(refreshTimer)
  }
}

function handleResize() {
  resizeKey.value += 1
}

function toggleFullscreen() {
  const element = document.documentElement
  if (!document.fullscreenElement) {
    element.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

onMounted(() => {
  tickClock()
  initializeDashboard()
  startAutoRefresh()
  clockTimer = window.setInterval(tickClock, 1000)
  window.addEventListener('resize', handleResize)
  document.addEventListener('visibilitychange', handleVisibilityChange)

})

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer)
  window.clearInterval(clockTimer)
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)

})
</script>

<template>
  <MainLayout full-width>
    <div class="panorama-page">
      <DashboardHeader :filters="filters" :current-time="currentTime" :updated-at="dashboard?.updatedAt || ''" @update:filters="updateFilters" @refresh="loadDashboard" @fullscreen="toggleFullscreen" />
      <p v-if="loadError" role="alert" class="p-4 bg-red-50 text-red-700 rounded">{{loadError}}</p>
      <p class="dashboard-caption">从一件物料，到一只鼠标。<span>按选定日期与班次统计 · 库存为当前余额</span></p>
      <p v-if="dateNotice" class="data-date-notice" role="status">{{dateNotice}}</p>
      <DashboardKpiRow v-if="dashboard" :kpis="displayedKpis" />
      <template v-if="dashboard">
        <WarmProductionCockpit :data="dashboard" :active-node="activeNode" @select="activeNode=$event"/>
        <div class="execution-section-title"><div><span>PRODUCTION EXECUTION</span><h2>生产执行与业务洞察</h2></div><button v-if="activeNode" @click="activeNode=''">查看全部工单 ×</button><small v-else>全景统览 · 明细追踪</small></div>
        <div class="execution-row"><ActiveWorkOrderTable :rows="filteredWorkOrders" /><div class="execution-insights"><MaterialReadiness :readiness-rate="dashboard.materialReadinessRate" :shortages="dashboard.materialShortages" :has-work="selectedDayHasWork" /><QualityAnalysis :key="`quality-${resizeKey}`" :trend="dashboard.qualityTrend" :defects="dashboard.defectRanks" /></div></div>
        <div class="insight-row"><StockSafetyChart :rows="dashboard.materialSafety||[]" /><InventoryOverview :inventory="dashboard.inventory" /><RealtimeAlertList :alerts="dashboard.alerts" /></div>
        <details class="dashboard-details"><summary>展开产量趋势、工单分布与设备明细</summary><div class="overview-row"><HourlyOutputChart :key="`hourly-${resizeKey}`" :data="dashboard.hourlyOutput" /><WorkOrderStatusChart :data="dashboard.workOrderStatus" /></div><EquipmentStatusChart :rows="dashboard.equipmentStatus||[]" :details="dashboard.equipmentDetails||[]" /></details>
      </template>
      <DashboardActionCenter v-if="canOpenOperations" :key="`actions-${businessRefreshKey}`" />
      <FulfillmentOverview v-if="canOpenFlow" :key="`flow-${businessRefreshKey}`" embedded />
    </div>
    <div v-if="operationOpen" class="operations-backdrop" @click.self="closeOperations" @keydown.esc="closeOperations">
      <section class="operations-drawer" role="dialog" aria-modal="true" aria-label="业务待办处理" tabindex="-1">
        <div class="operations-toolbar"><h2>业务待办处理</h2><button class="btn-secondary" autofocus @click="closeOperations">关闭并返回看板</button></div>
        <OperationsWorkspace embedded />
      </section>
    </div>
  </MainLayout>
</template>
<style scoped>
.operations-backdrop{position:fixed;inset:0;background:#17202d66;z-index:40;display:flex;justify-content:flex-end}
.operations-drawer{width:min(1200px,95vw);height:100%;overflow:auto;background:#faf9f6;padding:24px;box-shadow:-10px 0 40px #0002}
.operations-toolbar{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:24px}.operations-toolbar h2{font-weight:600;font-size:20px}
.panorama-page{display:flex;flex-direction:column;gap:22px;min-width:0}
.dashboard-caption{display:flex;justify-content:space-between;color:#788aa0;font-size:11px;margin-top:-7px;gap:10px;flex-wrap:wrap}
.dashboard-caption>span{color:#98a5b6;font-size:10px}
.overview-row{display:grid;grid-template-columns:minmax(0,1.4fr) minmax(0,1fr);gap:20px}
.execution-row{display:grid;grid-template-columns:minmax(0,1.85fr) minmax(0,1fr);align-items:start;gap:20px}
.execution-insights{display:grid;gap:20px;min-width:0}
.insight-row{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;align-items:start}
.panorama-page :deep(.panel){padding:20px;border:1px solid #e4eaf2;border-radius:15px;background:#fff;box-shadow:0 4px 20px -14px #23415d30;min-width:0}
.panorama-page :deep(.panel-title){margin-bottom:20px;gap:12px}
.panorama-page :deep(.panel-title h2){font-size:13px;font-weight:600;color:#2c405a}
.panorama-page :deep(.kpi-row){gap:14px}
.panorama-page :deep(.kpi-card){border:1px solid #e4eaf2;border-radius:13px;background:white;min-height:110px;padding:19px 20px;box-shadow:0 4px 18px -12px #23415d25}
.panorama-page :deep(.kpi-card::before){width:3px;inset:22px auto 22px 0;border-radius:4px}
.panorama-page :deep(.kpi-label){font-size:11px;color:#8290a3}
.panorama-page :deep(.kpi-value strong){font-size:29px;font-weight:650;letter-spacing:-.045em;color:#20354e}
.panorama-page :deep(.kpi-value){margin-top:13px}
.panorama-page :deep(.chart-svg){height:210px;width:100%}
.panorama-page :deep(.panel.chart-panel){height:auto}
.panorama-page :deep(.quality-chart){height:115px;min-height:115px}
.panorama-page :deep(.status-list){grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-top:18px}
.panorama-page :deep(.status-row){padding:11px;border-radius:9px;background:#f5f8fc}
.panorama-page :deep(.status-row .name){white-space:nowrap;font-size:10px}
.panorama-page :deep(.flow-grid){gap:12px}
.panorama-page :deep(.flow-node){background:#f8fafd;border-radius:10px;padding:14px;min-height:110px}
.panorama-page :deep(.flow-node h3){white-space:normal;font-size:11px;line-height:1.7}
.panorama-page :deep(.alert-list){height:auto;max-height:300px;overflow:auto;gap:9px}
.panorama-page :deep(.alert-row){padding:11px;min-height:65px;background:#f8fafd}
.panorama-page :deep(.table-container){overflow:auto}
.panorama-page :deep(.order-id){white-space:nowrap;font-weight:600}
.panorama-page :deep(.dashboard-header){flex-wrap:wrap;gap:18px}
.panorama-page :deep(.dashboard-header h1){font-size:27px;font-weight:650;line-height:1.4;letter-spacing:-.035em;color:#20354e}
.panorama-page :deep(.eyebrow){font-size:9px;color:#93a2b7;letter-spacing:.15em;text-transform:uppercase;margin-bottom:5px}
.panorama-page :deep(.time-box),.panorama-page :deep(.icon-button){background:white;border-color:#e1e8f1;color:#7990ac}
.panorama-page :deep(.time-box strong){font-size:11px;color:#45627f}
.panorama-page :deep(.time-box.muted){display:none}
.panorama-page :deep(.header-controls){flex-wrap:wrap;justify-content:flex-start}
.panorama-page :deep(.insight-row>.text-slate-100){color:#40566d;background:white;border-color:#e4eaf2;border-radius:15px;padding:20px;box-shadow:0 4px 20px -14px #23415d30}
.panorama-page :deep(.insight-row .text-cyan-300){color:#257ad5}
.panorama-page :deep(.insight-row .text-slate-400){color:#91a0b3}
.panorama-page :deep(.insight-row .bg-slate-700){background:#edf1f6}
@media(max-width:1200px){.panorama-page :deep(.kpi-row){grid-template-columns:repeat(3,minmax(0,1fr))}.execution-row{grid-template-columns:minmax(0,1fr)}.execution-insights{grid-template-columns:repeat(2,minmax(0,1fr))}.insight-row{grid-template-columns:repeat(2,minmax(0,1fr))}.insight-row>:last-child{grid-column:1/-1}}
@media(max-width:800px){.overview-row,.execution-insights,.insight-row{grid-template-columns:minmax(0,1fr)}.panorama-page :deep(.flow-grid){grid-template-columns:repeat(3,minmax(0,1fr))}.panorama-page :deep(.panel){padding:15px}.panorama-page :deep(.kpi-row){grid-template-columns:repeat(2,minmax(0,1fr))}.panorama-page :deep(.kpi-card){padding:14px;min-height:95px}.panorama-page :deep(.kpi-value strong){font-size:24px}.panorama-page{gap:17px}.panorama-page :deep(.header-controls .control-input){width:100px}.panorama-page :deep(.time-box){display:none}}
.panorama-page{padding:24px;background:radial-gradient(ellipse at 50% 15%,#f4eddf 0,transparent 60%),#f8f5ef;border:1px solid #ede5d9;border-radius:18px;gap:20px}
.dashboard-caption{color:#a18b6c;font-size:12px;margin-top:-6px}.dashboard-caption>span{color:#b6a58e}
.panorama-page :deep(.dashboard-header h1){color:#574631;font-size:28px;letter-spacing:.025em}.panorama-page :deep(.eyebrow){color:#b18a58;letter-spacing:.2em}.panorama-page :deep(.control-input){background:#fffdf8;border-color:#e5d9c5;color:#8a7659;border-radius:7px;font-size:11px}.panorama-page :deep(.time-box),.panorama-page :deep(.icon-button){background:#fffdf8;border-color:#e5d9c5;color:#aa8b63}.panorama-page :deep(.time-box strong){color:#8e7350}
.panorama-page :deep(.kpi-card){background:#fffdf9;border-color:#e8dfd1;box-shadow:none;border-radius:12px;min-height:99px;padding:17px 19px}.panorama-page :deep(.kpi-card::before){background:#c29a68!important;width:2px}.panorama-page :deep(.kpi-label){color:#a69379}.panorama-page :deep(.kpi-value strong){color:#725639;font-size:29px}.panorama-page :deep(.kpi-value span){color:#af9b7c}.panorama-page :deep(.kpi-blue),.panorama-page :deep(.kpi-green),.panorama-page :deep(.kpi-yellow),.panorama-page :deep(.kpi-red){--kpi-accent:#b78a55}
.panorama-page :deep(.panel){background:#fffdf9;border-color:#e9e0d3;box-shadow:none;border-radius:14px}.panorama-page :deep(.panel-title h2){color:#80684a}.panorama-page :deep(.panel-title span){color:#b5a28b}.panorama-page :deep(.status-row),.panorama-page :deep(.alert-row){background:#f8f3e9}.panorama-page :deep(th){background:#f7f2e8;color:#a18b6c}.panorama-page :deep(td){border-color:#f0e9de}.panorama-page :deep(.order-id){color:#947045}
.execution-section-title{display:flex;justify-content:space-between;align-items:end;margin-top:8px;gap:12px}.execution-section-title span{font-size:8px;letter-spacing:.2em;color:#b8a58c}.execution-section-title h2{font-size:17px;color:#756044;margin-top:5px;font-weight:500}.execution-section-title small,.execution-section-title button{font-size:10px;color:#b0936c}
.execution-row{grid-template-columns:minmax(0,1fr)}.execution-insights{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));align-items:start}
.data-date-notice{font-size:12px;color:#937444;background:#f3ead8;border:1px solid #e9dcc3;border-radius:8px;padding:11px 15px;line-height:1.7}
.execution-insights{align-items:stretch}.execution-insights>.panel{min-height:230px;max-height:350px;overflow:auto}.execution-row :deep(.table-container){max-height:350px;overflow:auto}.execution-row :deep(thead th){position:sticky;top:0;z-index:1}
.insight-row{align-items:stretch}.insight-row>*{height:340px;overflow:hidden}.insight-row :deep(.alert-list){max-height:270px;overflow:auto}.insight-row :deep(.inventory-card){background:#f7f1e7;border:1px solid #eee3d2}.insight-row :deep(.inventory-card strong){color:#9a784b}.insight-row :deep(.inventory-card span){color:#ad997c}
.execution-insights :deep(.rate-track span){background:linear-gradient(90deg,#c3a071,#9caa8b)}.execution-insights :deep(.shortage-row){background:#f8f3ea}.execution-insights :deep(.shortage-row strong){color:#b07858}.execution-insights :deep(.pass-line){stroke:#8e9e7e}.execution-insights :deep(.pass-dot){stroke:#8e9e7e}.execution-insights :deep(.fail-line){stroke:#c08a73}.execution-insights :deep(.fail-dot){stroke:#c08a73}.execution-insights :deep(.defect-row i){background:#c39a83}
@media(max-width:800px){.execution-insights{grid-template-columns:1fr}.insight-row>*{height:auto;min-height:180px;max-height:360px}}
.dashboard-details{border:1px solid #e9e0d3;border-radius:12px;padding:18px;background:#fffdf9}.dashboard-details summary{cursor:pointer;font-size:12px;color:#a1835b}.dashboard-details[open] summary{margin-bottom:20px}.dashboard-details>.overview-row{margin-bottom:20px}
@media(max-width:800px){.panorama-page{padding:14px;border-radius:12px}.panorama-page :deep(.dashboard-header h1){font-size:24px}}
/* Business sections share the warm dashboard palette and compact typography. */
.panorama-page :deep(.business-section){font-size:12px;color:#8a7659}
.panorama-page :deep(.business-section h2){font-size:15px;font-weight:600;color:#80684a;letter-spacing:.02em;line-height:1.5}
.panorama-page :deep(.business-section p.text-gray-500){font-size:11px;line-height:1.8;color:#ad997c}
.panorama-page :deep(.business-section .btn-secondary){background:#fffdf8;border:1px solid #e5d9c5;border-radius:7px;color:#96794f;font-size:11px;padding:8px 12px;min-height:34px;box-shadow:none;transition:background .15s,border-color .15s}
.panorama-page :deep(.business-section .btn-secondary:hover){background:#f5eddf;border-color:#c8ac83}
.panorama-page :deep(.business-section .btn-secondary:focus-visible),.panorama-page :deep(.action-card:focus-visible){outline:2px solid #b78a55;outline-offset:3px}
.panorama-page :deep(.business-section .text-blue-600){color:#a1835b}
.panorama-page :deep(.action-card){background:#faf6ee;border-color:#e9e0d3;border-radius:10px;padding:16px;color:#8a7659;gap:12px;transition:background .15s,border-color .15s}
.panorama-page :deep(.action-card:hover){background:#f3eadb;border-color:#cfb48c}
.panorama-page :deep(.action-card strong){font-size:23px;font-weight:600;color:#947045;white-space:nowrap}
.panorama-page :deep(.action-center>a){display:inline-block;font-size:11px;color:#a1835b}
.panorama-page :deep(.fulfillment-section){border-top:1px solid #e9e0d3;padding-top:16px;container-type:inline-size}
.panorama-page :deep(.fulfillment-grid){gap:12px;margin-top:16px;align-items:stretch}
.panorama-page :deep(.flow-card){display:flex;flex-direction:column;gap:8px;padding:16px;min-width:0}
.panorama-page :deep(.flow-card>:not([hidden])~:not([hidden])){margin-top:0}
.panorama-page :deep(.flow-card h2){font-size:13px}
.panorama-page :deep(.flow-card strong){font-size:27px;font-weight:600;color:#947045;margin-right:5px}
.panorama-page :deep(.flow-card>div:last-child){margin-top:auto;padding-top:2px;gap:6px}
.panorama-page :deep(.flow-card .btn-secondary){padding:6px 9px;min-height:30px}
@container(min-width:1120px){.panorama-page :deep(.fulfillment-grid){grid-template-columns:repeat(4,minmax(0,1fr))}}
</style>

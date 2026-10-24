<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue'
import DashboardKpiRow from '@/components/dashboard/DashboardKpiRow.vue'
import HourlyOutputChart from '@/components/dashboard/HourlyOutputChart.vue'
import WorkOrderStatusChart from '@/components/dashboard/WorkOrderStatusChart.vue'
import MaterialReadiness from '@/components/dashboard/MaterialReadiness.vue'
import ProductionFlow from '@/components/dashboard/ProductionFlow.vue'
import ActiveWorkOrderTable from '@/components/dashboard/ActiveWorkOrderTable.vue'
import QualityAnalysis from '@/components/dashboard/QualityAnalysis.vue'
import StockSafetyChart from '@/components/dashboard/StockSafetyChart.vue'
import InventoryOverview from '@/components/dashboard/InventoryOverview.vue'
import RealtimeAlertList from '@/components/dashboard/RealtimeAlertList.vue'
import { getProductionDashboard } from '@/api/dashboard'
import type { DashboardData, DashboardFilters } from '@/mock/dashboard'

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
  loadDashboard()
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
  <MainLayout>
    <div class="panorama-page">
      <DashboardHeader :filters="filters" :current-time="currentTime" :updated-at="dashboard?.updatedAt || ''" @update:filters="updateFilters" @refresh="loadDashboard" @fullscreen="toggleFullscreen" />
      <p v-if="loadError" role="alert" class="p-4 bg-red-50 text-red-700 rounded">{{loadError}}</p>
      <p class="dashboard-caption">生产概况与关键业务指标<span>按选定日期与班次统计 · 库存为当前余额</span></p>
      <DashboardKpiRow v-if="dashboard" :kpis="dashboard.kpis" />
      <template v-if="dashboard">
        <div class="overview-row"><HourlyOutputChart :key="`hourly-${resizeKey}`" :data="dashboard.hourlyOutput" /><WorkOrderStatusChart :data="dashboard.workOrderStatus" /></div>
        <ProductionFlow :nodes="dashboard.flowNodes" :active-node="activeNode" @select-node="activeNode = $event" />
        <div class="execution-row"><ActiveWorkOrderTable :rows="filteredWorkOrders" /><div class="execution-insights"><MaterialReadiness :readiness-rate="dashboard.materialReadinessRate" :shortages="dashboard.materialShortages" /><QualityAnalysis :key="`quality-${resizeKey}`" :trend="dashboard.qualityTrend" :defects="dashboard.defectRanks" /></div></div>
        <div class="insight-row"><StockSafetyChart :rows="dashboard.materialSafety||[]" /><InventoryOverview :inventory="dashboard.inventory" /><RealtimeAlertList :alerts="dashboard.alerts" /></div>
      </template>
    </div>
  </MainLayout>
</template>
<style scoped>
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
</style>

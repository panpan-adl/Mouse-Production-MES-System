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
const canvasRef = ref<HTMLCanvasElement | null>(null)

let refreshTimer = 0
let clockTimer = 0
let particleCtx: CanvasRenderingContext2D | null = null
let animId = 0

interface Particle {
  x: number; y: number; z: number
  vx: number; vy: number
  r: number; a: number
  h: number; s: number; l: number
  tw: number; ts: number
}

const particles: Particle[] = []
const cs = [[210,80,70],[190,70,65],[240,60,70],[200,50,85],[260,70,65],[180,60,60]]

function rand(min: number, max: number) { return Math.random() * (max - min) + min }

function initParticles(W: number, H: number) {
  particles.length = 0
  for (let i = 0; i < 200; i++) {
    const z = Math.random(), zf = 1 - z
    const ci = cs[Math.random() * cs.length | 0]
    particles.push({
      x: Math.random() * W, y: Math.random() * H, z,
      vx: rand(-0.2 - zf * 0.5, 0.2 + zf * 0.5),
      vy: rand(-0.1 - zf * 0.3, 0.1 + zf * 0.3),
      r: 0.5 + zf * 2.5, a: 0.15 + zf * 0.5,
      h: ci[0] + rand(-15,15), s: ci[1] + rand(-15,15), l: ci[2] + rand(-15,15),
      tw: Math.random() * Math.PI * 2,
      ts: 0.005 + Math.random() * 0.02 * (1 - z * 0.5),
    })
  }
}

function startParticles() {
  const c = canvasRef.value
  if (!c) return
  const rect = c.parentElement!.getBoundingClientRect()
  c.width = rect.width
  c.height = rect.height
  particleCtx = c.getContext('2d')!
  initParticles(c.width, c.height)
  particleCtx.fillStyle = '#050510'
  particleCtx.fillRect(0, 0, c.width, c.height)
  drawParticles()
}

function drawParticles() {
  const c = canvasRef.value
  if (!c || !particleCtx) { animId = requestAnimationFrame(drawParticles); return }
  const W = c.width, H = c.height

  particleCtx.fillStyle = 'rgba(5,5,16,0.08)'
  particleCtx.fillRect(0, 0, W, H)

  for (const p of particles) {
    p.x += p.vx; p.y += p.vy
    if (p.x < -50) p.x = W + 50; if (p.x > W + 50) p.x = -50
    if (p.y < -50) p.y = H + 50; if (p.y > H + 50) p.y = -50
    p.tw += p.ts
  }

  for (const p of particles) {
    const blk = 0.85 + Math.sin(p.tw) * 0.15
    const al = Math.min(p.a * blk, 1)
    particleCtx.beginPath()
    particleCtx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    if (p.r > 1.5) {
      particleCtx.shadowColor = `hsla(${p.h},${p.s}%,${p.l}%,${al * 0.2})`
      particleCtx.shadowBlur = p.r * 2
    }
    particleCtx.fillStyle = `hsla(${p.h},${p.s}%,${p.l}%,${al})`
    particleCtx.fill()
    particleCtx.shadowBlur = 0
  }

  animId = requestAnimationFrame(drawParticles)
}

function stopParticles() {
  if (animId) cancelAnimationFrame(animId)
}

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
  startParticles()
})

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer)
  window.clearInterval(clockTimer)
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  stopParticles()
})
</script>

<template>
  <MainLayout>
    <div class="panorama-page">
      <canvas ref="canvasRef"></canvas>
      <DashboardHeader
        :filters="filters"
        :current-time="currentTime"
        :updated-at="dashboard?.updatedAt || ''"
        @update:filters="updateFilters"
        @refresh="loadDashboard"
        @fullscreen="toggleFullscreen"
      />

      <p v-if="loadError" role="alert" class="p-4 bg-red-50 text-red-700 rounded">{{loadError}}</p>
      <p class="text-sm text-slate-500 my-3">按选定日期排程的制造工单统计。白班08:00—20:00，夜班20:00—次日08:00；完成量按末道质检放行时间统计。库存显示当前余额。</p>
      <DashboardKpiRow v-if="dashboard" :kpis="dashboard.kpis" />

      <main v-if="dashboard" class="panorama-grid">
        <aside class="left-column">
          <HourlyOutputChart :key="`hourly-${resizeKey}`" :data="dashboard.hourlyOutput" />
          <WorkOrderStatusChart :data="dashboard.workOrderStatus" />
          <MaterialReadiness :readiness-rate="dashboard.materialReadinessRate" :shortages="dashboard.materialShortages" />
        </aside>

        <section class="center-column">
          <ProductionFlow
            :nodes="dashboard.flowNodes"
            :active-node="activeNode"
            @select-node="activeNode = $event"
          />
          <ActiveWorkOrderTable :rows="filteredWorkOrders" />
        </section>

        <aside class="right-column">
          <QualityAnalysis :key="`quality-${resizeKey}`" :trend="dashboard.qualityTrend" :defects="dashboard.defectRanks" />
          <StockSafetyChart :rows="dashboard.materialSafety||[]" />
              <InventoryOverview :inventory="dashboard.inventory" />
          <RealtimeAlertList :alerts="dashboard.alerts" />
        </aside>
      </main>
    </div>
  </MainLayout>
</template>

<style scoped>
.panorama-page {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 10px;
  height: calc(100vh - 4rem);
  margin: -24px;
  padding: 14px;
  overflow: hidden;
  position: relative;
  background: transparent;
}

.panorama-page canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  display: block;
}

.panorama-page > *:not(canvas) {
  position: relative;
  z-index: 1;
}

.panorama-grid {
  display: grid;
  grid-template-columns: 3fr 6fr 3fr;
  gap: 10px;
  min-height: 0;
}

.left-column,
.right-column,
.center-column {
  display: grid;
  gap: 10px;
  min-width: 0;
  min-height: 0;
}

.left-column {
  grid-template-rows: 1.05fr 0.72fr 1fr;
}

.center-column {
  grid-template-rows: 158px minmax(0, 1fr);
}

.right-column {
  grid-template-rows: 1.25fr 0.72fr 1fr;
}

@media (max-width: 1280px) {
  .panorama-page {
    gap: 8px;
    padding: 10px;
  }

  .panorama-grid,
  .left-column,
  .right-column,
  .center-column {
    gap: 8px;
  }

  .center-column {
    grid-template-rows: 126px minmax(0, 1fr);
  }
}
</style>

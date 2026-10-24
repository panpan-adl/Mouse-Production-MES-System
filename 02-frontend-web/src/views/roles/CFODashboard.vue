<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import {
  ArrowUpRight,
  Boxes,
  ClipboardCheck,
  DollarSign,
  RefreshCw,
  ShoppingCart,
  TrendingUp,
  Wallet,
  Wrench,
  FileText,
  Package,
  AlertTriangle,
} from 'lucide-vue-next'
import { financeApi, type FinanceGroupStat, type FinanceInventoryValueStat, type FinanceQualityLossStat, type FinanceOverview, type FinanceTrendStat } from '@/api/mes'

const router = useRouter()
const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)
const loading = ref(false)
const message = ref('')
const month = ref(new Date().toISOString().slice(0, 7))

const overview = ref<FinanceOverview>({ salesIncome: 0, purchaseExpense: 0, maintenanceExpense: 0, inventoryAmount: 0, grossProfit: 0 })
const inventoryValue = ref<FinanceInventoryValueStat>({ currentQuantity: 0, currentAmount: 0, lowStockCount: 0 })
const qualityLoss = ref<FinanceQualityLossStat>({ defectiveQuantity: 0, scrapQuantity: 0, reworkQuantity: 0 })
const trend = ref<FinanceTrendStat[]>([])
const purchaseSupplier = ref<FinanceGroupStat[]>([])
const purchaseMaterial = ref<FinanceGroupStat[]>([])

const currentMonth = new Date().toISOString().slice(0, 7)

const visibleTrend = computed(() =>
  trend.value
    .filter((item) => /^\d{4}-\d{2}$/.test(item.month) && item.month <= currentMonth)
    .sort((a, b) => a.month.localeCompare(b.month))
)

const hiddenTrendCount = computed(() => trend.value.length - visibleTrend.value.length)

const maxTrendValue = computed(() => {
  const values = visibleTrend.value.flatMap((item) => [Number(item.salesIncome || 0), Number(item.purchaseExpense || 0), Number(item.maintenanceExpense || 0)])
  return Math.max(1, ...values)
})

// ── 可导航入口 ──
const navEntries = [
  { label: '收入趋势分析', icon: TrendingUp, color: 'green', path: '/finance/revenue' },
  { label: '采购支出统计', icon: ShoppingCart, color: 'orange', path: '/finance/purchase' },
  { label: '质检损失报告', icon: ClipboardCheck, color: 'red', path: '/finance/quality-loss' },
  { label: '库存价值概览', icon: Boxes, color: 'blue', path: '/inventory/material' },
  { label: '月度财务报表', icon: FileText, color: 'indigo', path: '/finance/reports' },
  { label: '低库存预警', icon: AlertTriangle, color: 'yellow', path: '/inventory/material-requests' },
  { label: '供应商采购统计', icon: Package, color: 'cyan', path: '/purchase/supplier' },
  { label: '设备维修成本', icon: Wrench, color: 'slate', path: '/equipment/bills' },
]

function goTo(path: string) {
  router.push(path)
}

onMounted(loadFinance)

async function loadFinance() {
  loading.value = true
  message.value = ''
  try {
    const [overviewData, inventoryData, qualityData, trendData, supplierData, materialData] = await Promise.all([
      financeApi.overview(),
      financeApi.inventoryValue(),
      financeApi.qualityLoss(),
      financeApi.trendMonthly(),
      financeApi.purchaseSupplier(),
      financeApi.purchaseMaterial(),
    ])
    overview.value = overviewData
    inventoryValue.value = inventoryData
    qualityLoss.value = qualityData
    trend.value = trendData
    purchaseSupplier.value = supplierData
    purchaseMaterial.value = materialData
  } catch (error: any) {
    message.value = error.message || '财务数据加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMonthOverview() {
  if (!month.value) return
  loading.value = true
  try {
    overview.value = await financeApi.overviewByMonth(month.value)
    message.value = `已切换到 ${month.value} 月度总览`
  } catch (error: any) {
    message.value = error.message || '月度总览加载失败'
  } finally {
    loading.value = false
  }
}

function money(value?: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function percent(part?: number, total?: number) {
  if (!total) return '0.0%'
  return `${((Number(part || 0) / Number(total)) * 100).toFixed(1)}%`
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <!-- ── 头部横幅 ── -->
      <div class="bg-gradient-to-r from-emerald-600 to-teal-700 rounded-2xl p-8 text-white">
        <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div class="flex items-center">
            <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mr-6">
              <Wallet class="w-10 h-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">财务看板</h1>
              <p class="text-emerald-100 mt-2">欢迎回来，{{ currentUser?.realName || '财务负责人' }}</p>
              <p class="text-emerald-200 text-sm mt-1">销售、采购、维修、库存和质检损失统一汇总。</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <input v-model="month" type="month" class="px-3 py-2 rounded-lg text-gray-800 text-sm" />
            <button class="bg-white/20 hover:bg-white/30 text-white px-4 py-2 rounded-lg text-sm" @click="loadMonthOverview">查月度</button>
            <button class="bg-white/20 hover:bg-white/30 text-white px-4 py-2 rounded-lg text-sm inline-flex items-center gap-2" @click="loadFinance">
              <RefreshCw class="w-4 h-4" />
              全部
            </button>
          </div>
        </div>
      </div>

      <p v-if="message" class="text-sm text-blue-600">{{ message }}</p>

      <!-- ════════════════════════════════════ -->
      <!-- 上半部分：数据看板（只看不点）       -->
      <!-- ════════════════════════════════════ -->
      <div class="border-b border-gray-200 pb-1 mb-2">
        <h2 class="text-sm font-semibold text-gray-400 uppercase tracking-wider">数据总览</h2>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-4">
        <div class="stat-card">
          <div class="flex items-center justify-between">
            <div><p class="stat-label">销售收入</p><p class="stat-value text-green-600">{{ money(overview.salesIncome) }}</p></div>
            <TrendingUp class="w-8 h-8 text-green-500" />
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center justify-between">
            <div><p class="stat-label">采购支出</p><p class="stat-value text-orange-600">{{ money(overview.purchaseExpense) }}</p></div>
            <ShoppingCart class="w-8 h-8 text-orange-500" />
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center justify-between">
            <div><p class="stat-label">维修支出</p><p class="stat-value text-red-600">{{ money(overview.maintenanceExpense) }}</p></div>
            <Wrench class="w-8 h-8 text-red-500" />
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center justify-between">
            <div><p class="stat-label">库存金额</p><p class="stat-value text-blue-600">{{ money(overview.inventoryAmount) }}</p></div>
            <Boxes class="w-8 h-8 text-blue-500" />
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center justify-between">
            <div><p class="stat-label">毛利润</p><p class="stat-value" :class="overview.grossProfit >= 0 ? 'text-emerald-600' : 'text-red-600'">{{ money(overview.grossProfit) }}</p></div>
            <DollarSign class="w-8 h-8 text-emerald-500" />
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div class="panel xl:col-span-2">
          <div class="flex items-center justify-between mb-5">
            <h2 class="text-lg font-semibold text-gray-900">月度趋势</h2>
            <span v-if="loading" class="text-sm text-gray-400">加载中...</span>
          </div>
          <div v-if="hiddenTrendCount > 0" class="mb-4 rounded-xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            已忽略 {{ hiddenTrendCount }} 条未来月份/异常月份数据，趋势只展示 {{ currentMonth }} 及以前的真实业务月份。
          </div>
          <div v-if="visibleTrend.length === 0" class="empty-cell">暂无趋势数据</div>
          <div v-else class="space-y-4">
            <div v-for="item in visibleTrend" :key="item.month" class="grid grid-cols-12 gap-3 items-center">
              <div class="col-span-2 text-sm text-gray-500">{{ item.month }}</div>
              <div class="col-span-8 space-y-1">
                <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full bg-green-500" :style="{ width: `${Number(item.salesIncome || 0) / maxTrendValue * 100}%` }"></div>
                </div>
                <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full bg-orange-400" :style="{ width: `${Number(item.purchaseExpense || 0) / maxTrendValue * 100}%` }"></div>
                </div>
                <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full bg-red-400" :style="{ width: `${Number(item.maintenanceExpense || 0) / maxTrendValue * 100}%` }"></div>
                </div>
              </div>
              <div class="col-span-2 text-right text-sm font-semibold" :class="item.profit >= 0 ? 'text-emerald-600' : 'text-red-600'">
                {{ money(item.profit) }}
              </div>
            </div>
          </div>
          <div class="flex gap-5 mt-5 text-sm text-gray-500">
            <span class="inline-flex items-center gap-2"><i class="w-3 h-3 bg-green-500 rounded-full"></i>收入</span>
            <span class="inline-flex items-center gap-2"><i class="w-3 h-3 bg-orange-400 rounded-full"></i>采购</span>
            <span class="inline-flex items-center gap-2"><i class="w-3 h-3 bg-red-400 rounded-full"></i>维修</span>
          </div>
        </div>

        <div class="panel">
          <h2 class="text-lg font-semibold text-gray-900 mb-5">库存与质检损失</h2>
          <div class="space-y-4">
            <div class="p-4 rounded-xl bg-blue-50">
              <p class="text-sm text-gray-500">当前库存数量</p>
              <p class="text-2xl font-bold text-blue-600">{{ inventoryValue.currentQuantity }}</p>
              <p class="text-xs text-gray-500 mt-1">库存金额：{{ money(inventoryValue.currentAmount) }}</p>
            </div>
            <div class="p-4 rounded-xl bg-yellow-50">
              <p class="text-sm text-gray-500">低库存物料</p>
              <p class="text-2xl font-bold text-yellow-600">{{ inventoryValue.lowStockCount }}</p>
            </div>
            <div class="p-4 rounded-xl bg-red-50">
              <p class="text-sm text-gray-500">质检异常数量</p>
              <p class="text-2xl font-bold text-red-600">{{ qualityLoss.defectiveQuantity }}</p>
              <p class="text-xs text-gray-500 mt-1">报废 {{ qualityLoss.scrapQuantity }} / 返工 {{ qualityLoss.reworkQuantity }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ════════════════════════════════════ -->
      <!-- 下半部分：可导航功能入口            -->
      <!-- ════════════════════════════════════ -->
      <div class="border-b border-gray-200 pb-1 mb-2 mt-8">
        <h2 class="text-sm font-semibold text-gray-400 uppercase tracking-wider">功能入口</h2>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div
          v-for="entry in navEntries"
          :key="entry.label"
          class="group cursor-pointer rounded-2xl border border-gray-100 bg-white p-5 shadow-sm hover:shadow-md hover:border-gray-200 transition-all duration-200"
          @click="goTo(entry.path)"
        >
          <div class="flex items-center justify-between mb-3">
            <component :is="entry.icon" :class="[
              'w-8 h-8',
              entry.color === 'green' ? 'text-green-600' :
              entry.color === 'orange' ? 'text-orange-600' :
              entry.color === 'red' ? 'text-red-600' :
              entry.color === 'blue' ? 'text-blue-600' :
              entry.color === 'indigo' ? 'text-indigo-600' :
              entry.color === 'yellow' ? 'text-yellow-600' :
              entry.color === 'cyan' ? 'text-cyan-600' : 'text-slate-600'
            ]" />
            <ArrowUpRight class="w-4 h-4 text-gray-300 group-hover:text-gray-500 transition-colors" />
          </div>
          <p class="text-sm font-semibold text-gray-800">{{ entry.label }}</p>
          <p class="text-xs text-gray-400 mt-1 group-hover:text-gray-600 transition-colors">前往查看 →</p>
        </div>
      </div>

      <!-- 底部：按供应商 / 按物料采购（保留为信息，不设交互） -->
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6 mt-4">
        <div class="panel">
          <h2 class="text-lg font-semibold text-gray-900 mb-5">按供应商采购金额</h2>
          <div class="space-y-3">
            <div v-if="purchaseSupplier.length === 0" class="text-sm text-gray-400">暂无供应商采购统计</div>
            <div v-for="item in purchaseSupplier.slice(0, 8)" v-else :key="item.groupKey">
              <div class="flex justify-between text-sm mb-1">
                <span class="text-gray-700">{{ item.groupName }}</span>
                <span class="font-medium">{{ money(item.amount) }}</span>
              </div>
              <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-cyan-500" :style="{ width: percent(item.amount, purchaseSupplier[0]?.amount) }"></div>
              </div>
            </div>
          </div>
        </div>
        <div class="panel">
          <h2 class="text-lg font-semibold text-gray-900 mb-5">按物料采购金额</h2>
          <div class="space-y-3">
            <div v-if="purchaseMaterial.length === 0" class="text-sm text-gray-400">暂无物料采购统计</div>
            <div v-for="item in purchaseMaterial.slice(0, 8)" v-else :key="item.groupKey">
              <div class="flex justify-between text-sm mb-1">
                <span class="text-gray-700">{{ item.groupName }}</span>
                <span class="font-medium">{{ money(item.amount) }}</span>
              </div>
              <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-indigo-500" :style="{ width: percent(item.amount, purchaseMaterial[0]?.amount) }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

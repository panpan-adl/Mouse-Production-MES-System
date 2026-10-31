<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import FinanceScreen from '@/components/FinanceScreen.vue'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import {
  Boxes,
  ClipboardCheck,
  ShoppingCart,
  Wrench,
  FileText,
} from 'lucide-vue-next'
import { financeApi, type FinanceGroupStat, type FinanceInventoryValueStat, type FinanceQualityLossStat, type FinanceOverview, type FinanceTrendStat } from '@/api/mes'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const message = ref('')
const month = ref(`${new Date().getFullYear()}-${String(new Date().getMonth()+1).padStart(2,'0')}`)
const scope=ref('全部业务')
const scopeLabel=computed(()=>scope.value==='全部业务'?'全部业务':`${scope.value} 月度`)
const chartMonths=computed(()=>visibleTrend.value.slice(-12))
const visibleEntries=computed(()=>navEntries.filter(entry=>router.resolve(entry.path).matched.length&&canVisit(Number(authStore.effectiveRoleId),entry.path)&&authStore.hasMenu(entry.path)))

const overview = ref<FinanceOverview>({ salesIncome: 0, purchaseExpense: 0, maintenanceExpense: 0, inventoryAmount: 0, grossProfit: 0 })
const inventoryValue = ref<FinanceInventoryValueStat>({ currentQuantity: 0, currentAmount: 0, lowStockCount: 0 })
const qualityLoss = ref<FinanceQualityLossStat>({ defectiveQuantity: 0, scrapQuantity: 0, reworkQuantity: 0 })
const trend = ref<FinanceTrendStat[]>([])
const purchaseSupplier = ref<FinanceGroupStat[]>([])
const purchaseMaterial = ref<FinanceGroupStat[]>([])

const currentMonth = `${new Date().getFullYear()}-${String(new Date().getMonth()+1).padStart(2,'0')}`

const visibleTrend = computed(() =>
  trend.value
    .filter((item) => /^\d{4}-\d{2}$/.test(item.month) && item.month <= currentMonth)
    .sort((a, b) => a.month.localeCompare(b.month))
)

// ── 可导航入口 ──
const navEntries = [
  { label: '销售订单', icon: FileText, path: '/order/list' },
  { label: '采购订单', icon: ShoppingCart, path: '/purchase/order' },
  { label: '库存与批次', icon: Boxes, path: '/inventory/trace' },
  { label: '质量记录', icon: ClipboardCheck, path: '/quality/check' },
  { label: '维修费用单', icon: Wrench, path: '/equipment/bills' },
]

function goTo(path: string) {
  router.push(path)
}

const autoRefresh = ref(true)
const updatedAt = ref('')
let refreshTimer: ReturnType<typeof setInterval> | undefined
let requestVersion = 0
const refreshOnFocus = () => { if (autoRefresh.value && !loading.value && !document.hidden) void loadFinance() }
onMounted(() => {
  void loadFinance()
  refreshTimer = setInterval(refreshOnFocus, 30000)
  window.addEventListener('focus', refreshOnFocus)
  document.addEventListener('visibilitychange', refreshOnFocus)
})
onUnmounted(() => {
  requestVersion++
  clearInterval(refreshTimer)
  window.removeEventListener('focus', refreshOnFocus)
  document.removeEventListener('visibilitychange', refreshOnFocus)
})
function showAll() { scope.value = '全部业务'; void loadFinance() }
function selectMonth(value: string) { month.value = value; void loadMonthOverview() }


async function loadFinance() {
  const version = ++requestVersion
  const selectedScope = scope.value
  loading.value = true
  message.value = ''
  try {
    const [overviewData, inventoryData, qualityData, trendData, supplierData, materialData] = await Promise.all([
      selectedScope === '全部业务' ? financeApi.overview() : financeApi.overviewByMonth(selectedScope),
      financeApi.inventoryValue(),
      financeApi.qualityLoss(),
      financeApi.trendMonthly(),
      financeApi.purchaseSupplier(),
      financeApi.purchaseMaterial(),
    ])
    if (version !== requestVersion) return
    overview.value = overviewData
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', {hour12:false})
    inventoryValue.value = inventoryData
    qualityLoss.value = qualityData
    trend.value = trendData
    purchaseSupplier.value = supplierData
    purchaseMaterial.value = materialData
  } catch (error: any) {
    if (version === requestVersion) message.value = error.message || '财务数据加载失败；保留上次成功数据'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

async function loadMonthOverview() {
  if (!/^\d{4}-\d{2}$/.test(month.value) || month.value > currentMonth) {
    message.value = '请选择有效月份，且不能晚于本月'; return
  }
  scope.value = month.value
  await loadFinance()
}

</script>

<template>
  <MainLayout full-width><FinanceScreen :overview="overview" :inventory="inventoryValue" :quality="qualityLoss" :trend="chartMonths" :suppliers="purchaseSupplier" :materials="purchaseMaterial" :loading="loading" :updated-at="updatedAt" :scope="scopeLabel" :message="message" :entries="visibleEntries" @refresh="loadFinance" @all="showAll" @month="selectMonth" @navigate="goTo"/></MainLayout>
</template>

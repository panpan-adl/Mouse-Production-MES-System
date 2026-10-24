<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { ArrowDownCircle, ArrowUpCircle, ClipboardCheck, Package, ShoppingCart } from 'lucide-vue-next'
import {
  inventoryApi,
  materialApi,
  materialRequestApi,
  purchaseOrderApi,
  stockInApi,
  stockOutApi,
  type Inventory,
  type Material,
  type MaterialRequest,
  type PurchaseOrder,
  type StockIn,
  type StockOut,
} from '@/api/mes'

interface RecentRecord {
  no: string
  type: 'IN' | 'OUT'
  businessType: string
  quantity: number
  time?: string
}

const authStore = useAuthStore()
const router = useRouter()
const currentUser = computed(() => authStore.currentUser)
const message = ref('')
const materials = ref<Material[]>([])
const lowStocks = ref<Inventory[]>([])
const stockIns = ref<StockIn[]>([])
const stockOuts = ref<StockOut[]>([])
const purchaseOrders = ref<PurchaseOrder[]>([])
const materialRequests = ref<MaterialRequest[]>([])

const totalInQuantity = computed(() => stockIns.value.reduce((sum, item) => sum + Number(item.totalQuantity || 0), 0))
const totalOutQuantity = computed(() => stockOuts.value.reduce((sum, item) => sum + Number(item.totalQuantity || 0), 0))
const pendingArrivals = computed(() => purchaseOrders.value.filter((item) => item.status !== 'RECEIVED'))
const pendingMaterialRequests = computed(() =>
  materialRequests.value.filter((item) => item.requestStatus === 'SHORTAGE' || item.requestStatus === 'PURCHASE_REQUESTED')
)

const recentRecords = computed<RecentRecord[]>(() => {
  const ins = stockIns.value.map((item) => ({
    no: item.stockInNo,
    type: 'IN' as const,
    businessType: item.stockInType,
    quantity: Number(item.totalQuantity || 0),
    time: item.createdAt,
  }))
  const outs = stockOuts.value.map((item) => ({
    no: item.stockOutNo,
    type: 'OUT' as const,
    businessType: item.stockOutType,
    quantity: Number(item.totalQuantity || 0),
    time: item.createdAt,
  }))
  return [...ins, ...outs]
    .sort((a, b) => new Date(b.time || '').getTime() - new Date(a.time || '').getTime())
    .slice(0, 5)
})

onMounted(loadDashboard)

async function loadDashboard() {
  message.value = ''
  try {
    const [materialList, lowStockList, stockInList, stockOutList, purchaseOrderList, materialRequestList] = await Promise.all([
      materialApi.list(),
      inventoryApi.lowStock(),
      stockInApi.list(),
      stockOutApi.list(),
      purchaseOrderApi.list(),
      materialRequestApi.list(),
    ])
    materials.value = materialList
    lowStocks.value = lowStockList
    stockIns.value = stockInList
    stockOuts.value = stockOutList
    purchaseOrders.value = purchaseOrderList
    materialRequests.value = materialRequestList
  } catch (error: any) {
    message.value = error.message || '仓管员首页数据加载失败'
  }
}

function materialName(id?: number) {
  return materials.value.find((item) => item.id === Number(id))?.materialName || `物料ID：${id || '-'}`
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}
</script>

<template>
  <MainLayout>
    <div class="p-6">
      <div class="bg-gradient-to-r from-amber-600 to-amber-700 rounded-2xl p-8 text-white mb-8">
        <div class="flex items-center justify-between gap-4">
          <div class="flex items-center">
            <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mr-6">
              <Package class="w-10 h-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">仓管员工作台</h1>
              <p class="text-amber-100 mt-2">欢迎回来，{{ currentUser?.realName }}。这里处理采购到货、库存预警和生产用料请求。</p>
              <p class="text-amber-200 text-sm mt-1">工号：{{ currentUser?.userCode }} | 职务：{{ currentUser?.roleName }}</p>
            </div>
          </div>
          <button class="px-4 py-2 rounded-xl bg-white text-amber-700 font-medium hover:bg-amber-50" @click="loadDashboard">刷新</button>
        </div>
      </div>

      <p v-if="message" class="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{{ message }}</p>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <button class="bg-white rounded-xl p-6 shadow-sm border border-gray-100 text-left" @click="router.push('/inventory/material')">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">库存物料</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">{{ materials.length }}</p>
            </div>
            <div class="w-14 h-14 bg-amber-50 rounded-xl flex items-center justify-center">
              <Package class="w-7 h-7 text-amber-600" />
            </div>
          </div>
        </button>

        <button class="bg-white rounded-xl p-6 shadow-sm border border-gray-100 text-left" @click="router.push('/inventory/in')">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">入库总量</p>
              <p class="text-3xl font-bold text-green-600 mt-2">{{ totalInQuantity }}</p>
            </div>
            <div class="w-14 h-14 bg-green-50 rounded-xl flex items-center justify-center">
              <ArrowDownCircle class="w-7 h-7 text-green-600" />
            </div>
          </div>
        </button>

        <button class="bg-white rounded-xl p-6 shadow-sm border border-gray-100 text-left" @click="router.push('/inventory/out')">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">出库总量</p>
              <p class="text-3xl font-bold text-red-600 mt-2">{{ totalOutQuantity }}</p>
            </div>
            <div class="w-14 h-14 bg-red-50 rounded-xl flex items-center justify-center">
              <ArrowUpCircle class="w-7 h-7 text-red-600" />
            </div>
          </div>
        </button>

        <button class="bg-white rounded-xl p-6 shadow-sm border border-gray-100 text-left" @click="router.push('/inventory/material-requests')">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">待处理物料请求</p>
              <p class="text-3xl font-bold text-blue-600 mt-2">{{ pendingMaterialRequests.length }}</p>
            </div>
            <div class="w-14 h-14 bg-blue-50 rounded-xl flex items-center justify-center">
              <ClipboardCheck class="w-7 h-7 text-blue-600" />
            </div>
          </div>
        </button>
      </div>

      <div class="mt-8 grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100 flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-800">采购到货状态</h2>
            <button class="text-sm text-blue-600" @click="router.push('/purchase/order')">查看采购单</button>
          </div>
          <div class="p-6 space-y-3">
            <div v-if="pendingArrivals.length === 0" class="text-sm text-gray-500">暂无未到货采购单</div>
            <div v-for="order in pendingArrivals.slice(0, 4)" v-else :key="order.id" class="p-4 bg-yellow-50 rounded-xl">
              <p class="font-medium text-gray-800">{{ order.poNo }}</p>
              <p class="text-sm text-gray-500 mt-1">预计到货：{{ order.expectedDate || '-' }}</p>
              <p class="text-sm text-yellow-700 mt-1">状态：未到货，到货后可选择仓库和库位办理入库</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100 flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-800">库存预警</h2>
            <button class="text-sm text-blue-600" @click="router.push('/inventory/warehouse')">查看库存</button>
          </div>
          <div class="p-6 space-y-3">
            <div v-if="lowStocks.length === 0" class="text-sm text-gray-500">暂无低库存物料</div>
            <div v-for="stock in lowStocks.slice(0, 4)" v-else :key="stock.id" class="p-4 bg-yellow-50 rounded-xl">
              <p class="font-medium text-gray-800">{{ materialName(stock.materialId) }}</p>
              <p class="text-sm text-gray-500 mt-1">当前库存：{{ stock.quantity }}</p>
              <p class="text-sm text-yellow-700 mt-1">低于安全库存时，可登记物料请求并发送采购</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100 flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-800">最近出入库记录</h2>
            <ShoppingCart class="w-5 h-5 text-gray-400" />
          </div>
          <div class="p-6 space-y-3">
            <div v-if="recentRecords.length === 0" class="text-sm text-gray-500">暂无出入库记录</div>
            <div
              v-for="record in recentRecords"
              v-else
              :key="record.no"
              :class="record.type === 'IN' ? 'bg-green-50' : 'bg-red-50'"
              class="flex items-center justify-between p-4 rounded-xl"
            >
              <div>
                <p class="font-medium text-gray-800">{{ record.no }}</p>
                <p class="text-sm text-gray-500">{{ record.businessType }} / 数量 {{ record.quantity }}</p>
                <p class="text-xs text-gray-400 mt-1">{{ formatDateTime(record.time) }}</p>
              </div>
              <span :class="record.type === 'IN' ? 'text-green-600' : 'text-red-600'" class="text-sm">
                {{ record.type === 'IN' ? '入库' : '出库' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

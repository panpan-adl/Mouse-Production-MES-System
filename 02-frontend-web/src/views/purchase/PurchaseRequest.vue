<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { FilePlus2, RefreshCw, Search, X } from 'lucide-vue-next'
import {
  materialRequestApi,
  purchaseOrderApi,
  supplierApi,
  type MaterialRequest,
  type Supplier,
} from '@/api/mes'
import { useAuthStore } from '@/stores/auth'

type RequestStatus = '' | 'PENDING_CHECK' | 'ENOUGH' | 'SHORTAGE' | 'PURCHASE_REQUESTED' | 'PURCHASE_ORDERED'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const keyword = ref('')
const statusFilter = ref<RequestStatus>('PURCHASE_REQUESTED')
const requests = ref<MaterialRequest[]>([])
const suppliers = ref<Supplier[]>([])
const showOrderForm = ref(false)
const currentRequest = ref<MaterialRequest | null>(null)

const orderForm = reactive({
  supplierId: 0,
  expectedDate: '',
  unitPrice: 0,
  remark: '',
})

const filteredRequests = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return requests.value.filter((item) => {
    const matchStatus = !statusFilter.value || item.requestStatus === statusFilter.value
    const matchKeyword =
      !word ||
      item.requestNo?.toLowerCase().includes(word) ||
      item.sourceTaskNo?.toLowerCase().includes(word) ||
      item.productName?.toLowerCase().includes(word) ||
      item.materialCode?.toLowerCase().includes(word) ||
      item.materialName?.toLowerCase().includes(word)
    return matchStatus && matchKeyword
  })
})

const pendingPurchaseCount = computed(() => requests.value.filter((item) => item.requestStatus === 'PURCHASE_REQUESTED').length)
const orderedCount = computed(() => requests.value.filter((item) => item.requestStatus === 'PURCHASE_ORDERED').length)
const shortageTotal = computed(() =>
  requests.value
    .filter((item) => item.requestStatus === 'PURCHASE_REQUESTED')
    .reduce((sum, item) => sum + Number(item.shortageQuantity || 0), 0)
)

onMounted(async () => {
  await Promise.all([loadRequests(), loadSuppliers()])
})

async function loadRequests() {
  loading.value = true
  message.value = ''
  try {
    requests.value = await materialRequestApi.list()
  } catch (error: any) {
    message.value = error.message || '采购需求加载失败，请确认后端已重启并执行最新数据库脚本。'
  } finally {
    loading.value = false
  }
}

async function loadSuppliers() {
  try {
    suppliers.value = await supplierApi.search({ status: 1 })
    orderForm.supplierId = suppliers.value[0]?.id || 0
  } catch {
    suppliers.value = []
  }
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING_CHECK: '待确认',
    ENOUGH: '库存充足',
    SHORTAGE: '库存不足',
    PURCHASE_REQUESTED: '待采购下单',
    PURCHASE_ORDERED: '已转采购单',
  }
  return map[status || ''] || '未知'
}

function statusClass(status?: string) {
  const map: Record<string, string> = {
    PENDING_CHECK: 'tag-yellow',
    ENOUGH: 'tag-green',
    SHORTAGE: 'tag-red',
    PURCHASE_REQUESTED: 'tag-blue',
    PURCHASE_ORDERED: 'tag-gray',
  }
  return map[status || ''] || 'tag-gray'
}

function openCreateOrder(item: MaterialRequest) {
  if (Number(item.shortageQuantity || 0) <= 0) {
    message.value = '该物料请求没有缺料数量，不需要生成采购单。'
    return
  }
  currentRequest.value = item
  Object.assign(orderForm, {
    supplierId: suppliers.value[0]?.id || 0,
    expectedDate: '',
    unitPrice: 0,
    remark: `来源物料请求 ${item.requestNo}，生产任务 ${item.sourceTaskNo || '-'}`,
  })
  showOrderForm.value = true
}

async function createPurchaseOrder() {
  if (!currentRequest.value?.id) return
  if (!orderForm.supplierId) {
    message.value = '请选择供应商。'
    return
  }
  if (Number(orderForm.unitPrice || 0) < 0) {
    message.value = '单价不能小于 0。'
    return
  }
  const shortageQuantity = Number(currentRequest.value.shortageQuantity || 0)
  if (shortageQuantity <= 0) {
    message.value = '缺料数量必须大于 0。'
    return
  }
  saving.value = true
  try {
    const unitPrice = Number(orderForm.unitPrice || 0)
    const amount = shortageQuantity * unitPrice
    await purchaseOrderApi.create({
      poNo: `PO-${Date.now()}`,
      supplierId: Number(orderForm.supplierId),
      orderDate: new Date().toISOString().slice(0, 10),
      expectedDate: orderForm.expectedDate,
      totalAmount: amount,
      status: 'DRAFT',
      remark: orderForm.remark,
      createdBy: authStore.currentUser?.id || 1,
      items: [
        {
          materialId: Number(currentRequest.value.materialId),
          quantity: shortageQuantity,
          unitPrice,
          amount,
          receivedQuantity: 0,
          remark: `由物料请求 ${currentRequest.value.requestNo} 转采购`,
        },
      ],
    })
    await materialRequestApi.markPurchaseOrdered(currentRequest.value.id)
    message.value = `已根据 ${currentRequest.value.requestNo} 生成采购订单，需求状态已更新为“已转采购单”。`
    completeDrafts();showOrderForm.value = false
    await loadRequests()
  } catch (error: any) {
    message.value = error.message || '生成采购订单失败'
  } finally {
    saving.value = false
  }
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

const completeDrafts=useFormDrafts({showOrderForm:{open:showOrderForm,fields:{orderForm},record:()=>currentRequest.value?.id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">采购需求</h1>
          <p class="text-gray-500 mt-1">这里接收仓管员发送的缺料请求；采购员确认供应商后生成采购订单。</p>
        </div>
        <button class="btn-secondary" @click="loadRequests">
          <RefreshCw class="w-4 h-4" />
          刷新
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <button class="stat-card text-left" @click="statusFilter = 'PURCHASE_REQUESTED'">
          <p class="stat-label">待采购下单</p>
          <p class="stat-value text-blue-600">{{ pendingPurchaseCount }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'PURCHASE_ORDERED'">
          <p class="stat-label">已转采购单</p>
          <p class="stat-value text-gray-700">{{ orderedCount }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'PURCHASE_REQUESTED'">
          <p class="stat-label">待采购缺口总量</p>
          <p class="stat-value text-red-600">{{ shortageTotal }}</p>
        </button>
      </div>

      <div class="panel">
        <div class="mb-5 overflow-x-auto pb-1">
          <div class="grid min-w-[820px] grid-cols-[minmax(340px,1fr)_210px_92px_92px] items-center gap-3">
            <label class="relative">
              <Search class="search-icon" />
              <input v-model="keyword" class="input pl-10" placeholder="搜索请求编号、来源任务、产品或物料" />
            </label>
            <select v-model="statusFilter" class="input">
              <option value="">全部状态</option>
              <option value="PURCHASE_REQUESTED">待采购下单</option>
              <option value="PURCHASE_ORDERED">已转采购单</option>
              <option value="SHORTAGE">库存不足未发送</option>
              <option value="ENOUGH">库存充足</option>
            </select>
            <button class="btn-primary w-full" @click="loadRequests"><Search class="w-4 h-4" />查询</button>
            <button class="btn-secondary w-full" @click="loadRequests"><RefreshCw class="w-4 h-4" />刷新</button>
          </div>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[1080px]">
            <thead>
              <tr>
                <th>请求编号</th>
                <th>来源任务</th>
                <th>产品</th>
                <th>物料</th>
                <th>需求数量</th>
                <th>当前库存</th>
                <th>缺口</th>
                <th>状态</th>
                <th>发送时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10" class="empty-cell">正在加载采购需求...</td>
              </tr>
              <tr v-else-if="filteredRequests.length === 0">
                <td colspan="10" class="empty-cell">暂无采购需求</td>
              </tr>
              <tr v-for="item in filteredRequests" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.requestNo }}</td>
                <td>{{ item.sourceTaskNo || '-' }}</td>
                <td>{{ item.productName || '-' }}</td>
                <td>{{ item.materialCode }} / {{ item.materialName }}</td>
                <td>{{ item.requiredQuantity }}</td>
                <td>{{ item.availableQuantity || 0 }}</td>
                <td class="font-medium" :class="Number(item.shortageQuantity || 0) > 0 ? 'text-red-600' : 'text-green-600'">
                  {{ item.shortageQuantity || 0 }}
                </td>
                <td><span :class="statusClass(item.requestStatus)">{{ statusText(item.requestStatus) }}</span></td>
                <td>{{ formatDateTime(item.updatedAt || item.createdAt) }}</td>
                <td class="text-right">
                  <button
                    v-if="item.requestStatus === 'PURCHASE_REQUESTED'"
                    class="link-btn"
                    :disabled="saving"
                    @click="openCreateOrder(item)"
                  >
                    <FilePlus2 class="inline-block w-4 h-4 mr-1" />
                    生成采购单
                  </button>
                  <span v-else class="text-gray-400">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showOrderForm && currentRequest" class="modal-mask">
        <div class="modal-card max-w-2xl">
          <div class="flex items-start justify-between gap-4 mb-5">
            <div>
              <p class="text-sm text-gray-500">生成采购订单</p>
              <h2 class="text-xl font-semibold text-gray-900">{{ currentRequest.materialName }}</h2>
            </div>
            <button class="icon-btn" @click="showOrderForm = false"><X class="w-5 h-5" /></button>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="rounded-xl bg-red-50 p-4 md:col-span-2">
              <p class="text-sm text-red-700">缺料数量</p>
              <p class="mt-1 text-2xl font-bold text-red-700">{{ currentRequest.shortageQuantity || 0 }}</p>
            </div>
            <label class="form-field md:col-span-2">
              <span>供应商</span>
              <select v-model.number="orderForm.supplierId" class="input">
                <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
                  {{ supplier.supplierCode }} / {{ supplier.supplierName }}
                </option>
              </select>
            </label>
            <label class="form-field">
              <span>预计到货日期</span>
              <input v-model="orderForm.expectedDate" type="date" class="input" />
            </label>
            <label class="form-field">
              <span>采购单价</span>
              <input v-model.number="orderForm.unitPrice" type="number" min="0" step="0.01" class="input" />
            </label>
            <div class="rounded-xl bg-gray-50 p-4 md:col-span-2">
              <p class="text-sm text-gray-500">预计金额</p>
              <p class="mt-1 text-lg font-semibold text-gray-900">
                {{ formatMoney(Number(currentRequest.shortageQuantity || 0) * Number(orderForm.unitPrice || 0)) }}
              </p>
            </div>
            <label class="form-field md:col-span-2">
              <span>备注</span>
              <textarea v-model="orderForm.remark" class="input min-h-[96px]"></textarea>
            </label>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showOrderForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="createPurchaseOrder">生成采购订单</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

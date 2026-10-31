<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
const props = defineProps<{ embedded?: boolean }>()
const emit = defineEmits<{ progressed: [stage: 'requests' | 'orders', id: number] }>()
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { canVisit } from '@/utils/permissions'
import MainLayout from '@/components/MainLayout.vue'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import PurchaseNavigation from '@/components/PurchaseNavigation.vue'
import { FilePlus2, RefreshCw, Search, X } from 'lucide-vue-next'
import {
  materialRequestApi,
  supplierApi,
  type MaterialRequest,
  type Supplier,
} from '@/api/mes'
import { useAuthStore } from '@/stores/auth'

type RequestStatus = '' | 'PENDING_CHECK' | 'ENOUGH' | 'SHORTAGE' | 'PURCHASE_REQUESTED' | 'PURCHASE_ORDERED'

const authStore = useAuthStore()
const router = useRouter()
const canOpenRequests = computed(() => canVisit(Number(authStore.effectiveRoleId), '/inventory/material-requests') && authStore.hasMenu('/inventory/material-requests'))
const canSend = computed(() => [1,2,3,5,9].includes(Number(authStore.effectiveRoleId)))
const canOrder = computed(() => [6,9].includes(Number(authStore.effectiveRoleId)))
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const keyword = ref('')
const statusFilter = ref<RequestStatus>('')
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
    return matchStatus && matchKeyword && (!props.embedded || ['PURCHASE_REQUESTED','PURCHASE_ORDERED'].includes(item.requestStatus || ''))
  })
})
const { page, pageSize, total, items: pageItems } = usePagination(filteredRequests)


const pendingPurchaseCount = computed(() => requests.value.filter((item) => item.requestStatus === 'PURCHASE_REQUESTED').length)
const orderedCount = computed(() => requests.value.filter((item) => item.requestStatus === 'PURCHASE_ORDERED').length)
const unsentCount = computed(() => requests.value.filter(item => item.requestStatus === 'SHORTAGE').length)

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
    showBusinessFormError(message.value)
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
    showBusinessFormError(message.value)
    return
  }
  if (Number(orderForm.unitPrice || 0) < 0) {
    message.value = '单价不能小于 0。'
    showBusinessFormError(message.value)
    return
  }
  const shortageQuantity = Number(currentRequest.value.shortageQuantity || 0)
  if (shortageQuantity <= 0) {
    message.value = '缺料数量必须大于 0。'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    const unitPrice = Number(orderForm.unitPrice || 0)
    const amount = shortageQuantity * unitPrice
    const created = await materialRequestApi.createOrder(currentRequest.value.id, {
      supplierId: Number(orderForm.supplierId),
      expectedDate: orderForm.expectedDate || undefined,
      remark: orderForm.remark,
      items: [{ materialId: Number(currentRequest.value.materialId), quantity: shortageQuantity, unitPrice, amount }],
    })
    message.value = `已根据 ${currentRequest.value.requestNo} 生成采购订单，需求状态已更新为“已转采购单”。`
    completeDrafts();showOrderForm.value = false
    if (props.embedded) emit('progressed', 'orders', Number(created.id))
    else await router.push({ path: '/purchase/order', query: { orderId: created.id, fromRequest: currentRequest.value.id } })
  } catch (error: any) {
    message.value = error.message || '生成采购订单失败'
  } finally {
    saving.value = false
  }
}

async function sendPurchase(item: MaterialRequest) {
  if (!item.id || saving.value) return
  saving.value = true
  try {
    await materialRequestApi.requestPurchase(item.id)
    await loadRequests()
    message.value = '已发送采购请求，采购员可在此选择供应商并生成订单。'
  } catch (error: any) { message.value = error.message || '发送采购失败' }
  finally { saving.value = false }
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

async function focusRequest(id: number) { await loadRequests(); keyword.value = ''; statusFilter.value = ''; const index = filteredRequests.value.findIndex(r => r.id === id); page.value = index < 0 ? 1 : Math.floor(index / pageSize.value) + 1 }
defineExpose({ refresh: loadRequests, focus: focusRequest })
const completeDrafts=useFormDrafts({showOrderForm:{open:showOrderForm,fields:{orderForm},record:()=>currentRequest.value?.id}}, '/purchase/requests')
</script>

<template>
  <component :is="props.embedded ? 'div' : MainLayout">
    <PurchaseNavigation v-if="!props.embedded" group="orders" />
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">采购需求</h1>
        </div>
        <button class="btn-secondary" @click="loadRequests">
          <RefreshCw class="w-4 h-4" />
          刷新
        </button>
      </div>

      <div v-if="!props.embedded" class="rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900 space-y-2">
        <p>业务流程：登记物料请求 → 仓库核对缺口并发送采购 → 采购员选择供应商生成订单 → 审批 → 收货检验与入库。</p>
        <router-link v-if="canOpenRequests" class="link-btn" to="/inventory/material-requests">去登记／处理物料请求</router-link>
        <p v-else>采购需求由生产或仓库岗位提交；尚未发送的缺料请求也在下方显示，供跟进处理。</p>
      </div>
      <div class="grid grid-cols-1 gap-4" :class="props.embedded ? 'md:grid-cols-2' : 'md:grid-cols-3'">
        <button class="stat-card text-left" @click="statusFilter = 'PURCHASE_REQUESTED'">
          <p class="stat-label">待采购下单</p>
          <p class="stat-value text-blue-600">{{ pendingPurchaseCount }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'PURCHASE_ORDERED'">
          <p class="stat-label">已转采购单</p>
          <p class="stat-value text-gray-700">{{ orderedCount }}</p>
        </button>
        <button v-if="!props.embedded" class="stat-card text-left" @click="statusFilter = 'SHORTAGE'">
          <p class="stat-label">待发送缺料请求</p>
          <p class="stat-value text-red-600">{{ unsentCount }}</p>
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
              <option v-if="!props.embedded" value="SHORTAGE">库存不足未发送</option>
              <option v-if="!props.embedded" value="ENOUGH">库存充足</option>
            </select>
            <button class="btn-primary w-full" @click="loadRequests"><Search class="w-4 h-4" />查询</button>
            <button class="btn-secondary w-full" @click="loadRequests"><RefreshCw class="w-4 h-4" />刷新</button>
          </div>
        </div>

        <div v-if="keyword || statusFilter" class="mb-4 flex items-center gap-3 text-sm text-gray-500">
          <span>当前筛选显示 {{ total }} 条，共 {{ requests.filter(r => !props.embedded || ['PURCHASE_REQUESTED','PURCHASE_ORDERED'].includes(r.requestStatus || '')).length }} 条采购需求</span>
          <button type="button" class="link-btn" @click="keyword = ''; statusFilter = ''">重置筛选，显示全部</button>
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
                <th>更新时间</th>
                <th>操作</th>

              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10" class="empty-cell">正在加载采购需求...</td>
              </tr>
              <tr v-else-if="filteredRequests.length === 0">
                <td colspan="10" class="empty-cell">
                  <p>{{ requests.length ? '当前筛选下没有记录，可切换全部状态查看。' : '暂无已生成的采购需求，请在上方物料需求区核对缺料并点击生成采购需求。' }}</p>
                  <button v-if="requests.length" class="link-btn mt-2" @click="statusFilter = ''; keyword = ''">查看全部需求</button>
                  <router-link v-else-if="canOpenRequests" class="link-btn mt-2" to="/inventory/material-requests">登记物料请求</router-link>
                </td>
              </tr>
              <tr v-for="item in pageItems" v-else :key="item.id">

                <td class="font-medium text-gray-900">{{ item.requestNo }}</td>
                <td>{{ item.sourceTaskNo || '-' }}</td>
                <td>{{ item.productName || '-' }}</td>
                <td :title="item.materialCode">{{ item.materialName }}</td>
                <td>{{ item.requiredQuantity }}</td>
                <td>{{ item.availableQuantity || 0 }}</td>
                <td class="font-medium" :class="Number(item.shortageQuantity || 0) > 0 ? 'text-red-600' : 'text-green-600'">
                  {{ item.shortageQuantity || 0 }}
                </td>
                <td><span :class="statusClass(item.requestStatus)">{{ statusText(item.requestStatus) }}</span></td>
                <td>{{ formatDateTime(item.updatedAt || item.createdAt) }}</td>
                <td class="text-left">
                  <button
                    v-if="item.requestStatus === 'PURCHASE_REQUESTED' && canOrder"
                    class="link-btn"
                    :disabled="saving"
                    @click="openCreateOrder(item)"
                  >
                    <FilePlus2 class="inline-block w-4 h-4 mr-1" />
                    生成采购单
                  </button>
                  <router-link v-else-if="item.purchaseOrderId" class="link-btn" :to="{ path: '/purchase/order', query: { orderId: item.purchaseOrderId } }">查看订单</router-link>
                  <button v-else-if="item.requestStatus === 'SHORTAGE' && canSend" class="link-btn" :disabled="saving" @click="sendPurchase(item)">发送采购请求</button>
                  <span v-else-if="item.requestStatus === 'SHORTAGE'" class="text-gray-500">等待仓库发送采购</span>
                  <span v-else-if="item.requestStatus === 'PURCHASE_ORDERED'" class="text-gray-500">历史转单记录未关联订单，请核对</span>
                  <span v-else class="text-gray-400">{{ item.requestStatus === 'ENOUGH' ? '库存充足，无需采购' : '等待处理' }}</span>
                </td>

              </tr>
            </tbody>
          </table>
        </div>
        <TablePagination v-model:page="page" v-model:page-size="pageSize" :total="total" label="采购需求" />
      </div>

      <div v-if="showOrderForm && currentRequest" class="modal-mask" data-business-form>
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
  </component>
</template>

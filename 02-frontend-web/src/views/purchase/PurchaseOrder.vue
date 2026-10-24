<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import {
  materialApi,
  locationApi,
  purchaseOrderApi,
  supplierApi,
  warehouseApi,
  type Location,
  type Material,
  type PurchaseOrder,
  type PurchaseOrderItem,
  type Supplier,
  type Warehouse,
} from '@/api/mes'
import { useAuthStore } from '@/stores/auth'

type ArrivalFilter = '' | 'NOT_RECEIVED' | 'PARTIAL_RECEIVED' | 'RECEIVED'
type FormMode = 'create' | 'edit'

interface ReceiveLine {
  purchaseOrderItemId: number
  materialId: number
  quantity: number
  receivedQuantity: number
  remainingQuantity: number
  locationId?: number
  remark?: string
}

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const authStore = useAuthStore()
const orders = ref<PurchaseOrder[]>([])
const suppliers = ref<Supplier[]>([])
const materials = ref<Material[]>([])
const warehouses = ref<Warehouse[]>([])
const locations = ref<Location[]>([])
const arrivalFilter = ref<ArrivalFilter>('')
const keyword = ref('')
const showForm = ref(false)
const showReceive = ref(false)
const showDetail = ref(false)
const showItemDetail = ref(false)
const formMode = ref<FormMode>('create')
const editingOrderId = ref<number>()
const receivingOrder = ref<PurchaseOrder | null>(null)
const detailOrder = ref<PurchaseOrder | null>(null)
const detailItem = ref<PurchaseOrderItem | null>(null)
const isWarehouseManager = computed(() => authStore.effectiveRoleId === 5)

const emptyItem = (): PurchaseOrderItem => ({
  materialId: materials.value[0]?.id || 0,
  quantity: 1,
  unitPrice: 0,
  amount: 0,
  remark: '',
})

const form = reactive<PurchaseOrder>({
  poNo: '',
  supplierId: 0,
  orderDate: new Date().toISOString().slice(0, 10),
  expectedDate: '',
  status: 'DRAFT',
  remark: '',
  createdBy: 1,
  items: [emptyItem()],
})

const receiveForm = reactive({
  warehouseId: 0,
  remark: '',
  items: [] as ReceiveLine[],
})

const filteredOrders = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return orders.value.filter((item) => {
    const matchKeyword =
      !word ||
      item.poNo?.toLowerCase().includes(word) ||
      supplierName(item.supplierId).toLowerCase().includes(word) ||
      orderItems(item).some((line) => materialText(line.materialId).toLowerCase().includes(word))
    const matchArrival = !arrivalFilter.value || arrivalStatus(item) === arrivalFilter.value
    return matchKeyword && matchArrival
  })
})

const notReceivedCount = computed(() => orders.value.filter((item) => arrivalStatus(item) === 'NOT_RECEIVED').length)
const receivedCount = computed(() => orders.value.filter((item) => arrivalStatus(item) === 'RECEIVED').length)

onMounted(async () => {
  await Promise.all([loadOrders(), loadSuppliers(), loadMaterials(), loadWarehouses(), loadLocations()])
})

async function loadOrders() {
  loading.value = true
  message.value = ''
  try {
    orders.value = await purchaseOrderApi.list()
  } catch (error: any) {
    message.value = error.message || '采购订单加载失败'
  } finally {
    loading.value = false
  }
}

async function loadSuppliers() {
  try {
    suppliers.value = await supplierApi.search({ status: 1 })
  } catch {
    suppliers.value = []
  }
}

async function loadMaterials() {
  try {
    materials.value = await materialApi.list()
  } catch {
    materials.value = []
  }
}

async function loadWarehouses() {
  try {
    warehouses.value = await warehouseApi.list()
  } catch {
    warehouses.value = []
  }
}

async function loadLocations() {
  try {
    locations.value = await locationApi.list()
  } catch {
    locations.value = []
  }
}

function supplierName(id?: number) {
  return suppliers.value.find((item) => item.id === id)?.supplierName || `供应商ID：${id || '-'}`
}

function materialById(id?: number) {
  return materials.value.find((item) => item.id === id)
}

function materialText(id?: number) {
  const material = materialById(id)
  if (!material) return `物料ID：${id || '-'}`
  return `${material.materialCode} / ${material.materialName}`
}

function materialUnit(id?: number) {
  return materialById(id)?.unit || ''
}

function materialSpec(id?: number) {
  return materialById(id)?.specification || ''
}

function warehouseText(warehouse?: Warehouse) {
  if (!warehouse) return ''
  return `${warehouse.warehouseCode} / ${warehouse.warehouseName}`
}

function locationText(location?: Location) {
  if (!location) return ''
  return `${location.locationCode} / ${location.locationName}`
}

function selectableLocations() {
  return locations.value.filter((item) => item.warehouseId === Number(receiveForm.warehouseId))
}

function defaultWarehouseId() {
  return warehouses.value[0]?.id || 0
}

function defaultLocationId() {
  return selectableLocations()[0]?.id
}

function handleReceiveWarehouseChange() {
  const firstLocationId = defaultLocationId()
  receiveForm.items.forEach((item) => {
    item.locationId = firstLocationId
  })
}

function orderItems(order?: PurchaseOrder | null) {
  return order?.items || []
}

function arrivalStatus(order: PurchaseOrder): ArrivalFilter {
  const lines=order.items||[];const received=lines.reduce((n,l)=>n+Number(l.receivedQuantity||0),0);const ordered=lines.reduce((n,l)=>n+Number(l.quantity||0),0);return ordered>0&&received>=ordered?'RECEIVED':received>0?'PARTIAL_RECEIVED':'NOT_RECEIVED'
}

function arrivalText(order: PurchaseOrder) {
  return arrivalStatus(order)==='RECEIVED'?'全部收货':arrivalStatus(order)==='PARTIAL_RECEIVED'?'部分收货':'未收货'
}

function arrivalClass(order: PurchaseOrder) {
  return arrivalStatus(order) === 'RECEIVED' ? 'tag-green' : 'tag-yellow'
}

function remainingQuantity(item: PurchaseOrderItem) {
  return Math.max(Number(item.quantity || 0) - Number(item.receivedQuantity || 0), 0)
}

function totalRemaining(order?: PurchaseOrder | null) {
  return orderItems(order).reduce((sum, item) => sum + remainingQuantity(item), 0)
}

function hasDetails(order: PurchaseOrder) {
  return orderItems(order).length > 0
}

function canReceive(order: PurchaseOrder) {
  return arrivalStatus(order) !== 'RECEIVED' && ['APPROVED','PARTIAL_RECEIVED'].includes(order.status||'') && hasDetails(order) && totalRemaining(order) > 0
}

function needsStatusSync(order: PurchaseOrder) {
  return arrivalStatus(order) === 'NOT_RECEIVED' && hasDetails(order) && totalRemaining(order) === 0
}

function resetForm() {
  editingOrderId.value = undefined
  Object.assign(form, {
    poNo: `PO-${Date.now()}`,
    supplierId: suppliers.value[0]?.id || 0,
    orderDate: new Date().toISOString().slice(0, 10),
    expectedDate: '',
    status: 'DRAFT',
    remark: '',
    createdBy: authStore.currentUser?.id || 1,
    items: [emptyItem()],
  })
}

function openCreate() {
  if (isWarehouseManager.value) {
    message.value = '仓管员只能查看采购订单并确认到货，采购单由采购员创建。'
    return
  }
  formMode.value = 'create'
  resetForm()
  showForm.value = true
}

function openDetail(order: PurchaseOrder) {
  detailOrder.value = order
  showDetail.value = true
}

function openItemDetail(item: PurchaseOrderItem) {
  detailItem.value = item
  showItemDetail.value = true
}

function openEdit(order: PurchaseOrder) {
  if (isWarehouseManager.value) {
    message.value = '仓管员不能修改采购订单，只能查看订单状态并办理收货入库。'
    return
  }
  if (arrivalStatus(order) === 'RECEIVED') {
    message.value = '已到货采购单不建议再修改明细'
    return
  }
  formMode.value = 'edit'
  editingOrderId.value = order.id
  Object.assign(form, {
    poNo: order.poNo,
    supplierId: order.supplierId,
    orderDate: toDateInput(order.orderDate),
    expectedDate: toDateInput(order.expectedDate),
    status: 'DRAFT',
    remark: order.remark || '',
    createdBy: order.createdBy || 1,
    items: orderItems(order).length
      ? orderItems(order).map((item) => ({
          id: item.id,
          poId: item.poId,
          materialId: Number(item.materialId || 0),
          quantity: Number(item.quantity || 0),
          unitPrice: Number(item.unitPrice || 0),
          amount: Number(item.amount || 0),
          receivedQuantity: Number(item.receivedQuantity || 0),
          remark: item.remark || '',
        }))
      : [emptyItem()],
  })
  showForm.value = true
}

function addItem() {
  form.items?.push(emptyItem())
}

function removeItem(index: number) {
  form.items?.splice(index, 1)
}

function normalizeItems() {
  form.items = (form.items || []).map((item) => {
    const quantity = Number(item.quantity || 0)
    const unitPrice = Number(item.unitPrice || 0)
    return {
      ...item,
      materialId: Number(item.materialId),
      quantity,
      unitPrice,
      amount: quantity * unitPrice,
    }
  })
}

function validateForm() {
  if (!form.poNo || !form.supplierId) {
    message.value = '订单号和供应商不能为空'
    return false
  }
  if (!form.items?.length) {
    message.value = '采购订单必须至少有一条明细'
    return false
  }
  const invalidItem = form.items.find((item) => !item.materialId || Number(item.quantity || 0) <= 0)
  if (invalidItem) {
    message.value = '每条采购明细都必须选择物料，并填写大于 0 的数量'
    return false
  }
  return true
}

async function transition(order:PurchaseOrder,kind:'submit'|'approve') {if(!order.id||saving.value)return;saving.value=true;try{await purchaseOrderApi[kind](order.id);await loadOrders();message.value=kind==='submit'?'采购单已提交审批':'采购单已审批，可办理来料检验及收货'}catch(e:any){message.value=e.message}finally{saving.value=false}}
async function saveOrder() {
  if (isWarehouseManager.value) {
    message.value = '仓管员不能保存采购订单，采购订单请由采购员维护。'
    return
  }
  if (!validateForm()) return
  saving.value = true
  try {
    normalizeItems()
    form.totalAmount = form.items!.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    const payload: PurchaseOrder = {
      poNo: form.poNo,
      supplierId: Number(form.supplierId),
      orderDate: form.orderDate,
      expectedDate: form.expectedDate,
      totalAmount: form.totalAmount,
      status: 'DRAFT',
      remark: form.remark,
      createdBy: authStore.currentUser?.id || form.createdBy || 1,
      items: form.items,
    }
    if (formMode.value === 'edit' && editingOrderId.value) {
      await purchaseOrderApi.update(editingOrderId.value, payload)
      message.value = '采购订单明细已保存到数据库'
    } else {
      await purchaseOrderApi.create(payload)
      message.value = '采购订单已新增'
    }
    completeDrafts();showForm.value=false
    await loadOrders()
  } catch (error: any) {
    message.value = error.message || '保存采购订单失败'
  } finally {
    saving.value = false
  }
}

function openReceive(order: PurchaseOrder) {
  window.location.assign('/purchase/receipts');return;
  if (!hasDetails(order)) {
    message.value = `采购单 ${order.poNo} 在数据库中没有明细，请先补录明细后再确认收货`
    if (!isWarehouseManager.value) {
      openEdit(order)
    }
    return
  }

  receivingOrder.value = order
  const warehouseId = defaultWarehouseId()
  receiveForm.warehouseId = warehouseId
  const firstLocationId = locations.value.find((item) => item.warehouseId === warehouseId)?.id
  Object.assign(receiveForm, {
    warehouseId,
    remark: '',
    items: orderItems(order)
      .filter((item) => item.id && remainingQuantity(item) > 0)
      .map((item) => ({
        purchaseOrderItemId: Number(item.id),
        materialId: Number(item.materialId),
        quantity: Number(item.quantity || 0),
        receivedQuantity: Number(item.receivedQuantity || 0),
        remainingQuantity: remainingQuantity(item),
        locationId: firstLocationId,
        remark: '',
      })),
  })
  if (receiveForm.items.length === 0) {
    message.value = '这张采购单没有剩余未到货数量，可点击“同步状态”修正到货状态'
    return
  }
  showReceive.value = true
}

async function receiveOrder() {
  if (!receivingOrder.value?.id || receiveForm.items.length === 0) {
    message.value = '当前采购单没有可收货的明细'
    return
  }
  if (!receiveForm.warehouseId) {
    message.value = '请选择收货仓库'
    return
  }
  if (receiveForm.items.some((item) => !item.locationId)) {
    message.value = '每条收货明细都必须选择库位'
    return
  }
  saving.value = true
  try {
    await purchaseOrderApi.receive(receivingOrder.value.id, {
      warehouseId: Number(receiveForm.warehouseId),
      createdBy: authStore.currentUser?.id || 1,
      remark: receiveForm.remark || '采购订单确认收货',
      items: receiveForm.items.map((item) => ({
        purchaseOrderItemId: item.purchaseOrderItemId,
        receiveQuantity: item.remainingQuantity,
        locationId: item.locationId ? Number(item.locationId) : undefined,
        remark: item.remark || receiveForm.remark,
      })),
    })
    message.value = '本次收货已登记，收货状态按累计数量更新'
    showReceive.value = false
    showDetail.value = false
    await loadOrders()
  } catch (error: any) {
    message.value = error.message || '采购收货失败'
  } finally {
    saving.value = false
  }
}

async function syncReceivedStatus(order: PurchaseOrder) {
  if (!order.id) return
  saving.value = true
  try {
    try {
      await purchaseOrderApi.syncReceived(order.id)
      message.value = '采购订单状态已同步为已到货'
    } catch (error: any) {
      if (!String(error?.message || '').includes('404')) {
        throw error
      }
      await purchaseOrderApi.update(order.id, {
        poNo: order.poNo,
        supplierId: order.supplierId,
        orderDate: order.orderDate,
        expectedDate: order.expectedDate,
        totalAmount: order.totalAmount,
        status: 'RECEIVED',
        remark: order.remark,
        createdBy: order.createdBy || 1,
      })
      message.value = '后端未重启导致同步接口不可用，已使用兼容方式同步为已到货'
    }
    showDetail.value = false
    await loadOrders()
  } catch (error: any) {
    message.value = error.message || '同步到货状态失败'
  } finally {
    saving.value = false
  }
}

async function removeOrder(order: PurchaseOrder) {
  if (isWarehouseManager.value) {
    message.value = '仓管员不能删除采购订单。'
    return
  }
  if (!order.id || !window.confirm(`确认删除采购订单“${order.poNo}”吗？`)) return
  try {
    await purchaseOrderApi.remove(order.id)
    message.value = '采购订单已删除'
    await loadOrders()
  } catch (error: any) {
    message.value = error.message || '删除采购订单失败'
  }
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString('zh-CN') : '-'
}

function toDateInput(value?: string) {
  if (!value) return ''
  return value.slice(0, 10)
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id || editingOrderId.value}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">采购订单</h1>
          <p class="text-gray-500 mt-1">
            {{ isWarehouseManager ? '采购员创建采购单；仓管员只查看状态，并在到货时选择仓库和库位办理入库。' : '列表只展示订单概要，点击详情查看数据库中的采购明细。' }}
          </p>
        </div>
        <button v-if="!isWarehouseManager" class="btn-primary" @click="openCreate">
          <Plus class="w-4 h-4" />
          新增采购单
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <button class="stat-card text-left" @click="arrivalFilter = ''">
          <p class="stat-label">订单数量</p>
          <p class="stat-value">{{ orders.length }}</p>
        </button>
        <button class="stat-card text-left" @click="arrivalFilter = 'NOT_RECEIVED'">
          <p class="stat-label">未到货</p>
          <p class="stat-value text-yellow-600">{{ notReceivedCount }}</p>
        </button>
        <button class="stat-card text-left" @click="arrivalFilter = 'RECEIVED'">
          <p class="stat-label">已到货</p>
          <p class="stat-value text-green-600">{{ receivedCount }}</p>
        </button>
      </div>

      <div class="panel">
        <div class="mb-5 overflow-x-auto pb-1">
          <div class="grid min-w-[720px] grid-cols-[minmax(320px,1fr)_190px_92px] items-center gap-3">
            <label class="relative">
              <Search class="search-icon" />
              <input v-model="keyword" class="input pl-10" placeholder="搜索采购单号、供应商或物料" />
            </label>
            <select v-model="arrivalFilter" class="input">
              <option value="">到货状态：全部</option>
              <option value="NOT_RECEIVED">到货状态：未到货</option>
              <option value="RECEIVED">到货状态：已到货</option>
            </select>
            <button class="btn-secondary w-full" @click="loadOrders"><RefreshCw class="w-4 h-4" />刷新</button>
          </div>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[980px]">
            <thead>
              <tr>
                <th>订单号</th>
                <th>供应商</th>
                <th>下单日期</th>
                <th>预计到货</th>
                <th>金额</th>
                <th>到货状态</th>
                <th>明细</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="empty-cell">正在加载采购订单...</td>
              </tr>
              <tr v-else-if="filteredOrders.length === 0">
                <td colspan="8" class="empty-cell">暂无采购订单</td>
              </tr>
              <tr v-for="item in filteredOrders" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.poNo }}</td>
                <td>{{ supplierName(item.supplierId) }}</td>
                <td>{{ formatDate(item.orderDate) }}</td>
                <td>{{ formatDate(item.expectedDate) }}</td>
                <td>{{ formatMoney(item.totalAmount) }}</td>
                <td><span :class="arrivalClass(item)">{{ arrivalText(item) }}</span></td>
                <td>
                  <span :class="hasDetails(item) ? 'tag-green' : 'tag-red'">
                    {{ orderItems(item).length }} 条
                  </span>
                </td>
                <td class="text-right space-x-2">
                  <button class="link-btn" @click="openDetail(item)">详情</button><span class="text-gray-500">{{({DRAFT:'草稿',SUBMITTED:'待审批',APPROVED:'已审批',PARTIAL_RECEIVED:'已审批',COMPLETED:'已完成',CANCELLED:'已取消'} as Record<string,string>)[item.status||'']||item.status}}</span><button v-if="!isWarehouseManager&&item.status==='DRAFT'" :disabled="saving" class="link-btn" @click="transition(item,'submit')">提交审批</button><button v-if="!isWarehouseManager&&item.status==='SUBMITTED'" :disabled="saving" class="link-btn" @click="transition(item,'approve')">审批</button>
                  <button v-if="canReceive(item)" class="link-btn" @click="openReceive(item)">确认收货</button>
                  <button v-else-if="needsStatusSync(item)" class="link-btn" :disabled="saving" @click="syncReceivedStatus(item)">同步状态</button>
                  <button v-else-if="!isWarehouseManager && item.status==='DRAFT'" class="link-btn" @click="openEdit(item)">补录明细</button>
                  <button v-if="!isWarehouseManager" class="danger-link" @click="removeOrder(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showDetail && detailOrder" class="modal-mask">
        <div class="modal-card max-w-5xl">
          <div class="flex items-start justify-between gap-4 mb-5">
            <div>
              <p class="text-sm text-gray-500">采购订单详情</p>
              <h2 class="text-xl font-semibold text-gray-900">{{ detailOrder.poNo }}</h2>
            </div>
            <button class="icon-btn" @click="showDetail = false"><X class="w-5 h-5" /></button>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-4 gap-3 mb-5">
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">供应商</p>
              <p class="mt-1 font-medium text-gray-900">{{ supplierName(detailOrder.supplierId) }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">到货状态</p>
              <p class="mt-1"><span :class="arrivalClass(detailOrder)">{{ arrivalText(detailOrder) }}</span></p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">订单金额</p>
              <p class="mt-1 font-medium text-gray-900">{{ formatMoney(detailOrder.totalAmount) }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">剩余未到货</p>
              <p class="mt-1 font-medium" :class="totalRemaining(detailOrder) > 0 ? 'text-yellow-600' : 'text-green-600'">
                {{ totalRemaining(detailOrder) }}
              </p>
            </div>
          </div>

          <div v-if="!hasDetails(detailOrder)" class="rounded-xl border border-dashed border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {{ isWarehouseManager ? '这张采购单在数据库中没有采购明细，仓管员不能补录，请联系采购员维护明细后再办理收货。' : '这张采购单在数据库中没有采购明细，不能确认收货。请点击“补录明细”保存物料、数量和单价。' }}
          </div>
          <div v-else class="rounded-xl border border-gray-100 overflow-hidden">
            <table class="data-table">
              <thead>
                <tr>
                  <th>物料</th>
                  <th>规格</th>
                  <th>采购数量</th>
                  <th>已到货</th>
                  <th>剩余未到货</th>
                  <th>单价</th>
                  <th>金额</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="line in orderItems(detailOrder)" :key="line.id">
                  <td class="font-medium text-gray-900">{{ materialText(line.materialId) }}</td>
                  <td>{{ materialSpec(line.materialId) || '-' }}</td>
                  <td>{{ line.quantity }} {{ materialUnit(line.materialId) }}</td>
                  <td>{{ line.receivedQuantity || 0 }} {{ materialUnit(line.materialId) }}</td>
                  <td class="font-medium" :class="remainingQuantity(line) > 0 ? 'text-yellow-600' : 'text-green-600'">
                    {{ remainingQuantity(line) }} {{ materialUnit(line.materialId) }}
                  </td>
                  <td>{{ formatMoney(line.unitPrice) }}</td>
                  <td>{{ formatMoney(line.amount) }}</td>
                  <td class="text-right">
                    <button class="link-btn" @click="openItemDetail(line)">查看明细</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showDetail = false">关闭</button>
            <button v-if="canReceive(detailOrder)" class="btn-primary" @click="openReceive(detailOrder)">确认收货</button>
            <button v-else-if="needsStatusSync(detailOrder)" class="btn-primary" :disabled="saving" @click="syncReceivedStatus(detailOrder)">同步为已到货</button>
            <button v-else-if="!isWarehouseManager && arrivalStatus(detailOrder) === 'NOT_RECEIVED'" class="btn-primary" @click="openEdit(detailOrder)">补录明细</button>
          </div>
        </div>
      </div>

      <div v-if="showItemDetail && detailItem" class="modal-mask">
        <div class="modal-card max-w-2xl">
          <div class="flex items-start justify-between gap-4 mb-5">
            <div>
              <p class="text-sm text-gray-500">采购明细详情</p>
              <h2 class="text-xl font-semibold text-gray-900">{{ materialText(detailItem.materialId) }}</h2>
            </div>
            <button class="icon-btn" @click="showItemDetail = false"><X class="w-5 h-5" /></button>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">所属订单</p>
              <p class="mt-1 font-medium text-gray-900">{{ detailOrder?.poNo || '-' }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">明细ID</p>
              <p class="mt-1 font-medium text-gray-900">{{ detailItem.id || '-' }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">物料编码 / 名称</p>
              <p class="mt-1 font-medium text-gray-900">{{ materialText(detailItem.materialId) }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">规格型号</p>
              <p class="mt-1 font-medium text-gray-900">{{ materialSpec(detailItem.materialId) || '-' }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">采购数量</p>
              <p class="mt-1 font-medium text-gray-900">{{ detailItem.quantity }} {{ materialUnit(detailItem.materialId) }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">已到货数量</p>
              <p class="mt-1 font-medium text-gray-900">{{ detailItem.receivedQuantity || 0 }} {{ materialUnit(detailItem.materialId) }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">剩余未到货</p>
              <p class="mt-1 font-medium" :class="remainingQuantity(detailItem) > 0 ? 'text-yellow-600' : 'text-green-600'">
                {{ remainingQuantity(detailItem) }} {{ materialUnit(detailItem.materialId) }}
              </p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3">
              <p class="text-xs text-gray-500">单价 / 金额</p>
              <p class="mt-1 font-medium text-gray-900">{{ formatMoney(detailItem.unitPrice) }} / {{ formatMoney(detailItem.amount) }}</p>
            </div>
            <div class="rounded-xl bg-gray-50 p-3 md:col-span-2">
              <p class="text-xs text-gray-500">备注</p>
              <p class="mt-1 font-medium text-gray-900">{{ detailItem.remark || '-' }}</p>
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showItemDetail = false">关闭</button>
          </div>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask">
        <div class="modal-card max-w-4xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">{{ formMode === 'edit' ? '编辑采购订单明细' : '新增采购订单' }}</h2>
            <button class="icon-btn" @click="showForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">订单号<input v-model="form.poNo" class="input mt-1" /></label>
            <label class="field-label">供应商
              <select v-model.number="form.supplierId" class="input mt-1">
                <option :value="0">请选择供应商</option>
                <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">{{ supplier.supplierName }}</option>
              </select>
            </label>
            <label class="field-label">下单日期<input v-model="form.orderDate" type="date" class="input mt-1" /></label>
            <label class="field-label">预计到货<input v-model="form.expectedDate" type="date" class="input mt-1" /></label>
            <label class="field-label md:col-span-2">备注<input v-model="form.remark" class="input mt-1" /></label>
          </div>

          <div class="mt-5">
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-gray-800">采购明细</h3>
              <button class="btn-secondary" @click="addItem">新增明细</button>
            </div>
            <div class="space-y-3">
              <div v-for="(item, index) in form.items" :key="index" class="grid grid-cols-1 md:grid-cols-[2fr_1fr_1fr_1.3fr_72px] gap-3 p-3 bg-gray-50 rounded-xl">
                <select v-model.number="item.materialId" class="input">
                  <option :value="0">请选择物料</option>
                  <option v-for="material in materials" :key="material.id" :value="material.id">
                    {{ material.materialCode }} / {{ material.materialName }}
                  </option>
                </select>
                <input v-model.number="item.quantity" class="input" type="number" min="0" placeholder="数量" />
                <input v-model.number="item.unitPrice" class="input" type="number" min="0" placeholder="单价" />
                <input v-model="item.remark" class="input" placeholder="备注" />
                <button class="danger-link text-left" @click="removeItem(index)">删除</button>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveOrder">{{ saving ? '保存中...' : '保存到数据库' }}</button>
          </div>
        </div>
      </div>

      <div v-if="showReceive" class="modal-mask">
        <div class="modal-card max-w-3xl">
          <div class="flex items-center justify-between mb-4">
            <div>
              <p class="text-sm text-gray-500">将所有剩余未到货明细一次性确认收货</p>
              <h2 class="text-lg font-semibold">确认收货：{{ receivingOrder?.poNo }}</h2>
            </div>
            <button class="icon-btn" @click="showReceive = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 gap-4">
            <label class="field-label">收货仓库
              <select v-model.number="receiveForm.warehouseId" class="input mt-1" @change="handleReceiveWarehouseChange">
                <option :value="0">请选择收货仓库</option>
                <option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">
                  {{ warehouseText(warehouse) }}
                </option>
              </select>
            </label>
            <p v-if="receiveForm.warehouseId && selectableLocations().length === 0" class="text-sm text-red-600">
              当前仓库没有可用库位，请先在仓库管理中维护库位。
            </p>
            <div class="rounded-xl border border-gray-100 overflow-hidden">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>物料</th>
                    <th>采购数量</th>
                    <th>已到货</th>
                    <th>本次收货</th>
                    <th>入库库位</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="line in receiveForm.items" :key="line.purchaseOrderItemId">
                    <td>{{ materialText(line.materialId) }}</td>
                    <td>{{ line.quantity }} {{ materialUnit(line.materialId) }}</td>
                    <td>{{ line.receivedQuantity }} {{ materialUnit(line.materialId) }}</td>
                    <td class="font-medium text-gray-900">{{ line.remainingQuantity }} {{ materialUnit(line.materialId) }}</td>
                    <td>
                      <select v-model.number="line.locationId" class="input">
                        <option :value="0">请选择库位</option>
                        <option v-for="location in selectableLocations()" :key="location.id" :value="location.id">
                          {{ locationText(location) }}
                        </option>
                      </select>
                    </td>
                  </tr>
                  <tr v-if="receiveForm.items.length === 0"><td colspan="5" class="empty-cell">没有未到货明细</td></tr>
                </tbody>
              </table>
            </div>
            <label class="field-label">备注<input v-model="receiveForm.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showReceive = false">取消</button>
            <button class="btn-primary" :disabled="saving || receiveForm.items.length === 0 || !receiveForm.warehouseId || receiveForm.items.some((item) => !item.locationId)" @click="receiveOrder">
              {{ saving ? '提交中...' : '确认收货并更新状态' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

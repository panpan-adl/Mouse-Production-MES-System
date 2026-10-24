<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Edit, Eye, Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { customerApi, orderApi, type Customer, type Order } from '@/api/mes'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const loading = ref(false)
const customerLoading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const message = ref('')
const orders = ref<Order[]>([])
const customers = ref<Customer[]>([])
const selectedOrder = ref<Order | null>(null)
const detailVisible = ref(false)
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingOrderId = ref<number | null>(null)

const filters = reactive({
  keyword: '',
  status: '' as '' | number,
})

const form = reactive({
  orderNo: '',
  customerId: '' as number | '',
  customerName: '',
  customerPhone: '',
  productName: '',
  quantity: 1,
  unitPrice: 0,
  orderDate: '',
  deliveryDate: '',
  priority: 2,
  remark: '',
})

const statusText: Record<number, string> = {
  [-1]: '未确定',
  0: '未确定',
  1: '已确认',
  2: '生产中',
  3: '已完成',
  4: '已取消',
}

const priorityText: Record<number, string> = {
  1: '高',
  2: '普通',
  3: '低',
}

const demoOrderDisplayFixes: Record<string, Partial<Pick<Order, 'customerName' | 'productName' | 'remark'>>> = {
  'SO-20260715-001': {
    customerName: '北京科创科技有限公司',
    productName: 'A100有线办公鼠标',
  },
}

function displayOrderField(order: Order | null, field: 'customerName' | 'productName' | 'remark') {
  if (!order) return '-'
  const fixedValue = demoOrderDisplayFixes[order.orderNo]?.[field]
  const value = String(fixedValue ?? order[field] ?? '').trim()
  return value || '-'
}

function normalizeOrderStatus(status?: number | string | null) {
  const numericStatus = Number(status)
  return Number.isInteger(numericStatus) && statusText[numericStatus] ? numericStatus : -1
}

function rawOrderStatus(status?: number | string | null) {
  const numericStatus = Number(status)
  return Number.isInteger(numericStatus) ? numericStatus : 0
}

function orderStatusLabel(status?: number | string | null) {
  return statusText[normalizeOrderStatus(status)]
}

const activeCustomers = computed(() => {
  return customers.value
    .filter((item) => item.isDeleted !== 1 && normalizeCustomerStatus(item.status) === '1')
    .sort((a, b) => (a.customerCode || '').localeCompare(b.customerCode || ''))
})

const filteredOrders = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return orders.value.filter((item) => {
    const matchKeyword =
      !keyword ||
      item.orderNo?.toLowerCase().includes(keyword) ||
      displayOrderField(item, 'customerName').toLowerCase().includes(keyword) ||
      displayOrderField(item, 'productName').toLowerCase().includes(keyword)
    const matchStatus = filters.status === '' || normalizeOrderStatus(item.orderStatus) === filters.status
    return matchKeyword && matchStatus
  })
})

const orderStats = computed(() => ({
  total: orders.value.length,
  pending: orders.value.filter((item) => normalizeOrderStatus(item.orderStatus) === 0).length,
  running: orders.value.filter((item) => normalizeOrderStatus(item.orderStatus) === 2).length,
  completed: orders.value.filter((item) => normalizeOrderStatus(item.orderStatus) === 3).length,
}))

const formTotalAmount = computed(() => {
  const quantity = Number(form.quantity || 0)
  const unitPrice = Number(form.unitPrice || 0)
  return Number((quantity * unitPrice).toFixed(2))
})

onMounted(() => {
  loadOrders()
  loadCustomers()
})

async function loadOrders() {
  loading.value = true
  message.value = ''
  try {
    orders.value = await orderApi.list()
  } catch (error: any) {
    message.value = error.message || '订单加载失败'
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  customerLoading.value = true
  try {
    customers.value = await customerApi.list()
  } catch (error: any) {
    message.value = error.message || '客户列表加载失败，暂时无法新增或修改订单'
  } finally {
    customerLoading.value = false
  }
}

async function openDetail(order: Order) {
  if (!order.id) return
  detailLoading.value = true
  detailVisible.value = true
  selectedOrder.value = order
  try {
    selectedOrder.value = await orderApi.get(order.id)
  } catch (error: any) {
    message.value = error.message || '订单详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function openCreate() {
  formMode.value = 'create'
  editingOrderId.value = null
  resetForm()
  formVisible.value = true
  if (customers.value.length === 0) {
    loadCustomers()
  }
}

async function openEdit(order: Order) {
  if (!order.id) return
  if (customers.value.length === 0) {
    await loadCustomers()
  }
  const customerName = displayOrderField(order, 'customerName')
  const productName = displayOrderField(order, 'productName')
  const matchedCustomer = activeCustomers.value.find((customer) => {
    return customer.customerName === customerName || (!!order.customerPhone && customer.phone === order.customerPhone)
  })

  formMode.value = 'edit'
  editingOrderId.value = order.id
  Object.assign(form, {
    orderNo: order.orderNo || '',
    customerId: matchedCustomer?.id || '',
    customerName,
    customerPhone: order.customerPhone || matchedCustomer?.phone || '',
    productName,
    quantity: Number(order.quantity || 1),
    unitPrice: Number(order.unitPrice || 0),
    orderDate: formatDateForInput(order.orderDate),
    deliveryDate: formatDateForInput(order.deliveryDate),
    priority: Number(order.priority || 2),
    remark: displayOrderField(order, 'remark') === '-' ? '' : displayOrderField(order, 'remark'),
  })
  formVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    orderNo: generateOrderNo(),
    customerId: '',
    customerName: '',
    customerPhone: '',
    productName: '',
    quantity: 1,
    unitPrice: 0,
    orderDate: formatDateInput(),
    deliveryDate: '',
    priority: 2,
    remark: '',
  })
}

function applySelectedCustomer() {
  const selected = activeCustomers.value.find((item) => item.id === Number(form.customerId))
  form.customerName = selected?.customerName || ''
  form.customerPhone = selected?.phone || ''
}

async function saveOrder() {
  if (!form.orderNo.trim()) {
    message.value = '订单编号不能为空'
    return
  }
  if (!form.customerId || !form.customerName.trim()) {
    message.value = '请选择客户列表中的客户'
    return
  }
  if (!form.productName.trim()) {
    message.value = '产品名称不能为空'
    return
  }
  if (Number(form.quantity) <= 0) {
    message.value = '订单数量必须大于 0'
    return
  }
  if (Number(form.unitPrice) < 0) {
    message.value = '产品单价不能小于 0'
    return
  }
  if (!form.orderDate) {
    message.value = '下单日期不能为空'
    return
  }
  if (!form.deliveryDate) {
    message.value = '交付日期不能为空'
    return
  }

  saving.value = true
  try {
    const existingOrder = orders.value.find((item) => item.id === editingOrderId.value)
    const payload: Order = {
      orderNo: form.orderNo.trim(),
      customerName: form.customerName.trim(),
      customerPhone: form.customerPhone.trim(),
      productName: form.productName.trim(),
      quantity: Number(form.quantity),
      unitPrice: Number(form.unitPrice),
      totalAmount: formTotalAmount.value,
      orderDate: form.orderDate,
      deliveryDate: form.deliveryDate,
      orderStatus: formMode.value === 'edit' ? rawOrderStatus(existingOrder?.orderStatus) : 0,
      priority: Number(form.priority),
      remark: form.remark.trim(),
      createBy: existingOrder?.createBy || authStore.currentUser?.id,
    }

    if (formMode.value === 'edit') {
      if (!editingOrderId.value) {
        message.value = '缺少订单ID，无法修改'
        return
      }
      await orderApi.update(editingOrderId.value, payload)
      message.value = '订单已修改'
    } else {
      await orderApi.create(payload)
      message.value = '订单已新增，状态为待确认'
    }

    formVisible.value = false
    await loadOrders()
  } catch (error: any) {
    message.value = error.message || (formMode.value === 'edit' ? '订单修改失败' : '订单新增失败')
  } finally {
    saving.value = false
  }
}

function clearFilters() {
  filters.keyword = ''
  filters.status = ''
}

function normalizeCustomerStatus(status?: string | number) {
  const value = String(status ?? '').trim().toUpperCase()
  if (['1', 'ACTIVE', 'ENABLE', 'ENABLED', 'NORMAL'].includes(value)) return '1'
  if (['0', 'INACTIVE', 'DISABLE', 'DISABLED', 'STOP'].includes(value)) return '0'
  return value || '1'
}

function generateOrderNo() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const tail = String(Date.now()).slice(-4)
  return `SO-${y}${m}${d}-${tail}`
}

function formatDateInput(date = new Date()) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatDateForInput(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.substring(0, 10)
  return formatDateInput(date)
}

function statusClass(status?: number | string | null) {
  const normalizedStatus = normalizeOrderStatus(status)
  if (normalizedStatus === 3) return 'tag-green'
  if (normalizedStatus === 2) return 'tag-blue'
  if (normalizedStatus === 4 || normalizedStatus === -1) return 'tag-gray'
  return 'tag-yellow'
}

function priorityClass(priority?: number) {
  if (priority === 1) return 'tag-red'
  if (priority === 3) return 'tag-gray'
  return 'tag-blue'
}

function stepReached(status: number | undefined, step: number) {
  const normalizedStatus = normalizeOrderStatus(status)
  if (normalizedStatus === 4 || normalizedStatus === -1) return false
  return normalizedStatus >= step
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm text-gray-500">销售业务</p>
          <h1 class="mt-1 text-2xl font-bold text-gray-900">订单列表</h1>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="btn-primary" @click="openCreate">
            <Plus class="h-4 w-4" />
            新增订单
          </button>
          <button class="btn-secondary" @click="loadOrders">
            <RefreshCw class="h-4 w-4" />
            刷新
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <button class="stat-card text-left" @click="filters.status = ''">
          <p class="stat-label">订单总数</p>
          <p class="stat-value">{{ orderStats.total }}</p>
        </button>
        <button class="stat-card text-left" @click="filters.status = 0">
          <p class="stat-label">待确认</p>
          <p class="stat-value text-yellow-600">{{ orderStats.pending }}</p>
        </button>
        <button class="stat-card text-left" @click="filters.status = 2">
          <p class="stat-label">生产中</p>
          <p class="stat-value text-blue-600">{{ orderStats.running }}</p>
        </button>
        <button class="stat-card text-left" @click="filters.status = 3">
          <p class="stat-label">已完成</p>
          <p class="stat-value text-green-600">{{ orderStats.completed }}</p>
        </button>
      </div>

      <section class="panel">
        <div class="mb-5 overflow-x-auto pb-1">
          <div class="grid min-w-[640px] grid-cols-[minmax(280px,1fr)_180px_92px] items-center gap-3">
            <label class="relative">
              <Search class="search-icon" />
              <input
                v-model="filters.keyword"
                class="input pl-10"
                placeholder="搜索订单编号、客户或产品"
                @keyup.enter="message = '已按当前条件筛选'"
              />
            </label>
            <select v-model="filters.status" class="input">
              <option value="">订单状态：全部</option>
              <option :value="-1">订单状态：未确定</option>
              <option :value="0">订单状态：待确认</option>
              <option :value="1">订单状态：已确认</option>
              <option :value="2">订单状态：生产中</option>
              <option :value="3">订单状态：已完成</option>
              <option :value="4">订单状态：已取消</option>
            </select>
            <button class="btn-secondary w-full" @click="message = '已按当前条件筛选'">
              <Search class="h-4 w-4" />
              查询
            </button>
          </div>
        </div>
        <div v-if="filters.keyword || filters.status !== ''" class="mb-5">
          <button class="btn-ghost" @click="clearFilters">
            <X class="h-4 w-4" />
            清空筛选
          </button>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[980px]">
            <thead>
              <tr>
                <th>订单编号</th>
                <th>客户名称</th>
                <th>产品名称</th>
                <th>数量</th>
                <th>总金额</th>
                <th>下单日期</th>
                <th>交付日期</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="empty-cell">正在加载订单...</td>
              </tr>
              <tr v-else-if="filteredOrders.length === 0">
                <td colspan="9" class="empty-cell">暂无符合条件的订单</td>
              </tr>
              <tr v-for="item in filteredOrders" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.orderNo }}</td>
                <td>{{ displayOrderField(item, 'customerName') }}</td>
                <td>{{ displayOrderField(item, 'productName') }}</td>
                <td>{{ item.quantity ?? 0 }}</td>
                <td>{{ formatMoney(item.totalAmount) }}</td>
                <td>{{ formatDate(item.orderDate) }}</td>
                <td>{{ formatDate(item.deliveryDate) }}</td>
                <td>
                  <span :class="statusClass(item.orderStatus)">{{ orderStatusLabel(item.orderStatus) }}</span>
                </td>
                <td class="text-right whitespace-nowrap">
                  <button class="link-btn inline-flex items-center gap-1" @click="openDetail(item)">
                    <Eye class="h-4 w-4" />
                    查看
                  </button>
                  <button class="link-btn ml-3 inline-flex items-center gap-1" @click="openEdit(item)">
                    <Edit class="h-4 w-4" />
                    修改
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <div v-if="formVisible" class="modal-mask" @click.self="formVisible = false">
        <section class="modal-card max-w-3xl">
          <div class="mb-6 flex items-start justify-between">
            <div>
              <p class="text-sm text-gray-500">销售订单</p>
              <h2 class="mt-1 text-xl font-bold text-gray-900">{{ formMode === 'edit' ? '修改订单' : '新增订单' }}</h2>
              <p class="mt-1 text-sm text-gray-500">客户必须从客户管理中的有效客户列表选择。</p>
            </div>
            <button class="icon-btn" :aria-label="formMode === 'edit' ? '关闭修改订单' : '关闭新增订单'" @click="formVisible = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="field-label">
              订单编号
              <div class="mt-1 flex gap-2">
                <input
                  v-model="form.orderNo"
                  class="input"
                  :readonly="formMode === 'edit'"
                  :class="formMode === 'edit' ? 'bg-gray-50' : ''"
                  placeholder="系统自动生成，可手动调整"
                />
                <button v-if="formMode !== 'edit'" class="btn-secondary shrink-0" type="button" @click="form.orderNo = generateOrderNo()">重新生成</button>
              </div>
            </label>
            <label class="field-label">
              客户
              <select
                v-model.number="form.customerId"
                class="input mt-1"
                :disabled="customerLoading"
                @change="applySelectedCustomer"
              >
                <option value="">{{ customerLoading ? '正在加载客户...' : '请选择客户' }}</option>
                <option v-for="customer in activeCustomers" :key="customer.id" :value="customer.id">
                  {{ customer.customerCode }} - {{ customer.customerName }}
                </option>
              </select>
            </label>
            <label class="field-label">
              客户名称
              <input v-model="form.customerName" class="input mt-1 bg-gray-50" readonly placeholder="选择客户后自动带出" />
            </label>
            <label class="field-label">
              客户电话
              <input v-model="form.customerPhone" class="input mt-1 bg-gray-50" readonly placeholder="选择客户后自动带出" />
            </label>
            <label class="field-label">
              产品名称
              <input v-model="form.productName" class="input mt-1" placeholder="例如：A100有线办公鼠标" />
            </label>
            <label class="field-label">
              优先级
              <select v-model.number="form.priority" class="input mt-1">
                <option :value="1">高</option>
                <option :value="2">普通</option>
                <option :value="3">低</option>
              </select>
            </label>
            <label class="field-label">
              数量
              <input v-model.number="form.quantity" type="number" min="1" class="input mt-1" />
            </label>
            <label class="field-label">
              单价
              <input v-model.number="form.unitPrice" type="number" min="0" step="0.01" class="input mt-1" />
            </label>
            <label class="field-label">
              下单日期
              <input v-model="form.orderDate" type="date" class="input mt-1" />
            </label>
            <label class="field-label">
              交付日期
              <input v-model="form.deliveryDate" type="date" class="input mt-1" />
            </label>
            <div>
              <p class="field-label">订单总金额</p>
              <div class="mt-1 rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-lg font-bold text-gray-900">
                {{ formatMoney(formTotalAmount) }}
              </div>
            </div>
            <label class="field-label md:col-span-2">
              备注
              <textarea v-model="form.remark" class="input mt-1 min-h-[86px]" placeholder="可填写交付要求、包装要求等"></textarea>
            </label>
          </div>

          <p v-if="activeCustomers.length === 0 && !customerLoading" class="mt-4 rounded-xl bg-yellow-50 px-4 py-3 text-sm text-yellow-700">
            当前没有可用客户，请先到客户管理中新增或启用客户。
          </p>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="formVisible = false">取消</button>
            <button class="btn-primary" :disabled="saving || activeCustomers.length === 0" @click="saveOrder">
              {{ saving ? '保存中...' : (formMode === 'edit' ? '保存修改' : '保存订单') }}
            </button>
          </div>
        </section>
      </div>

      <div v-if="detailVisible && selectedOrder" class="modal-mask" @click.self="detailVisible = false">
        <section class="modal-card max-w-3xl">
          <div class="mb-6 flex items-start justify-between">
            <div>
              <p class="text-sm text-gray-500">订单详情</p>
              <h2 class="mt-1 text-xl font-bold text-gray-900">{{ selectedOrder.orderNo }}</h2>
            </div>
            <button class="icon-btn" aria-label="关闭详情" @click="detailVisible = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div v-if="detailLoading" class="empty-cell">正在加载订单详情...</div>
          <template v-else>
            <div class="grid grid-cols-1 gap-4 text-sm md:grid-cols-3">
              <div>
                <p class="text-gray-500">客户名称</p>
                <p class="mt-1 font-medium text-gray-900">{{ displayOrderField(selectedOrder, 'customerName') }}</p>
              </div>
              <div>
                <p class="text-gray-500">联系电话</p>
                <p class="mt-1 font-medium text-gray-900">{{ selectedOrder.customerPhone || '-' }}</p>
              </div>
              <div>
                <p class="text-gray-500">订单状态</p>
                <p class="mt-1">
                  <span :class="statusClass(selectedOrder.orderStatus)">
                    {{ orderStatusLabel(selectedOrder.orderStatus) }}
                  </span>
                </p>
              </div>
              <div>
                <p class="text-gray-500">产品名称</p>
                <p class="mt-1 font-medium text-gray-900">{{ displayOrderField(selectedOrder, 'productName') }}</p>
              </div>
              <div>
                <p class="text-gray-500">数量</p>
                <p class="mt-1 font-medium text-gray-900">{{ selectedOrder.quantity ?? 0 }}</p>
              </div>
              <div>
                <p class="text-gray-500">优先级</p>
                <p class="mt-1">
                  <span :class="priorityClass(selectedOrder.priority)">
                    {{ priorityText[selectedOrder.priority ?? 2] || '普通' }}
                  </span>
                </p>
              </div>
              <div>
                <p class="text-gray-500">单价</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatMoney(selectedOrder.unitPrice) }}</p>
              </div>
              <div>
                <p class="text-gray-500">总金额</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatMoney(selectedOrder.totalAmount) }}</p>
              </div>
              <div>
                <p class="text-gray-500">创建人ID</p>
                <p class="mt-1 font-medium text-gray-900">{{ selectedOrder.createBy || '-' }}</p>
              </div>
              <div>
                <p class="text-gray-500">下单日期</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatDate(selectedOrder.orderDate) }}</p>
              </div>
              <div>
                <p class="text-gray-500">交付日期</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatDate(selectedOrder.deliveryDate) }}</p>
              </div>
              <div>
                <p class="text-gray-500">更新时间</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatDate(selectedOrder.updateTime) }}</p>
              </div>
              <div class="md:col-span-3">
                <p class="text-gray-500">备注</p>
                <p class="mt-1 font-medium text-gray-900">{{ displayOrderField(selectedOrder, 'remark') }}</p>
              </div>
            </div>

            <div class="mt-6">
              <h3 class="font-semibold text-gray-800">流程状态</h3>
              <div class="mt-3 flex flex-wrap items-center gap-2 text-sm">
                <span :class="stepReached(selectedOrder.orderStatus, 0) ? 'tag-green' : 'tag-gray'">接收订单</span>
                <span class="text-gray-300">→</span>
                <span :class="stepReached(selectedOrder.orderStatus, 1) ? 'tag-green' : 'tag-gray'">订单确认</span>
                <span class="text-gray-300">→</span>
                <span :class="stepReached(selectedOrder.orderStatus, 2) ? 'tag-blue' : 'tag-gray'">生产执行</span>
                <span class="text-gray-300">→</span>
                <span :class="stepReached(selectedOrder.orderStatus, 3) ? 'tag-green' : 'tag-gray'">订单完成</span>
                <template v-if="selectedOrder.orderStatus === 4">
                  <span class="text-gray-300">→</span>
                  <span class="tag-gray">已取消</span>
                </template>
              </div>
            </div>
          </template>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-primary" @click="openEdit(selectedOrder); detailVisible = false">修改订单</button>
            <button class="btn-secondary" @click="detailVisible = false">关闭</button>
          </div>
        </section>
      </div>
    </div>
  </MainLayout>
</template>
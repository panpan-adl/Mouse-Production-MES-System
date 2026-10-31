<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, ref } from 'vue'
import { AlertTriangle, Eye, Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
const auth=useAuthStore()
const workOrders=ref<Record<string,any>[]>([])
const workOrdersLoaded=ref(false)
const canGenerateWork=computed(()=>[1,2,9].includes(Number(auth.effectiveRoleId))&&canVisit(Number(auth.effectiveRoleId),'/manufacturing')&&auth.hasMenu('/manufacturing'))
const linkedWork=(plan:ProductionPlan)=>workOrders.value.find(w=>Number(w.plan_id)===Number(plan.id))

import {
  orderApi,
  productionPlanApi,
  productionTaskApi,
  type Order,
  type ProductionPlan,
  type ProductionTask,
} from '@/api/mes'

type PlanStage = 'UNKNOWN' | 'PLANNED' | 'ISSUED' | 'ACCEPTED' | 'COMPLETED'

/** 后端返回的生产计划列表。 */
const plans = ref<ProductionPlan[]>([])

/** 后端返回的销售订单列表，用于显示真实来源订单号和创建计划时选择订单。 */
const orders = ref<Order[]>([])

/** 后端返回的生产任务列表，用于推导计划状态，避免只看 planStatus 导致状态定位错误。 */
const allTasks = ref<ProductionTask[]>([])

/** 当前在详情中查看的生产计划。 */
const selectedPlan = ref<ProductionPlan | null>(null)

/** 当前计划下发出的生产任务列表。 */
const planTasks = ref<ProductionTask[]>([])

/** 详情里任务加载失败时的提示，不影响查看流程弹窗打开。 */
const detailTaskMessage = ref('')

/** 页面加载状态。 */
const loading = ref(false)

/** 保存新增计划时的按钮状态。 */
const saving = ref(false)

/** 计划编号和产品名称搜索关键字。 */
const keyword = ref('')

/** 计划阶段筛选值；空值表示全部。 */
const statusFilter = ref<PlanStage | ''>('')

/** 是否打开计划详情弹窗。 */
const detailVisible = ref(false)

/** 是否打开新增生产计划弹窗。 */
const createVisible = ref(false)

/** 接口失败时显示的错误内容。 */
const errorMessage = ref('')

/** 当前选择用来生成生产计划的订单ID。 */
const editingPlan=ref<ProductionPlan|null>(null)
function editPlan(plan:ProductionPlan){editingPlan.value=plan;selectedOrderId.value=plan.orderId||'';createForm.value={planNo:plan.planNo,startDate:toDateTimeLocal(plan.startDate),endDate:toDateTimeLocal(plan.endDate),priority:plan.priority??2};createVisible.value=true}
const selectedOrderId = ref<number | ''>('')

/** 新增生产计划表单，只保留用户真正需要确认的字段。 */
const createForm = ref({
  planNo: '',
  startDate: '',
  endDate: '',
  priority: 2,
})

/** 演示库中这条历史订单曾经出现过乱码，这里用于显示和新增计划时兜底修正。 */
const demoOrderDisplayFixes: Record<string, Partial<Pick<Order, 'customerName' | 'productName'>>> = {
  'SO-20260715-001': {
    customerName: '北京科创科技有限公司',
    productName: 'A100有线办公鼠标',
  },
}

/** 生产计划按业务链推导出的阶段名称。 */
const stageText: Record<PlanStage, string> = {
  UNKNOWN: '未确定',
  PLANNED: '已制定计划',
  ISSUED: '任务已下达',
  ACCEPTED: '任务已被接收',
  COMPLETED: '已完成',
}

/** 销售订单状态名称。 */
const orderStatusText: Record<number, string> = {
  [-1]: '未确定',
  0: '待确认',
  1: '已确认',
  2: '生产中',
  3: '已完成',
  4: '已取消',
}

/** 用订单ID建立索引，避免生产计划页只显示“订单 #id”。 */
const orderMap = computed(() => {
  const map = new Map<number, Order>()
  orders.value.forEach((order) => {
    if (order.id) map.set(order.id, order)
  })
  return map
})

/** 已经被生产计划占用的来源订单ID。 */
const plannedOrderIds = computed(() => {
  const set = new Set<number>()
  plans.value.forEach((plan) => {
    if (plan.orderId) set.add(plan.orderId)
  })
  return set
})

/** 未制定生产计划的订单，新增计划时只能从这里选择。 */
const unplannedOrders = computed(() => {
  return orders.value.filter((order) => {
    if (!order.id) return false
    const status = normalizeOrderStatus(order.orderStatus)
    return !plannedOrderIds.value.has(order.id) && status !== 3 && status !== 4
  })
})

/** 当前选择的订单。 */
const selectedOrder = computed(() => {
  if (selectedOrderId.value === '') return undefined
  return orders.value.find((order) => order.id === Number(selectedOrderId.value))
})

/** 计划阶段统计数据。 */
const planStats = computed(() => {
  const stages = plans.value.map((plan) => derivePlanStage(plan))
  return {
    total: plans.value.length,
    unplannedOrders: unplannedOrders.value.length,
    unknown: stages.filter((stage) => stage === 'UNKNOWN').length,
    planned: stages.filter((stage) => stage === 'PLANNED').length,
    issued: stages.filter((stage) => stage === 'ISSUED').length,
    accepted: stages.filter((stage) => stage === 'ACCEPTED').length,
    completed: stages.filter((stage) => stage === 'COMPLETED').length,
  }
})

/** 按关键字和阶段过滤后的计划。 */
const filteredPlans = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return plans.value.filter((plan) => {
    const order = getSourceOrder(plan)
    const textMatched = !text ||
      plan.planNo?.toLowerCase().includes(text) ||
      plan.productName?.toLowerCase().includes(text) ||
      order?.orderNo?.toLowerCase().includes(text) ||
      displayOrderField(order, 'customerName').toLowerCase().includes(text)
    const statusMatched = statusFilter.value === '' || derivePlanStage(plan) === statusFilter.value
    return textMatched && statusMatched
  })
})

/** 从后端加载订单、计划和任务；任务失败不影响计划页面展示。 */
async function loadData() {
  loading.value = true
  errorMessage.value = ''
  detailTaskMessage.value = ''
  try {
    const [planData, orderData] = await Promise.all([
      productionPlanApi.list(),
      orderApi.list(),
    ])
    workOrdersLoaded.value=false
    try{workOrders.value=await request.get<Record<string,any>[]>('/fulfillment/work-orders');workOrdersLoaded.value=true}catch{workOrders.value=[]}
    plans.value = planData
    orders.value = orderData

    try {
      allTasks.value = await productionTaskApi.list()
    } catch {
      allTasks.value = []
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载生产计划失败'
  } finally {
    loading.value = false
  }
}

/** 查看计划流程；任务接口失败时仍然打开弹窗，避免点击“查看流程”直接红屏。 */
async function openDetail(plan: ProductionPlan) {
  selectedPlan.value = plan
  detailVisible.value = true
  errorMessage.value = ''
  detailTaskMessage.value = ''
  planTasks.value = tasksForPlan(plan)

  if (!plan.id) return
  try {
    const latestTasks = await productionTaskApi.byPlan(plan.id)
    planTasks.value = latestTasks
    allTasks.value = mergeTasks(allTasks.value, latestTasks, plan.id)
  } catch {
    detailTaskMessage.value = '关联生产任务暂时无法从接口读取，已显示当前页面缓存的任务状态。'
  }
}

/** 打开新增生产计划弹窗，并默认选中第一条未制定计划的订单。 */
function openCreate() {
  editingPlan.value=null
  if (unplannedOrders.value.length === 0) {
    errorMessage.value = '当前没有未制定生产计划的订单'
    showBusinessFormError(errorMessage.value)
    return
  }
  selectedOrderId.value = unplannedOrders.value[0].id || ''
  createForm.value = {
    planNo: generatePlanNo(),
    startDate: toDateTimeLocal(new Date()),
    endDate: toDateTimeLocal(unplannedOrders.value[0].deliveryDate),
    priority: unplannedOrders.value[0].priority ?? 2,
  }
  createVisible.value = true
}

/** 选择订单后，自动同步交期和优先级。 */
function handleOrderChange() {
  const order = selectedOrder.value
  if (!order) return
  createForm.value.endDate = toDateTimeLocal(order.deliveryDate)
  createForm.value.priority = order.priority ?? 2
}

/** 根据选中的订单新增生产计划。 */
async function savePlan() {
  if(editingPlan.value?.id){saving.value=true;try{await productionPlanApi.update(editingPlan.value.id,{...editingPlan.value,planNo:createForm.value.planNo,startDate:fromDateTimeLocal(createForm.value.startDate),endDate:fromDateTimeLocal(createForm.value.endDate),priority:createForm.value.priority});completeDrafts();createVisible.value=false;editingPlan.value=null;await loadData()}catch(e:any){errorMessage.value=e.message;showBusinessFormError(e.message)}finally{saving.value=false}return}

  const order = selectedOrder.value
  if (!order?.id) {
    errorMessage.value = '请先选择来源订单'
    showBusinessFormError(errorMessage.value)
    return
  }
  if (!createForm.value.planNo.trim()) {
    errorMessage.value = '计划编号不能为空'
    showBusinessFormError(errorMessage.value)
    return
  }
  if (!createForm.value.startDate || !createForm.value.endDate) {
    errorMessage.value = '计划开始日期和结束日期不能为空'
    showBusinessFormError(errorMessage.value)
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    await productionPlanApi.create({
      planNo: createForm.value.planNo.trim(),
      orderId: order.id,
      productId: 1,
      productName: displayOrderField(order, 'productName'),
      planQuantity: order.quantity ?? 1,
      completedQuantity: 0,
      startDate: fromDateTimeLocal(createForm.value.startDate),
      endDate: fromDateTimeLocal(createForm.value.endDate),
      planStatus: 0,
      priority: createForm.value.priority,
      createBy: order.createBy,
    })
    completeDrafts();createVisible.value=false
    await loadData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '新增生产计划失败'
  } finally {
    saving.value = false
  }
}

/** 获取计划关联的真实来源订单。 */
function getSourceOrder(plan?: ProductionPlan | null) {
  if (!plan?.orderId) return undefined
  return orderMap.value.get(plan.orderId)
}

/** 获取计划下的生产任务。 */
function tasksForPlan(plan?: ProductionPlan | null) {
  if (!plan?.id) return []
  return allTasks.value.filter((task) => task.planId === plan.id)
}

/** 将指定计划的新任务列表合并进全量任务缓存。 */
function mergeTasks(current: ProductionTask[], latest: ProductionTask[], planId: number) {
  return [...current.filter((task) => task.planId !== planId), ...latest]
}

/** 获取订单展示字段，必要时对演示历史脏数据做显示兜底。 */
function displayOrderField(order: Order | undefined, field: 'customerName' | 'productName') {
  if (!order) return '-'
  return demoOrderDisplayFixes[order.orderNo]?.[field] || order[field] || '-'
}

/** 生产计划列表中的来源订单显示。 */
function sourceOrderLabel(plan: ProductionPlan) {
  const order = getSourceOrder(plan)
  if (!order) return plan.orderId ? `订单 #${plan.orderId}` : '-'
  return `${order.orderNo}｜${displayOrderField(order, 'customerName')}`
}

/** 生产计划列表中的产品名显示，优先使用来源订单，保证订单与计划一致。 */
function sourceProductName(plan: ProductionPlan) {
  const order = getSourceOrder(plan)
  return order ? displayOrderField(order, 'productName') : (plan.productName || '-')
}

/** 按业务链推导计划阶段。 */
function derivePlanStage(plan: ProductionPlan): PlanStage {
  const tasks = tasksForPlan(plan)
  const planQuantity = plan.planQuantity ?? 0
  const completedQuantity = plan.completedQuantity ?? 0

  if (plan.planStatus === 2 || (planQuantity > 0 && completedQuantity >= planQuantity) || (tasks.length > 0 && tasks.every((task) => task.taskStatus === 4))) {
    return 'COMPLETED'
  }
  if (!plan.orderId) {
    return 'UNKNOWN'
  }
  if (tasks.length === 0) {
    return 'PLANNED'
  }
  if (tasks.some((task) => [1, 2, 3].includes(task.taskStatus ?? -1))) {
    return 'ACCEPTED'
  }
  if (tasks.some((task) => task.taskStatus === 0)) {
    return 'ISSUED'
  }
  return 'PLANNED'
}

/** 订单状态归一化，非法值显示为未确定。 */
function normalizeOrderStatus(status?: number) {
  return [0, 1, 2, 3, 4].includes(Number(status)) ? Number(status) : -1
}

/** 显示计划阶段的统一颜色。 */
const flowStages:PlanStage[]=['UNKNOWN','PLANNED','ISSUED','ACCEPTED','COMPLETED']
const currentFlowStage=computed(()=>selectedPlan.value?derivePlanStage(selectedPlan.value):'UNKNOWN')
const visibleFlowStages=computed(()=>currentFlowStage.value==='UNKNOWN'?flowStages.slice(0,1):flowStages)
function stageClass(stage: PlanStage) {
  if (stage === 'COMPLETED') return 'tag-green'
  if (stage === 'ACCEPTED') return 'tag-blue'
  if (stage === 'ISSUED') return 'tag-yellow'
  if (stage === 'PLANNED') return 'tag-blue'
  return 'tag-gray'
}

/** 显示订单状态的统一颜色。 */
function orderStatusClass(status?: number) {
  const normalized = normalizeOrderStatus(status)
  if (normalized === 1 || normalized === 2) return 'tag-blue'
  if (normalized === 3) return 'tag-green'
  if (normalized === 4) return 'tag-gray'
  return 'tag-yellow'
}

/** 将日期转换为简洁的中文日期。 */
function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString('zh-CN') : '-'
}

/** 将日期转换为 datetime-local 输入框可用的值。 */
function toDateTimeLocal(value?: string | Date) {
  const date = value ? new Date(value) : new Date()
  if (Number.isNaN(date.getTime())) return ''
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 将 datetime-local 输入框值转换为后端可解析的 ISO 字符串。 */
function fromDateTimeLocal(value: string) {
  return new Date(value).toISOString()
}

/** 生成正式生产计划编号。 */
function generatePlanNo() {
  const now = new Date()
  const pad = (num: number) => String(num).padStart(2, '0')
  const date = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`
  const time = `${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  return `PP-${date}-${time}`
}

/** 获取任务的可读状态。 */
function taskStatus(task: ProductionTask) {
  return ({ 0: '待领取', 1: '已领取', 2: '生产中', 3: '待质检', 4: '已完成', 5: '已取消' } as Record<number, string>)[task.taskStatus ?? 0] || '未确定'
}

onMounted(loadData)

const completeDrafts=useFormDrafts({createVisible:{open:createVisible,fields:{createForm,selectedOrderId}}})
</script>

<template>
  <MainLayout>
    <div class="animate-fadeIn space-y-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm font-medium text-gray-500">生产管理</p>
          <h1 class="text-2xl font-bold text-gray-800">生产计划</h1>
          <p class="mt-1 text-gray-500">生产计划必须关联真实销售订单；新增计划时只能从未制定计划的订单中选择。</p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="btn-primary" @click="openCreate">
            <Plus class="h-4 w-4" />新增生产计划
          </button>
          <button class="btn-secondary" :disabled="loading" @click="loadData">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />刷新
          </button>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4 xl:grid-cols-6">
        <button class="stat-card text-left" @click="statusFilter = ''">
          <p class="stat-label">计划总数</p>
          <p class="stat-value text-gray-900">{{ planStats.total }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = ''">
          <p class="stat-label">订单未制定计划</p>
          <p class="stat-value text-red-600">{{ planStats.unplannedOrders }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'PLANNED'">
          <p class="stat-label">已制定计划</p>
          <p class="stat-value text-blue-600">{{ planStats.planned }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'ISSUED'">
          <p class="stat-label">任务已下达</p>
          <p class="stat-value text-yellow-600">{{ planStats.issued }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'ACCEPTED'">
          <p class="stat-label">任务已被接收</p>
          <p class="stat-value text-blue-600">{{ planStats.accepted }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'COMPLETED'">
          <p class="stat-label">已完成</p>
          <p class="stat-value text-green-600">{{ planStats.completed }}</p>
        </button>
      </div>

      <section class="panel">
        <div class="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center">
          <label class="relative w-full lg:max-w-lg">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索计划编号、来源订单、客户或产品" />
          </label>
          <select v-model="statusFilter" class="input w-full lg:w-48">
            <option value="">全部阶段</option>
            <option value="UNKNOWN">未确定</option>
            <option value="PLANNED">已制定计划</option>
            <option value="ISSUED">任务已下达</option>
            <option value="ACCEPTED">任务已被接收</option>
            <option value="COMPLETED">已完成</option>
          </select>
          <button class="btn-secondary" @click="keyword = ''; statusFilter = ''">清除筛选</button>
        </div>

        <p v-if="errorMessage" class="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{{ errorMessage }}</p>

        <div class="overflow-x-auto">
          <table class="data-table plan-table">
            <colgroup><col style="width:14%"><col style="width:20%"><col style="width:13%"><col style="width:6%"><col style="width:6%"><col style="width:16%"><col style="width:9%"><col style="width:16%"></colgroup>
            <thead>
              <tr>
                <th>计划编号</th>
                <th>来源订单</th>
                <th>产品名称</th>
                <th>计划数量</th>
                <th>完成数量</th>
                <th>计划周期</th>
                <th>阶段状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="plan in filteredPlans" :key="plan.id">
                <td class="font-medium text-gray-800">{{ plan.planNo }}</td>
                <td>{{ sourceOrderLabel(plan) }}</td>
                <td>{{ sourceProductName(plan) }}</td>
                <td>{{ plan.planQuantity ?? 0 }}</td>
                <td>{{ plan.completedQuantity ?? 0 }}</td>
                <td>{{ formatDate(plan.startDate) }} 至 {{ formatDate(plan.endDate) }}</td>
                <td><span :class="stageClass(derivePlanStage(plan))">{{ stageText[derivePlanStage(plan)] }}</span></td>
                <td>
                  <div class="plan-actions inline-flex items-center whitespace-nowrap flex-nowrap">
                  <button class="link-btn" @click="openDetail(plan)">
                    <Eye class="mr-1 inline h-4 w-4" />查看
                  </button>
                  <button v-if="canGenerateWork" class="link-btn" @click="editPlan(plan)">修改</button>
                  <router-link v-if="canGenerateWork&&workOrdersLoaded&&!linkedWork(plan)&&derivePlanStage(plan)!=='COMPLETED'" class="link-btn" :to="{path:'/manufacturing',query:{createFromPlan:plan.id}}">生成生产工单</router-link>
                  <button v-else-if="canGenerateWork" disabled class="text-xs text-gray-400" :title="linkedWork(plan)?'该计划已生成生产工单':'当前计划不可生成工单'">生成生产工单</button>
                  </div>
                </td>
              </tr>
              <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载生产计划…</td></tr>
              <tr v-else-if="filteredPlans.length === 0"><td colspan="8" class="empty-cell">暂无符合条件的生产计划</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="createVisible" class="modal-mask" data-business-form @click.self="createVisible = false">
      <section class="modal-card max-w-3xl">
        <div class="mb-6 flex items-start justify-between">
          <div>
            <p class="text-sm text-gray-500">从订单创建</p>
            <h2 class="mt-1 text-xl font-bold text-gray-900">{{editingPlan?'修改生产计划':'新增生产计划'}}</h2>
          </div>
          <button class="icon-btn" aria-label="关闭新增生产计划" @click="createVisible = false"><X class="h-5 w-5" /></button>
        </div>

        <div class="space-y-5">
          <label class="field-label">
            来源订单
            <select v-model="selectedOrderId" :disabled="!!editingPlan" class="input mt-1" @change="handleOrderChange">
              <option v-for="order in (editingPlan?orders:unplannedOrders)" :key="order.id" :value="order.id">
                {{ order.orderNo }}｜{{ displayOrderField(order, 'customerName') }}｜{{ displayOrderField(order, 'productName') }}｜{{ order.quantity ?? 0 }}只
              </option>
            </select>
          </label>

          <div v-if="selectedOrder" class="rounded-2xl border border-blue-100 bg-blue-50/60 p-4 text-sm text-gray-700">
            <div class="mb-2 flex items-center gap-2 font-semibold text-blue-700">
              <AlertTriangle class="h-4 w-4" />将按以下订单生成计划
            </div>
            <div class="grid gap-3 md:grid-cols-2">
              <p>订单编号：<span class="font-medium text-gray-900">{{ selectedOrder.orderNo }}</span></p>
              <p>客户：<span class="font-medium text-gray-900">{{ displayOrderField(selectedOrder, 'customerName') }}</span></p>
              <p>产品：<span class="font-medium text-gray-900">{{ displayOrderField(selectedOrder, 'productName') }}</span></p>
              <p>数量：<span class="font-medium text-gray-900">{{ selectedOrder.quantity ?? 0 }}</span></p>
              <p>交付日期：<span class="font-medium text-gray-900">{{ formatDate(selectedOrder.deliveryDate) }}</span></p>
              <p>订单状态：<span :class="orderStatusClass(selectedOrder.orderStatus)">{{ orderStatusText[normalizeOrderStatus(selectedOrder.orderStatus)] }}</span></p>
            </div>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <label class="field-label">计划编号<input v-model="createForm.planNo" class="input mt-1" /></label>
            <label class="field-label">优先级
              <select v-model.number="createForm.priority" class="input mt-1">
                <option :value="1">高</option>
                <option :value="2">中</option>
                <option :value="3">低</option>
              </select>
            </label>
            <label class="field-label">计划开始时间<input v-model="createForm.startDate" type="datetime-local" class="input mt-1" data-date-role="start" data-date-pair="date"></label>
            <label class="field-label">计划结束时间<input v-model="createForm.endDate" type="datetime-local" class="input mt-1" data-date-role="end" data-date-pair="date"></label>
          </div>
        </div>

        <div class="mt-8 flex justify-end gap-3">
          <button class="btn-secondary" type="button" @click="createVisible = false">取消</button>
          <button class="btn-primary" type="button" :disabled="saving" @click="savePlan">{{ saving ? '保存中…' : '保存计划' }}</button>
        </div>
      </section>
    </div>

    <div v-if="detailVisible && selectedPlan" class="modal-mask" data-business-form @click.self="detailVisible = false">
      <section class="modal-card max-w-4xl">
        <div class="mb-6 flex items-start justify-between">
          <div>
            <p class="text-sm text-gray-500">生产计划流程详情</p>
            <h2 class="mt-1 text-xl font-bold text-gray-800">{{ selectedPlan.planNo }}</h2>
          </div>
          <button class="icon-btn" aria-label="关闭详情" @click="detailVisible = false"><X class="h-5 w-5" /></button>
        </div>

        <p v-if="detailTaskMessage" class="mb-4 rounded-lg bg-yellow-50 px-3 py-2 text-sm text-yellow-700">{{ detailTaskMessage }}</p>

        <div class="grid grid-cols-2 gap-x-8 gap-y-5 border-y border-gray-100 py-5 text-sm md:grid-cols-5">
          <div><p class="text-gray-500">来源订单</p><p class="mt-1 font-medium text-gray-800">{{ sourceOrderLabel(selectedPlan) }}</p></div>
          <div><p class="text-gray-500">产品</p><p class="mt-1 font-medium text-gray-800">{{ sourceProductName(selectedPlan) }}</p></div>
          <div><p class="text-gray-500">计划数量</p><p class="mt-1 font-medium text-gray-800">{{ selectedPlan.planQuantity ?? 0 }}</p></div>
          <div><p class="text-gray-500">完成数量</p><p class="mt-1 font-medium text-gray-800">{{ selectedPlan.completedQuantity ?? 0 }}</p></div>
          <div><p class="text-gray-500">当前阶段</p><p class="mt-1"><span :class="stageClass(derivePlanStage(selectedPlan))">{{ stageText[derivePlanStage(selectedPlan)] }}</span></p></div>
        </div>

        <div class="mt-6">
          <h3 class="font-semibold text-gray-800">计划状态</h3>
          <ol class="plan-flow mt-3 flex flex-wrap items-center gap-2 text-sm" aria-label="计划业务进度">
            <li v-for="(stage,index) in visibleFlowStages" :key="stage" class="inline-flex items-center gap-2">
              <span v-if="index" aria-hidden="true" :class="['flow-arrow',{'reached':flowStages.indexOf(stage)<=flowStages.indexOf(currentFlowStage)}]">→</span>
              <span class="flow-stage" :class="{reached:flowStages.indexOf(stage)<=flowStages.indexOf(currentFlowStage),current:stage===currentFlowStage}" :aria-current="stage===currentFlowStage?'step':undefined">{{stageText[stage]}}<span class="sr-only">{{stage===currentFlowStage?'（当前阶段）':flowStages.indexOf(stage)<flowStages.indexOf(currentFlowStage)?'（已经过）':'（未到达）'}}</span></span>
            </li>
          </ol>
        </div>

        <div class="mt-6">
          <h3 class="mb-3 font-semibold text-gray-800">下属生产任务</h3>
          <div class="overflow-x-auto rounded-xl border border-gray-100">
            <table class="data-table min-w-[620px]">
              <thead><tr><th>任务编号</th><th>产品</th><th>数量</th><th>负责人</th><th>状态</th></tr></thead>
              <tbody>
                <tr v-for="task in planTasks" :key="task.id">
                  <td class="font-medium text-gray-800">{{ task.taskNo }}</td>
                  <td>{{ task.productName || '-' }}</td>
                  <td>{{ task.quantity ?? 0 }}</td>
                  <td>{{ task.assignedUserName || '待领取' }}</td>
                  <td>{{ taskStatus(task) }}</td>
                </tr>
                <tr v-if="planTasks.length === 0"><td colspan="5" class="empty-cell">该计划尚未下发生产任务</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<style scoped>
.plan-table{width:100%;table-layout:fixed;min-width:0}
.plan-table th,.plan-table td{padding-left:8px;padding-right:8px;white-space:normal;overflow-wrap:anywhere;font-size:12px}
.plan-table th{font-size:11px}
.plan-actions{gap:8px;max-width:100%}
.plan-actions :is(button,a){font-size:11px;white-space:nowrap;flex-shrink:0}
.plan-actions svg{width:12px;height:12px;margin-right:2px}
@media(max-width:1100px){.plan-table th,.plan-table td{padding-left:4px;padding-right:4px;font-size:11px}.plan-actions{gap:5px}.plan-actions :is(button,a){font-size:10px}.plan-actions svg{display:none}}
.plan-flow .flow-stage{display:inline-flex;align-items:center;padding:5px 10px;border:1px solid #e6e2db;border-radius:6px;background:#f2f0ec;color:#b1aaa0;font-size:12px;line-height:1.5}.plan-flow .flow-stage.reached{background:#e9dfce;border-color:#dac8ac;color:#80633e}.plan-flow .flow-stage.current{background:#80633e;border-color:#80633e;color:#fffdf8;font-weight:600}.plan-flow .flow-arrow{color:#d5d0c7}.plan-flow .flow-arrow.reached{color:#a18761}
</style>

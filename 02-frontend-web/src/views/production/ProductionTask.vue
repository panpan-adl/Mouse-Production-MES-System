<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Eye, Plus, RefreshCw, Search, Send, X } from 'lucide-vue-next'
import MainLayout from '@/components/MainLayout.vue'
import {
  materialApi,
  materialRequestApi,
  productionPlanApi,
  productionReportApi,
  productionTaskApi,
  warehouseApi,
  type Material,
  type ProductionPlan,
  type ProductionReport,
  type ProductionTask,
  type Warehouse,
} from '@/api/mes'
import { getUserList, getUsersByRoleId, type User } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const tasks = ref<ProductionTask[]>([])
const plans = ref<ProductionPlan[]>([])
const workers = ref<User[]>([])
const materials = ref<Material[]>([])
const warehouses = ref<Warehouse[]>([])
const selectedTask = ref<ProductionTask | null>(null)
const taskReports = ref<ProductionReport[]>([])

const loading = ref(false)
const savingTask = ref(false)
const detailVisible = ref(false)
const createTaskVisible = ref(false)
const materialRequestVisible = ref(false)
const feedback = ref('')
const keyword = ref('')
const statusFilter = ref<number | ''>('')

const createTaskForm = reactive({
  planId: undefined as number | undefined,
  taskNo: '',
  taskType: 1,
  productName: '',
  quantity: 1,
  assignedUserId: undefined as number | undefined,
  remark: '',
})

const materialRequestForm = reactive({
  materialId: 0,
  requiredQuantity: 1,
  warehouseId: undefined as number | undefined,
  remark: '',
})

const statusText: Record<number, string> = {
  0: '待领取',
  1: '已派发',
  2: '生产中',
  3: '待质检',
  4: '已完成',
  5: '已取消',
}

const taskTypeText: Record<number, string> = {
  1: '组装',
  2: '加工',
  3: '包装',
  4: '返工',
}

const planMap = computed(() => {
  const map = new Map<number, ProductionPlan>()
  plans.value.forEach((plan) => {
    if (plan.id) map.set(plan.id, plan)
  })
  return map
})

const selectedPlan = computed(() => {
  if (!createTaskForm.planId) return undefined
  return planMap.value.get(Number(createTaskForm.planId))
})

const planTaskCountMap = computed(() => {
  const map = new Map<number, number>()
  tasks.value.forEach((task) => {
    if (!task.planId) return
    const planId = Number(task.planId)
    map.set(planId, (map.get(planId) || 0) + 1)
  })
  return map
})

const formulatedPlans = computed(() => plans.value.filter((plan) => {
  if (!plan.id) return false
  return !isPlanCompleted(plan) && !planTaskCountMap.value.has(plan.id)
}))

const sourcePlans = computed(() => {
  const candidates = formulatedPlans.value
  if (candidates.length > 0) return candidates
  return plans.value.filter((plan) => plan.id && !isPlanCompleted(plan))
})

const isWorker = computed(() => authStore.effectiveRoleId === 3)
const canCreateTask = computed(() => [1, 2, 9].includes(Number(authStore.effectiveRoleId || 0)))
const canCreateMaterialRequest = computed(() => [1, 2, 9].includes(Number(authStore.effectiveRoleId || 0)))

const visibleTasks = computed(() => {
  if (!isWorker.value) return tasks.value
  const currentUserId = authStore.currentUser?.id
  return tasks.value.filter((task) => task.assignedUserId === currentUserId)
})

const taskStats = computed(() => ({
  pending: visibleTasks.value.filter((task) => task.taskStatus === 0).length,
  claimed: visibleTasks.value.filter((task) => task.taskStatus === 1).length,
  inProgress: visibleTasks.value.filter((task) => task.taskStatus === 2).length,
  pendingQc: visibleTasks.value.filter((task) => task.taskStatus === 3).length,
  completed: visibleTasks.value.filter((task) => task.taskStatus === 4).length,
}))

const filteredTasks = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return visibleTasks.value.filter((task) => {
    const textMatched = !normalizedKeyword
      || task.taskNo?.toLowerCase().includes(normalizedKeyword)
      || task.productName?.toLowerCase().includes(normalizedKeyword)
      || task.assignedUserName?.toLowerCase().includes(normalizedKeyword)
    const statusMatched = statusFilter.value === '' || task.taskStatus === statusFilter.value
    return textMatched && statusMatched
  })
})

function isPlanCompleted(plan: ProductionPlan) {
  const planQuantity = Number(plan.planQuantity || 0)
  const completedQuantity = Number(plan.completedQuantity || 0)
  return plan.planStatus === 2 || (planQuantity > 0 && completedQuantity >= planQuantity)
}

function isActiveUser(user: User) {
  return user.deleted !== 1 && user.status !== 0
}

function isDemoCeo(user: User) {
  return Number(user.roleId) === 9 || user.username?.toLowerCase() === 'ceo' || String(user.userCode || '').startsWith('109')
}

function isAssignableUser(user: User) {
  return isActiveUser(user) && (Number(user.roleId) === 3 || isDemoCeo(user))
}

function uniqueUsers(userList: User[]) {
  const map = new Map<number, User>()
  userList.forEach((user) => {
    if (user.id) map.set(user.id, user)
  })
  return Array.from(map.values())
}

function assigneeLabel(user: User) {
  const name = `${user.userCode} - ${user.realName}`
  return isDemoCeo(user) ? `${name}（演示需要）` : name
}
async function loadTasks() {
  loading.value = true
  feedback.value = ''
  const loadErrors: string[] = []

  try {
    tasks.value = await productionTaskApi.list()
  } catch (error) {
    tasks.value = []
    loadErrors.push(`生产任务加载失败：${getErrorMessage(error)}`)
  }

  try {
    plans.value = await productionPlanApi.list()
  } catch (error) {
    plans.value = []
    loadErrors.push(`生产计划加载失败：${getErrorMessage(error)}`)
  }

  try {
    const userData = await getUserList()
    workers.value = uniqueUsers(userData.filter(isAssignableUser))
  } catch (error) {
    const workerData = await getUsersByRoleId(3).catch(() => [] as User[])
    const ceoData = await getUsersByRoleId(9).catch(() => [] as User[])
    workers.value = uniqueUsers([...workerData, ...ceoData].filter(isAssignableUser))
    if (workers.value.length === 0) loadErrors.push(`指派人员加载失败：${getErrorMessage(error)}`)
  }

  try {
    materials.value = await materialApi.list()
  } catch (error) {
    materials.value = []
    loadErrors.push(`物料加载失败：${getErrorMessage(error)}`)
  }

  try {
    warehouses.value = await warehouseApi.list()
  } catch (error) {
    warehouses.value = []
    loadErrors.push(`仓库加载失败：${getErrorMessage(error)}`)
  }

  feedback.value = loadErrors.join('；')
  loading.value = false
}

function openCreateTask() {
  const firstPlan = sourcePlans.value[0]
  Object.assign(createTaskForm, {
    planId: firstPlan?.id,
    taskNo: generateTaskNo(),
    taskType: 1,
    productName: firstPlan?.productName || '',
    quantity: firstPlan?.planQuantity || 1,
    assignedUserId: undefined,
    remark: '',
  })
  createTaskVisible.value = true
}

function handlePlanChange() {
  const plan = selectedPlan.value
  if (!plan) return
  createTaskForm.productName = plan.productName || ''
  createTaskForm.quantity = Math.max(1, Number(plan.planQuantity || 1))
}

async function submitCreateTask() {
  if (!createTaskForm.planId) {
    feedback.value = '请选择来源生产计划'
    return
  }
  if (!createTaskForm.taskNo.trim()) {
    feedback.value = '任务编号不能为空'
    return
  }
  if (!createTaskForm.productName.trim()) {
    feedback.value = '产品名称不能为空'
    return
  }
  if (Number(createTaskForm.quantity) <= 0) {
    feedback.value = '任务数量必须大于 0'
    return
  }

  const assignedWorker = workers.value.find((worker) => worker.id === Number(createTaskForm.assignedUserId))
  savingTask.value = true
  try {
    await productionTaskApi.create({
      taskNo: createTaskForm.taskNo.trim(),
      planId: Number(createTaskForm.planId),
      taskType: Number(createTaskForm.taskType),
      productId: selectedPlan.value?.productId,
      productName: createTaskForm.productName.trim(),
      quantity: Number(createTaskForm.quantity),
      assignedUserId: assignedWorker?.id,
      assignedUserName: assignedWorker?.realName,
      taskStatus: assignedWorker ? 1 : 0,
      remark: createTaskForm.remark.trim(),
    })
    createTaskVisible.value = false
    feedback.value = assignedWorker ? '生产任务已新增并派发给操作工。' : '生产任务已新增，等待操作工领取。'
    await loadTasks()
  } catch (error) {
    feedback.value = getErrorMessage(error)
  } finally {
    savingTask.value = false
  }
}

async function openDetail(task: ProductionTask) {
  selectedTask.value = task
  detailVisible.value = true
  taskReports.value = []
  if (!task.id) return
  try {
    taskReports.value = await productionReportApi.byTask(task.id)
  } catch (error) {
    feedback.value = getErrorMessage(error)
  }
}

function openMaterialRequest(task: ProductionTask) {
  selectedTask.value = task
  Object.assign(materialRequestForm, {
    materialId: materials.value[0]?.id || 0,
    requiredQuantity: task.quantity || 1,
    warehouseId: warehouses.value[0]?.id,
    remark: `生产任务 ${task.taskNo} 的生产用料请求`,
  })
  materialRequestVisible.value = true
}

async function submitMaterialRequest() {
  if (!selectedTask.value || !authStore.currentUser) return
  if (!materialRequestForm.materialId || Number(materialRequestForm.requiredQuantity || 0) <= 0) {
    feedback.value = '请选择物料，并填写大于 0 的需求数量。'
    return
  }
  try {
    await materialRequestApi.create({
      sourceTaskId: selectedTask.value.id,
      sourceTaskNo: selectedTask.value.taskNo,
      productName: selectedTask.value.productName,
      materialId: Number(materialRequestForm.materialId),
      requiredQuantity: Number(materialRequestForm.requiredQuantity),
      warehouseId: materialRequestForm.warehouseId ? Number(materialRequestForm.warehouseId) : undefined,
      requestStatus: 'PENDING_CHECK',
      remark: materialRequestForm.remark,
      createdBy: authStore.currentUser.id,
    })
    materialRequestVisible.value = false
    feedback.value = `任务 ${selectedTask.value.taskNo} 的物料请求已发送给仓管员。`
  } catch (error) {
    feedback.value = getErrorMessage(error)
  }
}

function statusClass(status?: number) {
  if (status === 2 || status === 1) return 'tag-blue'
  if (status === 3 || status === 0) return 'tag-yellow'
  if (status === 4) return 'tag-green'
  return 'tag-gray'
}

function responsibleName(task: ProductionTask) {
  if (task.assignedUserName) return task.assignedUserName
  if (task.taskStatus === 1) return '已派发（未记录负责人）'
  return '待领取'
}

function taskContent(task: ProductionTask) {
  const typeName = task.taskType ? taskTypeText[task.taskType] : ''
  const product = task.productName || '未命名产品'
  const base = typeName ? `${product} - ${typeName}` : product
  return task.remark ? `${base}（${task.remark}）` : base
}

function planLabel(plan: ProductionPlan) {
  return `${plan.planNo} / ${plan.productName || '未命名产品'} / ${plan.planQuantity ?? 0}只`
}

function taskDeadline(task: ProductionTask) {
  const plan = task.planId ? planMap.value.get(task.planId) : undefined
  return formatDateOnly(plan?.endDate)
}

function generateTaskNo() {
  const now = new Date()
  const pad = (num: number) => String(num).padStart(2, '0')
  return `PT-${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

function formatDateOnly(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatDate(value?: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

onMounted(loadTasks)
</script>

<template>
  <MainLayout>
    <div class="animate-fadeIn space-y-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">生产任务</h1>
          <p class="mt-1 text-gray-500">
            {{ isWorker ? '这里只显示车间主任派发给你的生产任务；完成报工后会转入待质检。' : '查看和创建生产任务；操作工只能处理派发给自己的任务。' }}
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button v-if="canCreateTask" class="btn-primary" @click="openCreateTask"><Plus class="h-4 w-4" />新增生产任务</button>
          <button class="btn-secondary" :disabled="loading" @click="loadTasks"><RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />刷新任务</button>
        </div>
      </div>

      <div v-if="feedback" class="flex items-center justify-between rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
        <span>{{ feedback }}</span>
        <button class="icon-btn" aria-label="关闭提示" @click="feedback = ''"><X class="h-4 w-4" /></button>
      </div>

      <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <button v-if="!isWorker" class="stat-card text-left" @click="statusFilter = 0"><p class="stat-label">待领取</p><p class="stat-value text-yellow-600">{{ taskStats.pending }}</p></button>
        <button v-else class="stat-card text-left" @click="statusFilter = 1"><p class="stat-label">已派发待开始</p><p class="stat-value text-yellow-600">{{ taskStats.claimed }}</p></button>
        <button class="stat-card text-left" @click="statusFilter = 2"><p class="stat-label">生产中</p><p class="stat-value text-blue-600">{{ taskStats.inProgress }}</p></button>
        <button class="stat-card text-left" @click="statusFilter = 3"><p class="stat-label">待质检</p><p class="stat-value text-amber-600">{{ taskStats.pendingQc }}</p></button>
        <button class="stat-card text-left" @click="statusFilter = 4"><p class="stat-label">已完成</p><p class="stat-value text-green-600">{{ taskStats.completed }}</p></button>
      </div>

      <section class="panel">
        <div class="mb-5 flex flex-col gap-3 lg:flex-row">
          <label class="relative w-full lg:max-w-md"><Search class="search-icon" /><input v-model="keyword" class="input pl-10" placeholder="搜索任务编号、产品或负责人" /></label>
          <select v-model="statusFilter" class="input w-full lg:w-40">
            <option value="">全部状态</option>
            <option v-if="!isWorker" :value="0">待领取</option>
            <option :value="1">已派发</option>
            <option :value="2">生产中</option>
            <option :value="3">待质检</option>
            <option :value="4">已完成</option>
            <option :value="5">已取消</option>
          </select>
          <button class="btn-secondary" @click="keyword = ''; statusFilter = ''">清除筛选</button>
        </div>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[1080px]">
            <thead><tr><th>任务编号</th><th>任务内容</th><th>数量</th><th>截止时间</th><th>负责人</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="task in filteredTasks" :key="task.id">
                <td class="font-medium text-gray-800">{{ task.taskNo }}</td>
                <td class="max-w-[260px] truncate" :title="taskContent(task)">{{ taskContent(task) }}</td>
                <td>{{ task.quantity ?? 0 }}</td>
                <td>{{ taskDeadline(task) }}</td>
                <td>{{ responsibleName(task) }}</td>
                <td><span :class="statusClass(task.taskStatus)">{{ statusText[task.taskStatus ?? 0] || '未确定' }}</span></td>
                <td>
                  <div class="flex items-center gap-3">
                    <button class="link-btn" @click="openDetail(task)"><Eye class="mr-1 inline h-4 w-4" />查看</button>
                    <button v-if="canCreateMaterialRequest" class="text-sm font-medium text-amber-600 hover:text-amber-700" @click="openMaterialRequest(task)"><Send class="mr-1 inline h-4 w-4" />物料请求</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && filteredTasks.length === 0"><td colspan="7" class="empty-cell">暂无生产任务</td></tr>
              <tr v-if="loading"><td colspan="7" class="empty-cell">正在加载生产任务…</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="createTaskVisible" class="modal-mask" @click.self="createTaskVisible = false">
      <form class="modal-card max-w-3xl" @submit.prevent="submitCreateTask">
        <div class="mb-6 flex items-start justify-between">
          <div><p class="text-sm text-gray-500">生产计划下发</p><h2 class="mt-1 text-xl font-bold text-gray-800">新增生产任务</h2></div>
          <button type="button" class="icon-btn" aria-label="关闭新增生产任务" @click="createTaskVisible = false"><X class="h-5 w-5" /></button>
        </div>
        <div class="grid gap-4 md:grid-cols-2">
          <label class="field-label md:col-span-2">来源生产计划
            <select v-model.number="createTaskForm.planId" class="input mt-1" required @change="handlePlanChange">
              <option v-if="sourcePlans.length === 0" disabled value="">暂无已制定计划的生产计划</option>
              <option v-for="plan in sourcePlans" :key="plan.id" :value="plan.id">{{ planLabel(plan) }}</option>
            </select>
          </label>
          <label class="field-label">任务编号<input v-model="createTaskForm.taskNo" class="input mt-1" required /></label>
          <label class="field-label">任务类型
            <select v-model.number="createTaskForm.taskType" class="input mt-1">
              <option :value="1">组装</option><option :value="2">加工</option><option :value="3">包装</option><option :value="4">返工</option>
            </select>
          </label>
          <label class="field-label">产品名称<input v-model="createTaskForm.productName" class="input mt-1" required /></label>
          <label class="field-label">任务数量<input v-model.number="createTaskForm.quantity" class="input mt-1" min="1" required type="number" /></label>
          <label class="field-label md:col-span-2">指派操作工
            <select v-model.number="createTaskForm.assignedUserId" class="input mt-1">
              <option :value="undefined">暂不指派，等待领取</option>
              <option v-for="worker in workers" :key="worker.id" :value="worker.id">{{ assigneeLabel(worker) }}</option>
            </select>
          </label>
          <label class="field-label md:col-span-2">备注<textarea v-model="createTaskForm.remark" class="input mt-1 min-h-24" placeholder="例如：A100鼠标组装任务，优先处理"></textarea></label>
        </div>
        <div class="mt-6 flex justify-end gap-3"><button type="button" class="btn-secondary" @click="createTaskVisible = false">取消</button><button type="submit" class="btn-primary" :disabled="savingTask">{{ savingTask ? '保存中…' : '保存任务' }}</button></div>
      </form>
    </div>

    <div v-if="detailVisible && selectedTask" class="modal-mask" @click.self="detailVisible = false">
      <section class="modal-card max-w-4xl">
        <div class="mb-6 flex items-start justify-between"><div><p class="text-sm text-gray-500">生产任务详情</p><h2 class="mt-1 text-xl font-bold text-gray-800">{{ selectedTask.taskNo }}</h2></div><button class="icon-btn" aria-label="关闭详情" @click="detailVisible = false"><X class="h-5 w-5" /></button></div>
        <div class="grid grid-cols-2 gap-x-8 gap-y-5 border-y border-gray-100 py-5 text-sm md:grid-cols-4">
          <div><p class="text-gray-500">来源计划</p><p class="mt-1 font-medium text-gray-800">计划 #{{ selectedTask.planId }}</p></div>
          <div><p class="text-gray-500">任务内容</p><p class="mt-1 font-medium text-gray-800">{{ taskContent(selectedTask) }}</p></div>
          <div><p class="text-gray-500">任务数量</p><p class="mt-1 font-medium text-gray-800">{{ selectedTask.quantity ?? 0 }}</p></div>
          <div><p class="text-gray-500">截止时间</p><p class="mt-1 font-medium text-gray-800">{{ taskDeadline(selectedTask) }}</p></div>
          <div><p class="text-gray-500">当前状态</p><p class="mt-1"><span :class="statusClass(selectedTask.taskStatus)">{{ statusText[selectedTask.taskStatus ?? 0] }}</span></p></div>
          <div><p class="text-gray-500">负责人</p><p class="mt-1 font-medium text-gray-800">{{ responsibleName(selectedTask) }}</p></div>
          <div><p class="text-gray-500">开始时间</p><p class="mt-1 font-medium text-gray-800">{{ formatDate(selectedTask.startTime) }}</p></div>
          <div><p class="text-gray-500">结束时间</p><p class="mt-1 font-medium text-gray-800">{{ formatDate(selectedTask.endTime) }}</p></div>
          <div><p class="text-gray-500">备注</p><p class="mt-1 font-medium text-gray-800">{{ selectedTask.remark || '-' }}</p></div>
        </div>
        <div class="mt-6"><h3 class="font-semibold text-gray-800">状态流转</h3><div class="mt-3 flex flex-wrap gap-2 text-sm"><span :class="selectedTask.taskStatus! >= 0 ? 'tag-green' : 'tag-gray'">待领取</span><span class="text-gray-300">→</span><span :class="selectedTask.taskStatus! >= 1 ? 'tag-green' : 'tag-gray'">已派发</span><span class="text-gray-300">→</span><span :class="selectedTask.taskStatus! >= 2 ? 'tag-green' : 'tag-gray'">生产中</span><span class="text-gray-300">→</span><span :class="selectedTask.taskStatus! >= 3 ? 'tag-green' : 'tag-gray'">待质检</span><span class="text-gray-300">→</span><span :class="selectedTask.taskStatus! >= 4 ? 'tag-green' : 'tag-gray'">已完成</span></div></div>
        <div class="mt-6"><h3 class="mb-3 font-semibold text-gray-800">报工记录</h3><div class="overflow-x-auto rounded-xl border border-gray-100"><table class="data-table min-w-[620px]"><thead><tr><th>报工人</th><th>完成数量</th><th>报工时间</th><th>备注</th></tr></thead><tbody><tr v-for="report in taskReports" :key="report.id"><td>{{ report.reportUserName }}</td><td>{{ report.reportQuantity }}</td><td>{{ formatDate(report.reportTime) }}</td><td>{{ report.remark || '-' }}</td></tr><tr v-if="taskReports.length === 0"><td colspan="4" class="empty-cell">暂无报工记录</td></tr></tbody></table></div></div>
      </section>
    </div>

    <div v-if="materialRequestVisible && selectedTask" class="modal-mask" @click.self="materialRequestVisible = false">
      <form class="modal-card max-w-xl" @submit.prevent="submitMaterialRequest">
        <div class="mb-6 flex items-start justify-between"><div><p class="text-sm text-gray-500">发送给仓管员</p><h2 class="mt-1 text-xl font-bold text-gray-800">{{ selectedTask.taskNo }} 物料请求</h2></div><button type="button" class="icon-btn" aria-label="关闭物料请求" @click="materialRequestVisible = false"><X class="h-5 w-5" /></button></div>
        <div class="space-y-4"><label class="block"><span class="field-label">需求物料</span><select v-model.number="materialRequestForm.materialId" class="input mt-1" required><option v-for="material in materials" :key="material.id" :value="material.id">{{ material.materialCode }} / {{ material.materialName }}</option></select></label><label class="block"><span class="field-label">需求数量</span><input v-model.number="materialRequestForm.requiredQuantity" class="input mt-1" required min="1" type="number" /></label><label class="block"><span class="field-label">建议出料仓库</span><select v-model.number="materialRequestForm.warehouseId" class="input mt-1"><option :value="undefined">暂不指定</option><option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">{{ warehouse.warehouseCode }} / {{ warehouse.warehouseName }}</option></select></label><label class="block"><span class="field-label">说明</span><textarea v-model="materialRequestForm.remark" class="input mt-1 min-h-24" placeholder="例如：该生产任务下发前需确认物料齐套"></textarea></label><p class="rounded-lg bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-700">提交后，仓管员会在“仓库管理 / 物料请求”中看到该需求；若库存不足，仓管员可继续发送采购请求给采购员。</p></div>
        <div class="mt-6 flex justify-end gap-3"><button type="button" class="btn-secondary" @click="materialRequestVisible = false">取消</button><button type="submit" class="btn-primary">发送物料请求</button></div>
      </form>
    </div>
  </MainLayout>
</template>
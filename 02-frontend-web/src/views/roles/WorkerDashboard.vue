<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { AlertTriangle, Calendar, Clock, PackageCheck, Play, RefreshCw, Wrench, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { attendanceApi, type Attendance } from '@/api/attendance'
import { equipmentApi, maintenanceApi, productionPlanApi, productionTaskApi, type Equipment, type Maintenance, type ProductionPlan, type ProductionTask } from '@/api/mes'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const tasks = ref<ProductionTask[]>([])
const plans = ref<ProductionPlan[]>([])
const equipments = ref<Equipment[]>([])
const maintenances = ref<Maintenance[]>([])
const attendances = ref<Attendance[]>([])
const selectedTask = ref<ProductionTask | null>(null)
const showReportForm = ref(false)
const showRepairForm = ref(false)

const reportForm = reactive({
  reportQuantity: 0,
  remark: '',
})

const repairForm = reactive({
  equipmentId: 0,
  faultDesc: '',
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

const myTasks = computed(() => {
  const userId = currentUser.value?.id
  return tasks.value.filter((task) => task.assignedUserId === userId)
})

const planMap = computed(() => {
  const map = new Map<number, ProductionPlan>()
  plans.value.forEach((plan) => {
    if (plan.id) map.set(plan.id, plan)
  })
  return map
})

const myRepairRequests = computed(() => {
  const user = currentUser.value
  if (!user) return []
  return maintenances.value.filter((item) => {
    const remark = item.remark || ''
    return remark.includes(user.userCode) || remark.includes(user.realName)
  })
})

const todayAttendances = computed(() => {
  const today = toDateInput(new Date())
  return attendances.value.filter((item) => normalizeDate(item.attendanceDate) === today)
})

const stats = computed(() => ({
  assigned: myTasks.value.filter((task) => task.taskStatus === 1).length,
  inProgress: myTasks.value.filter((task) => task.taskStatus === 2).length,
  pendingQc: myTasks.value.filter((task) => task.taskStatus === 3).length,
  repairPending: myRepairRequests.value.filter((item) => item.status !== 'FINISHED' && item.status !== 'CANCELLED').length,
}))

onMounted(loadAll)

async function loadAll() {
  if (!currentUser.value) return
  loading.value = true
  message.value = ''
  try {
    const [taskData, planData, equipmentData, maintenanceData, attendanceData] = await Promise.all([
      productionTaskApi.list(),
      productionPlanApi.list(),
      equipmentApi.list(),
      maintenanceApi.list(),
      attendanceApi.byUser(currentUser.value.id),
    ])
    tasks.value = taskData
    plans.value = planData
    equipments.value = equipmentData
    maintenances.value = maintenanceData
    attendances.value = attendanceData
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function startTask(task: ProductionTask) {
  if (!task.id || !currentUser.value) return
  try {
    await productionTaskApi.start(task.id, {
      userId: currentUser.value.id,
      userName: currentUser.value.realName,
    })
    message.value = `任务 ${task.taskNo} 已开始生产`
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  }
}

function openReport(task: ProductionTask) {
  selectedTask.value = task
  reportForm.reportQuantity = 0
  reportForm.remark = ''
  showReportForm.value = true
}

async function submitReport() {
  if (!selectedTask.value?.id || !currentUser.value) return
  if (Number(reportForm.reportQuantity) <= 0) {
    message.value = '报工数量必须大于0'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    const updatedTask = await productionTaskApi.report(selectedTask.value.id, {
      userId: currentUser.value.id,
      userName: currentUser.value.realName,
      reportQuantity: Number(reportForm.reportQuantity),
      remark: reportForm.remark.trim(),
    })
    completeDrafts();showReportForm.value=false
    message.value = updatedTask.taskStatus === 3 ? '报工完成，任务已转入待质检' : '报工已提交'
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function openRepairForm() {
  repairForm.equipmentId = equipments.value.find((item) => item.status !== 'SCRAPPED')?.id || 0
  repairForm.faultDesc = ''
  repairForm.remark = ''
  showRepairForm.value = true
}

async function submitRepairRequest() {
  if (!currentUser.value) return
  if (!repairForm.equipmentId || !repairForm.faultDesc.trim()) {
    message.value = '请选择故障设备，并填写故障描述'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    await maintenanceApi.create({
      maintenanceNo: `MR-${Date.now()}`,
      equipmentId: Number(repairForm.equipmentId),
      maintenanceType: 2,
      faultDesc: repairForm.faultDesc.trim(),
      remark: `申请人：${currentUser.value.realName}（${currentUser.value.userCode}）；${repairForm.remark || '操作工提交设备故障维修申请'}`,
    })
    completeDrafts();showRepairForm.value=false
    message.value = '维修申请已提交，车间主任可在工作台中指派维修人员'
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function canStart(task: ProductionTask) {
  return task.taskStatus === 1 && task.assignedUserId === currentUser.value?.id
}

function canReport(task: ProductionTask) {
  return task.taskStatus === 2 && task.assignedUserId === currentUser.value?.id
}

function taskStatusClass(status?: number) {
  if (status === 1 || status === 2) return 'tag-blue'
  if (status === 0 || status === 3) return 'tag-yellow'
  if (status === 4) return 'tag-green'
  return 'tag-gray'
}

function maintenanceStatusText(status?: string) {
  const map: Record<string, string> = { PENDING: '待指派', PROCESSING: '维修中', FINISHED: '已完成', CANCELLED: '已取消' }
  return map[status || ''] || status || '-'
}

function maintenanceStatusClass(status?: string) {
  if (status === 'FINISHED') return 'tag-green'
  if (status === 'PROCESSING') return 'tag-blue'
  if (status === 'PENDING') return 'tag-yellow'
  return 'tag-gray'
}

function attendanceStatusText(status?: number) {
  const map: Record<number, string> = { 0: '缺勤', 1: '正常', 2: '迟到', 3: '早退', 4: '请假' }
  return map[Number(status)] || '未登记'
}

function taskContent(task: ProductionTask) {
  const typeMap: Record<number, string> = {
    1: '组装',
    2: '加工',
    3: '包装',
    4: '返工',
  }
  const product = task.productName || '未命名产品'
  const type = typeMap[Number(task.taskType)] || '生产'
  return task.remark ? `${product}｜${type}｜${task.remark}` : `${product}｜${type}`
}

function taskDeadline(task: ProductionTask) {
  const plan = task.planId ? planMap.value.get(task.planId) : undefined
  return formatDateOnly(plan?.endDate)
}

function formatDateOnly(value?: string | null) {
  return value ? String(value).slice(0, 10) : '-'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function normalizeDate(value?: string | null) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

function toDateInput(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return localDate.toISOString().slice(0, 10)
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

const completeDrafts=useFormDrafts({showReportForm:{open:showReportForm,fields:{reportForm},record:()=>selectedTask.value?.id},showRepairForm:{open:showRepairForm,fields:{repairForm}}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="rounded-2xl bg-gradient-to-r from-green-600 to-green-700 p-8 text-white">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div class="flex items-center">
            <div class="mr-6 flex h-20 w-20 items-center justify-center rounded-full bg-white/20">
              <Wrench class="h-10 w-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">操作工工作台</h1>
              <p class="mt-2 text-green-100">欢迎回来，{{ currentUser?.realName }}！这里只处理车间主任派发给你的生产任务、报工和设备故障申请。</p>
              <p class="mt-1 text-sm text-green-200">用户编码：{{ currentUser?.userCode }} | 角色：{{ currentUser?.roleName }}</p>
            </div>
          </div>
          <button class="rounded-xl bg-white px-4 py-2 text-sm font-medium text-green-700 shadow-sm hover:bg-green-50" :disabled="loading" @click="loadAll">
            <RefreshCw class="mr-2 inline h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><p class="stat-label">已派发待开始</p><p class="stat-value text-yellow-600">{{ stats.assigned }}</p></div>
        <div class="stat-card"><p class="stat-label">我的生产中</p><p class="stat-value text-blue-600">{{ stats.inProgress }}</p></div>
        <div class="stat-card"><p class="stat-label">待质检任务</p><p class="stat-value text-amber-600">{{ stats.pendingQc }}</p></div>
        <div class="stat-card"><p class="stat-label">我的维修申请</p><p class="stat-value text-red-600">{{ stats.repairPending }}</p></div>
      </div>

      <section class="panel">
        <div class="mb-4 flex items-center justify-between">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">我的生产任务</h2>
            <p class="mt-1 text-sm text-gray-500">只显示车间主任派发给你的任务，不显示其他操作工任务。</p>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table min-w-[980px]">
            <thead><tr><th>任务编号</th><th>任务内容</th><th>数量</th><th>截止时间</th><th>负责人</th><th>状态</th><th class="text-right">操作</th></tr></thead>
            <tbody>
              <tr v-if="loading"><td colspan="7" class="empty-cell">正在加载任务...</td></tr>
              <tr v-else-if="myTasks.length === 0"><td colspan="7" class="empty-cell">暂无车间主任派发给你的生产任务</td></tr>
              <tr v-for="task in myTasks" v-else :key="task.id">
                <td class="font-medium text-gray-900">{{ task.taskNo }}</td>
                <td class="max-w-[300px] truncate" :title="taskContent(task)">{{ taskContent(task) }}</td>
                <td>{{ task.quantity || 0 }}</td>
                <td>{{ taskDeadline(task) }}</td>
                <td>{{ task.assignedUserName || '待领取' }}</td>
                <td><span :class="taskStatusClass(task.taskStatus)">{{ statusText[task.taskStatus ?? 0] }}</span></td>
                <td class="text-right">
                  <button v-if="canStart(task)" class="link-btn mr-3" @click="startTask(task)"><Play class="mr-1 inline h-4 w-4" />开始</button>
                  <button v-if="canReport(task)" class="link-btn" @click="openReport(task)"><PackageCheck class="mr-1 inline h-4 w-4" />报工</button>
                  <span v-if="!canStart(task) && !canReport(task)" class="text-sm text-gray-400">暂无操作</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <div class="panel">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">设备故障维修申请</h2>
              <p class="mt-1 text-sm text-gray-500">设备故障时提交申请，车间主任会收到并指派维修员。</p>
            </div>
            <button class="btn-primary" @click="openRepairForm"><AlertTriangle class="h-4 w-4" />提交维修申请</button>
          </div>
          <div class="overflow-x-auto">
            <table class="data-table min-w-[720px]">
              <thead><tr><th>维修单号</th><th>设备</th><th>故障描述</th><th>维修员</th><th>状态</th></tr></thead>
              <tbody>
                <tr v-if="myRepairRequests.length === 0"><td colspan="5" class="empty-cell">暂无本人提交的维修申请</td></tr>
                <tr v-for="item in myRepairRequests" v-else :key="item.id">
                  <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                  <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                  <td class="max-w-[220px] truncate" :title="item.faultDesc">{{ item.faultDesc || '-' }}</td>
                  <td>{{ item.maintenanceUserName || '待指派' }}</td>
                  <td><span :class="maintenanceStatusClass(item.status)">{{ maintenanceStatusText(item.status) }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel">
          <h2 class="text-lg font-semibold text-gray-900">我的今日考勤</h2>
          <p class="mt-1 text-sm text-gray-500">操作工只能查看自己的考勤。</p>
          <div class="mt-4 space-y-3">
            <div v-if="todayAttendances.length === 0" class="rounded-xl border border-dashed border-gray-200 p-6 text-center text-sm text-gray-400">今天暂无考勤记录</div>
            <div v-for="item in todayAttendances" :key="item.id" class="rounded-xl bg-gray-50 p-4">
              <div class="flex items-center justify-between">
                <span class="font-medium text-gray-800">{{ normalizeDate(item.attendanceDate) }}</span>
                <span class="tag-blue">{{ attendanceStatusText(item.status) }}</span>
              </div>
              <div class="mt-3 grid grid-cols-2 gap-3 text-sm text-gray-600">
                <span><Clock class="mr-1 inline h-4 w-4" />签到：{{ formatDate(item.checkInTime || undefined) }}</span>
                <span><Calendar class="mr-1 inline h-4 w-4" />工时：{{ Number(item.workHours || 0).toFixed(2) }}h</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="showReportForm && selectedTask" class="modal-mask" data-business-form @click.self="showReportForm = false">
      <form class="modal-card max-w-lg" @submit.prevent="submitReport"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-5 flex items-center justify-between">
          <div><p class="text-sm text-gray-500">生产报工</p><h2 class="text-xl font-bold text-gray-900">{{ selectedTask.taskNo }}</h2></div>
          <button type="button" class="icon-btn" @click="showReportForm = false"><X class="h-5 w-5" /></button>
        </div>
        <div class="mb-5 grid grid-cols-1 gap-3 rounded-xl bg-gray-50 p-4 text-sm text-gray-600 sm:grid-cols-2">
          <div><span class="text-gray-400">任务内容：</span>{{ taskContent(selectedTask) }}</div>
          <div><span class="text-gray-400">截止时间：</span>{{ taskDeadline(selectedTask) }}</div>
          <div><span class="text-gray-400">任务数量：</span>{{ selectedTask.quantity || 0 }}</div>
          <div><span class="text-gray-400">当前状态：</span>{{ statusText[selectedTask.taskStatus ?? 0] }}</div>
        </div>
        <label class="field-label">本次完成数量<input v-model.number="reportForm.reportQuantity" type="number" min="1" class="input mt-1" /></label>
        <label class="field-label mt-4">生产说明<textarea v-model="reportForm.remark" class="input mt-1 h-24" placeholder="填写本次生产执行情况"></textarea></label>
        <div class="mt-6 flex justify-end gap-3"><button type="button" class="btn-secondary" @click="showReportForm = false">取消</button><button class="btn-primary" :disabled="saving">{{ saving ? '提交中...' : '提交报工' }}</button></div>
      </form>
    </div>

    <div v-if="showRepairForm" class="modal-mask" data-business-form @click.self="showRepairForm = false">
      <form class="modal-card max-w-xl" @submit.prevent="submitRepairRequest"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-5 flex items-center justify-between">
          <div><p class="text-sm text-gray-500">设备维修申请</p><h2 class="text-xl font-bold text-gray-900">提交故障设备</h2></div>
          <button type="button" class="icon-btn" @click="showRepairForm = false"><X class="h-5 w-5" /></button>
        </div>
        <label class="field-label">故障设备
          <select v-model.number="repairForm.equipmentId" class="input mt-1">
            <option :value="0">请选择设备</option>
            <option v-for="item in equipments" :key="item.id" :value="item.id" :disabled="item.status === 'SCRAPPED'">
              {{ item.equipmentCode }} - {{ item.equipmentName }}（{{ item.status }}）
            </option>
          </select>
        </label>
        <label class="field-label mt-4">故障描述<textarea v-model="repairForm.faultDesc" class="input mt-1 h-24" placeholder="例如：组装线电机异响，无法继续生产"></textarea></label>
        <label class="field-label mt-4">备注<input v-model="repairForm.remark" class="input mt-1" placeholder="可填写工位、班次等补充信息" /></label>
        <div class="mt-6 flex justify-end gap-3"><button type="button" class="btn-secondary" @click="showRepairForm = false">取消</button><button class="btn-primary" :disabled="saving">{{ saving ? '提交中...' : '提交申请' }}</button></div>
      </form>
    </div>
  </MainLayout>
</template>

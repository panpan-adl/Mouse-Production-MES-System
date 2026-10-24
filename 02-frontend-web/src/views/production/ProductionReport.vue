<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import { PackageCheck, Play, Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import MainLayout from '@/components/MainLayout.vue'
import { productionReportApi, productionTaskApi, type ProductionReport, type ProductionTask } from '@/api/mes'
import { getUserList, getUsersByRoleId, type User } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const reports = ref<ProductionReport[]>([])
const tasks = ref<ProductionTask[]>([])
const workers = ref<User[]>([])
const loading = ref(false)
const saving = ref(false)
const reportVisible = ref(false)
const keyword = ref('')
const feedback = ref('')
const activeTab = ref<'tasks' | 'records'>('tasks')
const formMode = ref<'create' | 'edit'>('create')
const editingReportId = ref<number | undefined>()

const form = reactive({
  taskId: undefined as number | undefined,
  reportUserId: undefined as number | undefined,
  reportQuantity: 1,
  remark: '',
})

const isWorker = computed(() => authStore.effectiveRoleId === 3)
const canCreateReport = computed(() => Boolean(authStore.currentUser))

const taskMap = computed(() => {
  const map = new Map<number, ProductionTask>()
  tasks.value.forEach((task) => {
    if (task.id) map.set(task.id, task)
  })
  return map
})

const taskReportedMap = computed(() => {
  const map = new Map<number, number>()
  reports.value.forEach((report) => {
    map.set(report.taskId, (map.get(report.taskId) || 0) + Number(report.reportQuantity || 0))
  })
  return map
})

const reportableTasks = computed(() => {
  return tasks.value.filter((task) => {
    if (!task.id || !task.assignedUserId) return false
    if (![1, 2].includes(Number(task.taskStatus))) return false
    if (isWorker.value) return task.assignedUserId === authStore.currentUser?.id
    return true
  })
})

const executableTasks = computed(() => reportableTasks.value)

const selectedTask = computed(() => {
  if (!form.taskId) return undefined
  return taskMap.value.get(Number(form.taskId))
})

const selectedReporter = computed(() => {
  if (!form.reportUserId) return undefined
  return workers.value.find((worker) => worker.id === Number(form.reportUserId))
})

const filteredReports = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  const visibleReports = isWorker.value
    ? reports.value.filter((report) => report.reportUserId === authStore.currentUser?.id)
    : reports.value
  if (!text) return visibleReports
  return visibleReports.filter((report) => {
    const task = taskMap.value.get(report.taskId)
    return task?.taskNo?.toLowerCase().includes(text)
      || task?.productName?.toLowerCase().includes(text)
      || report.reportUserName?.toLowerCase().includes(text)
  })
})

async function loadData() {
  loading.value = true
  feedback.value = ''
  try {
    const [reportList, taskList] = await Promise.all([
      productionReportApi.list(),
      productionTaskApi.list(),
    ])
    reports.value = reportList
    tasks.value = taskList
    await loadWorkerOptions(taskList)
  } catch (error) {
    feedback.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadWorkerOptions(taskList: ProductionTask[]) {
  const assignedUserIds = new Set(taskList.map((task) => task.assignedUserId).filter(Boolean) as number[])
  try {
    const allUsers = await getUserList()
    workers.value = allUsers
      .filter((user) => user.deleted !== 1 && user.status !== 0)
      .filter((user) => user.roleId === 3 || user.roleId === 9 || assignedUserIds.has(user.id))
  } catch {
    const workerList = await getUsersByRoleId(3)
    workers.value = workerList.filter((worker) => worker.deleted !== 1 && worker.status !== 0)
  }
}

function openCreateReport() {
  if (reportableTasks.value.length === 0) {
    feedback.value = '当前没有可报工的任务：需要任务已经派发给操作工，并处于“已派发”或“生产中”。'
    return
  }
  formMode.value = 'create'
  editingReportId.value = undefined
  const firstTask = reportableTasks.value[0]
  form.taskId = firstTask?.id
  form.reportUserId = isWorker.value ? authStore.currentUser?.id : firstTask?.assignedUserId
  form.reportQuantity = defaultReportQuantity(firstTask)
  form.remark = ''
  reportVisible.value = true
}

function openCreateReportForTask(task: ProductionTask) {
  formMode.value = 'create'
  editingReportId.value = undefined
  form.taskId = task.id
  form.reportUserId = isWorker.value ? authStore.currentUser?.id : task.assignedUserId
  form.reportQuantity = defaultReportQuantity(task)
  form.remark = ''
  reportVisible.value = true
}

async function startTask(task: ProductionTask) {
  if (!task.id || !authStore.currentUser) return
  saving.value = true
  feedback.value = ''
  try {
    await productionTaskApi.start(task.id, {
      userId: authStore.currentUser.id,
      userName: authStore.currentUser.realName,
    })
    feedback.value = `任务 ${task.taskNo} 已开始生产，可以继续报工。`
    await loadData()
  } catch (error) {
    feedback.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function openEditReport(report: ProductionReport) {
  if (isWorker.value && report.reportUserId !== authStore.currentUser?.id) {
    feedback.value = '操作工只能修改自己的报工记录。'
    return
  }
  formMode.value = 'edit'
  editingReportId.value = report.id
  form.taskId = report.taskId
  form.reportUserId = report.reportUserId
  form.reportQuantity = report.reportQuantity
  form.remark = report.remark || ''
  reportVisible.value = true
}

function handleTaskChange() {
  const task = selectedTask.value
  if (!task) return
  form.reportUserId = isWorker.value ? authStore.currentUser?.id : task.assignedUserId
  form.reportQuantity = defaultReportQuantity(task)
}

async function submitReport() {
  const task = selectedTask.value
  const reporter = selectedReporter.value
  if (!task?.id) {
    feedback.value = '请选择可报工的生产任务。'
    return
  }
  if (!form.reportUserId) {
    feedback.value = '请选择报工人。'
    return
  }
  if (Number(form.reportQuantity) <= 0) {
    feedback.value = '完成数量必须大于 0。'
    return
  }

  saving.value = true
  feedback.value = ''
  try {
    if (formMode.value === 'edit' && editingReportId.value) {
      const updated = await productionReportApi.update(editingReportId.value, {
        reportUserId: Number(form.reportUserId),
        reportUserName: reporter?.realName || authStore.currentUser?.realName || '报工人',
        reportQuantity: Number(form.reportQuantity),
        remark: form.remark.trim(),
      })
      completeDrafts();reportVisible.value=false
      feedback.value = `报工记录 ${updated.id || editingReportId.value} 已修改。`
    } else {
      const updatedTask = await productionTaskApi.report(task.id, {
        userId: Number(form.reportUserId),
        userName: reporter?.realName || authStore.currentUser?.realName || '报工人',
        reportQuantity: Number(form.reportQuantity),
        remark: form.remark.trim(),
      })
      completeDrafts();reportVisible.value=false
      feedback.value = updatedTask.taskStatus === 3
        ? `任务 ${updatedTask.taskNo} 已全部报工，进入待质检。`
        : `任务 ${updatedTask.taskNo} 报工成功。`
    }
    await loadData()
  } catch (error) {
    feedback.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function markReportCompleted(report: ProductionReport) {
  const task = taskMap.value.get(report.taskId)
  if (!task?.id || !report.id) {
    feedback.value = '找不到该报工对应的生产任务。'
    return
  }
  if (isWorker.value && report.reportUserId !== authStore.currentUser?.id) {
    feedback.value = '操作工只能完成自己的报工记录。'
    return
  }
  const total = taskReportedMap.value.get(report.taskId) || 0
  const taskQuantity = Number(task.quantity || 0)
  const remaining = Math.max(taskQuantity - total, 0)
  const nextQuantity = Number(report.reportQuantity || 0) + remaining
  saving.value = true
  feedback.value = ''
  try {
    await productionReportApi.update(report.id, {
      reportUserId: report.reportUserId,
      reportUserName: report.reportUserName,
      reportQuantity: nextQuantity,
      remark: report.remark,
    })
    feedback.value = remaining > 0
      ? `已补足 ${remaining} 件，任务 ${task.taskNo} 进入待质检。`
      : `任务 ${task.taskNo} 已经达到报工完成数量。`
    await loadData()
  } catch (error) {
    feedback.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function canCompleteReport(report: ProductionReport) {
  const task = taskMap.value.get(report.taskId)
  if (!task || ![1, 2].includes(Number(task.taskStatus))) return false
  if (isWorker.value && report.reportUserId !== authStore.currentUser?.id) return false
  return remainingQuantity(task) > 0
}

function canStartTask(task: ProductionTask) {
  return Number(task.taskStatus) === 1 && task.assignedUserId === authStore.currentUser?.id
}

function canReportTask(task: ProductionTask) {
  return Number(task.taskStatus) === 2 && task.assignedUserId === authStore.currentUser?.id
}

function taskResponsibleName(task: ProductionTask) {
  if (task.assignedUserName) return task.assignedUserName
  if (Number(task.taskStatus) === 1) return '已派发（未记录负责人）'
  return '待领取'
}

function defaultReportQuantity(task?: ProductionTask) {
  if (!task) return 1
  return Math.max(Math.min(remainingQuantity(task), 1), 1)
}

function remainingQuantity(task: ProductionTask) {
  return Math.max(Number(task.quantity || 0) - (taskReportedMap.value.get(Number(task.id)) || 0), 0)
}

function taskNo(taskId: number) {
  return taskMap.value.get(taskId)?.taskNo || `任务 #${taskId}`
}

function productName(taskId: number) {
  return taskMap.value.get(taskId)?.productName || '-'
}

function taskStatusLabel(status?: number) {
  const map: Record<number, string> = {
    0: '待领取',
    1: '已派发',
    2: '生产中',
    3: '待质检',
    4: '已完成',
    5: '已取消',
  }
  return status === undefined || status === null ? '未确定' : map[status] || '未确定'
}

function taskLabel(task: ProductionTask) {
  return `${task.taskNo} / ${task.productName || '未命名产品'} / ${taskStatusLabel(task.taskStatus)} / 负责人：${task.assignedUserName || '未指派'} / 剩余：${remainingQuantity(task)}`
}

function workerLabel(worker: User) {
  return `${worker.userCode} - ${worker.realName}${worker.roleId === 9 ? '（演示需要）' : ''}`
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试。'
}

onMounted(loadData)

const completeDrafts=useFormDrafts({reportVisible:{open:reportVisible,fields:{form},record:()=>editingReportId.value||selectedTask.value?.id}})
</script>

<template>
  <MainLayout>
    <div class="animate-fadeIn space-y-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">生产报工</h1>
          <p class="mt-1 text-gray-500">从已派发或生产中的任务提交实际完成数量；累计完成后任务自动进入待质检。</p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button v-if="canCreateReport" class="btn-primary" @click="openCreateReport">
            <Plus class="h-4 w-4" />新增报工
          </button>
          <button class="btn-secondary" :disabled="loading" @click="loadData">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />刷新记录
          </button>
        </div>
      </div>

      <div v-if="feedback" class="flex items-center justify-between rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
        <span>{{ feedback }}</span>
        <button class="icon-btn" aria-label="关闭提示" @click="feedback = ''"><X class="h-4 w-4" /></button>
      </div>

      <section class="panel">
        <div class="mb-5 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-lg font-semibold text-gray-800">生产报工工作台</h2>
            <p class="mt-1 text-sm text-gray-500">先处理待执行任务，完成动作后再在报工记录中追溯历史流水。</p>
          </div>
          <div class="inline-flex rounded-xl border border-gray-200 bg-gray-50 p-1 text-sm">
            <button
              class="rounded-lg px-4 py-2 transition"
              :class="activeTab === 'tasks' ? 'bg-white font-semibold text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-800'"
              @click="activeTab = 'tasks'"
            >待处理任务 {{ executableTasks.length }}</button>
            <button
              class="rounded-lg px-4 py-2 transition"
              :class="activeTab === 'records' ? 'bg-white font-semibold text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-800'"
              @click="activeTab = 'records'"
            >报工记录 {{ filteredReports.length }}</button>
          </div>
        </div>

        <div v-if="activeTab === 'tasks'">
          <div class="mb-4 rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">
            已派发任务先点击“开始”；进入生产中后再点击“报工”。生产任务页只负责查看和派发，不再承担执行动作。
          </div>
          <div class="overflow-x-auto">
            <table class="data-table min-w-[980px]">
              <thead>
                <tr>
                  <th>任务编号</th>
                  <th>产品名称</th>
                  <th>任务数量</th>
                  <th>已报工</th>
                  <th>剩余数量</th>
                  <th>负责人</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in executableTasks" :key="task.id">
                  <td class="font-medium text-gray-800">{{ task.taskNo }}</td>
                  <td>{{ task.productName || '-' }}</td>
                  <td>{{ task.quantity ?? 0 }}</td>
                  <td>{{ taskReportedMap.get(Number(task.id)) || 0 }}</td>
                  <td>{{ remainingQuantity(task) }}</td>
                  <td>{{ taskResponsibleName(task) }}</td>
                  <td>{{ taskStatusLabel(task.taskStatus) }}</td>
                  <td>
                    <div class="flex flex-wrap gap-3 text-sm">
                      <button v-if="canStartTask(task)" class="text-blue-600 hover:text-blue-800" :disabled="saving" @click="startTask(task)">
                        <Play class="mr-1 inline h-4 w-4" />开始
                      </button>
                      <button v-else-if="canReportTask(task)" class="text-indigo-600 hover:text-indigo-800" @click="openCreateReportForTask(task)">
                        <PackageCheck class="mr-1 inline h-4 w-4" />报工
                      </button>
                      <span v-else class="text-gray-400">等待负责人处理</span>
                    </div>
                  </td>
                </tr>
                <tr v-if="!loading && executableTasks.length === 0"><td colspan="8" class="empty-cell">暂无待执行或待报工任务</td></tr>
                <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载任务…</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-else>
          <div class="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center">
            <label class="relative w-full lg:max-w-md">
              <Search class="search-icon" />
              <input v-model="keyword" class="input pl-10" placeholder="搜索任务编号、产品或报工人" />
            </label>
            <p class="text-sm text-gray-500">共 {{ filteredReports.length }} 条记录</p>
            <p class="text-sm text-gray-500">可报工任务：{{ reportableTasks.length }} 个</p>
          </div>
          <div class="overflow-x-auto">
            <table class="data-table min-w-[1050px]">
              <thead>
                <tr>
                  <th>任务编号</th>
                  <th>产品名称</th>
                  <th>报工人</th>
                  <th>实际完成数量</th>
                  <th>任务状态</th>
                  <th>报工时间</th>
                  <th>生产说明</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="report in filteredReports" :key="report.id">
                  <td class="font-medium text-gray-800">{{ taskNo(report.taskId) }}</td>
                  <td>{{ productName(report.taskId) }}</td>
                  <td>{{ report.reportUserName || '-' }}</td>
                  <td>{{ report.reportQuantity }}</td>
                  <td>{{ taskStatusLabel(taskMap.get(report.taskId)?.taskStatus) }}</td>
                  <td>{{ formatDate(report.reportTime) }}</td>
                  <td class="max-w-xs truncate" :title="report.remark">{{ report.remark || '-' }}</td>
                  <td>
                    <div class="flex flex-wrap gap-3 text-sm">
                      <button class="text-blue-600 hover:text-blue-800" @click="openEditReport(report)">修改</button>
                      <button
                        v-if="canCompleteReport(report)"
                        class="text-emerald-600 hover:text-emerald-800"
                        :disabled="saving"
                        @click="markReportCompleted(report)"
                      >设为完成</button>
                      <span v-else class="text-gray-400">-</span>
                    </div>
                  </td>
                </tr>
                <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载报工记录…</td></tr>
                <tr v-else-if="filteredReports.length === 0"><td colspan="8" class="empty-cell">暂无生产报工记录</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>

    <div v-if="reportVisible" class="modal-mask" @click.self="reportVisible = false">
      <form class="modal-card max-w-2xl" @submit.prevent="submitReport"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-6 flex items-start justify-between">
          <div>
            <p class="text-sm text-gray-500">生产任务报工</p>
            <h2 class="mt-1 text-xl font-bold text-gray-800">{{ formMode === 'create' ? '新增报工' : '修改报工' }}</h2>
          </div>
          <button type="button" class="icon-btn" aria-label="关闭报工弹窗" @click="reportVisible = false"><X class="h-5 w-5" /></button>
        </div>
        <div class="space-y-4">
          <label class="field-label">生产任务
            <select v-model.number="form.taskId" class="input mt-1" required :disabled="formMode === 'edit'" @change="handleTaskChange">
              <option v-for="task in reportableTasks" :key="task.id" :value="task.id">{{ taskLabel(task) }}</option>
              <option v-if="formMode === 'edit' && selectedTask" :value="selectedTask.id">{{ taskLabel(selectedTask) }}</option>
            </select>
          </label>
          <label class="field-label">报工人
            <select v-if="!isWorker" v-model.number="form.reportUserId" class="input mt-1" required>
              <option v-for="worker in workers" :key="worker.id" :value="worker.id">{{ workerLabel(worker) }}</option>
            </select>
            <input v-else class="input mt-1" :value="authStore.currentUser?.realName" disabled />
          </label>
          <label class="field-label">本次完成数量
            <input v-model.number="form.reportQuantity" class="input mt-1" required min="1" type="number" />
          </label>
          <label class="field-label">生产说明
            <textarea v-model="form.remark" class="input mt-1 min-h-24" placeholder="例如：本批次组装完成，待质检"></textarea>
          </label>
          <p class="rounded-lg bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-700">
            报工只记录实际完成数量；合格、不合格和返工结论由质检模块处理。累计报工达到任务数量后，任务会自动进入待质检。
          </p>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="reportVisible = false">取消</button>
          <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '提交中…' : '保存报工' }}</button>
        </div>
      </form>
    </div>
  </MainLayout>
</template>

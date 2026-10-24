<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Building, CheckCircle, Clock, RefreshCw, Settings, Users, Wrench, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { maintenanceApi, productionTaskApi, type Maintenance, type ProductionTask } from '@/api/mes'
import { getUsersByRoleId, type User } from '@/api/user'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const tasks = ref<ProductionTask[]>([])
const maintenances = ref<Maintenance[]>([])
const maintainers = ref<User[]>([])
const workers = ref<User[]>([])
const selectedMaintenance = ref<Maintenance | null>(null)
const selectedTask = ref<ProductionTask | null>(null)
const showAssignForm = ref(false)
const showTaskAssignForm = ref(false)

const assignForm = reactive({
  userId: 0,
})

const taskAssignForm = reactive({
  userId: 0,
})

const taskStatusText: Record<number, string> = {
  0: '待领取',
  1: '已派发',
  2: '生产中',
  3: '待质检',
  4: '已完成',
  5: '已取消',
}

const workshopTasks = computed(() => tasks.value)
const pendingMaintenances = computed(() => maintenances.value.filter((item) => item.status === 'PENDING'))
const processingMaintenances = computed(() => maintenances.value.filter((item) => item.status === 'PROCESSING'))

const stats = computed(() => ({
  people: workers.value.length,
  taskTotal: workshopTasks.value.length,
  taskDone: workshopTasks.value.filter((task) => task.taskStatus === 4).length,
  repairing: processingMaintenances.value.length,
}))

const maintainerWorkloads = computed(() => maintainers.value.map((maintainer) => {
  const assignedOrders = maintenances.value.filter((item) => item.maintenanceUserId === maintainer.id)
  return {
    ...maintainer,
    processingCount: assignedOrders.filter((item) => item.status === 'PROCESSING').length,
    unfinishedCount: assignedOrders.filter((item) => ['PENDING', 'PROCESSING'].includes(item.status || '')).length,
  }
}))

const workerWorkloads = computed(() => workers.value.map((worker) => {
  const assignedTasks = tasks.value.filter((task) => task.assignedUserId === worker.id)
  return {
    ...worker,
    inProgressCount: assignedTasks.filter((task) => task.taskStatus === 2).length,
    unfinishedCount: assignedTasks.filter((task) => [1, 2, 3].includes(Number(task.taskStatus))).length,
  }
}))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [taskData, maintenanceData, maintainerData, workerData] = await Promise.all([
      productionTaskApi.list(),
      maintenanceApi.list(),
      getUsersByRoleId(8),
      getUsersByRoleId(3),
    ])
    tasks.value = taskData
    maintenances.value = maintenanceData
    maintainers.value = maintainerData.filter((user) => user.status !== 0 && user.deleted !== 1)
    workers.value = workerData.filter((user) => user.status !== 0 && user.deleted !== 1)
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function openAssign(item: Maintenance) {
  selectedMaintenance.value = item
  assignForm.userId = item.maintenanceUserId || 0
  showAssignForm.value = true
}

function selectMaintainer(maintainer: User) {
  assignForm.userId = maintainer.id
}

async function assignMaintenance() {
  const maintainer = maintainers.value.find((user) => user.id === assignForm.userId)
  if (!selectedMaintenance.value?.id || !maintainer) {
    message.value = '请选择维修人员'
    return
  }
  saving.value = true
  try {
    await maintenanceApi.assign(selectedMaintenance.value.id, {
      userId: maintainer.id,
      userName: maintainer.realName,
    })
    completeDrafts();showAssignForm.value=false
    message.value = `已将维修单 ${selectedMaintenance.value.maintenanceNo} 指派给 ${maintainer.realName}`
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function openAssignTask(task: ProductionTask) {
  selectedTask.value = task
  taskAssignForm.userId = task.assignedUserId || 0
  showTaskAssignForm.value = true
}

function selectWorker(worker: User) {
  taskAssignForm.userId = worker.id
}

async function assignProductionTask() {
  const worker = workers.value.find((user) => user.id === taskAssignForm.userId)
  if (!selectedTask.value?.id || !worker) {
    message.value = '请选择操作工'
    return
  }
  saving.value = true
  try {
    await productionTaskApi.update(selectedTask.value.id, {
      assignedUserId: worker.id,
      assignedUserName: worker.realName,
      taskStatus: 1,
    })
    completeDrafts();showTaskAssignForm.value = false
    message.value = `已将生产任务 ${selectedTask.value.taskNo} 派发给 ${worker.realName}`
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
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

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

const completeDrafts=useFormDrafts({showAssignForm:{open:showAssignForm,fields:{assignForm},record:()=>selectedMaintenance.value?.id},showTaskAssignForm:{open:showTaskAssignForm,fields:{taskAssignForm},record:()=>selectedTask.value?.id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="rounded-2xl bg-gradient-to-r from-indigo-600 to-indigo-700 p-8 text-white">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div class="flex items-center">
            <div class="mr-6 flex h-20 w-20 items-center justify-center rounded-full bg-white/20">
              <Building class="h-10 w-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">车间主任工作台</h1>
              <p class="mt-2 text-indigo-100">欢迎回来，{{ currentUser?.realName }}！这里处理车间任务和设备维修指派。</p>
              <p class="mt-1 text-sm text-indigo-200">用户编码：{{ currentUser?.userCode }} | 角色：{{ currentUser?.roleName }}</p>
            </div>
          </div>
          <button class="rounded-xl bg-white px-4 py-2 text-sm font-medium text-indigo-700 shadow-sm hover:bg-indigo-50" :disabled="loading" @click="loadAll">
            <RefreshCw class="mr-2 inline h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><Users class="mb-3 h-6 w-6 text-indigo-600" /><p class="stat-label">操作工人数</p><p class="stat-value">{{ stats.people }}</p></div>
        <div class="stat-card"><Clock class="mb-3 h-6 w-6 text-blue-600" /><p class="stat-label">生产任务</p><p class="stat-value">{{ stats.taskTotal }}</p></div>
        <div class="stat-card"><CheckCircle class="mb-3 h-6 w-6 text-green-600" /><p class="stat-label">已完成任务</p><p class="stat-value text-green-600">{{ stats.taskDone }}</p></div>
        <div class="stat-card"><Settings class="mb-3 h-6 w-6 text-orange-600" /><p class="stat-label">维修中设备</p><p class="stat-value text-orange-600">{{ stats.repairing }}</p></div>
      </div>

      <section class="panel">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">待指派设备维修</h2>
          <p class="mt-1 text-sm text-gray-500">操作工提交维修申请后，会作为待指派维修单出现在这里。</p>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table min-w-[900px]">
            <thead><tr><th>维修单号</th><th>设备</th><th>故障描述</th><th>申请备注</th><th>状态</th><th class="text-right">操作</th></tr></thead>
            <tbody>
              <tr v-if="loading"><td colspan="6" class="empty-cell">正在加载维修单...</td></tr>
              <tr v-else-if="pendingMaintenances.length === 0"><td colspan="6" class="empty-cell">暂无待指派维修单</td></tr>
              <tr v-for="item in pendingMaintenances" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td class="max-w-[240px] truncate" :title="item.faultDesc">{{ item.faultDesc || '-' }}</td>
                <td class="max-w-[260px] truncate" :title="item.remark">{{ item.remark || '-' }}</td>
                <td><span :class="maintenanceStatusClass(item.status)">{{ maintenanceStatusText(item.status) }}</span></td>
                <td class="text-right"><button class="link-btn" @click="openAssign(item)"><Wrench class="mr-1 inline h-4 w-4" />指派维修员</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">车间生产任务</h2>
          <p class="mt-1 text-sm text-gray-500">待领取任务由车间主任派发给操作工，操作工端只显示派发给自己的任务。</p>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table min-w-[900px]">
            <thead><tr><th>任务编号</th><th>产品</th><th>数量</th><th>负责人</th><th>状态</th><th class="text-right">操作</th></tr></thead>
            <tbody>
              <tr v-if="workshopTasks.length === 0"><td colspan="6" class="empty-cell">暂无生产任务</td></tr>
              <tr v-for="task in workshopTasks" v-else :key="task.id">
                <td class="font-medium text-gray-900">{{ task.taskNo }}</td>
                <td>{{ task.productName || '-' }}</td>
                <td>{{ task.quantity || 0 }}</td>
                <td>{{ task.assignedUserName || '待领取' }}</td>
                <td><span :class="taskStatusClass(task.taskStatus)">{{ taskStatusText[task.taskStatus ?? 0] }}</span></td>
                <td class="text-right">
                  <button v-if="task.taskStatus === 0" class="link-btn" @click="openAssignTask(task)">派发操作工</button>
                  <span v-else class="text-sm text-gray-400">已派发</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="showTaskAssignForm && selectedTask" class="modal-mask" @click.self="showTaskAssignForm = false">
      <div class="modal-card max-w-3xl">
        <div class="mb-4 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">派发生产任务</p>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedTask.taskNo }}</h2>
          </div>
          <button class="icon-btn" @click="showTaskAssignForm = false"><X class="h-5 w-5" /></button>
        </div>
        <p class="mb-4 rounded-xl bg-gray-50 px-4 py-3 text-sm text-gray-600">
          请选择一名在职操作工。派发后，只有该操作工能在生产任务里看到并处理这张任务。
        </p>
        <div v-if="workerWorkloads.length === 0" class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-400">
          暂无可派发的在职操作工，请先在员工管理中创建或启用“操作工”账号。
        </div>
        <div v-else class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <button
            v-for="worker in workerWorkloads"
            :key="worker.id"
            type="button"
            class="rounded-xl border p-4 text-left transition-colors"
            :class="taskAssignForm.userId === worker.id ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-gray-200 hover:border-blue-300 hover:bg-gray-50'"
            @click="selectWorker(worker)"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-gray-800">{{ worker.realName }}</p>
                <p class="mt-1 text-xs text-gray-500">{{ worker.userCode }} · 操作工</p>
              </div>
              <span v-if="taskAssignForm.userId === worker.id" class="tag-blue">已选择</span>
            </div>
            <div class="mt-4 flex gap-5 text-sm">
              <span><b class="text-blue-600">{{ worker.inProgressCount }}</b> 个生产中</span>
              <span><b class="text-amber-600">{{ worker.unfinishedCount }}</b> 个未完成</span>
            </div>
          </button>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-secondary" @click="showTaskAssignForm = false">取消</button>
          <button class="btn-primary" :disabled="saving || !taskAssignForm.userId" @click="assignProductionTask">{{ saving ? '提交中...' : '确认派发' }}</button>
        </div>
      </div>
    </div>

    <div v-if="showAssignForm && selectedMaintenance" class="modal-mask" @click.self="showAssignForm = false">
      <div class="modal-card max-w-3xl">
        <div class="mb-4 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">指派维修人员</p>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedMaintenance.maintenanceNo }}</h2>
          </div>
          <button class="icon-btn" @click="showAssignForm = false"><X class="h-5 w-5" /></button>
        </div>
        <p class="mb-4 rounded-xl bg-gray-50 px-4 py-3 text-sm text-gray-600">
          请选择一名在职设备维护员。每个人正在维修和未完成数量都来自当前维修单真实数据。
        </p>
        <div v-if="maintainerWorkloads.length === 0" class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-400">
          暂无可指派设备维护员，请先在员工管理中创建设备维护员账号。
        </div>
        <div v-else class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <button
            v-for="maintainer in maintainerWorkloads"
            :key="maintainer.id"
            type="button"
            class="rounded-xl border p-4 text-left transition-colors"
            :class="assignForm.userId === maintainer.id ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-gray-200 hover:border-blue-300 hover:bg-gray-50'"
            @click="selectMaintainer(maintainer)"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-gray-800">{{ maintainer.realName }}</p>
                <p class="mt-1 text-xs text-gray-500">{{ maintainer.userCode }} · 设备维护员</p>
              </div>
              <span v-if="assignForm.userId === maintainer.id" class="tag-blue">已选择</span>
            </div>
            <div class="mt-4 flex gap-5 text-sm">
              <span><b class="text-blue-600">{{ maintainer.processingCount }}</b> 台正在维修</span>
              <span><b class="text-amber-600">{{ maintainer.unfinishedCount }}</b> 张未完成单</span>
            </div>
          </button>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-secondary" @click="showAssignForm = false">取消</button>
          <button class="btn-primary" :disabled="saving || !assignForm.userId" @click="assignMaintenance">{{ saving ? '提交中...' : '确认指派' }}</button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

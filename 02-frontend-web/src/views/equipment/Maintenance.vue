<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { equipmentApi, maintenanceApi, type Equipment, type Maintenance } from '@/api/mes'
import { getUsersByRoleId, type User } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const maintenances = ref<Maintenance[]>([])
const equipments = ref<Equipment[]>([])
/** 可指派的在职设备维护员，来自用户管理的真实员工数据。 */
const maintainers = ref<User[]>([])
const keyword = ref('')
const statusFilter = ref('')
const showCreateForm = ref(false)
const showAssignForm = ref(false)
const showFinishForm = ref(false)
const selectedMaintenance = ref<Maintenance | null>(null)
const currentUser = computed(() => authStore.currentUser)
const isMaintainer = computed(() => currentUser.value?.roleId === 8)

const createForm = reactive<Maintenance>({
  maintenanceNo: '',
  equipmentId: 0,
  maintenanceType: 2,
  faultDesc: '',
  remark: '',
})

const assignForm = reactive({
  userId: 0,
})

const finishForm = reactive({
  maintenanceContent: '',
  result: 'REPAIRED',
  cost: 0,
  scrapEquipment: false,
})

const filteredMaintenances = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return maintenances.value.filter((item) => {
    if (isMaintainer.value && item.maintenanceUserId !== currentUser.value?.id) {
      return false
    }
    const matchWord = !word || item.maintenanceNo?.toLowerCase().includes(word) || item.equipmentName?.toLowerCase().includes(word)
    const matchStatus = !statusFilter.value || item.status === statusFilter.value
    return matchWord && matchStatus
  })
})

const pendingCount = computed(() => maintenances.value.filter((item) => item.status === 'PENDING').length)
const processingCount = computed(() => maintenances.value.filter((item) => item.status === 'PROCESSING').length)
const finishedCount = computed(() => maintenances.value.filter((item) => item.status === 'FINISHED').length)
const totalCost = computed(() => maintenances.value.reduce((sum, item) => sum + Number(item.cost || 0), 0))

/** 每位维护员当前的处理中设备数与未完成维修单数。 */
const maintainerWorkloads = computed(() => maintainers.value.map((maintainer) => {
  const assignedOrders = maintenances.value.filter((item) => item.maintenanceUserId === maintainer.id)
  return {
    ...maintainer,
    processingCount: assignedOrders.filter((item) => item.status === 'PROCESSING').length,
    unfinishedCount: assignedOrders.filter((item) => ['PENDING', 'PROCESSING'].includes(item.status || '')).length,
  }
}))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [maintenanceData, equipmentData, maintainerData] = await Promise.all([
      maintenanceApi.list(),
      equipmentApi.list(),
      getUsersByRoleId(8),
    ])
    maintenances.value = maintenanceData
    equipments.value = equipmentData
    maintainers.value = maintainerData.filter((user) => user.status !== 0 && user.deleted === 0)
  } catch (error: any) {
    message.value = error.message || '维修数据加载失败'
  } finally {
    loading.value = false
  }
}

function statusText(status?: string) {
  const map: Record<string, string> = { PENDING: '待维修', PROCESSING: '维修中', FINISHED: '已完成', CANCELLED: '已取消' }
  return map[status || ''] || status || '-'
}

function statusClass(status?: string) {
  if (status === 'FINISHED') return 'tag-green'
  if (status === 'PROCESSING') return 'tag-blue'
  if (status === 'PENDING') return 'tag-yellow'
  if (status === 'CANCELLED') return 'tag-gray'
  return 'tag-gray'
}

function typeText(type?: number) {
  const map: Record<number, string> = { 1: '保养', 2: '维修', 3: '巡检' }
  return map[type || 0] || '维修'
}

function openCreate() {
  if (isMaintainer.value) {
    message.value = '设备维护员只能处理已指派给自己的维修工单'
    return
  }
  Object.assign(createForm, {
    maintenanceNo: `MT-${Date.now()}`,
    equipmentId: equipments.value[0]?.id || 0,
    maintenanceType: 2,
    faultDesc: '',
    remark: '',
  })
  showCreateForm.value = true
}

async function saveMaintenance() {
  if (!createForm.maintenanceNo || !createForm.equipmentId) {
    message.value = '维修单号和设备不能为空'
    return
  }
  saving.value = true
  try {
    await maintenanceApi.create(createForm)
    message.value = '维修单已创建，设备状态已变为维修中'
    showCreateForm.value = false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '创建维修单失败'
  } finally {
    saving.value = false
  }
}

function openAssign(item: Maintenance) {
  if (isMaintainer.value) {
    message.value = '设备维护员不能指派维修人员'
    return
  }
  selectedMaintenance.value = item
  Object.assign(assignForm, {
    userId: item.maintenanceUserId || 0,
  })
  showAssignForm.value = true
}

/** 选择一名在职维护员，人员姓名由系统员工资料自动带出。 */
function selectMaintainer(maintainer: User) {
  assignForm.userId = maintainer.id
}

async function assignMaintenance() {
  const selectedMaintainer = maintainers.value.find((user) => user.id === assignForm.userId)
  if (!selectedMaintenance.value?.id || !selectedMaintainer) {
    message.value = '维修人员不能为空'
    return
  }
  saving.value = true
  try {
    await maintenanceApi.assign(selectedMaintenance.value.id, {
      userId: selectedMaintainer.id,
      userName: selectedMaintainer.realName,
    })
    message.value = '维修人员已指派'
    showAssignForm.value = false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '指派维修人员失败'
  } finally {
    saving.value = false
  }
}

async function startMaintenance(item: Maintenance) {
  if (!item.id) return
  if (isMaintainer.value && item.maintenanceUserId !== currentUser.value?.id) {
    message.value = '只能开始分配给自己的维修单'
    return
  }
  try {
    await maintenanceApi.start(item.id)
    message.value = '维修已开始'
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '开始维修失败'
  }
}

function openFinish(item: Maintenance) {
  if (isMaintainer.value && item.maintenanceUserId !== currentUser.value?.id) {
    message.value = '只能填报分配给自己的维修单'
    return
  }
  selectedMaintenance.value = item
  Object.assign(finishForm, {
    maintenanceContent: item.maintenanceContent || '',
    result: 'REPAIRED',
    cost: Number(item.cost || 0),
    scrapEquipment: false,
  })
  showFinishForm.value = true
}

async function finishMaintenance() {
  if (!selectedMaintenance.value?.id || !finishForm.maintenanceContent) {
    message.value = '维修内容不能为空'
    return
  }
  saving.value = true
  try {
    await maintenanceApi.finish(selectedMaintenance.value.id, {
      maintenanceContent: finishForm.maintenanceContent,
      result: finishForm.result,
      cost: Number(finishForm.cost || 0),
      scrapEquipment: finishForm.scrapEquipment,
    })
    message.value = '维修已完成'
    showFinishForm.value = false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '完成维修失败'
  } finally {
    saving.value = false
  }
}

async function cancelMaintenance(item: Maintenance) {
  if (isMaintainer.value) {
    message.value = '设备维护员不能取消维修单'
    return
  }
  if (!item.id || !window.confirm(`确认取消维修单「${item.maintenanceNo}」吗？`)) return
  try {
    await maintenanceApi.cancel(item.id)
    message.value = '维修单已取消'
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '取消维修失败'
  }
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function canAssign(item: Maintenance) {
  return !isMaintainer.value && item.status === 'PENDING'
}

function canStart(item: Maintenance) {
  return item.status === 'PENDING' && Boolean(item.maintenanceUserId) && (!isMaintainer.value || item.maintenanceUserId === currentUser.value?.id)
}

function canFinish(item: Maintenance) {
  return ['PENDING', 'PROCESSING'].includes(item.status || '') && (!isMaintainer.value || item.maintenanceUserId === currentUser.value?.id)
}

function canCancel(item: Maintenance) {
  return !isMaintainer.value && ['PENDING', 'PROCESSING'].includes(item.status || '')
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">设备维修</h1>
          <p class="text-gray-500 mt-1">{{ isMaintainer ? '设备维护员只处理分配给自己的维修工单。' : '创建维修单、指派人员、开始和完成维修。' }}</p>
        </div>
        <button v-if="!isMaintainer" class="btn-primary" @click="openCreate"><Plus class="w-4 h-4" />新增维修单</button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="stat-card"><p class="stat-label">待维修</p><p class="stat-value text-yellow-600">{{ pendingCount }}</p></div>
        <div class="stat-card"><p class="stat-label">维修中</p><p class="stat-value text-blue-600">{{ processingCount }}</p></div>
        <div class="stat-card"><p class="stat-label">已完成</p><p class="stat-value text-green-600">{{ finishedCount }}</p></div>
        <div class="stat-card"><p class="stat-label">维修费用</p><p class="stat-value text-gray-900">¥{{ totalCost.toFixed(2) }}</p></div>
      </div>

      <div class="panel">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-5">
          <div class="relative lg:col-span-2">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索维修单号或设备名称" />
          </div>
          <select v-model="statusFilter" class="input">
            <option value="">全部状态</option>
            <option value="PENDING">待维修</option>
            <option value="PROCESSING">维修中</option>
            <option value="FINISHED">已完成</option>
            <option value="CANCELLED">已取消</option>
          </select>
          <button class="btn-secondary" @click="loadAll"><RefreshCw class="w-4 h-4" />刷新</button>
        </div>
        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>维修单号</th>
                <th>设备</th>
                <th>类型</th>
                <th>维修人员</th>
                <th>开始时间</th>
                <th>结束时间</th>
                <th>费用</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="9" class="empty-cell">正在加载维修单...</td></tr>
              <tr v-else-if="filteredMaintenances.length === 0"><td colspan="9" class="empty-cell">暂无维修记录</td></tr>
              <tr v-for="item in filteredMaintenances" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td>{{ typeText(item.maintenanceType) }}</td>
                <td>{{ item.maintenanceUserName || '-' }}</td>
                <td>{{ formatDate(item.startTime) }}</td>
                <td>{{ formatDate(item.endTime) }}</td>
                <td>¥{{ Number(item.cost || 0).toFixed(2) }}</td>
                <td><span :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
                <td class="text-right space-x-2">
                  <button v-if="canAssign(item)" class="link-btn" @click="openAssign(item)">指派</button>
                  <button v-if="canStart(item)" class="link-btn" @click="startMaintenance(item)">开始</button>
                  <button v-else-if="!isMaintainer && item.status === 'PENDING'" class="text-sm text-gray-400" @click="openAssign(item)">请先指派</button>
                  <button v-if="canFinish(item)" class="link-btn" @click="openFinish(item)">完成</button>
                  <button v-if="canCancel(item)" class="danger-link" @click="cancelMaintenance(item)">取消</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showCreateForm" class="modal-mask">
        <div class="modal-card max-w-2xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">新增维修单</h2>
            <button class="icon-btn" @click="showCreateForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">维修单号<input v-model="createForm.maintenanceNo" class="input mt-1" /></label>
            <label class="field-label">设备
              <select v-model.number="createForm.equipmentId" class="input mt-1">
                <option :value="0">请选择设备</option>
                <option v-for="item in equipments" :key="item.id" :value="item.id">{{ item.equipmentName }}（{{ item.status }}）</option>
              </select>
            </label>
            <label class="field-label">维修类型
              <select v-model.number="createForm.maintenanceType" class="input mt-1">
                <option :value="1">保养</option>
                <option :value="2">维修</option>
                <option :value="3">巡检</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">故障描述<textarea v-model="createForm.faultDesc" class="input mt-1 h-20"></textarea></label>
            <label class="field-label md:col-span-2">备注<input v-model="createForm.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showCreateForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveMaintenance">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>

      <div v-if="showAssignForm" class="modal-mask">
        <div class="modal-card max-w-3xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">指派维修人员</h2>
            <button class="icon-btn" @click="showAssignForm = false"><X class="w-5 h-5" /></button>
          </div>
          <p class="mb-4 text-sm text-gray-500">请选择负责维修「{{ selectedMaintenance?.equipmentName || selectedMaintenance?.maintenanceNo }}」的在职设备维护员。人数和工作量均来自当前维修单。</p>
          <div v-if="maintainerWorkloads.length === 0" class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-400">
            暂无可指派的在职设备维护员，请先在员工管理中创建或启用“设备维护员”账号。
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
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showAssignForm = false">取消</button>
            <button class="btn-primary" :disabled="saving || !assignForm.userId" @click="assignMaintenance">{{ saving ? '提交中...' : '确认指派' }}</button>
          </div>
        </div>
      </div>

      <div v-if="showFinishForm" class="modal-mask">
        <div class="modal-card max-w-xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">完成维修</h2>
            <button class="icon-btn" @click="showFinishForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 gap-4">
            <label class="field-label">维修内容<textarea v-model="finishForm.maintenanceContent" class="input mt-1 h-24"></textarea></label>
            <label class="field-label">维修结果
              <select v-model="finishForm.result" class="input mt-1">
                <option value="REPAIRED">已修复</option>
                <option value="SCRAPPED">报废</option>
              </select>
            </label>
            <label class="field-label">维修费用<input v-model.number="finishForm.cost" type="number" min="0" class="input mt-1" /></label>
            <label class="inline-flex items-center gap-2 text-sm text-gray-700">
              <input v-model="finishForm.scrapEquipment" type="checkbox" class="rounded border-gray-300" />
              将设备标记为报废
            </label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showFinishForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="finishMaintenance">{{ saving ? '提交中...' : '确认完成' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

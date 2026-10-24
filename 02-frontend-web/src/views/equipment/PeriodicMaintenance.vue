<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import MaintenancePlans from '@/components/MaintenancePlans.vue'
import MainLayout from '@/components/MainLayout.vue'
import { CalendarClock, CheckCircle, Plus, RefreshCw, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { equipmentApi, maintenanceApi, type Equipment, type Maintenance } from '@/api/mes'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const equipments = ref<Equipment[]>([])
const maintenances = ref<Maintenance[]>([])
const selectedMaintenance = ref<Maintenance | null>(null)
const showCreateForm = ref(false)
const showFinishForm = ref(false)
const reminderMessage = ref('')
const remindedMaintenanceIds = new Set<number>()
let reminderTimer: number | undefined

const createForm = reactive({
  equipmentId: 0,
  planStartTime: '',
  planEndTime: '',
  maintenanceContent: '',
  remark: '',
})

const finishForm = reactive({
  maintenanceContent: '',
  cost: 0,
  remark: '',
})

const myPeriodicMaintenances = computed(() => {
  const userId = currentUser.value?.id
  return maintenances.value.filter((item) => Number(item.maintenanceType) === 1 && (!item.maintenanceUserId || item.maintenanceUserId === userId))
})

const stats = computed(() => ({
  pending: myPeriodicMaintenances.value.filter((item) => item.status === 'PENDING').length,
  processing: myPeriodicMaintenances.value.filter((item) => item.status === 'PROCESSING').length,
  finished: myPeriodicMaintenances.value.filter((item) => item.status === 'FINISHED').length,
  cost: myPeriodicMaintenances.value.reduce((sum, item) => sum + Number(item.cost || 0), 0),
}))

onMounted(() => {
  loadData()
  reminderTimer = window.setInterval(checkMaintenanceTimers, 30000)
})

onBeforeUnmount(() => {
  if (reminderTimer) {
    window.clearInterval(reminderTimer)
  }
})

async function loadData() {
  loading.value = true
  message.value = ''
  try {
    const [equipmentData, maintenanceData] = await Promise.all([
      equipmentApi.list(),
      maintenanceApi.list(),
    ])
    equipments.value = equipmentData
    maintenances.value = maintenanceData
    checkMaintenanceTimers()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  const now = new Date()
  const end = new Date(now.getTime() + 2 * 60 * 60 * 1000)
  Object.assign(createForm, {
    equipmentId: equipments.value.find((item) => item.status !== 'SCRAPPED')?.id || 0,
    planStartTime: toDateTimeLocal(now),
    planEndTime: toDateTimeLocal(end),
    maintenanceContent: '',
    remark: '',
  })
  showCreateForm.value = true
}

async function savePeriodicMaintenance() {
  if (!currentUser.value) return
  if (!createForm.equipmentId || !createForm.maintenanceContent.trim()) {
    message.value = '请选择设备并填写保养内容'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    await maintenanceApi.create({
      maintenanceNo: `PM-${Date.now()}`,
      equipmentId: Number(createForm.equipmentId),
      maintenanceType: 1,
      faultDesc: '定期维护保养',
      maintenanceContent: createForm.maintenanceContent.trim(),
      maintenanceUserId: currentUser.value.id,
      maintenanceUserName: currentUser.value.realName,
      startTime: fromDateTimeLocal(createForm.planStartTime),
      endTime: fromDateTimeLocal(createForm.planEndTime),
      cost: 0,
      result: '',
      remark: createForm.remark.trim() || '设备维护员创建的定期维护保养计划',
    })
    completeDrafts();showCreateForm.value=false
    message.value = '定期维护保养计划已创建'
    await loadData()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function startMaintenance(item: Maintenance) {
  if (!item.id) return
  try {
    await maintenanceApi.start(item.id)
    message.value = `保养单 ${item.maintenanceNo} 已开始`
    await loadData()
  } catch (error) {
    message.value = getErrorMessage(error)
  }
}

function openFinish(item: Maintenance) {
  selectedMaintenance.value = item
  finishForm.maintenanceContent = item.maintenanceContent || ''
  finishForm.cost = Number(item.cost || 0)
  finishForm.remark = item.remark && !item.remark.includes('进行中') ? item.remark : '保养已完成'
  showFinishForm.value = true
}

async function finishMaintenance() {
  if (!selectedMaintenance.value?.id) return
  if (!finishForm.maintenanceContent.trim()) {
    message.value = '请填写保养完成内容'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    await maintenanceApi.finish(selectedMaintenance.value.id, {
      maintenanceContent: finishForm.maintenanceContent.trim(),
      result: 'MAINTAINED',
      cost: Number(finishForm.cost || 0),
      scrapEquipment: false,
      remark: finishForm.remark.trim() || '保养已完成',
    })
    completeDrafts();showFinishForm.value=false
    message.value = `保养单 ${selectedMaintenance.value.maintenanceNo} 已完成`
    await loadData()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待保养',
    PROCESSING: '保养中',
    FINISHED: '已完成',
    CANCELLED: '已取消',
  }
  return map[status || ''] || status || '-'
}

function statusClass(status?: string) {
  if (status === 'FINISHED') return 'tag-green'
  if (status === 'PROCESSING') return 'tag-blue'
  if (status === 'PENDING') return 'tag-yellow'
  return 'tag-gray'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function toDateTimeLocal(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return localDate.toISOString().slice(0, 16)
}

function fromDateTimeLocal(value: string) {
  return value ? new Date(value).toISOString() : undefined
}

function canOperate(item: Maintenance) {
  return ['PENDING', 'PROCESSING'].includes(item.status || '')
}

function checkMaintenanceTimers() {
  const now = Date.now()
  const dueMaintenance = myPeriodicMaintenances.value.find((item) => {
    if (!item.id || item.status !== 'PENDING' || !item.startTime || remindedMaintenanceIds.has(item.id)) return false
    return new Date(item.startTime).getTime() <= now
  })
  if (!dueMaintenance?.id) return
  remindedMaintenanceIds.add(dueMaintenance.id)
  reminderMessage.value = `${dueMaintenance.maintenanceNo}（${dueMaintenance.equipmentName || `设备-${dueMaintenance.equipmentId}`}）已到计划时间，已到期，请手动开始并填报实际保养结果。`
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

const completeDrafts=useFormDrafts({showCreateForm:{open:showCreateForm,fields:{createForm}, record:()=>(createForm as Record<string,any>).id || selectedMaintenance.value?.id},showFinishForm:{open:showFinishForm,fields:{finishForm}, record:()=>selectedMaintenance.value?.id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6"><MaintenancePlans />
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">定期维护保养</h1>
          <p class="mt-1 text-gray-500">为设备创建保养计划，记录保养内容、计划时间和实际费用。</p>
        </div>
        <div class="flex gap-3">
          <button class="btn-secondary" :disabled="loading" @click="loadData">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
          <button class="btn-primary" @click="openCreate">
            <Plus class="h-4 w-4" />
            登记单次保养
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>
      <div v-if="reminderMessage" class="flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        <span>{{ reminderMessage }}</span>
        <button class="text-amber-700 underline" @click="reminderMessage = ''">我知道了</button>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><p class="stat-label">待保养</p><p class="stat-value text-yellow-600">{{ stats.pending }}</p></div>
        <div class="stat-card"><p class="stat-label">保养中</p><p class="stat-value text-blue-600">{{ stats.processing }}</p></div>
        <div class="stat-card"><p class="stat-label">已完成</p><p class="stat-value text-green-600">{{ stats.finished }}</p></div>
        <div class="stat-card"><p class="stat-label">保养费用</p><p class="stat-value text-gray-900">¥{{ stats.cost.toFixed(2) }}</p></div>
      </div>

      <section class="panel">
        <div class="overflow-x-auto">
          <table class="data-table min-w-[960px]">
            <thead>
              <tr>
                <th>保养单号</th>
                <th>设备</th>
                <th>保养内容</th>
                <th>计划/开始时间</th>
                <th>结束时间</th>
                <th>费用</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载保养计划...</td></tr>
              <tr v-else-if="myPeriodicMaintenances.length === 0"><td colspan="8" class="empty-cell">暂无定期维护保养记录</td></tr>
              <tr v-for="item in myPeriodicMaintenances" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td class="max-w-[280px] truncate" :title="item.maintenanceContent">{{ item.maintenanceContent || '-' }}</td>
                <td>{{ formatDate(item.startTime) }}</td>
                <td>{{ formatDate(item.endTime) }}</td>
                <td>¥{{ Number(item.cost || 0).toFixed(2) }}</td>
                <td><span :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
                <td class="text-right">
                  <button v-if="item.status === 'PENDING'" class="link-btn mr-3" @click="startMaintenance(item)">开始保养</button>
                  <button v-if="canOperate(item)" class="link-btn" @click="openFinish(item)">完成填报</button>
                  <span v-if="!canOperate(item)" class="text-sm text-gray-400">已结束</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="showCreateForm" class="modal-mask" data-business-form @click.self="showCreateForm = false">
      <form class="modal-card max-w-2xl" @submit.prevent="savePeriodicMaintenance"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-5 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">定期维护保养</p>
            <h2 class="text-xl font-bold text-gray-900">登记单次保养</h2>
          </div>
          <button type="button" class="icon-btn" @click="showCreateForm = false"><X class="h-5 w-5" /></button>
        </div>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <label class="field-label">
            保养设备
            <select v-model.number="createForm.equipmentId" class="input mt-1">
              <option :value="0">请选择设备</option>
              <option v-for="item in equipments" :key="item.id" :value="item.id" :disabled="item.status === 'SCRAPPED'">
                {{ item.equipmentCode }} - {{ item.equipmentName }}（{{ item.status }}）
              </option>
            </select>
          </label>
          <label class="field-label">
            计划开始时间
            <input v-model="createForm.planStartTime" required type="datetime-local" class="input mt-1" data-date-role="start" data-date-pair="plantime">
          </label>
          <label class="field-label">
            计划结束时间
            <input v-model="createForm.planEndTime" required type="datetime-local" class="input mt-1" data-date-role="end" data-date-pair="plantime">
          </label>
          <label class="field-label md:col-span-2">
            保养内容
            <textarea v-model="createForm.maintenanceContent" required class="input mt-1 h-24" placeholder="例如：清洁传送机构、检查电机皮带、润滑导轨"></textarea>
          </label>
          <label class="field-label md:col-span-2">
            备注
            <input v-model="createForm.remark" class="input mt-1" placeholder="可填写周期、班次或注意事项" />
          </label>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="showCreateForm = false">取消</button>
          <button class="btn-primary" :disabled="saving">
            <CalendarClock class="h-4 w-4" />
            {{ saving ? '保存中...' : '保存保养计划' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="showFinishForm && selectedMaintenance" class="modal-mask" data-business-form @click.self="showFinishForm = false">
      <form class="modal-card max-w-xl" @submit.prevent="finishMaintenance"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-5 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">保养完成填报</p>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedMaintenance.maintenanceNo }}</h2>
          </div>
          <button type="button" class="icon-btn" @click="showFinishForm = false"><X class="h-5 w-5" /></button>
        </div>
        <label class="field-label">
          实际保养内容
          <textarea v-model="finishForm.maintenanceContent" class="input mt-1 h-24"></textarea>
        </label>
        <label class="field-label mt-4">
          保养费用
          <input v-model.number="finishForm.cost" type="number" min="0" step="0.01" class="input mt-1" />
        </label>
        <label class="field-label mt-4">
          完成备注
          <input v-model="finishForm.remark" class="input mt-1" placeholder="例如：保养已完成，设备运行正常" />
        </label>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="showFinishForm = false">取消</button>
          <button class="btn-primary" :disabled="saving">
            <CheckCircle class="h-4 w-4" />
            {{ saving ? '提交中...' : '提交保养结果' }}
          </button>
        </div>
      </form>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { AlertCircle, CheckCircle, Clock, RefreshCw, Settings, Wrench, X } from 'lucide-vue-next'
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
const showFinishForm = ref(false)

const finishForm = reactive({
  maintenanceContent: '',
  result: 'REPAIRED',
  cost: 0,
  scrapEquipment: false,
})

const myMaintenances = computed(() => {
  const userId = currentUser.value?.id
  return maintenances.value.filter((item) => item.maintenanceUserId === userId)
})

const stats = computed(() => ({
  equipmentTotal: equipments.value.length,
  normal: equipments.value.filter((item) => item.status === 'NORMAL').length,
  myPending: myMaintenances.value.filter((item) => item.status === 'PENDING').length,
  myProcessing: myMaintenances.value.filter((item) => item.status === 'PROCESSING').length,
}))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [equipmentData, maintenanceData] = await Promise.all([
      equipmentApi.list(),
      maintenanceApi.list(),
    ])
    equipments.value = equipmentData
    maintenances.value = maintenanceData
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function startMaintenance(item: Maintenance) {
  if (!item.id) return
  try {
    await maintenanceApi.start(item.id)
    message.value = `维修单 ${item.maintenanceNo} 已开始`
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  }
}

function openFinish(item: Maintenance) {
  selectedMaintenance.value = item
  finishForm.maintenanceContent = item.maintenanceContent || ''
  finishForm.result = item.result || 'REPAIRED'
  finishForm.cost = Number(item.cost || 0)
  finishForm.scrapEquipment = item.result === 'SCRAPPED'
  showFinishForm.value = true
}

async function finishMaintenance() {
  if (!selectedMaintenance.value?.id) return
  if (!finishForm.maintenanceContent.trim()) {
    message.value = '请填写维修内容'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    await maintenanceApi.finish(selectedMaintenance.value.id, {
      maintenanceContent: finishForm.maintenanceContent.trim(),
      result: finishForm.result,
      cost: Number(finishForm.cost || 0),
      scrapEquipment: finishForm.scrapEquipment,
    })
    completeDrafts();showFinishForm.value=false
    message.value = `维修单 ${selectedMaintenance.value.maintenanceNo} 已完成`
    await loadAll()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
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
  return 'tag-gray'
}

function equipmentStatusText(status?: string) {
  const map: Record<string, string> = { NORMAL: '正常', FAULT: '故障', MAINTAINING: '维修中', SCRAPPED: '报废' }
  return map[status || ''] || status || '-'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

const completeDrafts=useFormDrafts({showFinishForm:{open:showFinishForm,fields:{finishForm}, record:()=>selectedMaintenance.value?.id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="rounded-2xl bg-gradient-to-r from-pink-600 to-pink-700 p-8 text-white">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div class="flex items-center">
            <div class="mr-6 flex h-20 w-20 items-center justify-center rounded-full bg-white/20">
              <Wrench class="h-10 w-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">设备维护员工作台</h1>
              <p class="mt-2 text-pink-100">欢迎回来，{{ currentUser?.realName }}！这里处理分配给你的维修任务。</p>
              <p class="mt-1 text-sm text-pink-200">用户编码：{{ currentUser?.userCode }} | 角色：{{ currentUser?.roleName }}</p>
            </div>
          </div>
          <button class="rounded-xl bg-white px-4 py-2 text-sm font-medium text-pink-700 shadow-sm hover:bg-pink-50" :disabled="loading" @click="loadAll">
            <RefreshCw class="mr-2 inline h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><Wrench class="mb-3 h-6 w-6 text-pink-600" /><p class="stat-label">设备总数</p><p class="stat-value">{{ stats.equipmentTotal }}</p></div>
        <div class="stat-card"><CheckCircle class="mb-3 h-6 w-6 text-green-600" /><p class="stat-label">正常设备</p><p class="stat-value text-green-600">{{ stats.normal }}</p></div>
        <div class="stat-card"><AlertCircle class="mb-3 h-6 w-6 text-yellow-600" /><p class="stat-label">我的待维修</p><p class="stat-value text-yellow-600">{{ stats.myPending }}</p></div>
        <div class="stat-card"><Settings class="mb-3 h-6 w-6 text-blue-600" /><p class="stat-label">我的维修中</p><p class="stat-value text-blue-600">{{ stats.myProcessing }}</p></div>
      </div>

      <section class="panel">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">我的维修工单</h2>
          <p class="mt-1 text-sm text-gray-500">车间主任指派给你的工单会出现在这里。维修完成后填写内容、费用和结果。</p>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table min-w-[960px]">
            <thead><tr><th>维修单号</th><th>设备</th><th>故障描述</th><th>开始时间</th><th>结束时间</th><th>费用</th><th>状态</th><th class="text-right">操作</th></tr></thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载维修工单...</td></tr>
              <tr v-else-if="myMaintenances.length === 0"><td colspan="8" class="empty-cell">暂无分配给你的维修工单</td></tr>
              <tr v-for="item in myMaintenances" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td class="max-w-[240px] truncate" :title="item.faultDesc">{{ item.faultDesc || '-' }}</td>
                <td>{{ formatDate(item.startTime) }}</td>
                <td>{{ formatDate(item.endTime) }}</td>
                <td>¥{{ Number(item.cost || 0).toFixed(2) }}</td>
                <td><span :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
                <td class="text-right">
                  <button v-if="item.status === 'PENDING'" class="link-btn mr-3" @click="startMaintenance(item)"><Clock class="mr-1 inline h-4 w-4" />开始维修</button>
                  <button v-if="['PENDING','PROCESSING'].includes(item.status || '')" class="link-btn" @click="openFinish(item)">填报完成</button>
                  <span v-if="!['PENDING','PROCESSING'].includes(item.status || '')" class="text-sm text-gray-400">已结束</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">设备状态概览</h2>
          <p class="mt-1 text-sm text-gray-500">只用于辅助维修判断，不在这里新增或删除设备。</p>
        </div>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          <div v-for="item in equipments" :key="item.id" class="rounded-xl border border-gray-100 bg-gray-50 p-4">
            <div class="flex items-start justify-between">
              <div>
                <p class="font-semibold text-gray-900">{{ item.equipmentName }}</p>
                <p class="mt-1 text-xs text-gray-500">{{ item.equipmentCode }} · {{ item.location || '-' }}</p>
              </div>
              <span class="tag-blue">{{ equipmentStatusText(item.status) }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="showFinishForm && selectedMaintenance" class="modal-mask" data-business-form @click.self="showFinishForm = false">
      <form class="modal-card max-w-xl" @submit.prevent="finishMaintenance"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-5 flex items-center justify-between">
          <div><p class="text-sm text-gray-500">维修完成填报</p><h2 class="text-xl font-bold text-gray-900">{{ selectedMaintenance.maintenanceNo }}</h2></div>
          <button type="button" class="icon-btn" @click="showFinishForm = false"><X class="h-5 w-5" /></button>
        </div>
        <label class="field-label">维修内容<textarea v-model="finishForm.maintenanceContent" class="input mt-1 h-24" placeholder="填写维修过程、更换零件、处理结果等"></textarea></label>
        <label class="field-label mt-4">维修结果
          <select v-model="finishForm.result" class="input mt-1">
            <option value="REPAIRED">已修复</option>
            <option value="SCRAPPED">报废</option>
          </select>
        </label>
        <label class="field-label mt-4">维修费用<input v-model.number="finishForm.cost" type="number" min="0" step="0.01" class="input mt-1" /></label>
        <label class="mt-4 inline-flex items-center gap-2 text-sm text-gray-700">
          <input v-model="finishForm.scrapEquipment" type="checkbox" class="rounded border-gray-300" />
          将设备标记为报废
        </label>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="showFinishForm = false">取消</button>
          <button class="btn-primary" :disabled="saving">{{ saving ? '提交中...' : '提交维修结果' }}</button>
        </div>
      </form>
    </div>
  </MainLayout>
</template>

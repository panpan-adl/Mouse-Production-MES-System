<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Clock, RefreshCw, Wrench, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { maintenanceApi, type Maintenance } from '@/api/mes'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const maintenances = ref<Maintenance[]>([])
const selectedMaintenance = ref<Maintenance | null>(null)
const showFinishForm = ref(false)

const finishForm = reactive({
  maintenanceContent: '',
  result: 'REPAIRED',
  cost: 0,
  scrapEquipment: false,
  remark: '',
})

const myRepairTasks = computed(() => {
  const userId = currentUser.value?.id
  return maintenances.value.filter((item) => item.maintenanceUserId === userId && Number(item.maintenanceType || 2) === 2)
})

const stats = computed(() => ({
  pending: myRepairTasks.value.filter((item) => item.status === 'PENDING').length,
  processing: myRepairTasks.value.filter((item) => item.status === 'PROCESSING').length,
  finished: myRepairTasks.value.filter((item) => item.status === 'FINISHED').length,
  totalCost: myRepairTasks.value.reduce((sum, item) => sum + Number(item.cost || 0), 0),
}))

onMounted(loadData)

async function loadData() {
  loading.value = true
  message.value = ''
  try {
    maintenances.value = await maintenanceApi.list()
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
    message.value = `维修任务 ${item.maintenanceNo} 已开始`
    await loadData()
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
  finishForm.remark = item.remark && !item.remark.includes('进行中') ? item.remark : '维修已完成'
  showFinishForm.value = true
}

async function finishMaintenance() {
  if (!selectedMaintenance.value?.id) return
  if (!finishForm.maintenanceContent.trim()) {
    message.value = '请填写维修内容'
    return
  }
  saving.value = true
  try {
    await maintenanceApi.finish(selectedMaintenance.value.id, {
      maintenanceContent: finishForm.maintenanceContent.trim(),
      result: finishForm.result,
      cost: Number(finishForm.cost || 0),
      scrapEquipment: finishForm.scrapEquipment,
      remark: finishForm.remark.trim() || '维修已完成',
    })
    showFinishForm.value = false
    message.value = `维修任务 ${selectedMaintenance.value.maintenanceNo} 已完成`
    await loadData()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待维修',
    PROCESSING: '维修中',
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

function canStart(item: Maintenance) {
  return item.status === 'PENDING'
}

function canFinish(item: Maintenance) {
  return ['PENDING', 'PROCESSING'].includes(item.status || '')
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">维修执行与记录</h1>
          <p class="mt-1 text-gray-500">接收车间主任指派给你的设备维修任务，开始维修并填报维修结果。</p>
        </div>
        <button class="btn-secondary" :disabled="loading" @click="loadData">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          刷新
        </button>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><p class="stat-label">待维修</p><p class="stat-value text-yellow-600">{{ stats.pending }}</p></div>
        <div class="stat-card"><p class="stat-label">维修中</p><p class="stat-value text-blue-600">{{ stats.processing }}</p></div>
        <div class="stat-card"><p class="stat-label">已完成</p><p class="stat-value text-green-600">{{ stats.finished }}</p></div>
        <div class="stat-card"><p class="stat-label">维修费用</p><p class="stat-value text-gray-900">¥{{ stats.totalCost.toFixed(2) }}</p></div>
      </div>

      <section class="panel">
        <div class="overflow-x-auto">
          <table class="data-table min-w-[980px]">
            <thead>
              <tr>
                <th>维修单号</th>
                <th>设备</th>
                <th>故障描述</th>
                <th>来源说明</th>
                <th>开始时间</th>
                <th>结束时间</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载维修任务...</td></tr>
              <tr v-else-if="myRepairTasks.length === 0"><td colspan="8" class="empty-cell">暂无车间主任指派给你的维修任务</td></tr>
              <tr v-for="item in myRepairTasks" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td class="max-w-[240px] truncate" :title="item.faultDesc">{{ item.faultDesc || '-' }}</td>
                <td class="max-w-[260px] truncate" :title="item.remark">{{ item.remark || '-' }}</td>
                <td>{{ formatDate(item.startTime) }}</td>
                <td>{{ formatDate(item.endTime) }}</td>
                <td><span :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
                <td class="text-right">
                  <button v-if="canStart(item)" class="link-btn mr-3" @click="startMaintenance(item)">
                    <Clock class="mr-1 inline h-4 w-4" />
                    开始维修
                  </button>
                  <button v-if="canFinish(item)" class="link-btn" @click="openFinish(item)">填报完成</button>
                  <span v-if="!canFinish(item)" class="text-sm text-gray-400">已结束</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="showFinishForm && selectedMaintenance" class="modal-mask" @click.self="showFinishForm = false">
      <form class="modal-card max-w-xl" @submit.prevent="finishMaintenance">
        <div class="mb-5 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">维修完成填报</p>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedMaintenance.maintenanceNo }}</h2>
          </div>
          <button type="button" class="icon-btn" @click="showFinishForm = false"><X class="h-5 w-5" /></button>
        </div>
        <label class="field-label">
          维修内容
          <textarea v-model="finishForm.maintenanceContent" class="input mt-1 h-24" placeholder="填写维修过程、更换零件、处理结果等"></textarea>
        </label>
        <label class="field-label mt-4">
          维修结果
          <select v-model="finishForm.result" class="input mt-1">
            <option value="REPAIRED">已修复</option>
            <option value="SCRAPPED">报废</option>
          </select>
        </label>
        <label class="field-label mt-4">
          维修费用
          <input v-model.number="finishForm.cost" type="number" min="0" step="0.01" class="input mt-1" />
        </label>
        <label class="field-label mt-4">
          完成备注
          <input v-model="finishForm.remark" class="input mt-1" placeholder="例如：维修已完成，设备恢复正常" />
        </label>
        <label class="mt-4 inline-flex items-center gap-2 text-sm text-gray-700">
          <input v-model="finishForm.scrapEquipment" type="checkbox" class="rounded border-gray-300" />
          将设备标记为报废
        </label>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="showFinishForm = false">取消</button>
          <button class="btn-primary" :disabled="saving">
            <Wrench class="h-4 w-4" />
            {{ saving ? '提交中...' : '提交维修结果' }}
          </button>
        </div>
      </form>
    </div>
  </MainLayout>
</template>

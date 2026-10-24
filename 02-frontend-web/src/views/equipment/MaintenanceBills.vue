<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { FileText, Plus, RefreshCw, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { equipmentApi, maintenanceApi, type Equipment, type Maintenance } from '@/api/mes'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const equipments = ref<Equipment[]>([])
const maintenances = ref<Maintenance[]>([])
const showCreateForm = ref(false)
const selectedBill = ref<Maintenance | null>(null)

const billForm = reactive({
  equipmentId: 0,
  maintenanceType: 2,
  startTime: '',
  endTime: '',
  maintenanceContent: '',
  cost: 0,
  result: 'REPAIRED',
  remark: '',
})

const billRecords = computed(() => {
  const userId = currentUser.value?.id
  return maintenances.value.filter((item) => {
    const isFinished = item.status === 'FINISHED'
    const isOwn = !item.maintenanceUserId || item.maintenanceUserId === userId
    return isFinished && isOwn
  })
})

const stats = computed(() => ({
  count: billRecords.value.length,
  repairCost: billRecords.value.filter((item) => Number(item.maintenanceType) === 2).reduce((sum, item) => sum + Number(item.cost || 0), 0),
  upkeepCost: billRecords.value.filter((item) => Number(item.maintenanceType) === 1).reduce((sum, item) => sum + Number(item.cost || 0), 0),
  totalCost: billRecords.value.reduce((sum, item) => sum + Number(item.cost || 0), 0),
}))

onMounted(loadData)

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
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  const now = new Date()
  const before = new Date(now.getTime() - 60 * 60 * 1000)
  selectedBill.value = null
  Object.assign(billForm, {
    equipmentId: equipments.value.find((item) => item.status !== 'SCRAPPED')?.id || 0,
    maintenanceType: 2,
    startTime: toDateTimeLocal(before),
    endTime: toDateTimeLocal(now),
    maintenanceContent: '',
    cost: 0,
    result: 'REPAIRED',
    remark: '',
  })
  showCreateForm.value = true
}

function openEdit(item: Maintenance) {
  const normalizedRemark = billRemark(item)
  selectedBill.value = item
  Object.assign(billForm, {
    equipmentId: item.equipmentId || 0,
    maintenanceType: Number(item.maintenanceType || 2),
    startTime: item.startTime ? toDateTimeLocal(new Date(item.startTime)) : '',
    endTime: item.endTime ? toDateTimeLocal(new Date(item.endTime)) : '',
    maintenanceContent: item.maintenanceContent || '',
    cost: Number(item.cost || 0),
    result: item.result || 'REPAIRED',
    remark: normalizedRemark === '-' ? '' : normalizedRemark,
  })
  showCreateForm.value = true
}

async function saveBill() {
  if (!currentUser.value) return
  if (!billForm.equipmentId || !billForm.maintenanceContent.trim()) {
    message.value = '请选择设备并填写维修/保养内容'
    return
  }
  saving.value = true
  try {
    const startTime = fromDateTimeLocal(billForm.startTime)
    const endTime = fromDateTimeLocal(billForm.endTime)
    const equipment = equipments.value.find((item) => item.id === Number(billForm.equipmentId))
    const finalRemark = billForm.remark.trim() || `${typeText(Number(billForm.maintenanceType))}已完成`
    if (selectedBill.value?.id) {
      await maintenanceApi.update(selectedBill.value.id, {
        ...selectedBill.value,
        equipmentId: Number(billForm.equipmentId),
        equipmentName: equipment?.equipmentName || selectedBill.value.equipmentName,
        maintenanceType: Number(billForm.maintenanceType),
        faultDesc: typeText(Number(billForm.maintenanceType)),
        maintenanceContent: billForm.maintenanceContent.trim(),
        maintenanceUserId: currentUser.value.id,
        maintenanceUserName: currentUser.value.realName,
        startTime,
        endTime,
        cost: Number(billForm.cost || 0),
        result: billForm.result,
        status: 'FINISHED',
        remark: finalRemark,
      })
      showCreateForm.value = false
      selectedBill.value = null
      message.value = '维护账单已修改'
      await loadData()
      return
    }
    const created = await maintenanceApi.create({
      maintenanceNo: `MB-${Date.now()}`,
      equipmentId: Number(billForm.equipmentId),
      maintenanceType: Number(billForm.maintenanceType),
      faultDesc: typeText(Number(billForm.maintenanceType)),
      maintenanceContent: billForm.maintenanceContent.trim(),
      maintenanceUserId: currentUser.value.id,
      maintenanceUserName: currentUser.value.realName,
      startTime,
      endTime,
      cost: Number(billForm.cost || 0),
      result: billForm.result,
      remark: finalRemark,
    })
    if (created.id) {
      const finished = await maintenanceApi.finish(created.id, {
        maintenanceContent: billForm.maintenanceContent.trim(),
        result: billForm.result,
        cost: Number(billForm.cost || 0),
        scrapEquipment: billForm.result === 'SCRAPPED',
        remark: finalRemark,
      })
      await maintenanceApi.update(created.id, {
        ...finished,
        startTime,
        endTime,
        maintenanceContent: billForm.maintenanceContent.trim(),
        cost: Number(billForm.cost || 0),
        result: billForm.result,
        status: 'FINISHED',
        remark: finalRemark,
      })
    }
    showCreateForm.value = false
    message.value = '维护账单已新增'
    await loadData()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function typeText(type?: number) {
  const map: Record<number, string> = {
    1: '保养',
    2: '维修',
    3: '巡检',
  }
  return map[Number(type)] || '维修'
}

function resultText(result?: string) {
  const map: Record<string, string> = {
    REPAIRED: '已修复',
    MAINTAINED: '已保养',
    SCRAPPED: '报废',
  }
  return map[result || ''] || result || '-'
}

function billRemark(item: Maintenance) {
  if (item.status === 'FINISHED' && item.remark?.includes('进行中')) {
    return `${typeText(item.maintenanceType)}已完成`
  }
  return item.remark || '-'
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

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">维护账单</h1>
          <p class="mt-1 text-gray-500">记录哪台设备、什么时间段、做了什么维修/保养，以及产生的费用。</p>
        </div>
        <div class="flex gap-3">
          <button class="btn-secondary" :disabled="loading" @click="loadData">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
          <button class="btn-primary" @click="openCreate">
            <Plus class="h-4 w-4" />
            新增账单
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><p class="stat-label">账单数量</p><p class="stat-value">{{ stats.count }}</p></div>
        <div class="stat-card"><p class="stat-label">维修费用</p><p class="stat-value text-blue-600">¥{{ stats.repairCost.toFixed(2) }}</p></div>
        <div class="stat-card"><p class="stat-label">保养费用</p><p class="stat-value text-green-600">¥{{ stats.upkeepCost.toFixed(2) }}</p></div>
        <div class="stat-card"><p class="stat-label">合计费用</p><p class="stat-value text-gray-900">¥{{ stats.totalCost.toFixed(2) }}</p></div>
      </div>

      <section class="panel">
        <div class="overflow-x-auto">
          <table class="data-table min-w-[1050px]">
            <thead>
              <tr>
                <th>账单/单号</th>
                <th>设备</th>
                <th>类型</th>
                <th>时间段</th>
                <th>内容</th>
                <th>结果</th>
                <th>费用</th>
                <th>备注</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="9" class="empty-cell">正在加载维护账单...</td></tr>
              <tr v-else-if="billRecords.length === 0"><td colspan="9" class="empty-cell">暂无维护账单</td></tr>
              <tr v-for="item in billRecords" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td>{{ typeText(item.maintenanceType) }}</td>
                <td>{{ formatDate(item.startTime) }} ～ {{ formatDate(item.endTime) }}</td>
                <td class="max-w-[260px] truncate" :title="item.maintenanceContent">{{ item.maintenanceContent || '-' }}</td>
                <td>{{ resultText(item.result) }}</td>
                <td>¥{{ Number(item.cost || 0).toFixed(2) }}</td>
                <td class="max-w-[240px] truncate" :title="billRemark(item)">{{ billRemark(item) }}</td>
                <td class="text-right"><button class="link-btn" @click="openEdit(item)">修改</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="showCreateForm" class="modal-mask" @click.self="showCreateForm = false">
      <form class="modal-card max-w-2xl" @submit.prevent="saveBill">
        <div class="mb-5 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">维护账单</p>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedBill ? '修改账单' : '新增账单' }}</h2>
          </div>
          <button type="button" class="icon-btn" @click="showCreateForm = false"><X class="h-5 w-5" /></button>
        </div>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <label class="field-label">
            设备
            <select v-model.number="billForm.equipmentId" class="input mt-1">
              <option :value="0">请选择设备</option>
              <option v-for="item in equipments" :key="item.id" :value="item.id" :disabled="item.status === 'SCRAPPED'">
                {{ item.equipmentCode }} - {{ item.equipmentName }}
              </option>
            </select>
          </label>
          <label class="field-label">
            类型
            <select v-model.number="billForm.maintenanceType" class="input mt-1">
              <option :value="2">维修</option>
              <option :value="1">保养</option>
              <option :value="3">巡检</option>
            </select>
          </label>
          <label class="field-label">
            开始时间
            <input v-model="billForm.startTime" type="datetime-local" class="input mt-1" />
          </label>
          <label class="field-label">
            结束时间
            <input v-model="billForm.endTime" type="datetime-local" class="input mt-1" />
          </label>
          <label class="field-label md:col-span-2">
            维修/保养内容
            <textarea v-model="billForm.maintenanceContent" class="input mt-1 h-24" placeholder="写清楚维护内容，例如更换微动测试夹具、润滑导轨、校准传送带等"></textarea>
          </label>
          <label class="field-label">
            结果
            <select v-model="billForm.result" class="input mt-1">
              <option value="REPAIRED">已修复</option>
              <option value="MAINTAINED">已保养</option>
              <option value="SCRAPPED">报废</option>
            </select>
          </label>
          <label class="field-label">
            费用
            <input v-model.number="billForm.cost" type="number" min="0" step="0.01" class="input mt-1" />
          </label>
          <label class="field-label md:col-span-2">
            备注
            <input v-model="billForm.remark" class="input mt-1" placeholder="可填写备件、供应商或说明" />
          </label>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="showCreateForm = false">取消</button>
          <button class="btn-primary" :disabled="saving">
            <FileText class="h-4 w-4" />
            {{ saving ? '保存中...' : selectedBill ? '保存修改' : '保存账单' }}
          </button>
        </div>
      </form>
    </div>
  </MainLayout>
</template>

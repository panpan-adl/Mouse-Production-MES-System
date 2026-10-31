<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { AlertTriangle, RefreshCw, Send, Wrench, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { equipmentApi, maintenanceApi, type Equipment, type Maintenance } from '@/api/mes'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const equipments = ref<Equipment[]>([])
const maintenances = ref<Maintenance[]>([])
const selectedEquipment = ref<Equipment | null>(null)
const showRepairForm = ref(false)

const repairForm = reactive({
  faultDesc: '',
  remark: '',
})

const activeStatuses = ['PENDING', 'PROCESSING']

const statusText: Record<string, string> = {
  NORMAL: '正常',
  FAULT: '故障', REPAIR_REQUESTED:'待维修', REPAIRING:'维修中', REPAIRED:'已修复',
  MAINTAINING: '维修中',
  SCRAPPED: '报废',
}

const maintenanceStatusText: Record<string, string> = {
  PENDING: '待车间主任指派',
  PROCESSING: '维修中',
  FINISHED: '已完成',
  CANCELLED: '已取消',
}

const activeMaintenanceByEquipment = computed(() => {
  const map = new Map<number, Maintenance>()
  maintenances.value.forEach((item) => {
    if (item.equipmentId && activeStatuses.includes(item.status || '')) {
      map.set(item.equipmentId, item)
    }
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

const stats = computed(() => ({
  total: equipments.value.length,
  normal: equipments.value.filter((item) => item.status === 'NORMAL').length,
  abnormal: equipments.value.filter((item) => ['FAULT', 'REPAIR_REQUESTED', 'MAINTAINING', 'REPAIRING'].includes(item.status || '')).length,
  myPending: myRepairRequests.value.filter((item) => activeStatuses.includes(item.status || '')).length,
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

function openRepairForm(equipment: Equipment) {
  if (!equipment.id) return
  if (equipment.status === 'SCRAPPED') {
    message.value = '报废设备不能再提交维修申请'
    showBusinessFormError(message.value)
    return
  }
  if (activeMaintenanceByEquipment.value.has(equipment.id)) {
    message.value = '该设备已经存在未完成维修单，不能重复上报'
    showBusinessFormError(message.value)
    return
  }
  selectedEquipment.value = equipment
  repairForm.faultDesc = ''
  repairForm.remark = ''
  showRepairForm.value = true
}

async function submitRepairRequest() {
  if (!selectedEquipment.value?.id || !currentUser.value) return
  if (!repairForm.faultDesc.trim()) {
    message.value = '请填写设备故障描述'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    await maintenanceApi.create({
      maintenanceNo: `MR-${Date.now()}`,
      equipmentId: selectedEquipment.value.id,
      equipmentName: selectedEquipment.value.equipmentName,
      maintenanceType: 2,
      faultDesc: repairForm.faultDesc.trim(),
      remark: `现场报修；申请人：${currentUser.value.realName}（${currentUser.value.userCode}）；设备：${selectedEquipment.value.equipmentCode}；${repairForm.remark.trim() || '无补充说明'}`,
    })
    completeDrafts();showRepairForm.value=false
    message.value = '设备报修已提交，车间主任工作台会出现待指派维修单'
    await loadData()
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function equipmentStatusClass(status?: string) {
  if (status === 'NORMAL') return 'tag-green'
  if (['FAULT','REPAIR_REQUESTED'].includes(status||'')) return 'tag-red'
  if (['MAINTAINING','REPAIRING'].includes(status||'')) return 'tag-yellow'
  return 'tag-gray'
}

function maintenanceStatusClass(status?: string) {
  if (status === 'FINISHED') return 'tag-green'
  if (status === 'PROCESSING') return 'tag-blue'
  if (status === 'PENDING') return 'tag-yellow'
  return 'tag-gray'
}

function activeMaintenanceText(equipment: Equipment) {
  if (!equipment.id) return '-'
  const maintenance = activeMaintenanceByEquipment.value.get(equipment.id)
  if (!maintenance) return '无未完成维修单'
  return `${maintenance.maintenanceNo}｜${maintenanceStatusText[maintenance.status || ''] || maintenance.status || '-'}`
}

function canReport(equipment: Equipment) {
  return Boolean(equipment.id && equipment.status !== 'SCRAPPED' && !activeMaintenanceByEquipment.value.has(equipment.id))
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

const completeDrafts=useFormDrafts({showRepairForm:{open:showRepairForm,fields:{repairForm}, record:()=>selectedEquipment.value?.id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 p-8 text-white">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div class="flex items-center">
            <div class="mr-6 flex h-20 w-20 items-center justify-center rounded-full bg-white/20">
              <Wrench class="h-10 w-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">设备报修</h1>
              <p class="mt-2 text-blue-100">查看现场设备状态，发现故障后上报给车间主任，由车间主任指派维修人员。</p>
              <p class="mt-1 text-sm text-blue-200">当前申请人：{{ currentUser?.realName }}｜工号：{{ currentUser?.userCode }}</p>
            </div>
          </div>
          <button class="rounded-xl bg-white px-4 py-2 text-sm font-medium text-blue-700 shadow-sm hover:bg-blue-50" :disabled="loading" @click="loadData">
            <RefreshCw class="mr-2 inline h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card"><p class="stat-label">设备总数</p><p class="stat-value">{{ stats.total }}</p></div>
        <div class="stat-card"><p class="stat-label">正常设备</p><p class="stat-value text-green-600">{{ stats.normal }}</p></div>
        <div class="stat-card"><p class="stat-label">故障/维修中</p><p class="stat-value text-red-600">{{ stats.abnormal }}</p></div>
        <div class="stat-card"><p class="stat-label">我的未完成报修</p><p class="stat-value text-yellow-600">{{ stats.myPending }}</p></div>
      </div>

      <section class="panel">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">现场设备状态</h2>
          <p class="mt-1 text-sm text-gray-500">操作工只能上报故障，不直接修改设备状态；设备维修流程由车间主任和维修员继续处理。</p>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table min-w-[980px]">
            <thead>
              <tr>
                <th>设备编号</th>
                <th>设备名称</th>
                <th>类型/型号</th>
                <th>位置</th>
                <th>当前状态</th>
                <th>未完成维修单</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="7" class="empty-cell">正在加载设备状态...</td></tr>
              <tr v-else-if="equipments.length === 0"><td colspan="7" class="empty-cell">暂无设备数据</td></tr>
              <tr v-for="equipment in equipments" v-else :key="equipment.id">
                <td class="font-medium text-gray-900">{{ equipment.equipmentCode }}</td>
                <td>{{ equipment.equipmentName }}</td>
                <td>{{ equipment.equipmentType || '-' }} / {{ equipment.model || '-' }}</td>
                <td>{{ equipment.location || '-' }}</td>
                <td><span :class="equipmentStatusClass(equipment.status)">{{ statusText[equipment.status || ''] || equipment.status || '-' }}</span></td>
                <td class="max-w-[260px] truncate" :title="activeMaintenanceText(equipment)">{{ activeMaintenanceText(equipment) }}</td>
                <td class="text-right">
                  <button v-if="canReport(equipment)" class="link-btn" @click="openRepairForm(equipment)">
                    <AlertTriangle class="mr-1 inline h-4 w-4" />
                    上报报修
                  </button>
                  <span v-else class="text-sm text-gray-400">{{ equipment.status === 'SCRAPPED' ? '报废不可报修' : '已有未完成维修单' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-gray-900">我的报修记录</h2>
          <p class="mt-1 text-sm text-gray-500">这里展示你提交过的设备报修，以及车间主任是否已经指派维修员。</p>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table min-w-[860px]">
            <thead>
              <tr>
                <th>维修单号</th>
                <th>设备</th>
                <th>故障描述</th>
                <th>维修员</th>
                <th>状态</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="myRepairRequests.length === 0"><td colspan="6" class="empty-cell">暂无本人提交的报修记录</td></tr>
              <tr v-for="item in myRepairRequests" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.maintenanceNo }}</td>
                <td>{{ item.equipmentName || `设备-${item.equipmentId}` }}</td>
                <td class="max-w-[260px] truncate" :title="item.faultDesc">{{ item.faultDesc || '-' }}</td>
                <td>{{ item.maintenanceUserName || '待车间主任指派' }}</td>
                <td><span :class="maintenanceStatusClass(item.status)">{{ maintenanceStatusText[item.status || ''] || item.status || '-' }}</span></td>
                <td class="max-w-[280px] truncate" :title="item.remark">{{ item.remark || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div v-if="showRepairForm && selectedEquipment" class="modal-mask" data-business-form @click.self="showRepairForm = false">
      <form class="modal-card max-w-xl" @submit.prevent="submitRepairRequest"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <div class="mb-5 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">设备报修上报</p>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedEquipment.equipmentCode }} - {{ selectedEquipment.equipmentName }}</h2>
          </div>
          <button type="button" class="icon-btn" @click="showRepairForm = false"><X class="h-5 w-5" /></button>
        </div>
        <div class="mb-5 rounded-xl bg-gray-50 p-4 text-sm text-gray-600">
          <p>当前状态：<span :class="equipmentStatusClass(selectedEquipment.status)">{{ statusText[selectedEquipment.status || ''] || selectedEquipment.status || '-' }}</span></p>
          <p class="mt-2">所在位置：{{ selectedEquipment.location || '-' }}</p>
        </div>
        <label class="field-label">
          故障描述
          <textarea v-model="repairForm.faultDesc" class="input mt-1 h-28" placeholder="例如：组装线电机异响，设备无法继续运行"></textarea>
        </label>
        <label class="field-label mt-4">
          补充说明
          <input v-model="repairForm.remark" class="input mt-1" placeholder="可填写工位、班次、影响的生产任务等" />
        </label>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="btn-secondary" @click="showRepairForm = false">取消</button>
          <button class="btn-primary" :disabled="saving">
            <Send class="h-4 w-4" />
            {{ saving ? '提交中...' : '提交给车间主任' }}
          </button>
        </div>
      </form>
    </div>
  </MainLayout>
</template>

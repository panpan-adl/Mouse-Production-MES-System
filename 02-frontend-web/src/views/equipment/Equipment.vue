<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { equipmentApi, type Equipment } from '@/api/mes'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const equipments = ref<Equipment[]>([])
const keyword = ref('')
const statusFilter = ref('')
const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<Equipment>({
  equipmentCode: '',
  equipmentName: '',
  equipmentType: '',
  model: '',
  manufacturer: '',
  purchaseDate: '',
  warrantyPeriod: 12,
  location: '',
  status: 'NORMAL',
  responsibleId: 1,
  responsibleName: '',
  remark: '',
})

const filteredEquipments = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return equipments.value.filter((item) => {
    const matchWord = !word || item.equipmentCode?.toLowerCase().includes(word) || item.equipmentName?.toLowerCase().includes(word)
    const matchStatus = !statusFilter.value || item.status === statusFilter.value
    return matchWord && matchStatus
  })
})

const normalCount = computed(() => equipments.value.filter((item) => item.status === 'NORMAL').length)
const faultCount = computed(() => equipments.value.filter((item) => item.status === 'FAULT').length)
const maintainingCount = computed(() => equipments.value.filter((item) => item.status === 'MAINTAINING').length)
const scrappedCount = computed(() => equipments.value.filter((item) => item.status === 'SCRAPPED').length)

onMounted(loadEquipments)

async function loadEquipments() {
  loading.value = true
  message.value = ''
  try {
    equipments.value = await equipmentApi.list()
  } catch (error: any) {
    message.value = error.message || '设备加载失败'
  } finally {
    loading.value = false
  }
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    NORMAL: '正常',
    FAULT: '故障',
    MAINTAINING: '维修中',
    SCRAPPED: '报废',
  }
  return map[status || ''] || status || '-'
}

function statusClass(status?: string) {
  if (status === 'NORMAL') return 'tag-green'
  if (status === 'FAULT') return 'tag-red'
  if (status === 'MAINTAINING') return 'tag-yellow'
  if (status === 'SCRAPPED') return 'tag-gray'
  return 'tag-gray'
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    equipmentCode: `EQ-${Date.now()}`,
    equipmentName: '',
    equipmentType: '',
    model: '',
    manufacturer: '',
    purchaseDate: '',
    warrantyPeriod: 12,
    location: '',
    status: 'NORMAL',
    responsibleId: 1,
    responsibleName: '',
    remark: '',
  })
}

function openCreate() {
  resetForm()
  showForm.value = true
}

function openEdit(item: Equipment) {
  editingId.value = item.id || null
  Object.assign(form, {
    ...item,
    purchaseDate: item.purchaseDate ? String(item.purchaseDate).slice(0, 10) : '',
  })
  showForm.value = true
}

async function saveEquipment() {
  if (!form.equipmentCode || !form.equipmentName) {
    message.value = '设备编码和名称不能为空'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await equipmentApi.update(editingId.value, form)
      message.value = '设备已更新'
    } else {
      await equipmentApi.create(form)
      message.value = '设备已新增'
    }
    completeDrafts();showForm.value=false
    await loadEquipments()
  } catch (error: any) {
    message.value = error.message || '保存设备失败'
  } finally {
    saving.value = false
  }
}

async function removeEquipment(item: Equipment) {
  if (!item.id || !window.confirm(`确认删除设备「${item.equipmentName}」吗？`)) return
  try {
    await equipmentApi.remove(item.id)
    message.value = '设备已删除'
    await loadEquipments()
  } catch (error: any) {
    message.value = error.message || '删除设备失败'
  }
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id || editingId.value}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">设备台账</h1>
          <p class="text-gray-500 mt-1">维护设备基础资料和运行状态。</p>
        </div>
        <button class="btn-primary" @click="openCreate"><Plus class="w-4 h-4" />新增设备</button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="stat-card"><p class="stat-label">正常</p><p class="stat-value text-green-600">{{ normalCount }}</p></div>
        <div class="stat-card"><p class="stat-label">故障</p><p class="stat-value text-red-600">{{ faultCount }}</p></div>
        <div class="stat-card"><p class="stat-label">维修中</p><p class="stat-value text-yellow-600">{{ maintainingCount }}</p></div>
        <div class="stat-card"><p class="stat-label">报废</p><p class="stat-value text-gray-500">{{ scrappedCount }}</p></div>
      </div>

      <div class="panel">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-5">
          <div class="relative lg:col-span-2">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索设备编码或名称" />
          </div>
          <select v-model="statusFilter" class="input">
            <option value="">全部状态</option>
            <option value="NORMAL">正常</option>
            <option value="FAULT">故障</option>
            <option value="MAINTAINING">维修中</option>
            <option value="SCRAPPED">报废</option>
          </select>
          <button class="btn-secondary" @click="loadEquipments"><RefreshCw class="w-4 h-4" />刷新</button>
        </div>
        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>设备编码</th>
                <th>设备名称</th>
                <th>类型</th>
                <th>型号</th>
                <th>位置</th>
                <th>负责人</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载设备...</td></tr>
              <tr v-else-if="filteredEquipments.length === 0"><td colspan="8" class="empty-cell">暂无设备数据</td></tr>
              <tr v-for="item in filteredEquipments" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.equipmentCode }}</td>
                <td>{{ item.equipmentName }}</td>
                <td>{{ item.equipmentType || '-' }}</td>
                <td>{{ item.model || '-' }}</td>
                <td>{{ item.location || '-' }}</td>
                <td>{{ item.responsibleName || '-' }}</td>
                <td><span :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
                <td class="text-right space-x-2">
                  <button class="link-btn" @click="openEdit(item)">修改</button>
                  <button class="danger-link" @click="removeEquipment(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask" data-business-form>
        <div class="modal-card max-w-3xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">{{ editingId ? '修改设备' : '新增设备' }}</h2>
            <button class="icon-btn" @click="showForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">设备编码<input v-model="form.equipmentCode" class="input mt-1" /></label>
            <label class="field-label">设备名称<input v-model="form.equipmentName" class="input mt-1" /></label>
            <label class="field-label">设备类型<input v-model="form.equipmentType" class="input mt-1" /></label>
            <label class="field-label">型号<input v-model="form.model" class="input mt-1" /></label>
            <label class="field-label">厂商<input v-model="form.manufacturer" class="input mt-1" /></label>
            <label class="field-label">购置日期<input v-model="form.purchaseDate" type="date" class="input mt-1" /></label>
            <label class="field-label">保修期（月）<input v-model.number="form.warrantyPeriod" type="number" class="input mt-1" /></label>
            <label class="field-label">位置<input v-model="form.location" class="input mt-1" /></label>
            <label class="field-label">负责人<input v-model="form.responsibleName" class="input mt-1" /></label>
            <label class="field-label">状态
              <select v-model="form.status" class="input mt-1">
                <option value="NORMAL">正常</option>
                <option value="FAULT">故障</option>
                <option value="MAINTAINING">维修中</option>
                <option value="SCRAPPED">报废</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">备注<input v-model="form.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveEquipment">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

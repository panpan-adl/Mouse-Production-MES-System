<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, Send, X } from 'lucide-vue-next'
import {
  materialApi,
  materialRequestApi,
  productionTaskApi,
  warehouseApi,
  type Material,
  type MaterialRequest as MaterialRequestRecord,
  type ProductionTask,
  type Warehouse,
} from '@/api/mes'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'

type RequestStatus = '' | 'PENDING_CHECK' | 'ENOUGH' | 'SHORTAGE' | 'PURCHASE_REQUESTED' | 'PURCHASE_ORDERED'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const keyword = ref('')
const statusFilter = ref<RequestStatus>('')
const showForm = ref(false)
const requests = ref<MaterialRequestRecord[]>([])
const tasks = ref<ProductionTask[]>([])
const materials = ref<Material[]>([])
const warehouses = ref<Warehouse[]>([])
const taskMaterials=ref<number[]|null>(null)
const stock=ref<Record<string,any>[]>([])
const availableMaterials=computed(()=>materials.value.filter(m=>!taskMaterials.value||taskMaterials.value.includes(Number(m.id))))
const availableStock=computed(()=>stock.value.filter(s=>Number(s.material_id)===Number(form.materialId)&&(!form.warehouseId||Number(s.warehouse_id)===Number(form.warehouseId))).reduce((n,s)=>n+Number(s.quantity||0),0))
const requestedQuantity=computed(()=>requests.value.filter(r=>r.sourceTaskId===Number(selectedTaskId.value)&&r.materialId===Number(form.materialId)).reduce((n,r)=>n+Number(r.requiredQuantity||0),0))
const selectedTaskId = ref<number | ''>('')

const form = reactive<MaterialRequestRecord>({
  sourceTaskId: undefined,
  requiredDate: new Date().toLocaleDateString('sv-SE'),
  sourceTaskNo: '',
  productName: '',
  materialId: 0,
  requiredQuantity: 1,
  warehouseId: undefined,
  requestStatus: 'PENDING_CHECK',
  remark: '',
  createdBy: authStore.currentUser?.id || 1,
})

const filteredRequests = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return requests.value.filter((item) => {
    const matchStatus = !statusFilter.value || item.requestStatus === statusFilter.value
    const matchKeyword =
      !word ||
      item.requestNo?.toLowerCase().includes(word) ||
      item.sourceTaskNo?.toLowerCase().includes(word) ||
      item.productName?.toLowerCase().includes(word) ||
      item.materialCode?.toLowerCase().includes(word) ||
      item.materialName?.toLowerCase().includes(word)
    return matchStatus && matchKeyword
  })
})

const pendingCount = computed(() => requests.value.filter((item) => item.requestStatus === 'PENDING_CHECK').length)
const shortageCount = computed(() => requests.value.filter((item) => Number(item.shortageQuantity || 0) > 0).length)
const requestedCount = computed(() => requests.value.filter((item) => item.requestStatus === 'PURCHASE_REQUESTED').length)
const enoughCount = computed(() => requests.value.filter((item) => item.requestStatus === 'ENOUGH').length)

onMounted(async () => {
  await Promise.all([loadRequests(), loadTasks(), loadMaterials(), loadWarehouses()])
})

async function loadRequests() {
  loading.value = true
  message.value = ''
  try {
    requests.value = await materialRequestApi.list()
  } catch (error: any) {
    message.value = error.message || '物料请求加载失败，请确认后端已重启并执行最新数据库脚本。'
  } finally {
    loading.value = false
  }
}

async function loadTasks() {
  try {
    tasks.value = await productionTaskApi.list()
  } catch {
    tasks.value = []
  }
}

async function loadMaterials() {
  try {
    materials.value = await materialApi.list()
    stock.value=await request.get('/fulfillment/stock')
    if (!form.materialId && materials.value[0]?.id) {
      form.materialId = materials.value[0].id
    }
  } catch {
    materials.value = []
  }
}

async function loadWarehouses() {
  try {
    warehouses.value = await warehouseApi.list()
    if (!form.warehouseId && warehouses.value[0]?.id) {
      form.warehouseId = warehouses.value[0].id
    }
  } catch {
    warehouses.value = []
  }
}

function resetForm() {
  selectedTaskId.value = '';taskMaterials.value=null
  Object.assign(form, {
    sourceTaskId: undefined,
  requiredDate: new Date().toLocaleDateString('sv-SE'),
    sourceTaskNo: '',
    productName: '',
    materialId: materials.value[0]?.id || 0,
    requiredQuantity: 1,
    warehouseId: warehouses.value[0]?.id,
    requestStatus: 'PENDING_CHECK',
    remark: '',
    createdBy: authStore.currentUser?.id || 1,
  })
}

function openCreate() {
  resetForm()
  showForm.value = true
}

async function handleTaskChange() {
  const task = tasks.value.find((item) => item.id === Number(selectedTaskId.value))
  form.sourceTaskId = task?.id
  form.sourceTaskNo = task?.taskNo || ''
  form.productName = task?.productName || ''
  taskMaterials.value=null;try{const orders=await request.get<Record<string,any>[]>(`/material-requests/task-materials/${task?.id}`);taskMaterials.value=orders.map(r=>Number(r.material_id));form.materialId=availableMaterials.value[0]?.id||0}catch(e:any){message.value=e.message}
}

function materialText(id?: number) {
  const material = materials.value.find((item) => item.id === Number(id))
  return material ? `${material.materialCode} / ${material.materialName}` : '-'
}

function warehouseText(id?: number) {
  const warehouse = warehouses.value.find((item) => item.id === Number(id))
  return warehouse ? `${warehouse.warehouseCode} / ${warehouse.warehouseName}` : '-'
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING_CHECK: '待确认',
    ENOUGH: '库存充足',
    SHORTAGE: '库存不足',
    PURCHASE_REQUESTED: '已发采购',
    PURCHASE_ORDERED: '已转采购单',
  }
  return map[status || ''] || '未知'
}

function statusClass(status?: string) {
  const map: Record<string, string> = {
    PENDING_CHECK: 'tag-yellow',
    ENOUGH: 'tag-green',
    SHORTAGE: 'tag-red',
    PURCHASE_REQUESTED: 'tag-blue',
    PURCHASE_ORDERED: 'tag-gray',
  }
  return map[status || ''] || 'tag-gray'
}

function canSendPurchaseRequest(item: MaterialRequestRecord) {
  return (
    Number(item.shortageQuantity || 0) > 0 &&
    item.requestStatus !== 'PURCHASE_REQUESTED' &&
    item.requestStatus !== 'PURCHASE_ORDERED'
  )
}

async function saveRequest() {
  if (!form.materialId || Number(form.requiredQuantity || 0) <= 0) {
    message.value = '请选择物料，并填写大于 0 的需求数量。'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    await materialRequestApi.create({
      ...form,
      materialId: Number(form.materialId),
      requiredQuantity: Number(form.requiredQuantity),
      warehouseId: form.warehouseId ? Number(form.warehouseId) : undefined,
      createdBy: authStore.currentUser?.id || 1,
    })
    message.value = '物料请求已登记，系统已自动计算库存和缺料数量。'
    completeDrafts();showForm.value=false
    await loadRequests()
  } catch (error: any) {
    message.value = error.message || '保存物料请求失败'
  } finally {
    saving.value = false
  }
}

async function refreshRequest(item: MaterialRequestRecord) {
  if (!item.id) return
  saving.value = true
  try {
    await materialRequestApi.update(item.id, {
      sourceTaskId: item.sourceTaskId,
      sourceTaskNo: item.sourceTaskNo,
      productName: item.productName,
      materialId: item.materialId,
      requiredQuantity: item.requiredQuantity,
      warehouseId: item.warehouseId,
      requestStatus: item.requestStatus,
      remark: item.remark,
    })
    message.value = '已重新读取当前库存并刷新缺料判断。'
    await loadRequests()
  } catch (error: any) {
    message.value = error.message || '刷新库存判断失败'
  } finally {
    saving.value = false
  }
}

async function sendPurchaseRequest(item: MaterialRequestRecord) {
  if (!item.id) return
  saving.value = true
  try {
    await materialRequestApi.requestPurchase(item.id)
    message.value = `物料请求 ${item.requestNo} 已发送给采购员。`
    await loadRequests()
  } catch (error: any) {
    message.value = error.message || '发送采购请求失败'
  } finally {
    saving.value = false
  }
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form,selectedTaskId}, record:()=>(form as Record<string,any>).id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">物料请求</h1>
          <p class="text-gray-500 mt-1">仓管员接收生产任务用料请求，判断库存是否足够；不足时发送采购请求给采购员。</p>
        </div>
        <button class="btn-primary" @click="openCreate">
          <Plus class="w-4 h-4" />
          登记物料请求
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <button class="stat-card text-left" @click="statusFilter = 'PENDING_CHECK'">
          <p class="stat-label">待确认</p>
          <p class="stat-value text-yellow-600">{{ pendingCount }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'SHORTAGE'">
          <p class="stat-label">存在缺料</p>
          <p class="stat-value text-red-600">{{ shortageCount }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'PURCHASE_REQUESTED'">
          <p class="stat-label">已发采购</p>
          <p class="stat-value text-blue-600">{{ requestedCount }}</p>
        </button>
        <button class="stat-card text-left" @click="statusFilter = 'ENOUGH'">
          <p class="stat-label">库存充足</p>
          <p class="stat-value text-green-600">{{ enoughCount }}</p>
        </button>
      </div>

      <div class="panel">
        <div class="mb-5 overflow-x-auto pb-1">
          <div class="grid min-w-[860px] grid-cols-[minmax(340px,1fr)_210px_92px_92px] items-center gap-3">
            <label class="relative">
              <Search class="search-icon" />
              <input v-model="keyword" class="input pl-10" placeholder="搜索请求编号、任务、产品或物料" />
            </label>
            <select v-model="statusFilter" class="input">
              <option value="">全部状态</option>
              <option value="PENDING_CHECK">待确认</option>
              <option value="ENOUGH">库存充足</option>
              <option value="SHORTAGE">库存不足</option>
              <option value="PURCHASE_REQUESTED">已发采购</option>
              <option value="PURCHASE_ORDERED">已转采购单</option>
            </select>
            <button class="btn-primary w-full" @click="loadRequests"><Search class="w-4 h-4" />查询</button>
            <button class="btn-secondary w-full" @click="loadRequests"><RefreshCw class="w-4 h-4" />刷新</button>
          </div>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[1120px]">
            <thead>
              <tr>
                <th>请求编号</th>
                <th>来源任务</th>
                <th>产品</th>
                <th>物料</th>
                <th>需求数量</th>
                <th>当前库存</th>
                <th>缺料数量</th>
                <th>建议仓库</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10" class="empty-cell">正在加载物料请求...</td>
              </tr>
              <tr v-else-if="filteredRequests.length === 0">
                <td colspan="10" class="empty-cell">暂无物料请求</td>
              </tr>
              <tr v-for="item in filteredRequests" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.requestNo }}</td>
                <td>{{ item.sourceTaskNo || '-' }}</td>
                <td>{{ item.productName || '-' }}</td>
                <td>{{ item.materialCode || materialText(item.materialId) }} / {{ item.materialName || '' }}</td>
                <td>{{ item.requiredQuantity }}</td>
                <td>{{ item.availableQuantity || 0 }}</td>
                <td :class="Number(item.shortageQuantity || 0) > 0 ? 'font-medium text-red-600' : 'text-green-600'">
                  {{ item.shortageQuantity || 0 }}
                </td>
                <td>{{ warehouseText(item.warehouseId) }}</td>
                <td><span :class="statusClass(item.requestStatus)">{{ statusText(item.requestStatus) }}</span></td>
                <td class="text-right space-x-2">
                  <button v-if="!['PURCHASE_REQUESTED','PURCHASE_ORDERED'].includes(item.requestStatus || '')" class="link-btn" :disabled="saving" @click="refreshRequest(item)">刷新判断</button>
                  <router-link v-if="['PURCHASE_REQUESTED','PURCHASE_ORDERED'].includes(item.requestStatus || '') && canVisit(Number(authStore.effectiveRoleId), '/purchase/requests') && authStore.hasMenu('/purchase/requests')" class="link-btn" :to="{ path: '/purchase/requests', query: { requestId: item.id, requestNo: item.requestNo } }">查看采购进度</router-link>
                  <button v-if="canSendPurchaseRequest(item)" class="link-btn" :disabled="saving" @click="sendPurchaseRequest(item)">
                    <Send class="inline-block w-4 h-4 mr-1" />
                    发送采购请求
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask" data-business-form>
        <div class="modal-card max-w-3xl">
          <div class="flex items-start justify-between gap-4 mb-5">
            <div>
              <p class="text-sm text-gray-500">新增物料请求</p>
              <h2 class="text-xl font-semibold text-gray-900">登记生产任务用料需求</h2>
            </div>
            <button class="icon-btn" @click="showForm = false"><X class="w-5 h-5" /></button>
          </div>

          <p class="text-sm text-blue-700 mb-4">可用库存：{{availableStock}}；该任务累计申请：{{requestedQuantity}}。登记请求不会扣库存，实际发料请由仓库办理。</p><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><label class="form-field"><span>需求日期</span><input v-model="form.requiredDate" type="date" required class="input" /></label>
            <label class="form-field md:col-span-2">
              <span>来源生产任务</span>
              <select v-model="selectedTaskId" required class="input" @change="handleTaskChange">
                <option value="">请选择有效生产任务</option>
                <option v-for="task in tasks.filter(t=>Number(t.taskStatus||0)<4)" :key="task.id" :value="task.id">
                  {{ task.taskNo }} - {{ task.productName || '未填写产品' }} - {{ task.quantity || 0 }}
                </option>
              </select>
            </label>
            <label class="form-field">
              <span>来源任务编号</span>
              <input v-model="form.sourceTaskNo" readonly class="input" placeholder="例如 WO-20260715-002" />
            </label>
            <label class="form-field">
              <span>产品名称</span>
              <input v-model="form.productName" readonly class="input" placeholder="例如 S300静音商务鼠标" />
            </label>
            <label class="form-field">
              <span>需求物料</span>
              <select v-model.number="form.materialId" class="input">
                <option v-for="material in availableMaterials" :key="material.id" :value="material.id">
                  {{ material.materialCode }} / {{ material.materialName }}
                </option>
              </select>
            </label>
            <label class="form-field">
              <span>需求数量</span>
              <input v-model.number="form.requiredQuantity" type="number" min="1" class="input" />
            </label>
            <label class="form-field md:col-span-2">
              <span>建议出料仓库</span>
              <select v-model.number="form.warehouseId" class="input">
                <option :value="undefined">暂不指定仓库</option>
                <option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">
                  {{ warehouse.warehouseCode }} / {{ warehouse.warehouseName }}
                </option>
              </select>
            </label>
            <label class="form-field md:col-span-2">
              <span>备注</span>
              <textarea v-model="form.remark" class="input min-h-[96px]" placeholder="说明生产经理提出的用料背景、紧急程度等"></textarea>
            </label>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveRequest">保存并判断库存</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

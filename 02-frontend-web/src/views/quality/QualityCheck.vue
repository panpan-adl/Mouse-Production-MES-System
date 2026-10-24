<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { Pencil, Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { productionTaskApi, qcInspectionApi, type ProductionTask, type QcInspection } from '@/api/mes'

const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const inspections = ref<QcInspection[]>([])
const keyword = ref('')
const typeFilter = ref('')
const resultFilter = ref('')
const showForm = ref(false)
const editingId = ref<number | null>(null)
const preserveExistingItems = ref(false)
const passResults = ['QUALIFIED', 'CONCESSION_ACCEPT']

const form = reactive<QcInspection & { items: NonNullable<QcInspection['items']> }>({
  inspectionNo: '',
  inspectionType: 'INCOMING',
  sourceOrderId: undefined,
  sourceOrderNo: '',
  sampleQuantity: 1,
  qualifiedQuantity: 1,
  result: 'QUALIFIED',
  defectReason: '',
  handleMethod: '',
  remark: '',
  createdBy: 1,
  items: [{
    inspectionItem: '外观检查',
    inspectionStandard: '符合标准',
    actualValue: '',
    sampleQuantity: 1,
    qualifiedQuantity: 1,
    itemResult: 'QUALIFIED',
    defectReason: '',
    handleMethod: '',
  }],
})

const filteredInspections = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return inspections.value.filter((item) => {
    const matchWord = !word || item.inspectionNo?.toLowerCase().includes(word) || item.sourceOrderNo?.toLowerCase().includes(word)
    const matchType = !typeFilter.value || item.inspectionType === typeFilter.value
    const matchResult = !resultFilter.value || item.result === resultFilter.value
    return matchWord && matchType && matchResult
  })
})

const passCount = computed(() => filteredInspections.value.filter((item) => passResults.includes(item.result || '')).length)
const failCount = computed(() => filteredInspections.value.filter((item) => !passResults.includes(item.result || '')).length)
const avgRate = computed(() => {
  if (filteredInspections.value.length === 0) return 0
  const total = filteredInspections.value.reduce((sum, item) => sum + Number(item.qualifiedRate || 0), 0)
  return total / filteredInspections.value.length
})
const formTitle = computed(() => editingId.value ? '修改质检单' : '新增质检单')

onMounted(async () => {
  await loadInspections()
  await applyRouteTask()
})

async function loadInspections() {
  loading.value = true
  message.value = ''
  try {
    inspections.value = await qcInspectionApi.list()
  } catch (error: any) {
    message.value = error.message || '质检记录加载失败'
  } finally {
    loading.value = false
  }
}

async function applyRouteTask() {
  const taskNo = typeof route.query.taskNo === 'string' ? route.query.taskNo : ''
  const taskId = Number(route.query.taskId || 0)
  if (!taskNo && !taskId) return

  typeFilter.value = 'PRODUCTION'
  if (taskNo) keyword.value = taskNo

  const existing = taskNo ? inspections.value.find((item) => item.sourceOrderNo === taskNo) : undefined
  if (existing) {
    message.value = `已筛选来源任务/单据「${taskNo}」的质检记录。`
    return
  }

  let task: ProductionTask | undefined
  if (taskId) {
    try {
      task = await productionTaskApi.get(taskId)
    } catch {
      task = undefined
    }
  }
  openCreateFromTask(taskNo || task?.taskNo || '', taskId || Number(task?.id || 0), task)
}

function typeText(type?: string) {
  const map: Record<string, string> = { INCOMING: '来料质检', PRODUCTION: '生产质检', FINISHED: '成品质检' }
  return map[type || ''] || type || '-'
}

function resultText(result?: string) {
  const map: Record<string, string> = {
    QUALIFIED: '合格',
    UNQUALIFIED: '不合格',
    CONCESSION_ACCEPT: '让步接收',
    REWORK: '返工',
    SCRAP: '报废',
  }
  return map[result || ''] || result || '-'
}

function resultClass(result?: string) {
  if (result === 'QUALIFIED') return 'tag-green'
  if (result === 'CONCESSION_ACCEPT') return 'tag-blue'
  if (result === 'REWORK') return 'tag-yellow'
  if (result === 'SCRAP' || result === 'UNQUALIFIED') return 'tag-red'
  return 'tag-gray'
}

function resetForm(overrides: Partial<QcInspection> = {}) {
  Object.assign(form, {
    id: undefined,
    inspectionNo: `QC-${Date.now()}`,
    inspectionType: 'INCOMING',
    sourceOrderId: undefined,
    sourceOrderNo: '',
    sampleQuantity: 1,
    qualifiedQuantity: 1,
    defectiveQuantity: 0,
    result: 'QUALIFIED',
    defectReason: '',
    handleMethod: '',
    remark: '',
    createdBy: authStore.currentUser?.id || 1,
    items: [{
      inspectionItem: '外观检查',
      inspectionStandard: '符合标准',
      actualValue: '',
      sampleQuantity: 1,
      qualifiedQuantity: 1,
      defectiveQuantity: 0,
      itemResult: 'QUALIFIED',
      defectReason: '',
      handleMethod: '',
    }],
    ...overrides,
  })
}

function openCreate() {
  editingId.value = null
  preserveExistingItems.value = false
  resetForm()
  showForm.value = true
}

function openEdit(item: QcInspection) {
  const firstItem = item.items?.[0]
  editingId.value = Number(item.id || 0) || null
  preserveExistingItems.value = Number(item.items?.length || 0) > 1
  resetForm({
    id: item.id,
    inspectionNo: item.inspectionNo,
    inspectionType: item.inspectionType,
    sourceOrderId: item.sourceOrderId,
    sourceOrderNo: item.sourceOrderNo || '',
    sampleQuantity: Number(item.sampleQuantity || 1),
    qualifiedQuantity: Number(item.qualifiedQuantity || 0),
    defectiveQuantity: Number(item.defectiveQuantity || 0),
    result: item.result || 'QUALIFIED',
    defectReason: item.defectReason || '',
    handleMethod: item.handleMethod || '',
    remark: item.remark || '',
    createdBy: item.createdBy || authStore.currentUser?.id || 1,
    items: [{
      inspectionItem: firstItem?.inspectionItem || '外观检查',
      inspectionStandard: firstItem?.inspectionStandard || firstItem?.standardValue || '符合标准',
      actualValue: firstItem?.actualValue || '',
      sampleQuantity: Number(firstItem?.sampleQuantity || item.sampleQuantity || 1),
      qualifiedQuantity: Number(firstItem?.qualifiedQuantity || item.qualifiedQuantity || 0),
      defectiveQuantity: Number(firstItem?.defectiveQuantity || item.defectiveQuantity || 0),
      itemResult: firstItem?.itemResult || item.result || 'QUALIFIED',
      defectReason: firstItem?.defectReason || item.defectReason || '',
      handleMethod: firstItem?.handleMethod || item.handleMethod || '',
      remark: firstItem?.remark || '',
    }],
  })
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingId.value = null
  preserveExistingItems.value = false
}

function openCreateFromTask(taskNo: string, taskId: number, task?: ProductionTask) {
  const taskQuantity = Number(task?.quantity || 1)
  resetForm({
    inspectionType: 'PRODUCTION',
    sourceOrderId: taskId || undefined,
    sourceOrderNo: taskNo,
    sampleQuantity: taskQuantity,
    qualifiedQuantity: taskQuantity,
    defectiveQuantity: 0,
    remark: task?.productName ? `来源生产任务：${task.productName}` : '来源生产任务',
  })
  if (form.items[0]) {
    form.items[0].inspectionItem = task?.productName ? `${task.productName}成品检验` : '生产任务成品检验'
    form.items[0].sampleQuantity = taskQuantity
    form.items[0].qualifiedQuantity = taskQuantity
    form.items[0].defectiveQuantity = 0
  }
  message.value = `已从待质检任务「${taskNo}」带入来源信息，请填写质检结果。`
  showForm.value = true
}

function buildInspectionPayload(includeItems: boolean): QcInspection {
  const payload: QcInspection = {
    id: form.id,
    inspectionNo: form.inspectionNo,
    inspectionType: form.inspectionType,
    sourceOrderId: form.sourceOrderId,
    sourceOrderNo: form.sourceOrderNo,
    sampleQuantity: form.sampleQuantity,
    qualifiedQuantity: form.qualifiedQuantity,
    defectiveQuantity: form.defectiveQuantity,
    qualifiedRate: form.qualifiedRate,
    result: form.result,
    defectReason: form.defectReason,
    handleMethod: form.handleMethod,
    businessStatus: form.businessStatus,
    remark: form.remark,
    createdBy: form.createdBy,
  }
  if (includeItems) {
    payload.items = form.items.map((item) => ({ ...item }))
  }
  return payload
}

async function saveInspection() {
  if (!form.inspectionNo || !form.inspectionType) {
    message.value = '质检单号和质检类型不能为空'
    return
  }
  if (Number(form.qualifiedQuantity || 0) > Number(form.sampleQuantity || 0)) {
    message.value = '合格数量不能大于检验数量'
    return
  }
  saving.value = true
  try {
    form.sampleQuantity = Number(form.sampleQuantity || 0)
    form.qualifiedQuantity = Number(form.qualifiedQuantity || 0)
    form.defectiveQuantity = Math.max(form.sampleQuantity - form.qualifiedQuantity, 0)
    if (form.items?.[0]) {
      form.items[0].sampleQuantity = Number(form.sampleQuantity)
      form.items[0].qualifiedQuantity = Number(form.qualifiedQuantity)
      form.items[0].defectiveQuantity = Number(form.defectiveQuantity)
      form.items[0].itemResult = form.result
      form.items[0].defectReason = form.defectReason
      form.items[0].handleMethod = form.handleMethod
    }
    const payload = buildInspectionPayload(!editingId.value || !preserveExistingItems.value)
    if (editingId.value) {
      await qcInspectionApi.update(editingId.value, payload)
    } else {
      await qcInspectionApi.create(payload)
    }
    const linkedTaskId = Number(form.sourceOrderId || 0)
    if (linkedTaskId && form.inspectionType === 'PRODUCTION') {
      try {
        const nextTaskStatus = passResults.includes(form.result || '') ? 4 : 3
        await productionTaskApi.update(linkedTaskId, { taskStatus: nextTaskStatus })
        message.value = editingId.value
          ? '质检单已修改，关联生产任务状态已同步'
          : '质检单已新增，关联生产任务状态已同步'
      } catch (syncError: any) {
        const action = editingId.value ? '已修改' : '已新增'
        message.value = syncError.message || `质检单${action}，但关联生产任务状态同步失败`
      }
    } else if (linkedTaskId) {
      message.value = editingId.value ? '质检单已修改' : '质检单已新增'
    } else {
      message.value = editingId.value ? '质检单已修改' : '质检单已新增'
    }
    completeDrafts();closeForm()
    await loadInspections()
  } catch (error: any) {
    message.value = error.message || '保存质检单失败'
  } finally {
    saving.value = false
  }
}

async function removeInspection(item: QcInspection) {
  if (!item.id || !window.confirm(`确认删除质检单「${item.inspectionNo}」吗？`)) return
  try {
    await qcInspectionApi.remove(item.id)
    message.value = '质检单已删除'
    await loadInspections()
  } catch (error: any) {
    message.value = error.message || '删除质检单失败'
  }
}


const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id || editingId.value}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">质检记录</h1>
          <p class="text-gray-500 mt-1">新增、查询和跟踪来料/生产/成品质检结果；来源任务/单据用于追溯上游生产任务、采购单或成品入库。</p>
        </div>
        <button class="btn-primary" @click="openCreate"><Plus class="w-4 h-4" />新增质检</button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="stat-card"><p class="stat-label">当前结果数</p><p class="stat-value">{{ filteredInspections.length }}</p></div>
        <div class="stat-card"><p class="stat-label">通过类结果</p><p class="stat-value text-green-600">{{ passCount }}</p></div>
        <div class="stat-card"><p class="stat-label">异常结果</p><p class="stat-value text-red-600">{{ failCount }}</p></div>
        <div class="stat-card"><p class="stat-label">平均合格率</p><p class="stat-value text-blue-600">{{ avgRate.toFixed(2) }}%</p></div>
      </div>

      <div class="panel">
        <div class="grid grid-cols-1 lg:grid-cols-6 gap-3 mb-5">
          <div class="relative lg:col-span-2">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索质检单号或来源任务/单据" />
          </div>
          <select v-model="typeFilter" class="input">
            <option value="">全部类型</option>
            <option value="INCOMING">来料质检</option>
            <option value="PRODUCTION">生产质检</option>
            <option value="FINISHED">成品质检</option>
          </select>
          <select v-model="resultFilter" class="input">
            <option value="">全部结果</option>
            <option value="QUALIFIED">合格</option>
            <option value="UNQUALIFIED">不合格</option>
            <option value="CONCESSION_ACCEPT">让步接收</option>
            <option value="REWORK">返工</option>
            <option value="SCRAP">报废</option>
          </select>
          <button class="btn-primary" @click="loadInspections"><Search class="w-4 h-4" />查询</button>
          <button class="btn-secondary" @click="loadInspections"><RefreshCw class="w-4 h-4" />刷新</button>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>质检单号</th>
                <th>类型</th>
                <th>来源任务/单据</th>
                <th>检验数量</th>
                <th>合格数量</th>
                <th>不合格数量</th>
                <th>合格率</th>
                <th>结果</th>
                <th>处理方式</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="10" class="empty-cell">正在加载质检记录...</td></tr>
              <tr v-else-if="filteredInspections.length === 0"><td colspan="10" class="empty-cell">暂无质检记录</td></tr>
              <tr v-for="item in filteredInspections" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.inspectionNo }}</td>
                <td>{{ typeText(item.inspectionType) }}</td>
                <td>{{ item.sourceOrderNo || '-' }}</td>
                <td>{{ item.sampleQuantity || 0 }}</td>
                <td class="text-green-600 font-semibold">{{ item.qualifiedQuantity || 0 }}</td>
                <td class="text-red-600 font-semibold">{{ item.defectiveQuantity || 0 }}</td>
                <td>{{ Number(item.qualifiedRate || 0).toFixed(2) }}%</td>
                <td><span :class="resultClass(item.result)">{{ resultText(item.result) }}</span></td>
                <td>{{ item.handleMethod || '-' }}</td>
                <td class="text-right">
                  <div class="inline-flex items-center justify-end gap-3">
                    <button class="link-btn inline-flex items-center gap-1" @click="openEdit(item)">
                      <Pencil class="h-4 w-4" />
                      修改
                    </button>
                    <button class="danger-link" @click="removeInspection(item)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask">
        <div class="modal-card max-w-2xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">{{ formTitle }}</h2>
            <button class="icon-btn" @click="closeForm"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">质检单号<input v-model="form.inspectionNo" class="input mt-1" /></label>
            <label class="field-label">质检类型
              <select v-model="form.inspectionType" class="input mt-1">
                <option value="INCOMING">来料质检</option>
                <option value="PRODUCTION">生产质检</option>
                <option value="FINISHED">成品质检</option>
              </select>
            </label>
            <label class="field-label">来源任务/单据<input v-model="form.sourceOrderNo" class="input mt-1" /></label>
            <p v-if="preserveExistingItems" class="md:col-span-2 rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-700">
              该质检单已有多条检测项明细，本次修改会保留原明细，只更新主单数量、结果、原因、处理方式和备注。
            </p>
            <label v-else class="field-label">检测项<input v-model="form.items[0].inspectionItem" class="input mt-1" /></label>
            <label class="field-label">检验数量<input v-model.number="form.sampleQuantity" type="number" min="0" class="input mt-1" /></label>
            <label class="field-label">合格数量<input v-model.number="form.qualifiedQuantity" type="number" min="0" class="input mt-1" /></label>
            <label class="field-label">质检结果
              <select v-model="form.result" class="input mt-1">
                <option value="QUALIFIED">合格</option>
                <option value="UNQUALIFIED">不合格</option>
                <option value="CONCESSION_ACCEPT">让步接收</option>
                <option value="REWORK">返工</option>
                <option value="SCRAP">报废</option>
              </select>
            </label>
            <label class="field-label">不合格原因<input v-model="form.defectReason" class="input mt-1" /></label>
            <label class="field-label md:col-span-2">处理方式<input v-model="form.handleMethod" class="input mt-1" placeholder="不合格、返工、报废时必填" /></label>
            <label class="field-label md:col-span-2">备注<input v-model="form.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="closeForm">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveInspection">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

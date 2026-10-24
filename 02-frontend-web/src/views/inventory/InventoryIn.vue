<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { materialApi, stockInApi, warehouseApi, type Material, type StockIn, type Warehouse } from '@/api/mes'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const records = ref<StockIn[]>([])
const materials = ref<Material[]>([])
const warehouses = ref<Warehouse[]>([])
const keyword = ref('')
const typeFilter = ref('')
const showForm = ref(false)

const form = reactive<StockIn & { items: NonNullable<StockIn['items']> }>({
  stockInNo: '',
  stockInType: 'PURCHASE',
  warehouseId: 0,
  sourceOrderNo: '',
  status: 'COMPLETED',
  remark: '',
  createdBy: 1,
  items: [{ materialId: 1, quantity: 1, locationId: undefined, remark: '' }],
})

const filteredRecords = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return records.value.filter((item) => {
    const matchWord = !word || item.stockInNo?.toLowerCase().includes(word) || item.sourceOrderNo?.toLowerCase().includes(word)
    const matchType = !typeFilter.value || item.stockInType === typeFilter.value
    return matchWord && matchType
  })
})

const totalQuantity = computed(() => filteredRecords.value.reduce((sum, item) => sum + Number(item.totalQuantity || 0), 0))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [stockData, materialData, warehouseData] = await Promise.all([
      stockInApi.list(),
      materialApi.list(),
      warehouseApi.list(),
    ])
    records.value = stockData
    materials.value = materialData
    warehouses.value = warehouseData
  } catch (error: any) {
    message.value = error.message || '入库数据加载失败'
  } finally {
    loading.value = false
  }
}

function materialName(id?: number) {
  return materials.value.find((item) => item.id === id)?.materialName || `物料-${id || '-'}`
}

function warehouseName(id?: number) {
  return warehouses.value.find((item) => item.id === id)?.warehouseName || `仓库-${id || '-'}`
}

function typeText(type?: string) {
  const map: Record<string, string> = { PURCHASE: '采购入库', PRODUCTION: '生产入库', OTHER: '其他入库' }
  return map[type || ''] || type || '-'
}

function openCreate() {
  Object.assign(form, {
    stockInNo: `IN-${Date.now()}`,
    stockInType: 'PURCHASE',
    warehouseId: warehouses.value[0]?.id || 0,
    sourceOrderNo: '',
    status: 'COMPLETED',
    remark: '',
    createdBy: 1,
    items: [{ materialId: materials.value[0]?.id || 1, quantity: 1, locationId: undefined, remark: '' }],
  })
  showForm.value = true
}

async function saveStockIn() {
  if (!form.stockInNo || !form.warehouseId || !form.items?.[0]?.materialId) {
    message.value = '入库单号、仓库和物料不能为空'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    form.items = form.items.map((item) => ({
      ...item,
      materialId: Number(item.materialId),
      quantity: Number(item.quantity),
      locationId: item.locationId ? Number(item.locationId) : undefined,
    }))
    await stockInApi.create(form)
    message.value = '入库成功，库存已增加'
    completeDrafts();showForm.value=false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '入库失败'
  } finally {
    saving.value = false
  }
}

function firstMaterial(item: StockIn) {
  return item.items?.[0]?.materialId
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">入库管理</h1>
          <p class="text-gray-500 mt-1">采购入库、生产入库和其他入库，提交后自动增加库存。</p>
        </div>
        <button class="btn-primary" @click="openCreate"><Plus class="w-4 h-4" />新增入库</button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="stat-card"><p class="stat-label">入库单数</p><p class="stat-value">{{ records.length }}</p></div>
        <div class="stat-card"><p class="stat-label">当前列表数量</p><p class="stat-value text-green-600">{{ totalQuantity }}</p></div>
        <div class="stat-card"><p class="stat-label">仓库数量</p><p class="stat-value text-blue-600">{{ warehouses.length }}</p></div>
      </div>

      <div class="panel">
        <div class="mb-4 rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
          入库单提交后会立即增加库存并写入库存流水。为保证账实一致，已过账单据不在这里直接修改或删除；录错时应通过冲销单/更正单处理。
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-5">
          <div class="relative lg:col-span-2">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索入库单号或来源单号" />
          </div>
          <select v-model="typeFilter" class="input">
            <option value="">全部类型</option>
            <option value="PURCHASE">采购入库</option>
            <option value="PRODUCTION">生产入库</option>
            <option value="OTHER">其他入库</option>
          </select>
          <button class="btn-secondary" @click="loadAll"><RefreshCw class="w-4 h-4" />刷新</button>
        </div>
        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>入库单号</th>
                <th>类型</th>
                <th>物料</th>
                <th>数量</th>
                <th>仓库</th>
                <th>来源单号</th>
                <th>时间</th>
                <th class="text-right">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="empty-cell">正在加载入库单...</td></tr>
              <tr v-else-if="filteredRecords.length === 0"><td colspan="8" class="empty-cell">暂无入库数据</td></tr>
              <tr v-for="item in filteredRecords" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.stockInNo }}</td>
                <td>{{ typeText(item.stockInType) }}</td>
                <td>{{ materialName(firstMaterial(item)) }}</td>
                <td class="font-semibold text-gray-900">{{ item.totalQuantity || item.items?.[0]?.quantity || 0 }}</td>
                <td>{{ warehouseName(item.warehouseId) }}</td>
                <td>{{ item.sourceOrderNo || '-' }}</td>
                <td>{{ formatDate(item.createdAt) }}</td>
                <td class="text-right"><span class="tag-blue">已过账只读</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask" data-business-form>
        <div class="modal-card max-w-2xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">新增入库</h2>
            <button class="icon-btn" @click="showForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">入库单号<input v-model="form.stockInNo" class="input mt-1" /></label>
            <label class="field-label">入库类型
              <select v-model="form.stockInType" class="input mt-1">
                <option value="PURCHASE">采购入库</option>
                <option value="PRODUCTION">生产入库</option>
                <option value="OTHER">其他入库</option>
              </select>
            </label>
            <label class="field-label">仓库
              <select v-model.number="form.warehouseId" class="input mt-1">
                <option :value="0">请选择仓库</option>
                <option v-for="item in warehouses" :key="item.id" :value="item.id">{{ item.warehouseName }}</option>
              </select>
            </label>
            <label class="field-label">物料
              <select v-model.number="form.items[0].materialId" class="input mt-1">
                <option v-for="item in materials" :key="item.id" :value="item.id">{{ item.materialName }}</option>
              </select>
            </label>
            <label class="field-label">数量<input v-model.number="form.items[0].quantity" type="number" min="0" class="input mt-1" /></label>
            <label class="field-label">库位ID（可空）<input v-model.number="form.items[0].locationId" type="number" class="input mt-1" /></label>
            <label class="field-label">来源单号<input v-model="form.sourceOrderNo" class="input mt-1" /></label>
            <label class="field-label">备注<input v-model="form.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveStockIn">{{ saving ? '提交中...' : '确认入库' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

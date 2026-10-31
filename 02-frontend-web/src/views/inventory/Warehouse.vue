<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import {
  inventoryApi,
  materialApi,
  warehouseApi,
  type Inventory,
  type Material,
  type Warehouse,
} from '@/api/mes'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const warehouses = ref<Warehouse[]>([])
const inventoryList = ref<Inventory[]>([])
const lowStockList = ref<Inventory[]>([])
const materials = ref<Material[]>([])
const showWarehouseForm = ref(false)
const editingWarehouseId = ref<number | null>(null)

const filters = reactive({
  materialId: '' as number | '',
  warehouseId: '' as number | '',
  locationId: '' as number | '',
})

const warehouseForm = reactive<Warehouse>({
  warehouseCode: '',
  warehouseName: '',
  location: '',
  managerName: '',
  status: 1,
  remark: '',
})

const inventoryPage = ref(1)
const inventoryPageSize = ref(10)
const inventoryPageCount = computed(() => Math.max(1, Math.ceil(inventoryList.value.length / inventoryPageSize.value)))
const pagedInventory = computed(() => inventoryList.value.slice((inventoryPage.value - 1) * inventoryPageSize.value, inventoryPage.value * inventoryPageSize.value))
watch(inventoryPageSize, () => { inventoryPage.value = 1 })
watch(inventoryPageCount, count => { inventoryPage.value = Math.min(inventoryPage.value, count) })

const totalQuantity = computed(() => inventoryList.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [warehouseData, materialData, inventoryData, lowData] = await Promise.all([
      warehouseApi.list(),
      materialApi.list(),
      inventoryApi.search(filters),
      inventoryApi.lowStock(),
    ])
    warehouses.value = warehouseData
    materials.value = materialData
    inventoryList.value = inventoryData
    inventoryPage.value = 1
    lowStockList.value = lowData
  } catch (error: any) {
    message.value = error.message || '仓库库存数据加载失败'
  } finally {
    loading.value = false
  }
}

async function searchInventory() {
  loading.value = true
  try {
    inventoryList.value = await inventoryApi.search(filters)
    inventoryPage.value = 1
  } catch (error: any) {
    message.value = error.message || '库存查询失败'
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

function resetWarehouseForm() {
  editingWarehouseId.value = null
  Object.assign(warehouseForm, {
    warehouseCode: `WH-${Date.now()}`,
    warehouseName: '',
    location: '',
    managerName: '',
    status: 1,
    remark: '',
  })
}

function openCreateWarehouse() {
  resetWarehouseForm()
  showWarehouseForm.value = true
}

function openEditWarehouse(item: Warehouse) {
  editingWarehouseId.value = item.id || null
  Object.assign(warehouseForm, item)
  showWarehouseForm.value = true
}

async function saveWarehouse() {
  if (!warehouseForm.warehouseCode || !warehouseForm.warehouseName) {
    message.value = '仓库编码和名称不能为空'
    showBusinessFormError(message.value)
    return
  }
  saving.value = true
  try {
    if (editingWarehouseId.value) {
      await warehouseApi.update(editingWarehouseId.value, warehouseForm)
      message.value = '仓库已更新'
    } else {
      await warehouseApi.create(warehouseForm)
      message.value = '仓库已新增'
    }
    completeDrafts();showWarehouseForm.value=false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '保存仓库失败'
  } finally {
    saving.value = false
  }
}

async function removeWarehouse(item: Warehouse) {
  if (!item.id || !window.confirm(`确认删除仓库「${item.warehouseName}」吗？`)) return
  try {
    await warehouseApi.remove(item.id)
    message.value = '仓库已删除'
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '删除仓库失败'
  }
}

const completeDrafts=useFormDrafts({showWarehouseForm:{open:showWarehouseForm,fields:{warehouseForm}, record:()=>(warehouseForm as Record<string,any>).id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">仓库库存</h1>
          <p class="text-gray-500 mt-1">管理仓库并查询物料库存、低库存预警。</p>
        </div>
        <button class="btn-primary" @click="openCreateWarehouse">
          <Plus class="w-4 h-4" />
          新增仓库
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="stat-card"><p class="stat-label">仓库数量</p><p class="stat-value">{{ warehouses.length }}</p></div>
        <div class="stat-card"><p class="stat-label">库存总数量</p><p class="stat-value text-blue-600">{{ totalQuantity }}</p></div>
        <div class="stat-card"><p class="stat-label">低库存记录</p><p class="stat-value text-red-600">{{ lowStockList.length }}</p></div>
      </div>

      <p v-if="message" class="text-sm text-blue-600">{{ message }}</p>

      <div class="grid grid-cols-1 xl:grid-cols-3 items-start gap-6">
        <div class="panel xl:col-span-2">
          <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3 mb-5">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">库存查询</h2>
              <p class="text-sm text-gray-500 mt-1">按物料、仓库、库位快速定位当前库存。</p>
            </div>
            <button class="btn-secondary" @click="loadAll"><RefreshCw class="w-4 h-4" />刷新</button>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-5">
            <select v-model="filters.materialId" class="input">
              <option value="">全部物料</option>
              <option v-for="item in materials" :key="item.id" :value="item.id">{{ item.materialName }}</option>
            </select>
            <select v-model="filters.warehouseId" class="input">
              <option value="">全部仓库</option>
              <option v-for="item in warehouses" :key="item.id" :value="item.id">{{ item.warehouseName }}</option>
            </select>
            <input v-model.number="filters.locationId" class="input" placeholder="库位ID（可空）" />
            <button class="btn-primary" @click="searchInventory"><Search class="w-4 h-4" />查询库存</button>
          </div>

          <div class="overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr>
                  <th>物料</th>
                  <th>仓库</th>
                  <th>库位ID</th>
                  <th>批次ID</th>
                  <th>数量</th>
                  <th>更新时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading"><td colspan="6" class="empty-cell">正在加载库存...</td></tr>
                <tr v-else-if="inventoryList.length === 0"><td colspan="6" class="empty-cell">暂无库存数据</td></tr>
                <tr v-for="item in pagedInventory" v-else :key="item.id">
                  <td>{{ materialName(item.materialId) }}</td>
                  <td>{{ warehouseName(item.warehouseId) }}</td>
                  <td>{{ item.locationId || '-' }}</td>
                  <td>{{ item.lotId || '-' }}</td>
                  <td class="font-semibold text-gray-900">{{ item.quantity }}</td>
                  <td>{{ item.lastUpdateTime ? new Date(item.lastUpdateTime).toLocaleString('zh-CN') : '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <nav class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-4 text-sm text-gray-500" aria-label="库存查询分页">
            <span aria-live="polite">共 {{ inventoryList.length }} 条<span v-if="inventoryList.length">，当前 {{ (inventoryPage - 1) * inventoryPageSize + 1 }}–{{ Math.min(inventoryPage * inventoryPageSize, inventoryList.length) }} 条</span></span>
            <div class="flex flex-wrap items-center gap-3">
              <label class="flex items-center gap-2">每页<select v-model.number="inventoryPageSize" :disabled="loading" class="rounded-lg border px-2 py-2" aria-label="库存每页条数"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label>
              <button type="button" class="btn-secondary" :disabled="loading || inventoryPage === 1" @click="inventoryPage--">上一页</button>
              <span>{{ inventoryPage }} / {{ inventoryPageCount }} 页</span>
              <button type="button" class="btn-secondary" :disabled="loading || inventoryPage >= inventoryPageCount" @click="inventoryPage++">下一页</button>
            </div>
          </nav>
        </div>

        <div class="space-y-6">
          <div class="panel">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold text-gray-900">仓库</h2>
              <button class="link-btn" @click="openCreateWarehouse">新增</button>
            </div>
            <div class="space-y-3 max-h-[420px] overflow-y-auto pr-1">
              <div v-if="warehouses.length === 0" class="text-gray-400 text-sm">暂无仓库数据</div>
              <div v-for="item in warehouses" v-else :key="item.id" class="rounded-xl border border-gray-100 bg-gray-50 p-4">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="font-semibold text-gray-900">{{ item.warehouseName }}</p>
                    <p class="text-xs text-gray-500 mt-1">{{ item.warehouseCode }}</p>
                  </div>
                  <span :class="item.status === 1 ? 'tag-green' : 'tag-gray'">{{ item.status === 1 ? '启用' : '停用' }}</span>
                </div>
                <p class="text-sm text-gray-500 mt-3">位置：{{ item.location || '-' }}</p>
                <p class="text-sm text-gray-500">负责人：{{ item.managerName || '-' }}</p>
                <div class="flex justify-end gap-3 mt-3">
                  <button class="link-btn" @click="openEditWarehouse(item)">修改</button>
                  <button class="danger-link" @click="removeWarehouse(item)">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div class="panel">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">低库存预警</h2>
            <div class="space-y-3 max-h-[360px] overflow-y-auto pr-1">
              <div v-if="lowStockList.length === 0" class="text-gray-400 text-sm">暂无低库存记录</div>
              <div v-for="item in lowStockList" v-else :key="item.id" class="p-4 rounded-xl bg-red-50 border border-red-100">
                <p class="font-semibold text-gray-900">{{ materialName(item.materialId) }}</p>
                <p class="text-sm text-gray-500 mt-1">{{ warehouseName(item.warehouseId) }}</p>
                <p class="text-sm text-red-600 mt-2">当前库存：{{ item.quantity }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showWarehouseForm" class="modal-mask" data-business-form>
        <div class="modal-card max-w-2xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">{{ editingWarehouseId ? '修改仓库' : '新增仓库' }}</h2>
            <button class="icon-btn" @click="showWarehouseForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">仓库编码<input v-model="warehouseForm.warehouseCode" class="input mt-1" /></label>
            <label class="field-label">仓库名称<input v-model="warehouseForm.warehouseName" class="input mt-1" /></label>
            <label class="field-label">位置<input v-model="warehouseForm.location" class="input mt-1" /></label>
            <label class="field-label">负责人<input v-model="warehouseForm.managerName" class="input mt-1" /></label>
            <label class="field-label">状态
              <select v-model.number="warehouseForm.status" class="input mt-1">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">备注<input v-model="warehouseForm.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showWarehouseForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveWarehouse">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

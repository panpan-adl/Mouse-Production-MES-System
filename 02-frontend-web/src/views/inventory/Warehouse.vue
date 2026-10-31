<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import {
  inventoryApi,
  materialApi,
  warehouseApi,
  type Inventory,
  type Material,
  type Warehouse,
} from '@/api/mes'

const warehouseTab=ref('inventory')
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const warehouses = ref<Warehouse[]>([])
const inventoryList = ref<Inventory[]>([])
const stockDetails=ref<Record<string,any>[]>([])
const locations=ref<Record<string,any>[]>([])
const thresholds=reactive<Record<number,number>>({})
const locationChoices=computed(()=>locations.value.filter(l=>!filters.warehouseId||Number(l.warehouse_id)===Number(filters.warehouseId)))

const lowStockList=computed(()=>materials.value.filter(m=>Number(m.safetyStock)>0).map(m=>{
 const quantity=stockDetails.value.filter(i=>Number(i.material_id)===Number(m.id)&&warehouses.value.some(w=>w.id===Number(i.warehouse_id)&&w.warehouseCode!=='WH-A100-LINE')).reduce((n,i)=>n+Number(i.quantity||0),0)
 return {id:m.id,materialId:m.id,quantity,threshold:Number(m.safetyStock),gap:Math.max(0,Number(m.safetyStock)-quantity),unit:m.unit||''}
}).filter(m=>m.quantity<m.threshold))
const stockDetail=(item:Inventory)=>stockDetails.value.find(r=>Number(r.id)===Number(item.id))
function storageName(item:Inventory){const l=locations.value.find(l=>Number(l.id)===Number(item.locationId));return l?[l.area_name,l.location_name||l.name].filter(Boolean).join(' / '):item.locationId?'库位 #'+item.locationId:'未指定库位'}
async function saveThreshold(m:Material){const value=Number(thresholds[Number(m.id)]);if(!Number.isFinite(value)||value<0){message.value='安全库存必须是大于等于 0 的数值';return}try{await materialApi.update(Number(m.id),{...m,safetyStock:value});message.value='安全库存阈值已保存';await loadAll()}catch(e:any){message.value=e.message||'阈值保存失败'}}
const materials = ref<Material[]>([])
const showWarehouseForm = ref(false)
const editingWarehouseId = ref<number | null>(null)

const filters = reactive({
  materialId: '' as number | '',
  warehouseId: '' as number | '',
  locationId: '' as number | '',
})

watch(()=>filters.warehouseId,()=>{filters.locationId=''})

const warehouseForm = reactive<Warehouse>({
  warehouseCode: '',
  warehouseName: '',
  location: '',
  managerName: '',
  status: 1,
  remark: '',
})

const onlyPositive=ref(true)
const visibleInventory=computed(()=>inventoryList.value.filter(i=>!onlyPositive.value||Number(i.quantity)>0))
const inventoryPage = ref(1)
const inventoryPageSize = ref(10)
const inventoryPageCount = computed(() => Math.max(1, Math.ceil(visibleInventory.value.length / inventoryPageSize.value)))
const pagedInventory = computed(() => visibleInventory.value.slice((inventoryPage.value - 1) * inventoryPageSize.value, inventoryPage.value * inventoryPageSize.value))
watch([inventoryPageSize,onlyPositive], () => { inventoryPage.value = 1 })
watch(inventoryPageCount, count => { inventoryPage.value = Math.min(inventoryPage.value, count) })

const stockedMaterialCount=computed(()=>new Set(inventoryList.value.filter(i=>Number(i.quantity)>0).map(item=>item.materialId)).size)
const quantityByUnit=computed(()=>{const groups=new Map<string,number>();for(const item of inventoryList.value){const unit=materials.value.find(m=>m.id===item.materialId)?.unit||'未标注单位';groups.set(unit,(groups.get(unit)||0)+Number(item.quantity||0))}return [...groups].map(([unit,quantity])=>({unit,quantity}))})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [warehouseData, materialData, inventoryData, stockData, locationData, areaData, foundationLocations] = await Promise.all([
      warehouseApi.list(),
      materialApi.list(),
      inventoryApi.search(filters),
      request.get<Record<string,any>[]>('/fulfillment/stock'),
      request.get<Record<string,any>[]>('/fulfillment/locations'),
      request.get<Record<string,any>[]>('/foundation/areas'),
      request.get<Record<string,any>[]>('/foundation/locations'),
    ])
    warehouses.value = warehouseData
    materials.value = materialData
    inventoryList.value = inventoryData
    inventoryPage.value = 1
    stockDetails.value=stockData;locations.value=locationData.map(l=>{const master=foundationLocations.find(m=>m.code===l.location_code);return {...l,warehouse_name:warehouseData.find(w=>w.id===Number(l.warehouse_id))?.warehouseName,area_name:areaData.find(a=>a.id===master?.area_id)?.name||'未配置库区'}});for(const m of materialData)thresholds[Number(m.id)]=Number(m.safetyStock||0)
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
          <h1 class="text-2xl font-bold text-gray-900">仓储库存工作台</h1>
          <p class="text-gray-500 mt-1">先定位库存，再处理预警；仓库维护单独办理，不与库存查询混排。</p>
        </div>
        <button class="btn-primary" @click="openCreateWarehouse">
          <Plus class="w-4 h-4" />
          新增仓库
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="stat-card"><p class="stat-label">仓库数量</p><p class="stat-value">{{ warehouses.length }}</p></div>
        <div class="stat-card"><p class="stat-label">当前范围物料种类</p><p class="stat-value text-blue-600">{{ stockedMaterialCount }}</p></div>
        <div class="stat-card"><p class="stat-label">全库低库存记录</p><p class="stat-value text-red-600">{{ lowStockList.length }}</p></div>
      </div>

      <p v-if="message" class="text-sm text-blue-600">{{ message }}</p>

      <nav class="module-view-tabs" aria-label="仓储工作区"><button v-for="tab in [{key:'inventory',label:'库存明细'},{key:'alerts',label:'低库存预警'},{key:'configuration',label:'仓库维护'}]" :key="tab.key" :aria-pressed="warehouseTab===tab.key" @click="warehouseTab=tab.key">{{tab.label}}</button></nav>
      <div class="warehouse-work-area">
        <div v-if="warehouseTab==='inventory'" class="panel">
          <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3 mb-5">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">库存查询</h2>
              <p class="text-sm text-gray-500 mt-1">按物料、仓库、库位定位当前余额；下方数量按单位分别汇总。</p>
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
            <select v-model="filters.locationId" class="input" aria-label="按库位筛选"><option value="">全部库位</option><option v-for="l in locationChoices" :key="l.id" :value="l.id">{{l.warehouse_name}} / {{l.area_name}} / {{l.location_name||l.name}}</option></select>
            <button class="btn-primary" @click="searchInventory"><Search class="w-4 h-4" />查询库存</button>
          </div>

          <label class="mb-3 flex items-center gap-2 text-sm text-gray-500"><input v-model="onlyPositive" type="checkbox" />仅显示有余额的库存（取消可查看已用完批次）</label><div v-if="quantityByUnit.length" class="quantity-summary"><span v-for="group in quantityByUnit" :key="group.unit">{{group.quantity.toLocaleString()}} <small>{{group.unit}}</small></span><small>当前查询范围 · 不同单位不合计</small></div><div class="overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr>
                  <th>物料</th>
                  <th>仓库</th>
                  <th>库区 / 库位</th>
                  <th>来源批次</th>
                  <th>数量</th>
                  <th>更新时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading"><td colspan="6" class="empty-cell">正在加载库存...</td></tr>
                <tr v-else-if="visibleInventory.length === 0"><td colspan="6" class="empty-cell">暂无库存数据</td></tr>
                <tr v-for="item in pagedInventory" v-else :key="item.id">
                  <td>{{ materialName(item.materialId) }}</td>
                  <td>{{ warehouseName(item.warehouseId) }}</td>
                  <td>{{ storageName(item) }}</td>
                  <td>{{ stockDetail(item)?.lot_no || (item.lotId ? '批次 #'+item.lotId : '未分配批次') }}</td>
                  <td class="font-semibold text-gray-900">{{ item.quantity }} {{ materials.find(m=>m.id===item.materialId)?.unit }}</td>
                  <td>{{ item.lastUpdateTime ? new Date(item.lastUpdateTime).toLocaleString('zh-CN') : '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <nav class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-4 text-sm text-gray-500" aria-label="库存查询分页">
            <span aria-live="polite">共 {{ visibleInventory.length }} 条<span v-if="visibleInventory.length">，当前 {{ (inventoryPage - 1) * inventoryPageSize + 1 }}–{{ Math.min(inventoryPage * inventoryPageSize, visibleInventory.length) }} 条</span></span>
            <div class="flex flex-wrap items-center gap-3">
              <label class="flex items-center gap-2">每页<select v-model.number="inventoryPageSize" :disabled="loading" class="rounded-lg border px-2 py-2" aria-label="库存每页条数"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label>
              <button type="button" class="btn-secondary" :disabled="loading || inventoryPage === 1" @click="inventoryPage--">上一页</button>
              <span>{{ inventoryPage }} / {{ inventoryPageCount }} 页</span>
              <button type="button" class="btn-secondary" :disabled="loading || inventoryPage >= inventoryPageCount" @click="inventoryPage++">下一页</button>
            </div>
          </nav>
        </div>

        <div class="space-y-6">
          <div v-if="warehouseTab==='configuration'" class="panel">
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

          <div v-if="warehouseTab==='alerts'" class="panel">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">低库存预警</h2><p class="text-sm text-gray-500 mb-4">按物料汇总各实体仓库余额（不含工单线边库）；当前数量小于安全库存时预警。阈值为 0 时不启用该物料预警。</p>
            <details class="mb-5 rounded-xl border p-4"><summary class="cursor-pointer font-semibold">配置物料安全库存阈值</summary><div class="overflow-x-auto mt-3"><table class="data-table"><thead><tr><th>物料</th><th>安全库存（小于此值预警）</th><th>操作</th></tr></thead><tbody><tr v-for="m in materials" :key="m.id"><td>{{m.materialName}} · {{m.unit}}</td><td><input v-model.number="thresholds[Number(m.id)]" type="number" min="0" step="0.01" class="input" :aria-label="m.materialName+'安全库存'" /></td><td><button class="link-btn" @click="saveThreshold(m)">保存阈值</button></td></tr></tbody></table></div></details>
            <div class="space-y-3 max-h-[360px] overflow-y-auto pr-1">
              <div v-if="lowStockList.length === 0" class="text-gray-400 text-sm">暂无低库存记录</div>
              <div v-for="item in lowStockList" v-else :key="item.id" class="p-4 rounded-xl bg-red-50 border border-red-100">
                <p class="font-semibold text-gray-900">{{ materialName(item.materialId) }}</p>
                <p class="text-sm text-gray-500 mt-1">实体仓库合计 · 安全阈值 {{item.threshold}} {{item.unit}}</p>
                <p class="text-sm text-red-600 mt-2">当前库存：{{ item.quantity }} {{item.unit}} · 缺口：{{item.gap}} {{item.unit}}</p><button class="link-btn mt-3" @click="filters.materialId=item.materialId||'';filters.warehouseId='';filters.locationId='';warehouseTab='inventory';searchInventory()">核对此物料库存</button>
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

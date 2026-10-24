<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { materialApi, type Material } from '@/api/mes'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const materials = ref<Material[]>([])
const keyword = ref('')
const typeFilter = ref('')
const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<Material>({
  materialCode: '',
  materialName: '',
  materialType: 1,
  specification: '',
  unit: '个',
  safetyStock: 0,
  currentStock: 0,
  status: 1,
  remark: '',
})

const filteredMaterials = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return materials.value.filter((item) => {
    const matchWord = !word || item.materialCode?.toLowerCase().includes(word) || item.materialName?.toLowerCase().includes(word)
    const matchType = !typeFilter.value || String(item.materialType || '') === typeFilter.value
    return matchWord && matchType
  })
})

const lowStockCount = computed(() =>
  materials.value.filter((item) => Number(item.currentStock || 0) < Number(item.safetyStock || 0)).length
)

onMounted(loadMaterials)

async function loadMaterials() {
  loading.value = true
  message.value = ''
  try {
    materials.value = await materialApi.list()
  } catch (error: any) {
    message.value = error.message || '物料加载失败'
  } finally {
    loading.value = false
  }
}

function typeText(type?: number) {
  const map: Record<number, string> = { 1: '原材料', 2: '半成品', 3: '成品' }
  return map[type || 0] || '其他'
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    materialCode: `MAT-${Date.now()}`,
    materialName: '',
    materialType: 1,
    specification: '',
    unit: '个',
    safetyStock: 0,
    currentStock: 0,
    status: 1,
    remark: '',
  })
}

function openCreate() {
  resetForm()
  showForm.value = true
}

function openEdit(item: Material) {
  editingId.value = item.id || null
  Object.assign(form, item)
  showForm.value = true
}

async function saveMaterial() {
  if (!form.materialCode || !form.materialName) {
    message.value = '物料编码和名称不能为空'
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await materialApi.update(editingId.value, form)
      message.value = '物料已更新'
    } else {
      await materialApi.create(form)
      message.value = '物料已新增'
    }
    completeDrafts();showForm.value=false
    await loadMaterials()
  } catch (error: any) {
    message.value = error.message || '保存物料失败'
  } finally {
    saving.value = false
  }
}

async function removeMaterial(item: Material) {
  if (!item.id || !window.confirm(`确认删除物料「${item.materialName}」吗？`)) return
  try {
    await materialApi.remove(item.id)
    message.value = '物料已删除'
    await loadMaterials()
  } catch (error: any) {
    message.value = error.message || '删除物料失败'
  }
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id || editingId.value}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">物料管理</h1>
          <p class="text-gray-500 mt-1">维护物料基础资料、安全库存和当前库存。</p>
        </div>
        <button class="btn-primary" @click="openCreate">
          <Plus class="w-4 h-4" />
          新增物料
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="stat-card"><p class="stat-label">物料总数</p><p class="stat-value">{{ materials.length }}</p></div>
        <div class="stat-card"><p class="stat-label">原材料</p><p class="stat-value text-blue-600">{{ materials.filter((i) => i.materialType === 1).length }}</p></div>
        <div class="stat-card"><p class="stat-label">成品/半成品</p><p class="stat-value text-green-600">{{ materials.filter((i) => i.materialType !== 1).length }}</p></div>
        <div class="stat-card"><p class="stat-label">低库存</p><p class="stat-value text-red-600">{{ lowStockCount }}</p></div>
      </div>

      <div class="panel">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-5">
          <div class="relative lg:col-span-2">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索物料编码或名称" />
          </div>
          <select v-model="typeFilter" class="input">
            <option value="">全部类型</option>
            <option value="1">原材料</option>
            <option value="2">半成品</option>
            <option value="3">成品</option>
          </select>
          <button class="btn-secondary" @click="loadMaterials"><RefreshCw class="w-4 h-4" />刷新</button>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>类型</th>
                <th>规格</th>
                <th>单位</th>
                <th>安全库存</th>
                <th>当前库存</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="9" class="empty-cell">正在加载物料...</td></tr>
              <tr v-else-if="filteredMaterials.length === 0"><td colspan="9" class="empty-cell">暂无物料数据</td></tr>
              <tr v-for="item in filteredMaterials" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.materialCode }}</td>
                <td>{{ item.materialName }}</td>
                <td>{{ typeText(item.materialType) }}</td>
                <td>{{ item.specification || '-' }}</td>
                <td>{{ item.unit || '-' }}</td>
                <td>{{ item.safetyStock || 0 }}</td>
                <td :class="Number(item.currentStock || 0) < Number(item.safetyStock || 0) ? 'text-red-600 font-semibold' : 'text-gray-900 font-semibold'">
                  {{ item.currentStock || 0 }}
                </td>
                <td><span :class="item.status === 1 ? 'tag-green' : 'tag-gray'">{{ item.status === 1 ? '启用' : '停用' }}</span></td>
                <td class="text-right space-x-2">
                  <button class="link-btn" @click="openEdit(item)">修改</button>
                  <button class="danger-link" @click="removeMaterial(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask">
        <div class="modal-card max-w-2xl">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">{{ editingId ? '修改物料' : '新增物料' }}</h2>
            <button class="icon-btn" @click="showForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">物料编码<input v-model="form.materialCode" class="input mt-1" /></label>
            <label class="field-label">物料名称<input v-model="form.materialName" class="input mt-1" /></label>
            <label class="field-label">类型
              <select v-model.number="form.materialType" class="input mt-1">
                <option :value="1">原材料</option>
                <option :value="2">半成品</option>
                <option :value="3">成品</option>
              </select>
            </label>
            <label class="field-label">单位<input v-model="form.unit" class="input mt-1" /></label>
            <label class="field-label">规格<input v-model="form.specification" class="input mt-1" /></label>
            <label class="field-label">安全库存<input v-model.number="form.safetyStock" type="number" class="input mt-1" /></label>
            <label class="field-label">当前库存<input v-model.number="form.currentStock" type="number" class="input mt-1" /></label>
            <label class="field-label">状态
              <select v-model.number="form.status" class="input mt-1">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">备注<input v-model="form.remark" class="input mt-1" /></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveMaterial">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

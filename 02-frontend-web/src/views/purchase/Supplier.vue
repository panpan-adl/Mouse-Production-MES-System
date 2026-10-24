<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Plus, RefreshCw, Search, X } from 'lucide-vue-next'
import { supplierApi, type Supplier } from '@/api/mes'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const suppliers = ref<Supplier[]>([])
const showForm = ref(false)
const editingId = ref<number | null>(null)

const filters = reactive({
  supplierCode: '',
  supplierName: '',
  status: '' as '' | 0 | 1,
})

const form = reactive<Supplier>({
  supplierCode: '',
  supplierName: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  bankName: '',
  bankAccount: '',
  status: 1,
  remark: '',
})

const activeCount = computed(() => suppliers.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => suppliers.value.filter((item) => item.status === 0).length)

onMounted(loadSuppliers)

async function loadSuppliers() {
  loading.value = true
  message.value = ''
  try {
    const params = {
      supplierCode: filters.supplierCode || undefined,
      supplierName: filters.supplierName || undefined,
      status: filters.status,
    }
    suppliers.value = await supplierApi.search(params)
  } catch (error: any) {
    message.value = error.message || '供应商加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    supplierCode: '',
    supplierName: '',
    contactPerson: '',
    contactPhone: '',
    address: '',
    bankName: '',
    bankAccount: '',
    status: 1,
    remark: '',
  })
}

function openCreate() {
  resetForm()
  showForm.value = true
}

function openEdit(item: Supplier) {
  editingId.value = item.id || null
  Object.assign(form, { ...item })
  showForm.value = true
}

async function saveSupplier() {
  if (!form.supplierCode || !form.supplierName) {
    message.value = '供应商编码和名称不能为空'
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await supplierApi.update(editingId.value, form)
      message.value = '供应商已更新'
    } else {
      await supplierApi.create(form)
      message.value = '供应商已新增'
    }
    showForm.value = false
    await loadSuppliers()
  } catch (error: any) {
    message.value = error.message || '保存供应商失败'
  } finally {
    saving.value = false
  }
}

async function toggleStatus(item: Supplier) {
  if (!item.id) return
  try {
    if (item.status === 1) {
      await supplierApi.disable(item.id)
      message.value = '供应商已禁用'
    } else {
      await supplierApi.enable(item.id)
      message.value = '供应商已启用'
    }
    await loadSuppliers()
  } catch (error: any) {
    message.value = error.message || '状态修改失败'
  }
}

async function removeSupplier(item: Supplier) {
  if (!item.id || !window.confirm(`确认删除供应商「${item.supplierName}」吗？`)) return
  try {
    await supplierApi.remove(item.id)
    message.value = '供应商已删除'
    await loadSuppliers()
  } catch (error: any) {
    message.value = error.message || '删除供应商失败'
  }
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">供应商管理</h1>
          <p class="text-gray-500 mt-1">维护供应商档案，支持按编码、名称和状态组合查询。</p>
        </div>
        <button class="btn-primary" @click="openCreate">
          <Plus class="w-4 h-4" />
          新增供应商
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="stat-card">
          <p class="stat-label">供应商总数</p>
          <p class="stat-value">{{ suppliers.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">活跃供应商</p>
          <p class="stat-value text-green-600">{{ activeCount }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">禁用供应商</p>
          <p class="stat-value text-gray-500">{{ disabledCount }}</p>
        </div>
      </div>

      <div class="panel">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-5">
          <div class="relative">
            <Search class="search-icon" />
            <input v-model="filters.supplierCode" class="input pl-10" placeholder="供应商编码" @keyup.enter="loadSuppliers" />
          </div>
          <input v-model="filters.supplierName" class="input" placeholder="供应商名称" @keyup.enter="loadSuppliers" />
          <select v-model="filters.status" class="input">
            <option value="">全部状态</option>
            <option :value="1">活跃</option>
            <option :value="0">禁用</option>
          </select>
          <div class="flex gap-2">
            <button class="btn-secondary flex-1" @click="loadSuppliers">
              <Search class="w-4 h-4" />
              查询
            </button>
            <button class="btn-ghost" @click="filters.supplierCode = ''; filters.supplierName = ''; filters.status = ''; loadSuppliers()">
              <RefreshCw class="w-4 h-4" />
            </button>
          </div>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>联系人</th>
                <th>电话</th>
                <th>地址</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="empty-cell">正在加载供应商...</td>
              </tr>
              <tr v-else-if="suppliers.length === 0">
                <td colspan="7" class="empty-cell">暂无供应商数据</td>
              </tr>
              <tr v-for="item in suppliers" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.supplierCode }}</td>
                <td>{{ item.supplierName }}</td>
                <td>{{ item.contactPerson || '-' }}</td>
                <td>{{ item.contactPhone || '-' }}</td>
                <td>{{ item.address || '-' }}</td>
                <td>
                  <span :class="item.status === 1 ? 'tag-green' : 'tag-gray'">
                    {{ item.status === 1 ? '活跃' : '禁用' }}
                  </span>
                </td>
                <td class="text-right space-x-2">
                  <button class="link-btn" @click="openEdit(item)">修改</button>
                  <button class="link-btn" @click="toggleStatus(item)">{{ item.status === 1 ? '禁用' : '启用' }}</button>
                  <button class="danger-link" @click="removeSupplier(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask">
        <div class="modal-card">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">{{ editingId ? '修改供应商' : '新增供应商' }}</h2>
            <button class="icon-btn" @click="showForm = false"><X class="w-5 h-5" /></button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="field-label">供应商编码<input v-model="form.supplierCode" class="input mt-1" /></label>
            <label class="field-label">供应商名称<input v-model="form.supplierName" class="input mt-1" /></label>
            <label class="field-label">联系人<input v-model="form.contactPerson" class="input mt-1" /></label>
            <label class="field-label">联系电话<input v-model="form.contactPhone" class="input mt-1" /></label>
            <label class="field-label md:col-span-2">地址<input v-model="form.address" class="input mt-1" /></label>
            <label class="field-label">开户行<input v-model="form.bankName" class="input mt-1" /></label>
            <label class="field-label">银行账号<input v-model="form.bankAccount" class="input mt-1" /></label>
            <label class="field-label">状态
              <select v-model="form.status" class="input mt-1">
                <option :value="1">活跃</option>
                <option :value="0">禁用</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">备注<textarea v-model="form.remark" class="input mt-1 h-20"></textarea></label>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveSupplier">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

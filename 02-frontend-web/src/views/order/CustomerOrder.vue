<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Edit3, Eye, Plus, RefreshCw, Search, Trash2, X } from 'lucide-vue-next'
import { customerApi, type Customer } from '@/api/mes'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const customers = ref<Customer[]>([])
const showForm = ref(false)
const showDetail = ref(false)
const editingId = ref<number | null>(null)
const selectedCustomer = ref<Customer | null>(null)

const filters = reactive({
  customerCode: '',
  customerName: '',
  status: '' as '' | '1' | '0',
})

const form = reactive<Customer>({
  customerCode: '',
  customerName: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  status: '1',
  isDeleted: 0,
})

function normalizeStatus(status?: string | number) {
  const value = String(status ?? '').trim().toUpperCase()
  if (['1', 'ACTIVE', 'ENABLE', 'ENABLED', 'NORMAL'].includes(value)) return '1'
  if (['0', 'INACTIVE', 'DISABLE', 'DISABLED', 'STOP'].includes(value)) return '0'
  return value || '1'
}

function isActive(customer: Customer) {
  return normalizeStatus(customer.status) === '1'
}

const sortedCustomers = computed(() => {
  return [...customers.value].sort((a, b) => {
    const statusWeight = (item: Customer) => (isActive(item) ? 0 : 1)
    if (statusWeight(a) !== statusWeight(b)) return statusWeight(a) - statusWeight(b)
    return (a.customerCode || '').localeCompare(b.customerCode || '')
  })
})

const filteredCustomers = computed(() => {
  const code = filters.customerCode.trim().toLowerCase()
  const name = filters.customerName.trim().toLowerCase()
  return sortedCustomers.value.filter((item) => {
    const matchCode = !code || item.customerCode?.toLowerCase().includes(code)
    const matchName = !name || item.customerName?.toLowerCase().includes(name)
    const matchStatus = !filters.status || normalizeStatus(item.status) === filters.status
    return matchCode && matchName && matchStatus
  })
})

const activeCount = computed(() => customers.value.filter((item) => isActive(item)).length)
const inactiveCount = computed(() => customers.value.filter((item) => !isActive(item)).length)

onMounted(loadCustomers)

async function loadCustomers() {
  loading.value = true
  message.value = ''
  try {
    customers.value = await customerApi.list()
  } catch (error: any) {
    message.value = error.message || '客户数据加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    customerCode: '',
    customerName: '',
    contactPerson: '',
    phone: '',
    email: '',
    address: '',
    status: '1',
    isDeleted: 0,
  })
}

function openCreate() {
  resetForm()
  showForm.value = true
}

function openEdit(customer: Customer) {
  editingId.value = customer.id || null
  Object.assign(form, {
    customerCode: customer.customerCode || '',
    customerName: customer.customerName || '',
    contactPerson: customer.contactPerson || '',
    phone: customer.phone || '',
    email: customer.email || '',
    address: customer.address || '',
    status: normalizeStatus(customer.status),
    isDeleted: customer.isDeleted ?? 0,
  })
  showForm.value = true
}

function openDetail(customer: Customer) {
  selectedCustomer.value = customer
  showDetail.value = true
}

function clearFilters() {
  filters.customerCode = ''
  filters.customerName = ''
  filters.status = ''
}

function buildPayload(): Customer {
  return {
    customerCode: form.customerCode.trim(),
    customerName: form.customerName.trim(),
    contactPerson: form.contactPerson?.trim(),
    phone: form.phone?.trim(),
    email: form.email?.trim(),
    address: form.address?.trim(),
    status: normalizeStatus(form.status),
    isDeleted: form.isDeleted ?? 0,
  }
}

async function saveCustomer() {
  if (!form.customerCode.trim() || !form.customerName.trim()) {
    message.value = '客户编码和客户名称不能为空'
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) {
      await customerApi.update(editingId.value, payload)
      message.value = '客户信息已更新'
    } else {
      await customerApi.create(payload)
      message.value = '客户已新增'
    }
    completeDrafts();showForm.value=false
    await loadCustomers()
  } catch (error: any) {
    message.value = error.message || '客户保存失败'
  } finally {
    saving.value = false
  }
}

async function toggleStatus(customer: Customer) {
  if (!customer.id) return
  const nextStatus = isActive(customer) ? '0' : '1'
  try {
    await customerApi.update(customer.id, {
      ...customer,
      status: nextStatus,
      isDeleted: customer.isDeleted ?? 0,
    })
    message.value = nextStatus === '1' ? '客户已恢复活跃' : '客户已停用'
    await loadCustomers()
  } catch (error: any) {
    message.value = error.message || '客户状态修改失败'
  }
}

async function removeCustomer(customer: Customer) {
  if (!customer.id) return
  if (!window.confirm(`确认删除客户“${customer.customerName}”吗？`)) return
  try {
    await customerApi.remove(customer.id)
    message.value = '客户已删除'
    await loadCustomers()
  } catch (error: any) {
    message.value = error.message || '客户删除失败'
  }
}

function statusClass(status?: string) {
  return normalizeStatus(status) === '1' ? 'tag-green' : 'tag-gray'
}

function statusLabel(status?: string) {
  return normalizeStatus(status) === '1' ? '活跃' : '停用'
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id || editingId.value}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm text-gray-500">销售基础资料</p>
          <h1 class="mt-1 text-2xl font-bold text-gray-900">客户管理</h1>
        </div>
        <button class="btn-primary" @click="openCreate">
          <Plus class="h-4 w-4" />
          新增客户
        </button>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <button class="stat-card text-left" @click="filters.status = ''">
          <p class="stat-label">客户总数</p>
          <p class="stat-value">{{ customers.length }}</p>
        </button>
        <button class="stat-card text-left" @click="filters.status = '1'">
          <p class="stat-label">活跃客户</p>
          <p class="stat-value text-green-600">{{ activeCount }}</p>
        </button>
        <button class="stat-card text-left" @click="filters.status = '0'">
          <p class="stat-label">停用客户</p>
          <p class="stat-value text-gray-500">{{ inactiveCount }}</p>
        </button>
      </div>

      <section class="panel">
        <div class="mb-5 overflow-x-auto pb-1">
          <div class="grid min-w-[820px] grid-cols-[190px_minmax(280px,1fr)_180px_92px_92px] items-center gap-3">
          <label class="relative">
            <Search class="search-icon" />
            <input v-model="filters.customerCode" class="input pl-10" placeholder="客户编码" @keyup.enter="loadCustomers" />
          </label>
          <input v-model="filters.customerName" class="input" placeholder="客户名称" @keyup.enter="loadCustomers" />
          <select v-model="filters.status" class="input">
            <option value="">客户状态：全部</option>
            <option value="1">客户状态：活跃</option>
            <option value="0">客户状态：停用</option>
          </select>
          <button class="btn-secondary w-full" @click="message = '已按当前条件筛选'">
            <Search class="h-4 w-4" />
            查询
          </button>
          <button class="btn-secondary w-full" title="刷新列表，不清空筛选条件" @click="loadCustomers">
            <RefreshCw class="h-4 w-4" />
            刷新
          </button>
          </div>
        </div>
        <div v-if="filters.customerCode || filters.customerName || filters.status" class="mb-5">
          <button class="btn-ghost" @click="clearFilters">
            <X class="h-4 w-4" />
            清空筛选
          </button>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[920px]">
            <thead>
              <tr>
                <th>客户编码</th>
                <th>客户名称</th>
                <th>联系人</th>
                <th>电话</th>
                <th>邮箱</th>
                <th>状态</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="empty-cell">正在加载客户...</td>
              </tr>
              <tr v-else-if="filteredCustomers.length === 0">
                <td colspan="8" class="empty-cell">暂无符合条件的客户</td>
              </tr>
              <tr v-for="item in filteredCustomers" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.customerCode }}</td>
                <td>{{ item.customerName }}</td>
                <td>{{ item.contactPerson || '-' }}</td>
                <td>{{ item.phone || '-' }}</td>
                <td>{{ item.email || '-' }}</td>
                <td>
                  <span :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
                </td>
                <td>{{ formatDate(item.updatedAt) }}</td>
                <td class="text-right">
                  <div class="flex justify-end gap-3">
                    <button class="link-btn inline-flex items-center gap-1" @click="openDetail(item)">
                      <Eye class="h-4 w-4" />
                      查看
                    </button>
                    <button class="link-btn inline-flex items-center gap-1" @click="openEdit(item)">
                      <Edit3 class="h-4 w-4" />
                      修改
                    </button>
                    <button class="link-btn" @click="toggleStatus(item)">
                      {{ isActive(item) ? '停用' : '启用' }}
                    </button>
                    <button class="danger-link inline-flex items-center gap-1" @click="removeCustomer(item)">
                      <Trash2 class="h-4 w-4" />
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <section class="modal-card">
          <div class="mb-5 flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">客户档案</p>
              <h2 class="text-lg font-semibold text-gray-900">{{ editingId ? '修改客户' : '新增客户' }}</h2>
            </div>
            <button class="icon-btn" aria-label="关闭" @click="showForm = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="field-label">
              客户编码
              <input v-model="form.customerCode" class="input mt-1" placeholder="例如 CUS-0001" />
            </label>
            <label class="field-label">
              客户名称
              <input v-model="form.customerName" class="input mt-1" placeholder="客户公司名称" />
            </label>
            <label class="field-label">
              联系人
              <input v-model="form.contactPerson" class="input mt-1" />
            </label>
            <label class="field-label">
              联系电话
              <input v-model="form.phone" class="input mt-1" />
            </label>
            <label class="field-label">
              邮箱
              <input v-model="form.email" type="email" class="input mt-1" />
            </label>
            <label class="field-label">
              状态
              <select v-model="form.status" class="input mt-1">
                <option value="1">活跃</option>
                <option value="0">停用</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">
              地址
              <input v-model="form.address" class="input mt-1" />
            </label>
          </div>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveCustomer">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </section>
      </div>

      <div v-if="showDetail && selectedCustomer" class="modal-mask" @click.self="showDetail = false">
        <section class="modal-card max-w-2xl">
          <div class="mb-5 flex items-start justify-between">
            <div>
              <p class="text-sm text-gray-500">客户详情</p>
              <h2 class="mt-1 text-xl font-bold text-gray-900">{{ selectedCustomer.customerName }}</h2>
            </div>
            <button class="icon-btn" aria-label="关闭" @click="showDetail = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="grid grid-cols-1 gap-4 text-sm md:grid-cols-2">
            <div>
              <p class="text-gray-500">客户编码</p>
              <p class="mt-1 font-medium text-gray-900">{{ selectedCustomer.customerCode }}</p>
            </div>
            <div>
              <p class="text-gray-500">合作状态</p>
              <p class="mt-1">
                <span :class="statusClass(selectedCustomer.status)">{{ statusLabel(selectedCustomer.status) }}</span>
              </p>
            </div>
            <div>
              <p class="text-gray-500">联系人</p>
              <p class="mt-1 font-medium text-gray-900">{{ selectedCustomer.contactPerson || '-' }}</p>
            </div>
            <div>
              <p class="text-gray-500">联系电话</p>
              <p class="mt-1 font-medium text-gray-900">{{ selectedCustomer.phone || '-' }}</p>
            </div>
            <div>
              <p class="text-gray-500">邮箱</p>
              <p class="mt-1 font-medium text-gray-900">{{ selectedCustomer.email || '-' }}</p>
            </div>
            <div>
              <p class="text-gray-500">更新时间</p>
              <p class="mt-1 font-medium text-gray-900">{{ formatDate(selectedCustomer.updatedAt) }}</p>
            </div>
            <div class="md:col-span-2">
              <p class="text-gray-500">地址</p>
              <p class="mt-1 font-medium text-gray-900">{{ selectedCustomer.address || '-' }}</p>
            </div>
          </div>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="showDetail = false">关闭</button>
            <button class="btn-primary" @click="openEdit(selectedCustomer); showDetail = false">修改客户</button>
          </div>
        </section>
      </div>
    </div>
  </MainLayout>
</template>

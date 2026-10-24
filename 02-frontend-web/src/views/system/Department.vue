<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Building2, Edit3, Plus, RefreshCw, Search, Trash2, Users, X } from 'lucide-vue-next'
import { departmentApi, type Department } from '@/api/department'
import { getUserList, type User } from '@/api/user'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const keyword = ref('')
const departments = ref<Department[]>([])
const users = ref<User[]>([])
const showForm = ref(false)
const editingDepartment = ref<Department | null>(null)

const form = reactive<Department>({
  parentId: 0,
  deptCode: '',
  deptName: '',
  leaderId: null,
  leaderName: '',
  sort: 0,
  status: 1,
})

/** 部门负责人候选规则：账号前三位现在表示部门，负责人资格必须按角色判断。 */
const departmentLeaderRoleIds: Record<string, number[]> = {
  '109': [9],
  '101': [1],
  '104': [4],
  '105': [5],
  '106': [6],
  '107': [7],
  '108': [8],
  '110': [10],
  '111': [11],
}

const roleNameById: Record<number, string> = {
  1: '生产经理',
  4: '质检员',
  5: '仓管员',
  6: '采购专员',
  7: '销售专员',
  8: '设备维护员',
  9: 'CEO',
  10: 'CFO',
  11: '人力经理',
}

const activeUsers = computed(() => {
  return users.value
    .filter((user) => user.deleted !== 1 && user.status !== 0)
    .sort((a, b) => Number(a.id || 0) - Number(b.id || 0))
})

const leaderCandidates = computed(() => {
  const department = editingDepartment.value
  if (!department?.id) return []

  const roleIds = getLeaderRoleIds(department)
  if (roleIds.length === 0) return []

  return activeUsers.value.filter((user) => {
    const sameDepartment = Number(user.deptId) === Number(department.id)
    return sameDepartment && roleIds.includes(Number(user.roleId))
  })
})

const filteredDepartments = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return departments.value.filter((item) => {
    return !word ||
      item.deptCode?.toLowerCase().includes(word) ||
      item.deptName?.toLowerCase().includes(word) ||
      item.leaderName?.toLowerCase().includes(word)
  })
})

const staffedDeptCount = computed(() => departments.value.filter((item) => item.leaderName).length)
const emptyLeaderCount = computed(() => departments.value.length - staffedDeptCount.value)

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    const [deptData, userData] = await Promise.all([
      departmentApi.list(),
      getUserList(),
    ])
    departments.value = deptData
    users.value = userData
  } catch (error: any) {
    message.value = error.message || '部门数据加载失败'
  } finally {
    loading.value = false
  }
}

function openCreateForm() {
  editingDepartment.value = null
  Object.assign(form, {
    id: undefined,
    parentId: 0,
    deptCode: '',
    deptName: '',
    leaderId: null,
    leaderName: '',
    sort: departments.value.length + 1,
    status: 1,
  })
  showForm.value = true
}

function openEditForm(item: Department) {
  editingDepartment.value = item
  Object.assign(form, {
    id: item.id,
    parentId: 0,
    deptCode: item.deptCode || '',
    deptName: item.deptName || '',
    leaderId: item.leaderId || null,
    leaderName: item.leaderName || '',
    sort: item.sort || 0,
    status: item.status ?? 1,
  })

  const leaderStillAllowed = leaderCandidates.value.some((user) => user.id === form.leaderId)
  if (form.leaderId && !leaderStillAllowed) {
    form.leaderId = null
    form.leaderName = ''
    message.value = '原负责人不符合该部门负责人角色规则，请重新选择'
  }

  showForm.value = true
}

async function saveDepartment() {
  if (!form.deptCode.trim() || !form.deptName.trim()) {
    message.value = '部门编码和部门名称不能为空'
    return
  }

  const selectedLeader = leaderCandidates.value.find((user) => user.id === form.leaderId)
  if (form.leaderId && !selectedLeader) {
    message.value = '部门负责人必须选择本部门中符合负责人角色的员工'
    return
  }

  const payload: Department = {
    parentId: 0,
    deptCode: form.deptCode.trim(),
    deptName: form.deptName.trim(),
    leaderId: selectedLeader ? selectedLeader.id : null,
    leaderName: selectedLeader ? selectedLeader.realName : '',
    sort: Number(form.sort || 0),
    status: Number(form.status ?? 1),
  }

  saving.value = true
  try {
    if (editingDepartment.value?.id) {
      await departmentApi.update(editingDepartment.value.id, payload)
      message.value = '部门信息已修改'
    } else {
      await departmentApi.create(payload)
      message.value = '部门已新增。新部门创建后，请先在员工管理中分配员工，再设置负责人。'
    }
    completeDrafts();showForm.value=false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '部门保存失败'
  } finally {
    saving.value = false
  }
}

async function disableDepartment(item: Department) {
  if (!item.id) return
  const confirmed = window.confirm(`确定要停用部门“${item.deptName}”吗？停用后该部门不会出现在部门列表中。`)
  if (!confirmed) return

  try {
    await departmentApi.remove(item.id)
    message.value = '部门已停用'
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '部门停用失败'
  }
}

function employeeCount(deptId?: number) {
  if (!deptId) return 0
  return activeUsers.value.filter((user) => Number(user.deptId) === Number(deptId)).length
}

function getLeaderRoleIds(department?: Department | null) {
  const deptCode = String(department?.deptCode || '').trim().toUpperCase()
  return departmentLeaderRoleIds[deptCode] || []
}

function leaderRuleText(department?: Department | null) {
  const roleIds = getLeaderRoleIds(department)
  if (roleIds.length === 0) return '该部门暂无配置负责人角色'
  return `负责人角色：${roleIds.map((id) => roleNameById[id] || `角色${id}`).join('、')}`
}


const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">部门管理</h1>
          <p class="mt-1 text-gray-500">管理公司部门。负责人必须属于本部门，并且角色编码匹配部门负责人规则。</p>
        </div>
        <button class="btn-primary" @click="openCreateForm">
          <Plus class="h-4 w-4" />
          新建部门
        </button>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50">
              <Building2 class="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <p class="stat-label">启用部门</p>
              <p class="stat-value">{{ departments.length }}</p>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50">
              <Users class="h-5 w-5 text-indigo-600" />
            </div>
            <div>
              <p class="stat-label">已配置负责人</p>
              <p class="stat-value text-indigo-600">{{ staffedDeptCount }}</p>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50">
              <Users class="h-5 w-5 text-amber-600" />
            </div>
            <div>
              <p class="stat-label">未配置负责人</p>
              <p class="stat-value text-amber-600">{{ emptyLeaderCount }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="mb-5 grid grid-cols-1 gap-3 lg:grid-cols-[minmax(260px,1fr)_96px]">
          <div class="relative">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索部门编码、部门名称或负责人" />
          </div>
          <button class="btn-secondary" @click="loadAll">
            <RefreshCw class="h-4 w-4" />
            刷新
          </button>
        </div>

        <p v-if="message" class="mb-4 rounded-lg bg-blue-50 px-4 py-2 text-sm text-blue-700">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>部门编码</th>
                <th>部门名称</th>
                <th>本部门员工</th>
                <th>负责人</th>
                <th>负责人规则</th>
                <th>排序</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="empty-cell">正在加载部门数据...</td>
              </tr>
              <tr v-else-if="filteredDepartments.length === 0">
                <td colspan="8" class="empty-cell">暂无符合条件的部门</td>
              </tr>
              <tr v-for="item in filteredDepartments" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.deptCode }}</td>
                <td class="font-medium text-gray-900">{{ item.deptName }}</td>
                <td>{{ employeeCount(item.id) }} 人</td>
                <td>{{ item.leaderName || '-' }}</td>
                <td>{{ leaderRuleText(item) }}</td>
                <td>{{ item.sort ?? 0 }}</td>
                <td>
                  <span :class="item.status === 1 ? 'tag-green' : 'tag-gray'">
                    {{ item.status === 1 ? '启用' : '停用' }}
                  </span>
                </td>
                <td class="text-right">
                  <div class="inline-flex items-center gap-3">
                    <button class="link-btn inline-flex items-center gap-1" @click="openEditForm(item)">
                      <Edit3 class="h-4 w-4" />
                      修改
                    </button>
                    <button class="danger-link inline-flex items-center gap-1" @click="disableDepartment(item)">
                      <Trash2 class="h-4 w-4" />
                      停用
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask">
        <div class="modal-card max-w-2xl">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">{{ editingDepartment ? '修改部门' : '新建部门' }}</h2>
              <p class="mt-1 text-sm text-gray-500">
                {{ editingDepartment ? leaderRuleText(editingDepartment) : '新部门创建后，先在员工管理中分配员工，再设置负责人。' }}
              </p>
            </div>
            <button class="icon-btn" @click="showForm = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="field-label">
              部门编码
              <input v-model="form.deptCode" class="input mt-1" placeholder="例如：PROD" />
            </label>
            <label class="field-label">
              部门名称
              <input v-model="form.deptName" class="input mt-1" placeholder="例如：生产部" />
            </label>
            <label class="field-label">
              部门负责人
              <select v-model.number="form.leaderId" class="input mt-1" :disabled="!editingDepartment">
                <option :value="null">无负责人</option>
                <option v-for="user in leaderCandidates" :key="user.id" :value="user.id">
                  {{ user.userCode }} - {{ user.realName || user.username }}
                </option>
              </select>
              <span v-if="editingDepartment && leaderCandidates.length === 0" class="mt-1 block text-xs text-amber-600">
                没有符合条件的负责人候选人：需要本部门员工，并且角色符合该部门负责人规则。
              </span>
            </label>
            <label class="field-label">
              排序
              <input v-model.number="form.sort" type="number" min="0" class="input mt-1" />
            </label>
            <label class="field-label">
              状态
              <select v-model.number="form.status" class="input mt-1">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
          </div>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveDepartment">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

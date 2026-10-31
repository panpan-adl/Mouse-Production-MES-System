<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, ref, onMounted } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import ParticleBackground from '@/components/dashboard/ParticleBackground.vue'
import { Crown, Users, Settings, BarChart3, Plus, Search, Edit, Trash2, RotateCcw, X, Check } from 'lucide-vue-next'
import { 
  getUserList, 
  createUser, 
  updateUser, 
  deleteUser,
  employeeLeave, 
  restoreUser,
  getUsersByRoleId,
  getUsersByDeptId,
  type User,
  type CreateUserParams,
  type UpdateUserParams
} from '@/api/user'
import { departmentApi, type Department } from '@/api/department'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const users = ref<User[]>([])
const searchKeyword = ref('')
const selectedRole = ref('')
const selectedDept = ref('')
const showAddModal = ref(false)
const showEditModal = ref(false)
const showConfirmModal = ref(false)
const confirmAction = ref<'leave' | 'restore'>('leave')
const confirmUserId = ref(0)

const newUser = ref<CreateUserParams>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  gender: 1,
  roleId: 1,
  deptId: 2,
  status: 1
})

const editUser = ref<UpdateUserParams>({})
const editUserId = ref(0)

const roles = [
  { value: '', label: '全部职务' },
  { value: '1', label: '生产经理', deptId: 2 },
  { value: '2', label: '车间主任', deptId: 2 },
  { value: '3', label: '操作工', deptId: 2 },
  { value: '4', label: '质检员', deptId: 3 },
  { value: '5', label: '仓管员', deptId: 4 },
  { value: '6', label: '采购专员', deptId: 5 },
  { value: '7', label: '销售专员', deptId: 6 },
  { value: '8', label: '设备维护员', deptId: 7 },
  { value: '9', label: 'CEO', deptId: 1 },
  { value: '10', label: 'CFO', deptId: 8 },
  { value: '11', label: '人力经理', deptId: 9 }
]

const departments = ref<Department[]>([])

const depts = computed(() => [
  { value: '', label: '全部部门' },
  ...departments.value.map((dept) => ({
    value: String(dept.id),
    label: dept.deptName
  }))
])

const selectableDepts = computed(() => depts.value.filter((dept) => dept.value))

const selectableRoles = computed(() => roles.filter((role) => role.value))

const newUserRoleOptions = computed(() => getRolesByDept(newUser.value.deptId))

const editUserRoleOptions = computed(() => getRolesByDept(editUser.value.deptId))

const canSubmitNewUser = computed(() => {
  const roleValue = String(newUser.value.roleId || '')
  const roleInDept = newUserRoleOptions.value.some((role) => role.value === roleValue)
  return Boolean(roleValue) && roleInDept && !isRoleDisabledForNewUser(roleValue)
})

const canSubmitEditUser = computed(() => {
  const roleValue = String(editUser.value.roleId || '')
  const roleInDept = editUserRoleOptions.value.some((role) => role.value === roleValue)
  return Boolean(roleValue) && roleInDept && !isRoleDisabledForEditUser(roleValue)
})

const filteredUsers = computed(() => {
  let result = users.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(u => 
      u.realName.toLowerCase().includes(keyword) ||
      u.userCode.toLowerCase().includes(keyword)
    )
  }
  return result
})

async function loadUsers() {
  try {
    const data = await getUserList()
    users.value = data
  } catch (error) {
    console.error('Failed to load users:', error)
  }
}

async function loadDepartments() {
  try {
    departments.value = await departmentApi.list()
  } catch (error) {
    console.error('Failed to load departments:', error)
  }
}

function expectedDeptIdByRole(roleId?: number) {
  switch (Number(roleId)) {
    case 9:
      return 1
    case 1:
    case 2:
    case 3:
      return 2
    case 4:
      return 3
    case 5:
      return 4
    case 6:
      return 5
    case 7:
      return 6
    case 8:
      return 7
    case 10:
      return 8
    case 11:
      return 9
    default:
      return 0
  }
}

function syncNewUserDept() {
  const deptId = expectedDeptIdByRole(newUser.value.roleId)
  if (deptId) newUser.value.deptId = deptId
}

function syncEditUserDept() {
  const deptId = expectedDeptIdByRole(editUser.value.roleId)
  if (deptId) editUser.value.deptId = deptId
}

function getRolesByDept(deptId?: number) {
  const selectedDeptId = Number(deptId)
  return selectableRoles.value.filter((role) => role.deptId === selectedDeptId)
}

function firstEnabledRoleByDept(deptId?: number, currentUserId?: number) {
  return getRolesByDept(deptId).find((role) => {
    return currentUserId
      ? !isRoleDisabledForEditUser(role.value)
      : !isRoleDisabledForNewUser(role.value)
  })
}

function syncNewUserRoleByDept() {
  const currentRole = newUserRoleOptions.value.find((role) => Number(role.value) === Number(newUser.value.roleId))
  if (currentRole && !isRoleDisabledForNewUser(currentRole.value)) {
    return
  }
  const nextRole = firstEnabledRoleByDept(newUser.value.deptId)
  newUser.value.roleId = nextRole ? Number(nextRole.value) : 0
}

function syncEditUserRoleByDept() {
  const currentRole = editUserRoleOptions.value.find((role) => Number(role.value) === Number(editUser.value.roleId))
  if (currentRole && !isRoleDisabledForEditUser(currentRole.value)) {
    return
  }
  const nextRole = firstEnabledRoleByDept(editUser.value.deptId, editUserId.value)
  editUser.value.roleId = nextRole ? Number(nextRole.value) : 0
}

function hasOtherCeo(currentUserId?: number) {
  return users.value.some((user) => user.roleId === 9 && user.deleted !== 1 && user.id !== currentUserId)
}

function isRoleDisabledForNewUser(roleValue: string) {
  return roleValue === '9' && hasOtherCeo()
}

function isRoleDisabledForEditUser(roleValue: string) {
  return roleValue === '9' && hasOtherCeo(editUserId.value)
}

async function loadUsersByRole() {
  if (!selectedRole.value) {
    await loadUsers()
    return
  }
  try {
    const data = await getUsersByRoleId(Number(selectedRole.value))
    users.value = data
  } catch (error) {
    console.error('Failed to load users by role:', error)
  }
}

async function loadUsersByDept() {
  if (!selectedDept.value) {
    await loadUsers()
    return
  }
  try {
    const data = await getUsersByDeptId(Number(selectedDept.value))
    users.value = data
  } catch (error) {
    console.error('Failed to load users by dept:', error)
  }
}

async function handleCreateUser() {
  try {
    if (!canSubmitNewUser.value) {
      alert('请选择该部门下可用的职务')
      return
    }
    const realName = newUser.value.realName.trim()
    if (!realName) {
      alert('请填写姓名')
      return
    }
    await createUser({
      ...newUser.value,
      username: realName,
      realName,
    })
    completeDrafts();showAddModal.value=false
    await loadUsers()
    newUser.value = {
      username: '',
      password: '',
      realName: '',
      phone: '',
      email: '',
      gender: 1,
      roleId: 1,
      deptId: 2,
      status: 1
    }
  } catch (error) {
    console.error('Failed to create user:', error)
    alert('创建员工失败，可能是工号或姓名已存在')
  }
}

async function handleEditUser(user: User) {
  editUserId.value = user.id
  editUser.value = {
    realName: user.realName,
    phone: user.phone || '',
    email: user.email || '',
    gender: user.gender,
    roleId: user.roleId,
    deptId: user.deptId,
    status: user.status
  }
  showEditModal.value = true
}

async function handleUpdateUser() {
  try {
    if (!canSubmitEditUser.value) {
      alert('请选择该部门下可用的职务')
      return
    }
    await updateUser(editUserId.value, editUser.value)
    completeDrafts();showEditModal.value=false
    await loadUsers()
  } catch (error) {
    console.error('Failed to update user:', error)
    alert('更新用户失败')
  }
}

async function handleDeleteUser(user: User) {
  if (!window.confirm(`确认删除员工「${user.realName || user.username}」吗？删除后该账号将从员工列表隐藏。`)) {
    return
  }
  try {
    await deleteUser(user.id)
    await loadUsers()
  } catch (error) {
    console.error('Failed to delete user:', error)
    alert('删除用户失败')
  }
}

function handleLeave(userId: number) {
  confirmUserId.value = userId
  confirmAction.value = 'leave'
  showConfirmModal.value = true
}

function handleRestore(userId: number) {
  confirmUserId.value = userId
  confirmAction.value = 'restore'
  showConfirmModal.value = true
}

async function handleConfirm() {
  try {
    if (confirmAction.value === 'leave') {
      await employeeLeave(confirmUserId.value)
    } else {
      await restoreUser(confirmUserId.value)
    }
    showConfirmModal.value = false
    await loadUsers()
  } catch (error) {
    console.error('Operation failed:', error)
    alert('操作失败')
  }
}

function getRoleName(roleId: number) {
  const role = roles.find(r => r.value === String(roleId))
  return role ? role.label : '未知'
}

function getDeptName(deptId: number) {
  const dept = departments.value.find(d => d.id === Number(deptId))
  return dept ? dept.deptName : '未知'
}

function getStatusText(status: number) {
  return status === 1 ? '在职' : '离职'
}

function getStatusClass(status: number) {
  return status === 1 
    ? 'bg-green-100 text-green-600' 
    : 'bg-red-100 text-red-600'
}

function formatDate(dateStr: string | undefined) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadDepartments()])
})

const completeDrafts=useFormDrafts({showAddModal:{open:showAddModal,fields:{newUser}},showEditModal:{open:showEditModal,fields:{editUser}, record:()=>editUserId.value}})
</script>

<template>
  <MainLayout>
    <ParticleBackground />
    <div class="p-6">
      <div class="bg-gradient-to-r from-purple-600 to-indigo-700 rounded-2xl p-8 text-white mb-8">
        <div class="flex items-center">
          <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mr-6">
            <Crown class="w-10 h-10" />
          </div>
          <div>
            <h1 class="text-3xl font-bold">CEO管理中心</h1>
            <p class="text-purple-100 mt-2">欢迎回来，{{ currentUser?.realName }}！</p>
            <p class="text-purple-200 text-sm mt-1">工号：{{ currentUser?.userCode }} | 职务：{{ currentUser?.roleName }}</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">系统用户</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">{{ users.length }}</p>
            </div>
            <div class="w-14 h-14 bg-blue-50 rounded-xl flex items-center justify-center">
              <Users class="w-7 h-7 text-blue-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">在职员工</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">{{ users.filter(u => u.status === 1).length }}</p>
            </div>
            <div class="w-14 h-14 bg-green-50 rounded-xl flex items-center justify-center">
              <BarChart3 class="w-7 h-7 text-green-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">离职员工</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">{{ users.filter(u => u.status === 0).length }}</p>
            </div>
            <div class="w-14 h-14 bg-orange-50 rounded-xl flex items-center justify-center">
              <Settings class="w-7 h-7 text-orange-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">部门数量</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">8</p>
            </div>
            <div class="w-14 h-14 bg-purple-50 rounded-xl flex items-center justify-center">
              <Crown class="w-7 h-7 text-purple-600" />
            </div>
          </div>
        </div>
      </div>

      <div class="mt-8 bg-white rounded-xl shadow-sm border border-gray-100">
        <div class="p-6 border-b border-gray-100">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-800">员工管理</h2>
            <button 
              class="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              @click="showAddModal = true"
            >
              <Plus class="w-4 h-4" />
              <span>新建用户（入职）</span>
            </button>
          </div>
        </div>

        <div class="p-6">
          <div class="flex items-center space-x-4 mb-6">
            <div class="relative flex-1 max-w-md">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索姓名或工号..."
                class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                @keyup.enter="loadUsers"
              />
            </div>
            <select 
              v-model="selectedRole"
              class="px-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              @change="loadUsersByRole"
            >
              <option v-for="role in roles" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
            <select 
              v-model="selectedDept"
              class="px-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              @change="loadUsersByDept"
            >
              <option v-for="dept in depts" :key="dept.value" :value="dept.value">
                {{ dept.label }}
              </option>
            </select>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="bg-gray-50">
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">工号</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">姓名</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">联系电话</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">邮箱</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">职务</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">部门</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">状态</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">入职日期</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">离职日期</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="user in filteredUsers" :key="user.id" class="hover:bg-gray-50 transition-colors">
                  <td class="px-4 py-4">
                    <span class="font-medium text-gray-800">{{ user.userCode }}</span>
                  </td>
                  <td class="px-4 py-4 text-gray-600">{{ user.realName }}</td>
                  <td class="px-4 py-4 text-gray-600">{{ user.phone || '-' }}</td>
                  <td class="px-4 py-4 text-gray-600">{{ user.email || '-' }}</td>
                  <td class="px-4 py-4 text-gray-600">{{ getRoleName(user.roleId) }}</td>
                  <td class="px-4 py-4 text-gray-600">{{ getDeptName(user.deptId) }}</td>
                  <td class="px-4 py-4">
                    <span :class="['px-2 py-1 text-xs font-medium rounded-full', getStatusClass(user.status)]">
                      {{ getStatusText(user.status) }}
                    </span>
                  </td>
                  <td class="px-4 py-4 text-gray-600 text-sm">{{ formatDate(user.entryDate) }}</td>
                  <td class="px-4 py-4 text-gray-600 text-sm">{{ formatDate(user.leaveDate) }}</td>
                  <td class="px-4 py-4">
                    <div class="flex items-center space-x-3">
                      <button 
                        class="text-blue-600 hover:text-blue-700 text-sm font-medium"
                        @click="handleEditUser(user)"
                      >
                        <Edit class="w-4 h-4 inline" />
                      </button>
                      <button 
                      v-if="user.status === 1"
                      class="text-orange-600 hover:text-orange-700 text-sm font-medium"
                      @click="handleLeave(user.id)"
                    >
                      离职
                    </button>
                      <button 
                        v-else
                        class="text-green-600 hover:text-green-700 text-sm font-medium"
                        @click="handleRestore(user.id)"
                    >
                      <RotateCcw class="w-4 h-4 inline" />
                    </button>
                    <button 
                      class="text-red-600 hover:text-red-700 text-sm font-medium"
                      @click="handleDeleteUser(user)"
                    >
                      <Trash2 class="w-4 h-4 inline" />
                    </button>
                  </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-mask fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" data-business-form>
      <div class="modal-card bg-white rounded-xl shadow-lg w-full max-w-md p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-xl font-bold text-gray-800">新建用户（员工入职）</h2>
          <button @click="showAddModal = false" class="text-gray-400 hover:text-gray-600">
            <X class="w-6 h-6" />
          </button>
        </div>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">密码 *</label>
            <input v-model="newUser.password" type="password" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">姓名 *</label>
            <input v-model="newUser.realName" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">联系电话</label>
            <input v-model="newUser.phone" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="例如：13800000000" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱</label>
            <input v-model="newUser.email" type="email" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="例如：name@example.com" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">部门 *</label>
            <select v-model="newUser.deptId" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" @change="syncNewUserRoleByDept">
              <option v-for="dept in selectableDepts" :key="dept.value" :value="Number(dept.value)">
                {{ dept.label }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">职务 *</label>
            <select v-model="newUser.roleId" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" @change="syncNewUserDept">
              <option v-for="role in newUserRoleOptions" :key="role.value" :value="Number(role.value)" :disabled="isRoleDisabledForNewUser(role.value)">
                {{ role.label }}
              </option>
            </select>
          </div>
        </div>
        <div class="flex justify-end space-x-3 mt-6">
          <button @click="showAddModal = false" class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">取消</button>
          <button @click="handleCreateUser" :disabled="!canSubmitNewUser" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center disabled:cursor-not-allowed disabled:opacity-50">
            <Check class="w-4 h-4 mr-2" />
            <span>确认入职</span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-mask fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" data-business-form>
      <div class="modal-card bg-white rounded-xl shadow-lg w-full max-w-md p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-xl font-bold text-gray-800">编辑用户</h2>
          <button @click="showEditModal = false" class="text-gray-400 hover:text-gray-600">
            <X class="w-6 h-6" />
          </button>
        </div>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
            <input v-model="editUser.realName" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">手机号</label>
            <input v-model="editUser.phone" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱</label>
            <input v-model="editUser.email" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">部门</label>
            <select v-model="editUser.deptId" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" @change="syncEditUserRoleByDept">
              <option v-for="dept in selectableDepts" :key="dept.value" :value="Number(dept.value)">
                {{ dept.label }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">职务</label>
            <select v-model="editUser.roleId" class="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" @change="syncEditUserDept">
              <option v-for="role in editUserRoleOptions" :key="role.value" :value="Number(role.value)" :disabled="isRoleDisabledForEditUser(role.value)">
                {{ role.label }}
              </option>
            </select>
          </div>
        </div>
        <div class="flex justify-end space-x-3 mt-6">
          <button @click="showEditModal = false" class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">取消</button>
          <button @click="handleUpdateUser" :disabled="!canSubmitEditUser" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:cursor-not-allowed disabled:opacity-50">保存</button>
        </div>
      </div>
    </div>

    <div v-if="showConfirmModal" class="modal-mask fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" data-business-form>
      <div class="modal-card bg-white rounded-xl shadow-lg w-full max-w-sm p-6">
        <div class="text-center">
          <div class="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Trash2 v-if="confirmAction === 'leave'" class="w-8 h-8 text-orange-600" />
            <RotateCcw v-else class="w-8 h-8 text-green-600" />
          </div>
          <h2 class="text-xl font-bold text-gray-800 mb-2">
            {{ confirmAction === 'leave' ? '确认离职' : '确认恢复' }}
          </h2>
          <p class="text-gray-600 mb-6">
            {{ confirmAction === 'leave' ? '确定要将该员工标记为离职吗？此操作不会删除数据，仅标记状态并记录离职时间。' : '确定要恢复该员工吗？将清除离职时间并恢复在职状态。' }}
          </p>
          <div class="flex justify-center space-x-3">
            <button @click="showConfirmModal = false" class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">取消</button>
            <button 
              @click="handleConfirm" 
              :class="['px-4 py-2 text-white rounded-lg transition-colors', confirmAction === 'leave' ? 'bg-orange-600 hover:bg-orange-700' : 'bg-green-600 hover:bg-green-700']"
            >
              {{ confirmAction === 'leave' ? '确认离职' : '确认恢复' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

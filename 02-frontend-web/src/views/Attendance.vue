<script setup lang="ts">
import { showBusinessFormError } from '@/utils/businessFormValidation'
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Calendar, Clock, Edit3, Plus, RefreshCw, Search, Users, X } from 'lucide-vue-next'
import { attendanceApi, type Attendance } from '@/api/attendance'
import { getUserList, type User } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

interface AttendanceRow {
  id?: number
  userId: number
  userCode: string
  userName: string
  accountStatus?: number
  attendanceDate: string
  checkInTime?: string | null
  checkOutTime?: string | null
  workHours?: number
  status: number
  remark?: string
  missingRecord: boolean
}

const loading = ref(false)
const saving = ref(false)
const savingStatusKey = ref('')
const message = ref('')
const users = ref<User[]>([])
const attendances = ref<Attendance[]>([])
const keyword = ref('')
const statusFilter = ref<number | ''>('')
const dateFilter = ref(today())
const showForm = ref(false)
const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)
const canManageAttendance = computed(() => [1, 2, 9, 11].includes(Number(currentUser.value?.roleId || 0)))

const form = reactive({
  id: undefined as number | undefined,
  userId: 0,
  attendanceDate: today(),
  checkInTime: '',
  checkOutTime: '',
  workHours: 0,
  status: 1,
  remark: '',
})

const activeUsers = computed(() => {
  return users.value
    .filter((user) => user.deleted !== 1)
    .sort((a, b) => Number(a.id || 0) - Number(b.id || 0))
})

const attendanceRows = computed<AttendanceRow[]>(() => {
  const selectedDate = dateFilter.value
  const records = selectedDate
    ? attendances.value.filter((item) => normalizeDate(item.attendanceDate) === selectedDate)
    : attendances.value

  if (!selectedDate) {
    return records.map((record) => toRecordRow(record))
  }

  const recordMap = new Map<number, Attendance>()
  records.forEach((record) => {
    const oldRecord = recordMap.get(record.userId)
    if (!oldRecord || Number(record.id || 0) > Number(oldRecord.id || 0)) {
      recordMap.set(record.userId, record)
    }
  })

  return activeUsers.value.map((user) => {
    const record = recordMap.get(user.id)
    if (record) {
      return toRecordRow(record, user)
    }
    return {
      userId: user.id,
      userCode: user.userCode,
      userName: user.realName || user.username,
      accountStatus: user.status,
      attendanceDate: selectedDate,
      checkInTime: null,
      checkOutTime: null,
      workHours: 0,
      status: 0,
      remark: '当天暂无考勤记录',
      missingRecord: true,
    }
  })
})

const filteredRows = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return attendanceRows.value.filter((row) => {
    const matchWord =
      !word ||
      row.userName.toLowerCase().includes(word) ||
      row.userCode.toLowerCase().includes(word)
    const matchStatus = statusFilter.value === '' || row.status === statusFilter.value
    return matchWord && matchStatus
  })
})

const page = ref(1)
const pageSize = ref(10)
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)))
const pagedRows = computed(() => filteredRows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

watch([keyword, statusFilter, dateFilter, pageSize], () => { page.value = 1 })
watch(pageCount, (count) => { page.value = Math.min(page.value, count) })

const summaryRows = computed(() => attendanceRows.value)
const presentCount = computed(() => summaryRows.value.filter((row) => [1, 2, 3].includes(row.status)).length)
const absentCount = computed(() => summaryRows.value.filter((row) => row.status === 0).length)
const lateCount = computed(() => summaryRows.value.filter((row) => [2, 3].includes(row.status)).length)
const leaveCount = computed(() => summaryRows.value.filter((row) => row.status === 4).length)

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  message.value = ''
  try {
    if (canManageAttendance.value) {
      const [userData, attendanceData] = await Promise.all([
        getUserList(),
        attendanceApi.list(),
      ])
      users.value = userData
      attendances.value = attendanceData
    } else if (currentUser.value) {
      users.value = [currentUser.value as unknown as User]
      attendances.value = await attendanceApi.byUser(currentUser.value.id)
    }
  } catch (error: any) {
    message.value = error.message || '考勤数据加载失败'
  } finally {
    loading.value = false
  }
}

async function queryAttendance() {
  page.value = 1
  await loadAll()
  message.value = '已按当前条件查询考勤数据'
}

async function refreshAttendance() {
  page.value = 1
  await loadAll()
}

function openCreateForm() {
  if (!canManageAttendance.value) {
    message.value = '当前角色只能查看本人考勤，不能登记或修改考勤'
    showBusinessFormError(message.value)
    return
  }
  resetForm()
  form.userId = activeUsers.value[0]?.id || 0
  showForm.value = true
}

function openEditForm(row: AttendanceRow) {
  if (!canManageAttendance.value) {
    message.value = '当前角色只能查看本人考勤，不能登记或修改考勤'
    showBusinessFormError(message.value)
    return
  }
  Object.assign(form, {
    id: row.id,
    userId: row.userId,
    attendanceDate: row.attendanceDate || dateFilter.value || today(),
    checkInTime: toTimeInput(row.checkInTime),
    checkOutTime: toTimeInput(row.checkOutTime),
    workHours: Number(row.workHours || 0),
    status: row.status,
    remark: row.missingRecord ? '' : row.remark || '',
  })
  showForm.value = true
}

async function saveAttendance() {
  if (!canManageAttendance.value) {
    message.value = '当前角色只能查看本人考勤，不能登记或修改考勤'
    showBusinessFormError(message.value)
    return
  }
  const selectedUser = activeUsers.value.find((user) => user.id === form.userId)
  if (!selectedUser) {
    message.value = '请选择有效员工'
    showBusinessFormError(message.value)
    return
  }
  if (!form.attendanceDate) {
    message.value = '请选择考勤日期'
    showBusinessFormError(message.value)
    return
  }

  saving.value = true
  try {
    const sameDayRecord = attendances.value.find((item) => {
      return item.userId === form.userId && normalizeDate(item.attendanceDate) === form.attendanceDate
    })
    const targetId = form.id || sameDayRecord?.id
    const payload = buildFormPayload(selectedUser)

    if (targetId) {
      await attendanceApi.update(targetId, { ...payload, id: targetId })
      message.value = '考勤记录已更新'
    } else {
      await attendanceApi.create(payload)
      message.value = '考勤记录已新增'
    }

    completeDrafts();showForm.value=false
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '考勤记录保存失败'
  } finally {
    saving.value = false
  }
}

async function changeAttendanceStatus(row: AttendanceRow, event: Event) {
  if (!canManageAttendance.value) {
    message.value = '当前角色只能查看本人考勤，不能修改考勤状态'
    showBusinessFormError(message.value)
    return
  }
  const target = event.target as HTMLSelectElement
  const nextStatus = Number(target.value)
  const key = rowKey(row)
  savingStatusKey.value = key
  message.value = ''

  try {
    const payload = buildRowPayload(row, nextStatus)
    if (row.id) {
      await attendanceApi.update(row.id, { ...payload, id: row.id })
      message.value = '考勤状态已修改'
    } else {
      await attendanceApi.create(payload)
      message.value = '考勤状态已登记'
    }
    await loadAll()
  } catch (error: any) {
    message.value = error.message || '考勤状态修改失败'
    target.value = String(row.status)
  } finally {
    savingStatusKey.value = ''
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    userId: 0,
    attendanceDate: dateFilter.value || today(),
    checkInTime: '',
    checkOutTime: '',
    workHours: 0,
    status: 1,
    remark: '',
  })
}

function buildFormPayload(user: User): Attendance {
  return {
    userId: user.id,
    userName: user.realName || user.username,
    attendanceDate: toChinaDatetime(form.attendanceDate, '00:00'),
    checkInTime: form.checkInTime ? toChinaDatetime(form.attendanceDate, form.checkInTime) : null,
    checkOutTime: form.checkOutTime ? toChinaDatetime(form.attendanceDate, form.checkOutTime) : null,
    workHours: Number(form.workHours || 0),
    status: Number(form.status),
    remark: form.remark,
  }
}

function buildRowPayload(row: AttendanceRow, status: number): Attendance {
  const user = activeUsers.value.find((item) => item.id === row.userId)
  const checkInTime = toTimeInput(row.checkInTime)
  const checkOutTime = toTimeInput(row.checkOutTime)
  return {
    userId: row.userId,
    userName: user?.realName || row.userName,
    attendanceDate: toChinaDatetime(row.attendanceDate || dateFilter.value || today(), '00:00'),
    checkInTime: checkInTime ? toChinaDatetime(row.attendanceDate, checkInTime) : null,
    checkOutTime: checkOutTime ? toChinaDatetime(row.attendanceDate, checkOutTime) : null,
    workHours: Number(row.workHours || 0),
    status,
    remark: row.missingRecord && status !== 0 ? '' : row.remark,
  }
}

function toRecordRow(record: Attendance, user?: User): AttendanceRow {
  const matchedUser = user || activeUsers.value.find((item) => item.id === record.userId)
  return {
    id: record.id,
    userId: record.userId,
    userCode: matchedUser?.userCode || `ID-${record.userId}`,
    userName: record.userName || matchedUser?.realName || matchedUser?.username || `员工-${record.userId}`,
    accountStatus: matchedUser?.status,
    attendanceDate: normalizeDate(record.attendanceDate),
    checkInTime: record.checkInTime,
    checkOutTime: record.checkOutTime,
    workHours: Number(record.workHours || 0),
    status: Number(record.status ?? 0),
    remark: record.remark || '',
    missingRecord: false,
  }
}

function accountStatusText(status?: number) {
  return status === 0 ? '账号停用' : '账号启用'
}

function accountStatusClass(status?: number) {
  return status === 0 ? 'tag-gray' : 'tag-green'
}

function normalizeDate(value?: string | null) {
  if (!value) return ''
  const text = String(value)
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10)
  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return text
  return toDateInput(date)
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  const text = String(value)
  const match = text.match(/(?:T|\s)(\d{2}:\d{2})/)
  if (match) return match[1]
  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}

function toTimeInput(value?: string | null) {
  if (!value) return ''
  const text = String(value)
  const match = text.match(/(?:T|\s)(\d{2}:\d{2})/)
  if (match) return match[1]
  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return ''
  return date.toTimeString().slice(0, 5)
}

function today() {
  return toDateInput(new Date())
}

function toDateInput(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return localDate.toISOString().slice(0, 10)
}

function toChinaDatetime(date: string, time: string) {
  return `${date}T${time.length === 5 ? `${time}:00` : time}+08:00`
}

function rowKey(row: AttendanceRow) {
  return `${row.userId}-${row.attendanceDate}-${row.id || 'new'}`
}

function recalculateWorkHours() {
  if (!form.checkInTime || !form.checkOutTime) return
  const start = new Date(`${form.attendanceDate}T${form.checkInTime}:00+08:00`)
  const end = new Date(`${form.attendanceDate}T${form.checkOutTime}:00+08:00`)
  const hours = (end.getTime() - start.getTime()) / 3600000
  form.workHours = hours > 0 ? Number(hours.toFixed(2)) : 0
}

const completeDrafts=useFormDrafts({showForm:{open:showForm,fields:{form}, record:()=>(form as Record<string,any>).id}})
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">考勤管理</h1>
          <p class="mt-1 text-gray-500">
            {{ canManageAttendance ? '从员工表和考勤表读取数据；管理角色可登记和修改考勤。' : '当前角色只能查看本人考勤，不能查看或修改其他员工考勤。' }}
          </p>
        </div>
        <button v-if="canManageAttendance" class="btn-primary" @click="openCreateForm">
          <Plus class="h-4 w-4" />
          新增考勤
        </button>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50">
              <Users class="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <p class="stat-label">{{ canManageAttendance ? '员工总数' : '我的考勤' }}</p>
              <p class="stat-value">{{ canManageAttendance ? activeUsers.length : attendances.length }}</p>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-green-50">
              <Calendar class="h-5 w-5 text-green-600" />
            </div>
            <div>
              <p class="stat-label">出勤人数</p>
              <p class="stat-value text-green-600">{{ presentCount }}</p>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-red-50">
              <Calendar class="h-5 w-5 text-red-600" />
            </div>
            <div>
              <p class="stat-label">缺勤/未登记</p>
              <p class="stat-value text-red-600">{{ absentCount }}</p>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-yellow-50">
              <Clock class="h-5 w-5 text-yellow-600" />
            </div>
            <div>
              <p class="stat-label">迟到/早退</p>
              <p class="stat-value text-yellow-600">{{ lateCount }}</p>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50">
              <Calendar class="h-5 w-5 text-indigo-600" />
            </div>
            <div>
              <p class="stat-label">请假</p>
              <p class="stat-value text-indigo-600">{{ leaveCount }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="mb-5 grid grid-cols-1 gap-3 xl:grid-cols-[minmax(220px,1.4fr)_180px_160px_96px_96px]">
          <div class="relative">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="按员工姓名或员工编码搜索" />
          </div>
          <input v-model="dateFilter" type="date" class="input" />
          <select v-model="statusFilter" class="input">
            <option value="">全部状态</option>
            <option :value="1">正常</option>
            <option :value="2">迟到</option>
            <option :value="3">早退</option>
            <option :value="0">缺勤</option>
            <option :value="4">请假</option>
          </select>
          <button class="btn-primary" @click="queryAttendance">查询</button>
          <button class="btn-secondary" @click="refreshAttendance">
            <RefreshCw class="h-4 w-4" />
            刷新
          </button>
        </div>

        <p v-if="message" class="mb-4 rounded-lg bg-blue-50 px-4 py-2 text-sm text-blue-700">{{ message }}</p>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>员工编码</th>
                <th>员工姓名</th>
                <th>账号状态</th>
                <th>考勤日期</th>
                <th>签到时间</th>
                <th>签退时间</th>
                <th>工时</th>
                <th>考勤状态</th>
                <th>备注</th>
                <th v-if="canManageAttendance" class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td :colspan="canManageAttendance ? 10 : 9" class="empty-cell">正在加载考勤数据...</td>
              </tr>
              <tr v-else-if="filteredRows.length === 0">
                <td :colspan="canManageAttendance ? 10 : 9" class="empty-cell">暂无符合条件的考勤数据</td>
              </tr>
              <tr v-for="row in pagedRows" v-else :key="rowKey(row)">
                <td class="font-medium text-gray-900">{{ row.userCode }}</td>
                <td class="font-medium text-gray-900">{{ row.userName }}</td>
                <td>
                  <span :class="accountStatusClass(row.accountStatus)">{{ accountStatusText(row.accountStatus) }}</span>
                </td>
                <td>{{ row.attendanceDate || '-' }}</td>
                <td>{{ formatTime(row.checkInTime) }}</td>
                <td>{{ formatTime(row.checkOutTime) }}</td>
                <td>{{ Number(row.workHours || 0).toFixed(2) }} 小时</td>
                <td>
                  <select
                    class="input min-w-[96px] py-1"
                    :value="row.status"
                    :disabled="!canManageAttendance || savingStatusKey === rowKey(row)"
                    @change="changeAttendanceStatus(row, $event)"
                  >
                    <option :value="1">正常</option>
                    <option :value="2">迟到</option>
                    <option :value="3">早退</option>
                    <option :value="0">缺勤</option>
                    <option :value="4">请假</option>
                  </select>
                </td>
                <td class="max-w-[220px] truncate" :title="row.remark || '-'">{{ row.remark || '-' }}</td>
                <td v-if="canManageAttendance" class="text-right">
                  <button class="link-btn inline-flex items-center gap-1" @click="openEditForm(row)">
                    <Edit3 class="h-4 w-4" />
                    {{ row.missingRecord ? '登记详情' : '修改详情' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-gray-600">
          <span>共 {{ filteredRows.length }} 条 · 第 {{ page }} / {{ pageCount }} 页</span>
          <div class="flex flex-wrap items-center gap-3">
            <label class="flex items-center gap-2">
              每页
              <select v-model.number="pageSize" :disabled="loading" class="rounded-lg border px-2 py-2" aria-label="考勤每页条数">
                <option :value="10">10 条</option>
                <option :value="20">20 条</option>
                <option :value="50">50 条</option>
              </select>
            </label>
            <button type="button" class="btn-secondary" :disabled="loading || page === 1" @click="page--">上一页</button>
            <button type="button" class="btn-secondary" :disabled="loading || page >= pageCount" @click="page++">下一页</button>
          </div>
        </div>
      </div>

      <div v-if="showForm" class="modal-mask" data-business-form>
        <div class="modal-card max-w-2xl">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">{{ form.id ? '修改考勤' : '登记考勤' }}</h2>
              <p class="mt-1 text-sm text-gray-500">员工列表来自用户管理，保存后写入考勤表。</p>
            </div>
            <button class="icon-btn" @click="showForm = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="field-label">
              员工
              <select v-model.number="form.userId" class="input mt-1" :disabled="!!form.id">
                <option :value="0">请选择员工</option>
                <option v-for="user in activeUsers" :key="user.id" :value="user.id">
                  {{ user.userCode }} - {{ user.realName || user.username }}
                </option>
              </select>
            </label>
            <label class="field-label">
              考勤日期
              <input v-model="form.attendanceDate" type="date" class="input mt-1" data-date-role="end" data-date-pair="attancedate">
            </label>
            <label class="field-label">
              签到时间
              <input v-model="form.checkInTime" type="time" class="input mt-1" @change="recalculateWorkHours" />
            </label>
            <label class="field-label">
              签退时间
              <input v-model="form.checkOutTime" type="time" class="input mt-1" @change="recalculateWorkHours" />
            </label>
            <label class="field-label">
              工作时长
              <input v-model.number="form.workHours" type="number" step="0.01" min="0" class="input mt-1" />
            </label>
            <label class="field-label">
              考勤状态
              <select v-model.number="form.status" class="input mt-1">
                <option :value="1">正常</option>
                <option :value="2">迟到</option>
                <option :value="3">早退</option>
                <option :value="0">缺勤</option>
                <option :value="4">请假</option>
              </select>
            </label>
            <label class="field-label md:col-span-2">
              备注
              <textarea v-model="form.remark" class="input mt-1 h-24 resize-none" placeholder="例如：迟到5分钟、事假、补录考勤等"></textarea>
            </label>
          </div>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="saveAttendance">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

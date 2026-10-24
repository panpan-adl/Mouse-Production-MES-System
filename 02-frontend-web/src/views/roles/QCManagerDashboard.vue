<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { AlertCircle, BarChart3, CheckCircle, ClipboardCheck, RefreshCw } from 'lucide-vue-next'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { productionTaskApi, type ProductionTask } from '@/api/mes'

const router = useRouter()
const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const loading = ref(false)
const message = ref('')
const tasks = ref<ProductionTask[]>([])
const inspections = ref<Record<string,any>[]>([])
const passResults = ['RELEASED']

const passedSourceNos = computed(() => new Set(
  inspections.value
    .filter((item) => passResults.includes(item.status || '') && item.source_type==='TASK')
    .map((item) => Number(item.source_id)),
))
const pendingTasks = computed(() => tasks.value.filter((task) => task.taskStatus === 3 && !passedSourceNos.value.has(Number(task.id))))
const completedInspections = computed(() => inspections.value.filter((item) => item.status))
const qualifiedCount = computed(() => inspections.value.reduce((sum, item) => sum + Number(item.qualified_quantity || 0), 0))
const defectiveCount = computed(() => inspections.value.reduce((sum, item) => sum + Number(Number(item.quantity || 0)-Number(item.qualified_quantity || 0)), 0))
const qualifiedRate = computed(() => {
  const total = qualifiedCount.value + defectiveCount.value
  if (!total) return '0.0%'
  return `${((qualifiedCount.value / total) * 100).toFixed(1)}%`
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  message.value = ''
  const errors: string[] = []

  const taskRequest = productionTaskApi.list()
    .then((taskData) => {
      tasks.value = taskData
    })
    .catch((error) => {
      tasks.value = []
      errors.push(`待质检任务加载失败：${getErrorMessage(error)}`)
    })

  const inspectionRequest = request.get<Record<string,any>[]>('/foundation/inspections')
    .then((inspectionData) => {
      inspections.value = inspectionData
    })
    .catch((error) => {
      inspections.value = []
      errors.push(`质检统计加载失败：${getErrorMessage(error)}`)
    })

  await Promise.all([taskRequest, inspectionRequest])
  if (errors.length > 0) {
    message.value = errors.join('；')
  }
  loading.value = false
}

function taskContent(task: ProductionTask) {
  const typeMap: Record<number, string> = {
    1: '组装',
    2: '加工',
    3: '包装',
    4: '返工',
  }
  const product = task.productName || '未命名产品'
  const type = typeMap[Number(task.taskType)] || '生产'
  return task.remark ? `${product}｜${type}｜${task.remark}` : `${product}｜${type}`
}

function goQualityCheck(task?: ProductionTask) {
  router.push({
    path: '/manufacturing',
    query: task?.id ? { taskId: String(task.id), taskNo: task.taskNo } : undefined,
  })
}

function goQualityTasks() {
  router.push('/quality/tasks')
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '加载质检工作台失败，请稍后重试'
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="rounded-2xl bg-gradient-to-r from-red-600 to-red-700 p-8 text-white">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div class="flex items-center">
            <div class="mr-6 flex h-20 w-20 items-center justify-center rounded-full bg-white/20">
              <CheckCircle class="h-10 w-10" />
            </div>
            <div>
              <h1 class="text-3xl font-bold">质检员工作台</h1>
              <p class="mt-2 text-red-100">欢迎回来，{{ currentUser?.realName }}！这里显示操作工报工后转入待质检的生产任务。</p>
              <p class="mt-1 text-sm text-red-200">工号：{{ currentUser?.userCode }} | 职务：{{ currentUser?.roleName }}</p>
            </div>
          </div>
          <button class="rounded-xl bg-white px-4 py-2 text-sm font-medium text-red-700 shadow-sm hover:bg-red-50" :disabled="loading" @click="loadData">
            <RefreshCw class="mr-2 inline h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
        </div>
      </div>

      <p v-if="message" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card">
          <p class="stat-label">待质检任务</p>
          <p class="stat-value text-yellow-600">{{ pendingTasks.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">已登记质检单</p>
          <p class="stat-value text-blue-600">{{ completedInspections.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">合格数量</p>
          <p class="stat-value text-green-600">{{ qualifiedCount }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">合格率</p>
          <p class="stat-value text-gray-800">{{ qualifiedRate }}</p>
        </div>
      </div>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <div class="panel">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">待质检生产任务</h2>
              <p class="mt-1 text-sm text-gray-500">操作工提交报工后，生产任务状态变为“待质检”；登记通过类质检记录后，该任务会从这里移除。</p>
            </div>
            <button class="btn-secondary" @click="goQualityTasks">
              <ClipboardCheck class="h-4 w-4" />
              进入质检任务
            </button>
          </div>

          <div class="overflow-x-auto">
            <table class="data-table min-w-[840px]">
              <thead>
                <tr>
                  <th>任务编号</th>
                  <th>任务内容</th>
                  <th>数量</th>
                  <th>操作工</th>
                  <th>状态</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="6" class="empty-cell">正在加载待质检任务...</td>
                </tr>
                <tr v-else-if="pendingTasks.length === 0">
                  <td colspan="6" class="empty-cell">暂无操作工报工后的待质检任务</td>
                </tr>
                <tr v-for="task in pendingTasks" v-else :key="task.id">
                  <td class="font-medium text-gray-900">{{ task.taskNo }}</td>
                  <td class="max-w-[300px] truncate" :title="taskContent(task)">{{ taskContent(task) }}</td>
                  <td>{{ task.quantity || 0 }}</td>
                  <td>{{ task.assignedUserName || '-' }}</td>
                  <td><span class="tag-yellow">待质检</span></td>
                  <td class="text-right">
                    <button class="link-btn" @click="goQualityCheck(task)">去质检</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel">
          <h2 class="text-lg font-semibold text-gray-900">质检概览</h2>
          <p class="mt-1 text-sm text-gray-500">统计来自真实质检记录，和左侧待质检任务通过来源任务/单据关联。</p>
          <div class="mt-5 space-y-4">
            <div class="rounded-xl bg-green-50 p-4">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-green-700">合格数量</span>
                <CheckCircle class="h-5 w-5 text-green-600" />
              </div>
              <p class="mt-2 text-2xl font-bold text-green-700">{{ qualifiedCount }}</p>
            </div>
            <div class="rounded-xl bg-red-50 p-4">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-red-700">不合格数量</span>
                <AlertCircle class="h-5 w-5 text-red-600" />
              </div>
              <p class="mt-2 text-2xl font-bold text-red-700">{{ defectiveCount }}</p>
            </div>
            <div class="rounded-xl bg-blue-50 p-4">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-blue-700">综合合格率</span>
                <BarChart3 class="h-5 w-5 text-blue-600" />
              </div>
              <p class="mt-2 text-2xl font-bold text-blue-700">{{ qualifiedRate }}</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

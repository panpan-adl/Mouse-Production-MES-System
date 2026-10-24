<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { AlertCircle, CheckCircle, ClipboardCheck, RefreshCw, Search } from 'lucide-vue-next'
import { productionTaskApi, qcInspectionApi, type ProductionTask, type QcInspection } from '@/api/mes'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const keyword = ref('')
const tasks = ref<ProductionTask[]>([])
const inspections = ref<QcInspection[]>([])

const passResults = ['QUALIFIED', 'CONCESSION_ACCEPT']
const abnormalResults = ['UNQUALIFIED', 'REWORK', 'SCRAP']

const passedSourceNos = computed(() => new Set(
  inspections.value
    .filter((item) => passResults.includes(item.result || '') && item.sourceOrderNo)
    .map((item) => item.sourceOrderNo),
))

const pendingTasks = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return tasks.value
    .filter((task) => task.taskStatus === 3 && !passedSourceNos.value.has(task.taskNo))
    .filter((task) => {
      if (!word) return true
      return (
        task.taskNo?.toLowerCase().includes(word) ||
        task.productName?.toLowerCase().includes(word) ||
        task.assignedUserName?.toLowerCase().includes(word)
      )
    })
})

const abnormalInspections = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return inspections.value
    .filter((item) => item.inspectionType === 'PRODUCTION' && abnormalResults.includes(item.result || ''))
    .filter((item) => {
      if (!word) return true
      return (
        item.inspectionNo?.toLowerCase().includes(word) ||
        item.sourceOrderNo?.toLowerCase().includes(word) ||
        item.handleMethod?.toLowerCase().includes(word)
      )
    })
})

const processedCount = computed(() => inspections.value.filter((item) => item.inspectionType === 'PRODUCTION' && item.result).length)
const passCount = computed(() => inspections.value.filter((item) => item.inspectionType === 'PRODUCTION' && passResults.includes(item.result || '')).length)

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
      errors.push(`待质检生产任务加载失败：${getErrorMessage(error)}`)
    })

  const inspectionRequest = qcInspectionApi.list()
    .then((inspectionData) => {
      inspections.value = inspectionData
    })
    .catch((error) => {
      inspections.value = []
      errors.push(`质检异常任务加载失败：${getErrorMessage(error)}`)
    })

  await Promise.all([taskRequest, inspectionRequest])
  if (errors.length > 0) {
    message.value = errors.join('；')
  }
  loading.value = false
}

function taskTypeText(type?: number) {
  return ({ 1: '组装', 2: '加工', 3: '包装', 4: '返工' } as Record<number, string>)[Number(type)] || '生产'
}

function resultText(result?: string) {
  return ({
    QUALIFIED: '合格',
    UNQUALIFIED: '不合格',
    CONCESSION_ACCEPT: '让步接收',
    REWORK: '返工',
    SCRAP: '报废',
  } as Record<string, string>)[result || ''] || result || '-'
}

function resultClass(result?: string) {
  if (result === 'REWORK') return 'tag-yellow'
  if (result === 'UNQUALIFIED' || result === 'SCRAP') return 'tag-red'
  return 'tag-gray'
}

function goQualityCheck(task?: ProductionTask) {
  router.push({
    path: '/quality/check',
    query: task?.id ? { taskId: String(task.id), taskNo: task.taskNo } : undefined,
  })
}

function goInspectionRecord(inspection: QcInspection) {
  router.push({
    path: '/quality/check',
    query: inspection.sourceOrderNo ? { taskNo: inspection.sourceOrderNo } : undefined,
  })
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '接口请求失败'
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm text-gray-500">质量管理 / 待办任务</p>
          <h1 class="mt-1 text-2xl font-bold text-gray-900">质检任务</h1>
          <p class="mt-2 text-gray-500">这里处理“还没形成质检结论”的待检任务，以及返工、不合格、报废等需要继续跟进的异常任务。</p>
        </div>
        <button class="btn-secondary" :disabled="loading" @click="loadData">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          刷新
        </button>
      </div>

      <p v-if="message" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{{ message }}</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="stat-card">
          <p class="stat-label">待质检生产任务</p>
          <p class="stat-value text-yellow-600">{{ pendingTasks.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">异常待处理</p>
          <p class="stat-value text-red-600">{{ abnormalInspections.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">已登记生产质检</p>
          <p class="stat-value text-blue-600">{{ processedCount }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">通过类生产质检</p>
          <p class="stat-value text-green-600">{{ passCount }}</p>
        </div>
      </div>

      <section class="panel">
        <div class="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">待质检生产任务</h2>
            <p class="mt-1 text-sm text-gray-500">来源于操作工报工完成后的生产任务，质检员点击“登记质检”形成质检记录。</p>
          </div>
          <label class="relative w-full lg:w-[420px]">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索任务编号、产品或操作工" />
          </label>
        </div>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[900px]">
            <thead>
              <tr>
                <th>任务编号</th>
                <th>产品</th>
                <th>工序</th>
                <th>任务数量</th>
                <th>操作工</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="empty-cell">正在加载质检任务...</td>
              </tr>
              <tr v-else-if="pendingTasks.length === 0">
                <td colspan="7" class="empty-cell">暂无待质检生产任务</td>
              </tr>
              <tr v-for="task in pendingTasks" v-else :key="task.id">
                <td class="font-medium text-gray-900">{{ task.taskNo }}</td>
                <td>{{ task.productName || '-' }}</td>
                <td>{{ taskTypeText(task.taskType) }}</td>
                <td>{{ task.quantity || 0 }}</td>
                <td>{{ task.assignedUserName || '-' }}</td>
                <td><span class="tag-yellow">待质检</span></td>
                <td class="text-right">
                  <button class="link-btn inline-flex items-center gap-1" @click="goQualityCheck(task)">
                    <ClipboardCheck class="h-4 w-4" />
                    登记质检
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="mb-5 flex items-center justify-between">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">异常待处理 / 复检任务</h2>
            <p class="mt-1 text-sm text-gray-500">来源于生产质检记录中的不合格、返工、报废结果，用来提醒后续返工、复检或报废处理。</p>
          </div>
          <AlertCircle class="h-5 w-5 text-red-500" />
        </div>

        <div class="overflow-x-auto">
          <table class="data-table min-w-[900px]">
            <thead>
              <tr>
                <th>质检单号</th>
                <th>来源任务/单据</th>
                <th>检验数量</th>
                <th>不合格数量</th>
                <th>结果</th>
                <th>处理方式</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="empty-cell">正在加载异常任务...</td>
              </tr>
              <tr v-else-if="abnormalInspections.length === 0">
                <td colspan="7" class="empty-cell">暂无异常待处理质检任务</td>
              </tr>
              <tr v-for="item in abnormalInspections" v-else :key="item.id">
                <td class="font-medium text-gray-900">{{ item.inspectionNo }}</td>
                <td>{{ item.sourceOrderNo || '-' }}</td>
                <td>{{ item.sampleQuantity || 0 }}</td>
                <td class="font-semibold text-red-600">{{ item.defectiveQuantity || 0 }}</td>
                <td><span :class="resultClass(item.result)">{{ resultText(item.result) }}</span></td>
                <td>{{ item.handleMethod || '-' }}</td>
                <td class="text-right">
                  <button class="link-btn inline-flex items-center gap-1" @click="goInspectionRecord(item)">
                    <CheckCircle class="h-4 w-4" />
                    查看/处理
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Eye, Power, RefreshCw, Search, X } from 'lucide-vue-next'
import { qualityStandardApi, type QualityStandard } from '@/api/mes'

const loading = ref(false)
const detailLoading = ref(false)
const message = ref('')
const keyword = ref('')
const standards = ref<QualityStandard[]>([])
const selectedStandard = ref<QualityStandard | null>(null)
const detailVisible = ref(false)
const statusSaving = ref(false)

const filteredStandards = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return standards.value
  return standards.value.filter((item) => {
    return (
      item.standardName?.toLowerCase().includes(text) ||
      item.productName?.toLowerCase().includes(text) ||
      formatContent(item.standardContent).toLowerCase().includes(text)
    )
  })
})

const activeCount = computed(() => standards.value.filter((item) => item.status === 1).length)

onMounted(loadStandards)

async function loadStandards() {
  loading.value = true
  message.value = ''
  try {
    standards.value = await qualityStandardApi.list()
  } catch (error: any) {
    message.value = error.message || '质检标准加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(standard: QualityStandard) {
  if (!standard.id) return
  detailVisible.value = true
  detailLoading.value = true
  selectedStandard.value = standard
  try {
    selectedStandard.value = await qualityStandardApi.get(standard.id)
  } catch (error: any) {
    message.value = error.message || '质检标准详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function buildStandardPayload(standard: QualityStandard, status: number): QualityStandard {
  return {
    id: standard.id,
    productId: standard.productId,
    standardName: standard.standardName,
    standardContent: standard.standardContent,
    qualifiedThreshold: standard.qualifiedThreshold,
    status,
  }
}

async function toggleStatus(standard: QualityStandard) {
  if (!standard.id) return
  const nextStatus = standard.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '弃用'
  if (!window.confirm(`确认${actionText}质检标准「${standard.standardName}」吗？`)) return
  statusSaving.value = true
  message.value = ''
  try {
    await qualityStandardApi.update(standard.id, buildStandardPayload(standard, nextStatus))
    message.value = `质检标准已${actionText}`
    await loadStandards()
    if (selectedStandard.value?.id === standard.id) {
      selectedStandard.value = await qualityStandardApi.get(standard.id)
    }
  } catch (error: any) {
    message.value = error.message || '质检标准状态修改失败'
  } finally {
    statusSaving.value = false
  }
}

function formatContent(content?: string) {
  return (content || '')
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, ' ')
    .trim()
}

function standardItems(content?: string) {
  const normalized = formatContent(content)
  if (!normalized) return []
  return normalized
    .split(/\n|；|;/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function previewText(content?: string) {
  const items = standardItems(content)
  if (items.length === 0) return '-'
  return items.slice(0, 3).join('；')
}

function statusClass(status?: number) {
  return status === 1 ? 'tag-green' : 'tag-gray'
}

function statusLabel(status?: number) {
  return status === 1 ? '已启用' : '已弃用'
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6 animate-fadeIn">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm text-gray-500">质量基础资料</p>
          <h1 class="mt-1 text-2xl font-bold text-gray-900">质检标准</h1>
        </div>
        <button class="btn-secondary" @click="loadStandards">
          <RefreshCw class="h-4 w-4" />
          刷新
        </button>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div class="stat-card">
          <p class="stat-label">标准总数</p>
          <p class="stat-value">{{ standards.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">启用标准</p>
          <p class="stat-value text-green-600">{{ activeCount }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">覆盖产品</p>
          <p class="stat-value text-blue-600">{{ new Set(standards.map((item) => item.productId)).size }}</p>
        </div>
      </div>

      <section class="panel">
        <div class="mb-5 grid min-w-[360px] grid-cols-[minmax(260px,420px)_92px] items-center gap-3">
          <label class="relative">
            <Search class="search-icon" />
            <input v-model="keyword" class="input pl-10" placeholder="搜索标准名称、产品或内容" />
          </label>
          <button class="btn-secondary w-full" @click="message = '已按当前条件筛选'">
            <Search class="h-4 w-4" />
            查询
          </button>
        </div>

        <p v-if="message" class="mb-4 text-sm text-blue-600">{{ message }}</p>

        <div class="space-y-4">
          <div v-if="loading" class="empty-cell">正在加载质检标准...</div>
          <div v-else-if="filteredStandards.length === 0" class="empty-cell">暂无符合条件的质检标准</div>
          <article v-for="item in filteredStandards" v-else :key="item.id" class="rounded-lg border border-gray-100 bg-gray-50 p-4">
            <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div>
                <h3 class="font-semibold text-gray-900">{{ item.standardName }}</h3>
                <p class="mt-1 text-sm text-gray-500">{{ item.productName || '未绑定产品' }}</p>
              </div>
              <span :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            </div>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-gray-600">{{ previewText(item.standardContent) }}</p>
            <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <span class="text-xs text-gray-500">合格阈值：{{ item.qualifiedThreshold ?? 0 }}%</span>
              <div class="flex items-center gap-3">
                <button class="link-btn inline-flex items-center gap-1" :disabled="statusSaving" @click="toggleStatus(item)">
                  <Power class="h-4 w-4" />
                  {{ item.status === 1 ? '弃用' : '启用' }}
                </button>
                <button class="link-btn inline-flex items-center gap-1" @click="openDetail(item)">
                  <Eye class="h-4 w-4" />
                  查看详情
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <div v-if="detailVisible && selectedStandard" class="modal-mask" @click.self="detailVisible = false">
        <section class="modal-card max-w-3xl">
          <div class="mb-6 flex items-start justify-between">
            <div>
              <p class="text-sm text-gray-500">质检标准详情</p>
              <h2 class="mt-1 text-xl font-bold text-gray-900">{{ selectedStandard.standardName }}</h2>
            </div>
            <button class="icon-btn" aria-label="关闭详情" @click="detailVisible = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div v-if="detailLoading" class="empty-cell">正在加载标准详情...</div>
          <template v-else>
            <div class="grid grid-cols-1 gap-4 text-sm md:grid-cols-3">
              <div>
                <p class="text-gray-500">适用产品</p>
                <p class="mt-1 font-medium text-gray-900">{{ selectedStandard.productName || '-' }}</p>
              </div>
              <div>
                <p class="text-gray-500">合格阈值</p>
                <p class="mt-1 font-medium text-gray-900">{{ selectedStandard.qualifiedThreshold ?? 0 }}%</p>
              </div>
              <div>
                <p class="text-gray-500">状态</p>
                <p class="mt-1">
                  <span :class="statusClass(selectedStandard.status)">{{ statusLabel(selectedStandard.status) }}</span>
                </p>
              </div>
              <div>
                <p class="text-gray-500">创建时间</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatDate(selectedStandard.createTime) }}</p>
              </div>
              <div>
                <p class="text-gray-500">更新时间</p>
                <p class="mt-1 font-medium text-gray-900">{{ formatDate(selectedStandard.updateTime) }}</p>
              </div>
              <div>
                <p class="text-gray-500">产品ID</p>
                <p class="mt-1 font-medium text-gray-900">{{ selectedStandard.productId || '-' }}</p>
              </div>
            </div>

            <div class="mt-6">
              <h3 class="font-semibold text-gray-800">检验要求</h3>
              <ol class="mt-3 space-y-3">
                <li
                  v-for="(content, index) in standardItems(selectedStandard.standardContent)"
                  :key="`${selectedStandard.id}-${index}`"
                  class="flex gap-3 rounded-lg border border-gray-100 bg-gray-50 px-4 py-3 text-sm leading-6 text-gray-700"
                >
                  <span class="mt-0.5 inline-flex h-6 w-6 flex-none items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-700">
                    {{ index + 1 }}
                  </span>
                  <span>{{ content.replace(/^\d+[.、]\s*/, '') }}</span>
                </li>
              </ol>
              <div v-if="standardItems(selectedStandard.standardContent).length === 0" class="empty-cell">暂无标准内容</div>
            </div>
          </template>

          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" :disabled="statusSaving" @click="toggleStatus(selectedStandard)">
              <Power class="h-4 w-4" />
              {{ selectedStandard.status === 1 ? '弃用标准' : '启用标准' }}
            </button>
            <button class="btn-secondary" @click="detailVisible = false">关闭</button>
          </div>
        </section>
      </div>
    </div>
  </MainLayout>
</template>

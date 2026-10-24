<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
import { Maximize2, RefreshCw } from 'lucide-vue-next'
import type { DashboardFilters } from '@/mock/dashboard'

defineProps<{
  filters: DashboardFilters
  currentTime: string
  updatedAt: string
}>()

const workshops=ref<Array<{id:number,name:string}>>([])
onMounted(async()=>{try{workshops.value=await request.get('/foundation/workshops')}catch{workshops.value=[]}})

const emit = defineEmits<{
  (e: 'update:filters', value: DashboardFilters): void
  (e: 'refresh'): void
  (e: 'fullscreen'): void
}>()

function updateField(filters: DashboardFilters, key: keyof DashboardFilters, value: string) {
  emit('update:filters', { ...filters, [key]: value })
}
</script>

<template>
  <header class="dashboard-header">
    <div>
      <p class="eyebrow">Mouse MES</p>
      <h1>生产全景看板</h1>
    </div>

    <div class="header-controls">
      <input
        :value="filters.date"
        type="date"
        class="control-input"
        @change="updateField(filters, 'date', ($event.target as HTMLInputElement).value)"
      />
      <select
        :value="filters.shift"
        class="control-input"
        @change="updateField(filters, 'shift', ($event.target as HTMLSelectElement).value)"
      >
        <option value="day">白班</option>
        <option value="night">夜班</option>
        <option value="all">全天</option>
      </select>
      <select
        :value="filters.line"
        class="control-input"
        @change="updateField(filters, 'line', ($event.target as HTMLSelectElement).value)"
      >
        <option value="all">全部车间</option>
        <option v-for="w in workshops" :key="w.id" :value="`workshop:${w.id}`">{{w.name}}</option>
      </select>
      <div class="time-box">
        <span>当前时间</span>
        <strong>{{ currentTime }}</strong>
      </div>
      <div class="time-box muted">
        <span>更新</span>
        <strong>{{ updatedAt || '--:--:--' }}</strong>
      </div>
      <button class="icon-button" title="刷新数据" @click="emit('refresh')">
        <RefreshCw class="h-4 w-4" />
      </button>
      <button class="icon-button" title="全屏" @click="emit('fullscreen')">
        <Maximize2 class="h-4 w-4" />
      </button>
    </div>
  </header>
</template>

<style scoped>
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
}

.eyebrow {
  color: #4f7cff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

h1 {
  color: #273d56;
  font-size: clamp(22px, 2vw, 32px);
  font-weight: 800;
  line-height: 1.1;
}

.header-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.control-input,
.icon-button,
.time-box {
  height: 34px;
  border: 1px solid rgba(132, 146, 168, 0.18);
  border-radius: 8px;
  background: #ffffff;
  color: #273d56;
}

.control-input {
  width: 122px;
  padding: 0 10px;
  font-size: 12px;
  outline: none;
}

.control-input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.time-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  white-space: nowrap;
}

.time-box span {
  color: #8492a8;
  font-size: 11px;
}

.time-box strong {
  font-size: 13px;
  color: #273d56;
}

.time-box.muted strong {
  color: #18a78e;
}

.icon-button {
  width: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #8492a8;
  transition: border-color 160ms ease, color 160ms ease, background 160ms ease;
}

.icon-button:hover {
  border-color: rgba(79, 124, 255, 0.8);
  color: #273d56;
  background: rgba(79, 124, 255, 0.12);
}

@media (max-width: 1280px) {
  .dashboard-header {
    min-height: 44px;
  }

  .header-controls {
    gap: 6px;
  }

  .control-input {
    width: 100px;
  }

  .time-box {
    display: none;
  }
}
</style>

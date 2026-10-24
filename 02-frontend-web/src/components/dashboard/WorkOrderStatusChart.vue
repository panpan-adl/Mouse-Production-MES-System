<script setup lang="ts">
import { computed } from 'vue'
import type { WorkOrderStatus } from '@/mock/dashboard'

const props = defineProps<{
  data: WorkOrderStatus[]
}>()

const total = computed(() => props.data.reduce((sum, item) => sum + item.value, 0))

const toneClass: Record<WorkOrderStatus['tone'], string> = {
  blue: 'tone-blue',
  green: 'tone-green',
  yellow: 'tone-yellow',
  red: 'tone-red',
  gray: 'tone-gray',
}
</script>

<template>
  <section class="panel">
    <div class="panel-title">
      <h2>工单执行状态</h2>
      <span>{{ total }} 单</span>
    </div>
    <div class="stack-bar" :aria-label="`工单总数 ${total}`">
      <span
        v-for="item in data"
        :key="item.label"
        :class="['stack-item', toneClass[item.tone]]"
        :style="{ width: `${Math.max((item.value / total) * 100, 5)}%` }"
        :title="`${item.label}: ${item.value}单`"
      ></span>
    </div>
    <div class="status-list">
      <div v-for="item in data" :key="item.label" class="status-row">
        <span :class="['dot', toneClass[item.tone]]"></span>
        <span class="name">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  border: 1px solid rgba(132, 146, 168, 0.14);
  border-radius: 8px;
  background: #0d1424;
  padding: 12px;
  min-height: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

h2 {
  color: #f4f7fc;
  font-size: 14px;
  font-weight: 700;
}

.panel-title span {
  color: #8492a8;
  font-size: 12px;
}

.stack-bar {
  display: flex;
  gap: 2px;
  height: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(132, 146, 168, 0.1);
}

.stack-item {
  height: 100%;
  transition: width 500ms ease;
}

.status-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.status-row {
  min-width: 0;
  border-radius: 7px;
  background: rgba(7, 11, 20, 0.54);
  padding: 8px;
}

.dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 5px;
  border-radius: 999px;
}

.name {
  color: #8492a8;
  font-size: 11px;
}

strong {
  display: block;
  margin-top: 3px;
  color: #f4f7fc;
  font-size: 18px;
  line-height: 1;
}

.tone-blue {
  background: #4f7cff;
}

.tone-green {
  background: #31d0aa;
}

.tone-yellow {
  background: #ffb547;
}

.tone-red {
  background: #ff5d73;
}

.tone-gray {
  background: #8492a8;
}

@media (max-width: 1280px) {
  .status-list {
    gap: 5px;
  }

  .status-row {
    padding: 6px;
  }

  strong {
    font-size: 15px;
  }
}
</style>

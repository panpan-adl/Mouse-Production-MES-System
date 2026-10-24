<script setup lang="ts">
import type { MaterialShortage } from '@/mock/dashboard'

defineProps<{
  readinessRate: number
  shortages: MaterialShortage[]
}>()
</script>

<template>
  <section class="panel">
    <div class="panel-title">
      <h2>物料齐套与缺料</h2>
      <strong>{{ readinessRate.toFixed(1) }}%</strong>
    </div>
    <div class="rate-track">
      <span :style="{ width: `${readinessRate}%` }"></span>
    </div>
    <div class="shortage-list">
      <div v-for="item in shortages.slice(0, 5)" :key="item.materialName" class="shortage-row">
        <div class="name">
          <span>{{ item.materialName }}</span>
          <small>待到货: {{ item.incomingQuantity }}{{ item.unit }}</small>
        </div>
        <strong>{{ item.shortageQuantity }}{{ item.unit }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  border: 1px solid rgba(132, 146, 168, 0.14);
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
  min-height: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h2 {
  color: #273d56;
  font-size: 14px;
  font-weight: 700;
}

.panel-title strong {
  color: #dba344;
  font-size: 20px;
}

.rate-track {
  height: 7px;
  margin: 10px 0;
  border-radius: 999px;
  background: rgba(132, 146, 168, 0.12);
  overflow: hidden;
}

.rate-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4f7cff, #18a78e);
  transition: width 500ms ease;
}

.shortage-list {
  display: grid;
  gap: 7px;
}

.shortage-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 7px;
  background: #f5f8fc;
  padding: 7px 9px;
}

.name {
  min-width: 0;
}

.name span,
.name small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name span {
  color: #435a72;
  font-size: 12px;
}

.name small {
  margin-top: 2px;
  color: #8492a8;
  font-size: 10px;
}

.shortage-row strong {
  color: #e26677;
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .shortage-row {
    padding: 5px 7px;
  }

  .name small {
    display: none;
  }
}
</style>

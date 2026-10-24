<script setup lang="ts">
import type { InventorySummary } from '@/mock/dashboard'

defineProps<{
  inventory: InventorySummary
}>()

const items = [
  { key: 'rawMaterial', label: '原材料', tone: 'blue' },
  { key: 'wip', label: '半成品库存', tone: 'yellow' },
  { key: 'finishedGoods', label: '成品库存', tone: 'green' },
  { key: 'lowStockCount', label: '低库存', tone: 'red' },
] as const
</script>

<template>
  <section class="panel">
    <div class="panel-title">
      <h2>库存状态</h2>
      <span>实时库存</span>
    </div>
    <div class="inventory-grid">
      <div v-for="item in items" :key="item.key" :class="['inventory-card', `tone-${item.tone}`]">
        <span>{{ item.label }}</span>
        <strong>{{ inventory[item.key] }}</strong>
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
  margin-bottom: 10px;
}

h2 {
  color: #273d56;
  font-size: 14px;
  font-weight: 700;
}

.panel-title span {
  color: #8492a8;
  font-size: 11px;
}

.inventory-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.inventory-card {
  border-radius: 8px;
  border: 1px solid rgba(132, 146, 168, 0.14);
  padding: 10px;
  background: #f5f8fc;
}

.inventory-card span {
  color: #8492a8;
  font-size: 11px;
}

.inventory-card strong {
  display: block;
  margin-top: 6px;
  color: var(--tone);
  font-size: 25px;
  line-height: 1;
}

.tone-blue {
  --tone: #3b82f6;
}

.tone-yellow {
  --tone: #f59e0b;
}

.tone-green {
  --tone: #10b981;
}

.tone-red {
  --tone: #ef4444;
}

@media (max-width: 1280px) {
  .inventory-card {
    padding: 8px;
  }

  .inventory-card strong {
    font-size: 20px;
  }
}
</style>

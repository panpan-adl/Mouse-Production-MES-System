<script setup lang="ts">
import { ChevronRight } from 'lucide-vue-next'
import type { ProductionFlowNode } from '@/mock/dashboard'

defineProps<{
  nodes: ProductionFlowNode[]
  activeNode: string
}>()

const emit = defineEmits<{
  (e: 'select-node', value: string): void
}>()

const statusLabel: Record<ProductionFlowNode['status'], string> = {
  normal: '正常',
  warning: '预警',
  error: '异常',
}
</script>

<template>
  <section class="panel flow-panel">
    <div class="panel-title">
      <h2>生产闭环主链路</h2>
      <span>点击节点筛选工单</span>
    </div>
    <div class="flow-grid">
      <template v-for="(node, index) in nodes" :key="node.key">
        <button
          :class="['flow-node', `status-${node.status}`, activeNode === node.key ? 'active' : '']"
          @click="emit('select-node', activeNode === node.key ? '' : node.key)"
        >
          <span class="node-status">{{ statusLabel[node.status] }}</span>
          <strong>{{ node.name }}</strong>
          <div class="node-meta">
            <span>{{ node.count }} 单</span>
            <span>{{ node.completionRate }}%</span>
          </div>
          <div class="node-progress">
            <i :style="{ width: `${node.completionRate}%` }"></i>
          </div>
        </button>
        <div v-if="index < nodes.length - 1" class="flow-link">
          <ChevronRight class="h-4 w-4" />
        </div>
      </template>
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

.flow-panel {
  position: relative;
  overflow: hidden;
}

.flow-panel::before {
  content: '';
  position: absolute;
  inset: 42px 20px auto 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(79, 124, 255, 0.45), transparent);
}

.flow-panel::after {
  content: '';
  position: absolute;
  top: 41px;
  left: 22px;
  width: 80px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #31d0aa, transparent);
  animation: flowMove 3s linear infinite;
}

.panel-title {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

h2 {
  color: #f4f7fc;
  font-size: 15px;
  font-weight: 800;
}

.panel-title span {
  color: #8492a8;
  font-size: 11px;
}

.flow-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  align-items: stretch;
}

.flow-link {
  display: none;
}

.flow-node {
  min-width: 0;
  min-height: 116px;
  border: 1px solid rgba(132, 146, 168, 0.14);
  border-radius: 8px;
  padding: 10px;
  text-align: left;
  background: rgba(7, 11, 20, 0.55);
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.flow-node:hover,
.flow-node.active {
  border-color: var(--status-color);
  background: rgba(79, 124, 255, 0.11);
  transform: translateY(-1px);
}

.node-status {
  display: inline-flex;
  padding: 2px 7px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--status-color) 18%, transparent);
  color: var(--status-color);
  font-size: 10px;
  font-weight: 700;
}

.flow-node strong {
  display: block;
  margin-top: 10px;
  color: #f4f7fc;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 9px;
  color: #8492a8;
  font-size: 11px;
}

.node-progress {
  height: 5px;
  margin-top: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(132, 146, 168, 0.13);
}

.node-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--status-color);
  transition: width 500ms ease;
}

.status-normal {
  --status-color: #31d0aa;
}

.status-warning {
  --status-color: #ffb547;
}

.status-error {
  --status-color: #ff5d73;
}

@keyframes flowMove {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(calc(100vw - 430px));
  }
}

@media (max-width: 1280px) {
  .flow-grid {
    gap: 6px;
  }

  .flow-node {
    min-height: 94px;
    padding: 8px;
  }

  .flow-node strong {
    margin-top: 7px;
    font-size: 12px;
  }

  .node-meta {
    margin-top: 6px;
  }
}
</style>

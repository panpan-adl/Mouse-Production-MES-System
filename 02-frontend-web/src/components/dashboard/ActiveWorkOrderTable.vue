<script setup lang="ts">
import type { WorkOrderRow } from '@/mock/dashboard'
defineProps<{
  rows: WorkOrderRow[]
}>()

defineEmits<{
  (e: 'select-row', id: number): void
}>()
</script>

<template>
  <section class="panel">
    <div class="panel-title">
      <h2>工单列表</h2>
      <span>{{ rows.length }} 条</span>
    </div>
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>工单号</th>
            <th>产品</th>
            <th>计划 / 完成</th>
            <th>状态</th>
            <th>进度</th>
            <th>交付</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td class="order-id">{{ row.id }}</td>
            <td>{{ row.productModel }}</td>
            <td>{{ row.planQuantity }} / {{ row.completedQuantity }}</td>
            <td>
              <span :class="['status-badge', `status-${row.statusTone}`]">{{ row.status }}</span>
            </td>
            <td>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: `${row.planQuantity ? Math.min(100,row.completedQuantity/row.planQuantity*100) : 0}%` }"></div>
              </div>
            </td>
            <td class="delivery-date">{{ row.dueDate || '未指定' }}</td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="6" class="empty">暂无符合筛选条件的制造工单</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.panel {
  border: 1px solid rgba(132, 146, 168, 0.14);
  border-radius: 8px;
  background: rgba(13, 20, 36, 0.85);
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
  font-size: 11px;
}

.table-container {
  overflow-x: auto;
  min-height: 0;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

th {
  text-align: left;
  padding: 6px 8px;
  color: #ffffff;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.15);
  white-space: nowrap;
}

td {
  padding: 6px 8px;
  color: #ffffff;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.order-id {
  font-family: monospace;
  font-weight: 600;
  color: #ffffff;
}

.delivery-date {
  color: rgba(255,255,255,0.7);
  white-space: nowrap;
}

.status-badge {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.status-0 { background: #fef3c7; color: #92400e; }
.status-1 { background: #dbeafe; color: #1e40af; }
.status-2 { background: #d1fae5; color: #065f46; }
.status-3 { background: #f3e8ff; color: #6b21a8; }
.status-blue { background: #dbeafe; color: #1e40af; }
.status-green { background: #d1fae5; color: #065f46; }
.status-yellow { background: #fef3c7; color: #92400e; }
.status-red { background: #fee2e2; color: #991b1b; }

.progress-bar {
  width: 80px;
  height: 4px;
  background: rgba(132, 146, 168, 0.12);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4f7cff, #31d0aa);
  border-radius: 2px;
  transition: width 400ms ease;
}

.priority-badge {
  display: inline-block;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
}

.priority-1 { background: #fef2f2; color: #fca5a5; }
.priority-2 { background: #fff7ed; color: #ea580c; }
.priority-3 { background: #fef9c3; color: #a16207; }

.empty {
  text-align: center;
  padding: 20px;
  color: rgba(255,255,255,0.5);
}
</style>

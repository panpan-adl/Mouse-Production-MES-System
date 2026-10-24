<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'
import type { RealtimeAlert } from '@/mock/dashboard'

defineProps<{
  alerts: RealtimeAlert[]
}>()
</script>

<template>
  <section class="panel alert-panel">
    <div class="panel-title">
      <h2>实时异常</h2>
      <span>{{ alerts.length }} 条</span>
    </div>
    <div class="alert-list">
      <article v-for="alert in alerts" :key="alert.id" :class="['alert-row', alert.type]">
        <div class="alert-icon">
          <AlertTriangle class="h-4 w-4" />
        </div>
        <div class="alert-body">
          <div class="alert-head">
            <strong>{{ alert.title }}</strong>
            <span>{{ alert.time }}</span>
          </div>
          <p>{{ alert.content }}</p>
        </div>
      </article>
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

.alert-panel {
  height: 100%;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 9px;
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

.alert-list {
  display: grid;
  gap: 8px;
  height: calc(100% - 28px);
  overflow: hidden;
}

.alert-row {
  display: flex;
  gap: 9px;
  min-height: 0;
  border-radius: 8px;
  padding: 9px;
  background: #f5f8fc;
  border-left: 3px solid var(--tone);
}

.alert-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: var(--tone);
  background: color-mix(in srgb, var(--tone) 16%, transparent);
  flex: 0 0 auto;
}

.alert-body {
  min-width: 0;
}

.alert-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.alert-head strong {
  color: #273d56;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-head span {
  color: #8492a8;
  font-size: 10px;
  white-space: nowrap;
}

p {
  display: -webkit-box;
  margin-top: 4px;
  color: #8492a8;
  font-size: 11px;
  line-height: 1.35;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.warning {
  --tone: #dba344;
}

.error {
  --tone: #e26677;
}

@media (max-width: 1280px) {
  .alert-list {
    gap: 5px;
  }

  .alert-row {
    padding: 7px;
  }

  p {
    -webkit-line-clamp: 1;
  }
}
</style>

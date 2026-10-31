<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { DashboardKpi } from '@/mock/dashboard'

const props = defineProps<{
  kpis: DashboardKpi[]
}>()

const animatedValues = ref<Record<string, number>>({})
let frameId = 0

const toneClass = computed(() => ({
  blue: 'kpi-blue',
  green: 'kpi-green',
  yellow: 'kpi-yellow',
  red: 'kpi-red',
}))

function displayValue(kpi: DashboardKpi) {
  if(kpi.unavailable)return '—'
  const value = animatedValues.value[kpi.key] ?? 0
  return value.toFixed(kpi.precision ?? 0)
}

function animateKpis() {
  cancelAnimationFrame(frameId)
  const duration = 600
  const start = performance.now()
  const fromValues = props.kpis.reduce<Record<string, number>>((map, item) => {
    map[item.key] = animatedValues.value[item.key] ?? 0
    return map
  }, {})

  function step(now: number) {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    const next: Record<string, number> = {}
    props.kpis.forEach((item) => {
      const from = fromValues[item.key] ?? 0
      next[item.key] = from + (item.value - from) * eased
    })
    animatedValues.value = next
    if (progress < 1) frameId = requestAnimationFrame(step)
  }

  frameId = requestAnimationFrame(step)
}

watch(() => props.kpis, animateKpis, { immediate: true, deep: true })

onBeforeUnmount(() => cancelAnimationFrame(frameId))
</script>

<template>
  <section class="kpi-row">
    <article
      v-for="kpi in kpis"
      :key="kpi.key"
      :class="['kpi-card', toneClass[kpi.tone]]"
    >
      <span class="kpi-label">{{ kpi.label }}</span>
      <div class="kpi-value">
        <strong>{{ displayValue(kpi) }}</strong>
        <span>{{ kpi.unavailable?'暂无数据':kpi.unit }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.kpi-card {
  position: relative;
  min-height: 82px;
  border: 1px solid rgba(132, 146, 168, 0.14);
  border-radius: 8px;
  padding: 14px 16px;
  overflow: hidden;
  background: #ffffff;
}

.kpi-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--tone);
}

.kpi-label {
  color: #8492a8;
  font-size: 12px;
}

.kpi-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-top: 8px;
}

.kpi-value strong {
  color: #273d56;
  font-size: clamp(24px, 2vw, 34px);
  font-weight: 800;
  line-height: 1;
}

.kpi-value span {
  color: #8492a8;
  font-size: 12px;
}

.kpi-blue {
  --tone: #4f7cff;
}

.kpi-green {
  --tone: #18a78e;
}

.kpi-yellow {
  --tone: #dba344;
}

.kpi-red {
  --tone: #e26677;
}

@media (max-width: 1280px) {
  .kpi-row {
    gap: 8px;
  }

  .kpi-card {
    min-height: 66px;
    padding: 10px 12px;
  }

  .kpi-label {
    font-size: 11px;
  }

  .kpi-value {
    margin-top: 5px;
  }
}
</style>

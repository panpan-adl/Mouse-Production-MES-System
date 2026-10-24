<script setup lang="ts">
import { computed } from 'vue'
import type { DefectRank, QualityTrend } from '@/mock/dashboard'

const props = defineProps<{
  trend: QualityTrend[]
  defects: DefectRank[]
}>()

const chartWidth = 330
const chartHeight = 118
const maxTotal = computed(() => Math.max(...props.trend.map((item) => item.passed + item.failed), 1))
const totalPassed = computed(() => props.trend.reduce((sum, item) => sum + item.passed, 0))
const totalFailed = computed(() => props.trend.reduce((sum, item) => sum + item.failed, 0))
const passRate = computed(() => {
  const total = totalPassed.value + totalFailed.value
  return total ? (totalPassed.value / total) * 100 : 0
})
const maxDefect = computed(() => Math.max(...props.defects.map((item) => item.quantity), 1))

function xAt(index: number) {
  return 24 + ((chartWidth - 42) / Math.max(props.trend.length - 1, 1)) * index
}

function yAt(value: number) {
  return 12 + (chartHeight - 34) - (value / maxTotal.value) * (chartHeight - 34)
}

const passPoints = computed(() => props.trend.map((item, index) => `${xAt(index)},${yAt(item.passed)}`).join(' '))
const failPoints = computed(() => props.trend.map((item, index) => `${xAt(index)},${yAt(item.failed)}`).join(' '))
</script>

<template>
  <section class="panel quality-panel">
    <div class="panel-title">
      <h2>质量分析</h2>
      <strong>{{ passRate.toFixed(1) }}%</strong>
    </div>
    <svg class="quality-chart" :viewBox="`0 0 ${chartWidth} ${chartHeight}`">
      <polyline :points="passPoints" class="pass-line" />
      <polyline :points="failPoints" class="fail-line" />
      <circle v-for="(item, index) in trend" :key="`${item.label}-pass`" :cx="xAt(index)" :cy="yAt(item.passed)" r="2.8" class="pass-dot">
        <title>{{ item.label }} 合格 {{ item.passed }}</title>
      </circle>
      <circle v-for="(item, index) in trend" :key="`${item.label}-fail`" :cx="xAt(index)" :cy="yAt(item.failed)" r="2.6" class="fail-dot">
        <title>{{ item.label }} 不合格 {{ item.failed }}</title>
      </circle>
      <text v-for="(item, index) in trend" :key="item.label" :x="xAt(index)" :y="chartHeight - 6" text-anchor="middle">{{ item.label }}</text>
    </svg>
    <div class="defect-list"><p class="text-xs text-slate-500 mb-1">不合格检测项次数</p>
      <div v-for="item in defects" :key="item.defectType" class="defect-row">
        <span>{{ item.defectType }}</span>
        <div>
          <i :style="{ width: `${(item.quantity / maxDefect) * 100}%` }"></i>
        </div>
        <strong>{{ item.quantity }}</strong>
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

.quality-panel {
  height: 100%;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h2 {
  color: #f4f7fc;
  font-size: 14px;
  font-weight: 700;
}

.panel-title strong {
  color: #31d0aa;
  font-size: 22px;
}

.quality-chart {
  display: block;
  width: 100%;
  height: 39%;
  min-height: 82px;
  margin-top: 5px;
}

.pass-line,
.fail-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.pass-line {
  stroke: #31d0aa;
}

.fail-line {
  stroke: #ff5d73;
}

.pass-dot {
  fill: #f9fafb;
  stroke: #31d0aa;
  stroke-width: 2;
}

.fail-dot {
  fill: #f9fafb;
  stroke: #ff5d73;
  stroke-width: 2;
}

text {
  fill: #9ca3af;
  font-size: 10px;
}

.defect-list {
  display: grid;
  gap: 7px;
}

.defect-row {
  display: grid;
  grid-template-columns: minmax(78px, 1fr) 1.4fr 24px;
  align-items: center;
  gap: 8px;
}

.defect-row span {
  overflow: hidden;
  color: #dbe4f2;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.defect-row div {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(132, 146, 168, 0.14);
}

.defect-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #ff5d73;
}

.defect-row strong {
  color: #f4f7fc;
  font-size: 12px;
  text-align: right;
}

@media (max-width: 1280px) {
  .quality-chart {
    height: 34%;
    min-height: 70px;
  }

  .defect-list {
    gap: 5px;
  }
}
</style>

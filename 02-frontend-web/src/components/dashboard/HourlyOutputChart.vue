<script setup lang="ts">
import { computed } from 'vue'
import type { HourlyOutput } from '@/mock/dashboard'

const props = defineProps<{
  data: HourlyOutput[]
}>()

const chartWidth = 560
const chartHeight = 178
const padding = { top: 14, right: 12, bottom: 26, left: 28 }

const maxValue = computed(() => {
  const values = props.data.flatMap((item) => [item.plan, item.actual])
  return Math.max(...values, 1)
})

const barWidth = computed(() => {
  const innerWidth = chartWidth - padding.left - padding.right
  return innerWidth / Math.max(props.data.length, 1) * 0.44
})

function xAt(index: number) {
  const innerWidth = chartWidth - padding.left - padding.right
  return padding.left + (innerWidth / Math.max(props.data.length - 1, 1)) * index
}

function yAt(value: number) {
  const innerHeight = chartHeight - padding.top - padding.bottom
  return padding.top + innerHeight - (value / maxValue.value) * innerHeight
}

function barX(index: number) {
  const innerWidth = chartWidth - padding.left - padding.right
  const cell = innerWidth / Math.max(props.data.length, 1)
  return padding.left + cell * index + cell * 0.28
}

const planPoints = computed(() => props.data.map((item, index) => `${xAt(index)},${yAt(item.plan)}`).join(' '))
</script>

<template>
  <section class="panel chart-panel">
    <div class="panel-title">
      <h2>小时关键工序放行量</h2>
      <div class="legend">
        <span><i class="actual"></i>实际</span>
        <span><i class="plan"></i>计划</span>
      </div>
    </div>
    <svg class="chart-svg" :viewBox="`0 0 ${chartWidth} ${chartHeight}`" role="img">
      <g class="axis">
        <line :x1="padding.left" :y1="chartHeight - padding.bottom" :x2="chartWidth - padding.right" :y2="chartHeight - padding.bottom" />
      </g>
      <g>
        <rect
          v-for="(item, index) in data"
          :key="item.hour"
          :x="barX(index)"
          :y="yAt(item.actual)"
          :width="barWidth"
          :height="chartHeight - padding.bottom - yAt(item.actual)"
          rx="3"
          class="actual-bar"
        >
          <title>{{ item.hour }}:00 实际 {{ item.actual }} / 计划 {{ item.plan }}</title>
        </rect>
      </g>
      <polyline :points="planPoints" class="plan-line" />
      <circle
        v-for="(item, index) in data"
        :key="`${item.hour}-dot`"
        :cx="xAt(index)"
        :cy="yAt(item.plan)"
        r="3"
        class="plan-dot"
      >
        <title>{{ item.hour }}:00 计划 {{ item.plan }}</title>
      </circle>
      <g class="labels">
        <text
          v-for="(item, index) in data"
          :key="`${item.hour}-label`"
          :x="xAt(index)"
          :y="chartHeight - 7"
          text-anchor="middle"
        >
          {{ item.hour }}
        </text>
      </g>
    </svg>
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

.chart-panel {
  height: 100%;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

h2 {
  color: #273d56;
  font-size: 14px;
  font-weight: 700;
}

.legend {
  display: flex;
  gap: 10px;
  color: #8492a8;
  font-size: 11px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.legend .actual {
  background: #18a78e;
}

.legend .plan {
  background: #dba344;
}

.chart-svg {
  display: block;
  width: 100%;
  height: calc(100% - 24px);
  min-height: 120px;
}

.axis line {
  stroke: rgba(132, 146, 168, 0.18);
}

.actual-bar {
  fill: url(#none);
  fill: #18a78e;
  opacity: 0.88;
  transition: opacity 180ms ease;
}

.actual-bar:hover {
  opacity: 1;
}

.plan-line {
  fill: none;
  stroke: #dba344;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.plan-dot {
  fill: #f9fafb;
  stroke: #dba344;
  stroke-width: 2;
}

.labels text {
  fill: #9ca3af;
  font-size: 11px;
}
</style>

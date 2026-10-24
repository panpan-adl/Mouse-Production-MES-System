<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ active?: boolean }>(), { active: true })
const canvas = ref<HTMLCanvasElement | null>(null)
type Particle = { x: number; y: number; z: number; size: number; phase: number; color: number }
const colors = ['115,100,76', '135,121,94', '81,107,110', '164,120,70']
let particles: Particle[] = []
let context: CanvasRenderingContext2D | null = null
let observer: ResizeObserver | undefined
let reduced: MediaQueryList | undefined
let frame = 0, last = 0, elapsed = 0, width = 0, height = 0, tiltX = 0, tiltY = 0
let pointer = { x: -10000, y: -10000 }

function resize() {
  const element = canvas.value
  if (!element) return
  const bounds = element.getBoundingClientRect()
  width = bounds.width; height = bounds.height
  const ratio = Math.min(window.devicePixelRatio || 1, 1.5)
  element.width = Math.round(width * ratio); element.height = Math.round(height * ratio)
  context = element.getContext('2d')
  context?.setTransform(ratio, 0, 0, ratio, 0, 0)
  // Area-based density keeps the field equally full on tall and wide screens.
  const count = Math.min(3800, Math.max(650, Math.round(width * height / 260)))
  particles = Array.from({ length: count }, (_, i) => ({
    x: Math.random(), y: Math.random(), z: Math.random(),
    size: .65 + Math.random() * 1.05, phase: Math.random() * Math.PI * 2,
    color: i % colors.length,
  }))
  draw()
}

function draw() {
  if (!context || !width || !height) return
  context.clearRect(0, 0, width, height)
  const wrap = (value: number, span: number) => ((value % span) + span) % span
  for (const p of particles) {
    const speed = .45 + p.z * .8
    // Wrap around padded edges so the whole panel stays populated during motion.
    let px = wrap(p.x * (width + 48) + Math.sin(elapsed * .18 + p.phase) * 22
      + elapsed * 5 * speed + tiltX * 55, width + 48) - 24
    let py = wrap(p.y * (height + 48) - elapsed * 9 * speed
      + Math.cos(elapsed * .14 + p.phase) * 18 + tiltY * 55, height + 48) - 24
    const dx = px - pointer.x, dy = py - pointer.y
    const distance = Math.hypot(dx, dy)
    if (distance < 115 && distance > .01) {
      const force = Math.pow(1 - distance / 115, 2) * 22
      px += dx / distance * force; py += dy / distance * force
    }
    // Warm graphite points remain crisp against the paper background.
    const shimmer = .88 + Math.sin(elapsed * .8 + p.phase) * .12
    const alpha = (.22 + p.z * .4) * shimmer
    const size = p.size * (.8 + p.z * .4)
    context.fillStyle = `rgba(${colors[p.color]},${alpha})`
    context.beginPath(); context.arc(px, py, size, 0, Math.PI * 2); context.fill()
  }
}

function tick(time: number) {
  if (!last) last = time
  const delta = time - last
  if (delta >= 32) {
    elapsed += Math.min(delta, 64) / 1000
    last = time
    const targetX = pointer.x < 0 ? 0 : (pointer.x / width - .5) * .24
    const targetY = pointer.y < 0 ? 0 : (pointer.y / height - .5) * .15
    tiltX += (targetX - tiltX) * .035; tiltY += (targetY - tiltY) * .035
    draw()
  }
  frame = requestAnimationFrame(tick)
}
function sync() {
  cancelAnimationFrame(frame); frame = 0; last = 0
  if (props.active && !document.hidden && !reduced?.matches) frame = requestAnimationFrame(tick)
  else draw()
}
function move(event: PointerEvent) {
  if (event.pointerType === 'touch') return
  const bounds = canvas.value?.getBoundingClientRect()
  if (bounds) pointer = { x: event.clientX - bounds.left, y: event.clientY - bounds.top }
}
function leave(event: PointerEvent) { if (!event.relatedTarget) pointer = { x: -10000, y: -10000 } }
watch(() => props.active, sync)
onMounted(() => {
  reduced = matchMedia('(prefers-reduced-motion: reduce)')
  observer = new ResizeObserver(resize)
  if (canvas.value) observer.observe(canvas.value)
  window.addEventListener('pointermove', move, { passive: true })
  window.addEventListener('pointerout', leave, { passive: true })
  document.addEventListener('visibilitychange', sync)
  reduced.addEventListener('change', sync)
  resize(); sync()
})
onBeforeUnmount(() => {
  cancelAnimationFrame(frame); observer?.disconnect()
  window.removeEventListener('pointermove', move); window.removeEventListener('pointerout', leave)
  document.removeEventListener('visibilitychange', sync); reduced?.removeEventListener('change', sync)
})
</script>

<template><canvas ref="canvas" class="login-particle-cloud" aria-hidden="true"></canvas></template>
<style scoped>
.login-particle-cloud{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}
</style>

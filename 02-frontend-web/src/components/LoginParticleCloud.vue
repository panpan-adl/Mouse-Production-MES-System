<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ active?: boolean }>(), { active: true })
const canvas = ref<HTMLCanvasElement | null>(null)
type Particle = { x: number; y: number; z: number; size: number; phase: number; color: number; dust: boolean }
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
  // A surface cloud plus a thinner inner volume gives the sphere depth without links.
  const count = width < 500 ? 3200 : 6200
  particles = Array.from({ length: count }, (_, i) => {
    const y = 1 - 2 * (i + .5) / count
    const angle = i * Math.PI * (3 - Math.sqrt(5))
    const ring = Math.sqrt(1 - y * y)
    const radius = i % 5 === 0 ? .4 + Math.random() * .58 : .94 + Math.random() * .1
    return { x: Math.cos(angle) * ring * radius, y: y * radius, z: Math.sin(angle) * ring * radius,
      size: .65 + Math.random() * 1.15, phase: Math.random() * Math.PI * 2,
      color: Math.random() < .1 ? 3 : i % 3, dust: i % 11 === 0 }
  })
  draw()
}

function draw() {
  if (!context || !width || !height) return
  context.clearRect(0, 0, width, height)
  const mobile = width < 500
  const centerX = width * .52
  const centerY = height * (mobile ? .75 : .66)
  const radius = mobile ? Math.min(width * .37, 160) : Math.min(width * .39, height * .27)
  const rotation = elapsed * .12 + tiltX
  const cy = Math.cos(rotation), sy = Math.sin(rotation)
  const cx = Math.cos(-.16 + tiltY), sx = Math.sin(-.16 + tiltY)
  const intro = reduced?.matches ? 1 : Math.min(1, elapsed / 2.2)
  const gather = 1 - Math.pow(1 - intro, 3)
  for (const p of particles) {
    const ripple = 1 + .026 * Math.sin(p.y * 7 + elapsed * .7 + p.phase * .12)
    const x = (p.x * cy + p.z * sy) * ripple
    const z0 = -p.x * sy + p.z * cy
    const y = (p.y * cx - z0 * sx) * ripple
    const z = p.y * sx + z0 * cx
    const perspective = 3.8 / (3.8 - z)
    let px = centerX + x * radius * perspective
    let py = centerY + y * radius * perspective
    // Sparse ambient dust extends the main cloud across the page.
    if (p.dust) {
      px = (p.x + 1) * width * .5 + Math.sin(elapsed * .15 + p.phase) * 12
      py = (p.y + 1) * height * .5 + Math.cos(elapsed * .12 + p.phase) * 10
    } else {
      px += x * radius * (1 - gather) * .9
      py += y * radius * (1 - gather) * .9
    }
    const dx = px - pointer.x, dy = py - pointer.y
    const distance = Math.hypot(dx, dy)
    if (distance < 115 && distance > .01) {
      const force = Math.pow(1 - distance / 115, 2) * 22
      px += dx / distance * force; py += dy / distance * force
    }
    // Warm graphite points remain crisp against the paper background.
    const depth = (z + 1.2) / 2.4
    const alpha = p.dust ? .27 : .24 + depth * .56
    const size = p.size * (p.dust ? .8 : perspective)
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

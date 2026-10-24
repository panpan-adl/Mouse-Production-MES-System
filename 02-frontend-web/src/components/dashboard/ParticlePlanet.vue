<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref<HTMLCanvasElement>()

// ── 粒子星球参数 ──
const PARTICLE_COUNT = 2200
const BASE_RADIUS = 160
const ROTATION_SPEED = 0.0018

interface Particle {
  x: number; y: number; z: number
  size: number
  brightness: number
  ox: number; oy: number; oz: number
}

let particles: Particle[] = []
let canvas: HTMLCanvasElement
let ctx: CanvasRenderingContext2D
let animId = 0
let mouseX = 0
let mouseY = 0
let targetSpread = 0
let currentSpread = 0
let rotation = 0

function initParticles() {
  particles = []
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    // 球面上均匀分布（斐波那契球体）
    const phi = Math.acos(1 - 2 * (i + 0.5) / PARTICLE_COUNT)
    const theta = Math.PI * (1 + Math.sqrt(5)) * i

    const r = BASE_RADIUS * (0.85 + Math.random() * 0.15)
    const p: Particle = {
      ox: r * Math.sin(phi) * Math.cos(theta),
      oy: r * Math.cos(phi),
      oz: r * Math.sin(phi) * Math.sin(theta),
      x: 0, y: 0, z: 0,
      size: 0.8 + Math.random() * 1.8,
      brightness: 0.3 + Math.random() * 0.7,
    }
    particles.push(p)
  }
}

function resize() {
  if (!canvas) return
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
}

function draw() {
  if (!ctx || !canvas) return

  const w = canvas.width
  const h = canvas.height
  const cx = w / 2
  const cy = h / 2

  // 平滑插值 spread
  currentSpread += (targetSpread - currentSpread) * 0.06
  rotation += ROTATION_SPEED * (1 + currentSpread * 0.3)

  // 清屏 — 带拖尾
  ctx.fillStyle = 'rgba(7, 11, 20, 0.25)'
  ctx.fillRect(0, 0, w, h)

  const spread = 1 + currentSpread
  const sinR = Math.sin(rotation)
  const cosR = Math.cos(rotation)

  // 按 z 排序（从远到近）
  const sorted = particles.map((p) => {
    // 旋转
    const rx = p.ox * cosR - p.oz * sinR
    const rz = p.ox * sinR + p.oz * cosR
    // 发散
    const sx = rx * spread
    const sy = p.oy * spread
    const sz = rz * spread
    return { p, sx, sy, sz }
  }).sort((a, b) => a.sz - b.sz)

  for (const { p, sx, sy, sz } of sorted) {
    // 透视投影
    const perspective = 600 / (600 + sz)
    const px = cx + sx * perspective
    const py = cy + sy * perspective
    const size = p.size * perspective
    const alpha = Math.max(0.15, p.brightness * perspective)

    ctx.beginPath()
    ctx.arc(px, py, size, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(148, 200, 255, ${alpha})`
    ctx.fill()
  }

  animId = requestAnimationFrame(draw)
}

function onMouseMove(e: MouseEvent) {
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const w = canvas.width
  const h = canvas.height
  mouseX = (e.clientX - rect.left) / w
  mouseY = (e.clientY - rect.top) / h

  // 鼠标越靠近边缘 → spread 越大
  const dx = Math.abs(mouseX - 0.5) * 2
  const dy = Math.abs(mouseY - 0.5) * 2
  const distance = Math.sqrt(dx * dx + dy * dy)
  targetSpread = Math.min(distance * 0.8, 0.6)
}

function onWheel(e: WheelEvent) {
  e.preventDefault()
  targetSpread = Math.max(0, Math.min(1.5, targetSpread + e.deltaY * 0.002))
}

onMounted(() => {
  canvas = canvasRef.value!
  ctx = canvas.getContext('2d')!
  initParticles()
  resize()
  draw()
  window.addEventListener('resize', resize)
  window.addEventListener('mousemove', onMouseMove)
  canvas.addEventListener('wheel', onWheel, { passive: false })
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', onMouseMove)
  canvas?.removeEventListener('wheel', onWheel)
})
</script>

<template>
  <canvas ref="canvasRef" class="particle-planet" />
</template>

<style scoped>
.particle-planet {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
  pointer-events: none;
}
</style>

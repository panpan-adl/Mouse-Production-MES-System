<template>
  <canvas ref="canvasRef"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let W = 0, H = 0
let animId = 0

interface Particle {
  x: number; y: number; z: number
  vx: number; vy: number
  r: number; a: number
  h: number; s: number; l: number
  tw: number; ts: number
}

const N = 200
const particles: Particle[] = []
const cs = [
  [210, 80, 70], [190, 70, 65], [240, 60, 70],
  [200, 50, 85], [260, 70, 65], [180, 60, 60]
]

function rand(min: number, max: number) {
  return Math.random() * (max - min) + min
}

function resize() {
  const c = canvasRef.value
  if (!c) return
  const parent = c.parentElement
  if (!parent) return
  W = c.width = parent.clientWidth
  H = c.height = parent.clientHeight
}

function initParticles() {
  particles.length = 0
  for (let i = 0; i < N; i++) {
    const z = Math.random(), zf = 1 - z
    const ci = cs[Math.floor(Math.random() * cs.length)]
    particles.push({
      x: Math.random() * W, y: Math.random() * H, z,
      vx: rand(-0.2 - zf * 0.5, 0.2 + zf * 0.5),
      vy: rand(-0.1 - zf * 0.3, 0.1 + zf * 0.3),
      r: 0.5 + zf * 2.5,
      a: 0.15 + zf * 0.5,
      h: ci[0] + rand(-15, 15),
      s: ci[1] + rand(-15, 15),
      l: ci[2] + rand(-15, 15),
      tw: Math.random() * Math.PI * 2,
      ts: 0.005 + Math.random() * 0.02 * (1 - z * 0.5),
    })
  }
}

function draw() {
  if (!ctx || !canvasRef.value) return
  ctx.fillStyle = 'rgba(5,5,16,0.08)'
  ctx.fillRect(0, 0, W, H)

  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    if (p.x < -50) p.x = W + 50
    if (p.x > W + 50) p.x = -50
    if (p.y < -50) p.y = H + 50
    if (p.y > H + 50) p.y = -50
    p.tw += p.ts
  }

  for (const p of particles) {
    const blk = 0.85 + Math.sin(p.tw) * 0.15
    const al = Math.min(p.a * blk, 1)
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    if (p.r > 1.5) {
      ctx.shadowColor = `hsla(${p.h},${p.s}%,${p.l}%,${al * 0.2})`
      ctx.shadowBlur = p.r * 2
    }
    ctx.fillStyle = `hsla(${p.h},${p.s}%,${p.l}%,${al})`
    ctx.fill()
    ctx.shadowBlur = 0
  }

  animId = requestAnimationFrame(draw)
}

onMounted(() => {
  const c = canvasRef.value
  if (!c) return
  ctx = c.getContext('2d')
  if (!ctx) return
  resize()
  ctx.fillStyle = '#050510'
  ctx.fillRect(0, 0, W, H)
  initParticles()
  draw()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  if (animId) cancelAnimationFrame(animId)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: block;
}
</style>

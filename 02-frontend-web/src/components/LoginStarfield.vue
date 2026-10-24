<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

// ── 核心参数 ──
const STAR_LAYERS = 3
const BASE_COUNT = 280
const COLORS = [
  [255, 255, 255],   // 白
  [200, 220, 255],   // 冷白
  [255, 240, 210],   // 暖白
  [180, 210, 255],   // 淡蓝
  [255, 220, 190],   // 淡橙
]

// ── 恒星 ──
interface Star {
  x: number; y: number
  z: number       // 0-1 深度
  size: number    // 基础半径
  layer: number   // 所属层
  color: [number, number, number]
  phase: number   // 闪烁相位
  twinkleSpeed: number
  driftX: number
  driftY: number
}

let stars: Star[] = []
let w = 0, h = 0
let animId = 0
let mouseX = 0, mouseY = 0
let smoothMX = 0, smoothMY = 0
let mouseTimer: ReturnType<typeof setTimeout> | null = null
let time = 0

// ── 生成星空 ──
function spawn(width: number, height: number) {
  const total = Math.floor(BASE_COUNT * (width * height) / (1920 * 1080))
  stars = []
  for (let i = 0; i < total; i++) {
    const layer = Math.floor(Math.random() * STAR_LAYERS)
    // 深度按层映射：层0最近(0.6-1), 层1中等(0.3-0.65), 层2最远(0.05-0.35)
    const zBase = 0.05 + Math.random() * 0.95
    const z = layer === 0 ? 0.55 + zBase * 0.45
            : layer === 1 ? 0.25 + zBase * 0.40
            : 0.05 + zBase * 0.30

    const baseSize = layer === 0 ? 0.6 + Math.random() * 2.0
                   : layer === 1 ? 0.4 + Math.random() * 1.2
                   : 0.2 + Math.random() * 0.6

    stars.push({
      x: Math.random() * width,
      y: Math.random() * height,
      z,
      size: baseSize,
      layer,
      color: COLORS[Math.floor(Math.random() * COLORS.length)] as [number, number, number],
      phase: Math.random() * Math.PI * 2,
      twinkleSpeed: 0.5 + Math.random() * 2.5,
      driftX: (Math.random() - 0.5) * 0.015,
      driftY: (Math.random() - 0.5) * 0.015,
    })
  }
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = rect.width
  h = rect.height
  canvas.width = Math.floor(w * dpr)
  canvas.height = Math.floor(h * dpr)
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  spawn(w, h)
}

// ── 主渲染 ──
function draw(now: number) {
  time = now * 0.001
  const canvas = canvasRef.value
  const ctx = canvas?.getContext('2d')
  if (!canvas || !ctx) return

  // 平滑鼠标
  smoothMX += (mouseX - smoothMX) * 0.06
  smoothMY += (mouseY - smoothMY) * 0.06

  // 视差偏移（鼠标驱动）
  const px = (smoothMX / w - 0.5) * 2
  const py = (smoothMY / h - 0.5) * 2

  ctx.clearRect(0, 0, w, h)

  // ── 按层绘制（先远后近） ──
  for (let layer = STAR_LAYERS - 1; layer >= 0; layer--) {
    const isNear = layer === 0

    // 近层额外绘制星云光晕
    if (isNear) drawNebula(ctx)

    for (const star of stars) {
      if (star.layer !== layer) continue

      // 视差偏移
      const parallax = star.z * 20
      let sx = star.x + px * parallax
      let sy = star.y + py * parallax

      // 微小漂移
      sx += Math.sin(time * 0.1 + star.phase) * star.driftX * 30
      sy += Math.cos(time * 0.08 + star.phase * 1.3) * star.driftY * 30

      // 闪烁
      const twinkle = 0.55 + 0.45 * Math.sin(time * star.twinkleSpeed + star.phase)
      const alpha = Math.min(1, (0.15 + star.z * 0.5 + twinkle * 0.35))

      if (alpha < 0.02) continue

      let radius = star.size * (0.6 + star.z * 0.8 + twinkle * 0.25)

      // 近层大星更突出
      if (isNear) radius *= 1.15

      // 颜色
      const [r, g, b] = star.color
      const color = `rgba(${r},${g},${b},${alpha})`

      // ── 绘制 ──
      // 1. 外层大光晕（只有近中层的星才有）
      if (isNear && radius > 1.8) {
        const glowR = radius * (4 + star.z * 3)
        const gAlpha = alpha * 0.15 * (0.5 + twinkle * 0.5)
        const glow = ctx.createRadialGradient(sx, sy, 0, sx, sy, glowR)
        glow.addColorStop(0, `rgba(${r},${g},${b},${gAlpha})`)
        glow.addColorStop(1, `rgba(${r},${g},${b},0)`)
        ctx.fillStyle = glow
        ctx.beginPath()
        ctx.arc(sx, sy, glowR, 0, Math.PI * 2)
        ctx.fill()
      }

      // 2. 内层光晕
      if (radius > 1.0) {
        const innerR = radius * 2.5
        const iAlpha = alpha * 0.3
        const glow = ctx.createRadialGradient(sx, sy, 0, sx, sy, innerR)
        glow.addColorStop(0, `rgba(${r},${g},${b},${iAlpha})`)
        glow.addColorStop(1, `rgba(${r},${g},${b},0)`)
        ctx.fillStyle = glow
        ctx.beginPath()
        ctx.arc(sx, sy, innerR, 0, Math.PI * 2)
        ctx.fill()
      }

      // 3. 星核
      ctx.fillStyle = color
      ctx.beginPath()
      ctx.arc(sx, sy, Math.max(0.4, radius * 0.5), 0, Math.PI * 2)
      ctx.fill()
    }
  }
}

// ── 星云雾气（随机位置 + 鼠标跟随） ──
function drawNebula(ctx: CanvasRenderingContext2D) {
  const seed = 42
  const count = 5
  for (let i = 0; i < count; i++) {
    const cx = (Math.sin(seed + i * 2.7 + time * 0.015) * 0.5 + 0.5) * w * 0.8 + w * 0.1
    const cy = (Math.cos(seed + i * 3.1 + time * 0.012) * 0.5 + 0.5) * h * 0.8 + h * 0.1
    const r = 80 + Math.sin(time * 0.02 + i) * 30
    const hue = 220 + i * 20 + Math.sin(time * 0.01 + i) * 10
    const alpha = 0.035 + Math.sin(time * 0.008 + i * 1.5) * 0.015

    const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, r)
    grad.addColorStop(0, `hsla(${hue}, 70%, 60%, ${alpha})`)
    grad.addColorStop(0.5, `hsla(${hue + 15}, 60%, 50%, ${alpha * 0.5})`)
    grad.addColorStop(1, `hsla(${hue}, 60%, 40%, 0)`)
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(cx, cy, r, 0, Math.PI * 2)
    ctx.fill()
  }
}

// ── 鼠标跟踪 ──
function onPointer(e: PointerEvent) {
  mouseX = e.clientX
  mouseY = e.clientY
  if (mouseTimer) clearTimeout(mouseTimer)
  mouseTimer = setTimeout(() => {
    // 慢慢回到中心
  }, 3000)
}

function loop(time: number) {
  draw(time)
  animId = requestAnimationFrame(loop)
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('pointermove', onPointer)
  animId = requestAnimationFrame(loop)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', onPointer)
  cancelAnimationFrame(animId)
  if (mouseTimer) clearTimeout(mouseTimer)
})
</script>

<template>
  <canvas
    ref="canvasRef"
    class="starfield-canvas"
  />
</template>

<style scoped>
.starfield-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
}
</style>

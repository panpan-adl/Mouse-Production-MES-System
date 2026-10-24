<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  ArrowRight,
  Eye,
  EyeOff,
  Factory,
  Lock,
  MousePointer2,
  Network,
  ShieldCheck,
  User,
} from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()

const userCode = ref('')
const password = ref('')
const showPassword = ref(false)
const errorMsg = ref('')
const isLoading = ref(false)
const isShaking = ref(false)
const mouseX = ref(50)
const mouseY = ref(50)
const canvasRef = ref<HTMLCanvasElement | null>(null)

// ── 星星参数 ──
const STAR_COLORS = [
  [255, 255, 255],
  [210, 228, 255],
  [255, 243, 220],
  [190, 218, 255],
  [255, 228, 200],
]
const LAYERS = 3
const BASE_STAR_COUNT = 500

interface Star {
  x: number; y: number
  z: number
  baseRadius: number
  layer: number
  color: [number, number, number]
  phase: number
  twinkleSpeed: number
  dx: number; dy: number
}

let stars: Star[] = []
let W = 0, H = 0
let rafId = 0
let mx = 0, my = 0
let smx = 0, smy = 0
let t = 0

function spawn(w: number, h: number) {
  const scale = Math.sqrt((w * h) / (1920 * 1080))
  const total = Math.floor(BASE_STAR_COUNT * Math.min(scale, 1.8))
  stars = []

  for (let i = 0; i < total; i++) {
    const layer = Math.floor(Math.random() * LAYERS)
    const r = Math.random()
    const z = layer === 0 ? 0.55 + r * 0.45
            : layer === 1 ? 0.20 + r * 0.40
            : 0.02 + r * 0.28

    const baseRadius = layer === 0 ? 0.3 + Math.random() * 2.0
                     : layer === 1 ? 0.2 + Math.random() * 0.9
                     : 0.12 + Math.random() * 0.4

    stars.push({
      x: Math.random() * w,
      y: Math.random() * h,
      z,
      baseRadius,
      layer,
      color: STAR_COLORS[Math.floor(Math.random() * STAR_COLORS.length)] as [number, number, number],
      phase: Math.random() * Math.PI * 2,
      twinkleSpeed: 0.4 + Math.random() * 2.8,
      dx: (Math.random() - 0.5) * 0.02,
      dy: (Math.random() - 0.5) * 0.02,
    })
  }
}

function resize() {
  const c = canvasRef.value
  if (!c) return
  const rect = c.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  W = rect.width
  H = rect.height
  c.width = Math.floor(W * dpr)
  c.height = Math.floor(H * dpr)
  const ctx = c.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  spawn(W, H)
}

// ── 核心渲染 ──
// 关键原则：
// - 星星 = 清晰的圆点，不是模糊光斑
// - 光晕仅保留给最大的几颗星，且极其克制
// - 小星星直接画点，不产生任何渐变（保证清晰）
function render() {
  t += 0.016
  smx += (mx - smx) * 0.05
  smy += (my - smy) * 0.05

  const c = canvasRef.value
  const ctx = c?.getContext('2d')
  if (!c || !ctx || W === 0 || H === 0) {
    rafId = requestAnimationFrame(render)
    return
  }

  ctx.clearRect(0, 0, W, H)

  const px = ((smx - W / 2) / W) * 2
  const py = ((smy - H / 2) / H) * 2

  // 从远到近绘制
  for (let layer = LAYERS - 1; layer >= 0; layer--) {
    for (const star of stars) {
      if (star.layer !== layer) continue

      const sx = star.x + px * (star.z * 28)
          + Math.sin(t * 0.08 + star.phase) * star.dx * 40 * (1 + star.z)
      const sy = star.y + py * (star.z * 28)
          + Math.cos(t * 0.06 + star.phase * 1.3) * star.dy * 40 * (1 + star.z)

      const twinkle = 0.50 + 0.50 * Math.sin(t * star.twinkleSpeed + star.phase)
      const alpha = Math.min(0.95, 0.08 + star.z * 0.50 + twinkle * 0.28)
      if (alpha < 0.02) continue

      let r = star.baseRadius * (0.55 + star.z * 0.60 + twinkle * 0.20)
      if (layer === 0) r *= 1.10

      const [cr, cg, cb] = star.color

      // 只有 >2px 的星才加极淡光晕（避免模糊感）
      if (r > 2.0) {
        const gr = r * 1.8
        const g = ctx.createRadialGradient(sx, sy, r * 0.3, sx, sy, gr)
        g.addColorStop(0, `rgba(${cr},${cg},${cb},${alpha * 0.08})`)
        g.addColorStop(1, `rgba(${cr},${cg},${cb},0)`)
        ctx.fillStyle = g
        ctx.beginPath()
        ctx.arc(sx, sy, gr, 0, Math.PI * 2)
        ctx.fill()
      }

      // 星核：清亮点
      // 小星星（r <= 1）直接画死点，没有模糊
      const coreR = Math.max(0.35, r * 0.50)
      const br = Math.min(255, cr + 60)
      const bg = Math.min(255, cg + 60)
      const bb = Math.min(255, cb + 60)
      ctx.fillStyle = `rgba(${br},${bg},${bb},${Math.min(1, alpha + 0.12)})`
      ctx.beginPath()
      ctx.arc(sx, sy, coreR, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  rafId = requestAnimationFrame(render)
}

function onPointerMove(e: PointerEvent) {
  mx = e.clientX
  my = e.clientY
}

const pageStyle = computed(() => ({
  '--mx': `${mouseX.value}%`,
  '--my': `${mouseY.value}%`,
  '--tilt-x': `${(50 - mouseY.value) / 48}deg`,
  '--tilt-y': `${(mouseX.value - 50) / 52}deg`,
}))

function handlePointerMove(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  mouseX.value = Number((((event.clientX - rect.left) / rect.width) * 100).toFixed(2))
  mouseY.value = Number((((event.clientY - rect.top) / rect.height) * 100).toFixed(2))
}

function resetPointer() {
  mouseX.value = 50
  mouseY.value = 50
}

async function handleLogin() {
  if (!userCode.value.trim() || !password.value.trim()) {
    errorMsg.value = '请输入工号和密码'
    triggerShake()
    return
  }

  if (!/^\d{6}$/.test(userCode.value.trim())) {
    errorMsg.value = '工号应为 6 位数字'
    triggerShake()
    return
  }

  isLoading.value = true
  errorMsg.value = ''

  try {
    const success = await authStore.login(userCode.value.trim(), password.value)
    if (success) {
      router.push(authStore.redirectPath)
    } else {
      errorMsg.value = '工号或密码错误'
      triggerShake()
    }
  } catch {
    errorMsg.value = '登录失败，请稍后重试'
    triggerShake()
  } finally {
    isLoading.value = false
  }
}

function triggerShake() {
  isShaking.value = true
  setTimeout(() => { isShaking.value = false }, 420)
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('pointermove', onPointerMove)
  rafId = requestAnimationFrame(render)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', onPointerMove)
  cancelAnimationFrame(rafId)
})
</script>

<template>
  <main
    class="login-page"
    :style="pageStyle"
    @pointermove="handlePointerMove"
    @pointerleave="resetPointer"
  >
    <!-- 深空底色 -->
    <div class="bg-base" aria-hidden="true">
      <div class="bg-gradient"></div>
    </div>

    <!-- 星光层 -->
    <canvas ref="canvasRef" class="star-canvas"></canvas>

    <!-- 鼠标光晕 -->
    <div class="mouse-glow" aria-hidden="true"></div>

    <!-- 左侧面板 -->
    <section class="intro-panel">
      <div class="brand-row">
        <div class="brand-mark">
          <MousePointer2 class="h-7 w-7" />
        </div>
        <div>
          <p class="eyebrow">Mouse MES</p>
          <h1>鼠标生产制造执行系统</h1>
        </div>
      </div>

      <p class="subtitle">
        连接订单、生产、仓储、质检、设备与财务，用清晰的状态流转管理每一条制造记录。
      </p>

      <div class="feature-board">
        <article class="feature-line">
          <Factory class="h-5 w-5" />
          <div>
            <strong>制造流程追踪</strong>
            <span>从生产任务到质检、入库、出库，关键节点一目了然。</span>
          </div>
        </article>
        <article class="feature-line">
          <Network class="h-5 w-5" />
          <div>
            <strong>业务模块联动</strong>
            <span>缺料请求、采购到货、库存变化形成闭环。</span>
          </div>
        </article>
        <article class="feature-line">
          <ShieldCheck class="h-5 w-5" />
          <div>
            <strong>角色权限清晰</strong>
            <span>不同岗位进入不同工作台，减少误操作。</span>
          </div>
        </article>
      </div>
    </section>

    <!-- 右侧登录 -->
    <section class="login-shell" :class="{ 'animate-shake': isShaking }">
      <div class="login-card">
        <div class="login-card-head">
          <p>欢迎回来</p>
          <h2>登录工作台</h2>
          <span>Manufacturing control center</span>
        </div>

        <form class="login-form" @submit.prevent="handleLogin">
          <div class="field-group">
            <label class="field-title" for="login-user-code">工号</label>
            <div class="input-wrap">
              <User class="input-icon h-5 w-5" />
              <input
                id="login-user-code"
                v-model="userCode"
                type="text"
                inputmode="numeric"
                maxlength="6"
                autocomplete="username"
                aria-label="请输入 6 位工号"
              />
            </div>
          </div>

          <div class="field-group">
            <label class="field-title" for="login-password">密码</label>
            <div class="input-wrap">
              <Lock class="input-icon h-5 w-5" />
              <input
                id="login-password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                aria-label="请输入登录密码"
              />
              <button
                type="button"
                class="password-toggle"
                aria-label="切换密码可见性"
                @mousedown.stop
                @click.stop="showPassword = !showPassword"
              >
                <Eye v-if="showPassword" class="h-5 w-5" />
                <EyeOff v-else class="h-5 w-5" />
              </button>
            </div>
          </div>

          <div v-if="errorMsg" class="error-box">
            {{ errorMsg }}
          </div>

          <button
            type="submit"
            class="login-button"
            :disabled="isLoading || !userCode.trim() || !password.trim()"
          >
            <span>{{ isLoading ? '正在登录...' : '进入系统' }}</span>
            <ArrowRight class="h-5 w-5" />
          </button>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  --mx: 50%;
  --my: 50%;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(390px, 460px);
  gap: clamp(36px, 6vw, 92px);
  align-items: center;
  overflow: hidden;
  padding: clamp(34px, 5.5vw, 76px);
  color: #e8edf5;
  isolation: isolate;
}

/* ── 背景底色 ── */
.bg-base {
  position: absolute;
  inset: 0;
  z-index: -2;
  pointer-events: none;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(900px 550px at 15% 20%, rgba(30, 58, 138, 0.50), transparent 65%),
    radial-gradient(750px 500px at 82% 85%, rgba(15, 23, 42, 0.60), transparent 55%),
    radial-gradient(650px 450px at 50% 50%, rgba(30, 64, 175, 0.18), transparent 50%),
    linear-gradient(165deg, #070b18 0%, #0a1120 30%, #0d1528 55%, #0f1a30 80%, #080e1a 100%);
}

/* ── Canvas 星光层 ── */
.star-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  z-index: -1;
  pointer-events: none;
}

/* ── 鼠标光晕 ── */
.mouse-glow {
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  left: calc(var(--mx) - 250px);
  top: calc(var(--my) - 250px);
  background: radial-gradient(circle, rgba(59, 130, 246, 0.07) 0%, transparent 60%);
  filter: blur(25px);
  z-index: 0;
  pointer-events: none;
  transition: left 0.12s linear, top 0.12s linear;
}

/* ── 左侧面板 ── */
.intro-panel {
  position: relative;
  z-index: 3;
  max-width: 780px;
}

.brand-row {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 20px;
  align-items: start;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 22px;
  color: #2563eb;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.12);
}

.eyebrow {
  margin-bottom: 12px;
  color: #93c5fd;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

h1 {
  max-width: 760px;
  color: #f1f5f9;
  font-size: clamp(42px, 5.5vw, 72px);
  line-height: 1.02;
  font-weight: 950;
  letter-spacing: -0.075em;
}

.subtitle {
  max-width: 620px;
  margin-top: 26px;
  color: #94a3b8;
  font-size: 18px;
  line-height: 1.9;
}

.feature-board {
  display: grid;
  gap: 14px;
  max-width: 620px;
  margin-top: 38px;
}

.feature-line {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 16px 45px rgba(0, 0, 0, 0.10);
}

.feature-line svg {
  color: #2563eb;
}

.feature-line strong {
  display: block;
  color: #1e293b;
  font-size: 16px;
}

.feature-line span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

/* ── 右侧登录 ── */
.login-shell {
  position: relative;
  z-index: 3;
  transform: perspective(1200px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transform-style: preserve-3d;
  transition: transform 150ms ease-out;
}

.login-card {
  position: relative;
  width: min(100%, 456px);
  margin-inline: auto;
  padding: 34px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(24px);
  box-shadow:
    0 24px 70px rgba(0, 0, 0, 0.20),
    inset 0 1px 0 rgba(255, 255, 255, 0.90);
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    radial-gradient(280px 220px at calc(var(--mx)) calc(var(--my)), rgba(37, 99, 235, 0.06), transparent 70%),
    linear-gradient(135deg, rgba(37, 99, 235, 0.05), transparent 40%, rgba(20, 184, 166, 0.04) 100%);
  pointer-events: none;
}

.login-card > * {
  position: relative;
  z-index: 1;
}

.login-card-head {
  margin-bottom: 30px;
}

.login-card-head p {
  color: #2563eb;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h2 {
  margin-top: 8px;
  color: #111827;
  font-size: 32px;
  font-weight: 950;
  letter-spacing: -0.04em;
}

.login-card-head span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.login-form {
  display: grid;
  gap: 18px;
}

.field-title {
  display: block;
  margin-bottom: 8px;
  color: #374151;
  font-size: 14px;
  font-weight: 800;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 54px;
  border: 1px solid rgba(148, 163, 184, 0.30);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.85);
  cursor: text;
  transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.input-wrap:focus-within {
  border-color: rgba(37, 99, 235, 0.50);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.input-icon {
  position: absolute;
  left: 16px;
  color: #2563eb;
  pointer-events: none;
  z-index: 2;
}

.input-wrap input {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  padding: 0 48px;
  border: 0;
  border-radius: inherit;
  outline: none;
  color: #0f172a;
  caret-color: #2563eb;
  background: transparent;
  cursor: text;
}

.input-wrap input::placeholder {
  color: #94a3b8;
}

.password-toggle {
  position: absolute;
  right: 13px;
  z-index: 3;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  color: #94a3b8;
  transition: color 160ms ease, background 160ms ease;
  background: none;
  border: none;
  cursor: pointer;
}

.password-toggle:hover {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
}

.error-box {
  border: 1px solid rgba(248, 113, 113, 0.30);
  border-radius: 14px;
  padding: 11px 14px;
  color: #b91c1c;
  background: rgba(254, 242, 242, 0.85);
  font-size: 14px;
}

.login-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 56px;
  margin-top: 4px;
  border: none;
  border-radius: 16px;
  color: white;
  font-weight: 900;
  font-size: 16px;
  cursor: pointer;
  background: linear-gradient(135deg, #2563eb, #0ea5e9);
  box-shadow: 0 16px 34px rgba(37, 99, 235, 0.30);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.login-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 44px rgba(37, 99, 235, 0.40);
}

.login-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
  box-shadow: none;
}

@keyframes shake {
  0%, 100% {
    transform: perspective(1200px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y)) translateX(0);
  }
  25% {
    transform: perspective(1200px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y)) translateX(-7px);
  }
  50% {
    transform: perspective(1200px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y)) translateX(7px);
  }
  75% {
    transform: perspective(1200px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y)) translateX(-5px);
  }
}

.animate-shake {
  animation: shake 420ms ease-in-out;
}

@media (max-width: 1060px) {
  .login-page {
    grid-template-columns: 1fr;
    align-content: center;
  }

  .intro-panel {
    max-width: none;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 22px;
  }

  .brand-row {
    grid-template-columns: 1fr;
  }

  .brand-mark {
    width: 58px;
    height: 58px;
    border-radius: 18px;
  }

  .login-card {
    padding: 24px;
    border-radius: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-shell,
  .login-button,
  .mouse-glow {
    transition: none;
    transform: none;
  }
}
</style>

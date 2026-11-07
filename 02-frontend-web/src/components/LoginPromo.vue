<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Film } from 'lucide-vue-next'

const props = defineProps<{ motionEnabled: boolean }>()
const player = ref<HTMLVideoElement>()
const failed = ref(false)
let reducedMotion: MediaQueryList | undefined

function syncPlayback() {
  if (!player.value) return
  if (!props.motionEnabled || reducedMotion?.matches || document.hidden) player.value.pause()
  else player.value.play().catch(() => { /* Native controls remain available if autoplay is blocked. */ })
}
watch(() => props.motionEnabled, syncPlayback)
onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion.addEventListener('change', syncPlayback)
  document.addEventListener('visibilitychange', syncPlayback)
  syncPlayback()
})
onBeforeUnmount(() => {
  reducedMotion?.removeEventListener('change', syncPlayback)
  document.removeEventListener('visibilitychange', syncPlayback)
  player.value?.pause()
})
</script>

<template>
  <section class="login-promo" aria-label="Mouse MES 产品宣传片">
    <div class="promo-heading"><span><Film :size="15" /> 从一份订单，到每一次交付</span><small>产品短片 · 30秒</small></div>
    <video ref="player" class="promo-player" controls muted loop playsinline preload="metadata"
      poster="/media/mousemes-intro-poster.jpg" aria-label="Mouse MES 宣传片，可开启声音或全屏观看"
      @error="failed = true">
      <source src="/media/mousemes-intro.mp4" type="video/mp4" />
      <track kind="captions" src="/media/mousemes-intro.zh.vtt" srclang="zh" label="中文字幕" />
      您的浏览器暂不支持视频播放。
    </video>
    <div class="promo-caption"><span>{{ failed ? '短片暂未加载，可刷新页面重试。' : '默认静音播放 · 可开启声音或全屏观看' }}</span><a href="/media/mousemes-intro.mp4" target="_blank" rel="noopener">单独观看 ↗</a></div>
  </section>
</template>

<style scoped>
.login-promo{position:relative;width:100%;max-width:580px;margin-top:26px;border:1px solid #dcd4c5;border-radius:12px;background:#fffdf8;overflow:hidden;box-shadow:0 8px 28px #63513908}
.promo-heading{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:12px 15px;color:#655a49}
.promo-heading>span{display:flex;align-items:center;gap:8px;font-size:12px;font-weight:600}
.promo-heading svg{color:#b77945}.promo-heading small{font-size:10px;color:#a09482;white-space:nowrap}
.promo-player{display:block;width:100%;aspect-ratio:16/9;object-fit:contain;background:#f4f1e9}
.promo-caption{display:flex;justify-content:space-between;gap:10px;padding:10px 15px;font-size:10px;line-height:1.6;color:#9b8e7b}
.promo-caption a{color:#8c6944;white-space:nowrap}.promo-caption a:hover{text-decoration:underline}
@media(max-width:760px){.login-promo{margin-top:20px;max-width:none}.promo-heading{padding:10px 12px}.promo-heading>span{font-size:11px}.promo-caption{padding:9px 12px;font-size:9px}}
</style>

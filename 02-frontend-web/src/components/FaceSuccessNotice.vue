<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
defineProps<{ title: string }>()
const emit = defineEmits<{ done: [] }>()
let timer: ReturnType<typeof setTimeout>
onMounted(() => { timer = setTimeout(() => emit('done'), 3000) })
onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <Teleport to="body">
    <div class="notice-shade">
      <section class="notice" role="status" aria-live="polite">
        <span class="check" aria-hidden="true">✓</span>
        <h2>{{ title }}</h2>
        <slot />
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.notice-shade { position: fixed; inset: 0; z-index: 2000; display: grid; place-items: center; padding: 20px; background: #18211e55; }
.notice { width: min(100%, 440px); padding: 38px 28px; text-align: center; color: #193a30; border-radius: 24px; background: #fff; box-shadow: 0 24px 80px #152a2840; }
.check { display: grid; place-items: center; margin: 0 auto 18px; width: 72px; height: 72px; font-size: 40px; border-radius: 50%; background: #e2f8ee; color: #168354; }
h2 { margin: 0 0 12px; font-size: 27px; font-weight: 700; }
</style>

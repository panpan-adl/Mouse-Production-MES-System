<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
const demo = import.meta.env.VITE_DEMO_MODE === '1'
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <Sidebar />
    <div class="ml-64 min-h-screen">
      <Header />
      <p v-if="demo" class="bg-amber-100 text-amber-900 px-6 py-2 text-sm">隔离验收演示环境 · 数据由完整业务流程生成，仅用于测试与答辩，正式业务库未写入这些测试单据。</p>
      <main class="p-6">
        <slot />
      </main>
    </div>
  </div>
</template>

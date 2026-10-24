<script setup lang="ts">
import { onMounted } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import AgentEntry from './AgentEntry.vue'
import { sidebarCollapsed } from '@/stores/layout'
const demo = import.meta.env.VITE_DEMO_MODE === '1'
onMounted(()=>{if(window.innerWidth<900)sidebarCollapsed.value=true})
</script>
<template>
  <div class="mes-workspace notion-workspace" :class="{'nav-collapsed':sidebarCollapsed}">
    <Sidebar />
    <AgentEntry />
    <div class="workspace-body">
      <Header />
      <div v-if="demo" class="environment-notice"><span class="environment-dot"></span><strong>演示环境</strong><span>当前使用隔离演示数据</span><span class="notice-note">用于测试与答辩展示</span></div>
      <main class="workspace-content"><slot /></main>
      <footer class="workspace-footer"><span>MOUSE MES <span class="footer-divider">/</span> 制造执行管理平台</span><span>让每一道工序清晰可见</span></footer>
    </div>
  </div>
</template>

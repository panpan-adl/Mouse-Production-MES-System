<script setup lang="ts">
import { onMounted,onBeforeUnmount } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import AgentEntry from './AgentEntry.vue'
import { sidebarCollapsed } from '@/stores/layout'
function fitNavigation(){if(window.innerWidth<900)sidebarCollapsed.value=true}
onMounted(()=>{fitNavigation();window.addEventListener('resize',fitNavigation)})
onBeforeUnmount(()=>window.removeEventListener('resize',fitNavigation))
</script>
<template>
  <div class="mes-workspace notion-workspace" :class="{'nav-collapsed':sidebarCollapsed}">
    <Sidebar />
    <AgentEntry />
    <div class="workspace-body">
      <Header />
      <main class="workspace-content"><slot /></main>
      <footer class="workspace-footer"><span>MOUSE MES <span class="footer-divider">/</span> 制造执行管理平台</span><span>让每一道工序清晰可见</span></footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted,onBeforeUnmount,computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import AgentEntry from './AgentEntry.vue'
import ProductionNavigation from './ProductionNavigation.vue'
import WarehouseNavigation from './WarehouseNavigation.vue'
import ModuleWorkflow from './ModuleWorkflow.vue'
import SystemNavigation from './SystemNavigation.vue'
import { sidebarCollapsed } from '@/stores/layout'
defineProps<{ fullWidth?: boolean; edgeToEdge?: boolean }>()
const themeRoute=useRoute()
const unifiedTheme=computed(()=>/^\/(equipment|system|foundation|inventory)(\/|$)/.test(themeRoute.path)||['/attendance','/cfo','/assistant','/automation','/production/equipment-status','/notifications'].includes(themeRoute.path))

function fitNavigation(){if(window.innerWidth<900)sidebarCollapsed.value=true}
onMounted(()=>{fitNavigation();window.addEventListener('resize',fitNavigation)})
onBeforeUnmount(()=>window.removeEventListener('resize',fitNavigation))
</script>
<template>
  <div class="mes-workspace notion-workspace" :class="{'nav-collapsed':sidebarCollapsed,'unified-module-theme':unifiedTheme}">
    <Sidebar />
    <AgentEntry />
    <div class="workspace-body">
      <Header />
      <main class="workspace-content" :class="{'workspace-content-full':fullWidth,'workspace-content-edge':edgeToEdge}"><ModuleWorkflow /><ProductionNavigation /><WarehouseNavigation /><SystemNavigation /><slot /></main>

      <footer class="workspace-footer"><span>MOUSE MES <span class="footer-divider">/</span> 制造执行管理平台</span><span>让每一道工序清晰可见</span></footer>
    </div>
  </div>
</template>
<style scoped>
.notion-workspace .workspace-content.workspace-content-full{max-width:none;width:100%;padding:24px;min-width:0}
.workspace-content:has(>.production-navigation) :deep(.mes-hero-label){display:none}
.workspace-content:has(>.production-navigation)>:not(.production-navigation) :deep(h1){font-size:20px;letter-spacing:0}
@media(max-width:800px){.notion-workspace .workspace-content.workspace-content-full{padding:12px}}
.notion-workspace .workspace-content.workspace-content-edge{max-width:none;width:100%;padding-left:0;padding-right:0}
</style>

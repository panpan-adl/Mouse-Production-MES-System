<script setup lang="ts">
import {Bot} from 'lucide-vue-next'
import {useRoute} from 'vue-router'
const route=useRoute()
</script>
<template><router-link v-if="route.path!=='/assistant'" to="/assistant" class="agent-entry" aria-label="打开 Skill 业务智能体"><Bot :size="21"/><span>业务智能体</span></router-link></template>
<style scoped>
.agent-entry{position:fixed;right:25px;bottom:25px;z-index:35;display:flex;align-items:center;gap:9px;padding:12px 17px;border:1px solid #a7d4ed;border-radius:30px;background:linear-gradient(110deg,#f6fcfff2,#dceefff2);color:#2376ad;box-shadow:0 8px 30px #407caf24;backdrop-filter:blur(12px);font-size:12px;transition:transform .2s}.agent-entry:hover{transform:translateY(-3px)}@media(max-width:760px){.agent-entry{right:14px;bottom:16px;padding:12px}.agent-entry span{display:none}}
</style>

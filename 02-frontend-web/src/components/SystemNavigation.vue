<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import { systemSections } from '@/utils/systemNavigation'
const route=useRoute(),auth=useAuthStore()
const section=computed(()=>systemSections.find(s=>s.tabs.some(t=>t.path===route.path)))
const tabs=computed(()=>section.value?.tabs.filter(t=>canVisit(Number(auth.effectiveRoleId),t.path)&&auth.hasMenu(t.path))||[])
</script>
<template>
  <section v-if="section" class="system-navigation" :aria-label="section.title">
    <h1>{{section.title}}</h1><p>{{section.description}}</p>
    <nav v-if="tabs.length>1" :aria-label="`${section.title}功能`">
      <router-link v-for="tab in tabs" :key="tab.path" :to="tab.path.startsWith('/foundation/')?{path:tab.path,query:route.query}:tab.path" :class="{active:route.path===tab.path}" :aria-current="route.path===tab.path?'page':undefined">{{tab.label}}</router-link>
    </nav>
  </section>
</template>
<style scoped>
.system-navigation{margin-bottom:22px}.system-navigation h1{font-size:25px;font-weight:600;color:#665239}.system-navigation>p{font-size:12px;color:#9a8f7e;margin-top:7px}.system-navigation nav{display:flex;gap:6px;flex-wrap:wrap;border-bottom:1px solid #e5dfd4;padding:12px 0;margin-top:4px}.system-navigation a{padding:8px 13px;border-radius:6px;font-size:12px;color:#998b77}.system-navigation a:hover{background:#f3ede2;color:#80633e}.system-navigation a.active{background:#eee6d8;color:#80633e;font-weight:600}
</style>

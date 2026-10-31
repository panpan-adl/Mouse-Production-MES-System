<script setup lang="ts">
import { computed } from 'vue'
import { SlidersHorizontal, Users, ShieldCheck, ScrollText } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import { systemSections } from '@/utils/systemNavigation'
const route=useRoute(),auth=useAuthStore()
const section=computed(()=>systemSections.find(s=>s.tabs.some(t=>t.path===route.path)))
const sectionIcon=computed(()=>section.value?.title==='组织与人员'?Users:section.value?.title==='角色与权限'?ShieldCheck:section.value?.title==='日志管理'?ScrollText:SlidersHorizontal)
const tabs=computed(()=>section.value?.tabs.filter(t=>canVisit(Number(auth.effectiveRoleId),t.path)&&auth.hasMenu(t.path))||[])
</script>
<template>
  <section v-if="section" class="system-navigation" :aria-label="section.title">
    <div class="system-module-heading"><div class="system-module-icon"><component :is="sectionIcon" :size="23" :stroke-width="1.7" /></div><div><h1>{{section.title}}</h1><p>{{section.description}}</p></div></div>
    <nav v-if="tabs.length>1" :aria-label="`${section.title}功能`">
      <router-link v-for="tab in tabs" :key="tab.path" :to="tab.path.startsWith('/foundation/')?{path:tab.path,query:route.query}:tab.path" :class="{active:route.path===tab.path}" :aria-current="route.path===tab.path?'page':undefined">{{tab.label}}</router-link>
    </nav>
  </section>
</template>
<style scoped>
.system-navigation{margin-bottom:24px;padding:24px 26px 0;background:#fff;border:1px solid #e8e3da;border-radius:12px}
.system-module-heading{display:flex;align-items:center;gap:15px;padding-bottom:22px}
.system-module-icon{display:grid;place-items:center;flex-shrink:0;width:48px;height:48px;border:1px solid #e8dfd0;background:#f6f1e8;border-radius:12px;color:#987344}
.system-navigation .system-module-heading h1{font-size:23px;font-weight:600;letter-spacing:0;color:#403a31;line-height:1.4}
.system-module-heading p{font-size:12px;color:#92897c;margin-top:6px;line-height:1.6}
.system-navigation nav{display:flex;gap:28px;overflow-x:auto;border-top:1px solid #f0ece5;scrollbar-width:thin}
.system-navigation a{position:relative;flex-shrink:0;padding:15px 2px;font-size:13px;color:#8e867a;transition:color .15s}
.system-navigation a:hover{color:#765a36}
.system-navigation a.active{color:#896439;font-weight:600}
.system-navigation a.active:after{content:'';position:absolute;bottom:0;left:0;right:0;height:3px;border-radius:3px 3px 0 0;background:#ac8350}
.system-navigation a:focus-visible{outline:2px solid #ac8350;outline-offset:-3px;border-radius:4px}
@media(max-width:700px){.system-navigation{padding:18px 16px 0}.system-module-heading{align-items:flex-start;gap:12px}.system-navigation .system-module-heading h1{font-size:20px}.system-navigation nav{gap:22px}.system-module-icon{width:40px;height:40px}}
</style>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import { productionSections } from '@/utils/productionNavigation'
const route=useRoute(),auth=useAuthStore()
const allowed=(path:string)=>canVisit(Number(auth.effectiveRoleId),path)&&auth.hasMenu(path)
const section=computed(()=>productionSections.find(g=>g.tabs.some(t=>t.path===route.path)))
// FoundationCenter already provides tabs for individual configuration resources.
const tabs=computed(()=>section.value?.tabs.filter(t=>t.label&&allowed(t.path))||[])
const configuration=computed(()=>section.value&&['班组与排班','点检与保养'].includes(section.value.title)?section.value.tabs.find(t=>!t.label&&allowed(t.path)):undefined)
const isConfiguration=computed(()=>route.path.startsWith('/foundation/'))
</script>
<template>
  <section v-if="section&&section.title!=='产品与工艺'" class="production-navigation" :aria-label="section.title">
    <h1>{{section.title}}</h1><p>{{section.description}}</p>
    <nav :aria-label="`${section.title}功能`">
      <router-link v-for="t in tabs" :key="t.path" :to="t.path" :class="{active:route.path===t.path}" :aria-current="route.path===t.path?'page':undefined">{{t.label}}</router-link>
      <router-link v-if="configuration" :to="configuration.path" :class="{active:isConfiguration}" :aria-current="isConfiguration?'page':undefined">{{section.title==='点检与保养'?'点检配置与记录':'班组与班次配置'}}</router-link>
      <router-link v-if="section.title==='生产执行'&&allowed('/production/schedules')" to="/production/schedules" class="shortcut">查看排班 →</router-link>
      <router-link v-if="section.title==='生产执行'&&allowed('/mes')&&[1,2,9].includes(Number(auth.effectiveRoleId))" :to="{path:'/dashboard',query:{view:'operations',tab:'reports'}}" class="shortcut">报工审批 →</router-link>
    </nav>
  </section>
</template>
<style scoped>
.production-navigation{margin-bottom:22px}.production-navigation h1{font-size:25px;font-weight:600;color:#665239}.production-navigation>p{font-size:12px;color:#9a8f7e;margin-top:7px}.production-navigation nav{display:flex;gap:6px;flex-wrap:wrap;align-items:center;border-bottom:1px solid #e5dfd4;padding:12px 0;margin-top:4px}.production-navigation a{padding:8px 13px;border-radius:6px;font-size:12px;color:#998b77}.production-navigation a:hover{background:#f3ede2;color:#80633e}.production-navigation a.active{background:#eee6d8;color:#80633e;font-weight:600}.production-navigation .shortcut{font-size:11px;padding-left:10px}
</style>

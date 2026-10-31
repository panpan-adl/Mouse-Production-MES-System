<script setup lang="ts">
import { computed, inject, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import { warehouseDocumentPaths, warehouseSections } from '@/utils/warehouseNavigation'
const route=useRoute(),auth=useAuthStore()
const inbound = inject<{ selected: Ref<string>; disabled: Ref<boolean>; select: (kind: string) => void } | null>('warehouseInbound', null)
const localInbound = computed(() => ['/inventory/in','/inventory/out'].includes(route.path) && inbound)
const inboundKind = (path: string) => Object.keys(warehouseDocumentPaths).find(kind => warehouseDocumentPaths[kind] === path) || ''

// Procurement retains its own receipts navigation; warehouse overview links to it.
const section=computed(()=>route.path==='/purchase/receipts'?undefined:warehouseSections.find(s=>s.tabs.some(t=>t.path===route.path)))
const tabs=computed(()=>section.value?.tabs.filter(t=>canVisit(Number(auth.effectiveRoleId),t.path)&&auth.hasMenu(t.path))||[])
</script>
<template>
  <section v-if="section" class="warehouse-navigation" :aria-label="section.title">
    <h1>{{section.title}}</h1><p>{{section.description}}</p>
    <nav v-if="tabs.length>1" :aria-label="`${section.title}功能`">
      <template v-if="localInbound"><button v-for="tab in tabs.filter(t=>t.path!=='/inventory/packages')" :key="tab.path" type="button" :disabled="inbound?.disabled.value" :class="{active:inbound?.selected.value===inboundKind(tab.path)}" :aria-pressed="inbound?.selected.value===inboundKind(tab.path)" @click="inbound?.select(inboundKind(tab.path))">{{tab.label}}</button><router-link v-if="tabs.some(t=>t.path==='/inventory/packages')" to="/inventory/packages">装箱作业</router-link></template>
      <template v-else><router-link v-for="tab in tabs" :key="tab.path" :to="tab.path.startsWith('/foundation/')?{path:tab.path,query:route.query}:tab.path" :class="{active:route.path===tab.path}" :aria-current="route.path===tab.path?'page':undefined">{{tab.label}}</router-link></template>
    </nav>
  </section>
</template>
<style scoped>
.warehouse-navigation{margin-bottom:22px}.warehouse-navigation h1{font-size:25px;font-weight:600;color:#665239}.warehouse-navigation>p{font-size:12px;color:#9a8f7e;margin-top:7px}.warehouse-navigation nav{display:flex;gap:6px;flex-wrap:wrap;border-bottom:1px solid #e5dfd4;padding:12px 0;margin-top:4px}.warehouse-navigation :is(a,button){padding:8px 13px;border-radius:6px;font-size:12px;color:#998b77}.warehouse-navigation :is(a,button):hover{background:#f3ede2;color:#80633e}.warehouse-navigation :is(a,button).active{background:#eee6d8;color:#80633e;font-weight:600}
</style>

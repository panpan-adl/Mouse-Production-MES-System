<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'

const props = defineProps<{ group: 'orders' | 'receipts' }>()
const route = useRoute()
const auth = useAuthStore()
const groups = {
  orders: { title: '采购申请与订单', description: '承接物料缺口，核对需求后向供应商下单。', tabs: [
    { path: '/inventory/material-requests', label: '物料需求' },
    { path: '/purchase/requests', label: '采购需求' },
    { path: '/purchase/order', label: '采购订单' },
  ] },
  receipts: { title: '采购收货与退货', description: '登记实际到货与检验结果，按原采购来源和收货批次办理退货。', tabs: [
    { path: '/purchase/receipts', label: '采购收货' },
    { path: '/purchase/returns', label: '供应商退货' },
  ] },
}
const section = computed(() => groups[props.group])
const tabs = computed(() => section.value.tabs.filter(tab =>
  canVisit(Number(auth.effectiveRoleId), tab.path) && auth.hasMenu(tab.path)))
</script>

<template>
  <section class="mb-5" aria-label="采购模块导航">
    <h1 class="text-2xl font-semibold text-gray-900">{{ section.title }}</h1>
    <p class="mt-2 text-sm text-gray-500">{{ section.description }}</p>
    <nav class="mt-4 flex gap-2 border-b border-gray-200" :aria-label="section.title">
      <router-link v-for="tab in tabs" :key="tab.path" :to="tab.path"
        class="px-4 py-3 text-sm border-b-2"
        :class="route.path === tab.path ? 'border-blue-600 text-blue-700 font-semibold' : 'border-transparent text-gray-600 hover:text-blue-600'"
        :aria-current="route.path === tab.path ? 'page' : undefined">{{ tab.label }}</router-link>
    </nav>
  </section>
</template>

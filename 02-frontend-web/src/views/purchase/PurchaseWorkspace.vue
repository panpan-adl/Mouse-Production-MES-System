<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import MaterialRequest from '@/views/inventory/MaterialRequest.vue'
import PurchaseRequest from './PurchaseRequest.vue'
import PurchaseOrder from './PurchaseOrder.vue'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'

type Stage = 'materials' | 'requests' | 'orders'
type Panel = { refresh: () => Promise<void>; focus?: (id: number) => Promise<void> }
const auth = useAuthStore(), route = useRoute()
const materials = ref<Panel>(), requests = ref<Panel>(), orders = ref<Panel>()
const message = ref('')
const stages = [
  { key: 'materials' as const, title: '物料需求', path: '/inventory/material-requests', description: '登记用料并核对库存，缺料时生成采购需求。' },
  { key: 'requests' as const, title: '采购需求', path: '/purchase/requests', description: '承接已提交的缺料需求，选择供应商并生成采购订单。' },
  { key: 'orders' as const, title: '采购订单', path: '/purchase/order', description: '核对订单明细，完成审批后继续办理收货。' },
]
const visibleStages = computed(() => stages.filter(s => canVisit(Number(auth.effectiveRoleId), s.path) && auth.hasMenu(s.path)))
function register(stage: Stage, instance: unknown) {
  const targets = { materials, requests, orders }; targets[stage].value = instance as Panel | undefined
}
const panels = () => ({ materials: materials.value, requests: requests.value, orders: orders.value })
function scrollTo(stage: Stage) {
  document.getElementById(`purchase-${stage}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
async function locateRoute() {
  await nextTick()
  const stage = visibleStages.value.find(s => s.path === route.path)
  if (!stage) return
  try {
    const id = Number(stage.key === 'orders' ? route.query.orderId : route.query.requestId)
    if (id) await panels()[stage.key]?.focus?.(id)
    scrollTo(stage.key)
  } catch (e: any) { message.value = e.message || '关联记录加载失败，请刷新对应区块。' }
}
async function progressed(stage: 'requests' | 'orders', id: number) {
  message.value = stage === 'requests' ? '采购需求已生成，请在下方选择供应商生成订单。' : '采购订单已生成，已定位到对应订单。'
  await Promise.all([materials.value?.refresh(), requests.value?.refresh(), orders.value?.refresh()])
  if (!visibleStages.value.some(s => s.key === stage)) {
    message.value += ' 后续步骤由具有相应权限的岗位处理。'
    return
  }
  await panels()[stage]?.focus?.(id)
  scrollTo(stage)
}
watch(() => route.fullPath, locateRoute)
onMounted(locateRoute)
</script>

<template>
  <MainLayout full-width>
    <header class="mb-5">
      <h1 class="text-2xl font-semibold">采购申请与订单</h1>
    </header>
    <nav class="purchase-stage-nav" aria-label="采购流程区块">
      <button v-for="stage in visibleStages" :key="stage.key" type="button" @click="scrollTo(stage.key)">{{ stage.title }}</button>
    </nav>
    <p v-if="message" role="status" class="bg-blue-50 text-blue-800 p-3 rounded mb-4">{{ message }}</p>
    <section v-for="stage in visibleStages" :id="`purchase-${stage.key}`" :key="stage.key" class="purchase-stage" :aria-label="stage.title">
      <MaterialRequest v-if="stage.key === 'materials'" :ref="el => register('materials', el)" embedded @progressed="progressed" />
      <PurchaseRequest v-else-if="stage.key === 'requests'" :ref="el => register('requests', el)" embedded @progressed="progressed" />
      <PurchaseOrder v-else :ref="el => register('orders', el)" embedded />
    </section>
  </MainLayout>
</template>

<style scoped>
.purchase-stage-nav{position:sticky;top:64px;z-index:30;display:flex;gap:12px;flex-wrap:wrap;background:#faf9f6;border-bottom:1px solid #e5dfd4;padding:12px 0;margin-bottom:24px}
.purchase-stage-nav button{padding:8px 16px;border-radius:6px;color:#80633e;background:#eee6d8}
.purchase-stage{scroll-margin-top:140px;border-bottom:1px solid #e5dfd4;padding-bottom:32px;margin-bottom:32px;min-width:0}
.purchase-stage :deep(h1){font-size:22px}
.purchase-stage :deep(.panel){min-width:0}
.purchase-stage :deep(.overflow-x-auto){overflow-x:visible;min-width:0}
.purchase-stage :deep(.data-table){width:100%;min-width:0!important;table-layout:fixed}
.purchase-stage :deep(.data-table th),.purchase-stage :deep(.data-table td){white-space:normal!important;overflow-wrap:anywhere;padding:14px 8px!important;vertical-align:middle;text-align:center!important;line-height:1.7}
.purchase-stage :deep(.data-table .link-btn){white-space:normal;display:inline-flex;align-items:center;justify-content:center;gap:4px;margin:3px 0;max-width:100%;vertical-align:middle;line-height:1.7}
.purchase-stage :deep(.data-table .link-btn svg){flex-shrink:0;margin-right:0}
.purchase-stage :deep(.data-table .tag-green),.purchase-stage :deep(.data-table .tag-blue),.purchase-stage :deep(.data-table .tag-yellow),.purchase-stage :deep(.data-table .tag-red),.purchase-stage :deep(.data-table .tag-gray){white-space:normal}
.purchase-stage :deep(.mb-5.overflow-x-auto>.grid){min-width:0!important;grid-template-columns:minmax(0,1fr) minmax(120px,180px) 100px 100px}
#purchase-orders :deep(.mb-5.overflow-x-auto>.grid){grid-template-columns:minmax(0,1fr) minmax(120px,180px) 100px}
.purchase-stage :deep(.data-table th:first-child){width:14%}
#purchase-materials :deep(.data-table th:nth-child(1)){width:18%}
#purchase-materials :deep(.data-table th:nth-child(2)){width:8%}
#purchase-materials :deep(.data-table th:nth-child(3)){width:13%}
#purchase-materials :deep(.data-table th:nth-child(4)){width:18%}
#purchase-materials :deep(.data-table th:nth-child(5)),#purchase-materials :deep(.data-table th:nth-child(6)),#purchase-materials :deep(.data-table th:nth-child(7)){width:6%}
#purchase-materials :deep(.data-table th:nth-child(8)){width:9%}
#purchase-materials :deep(.data-table th:nth-child(9)){width:7%}
#purchase-materials :deep(.data-table th:nth-child(10)){width:9%}
#purchase-materials :deep(.data-table th),#purchase-materials :deep(.data-table td){padding:12px 4px!important;font-size:12px;line-height:1.5}
@media(min-width:1500px){#purchase-materials :deep(.data-table td){overflow-wrap:break-word;word-break:normal}}
#purchase-requests :deep(.data-table th:nth-child(1)){width:18%}
#purchase-requests :deep(.data-table th:nth-child(2)){width:8%}
#purchase-requests :deep(.data-table th:nth-child(3)){width:13%}
#purchase-requests :deep(.data-table th:nth-child(4)){width:18%}
#purchase-requests :deep(.data-table th:nth-child(5)),#purchase-requests :deep(.data-table th:nth-child(6)),#purchase-requests :deep(.data-table th:nth-child(7)){width:5%}
#purchase-requests :deep(.data-table th:nth-child(8)){width:8%}
#purchase-requests :deep(.data-table th:nth-child(9)){width:12%}
#purchase-requests :deep(.data-table th:nth-child(10)){width:8%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(1)){width:18%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(2)){width:19%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(3)),#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(4)){width:8%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(5)){width:9%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(6)){width:8%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(7)){width:5%}
#purchase-orders :deep(.panel>.overflow-x-auto>.data-table th:nth-child(8)){width:25%}
#purchase-requests :deep(.data-table th),#purchase-requests :deep(.data-table td),#purchase-orders :deep(.data-table th),#purchase-orders :deep(.data-table td){padding:12px 4px!important;font-size:12px;line-height:1.5}
#purchase-orders :deep(.data-table td:last-child .link-btn),#purchase-orders :deep(.data-table td:last-child .danger-link){margin:3px 4px;white-space:nowrap}
@media(min-width:1500px){#purchase-requests :deep(.data-table td),#purchase-orders :deep(.data-table td){overflow-wrap:break-word;word-break:normal}}
@media(max-width:800px){.purchase-stage :deep(.mb-5.overflow-x-auto>.grid),#purchase-orders :deep(.mb-5.overflow-x-auto>.grid){grid-template-columns:minmax(0,1fr) minmax(0,1fr)}.purchase-stage :deep(.data-table th),.purchase-stage :deep(.data-table td){padding:8px 3px!important;font-size:12px}}
</style>

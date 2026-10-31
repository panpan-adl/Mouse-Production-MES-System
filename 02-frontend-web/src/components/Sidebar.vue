<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  Bot,
  Calendar,
  CheckCircle,
  ClipboardCheck,
  ChevronLeft,
  ChevronRight,
  FileText,
  LayoutDashboard,
  LogOut,
  MousePointer2,
  Package,
  Settings,
  ShoppingCart,

  Users,
  Wallet,
  Wrench,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from './UserAvatar.vue'
import { sidebarCollapsed } from '@/stores/layout'
import { foundationGroups } from '@/utils/foundationNavigation'
import { canVisit } from '@/utils/permissions'
import { productionSections } from '@/utils/productionNavigation'
import { warehouseSections } from '@/utils/warehouseNavigation'
import { systemSections } from '@/utils/systemNavigation'

interface ChildMenuItem {
  relatedPaths?: string[]
  path: string
  name: string
  icon: any
  roles?: number[]
}

interface MenuItem {
  path?: string
  name: string
  icon: any
  children?: ChildMenuItem[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const collapsed = sidebarCollapsed
// Menu entries are computed copies; keep interactive state in a reactive store.
const expandedMenus = ref<Record<string, boolean>>({})

function menuExpanded(item: MenuItem) {
  return expandedMenus.value[item.name] ?? isChildActive(item)
}

function toggleMenu(item: MenuItem) {
  if (collapsed.value) {
    collapsed.value = false
    expandedMenus.value[item.name] = true
    return
  }
  expandedMenus.value[item.name] = !menuExpanded(item)
}

const allMenuItems = ref<MenuItem[]>([
  {path:'/assistant',name:'Skill业务智能体',icon:Bot},
  {path:'/mes',name:'MES运营中心',icon:Package},
  {path:'/automation',name:'自动任务与提醒',icon:ClipboardCheck},
  { path: '/', name: '首页', icon: LayoutDashboard },
  { path: '/foundation/units', name: '基础配置中心', icon: Settings },
  { path: '/manufacturing', name: '制造与质量放行', icon: ClipboardCheck },
  { path: '/fulfillment', name: '生产与仓储流程总览', icon: Package },
  {
    name: '销售订单',
    icon: FileText,
    children: [
      { path: '/order/customer', name: '客户管理', icon: Users },
      { path: '/order/list', name: '订单列表', icon: FileText },
    ],
  },
  {
    name: '采购管理',
    icon: ShoppingCart,
    children: [
      { path: '/inventory/material-requests', name: '物料需求', icon: ClipboardCheck },
      { path: '/purchase/requests', name: '采购需求', icon: ClipboardCheck },
      { path: '/purchase/order', name: '采购订单', icon: FileText },
      {path:'/purchase/receipts',name:'采购收货',icon:Package,roles:[1,2,4,5,6,10]},
      { path: '/purchase/returns', name: '供应商退货', icon: FileText, roles: [1,2,5,6,10] },
      { path: '/purchase/supplier', name: '供应商管理', icon: Users },
    ],
  },
  {
    name: '生产管理',
    icon: MousePointer2,
    children: [
      { path: '/production/work-orders', name: '生产工单', icon: ClipboardCheck, roles: [1,2,3,4,5] },
      {path:'/production/schedules',name:'班组排班计划',icon:Calendar},
      { path: '/production/plan', name: '生产计划', icon: FileText },
      { path: '/production/task', name: '生产任务', icon: CheckCircle },
      { path: '/production/report', name: '生产报工', icon: BarChart3 },
    ],
  },
  {
    name: '质量管理',
    icon: CheckCircle,
    children: [
      { path: '/quality/tasks', name: '质检任务', icon: ClipboardCheck, relatedPaths: ['/quality/standard', ...foundationGroups.filter(group => group.module === '质量管理').flatMap(group => group.keys.map(key => `/foundation/${key}`))] },
      { path: '/quality/check', name: '质检记录', icon: CheckCircle },
    ],
  },
  {
    name: '仓库管理',
    icon: Package,
    children: [
      { path: '/inventory/warehouse', name: '仓库库存', icon: Package },
      { path: '/foundation/warehouses', name: '仓库设置', icon: Package },
      { path: '/foundation/areas', name: '库区设置', icon: Package },
      { path: '/foundation/locations', name: '库位设置', icon: Package },
      { path: '/inventory/material', name: '物料管理', icon: Package },
      { path: '/inventory/in', name: '入库管理', icon: ShoppingCart },
      { path: '/inventory/out', name: '出库管理', icon: ShoppingCart },
      { path: '/inventory/production-issue', name: '生产领料', icon: Package, roles:[1,2,5] },
      { path: '/inventory/production-return', name: '生产退料', icon: Package, roles:[1,2,5] },
      { path: '/inventory/product-receipts', name: '产成品入库', icon: Package, roles:[1,2,5] },
      {path:'/inventory/shipments',name:'销售发货',icon:Package,roles:[1,2,4,5,7]},
      {path:'/inventory/trace',name:'库存与批次追溯',icon:Package},
      { path: '/inventory/packages', name: '装箱管理', icon: Package, roles:[1,2,5,7] },
    ],
  },
  { path: '/attendance', name: '考勤管理', icon: Calendar },
  {
    name: '设备管理',
    icon: Wrench,
    children: [
      { path: '/equipment/list', name: '设备台账', icon: Wrench },
      {path:'/production/equipment-status',name:'设备报修',icon:Wrench,roles:[1,2,3]},
      { path: '/equipment/maintenance', name: '设备维修管理', icon: Settings },
      { path: '/equipment/periodic', name: '定期保养计划', icon: Calendar, roles: [8] },
    ],
  },
  { path: '/cfo', name: '财务看板', icon: Wallet },
  {
    name: '系统管理',
    icon: Settings,
    children: [
      { path: '/system/user', name: '员工管理', icon: Users },
      { path: '/system/role', name: '岗位与权限管理', icon: Settings },
      { path: '/system/department', name: '部门管理', icon: Users },
      { path: '/system/logs', name: '日志管理', icon: FileText },
    ],
  },
])

// Each domain opens its own configuration group rather than one global directory.
for (const group of foundationGroups) {
  if (['基础配置中心', '仓库管理', '质量管理'].includes(group.module)) continue
  const menu = allMenuItems.value.find(item => item.name === group.module)
  menu?.children?.push({ path: `/foundation/${group.keys[0]}`, name: group.title, icon: Settings })
}

// Consolidate cross-module shortcuts and put operational modules in workflow order.
const takeEntry = (path: string): ChildMenuItem => {
  const index = allMenuItems.value.findIndex(item => item.path === path)
  const [entry] = allMenuItems.value.splice(index, 1)
  return { path, name: entry.name, icon: entry.icon }
}
takeEntry('/mes')
takeEntry('/fulfillment')
const assistantEntries = [takeEntry('/assistant'), takeEntry('/automation')]
const sharedEntry = takeEntry('/foundation/units')
sharedEntry.name = '通用基础资料'
sharedEntry.roles = [1,2,4,5,6,7,8,11]
const manufacturingEntry = takeEntry('/manufacturing')
manufacturingEntry.roles = [1,2,3,4,5]
allMenuItems.value.find(item=>item.name==='生产管理')?.children?.push(manufacturingEntry)
allMenuItems.value.find(item=>item.name==='系统管理')?.children?.unshift(sharedEntry)
allMenuItems.value.push({ name: '智能与提醒', icon: Bot, children: assistantEntries })

// Move the existing status page, retaining its worker-only access rules.
const productionMenu=allMenuItems.value.find(item=>item.name==='生产管理')!
const equipmentMenu=allMenuItems.value.find(item=>item.name==='设备管理')!
const statusIndex=productionMenu.children!.findIndex(child=>child.path==='/production/equipment-status')
if(statusIndex>=0)equipmentMenu.children!.push(...productionMenu.children!.splice(statusIndex,1))

const moduleOrder = ['首页','销售订单','采购管理','生产管理','质量管理','仓库管理','设备管理','考勤管理','财务看板','系统管理','智能与提醒']
allMenuItems.value.sort((a,b)=>moduleOrder.indexOf(a.name)-moduleOrder.indexOf(b.name))
const childOrder: Record<string,string[]> = {
  销售订单: ['客户管理','订单列表'],
  采购管理: ['供应商管理','物料需求','采购需求','采购订单','采购收货','供应商退货'],
  生产管理: ['产品与工艺','班组与排班配置','生产计划','生产工单','班组排班计划','生产任务','生产报工','制造与质量放行','设备状态管理'],
  质量管理: ['质检任务','质检记录'],
  仓库管理: ['仓库设置','库区设置','库位设置','物料管理','仓库库存','入库管理','出库管理','生产领料','生产退料','产成品入库','装箱管理','销售发货','库存与批次追溯','物料请求'],
  设备管理: ['设备台账','设备点检配置','定期保养计划','设备维修管理'],
  系统管理: ['通用基础资料','部门管理','员工管理','岗位与权限管理','组织与系统配置','日志管理'],
}
for(const item of allMenuItems.value){
  const order=childOrder[item.name]
  if(order)item.children?.sort((a,b)=>order.indexOf(a.name)-order.indexOf(b.name))
}

const roleMenus: Record<number, string[]> = {
  1: ['首页', '生产管理', '质量管理', '仓库管理', '设备管理', '考勤管理'],
  2: ['首页', '生产管理', '设备管理', '考勤管理'],
  3: ['首页','仓库管理', '生产管理', '设备管理', '考勤管理'],
  4: ['首页', '质量管理', '考勤管理'],
  5: ['首页', '仓库管理', '采购管理', '考勤管理'],
  6: ['首页', '采购管理', '仓库管理', '考勤管理'],
  7: ['首页', '销售订单', '考勤管理'],
  8: ['首页', '设备管理', '考勤管理'],
  9: ['首页', '生产管理', '质量管理', '仓库管理', '采购管理', '销售订单', '设备管理', '财务看板', '系统管理', '考勤管理'],
  10: ['首页', '财务看板', '采购管理', '仓库管理', '质量管理', '设备管理'],
  11: ['首页', '系统管理', '考勤管理'],
}

const roleChildMenus: Record<number, Record<string, string[]>> = {
  3: {
    生产管理: ['生产工单', '生产任务', '设备状态管理','班组排班计划'],
    采购管理: ['物料需求'],
    设备管理: ['设备状态管理'],
  },
  5: {
    仓库管理: ['仓库库存', '仓库设置', '库区设置', '库位设置', '物料管理', '入库管理', '出库管理', '物料请求','生产领料','生产退料','产成品入库','装箱管理','销售发货','库存与批次追溯'],
    采购管理: ['物料需求','采购订单','采购收货','供应商退货'],
  },
  6: {
    采购管理: ['物料需求','采购需求', '采购订单', '供应商管理','采购收货','供应商退货'],
  },
  8: {
    设备管理: ['设备台账', '设备维修管理', '定期保养计划'],
  },
}

const roleMenuItems = computed(() => {
  const roleId = authStore.effectiveRoleId
  if (!roleId || !roleMenus[roleId]) return []
  return allMenuItems.value.filter(item => roleId===9 || roleMenus[roleId].includes(item.name) || ['业务总览','智能与提醒','系统管理'].includes(item.name) || (item.name==='采购管理'&&[1,2,3].includes(roleId)) || (item.name==='生产管理'&&[1,2,3,4].includes(roleId)))
})

const menuItems=computed(()=>roleMenuItems.value.map(item=>{
  if(item.path?.startsWith('/foundation')&&!authStore.hasMenu(item.path)){
    const granted=authStore.menuPaths?.find(p=>['/foundation/units','/foundation/categories'].includes(p))
    if(granted)return {...item,path:granted}
  }
  if(item.children)return {...item,children:item.children.map(child=>{
    const group=foundationGroups.find(group=>child.name===group.title)
    if(!group)return child
    const granted=group.keys.find(key=>authStore.hasMenu(`/foundation/${key}`))
    return granted?{...child,path:`/foundation/${granted}`}:child
  })}
  return item
}).filter(item=>item.path?authStore.hasMenu(item.path):visibleChildren(item).length>0))
const currentUser = computed(() => authStore.currentUser)

function visibleChildren(item: MenuItem) {
  const roleId = authStore.effectiveRoleId
  const allowedChildren = roleId ? roleChildMenus[roleId]?.[item.name] : undefined
  if (!item.children) return []
  const roleVisibleChildren = item.children.filter(child=>canVisit(roleId,child.path)&&(roleId===9||!child.roles||child.roles.includes(roleId)))
  const accessible = roleVisibleChildren.filter(child => (!allowedChildren || allowedChildren.includes(child.name) || child.path.startsWith('/foundation/') || child.path === '/manufacturing') && authStore.hasMenu(child.path))
  if (item.name === '系统管理') {
    return systemSections.flatMap(section => {
      const tabs = section.tabs.filter(tab => canVisit(roleId, tab.path) && authStore.hasMenu(tab.path))
      if (!tabs.length) return []
      const current = tabs.find(tab => tab.path === route.path) || tabs[0]
      return [{ path: current.path, name: section.title, icon: section.title === '组织与人员' ? Users : section.title === '日志管理' ? FileText : Settings, relatedPaths: section.tabs.map(tab => tab.path) }]
    })
  }
  if (item.name === '生产管理' || item.name === '设备管理') {
    const sections=productionSections.filter(section=>section.module===item.name)
    const merged=sections.flatMap(section=>{
      const paths=section.tabs.map(tab=>tab.path)
      const members=paths.flatMap(path=>accessible.filter(child=>child.path===path))
      if(!members.length)return []
      const current=members.find(child=>child.path===route.path)||members[0]
      return [{...current,name:section.title,relatedPaths:paths}]
    })
    const groupedPaths=sections.flatMap(section=>section.tabs.map(tab=>tab.path))
    return [...merged,...accessible.filter(child=>!groupedPaths.includes(child.path))]
  }
  if (item.name === '仓库管理') {
    return warehouseSections.flatMap(section => {
      const members = section.tabs.flatMap(tab => accessible.filter(child => child.path === tab.path))
      if (!members.length) return []
      const current = members.find(child => child.path === route.path) || members[0]
      return [{ ...current, name: section.title, relatedPaths: section.tabs.map(tab => tab.path) }]
    })
  }
  if (item.name !== '采购管理') return accessible
  return [
    { name: '供应商管理', paths: ['/purchase/supplier'] },
    { name: '采购申请与订单', paths: ['/inventory/material-requests', '/purchase/requests', '/purchase/order'] },
    { name: '采购收货与退货', paths: ['/purchase/receipts', '/purchase/returns'] },
  ].flatMap(group => {
    const members = group.paths.flatMap(path => accessible.filter(child => child.path === path))
    if (!members.length) return []
    const current = members.find(child => child.path === route.path) || members[0]
    return [{ ...current, name: group.name, relatedPaths: members.map(child => child.path) }]
  })
}

function isChildActive(item: MenuItem) {
  return visibleChildren(item).some(child => childActive(child))
}

function childActive(child:ChildMenuItem){
  // Cross-module shortcuts must not activate the warehouse menu on procurement pages.
  if (route.path.startsWith('/purchase/') && warehouseSections.some(section => section.title === child.name)) return false
  if (child.relatedPaths?.includes(route.path)) return true
  const group=foundationGroups.find(group=>group.title===child.name)
  return group?group.keys.some(key=>route.path===`/foundation/${key}`):route.path===child.path||route.path.startsWith(child.path+'/')
}

function navigate(path?: string) {
  if (!path) return
  router.push(path === '/' ? authStore.redirectPath : path)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside
    :class="[
      'mes-sidebar fixed left-0 top-0 h-screen z-50 flex flex-col',
      collapsed ? 'sidebar-small' : 'sidebar-large'
    ]"
  >
    <div class="sidebar-brand flex items-center justify-between px-4 shrink-0">
      <div v-if="!collapsed" class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
          <MousePointer2 class="w-5 h-5 text-white" />
        </div>
        <span class="text-white font-bold text-sm">{{authStore.settings.APP_NAME||'Mouse MES'}}<small class="brand-caption">制造执行管理平台</small></span>
      </div>
      <div v-else class="flex justify-center w-full">
        <MousePointer2 class="w-5 h-5 text-white" />
      </div>
      <button v-if="!collapsed" aria-label="收起导航" class="p-1 text-white/70 hover:text-white" @click="collapsed = true">
        <ChevronLeft class="w-5 h-5" />
      </button>
    </div>

    <div class="sidebar-identity p-3">
      <div class="flex items-center space-x-3">
        <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center shrink-0">
          <UserAvatar :src="currentUser?.avatar" class="w-10 h-10"/>
        </div>
        <div v-if="!collapsed" class="min-w-0">
          <p class="text-sm font-medium text-gray-800 truncate">{{ currentUser?.realName || '未登录' }}</p>
          <p class="text-xs text-gray-500 truncate">{{ currentUser?.roleName || '访客' }}</p>
        </div>
      </div>
    </div>

    <nav class="flex-1 overflow-y-auto py-4">
      <ul class="space-y-1 px-2">
        <li v-for="item in menuItems" :key="item.name">
          <template v-if="item.children">
            <button
              :class="[
                'w-full flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-all',
                isChildActive(item) ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-50'
              ]"
              @click="toggleMenu(item)"
              :title="item.name" :aria-expanded="menuExpanded(item)"
            >
              <component :is="item.icon" class="w-5 h-5 mr-3 shrink-0" />
              <span v-if="!collapsed" class="flex-1 text-left">{{ item.name }}</span>
              <ChevronRight v-if="!collapsed" :class="['w-4 h-4 transition-transform', menuExpanded(item) ? 'rotate-90' : '']" />
            </button>
            <div v-if="!collapsed && menuExpanded(item)" class="mt-1 space-y-1 ml-4">
              <button
                v-for="child in visibleChildren(item)"
                :key="child.path"
                :class="[
                  'w-full flex items-center px-3 py-2 rounded-lg text-sm font-medium transition-all',
                  childActive(child) ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-50'
                ]"
                @click="navigate(child.path)"
              >
                <component :is="child.icon" class="w-4 h-4 mr-3 shrink-0" />
                <span>{{ child.name }}</span>
              </button>
            </div>
          </template>
          <button
            v-else
            :class="[
                'w-full flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-all',
                route.path === item.path ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-50'
              ]"
            @click="navigate(item.path)" :title="item.name"
          >
            <component :is="item.icon" class="w-5 h-5 mr-3 shrink-0" />
            <span v-if="!collapsed">{{ item.path==='/'?'生产全景看板':item.name }}</span>
          </button>
        </li>
      </ul>
    </nav>

    <div class="sidebar-bottom p-2">
      <button
        :class="[
          'w-full flex items-center px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50 transition-all',
          collapsed ? 'justify-center' : ''
        ]"
        @click="collapsed = !collapsed"
      >
        <component :is="collapsed ? ChevronRight : ChevronLeft" class="w-5 h-5 mr-3 shrink-0" />
        <span v-if="!collapsed">收起菜单</span>
      </button>
      <button
        :class="[
          'w-full flex items-center px-3 py-2.5 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 transition-all mt-1',
          collapsed ? 'justify-center' : ''
        ]"
        @click="handleLogout"
      >
        <LogOut class="w-5 h-5 mr-3 shrink-0" />
        <span v-if="!collapsed">退出登录</span>
      </button>
    </div>
  </aside>
</template>

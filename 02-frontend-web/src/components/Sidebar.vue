<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
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
  User,
  Users,
  Wallet,
  Wrench,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

interface ChildMenuItem {
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
  expanded?: boolean
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const collapsed = ref(false)

const allMenuItems = ref<MenuItem[]>([
  {path:'/intelligence',name:'智能业务查询',icon:BarChart3},
  {path:'/automation',name:'规则与自动化',icon:ClipboardCheck},
  { path: '/', name: '首页', icon: LayoutDashboard },
  { path: '/foundation/workshops', name: '基础配置中心', icon: Settings },
  { path: '/manufacturing', name: '制造与质量放行', icon: ClipboardCheck },
  { path: '/fulfillment', name: '生产与仓储闭环', icon: Package },
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
      { path: '/purchase/requests', name: '采购需求', icon: ClipboardCheck },
      { path: '/purchase/order', name: '采购订单', icon: FileText },
      { path: '/purchase/supplier', name: '供应商管理', icon: Users },
    ],
  },
  {
    name: '生产管理',
    icon: MousePointer2,
    children: [
      { path: '/production/plan', name: '生产计划', icon: FileText },
      { path: '/production/task', name: '生产任务', icon: CheckCircle },
      { path: '/production/equipment-status', name: '设备状态管理', icon: Wrench, roles: [3] },
      { path: '/production/report', name: '生产报工', icon: BarChart3 },
    ],
  },
  {
    name: '质量管理',
    icon: CheckCircle,
    children: [
      { path: '/quality/tasks', name: '质检任务', icon: ClipboardCheck },
      { path: '/quality/check', name: '质检记录', icon: CheckCircle },
      { path: '/quality/standard', name: '质检标准', icon: FileText },
    ],
  },
  {
    name: '仓库管理',
    icon: Package,
    children: [
      { path: '/inventory/warehouse', name: '仓库库存', icon: Package },
      { path: '/inventory/material', name: '物料管理', icon: Package },
      { path: '/inventory/in', name: '入库管理', icon: ShoppingCart },
      { path: '/inventory/out', name: '出库管理', icon: ShoppingCart },
      { path: '/inventory/material-requests', name: '物料请求', icon: ClipboardCheck },
    ],
  },
  { path: '/attendance', name: '考勤管理', icon: Calendar },
  {
    name: '设备管理',
    icon: Wrench,
    children: [
      { path: '/equipment/list', name: '设备台账', icon: Wrench },
      { path: '/equipment/maintenance', name: '设备维修', icon: Settings },
      { path: '/equipment/repair-tasks', name: '设备维修任务', icon: Wrench, roles: [8] },
      { path: '/equipment/periodic', name: '定期维护保养', icon: Calendar, roles: [8] },
      { path: '/equipment/bills', name: '维护账单', icon: FileText, roles: [8] },
    ],
  },
  { path: '/cfo', name: '财务看板', icon: Wallet },
  {
    name: '系统管理',
    icon: Settings,
    children: [
      { path: '/system/user', name: '员工管理', icon: Users },
      { path: '/system/role', name: '角色管理', icon: Settings },
      { path: '/system/department', name: '部门管理', icon: Users },
    ],
  },
])

const roleMenus: Record<number, string[]> = {
  1: ['首页', '生产管理', '质量管理', '仓库管理', '设备管理', '考勤管理'],
  2: ['首页', '生产管理', '设备管理', '考勤管理'],
  3: ['首页', '生产管理', '考勤管理'],
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
    生产管理: ['生产任务', '设备状态管理'],
  },
  5: {
    仓库管理: ['仓库库存', '物料管理', '入库管理', '出库管理', '物料请求'],
    采购管理: ['采购订单'],
  },
  6: {
    采购管理: ['采购需求', '采购订单', '供应商管理'],
  },
  8: {
    设备管理: ['设备维修任务', '定期维护保养', '维护账单'],
  },
}

const menuItems = computed(() => {
  const roleId = authStore.currentUser?.roleId
  if (!roleId || !roleMenus[roleId]) return []
  return allMenuItems.value.filter((item) => roleId === 9 || (item.name === '智能业务查询' && [1,2,4,5,6,7,10].includes(roleId)) || (item.name === '规则与自动化' && [1,2,4,6].includes(roleId)) || (item.name === '生产与仓储闭环' && [1,2,3,4,5,6,7,8,10].includes(roleId)) || roleMenus[roleId].includes(item.name) || (item.name === '基础配置中心' && [1,2,4,5,6,7,8].includes(roleId)) || (item.name === '制造与质量放行' && [1,2,3,4].includes(roleId)))
})

const currentUser = computed(() => authStore.currentUser)

function visibleChildren(item: MenuItem) {
  const roleId = authStore.currentUser?.roleId
  const allowedChildren = roleId ? roleChildMenus[roleId]?.[item.name] : undefined
  if (!item.children) return []
  const roleVisibleChildren = item.children.filter((child) => roleId === 9 || !child.roles || (roleId && child.roles.includes(roleId)))
  if (!allowedChildren) return roleVisibleChildren
  return roleVisibleChildren.filter((child) => allowedChildren.includes(child.name))
}

function isChildActive(item: MenuItem) {
  return visibleChildren(item).some((child) => route.path === child.path || route.path.startsWith(child.path + '/'))
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
      'fixed left-0 top-0 h-screen bg-white border-r border-gray-200 z-50 transition-all duration-300 flex flex-col',
      collapsed ? 'w-16' : 'w-64'
    ]"
  >
    <div class="h-16 bg-gradient-to-r from-blue-600 to-blue-700 flex items-center justify-between px-4 shrink-0">
      <div v-if="!collapsed" class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
          <MousePointer2 class="w-5 h-5 text-white" />
        </div>
        <span class="text-white font-bold text-sm">Mouse MES</span>
      </div>
      <div v-else class="flex justify-center w-full">
        <MousePointer2 class="w-5 h-5 text-white" />
      </div>
      <button v-if="!collapsed" class="p-1 text-white/70 hover:text-white" @click="collapsed = true">
        <ChevronLeft class="w-5 h-5" />
      </button>
    </div>

    <div class="p-3 border-b border-gray-100">
      <div class="flex items-center space-x-3">
        <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center shrink-0">
          <User class="w-5 h-5 text-blue-600" />
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
              @click="item.expanded = !item.expanded"
            >
              <component :is="item.icon" class="w-5 h-5 mr-3 shrink-0" />
              <span v-if="!collapsed" class="flex-1 text-left">{{ item.name }}</span>
              <ChevronRight v-if="!collapsed" :class="['w-4 h-4 transition-transform', item.expanded || isChildActive(item) ? 'rotate-90' : '']" />
            </button>
            <div v-if="!collapsed && (item.expanded || isChildActive(item))" class="mt-1 space-y-1 ml-4">
              <button
                v-for="child in visibleChildren(item)"
                :key="child.path"
                :class="[
                  'w-full flex items-center px-3 py-2 rounded-lg text-sm font-medium transition-all',
                  route.path === child.path ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-50'
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
            @click="navigate(item.path)"
          >
            <component :is="item.icon" class="w-5 h-5 mr-3 shrink-0" />
            <span v-if="!collapsed">{{ item.name }}</span>
          </button>
        </li>
      </ul>
    </nav>

    <div class="p-2 border-t border-gray-100">
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

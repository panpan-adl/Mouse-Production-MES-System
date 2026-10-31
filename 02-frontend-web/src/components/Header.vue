<script setup lang="ts">
import { computed, ref,onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Bell, Settings, Menu, ChevronRight } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from './UserAvatar.vue'
import { sidebarCollapsed } from '@/stores/layout'
import { productionSections } from '@/utils/productionNavigation'
import { warehouseSections } from '@/utils/warehouseNavigation'
import { systemSections } from '@/utils/systemNavigation'

const router = useRouter()
const route = useRoute()
const pageLabels:Record<string,string>={'/dashboard':'生产全景看板','/mes':'MES运营中心','/manufacturing':'制造与质量放行','/equipment/maintenance':'设备维修管理','/fulfillment':'生产与仓储流程总览','/system/logs':'日志管理','/system/user':'员工管理','/system/role':'岗位与权限管理','/system/department':'部门管理','/automation':'自动任务与提醒','/notifications':'通知中心','/user/profile':'个人中心','/attendance':'考勤管理','/production/task':'生产任务','/inventory/stock':'库存管理'}
const domains:Record<string,string>={foundation:'基础配置',production:'生产管理',quality:'质量管理',inventory:'仓储管理',purchase:'采购管理',equipment:'设备管理',system:'系统管理',order:'销售管理',cfo:'财务看板'}
const productionSection=computed(()=>productionSections.find(section=>section.tabs.some(tab=>tab.path===route.path)))
const warehouseSection=computed(()=>route.path==='/purchase/receipts'?undefined:warehouseSections.find(section=>section.tabs.some(tab=>tab.path===route.path)))
const systemSection=computed(()=>systemSections.find(section=>section.tabs.some(tab=>tab.path===route.path)))
const pageTitle=computed(()=>['/inventory/material-requests','/purchase/requests','/purchase/order'].includes(route.path)?'采购申请与订单':systemSection.value?.title||warehouseSection.value?.title||productionSection.value?.title||String(route.meta.title||'')||pageLabels[route.path]||domains[route.path.split('/')[1]]||'业务工作台')
const sectionTitle=computed(()=>['/inventory/material-requests','/purchase/requests','/purchase/order'].includes(route.path)?'采购管理':(systemSection.value?'系统管理':'')||(warehouseSection.value?'仓库管理':'')||productionSection.value?.module||domains[route.path.split('/')[1]]||'工作空间')
const todayLabel=new Date().toLocaleDateString('zh-CN',{month:'long',day:'numeric',weekday:'long'})
const authStore = useAuthStore()
onMounted(()=>Promise.all([authStore.refreshProfile(),authStore.loadNotifications(),authStore.refreshMenuAccess(),authStore.refreshSettings()]).catch(()=>{}))
const showNotifications = ref(false)

function goToNotifications() {
  showNotifications.value = false
  router.push('/notifications')
}

function goToProfile() {
  router.push('/user/profile')
}
</script>

<template>
  <header class="workspace-header flex items-center justify-between sticky top-0 z-40">

    <div class="header-location"><button class="header-menu-toggle" aria-label="展开或收起导航" @click="sidebarCollapsed=!sidebarCollapsed"><Menu class="w-5 h-5" /></button><div class="header-breadcrumb"><span>{{sectionTitle}}</span><ChevronRight class="w-3 h-3"/><strong>{{pageTitle}}</strong></div></div>
    <div class="flex items-center space-x-4"><span class="header-date">{{todayLabel}}</span>
      <div class="relative">
        <button
          aria-label="查看通知" :aria-expanded="showNotifications" @click="showNotifications = !showNotifications"
          class="relative p-2 rounded-lg hover:bg-gray-100 transition-colors"
        >
          <Bell class="w-5 h-5 text-gray-600" />
          <span
            v-if="authStore.unreadCount > 0"
            class="absolute top-1 right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center"
          >
            {{ authStore.unreadCount > 9 ? '9+' : authStore.unreadCount }}
          </span>
        </button>

        <div
          v-if="showNotifications"
          class="absolute right-0 top-12 w-80 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden animate-fadeIn"
        >
          <div class="p-4 border-b border-gray-100 flex items-center justify-between">
            <h3 class="font-semibold text-gray-800">通知</h3>
            <span class="text-xs text-gray-500">{{ authStore.notifications.length }} 条</span>
          </div>
          <div class="max-h-80 overflow-y-auto">
            <div
              v-for="notification in authStore.notifications.slice(0, 5)"
              :key="notification.id"
              @click="goToNotifications"
              :class="[
                'p-4 cursor-pointer hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0',
                !notification.isRead ? 'bg-blue-50/50' : ''
              ]"
            >
              <div class="flex items-start space-x-3">
                <div
                  :class="[
                    'w-8 h-8 rounded-lg flex items-center justify-center shrink-0',
                    notification.type === 'success' ? 'bg-green-100 text-green-600' : '',
                    notification.type === 'warning' ? 'bg-yellow-100 text-yellow-600' : '',
                    notification.type === 'danger' ? 'bg-red-100 text-red-600' : '',
                    notification.type === 'info' ? 'bg-blue-100 text-blue-600' : '',
                  ]"
                >
                  <Bell class="w-4 h-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="text-sm font-medium text-gray-800 truncate">{{ notification.title }}</h4>
                  <p class="text-xs text-gray-500 mt-1 line-clamp-2">{{ notification.content }}</p>
                  <p class="text-xs text-gray-400 mt-2">{{ notification.createdAt }}</p>
                </div>
              </div>
            </div>
          </div>
          <div class="p-3 border-t border-gray-100">
            <button
              @click="goToNotifications"
              class="w-full py-2 text-sm text-blue-600 hover:text-blue-700 font-medium"
            >
              查看全部通知
            </button>
          </div>
        </div>
      </div>

      <button aria-label="个人设置" @click="goToProfile" class="p-2 rounded-lg hover:bg-gray-100 transition-colors">
        <Settings class="w-5 h-5 text-gray-600" />
      </button>

      <div class="flex items-center space-x-3 pl-4 border-l border-gray-200">
        <button
          @click="goToProfile"
          class="flex items-center space-x-3 hover:bg-gray-50 p-2 rounded-lg transition-colors cursor-pointer"
        >
          <div class="w-9 h-9 rounded-full bg-blue-100 flex items-center justify-center">
            <UserAvatar :src="authStore.currentUser?.avatar" class="w-9 h-9"/>
          </div>
          <div class="text-left">
            <p class="text-sm font-medium text-gray-800">{{ authStore.currentUser?.realName }}</p>
            <p class="text-xs text-gray-500">{{ authStore.currentUser?.roleName }}</p>
          </div>
        </button>
      </div>
    </div>
  </header>
</template>

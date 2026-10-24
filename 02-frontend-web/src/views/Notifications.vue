<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { Bell, CheckCircle, AlertCircle, Info, XCircle, Clock, CheckSquare, Filter } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
onMounted(() => authStore.loadNotifications())
const selectedType = ref<'all' | 'info' | 'warning' | 'success' | 'danger'>('all')

const filteredNotifications = computed(() => {
  if (selectedType.value === 'all') {
    return authStore.notifications
  }
  return authStore.notifications.filter((n) => n.type === selectedType.value)
})

function getIcon(type: string) {
  switch (type) {
    case 'success':
      return CheckCircle
    case 'warning':
      return AlertCircle
    case 'danger':
      return XCircle
    default:
      return Info
  }
}

function getColor(type: string) {
  switch (type) {
    case 'success':
      return 'bg-green-100 text-green-600'
    case 'warning':
      return 'bg-yellow-100 text-yellow-600'
    case 'danger':
      return 'bg-red-100 text-red-600'
    default:
      return 'bg-blue-100 text-blue-600'
  }
}
</script>

<template>
  <MainLayout>
    <div class="animate-fadeIn">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">通知公告</h1>
          <p class="text-gray-500 mt-1">查看系统通知和CEO发布的公告</p>
        </div>
        <button
          @click="authStore.markAllAsRead"
          class="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <CheckSquare class="w-4 h-4" />
          <span>全部已读</span>
        </button>
      </div>

      <div class="flex items-center space-x-2 mb-6">
        <Filter class="w-4 h-4 text-gray-500" />
        <button
          v-for="type in ['all', 'info', 'warning', 'success', 'danger']"
          :key="type"
          @click="selectedType = type as any"
          :class="[
            'px-4 py-2 text-sm font-medium rounded-lg transition-colors',
            selectedType === type
              ? 'bg-blue-600 text-white'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200'
          ]"
        >
          {{ type === 'all' ? '全部' : type === 'info' ? '信息' : type === 'warning' ? '提醒' : type === 'success' ? '成功' : '错误' }}
        </button>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div class="p-4 bg-gray-50 border-b border-gray-100 flex items-center justify-between">
          <span class="text-sm font-medium text-gray-600">共 {{ filteredNotifications.length }} 条通知</span>
          <span class="text-sm text-gray-500">未读：{{ authStore.unreadCount }} 条</span>
        </div>

        <div class="divide-y divide-gray-50">
          <div
            v-for="notification in filteredNotifications"
            :key="notification.id"
            @click="authStore.markNotificationAsRead(notification.id)"
            :class="[
              'p-6 cursor-pointer transition-colors',
              !notification.isRead ? 'bg-blue-50/30 hover:bg-blue-50' : 'hover:bg-gray-50'
            ]"
          >
            <div class="flex items-start space-x-4">
              <div :class="['w-10 h-10 rounded-lg flex items-center justify-center shrink-0', getColor(notification.type)]">
                <component :is="getIcon(notification.type)" class="w-5 h-5" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                  <h3 class="font-semibold text-gray-800">{{ notification.title }}</h3>
                  <span class="text-xs text-gray-400 flex items-center">
                    <Clock class="w-3 h-3 mr-1" />
                    {{ notification.createdAt }}
                  </span>
                </div>
                <p class="text-gray-600 mt-2 leading-relaxed">{{ notification.content }}</p>
                <div class="flex items-center mt-3">
                  <span
                    v-if="!notification.isRead"
                    class="px-2 py-0.5 text-xs font-medium bg-blue-100 text-blue-600 rounded-full"
                  >
                    未读
                  </span>
                  <span
                    v-else
                    class="px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-500 rounded-full"
                  >
                    已读
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="filteredNotifications.length === 0" class="p-12 text-center">
            <Bell class="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p class="text-gray-500">暂无通知</p>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

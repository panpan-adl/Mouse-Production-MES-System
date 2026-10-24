<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, User, Settings } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
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
  <header class="h-16 bg-white border-b border-gray-200 px-6 flex items-center justify-end sticky top-0 z-40">

    <div class="flex items-center space-x-4">
      <div class="relative">
        <button
          @click="showNotifications = !showNotifications"
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

      <button class="p-2 rounded-lg hover:bg-gray-100 transition-colors">
        <Settings class="w-5 h-5 text-gray-600" />
      </button>

      <div class="flex items-center space-x-3 pl-4 border-l border-gray-200">
        <button
          @click="goToProfile"
          class="flex items-center space-x-3 hover:bg-gray-50 p-2 rounded-lg transition-colors cursor-pointer"
        >
          <div class="w-9 h-9 rounded-full bg-blue-100 flex items-center justify-center">
            <User class="w-5 h-5 text-blue-600" />
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

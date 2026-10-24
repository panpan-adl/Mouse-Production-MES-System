<script setup lang="ts">
import { computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { MousePointer2, FileText, CheckCircle, BarChart3 } from 'lucide-vue-next'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)
</script>

<template>
  <MainLayout>
    <div class="p-6">
      <div class="bg-gradient-to-r from-blue-600 to-blue-700 rounded-2xl p-8 text-white mb-8">
        <div class="flex items-center">
          <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mr-6">
            <MousePointer2 class="w-10 h-10" />
          </div>
          <div>
            <h1 class="text-3xl font-bold">生产经理工作台</h1>
            <p class="text-blue-100 mt-2">欢迎回来，{{ currentUser?.realName }}！</p>
            <p class="text-blue-200 text-sm mt-1">用户编码：{{ currentUser?.userCode }} | 角色：{{ currentUser?.roleName }}</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">生产计划</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">5</p>
            </div>
            <div class="w-14 h-14 bg-blue-50 rounded-xl flex items-center justify-center">
              <FileText class="w-7 h-7 text-blue-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">进行中任务</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">12</p>
            </div>
            <div class="w-14 h-14 bg-green-50 rounded-xl flex items-center justify-center">
              <CheckCircle class="w-7 h-7 text-green-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">已完成</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">45</p>
            </div>
            <div class="w-14 h-14 bg-green-50 rounded-xl flex items-center justify-center">
              <CheckCircle class="w-7 h-7 text-green-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">报工记录</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">89</p>
            </div>
            <div class="w-14 h-14 bg-orange-50 rounded-xl flex items-center justify-center">
              <BarChart3 class="w-7 h-7 text-orange-600" />
            </div>
          </div>
        </div>
      </div>

      <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800">生产计划列表</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">PLAN-2024-001</p>
                  <p class="text-sm text-gray-500">鼠标外壳生产计划</p>
                </div>
                <span class="px-3 py-1 bg-blue-100 text-blue-600 text-sm rounded-full">进行中</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">PLAN-2024-002</p>
                  <p class="text-sm text-gray-500">鼠标按键生产计划</p>
                </div>
                <span class="px-3 py-1 bg-green-100 text-green-600 text-sm rounded-full">已完成</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">PLAN-2024-003</p>
                  <p class="text-sm text-gray-500">鼠标滚轮生产计划</p>
                </div>
                <span class="px-3 py-1 bg-gray-100 text-gray-600 text-sm rounded-full">待开始</span>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800">待分配任务</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">TASK-001</p>
                  <p class="text-sm text-gray-500">组装车间 - 外壳组装</p>
                </div>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-600 text-sm rounded-full">待领取</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">TASK-002</p>
                  <p class="text-sm text-gray-500">注塑车间 - 外壳注塑</p>
                </div>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-600 text-sm rounded-full">待领取</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">TASK-003</p>
                  <p class="text-sm text-gray-500">组装车间 - 按键组装</p>
                </div>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-600 text-sm rounded-full">待领取</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

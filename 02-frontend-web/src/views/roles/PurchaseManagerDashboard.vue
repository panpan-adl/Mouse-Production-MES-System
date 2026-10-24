<script setup lang="ts">
import { computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { ShoppingCart, Users, FileText, CheckCircle } from 'lucide-vue-next'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)
</script>

<template>
  <MainLayout>
    <div class="p-6">
      <div class="bg-gradient-to-r from-cyan-600 to-cyan-700 rounded-2xl p-8 text-white mb-8">
        <div class="flex items-center">
          <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mr-6">
            <ShoppingCart class="w-10 h-10" />
          </div>
          <div>
            <h1 class="text-3xl font-bold">采购专员工作台</h1>
            <p class="text-cyan-100 mt-2">欢迎回来，{{ currentUser?.realName }}！</p>
            <p class="text-cyan-200 text-sm mt-1">用户编码：{{ currentUser?.userCode }} | 角色：{{ currentUser?.roleName }}</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">采购订单</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">15</p>
            </div>
            <div class="w-14 h-14 bg-cyan-50 rounded-xl flex items-center justify-center">
              <ShoppingCart class="w-7 h-7 text-cyan-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">供应商</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">8</p>
            </div>
            <div class="w-14 h-14 bg-blue-50 rounded-xl flex items-center justify-center">
              <Users class="w-7 h-7 text-blue-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">待审批</p>
              <p class="text-3xl font-bold text-yellow-600 mt-2">3</p>
            </div>
            <div class="w-14 h-14 bg-yellow-50 rounded-xl flex items-center justify-center">
              <FileText class="w-7 h-7 text-yellow-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">已完成</p>
              <p class="text-3xl font-bold text-green-600 mt-2">12</p>
            </div>
            <div class="w-14 h-14 bg-green-50 rounded-xl flex items-center justify-center">
              <CheckCircle class="w-7 h-7 text-green-600" />
            </div>
          </div>
        </div>
      </div>

      <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800">待审批采购单</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">PO-2024-001</p>
                  <p class="text-sm text-gray-500">ABS塑料颗粒 500kg</p>
                </div>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-600 text-sm rounded-full">待审批</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">PO-2024-002</p>
                  <p class="text-sm text-gray-500">鼠标滚轮 200个</p>
                </div>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-600 text-sm rounded-full">待审批</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">PO-2024-003</p>
                  <p class="text-sm text-gray-500">USB连接线 100条</p>
                </div>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-600 text-sm rounded-full">待审批</span>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800">供应商列表</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <div class="p-4 bg-gray-50 rounded-xl">
                <p class="font-medium text-gray-800">深圳塑胶有限公司</p>
                <p class="text-sm text-gray-500 mt-1">联系人：王经理 | 电话：13800138001</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-xl">
                <p class="font-medium text-gray-800">东莞五金加工厂</p>
                <p class="text-sm text-gray-500 mt-1">联系人：李经理 | 电话：13800138002</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-xl">
                <p class="font-medium text-gray-800">广州电子元件厂</p>
                <p class="text-sm text-gray-500 mt-1">联系人：张经理 | 电话：13800138003</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

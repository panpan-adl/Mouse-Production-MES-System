<script setup lang="ts">
import { computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { FileText, Users, Clock, CheckCircle } from 'lucide-vue-next'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)
</script>

<template>
  <MainLayout>
    <div class="p-6">
      <div class="bg-gradient-to-r from-teal-600 to-teal-700 rounded-2xl p-8 text-white mb-8">
        <div class="flex items-center">
          <div class="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mr-6">
            <FileText class="w-10 h-10" />
          </div>
          <div>
            <h1 class="text-3xl font-bold">销售专员工作台</h1>
            <p class="text-teal-100 mt-2">欢迎回来，{{ currentUser?.realName }}！</p>
            <p class="text-teal-200 text-sm mt-1">用户编码：{{ currentUser?.userCode }} | 角色：{{ currentUser?.roleName }}</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">客户数量</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">50+</p>
            </div>
            <div class="w-14 h-14 bg-teal-50 rounded-xl flex items-center justify-center">
              <Users class="w-7 h-7 text-teal-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">本月订单</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">25</p>
            </div>
            <div class="w-14 h-14 bg-blue-50 rounded-xl flex items-center justify-center">
              <FileText class="w-7 h-7 text-blue-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">订单金额</p>
              <p class="text-3xl font-bold text-gray-800 mt-2">¥50万</p>
            </div>
            <div class="w-14 h-14 bg-green-50 rounded-xl flex items-center justify-center">
              <CheckCircle class="w-7 h-7 text-green-600" />
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">待确认订单</p>
              <p class="text-3xl font-bold text-yellow-600 mt-2">5</p>
            </div>
            <div class="w-14 h-14 bg-yellow-50 rounded-xl flex items-center justify-center">
              <Clock class="w-7 h-7 text-yellow-600" />
            </div>
          </div>
        </div>
      </div>

      <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800">客户订单列表</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">ORD-2024-001</p>
                  <p class="text-sm text-gray-500">客户：深圳科技公司</p>
                  <p class="text-sm text-gray-500">产品：无线鼠标 500个</p>
                </div>
                <span class="px-3 py-1 bg-blue-100 text-blue-600 text-sm rounded-full">生产中</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">ORD-2024-002</p>
                  <p class="text-sm text-gray-500">客户：广州贸易公司</p>
                  <p class="text-sm text-gray-500">产品：有线鼠标 300个</p>
                </div>
                <span class="px-3 py-1 bg-gray-100 text-gray-600 text-sm rounded-full">待确认</span>
              </div>
              <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div>
                  <p class="font-medium text-gray-800">ORD-2024-003</p>
                  <p class="text-sm text-gray-500">客户：东莞电子厂</p>
                  <p class="text-sm text-gray-500">产品：游戏鼠标 200个</p>
                </div>
                <span class="px-3 py-1 bg-green-100 text-green-600 text-sm rounded-full">已完成</span>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="p-6 border-b border-gray-100">
            <h2 class="text-lg font-semibold text-gray-800">客户管理</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <div class="p-4 bg-gray-50 rounded-xl">
                <p class="font-medium text-gray-800">深圳科技有限公司</p>
                <p class="text-sm text-gray-500 mt-1">联系人：陈经理 | 电话：13800138001</p>
                <p class="text-sm text-gray-500">累计订单：12笔 | 累计金额：¥20万</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-xl">
                <p class="font-medium text-gray-800">广州贸易有限公司</p>
                <p class="text-sm text-gray-500 mt-1">联系人：林经理 | 电话：13800138002</p>
                <p class="text-sm text-gray-500">累计订单：8笔 | 累计金额：¥15万</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-xl">
                <p class="font-medium text-gray-800">东莞电子有限公司</p>
                <p class="text-sm text-gray-500 mt-1">联系人：黄经理 | 电话：13800138003</p>
                <p class="text-sm text-gray-500">累计订单：5笔 | 累计金额：¥8万</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

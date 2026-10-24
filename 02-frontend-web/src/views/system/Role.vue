<script setup lang="ts">
import MainLayout from '@/components/MainLayout.vue'
import { Plus, Search } from 'lucide-vue-next'
</script>

<template>
  <MainLayout>
    <div class="animate-fadeIn">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">角色管理</h1>
          <p class="text-gray-500 mt-1">管理系统角色</p>
        </div>
        <div class="flex items-center space-x-3">
          <button class="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
            <Plus class="w-4 h-4" />
            <span>新建角色</span>
          </button>
        </div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <div class="flex items-center space-x-4 mb-6">
          <div class="relative flex-1 max-w-md">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="搜索角色名称..."
              class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="bg-gray-50">
                <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">角色编码</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">角色名称</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">描述</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">状态</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr class="hover:bg-gray-50 transition-colors">
                <td class="px-4 py-4">
                  <span class="font-medium text-gray-800">109</span>
                </td>
                <td class="px-4 py-4 text-gray-600">CEO</td>
                <td class="px-4 py-4 text-gray-600">公司最高管理者</td>
                <td class="px-4 py-4">
                  <span class="px-2 py-1 text-xs font-medium bg-green-100 text-green-600 rounded-full">启用</span>
                </td>
                <td class="px-4 py-4">
                  <button class="text-blue-600 hover:text-blue-700 text-sm font-medium">编辑</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

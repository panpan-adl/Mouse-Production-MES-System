<script setup lang="ts">
import { ref, computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import {
  Package,
  AlertCircle,
  CheckCircle,
  Clock,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-vue-next'

const authStore = useAuthStore()
const currentUser = computed(() => authStore.currentUser)

const stats = ref([
  {
    title: '今日产量',
    value: '520',
    unit: '件',
    trend: '+12%',
    trendType: 'up',
    icon: Package,
    color: 'blue',
  },
  {
    title: '合格率',
    value: '98.5',
    unit: '%',
    trend: '+0.5%',
    trendType: 'up',
    icon: CheckCircle,
    color: 'green',
  },
  {
    title: '待处理订单',
    value: '12',
    unit: '个',
    trend: '+3',
    trendType: 'down',
    icon: Clock,
    color: 'yellow',
  },
  {
    title: '设备故障',
    value: '2',
    unit: '台',
    trend: '-1',
    trendType: 'up',
    icon: AlertCircle,
    color: 'red',
  },
])

const recentOrders = ref([
  { id: 'ORD-2024-0001', product: '有线办公鼠标', quantity: 500, status: '生产中', progress: 65 },
  { id: 'ORD-2024-0002', product: '无线游戏鼠标', quantity: 200, status: '待确认', progress: 0 },
  { id: 'ORD-2024-0003', product: '静音商务鼠标', quantity: 300, status: '已确认', progress: 10 },
])

const productionChart = ref([65, 78, 90, 81, 95, 88, 105])
</script>

<template>
  <MainLayout>
    <div class="animate-fadeIn">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">首页</h1>
          <p class="text-gray-500 mt-1">欢迎回来，{{ currentUser?.realName }}！这是今天的生产概览</p>
        </div>
        <div class="flex items-center space-x-2">
          <span class="text-sm text-gray-500">今日日期：</span>
          <span class="text-sm font-medium text-gray-800">{{ new Date().toLocaleDateString('zh-CN') }}</span>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        <div
          v-for="stat in stats"
          :key="stat.title"
          class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 hover:shadow-md transition-shadow"
        >
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">{{ stat.title }}</p>
              <div class="flex items-baseline mt-2">
                <span class="text-3xl font-bold text-gray-800">{{ stat.value }}</span>
                <span class="text-sm text-gray-500 ml-1">{{ stat.unit }}</span>
              </div>
              <div class="flex items-center mt-2">
                <span
                  :class="[
                    'text-sm font-medium flex items-center',
                    stat.trendType === 'up' ? 'text-green-600' : 'text-red-600'
                  ]"
                >
                  <ArrowUpRight v-if="stat.trendType === 'up'" class="w-4 h-4 mr-1" />
                  <ArrowDownRight v-else class="w-4 h-4 mr-1" />
                  {{ stat.trend }}
                </span>
                <span class="text-xs text-gray-400 ml-2">较昨日</span>
              </div>
            </div>
            <div
              :class="[
                'w-12 h-12 rounded-xl flex items-center justify-center',
                stat.color === 'blue' ? 'bg-blue-50 text-blue-600' : '',
                stat.color === 'green' ? 'bg-green-50 text-green-600' : '',
                stat.color === 'yellow' ? 'bg-yellow-50 text-yellow-600' : '',
                stat.color === 'red' ? 'bg-red-50 text-red-600' : '',
              ]"
            >
              <component :is="stat.icon" class="w-6 h-6" />
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-gray-800">生产趋势</h2>
            <div class="flex items-center space-x-2">
              <button class="px-3 py-1 text-xs font-medium text-blue-600 bg-blue-50 rounded-lg">日</button>
              <button class="px-3 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50 rounded-lg">周</button>
              <button class="px-3 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50 rounded-lg">月</button>
            </div>
          </div>
          <div class="h-48 flex items-end justify-between space-x-4">
            <div
              v-for="(value, index) in productionChart"
              :key="index"
              class="flex-1 flex flex-col items-center"
            >
              <div
                class="w-full bg-gradient-to-t from-blue-500 to-blue-400 rounded-t-lg transition-all hover:from-blue-600 hover:to-blue-500"
                :style="{ height: `${value}%` }"
              ></div>
              <span class="text-xs text-gray-500 mt-2">{{ ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][index] }}</span>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-gray-800">待处理订单</h2>
            <button class="text-sm text-blue-600 hover:text-blue-700 font-medium">查看全部</button>
          </div>
          <div class="space-y-4">
            <div
              v-for="order in recentOrders"
              :key="order.id"
              class="p-4 bg-gray-50 rounded-lg"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm font-medium text-gray-800">{{ order.id }}</span>
                <span
                  :class="[
                    'text-xs px-2 py-0.5 rounded-full',
                    order.status === '生产中' ? 'bg-blue-100 text-blue-600' : '',
                    order.status === '待确认' ? 'bg-yellow-100 text-yellow-600' : '',
                    order.status === '已确认' ? 'bg-green-100 text-green-600' : '',
                  ]"
                >
                  {{ order.status }}
                </span>
              </div>
              <p class="text-sm text-gray-500">{{ order.product }}</p>
              <p class="text-xs text-gray-400 mt-1">数量：{{ order.quantity }}件</p>
              <div class="mt-3">
                <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <div
                    class="h-full bg-blue-500 rounded-full transition-all"
                    :style="{ width: `${order.progress}%` }"
                  ></div>
                </div>
                <span class="text-xs text-gray-400 mt-1">{{ order.progress }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-gray-800">设备状态</h2>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="p-4 bg-green-50 rounded-lg text-center">
              <div class="text-2xl font-bold text-green-600">3</div>
              <div class="text-xs text-gray-500 mt-1">正常运行</div>
            </div>
            <div class="p-4 bg-yellow-50 rounded-lg text-center">
              <div class="text-2xl font-bold text-yellow-600">1</div>
              <div class="text-xs text-gray-500 mt-1">维护中</div>
            </div>
            <div class="p-4 bg-red-50 rounded-lg text-center">
              <div class="text-2xl font-bold text-red-600">1</div>
              <div class="text-xs text-gray-500 mt-1">故障</div>
            </div>
            <div class="p-4 bg-gray-50 rounded-lg text-center">
              <div class="text-2xl font-bold text-gray-600">5</div>
              <div class="text-xs text-gray-500 mt-1">总设备</div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-gray-800">物料库存预警</h2>
          </div>
          <div class="space-y-3">
            <div class="flex items-center justify-between p-3 bg-red-50 rounded-lg">
              <div>
                <p class="text-sm font-medium text-gray-800">PCB主板</p>
                <p class="text-xs text-gray-500">当前库存：80件</p>
              </div>
              <span class="text-xs text-red-600 font-medium">低于安全库存</span>
            </div>
            <div class="flex items-center justify-between p-3 bg-yellow-50 rounded-lg">
              <div>
                <p class="text-sm font-medium text-gray-800">微动开关</p>
                <p class="text-xs text-gray-500">当前库存：450件</p>
              </div>
              <span class="text-xs text-yellow-600 font-medium">接近安全库存</span>
            </div>
            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div>
                <p class="text-sm font-medium text-gray-800">编码器</p>
                <p class="text-xs text-gray-500">当前库存：120件</p>
              </div>
              <span class="text-xs text-green-600 font-medium">库存充足</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

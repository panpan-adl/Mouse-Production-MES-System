<script setup lang="ts">
import { ref, computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import {
  User, Mail, Phone, Building2, Tag, Edit2, Save, X,
  Settings, Shield, Lock, Eye, EyeOff, ChevronRight, UserCircle,
  Clock, MapPin, Briefcase
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const activeTab = ref('profile')
const isEditingProfile = ref(false)
const showPasswordChange = ref(false)

const userData = ref({
  realName: '',
  phone: '',
  email: '',
  gender: 1,
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const showOldPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const passwordError = ref('')

const currentUser = computed(() => authStore.currentUser)

const genderText = computed(() => userData.value.gender === 1 ? '男' : '女')

const statusText = computed(() => currentUser.value?.status === 1 ? '在职' : '离职')

function loadUserData() {
  if (currentUser.value) {
    userData.value = {
      realName: currentUser.value.realName,
      phone: currentUser.value.phone,
      email: currentUser.value.email,
      gender: currentUser.value.gender,
    }
  }
}

function startEditProfile() {
  loadUserData()
  isEditingProfile.value = true
}

function saveProfile() {
  isEditingProfile.value = false
}

function cancelEditProfile() {
  isEditingProfile.value = false
}

function validatePassword() {
  if (!passwordForm.value.oldPassword) {
    passwordError.value = '请输入旧密码'
    return false
  }
  if (!passwordForm.value.newPassword) {
    passwordError.value = '请输入新密码'
    return false
  }
  if (passwordForm.value.newPassword.length < 6) {
    passwordError.value = '新密码至少6位'
    return false
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return false
  }
  passwordError.value = ''
  return true
}

function savePassword() {
  if (validatePassword()) {
    showPasswordChange.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  }
}

function cancelPassword() {
  showPasswordChange.value = false
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordError.value = ''
}

const menuItems = [
  { id: 'profile', name: '个人资料', icon: UserCircle },
  { id: 'settings', name: '账号设置', icon: Settings },
  { id: 'security', name: '安全中心', icon: Shield },
]

loadUserData()
</script>

<template>
  <MainLayout>
    <div class="min-h-screen bg-gray-50">
      <div class="max-w-6xl mx-auto p-6">
        <div class="flex gap-6">
          <aside class="w-56 flex-shrink-0">
            <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
              <div class="p-4 border-b border-gray-100">
                <div class="flex items-center space-x-3">
                  <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center">
                    <User class="w-6 h-6 text-white" />
                  </div>
                  <div>
                    <h3 class="font-semibold text-gray-800 text-sm">{{ currentUser?.realName }}</h3>
                    <p class="text-xs text-gray-500">{{ currentUser?.roleName }}</p>
                  </div>
                </div>
              </div>
              <nav class="p-2">
                <button
                  v-for="item in menuItems"
                  :key="item.id"
                  @click="activeTab = item.id"
                  :class="[
                    'w-full flex items-center space-x-3 px-4 py-3 rounded-lg transition-all duration-200',
                    activeTab === item.id
                      ? 'bg-blue-50 text-blue-600 font-medium'
                      : 'text-gray-600 hover:bg-gray-50'
                  ]"
                >
                  <component :is="item.icon" class="w-4 h-4" />
                  <span>{{ item.name }}</span>
                  <ChevronRight class="w-4 h-4 ml-auto opacity-50" />
                </button>
              </nav>
            </div>
          </aside>

          <main class="flex-1">
            <div v-if="activeTab === 'profile'" class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
              <div class="bg-gradient-to-r from-blue-600 via-blue-700 to-indigo-700 px-8 py-10">
                <div class="flex items-center space-x-6">
                  <div class="relative">
                    <div class="w-28 h-28 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center border-4 border-white/30">
                      <User class="w-14 h-14 text-white" />
                    </div>
                    <button class="absolute -bottom-1 -right-1 w-8 h-8 bg-white rounded-full shadow-md flex items-center justify-center hover:bg-gray-50 transition-colors">
                      <Edit2 class="w-4 h-4 text-gray-600" />
                    </button>
                  </div>
                  <div>
                    <h2 class="text-3xl font-bold text-white">{{ currentUser?.realName }}</h2>
                    <div class="flex items-center space-x-4 mt-2">
                      <span class="px-3 py-1 bg-white/20 backdrop-blur-sm text-white text-sm rounded-full">
                        {{ currentUser?.roleName }}
                      </span>
                      <span class="px-3 py-1 bg-green-500/30 backdrop-blur-sm text-green-100 text-sm rounded-full">
                        {{ statusText }}
                      </span>
                    </div>
                    <div class="flex items-center space-x-6 mt-3 text-blue-100">
                      <span class="text-sm flex items-center space-x-1">
                        <Building2 class="w-4 h-4" />
                        <span>{{ currentUser?.deptName }}</span>
                      </span>
                      <span class="text-sm flex items-center space-x-1">
                        <Tag class="w-4 h-4" />
                        <span>用户编码: {{ currentUser?.userCode }}</span>
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="p-8">
                <div class="flex items-center justify-between mb-6">
                  <h3 class="text-lg font-semibold text-gray-800">基本信息</h3>
                  <button
                    v-if="!isEditingProfile"
                    @click="startEditProfile"
                    class="flex items-center space-x-1 px-3 py-2 text-sm text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition-colors"
                  >
                    <Edit2 class="w-4 h-4" />
                    <span>编辑资料</span>
                  </button>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div class="space-y-5">
                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">用户编码</label>
                      <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <Tag class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800 font-medium">{{ currentUser?.userCode }}</span>
                      </div>
                    </div>

                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">用户名</label>
                      <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <User class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ currentUser?.username }}</span>
                      </div>
                    </div>

                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">真实姓名</label>
                      <div v-if="!isEditingProfile" class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <UserCircle class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ userData.realName }}</span>
                      </div>
                      <input
                        v-else
                        v-model="userData.realName"
                        type="text"
                        class="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                        placeholder="请输入真实姓名"
                      />
                    </div>

                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">手机号码</label>
                      <div v-if="!isEditingProfile" class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <Phone class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ userData.phone }}</span>
                      </div>
                      <input
                        v-else
                        v-model="userData.phone"
                        type="tel"
                        class="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                        placeholder="请输入手机号码"
                      />
                    </div>
                  </div>

                  <div class="space-y-5">
                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">邮箱地址</label>
                      <div v-if="!isEditingProfile" class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <Mail class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ userData.email }}</span>
                      </div>
                      <input
                        v-else
                        v-model="userData.email"
                        type="email"
                        class="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                        placeholder="请输入邮箱地址"
                      />
                    </div>

                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">性别</label>
                      <div v-if="!isEditingProfile" class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <Tag class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ genderText }}</span>
                      </div>
                      <div v-else class="flex space-x-6 p-4 bg-gray-50 rounded-xl">
                        <label class="flex items-center space-x-2 cursor-pointer">
                          <input
                            v-model="userData.gender"
                            type="radio"
                            :value="1"
                            class="w-4 h-4 text-blue-600 focus:ring-blue-500"
                          />
                          <span class="text-gray-700">男</span>
                        </label>
                        <label class="flex items-center space-x-2 cursor-pointer">
                          <input
                            v-model="userData.gender"
                            type="radio"
                            :value="0"
                            class="w-4 h-4 text-blue-600 focus:ring-blue-500"
                          />
                          <span class="text-gray-700">女</span>
                        </label>
                      </div>
                    </div>

                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">所属部门</label>
                      <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <Building2 class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ currentUser?.deptName }}</span>
                      </div>
                    </div>

                    <div>
                      <label class="block text-sm font-medium text-gray-500 mb-2">角色</label>
                      <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                        <Briefcase class="w-5 h-5 text-gray-400" />
                        <span class="text-gray-800">{{ currentUser?.roleName }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="mt-8 pt-6 border-t border-gray-100">
                  <h3 class="text-lg font-semibold text-gray-800 mb-4">登录信息</h3>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                      <Clock class="w-5 h-5 text-gray-400" />
                      <div>
                        <span class="text-sm text-gray-500">上次登录时间</span>
                        <p class="text-gray-800 mt-0.5">2024-07-14 17:30:00</p>
                      </div>
                    </div>
                    <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
                      <MapPin class="w-5 h-5 text-gray-400" />
                      <div>
                        <span class="text-sm text-gray-500">上次登录IP</span>
                        <p class="text-gray-800 mt-0.5">192.168.1.100</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="isEditingProfile" class="mt-8 flex justify-end space-x-3">
                  <button
                    @click="cancelEditProfile"
                    class="flex items-center space-x-2 px-4 py-2 border border-gray-200 text-gray-600 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    <X class="w-4 h-4" />
                    <span>取消</span>
                  </button>
                  <button
                    @click="saveProfile"
                    class="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    <Save class="w-4 h-4" />
                    <span>保存</span>
                  </button>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'settings'" class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
              <div class="px-8 py-6 border-b border-gray-100">
                <h2 class="text-xl font-semibold text-gray-800">账号设置</h2>
                <p class="text-gray-500 mt-1">管理您的账号信息</p>
              </div>
              <div class="p-8">
                <div class="space-y-6">
                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div class="flex items-center space-x-3">
                      <Mail class="w-5 h-5 text-gray-400" />
                      <div>
                        <h4 class="font-medium text-gray-800">修改邮箱</h4>
                        <p class="text-sm text-gray-500">当前邮箱: {{ currentUser?.email }}</p>
                      </div>
                    </div>
                    <button class="text-blue-600 hover:text-blue-700 text-sm font-medium">
                      修改 <ChevronRight class="w-4 h-4 inline" />
                    </button>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div class="flex items-center space-x-3">
                      <Phone class="w-5 h-5 text-gray-400" />
                      <div>
                        <h4 class="font-medium text-gray-800">修改手机号</h4>
                        <p class="text-sm text-gray-500">当前手机: {{ currentUser?.phone }}</p>
                      </div>
                    </div>
                    <button class="text-blue-600 hover:text-blue-700 text-sm font-medium">
                      修改 <ChevronRight class="w-4 h-4 inline" />
                    </button>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl" @click="showPasswordChange = !showPasswordChange">
                    <div class="flex items-center space-x-3">
                      <Lock class="w-5 h-5 text-gray-400" />
                      <div>
                        <h4 class="font-medium text-gray-800">修改密码</h4>
                        <p class="text-sm text-gray-500">定期更换密码保证账号安全</p>
                      </div>
                    </div>
                    <ChevronRight :class="['w-4 h-4 text-gray-400 transition-transform', showPasswordChange ? 'rotate-90' : '']" />
                  </div>

                  <div v-if="showPasswordChange" class="p-6 bg-gray-50 rounded-xl space-y-4">
                    <div v-if="passwordError" class="px-4 py-3 bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg">
                      {{ passwordError }}
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-600 mb-2">旧密码</label>
                      <div class="relative">
                        <input
                          v-model="passwordForm.oldPassword"
                          :type="showOldPassword ? 'text' : 'password'"
                          class="w-full px-4 py-3 pr-10 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          placeholder="请输入旧密码"
                        />
                        <button
                          type="button"
                          @click="showOldPassword = !showOldPassword"
                          class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                        >
                          <Eye v-if="showOldPassword" class="w-5 h-5" />
                          <EyeOff v-else class="w-5 h-5" />
                        </button>
                      </div>
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-600 mb-2">新密码</label>
                      <div class="relative">
                        <input
                          v-model="passwordForm.newPassword"
                          :type="showNewPassword ? 'text' : 'password'"
                          class="w-full px-4 py-3 pr-10 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          placeholder="请输入新密码（至少6位）"
                        />
                        <button
                          type="button"
                          @click="showNewPassword = !showNewPassword"
                          class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                        >
                          <Eye v-if="showNewPassword" class="w-5 h-5" />
                          <EyeOff v-else class="w-5 h-5" />
                        </button>
                      </div>
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-600 mb-2">确认新密码</label>
                      <div class="relative">
                        <input
                          v-model="passwordForm.confirmPassword"
                          :type="showConfirmPassword ? 'text' : 'password'"
                          class="w-full px-4 py-3 pr-10 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          placeholder="请再次输入新密码"
                        />
                        <button
                          type="button"
                          @click="showConfirmPassword = !showConfirmPassword"
                          class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                        >
                          <Eye v-if="showConfirmPassword" class="w-5 h-5" />
                          <EyeOff v-else class="w-5 h-5" />
                        </button>
                      </div>
                    </div>
                    <div class="flex justify-end space-x-3 pt-2">
                      <button
                        @click="cancelPassword"
                        class="px-4 py-2 text-gray-600 hover:text-gray-800 transition-colors"
                      >
                        取消
                      </button>
                      <button
                        @click="savePassword"
                        class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                      >
                        确认修改
                      </button>
                    </div>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div class="flex items-center space-x-3">
                      <UserCircle class="w-5 h-5 text-gray-400" />
                      <div>
                        <h4 class="font-medium text-gray-800">更换头像</h4>
                        <p class="text-sm text-gray-500">支持 JPG、PNG 格式，大小不超过 2MB</p>
                      </div>
                    </div>
                    <button class="text-blue-600 hover:text-blue-700 text-sm font-medium">
                      更换 <ChevronRight class="w-4 h-4 inline" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'security'" class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
              <div class="px-8 py-6 border-b border-gray-100">
                <h2 class="text-xl font-semibold text-gray-800">安全中心</h2>
                <p class="text-gray-500 mt-1">保护您的账号安全</p>
              </div>
              <div class="p-8">
                <div class="space-y-4">
                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div>
                      <h4 class="font-medium text-gray-800">登录密码</h4>
                      <p class="text-sm text-gray-500">已设置</p>
                    </div>
                    <span class="px-3 py-1 bg-green-100 text-green-600 text-sm font-medium rounded-full">已保护</span>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div>
                      <h4 class="font-medium text-gray-800">手机验证</h4>
                      <p class="text-sm text-gray-500">已绑定 {{ currentUser?.phone }}</p>
                    </div>
                    <span class="px-3 py-1 bg-green-100 text-green-600 text-sm font-medium rounded-full">已保护</span>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div>
                      <h4 class="font-medium text-gray-800">邮箱验证</h4>
                      <p class="text-sm text-gray-500">已绑定 {{ currentUser?.email }}</p>
                    </div>
                    <span class="px-3 py-1 bg-green-100 text-green-600 text-sm font-medium rounded-full">已保护</span>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div>
                      <h4 class="font-medium text-gray-800">登录保护</h4>
                      <p class="text-sm text-gray-500">异常登录时发送提醒</p>
                    </div>
                    <button class="text-blue-600 hover:text-blue-700 text-sm font-medium">
                      开启 <ChevronRight class="w-4 h-4 inline" />
                    </button>
                  </div>

                  <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div>
                      <h4 class="font-medium text-gray-800">登录记录</h4>
                      <p class="text-sm text-gray-500">查看最近的登录历史</p>
                    </div>
                    <button class="text-blue-600 hover:text-blue-700 text-sm font-medium">
                      查看 <ChevronRight class="w-4 h-4 inline" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

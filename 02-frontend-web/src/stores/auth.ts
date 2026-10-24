import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { login as apiLogin } from '@/api/auth'

export interface User {
  id: number
  userCode: string
  username: string
  realName: string
  phone: string
  email: string
  gender: number
  roleId: number
  roleName: string
  deptId: number
  deptName: string
  status: number
  token?: string
}

export interface Notification {
  id: number
  title: string
  content: string
  type: 'info' | 'warning' | 'success' | 'danger'
  createdAt: string
  isRead: boolean
}

const STORAGE_KEY = 'ptl_auth'

const mockNotifications: Notification[] = [
  {
    id: 1,
    title: '生产计划更新',
    content: '订单 ORD-2024-0001 的生产计划已更新，请及时查看',
    type: 'info',
    createdAt: '2024-07-13 09:30',
    isRead: false,
  },
  {
    id: 2,
    title: 'CEO公告',
    content: '公司将于下周一举行年度总结会议，请各部门负责人准时参加',
    type: 'warning',
    createdAt: '2024-07-13 08:00',
    isRead: false,
  },
  {
    id: 3,
    title: '质检通知',
    content: '批次 LOT-2024-001 产品质检完成，合格率98.5%',
    type: 'success',
    createdAt: '2024-07-12 16:45',
    isRead: true,
  },
  {
    id: 4,
    title: '设备维护提醒',
    content: '设备 EQ-005 维护周期已到，请安排维护人员进行保养',
    type: 'warning',
    createdAt: '2024-07-12 10:00',
    isRead: false,
  },
]

function loadPersistedState(): { currentUser: User | null } {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      return { currentUser: parsed.currentUser || null }
    }
  } catch {
    // ignore corrupt data
  }
  return { currentUser: null }
}

function savePersistedState(currentUser: User | null) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ currentUser }))
  } catch {
    // storage full or unavailable
  }
}

export const useAuthStore = defineStore('auth', () => {
  const { currentUser: savedUser } = loadPersistedState()
  const currentUser = ref<User | null>(savedUser)
  const notifications = ref<Notification[]>([...mockNotifications])
  const isLoggedIn = computed(() => currentUser.value !== null)
  const unreadCount = computed(() =>
    notifications.value.filter((n) => !n.isRead).length
  )

  const roleCode = computed(() => {
    if (!currentUser.value?.roleId) return ''
    return String(currentUser.value.roleId)
  })

  const redirectPath = computed(() => {
    const roleId = currentUser.value?.roleId
    switch (roleId) {
      case 9:
        return '/admin'
      case 10:
        return '/cfo'
      case 1:
        return '/prod-manager'
      case 2:
        return '/workshop-manager'
      case 3:
        return '/worker'
      case 4:
        return '/qc-manager'
      case 5:
        return '/warehouse-manager'
      case 6:
        return '/purchase-manager'
      case 7:
        return '/sales-manager'
      case 8:
        return '/equipment-manager'
      case 11:
        return '/admin'
      default:
        return '/'
    }
  })

  // 自动持久化：currentUser 变化时写入 localStorage
  watch(currentUser, (val) => {
    savePersistedState(val)
  }, { deep: true })

  async function login(userCode: string, password: string): Promise<boolean> {
    try {
      const user = await apiLogin({ userCode, password })
      currentUser.value = user as User
      return true
    } catch (error) {
      console.error('Login failed:', error)
      return false
    }
  }

  function logout() {
    currentUser.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  function markNotificationAsRead(id: number) {
    const notification = notifications.value.find((n) => n.id === id)
    if (notification) {
      notification.isRead = true
    }
  }

  function markAllAsRead() {
    notifications.value.forEach((n) => {
      n.isRead = true
    })
  }

  return {
    currentUser,
    notifications,
    isLoggedIn,
    unreadCount,
    roleCode,
    redirectPath,
    login,
    logout,
    markNotificationAsRead,
    markAllAsRead,
  }
})

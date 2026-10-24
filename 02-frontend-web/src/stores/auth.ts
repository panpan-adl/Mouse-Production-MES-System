import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { login as apiLogin } from '@/api/auth'
import request from '@/utils/request'

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
  const notifications = ref<Notification[]>([])
  const menuPaths=ref<string[]|null>(null)
  const settings=ref<Record<string,string>>({APP_NAME:'Mouse MES',DEFAULT_DURATION_MINUTES:'60'})
  async function refreshSettings(){const rows=await request.get<{code:string;value:string}[]>('/mes/settings');settings.value=Object.fromEntries(rows.map(r=>[r.code,r.value]));if(settings.value.APP_NAME)document.title=settings.value.APP_NAME}
  async function refreshMenuAccess(){const rows=await request.get<{path:string}[]>('/mes/menu-access');menuPaths.value=rows.map(r=>r.path)}
  function hasMenu(path:string):boolean {
    if(currentUser.value?.roleId===9||!menuPaths.value?.length||['/','/dashboard','/login','/notifications','/user/profile'].includes(path))return true
    return menuPaths.value.some(p=>path===p||path.startsWith(p+'/'))
  }
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
    notifications.value = []
    currentUser.value = null
    menuPaths.value=null
    localStorage.removeItem(STORAGE_KEY)
  }

  async function loadNotifications() {
    if (!currentUser.value) { notifications.value = []; return }
    const rows = await request.get<Notification[]>('/mes/notices')
    notifications.value = rows.map(n => ({...n,isRead: Boolean(n.isRead)}))
  }
  async function markNotificationAsRead(id: number) {
    await request.post(`/mes/notices/${id}/read`)
    await loadNotifications()
  }
  async function markAllAsRead() {
    for (const notice of notifications.value.filter(n => !n.isRead)) await request.post(`/mes/notices/${notice.id}/read`)
    await loadNotifications()
  }
  return {
    currentUser,
    notifications,
    menuPaths,
    settings,
    refreshSettings,
    refreshMenuAccess,
    hasMenu,
    loadNotifications,
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

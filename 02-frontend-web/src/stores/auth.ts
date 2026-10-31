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
  avatar?: string
  gender: number
  permissionRoleId?: number
  roleId: number
  roleName: string
  deptId: number
  deptName: string
  status: number
  token?: string
}

export interface Notification {
  source?: 'notice' | 'security' | 'business'
  recordId?: number
  targetPath?: string
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
  const effectiveRoleId=computed(()=>currentUser.value?.permissionRoleId||currentUser.value?.roleId||0)
  const notifications = ref<Notification[]>([])
  const notificationLoading=ref(false),notificationError=ref('')
  const menuPaths=ref<string[]|null>(null)
  const settings=ref<Record<string,string>>({APP_NAME:'Mouse MES',DEFAULT_DURATION_MINUTES:'60'})
  async function refreshSettings(){const rows=await request.get<{code:string;value:string}[]>('/mes/settings');settings.value=Object.fromEntries(rows.map(r=>[r.code,r.value]));if(settings.value.APP_NAME)document.title=settings.value.APP_NAME}
  async function refreshMenuAccess(){if(currentUser.value)currentUser.value.permissionRoleId=await request.get<number>('/mes/access-role');const rows=await request.get<{path:string}[]>('/mes/menu-access');menuPaths.value=rows.map(r=>r.path)}
  function hasMenu(path:string):boolean {
    if(currentUser.value?.roleId===9||!menuPaths.value?.length||['/','/dashboard','/login','/notifications','/user/profile'].includes(path))return true
    const aliases:Record<string,string[]>={'/manufacturing':['/production/work-orders'],'/equipment/maintenance':['/equipment/repair-tasks','/equipment/bills'],'/assistant':['/intelligence']}
    return menuPaths.value.some(p=>path===p||path.startsWith(p+'/')||aliases[path]?.includes(p))
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
    const roleId = effectiveRoleId.value
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
    const user = await apiLogin({ userCode, password })
    currentUser.value = user as User
    return true
  }

  function logout() {
    notifications.value = []
    currentUser.value = null
    menuPaths.value=null
    localStorage.removeItem(STORAGE_KEY)
  }

  async function loadNotifications() {
    if(!currentUser.value){notifications.value=[];return}
    const token=currentUser.value.token;notificationLoading.value=true;notificationError.value=''
    try {
      const [rows,alerts,business]=await Promise.all([request.get<Notification[]>('/mes/notices'),request.get<Notification[]>('/account/alerts'),Number(effectiveRoleId.value)===9?request.get<Notification[]>('/mes/business-notices'):Promise.resolve([] as Notification[])])
      if(currentUser.value?.token!==token)return
      notifications.value=[...business.map(n=>({...n,source:'business' as const,recordId:n.id,id:-1000000000-n.id})),...alerts.map(n=>({...n,source:'security' as const,recordId:n.id,id:-n.id})),...rows.map(n=>({...n,source:'notice' as const,recordId:n.id}))].map(n=>({...n,isRead:Boolean(n.isRead)})).sort((a,b)=>String(b.createdAt).localeCompare(String(a.createdAt)))
    }catch(e:any){if(currentUser.value?.token===token)notificationError.value=e.message||'通知读取失败，请刷新重试'}finally{notificationLoading.value=false}
  }
  async function readNotification(n:Notification){await request.post(n.source==='business'?`/mes/business-notices/${n.recordId}/read`:n.id<0?`/account/alerts/${n.recordId||-n.id}/read`:`/mes/notices/${n.id}/read`)}
  async function markNotificationAsRead(id:number){const notice=notifications.value.find(n=>n.id===id);if(!notice||notice.isRead)return;await readNotification(notice);notice.isRead=true}
  async function markAllAsRead(){for(const n of notifications.value.filter(n=>!n.isRead)){await readNotification(n);n.isRead=true}}
  async function refreshProfile() {
    if (!currentUser.value) return
    const token=currentUser.value.token
    const fresh=await request.get<User>('/account/profile')
    if(currentUser.value?.token===token)currentUser.value={...currentUser.value,...fresh,token}
  }
  return {
    refreshProfile,
    currentUser,
    effectiveRoleId,
    notifications, notificationLoading, notificationError,
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

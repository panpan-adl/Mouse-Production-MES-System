import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'

const routes: RouteRecordRaw[] = [
  {path:'/intelligence',component:()=>import('@/views/foundation/Intelligence.vue'),meta:{requiresAuth:true}},
  {path:'/automation',component:()=>import('@/views/foundation/Automation.vue'),meta:{requiresAuth:true}},
  { path: '/fulfillment', component: () => import('@/views/foundation/Fulfillment.vue'), meta: { requiresAuth: true } },
  { path: '/foundation/:key?', component: () => import('@/views/foundation/FoundationCenter.vue'), meta: { requiresAuth: true } },
  { path: '/manufacturing', component: () => import('@/views/foundation/Manufacturing.vue'), meta: { requiresAuth: true } },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/dashboard',
    name: 'ProductionPanoramaDashboard',
    component: () => import('@/views/dashboard/ProductionPanoramaDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/production/plan',
    name: 'ProductionPlan',
    component: () => import('@/views/production/ProductionPlan.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/production/task',
    name: 'ProductionTask',
    component: () => import('@/views/production/ProductionTask.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/production/equipment-status',
    name: 'ProductionEquipmentStatus',
    component: () => import('@/views/production/EquipmentStatus.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/production/report',
    name: 'ProductionReport',
    component: () => import('@/views/production/ProductionReport.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/quality/tasks',
    name: 'QualityTask',
    redirect: '/manufacturing',
    meta: { requiresAuth: true },
  },
  {
    path: '/quality/check',
    name: 'QualityCheck',
    component: () => import('@/views/quality/QualityCheck.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/quality/standard',
    name: 'QualityStandard',
    component: () => import('@/views/quality/QualityStandard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/inventory/warehouse',
    name: 'WarehouseInventory',
    component: () => import('@/views/inventory/Warehouse.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/inventory/material',
    name: 'Material',
    component: () => import('@/views/inventory/Material.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/inventory/in',
    name: 'InventoryIn',
    redirect: '/fulfillment?tab=purchases',
    meta: { requiresAuth: true },
  },
  {
    path: '/inventory/out',
    name: 'InventoryOut',
    redirect: '/fulfillment',
    meta: { requiresAuth: true },
  },
  {
    path: '/inventory/material-requests',
    name: 'InventoryMaterialRequests',
    component: () => import('@/views/inventory/MaterialRequest.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/purchase/requests',
    name: 'PurchaseRequests',
    component: () => import('@/views/purchase/PurchaseRequest.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/purchase/order',
    name: 'PurchaseOrder',
    component: () => import('@/views/purchase/PurchaseOrder.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/purchase/supplier',
    name: 'Supplier',
    component: () => import('@/views/purchase/Supplier.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/order/customer',
    name: 'CustomerOrder',
    component: () => import('@/views/order/CustomerOrder.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/order/OrderList.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/equipment/list',
    name: 'Equipment',
    component: () => import('@/views/equipment/Equipment.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/equipment/maintenance',
    name: 'Maintenance',
    component: () => import('@/views/equipment/Maintenance.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/equipment/repair-tasks',
    name: 'EquipmentRepairTasks',
    component: () => import('@/views/equipment/RepairTasks.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/equipment/periodic',
    name: 'EquipmentPeriodicMaintenance',
    component: () => import('@/views/equipment/PeriodicMaintenance.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/equipment/bills',
    name: 'EquipmentMaintenanceBills',
    component: () => import('@/views/equipment/MaintenanceBills.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/system/user',
    name: 'SystemUser',
    component: () => import('@/views/system/User.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/system/role',
    name: 'SystemRole',
    component: () => import('@/views/system/Role.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/system/department',
    name: 'SystemDepartment',
    component: () => import('@/views/system/Department.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/attendance',
    name: 'Attendance',
    component: () => import('@/views/Attendance.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    redirect: '/dashboard',
    meta: { requiresAuth: true },
  },
  {
    path: '/prod-manager',
    name: 'ProdManagerDashboard',
    component: () => import('@/views/roles/ProdManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/workshop-manager',
    name: 'WorkshopManagerDashboard',
    component: () => import('@/views/roles/WorkshopManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/worker',
    name: 'WorkerDashboard',
    component: () => import('@/views/roles/WorkerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/qc-manager',
    name: 'QCManagerDashboard',
    component: () => import('@/views/roles/QCManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/warehouse-manager',
    name: 'WarehouseManagerDashboard',
    component: () => import('@/views/roles/WarehouseManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/purchase-manager',
    name: 'PurchaseManagerDashboard',
    component: () => import('@/views/roles/PurchaseManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/sales-manager',
    name: 'SalesManagerDashboard',
    component: () => import('@/views/roles/SalesManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/equipment-manager',
    name: 'EquipmentManagerDashboard',
    component: () => import('@/views/roles/EquipmentManagerDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/cfo',
    name: 'CFODashboard',
    component: () => import('@/views/roles/CFODashboard.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && authStore.isLoggedIn) {
    next(authStore.redirectPath)
  } else if (to.path === '/' && authStore.isLoggedIn && authStore.redirectPath !== '/') {
    next(authStore.redirectPath)
  } else if (to.meta.requiresAuth && !canVisit(Number(authStore.currentUser?.roleId), to.path)) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

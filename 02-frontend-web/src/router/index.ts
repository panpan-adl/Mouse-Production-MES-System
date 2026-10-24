import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'

const routes: RouteRecordRaw[] = [
  {path:'/production/schedules',component:()=>import('@/views/production/SchedulePlanning.vue'),meta:{requiresAuth:true}},
  ...[
    {path:'/purchase/receipts',title:'采购收货',mesTab:'documents',documentKind:'PURCHASE_RECEIPT',description:'选择已审批采购单，登记实际到货批次，逐批检验放行后确认过账。'},
    {path:'/inventory/shipments',title:'销售发货',mesTab:'documents',documentKind:'SALES_SHIPMENT',description:'选择销售订单与成品库存批次，出货检验放行后过账发货。'},
    {path:'/purchase/returns',title:'供应商退货',mesTab:'documents',documentKind:'PURCHASE_RETURN',description:'按原采购单与收货批次退货，确认后从指定仓库扣减库存。'},
    {path:'/inventory/production-issue',title:'生产领料',mesTab:'documents',documentKind:'LINE_ISSUE',description:'选择生产工单与库存批次，将物料从原仓库转入工单线边库。'},
    {path:'/inventory/production-return',title:'生产退料',mesTab:'documents',documentKind:'LINE_RETURN',description:'将指定工单线边库中的剩余物料退回仓库，保留批次与库存流水。'},
    {path:'/inventory/product-receipts',title:'产成品入库',mesTab:'documents',documentKind:'PRODUCTION_RECEIPT',description:'将已完成检验放行的工单产出转入成品仓库，之后可装箱与发货。'},
    {path:'/inventory/packages',title:'装箱管理',mesTab:'packages',description:'新建包装箱、装入成品或关联子箱，确认装箱并生成追溯二维码。'},
    {path:'/production/work-orders',title:'生产工单',mesTab:'work-orders',description:'创建订单或备货工单，选择产品、BOM、工艺路线及负责人后下达。'},
  ].map(module=>({path:module.path,component:()=>import('@/views/foundation/MesOperations.vue'),meta:{requiresAuth:true,...module}})),
  {path:'/mes',component:()=>import('@/views/foundation/MesOperations.vue'),meta:{requiresAuth:true}},
  {path:'/intelligence',redirect:'/assistant'},
  {path:'/assistant',component:()=>import('@/views/foundation/SkillAssistant.vue'),meta:{requiresAuth:true,title:'Skill 业务智能体'}},
  {path:'/automation',component:()=>import('@/views/foundation/Automation.vue'),meta:{requiresAuth:true}},
  {path:'/inventory/trace',component:()=>import('@/views/inventory/StockTrace.vue'),meta:{requiresAuth:true}},
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
    redirect: '/purchase/receipts',
    meta: { requiresAuth: true },
  },
  {
    path: '/inventory/out',
    name: 'InventoryOut',
    redirect: '/inventory/shipments',
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
    redirect:'/equipment/maintenance?tab=execution',
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
    redirect:'/equipment/maintenance?tab=bills',
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
    path: '/system/logs',
    name: 'SystemLogs',
    component: () => import('@/views/system/Logs.vue'),
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

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  if(authStore.isLoggedIn&&to.meta.requiresAuth){try{await authStore.refreshMenuAccess()}catch{authStore.logout();next('/login');return}}
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && authStore.isLoggedIn) {
    next(authStore.redirectPath)
  } else if (to.path === '/' && authStore.isLoggedIn && authStore.redirectPath !== '/') {
    next(authStore.redirectPath)
  } else if (to.meta.requiresAuth && !canVisit(Number(authStore.effectiveRoleId), to.path)) {
    next('/dashboard')
  } else if(to.meta.requiresAuth&&!authStore.hasMenu(to.path)) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

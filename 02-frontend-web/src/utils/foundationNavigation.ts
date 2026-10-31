export const foundationGroups = [
  { title: '基础资料与配置', module: '系统管理', keys: ['units', 'categories', 'code-rules', 'parameters', 'jobs'], returnPath: '', returnLabel: '' },
  { title: '产品与工艺', module: '生产管理', keys: ['products', 'workshops', 'stations', 'station-resources', 'bom-versions', 'bom-lines', 'routes', 'operations', 'consumption', 'sops'], returnPath: '/production/work-orders', returnLabel: '生产工单' },
  { title: '班组与排班配置', module: '生产管理', keys: ['teams', 'members', 'shifts', 'schedules', 'holidays', 'rotations', 'rotation-teams', 'rotation-shifts'], returnPath: '/production/schedules', returnLabel: '班组排班计划' },
  { title: '检验与缺陷配置', module: '质量管理', keys: ['defects', 'qc-items', 'qc-templates', 'qc-lines'], returnPath: '/quality/tasks', returnLabel: '质检任务' },
  { title: '设备点检配置', module: '设备管理', keys: ['check-subjects', 'checks', 'check-plan-subjects', 'check-records'], returnPath: '/equipment/list', returnLabel: '设备台账' },
  { title: '角色与权限', module: '系统管理', keys: ['positions', 'user-positions', 'menus', 'role-menus'], returnPath: '/system/role', returnLabel: '角色与业务权限' },
  { title: '仓储位置', module: '仓库管理', keys: ['warehouses', 'areas', 'locations'], returnPath: '/inventory/warehouse', returnLabel: '仓库库存' },
]

// Existing business pages already manage these archives. Keep old bookmarks usable.
export const foundationDestinations: Record<string, string> = {
  customers: '/order/customer', suppliers: '/purchase/supplier', materials: '/inventory/material',
  equipment: '/equipment/list', 'maintenance-plans': '/equipment/periodic',
}

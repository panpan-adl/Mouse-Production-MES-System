// Shared system modules keep existing URLs and permission checks.
export const systemSections = [
  { title: '组织与人员', description: '维护部门结构与员工档案，管理员工所属部门和在职状态。', tabs: [
    { path: '/system/department', label: '部门管理' },
    { path: '/system/user', label: '员工管理' },
  ] },
  { title: '角色与权限', description: '维护岗位职责、员工岗位分配以及菜单访问权限。', tabs: [
    { path: '/system/role', label: '角色与业务权限' },
    { path: '/foundation/positions', label: '岗位档案' },
    { path: '/foundation/user-positions', label: '员工岗位分配' },
    { path: '/foundation/menus', label: '菜单管理' },
    { path: '/foundation/role-menus', label: '角色菜单权限' },
  ] },
  { title: '基础资料与配置', description: '统一维护通用基础资料、编码规则、系统参数与定时任务。', tabs: [
    { path: '/foundation/units', label: '计量单位' },
    { path: '/foundation/categories', label: '分类管理' },
    { path: '/foundation/code-rules', label: '编码规则' },
    { path: '/foundation/parameters', label: '系统参数' },
    { path: '/foundation/jobs', label: '定时任务' },
  ] },
  { title: '日志管理', description: '查询系统操作记录，追踪操作人员、执行结果与异常信息。', tabs: [
    { path: '/system/logs', label: '系统日志' },
  ] },
]

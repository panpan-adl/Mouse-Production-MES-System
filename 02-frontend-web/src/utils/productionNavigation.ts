import { foundationGroups } from './foundationNavigation'

export const productionSections = [
  { title: '产品与工艺', description: '维护产品、BOM、工艺路线和生产资源。', module: '生产管理', tabs:
    foundationGroups.find(g=>g.title==='产品与工艺')!.keys.map(key=>({path:`/foundation/${key}`,label:''})) },
  { title: '生产计划与工单', description: '安排生产计划，下达工单并跟踪执行进度。', module: '生产管理', tabs: [
    {path:'/production/plan',label:'生产计划'}, {path:'/production/work-orders',label:'生产工单'}] },
  { title: '生产执行', description: '围绕任务处理开工、报工、工序检验与放行。', module: '生产管理', tabs: [
    {path:'/production/task',label:'生产任务'}, {path:'/production/report',label:'报工记录'}, {path:'/manufacturing',label:'工序与质量放行'}] },
  { title: '班组与排班', description: '安排班组班次，维护人员、轮班规则与排班日历。', module: '生产管理', tabs: [
    {path:'/production/schedules',label:'排班日历'},
    ...foundationGroups.find(g=>g.title==='班组与排班配置')!.keys.map(key=>({path:`/foundation/${key}`,label:''}))] },
  { title: '设备台账', description: '查看设备档案、运行状态与故障报修。', module: '设备管理', tabs: [
    {path:'/equipment/list',label:'设备台账'}, {path:'/production/equipment-status',label:'设备状态与报修'}] },
  { title: '点检与保养', description: '配置点检项目与计划，安排定期保养并查看执行记录。', module: '设备管理', tabs: [
    ...foundationGroups.find(g=>g.title==='设备点检配置')!.keys.map(key=>({path:`/foundation/${key}`,label:''})),
    {path:'/equipment/periodic',label:'保养计划与执行'}] },
  { title: '设备维修', description: '处理设备故障、维修任务与维修记录。', module: '设备管理', tabs: [
    {path:'/equipment/maintenance',label:'维修管理'}] },
]

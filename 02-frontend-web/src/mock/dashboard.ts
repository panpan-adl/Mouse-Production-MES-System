export interface DashboardFilters {
  date: string
  shift: string
  line: string
}

export interface DashboardKpi {
  unavailable?: boolean
  key: string
  label: string
  value: number
  unit: string
  tone: 'blue' | 'green' | 'yellow' | 'red'
  precision?: number
}

export interface HourlyOutput {
  hour: string
  plan: number
  actual: number
}

export interface WorkOrderStatus {
  label: string
  value: number
  tone: 'blue' | 'green' | 'yellow' | 'red' | 'gray'
}

export interface MaterialShortage {
  materialName: string
  shortageQuantity: number
  incomingQuantity: number
  unit: string
}

export interface ProductionFlowNode {
  key: string
  name: string
  count: number
  completionRate: number
  status: 'normal' | 'warning' | 'error'
}

export interface WorkOrderRow {
  id: string
  productModel: string
  batchNo: string
  planQuantity: number
  completedQuantity: number
  dueDate: string
  status: string
  statusTone: 'blue' | 'green' | 'yellow' | 'red'
  nodeKey: string
}

export interface QualityTrend {
  label: string
  passed: number
  failed: number
}

export interface DefectRank {
  defectType: string
  quantity: number
}

export interface InventorySummary {
  rawMaterial: number
  wip: number
  finishedGoods: number
  lowStockCount: number
}

export interface RealtimeAlert {
  id: string
  time: string
  type: 'warning' | 'error'
  title: string
  content: string
}

export interface DashboardData {
  equipmentStatus?: {label:string;value:number}[]
  equipmentDetails?: {id:number;equipment_name:string;status:string;availability:string}[]
  materialSafety?: {id:number;material_name:string;unit:string;safety_stock:number;current_quantity:number}[]
  filters: DashboardFilters
  updatedAt: string
  kpis: DashboardKpi[]
  hourlyOutput: HourlyOutput[]
  workOrderStatus: WorkOrderStatus[]
  materialReadinessRate: number
  materialShortages: MaterialShortage[]
  flowNodes: ProductionFlowNode[]
  workOrders: WorkOrderRow[]
  qualityTrend: QualityTrend[]
  defectRanks: DefectRank[]
  inventory: InventorySummary
  alerts: RealtimeAlert[]
}

const workOrders: WorkOrderRow[] = [
  { id: 'WO-20260717-001', productModel: 'A100有线办公鼠标', batchNo: 'BATCH-A100-0717', planQuantity: 500, completedQuantity: 420, dueDate: '2026/7/17 18:00', status: '生产中', statusTone: 'blue', nodeKey: 'work' },
  { id: 'WO-20260717-002', productModel: 'B200无线电竞鼠标', batchNo: 'BATCH-B200-0717', planQuantity: 300, completedQuantity: 180, dueDate: '2026/7/17 20:00', status: '生产中', statusTone: 'blue', nodeKey: 'work' },
  { id: 'WO-20260717-003', productModel: 'B300静音办公鼠标', batchNo: 'BATCH-B300-0717', planQuantity: 240, completedQuantity: 180, dueDate: '2026/7/18 10:00', status: '待质检', statusTone: 'yellow', nodeKey: 'qc' },
  { id: 'WO-20260717-004', productModel: 'A200有线电竞鼠标', batchNo: 'BATCH-A200-0717', planQuantity: 160, completedQuantity: 85, dueDate: '2026/7/17 17:30', status: '缺料暂停', statusTone: 'red', nodeKey: 'bom' },
  { id: 'WO-20260717-005', productModel: 'A100有线办公鼠标', batchNo: 'BATCH-A100-0716', planQuantity: 360, completedQuantity: 360, dueDate: '2026/7/17 14:00', status: '已完成', statusTone: 'green', nodeKey: 'stock' },
]

const alerts: RealtimeAlert[] = [
  { id: 'ALERT-001', time: '14:42', type: 'error', title: '微动开关库存不足', content: 'WO-20260717-004 缺少 120 个微动开关，已建议生成采购需求。' },
  { id: 'ALERT-002', time: '14:18', type: 'warning', title: '工单存在延期风险', content: 'A200有线电竞鼠标完成进度 53%，距离交期不足 3 小时。' },
  { id: 'ALERT-003', time: '13:55', type: 'warning', title: '成品质检待处理', content: 'B300静音办公鼠标已完成报工，等待质检员登记检验结果。' },
  { id: 'ALERT-004', time: '11:36', type: 'error', title: '重复报工被拦截', content: 'WO-20260717-002 已存在 10:00 班次报工记录，系统阻止重复提交。' },
]

const hourlyOutput: HourlyOutput[] = [
  { hour: '08', plan: 90, actual: 82 },
  { hour: '09', plan: 120, actual: 118 },
  { hour: '10', plan: 150, actual: 142 },
  { hour: '11', plan: 150, actual: 136 },
  { hour: '12', plan: 80, actual: 76 },
  { hour: '13', plan: 150, actual: 124 },
  { hour: '14', plan: 170, actual: 156 },
  { hour: '15', plan: 160, actual: 131 },
]

export function createDashboardData(filters: DashboardFilters): DashboardData {
  const planOutput = workOrders.reduce((sum, item) => sum + item.planQuantity, 0)
  const completedOutput = workOrders.reduce((sum, item) => sum + item.completedQuantity, 0)
  const totalInspected = 830
  const firstPassQuantity = 800
  const materialShortages: MaterialShortage[] = [
    { materialName: '微动开关', shortageQuantity: 120, incomingQuantity: 300, unit: '个' },
    { materialName: '静音编码器', shortageQuantity: 80, incomingQuantity: 100, unit: '个' },
    { materialName: 'B200上盖外壳', shortageQuantity: 64, incomingQuantity: 120, unit: '套' },
    { materialName: 'USB连接线', shortageQuantity: 40, incomingQuantity: 200, unit: '根' },
    { materialName: '防滑脚垫', shortageQuantity: 30, incomingQuantity: 60, unit: '套' },
  ]

  return {
    filters,
    updatedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    kpis: [
      { key: 'plan', label: '今日计划产量', value: planOutput, unit: '只', tone: 'blue' },
      { key: 'done', label: '今日完成产量', value: completedOutput, unit: '只', tone: 'green' },
      { key: 'rate', label: '计划达成率', value: (completedOutput / planOutput) * 100, unit: '%', tone: 'yellow', precision: 1 },
      { key: 'pass', label: '一次合格率', value: (firstPassQuantity / totalInspected) * 100, unit: '%', tone: 'green', precision: 1 },
      { key: 'active', label: '在制工单数', value: workOrders.filter((item) => item.status !== '已完成').length, unit: '单', tone: 'blue' },
      { key: 'alerts', label: '缺料与异常数', value: alerts.length, unit: '条', tone: 'red' },
    ],
    hourlyOutput,
    workOrderStatus: [
      { label: '待下发', value: 2, tone: 'yellow' },
      { label: '生产中', value: 4, tone: 'blue' },
      { label: '待检', value: 3, tone: 'yellow' },
      { label: '已完成', value: 5, tone: 'green' },
      { label: '暂停', value: 1, tone: 'red' },
    ],
    materialReadinessRate: 87.5,
    materialShortages,
    flowNodes: [
      { key: 'order', name: '客户订单', count: 6, completionRate: 83, status: 'normal' },
      { key: 'plan', name: '生产计划', count: 5, completionRate: 78, status: 'normal' },
      { key: 'bom', name: 'BOM齐套', count: 4, completionRate: 88, status: 'warning' },
      { key: 'work', name: '生产工单', count: 5, completionRate: 69, status: 'warning' },
      { key: 'report', name: '过程报工', count: 4, completionRate: 72, status: 'normal' },
      { key: 'qc', name: '质量检验', count: 3, completionRate: 96, status: 'normal' },
      { key: 'stock', name: '成品入库', count: 2, completionRate: 40, status: 'warning' },
    ],
    workOrders,
    qualityTrend: [
      { label: '08', passed: 80, failed: 2 },
      { label: '09', passed: 116, failed: 2 },
      { label: '10', passed: 136, failed: 6 },
      { label: '11', passed: 132, failed: 4 },
      { label: '12', passed: 75, failed: 1 },
      { label: '13', passed: 119, failed: 5 },
      { label: '14', passed: 142, failed: 14 },
    ],
    defectRanks: [
      { defectType: '按键手感异常', quantity: 12 },
      { defectType: '外观划伤', quantity: 9 },
      { defectType: '滚轮跳动', quantity: 6 },
      { defectType: 'DPI偏差', quantity: 4 },
      { defectType: 'USB接触不良', quantity: 3 },
    ],
    inventory: { rawMaterial: 6820, wip: 745, finishedGoods: 520, lowStockCount: materialShortages.length },
    alerts,
  }
}

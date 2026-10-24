import request from '@/utils/request'

export interface Customer {
  id?: number
  customerCode: string
  customerName: string
  contactPerson?: string
  phone?: string
  email?: string
  address?: string
  status?: string
  createdAt?: string
  updatedAt?: string
  isDeleted?: number
}

export interface Order {
  id?: number
  orderNo: string
  customerName: string
  customerPhone?: string
  productName: string
  quantity?: number
  unitPrice?: number
  totalAmount?: number
  orderDate?: string
  deliveryDate?: string
  orderStatus?: number
  priority?: number
  remark?: string
  createBy?: number
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface Supplier {
  id?: number
  supplierCode: string
  supplierName: string
  contactPerson?: string
  contactPhone?: string
  address?: string
  bankName?: string
  bankAccount?: string
  status?: number
  remark?: string
}

export interface PurchaseOrderItem {
  id?: number
  poId?: number
  materialId: number
  quantity: number
  unitPrice?: number
  amount?: number
  receivedQuantity?: number
  remark?: string
}

export interface PurchaseOrder {
  id?: number
  poNo: string
  supplierId: number
  orderDate?: string
  expectedDate?: string
  totalAmount?: number
  status?: string
  remark?: string
  createdBy?: number
  items?: PurchaseOrderItem[]
}

export interface PurchaseReceiveRequest {
  warehouseId: number
  createdBy?: number
  remark?: string
  items: {
    purchaseOrderItemId: number
    receiveQuantity: number
    lotId?: number
    locationId?: number
    remark?: string
  }[]
}

export interface MaterialRequest {
  id?: number
  requestNo?: string
  sourceTaskId?: number
  sourceTaskNo?: string
  productName?: string
  materialId: number
  materialCode?: string
  materialName?: string
  requiredQuantity: number
  availableQuantity?: number
  shortageQuantity?: number
  warehouseId?: number
  requestStatus?: string
  remark?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
  isDeleted?: number
}

export interface Material {
  id?: number
  materialCode: string
  materialName: string
  materialType?: number
  specification?: string
  unit?: string
  safetyStock?: number
  currentStock?: number
  status?: number
  remark?: string
}

export interface Warehouse {
  id?: number
  warehouseCode: string
  warehouseName: string
  location?: string
  managerId?: number
  managerName?: string
  status?: number
  remark?: string
}

export interface Location {
  id?: number
  warehouseId: number
  locationCode: string
  locationName: string
  locationType?: string
  status?: string
  remark?: string
}

export interface Inventory {
  id?: number
  warehouseId: number
  locationId?: number
  materialId: number
  lotId?: number
  quantity: number
  lastUpdateTime?: string
}

export interface StockItem {
  id?: number
  materialId: number
  lotId?: number
  quantity: number
  locationId?: number
  remark?: string
}

export interface StockIn {
  id?: number
  stockInNo: string
  stockInType: string
  warehouseId: number
  sourceOrderId?: number
  sourceOrderNo?: string
  totalQuantity?: number
  status?: string
  remark?: string
  createdBy?: number
  createdAt?: string
  items?: StockItem[]
}

export interface StockOut {
  id?: number
  stockOutNo: string
  stockOutType: string
  warehouseId: number
  sourceOrderId?: number
  sourceOrderNo?: string
  totalQuantity?: number
  status?: string
  remark?: string
  createdBy?: number
  createdAt?: string
  items?: StockItem[]
}

export interface QcInspectionItem {
  id?: number
  inspectionItem: string
  inspectionStandard?: string
  standardValue?: string
  actualValue?: string
  sampleQuantity?: number
  qualifiedQuantity?: number
  defectiveQuantity?: number
  itemResult?: string
  defectReason?: string
  handleMethod?: string
  remark?: string
}

export interface QcInspection {
  id?: number
  inspectionNo: string
  inspectionType: string
  sourceOrderId?: number
  sourceOrderNo?: string
  sampleQuantity?: number
  qualifiedQuantity?: number
  defectiveQuantity?: number
  qualifiedRate?: number
  result?: string
  defectReason?: string
  handleMethod?: string
  businessStatus?: string
  remark?: string
  createdBy?: number
  createdAt?: string
  items?: QcInspectionItem[]
}

export interface QualityStandard {
  id?: number
  productId?: number
  productName?: string
  standardName: string
  standardContent: string
  qualifiedThreshold?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface Equipment {
  id?: number
  equipmentCode: string
  equipmentName: string
  equipmentType?: string
  model?: string
  manufacturer?: string
  purchaseDate?: string
  warrantyPeriod?: number
  location?: string
  status?: string
  responsibleId?: number
  responsibleName?: string
  remark?: string
}

export interface Maintenance {
  id?: number
  maintenanceNo: string
  equipmentId: number
  equipmentName?: string
  maintenanceType?: number
  faultDesc?: string
  maintenanceContent?: string
  maintenanceUserId?: number
  maintenanceUserName?: string
  startTime?: string
  endTime?: string
  cost?: number
  result?: string
  status?: string
  remark?: string
}

export interface FinanceOverview {
  salesIncome: number
  purchaseExpense: number
  maintenanceExpense: number
  inventoryAmount: number
  grossProfit: number
}

export interface FinanceGroupStat {
  groupKey: string
  groupName: string
  amount: number
}

export interface FinanceInventoryValueStat {
  currentQuantity: number
  currentAmount: number
  lowStockCount: number
}

export interface FinanceQualityLossStat {
  defectiveQuantity: number
  scrapQuantity: number
  reworkQuantity: number
}

export interface FinanceTrendStat {
  month: string
  salesIncome: number
  purchaseExpense: number
  maintenanceExpense: number
  profit: number
}

/** 生产任务：由生产计划下发，由操作工领取和执行。 */
export interface ProductionTask {
  id?: number
  taskNo: string
  planId: number
  taskType?: number
  productId?: number
  productName?: string
  quantity?: number
  assignedUserId?: number
  assignedUserName?: string
  taskStatus?: number
  startTime?: string
  endTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 生产计划：由销售订单生成，记录产品、数量与计划周期。 */
export interface ProductionPlan {
  id?: number
  planNo: string
  orderId?: number
  productId?: number
  productName?: string
  planQuantity?: number
  completedQuantity?: number
  startDate?: string
  endDate?: string
  planStatus?: number
  priority?: number
  createBy?: number
  createTime?: string
  updateTime?: string
}

/** 生产报工：只记录实际完成数量，质量结论由质检环节负责。 */export interface ProductionReport {
  id?: number
  taskId: number
  reportUserId: number
  reportUserName?: string
  reportQuantity: number
  qualifiedQuantity?: number
  defectQuantity?: number
  reportTime?: string
  remark?: string
}

export const customerApi = {
  list: () => request.get<Customer[]>('/customer/list'),
  get: (id: number) => request.get<Customer>(`/customer/${id}`),
  byCode: (customerCode: string) => request.get<Customer>(`/customer/code/${customerCode}`),
  create: (data: Customer) => request.post<Customer>('/customer/add', data),
  update: (id: number, data: Customer) => request.put<Customer>(`/customer/${id}`, data),
  remove: (id: number) => request.delete(`/customer/${id}`),
}

export const orderApi = {
  list: () => request.get<Order[]>('/orders'),
  get: (id: number) => request.get<Order>(`/orders/${id}`),
  byOrderNo: (orderNo: string) => request.get<Order>(`/orders/orderno/${orderNo}`),
  byStatus: (status: number) => request.get<Order[]>(`/orders/status/${status}`),
  create: (data: Order) => request.post<Order>('/orders', data),
  update: (id: number, data: Order) => request.put<Order>(`/orders/${id}`, data),
  confirm: (id: number) => request.put<Order>(`/orders/${id}/confirm`),
  cancel: (id: number) => request.put<Order>(`/orders/${id}/cancel`),
}

export const supplierApi = {
  list: () => request.get<Supplier[]>('/suppliers'),
  search: (params: { supplierCode?: string; supplierName?: string; status?: number | '' }) =>
    request.get<Supplier[]>('/suppliers/search', { params }),
  create: (data: Supplier) => request.post<Supplier>('/suppliers', data),
  update: (id: number, data: Supplier) => request.put<Supplier>(`/suppliers/${id}`, data),
  enable: (id: number) => request.put<Supplier>(`/suppliers/${id}/enable`),
  disable: (id: number) => request.put<Supplier>(`/suppliers/${id}/disable`),
  remove: (id: number) => request.delete(`/suppliers/${id}`),
}

export const purchaseOrderApi = {
  list: () => request.get<PurchaseOrder[]>('/purchase-order/list'),
  byStatus: (status: string) => request.get<PurchaseOrder[]>(`/purchase-order/status/${status}`),
  create: (data: PurchaseOrder) => request.post<PurchaseOrder>('/purchase-order/add', data),
  update: (id: number, data: Partial<PurchaseOrder>) => request.put<PurchaseOrder>(`/purchase-order/${id}`, data),
  submit: (id: number) => request.put<PurchaseOrder>(`/purchase-order/${id}/submit`),
  approve: (id: number) => request.put<PurchaseOrder>(`/purchase-order/${id}/approve`),
  cancel: (id: number) => request.put<PurchaseOrder>(`/purchase-order/${id}/cancel`),
  receive: (id: number, data: PurchaseReceiveRequest) => request.put<PurchaseOrder>(`/purchase-order/${id}/receive`, data),
  syncReceived: (id: number) => request.put<PurchaseOrder>(`/purchase-order/${id}/sync-received`),
  remove: (id: number) => request.delete(`/purchase-order/${id}`),
}

export const materialRequestApi = {
  list: () => request.get<MaterialRequest[]>('/material-requests'),
  byStatus: (requestStatus: string) => request.get<MaterialRequest[]>(`/material-requests/status/${requestStatus}`),
  get: (id: number) => request.get<MaterialRequest>(`/material-requests/${id}`),
  create: (data: MaterialRequest) => request.post<MaterialRequest>('/material-requests', data),
  update: (id: number, data: Partial<MaterialRequest>) => request.put<MaterialRequest>(`/material-requests/${id}`, data),
  requestPurchase: (id: number) => request.put<MaterialRequest>(`/material-requests/${id}/request-purchase`),
  markPurchaseOrdered: (id: number) => request.put<MaterialRequest>(`/material-requests/${id}/purchase-ordered`),
  remove: (id: number) => request.delete(`/material-requests/${id}`),
}

export const materialApi = {
  list: () => request.get<Material[]>('/materials'),
  create: (data: Material) => request.post<Material>('/materials', data),
  update: (id: number, data: Material) => request.put<Material>(`/materials/${id}`, data),
  remove: (id: number) => request.delete(`/materials/${id}`),
}

export const warehouseApi = {
  list: () => request.get<Warehouse[]>('/warehouses'),
  create: (data: Warehouse) => request.post<Warehouse>('/warehouses', data),
  update: (id: number, data: Warehouse) => request.put<Warehouse>(`/warehouses/${id}`, data),
  remove: (id: number) => request.delete(`/warehouses/${id}`),
}

export const locationApi = {
  list: () => request.get<Location[]>('/locations/list'),
  byWarehouse: (warehouseId: number) => request.get<Location[]>(`/locations/warehouse/${warehouseId}`),
  create: (data: Location) => request.post<Location>('/locations/add', data),
  update: (id: number, data: Location) => request.put<Location>(`/locations/${id}`, data),
  remove: (id: number) => request.delete(`/locations/${id}`),
}

export const inventoryApi = {
  list: () => request.get<Inventory[]>('/inventory/list'),
  search: (params: { materialId?: number | ''; warehouseId?: number | ''; locationId?: number | '' }) =>
    request.get<Inventory[]>('/inventory/search', { params }),
  lowStock: () => request.get<Inventory[]>('/inventory/low-stock'),
  transactions: () => request.get<any[]>('/inventory/transactions'),
  create: (data: Inventory) => request.post<Inventory>('/inventory/add', data),
  update: (id: number, data: Inventory) => request.put<Inventory>(`/inventory/${id}`, data),
  remove: (id: number) => request.delete(`/inventory/${id}`),
}

export const stockInApi = {
  list: () => request.get<StockIn[]>('/stock-in/list'),
  create: (data: StockIn) => request.post<StockIn>('/stock-in/add', data),
  update: (id: number, data: StockIn) => request.put<StockIn>(`/stock-in/${id}`, data),
  remove: (id: number) => request.delete(`/stock-in/${id}`),
}

export const stockOutApi = {
  list: () => request.get<StockOut[]>('/stock-out/list'),
  create: (data: StockOut) => request.post<StockOut>('/stock-out/add', data),
  update: (id: number, data: StockOut) => request.put<StockOut>(`/stock-out/${id}`, data),
  remove: (id: number) => request.delete(`/stock-out/${id}`),
}

export const qcInspectionApi = {
  list: () => request.get<QcInspection[]>('/qc-inspection/list'),
  byType: (inspectionType: string) => request.get<QcInspection[]>(`/qc-inspection/type/${inspectionType}`),
  byResult: (result: string) => request.get<QcInspection[]>(`/qc-inspection/result/${result}`),
  create: (data: QcInspection) => request.post<QcInspection>('/qc-inspection/add', data),
  update: (id: number, data: QcInspection) => request.put<QcInspection>(`/qc-inspection/${id}`, data),
  remove: (id: number) => request.delete(`/qc-inspection/${id}`),
}

export const qualityStandardApi = {
  list: () => request.get<QualityStandard[]>('/quality-standards'),
  get: (id: number) => request.get<QualityStandard>(`/quality-standards/${id}`),
  byProduct: (productId: number) => request.get<QualityStandard[]>(`/quality-standards/product/${productId}`),
  update: (id: number, data: QualityStandard) => request.put<QualityStandard>(`/quality-standards/${id}`, data),
}

export const equipmentApi = {
  list: () => request.get<Equipment[]>('/equipments'),
  create: (data: Equipment) => request.post<Equipment>('/equipments', data),
  update: (id: number, data: Equipment) => request.put<Equipment>(`/equipments/${id}`, data),
  remove: (id: number) => request.delete(`/equipments/${id}`),
}

export const maintenanceApi = {
  list: () => request.get<Maintenance[]>('/maintenances'),
  create: (data: Maintenance) => request.post<Maintenance>('/maintenances', data),
  update: (id: number, data: Maintenance) => request.put<Maintenance>(`/maintenances/${id}`, data),
  assign: (id: number, data: { userId: number; userName: string }) => request.put<Maintenance>(`/maintenances/${id}/assign`, data),
  start: (id: number) => request.put<Maintenance>(`/maintenances/${id}/start`),
  finish: (id: number, data: { maintenanceContent: string; result: string; cost: number; scrapEquipment: boolean; remark?: string }) =>
    request.put<Maintenance>(`/maintenances/${id}/finish`, data),
  cancel: (id: number) => request.put<Maintenance>(`/maintenances/${id}/cancel`),
}

export const financeApi = {
  overview: () => request.get<FinanceOverview>('/finance/overview'),
  overviewByMonth: (month: string) => request.get<FinanceOverview>(`/finance/overview/month/${month}`),
  purchaseMonthly: () => request.get<FinanceGroupStat[]>('/finance/purchase/monthly'),
  purchaseSupplier: () => request.get<FinanceGroupStat[]>('/finance/purchase/supplier'),
  purchaseMaterial: () => request.get<FinanceGroupStat[]>('/finance/purchase/material'),
  inventoryValue: () => request.get<FinanceInventoryValueStat>('/finance/inventory/value'),
  qualityLoss: () => request.get<FinanceQualityLossStat>('/finance/quality/loss'),
  trendMonthly: () => request.get<FinanceTrendStat[]>('/finance/trend/monthly'),
}

export const productionTaskApi = {
  list: () => request.get<ProductionTask[]>('/production-tasks'),
  get: (id: number) => request.get<ProductionTask>(`/production-tasks/${id}`),
  byPlan: (planId: number) => request.get<ProductionTask[]>(`/production-tasks/plan/${planId}`),
  create: (data: ProductionTask) => request.post<ProductionTask>('/production-tasks', data),
  update: (id: number, data: Partial<ProductionTask>) => request.put<ProductionTask>(`/production-tasks/${id}`, data),
  claim: (id: number, data: { userId: number; userName: string }) =>
    request.put<ProductionTask>(`/production-tasks/${id}/claim`, data),
  start: (id: number, data: { userId: number; userName: string }) =>
    request.put<ProductionTask>(`/production-tasks/${id}/start`, data),
  report: (id: number, data: { userId: number; userName: string; reportQuantity: number; remark?: string }) =>
    request.post<ProductionTask>(`/production-tasks/${id}/report`, data),
}

export const productionPlanApi = {
  list: () => request.get<ProductionPlan[]>('/production-plans'),
  get: (id: number) => request.get<ProductionPlan>(`/production-plans/${id}`),
  byOrder: (orderId: number) => request.get<ProductionPlan[]>(`/production-plans/order/${orderId}`),
  create: (data: ProductionPlan) => request.post<ProductionPlan>('/production-plans', data),
  createFromOrder: (orderId: number) => request.post<ProductionPlan>(`/production-plans/from-order/${orderId}`),
  update: (id: number, data: Partial<ProductionPlan>) => request.put<ProductionPlan>(`/production-plans/${id}`, data),
  remove: (id: number) => request.delete(`/production-plans/${id}`),
}

export const productionReportApi = {
  list: () => request.get<ProductionReport[]>('/production-reports'),
  byTask: (taskId: number) => request.get<ProductionReport[]>(`/production-reports/task/${taskId}`),
  create: (data: ProductionReport) => request.post<ProductionReport>('/production-reports', data),
  update: (id: number, data: Partial<ProductionReport>) => request.put<ProductionReport>(`/production-reports/${id}`, data),
}


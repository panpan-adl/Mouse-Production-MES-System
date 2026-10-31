// Shared by the sidebar, breadcrumbs and in-page navigation.
export const warehouseSections = [
  { title: '仓储基础', description: '按仓库、库区、库位维护仓储结构，并管理物料档案。', tabs: [
    {path:'/foundation/warehouses',label:'仓库'}, {path:'/foundation/areas',label:'库区'},
    {path:'/foundation/locations',label:'库位'}, {path:'/inventory/material',label:'物料档案'}] },
  { title: '库存管理', description: '查看库存余额、安全库存及批次流转记录。', tabs: [
    {path:'/inventory/warehouse',label:'库存总览'}, {path:'/inventory/trace',label:'批次追溯'}] },
  { title: '入库管理', description: '统一查看入库单据，按采购收货、生产退料和产成品入库办理业务。', tabs: [
    {path:'/inventory/in',label:'全部入库'}, {path:'/purchase/receipts',label:'采购收货'},
    {path:'/inventory/production-return',label:'生产退料'}, {path:'/inventory/product-receipts',label:'产成品入库'}] },
  { title: '出库管理', description: '统一查看出库单据，处理生产领料、销售发货及装箱作业。', tabs: [
    {path:'/inventory/out',label:'全部出库'}, {path:'/inventory/production-issue',label:'生产领料'},
    {path:'/inventory/shipments',label:'销售发货'}, {path:'/inventory/packages',label:'装箱作业'}] },
  { title: '物料需求', description: '提交物料需求，核对库存缺口并跟踪采购处理进度。', tabs: [
    {path:'/inventory/material-requests',label:'需求申请与处理'}] },
]

export const warehouseDocumentPaths: Record<string, string> = {
  PURCHASE_RECEIPT: '/purchase/receipts', LINE_RETURN: '/inventory/production-return',
  PRODUCTION_RECEIPT: '/inventory/product-receipts', LINE_ISSUE: '/inventory/production-issue',
  SALES_SHIPMENT: '/inventory/shipments',
}

// Shared by the sidebar, breadcrumbs and in-page navigation.
export const warehouseSections = [
  { title: '仓储基础', description: '按仓库、库区、库位维护仓储结构，并管理物料档案。', tabs: [
    {path:'/inventory/material',label:'物料档案'}, {path:'/foundation/warehouses',label:'仓库'},
    {path:'/foundation/areas',label:'库区'}, {path:'/foundation/locations',label:'库位'}] },
  { title: '入库管理', description: '按来源办理生产退料或产成品入库，依次完成确认、检验与过账；采购收货在采购板块办理。', tabs: [
    {path:'/inventory/in',label:'全部入库'},

    {path:'/inventory/production-return',label:'生产退料'}, {path:'/inventory/product-receipts',label:'产成品入库'}] },
  { title: '出库管理', description: '按用途办理生产领料或销售发货；装箱用于关联产品批次，不代替发货过账。', tabs: [
    {path:'/inventory/out',label:'全部出库'}, {path:'/inventory/production-issue',label:'生产领料'},
    {path:'/inventory/shipments',label:'销售发货'}, {path:'/inventory/packages',label:'装箱作业'}] },
  { title: '库存管理', description: '查看库存余额、安全库存及批次流转记录。', tabs: [
    {path:'/inventory/warehouse',label:'库存总览'}, {path:'/inventory/trace',label:'批次追溯'}] },
]

export const warehouseDocumentPaths: Record<string, string> = {
  LINE_RETURN: '/inventory/production-return',
  PRODUCTION_RECEIPT: '/inventory/product-receipts', LINE_ISSUE: '/inventory/production-issue',
  SALES_SHIPMENT: '/inventory/shipments',
}

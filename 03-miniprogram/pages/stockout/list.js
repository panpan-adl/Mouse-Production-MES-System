const { stockOutApi, warehouseApi } = require('../../utils/api.js')

Page({
  data: {
    stockOuts: [],
    warehouses: [],
    loading: false,
    searchText: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    Promise.all([
      stockOutApi.list().catch(() => []),
      warehouseApi.list().catch(() => [])
    ]).then(([stockOuts, warehouses]) => {
      this.setData({
        stockOuts,
        warehouses,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  onPullDownRefresh() {
    this.loadData()
    wx.stopPullDownRefresh()
  },

  searchInput(e) {
    this.setData({ searchText: e.detail.value })
  },

  getWarehouseName(id) {
    const warehouse = this.data.warehouses.find(w => w.id === id)
    return warehouse ? warehouse.warehouseName : '未知仓库'
  },

  getStatusText(status) {
    const map = { 'PENDING': '待审核', 'APPROVED': '已审核', 'COMPLETED': '已完成' }
    return map[status] || status
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/stockout/detail?id=' + id
    })
  },

  addStockOut() {
    wx.showToast({ title: '新增出库单功能开发中', icon: 'none' })
  }
})
const { stockInApi, warehouseApi, materialApi } = require('../../utils/api.js')

Page({
  data: {
    stockIn: {},
    warehouse: {},
    materials: [],
    loading: false
  },

  onLoad(options) {
    this.loadData(options.id)
  },

  loadData(id) {
    this.setData({ loading: true })
    Promise.all([
      stockInApi.get(id).catch(() => {}),
      warehouseApi.list().catch(() => []),
      materialApi.list().catch(() => [])
    ]).then(([stockIn, warehouses, materials]) => {
      const warehouse = warehouses.find(w => w.id === stockIn.warehouseId) || {}
      this.setData({
        stockIn,
        warehouse,
        materials,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  getMaterialName(id) {
    const material = this.data.materials.find(m => m.id === id)
    return material ? material.materialName : '未知物料'
  },

  getStatusText(status) {
    const map = { 'PENDING': '待审核', 'APPROVED': '已审核', 'COMPLETED': '已完成' }
    return map[status] || status
  },

  goBack() {
    wx.navigateBack()
  }
})
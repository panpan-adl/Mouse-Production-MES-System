const { inventoryApi, materialApi, warehouseApi } = require('../../utils/api.js')

Page({
  data: {
    inventory: {},
    material: {},
    warehouse: {},
    loading: false
  },

  onLoad(options) {
    this.loadData(options.id)
  },

  loadData(id) {
    this.setData({ loading: true })
    Promise.all([
      inventoryApi.get(id).catch(() => {}),
      materialApi.list().catch(() => []),
      warehouseApi.list().catch(() => [])
    ]).then(([inventory, materials, warehouses]) => {
      const material = materials.find(m => m.id === inventory.materialId) || {}
      const warehouse = warehouses.find(w => w.id === inventory.warehouseId) || {}
      this.setData({
        inventory,
        material,
        warehouse,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
const { maintenanceApi, equipmentApi } = require('../../utils/api.js')

Page({
  data: {
    maintenance: {},
    equipment: {},
    loading: false
  },

  onLoad(options) {
    this.loadData(options.id)
  },

  loadData(id) {
    this.setData({ loading: true })
    Promise.all([
      maintenanceApi.get(id).catch(() => {}),
      equipmentApi.list().catch(() => [])
    ]).then(([maintenance, equipments]) => {
      const equipment = equipments.find(e => e.id === maintenance.equipmentId) || {}
      this.setData({
        maintenance,
        equipment,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  getStatusText(status) {
    const map = { 'PENDING': '待处理', 'IN_PROGRESS': '处理中', 'COMPLETED': '已完成' }
    return map[status] || status
  },

  goBack() {
    wx.navigateBack()
  }
})
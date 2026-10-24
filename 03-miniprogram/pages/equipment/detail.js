const { equipmentApi } = require('../../utils/api.js')

Page({
  data: {
    equipment: {},
    loading: false
  },

  onLoad(options) {
    this.loadData(options.id)
  },

  loadData(id) {
    this.setData({ loading: true })
    equipmentApi.get(id).then(equipment => {
      this.setData({
        equipment,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  getStatusText(status) {
    const map = { '0': '停用', '1': '正常', '2': '维修中' }
    return map[status] || status
  },

  goBack() {
    wx.navigateBack()
  }
})
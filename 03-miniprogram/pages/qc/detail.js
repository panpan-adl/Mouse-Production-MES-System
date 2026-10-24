const { qcInspectionApi } = require('../../utils/api.js')

Page({
  data: {
    inspection: {},
    loading: false
  },

  onLoad(options) {
    this.loadData(options.id)
  },

  loadData(id) {
    this.setData({ loading: true })
    qcInspectionApi.get(id).then(inspection => {
      this.setData({
        inspection,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  getTypeText(type) {
    const map = { 'INCOMING': '来料质检', 'PRODUCTION': '生产质检', 'FINISHED': '成品质检' }
    return map[type] || type
  },

  getResultText(result) {
    const map = { 'QUALIFIED': '合格', 'UNQUALIFIED': '不合格', 'CONCESSION_ACCEPT': '特采', 'REWORK': '返工', 'SCRAP': '报废' }
    return map[result] || result
  },

  goBack() {
    wx.navigateBack()
  }
})
const { productionPlanApi } = require('../../utils/api.js')

Page({
  data: {
    plan: null,
    progress: 0,
    loading: true
  },

  onLoad(options) {
    const id = options.id
    if (id) {
      this.loadPlan(id)
    }
  },

  loadPlan(id) {
    this.setData({ loading: true })
    productionPlanApi.get(id).then(data => {
      const progress = data.planQuantity && data.planQuantity > 0 
        ? Math.round(((data.completedQuantity || 0) / data.planQuantity) * 100) 
        : 0
      this.setData({
        plan: data,
        progress: progress,
        loading: false
      })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
  },

  goBack() {
    wx.navigateBack()
  },

  getStatusText(status) {
    const statusMap = {
      '0': '计划中',
      '1': '进行中',
      '2': '已完成'
    }
    return statusMap[String(status)] || status
  },

  getStatusColor(status) {
    const colorMap = {
      '0': '#9c27b0',
      '1': '#ff9800',
      '2': '#4caf50'
    }
    return colorMap[String(status)] || '#999'
  },

  getPriorityText(priority) {
    if (priority === 1) return '高'
    if (priority === 2) return '中'
    return '低'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 19)
  }
})
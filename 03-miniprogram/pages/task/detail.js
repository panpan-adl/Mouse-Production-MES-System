const { productionTaskApi, productionReportApi } = require('../../utils/api.js')

Page({
  data: {
    task: null,
    reports: [],
    loading: true
  },

  onLoad(options) {
    const id = options.id
    if (id) {
      this.loadTask(id)
      this.loadReports(id)
    }
  },

  loadTask(id) {
    this.setData({ loading: true })
    productionTaskApi.get(id).then(data => {
      this.setData({
        task: data,
        loading: false
      })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
  },

  loadReports(taskId) {
    productionReportApi.getByTaskId(taskId).then(data => {
      this.setData({ reports: data })
    }).catch(() => {
      this.setData({ reports: [] })
    })
  },

  goToReport(e) {
    const taskId = this.data.task.id
    wx.navigateTo({
      url: '/pages/report/submit?taskId=' + taskId
    })
  },

  goBack() {
    wx.navigateBack()
  },

  getStatusText(status) {
    const statusMap = {
      'PENDING': '待处理',
      'IN_PROGRESS': '进行中',
      'COMPLETED': '已完成'
    }
    return statusMap[status] || status
  },

  getStatusColor(status) {
    const colorMap = {
      'PENDING': '#ff9800',
      'IN_PROGRESS': '#2196f3',
      'COMPLETED': '#4caf50'
    }
    return colorMap[status] || '#999'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 19)
  }
})

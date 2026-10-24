const { productionReportApi } = require('../../utils/api.js')

Page({
  data: {
    reports: [],
    tasks: {},
    loading: true
  },

  onLoad() {
    this.loadReports()
    this.loadTasks()
  },

  onShow() {
    this.loadReports()
  },

  loadReports() {
    this.setData({ loading: true })
    productionReportApi.list().then(function(data) {
      this.setData({
        reports: data,
        loading: false
      })
    }.bind(this)).catch(function() {
      this.setData({ loading: false })
    }.bind(this))
  },

  loadTasks() {
    this.setData({ tasks: {} })
  },

  getTaskNo(taskId) {
    var task = this.data.tasks[taskId]
    return task ? task.taskNo : '-'
  },

  getTaskProductName(taskId) {
    var task = this.data.tasks[taskId]
    return task ? (task.productName || '-') : '-'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 16)
  },

  goToSubmit() {
    wx.navigateTo({
      url: '/pages/report/submit'
    })
  }
})

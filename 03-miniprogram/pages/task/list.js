const { productionTaskApi } = require('../../utils/api.js')

Page({
  data: {
    tasks: [],
    loading: true,
    statusFilter: 'ALL',
    statusOptions: [
      { value: 'ALL', label: '全部' },
      { value: 'PENDING', label: '待处理' },
      { value: 'IN_PROGRESS', label: '进行中' },
      { value: 'COMPLETED', label: '已完成' }
    ]
  },

  onLoad() {
    this.loadTasks()
  },

  onShow() {
    this.loadTasks()
  },

  loadTasks() {
    this.setData({ loading: true })
    productionTaskApi.list().then(data => {
      let filtered = data
      if (this.data.statusFilter !== 'ALL') {
        filtered = data.filter(item => item.status === this.data.statusFilter)
      }
      this.setData({
        tasks: filtered,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/task/detail?id=' + id
    })
  },

  onStatusChange(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.value })
    this.loadTasks()
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

  getStatusBgColor(status) {
    const colorMap = {
      'PENDING': 'rgba(255, 152, 0, 0.1)',
      'IN_PROGRESS': 'rgba(33, 150, 243, 0.1)',
      'COMPLETED': 'rgba(76, 175, 80, 0.1)'
    }
    return colorMap[status] || 'rgba(0, 0, 0, 0.05)'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 16)
  }
})

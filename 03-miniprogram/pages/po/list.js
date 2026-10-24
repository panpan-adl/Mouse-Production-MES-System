const { productionOrderApi } = require('../../utils/api.js')

Page({
  data: {
    orders: [],
    loading: true,
    statusFilter: 'ALL',
    statusOptions: [
      { value: 'ALL', label: '全部' },
      { value: 'PLANNED', label: '计划中' },
      { value: 'IN_PROGRESS', label: '进行中' },
      { value: 'COMPLETED', label: '已完成' }
    ]
  },

  onLoad() {
    this.loadOrders()
  },

  onShow() {
    this.loadOrders()
  },

  loadOrders() {
    this.setData({ loading: true })
    productionOrderApi.list().then(data => {
      let filtered = data
      if (this.data.statusFilter !== 'ALL') {
        filtered = data.filter(item => item.status === this.data.statusFilter)
      }
      this.setData({
        orders: filtered,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/po/detail?id=' + id
    })
  },

  onStatusChange(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.value })
    this.loadOrders()
  },

  getStatusText(status) {
    const statusMap = {
      'PLANNED': '计划中',
      'IN_PROGRESS': '进行中',
      'COMPLETED': '已完成'
    }
    return statusMap[status] || status
  },

  getStatusColor(status) {
    const colorMap = {
      'PLANNED': '#9c27b0',
      'IN_PROGRESS': '#ff9800',
      'COMPLETED': '#4caf50'
    }
    return colorMap[status] || '#999'
  },

  getStatusBgColor(status) {
    const colorMap = {
      'PLANNED': 'rgba(156, 39, 176, 0.1)',
      'IN_PROGRESS': 'rgba(255, 152, 0, 0.1)',
      'COMPLETED': 'rgba(76, 175, 80, 0.1)'
    }
    return colorMap[status] || 'rgba(0, 0, 0, 0.05)'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 16)
  }
})

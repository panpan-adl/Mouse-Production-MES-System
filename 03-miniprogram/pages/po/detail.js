const { productionOrderApi, workOrderApi } = require('../../utils/api.js')

Page({
  data: {
    order: null,
    workOrders: [],
    loading: true
  },

  onLoad(options) {
    const id = options.id
    if (id) {
      this.loadOrder(id)
      this.loadWorkOrders(id)
    }
  },

  loadOrder(id) {
    this.setData({ loading: true })
    productionOrderApi.get(id).then(data => {
      this.setData({
        order: data,
        loading: false
      })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
  },

  loadWorkOrders(orderId) {
    workOrderApi.getByProductionOrderId(orderId).then(data => {
      this.setData({ workOrders: data })
    }).catch(() => {
      this.setData({ workOrders: [] })
    })
  },

  goToWorkOrderDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/workorder/detail?id=' + id
    })
  },

  goBack() {
    wx.navigateBack()
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

  getWorkOrderStatusText(status) {
    const statusMap = {
      'PENDING': '待分配',
      'ASSIGNED': '已分配',
      'IN_PROGRESS': '进行中',
      'COMPLETED': '已完成'
    }
    return statusMap[status] || status
  },

  getWorkOrderStatusColor(status) {
    const colorMap = {
      'PENDING': '#ff9800',
      'ASSIGNED': '#2196f3',
      'IN_PROGRESS': '#9c27b0',
      'COMPLETED': '#4caf50'
    }
    return colorMap[status] || '#999'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 19)
  }
})

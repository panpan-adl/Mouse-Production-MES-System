const { workOrderApi } = require('../../utils/api.js')

Page({
  data: {
    workOrder: null,
    loading: true
  },

  onLoad(options) {
    const id = options.id
    if (id) {
      this.loadWorkOrder(id)
    }
  },

  loadWorkOrder(id) {
    this.setData({ loading: true })
    workOrderApi.get(id).then(data => {
      this.setData({
        workOrder: data,
        loading: false
      })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
  },

  assignWorkOrder() {
    if (this.data.workOrder.status !== 'PLANNED') {
      wx.showToast({ title: '当前状态不允许分配', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '分配工单',
      content: '确定要分配该工单吗？',
      success: (res) => {
        if (res.confirm) {
          workOrderApi.assign(this.data.workOrder.id, {}).then(() => {
            wx.showToast({ title: '分配成功', icon: 'success' })
            this.loadWorkOrder(this.data.workOrder.id)
          }).catch(() => {
            wx.showToast({ title: '分配失败', icon: 'none' })
          })
        }
      }
    })
  },

  claimWorkOrder() {
    if (this.data.workOrder.status !== 'ASSIGNED') {
      wx.showToast({ title: '当前状态不允许领取', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '领取工单',
      content: '确定要领取该工单吗？',
      success: (res) => {
        if (res.confirm) {
          workOrderApi.claim(this.data.workOrder.id, {}).then(() => {
            wx.showToast({ title: '领取成功', icon: 'success' })
            this.loadWorkOrder(this.data.workOrder.id)
          }).catch(() => {
            wx.showToast({ title: '领取失败', icon: 'none' })
          })
        }
      }
    })
  },

  completeWorkOrder() {
    if (this.data.workOrder.status !== 'IN_PROGRESS') {
      wx.showToast({ title: '当前状态不允许完成', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '完成工单',
      content: '确定要完成该工单吗？',
      success: (res) => {
        if (res.confirm) {
          workOrderApi.complete(this.data.workOrder.id, {}).then(() => {
            wx.showToast({ title: '完成成功', icon: 'success' })
            this.loadWorkOrder(this.data.workOrder.id)
          }).catch(() => {
            wx.showToast({ title: '完成失败', icon: 'none' })
          })
        }
      }
    })
  },

  goBack() {
    wx.navigateBack()
  },

  getStatusText(status) {
    const statusMap = {
      'PLANNED': '待分配',
      'ASSIGNED': '已分配',
      'IN_PROGRESS': '进行中',
      'COMPLETED': '已完成'
    }
    return statusMap[status] || status
  },

  getStatusColor(status) {
    const colorMap = {
      'PLANNED': '#ff9800',
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

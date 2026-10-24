const { orderApi, productionPlanApi } = require('../../utils/api.js')
const barcodeUtil = require('../../utils/barcode.js')

Page({
  data: {
    order: null,
    loading: true,
    showBarcodeModal: false
  },

  onLoad(options) {
    const id = options.id
    if (id) {
      this.loadOrder(id)
    }
  },

  loadOrder(id) {
    this.setData({ loading: true })
    orderApi.get(id).then(data => {
      this.setData({
        order: data,
        loading: false
      })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
  },

  confirmOrder() {
    if (this.data.order.orderStatus !== 0) {
      wx.showToast({ title: '当前状态不允许确认', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '确认订单',
      content: '确定要确认该订单吗？确认后将生成生产计划。',
      success: (res) => {
        if (res.confirm) {
          orderApi.confirm(this.data.order.id).then(() => {
            wx.showToast({ title: '确认成功', icon: 'success' })
            this.loadOrder(this.data.order.id)
          }).catch(() => {
            wx.showToast({ title: '确认失败', icon: 'none' })
          })
        }
      }
    })
  },

  cancelOrder() {
    if (this.data.order.orderStatus !== 0) {
      wx.showToast({ title: '当前状态不允许取消', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '取消订单',
      content: '确定要取消该订单吗？',
      success: (res) => {
        if (res.confirm) {
          orderApi.cancel(this.data.order.id).then(() => {
            wx.showToast({ title: '取消成功', icon: 'success' })
            this.loadOrder(this.data.order.id)
          }).catch(() => {
            wx.showToast({ title: '取消失败', icon: 'none' })
          })
        }
      }
    })
  },

  generatePlan() {
    if (this.data.order.orderStatus !== 1) {
      wx.showToast({ title: '请先确认订单', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '生成生产计划',
      content: '确定要基于此订单生成生产计划吗？',
      success: (res) => {
        if (res.confirm) {
          productionPlanApi.createFromOrder(this.data.order.id).then(() => {
            wx.showToast({ title: '生成成功', icon: 'success' })
            wx.navigateTo({ url: '/pages/plan/list' })
          }).catch(() => {
            wx.showToast({ title: '生成失败', icon: 'none' })
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
      '0': '待确认',
      '1': '已确认',
      '2': '已取消',
      '3': '已完成'
    }
    return statusMap[String(status)] || status
  },

  getStatusColor(status) {
    const colorMap = {
      '0': '#ff9800',
      '1': '#2196f3',
      '2': '#f44336',
      '3': '#4caf50'
    }
    return colorMap[String(status)] || '#999'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 19)
  },

  generateBarcode() {
    const orderNum = this.data.order.orderNo.replace('ORD-', '')
    this.setData({ showBarcodeModal: true })
    setTimeout(() => {
      const ctx = wx.createCanvasContext('barcodeCanvas')
      ctx.setFillStyle('#ffffff')
      ctx.fillRect(0, 0, 600, 240)
      barcodeUtil.drawBarcode(ctx, orderNum, {
        width: 4,
        height: 120,
        x: 40,
        y: 40
      })
      ctx.draw()
    }, 200)
  },

  closeBarcodeModal() {
    this.setData({ showBarcodeModal: false })
  },

  saveBarcode() {
    wx.canvasToTempFilePath({
      canvasId: 'barcodeCanvas',
      success: (res) => {
        wx.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success: () => {
            wx.showToast({ title: '保存成功', icon: 'success' })
            this.closeBarcodeModal()
          },
          fail: () => {
            wx.showToast({ title: '保存失败', icon: 'none' })
          }
        })
      },
      fail: () => {
        wx.showToast({ title: '生成图片失败', icon: 'none' })
      }
    })
  },

  noop() {}
})

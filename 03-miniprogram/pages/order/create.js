const { orderApi } = require('../../utils/api.js')
const barcodeUtil = require('../../utils/barcode.js')

Page({
  data: {
    form: {
      orderNo: '',
      customerName: '',
      customerPhone: '',
      productName: '',
      productModel: '',
      quantity: '',
      unitPrice: '',
      totalAmount: '',
      orderDate: '',
      deliveryDate: '',
      priority: '1',
      remark: ''
    },
    submitting: false,
    showBarcodeModal: false,
    priorityOptions: [
      { value: '1', label: '高' },
      { value: '2', label: '中' },
      { value: '3', label: '低' }
    ]
  },

  onLoad(options) {
    const today = new Date()
    const todayStr = today.toISOString().split('T')[0]
    const deliveryDate = new Date(today.getTime() + 10 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
    
    let orderNo = ''
    if (options && options.orderNo) {
      orderNo = 'ORD-' + options.orderNo
      orderApi.getByOrderNo(orderNo).then(order => {
        if (order) {
          this.setData({
            'form.customerName': order.customerName || '',
            'form.customerPhone': order.customerPhone || '',
            'form.productName': order.productName || '',
            'form.productModel': order.productModel || '',
            'form.quantity': order.quantity || '',
            'form.unitPrice': order.unitPrice || '',
            'form.totalAmount': order.totalAmount || '',
            'form.orderDate': order.orderDate ? order.orderDate.split('T')[0] : todayStr,
            'form.deliveryDate': order.deliveryDate ? order.deliveryDate.split('T')[0] : deliveryDate,
            'form.priority': String(order.priority || '1'),
            'form.remark': order.remark || ''
          })
        }
      }).catch(() => {})
    } else {
      const num = barcodeUtil.generateOrderNo()
      orderNo = 'ORD-' + num
    }
    this.setData({
      'form.orderNo': orderNo,
      'form.orderDate': todayStr,
      'form.deliveryDate': deliveryDate
    })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    
    this.setData({
      ['form.' + field]: value
    })
    
    if (field === 'quantity' || field === 'unitPrice') {
      this.calculateTotalAmount()
    }
  },

  calculateTotalAmount() {
    const { quantity, unitPrice } = this.data.form
    if (quantity && unitPrice) {
      const total = parseFloat(quantity) * parseFloat(unitPrice)
      this.setData({
        'form.totalAmount': total.toFixed(2)
      })
    }
  },

  onPriorityChange(e) {
    this.setData({
      'form.priority': e.currentTarget.dataset.value
    })
  },

  onSubmit() {
    const { form } = this.data
    
    if (!form.customerName) {
      wx.showToast({ title: '请输入客户名称', icon: 'none' })
      return
    }
    if (!form.productName) {
      wx.showToast({ title: '请输入产品名称', icon: 'none' })
      return
    }
    if (!form.quantity) {
      wx.showToast({ title: '请输入订单数量', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    const submitData = {
      orderNo: form.orderNo,
      customerName: form.customerName,
      customerPhone: form.customerPhone,
      productName: form.productName,
      productModel: form.productModel,
      quantity: parseInt(form.quantity) || 0,
      unitPrice: parseFloat(form.unitPrice) || 0,
      totalAmount: parseFloat(form.totalAmount) || 0,
      orderDate: form.orderDate,
      deliveryDate: form.deliveryDate,
      priority: parseInt(form.priority) || 1,
      remark: form.remark,
      orderStatus: 0
    }

    orderApi.create(submitData).then(() => {
      wx.showToast({ title: '创建成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }).catch(() => {
      wx.showToast({ title: '创建失败', icon: 'none' })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  },

  goBack() {
    wx.navigateBack()
  },

  generateBarcode() {
    const orderNum = this.data.form.orderNo.replace('ORD-', '')
    console.log('=== 生成条形码 ===')
    console.log('内容:', orderNum)
    console.log('长度:', orderNum.length)
    
    const codes = barcodeUtil.encode(orderNum)
    console.log('编码:', codes)
    
    this.setData({ showBarcodeModal: true })
    setTimeout(() => {
      const ctx = wx.createCanvasContext('barcodeCanvas')
      barcodeUtil.drawBarcode(ctx, orderNum, {
        width: 4,
        height: 180,
        x: 60,
        y: 50,
        canvasWidth: 800,
        canvasHeight: 300
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

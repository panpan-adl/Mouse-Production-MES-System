const { orderApi } = require('../../utils/api.js')

Page({
  data: {
    orders: [],
    allOrders: [],
    loading: true,
    statusFilter: 'ALL',
    searchKeyword: '',
    statusOptions: [
      { value: 'ALL', label: '全部' },
      { value: '0', label: '待确认' },
      { value: '1', label: '已确认' },
      { value: '2', label: '已取消' },
      { value: '3', label: '已完成' }
    ],
    scanTestOptions: [
      { label: '测试: 订单已存在 (20260715001)', value: '20260715001' },
      { label: '测试: 订单已存在 (20260715002)', value: '20260715002' },
      { label: '测试: 订单已取消 (20260715003)', value: '20260715003' },
      { label: '测试: 订单不存在 (20260715004)', value: '20260715004' },
      { label: '测试: 订单不存在 (20260715005)', value: '20260715005' }
    ],
    showScanTest: false
  },

  onLoad() {
    this.loadOrders()
  },

  onShow() {
    this.loadOrders()
  },

  loadOrders() {
    this.setData({ loading: true })
    orderApi.list().then(data => {
      this.setData({
        allOrders: data,
        loading: false
      })
      this.filterOrders()
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  filterOrders() {
    let filtered = this.data.allOrders
    
    if (this.data.statusFilter !== 'ALL') {
      filtered = filtered.filter(item => String(item.orderStatus) === this.data.statusFilter)
    }
    
    if (this.data.searchKeyword) {
      const keyword = this.data.searchKeyword.toLowerCase()
      filtered = filtered.filter(item => {
        const orderNo = (item.orderNo || '').toLowerCase()
        const customerName = (item.customerName || '').toLowerCase()
        const productName = (item.productName || '').toLowerCase()
        return orderNo.includes(keyword) || customerName.includes(keyword) || productName.includes(keyword)
      })
    }
    
    const processedOrders = filtered.map(item => ({
      ...item,
      displayTotalAmount: (item.totalAmount || 0).toFixed(2),
      displayUnitPrice: (item.unitPrice || 0).toFixed(2),
      displayQuantity: item.quantity || 0,
      displayCustomerName: item.customerName || '未知客户',
      displayProductName: item.productName || '未知产品',
      displayStatusText: this.getStatusText(item.orderStatus),
      displayStatusColor: this.getStatusColor(item.orderStatus),
      displayStatusBgColor: this.getStatusBgColor(item.orderStatus),
      displayCreateTime: this.formatDate(item.createTime),
      isPriority: item.priority === 1,
      canConfirm: item.orderStatus === 0,
      canCancel: item.orderStatus === 0,
      canDelete: item.orderStatus !== 2 && item.orderStatus !== 3,
      progressStep1: true,
      progressStep2: item.orderStatus >= 1,
      progressStep3: item.orderStatus >= 3
    }))
    
    this.setData({ orders: processedOrders })
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
    this.filterOrders()
  },

  onSearch() {
    this.filterOrders()
  },

  clearSearch() {
    this.setData({ searchKeyword: '' })
    this.filterOrders()
  },

  focusSearch() {
    this.selectComponent('.search-input')?.focus()
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/order/detail?id=' + id
    })
  },

  goToCreate() {
    wx.navigateTo({
      url: '/pages/order/create'
    })
  },

  scanOrder() {
    console.log('=== 步骤1: 开始扫码 ===')
    wx.showLoading({ title: '正在打开相机...', mask: true })
    
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        wx.hideLoading()
        console.log('=== 步骤2: 扫码成功 ===')
        console.log('原始结果:', res.result)
        console.log('扫描类型:', res.scanType)
        console.log('字符长度:', res.result.length)
        
        this.processScanResult(res.result)
      },
      fail: (err) => {
        wx.hideLoading()
        console.error('=== 步骤2: 扫码失败 ===')
        console.error('错误信息:', err)
        
        wx.showModal({
          title: '扫码失败',
          content: '无法调用摄像头，是否使用模拟测试？',
          confirmText: '模拟测试',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.setData({ showScanTest: true })
            }
          }
        })
      }
    })
  },

  showScanTestMenu() {
    this.setData({ showScanTest: true })
  },

  closeScanTestMenu() {
    this.setData({ showScanTest: false })
  },

  simulateScan(e) {
    const testValue = e.currentTarget.dataset.value
    console.log('=== 模拟扫码: ' + testValue + ' ===')
    this.setData({ showScanTest: false })
    this.processScanResult(testValue)
  },

  processScanResult(rawResult) {
    console.log('=== 步骤3: 处理扫码结果 ===')
    
    const trimmed = rawResult.trim()
    console.log('trim后:', trimmed)
    console.log('trim后长度:', trimmed.length)
    console.log('字符码:', Array.from(trimmed).map(c => c.charCodeAt(0)))
    
    if (!/^\d{11}$/.test(trimmed)) {
      console.error('=== 步骤4: 验证失败 ===')
      console.error('期望: 11位纯数字')
      console.error('实际:', trimmed, '类型:', typeof trimmed)
      
      wx.showModal({
        title: '条码格式错误',
        content: '扫码结果: ' + trimmed + '\n\n期望: 11位纯数字\n实际: ' + trimmed.length + '位',
        showCancel: false
      })
      return
    }
    
    console.log('=== 步骤4: 格式验证通过 ===')
    console.log('订单编号:', trimmed)
    
    const fullOrderNo = 'ORD-' + trimmed
    console.log('=== 步骤5: 拼接完整订单号 ===')
    console.log('完整订单号:', fullOrderNo)
    
    wx.showLoading({ title: '查询订单中...', mask: true })
    
    orderApi.getByOrderNo(fullOrderNo).then(order => {
      wx.hideLoading()
      console.log('=== 步骤6: 查询结果 ===')
      console.log('查询返回:', order)
      
      if (order && order.id) {
        console.log('=== 步骤7: 订单已存在 ===')
        console.log('订单ID:', order.id)
        
        wx.showModal({
          title: '订单已存在',
          content: '订单号: ' + order.orderNo + '\n客户: ' + (order.customerName || '未知') + '\n产品: ' + (order.productName || '未知'),
          confirmText: '查看详情',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              wx.navigateTo({
                url: '/pages/order/detail?id=' + order.id
              })
            }
          }
        })
      } else {
        console.log('=== 步骤7: 订单不存在 ===')
        console.log('跳转创建页面:', '/pages/order/create?orderNo=' + trimmed)
        
        wx.showModal({
          title: '订单未找到',
          content: '订单号: ' + fullOrderNo + '\n\n该订单不存在，是否创建新订单？',
          confirmText: '创建订单',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              wx.navigateTo({
                url: '/pages/order/create?orderNo=' + trimmed
              })
            }
          }
        })
      }
    })
  },

  confirmOrder(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认订单',
      content: '确定要确认该订单吗？',
      success: (res) => {
        if (res.confirm) {
          orderApi.confirm(id).then(() => {
            wx.showToast({ title: '确认成功', icon: 'success' })
            this.loadOrders()
          }).catch(() => {
            wx.showToast({ title: '确认失败', icon: 'none' })
          })
        }
      }
    })
  },

  cancelOrder(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消订单',
      content: '确定要取消该订单吗？',
      success: (res) => {
        if (res.confirm) {
          orderApi.cancel(id).then(() => {
            wx.showToast({ title: '取消成功', icon: 'success' })
            this.loadOrders()
          }).catch(() => {
            wx.showToast({ title: '取消失败', icon: 'none' })
          })
        }
      }
    })
  },

  deleteOrder(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除订单',
      content: '确定要删除该订单吗？',
      success: (res) => {
        if (res.confirm) {
          orderApi.delete(id).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' })
            this.loadOrders()
          }).catch(() => {
            wx.showToast({ title: '删除失败', icon: 'none' })
          })
        }
      }
    })
  },

  onStatusChange(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.value })
    this.loadOrders()
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

  getStatusBgColor(status) {
    const colorMap = {
      '0': 'rgba(255, 152, 0, 0.1)',
      '1': 'rgba(33, 150, 243, 0.1)',
      '2': 'rgba(244, 67, 54, 0.1)',
      '3': 'rgba(76, 175, 80, 0.1)'
    }
    return colorMap[String(status)] || 'rgba(0, 0, 0, 0.05)'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 16)
  },

  getStatusCount(status) {
    if (status === 'ALL') return this.data.allOrders.length
    return this.data.allOrders.filter(item => String(item.orderStatus) === status).length
  }
})

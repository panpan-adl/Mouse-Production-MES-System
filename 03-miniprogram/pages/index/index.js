const app = getApp()
const { dashboardApi, orderApi } = require('../../utils/api.js')

Page({
  data: {
    isLogin: false,
    userInfo: null,
    summary: {},
    orderSummary: {},
    productionSummary: {},
    taskSummary: {},
    currentDate: '',
    quickLinks: [
      { name: '创建订单', icon: '📝', path: '/pages/order/create', bgColor: '#e6f7ff' },
      { name: '生产计划', icon: '📋', path: '/pages/plan/list', bgColor: '#f6ffed' },
      { name: '工单管理', icon: '🔧', path: '/pages/workorder/list', bgColor: '#fff7e6' },
      { name: '报工查看', icon: '📊', path: '/pages/report/list', bgColor: '#f9f0ff' },
      { name: '库存管理', icon: '🏭', path: '/pages/index/index', bgColor: '#fff1f0' },
      { name: '设备管理', icon: '⚙️', path: '/pages/index/index', bgColor: '#e6fffb' },
      { name: '质检管理', icon: '✅', path: '/pages/index/index', bgColor: '#fffbe6' },
      { name: '报表中心', icon: '📈', path: '/pages/report/list', bgColor: '#f0f5ff' }
    ],
    recentOrders: [],
    loading: true,
    completionRate: 0,
    orderCount: 0,
    planCount: 0,
    taskCount: 0,
    workOrderCount: 0,
    orderTrend: 12,
    planTrend: 8,
    taskTrend: -3,
    woTrend: 15,
    todoList: []
  },

  onLoad() {
    const now = new Date()
    const dateStr = now.getFullYear() + '年' + (now.getMonth() + 1) + '月' + now.getDate() + '日'
    this.setData({ currentDate: dateStr })
    this.checkLoginStatus()
  },

  onShow() {
    this.checkLoginStatus()
    if (this.data.isLogin) {
      this.loadDashboardData()
    }
  },

  checkLoginStatus() {
    const isLogin = app.checkLogin()
    const userInfo = wx.getStorageSync('userInfo')
    
    this.setData({
      isLogin: isLogin,
      userInfo: userInfo || null
    })

    if (isLogin) {
      this.loadDashboardData()
    }
  },

  loadDashboardData() {
    this.setData({ loading: true })
    
    Promise.all([
      dashboardApi.getSummary().catch(function() { return {} }),
      dashboardApi.getRecentOrders().catch(function() { return [] })
    ]).then(function(results) {
      var summary = results[0]
      var orders = results[1]
      
      const orderCount = this.getOrderCount()
      const planCount = this.getPlanCount()
      const taskCount = this.getTaskCount()
      const workOrderCount = this.getWorkOrderCount()
      const completionRate = this.getCompletionRate()
      
      const processedOrders = orders.slice(0, 5).map(item => ({
        ...item,
        displayNo: item.orderNo || item.poNo || '',
        displayTime: this.formatOrderTime(item),
        displayStatusText: this.getStatusText(item.status || item.orderStatus),
        displayStatusColor: this.getStatusColor(item.status || item.orderStatus),
        displayStatusBgColor: this.getStatusBgColor(item.status || item.orderStatus)
      }))
      
      this.setData({
        summary: summary,
        recentOrders: processedOrders,
        loading: false,
        orderCount: orderCount,
        planCount: planCount,
        taskCount: taskCount,
        workOrderCount: workOrderCount,
        completionRate: completionRate,
        todoList: this.generateTodoList()
      })

      setTimeout(() => {
        this.drawProgressRing(completionRate)
      }, 300)
    }.bind(this)).catch(function() {
      this.setData({ loading: false })
    }.bind(this))
  },

  drawProgressRing(rate) {
    const ctx = wx.createCanvasContext('progressRing')
    const dpr = wx.getSystemInfoSync().pixelRatio
    const size = 160 * dpr
    const centerX = size / 2
    const centerY = size / 2
    const radius = (size - 32) / 2
    const lineWidth = 12 * dpr
    
    ctx.setLineWidth(lineWidth)
    ctx.setStrokeStyle('#f0f0f0')
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
    ctx.stroke()
    
    const startAngle = -Math.PI / 2
    const endAngle = startAngle + (2 * Math.PI * rate / 100)
    
    const gradient = ctx.createLinearGradient(0, 0, size, size)
    gradient.addColorStop(0, '#1890ff')
    gradient.addColorStop(1, '#096dd9')
    
    ctx.setLineWidth(lineWidth)
    ctx.setStrokeStyle(gradient)
    ctx.setLineCap('round')
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, startAngle, endAngle)
    ctx.stroke()
    
    ctx.draw()
  },

  generateTodoList() {
    return [
      { id: 1, title: '确认3个待审核订单', meta: '订单管理', priority: 'high', completed: false },
      { id: 2, title: '查看生产计划进度', meta: '计划管理', priority: 'normal', completed: false },
      { id: 3, title: '分配今日工单', meta: '工单管理', priority: 'high', completed: false },
      { id: 4, title: '提交生产日报', meta: '报表中心', priority: 'normal', completed: true }
    ]
  },

  toggleTodo(e) {
    const id = e.currentTarget.dataset.id
    const todoList = this.data.todoList.map(item => {
      if (item.id === id) {
        return { ...item, completed: !item.completed }
      }
      return item
    })
    this.setData({ todoList })
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  },

  goToPage(e) {
    var path = e.currentTarget.dataset.path
    var tabBarPages = ['/pages/order/list', '/pages/plan/list', '/pages/workorder/list', '/pages/index/index', '/pages/profile/index']
    if (tabBarPages.includes(path)) {
      wx.switchTab({
        url: path
      })
    } else {
      wx.navigateTo({
        url: path
      })
    }
  },

  goToOrderDetail(e) {
    var id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/order/detail?id=' + id
    })
  },

  getStatusText(status) {
    var statusMap = {
      'PENDING': '待确认',
      'CONFIRMED': '已确认',
      'CANCELLED': '已取消',
      'COMPLETED': '已完成',
      'PLANNED': '计划中',
      'IN_PROGRESS': '进行中',
      'ASSIGNED': '已分配',
      '0': '待确认',
      '1': '已确认',
      '2': '已取消',
      '3': '已完成'
    }
    return statusMap[status] || status
  },

  getStatusColor(status) {
    var colorMap = {
      'PENDING': '#ff9800',
      'CONFIRMED': '#2196f3',
      'CANCELLED': '#f44336',
      'COMPLETED': '#4caf50',
      'PLANNED': '#9c27b0',
      'IN_PROGRESS': '#ff9800',
      'ASSIGNED': '#2196f3',
      '0': '#ff9800',
      '1': '#2196f3',
      '2': '#f44336',
      '3': '#4caf50'
    }
    return colorMap[status] || '#999'
  },

  getStatusBgColor(status) {
    var colorMap = {
      'PENDING': 'rgba(255, 152, 0, 0.1)',
      'CONFIRMED': 'rgba(33, 150, 243, 0.1)',
      'CANCELLED': 'rgba(244, 67, 54, 0.1)',
      'COMPLETED': 'rgba(76, 175, 80, 0.1)',
      'PLANNED': 'rgba(156, 39, 176, 0.1)',
      'IN_PROGRESS': 'rgba(255, 152, 0, 0.1)',
      'ASSIGNED': 'rgba(33, 150, 243, 0.1)',
      '0': 'rgba(255, 152, 0, 0.1)',
      '1': 'rgba(33, 150, 243, 0.1)',
      '2': 'rgba(244, 67, 54, 0.1)',
      '3': 'rgba(76, 175, 80, 0.1)'
    }
    return colorMap[status] || 'rgba(0, 0, 0, 0.05)'
  },

  formatOrderTime(order) {
    var time = order.createTime || order.createdAt || ''
    return time.substring(0, 16)
  },

  getOrderCount() {
    var orders = this.data.summary.orders || {}
    return (orders.pending || 0) + (orders.confirmed || 0) + (orders.cancelled || 0) + (orders.completed || 0) || 0
  },

  getPlanCount() {
    var plans = this.data.summary.plans || {}
    return plans.pending + plans.inProgress + plans.completed || 0
  },

  getTaskCount() {
    return this.data.summary.totalTasks || 0
  },

  getWorkOrderCount() {
    var workOrders = this.data.summary.workOrders || {}
    return workOrders.planned + workOrders.assigned + workOrders.inProgress + workOrders.completed || 0
  },

  getCompletionRate() {
    var orders = this.data.summary.orders || {}
    const total = (orders.pending || 0) + (orders.confirmed || 0) + (orders.cancelled || 0) + (orders.completed || 0) || 1
    const completed = orders.completed || 0
    return Math.round((completed / total) * 100)
  }
})

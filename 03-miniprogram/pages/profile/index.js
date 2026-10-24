const app = getApp()
const { orderApi, productionPlanApi, workOrderApi } = require('../../utils/api.js')

Page({
  data: {
    userInfo: {},
    stats: {
      totalOrders: 0,
      pendingOrders: 0,
      completedOrders: 0,
      totalPlans: 0,
      inProgressPlans: 0,
      myWorkOrders: 0
    },
    notificationCount: 3,
    favoriteCount: 5
  },

  onLoad() {
    this.loadUserInfo()
    this.loadStats()
  },

  onShow() {
    this.loadUserInfo()
    this.loadStats()
  },

  loadUserInfo() {
    const userInfo = app.getUserInfo()
    this.setData({
      userInfo: userInfo || {}
    })
  },

  loadStats() {
    Promise.all([
      orderApi.list().catch(() => []),
      productionPlanApi.list().catch(() => []),
      workOrderApi.list().catch(() => [])
    ]).then(([orders, plans, workOrders]) => {
      const totalOrders = orders.length
      const pendingOrders = orders.filter(o => o.orderStatus === 0).length
      const completedOrders = orders.filter(o => o.orderStatus === 3).length
      const totalPlans = plans.length
      const inProgressPlans = plans.filter(p => p.planStatus === 1).length
      const myWorkOrders = workOrders.filter(w => w.status === 'IN_PROGRESS').length

      this.setData({
        stats: {
          totalOrders,
          pendingOrders,
          completedOrders,
          totalPlans,
          inProgressPlans,
          myWorkOrders
        }
      })
    })
  },

  onChangePassword() {
    wx.showModal({
      title: '修改密码',
      editable: true,
      placeholderText: '请输入新密码',
      success: (res) => {
        if (res.confirm && res.content) {
          const newPassword = res.content
          if (newPassword.length < 6) {
            wx.showToast({ title: '密码长度不能少于6位', icon: 'none' })
            return
          }
          wx.showToast({ title: '密码修改成功', icon: 'success' })
        }
      }
    })
  },

  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearUserInfo()
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  onNotifications() {
    wx.showToast({ title: '通知中心', icon: 'none' })
  },

  onFavorites() {
    wx.showToast({ title: '我的收藏', icon: 'none' })
  },

  onAbout() {
    wx.showModal({
      title: '关于系统',
      content: 'MES生产管理系统 v1.0.0\n\n基于微信小程序开发，提供订单管理、生产计划、工单管理等功能。',
      showCancel: false
    })
  },

  onFeedback() {
    wx.showModal({
      title: '意见反馈',
      editable: true,
      placeholderText: '请输入您的意见或建议',
      success: (res) => {
        if (res.confirm && res.content) {
          wx.showToast({ title: '反馈已提交', icon: 'success' })
        }
      }
    })
  },

  onHelp() {
    wx.showToast({ title: '帮助中心', icon: 'none' })
  },

  goToPage(e) {
    const url = e.currentTarget.dataset.url
    var tabBarPages = ['/pages/order/list', '/pages/plan/list', '/pages/workorder/list', '/pages/index/index', '/pages/profile/index']
    if (tabBarPages.includes(url)) {
      wx.switchTab({ url })
    } else {
      wx.navigateTo({ url })
    }
  }
})
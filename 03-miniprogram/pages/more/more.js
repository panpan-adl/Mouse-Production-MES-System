const app = getApp()
const { getUserInfo } = require('../../utils/api.js')

Page({
  data: {
    userInfo: null,
    avatarText: '',
    userName: '',
    userRole: '',
    menuList: [
      { name: '订单管理', icon: '📦', path: '/pages/order/list' },
      { name: '生产计划', icon: '📋', path: '/pages/plan/list' },
      { name: '生产任务', icon: '🔧', path: '/pages/task/list' },
      { name: '工单管理', icon: '✅', path: '/pages/workorder/list' },
      { name: '生产订单', icon: '📝', path: '/pages/po/list' },
      { name: '报工记录', icon: '📊', path: '/pages/report/list' }
    ]
  },

  onLoad() {
    this.loadUserInfo()
  },

  onShow() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    var userInfo = app.getUserInfo()
    if (userInfo) {
      this.updateUserDisplay(userInfo)
    } else {
      this.setData({ 
        userInfo: null, 
        avatarText: '',
        userName: '',
        userRole: ''
      })
    }
  },

  updateUserDisplay(userInfo) {
    var name = userInfo.realName || userInfo.username || '用户'
    var avatar = name.charAt(0)
    var role = userInfo.roleName || '普通用户'
    
    this.setData({ 
      userInfo: userInfo,
      avatarText: avatar,
      userName: name,
      userRole: role
    })
  },

  goToPage(e) {
    var path = e.currentTarget.dataset.path
    wx.switchTab({
      url: path
    }).catch(function() {
      wx.navigateTo({
        url: path
      })
    })
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: function(res) {
        if (res.confirm) {
          app.clearUserInfo()
          wx.showToast({ title: '已退出', icon: 'success' })
          setTimeout(function() {
            wx.navigateTo({
              url: '/pages/login/login'
            })
          }, 1000)
        }
      }
    })
  }
})

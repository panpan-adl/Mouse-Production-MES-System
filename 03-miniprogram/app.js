App({
  onLaunch() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    this.globalData.token = token || null
    this.globalData.userInfo = userInfo || null
  },

  globalData: {
    userInfo: null,
    token: null
  },

  checkLogin() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
      return true
    }
    return false
  },

  setUserInfo(userInfo) {
    this.globalData.userInfo = userInfo
    wx.setStorageSync('userInfo', userInfo)
  },

  setToken(token) {
    this.globalData.token = token
    wx.setStorageSync('token', token)
  },

  clearUserInfo() {
    this.globalData.userInfo = null
    this.globalData.token = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
  },

  getUserInfo() {
    return this.globalData.userInfo || wx.getStorageSync('userInfo')
  },

  getToken() {
    return this.globalData.token || wx.getStorageSync('token')
  }
})

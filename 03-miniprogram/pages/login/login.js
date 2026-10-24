const api = require('../../utils/api.js')

Page({
  data: {
    username: '',
    serverUrl: api.getBaseUrl(),
    password: '',
    loading: false
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value })
  },
  onServerInput(e) { this.setData({serverUrl:e.detail.value}) },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  onLogin() {
    const server=String(this.data.serverUrl||'').trim().replace(/\/$/,'')
    if(!/^https?:\/\/[a-zA-Z0-9.\-\[\]:]+$/.test(server)){wx.showToast({title:'请填写有效服务器地址',icon:'none'});return}
    wx.setStorageSync('mesBaseUrl',server)
    const { username, password } = this.data
    
    if (!username) {
      wx.showToast({
        title: '请输入用户编码',
        icon: 'none'
      })
      return
    }
    
    if (!password) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    api.login(username, password)
      .then(data => {
        const token = data.token
        wx.setStorageSync('token', token)
        
        const app = getApp()
        app.globalData.token = token
        
        wx.setStorageSync('userInfo', data)
        app.globalData.userInfo = data
        
        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1500
        })
        
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index'
          })
        }, 1500)
      })
      .catch(err => {
        console.error('登录失败:', err)
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  }
})

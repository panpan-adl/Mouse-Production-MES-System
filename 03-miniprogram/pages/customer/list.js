const { customerApi } = require('../../utils/api.js')

Page({
  data: {
    customers: [],
    loading: false,
    searchText: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    customerApi.list().then(customers => {
      this.setData({
        customers,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  onPullDownRefresh() {
    this.loadData()
    wx.stopPullDownRefresh()
  },

  searchInput(e) {
    this.setData({ searchText: e.detail.value })
  },

  getStatusText(status) {
    const map = { '0': '禁用', '1': '启用' }
    return map[status] || status
  },

  getStatusColor(status) {
    return status === '1' ? 'green' : 'gray'
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.showToast({ title: '详情功能开发中', icon: 'none' })
  },

  addCustomer() {
    wx.showToast({ title: '新增客户功能开发中', icon: 'none' })
  }
})
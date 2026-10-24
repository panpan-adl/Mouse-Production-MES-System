const { equipmentApi } = require('../../utils/api.js')

Page({
  data: {
    equipments: [],
    loading: false,
    searchText: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    equipmentApi.list().then(equipments => {
      this.setData({
        equipments,
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
    const map = { '0': '停用', '1': '正常', '2': '维修中' }
    return map[status] || status
  },

  getStatusColor(status) {
    const map = { '0': 'gray', '1': 'green', '2': 'orange' }
    return map[status] || 'gray'
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/equipment/detail?id=' + id
    })
  },

  addEquipment() {
    wx.showToast({ title: '新增设备功能开发中', icon: 'none' })
  }
})
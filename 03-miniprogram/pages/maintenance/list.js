const { maintenanceApi, equipmentApi } = require('../../utils/api.js')

Page({
  data: {
    maintenances: [],
    equipments: [],
    loading: false,
    searchText: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    Promise.all([
      maintenanceApi.list().catch(() => []),
      equipmentApi.list().catch(() => [])
    ]).then(([maintenances, equipments]) => {
      this.setData({
        maintenances,
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

  getEquipmentName(id) {
    const equipment = this.data.equipments.find(e => e.id === id)
    return equipment ? equipment.equipmentName : '未知设备'
  },

  getStatusText(status) {
    const map = { 'PENDING': '待处理', 'IN_PROGRESS': '处理中', 'COMPLETED': '已完成' }
    return map[status] || status
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/maintenance/detail?id=' + id
    })
  },

  addMaintenance() {
    wx.showToast({ title: '新增维护单功能开发中', icon: 'none' })
  }
})
const { qcInspectionApi } = require('../../utils/api.js')

Page({
  data: {
    inspections: [],
    loading: false,
    searchText: '',
    selectedType: '',
    selectedResult: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    qcInspectionApi.list().then(inspections => {
      this.setData({
        inspections,
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

  getTypeText(type) {
    const map = { 'INCOMING': '来料质检', 'PRODUCTION': '生产质检', 'FINISHED': '成品质检' }
    return map[type] || type
  },

  getResultText(result) {
    const map = { 'QUALIFIED': '合格', 'UNQUALIFIED': '不合格', 'CONCESSION_ACCEPT': '特采', 'REWORK': '返工', 'SCRAP': '报废' }
    return map[result] || result
  },

  getResultColor(result) {
    const map = { 'QUALIFIED': 'green', 'UNQUALIFIED': 'red', 'CONCESSION_ACCEPT': 'orange', 'REWORK': 'yellow', 'SCRAP': 'gray' }
    return map[result] || 'gray'
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/qc/detail?id=' + id
    })
  },

  addInspection() {
    wx.showToast({ title: '新增质检单功能开发中', icon: 'none' })
  }
})
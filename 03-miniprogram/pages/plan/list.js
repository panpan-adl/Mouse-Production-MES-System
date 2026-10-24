const { productionPlanApi } = require('../../utils/api.js')

Page({
  data: {
    plans: [],
    allPlans: [],
    loading: true,
    statusFilter: 'ALL',
    searchKeyword: '',
    viewMode: 'list',
    statusOptions: [
      { value: 'ALL', label: '全部', count: 0 },
      { value: '0', label: '计划中', count: 0 },
      { value: '1', label: '进行中', count: 0 },
      { value: '2', label: '已完成', count: 0 }
    ]
  },

  onLoad() {
    this.loadPlans()
  },

  onShow() {
    this.loadPlans()
  },

  loadPlans() {
    this.setData({ loading: true })
    productionPlanApi.list().then(data => {
      const allPlans = data
      const statusOptions = this.data.statusOptions.map(item => ({
        ...item,
        count: item.value === 'ALL' ? allPlans.length : 
          allPlans.filter(p => String(p.planStatus) === item.value).length
      }))
      
      this.setData({
        allPlans: allPlans,
        loading: false,
        statusOptions: statusOptions
      })
      this.filterPlans()
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  filterPlans() {
    let filtered = this.data.allPlans
    
    if (this.data.statusFilter !== 'ALL') {
      filtered = filtered.filter(item => String(item.planStatus) === this.data.statusFilter)
    }
    
    if (this.data.searchKeyword) {
      const keyword = this.data.searchKeyword.toLowerCase()
      filtered = filtered.filter(item => {
        const planNo = (item.planNo || '').toLowerCase()
        const productName = (item.productName || '').toLowerCase()
        return planNo.includes(keyword) || productName.includes(keyword)
      })
    }
    
    const processedPlans = filtered.map(item => {
      const progress = this.getProgress(item)
      return {
        ...item,
        displayProductName: item.productName || '未知产品',
        displayCompletedQty: item.completedQuantity || 0,
        displayPlanQty: item.planQuantity || 0,
        displayProgress: progress,
        displayProgressColor: this.getProgressColor(progress),
        displayStatusText: this.getStatusText(item.planStatus),
        displayStatusColor: this.getStatusColor(item.planStatus),
        displayStatusBgColor: this.getStatusBgColor(item.planStatus),
        displayStartDate: this.formatDate(item.startDate),
        displayEndDate: this.formatDate(item.endDate),
        displayUpdateTime: item.updateTime ? this.formatDate(item.updateTime) : '-',
        isPriority: item.priority === 1,
        canDelete: item.planStatus !== 2,
        ganttWidth: progress + '%',
        ganttBgColor: this.getProgressColor(progress)
      }
    })
    
    this.setData({ plans: processedPlans })
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
    this.filterPlans()
  },

  clearSearch() {
    this.setData({ searchKeyword: '' })
    this.filterPlans()
  },

  switchView(e) {
    this.setData({ viewMode: e.currentTarget.dataset.mode })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/plan/detail?id=' + id
    })
  },

  deletePlan(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除计划',
      content: '确定要删除该生产计划吗？',
      success: (res) => {
        if (res.confirm) {
          productionPlanApi.delete(id).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' })
            this.loadPlans()
          }).catch(() => {
            wx.showToast({ title: '删除失败', icon: 'none' })
          })
        }
      }
    })
  },

  onStatusChange(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.value })
    this.loadPlans()
  },

  getStatusText(status) {
    const statusMap = {
      '0': '计划中',
      '1': '进行中',
      '2': '已完成'
    }
    return statusMap[String(status)] || status
  },

  getStatusColor(status) {
    const colorMap = {
      '0': '#9c27b0',
      '1': '#ff9800',
      '2': '#4caf50'
    }
    return colorMap[String(status)] || '#999'
  },

  getStatusBgColor(status) {
    const colorMap = {
      '0': 'rgba(156, 39, 176, 0.1)',
      '1': 'rgba(255, 152, 0, 0.1)',
      '2': 'rgba(76, 175, 80, 0.1)'
    }
    return colorMap[String(status)] || 'rgba(0, 0, 0, 0.05)'
  },

  getPriorityText(priority) {
    if (priority === 1) return '高'
    if (priority === 2) return '中'
    return '低'
  },

  getProgress(plan) {
    if (!plan.planQuantity || plan.planQuantity === 0) return 0
    const completed = plan.completedQuantity || 0
    return Math.round((completed / plan.planQuantity) * 100)
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 16)
  },

  getStatusCount(status) {
    if (status === 'ALL') return this.data.allPlans.length
    return this.data.allPlans.filter(item => String(item.planStatus) === status).length
  },

  getProgressColor(progress) {
    if (progress >= 100) return '#4caf50'
    if (progress >= 50) return '#ff9800'
    return '#9c27b0'
  },

  getGanttStyle(item) {
    const progress = this.getProgress(item)
    return {
      width: progress + '%',
      background: this.getProgressColor(progress)
    }
  }
})

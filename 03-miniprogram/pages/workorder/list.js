const { workOrderApi } = require('../../utils/api.js')

Page({
  data: {
    workOrders: [],
    allWorkOrders: [],
    loading: true,
    statusFilter: 'ALL',
    searchKeyword: '',
    statusOptions: [
      { value: 'ALL', label: '全部', count: 0 },
      { value: 'PLANNED', label: '待分配', count: 0 },
      { value: 'ASSIGNED', label: '已分配', count: 0 },
      { value: 'IN_PROGRESS', label: '进行中', count: 0 },
      { value: 'COMPLETED', label: '已完成', count: 0 }
    ]
  },

  onLoad() {
    this.loadWorkOrders()
  },

  onShow() {
    this.loadWorkOrders()
  },

  loadWorkOrders() {
    this.setData({ loading: true })
    workOrderApi.list().then(data => {
      const allWorkOrders = data
      const statusOptions = this.data.statusOptions.map(item => ({
        ...item,
        count: item.value === 'ALL' ? allWorkOrders.length : 
          allWorkOrders.filter(w => w.status === item.value).length
      }))
      
      this.setData({
        allWorkOrders: allWorkOrders,
        loading: false,
        statusOptions: statusOptions
      })
      this.filterWorkOrders()
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  filterWorkOrders() {
    let filtered = this.data.allWorkOrders
    
    if (this.data.statusFilter !== 'ALL') {
      filtered = filtered.filter(item => item.status === this.data.statusFilter)
    }
    
    if (this.data.searchKeyword) {
      const keyword = this.data.searchKeyword.toLowerCase()
      filtered = filtered.filter(item => {
        const woNo = (item.woNo || '').toLowerCase()
        const productName = (item.productName || '').toLowerCase()
        const processName = (item.processName || '').toLowerCase()
        const assignedUserName = (item.assignedUserName || '').toLowerCase()
        return woNo.includes(keyword) || productName.includes(keyword) || processName.includes(keyword) || assignedUserName.includes(keyword)
      })
    }
    
    const processedOrders = filtered.map(item => {
      const progress = this.getProgress(item)
      return {
        ...item,
        displayProductName: item.productName || '未知产品',
        displayPoNo: item.poNo || '-',
        displayProcessName: item.processName || '',
        displayActualQty: item.actualQuantity || 0,
        displayPlannedQty: item.plannedQuantity || 0,
        displayProgress: progress,
        displayProgressColor: this.getProgressColor(progress),
        displayStatusText: this.getStatusText(item.status),
        displayStatusColor: this.getStatusColor(item.status),
        displayStatusBgColor: this.getStatusBgColor(item.status),
        displayCreatedAt: this.formatDate(item.createdAt),
        hasProgress: progress > 0,
        hasProcess: !!item.processName,
        hasOwner: !!item.assignedUserName
      }
    })
    
    this.setData({ workOrders: processedOrders })
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
    this.filterWorkOrders()
  },

  clearSearch() {
    this.setData({ searchKeyword: '' })
    this.filterWorkOrders()
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/workorder/detail?id=' + id
    })
  },

  onStatusChange(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.value })
    this.loadWorkOrders()
  },

  getStatusText(status) {
    const statusMap = {
      'PLANNED': '待分配',
      'ASSIGNED': '已分配',
      'IN_PROGRESS': '进行中',
      'COMPLETED': '已完成'
    }
    return statusMap[status] || status
  },

  getStatusColor(status) {
    const colorMap = {
      'PLANNED': '#ff9800',
      'ASSIGNED': '#2196f3',
      'IN_PROGRESS': '#9c27b0',
      'COMPLETED': '#4caf50'
    }
    return colorMap[status] || '#999'
  },

  getStatusBgColor(status) {
    const colorMap = {
      'PLANNED': 'rgba(255, 152, 0, 0.1)',
      'ASSIGNED': 'rgba(33, 150, 243, 0.1)',
      'IN_PROGRESS': 'rgba(156, 39, 176, 0.1)',
      'COMPLETED': 'rgba(76, 175, 80, 0.1)'
    }
    return colorMap[status] || 'rgba(0, 0, 0, 0.05)'
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    return dateStr.substring(0, 16)
  },

  getStatusCount(status) {
    if (status === 'ALL') return this.data.allWorkOrders.length
    return this.data.allWorkOrders.filter(item => item.status === status).length
  },

  getProgress(wo) {
    if (!wo.plannedQuantity || wo.plannedQuantity === 0) return 0
    const actual = wo.actualQuantity || 0
    return Math.round((actual / wo.plannedQuantity) * 100)
  },

  getProgressColor(progress) {
    if (progress >= 100) return '#4caf50'
    if (progress >= 50) return '#ff9800'
    return '#9c27b0'
  }
})

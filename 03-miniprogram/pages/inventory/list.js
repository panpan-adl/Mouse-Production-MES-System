const { inventoryApi, materialApi, warehouseApi } = require('../../utils/api.js')

Page({
  data: {
    inventories: [],
    materials: [],
    warehouses: [],
    loading: false,
    searchText: '',
    selectedMaterialId: '',
    selectedWarehouseId: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    Promise.all([
      inventoryApi.list().catch(() => []),
      materialApi.list().catch(() => []),
      warehouseApi.list().catch(() => [])
    ]).then(([inventories, materials, warehouses]) => {
      this.setData({
        inventories,
        materials,
        warehouses,
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

  onSearch() {
    this.loadData()
  },

  clearSearch() {
    this.setData({ searchText: '', selectedMaterialId: '', selectedWarehouseId: '' })
    this.loadData()
  },

  selectMaterial(e) {
    this.setData({ selectedMaterialId: e.detail.value })
  },

  selectWarehouse(e) {
    this.setData({ selectedWarehouseId: e.detail.value })
  },

  getMaterialName(id) {
    const material = this.data.materials.find(m => m.id === id)
    return material ? material.materialName : '未知物料'
  },

  getWarehouseName(id) {
    const warehouse = this.data.warehouses.find(w => w.id === id)
    return warehouse ? warehouse.warehouseName : '未知仓库'
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/inventory/detail?id=' + id
    })
  },

  viewTransactions() {
    wx.showToast({ title: '库存流水功能开发中', icon: 'none' })
  },

  addInventory() {
    wx.showToast({ title: '新增库存功能开发中', icon: 'none' })
  }
})
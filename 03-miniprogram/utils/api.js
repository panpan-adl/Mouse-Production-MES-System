const isDev = true
const DEV_BASE_URL = 'http://172.24.79.157:8080'
const PROD_BASE_URL = 'http://your-domain.com:8080'
const BASE_URL = isDev ? DEV_BASE_URL : PROD_BASE_URL

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token || ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data
          if (data.code === 200) {
            resolve(data.data)
          } else {
            wx.showToast({
              title: data.msg || '请求失败',
              icon: 'none'
            })
            reject(data)
          }
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          const app = getApp()
          app.globalData.userInfo = null
          app.globalData.token = null
          wx.redirectTo({
            url: '/pages/login/login'
          })
          reject(res)
        } else {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        console.error('[API请求失败] URL:', BASE_URL + options.url, 'Error:', err.errMsg)
        wx.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

const login = (userCode, password) => {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data: { userCode, password }
  })
}

const logout = () => {
  return request({
    url: '/api/users/logout',
    method: 'POST'
  })
}

const getUserInfo = () => {
  return request({
    url: '/api/users/info',
    method: 'GET'
  })
}

const orderApi = {
  list: () => request({ url: '/api/orders', method: 'GET' }),
  get: (id) => request({ url: '/api/orders/' + id, method: 'GET' }),
  getByOrderNo: (orderNo) => {
    return new Promise((resolve) => {
      const token = wx.getStorageSync('token')
      wx.request({
        url: BASE_URL + '/api/orders/no/' + orderNo,
        method: 'GET',
        header: {
          'Content-Type': 'application/json',
          'Authorization': token || ''
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.code === 200) {
            resolve(res.data.data)
          } else {
            resolve(null)
          }
        },
        fail: () => {
          resolve(null)
        }
      })
    })
  },
  create: (data) => request({ url: '/api/orders', method: 'POST', data }),
  update: (id, data) => request({ url: '/api/orders/' + id, method: 'PUT', data }),
  delete: (id) => request({ url: '/api/orders/' + id, method: 'DELETE' }),
  confirm: (id) => request({ url: '/api/orders/' + id + '/confirm', method: 'PUT' }),
  cancel: (id) => request({ url: '/api/orders/' + id + '/cancel', method: 'PUT' })
}

const productionPlanApi = {
  list: () => request({ url: '/api/production-plans', method: 'GET' }),
  get: (id) => request({ url: '/api/production-plans/' + id, method: 'GET' }),
  getProgress: (id) => request({ url: '/api/production-plans/' + id + '/progress', method: 'GET' }),
  create: (data) => request({ url: '/api/production-plans', method: 'POST', data }),
  update: (id, data) => request({ url: '/api/production-plans/' + id, method: 'PUT', data }),
  delete: (id) => request({ url: '/api/production-plans/' + id, method: 'DELETE' }),
  createFromOrder: (orderId) => request({ url: '/api/production-plans/from-order/' + orderId, method: 'POST' })
}

const workOrderApi = {
  list: () => request({ url: '/api/work-order/list', method: 'GET' }),
  get: (id) => request({ url: '/api/work-order/' + id, method: 'GET' }),
  getByNo: (woNo) => request({ url: '/api/work-order/no/' + woNo, method: 'GET' }),
  getByProductionOrderId: (productionOrderId) => request({ url: '/api/work-order/production/' + productionOrderId, method: 'GET' }),
  getByStatus: (status) => request({ url: '/api/work-order/status/' + status, method: 'GET' }),
  getByUser: (userId) => request({ url: '/api/work-order/user/' + userId, method: 'GET' }),
  create: (data) => request({ url: '/api/work-order/add', method: 'POST', data }),
  update: (id, data) => request({ url: '/api/work-order/' + id, method: 'PUT', data }),
  delete: (id) => request({ url: '/api/work-order/' + id, method: 'DELETE' }),
  assign: (id, data) => request({ url: '/api/work-order/' + id + '/assign', method: 'PUT', data }),
  claim: (id, data) => request({ url: '/api/work-order/' + id + '/claim', method: 'PUT', data }),
  complete: (id, data) => request({ url: '/api/work-order/' + id + '/complete', method: 'PUT', data })
}

const productionReportApi = {
  list: () => request({ url: '/api/production-reports', method: 'GET' }),
  get: (id) => request({ url: '/api/production-reports/' + id, method: 'GET' }),
  getByTaskId: (taskId) => request({ url: '/api/production-reports/task/' + taskId, method: 'GET' }),
  getTaskSummary: (taskId) => request({ url: '/api/production-reports/task/' + taskId + '/summary', method: 'GET' }),
  getByUserId: (userId) => request({ url: '/api/production-reports/user/' + userId, method: 'GET' }),
  create: (data) => request({ url: '/api/production-reports', method: 'POST', data }),
  update: (id, data) => request({ url: '/api/production-reports/' + id, method: 'PUT', data })
}

const dashboardApi = {
  getSummary: () => request({ url: '/api/dashboard/summary', method: 'GET' }),
  getOrderSummary: () => request({ url: '/api/dashboard/summary', method: 'GET' }),
  getProductionSummary: () => request({ url: '/api/dashboard/summary', method: 'GET' }),
  getTaskSummary: () => request({ url: '/api/dashboard/summary', method: 'GET' }),
  getRecentOrders: () => request({ url: '/api/dashboard/recent-orders', method: 'GET' }),
  getRecentPlans: () => request({ url: '/api/dashboard/recent-plans', method: 'GET' }),
  getRecentWorkOrders: () => request({ url: '/api/dashboard/recent-work-orders', method: 'GET' })
}

module.exports = {
  request,
  login,
  logout,
  getUserInfo,
  BASE_URL,
  orderApi,
  productionPlanApi,
  workOrderApi,
  productionReportApi,
  dashboardApi
}
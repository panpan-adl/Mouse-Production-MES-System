const { productionReportApi } = require('../../utils/api.js')

Page({
  data: {
    taskId: '',
    tasks: [],
    taskOptions: [],
    form: {
      taskId: '',
      reportQuantity: '',
      reportDate: '',
      remark: ''
    },
    submitting: false
  },

  onLoad(options) {
    if (options.taskId) {
      this.setData({
        taskId: options.taskId,
        'form.taskId': options.taskId
      })
    }
    this.loadTasks()
    var today = new Date()
    var dateStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')
    this.setData({
      'form.reportDate': dateStr
    })
  },

  loadTasks() {
    this.setData({ tasks: [], taskOptions: ['暂无任务'] })
  },

  onInputChange(e) {
    var field = e.currentTarget.dataset.field
    this.setData({
      ['form.' + field]: e.detail.value
    })
  },

  onTaskChange(e) {
    var index = e.detail.value
    var task = this.data.tasks[index]
    if (task) {
      this.setData({
        'form.taskId': task.id
      })
    }
  },

  onSubmit() {
    var form = this.data.form
    
    if (!form.taskId) {
      wx.showToast({ title: '请选择生产任务', icon: 'none' })
      return
    }
    if (!form.reportQuantity) {
      wx.showToast({ title: '请输入报工数量', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    var submitData = {
      taskId: parseInt(form.taskId),
      reportQuantity: parseInt(form.reportQuantity) || 0,
      reportDate: form.reportDate,
      remark: form.remark
    }

    productionReportApi.create(submitData).then(function() {
      wx.showToast({ title: '报工成功', icon: 'success' })
      setTimeout(function() {
        wx.navigateBack()
      }, 1500)
    }).catch(function() {
      wx.showToast({ title: '报工失败', icon: 'none' })
    }).finally(function() {
      this.setData({ submitting: false })
    }.bind(this))
  },

  goBack() {
    wx.navigateBack()
  },

  getSelectedTaskText() {
    var form = this.data.form
    if (!form.taskId) return '请选择任务'
    var tasks = this.data.tasks
    for (var i = 0; i < tasks.length; i++) {
      if (tasks[i].id === parseInt(form.taskId)) {
        return tasks[i].taskNo + ' - ' + (tasks[i].productName || '')
      }
    }
    return '请选择任务'
  }
})
